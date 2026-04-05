# 🐝 Dự án: BeeChat - Ứng dụng di động nhắn tin Real-time

## Đây là tài liệu đặc tả dự án dành cho AI Agent để hỗ trợ lập trình, thiết kế cơ sở dữ liệu và tư vấn logic hệ thống.

## 🛠 1. Công cụ & Công nghệ phát triển

- Ngôn ngữ lập trình: Java
- Công cụ phát triển (IDE): Android Studio
- Thiết kế giao diện: Figma
- Cơ sở dữ liệu & Back-end: Firebase (Realtime Database/Firestore/Auth/Storage)

---

## 📋 2. Danh sách Chức năng chi tiết

### 👤 Quản lý Tài khoản

- Xác thực: Đăng ký, Đăng nhập, Đổi mật khẩu.
- Hồ sơ người dùng: Chỉnh sửa thông tin cá nhân hoặc xóa tài khoản.
- Hệ thống Bạn bè: \* Tìm kiếm tài khoản qua tên/ID hoặc quét mã QR
  - Gửi, chấp nhận hoặc từ chối yêu cầu kết bạn.
  - Quản lý danh sách bạn bè và hủy kết bạn.

### 💬 Hệ thống Chat cá nhân (1-1)

- Cơ bản: Nhắn tin và cập nhật thời gian thực (Real-time).
- Đa phương tiện: Gửi hình ảnh, video, voice chat (giới hạn dung lượng < 100MB).
- Tương tác tin nhắn:
  - Hiển thị trạng thái: "Đang nhập tin nhắn", "Đã xem", "Trạng thái hoạt động".
  - Quản lý tin nhắn: Thu hồi, xóa hoặc tìm kiếm nội dung tin nhắn.
  - Phản hồi chuyên sâu: Trả lời riêng biệt (Reply/Quote) và thả cảm xúc (Reactions: ❤️, 👍, 😂...).

- Thông báo: Đẩy thông báo tức thì khi có tin nhắn mới.

### 👨‍👩‍👧‍👦 Đoạn chat nhóm

- Quản lý thành viên: Thêm/mời hoặc mời ra khỏi nhóm.
- Phân quyền Admin: Chỉ Admin mới có quyền đổi tên nhóm, ảnh đại diện và quản lý thành viên.
- Cá nhân hóa nhóm: Đặt biệt danh (Nickname) cho từng thành viên.
- Ghim tin nhắn: Ghim các thông báo quan trọng lên đầu trang chat.

### 🔒 Bảo mật & Cá nhân hóa

- Chế độ tối (Dark Mode): Hỗ trợ giao diện nền tối tiêu chuẩn.
- Quyền riêng tư: \* Tùy chọn ẩn/hiện trạng thái Online/Offline.
  - Chức năng Chặn (Block) tài khoản không mong muốn.

---

## 🎯 3. Yêu cầu kỹ thuật & Trải nghiệm (UX/UI)

- Tính ổn định: Các chức năng quản lý phải hoạt động ổn định, không lỗi logic.
- Tốc độ: Cập nhật dữ liệu (tin nhắn/thông báo) phải diễn ra Real-time.
- Giao diện: Đơn giản, tinh tế nhưng đảm bảo trải nghiệm người dùng mượt mà.
