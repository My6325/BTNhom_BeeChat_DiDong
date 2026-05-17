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
import com.google.firebase.auth.FirebaseUser;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Application class chính của BeeChat.
 * Khởi tạo Firebase, Cloudinary và Zego khi app bắt đầu.
 */
public class BeeChatsApp extends Application {
    private static final String TAG = "BeeChatsApp";
    private boolean isZegoServiceStarted = false;

    @Override
    public void onCreate() {
        super.onCreate();
        ThemeHelper.applyStoredNightMode(this);
        FirebaseApp.initializeApp(this);
        registerLifecycleObserver();
        initCloudinary();
        initZegoIfUserLoggedIn();
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

    public void initZegoIfUserLoggedIn() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.d(TAG, "Chưa có user đăng nhập, bỏ qua khởi tạo Zego");
            return;
        }

        if (isZegoServiceStarted) {
            return;
        }

        String appId = BuildConfig.ZEGO_APP_ID;
        String appSign = BuildConfig.ZEGO_APP_SIGN;
        if (appId == null || appId.trim().isEmpty() || appSign == null || appSign.trim().isEmpty()) {
            Log.w(TAG, "Thiếu ZEGO_APP_ID hoặc ZEGO_APP_SIGN, không thể khởi tạo Zego service");
            return;
        }

        String userId = user.getUid();
        String userName = user.getDisplayName() != null ? user.getDisplayName() : user.getEmail();
        if (userName == null || userName.trim().isEmpty()) {
            userName = userId;
        }

        try {
            Class<?> serviceClass = Class.forName("com.zegocloud.uikit.prebuilt.call.invitation.ZegoUIKitPrebuiltCallInvitationService");
            Method initMethod = serviceClass.getMethod(
                    "init",
                    Application.class,
                    long.class,
                    String.class,
                    String.class,
                    String.class,
                    String.class,
                    Object.class
            );

            long parsedAppId = Long.parseLong(appId.trim());
            initMethod.invoke(null, this, parsedAppId, appSign.trim(), userId, userName, null, null);
            isZegoServiceStarted = true;
        } catch (Exception e) {
            Log.e(TAG, "Không thể khởi tạo Zego call invitation service", e);
        }
    }
}
