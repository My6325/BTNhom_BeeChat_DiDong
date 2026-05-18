package com.example.beechats;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.lifecycle.ProcessLifecycleOwner;

import com.cloudinary.android.MediaManager;
import com.example.beechats.data.repositories.ConversationRepository;
import com.example.beechats.data.repositories.UserRepository;
import com.example.beechats.utils.AppLifecycleObserver;
import com.example.beechats.utils.ThemeHelper;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Application class chính của BeeChat.
 * Khởi tạo Firebase, Cloudinary và Zego khi app bắt đầu.
 */
public class BeeChatsApp extends Application {
    private static final String TAG = "BeeChatsApp";
    private static final String PREFS_NAME = "beechats_app_prefs";
    private static final String KEY_CONVERSATION_MIGRATION_DONE = "conversation_migration_done";
    private boolean isZegoServiceStarted = false;
    private final ExecutorService appInitExecutor = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate() {
        super.onCreate();
        ThemeHelper.applyStoredNightMode(this);
        FirebaseApp.initializeApp(this);
        registerLifecycleObserver();
        initCloudinary();
        runStartupScripts();
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

    private void runStartupScripts() {
        appInitExecutor.execute(() -> {
            if (!isConversationMigrationDone()) {
                new ConversationRepository().normalizeConversationData(new ConversationRepository.OnMigrationCallback() {
                    @Override
                    public void onSuccess(int updatedCount) {
                        markConversationMigrationDone();
                        Log.d(TAG, "Conversation migration completed. Updated " + updatedCount + " documents.");
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "Conversation migration failed", new RuntimeException(errorMessage));
                    }
                });
            }
        });
    }

    private boolean isConversationMigrationDone() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_CONVERSATION_MIGRATION_DONE, false);
    }

    private void markConversationMigrationDone() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_CONVERSATION_MIGRATION_DONE, true).apply();
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
