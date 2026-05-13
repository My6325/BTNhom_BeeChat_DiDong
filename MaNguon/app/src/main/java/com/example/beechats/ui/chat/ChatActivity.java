package com.example.beechats.ui.chat;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beechats.R;
import com.example.beechats.data.models.Message;
import com.example.beechats.data.repositories.MessageRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    // Intent extras keys — dùng khi mở ChatActivity từ nơi khác
    public static final String EXTRA_CONVERSATION_ID = "conversation_id";
    public static final String EXTRA_RECEIVER_NAME = "receiver_name";

    private RecyclerView recyclerViewChat;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private EditText editMessage;
    private ImageView btnSend;
    private ImageView btnBack;
    private TextView txtName;

    private MessageRepository messageRepository;
    private ListenerRegistration messageListener;

    private String currentUserId;
    private String currentUserName;
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

        // Lấy conversationId và tên người nhận từ Intent
        conversationId = getIntent().getStringExtra(EXTRA_CONVERSATION_ID);
        String receiverName = getIntent().getStringExtra(EXTRA_RECEIVER_NAME);

        if (conversationId == null || conversationId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy hội thoại!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        messageRepository = new MessageRepository();

        initViews();
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
