---
applyTo: "app/src/main/java/com/example/beechats/repositories/*.java, app/src/main/java/com/example/beechats/models/*.java"
---

# 🐝 BeeChat - Quy tắc Thiết kế & Triển khai Database (Firestore)

## 1. Nguyên tắc cốt lõi
- **Mô hình:** NoSQL Document-based, tối ưu cho truy vấn Real-time.
- **Tối ưu chi phí:** Sử dụng Denormalization (cache dữ liệu) để giảm số lần đọc Document.
- **Tính nhất quán:** Các tác vụ liên quan đến nhiều Collection/Document phải sử dụng `WriteBatch` hoặc `Transaction`.

## 2. Chi tiết Cấu trúc Collections

### 2.1 Collection `users/{userId}`
| Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- |
| `displayName` | string | Tên hiển thị người dùng |
| `email` | string | Email đăng ký |
| `photoUrl` | string \| null | URL ảnh đại diện (Cloudinary) |
| `searchKeywords` | string[] | Mảng lowercase để tìm kiếm bằng `array-contains` |
| `isOnline` | boolean | Trạng thái Online/Offline |
| `settings` | map | Cài đặt: `isOnlineVisible`, `darkMode`, `notificationsEnabled` |
| `fcmToken` | string | Token phục vụ Push Notification (FCM) |

### 2.2 Collection `conversations/{id}`
Phòng chat cho cả 1-1 và Nhóm.
- **ID hội thoại 1-1:** Phải tạo theo quy tắc deterministic: `sort([userId_A, userId_B]).join("_")`.

| Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- |
| `type` | string | "private" hoặc "group" |
| `participants` | string[] | Mảng userId tham gia |
| `lastMessage` | map | Cache tin nhắn cuối: `text`, `senderId`, `senderName`, `type`, `timestamp` |
| `adminIds` | string[] | Danh sách admin (chỉ dùng cho Group) |
| `updatedAt` | timestamp | Dùng để sắp xếp danh sách chat theo thời gian mới nhất |

### 2.3 Subcollection `messages/{id}` (Thuộc `conversations`)
| Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- |
| `senderId` | string | ID người gửi |
| `type` | string | "text", "image", "video", "voice", "system" |
| `status` | string | "sent", "delivered", "read" |
| `readBy` | map | `{ "userId": timestamp }` (Dùng cho group chat) |
| `replyTo` | map \| null | Lưu info tin nhắn gốc khi Reply: `messageId`, `text`, `senderName` |
| `reactions` | map | `{ "userId": "emoji" }` |

---

## 3. Quy tắc Logic & Nghiệp vụ (Business Logic)

### 3.1 Quan hệ Bạn bè & Chặn
- **Kết bạn:** Khi chấp nhận, phải tạo 2 document chéo nhau tại `friends/{userA}/friendList/{userB}` và ngược lại.
- **Chặn:** Luôn kiểm tra sự tồn tại của document trong `blockedUsers/{userId}/blockedList/{targetId}` trước khi gửi tin nhắn hoặc lời mời.

### 3.2 Lưu trữ Media (Cloudinary)
- **Tuyệt đối:** Không sử dụng Firebase Storage để lưu media chat.
- **Cấu trúc:** Sử dụng các Upload Preset `beechat_profile`, `beechat_group`, `beechat_chat`.
- **Giới hạn:** Ảnh/Profile < 10MB, Media Chat < 100MB.

---

## 4. Chỉ dẫn cho AI Agent khi gặp lỗi logic
Khi phát hiện mã nguồn vi phạm cấu trúc database hoặc logic nghiệp vụ trên:
1. **Phân tích lỗi:** Chỉ rõ vị trí mã nguồn không khớp với bảng thiết kế (sử dụng bảng so sánh).
2. **Đề xuất giải pháp:** Cung cấp 2-3 phương án xử lý:
    - **Phương án A:** Sửa mã nguồn để tuân thủ thiết kế hiện tại.
    - **Phương án B:** Tối ưu hóa truy vấn bằng Index hoặc WriteBatch để tăng hiệu suất.
3. **Ngôn ngữ:** Luôn phản hồi bằng tiếng Việt, súc tích, không khen ngợi sáo rỗng.

---

## 5. Danh sách Java Models cần tuân thủ
Các class trong package `com.example.beechats.models` phải khớp với schema:
1. `User`, `UserSettings`
2. `FriendRequest`, `Friend`
3. `Conversation`, `ConversationMember`
4. `Message`, `ReplyInfo`, `LastMessageInfo`
5. `BlockedUser`