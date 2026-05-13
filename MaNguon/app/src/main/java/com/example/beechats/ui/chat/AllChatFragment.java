package com.example.beechats.ui.chat;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

/**
 * Fragment hiển thị TẤT CẢ hội thoại (tab "Tất cả").
 * Lắng nghe real-time danh sách conversations mà user hiện tại tham gia.
 */
public class AllChatFragment extends Fragment {

    private static final String TAG = "AllChatFragment";

    private RecyclerView rvChat;
    private ConversationAdapter conversationAdapter;
    private List<Conversation> conversationList;
    private ListenerRegistration conversationListener;

    private String currentUserId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_all_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Lấy current user
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            return;
        }
        currentUserId = firebaseUser.getUid();
        Log.d(TAG, "Current user ID: " + currentUserId);

        // Khởi tạo RecyclerView + Adapter
        rvChat = view.findViewById(R.id.rvChat);
        conversationList = new ArrayList<>();
        conversationAdapter = new ConversationAdapter(conversationList, currentUserId);
        rvChat.setLayoutManager(new LinearLayoutManager(getContext()));
        rvChat.setAdapter(conversationAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        startListeningConversations();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (conversationListener != null) {
            conversationListener.remove();
            conversationListener = null;
        }
    }

    /**
     * Lắng nghe real-time danh sách conversations mà currentUserId nằm trong participants.
     * Không dùng orderBy trên Firestore (tránh yêu cầu composite index),
     * mà sắp xếp theo updatedAt trong bộ nhớ sau khi nhận dữ liệu.
     */
    private void startListeningConversations() {
        if (currentUserId == null) return;

        conversationListener = FirebaseFirestore.getInstance()
                .collection("conversations")
                .whereArrayContains("participants", currentUserId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Lỗi lắng nghe conversations: " + error.getMessage(), error);
                        if (getContext() != null) {
                            Toast.makeText(getContext(),
                                    "Lỗi: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        }
                        return;
                    }

                    conversationList.clear();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Conversation conv = doc.toObject(Conversation.class);
                            if (conv != null) {
                                conv.setConversationId(doc.getId());
                                conversationList.add(conv);
                            }
                        }
                    }

                    // Sắp xếp trong bộ nhớ: hội thoại mới nhất lên đầu
                    Collections.sort(conversationList, (a, b) -> {
                        if (a.getUpdatedAt() == null && b.getUpdatedAt() == null) return 0;
                        if (a.getUpdatedAt() == null) return 1;
                        if (b.getUpdatedAt() == null) return -1;
                        return b.getUpdatedAt().compareTo(a.getUpdatedAt());
                    });

                    Log.d(TAG, "Đã tải " + conversationList.size() + " hội thoại");
                    conversationAdapter.notifyDataSetChanged();
                });
    }
}

