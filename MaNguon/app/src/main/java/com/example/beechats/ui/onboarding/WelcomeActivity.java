package com.example.beechats.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
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

        // TEST TASK 1.6 — xóa sau khi verify trên Firebase Console
        testLogin();
    }

    /** Thử login; nếu user chưa tồn tại thì đăng ký trước rồi login lại. */
    private void testLogin() {
        FirebaseAuthRepository authRepo = new FirebaseAuthRepository();
        String testEmail = "test.login.1_6@beechat.com";
        String testPassword = "Test@123456";
        String testName   = "BeeChat Test 1.6";

        authRepo.login(testEmail, testPassword, new FirebaseAuthRepository.OnAuthCallback() {
            @Override
            public void onSuccess() {
                Log.d("BeeChat_Test", "✅ Login thành công → kiểm tra isOnline=true trên Firebase Console");
            }

            @Override
            public void onError(String errorMessage) {
                Log.w("BeeChat_Test", "Login lần 1: " + errorMessage + " → thử đăng ký...");

                // User chưa tồn tại → đăng ký rồi login lại
                authRepo.register(testEmail, testPassword, testName,
                        new FirebaseAuthRepository.OnAuthCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d("BeeChat_Test", "✅ Đăng ký OK → logout → login lại để test isOnline...");
                                authRepo.logout();
                                authRepo.login(testEmail, testPassword,
                                        new FirebaseAuthRepository.OnAuthCallback() {
                                            @Override
                                            public void onSuccess() {
                                                Log.d("BeeChat_Test", "✅ Login thành công → kiểm tra isOnline=true trên Firebase Console");
                                            }
                                            @Override
                                            public void onError(String msg) {
                                                Log.e("BeeChat_Test", "❌ Login sau đăng ký thất bại: " + msg);
                                            }
                                        });
                            }

                            @Override
                            public void onError(String msg) {
                                Log.e("BeeChat_Test", "❌ Đăng ký thất bại: " + msg);
                            }
                        });
            }
        });
    }
}
