package com.example.beechats.data.repositories;

import com.example.beechats.data.models.Friend;
import com.example.beechats.data.models.FriendRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendRepository {

    public interface OnCompleteCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public interface OnFriendRequestCallback {
        void onSuccess(String requestId);
        void onError(String errorMessage);
    }

    public interface OnFriendListCallback {
        void onSuccess(List<Friend> friends);
        void onError(String errorMessage);
    }

    public interface OnRequestListCallback {
        void onSuccess(List<FriendRequest> requests);
        void onError(String errorMessage);
    }

    // Collection gốc chứa tất cả lời mời kết bạn
    private static final String FRIEND_REQUESTS = "friendRequests";
    // Collection gốc chứa danh sách bạn bè
    private static final String FRIENDS = "friends";
    // Subcollection bên trong friends/{uid}/
    private static final String FRIEND_LIST = "friendList";

    private final FirebaseFirestore db;

    public FriendRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /** Constructor inject dependency cho unit test. */
    public FriendRepository(FirebaseFirestore db) {
        this.db = db;
    }

    // -------------------------------------------------------------------------
    // 1. sendFriendRequest
    // -------------------------------------------------------------------------

    /**
     * Gửi lời mời kết bạn từ fromUid đến toUid.
     * Tạo document trong `friendRequests` với status="pending".
     *
     * @param fromUid  UID người gửi
     * @param toUid    UID người nhận
     * @param callback Kết quả (onSuccess với requestId, hoặc onError)
     */
    public void sendFriendRequest(String fromUid, String toUid, OnFriendRequestCallback callback) {
        if (fromUid == null || fromUid.trim().isEmpty()
                || toUid == null || toUid.trim().isEmpty()) {
            callback.onError("UID người dùng không hợp lệ.");
            return;
        }
        if (fromUid.trim().equals(toUid.trim())) {
            callback.onError("Không thể gửi lời mời cho chính mình.");
            return;
        }

        DocumentReference requestRef = db.collection(FRIEND_REQUESTS).document();
        String requestId = requestRef.getId();

        Map<String, Object> data = new HashMap<>();
        data.put("requestId", requestId);
        data.put("fromUserId", fromUid.trim());
        data.put("toUserId", toUid.trim());
        data.put("status", "pending");
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("updatedAt", FieldValue.serverTimestamp());

        requestRef.set(data)
                .addOnSuccessListener(unused -> callback.onSuccess(requestId))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // -------------------------------------------------------------------------
    // 2. acceptFriendRequest
    // -------------------------------------------------------------------------

    /**
     * Chấp nhận lời mời kết bạn.
     * Dùng WriteBatch để đồng bộ: cập nhật friendRequests status + tạo friends cả 2 phía.
     *
     * @param requestId ID của lời mời
     * @param fromUid   UID người đã gửi lời mời (A)
     * @param toUid     UID người nhận và chấp nhận (B)
     * @param callback  Kết quả hoàn thành
     */
    public void acceptFriendRequest(String requestId, String fromUid, String toUid,
                                    OnCompleteCallback callback) {
        if (requestId == null || requestId.trim().isEmpty()
                || fromUid == null || fromUid.trim().isEmpty()
                || toUid == null || toUid.trim().isEmpty()) {
            callback.onError("Tham số không hợp lệ.");
            return;
        }

        WriteBatch batch = db.batch();

        // Cập nhật status lời mời → "accepted"
        DocumentReference requestRef = db.collection(FRIEND_REQUESTS).document(requestId.trim());
        Map<String, Object> statusUpdate = new HashMap<>();
        statusUpdate.put("status", "accepted");
        statusUpdate.put("updatedAt", FieldValue.serverTimestamp());
        batch.update(requestRef, statusUpdate);

        // Tạo friends/{fromUid}/friendList/{toUid}
        DocumentReference friendRefA = db.collection(FRIENDS)
                .document(fromUid.trim())
                .collection(FRIEND_LIST)
                .document(toUid.trim());
        Map<String, Object> friendDataA = new HashMap<>();
        friendDataA.put("friendUserId", toUid.trim());
        friendDataA.put("addedAt", FieldValue.serverTimestamp());
        batch.set(friendRefA, friendDataA);

        // Tạo friends/{toUid}/friendList/{fromUid}
        DocumentReference friendRefB = db.collection(FRIENDS)
                .document(toUid.trim())
                .collection(FRIEND_LIST)
                .document(fromUid.trim());
        Map<String, Object> friendDataB = new HashMap<>();
        friendDataB.put("friendUserId", fromUid.trim());
        friendDataB.put("addedAt", FieldValue.serverTimestamp());
        batch.set(friendRefB, friendDataB);

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // -------------------------------------------------------------------------
    // 3. declineFriendRequest
    // -------------------------------------------------------------------------

    /**
     * Từ chối lời mời kết bạn → cập nhật status="declined".
     *
     * @param requestId ID của lời mời
     * @param callback  Kết quả hoàn thành
     */
    public void declineFriendRequest(String requestId, OnCompleteCallback callback) {
        if (requestId == null || requestId.trim().isEmpty()) {
            callback.onError("Request ID không hợp lệ.");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("status", "declined");
        data.put("updatedAt", FieldValue.serverTimestamp());

        db.collection(FRIEND_REQUESTS)
                .document(requestId.trim())
                .update(data)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // -------------------------------------------------------------------------
    // 4. unfriend
    // -------------------------------------------------------------------------

    /**
     * Hủy kết bạn giữa uidA và uidB.
     * Dùng WriteBatch để xóa đồng thời 2 document:
     * friends/{uidA}/friendList/{uidB} và friends/{uidB}/friendList/{uidA}.
     *
     * @param uidA     UID người thứ nhất
     * @param uidB     UID người thứ hai
     * @param callback Kết quả hoàn thành
     */
    public void unfriend(String uidA, String uidB, OnCompleteCallback callback) {
        if (uidA == null || uidA.trim().isEmpty()
                || uidB == null || uidB.trim().isEmpty()) {
            callback.onError("UID người dùng không hợp lệ.");
            return;
        }

        WriteBatch batch = db.batch();

        DocumentReference friendRefA = db.collection(FRIENDS)
                .document(uidA.trim())
                .collection(FRIEND_LIST)
                .document(uidB.trim());

        DocumentReference friendRefB = db.collection(FRIENDS)
                .document(uidB.trim())
                .collection(FRIEND_LIST)
                .document(uidA.trim());

        batch.delete(friendRefA);
        batch.delete(friendRefB);

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // -------------------------------------------------------------------------
    // 5. getPendingRequests
    // -------------------------------------------------------------------------

    /**
     * Lấy danh sách lời mời kết bạn đang chờ xử lý (status="pending") gửi đến uid.
     *
     * @param uid      UID người nhận cần lấy danh sách
     * @param callback Kết quả (onSuccess với List<FriendRequest>)
     */
    public void getPendingRequests(String uid, OnRequestListCallback callback) {
        if (uid == null || uid.trim().isEmpty()) {
            callback.onError("UID không hợp lệ.");
            return;
        }

        db.collection(FRIEND_REQUESTS)
                .whereEqualTo("toUserId", uid.trim())
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<FriendRequest> requests = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        FriendRequest req = doc.toObject(FriendRequest.class);
                        if (req != null) {
                            requests.add(req);
                        }
                    }
                    callback.onSuccess(requests);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // -------------------------------------------------------------------------
    // 6. getFriendList
    // -------------------------------------------------------------------------

    /**
     * Lấy danh sách bạn bè của uid từ subcollection friends/{uid}/friendList.
     *
     * @param uid      UID cần lấy danh sách bạn bè
     * @param callback Kết quả (onSuccess với List<Friend>)
     */
    public void getFriendList(String uid, OnFriendListCallback callback) {
        if (uid == null || uid.trim().isEmpty()) {
            callback.onError("UID không hợp lệ.");
            return;
        }

        db.collection(FRIENDS)
                .document(uid.trim())
                .collection(FRIEND_LIST)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Friend> friends = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Friend friend = doc.toObject(Friend.class);
                        if (friend != null) {
                            friends.add(friend);
                        }
                    }
                    callback.onSuccess(friends);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}
