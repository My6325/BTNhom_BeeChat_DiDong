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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter hiển thị danh sách hội thoại trong tab "Tất cả".
 * Mỗi item hiển thị: tên người chat, tin nhắn cuối, thời gian, trạng thái đã đọc.
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
            // --- Hiển thị tên người đang chat ---
            String otherUserId = getOtherUserId(conversation);

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
                
                if (lastMsg.getSenderId() != null && lastMsg.getSenderId().equals(currentUserId)) {
                    // Mình gửi tin nhắn cuối -> Chắc chắn mình đã đọc
                    txtMessage.setTextColor(android.graphics.Color.parseColor("#565656"));
                    txtMessage.setTypeface(null, android.graphics.Typeface.NORMAL);
                    checkLastMessageReadStatus(conversation, otherUserId, true);
                } else {
                    // Người khác gửi -> Cần check xem mình đã đọc chưa (tạm để đen in đậm)
                    txtMessage.setTextColor(android.graphics.Color.parseColor("#000000"));
                    txtMessage.setTypeface(null, android.graphics.Typeface.BOLD);
                    checkLastMessageReadStatus(conversation, otherUserId, false);
                }
            } else {
                txtMessage.setText("");
                txtMessage.setTextColor(android.graphics.Color.parseColor("#565656"));
                txtMessage.setTypeface(null, android.graphics.Typeface.NORMAL);
                imgStatusRead.setVisibility(View.GONE);
                imgReadAvatar.setVisibility(View.GONE);
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
            itemView.setOnClickListener(v -> {
                Context context = v.getContext();
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversation.getConversationId());
                intent.putExtra(ChatActivity.EXTRA_RECEIVER_ID, otherUserId);
                intent.putExtra(ChatActivity.EXTRA_RECEIVER_NAME, txtUserName.getText().toString());
                context.startActivity(intent);
            });
        }

        private String getOtherUserId(Conversation conversation) {
            String otherUserId = null;
            if (conversation.getParticipants() != null) {
                for (String uid : conversation.getParticipants()) {
                    if (!uid.equals(currentUserId)) {
                        otherUserId = uid;
                        break;
                    }
                }
            }
            return otherUserId;
        }

        /**
         * Kiểm tra tin nhắn mới nhất đã được đọc chưa.
         * Đổi màu text tin nhắn và cập nhật avatar nhỏ/chấm tròn (nếu mình gửi).
         */
        private void checkLastMessageReadStatus(Conversation conversation, String otherUserId, boolean isSentByMe) {
            if (conversation.getConversationId() == null) return;

            // Mặc định ẩn trạng thái
            imgStatusRead.setVisibility(View.GONE);
            imgReadAvatar.setVisibility(View.GONE);

            // Query tin nhắn mới nhất trong conversation
            FirebaseFirestore.getInstance()
                    .collection("conversations")
                    .document(conversation.getConversationId())
                    .collection("messages")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (querySnapshot.isEmpty()) return;

                        DocumentSnapshot lastMsgDoc = querySnapshot.getDocuments().get(0);
                        String status = lastMsgDoc.getString("status");

                        // Kiểm tra xem MÌNH đã đọc chưa (để đổi màu text)
                        boolean isReadByMe = false;
                        if (isSentByMe) {
                            isReadByMe = true;
                        } else {
                            if ("read".equals(status)) {
                                isReadByMe = true;
                            } else {
                                Object readByObj = lastMsgDoc.get("readBy");
                                if (readByObj instanceof java.util.Map) {
                                    java.util.Map<String, Object> readBy = (java.util.Map<String, Object>) readByObj;
                                    if (readBy.containsKey(currentUserId)) {
                                        isReadByMe = true;
                                    }
                                }
                            }
                        }

                        if (isReadByMe) {
                            txtMessage.setTextColor(android.graphics.Color.parseColor("#565656")); // Xám
                            txtMessage.setTypeface(null, android.graphics.Typeface.NORMAL);
                        } else {
                            txtMessage.setTextColor(android.graphics.Color.parseColor("#000000")); // Đen
                            txtMessage.setTypeface(null, android.graphics.Typeface.BOLD);
                        }

                        // Nếu MÌNH gửi -> kiểm tra xem NGƯỜI NHẬN đã đọc chưa để hiện avatar/chấm tròn
                        if (isSentByMe) {
                            boolean isReadByOther = "read".equals(status);
                            if (!isReadByOther) {
                                Object readByObj = lastMsgDoc.get("readBy");
                                if (readByObj instanceof java.util.Map) {
                                    java.util.Map<String, Object> readBy = (java.util.Map<String, Object>) readByObj;
                                    if (otherUserId != null && readBy.containsKey(otherUserId)) {
                                        isReadByOther = true;
                                    }
                                }
                            }

                            if (isReadByOther) {
                                // Đã đọc → hiện avatar nhỏ của người nhận
                                imgStatusRead.setVisibility(View.GONE);
                                imgReadAvatar.setVisibility(View.VISIBLE);

                                // Load avatar người nhận
                                if (otherUserId != null) {
                                    userRepository.getUser(otherUserId, new UserRepository.OnUserCallback() {
                                        @Override
                                        public void onSuccess(User user) {
                                            // TODO: Dùng Glide/Picasso load ảnh từ URL
                                        }

                                        @Override
                                        public void onError(String errorMessage) {}
                                    });
                                }
                            } else {
                                // Chưa đọc (sent/delivered) → hiện chấm tròn
                                imgStatusRead.setVisibility(View.VISIBLE);
                                imgReadAvatar.setVisibility(View.GONE);
                            }
                        }
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
