package com.example.beechats.ui.onboarding;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.beechats.R;
import com.example.beechats.data.local.SavedAccountManager;
import com.example.beechats.data.repositories.FirebaseAuthRepository;
import com.example.beechats.ui.main.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Register_Activity extends AppCompatActivity {

    private ImageView btnBack;
    private ImageView btnSeePassword;
    private ImageView btnSeePasswordConfirm;
    private EditText edtEmail;
    private EditText edtUsername;
    private EditText edtPassword;
    private EditText edtConfirmPassword;
    private CheckBox cbAgree;
    private Button btnCreateAccount;

    private FirebaseAuthRepository authRepository;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);
        initViews();
        initRepositories();
        setupEvents();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnSeePassword = findViewById(R.id.btnSeePassword);
        btnSeePasswordConfirm = findViewById(R.id.btnSeePassword_confirm);
        edtEmail = findViewById(R.id.edtEmailRegister);
        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        cbAgree = findViewById(R.id.cbAgree);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang tạo tài khoản...");
        progressDialog.setCancelable(false);
    }

    private void initRepositories() {
        authRepository = new FirebaseAuthRepository();
    }

    private void setupEvents() {
        btnBack.setOnClickListener(v -> {
            onBackPressed();
            overridePendingTransition(0, 0);
        });

        btnCreateAccount.setOnClickListener(v -> attemptRegister());

        setupPasswordToggle(btnSeePassword, edtPassword);
        setupPasswordToggle(btnSeePasswordConfirm, edtConfirmPassword);
    }

    private void setupPasswordToggle(ImageView toggleBtn, EditText editText) {
        toggleBtn.setOnClickListener(v -> {
            boolean visible = (editText.getInputType()
                    & InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) != 0;
            if (visible) {
                editText.setInputType(InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                toggleBtn.setImageResource(R.drawable.eye_close);
            } else {
                editText.setInputType(InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                toggleBtn.setImageResource(R.drawable.eye);
            }
            editText.setSelection(editText.getText().length());
        });
    }

    private void attemptRegister() {
        String email = edtEmail.getText().toString().trim();
        String username = edtUsername.getText().toString().trim();
        String password = edtPassword.getText().toString();
        String confirmPassword = edtConfirmPassword.getText().toString();

        if (!validateInput(email, username, password, confirmPassword)) {
            return;
        }

        showLoading(true);

        authRepository.register(email, password, username, new FirebaseAuthRepository.OnAuthCallback() {
            @Override
            public void onSuccess() {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    SavedAccountManager.saveAccount(Register_Activity.this,
                            user.getUid(), username, email, null);
                }
                showLoading(false);
                showToast("Đăng ký thành công!");
                navigateToMain();
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                showToast(errorMessage);
            }
        });
    }

    private boolean validateInput(String email, String username, String password, String confirmPassword) {
        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Email không được để trống");
            edtEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Email không hợp lệ");
            edtEmail.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(username)) {
            edtUsername.setError("Tên đăng nhập không được để trống");
            edtUsername.requestFocus();
            return false;
        }

        if (username.length() < 2) {
            edtUsername.setError("Tên đăng nhập phải có ít nhất 2 ký tự");
            edtUsername.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Mật khẩu không được để trống");
            edtPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            edtPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            edtPassword.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            edtConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            edtConfirmPassword.requestFocus();
            return false;
        }

        if (!cbAgree.isChecked()) {
            showToast("Vui lòng đồng ý với điều khoản sử dụng");
            return false;
        }

        return true;
    }

    private void showLoading(boolean show) {
        if (show) {
            progressDialog.show();
        } else {
            progressDialog.dismiss();
        }
        btnCreateAccount.setEnabled(!show);
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }
}
