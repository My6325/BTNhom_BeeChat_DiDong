package com.example.beechats.ui.chat;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beechats.R;
import com.example.beechats.data.models.Conversation;
import com.example.beechats.data.models.LastMessageInfo;
import com.example.beechats.data.models.User;
import com.example.beechats.data.repositories.UserRepository;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter hiển thị danh sách hội thoại trong tab "Tất cả".
 * Mỗi item hiển thị: tên người chat, tin nhắn cuối, thời gian.
 * Click vào item → mở ChatActivity.
 */
public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

    private final List<Conversation> conversationList;
    private final String currentUserId;
    private final UserRepository userRepository;

    public ConversationAdapter(List<Conversation> conversationList, String currentUserId) {
        this.conversationList = conversationList;
        this.currentUserId = currentUserId;
        this.userRepository = new UserRepository();
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        Conversation conversation = conversationList.get(position);
        holder.bind(conversation);
    }

    @Override
    public int getItemCount() {
        return conversationList != null ? conversationList.size() : 0;
    }

    class ConversationViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtUserName;
        private final TextView txtMessage;
        private final TextView txtTime;

        public ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            txtUserName = itemView.findViewById(R.id.txt_user_name);
            txtMessage = itemView.findViewById(R.id.txt_message);
            txtTime = itemView.findViewById(R.id.txt_time);
        }

        public void bind(Conversation conversation) {
            // --- Hiển thị tên người đang chat ---
            // Tìm UID của đối phương trong danh sách participants
            String otherUserId = null;
            if (conversation.getParticipants() != null) {
                for (String uid : conversation.getParticipants()) {
                    if (!uid.equals(currentUserId)) {
                        otherUserId = uid;
                        break;
                    }
                }
            }

            // Load tên đối phương từ Firestore collection "users"
            if (otherUserId != null) {
                final String otherUid = otherUserId;
                userRepository.getUser(otherUserId, new UserRepository.OnUserCallback() {
                    @Override
                    public void onSuccess(User user) {
                        if (user != null && user.getDisplayName() != null) {
                            txtUserName.setText(user.getDisplayName());
                        } else {
                            txtUserName.setText(otherUid);
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        txtUserName.setText(otherUid);
                    }
                });
            } else {
                txtUserName.setText("Không rõ");
            }

            // --- Hiển thị tin nhắn cuối ---
            LastMessageInfo lastMsg = conversation.getLastMessage();
            if (lastMsg != null && lastMsg.getText() != null) {
                txtMessage.setText(lastMsg.getText());
            } else {
                txtMessage.setText("");
            }

            // --- Hiển thị thời gian ---
            if (lastMsg != null && lastMsg.getTimestamp() != null) {
                txtTime.setText(formatTime(lastMsg.getTimestamp()));
            } else if (conversation.getUpdatedAt() != null) {
                txtTime.setText(formatTime(conversation.getUpdatedAt()));
            } else {
                txtTime.setText("");
            }

            // --- Click item → mở ChatActivity ---
            final String receiverName = txtUserName.getText().toString();
            itemView.setOnClickListener(v -> {
                Context context = v.getContext();
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversation.getConversationId());
                intent.putExtra(ChatActivity.EXTRA_RECEIVER_NAME, txtUserName.getText().toString());
                context.startActivity(intent);
            });
        }

        /**
         * Format Timestamp thành dạng "HH:mm" nếu cùng ngày, hoặc "dd/MM" nếu khác ngày.
         */
        private String formatTime(Timestamp timestamp) {
            Date date = timestamp.toDate();
            Date now = new Date();

            SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            if (sdfDate.format(date).equals(sdfDate.format(now))) {
                // Cùng ngày → hiển thị giờ:phút
                return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
            } else {
                // Khác ngày → hiển thị ngày/tháng
                return new SimpleDateFormat("dd/MM", Locale.getDefault()).format(date);
            }
        }
    }
}
