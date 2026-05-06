package com.example.beechats.data.repositories;

import com.example.beechats.data.models.Conversation;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.CollectionReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConversationRepository {

    public interface OnConversationCallback {
        void onSuccess(String conversationId);
        void onError(String errorMessage);
    }

    public interface OnGetConversationCallback {
        void onSuccess(Conversation conversation);
        void onError(String errorMessage);
    }

    public interface OnCompleteCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    private final FirebaseFirestore db;
    private static final String COLLECTION = "conversations";

    public ConversationRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /** Constructor cho phép inject dependency (dùng trong unit test). */
    public ConversationRepository(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Tạo hoặc lấy hội thoại 1-1 giữa hai người dùng.
     * ID hội thoại được tạo theo quy tắc deterministic: sort([uid1, uid2]).join("_").
     * Dùng SetOptions.merge() để idempotent — gọi nhiều lần không overwrite dữ liệu cũ.
     *
     * @param uid1     UID người dùng thứ nhất
     * @param uid2     UID người dùng thứ hai
     * @param callback Kết quả trả về (onSuccess với conversationId, hoặc onError)
     */
    public void createOrGetPrivateConversation(String uid1, String uid2, OnConversationCallback callback) {
        if (uid1 == null || uid1.trim().isEmpty() || uid2 == null || uid2.trim().isEmpty()) {
            callback.onError("ID người dùng không hợp lệ.");
            return;
        }

        // Sắp xếp để đảm bảo cùng ID dù đổi thứ tự uid1/uid2
        List<String> sorted = Arrays.asList(uid1.trim(), uid2.trim());
        Collections.sort(sorted);
        String conversationId = sorted.get(0) + "_" + sorted.get(1);

        Map<String, Object> data = new HashMap<>();
        data.put("type", "private");
        data.put("participants", sorted);
        data.put("updatedAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION)
                .document(conversationId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess(conversationId))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Tạo nhóm chat với nhiều thành viên.
     * Dùng WriteBatch để atomic: tạo conversation document (type="group") + subcollection
     * members/{userId} cho từng thành viên. Người tạo tự động là admin.
     *
     * @param creatorId  UID người tạo nhóm (tự động thêm vào participants nếu chưa có)
     * @param groupName  Tên nhóm (không được rỗng)
     * @param memberIds  Danh sách UID thành viên (không được null/rỗng)
     * @param callback   Kết quả trả về (onSuccess với conversationId, hoặc onError)
     */
    public void createGroupConversation(String creatorId, String groupName,
                                        List<String> memberIds, OnConversationCallback callback) {
        if (creatorId == null || creatorId.trim().isEmpty()) {
            callback.onError("ID người tạo không hợp lệ.");
            return;
        }
        if (groupName == null || groupName.trim().isEmpty()) {
            callback.onError("Tên nhóm không được để trống.");
            return;
        }
        if (memberIds == null || memberIds.isEmpty()) {
            callback.onError("Danh sách thành viên không hợp lệ.");
            return;
        }

        // Đảm bảo creator có trong danh sách participants
        List<String> participants = new ArrayList<>(memberIds);
        String trimmedCreatorId = creatorId.trim();
        if (!participants.contains(trimmedCreatorId)) {
            participants.add(trimmedCreatorId);
        }

        // Tạo conversation doc với random ID
        DocumentReference convRef = db.collection(COLLECTION).document();
        String conversationId = convRef.getId();

        Map<String, Object> convData = new HashMap<>();
        convData.put("type", "group");
        convData.put("groupName", groupName.trim());
        convData.put("participants", participants);
        convData.put("participantCount", participants.size());
        convData.put("adminIds", Collections.singletonList(trimmedCreatorId));
        convData.put("createdBy", trimmedCreatorId);
        convData.put("createdAt", FieldValue.serverTimestamp());
        convData.put("updatedAt", FieldValue.serverTimestamp());

        WriteBatch batch = db.batch();
        batch.set(convRef, convData);

        // Tạo member document trong subcollection members/ cho từng thành viên
        CollectionReference membersRef = convRef.collection("members");
        for (String memberId : participants) {
            DocumentReference memberRef = membersRef.document(memberId);
            Map<String, Object> memberData = new HashMap<>();
            memberData.put("memberId", memberId);
            memberData.put("role", memberId.equals(trimmedCreatorId) ? "admin" : "member");
            memberData.put("joinedAt", FieldValue.serverTimestamp());
            batch.set(memberRef, memberData);
        }

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(conversationId))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Thêm thành viên vào nhóm chat.
     * Dùng WriteBatch để atomic: cập nhật participants (arrayUnion) + participantCount +
     * tạo member document trong subcollection members/.
     *
     * @param convId   ID hội thoại nhóm
     * @param userId   UID thành viên cần thêm
     * @param callback Kết quả trả về
     */
    public void addMember(String convId, String userId, OnCompleteCallback callback) {
        if (convId == null || convId.trim().isEmpty()) {
            callback.onError("ID hội thoại không hợp lệ.");
            return;
        }
        if (userId == null || userId.trim().isEmpty()) {
            callback.onError("ID thành viên không hợp lệ.");
            return;
        }

        String trimmedConvId = convId.trim();
        String trimmedUserId = userId.trim();

        DocumentReference convRef = db.collection(COLLECTION).document(trimmedConvId);
        DocumentReference memberRef = convRef.collection("members").document(trimmedUserId);

        Map<String, Object> convUpdate = new HashMap<>();
        convUpdate.put("participants", FieldValue.arrayUnion(trimmedUserId));
        convUpdate.put("participantCount", FieldValue.increment(1));
        convUpdate.put("updatedAt", FieldValue.serverTimestamp());

        Map<String, Object> memberData = new HashMap<>();
        memberData.put("memberId", trimmedUserId);
        memberData.put("role", "member");
        memberData.put("joinedAt", FieldValue.serverTimestamp());

        WriteBatch batch = db.batch();
        batch.update(convRef, convUpdate);
        batch.set(memberRef, memberData);

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Xóa thành viên khỏi nhóm chat. Chỉ admin mới có quyền.
     * Đọc role của requesterId trước, sau đó dùng WriteBatch atomic: arrayRemove + delete member doc.
     *
     * @param convId      ID hội thoại nhóm
     * @param userId      UID thành viên cần xóa
     * @param requesterId UID người yêu cầu (phải là admin)
     * @param callback    Kết quả trả về
     */
    public void removeMember(String convId, String userId, String requesterId,
                             OnCompleteCallback callback) {
        if (convId == null || convId.trim().isEmpty()) {
            callback.onError("ID hội thoại không hợp lệ.");
            return;
        }
        if (userId == null || userId.trim().isEmpty()) {
            callback.onError("ID thành viên không hợp lệ.");
            return;
        }
        if (requesterId == null || requesterId.trim().isEmpty()) {
            callback.onError("ID người yêu cầu không hợp lệ.");
            return;
        }

        String trimmedConvId = convId.trim();
        String trimmedUserId = userId.trim();
        String trimmedRequesterId = requesterId.trim();

        DocumentReference convRef = db.collection(COLLECTION).document(trimmedConvId);
        DocumentReference requesterRef = convRef.collection("members").document(trimmedRequesterId);

        // Kiểm tra quyền admin của requester trước khi thực hiện
        requesterRef.get()
                .addOnFailureListener(e -> callback.onError(e.getMessage()))
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        callback.onError("Người yêu cầu không phải thành viên nhóm.");
                        return;
                    }
                    String role = snapshot.getString("role");
                    if (!"admin".equals(role)) {
                        callback.onError("Chỉ admin mới có thể xóa thành viên.");
                        return;
                    }

                    DocumentReference memberRef = convRef.collection("members").document(trimmedUserId);

                    Map<String, Object> convUpdate = new HashMap<>();
                    convUpdate.put("participants", FieldValue.arrayRemove(trimmedUserId));
                    convUpdate.put("participantCount", FieldValue.increment(-1));
                    convUpdate.put("updatedAt", FieldValue.serverTimestamp());

                    WriteBatch batch = db.batch();
                    batch.update(convRef, convUpdate);
                    batch.delete(memberRef);

                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                });
    }

    /**
     * Lấy thông tin hội thoại từ Firestore.
     *
     * @param conversationId ID hội thoại cần lấy
     * @param callback       Kết quả trả về (onSuccess với Conversation object, hoặc onError)
     */
    public void getConversation(String conversationId, OnGetConversationCallback callback) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            callback.onError("ID hội thoại không hợp lệ.");
            return;
        }

        db.collection(COLLECTION)
                .document(conversationId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Conversation conversation = snapshot.toObject(Conversation.class);
                        callback.onSuccess(conversation);
                    } else {
                        callback.onError("Hội thoại không tồn tại.");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}
