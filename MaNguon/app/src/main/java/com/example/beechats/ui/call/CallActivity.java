package com.example.beechats.ui.call;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.beechats.BuildConfig;
import com.example.beechats.R;

import java.lang.reflect.Method;

public class CallActivity extends AppCompatActivity {

    public static final String EXTRA_CALL_ID = "extra_call_id";
    public static final String EXTRA_USER_ID = "extra_user_id";
    public static final String EXTRA_USER_NAME = "extra_user_name";
    public static final String EXTRA_CALL_TYPE = "extra_call_type";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        if (savedInstanceState == null) {
            addCallFragment();
        }
    }

    private void addCallFragment() {
        long appID = Long.parseLong(BuildConfig.ZEGO_APP_ID);
        String appSign = BuildConfig.ZEGO_APP_SIGN;
        String callID = getIntent().getStringExtra(EXTRA_CALL_ID);
        String userID = getIntent().getStringExtra(EXTRA_USER_ID);
        String userName = getIntent().getStringExtra(EXTRA_USER_NAME);
        String callType = getIntent().getStringExtra(EXTRA_CALL_TYPE);

        if (callID == null || userID == null || userName == null) {
            finish();
            return;
        }

        try {
            Class<?> configClass = loadClass(
                "im.zego.zego_uikit.ZegoUIKitPrebuiltCallConfig",
                "com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallConfig"
            );
            Object config = configClass.getDeclaredConstructor().newInstance();
            if ("voice".equals(callType)) {
                setBooleanField(config, "turnOnCameraWhenJoining", false);
                Object topMenuBarConfig = getFieldValue(config, "topMenuBarConfig");
                if (topMenuBarConfig != null) {
                    setBooleanField(topMenuBarConfig, "showCameraButton", false);
                }
            } else {
                setBooleanField(config, "turnOnCameraWhenJoining", true);
            }

            Class<?> fragmentClass = loadClass(
                "im.zego.zego_uikit.ZegoUIKitPrebuiltCallFragment",
                "com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallFragment"
            );
            Method newInstanceMethod = fragmentClass.getMethod(
                "newInstance",
                long.class,
                String.class,
                String.class,
                String.class,
                String.class,
                configClass
            );
            Object fragment = newInstanceMethod.invoke(null, appID, appSign, userID, userName, callID, config);
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, (Fragment) fragment)
                .commitNow();
        } catch (Throwable e) {
            Log.e("CallActivity", "Cannot init Zego call UI", e);
            Toast.makeText(this, "Không thể mở màn hình cuộc gọi", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private Class<?> loadClass(String... candidates) throws ClassNotFoundException {
        ClassNotFoundException last = null;
        for (String name : candidates) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException e) {
                last = e;
            }
        }
        throw last != null ? last : new ClassNotFoundException("Zego class not found");
    }


    private void setBooleanField(Object target, String fieldName, boolean value) {
        try {
            java.lang.reflect.Field field = target.getClass().getField(fieldName);
            field.setBoolean(target, value);
        } catch (Exception ignored) {
        }
    }

    private Object getFieldValue(Object target, String fieldName) {
        try {
            java.lang.reflect.Field field = target.getClass().getField(fieldName);
            return field.get(target);
        } catch (Exception e) {
            return null;
        }
    }
}
