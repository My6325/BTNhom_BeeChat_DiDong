package com.example.beechats.ui.chat;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.beechats.R;

/**
 * Fragment hiển thị tin nhắn NHÓM (tab "Nhóm")
 */
public class GroupChatFragment extends Fragment {

    private RecyclerView rvChat;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_group_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvChat = view.findViewById(R.id.rvChat);
        rvChat.setLayoutManager(new LinearLayoutManager(getContext()));

        // TODO: Thiết lập Adapter và load dữ liệu tin nhắn nhóm ở đây
        // Ví dụ: rvChat.setAdapter(new ChatAdapter(groupChatList));
    }
}
