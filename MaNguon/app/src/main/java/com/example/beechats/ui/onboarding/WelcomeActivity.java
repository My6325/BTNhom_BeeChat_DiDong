package com.example.beechats.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.beechats.R;
import com.example.beechats.data.repositories.FirebaseAuthRepository;
import com.example.beechats.data.repositories.UserRepository;

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

        // TEST TASK 1.9 — xóa sau khi verify trên Firebase Console
        testUpdateProfile();
    }

    /**
     * Test flow cập nhật profile:
     * 1. Đăng ký user test (nếu chưa có)
     * 2. Login → lấy uid → gọi updateProfile(uid, displayName, bio, callback)
     * Verify trên Firebase Console > Firestore > users/{uid}:
     *   - displayName: "BeeChat User 1.9"
     *   - bio: "Bio test 1.9"
     *   - searchKeywords: chứa prefix của "beechat"
     */
    private void testUpdateProfile() {
        FirebaseAuthRepository authRepo = new FirebaseAuthRepository();
        UserRepository userRepo = new UserRepository();
        String testEmail    = "test.profile.1_9@beechat.com";
        String testPassword = "ProfilePass@1234";
        String testName     = "BeeChat Test 1.9";

        // Thử login trước — nếu user đã tồn tại thì update profile luôn
        authRepo.login(testEmail, testPassword, new FirebaseAuthRepository.OnAuthCallback() {
            @Override
            public void onSuccess() {
                String uid = authRepo.getCurrentUserId();
                Log.d("BeeChat_Test", "Login OK (uid=" + uid + ") → tiến hành update profile...");
                doUpdateProfile(userRepo, uid);
            }

            @Override
            public void onError(String msg) {
                Log.w("BeeChat_Test", "Login thất bại: " + msg + " → đăng ký user mới...");
                authRepo.register(testEmail, testPassword, testName,
                        new FirebaseAuthRepository.OnAuthCallback() {
                            @Override
                            public void onSuccess() {
                                String uid = authRepo.getCurrentUserId();
                                Log.d("BeeChat_Test", "✅ Đăng ký OK (uid=" + uid + ") → update profile...");
                                doUpdateProfile(userRepo, uid);
                            }
                            @Override
                            public void onError(String e) {
                                Log.e("BeeChat_Test", "❌ Đăng ký thất bại: " + e);
                            }
                        });
            }
        });
    }

    /** Cập nhật profile và log kết quả. */
    private void doUpdateProfile(UserRepository userRepo, String uid) {
        userRepo.updateProfile(uid, "BeeChat User 1.9", "Bio test 1.9",
                new UserRepository.OnCompleteCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d("BeeChat_Test", "✅ Cập nhật profile thành công → kiểm tra Firebase Console: Firestore > users/" + uid);
                    }
                    @Override
                    public void onError(String e) {
                        Log.e("BeeChat_Test", "❌ Cập nhật profile thất bại: " + e);
                    }
                });
    }
}
