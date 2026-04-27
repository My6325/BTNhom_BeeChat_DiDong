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

        testSendMessage();
    }

    // =====================================================================
    // TEST CODE — Task 2.2: Gửi tin nhắn text + cập nhật lastMessage
    // Xóa sau khi verify trên Firebase Console
    // =====================================================================

    private static final String TAG = "BeeChat_Test";
    private static final String USER_A_EMAIL = "test.conv.a@beechat.com";
    private static final String USER_B_EMAIL = "test.conv.b@beechat.com";
    private static final String TEST_PASS = "ConvPass@1234";
    private static final String TEST_MESSAGE = "Xin chào từ BeeChat Task 2.2!";

    private void testSendMessage() {
        FirebaseAuthRepository authRepo = new FirebaseAuthRepository();
        ConversationRepository convRepo = new ConversationRepository();
        MessageRepository msgRepo = new MessageRepository();

        // Bước 1: xác thực User A (login hoặc register)
        authRepo.login(USER_A_EMAIL, TEST_PASS, new FirebaseAuthRepository.OnAuthCallback() {
            @Override
            public void onSuccess() {
                String uidA = authRepo.getCurrentUserId();
                Log.d(TAG, "✅ Login User A OK, uid=" + uidA);
                authRepo.logout();
                getUidBThenSend(authRepo, convRepo, msgRepo, uidA);
            }
            @Override
            public void onError(String msg) {
                Log.d(TAG, "ℹ️ Login User A thất bại, thử đăng ký: " + msg);
                authRepo.register(USER_A_EMAIL, TEST_PASS, "User A Test", new FirebaseAuthRepository.OnAuthCallback() {
                    @Override
                    public void onSuccess() {
                        String uidA = authRepo.getCurrentUserId();
                        Log.d(TAG, "✅ Đăng ký User A OK, uid=" + uidA);
                        authRepo.logout();
                        getUidBThenSend(authRepo, convRepo, msgRepo, uidA);
                    }
                    @Override
                    public void onError(String err) {
                        Log.e(TAG, "❌ Không thể xác thực User A: " + err);
                    }
                });
            }
        });
    }

    private void getUidBThenSend(FirebaseAuthRepository authRepo, ConversationRepository convRepo,
                                  MessageRepository msgRepo, String uidA) {
        authRepo.login(USER_B_EMAIL, TEST_PASS, new FirebaseAuthRepository.OnAuthCallback() {
            @Override
            public void onSuccess() {
                String uidB = authRepo.getCurrentUserId();
                Log.d(TAG, "✅ Login User B OK, uid=" + uidB);
                doSendMessage(authRepo, convRepo, msgRepo, uidA, uidB);
            }
            @Override
            public void onError(String msg) {
                Log.d(TAG, "ℹ️ Login User B thất bại, thử đăng ký: " + msg);
                authRepo.register(USER_B_EMAIL, TEST_PASS, "User B Test", new FirebaseAuthRepository.OnAuthCallback() {
                    @Override
                    public void onSuccess() {
                        String uidB = authRepo.getCurrentUserId();
                        Log.d(TAG, "✅ Đăng ký User B OK, uid=" + uidB);
                        doSendMessage(authRepo, convRepo, msgRepo, uidA, uidB);
                    }
                    @Override
                    public void onError(String err) {
                        Log.e(TAG, "❌ Không thể xác thực User B: " + err);
                    }
                });
            }
        });
    }

    private void doSendMessage(FirebaseAuthRepository authRepo, ConversationRepository convRepo,
                                MessageRepository msgRepo, String uidA, String uidB) {
        // Bước 2: tạo hoặc lấy conversation giữa A và B
        convRepo.createOrGetPrivateConversation(uidA, uidB, new ConversationRepository.OnConversationCallback() {
            @Override
            public void onSuccess(String conversationId) {
                Log.d(TAG, "✅ Conversation OK: " + conversationId);
                // Bước 3: gửi tin nhắn từ User A (cần login lại để có currentUser)
                authRepo.login(USER_A_EMAIL, TEST_PASS, new FirebaseAuthRepository.OnAuthCallback() {
                    @Override
                    public void onSuccess() {
                        msgRepo.sendMessage(conversationId, uidA, "User A Test", TEST_MESSAGE,
                                new MessageRepository.OnSendMessageCallback() {
                                    @Override
                                    public void onSuccess(String messageId) {
                                        Log.d(TAG, "✅ Gửi tin nhắn thành công! messageId=" + messageId);
                                        Log.d(TAG, "→ Kiểm tra Firestore: conversations/" + conversationId + "/messages/" + messageId);
                                    }
                                    @Override
                                    public void onError(String err) {
                                        Log.e(TAG, "❌ Gửi tin nhắn thất bại: " + err);
                                    }
                                });
                    }
                    @Override
                    public void onError(String err) {
                        Log.e(TAG, "❌ Re-login User A thất bại: " + err);
                    }
                });
            }
            @Override
            public void onError(String err) {
                Log.e(TAG, "❌ Tạo conversation thất bại: " + err);
            }
        });
    }
    // =====================================================================
    // END TEST CODE

}
