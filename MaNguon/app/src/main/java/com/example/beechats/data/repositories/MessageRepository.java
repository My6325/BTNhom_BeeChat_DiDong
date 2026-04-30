package com.example.beechats.data.repositories;

import com.example.beechats.data.models.Message;
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
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(30)
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
