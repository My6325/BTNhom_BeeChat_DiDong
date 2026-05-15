package com.example.beechats.ui.setting;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.beechats.R;
import com.example.beechats.data.repositories.FirebaseAuthRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private ImageView btnBack;
    private EditText edtCurrentPassword;
    private EditText edtNewPassword;
    private EditText edtConfirmPassword;
    private Button btnSavePassword;

    private FirebaseAuthRepository authRepository;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.change_password);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.change_password_login_required, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (TextUtils.isEmpty(user.getEmail())) {
            Toast.makeText(this, R.string.change_password_no_email, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        authRepository = new FirebaseAuthRepository();
        initViews();
        bindEvents();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        edtCurrentPassword = findViewById(R.id.edtCurrentPassword);
        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnSavePassword = findViewById(R.id.btnSavePassword);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.change_password_loading));
        progressDialog.setCancelable(false);
    }

    private void bindEvents() {
        btnBack.setOnClickListener(v -> {
            onBackPressed();
            overridePendingTransition(0, 0);
        });

        btnSavePassword.setOnClickListener(v -> attemptChangePassword());
    }

    private void attemptChangePassword() {
        String current = edtCurrentPassword.getText().toString();
        String newPass = edtNewPassword.getText().toString();
        String confirm = edtConfirmPassword.getText().toString();

        if (TextUtils.isEmpty(current)) {
            edtCurrentPassword.setError(getString(R.string.current_password_required));
            edtCurrentPassword.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(newPass)) {
            edtNewPassword.setError(getString(R.string.new_password_required));
            edtNewPassword.requestFocus();
            return;
        }
        if (newPass.length() < 6) {
            edtNewPassword.setError(getString(R.string.new_password_too_short));
            edtNewPassword.requestFocus();
            return;
        }
        if (!newPass.equals(confirm)) {
            edtConfirmPassword.setError(getString(R.string.password_mismatch));
            edtConfirmPassword.requestFocus();
            return;
        }
        if (current.equals(newPass)) {
            edtNewPassword.setError(getString(R.string.new_password_same_as_old));
            edtNewPassword.requestFocus();
            return;
        }

        progressDialog.show();
        btnSavePassword.setEnabled(false);

        authRepository.changePassword(current, newPass, new FirebaseAuthRepository.OnAuthCallback() {
            @Override
            public void onSuccess() {
                progressDialog.dismiss();
                btnSavePassword.setEnabled(true);
                Toast.makeText(ChangePasswordActivity.this, R.string.password_change_success, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                progressDialog.dismiss();
                btnSavePassword.setEnabled(true);
                edtCurrentPassword.setText("");
                Toast.makeText(ChangePasswordActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
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
