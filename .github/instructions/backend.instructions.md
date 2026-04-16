---
applyTo: "app/src/main/java/com/example/beechats/repositories/*.java"
---

# Backend & Auth Logic Rules

## Firebase Auth
- Khi đăng ký (`register`), phải thực hiện đồng bộ: tạo Auth user trước, sau đó tạo Firestore document tại `users/{userId}` với các giá trị mặc định.
- Luôn xử lý các ngoại lệ cụ thể: `FirebaseAuthUserCollisionException`, `FirebaseAuthWeakPasswordException`.

## Firestore Operations
1. **Atomic Operations:** Sử dụng `WriteBatch` khi thực hiện các tác vụ liên quan đến nhiều document (ví dụ: gửi tin nhắn + cập nhật `lastMessage`, hoặc chấp nhận kết bạn).
2. **Security:** Không bao giờ lưu trực tiếp mật khẩu vào Firestore.
3. **Offline-first:** Tận dụng Firestore Cache. Khi gửi tin nhắn, mặc định trạng thái ban đầu là "sent".

## Business Logic
- **Search:** Khi cập nhật `displayName`, phải tự động gọi hàm helper để regenerate `searchKeywords` (mảng chứa các prefix và từ đơn lowercase).
- **Presence:** Cập nhật `isOnline` và `lastSeen` dựa trên vòng đời của ứng dụng (Foreground/Background).
- **Group Chat:** Khi tạo nhóm, phải tạo đồng thời conversation document và subcollection `members` cho từng thành viên.

## Cloudinary 
- Bảo mật thông tin: Tuyệt đối không lưu trữ API_KEY hoặc API_SECRET trực tiếp trong mã nguồn (hardcoded). Các thông tin này phải được quản lý qua local.properties hoặc biến môi trường.
- Phương thức tải lên: Chỉ sử dụng Unsigned Upload Presets (beechat_profile, beechat_group, beechat_chat) để thực hiện upload trực tiếp từ Client.
- Đóng gói Logic: Toàn bộ tác vụ liên quan đến Cloudinary phải thông qua lớp CloudinaryManager để đảm bảo tính nhất quán và dễ dàng kiểm soát luồng dữ liệu.