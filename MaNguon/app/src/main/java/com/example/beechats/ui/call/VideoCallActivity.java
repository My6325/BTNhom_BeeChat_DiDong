package com.example.beechats.ui.call;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.beechats.data.repositories.CallRepository;

import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallConfig;
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallFragment;
import com.zegocloud.uikit.prebuilt.call.invite.internal.ZegoCallType;
import com.zegocloud.uikit.prebuilt.call.invite.internal.ZegoCallUser;

public class VideoCallActivity extends AppCompatActivity {
    public static final String EXTRA_CALL_ID = "call_id";
    public static final String EXTRA_ROOM_ID = "room_id";
    public static final String EXTRA_CALL_TYPE = "call_type";
    public static final String EXTRA_CALLER_ID = "caller_id";
    public static final String EXTRA_CALLEE_ID = "callee_id";
    public static final String EXTRA_CALLEE_NAME = "callee_name";
    public static final String EXTRA_CALLER_NAME = "caller_name";

    private static final String TAG = "VideoCallActivity";

    private CallRepository callRepository;
    private String callId;
    private String roomId;
    private String callType;
    private String callerId;
    private String callerName;
    private String calleeId;
    private String calleeName;
    private FrameLayout container;
    private TextView txtStatus;
    private final int fragmentContainerId = View.generateViewId();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        callRepository = new CallRepository();
        callId = getIntent().getStringExtra(EXTRA_CALL_ID);
        roomId = getIntent().getStringExtra(EXTRA_ROOM_ID);
        callType = getIntent().getStringExtra(EXTRA_CALL_TYPE);
        callerId = getIntent().getStringExtra(EXTRA_CALLER_ID);
        callerName = getIntent().getStringExtra(EXTRA_CALLER_NAME);
        calleeId = getIntent().getStringExtra(EXTRA_CALLEE_ID);
        calleeName = getIntent().getStringExtra(EXTRA_CALLEE_NAME);

        buildUi();

        if (callId == null || roomId == null) {
            txtStatus.setText("Thiếu dữ liệu cuộc gọi");
            Toast.makeText(this, "Thiếu dữ liệu cuộc gọi", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (callerId == null || callerId.trim().isEmpty()) {
            txtStatus.setText("Thiếu callerId");
            finish();
            return;
        }

        attachZegoFragment();
    }

    private void buildUi() {
        container = new FrameLayout(this);
        container.setId(fragmentContainerId);
        container.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        container.setBackgroundColor(0xFF000000);

        txtStatus = new TextView(this);
        txtStatus.setTextColor(0xFFFFFFFF);
        txtStatus.setTextSize(18f);
        txtStatus.setGravity(Gravity.CENTER);
        txtStatus.setText("Đang khởi tạo cuộc gọi...");

        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        textParams.gravity = Gravity.CENTER;
        container.addView(txtStatus, textParams);

        setContentView(container);
    }

    private void attachZegoFragment() {
        try {
            ZegoCallUser localUser = new ZegoCallUser(callerId, callerName != null ? callerName : callerId);

            ZegoUIKitPrebuiltCallConfig config = "video".equalsIgnoreCase(callType)
                    ? ZegoUIKitPrebuiltCallConfig.oneOnOneVideoCall()
                    : ZegoUIKitPrebuiltCallConfig.oneOnOneVoiceCall();

            ZegoUIKitPrebuiltCallFragment fragment = ZegoUIKitPrebuiltCallFragment.newInstance(
                    0L,
                    "",
                    localUser.getUserID(),
                    localUser.getUserName(),
                    roomId,
                    config
            );

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(fragmentContainerId, fragment)
                    .commitNowAllowingStateLoss();

            txtStatus.setText("Đã vào phòng: " + roomId);
            callRepository.updateCallStatus(callId, "connected", new CallRepository.OnCallStateCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Call status updated to connected");
                }

                @Override
                public void onError(String message) {
                    Log.w(TAG, "Không update được trạng thái call: " + message);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Không attach được Zego fragment", e);
            txtStatus.setText("Không khởi tạo được màn gọi");
            Toast.makeText(this, "Không khởi tạo được cuộc gọi", Toast.LENGTH_SHORT).show();
        }
    }
}
