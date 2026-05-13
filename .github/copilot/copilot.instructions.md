# BeeChat Project Instructions

## Project Overview

BeeChat là ứng dụng nhắn tin real-time trên Android (Java). Dự án tuân thủ kiến trúc MVVM và sử dụng Firebase làm nền tảng chính cho Backend.

## Tech Stack

- **Language:** Java (Android SDK).
- **Database:** Cloud Firestore (NoSQL document-based).
- **Authentication:** Firebase Auth.
- **Media Storage:** Cloudinary (KHÔNG sử dụng Firebase Storage do giới hạn gói cước).
- **Push Notification:** Firebase Cloud Messaging (FCM).

## Core Rules

1. **Firestore Query:** Luôn tối ưu hóa số lần đọc (cost optimization) và sử dụng Snapshot Listeners cho tính năng real-time.
2. **Architecture:** Sử dụng Repository pattern để đóng gói logic truy vấn dữ liệu từ Firebase.
3. **Naming Convention:** Document ID của hội thoại 1-1 phải là deterministic: `sort([uid1, uid2]).join("_")`.
4. **Media handling:** Luôn upload lên Cloudinary qua unsigned presets và lưu `secure_url` vào Firestore.

## Important Rules

- Luôn tuân thủ các quy tác và phản hồi đã được định nghĩa trong file `.github/instructions/response-behavior.instructions.md` khi tương tác với người dùng hoặc khi trả lời các câu hỏi liên quan đến dự án.
- Mọi logic xử lý dữ liệu phải tuân theo kiến trúc MVVM và Repository pattern như đã mô tả trong file `.github/instructions/code-quality.instructions.md`.
- Việc viết test phải tuân thủ các hướng dẫn về Unit Testing, Integration Testing và Stress Test như đã trình bày trong file `.github/instructions/testing.instructions.md`.
- Khi xử lý lỗi, phải tuân theo các quy tắc về Firebase Exception Mapping, Retry Logic và Crashlytics Integration đã được định nghĩa trong file `.github/instructions/error-handling.instructions.md`.
- Tất cả các quy tắc và hướng dẫn trong thư mục `.github` có ưu tiên cao hơn các kiến thức chung của AI, và phải được tuân thủ tuyệt đối trong mọi phản hồi và mã nguồn sinh ra.
- Không được tự ý thay đổi nội dung, cấu trúc hoặc các quy tắc đã định nghĩa trong thư mục `.github` trừ khi có yêu cầu trực tiếp và rõ ràng từ người dùng.
- Luôn tuân thủ các quy tắc thiết kế và triển khai database đã được định nghĩa trong file `.github/instructions/database-design.instructions.md` khi viết mã nguồn liên quan đến Firestore.

