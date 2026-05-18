package com.example.beechats.ui.chat;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beechats.R;
import com.example.beechats.data.models.Conversation;
import com.example.beechats.data.models.LastMessageInfo;
import com.example.beechats.data.models.User;
import com.example.beechats.data.repositories.UserRepository;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adapter hiển thị danh sách hội thoại trong tab "Tất cả".
 * Mỗi item hiển thị: tên người chat, tin nhắn cuối, thời gian, trạng thái đã đọc.
 * Click vào item → mở ChatActivity.
 */
public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

    private final List<Conversation> conversationList;
    private final String currentUserId;
    private final UserRepository userRepository;
    private final Map<String, User> userCache = new HashMap<>();

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
        private final ImageView imgStatusRead;
        private final ShapeableImageView imgReadAvatar;

        public ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            txtUserName = itemView.findViewById(R.id.txt_user_name);
            txtMessage = itemView.findViewById(R.id.txt_message);
            txtTime = itemView.findViewById(R.id.txt_time);
            imgStatusRead = itemView.findViewById(R.id.img_status_read);
            imgReadAvatar = itemView.findViewById(R.id.img_read_avatar);
        }

        public void bind(Conversation conversation) {
            String otherUserId = getOtherUserId(conversation);
            resetState();
            bindUserName(conversation, otherUserId);
            bindLastMessage(conversation);
            bindReadState(conversation, otherUserId);
            bindTime(conversation);
            bindClickAction(conversation, otherUserId);
        }

        private void resetState() {
            txtUserName.setText("Không rõ");
            txtMessage.setText("");
            txtMessage.setTextColor(android.graphics.Color.parseColor("#565656"));
            txtMessage.setTypeface(null, android.graphics.Typeface.NORMAL);
            txtTime.setText("");
            imgStatusRead.setVisibility(View.GONE);
            imgReadAvatar.setVisibility(View.GONE);
        }

        private String getOtherUserId(Conversation conversation) {
            if (conversation == null || conversation.getParticipants() == null) {
                return null;
            }
            for (String uid : conversation.getParticipants()) {
                if (uid != null && !uid.equals(currentUserId)) {
                    return uid;
                }
            }
            return null;
        }

        private void bindUserName(Conversation conversation, String otherUserId) {
            if (otherUserId == null) {
                txtUserName.setText(conversation != null && "group".equals(conversation.getType())
                        && conversation.getGroupName() != null
                        ? conversation.getGroupName()
                        : "Không rõ");
                return;
            }

            User cachedUser = userCache.get(otherUserId);
            if (cachedUser != null) {
                txtUserName.setText(cachedUser.getDisplayName() != null ? cachedUser.getDisplayName() : otherUserId);
                return;
            }

            final String bindingConversationId = conversation != null ? conversation.getConversationId() : null;
            final int boundPosition = getBindingAdapterPosition();
            txtUserName.setText(otherUserId);
            userRepository.getUser(otherUserId, new UserRepository.OnUserCallback() {
                @Override
                public void onSuccess(User user) {
                    if (user == null) {
                        return;
                    }
                    userCache.put(otherUserId, user);
                    if (isBindingStillValid(bindingConversationId, boundPosition)) {
                        txtUserName.setText(user.getDisplayName() != null ? user.getDisplayName() : otherUserId);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    if (isBindingStillValid(bindingConversationId, boundPosition)) {
                        txtUserName.setText(otherUserId);
                    }
                }
            });
        }

        private boolean isBindingStillValid(String conversationId, int position) {
            if (position == RecyclerView.NO_POSITION || conversationId == null) {
                return false;
            }
            int currentPosition = getBindingAdapterPosition();
            if (currentPosition != position || currentPosition < 0 || currentPosition >= conversationList.size()) {
                return false;
            }
            Conversation currentConversation = conversationList.get(currentPosition);
            return currentConversation != null && conversationId.equals(currentConversation.getConversationId());
        }

        private void bindLastMessage(Conversation conversation) {
            LastMessageInfo lastMsg = conversation != null ? conversation.getLastMessage() : null;
            if (lastMsg == null) {
                return;
            }

            String previewText = "Đã gửi 1 tin nhắn";
            txtMessage.setText(previewText);

            if (lastMsg.getSenderId() != null && lastMsg.getSenderId().equals(currentUserId)) {
                txtMessage.setTextColor(android.graphics.Color.parseColor("#565656"));
                txtMessage.setTypeface(null, android.graphics.Typeface.NORMAL);
            } else {
                txtMessage.setTextColor(android.graphics.Color.parseColor("#000000"));
                txtMessage.setTypeface(null, android.graphics.Typeface.BOLD);
            }
        }

        private void bindReadState(Conversation conversation, String otherUserId) {
            LastMessageInfo lastMsg = conversation != null ? conversation.getLastMessage() : null;
            if (lastMsg == null || lastMsg.getSenderId() == null || !lastMsg.getSenderId().equals(currentUserId)) {
                return;
            }

            // Mặc định hiển thị chấm trạng thái; sẽ đổi sang avatar nếu có thể xác nhận đã đọc.
            imgStatusRead.setVisibility(View.VISIBLE);
            imgReadAvatar.setVisibility(View.GONE);

            if (conversation == null || conversation.getConversationId() == null || otherUserId == null) {
                return;
            }

            // Không query Firestore trong onBind nếu dữ liệu đã có thể cache được.
            // Read state thật sự nên đến từ conversation/messages listener; ở list chỉ phản ánh nhanh.
            if (lastMsg.getTimestamp() != null) {
                imgStatusRead.setVisibility(View.VISIBLE);
            }
        }

        private void bindTime(Conversation conversation) {
            LastMessageInfo lastMsg = conversation != null ? conversation.getLastMessage() : null;
            if (lastMsg != null && lastMsg.getTimestamp() != null) {
                txtTime.setText(formatTime(lastMsg.getTimestamp()));
            } else if (conversation != null && conversation.getUpdatedAt() != null) {
                txtTime.setText(formatTime(conversation.getUpdatedAt()));
            }
        }

        private void bindClickAction(Conversation conversation, String otherUserId) {
            itemView.setOnClickListener(v -> {
                Context context = v.getContext();
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID,
                        conversation != null ? conversation.getConversationId() : null);
                intent.putExtra(ChatActivity.EXTRA_RECEIVER_ID, otherUserId);
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
                return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
            } else {
                return new SimpleDateFormat("dd/MM", Locale.getDefault()).format(date);
            }
        }
    }
}
