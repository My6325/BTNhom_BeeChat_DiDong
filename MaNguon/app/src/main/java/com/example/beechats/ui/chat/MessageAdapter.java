package com.example.beechats.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beechats.R;
import com.example.beechats.data.models.Message;

import java.util.List;

/**
 * Adapter hiển thị danh sách tin nhắn trong RecyclerView.
 * Phân biệt tin nhắn gửi (item_chat_user) và tin nhắn nhận (item_chat_friend)
 * dựa trên senderId so với currentUserId.
 */
public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final List<Message> messageList;
    private final String currentUserId;

    public MessageAdapter(List<Message> messageList, String currentUserId) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        if (message.getSenderId() != null && message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            // Layout tin nhắn gửi đi — nằm bên phải, background vàng
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_user, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            // Layout tin nhắn nhận — nằm bên trái, có avatar
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_friend, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);

        if (holder.getItemViewType() == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).bind(message);
        } else {
            ((ReceivedMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messageList != null ? messageList.size() : 0;
    }

    // ─── ViewHolder cho tin nhắn GỬI ĐI (item_chat_user.xml) ───

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMessage;

        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }

        public void bind(Message message) {
            if (message.isRecalled()) {
                tvMessage.setText("Tin nhắn đã thu hồi");
                tvMessage.setAlpha(0.5f);
            } else {
                tvMessage.setText(message.getText());
                tvMessage.setAlpha(1.0f);
            }
        }
    }

    // ─── ViewHolder cho tin nhắn NHẬN (item_chat_friend.xml) ───

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtMessage;

        public ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            txtMessage = itemView.findViewById(R.id.txtMessage);
        }

        public void bind(Message message) {
            if (message.isRecalled()) {
                txtMessage.setText("Tin nhắn đã thu hồi");
                txtMessage.setAlpha(0.5f);
            } else {
                txtMessage.setText(message.getText());
                txtMessage.setAlpha(1.0f);
            }
        }
    }
}
