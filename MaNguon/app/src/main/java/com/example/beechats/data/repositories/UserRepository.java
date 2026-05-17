package com.example.beechats.data.repositories;

import com.example.beechats.data.models.User;
import com.example.beechats.data.models.UserSettings;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class UserRepository {

    public interface OnUserCallback {
        void onSuccess(User user);

        void onError(String errorMessage);
    }

    public interface OnCompleteCallback {
        void onSuccess();

        void onError(String errorMessage);
    }

    public interface OnUserListCallback {
        void onSuccess(List<User> users);

        void onError(String errorMessage);
    }

    private final FirebaseFirestore db;
    private static final String COLLECTION = "users";

    public UserRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /** Constructor cho phép inject dependency (dùng trong unit test). */
    public UserRepository(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Tạo document người dùng mới trong Firestore.
     *
     * @param user     Object User đã được điền đầy đủ field
     * @param callback Kết quả trả về
     */
    public void createUser(User user, OnCompleteCallback callback) {
        db.collection(COLLECTION)
                .document(user.getUserId())
                .set(user)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Lấy thông tin người dùng từ Firestore.
     *
     * @param userId   UID của người dùng cần lấy
     * @param callback Kết quả trả về (onSuccess với User object, hoặc onError)
     */
    public void getUser(String userId, OnUserCallback callback) {
        db.collection(COLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        User user = snapshot.toObject(User.class);
                        callback.onSuccess(user);
                    } else {
                        callback.onError("Người dùng không tồn tại");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Cập nhật thông tin profile: displayName, bio và tự động tái sinh
     * searchKeywords.
     * Validate displayName không được rỗng.
     *
     * @param userId      UID của người dùng
     * @param displayName Tên hiển thị mới (không được rỗng)
     * @param bio         Mô tả bản thân (có thể rỗng)
     * @param callback    Kết quả trả về
     */
    public void updateProfile(String userId, String displayName, String bio, OnCompleteCallback callback) {
        if (displayName == null || displayName.trim().isEmpty()) {
            callback.onError("Tên hiển thị không được để trống.");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("displayName", displayName.trim());
        updates.put("bio", bio != null ? bio : "");
        updates.put("searchKeywords", generateSearchKeywords(displayName.trim()));
        updates.put("updatedAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION)
                .document(userId)
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Cập nhật riêng searchKeywords dựa trên displayName hiện tại.
     * Dùng khi displayName thay đổi từ nguồn khác (ví dụ: sync từ Auth).
     *
     * @param userId      UID của người dùng
     * @param displayName Tên dùng để tái sinh keywords
     * @param callback    Kết quả trả về
     */
    public void updateSearchKeywords(String userId, String displayName, OnCompleteCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("searchKeywords", generateSearchKeywords(displayName));
        updates.put("updatedAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION)
                .document(userId)
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Cập nhật trạng thái online/offline và lastSeen của người dùng.
     *
     * @param userId   UID của người dùng
     * @param isOnline true = online, false = offline
     * @param callback Kết quả trả về
     */
    public void updateOnlineStatus(String userId, boolean isOnline, OnCompleteCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isOnline", isOnline);
        updates.put("lastSeen", FieldValue.serverTimestamp());

        db.collection(COLLECTION)
                .document(userId)
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Cập nhật cài đặt người dùng.
     *
     * @param userId   UID của người dùng
     * @param settings Object UserSettings mới
     * @param callback Kết quả trả về
     */
    public void updateSettings(String userId, UserSettings settings, OnCompleteCallback callback) {
        db.collection(COLLECTION)
                .document(userId)
                .update("settings", settings)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Cập nhật chỉ {@code settings.darkMode} (không ghi đè toàn bộ object settings).
     */
    public void updateDarkMode(String userId, boolean darkMode, OnCompleteCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onError("ID người dùng không hợp lệ.");
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("settings.darkMode", darkMode);
        updates.put("updatedAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION)
                .document(userId.trim())
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Lưu FCM token vào Firestore để nhận push notification.
     * Gọi sau khi đăng nhập thành công và khi token được làm mới.
     *
     * @param userId   UID của người dùng
     * @param token    FCM token mới (không được rỗng)
     * @param callback Kết quả trả về
     */
    public void updateFcmToken(String userId, String token, OnCompleteCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onError("ID người dùng không hợp lệ.");
            return;
        }
        if (token == null || token.trim().isEmpty()) {
            callback.onError("FCM Token không hợp lệ.");
            return;
        }

        db.collection(COLLECTION)
                .document(userId)
                .update("fcmToken", token.trim())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Xóa FCM token khỏi Firestore khi đăng xuất.
     * Đặt fcmToken = "" để thiết bị không còn nhận push notification.
     *
     * @param userId   UID của người dùng
     * @param callback Kết quả trả về
     */
    public void clearFcmToken(String userId, OnCompleteCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onError("ID người dùng không hợp lệ.");
            return;
        }

        db.collection(COLLECTION)
                .document(userId)
                .update("fcmToken", "")
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Xóa document người dùng khỏi Firestore.
     * Gọi trước khi xóa Auth user để tránh mất quyền ghi.
     *
     * @param userId   UID của người dùng cần xóa
     * @param callback Kết quả trả về
     */
    public void deleteUser(String userId, OnCompleteCallback callback) {
        db.collection(COLLECTION)
                .document(userId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Tìm kiếm người dùng theo keyword dùng mảng searchKeywords.
     * Loại chính người dùng hiện tại khỏi kết quả.
     *
     * @param keyword       Từ khóa tìm kiếm (không được rỗng)
     * @param currentUserId UID của người dùng đang tìm (để loại khỏi kết quả)
     * @param callback      Kết quả trả về (onSuccess với List<User>, hoặc onError)
     */
    public void searchUsers(String keyword, String currentUserId, OnUserListCallback callback) {
        if (keyword == null || keyword.trim().isEmpty()) {
            callback.onError("Từ khóa tìm kiếm không được để trống.");
            return;
        }
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            callback.onError("ID người dùng không hợp lệ.");
            return;
        }

        db.collection(COLLECTION)
                .whereArrayContains("searchKeywords", keyword.trim().toLowerCase())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<User> results = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        User user = doc.toObject(User.class);
                        if (user != null && !currentUserId.equals(user.getUserId())) {
                            results.add(user);
                        }
                    }
                    callback.onSuccess(results);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Sinh mảng searchKeywords từ displayName: bỏ dấu tiếng Việt, lowercase,
     * tạo tất cả prefix của từng từ (để hỗ trợ tìm kiếm array-contains).
     * Ví dụ: "Bee" → ["b", "be", "bee"]
     *
     * @param displayName Tên hiển thị cần sinh keywords
     * @return Danh sách keyword (có thể rỗng nếu displayName null/empty)
     */
    public static List<String> generateSearchKeywords(String displayName) {
        List<String> keywords = new ArrayList<>();
        if (displayName == null || displayName.isEmpty())
            return keywords;

        // Bỏ dấu tiếng Việt, lowercase
        String normalized = Normalizer.normalize(displayName, Normalizer.Form.NFD);
        normalized = Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(normalized).replaceAll("");
        normalized = normalized.toLowerCase().trim();

        // Sinh prefix cho từng từ
        String[] words = normalized.split("\\s+");
        for (String word : words) {
            if (word.isEmpty())
                continue;
            for (int i = 1; i <= word.length(); i++) {
                keywords.add(word.substring(0, i));
            }
        }
        return keywords;
    }
}
