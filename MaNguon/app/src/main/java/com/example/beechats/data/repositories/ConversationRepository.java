package com.example.beechats.data.repositories;

import com.example.beechats.data.models.Conversation;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

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
