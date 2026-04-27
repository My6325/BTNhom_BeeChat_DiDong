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

        testMessageStatus();
    }

    // =====================================================================
    // TEST CODE — Task 2.4: Trạng thái tin nhắn (sent → delivered → read)
    // Xóa sau khi verify trên Firebase Console
    // =====================================================================

    private static final String TAG = "BeeChat_Test";
    private static final String USER_A_EMAIL = "test.conv.a@beechat.com";
    private static final String USER_B_EMAIL = "test.conv.b@beechat.com";
    private static final String TEST_PASS = "ConvPass@1234";

    private void testMessageStatus() {
        FirebaseAuthRepository authRepo = new FirebaseAuthRepository();
        ConversationRepository convRepo = new ConversationRepository();
        MessageRepository msgRepo = new MessageRepository();

        // Bước 1: Login A để lấy UID, sau đó logout
        authRepo.login(USER_A_EMAIL, TEST_PASS, new FirebaseAuthRepository.OnAuthCallback() {
            @Override
            public void onSuccess() {
                String uidA = authRepo.getCurrentUserId();
                Log.d(TAG, "✅ Login User A OK, uid=" + uidA);
                authRepo.logout();
                getUidBAndTest(authRepo, convRepo, msgRepo, uidA);
            }
            @Override
            public void onError(String msg) {
                Log.e(TAG, "❌ Login User A thất bại: " + msg);
            }
        });
    }

    private void getUidBAndTest(FirebaseAuthRepository authRepo, ConversationRepository convRepo,
                                MessageRepository msgRepo, String uidA) {
        // Bước 2: Login B để lấy UID
        authRepo.login(USER_B_EMAIL, TEST_PASS, new FirebaseAuthRepository.OnAuthCallback() {
            @Override
            public void onSuccess() {
                String uidB = authRepo.getCurrentUserId();
                Log.d(TAG, "✅ Login User B OK, uid=" + uidB);
                authRepo.logout();

                // Bước 3: Login lại A để gửi tin nhắn (cần đúng auth context)
                authRepo.login(USER_A_EMAIL, TEST_PASS, new FirebaseAuthRepository.OnAuthCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "✅ Re-login User A OK");
                        convRepo.createOrGetPrivateConversation(uidA, uidB,
                                new ConversationRepository.OnConversationCallback() {
                                    @Override
                                    public void onSuccess(String convId) {
                                        Log.d(TAG, "✅ Conversation ID: " + convId);
                                        sendAndMarkStatus(msgRepo, convId, uidA, uidB);
                                    }
                                    @Override
                                    public void onError(String err) {
                                        Log.e(TAG, "❌ Tạo conversation thất bại: " + err);
                                    }
                                });
                    }
                    @Override
                    public void onError(String msg) {
                        Log.e(TAG, "❌ Re-login User A thất bại: " + msg);
                    }
                });
            }
            @Override
            public void onError(String msg) {
                Log.e(TAG, "❌ Login User B thất bại: " + msg);
            }
        });
    }

    private void sendAndMarkStatus(MessageRepository msgRepo, String convId,
                                   String uidA, String uidB) {
        // Bước 4: Gửi tin nhắn (TC1 → status="sent" tự động)
        msgRepo.sendMessage(convId, uidA, "User A Test", "Task 2.4: test message status",
                new MessageRepository.OnSendMessageCallback() {
                    @Override
                    public void onSuccess(String messageId) {
                        Log.d(TAG, "✅ Gửi tin thành công, messageId=" + messageId + " (status=sent)");

                        // Bước 5: markDelivered — TC2: đối phương online → status="delivered"
                        msgRepo.markDelivered(convId, messageId,
                                new MessageRepository.OnMessageStatusCallback() {
                                    @Override
                                    public void onSuccess() {
                                        Log.d(TAG, "✅ markDelivered OK → status=delivered");

                                        // Bước 6: markAsRead — TC3: đối phương mở chat → status="read"
                                        msgRepo.markAsRead(convId, uidB,
                                                new MessageRepository.OnMessageStatusCallback() {
                                                    @Override
                                                    public void onSuccess() {
                                                        Log.d(TAG, "✅ markAsRead OK → status=read, readBy có uid=" + uidB);
                                                        Log.d(TAG, "✅ Task 2.4 hoàn tất! Kiểm tra Firebase Console:");
                                                        Log.d(TAG, "   conversations/" + convId + "/messages/" + messageId);
                                                        Log.d(TAG, "   → status=read, readBy." + uidB + "=timestamp");
                                                    }
                                                    @Override
                                                    public void onError(String err) {
                                                        Log.e(TAG, "❌ markAsRead thất bại: " + err);
                                                    }
                                                });
                                    }
                                    @Override
                                    public void onError(String err) {
                                        Log.e(TAG, "❌ markDelivered thất bại: " + err);
                                    }
                                });
                    }
                    @Override
                    public void onError(String err) {
                        Log.e(TAG, "❌ Gửi tin thất bại: " + err);
                    }
                });
    }
    // =====================================================================
    // END TEST CODE

}
