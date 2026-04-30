package com.example.beechats.ui.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.beechats.R;
import com.example.beechats.data.repositories.FirebaseAuthRepository;
import com.example.beechats.ui.auth.LoginActivity;
import com.example.beechats.ui.chat.ChatListFragment;
import com.example.beechats.ui.friend.FriendsFragment;
import com.example.beechats.ui.onboarding.WelcomeActivity;
import com.example.beechats.ui.onboarding.QRCode_Activity;
import android.widget.ImageView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    // =====================================================================
    // TEST CODE — Task 2.6: Online/Offline Status
    // Xóa sau khi verify trên Firebase Console
    // =====================================================================
    private static final String TAG = "BeeChat_Test";
    private static final String USER_A_EMAIL = "test.conv.a@beechat.com";
    private static final String TEST_PASS = "ConvPass@1234";
    // =====================================================================

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("BeeChatsPrefs", MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean("isFirstLaunch", true);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (isFirstLaunch) {
            prefs.edit().putBoolean("isFirstLaunch", false).apply();
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;

        } else if (currentUser == null) {
            // TEST CODE: auto-login User A để AppLifecycleObserver hoạt động
            testAutoLoginForOnlineStatus();
            return;
        }

        // Đã đăng nhập
        Log.d(TAG, "✅ Đã đăng nhập: users/" + currentUser.getUid());
        Log.d(TAG, "   → isOnline=true ghi bởi AppLifecycleObserver.onStart");
        Log.d(TAG, "   → Nhấn Home → isOnline=false, quay lại → isOnline=true");
        initMainUI();
    }

    /** Khởi tạo giao diện chính (bottom navigation + fragments). */
    private void initMainUI() {
        setContentView(R.layout.activity_main);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        ImageView imgScanQr = findViewById(R.id.img_scan_qr);

        imgScanQr.setOnClickListener(v ->
                startActivity(new Intent(this, QRCode_Activity.class)));

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new ChatListFragment())
                .commit();

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.menu_chat) {
                selectedFragment = new ChatListFragment();
            } else if (itemId == R.id.menu_friends) {
                selectedFragment = new FriendsFragment();
            }
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });
    }

    // =====================================================================
    // TEST CODE — auto-login User A, sau đó load UI trực tiếp (không restart Activity)
    // =====================================================================
    private void testAutoLoginForOnlineStatus() {
        Log.d(TAG, "🔄 Chưa đăng nhập — auto-login " + USER_A_EMAIL + " ...");
        new FirebaseAuthRepository().login(USER_A_EMAIL, TEST_PASS,
                new FirebaseAuthRepository.OnAuthCallback() {
                    @Override
                    public void onSuccess() {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        String uid = (user != null) ? user.getUid() : "null";
                        Log.d(TAG, "✅ Auto-login OK, uid=" + uid);
                        Log.d(TAG, "   → AppLifecycleObserver đang theo dõi foreground/background");
                        Log.d(TAG, "   → Kiểm tra Firebase Console: users/" + uid + ".isOnline=true");
                        Log.d(TAG, "   → Nhấn Home → isOnline=false, quay lại → isOnline=true");
                        // Load UI trực tiếp trong Activity hiện tại
                        initMainUI();
                    }

                    @Override
                    public void onError(String msg) {
                        Log.e(TAG, "❌ Auto-login thất bại: " + msg);
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        finish();
                    }
                });
    }
    // =====================================================================
    // END TEST CODE
}

