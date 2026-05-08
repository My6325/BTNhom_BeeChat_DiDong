package com.example.beechats.data.repositories;

import com.example.beechats.data.models.Message;
import com.example.beechats.data.models.ReplyInfo;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageRepository {

    public interface OnSendMessageCallback {
        void onSuccess(String messageId);
        void onError(String errorMessage);
    }

    public interface OnMessagesCallback {
        void onSuccess(List<Message> messages);
        void onError(String errorMessage);
    }

    public interface OnMessageStatusCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    private final FirebaseFirestore db;
    private static final String CONV_COLLECTION = "conversations";
    private static final String MSG_SUBCOLLECTION = "messages";

    public MessageRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /** Constructor cho phép inject dependency (dùng trong unit test). */
    public MessageRepository(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Gửi tin nhắn text vào hội thoại và cập nhật cache lastMessage trong conversation document.
     * Dùng WriteBatch để đảm bảo atomic: cả hai thao tác thành công hoặc cùng thất bại.
     *
     * @param conversationId ID hội thoại (không được rỗng)
     * @param senderId       UID người gửi (không được rỗng)
     * @param senderName     Tên người gửi
     * @param text           Nội dung tin nhắn (không được rỗng)
     * @param callback       Kết quả trả về (onSuccess với messageId, hoặc onError)
     */
    public void sendMessage(String conversationId, String senderId, String senderName,
                            String text, OnSendMessageCallback callback) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            callback.onError("ID hội thoại không hợp lệ.");
            return;
        }
        if (senderId == null || senderId.trim().isEmpty()) {
            callback.onError("ID người gửi không hợp lệ.");
            return;
        }
        if (text == null || text.trim().isEmpty()) {
            callback.onError("Nội dung tin nhắn không được để trống.");
            return;
        }

        // Tạo auto-ID cho message document mới
        DocumentReference msgRef = db.collection(CONV_COLLECTION)
                .document(conversationId)
                .collection(MSG_SUBCOLLECTION)
                .document();

        DocumentReference convRef = db.collection(CONV_COLLECTION)
                .document(conversationId);

        // Dữ liệu message document
        Map<String, Object> msgData = new HashMap<>();
        msgData.put("senderId", senderId.trim());
        msgData.put("senderName", senderName != null ? senderName : "");
        msgData.put("text", text.trim());
        msgData.put("type", "text");
        msgData.put("status", "sent");
        msgData.put("createdAt", FieldValue.serverTimestamp());
        msgData.put("updatedAt", FieldValue.serverTimestamp());

        // Cache lastMessage trong conversation document
        Map<String, Object> lastMessage = new HashMap<>();
        lastMessage.put("text", text.trim());
        lastMessage.put("senderId", senderId.trim());
        lastMessage.put("senderName", senderName != null ? senderName : "");
        lastMessage.put("type", "text");
        lastMessage.put("timestamp", FieldValue.serverTimestamp());

        Map<String, Object> convUpdate = new HashMap<>();
        convUpdate.put("lastMessage", lastMessage);
        convUpdate.put("updatedAt", FieldValue.serverTimestamp());

        // Atomic WriteBatch: tạo message + cập nhật conversation
        WriteBatch batch = db.batch();
        batch.set(msgRef, msgData);
        batch.update(convRef, convUpdate);

        String messageId = msgRef.getId();
        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(messageId))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Gửi tin nhắn reply/quote kèm thông tin tin nhắn gốc.
     * Nếu tin gốc đã bị thu hồi (text == null), preview tự động là "[Tin nhắn đã thu hồi]".
     * Dùng WriteBatch để đảm bảo atomic: tạo message + cập nhật lastMessage trong conversation.
     *
     * @param conversationId ID hội thoại (không được rỗng)
     * @param senderId       UID người gửi (không được rỗng)
     * @param senderName     Tên người gửi
     * @param text           Nội dung tin nhắn reply (không được rỗng)
     * @param replyTo        Thông tin tin nhắn gốc (không được null, messageId không được rỗng)
     * @param callback       Kết quả trả về (onSuccess với messageId, hoặc onError)
     */
    public void sendReplyMessage(String conversationId, String senderId, String senderName,
                                 String text, ReplyInfo replyTo, OnSendMessageCallback callback) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            callback.onError("ID hội thoại không hợp lệ.");
            return;
        }
        if (senderId == null || senderId.trim().isEmpty()) {
            callback.onError("ID người gửi không hợp lệ.");
            return;
        }
        if (text == null || text.trim().isEmpty()) {
            callback.onError("Nội dung tin nhắn không được để trống.");
            return;
        }
        if (replyTo == null) {
            callback.onError("Thông tin tin nhắn gốc không hợp lệ.");
            return;
        }
        if (replyTo.getMessageId() == null || replyTo.getMessageId().trim().isEmpty()) {
            callback.onError("ID tin nhắn gốc không hợp lệ.");
            return;
        }

        // Nếu tin gốc đã bị thu hồi (text bị xóa), hiển thị placeholder
        String replyPreview = replyTo.getText() != null ? replyTo.getText() : "[Tin nhắn đã thu hồi]";

        DocumentReference msgRef = db.collection(CONV_COLLECTION)
                .document(conversationId)
                .collection(MSG_SUBCOLLECTION)
                .document();

        DocumentReference convRef = db.collection(CONV_COLLECTION)
                .document(conversationId);

        // replyTo map lưu dạng Map để Firestore serialize đúng cấu trúc
        Map<String, Object> replyToMap = new HashMap<>();
        replyToMap.put("messageId", replyTo.getMessageId().trim());
        replyToMap.put("text", replyPreview);
        replyToMap.put("senderId", replyTo.getSenderId() != null ? replyTo.getSenderId() : "");
        replyToMap.put("senderName", replyTo.getSenderName() != null ? replyTo.getSenderName() : "");

        Map<String, Object> msgData = new HashMap<>();
        msgData.put("senderId", senderId.trim());
        msgData.put("senderName", senderName != null ? senderName : "");
        msgData.put("text", text.trim());
        msgData.put("type", "text");
        msgData.put("status", "sent");
        msgData.put("replyTo", replyToMap);
        msgData.put("createdAt", FieldValue.serverTimestamp());
        msgData.put("updatedAt", FieldValue.serverTimestamp());

        Map<String, Object> lastMessage = new HashMap<>();
        lastMessage.put("text", text.trim());
        lastMessage.put("senderId", senderId.trim());
        lastMessage.put("senderName", senderName != null ? senderName : "");
        lastMessage.put("type", "text");
        lastMessage.put("timestamp", FieldValue.serverTimestamp());

        Map<String, Object> convUpdate = new HashMap<>();
        convUpdate.put("lastMessage", lastMessage);
        convUpdate.put("updatedAt", FieldValue.serverTimestamp());

        String messageId = msgRef.getId();
        WriteBatch batch = db.batch();
        batch.set(msgRef, msgData);
        batch.update(convRef, convUpdate);
        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(messageId))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Lắng nghe tin nhắn real-time trong hội thoại.
     * Trả về ListenerRegistration — caller phải gọi .remove() trong onStop() để tránh memory leak.
     *
     * <p>Cách sử dụng:
     * <pre>
     *   // Trong onStart()/onResume():
     *   listenerReg = msgRepo.listenToMessages(convId, callback);
     *   // Trong onStop():
     *   if (listenerReg != null) listenerReg.remove();
     * </pre>
     *
     * @param conversationId ID hội thoại (không được rỗng)
     * @param callback       Callback nhận danh sách tin nhắn mới nhất (giảm dần theo createdAt)
     * @return ListenerRegistration để detach listener, hoặc null nếu conversationId không hợp lệ
     */
    public ListenerRegistration listenToMessages(String conversationId, OnMessagesCallback callback) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            callback.onError("ID hội thoại không hợp lệ.");
            return null;
        }

        return db.collection(CONV_COLLECTION)
                .document(conversationId)
                .collection(MSG_SUBCOLLECTION)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }
                    List<Message> messages = new ArrayList<>();
                    if (snapshots != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                            Message msg = doc.toObject(Message.class);
                            if (msg != null) {
                                msg.setMessageId(doc.getId());
                                messages.add(msg);
                            }
                        }
                    }
                    callback.onSuccess(messages);
                });
    }

    /**
     * Cập nhật trạng thái tin nhắn cụ thể sang "delivered".
     * Gọi khi đối phương online và nhận được tin nhắn.
     *
     * @param conversationId ID hội thoại (không được rỗng)
     * @param messageId      ID tin nhắn cần cập nhật (không được rỗng)
     * @param callback       Kết quả trả về (onSuccess hoặc onError)
     */
    public void markDelivered(String conversationId, String messageId,
                              OnMessageStatusCallback callback) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            callback.onError("ID hội thoại không hợp lệ.");
            return;
        }
        if (messageId == null || messageId.trim().isEmpty()) {
            callback.onError("ID tin nhắn không hợp lệ.");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "delivered");
        updates.put("updatedAt", FieldValue.serverTimestamp());

        db.collection(CONV_COLLECTION)
                .document(conversationId)
                .collection(MSG_SUBCOLLECTION)
                .document(messageId)
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Thu hồi tin nhắn trong vòng 5 phút sau khi gửi.
     * Chỉ người gửi (senderId) mới được phép thu hồi.
     * Sau khi thu hồi: isRecalled=true, text và mediaUrl bị xóa khỏi document.
     *
     * @param conversationId ID hội thoại (không được rỗng)
     * @param messageId      ID tin nhắn cần thu hồi (không được rỗng)
     * @param callerUid      UID người thực hiện thu hồi (phải là senderId)
     * @param callback       Kết quả trả về (onSuccess hoặc onError)
     */
    public void recallMessage(String conversationId, String messageId,
                              String callerUid, OnMessageStatusCallback callback) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            callback.onError("ID hội thoại không hợp lệ.");
            return;
        }
        if (messageId == null || messageId.trim().isEmpty()) {
            callback.onError("ID tin nhắn không hợp lệ.");
            return;
        }
        if (callerUid == null || callerUid.trim().isEmpty()) {
            callback.onError("ID người dùng không hợp lệ.");
            return;
        }

        db.collection(CONV_COLLECTION)
                .document(conversationId)
                .collection(MSG_SUBCOLLECTION)
                .document(messageId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        callback.onError("Tin nhắn không tồn tại.");
                        return;
                    }

                    String senderId = snapshot.getString("senderId");
                    if (!callerUid.trim().equals(senderId)) {
                        callback.onError("Chỉ người gửi mới được thu hồi tin nhắn.");
                        return;
                    }

                    com.google.firebase.Timestamp createdAt = snapshot.getTimestamp("createdAt");
                    if (createdAt == null) {
                        callback.onError("Không thể xác định thời gian gửi.");
                        return;
                    }

                    long ageMs = System.currentTimeMillis() - createdAt.toDate().getTime();
                    if (ageMs > 5 * 60 * 1000L) {
                        callback.onError("Chỉ có thể thu hồi trong 5 phút đầu.");
                        return;
                    }

                    // Xóa nội dung và đánh dấu thu hồi
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("isRecalled", true);
                    updates.put("text", FieldValue.delete());
                    updates.put("mediaUrl", FieldValue.delete());
                    updates.put("recalledAt", FieldValue.serverTimestamp());
                    updates.put("updatedAt", FieldValue.serverTimestamp());

                    snapshot.getReference()
                            .update(updates)
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Thêm hoặc ghi đè reaction (emoji) của người dùng vào tin nhắn.
     * Dùng dot-notation để update nested map field mà không ghi đè toàn bộ map reactions.
     *
     * @param conversationId ID hội thoại (không được rỗng)
     * @param messageId      ID tin nhắn (không được rỗng)
     * @param userId         UID người react (không được rỗng)
     * @param emoji          Emoji string (không được rỗng)
     * @param callback       Kết quả trả về (onSuccess hoặc onError)
     */
    public void addReaction(String conversationId, String messageId, String userId,
                            String emoji, OnMessageStatusCallback callback) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            callback.onError("ID hội thoại không hợp lệ.");
            return;
        }
        if (messageId == null || messageId.trim().isEmpty()) {
            callback.onError("ID tin nhắn không hợp lệ.");
            return;
        }
        if (userId == null || userId.trim().isEmpty()) {
            callback.onError("ID người dùng không hợp lệ.");
            return;
        }
        if (emoji == null || emoji.trim().isEmpty()) {
            callback.onError("Emoji không được để trống.");
            return;
        }

        db.collection(CONV_COLLECTION)
                .document(conversationId)
                .collection(MSG_SUBCOLLECTION)
                .document(messageId)
                .update("reactions." + userId.trim(), emoji.trim())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Xóa reaction của người dùng khỏi tin nhắn.
     * Dùng FieldValue.delete() để xóa key userId khỏi map reactions.
     *
     * @param conversationId ID hội thoại (không được rỗng)
     * @param messageId      ID tin nhắn (không được rỗng)
     * @param userId         UID người muốn xóa reaction (không được rỗng)
     * @param callback       Kết quả trả về (onSuccess hoặc onError)
     */
    public void removeReaction(String conversationId, String messageId, String userId,
                               OnMessageStatusCallback callback) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            callback.onError("ID hội thoại không hợp lệ.");
            return;
        }
        if (messageId == null || messageId.trim().isEmpty()) {
            callback.onError("ID tin nhắn không hợp lệ.");
            return;
        }
        if (userId == null || userId.trim().isEmpty()) {
            callback.onError("ID người dùng không hợp lệ.");
            return;
        }

        db.collection(CONV_COLLECTION)
                .document(conversationId)
                .collection(MSG_SUBCOLLECTION)
                .document(messageId)
                .update("reactions." + userId.trim(), FieldValue.delete())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Đánh dấu toàn bộ tin nhắn chưa đọc trong hội thoại là "read".
     * Dùng WriteBatch để cập nhật atomic. Ghi thêm readBy.{userId}=serverTimestamp() cho group chat.
     * Nếu không có tin nhắn nào cần đánh dấu, gọi onSuccess() ngay mà không tạo batch.
     *
     * @param conversationId ID hội thoại (không được rỗng)
     * @param userId         UID người đọc (không được rỗng)
     * @param callback       Kết quả trả về (onSuccess hoặc onError)
     */
    public void markAsRead(String conversationId, String userId,
                           OnMessageStatusCallback callback) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            callback.onError("ID hội thoại không hợp lệ.");
            return;
        }
        if (userId == null || userId.trim().isEmpty()) {
            callback.onError("ID người dùng không hợp lệ.");
            return;
        }

        // Query tất cả tin nhắn chưa được đọc (sent hoặc delivered)
        db.collection(CONV_COLLECTION)
                .document(conversationId)
                .collection(MSG_SUBCOLLECTION)
                .whereIn("status", Arrays.asList("sent", "delivered"))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        // Không có tin nhắn nào cần cập nhật
                        callback.onSuccess();
                        return;
                    }

                    // Batch update tất cả: status=read + readBy.userId=serverTimestamp()
                    WriteBatch batch = db.batch();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("status", "read");
                        updates.put("readBy." + userId.trim(), FieldValue.serverTimestamp());
                        updates.put("updatedAt", FieldValue.serverTimestamp());
                        batch.update(doc.getReference(), updates);
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}
