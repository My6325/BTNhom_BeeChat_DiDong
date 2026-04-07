package com.example.beechats.data.models;

import java.util.Date;

public class BlockedUser {
    private String blockedUserId;
    private Date blockedAt;

    public BlockedUser() {}

    public String getBlockedUserId() {
        return blockedUserId;
    }

    public void setBlockedUserId(String blockedUserId) {
        this.blockedUserId = blockedUserId;
    }

    public Date getBlockedAt() {
        return blockedAt;
    }

    public void setBlockedAt(Date blockedAt) {
        this.blockedAt = blockedAt;
    }
}
