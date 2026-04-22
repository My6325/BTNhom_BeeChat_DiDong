package com.example.beechats.data.repositories;

import com.example.beechats.data.models.User;
import com.example.beechats.data.models.UserSettings;
import com.example.beechats.utils.ErrorHandler;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FirebaseAuthRepository {

    public interface OnAuthCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    private final FirebaseAuth auth;
    private final UserRepository userRepository;

    public FirebaseAuthRepository() {
        this.auth = FirebaseAuth.getInstance();
        this.userRepository = new UserRepository();
    }

    /** Constructor cho phép inject dependency (dùng trong unit test). */
    public FirebaseAuthRepository(FirebaseAuth auth, UserRepository userRepository) {
        this.auth = auth;
        this.userRepository = userRepository;
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    /**
     * Đăng ký tài khoản mới: tạo Auth user → tạo Firestore document users/{uid}.
     * Chỉ gọi onSuccess() sau khi cả hai bước hoàn tất.
     *
     * @param email       Email đăng ký
     * @param password    Mật khẩu (≥ 6 ký tự)
     * @param displayName Tên hiển thị
     * @param callback    Kết quả trả về
     */
    public void register(String email, String password, String displayName, OnAuthCallback callback) {
        // Validate input trước khi gọi Firebase
        if (email == null || email.trim().isEmpty()) {
            callback.onError("Email không được để trống.");
            return;
        }
        if (password == null || password.isEmpty()) {
            callback.onError("Mật khẩu không được để trống.");
            return;
        }
        if (displayName == null || displayName.trim().isEmpty()) {
            callback.onError("Tên hiển thị không được để trống.");
            return;
        }

        auth.createUserWithEmailAndPassword(email.trim(), password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();

                    // Xây dựng User document với giá trị mặc định
                    User newUser = new User();
                    newUser.setUserId(uid);
                    newUser.setDisplayName(displayName.trim());
                    newUser.setEmail(email.trim());
                    newUser.setPhotoUrl(null);
                    newUser.setBio("");
                    newUser.setSearchKeywords(UserRepository.generateSearchKeywords(displayName.trim()));
                    newUser.setOnline(false);
                    newUser.setSettings(new UserSettings());
                    newUser.setFcmToken("");

                    // Tạo document Firestore — chỉ onSuccess() sau khi cả hai xong
                    userRepository.createUser(newUser, new UserRepository.OnCompleteCallback() {
                        @Override
                        public void onSuccess() {
                            callback.onSuccess();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            callback.onError(errorMessage);
                        }
                    });
                })
                .addOnFailureListener(e -> callback.onError(ErrorHandler.getAuthErrorMessage(e)));
    }

    /**
     * Đăng nhập bằng email/password, cập nhật isOnline=true và lastSeen sau khi xác thực thành công.
     * Nếu Firestore update thất bại, vẫn gọi onSuccess() để không block đăng nhập.
     *
     * @param email    Email đăng nhập
     * @param password Mật khẩu
     * @param callback Kết quả trả về
     */
    public void login(String email, String password, OnAuthCallback callback) {
        if (email == null || email.trim().isEmpty()) {
            callback.onError("Email không được để trống.");
            return;
        }
        if (password == null || password.isEmpty()) {
            callback.onError("Mật khẩu không được để trống.");
            return;
        }

        auth.signInWithEmailAndPassword(email.trim(), password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    // Cập nhật trạng thái online — không block đăng nhập nếu Firestore fail
                    userRepository.updateOnlineStatus(uid, true, new UserRepository.OnCompleteCallback() {
                        @Override
                        public void onSuccess() {
                            callback.onSuccess();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            // Auth thành công — vẫn cho đăng nhập dù Firestore update thất bại
                            callback.onSuccess();
                        }
                    });
                })
                .addOnFailureListener(e -> callback.onError(ErrorHandler.getAuthErrorMessage(e)));
    }

    /**
     * Đăng xuất: set isOnline=false (fire-and-forget) rồi signOut ngay.
     * Không có callback — signOut luôn thành công ở local.
     */
    public void logout() {
        String uid = getCurrentUserId();
        if (uid != null) {
            // Fire-and-forget: không đợi Firestore để đảm bảo UX
            userRepository.updateOnlineStatus(uid, false, new UserRepository.OnCompleteCallback() {
                @Override public void onSuccess() {}
                @Override public void onError(String errorMessage) {}
            });
        }
        auth.signOut();
    }

    /**
     * Đổi mật khẩu: re-authenticate bằng mật khẩu hiện tại trước, sau đó mới cập nhật mật khẩu mới.
     * Firebase yêu cầu re-authentication để bảo mật cho thao tác nhạy cảm này.
     *
     * @param currentPassword Mật khẩu hiện tại (dùng để re-authenticate)
     * @param newPassword     Mật khẩu mới (phải ≥ 6 ký tự)
     * @param callback        Kết quả trả về
     */
    public void changePassword(String currentPassword, String newPassword, OnAuthCallback callback) {
        if (newPassword == null || newPassword.isEmpty()) {
            callback.onError("Mật khẩu không được để trống.");
            return;
        }
        if (newPassword.length() < 6) {
            callback.onError("Mật khẩu phải có ít nhất 6 ký tự.");
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("Chưa đăng nhập");
            return;
        }

        String email = user.getEmail();
        user.reauthenticate(getEmailCredential(email, currentPassword))
                .addOnSuccessListener(unused ->
                        user.updatePassword(newPassword)
                                .addOnSuccessListener(v -> callback.onSuccess())
                                .addOnFailureListener(e -> callback.onError(ErrorHandler.getAuthErrorMessage(e)))
                )
                .addOnFailureListener(e -> callback.onError(ErrorHandler.getAuthErrorMessage(e)));
    }

    /** Protected để cho phép override trong unit test (tránh Android TextUtils không mock được). */
    protected com.google.firebase.auth.AuthCredential getEmailCredential(String email, String password) {
        return EmailAuthProvider.getCredential(email, password);
    }

    public void deleteAccount(OnAuthCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("Chưa đăng nhập");
            return;
        }
        user.delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(ErrorHandler.getAuthErrorMessage(e)));
    }
}
