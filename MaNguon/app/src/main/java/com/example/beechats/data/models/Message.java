package com.example.beechats.data.models;

import com.google.firebase.Timestamp;
import java.util.List;
import java.util.Map;

public class Message {
    private String messageId;
    private String senderId;
    private String senderName;
    private String text;
    private String type;

    // Đa phương tiện
    private String mediaUrl;
    private String mediaThumbnailUrl;
    private Long mediaSize;
    private Long mediaDuration;

    // Trạng thái
    private String status;
    private Map<String, Timestamp> readBy;
    private boolean isRecalled;
    private Timestamp recalledAt;
    private List<String> deletedFor;

    // Trả lời & phản ứng
    private ReplyInfo replyTo;
    private Map<String, String> reactions;

    // Ghim
    private boolean isPinned;
    private String pinnedBy;
    private Timestamp pinnedAt;

    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Message() {}

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getMediaThumbnailUrl() {
        return mediaThumbnailUrl;
    }

    public void setMediaThumbnailUrl(String mediaThumbnailUrl) {
        this.mediaThumbnailUrl = mediaThumbnailUrl;
    }

    public Long getMediaSize() {
        return mediaSize;
    }

    public void setMediaSize(Long mediaSize) {
        this.mediaSize = mediaSize;
    }

    public Long getMediaDuration() {
        return mediaDuration;
    }

    public void setMediaDuration(Long mediaDuration) {
        this.mediaDuration = mediaDuration;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Date> getReadBy() {
        return readBy;
    }

    public void setReadBy(Map<String, Date> readBy) {
        this.readBy = readBy;
    }

    public boolean isRecalled() {
        return isRecalled;
    }

    public void setRecalled(boolean recalled) {
        isRecalled = recalled;
    }

    public Timestamp getRecalledAt() {
        return recalledAt;
    }

    public void setRecalledAt(Timestamp recalledAt) {
        this.recalledAt = recalledAt;
    }

    public List<String> getDeletedFor() {
        return deletedFor;
    }

    public void setDeletedFor(List<String> deletedFor) {
        this.deletedFor = deletedFor;
    }

    public ReplyInfo getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(ReplyInfo replyTo) {
        this.replyTo = replyTo;
    }

    public Map<String, String> getReactions() {
        return reactions;
    }

    public void setReactions(Map<String, String> reactions) {
        this.reactions = reactions;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public void setPinned(boolean pinned) {
        isPinned = pinned;
    }

    public String getPinnedBy() {
        return pinnedBy;
    }

    public void setPinnedBy(String pinnedBy) {
        this.pinnedBy = pinnedBy;
    }

    public Timestamp getPinnedAt() {
        return pinnedAt;
    }

    public void setPinnedAt(Timestamp pinnedAt) {
        this.pinnedAt = pinnedAt;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
