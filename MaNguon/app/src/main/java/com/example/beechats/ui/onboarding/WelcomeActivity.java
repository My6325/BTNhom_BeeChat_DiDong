package com.example.beechats.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.beechats.R;
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

        // TEST TASK 1.8 — xóa sau khi verify trên Firebase Console
        testDeleteAccount();
    }

    /**
     * Test flow xóa tài khoản:
     * 1. Đăng ký user test (nếu chưa có)
     * 2. Login → xóa tài khoản bằng deleteAccount(password, callback)
     * Verify trên Firebase Console:
     *   - Authentication: test.delete.1_8@beechat.com KHÔNG còn tồn tại
     *   - Firestore > users: document {uid} KHÔNG còn tồn tại
     */
    private void testDeleteAccount() {
        FirebaseAuthRepository authRepo = new FirebaseAuthRepository();
        String testEmail    = "test.delete.1_8@beechat.com";
        String testPassword = "DeletePass@1234";
        String testName     = "BeeChat Test 1.8";

        // Thử login trước — nếu user đã tồn tại từ lần chạy trước thì xóa luôn
        authRepo.login(testEmail, testPassword, new FirebaseAuthRepository.OnAuthCallback() {
            @Override
            public void onSuccess() {
                Log.d("BeeChat_Test", "Login OK → tiến hành xóa tài khoản...");
                doDeleteAccount(authRepo, testPassword);
            }

            @Override
            public void onError(String msg) {
                Log.w("BeeChat_Test", "Login thất bại: " + msg + " → đăng ký user mới...");
                // User chưa tồn tại → đăng ký rồi xóa
                authRepo.register(testEmail, testPassword, testName,
                        new FirebaseAuthRepository.OnAuthCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d("BeeChat_Test", "✅ Đăng ký OK → logout → login → xóa tài khoản...");
                                authRepo.logout();
                                authRepo.login(testEmail, testPassword,
                                        new FirebaseAuthRepository.OnAuthCallback() {
                                            @Override
                                            public void onSuccess() {
                                                doDeleteAccount(authRepo, testPassword);
                                            }
                                            @Override
                                            public void onError(String e) {
                                                Log.e("BeeChat_Test", "❌ Login sau đăng ký thất bại: " + e);
                                            }
                                        });
                            }
                            @Override
                            public void onError(String e) {
                                Log.e("BeeChat_Test", "❌ Đăng ký thất bại: " + e);
                            }
                        });
            }
        });
    }

    /** Xóa tài khoản và log kết quả. */
    private void doDeleteAccount(FirebaseAuthRepository authRepo, String password) {
        authRepo.deleteAccount(password, new FirebaseAuthRepository.OnAuthCallback() {
            @Override
            public void onSuccess() {
                Log.d("BeeChat_Test", "✅ Xóa tài khoản thành công → kiểm tra Firebase Console: user không còn trong Auth và Firestore");
            }
            @Override
            public void onError(String e) {
                Log.e("BeeChat_Test", "❌ Xóa tài khoản thất bại: " + e);
            }
        });
    }
}
