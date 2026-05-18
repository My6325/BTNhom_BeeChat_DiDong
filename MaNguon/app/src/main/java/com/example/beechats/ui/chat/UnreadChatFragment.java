package com.example.beechats.ui.chat;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beechats.R;
import com.example.beechats.data.models.Conversation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Fragment hiển thị các hội thoại CHƯA ĐỌC (tab "Chưa đọc")
 */
public class UnreadChatFragment extends Fragment {

    private static final String TAG = "UnreadChatFragment";

    private RecyclerView rvChat;
    private ConversationAdapter conversationAdapter;
    private List<Conversation> conversationList;
    private ListenerRegistration conversationListener;
    private String currentUserId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_unread_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            return;
        }
        currentUserId = firebaseUser.getUid();

        rvChat = view.findViewById(R.id.rvChat);
        conversationList = new ArrayList<>();
        conversationAdapter = new ConversationAdapter(conversationList, currentUserId);
        rvChat.setLayoutManager(new LinearLayoutManager(getContext()));
        rvChat.setAdapter(conversationAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        startListeningUnreadConversations();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (conversationListener != null) {
            conversationListener.remove();
            conversationListener = null;
        }
    }

    private void startListeningUnreadConversations() {
        if (currentUserId == null) return;

        conversationListener = FirebaseFirestore.getInstance()
                .collection("conversations")
                .whereArrayContains("participants", currentUserId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Lỗi lắng nghe conversations chưa đọc: " + error.getMessage(), error);
                        return;
                    }

                    conversationList.clear();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Conversation conversation = doc.toObject(Conversation.class);
                            if (conversation == null) continue;
                            conversation.setConversationId(doc.getId());
                            if (isUnreadConversation(doc)) {
                                conversationList.add(conversation);
                            }
                        }
                    }

                    sortByUpdatedAt(conversationList);
                    conversationAdapter.notifyDataSetChanged();
                });
    }

    private boolean isUnreadConversation(DocumentSnapshot doc) {
        String type = doc.getString("type");
        Object lastMessageObj = doc.get("lastMessage");
        if (!(lastMessageObj instanceof Map)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> lastMessage = (Map<String, Object>) lastMessageObj;
        String senderId = stringValue(lastMessage.get("senderId"));
        if (currentUserId.equals(senderId)) {
            return false;
        }

        Long lastMessageReadAt = lastReadAtFor(doc, currentUserId);
        Long lastMessageCreatedAt = timestampMillis(lastMessage.get("timestamp"));
        if (lastMessageCreatedAt == null) {
            return false;
        }

        if (lastMessageReadAt != null && lastMessageReadAt >= lastMessageCreatedAt) {
            return false;
        }

        return "private".equals(type) || "group".equals(type) || type == null;
    }

    private void sortByUpdatedAt(List<Conversation> list) {
        Collections.sort(list, (a, b) -> {
            if (a.getUpdatedAt() == null && b.getUpdatedAt() == null) return 0;
            if (a.getUpdatedAt() == null) return 1;
            if (b.getUpdatedAt() == null) return -1;
            return b.getUpdatedAt().compareTo(a.getUpdatedAt());
        });
    }

    private Long lastReadAtFor(DocumentSnapshot doc, String userId) {
        Object lastReadAtObj = doc.get("lastReadAt");
        if (!(lastReadAtObj instanceof Map)) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> lastReadAt = (Map<String, Object>) lastReadAtObj;
        Object value = lastReadAt.get(userId);
        return timestampMillis(value);
    }

    private Long timestampMillis(Object value) {
        if (value instanceof com.google.firebase.Timestamp) {
            return ((com.google.firebase.Timestamp) value).toDate().getTime();
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
