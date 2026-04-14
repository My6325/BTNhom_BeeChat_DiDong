package com.example.beechats.data.models;

import com.google.firebase.Timestamp;

public class ConversationMember {
    private String memberId;
    private String nickname;
    private String role;
    private Timestamp joinedAt;
    private String lastReadMessageId;
    private Timestamp lastReadTimestamp;
    private boolean isMuted;

    public ConversationMember() {}

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Timestamp getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Timestamp joinedAt) {
        this.joinedAt = joinedAt;
    }

    public String getLastReadMessageId() {
        return lastReadMessageId;
    }

    public void setLastReadMessageId(String lastReadMessageId) {
        this.lastReadMessageId = lastReadMessageId;
    }

    public Timestamp getLastReadTimestamp() {
        return lastReadTimestamp;
    }

    public void setLastReadTimestamp(Timestamp lastReadTimestamp) {
        this.lastReadTimestamp = lastReadTimestamp;
    }

    public boolean isMuted() {
        return isMuted;
    }

    public void setMuted(boolean muted) {
        isMuted = muted;
    }
}
