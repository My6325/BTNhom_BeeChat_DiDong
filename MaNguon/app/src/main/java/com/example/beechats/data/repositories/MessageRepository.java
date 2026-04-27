package com.example.beechats.data.repositories;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class MessageRepository {

    public interface OnSendMessageCallback {
        void onSuccess(String messageId);
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
}
