package com.example.beechats;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.ProcessLifecycleOwner;

import com.cloudinary.android.MediaManager;
import com.example.beechats.data.repositories.UserRepository;
import com.example.beechats.utils.AppLifecycleObserver;
import com.example.beechats.utils.ThemeHelper;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
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
        ThemeHelper.applyStoredNightMode(this);
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

    private void initZegoCallUIKit() {
        if (ZEGOCLOUD_APP_ID <= 0) {
            Log.w(TAG, "ZEGOCLOUD_APP_ID chưa được cấu hình, bỏ qua init Zego Call UIKit");
            return;
        }

        ZegoTokenProvider provider = new ZegoTokenProvider() {
            @Override
            public void getToken(String userID, ZegoTokenCallback callback) {
                getTokenFromCloudFunction(userID, 24 * 60 * 60, callback);
            }
        };

        ZegoCallManager.getInstance().init(ZEGOCLOUD_APP_ID, this, provider);
    }

    private void getTokenFromCloudFunction(String userID, long effectiveTime, ZegoTokenCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", userID);
        data.put("effective_time", effectiveTime);

        FirebaseFunctions.getInstance().getHttpsCallable("getToken")
                .call(data)
                .continueWith(task -> task.getResult().getData())
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Exception e = task.getException();
                        if (e instanceof FirebaseFunctionsException) {
                            FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                            Log.e(TAG, "FirebaseFunctions error: " + ffe.getCode() + ", details=" + ffe.getDetails());
                        } else if (e != null) {
                            Log.e(TAG, "Không lấy được Zego token: " + e.getMessage(), e);
                        }
                        callback.onTokenCallback(-1, null);
                        return;
                    }

                    Object resultData = task.getResult().getData();
                    if (!(resultData instanceof Map)) {
                        Log.e(TAG, "Token response không hợp lệ");
                        callback.onTokenCallback(-1, null);
                        return;
                    }

                    Map<?, ?> result = (Map<?, ?>) resultData;
                    Object tokenObj = result.get("token");
                    String token = tokenObj != null ? tokenObj.toString() : null;
                    if (token == null || token.isEmpty()) {
                        Log.e(TAG, "Token rỗng từ Cloud Function");
                        callback.onTokenCallback(-1, null);
                        return;
                    }

                    Log.d(TAG, "Nhận Zego token thành công");
                    callback.onTokenCallback(0, token);
                });
    }
}
