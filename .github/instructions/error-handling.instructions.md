---
applyTo: "app/src/main/java/com/example/beechats/**/*.java"
---

# Error Handling & Resilience Instructions

## 1. Firebase Exception Mapping
- Luôn sử dụng `ErrorHandler` utility để map các Firebase Exception sang thông báo tiếng Việt cho người dùng.
- Đối với `FirebaseAuthException`:
  - `ERROR_INVALID_EMAIL` -> "Email không hợp lệ."
  - `ERROR_WRONG_PASSWORD` -> "Mật khẩu không chính xác."
  - `ERROR_USER_NOT_FOUND` -> "Tài khoản không tồn tại.".

## 2. Retry Logic & Validation
- Áp dụng Retry logic (tối đa 3 lần với exponential backoff) cho các tác vụ mạng quan trọng như gửi tin nhắn hoặc upload file.
- Luôn kiểm tra kết nối mạng bằng `ConnectivityManager` trước khi thực hiện Firestore/Auth request.
- Validate input (null check, empty string) ở tầng Repository trước khi gọi Firebase SDK.

## 3. Crashlytics Integration
- Đối với các lỗi không làm sập app (non-fatal), sử dụng `FirebaseCrashlytics.getInstance().recordException(e)`.
- Luôn đính kèm `userId` hoặc `conversationId` vào log khi ghi nhận lỗi để phục vụ debug.