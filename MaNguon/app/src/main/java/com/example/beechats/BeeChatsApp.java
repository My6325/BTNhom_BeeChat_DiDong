package com.example.beechats;

import android.app.Application;

import com.cloudinary.android.MediaManager;
import com.google.firebase.FirebaseApp;

import java.util.HashMap;
import java.util.Map;

/**
 * Application class chính của BeeChat.
 * Khởi tạo Firebase và Cloudinary khi app bắt đầu.
 */
public class BeeChatsApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        initCloudinary();
    }

    private void initCloudinary() {
        Map<String, Object> config = new HashMap<>();
        config.put("cloud_name", "dkhkio1ml");
        MediaManager.init(this, config);
    }
}
