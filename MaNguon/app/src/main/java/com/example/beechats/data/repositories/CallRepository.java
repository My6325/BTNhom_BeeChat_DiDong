package com.example.beechats.data.repositories;

import androidx.annotation.NonNull;

import com.example.beechats.data.models.CallSession;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CallRepository {
    private static final String COLLECTION_CALLS = "call_sessions";

    public interface OnCallSessionCallback {
        void onSuccess(CallSession callSession);
        void onError(String message);
    }

    public interface OnCallStateCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface OnCallSessionListener {
        void onChanged(CallSession callSession);
        void onError(String message);
    }

    private final FirebaseFirestore db;

    public CallRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public void createOutgoingCall(@NonNull String callerId,
                                   String callerName,
                                   @NonNull String calleeId,
                                   String calleeName,
                                   @NonNull String type,
                                   @NonNull OnCallSessionCallback callback) {
        String callId = UUID.randomUUID().toString();
        String roomId = callId;

        Map<String, Object> data = new HashMap<>();
        data.put("callId", callId);
        data.put("roomId", roomId);
        data.put("callerId", callerId);
        data.put("callerName", callerName != null ? callerName : "");
        data.put("calleeId", calleeId);
        data.put("calleeName", calleeName != null ? calleeName : "");
        data.put("type", type);
        data.put("status", "calling");
        data.put("createdAt", Timestamp.now());
        data.put("updatedAt", Timestamp.now());

        db.collection(COLLECTION_CALLS)
                .document(callId)
                .set(data)
                .addOnSuccessListener(unused -> {
                    CallSession session = new CallSession();
                    session.setCallId(callId);
                    session.setRoomId(roomId);
                    session.setCallerId(callerId);
                    session.setCallerName(callerName);
                    session.setCalleeId(calleeId);
                    session.setCalleeName(calleeName);
                    session.setType(type);
                    session.setStatus("calling");
                    callback.onSuccess(session);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public ListenerRegistration listenCallSession(String callId, @NonNull OnCallSessionListener listener) {
        return db.collection(COLLECTION_CALLS)
                .document(callId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error.getMessage());
                        return;
                    }
                    if (snapshot == null || !snapshot.exists()) {
                        listener.onError("Call session not found");
                        return;
                    }
                    CallSession callSession = snapshot.toObject(CallSession.class);
                    listener.onChanged(callSession);
                });
    }

    public void updateCallStatus(@NonNull String callId,
                                 @NonNull String status,
                                 @NonNull OnCallStateCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("updatedAt", Timestamp.now());

        db.collection(COLLECTION_CALLS)
                .document(callId)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void endCall(@NonNull String callId, @NonNull OnCallStateCallback callback) {
        updateCallStatus(callId, "ended", callback);
    }

    public String getCurrentUid() {
        return FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
    }
}
