---
applyTo: "app/src/test/java/com/example/beechats/**/*.java, app/src/androidTest/java/com/example/beechats/**/*.java"
---

# Testing & Validation Instructions

## 1. Unit Testing (Mockito)
- Mục tiêu độ bao phủ (Coverage) ≥ 70%.
- Khi viết test cho Repository, hãy mock Firebase SDK (Auth, Firestore) bằng Mockito.
- Mỗi hàm cần tối thiểu 2 test case:
  - **Happy Path:** Dữ liệu hợp lệ, kết quả trả về thành công.
  - **Error Case:** Giả lập timeout, lỗi quyền truy cập hoặc dữ liệu null.

## 2. Integration Testing (Firebase Emulator)
- Các bài test tích hợp phải được thiết kế để chạy trên Firebase Emulator Suite (không chạy trên production data).
- Quy trình test mẫu: Register -> Login -> Create Conversation -> Send Message -> Verify Firestore Document.

## 3. Stress Test & Security Validation
- Viết test kiểm tra Security Rules: Giả lập User A truy cập message của User B và mong đợi kết quả bị từ chối (Permission Denied).
- Test hiệu năng: Giả lập nhận 100 tin nhắn liên tiếp để kiểm tra độ ổn định của Listener.