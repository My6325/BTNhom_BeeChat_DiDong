package com.example.beechats.ui.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.beechats.R;
import com.example.beechats.ui.auth.LoginActivity;
import com.example.beechats.ui.chat.ChatListFragment;
import com.example.beechats.ui.friend.FriendsFragment;
import com.example.beechats.ui.onboarding.WelcomeActivity;
import com.example.beechats.ui.onboarding.QRCode_Activity;
import com.example.beechats.ui.setting.SettingsFragment;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.view.View;


public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private LinearLayout headerContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Lấy SharedPreferences để kiểm tra lần đầu mở app
        SharedPreferences prefs = getSharedPreferences("BeeChatsPrefs", MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean("isFirstLaunch", true);

        // Kiểm tra trạng thái đăng nhập từ Firebase
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (isFirstLaunch) {
            // *** Người dùng mới tải app lần đầu ***
            // Đánh dấu đã mở app rồi (lần sau sẽ không hiện Welcome nữa)
            prefs.edit().putBoolean("isFirstLaunch", false).apply();

            // Chuyển sang màn hình Welcome → Introduce → ...
            Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
            startActivity(intent);
            finish();
            return; // Dừng lại, không cần load giao diện MainActivity

        } else if (currentUser == null) {
            // *** Đã từng mở app nhưng chưa đăng nhập (hoặc đã đăng xuất) ***
            // Chuyển sang màn hình Đăng nhập
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return; // Dừng lại, không cần load giao diện MainActivity
        }

        // *** Đã đăng nhập → Hiển thị màn hình tin nhắn ***
        setContentView(R.layout.activity_main);

        // Ánh xạ View từ layout activity_main.xml
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        headerContainer = findViewById(R.id.header_container);
        ImageView imgScanQr = findViewById(R.id.img_scan_qr);

        imgScanQr.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, QRCode_Activity.class);
            startActivity(intent);
        });

        // Thiết lập màn hình mặc định khi vừa mở App (Tab Tin nhắn)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ChatListFragment())
                    .commit();
        }

        //Xử lý sự kiện khi bấm vào các Tab dưới đáy
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
    }
}
