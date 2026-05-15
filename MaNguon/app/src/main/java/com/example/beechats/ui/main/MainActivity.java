package com.example.beechats.ui.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.beechats.R;
import com.example.beechats.data.models.User;
import com.example.beechats.data.repositories.FirebaseAuthRepository;
import com.example.beechats.data.repositories.UserRepository;
import com.example.beechats.utils.ThemeHelper;
import com.example.beechats.ui.auth.LoginActivity;
import com.example.beechats.ui.chat.ChatListFragment;
import com.example.beechats.ui.friend.FriendsFragment;
import com.example.beechats.ui.onboarding.WelcomeActivity;
import com.example.beechats.ui.onboarding.ScanQrActivity;
import com.example.beechats.ui.setting.SettingsFragment;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.view.View;


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
    private LinearLayout headerContainer;

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
        headerContainer = findViewById(R.id.header_container);
        ImageView imgScanQr = findViewById(R.id.img_scan_qr);

        imgScanQr.setOnClickListener(v ->
                startActivity(new Intent(this, ScanQrActivity.class)));

        int pendingNav = ThemeHelper.consumePendingBottomNavItem(this);
        int startMenuId = pendingNav != 0 ? pendingNav : R.id.menu_chat;

        Fragment startFragment;
        if (startMenuId == R.id.menu_settings) {
            startFragment = new SettingsFragment();
            headerContainer.setVisibility(View.GONE);
        } else if (startMenuId == R.id.menu_friends) {
            startFragment = new FriendsFragment();
            headerContainer.setVisibility(View.VISIBLE);
        } else {
            startFragment = new ChatListFragment();
            headerContainer.setVisibility(View.VISIBLE);
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, startFragment)
                .commit();

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.menu_chat) {
                selectedFragment = new ChatListFragment();
                headerContainer.setVisibility(View.VISIBLE);
            } else if (itemId == R.id.menu_friends) {
                selectedFragment = new FriendsFragment();
                headerContainer.setVisibility(View.VISIBLE);
            } else if (itemId == R.id.menu_settings) {
                selectedFragment = new SettingsFragment();
                headerContainer.setVisibility(View.GONE);
            }
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        bottomNavigationView.post(() -> bottomNavigationView.setSelectedItemId(startMenuId));

        syncDarkModeFromFirestore();
    }

    /** Đồng bộ chế độ tối từ Firestore (settings.darkMode) với máy khi vào màn chính. */
    private void syncDarkModeFromFirestore() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) {
            return;
        }
        new UserRepository().getUser(u.getUid(), new UserRepository.OnUserCallback() {
            @Override
            public void onSuccess(User user) {
                boolean dark = user.getSettings() != null && user.getSettings().isDarkMode();
                if (ThemeHelper.getStoredDarkMode(MainActivity.this) != dark) {
                    ThemeHelper.setDarkModeEnabled(MainActivity.this, dark);
                }
            }

            @Override
            public void onError(String errorMessage) {
                // Giữ theo prefs local
            }
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

