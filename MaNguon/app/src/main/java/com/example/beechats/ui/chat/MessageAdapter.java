package com.example.beechats.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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

    public interface OnMediaClickListener {
        void onVideoClick(String videoUrl);
    }

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final List<Message> messageList;
    private final String currentUserId;
    private final OnMediaClickListener mediaClickListener;
    private String peerUserId;

    // Vị trí tin nhắn cuối cùng do mình gửi mà đối phương đã đọc
    private int lastReadPosition = -1;

    public MessageAdapter(List<Message> messageList, String currentUserId) {
        this(messageList, currentUserId, null);
    }

    public MessageAdapter(List<Message> messageList, String currentUserId, OnMediaClickListener mediaClickListener) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
        this.mediaClickListener = mediaClickListener;
    }

    public void setConversationParticipants(String currentUserId, String peerUserId) {
        this.peerUserId = peerUserId;
        updateReadStatus();
    }

    /**
     * Tìm vị trí tin nhắn cuối cùng do mình gửi mà đối phương đã đọc.
     * Gọi trước notifyDataSetChanged().
     */
    public void updateReadStatus() {
        lastReadPosition = -1;
        for (int i = messageList.size() - 1; i >= 0; i--) {
            Message msg = messageList.get(i);
            if (msg == null || msg.getSenderId() == null || !msg.getSenderId().equals(currentUserId)) {
                continue;
            }
            if (hasPeerReadMessage(msg)) {
                lastReadPosition = i;
                break;
            }
        }
    }

    private boolean hasPeerReadMessage(Message msg) {
        if ("read".equals(msg.getStatus())) {
            return true;
        }
        Map<String, Object> readBy = msg.getReadBy();
        if (readBy == null || readBy.isEmpty()) {
            return false;
        }
        if (peerUserId != null && readBy.containsKey(peerUserId)) {
            return true;
        }
        return !readBy.containsKey(currentUserId) && !readBy.isEmpty();
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

        boolean isRead = hasMessageBeenRead(message);

        if (holder.getItemViewType() == VIEW_TYPE_SENT) {
            boolean showAvatar = (position == lastReadPosition);
            ((SentMessageViewHolder) holder).bind(message, isRead, showAvatar, mediaClickListener);
        } else {
            ((ReceivedMessageViewHolder) holder).bind(message, isRead, mediaClickListener);
        }
    }

    @Override
    public int getItemCount() {
        return messageList != null ? messageList.size() : 0;
    }

    private boolean hasMessageBeenRead(Message message) {
        if (message == null) {
            return false;
        }
        if ("read".equals(message.getStatus())) {
            return true;
        }
        Map<String, Object> readBy = message.getReadBy();
        if (readBy == null || readBy.isEmpty()) {
            return false;
        }
        if (message.getSenderId() != null && message.getSenderId().equals(currentUserId)) {
            if (peerUserId != null && readBy.containsKey(peerUserId)) {
                return true;
            }
            return readBy.size() > 1;
        }
        return readBy.containsKey(currentUserId);
    }

    private static boolean isMediaMessage(Message message) {
        if (message == null) {
            return false;
        }
        String type = message.getType();
        return "image".equals(type) || "video".equals(type);
    }

    // ─── ViewHolder cho tin nhắn GỬI ĐI (item_chat_user.xml) ───

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMessage;
        private final ImageView imgMessage;
        private final TextView tvStatusText;
        private final ShapeableImageView imgReadAvatar;

        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            imgMessage = itemView.findViewById(R.id.imgMessage);
            tvStatusText = itemView.findViewById(R.id.tvStatusText);
            imgReadAvatar = itemView.findViewById(R.id.imgReadAvatar);
        }

        public void bind(Message message, boolean isRead, boolean showAvatar, OnMediaClickListener mediaClickListener) {
            if (message.isRecalled()) {
                showText("Tin nhắn đã thu hồi");
                tvStatusText.setVisibility(View.GONE);
                imgReadAvatar.setVisibility(View.GONE);
                return;
            }

            if (isMediaMessage(message)) {
                showMedia(message, mediaClickListener);
            } else {
                showText(message.getText());
            }

            if (showAvatar && isRead) {
                tvStatusText.setVisibility(View.GONE);
                imgReadAvatar.setVisibility(View.VISIBLE);
            } else if (!isRead) {
                tvStatusText.setVisibility(View.VISIBLE);
                imgReadAvatar.setVisibility(View.GONE);
            } else {
                tvStatusText.setVisibility(View.GONE);
                imgReadAvatar.setVisibility(View.GONE);
            }
        }

        private void showText(String text) {
            tvMessage.setVisibility(View.VISIBLE);
            imgMessage.setVisibility(View.GONE);
            tvMessage.setText(text != null ? text : "");
            tvMessage.setAlpha(1.0f);
            tvMessage.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.black));
        }

        private void showMedia(Message message, OnMediaClickListener mediaClickListener) {
            tvMessage.setVisibility(View.GONE);
            imgMessage.setVisibility(View.VISIBLE);
            String mediaUrl = message.getMediaUrl();
            if (mediaUrl == null || mediaUrl.trim().isEmpty()) {
                mediaUrl = message.getMediaThumbnailUrl();
            }
            final String finalMediaUrl = mediaUrl;
            Glide.with(itemView.getContext())
                    .load(finalMediaUrl)
                    .placeholder(R.drawable.icon)
                    .error(R.drawable.icon)
                    .into(imgMessage);

            if (mediaClickListener != null && "video".equals(message.getType())) {
                itemView.setOnClickListener(v -> mediaClickListener.onVideoClick(finalMediaUrl));
            } else {
                itemView.setOnClickListener(null);
            }
        }

    }

    // ─── ViewHolder cho tin nhắn NHẬN (item_chat_friend.xml) ───

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtMessage;
        private final ImageView imgMessage;

        public ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            txtMessage = itemView.findViewById(R.id.txtMessage);
            imgMessage = itemView.findViewById(R.id.imgMessage);
        }

        public void bind(Message message, boolean isRead, OnMediaClickListener mediaClickListener) {
            itemView.setOnClickListener(null);
            if (message.isRecalled()) {
                txtMessage.setVisibility(View.VISIBLE);
                imgMessage.setVisibility(View.GONE);
                txtMessage.setText("Tin nhắn đã thu hồi");
                txtMessage.setAlpha(0.5f);
                txtMessage.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.gray_dam));
                return;
            }

            if (isMediaMessage(message)) {
                txtMessage.setVisibility(View.GONE);
                imgMessage.setVisibility(View.VISIBLE);
                String mediaUrl = message.getMediaUrl();
                if (mediaUrl == null || mediaUrl.trim().isEmpty()) {
                    mediaUrl = message.getMediaThumbnailUrl();
                }
                final String finalMediaUrl = mediaUrl;
                Glide.with(itemView.getContext())
                        .load(finalMediaUrl)
                        .placeholder(R.drawable.icon)
                        .error(R.drawable.icon)
                        .into(imgMessage);

                if (mediaClickListener != null && "video".equals(message.getType())) {
                    itemView.setOnClickListener(v -> mediaClickListener.onVideoClick(finalMediaUrl));
                }
            } else {
                txtMessage.setVisibility(View.VISIBLE);
                imgMessage.setVisibility(View.GONE);
                txtMessage.setText(message.getText());
                txtMessage.setAlpha(1.0f);
                txtMessage.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.black));
            }
        }
    }
}
