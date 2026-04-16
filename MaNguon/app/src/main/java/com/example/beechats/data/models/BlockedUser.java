package com.example.beechats.data.models;

import com.google.firebase.Timestamp;

public class BlockedUser {
    private String blockedUserId;
    private Timestamp blockedAt;

    public BlockedUser() {}

    public String getBlockedUserId() {
        return blockedUserId;
    }

    public void setBlockedUserId(String blockedUserId) {
        this.blockedUserId = blockedUserId;
    }

    public Timestamp getBlockedAt() {
        return blockedAt;
    }

    public void setBlockedAt(Timestamp blockedAt) {
        this.blockedAt = blockedAt;
    }
}
