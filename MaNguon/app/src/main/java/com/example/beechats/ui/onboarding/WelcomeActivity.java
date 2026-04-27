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


        testCreateConversation();
    }

    // =====================================================================
    // TEST CODE — Task 2.1: Tạo hội thoại 1-1
    // Xóa sau khi verify trên Firebase Console
    // =====================================================================

    private static final String TAG = "BeeChat_Test";
    private static final String USER_A_EMAIL = "test.conv.a@beechat.com";
    private static final String USER_B_EMAIL = "test.conv.b@beechat.com";
    private static final String TEST_PASS = "ConvPass@1234";

    private void testCreateConversation() {
        FirebaseAuthRepository authRepo = new FirebaseAuthRepository();
        ConversationRepository convRepo = new ConversationRepository();

        // Thử login User A trước — nếu thất bại mới đăng ký
        authRepo.login(USER_A_EMAIL, TEST_PASS, new FirebaseAuthRepository.OnAuthCallback() {
            @Override
            public void onSuccess() {
                String uidA = authRepo.getCurrentUserId();
                Log.d(TAG, "✅ Login User A OK, uid=" + uidA);
                authRepo.logout();
                getUidBAndCreateConversation(authRepo, convRepo, uidA);
            }
            @Override
            public void onError(String msg) {
                Log.d(TAG, "ℹ️ Login User A thất bại, thử đăng ký: " + msg);
                registerUserA(authRepo, convRepo);
            }
        });
    }

    private void registerUserA(FirebaseAuthRepository authRepo, ConversationRepository convRepo) {
        authRepo.register(USER_A_EMAIL, TEST_PASS, "User A Test", new FirebaseAuthRepository.OnAuthCallback() {
            @Override
            public void onSuccess() {
                String uidA = authRepo.getCurrentUserId();
                Log.d(TAG, "✅ Đăng ký User A OK, uid=" + uidA);
                authRepo.logout();
                getUidBAndCreateConversation(authRepo, convRepo, uidA);
            }
            @Override
            public void onError(String msg) {
                Log.e(TAG, "❌ Không thể xác thực User A: " + msg);
            }
        });
    }

    private void getUidBAndCreateConversation(FirebaseAuthRepository authRepo, ConversationRepository convRepo, String uidA) {
        // Thử login User B trước — nếu thất bại mới đăng ký
        authRepo.login(USER_B_EMAIL, TEST_PASS, new FirebaseAuthRepository.OnAuthCallback() {
            @Override
            public void onSuccess() {
                String uidB = authRepo.getCurrentUserId();
                Log.d(TAG, "✅ Login User B OK, uid=" + uidB);
                doCreateConversation(convRepo, uidA, uidB);
            }
            @Override
            public void onError(String msg) {
                Log.d(TAG, "ℹ️ Login User B thất bại, thử đăng ký: " + msg);
                registerUserB(authRepo, convRepo, uidA);
            }
        });
    }

    private void registerUserB(FirebaseAuthRepository authRepo, ConversationRepository convRepo, String uidA) {
        authRepo.register(USER_B_EMAIL, TEST_PASS, "User B Test", new FirebaseAuthRepository.OnAuthCallback() {
            @Override
            public void onSuccess() {
                String uidB = authRepo.getCurrentUserId();
                Log.d(TAG, "✅ Đăng ký User B OK, uid=" + uidB);
                doCreateConversation(convRepo, uidA, uidB);
            }
            @Override
            public void onError(String msg) {
                Log.e(TAG, "❌ Không thể xác thực User B: " + msg);
            }
        });
    }

    private void doCreateConversation(ConversationRepository convRepo, String uidA, String uidB) {
        convRepo.createOrGetPrivateConversation(uidA, uidB, new ConversationRepository.OnConversationCallback() {
            @Override
            public void onSuccess(String conversationId) {
                Log.d(TAG, "✅ Tạo hội thoại thành công! conversationId=" + conversationId);
                Log.d(TAG, "→ Kiểm tra Firestore Console: conversations/" + conversationId);
            }
            @Override
            public void onError(String msg) {
                Log.e(TAG, "❌ Tạo hội thoại thất bại: " + msg);
            }
        });
    }
    // =====================================================================
    // END TEST CODE

}
