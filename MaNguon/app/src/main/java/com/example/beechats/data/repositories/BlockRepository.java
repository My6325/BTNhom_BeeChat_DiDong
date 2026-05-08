package com.example.beechats.data.repositories;

import com.example.beechats.data.models.BlockedUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockRepository {

    public interface OnCompleteCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public interface OnBooleanCallback {
        void onResult(boolean isBlocked);
        void onError(String errorMessage);
    }

    public interface OnBlockedListCallback {
        void onSuccess(List<BlockedUser> blockedUsers);
        void onError(String errorMessage);
    }

    // blockedUsers/{userId}/blockedList/{targetId}
    private static final String BLOCKED_USERS = "blockedUsers";
    private static final String BLOCKED_LIST  = "blockedList";

    private final FirebaseFirestore db;

    public BlockRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /** Constructor inject dependency cho unit test. */
    public BlockRepository(FirebaseFirestore db) {
        this.db = db;
    }

    // -------------------------------------------------------------------------
    // 1. blockUser
    // -------------------------------------------------------------------------

    /**
     * Chặn người dùng: tạo document tại
     * {@code blockedUsers/{userId}/blockedList/{targetUserId}}.
     *
     * @param userId       UID của người thực hiện chặn
     * @param targetUserId UID của người bị chặn
     * @param callback     Kết quả trả về
     */
    public void blockUser(String userId, String targetUserId, OnCompleteCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onError("ID người dùng không hợp lệ.");
            return;
        }
        if (targetUserId == null || targetUserId.trim().isEmpty()) {
            callback.onError("ID người bị chặn không hợp lệ.");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("blockedUserId", targetUserId);
        data.put("blockedAt", FieldValue.serverTimestamp());

        db.collection(BLOCKED_USERS)
                .document(userId)
                .collection(BLOCKED_LIST)
                .document(targetUserId)
                .set(data)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // -------------------------------------------------------------------------
    // 2. unblockUser
    // -------------------------------------------------------------------------

    /**
     * Bỏ chặn người dùng: xóa document tại
     * {@code blockedUsers/{userId}/blockedList/{targetUserId}}.
     *
     * @param userId       UID của người thực hiện bỏ chặn
     * @param targetUserId UID của người được bỏ chặn
     * @param callback     Kết quả trả về
     */
    public void unblockUser(String userId, String targetUserId, OnCompleteCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onError("ID người dùng không hợp lệ.");
            return;
        }
        if (targetUserId == null || targetUserId.trim().isEmpty()) {
            callback.onError("ID người bị chặn không hợp lệ.");
            return;
        }

        db.collection(BLOCKED_USERS)
                .document(userId)
                .collection(BLOCKED_LIST)
                .document(targetUserId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // -------------------------------------------------------------------------
    // 3. isBlocked
    // -------------------------------------------------------------------------

    /**
     * Kiểm tra xem {@code userId} có đang chặn {@code targetUserId} hay không.
     * Kiểm tra document tại {@code blockedUsers/{userId}/blockedList/{targetUserId}}.
     *
     * @param userId       UID của người kiểm tra
     * @param targetUserId UID cần kiểm tra
     * @param callback     Trả về onResult(true) nếu đã chặn, onResult(false) nếu chưa
     */
    public void isBlocked(String userId, String targetUserId, OnBooleanCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onError("ID người dùng không hợp lệ.");
            return;
        }
        if (targetUserId == null || targetUserId.trim().isEmpty()) {
            callback.onError("ID người cần kiểm tra không hợp lệ.");
            return;
        }

        db.collection(BLOCKED_USERS)
                .document(userId)
                .collection(BLOCKED_LIST)
                .document(targetUserId)
                .get()
                .addOnSuccessListener(snapshot -> callback.onResult(snapshot.exists()))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // -------------------------------------------------------------------------
    // 4. getBlockedList
    // -------------------------------------------------------------------------

    /**
     * Lấy danh sách những người mà {@code userId} đã chặn.
     * Query subcollection {@code blockedUsers/{userId}/blockedList}.
     *
     * @param userId   UID của người dùng
     * @param callback Trả về List<BlockedUser> hoặc onError
     */
    public void getBlockedList(String userId, OnBlockedListCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onError("ID người dùng không hợp lệ.");
            return;
        }

        db.collection(BLOCKED_USERS)
                .document(userId)
                .collection(BLOCKED_LIST)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<BlockedUser> result = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        BlockedUser blocked = doc.toObject(BlockedUser.class);
                        if (blocked != null) {
                            result.add(blocked);
                        }
                    }
                    callback.onSuccess(result);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}
