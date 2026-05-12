package com.example.beechats.ui.chat;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beechats.R;
import com.example.beechats.data.models.Message;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;
import java.util.Map;

/**
 * Adapter hiển thị danh sách tin nhắn trong RecyclerView.
 * - Tin nhắn gửi (item_chat_user): hiện "Đã gửi" hoặc avatar người nhận nếu đã đọc
 * - Tin nhắn nhận (item_chat_friend): không hiện gì ở cuối
 * Avatar chỉ hiện ở tin nhắn cuối cùng mà đối phương đã đọc.
 */
public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final List<Message> messageList;
    private final String currentUserId;

    // Vị trí tin nhắn cuối cùng do mình gửi mà đối phương đã đọc
    private int lastReadPosition = -1;

    public MessageAdapter(List<Message> messageList, String currentUserId) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    /**
     * Tìm vị trí tin nhắn cuối cùng do mình gửi mà đối phương đã đọc.
     * Gọi trước notifyDataSetChanged().
     */
    public void updateReadStatus() {
        lastReadPosition = -1;
        for (int i = messageList.size() - 1; i >= 0; i--) {
            Message msg = messageList.get(i);
            if (msg.getSenderId() != null && msg.getSenderId().equals(currentUserId)) {
                // Kiểm tra status == "read"
                if ("read".equals(msg.getStatus())) {
                    lastReadPosition = i;
                    break;
                }
                // Kiểm tra readBy map có UID của đối phương
                Map<String, Object> readBy = msg.getReadBy();
                if (readBy != null) {
                    for (String uid : readBy.keySet()) {
                        if (!uid.equals(currentUserId)) {
                            lastReadPosition = i;
                            break;
                        }
                    }
                    if (lastReadPosition >= 0) break;
                }
            }
        }
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
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_user, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_friend, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);

        boolean isRead = "read".equals(message.getStatus());
        // Kiểm tra thêm readBy
        if (!isRead && message.getReadBy() != null) {
            // Nếu là người gửi, xem người nhận đã đọc chưa. 
            // Nếu là người nhận, xem mình đã đọc chưa (hoặc người khác trong nhóm).
            for (String uid : message.getReadBy().keySet()) {
                if (holder.getItemViewType() == VIEW_TYPE_SENT) {
                    if (!uid.equals(currentUserId)) {
                        isRead = true;
                        break;
                    }
                } else {
                    if (uid.equals(currentUserId) || "read".equals(message.getStatus())) {
                        isRead = true;
                        break;
                    }
                }
            }
        }

        if (holder.getItemViewType() == VIEW_TYPE_SENT) {
            boolean showAvatar = (position == lastReadPosition);
            ((SentMessageViewHolder) holder).bind(message, isRead, showAvatar);
        } else {
            ((ReceivedMessageViewHolder) holder).bind(message, isRead);
        }
    }

    @Override
    public int getItemCount() {
        return messageList != null ? messageList.size() : 0;
    }

    // ─── ViewHolder cho tin nhắn GỬI ĐI (item_chat_user.xml) ───

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMessage;
        private final TextView tvStatusText;
        private final ShapeableImageView imgReadAvatar;

        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvStatusText = itemView.findViewById(R.id.tvStatusText);
            imgReadAvatar = itemView.findViewById(R.id.imgReadAvatar);
        }

        public void bind(Message message, boolean isRead, boolean showAvatar) {
            if (message.isRecalled()) {
                tvMessage.setText("Tin nhắn đã thu hồi");
                tvMessage.setAlpha(0.5f);
                tvMessage.setTextColor(Color.parseColor("#888888"));
                tvStatusText.setVisibility(View.GONE);
                imgReadAvatar.setVisibility(View.GONE);
                return;
            }

            tvMessage.setText(message.getText());
            tvMessage.setAlpha(1.0f);

            if (showAvatar && isRead) {
                // Tin nhắn cuối cùng được đọc → hiện avatar người nhận
                tvStatusText.setVisibility(View.GONE);
                imgReadAvatar.setVisibility(View.VISIBLE);
            } else if (!isRead) {
                // Chưa đọc → hiện "Đã gửi"
                tvStatusText.setVisibility(View.VISIBLE);
                imgReadAvatar.setVisibility(View.GONE);
            } else {
                // Đã đọc nhưng không phải tin cuối cùng → ẩn cả hai
                tvStatusText.setVisibility(View.GONE);
                imgReadAvatar.setVisibility(View.GONE);
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

        public void bind(Message message, boolean isRead) {
            if (message.isRecalled()) {
                txtMessage.setText("Tin nhắn đã thu hồi");
                txtMessage.setAlpha(0.5f);
                txtMessage.setTextColor(Color.parseColor("#888888"));
            } else {
                txtMessage.setText(message.getText());
                txtMessage.setAlpha(1.0f);
                txtMessage.setTextColor(Color.parseColor("#000000"));
            }
        }
    }
}
