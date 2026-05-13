package com.example.beechats;

import android.app.Application;

import androidx.lifecycle.ProcessLifecycleOwner;

import com.cloudinary.android.MediaManager;
import com.example.beechats.BuildConfig;
import com.example.beechats.data.repositories.UserRepository;
import com.example.beechats.utils.AppLifecycleObserver;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

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
        registerLifecycleObserver();
        initCloudinary();
    }

    /**
     * Đăng ký AppLifecycleObserver để tự động cập nhật isOnline theo foreground/background.
     * ProcessLifecycleOwner đảm bảo onStop() chỉ kích hoạt khi app thực sự ra background,
     * không phải khi chuyển giữa các Activity.
     */
    private void registerLifecycleObserver() {
        ProcessLifecycleOwner.get().getLifecycle().addObserver(
                new AppLifecycleObserver(FirebaseAuth.getInstance(), new UserRepository())
        );
    }

    private void initCloudinary() {
        Map<String, Object> config = new HashMap<>();
        config.put("cloud_name", "dkhkio1ml");
        MediaManager.init(this, config);
    }
}
