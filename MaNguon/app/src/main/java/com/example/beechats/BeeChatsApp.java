package com.example.beechats;

import android.app.Application;

import com.google.firebase.FirebaseApp;

/**
 * Application class chính của BeeChat.
 * Khởi tạo Firebase và các service cần thiết khi app bắt đầu.
 */
public class BeeChatsApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
    }
}
