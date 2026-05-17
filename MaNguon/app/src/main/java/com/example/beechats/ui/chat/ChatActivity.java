package com.example.beechats.ui.chat;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.content.Intent;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.beechats.R;
import com.example.beechats.data.models.CallSession;
import com.example.beechats.data.models.Message;
import com.example.beechats.data.repositories.CallRepository;
import com.example.beechats.data.repositories.MessageRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    // Intent extras keys — dùng khi mở ChatActivity từ nơi khác
    public static final String EXTRA_CONVERSATION_ID = "conversation_id";
    public static final String EXTRA_RECEIVER_ID = "receiver_id";
    public static final String EXTRA_RECEIVER_NAME = "receiver_name";

    private RecyclerView recyclerViewChat;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private EditText editMessage;
    private ImageView btnSend;
    private ImageView btnPickImage;
    private ImageView btnBack;
    private TextView txtName;

    private MessageRepository messageRepository;
    private CallRepository callRepository;
    private ListenerRegistration messageListener;
    private ActivityResultLauncher<String> pickImageLauncher;

    private String currentUserId;
    private String currentUserName;
    private String receiverId;
    private String receiverName;
    private String conversationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_chat);

        // Lấy thông tin user hiện tại từ Firebase Auth
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            currentUserId = firebaseUser.getUid();
            currentUserName = firebaseUser.getDisplayName() != null
                    ? firebaseUser.getDisplayName() : "Tôi";
        } else {
            Toast.makeText(this, "Chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Lấy conversationId, receiverId và tên người nhận từ Intent
        conversationId = getIntent().getStringExtra(EXTRA_CONVERSATION_ID);
        receiverId = getIntent().getStringExtra(EXTRA_RECEIVER_ID);
        receiverName = getIntent().getStringExtra(EXTRA_RECEIVER_NAME);

        if (conversationId == null || conversationId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy hội thoại!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        messageRepository = new MessageRepository();
        callRepository = new CallRepository();

        initViews();
        setupImagePicker();
        setupRecyclerView();
        setupListeners();

        // Hiển thị tên người đang chat
        if (receiverName != null && !receiverName.isEmpty()) {
            txtName.setText(receiverName);
        }
    }

    private void initViews() {
        recyclerViewChat = findViewById(R.id.recyclerViewChat);
        editMessage = findViewById(R.id.editMessage);
        btnSend = findViewById(R.id.btnSend);
        btnPickImage = findViewById(R.id.btnPickImage);
        btnBack = findViewById(R.id.btnBack);
        txtName = findViewById(R.id.txtName);
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList, currentUserId);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Hiển thị tin nhắn mới nhất ở dưới cùng
        recyclerViewChat.setLayoutManager(layoutManager);
        recyclerViewChat.setAdapter(messageAdapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> {
            String text = editMessage.getText().toString().trim();
            if (!TextUtils.isEmpty(text)) {
                sendMessage(text);
            }
        });

        btnPickImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        ImageView btnVideoCall = findViewById(R.id.img_telephone);
        if (btnVideoCall != null) {
            btnVideoCall.setOnClickListener(v -> startVideoCall());
        }
    }

    private void setupImagePicker() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        uploadImageAndSend(uri);
                    }
                }
        );
    }

    /**
     * Gửi tin nhắn lên Firebase Firestore thông qua MessageRepository.
     */
    private void sendMessage(String text) {
        // Xóa text ở ô nhập liệu ngay lập tức để UX mượt hơn
        editMessage.setText("");

        messageRepository.sendMessage(conversationId, currentUserId, currentUserName, text,
                new MessageRepository.OnSendMessageCallback() {
                    @Override
                    public void onSuccess(String messageId) {
                        Log.d(TAG, "Gửi tin nhắn thành công: " + messageId);
                        // Tin nhắn sẽ tự động xuất hiện thông qua listener real-time
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "Lỗi gửi tin nhắn: " + errorMessage);
                        Toast.makeText(ChatActivity.this,
                                "Gửi thất bại: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadImageAndSend(Uri uri) {
        long fileSize = getFileSize(uri);
        if (fileSize > 100L * 1024L * 1024L) {
            Toast.makeText(this, "Ảnh phải nhỏ hơn 100MB", Toast.LENGTH_SHORT).show();
            return;
        }

        MediaManager.get().upload(uri)
                .unsigned("beechat_chat")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        Log.d(TAG, "Bắt đầu upload ảnh: " + requestId);
                    }

                    @Override
                    public void onProgress(String requestId, long bytesCurrent, long bytesTotal) { }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = String.valueOf(resultData.get("secure_url"));
                        sendImageMessage(imageUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Log.e(TAG, "Upload ảnh thất bại: " + error.getDescription());
                        Toast.makeText(ChatActivity.this,
                                "Upload ảnh thất bại", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) { }
                })
                .dispatch();
    }

    private long getFileSize(Uri uri) {
        try {
            if (getContentResolver().openFileDescriptor(uri, "r") != null) {
                return getContentResolver().openFileDescriptor(uri, "r").getStatSize();
            }
        } catch (Exception e) {
            Log.e(TAG, "Không đọc được kích thước file", e);
        }
        return -1;
    }

    private void startVideoCall() {
        if (TextUtils.isEmpty(currentUserId) || TextUtils.isEmpty(conversationId)) {
            Toast.makeText(this, "Không thể bắt đầu cuộc gọi", Toast.LENGTH_SHORT).show();
            return;
        }

        String displayReceiverName = receiverName != null ? receiverName : (txtName != null ? txtName.getText().toString() : "");
        String calleeId = TextUtils.isEmpty(receiverId) ? conversationId : receiverId;
        callRepository.createOutgoingCall(
                currentUserId,
                currentUserName,
                calleeId,
                displayReceiverName,
                "video",
                new CallRepository.OnCallSessionCallback() {
                    @Override
                    public void onSuccess(CallSession callSession) {
                        Intent intent = new Intent(ChatActivity.this, com.example.beechats.ui.call.VideoCallActivity.class);
                        intent.putExtra(com.example.beechats.ui.call.VideoCallActivity.EXTRA_CALL_ID, callSession.getCallId());
                        intent.putExtra(com.example.beechats.ui.call.VideoCallActivity.EXTRA_ROOM_ID, callSession.getRoomId());
                        intent.putExtra(com.example.beechats.ui.call.VideoCallActivity.EXTRA_CALL_TYPE, callSession.getType());
                        intent.putExtra(com.example.beechats.ui.call.VideoCallActivity.EXTRA_CALLER_ID, callSession.getCallerId());
                        intent.putExtra(com.example.beechats.ui.call.VideoCallActivity.EXTRA_CALLEE_ID, callSession.getCalleeId());
                        intent.putExtra(com.example.beechats.ui.call.VideoCallActivity.EXTRA_CALLER_NAME, callSession.getCallerName());
                        intent.putExtra(com.example.beechats.ui.call.VideoCallActivity.EXTRA_CALLEE_NAME, callSession.getCalleeName());
                        startActivity(intent);
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(ChatActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void sendImageMessage(String imageUrl) {
        if (TextUtils.isEmpty(imageUrl)) {
            Toast.makeText(this, "Không lấy được link ảnh", Toast.LENGTH_SHORT).show();
            return;
        }

        messageRepository.sendMediaMessage(conversationId, currentUserId, currentUserName,
                "image", imageUrl, null, null,
                new MessageRepository.OnSendMessageCallback() {
                    @Override
                    public void onSuccess(String messageId) {
                        Log.d(TAG, "Gửi ảnh thành công: " + messageId);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "Lỗi lưu ảnh vào Firestore: " + errorMessage);
                        Toast.makeText(ChatActivity.this,
                                "Lưu ảnh thất bại", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Bắt đầu lắng nghe tin nhắn real-time khi Activity hiện lên.
     */
    @Override
    protected void onStart() {
        super.onStart();
        startListeningMessages();
    }

    /**
     * Dừng lắng nghe tin nhắn khi Activity bị ẩn để tránh memory leak.
     */
    @Override
    protected void onStop() {
        super.onStop();
        if (messageListener != null) {
            messageListener.remove();
            messageListener = null;
        }
    }

    private void startListeningMessages() {
        Log.d(TAG, "Bắt đầu lắng nghe tin nhắn cho conversationId: " + conversationId);

        messageListener = messageRepository.listenToMessages(conversationId,
                new MessageRepository.OnMessagesCallback() {
                    @Override
                    public void onSuccess(List<Message> messages) {
                        Log.d(TAG, "Nhận được " + messages.size() + " tin nhắn");

                        messageList.clear();
                        messageList.addAll(messages);
                        messageAdapter.updateReadStatus();
                        messageAdapter.notifyDataSetChanged();

                        // Tự động cuộn xuống tin nhắn mới nhất
                        if (!messageList.isEmpty()) {
                            recyclerViewChat.scrollToPosition(messageList.size() - 1);
                        }

                        // Đánh dấu đã đọc khi mở cuộc hội thoại
                        messageRepository.markAsRead(conversationId, currentUserId,
                                new MessageRepository.OnMessageStatusCallback() {
                                    @Override
                                    public void onSuccess() { }

                                    @Override
                                    public void onError(String errorMessage) {
                                        Log.e(TAG, "Lỗi markAsRead: " + errorMessage);
                                    }
                                });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "Lỗi lắng nghe tin nhắn: " + errorMessage);
                        Toast.makeText(ChatActivity.this,
                                "Lỗi tải tin nhắn", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
