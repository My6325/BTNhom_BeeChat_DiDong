package com.example.beechats.data.repositories;

import com.example.beechats.data.models.User;
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

    private final FirebaseFirestore db;
    private static final String COLLECTION = "users";

    public UserRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void createUser(User user, OnCompleteCallback callback) {
        db.collection(COLLECTION)
                .document(user.getUserId())
                .set(user)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

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

    public void updateProfile(String userId, String displayName, String bio, OnCompleteCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("displayName", displayName);
        updates.put("bio", bio);
        updates.put("searchKeywords", generateSearchKeywords(displayName));
        updates.put("updatedAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION)
                .document(userId)
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

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

    public void deleteUser(String userId, OnCompleteCallback callback) {
        db.collection(COLLECTION)
                .document(userId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public static List<String> generateSearchKeywords(String displayName) {
        List<String> keywords = new ArrayList<>();
        if (displayName == null || displayName.isEmpty()) return keywords;

        // Bỏ dấu tiếng Việt, lowercase
        String normalized = Normalizer.normalize(displayName, Normalizer.Form.NFD);
        normalized = Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(normalized).replaceAll("");
        normalized = normalized.toLowerCase().trim();

        // Sinh prefix cho từng từ
        String[] words = normalized.split("\\s+");
        for (String word : words) {
            if (word.isEmpty()) continue;
            for (int i = 1; i <= word.length(); i++) {
                keywords.add(word.substring(0, i));
            }
        }
        return keywords;
    }
}
