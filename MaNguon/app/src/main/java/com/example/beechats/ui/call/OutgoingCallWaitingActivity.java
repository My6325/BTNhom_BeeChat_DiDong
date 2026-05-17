package com.example.beechats.ui.call;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.beechats.R;
import com.example.beechats.data.models.CallSession;
import com.example.beechats.data.models.User;
import com.example.beechats.data.repositories.CallRepository;
import com.example.beechats.data.repositories.UserRepository;
import com.google.firebase.firestore.ListenerRegistration;

public class OutgoingCallWaitingActivity extends AppCompatActivity {

    public static final String EXTRA_CALL_ID = "extra_call_id";
    public static final String EXTRA_USER_ID = "extra_user_id";
    public static final String EXTRA_USER_NAME = "extra_user_name";
    public static final String EXTRA_CALL_TYPE = "extra_call_type";
    public static final String EXTRA_PEER_ID = "extra_peer_id";
    public static final String EXTRA_PEER_NAME = "extra_peer_name";

    private String callId;
    private String userId;
    private String userName;
    private String callType;
    private String peerName;
    private String peerId;
    private CallRepository callRepository;
    private ListenerRegistration callListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_waiting);

        callId = getIntent().getStringExtra(EXTRA_CALL_ID);
        userId = getIntent().getStringExtra(EXTRA_USER_ID);
        userName = getIntent().getStringExtra(EXTRA_USER_NAME);
        callType = getIntent().getStringExtra(EXTRA_CALL_TYPE);
        peerName = getIntent().getStringExtra(EXTRA_PEER_NAME);
        peerId = getIntent().getStringExtra(EXTRA_PEER_ID);

        callRepository = new CallRepository();

        TextView txtPeerName = findViewById(R.id.txtPeerName);
        txtPeerName.setText(peerName != null && !peerName.isEmpty() ? peerName : "");

        ImageView imgPeerAvatar = findViewById(R.id.ivWaitingIcon);
        if (peerId != null && !peerId.isEmpty()) {
            new UserRepository().getUser(peerId, new UserRepository.OnUserCallback() {
                @Override
                public void onSuccess(User user) {
                    String photoUrl = user != null ? user.getPhotoUrl() : null;
                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        Glide.with(OutgoingCallWaitingActivity.this)
                                .load(photoUrl)
                                .placeholder(R.drawable.male_avatar)
                                .error(R.drawable.male_avatar)
                                .into(imgPeerAvatar);
                    } else {
                        imgPeerAvatar.setImageResource(R.drawable.male_avatar);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    imgPeerAvatar.setImageResource(R.drawable.male_avatar);
                }
            });
        }

        ImageView btnCancelCall = findViewById(R.id.btnCancelCall);
        btnCancelCall.setOnClickListener(v -> cancelCall());

        listenForCallState();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (callListener != null) {
            callListener.remove();
            callListener = null;
        }
    }

    private void listenForCallState() {
        if (callId == null || callId.isEmpty()) return;
        callListener = callRepository.listenCallSession(callId, new CallRepository.OnCallSessionListener() {
            @Override
            public void onChanged(CallSession callSession) {
                if (callSession == null || callSession.getStatus() == null) return;
                if ("accepted".equals(callSession.getStatus())) {
                    openCallScreen();
                } else if ("rejected".equals(callSession.getStatus()) || "ended".equals(callSession.getStatus())) {
                    finish();
                }
            }

            @Override
            public void onError(String message) {
                // giữ màn hình chờ, không tự thoát để tránh UX xấu khi tạm mất mạng
            }
        });
    }

    private void cancelCall() {
        if (callId != null && !callId.isEmpty()) {
            callRepository.endCall(callId, new CallRepository.OnCallStateCallback() {
                @Override
                public void onSuccess() {
                    finish();
                }

                @Override
                public void onError(String message) {
                    finish();
                }
            });
        } else {
            finish();
        }
    }

    private void openCallScreen() {
        Intent intent = new Intent(this, CallActivity.class);
        intent.putExtra(CallActivity.EXTRA_CALL_ID, callId);
        intent.putExtra(CallActivity.EXTRA_USER_ID, userId);
        intent.putExtra(CallActivity.EXTRA_USER_NAME, userName);
        intent.putExtra(CallActivity.EXTRA_CALL_TYPE, callType);
        startActivity(intent);
        finish();
    }
}
