package com.example.beechats.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

        // TEST TASK 1.7 — xóa sau khi verify trên Firebase Console
        testChangePassword();
    }

    /**
     * Test flow đổi mật khẩu:
     * 1. Đăng ký user test (nếu chưa có)
     * 2. Login → đổi password sang password mới
     * 3. Logout → login lại bằng password mới
     * Verify trên Firebase Console: Authentication user vẫn tồn tại, Firestore isOnline=true.
     */
    private void testChangePassword() {
        FirebaseAuthRepository authRepo = new FirebaseAuthRepository();
        String testEmail    = "test.change.1_7@beechat.com";
        String oldPassword  = "OldPass@1234";
        String newPassword  = "NewPass@5678";
        String testName     = "BeeChat Test 1.7";

        // Bước 1: thử login bằng oldPassword (nếu user đã tồn tại từ lần chạy trước)
        authRepo.login(testEmail, oldPassword, new FirebaseAuthRepository.OnAuthCallback() {
            @Override
            public void onSuccess() {
                Log.d("BeeChat_Test", "Login (oldPass) OK → tiến hành đổi mật khẩu...");
                doChangePassword(authRepo, oldPassword, newPassword, testEmail);
            }

            @Override
            public void onError(String msg) {
                Log.w("BeeChat_Test", "Login (oldPass) thất bại: " + msg + " → thử login bằng newPass...");
                // Có thể đã đổi password từ lần chạy trước → thử newPassword
                authRepo.login(testEmail, newPassword, new FirebaseAuthRepository.OnAuthCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d("BeeChat_Test", "✅ Login (newPass) OK — password đã được đổi từ lần chạy trước. Test thành công!");
                    }

                    @Override
                    public void onError(String msg2) {
                        Log.w("BeeChat_Test", "Login (newPass) thất bại: " + msg2 + " → đăng ký user mới...");
                        // User chưa tồn tại → đăng ký
                        authRepo.register(testEmail, oldPassword, testName,
                                new FirebaseAuthRepository.OnAuthCallback() {
                                    @Override
                                    public void onSuccess() {
                                        Log.d("BeeChat_Test", "✅ Đăng ký OK → logout → login → đổi mật khẩu...");
                                        authRepo.logout();
                                        authRepo.login(testEmail, oldPassword,
                                                new FirebaseAuthRepository.OnAuthCallback() {
                                                    @Override
                                                    public void onSuccess() {
                                                        doChangePassword(authRepo, oldPassword, newPassword, testEmail);
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
        });
    }

    /** Đổi mật khẩu → logout → login lại bằng password mới. */
    private void doChangePassword(FirebaseAuthRepository authRepo,
                                  String currentPass, String newPass, String email) {
        authRepo.changePassword(currentPass, newPass, new FirebaseAuthRepository.OnAuthCallback() {
            @Override
            public void onSuccess() {
                Log.d("BeeChat_Test", "✅ Đổi mật khẩu thành công → logout → login lại bằng pass mới...");
                authRepo.logout();
                authRepo.login(email, newPass, new FirebaseAuthRepository.OnAuthCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d("BeeChat_Test", "✅ Login bằng password mới thành công → kiểm tra isOnline=true trên Firebase Console");
                    }
                    @Override
                    public void onError(String e) {
                        Log.e("BeeChat_Test", "❌ Login bằng password mới thất bại: " + e);
                    }
                });
            }
            @Override
            public void onError(String e) {
                Log.e("BeeChat_Test", "❌ Đổi mật khẩu thất bại: " + e);
            }
        });
    }
}
