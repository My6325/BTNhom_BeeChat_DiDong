package com.example.beechats.ui.onboarding;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.beechats.R;

public class QRCode_Activity extends AppCompatActivity {
    private ImageView btnBack;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_qrcode);
        initViews();
        event();
    }
    private void initViews()
    {
        btnBack=findViewById(R.id.btnBack);
    }
    private void event()
    {
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
                //Xóa hiệu ứng mặc định
                overridePendingTransition(0, 0);
            }
        });
    }
}
