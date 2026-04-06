package com.example.beechats.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.beechats.R;
import com.example.beechats.ui.onboarding.ForgetPassword;
import com.example.beechats.ui.onboarding.Register_Activity;

public class LoginActivity extends AppCompatActivity {
    private Button btnRegister;
    private TextView txtForgetPass;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        initViews();
        event();
    }
    private void initViews()
    {
        btnRegister=findViewById(R.id.btnRegister);
        txtForgetPass=findViewById(R.id.txtForgetPassword);
    }
    private void event()
    {
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(LoginActivity.this, Register_Activity.class);
                // Flag này giúp loại bỏ hiệu ứng chuyển tiếp giữa các Activity
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                //Gọi để không có hiệu ứng mặc định
                overridePendingTransition(0, 0);
            }
        });
        txtForgetPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(LoginActivity.this, ForgetPassword.class);
                // Flag này giúp loại bỏ hiệu ứng chuyển tiếp giữa các Activity
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                //Gọi để không có hiệu ứng mặc định
                overridePendingTransition(0, 0);
            }
        });
    }
}
