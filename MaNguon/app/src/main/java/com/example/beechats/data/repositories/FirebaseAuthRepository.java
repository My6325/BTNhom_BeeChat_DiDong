package com.example.beechats.data.repositories;

import com.example.beechats.data.models.User;
import com.example.beechats.data.models.UserSettings;
import com.example.beechats.utils.ErrorHandler;
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

    public void login(String email, String password, OnAuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(ErrorHandler.getAuthErrorMessage(e)));
    }

    public void logout() {
        auth.signOut();
    }

    public void changePassword(String newPassword, OnAuthCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("Chưa đăng nhập");
            return;
        }
        user.updatePassword(newPassword)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(ErrorHandler.getAuthErrorMessage(e)));
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
