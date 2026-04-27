package com.example.beechats.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.beechats.R;
import com.example.beechats.data.repositories.ConversationRepository;
import com.example.beechats.data.repositories.FirebaseAuthRepository;
import com.example.beechats.data.repositories.MessageRepository;
import com.google.firebase.firestore.ListenerRegistration;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);
        LinearLayout lnMain;
        lnMain=findViewById(R.id.main);
        lnMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(WelcomeActivity.this, IntroduceActivity.class);
                // Flag này giúp loại bỏ hiệu ứng chuyển tiếp giữa các Activity
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                //Gọi để không có hiệu ứng mặc định
                overridePendingTransition(0, 0);
                finish();
            }
        });

        testListenMessages();
    }

    // =====================================================================
    // TEST CODE — Task 2.3: listenToMessages (SnapshotListener)
    // Xóa sau khi verify trên Firebase Console
    // =====================================================================

    private static final String TAG = "BeeChat_Test";
    private static final String USER_A_EMAIL = "test.conv.a@beechat.com";
    private static final String USER_B_EMAIL = "test.conv.b@beechat.com";
    private static final String TEST_PASS = "ConvPass@1234";

    // Lưu ListenerRegistration để detach trong onStop() — minh họa pattern production
    private ListenerRegistration messagesListener;

    private void testListenMessages() {
        FirebaseAuthRepository authRepo = new FirebaseAuthRepository();
        ConversationRepository convRepo = new ConversationRepository();
        MessageRepository msgRepo = new MessageRepository();

        // Bước 1: login User A để lấy UID
        authRepo.login(USER_A_EMAIL, TEST_PASS, new FirebaseAuthRepository.OnAuthCallback() {
            @Override
            public void onSuccess() {
                String uidA = authRepo.getCurrentUserId();
                Log.d(TAG, "✅ Login User A OK, uid=" + uidA);
                authRepo.logout();
                getUidBAndListen(authRepo, convRepo, msgRepo, uidA);
            }
            @Override
            public void onError(String msg) {
                Log.e(TAG, "❌ Login User A thất bại (cần có dữ liệu từ Task 2.2): " + msg);
            }
        });
    }

    private void getUidBAndListen(FirebaseAuthRepository authRepo, ConversationRepository convRepo,
                                   MessageRepository msgRepo, String uidA) {
        authRepo.login(USER_B_EMAIL, TEST_PASS, new FirebaseAuthRepository.OnAuthCallback() {
            @Override
            public void onSuccess() {
                String uidB = authRepo.getCurrentUserId();
                Log.d(TAG, "✅ Login User B OK, uid=" + uidB);
                // Bước 2: lấy/tạo conversation rồi gắn listener
                convRepo.createOrGetPrivateConversation(uidA, uidB,
                        new ConversationRepository.OnConversationCallback() {
                            @Override
                            public void onSuccess(String conversationId) {
                                Log.d(TAG, "✅ Conversation ID: " + conversationId);
                                // Bước 3: attach listener — lưu registration để detach sau
                                messagesListener = msgRepo.listenToMessages(conversationId,
                                        new MessageRepository.OnMessagesCallback() {
                                            @Override
                                            public void onSuccess(java.util.List<com.example.beechats.data.models.Message> messages) {
                                                Log.d(TAG, "✅ Snapshot nhận được: " + messages.size() + " tin nhắn");
                                                for (com.example.beechats.data.models.Message m : messages) {
                                                    Log.d(TAG, "  → [" + m.getMessageId() + "] " + m.getText());
                                                }
                                                // Detach ngay sau snapshot đầu tiên (chỉ dùng cho test)
                                                // Trong production: gọi messagesListener.remove() trong onStop()
                                                if (messagesListener != null) {
                                                    messagesListener.remove();
                                                    Log.d(TAG, "✅ Listener đã detach");
                                                }
                                            }
                                            @Override
                                            public void onError(String err) {
                                                Log.e(TAG, "❌ listenToMessages lỗi: " + err);
                                            }
                                        });
                            }
                            @Override
                            public void onError(String err) {
                                Log.e(TAG, "❌ Tạo conversation thất bại: " + err);
                            }
                        });
            }
            @Override
            public void onError(String msg) {
                Log.e(TAG, "❌ Login User B thất bại: " + msg);
            }
        });
    }
    // =====================================================================
    // END TEST CODE

}
