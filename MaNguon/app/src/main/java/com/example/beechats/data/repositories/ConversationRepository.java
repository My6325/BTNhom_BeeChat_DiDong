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
    private static final String MSG_COLLECTION = "messages";

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
            .addOnSuccessListener(requesterSnap -> {
                if (!requesterSnap.exists()) {
                    callback.onError("Người yêu cầu không phải thành viên nhóm.");
                    return;
                }
                String role = requesterSnap.getString("role");
                if (!"admin".equals(role)) {
                    callback.onError("Chỉ admin mới có thể xóa thành viên.");
                    return;
                }

                DocumentReference memberRef = convRef.collection("members").document(trimmedUserId);
                memberRef.get()
                    .addOnFailureListener(e -> callback.onError(e.getMessage()))
                    .addOnSuccessListener(memberSnap -> {
                        if (!memberSnap.exists()) {
                            callback.onError("Thành viên đã được xóa hoặc không tồn tại.");
                            return;
                        }

                        convRef.get()
                            .addOnFailureListener(e -> callback.onError(e.getMessage()))
                            .addOnSuccessListener(convSnap -> {
                                @SuppressWarnings("unchecked")
                                List<String> participants = (List<String>) convSnap.get("participants");
                                boolean shouldDecrement = participants != null && participants.contains(trimmedUserId);

                                WriteBatch batch = db.batch();
                                Map<String, Object> convUpdate = new HashMap<>();
                                convUpdate.put("participants", FieldValue.arrayRemove(trimmedUserId));
                                convUpdate.put("updatedAt", FieldValue.serverTimestamp());
                                if (shouldDecrement) {
                                    convUpdate.put("participantCount", FieldValue.increment(-1));
                                }

                                batch.update(convRef, convUpdate);
                                batch.delete(memberRef);

                                batch.commit()
                                    .addOnSuccessListener(unused -> callback.onSuccess())
                                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
                            });
                    });
            });
    }

    /**
     * Thay đổi vai trò của thành viên trong nhóm chat.
     * Chỉ admin mới có thể thay đổi. Admin cuối cùng không thể bị hạ cấp.
     * Khi thăng: WriteBatch update members/{memberId}.role + arrayUnion(adminIds).
     * Khi hạ: kiểm tra adminIds.size() > 1 trước, sau đó WriteBatch + arrayRemove(adminIds).
     *
     * @param convId      ID hội thoại nhóm
     * @param memberId    UID thành viên cần thay đổi role
     * @param requesterId UID người yêu cầu (phải là admin)
     * @param newRole     Vai trò mới ("admin" hoặc "member")
     * @param callback    Kết quả trả về
     */
    public void setMemberRole(String convId, String memberId, String requesterId,
                              String newRole, OnCompleteCallback callback) {
        if (convId == null || convId.trim().isEmpty()) {
            callback.onError("ID hội thoại không hợp lệ.");
            return;
        }
        if (memberId == null || memberId.trim().isEmpty()) {
            callback.onError("ID thành viên không hợp lệ.");
            return;
        }
        if (requesterId == null || requesterId.trim().isEmpty()) {
            callback.onError("ID người yêu cầu không hợp lệ.");
            return;
        }
        if (!"admin".equals(newRole) && !"member".equals(newRole)) {
            callback.onError("Vai trò không hợp lệ. Chỉ chấp nhận \"admin\" hoặc \"member\".");
            return;
        }

        String trimmedConvId = convId.trim();
        String trimmedMemberId = memberId.trim();
        String trimmedRequesterId = requesterId.trim();

        DocumentReference convRef = db.collection(COLLECTION).document(trimmedConvId);
        DocumentReference requesterRef = convRef.collection("members").document(trimmedRequesterId);
        DocumentReference memberRef = convRef.collection("members").document(trimmedMemberId);

        // Kiểm tra quyền admin của requester trước khi thực hiện
        requesterRef.get()
            .addOnFailureListener(e -> callback.onError(e.getMessage()))
            .addOnSuccessListener(requesterSnap -> {
                if (!requesterSnap.exists()) {
                    callback.onError("Người yêu cầu không phải thành viên nhóm.");
                    return;
                }
                if (!"admin".equals(requesterSnap.getString("role"))) {
                    callback.onError("Chỉ admin mới có thể thay đổi vai trò thành viên.");
                    return;
                }

                if ("member".equals(newRole)) {
                    // Cần kiểm tra không được hạ cấp admin cuối cùng
                    convRef.get()
                        .addOnFailureListener(e -> callback.onError(e.getMessage()))
                        .addOnSuccessListener(convSnap -> {
                            @SuppressWarnings("unchecked")
                            List<String> adminIds = (List<String>) convSnap.get("adminIds");
                            if (adminIds != null && adminIds.size() == 1
                                && adminIds.contains(trimmedMemberId)) {
                                callback.onError("Không thể hạ cấp admin cuối cùng. Hãy chỉ định admin khác trước.");
                                return;
                            }
                            doSetRoleBatch(convRef, memberRef, trimmedMemberId, newRole, callback);
                        });
                } else {
                    // Thăng lên admin — không cần kiểm tra thêm
                    doSetRoleBatch(convRef, memberRef, trimmedMemberId, newRole, callback);
                }
            });
    }

    /** Thực hiện WriteBatch cập nhật role thành viên + adminIds trong conversation. */
    private void doSetRoleBatch(DocumentReference convRef, DocumentReference memberRef,
                                String memberId, String newRole, OnCompleteCallback callback) {
        Map<String, Object> memberUpdate = new HashMap<>();
        memberUpdate.put("role", newRole);

        Map<String, Object> convUpdate = new HashMap<>();
        convUpdate.put("adminIds", "admin".equals(newRole)
            ? FieldValue.arrayUnion(memberId)
            : FieldValue.arrayRemove(memberId));
        convUpdate.put("updatedAt", FieldValue.serverTimestamp());

        WriteBatch batch = db.batch();
        batch.update(memberRef, memberUpdate);
        batch.update(convRef, convUpdate);

        batch.commit()
            .addOnSuccessListener(unused -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Đặt hoặc xóa biệt danh (nickname) của một thành viên trong nhóm.
     * Nếu nickname là null hoặc chuỗi trống → xóa field nickname (dùng FieldValue.delete()).
     * Chỉ cập nhật members/{memberId}.nickname, không kiểm tra quyền admin ở đây
     * (UI layer chịu trách nhiệm kiểm tra quyền trước khi gọi).
     *
     * @param convId     ID hội thoại nhóm
     * @param memberId   UID thành viên cần đặt nickname
     * @param nickname   Biệt danh mới; null hoặc blank để xóa nickname
     * @param callback   Kết quả trả về (onSuccess hoặc onError)
     */
    public void setNickname(String convId, String memberId, String nickname,
                            OnCompleteCallback callback) {
        if (convId == null || convId.trim().isEmpty()) {
            callback.onError("ID hội thoại không hợp lệ.");
            return;
        }
        if (memberId == null || memberId.trim().isEmpty()) {
            callback.onError("ID thành viên không hợp lệ.");
            return;
        }

        DocumentReference memberRef = db.collection(COLLECTION)
            .document(convId.trim())
            .collection("members")
            .document(memberId.trim());

        Map<String, Object> update = new HashMap<>();
        // Nickname null hoặc blank → xóa field để hiển thị về displayName mặc định
        if (nickname == null || nickname.trim().isEmpty()) {
            update.put("nickname", FieldValue.delete());
        } else {
            update.put("nickname", nickname.trim());
        }
        update.put("updatedAt", FieldValue.serverTimestamp());

        memberRef.update(update)
            .addOnSuccessListener(unused -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
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
