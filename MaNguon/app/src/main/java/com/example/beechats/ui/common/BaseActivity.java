package com.example.beechats.ui.common;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity cơ sở cho tất cả Activity trong ứng dụng.
 * Các Activity khác kế thừa class này để dùng chung logic.
 */
public abstract class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
