package com.example.beechats.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.beechats.R;
import com.example.beechats.data.local.SavedAccountManager;
import com.example.beechats.data.models.User;
import com.example.beechats.data.repositories.FirebaseAuthRepository;
import com.example.beechats.data.repositories.UserRepository;
import com.example.beechats.ui.main.MainActivity;
import com.example.beechats.ui.onboarding.ForgetPassword;
import com.example.beechats.ui.onboarding.Register_Activity;
import android.text.InputType;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {
    private Button btnRegister, btnLogin;
    private TextView txtForgetPass;
    private EditText edtEmail, edtPassWord;
    private ImageView btnTogglePassword;
    private FirebaseAuthRepository authRepository;
    private boolean isPasswordVisible = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        
        authRepository = new FirebaseAuthRepository();
        
        initViews();
        event();
    }
    private void initViews()
    {
        btnRegister=findViewById(R.id.btnRegister);
        txtForgetPass=findViewById(R.id.txtForgetPassword);
        btnLogin=findViewById(R.id.btnLogin);
        edtEmail=findViewById(R.id.edtEmail);
        edtPassWord=findViewById(R.id.edtPassWord);
        btnTogglePassword=findViewById(R.id.btnSeePassword);
    }
    private void event()
    {
        btnTogglePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPasswordVisible = !isPasswordVisible;
                if (isPasswordVisible) {
                    // Show password
                    edtPassWord.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    btnTogglePassword.setImageResource(R.drawable.eye);
                } else {
                    // Hide password
                    edtPassWord.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    btnTogglePassword.setImageResource(R.drawable.eye_close);
                }
                // Đưa con trỏ nhấp nháy về cuối chữ
                edtPassWord.setSelection(edtPassWord.getText().length());
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = edtEmail.getText().toString().trim();
                String password = edtPassWord.getText().toString().trim();
                
                authRepository.login(email, password, new FirebaseAuthRepository.OnAuthCallback() {
                    @Override
                    public void onSuccess() {
                        saveCurrentAccount();
                        ((com.example.beechats.BeeChatsApp) getApplication()).initZegoIfUserLoggedIn();
                        Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(LoginActivity.this, "Lỗi: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

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

    private void saveCurrentAccount() {
        com.google.firebase.auth.FirebaseUser user =
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();

        new UserRepository().getUser(uid, new UserRepository.OnUserCallback() {
            @Override
            public void onSuccess(User u) {
                SavedAccountManager.saveAccount(LoginActivity.this,
                        uid,
                        u != null ? u.getDisplayName() : user.getDisplayName(),
                        u != null ? u.getEmail() : user.getEmail(),
                        u != null ? u.getPhotoUrl() : null);
            }

            @Override
            public void onError(String errorMessage) {
                SavedAccountManager.saveAccount(LoginActivity.this,
                        uid,
                        user.getDisplayName(),
                        user.getEmail(),
                        null);
            }
        });
    }
}
