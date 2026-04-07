package com.example.beechats.data.models;

public class UserSettings {
    private boolean isOnlineVisible;
    private boolean darkMode;
    private boolean notificationsEnabled;

    public UserSettings() {
        this.isOnlineVisible = true;
        this.darkMode = false;
        this.notificationsEnabled = true;
    }

    public boolean isOnlineVisible() {
        return isOnlineVisible;
    }

    public void setOnlineVisible(boolean onlineVisible) {
        isOnlineVisible = onlineVisible;
    }

    public boolean isDarkMode() {
        return darkMode;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }
}
