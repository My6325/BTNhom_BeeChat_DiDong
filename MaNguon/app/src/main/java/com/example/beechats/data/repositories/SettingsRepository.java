package com.example.beechats.data.repositories;

import com.example.beechats.data.models.User;
import com.example.beechats.data.models.UserSettings;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SettingsRepository {

    public interface OnCompleteCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public interface OnSettingsCallback {
        void onSuccess(UserSettings settings);
        void onError(String errorMessage);
    }

    public interface OnBooleanCallback {
        void onResult(boolean value);
        void onError(String errorMessage);
    }

    private final FirebaseFirestore db;
    private static final String COLLECTION = "users";

    public SettingsRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /** Constructor cho phép inject dependency (dùng trong unit test). */
    public SettingsRepository(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Cập nhật cài đặt ẩn danh của người dùng vào Firestore.
     * Dùng dot-notation để chỉ cập nhật nested fields mà không ghi đè toàn bộ settings map.
     *
     * @param userId               UID người dùng
     * @param isOnlineVisible      true = hiển thị online cho người khác
     * @param isReadReceiptVisible true = cập nhật readBy khi đọc tin nhắn
     * @param callback             Kết quả trả về
     */
    public void updatePrivacySettings(String userId, boolean isOnlineVisible,
                                      boolean isReadReceiptVisible, OnCompleteCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onError("ID người dùng không hợp lệ.");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("settings.isOnlineVisible", isOnlineVisible);
        updates.put("settings.isReadReceiptVisible", isReadReceiptVisible);

        db.collection(COLLECTION)
                .document(userId.trim())
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Đọc cài đặt privacy hiện tại của người dùng từ Firestore.
     *
     * @param userId   UID người dùng
     * @param callback Kết quả trả về (onSuccess với UserSettings, hoặc onError)
     */
    public void getPrivacySettings(String userId, OnSettingsCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onError("ID người dùng không hợp lệ.");
            return;
        }

        db.collection(COLLECTION)
                .document(userId.trim())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || !snapshot.exists()) {
                        callback.onError("Không tìm thấy thông tin người dùng.");
                        return;
                    }
                    User user = snapshot.toObject(User.class);
                    if (user == null || user.getSettings() == null) {
                        callback.onError("Không tìm thấy cài đặt người dùng.");
                        return;
                    }
                    callback.onSuccess(user.getSettings());
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Trả về trạng thái online thực tế, tôn trọng cài đặt `isOnlineVisible`.
     * - Nếu `isOnlineVisible=false` → luôn trả về false (ẩn online với người khác).
     * - Nếu `isOnlineVisible=true` → trả về giá trị thực của `isOnline`.
     *
     * @param userId   UID người dùng cần kiểm tra
     * @param callback Kết quả trả về (onResult với boolean, hoặc onError)
     */
    public void getEffectiveOnlineStatus(String userId, OnBooleanCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onError("ID người dùng không hợp lệ.");
            return;
        }

        db.collection(COLLECTION)
                .document(userId.trim())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || !snapshot.exists()) {
                        callback.onError("Không tìm thấy thông tin người dùng.");
                        return;
                    }
                    User user = snapshot.toObject(User.class);
                    if (user == null) {
                        callback.onError("Không thể đọc dữ liệu người dùng.");
                        return;
                    }
                    // Nếu ẩn online → luôn trả false dù isOnline thực có là gì
                    UserSettings settings = user.getSettings();
                    boolean isOnlineVisible = (settings == null) || settings.isOnlineVisible();
                    boolean effectiveStatus = isOnlineVisible && user.isOnline();
                    callback.onResult(effectiveStatus);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}
