package com.example.beechats.utils;

import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

/**
 * Tiện ích map Firebase exceptions sang thông báo tiếng Việt.
 * Dùng trong tất cả Repository để trả error message cho ViewModel/UI.
 */
public class ErrorHandler {

    /**
     * Chuyển Firebase Auth exception sang thông báo tiếng Việt.
     *
     * @param e Exception từ Firebase Auth
     * @return Thông báo lỗi tiếng Việt phù hợp hiển thị cho người dùng
     */
    public static String getAuthErrorMessage(Exception e) {
        if (e instanceof FirebaseAuthUserCollisionException) {
            return "Email đã được sử dụng.";
        }
        if (e instanceof FirebaseAuthWeakPasswordException) {
            return "Mật khẩu phải có ít nhất 6 ký tự.";
        }
        if (e instanceof FirebaseAuthRecentLoginRequiredException) {
            return "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";
        }
        if (e instanceof FirebaseAuthInvalidCredentialsException) {
            String errorCode = ((FirebaseAuthInvalidCredentialsException) e).getErrorCode();
            if ("ERROR_INVALID_EMAIL".equals(errorCode)) {
                return "Email không hợp lệ.";
            }
            if ("ERROR_WRONG_PASSWORD".equals(errorCode)) {
                return "Mật khẩu không chính xác.";
            }
            return "Thông tin đăng nhập không hợp lệ.";
        }
        if (e instanceof FirebaseAuthInvalidUserException) {
            String errorCode = ((FirebaseAuthInvalidUserException) e).getErrorCode();
            if ("ERROR_USER_NOT_FOUND".equals(errorCode)) {
                return "Tài khoản không tồn tại.";
            }
            if ("ERROR_USER_DISABLED".equals(errorCode)) {
                return "Tài khoản đã bị vô hiệu hóa.";
            }
        }
        // Fallback: trả nguyên message gốc nếu không map được
        return e.getMessage() != null ? e.getMessage() : "Đã xảy ra lỗi không xác định.";
    }
}
