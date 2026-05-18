package com.example.beechats.ui.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.beechats.R;
import com.example.beechats.data.models.User;
import com.example.beechats.data.repositories.UserRepository;
import com.example.beechats.ui.chat.ChatListFragment;
import com.example.beechats.ui.friend.FriendsFragment;
import com.example.beechats.ui.onboarding.ScanQrActivity;
import com.example.beechats.ui.onboarding.WelcomeActivity;
import com.example.beechats.ui.setting.SettingsFragment;
import com.example.beechats.utils.ThemeHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

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
        }

        if (currentUser == null) {
            startActivity(new Intent(this, com.example.beechats.ui.auth.LoginActivity.class));
            finish();
            return;
        }

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
}

