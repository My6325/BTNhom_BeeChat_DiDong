---
applyTo: 'app/src/main/java/com/example/beechats/**/*.java'
---

# Code Quality & Architecture Standards

## 1. MVVM & Repository Pattern

- Tuyệt đối không viết logic xử lý dữ liệu trực tiếp trong Activity/Fragment. Tất cả phải thông qua ViewModel và Repository.
- Repository chỉ trả về `LiveData`, `StateFlow` hoặc `Task<T>`, không trả về trực tiếp kết quả Firestore để đảm bảo tính bất đồng bộ.

## 2. Resource Management (Memory Leaks)

- Luôn quản lý vòng đời của `ListenerRegistration` (Firestore). Phải gọi `.remove()` trong phương thức `onStop()` hoặc khi ViewModel bị clear để tránh leak bộ nhớ.
- Tránh truyền trực tiếp `Context` vào trong Repository; sử dụng `ApplicationContext` nếu cần.

## 3. Clean Code Practices

- Không sử dụng hardcoded strings; mọi thông báo hoặc nhãn phải thông qua `R.string`.
- Phương thức public phải có Javadoc ngắn gọn mô tả mục đích, tham số và kết quả trả về.
- Đảm bảo tính đóng gói: Các field trong Model phải là `private` và có getter/setter phù hợp.

## 4. Ràng buộc Dependency

- Tính đóng băng: Không tự ý thêm, xóa hoặc thay đổi phiên bản của các thư viện trong build.gradle (Module: app) trừ khi có yêu cầu cụ thể.
- Kiểm tra xung đột: Khi cần đề xuất thư viện mới, phải kiểm tra sự tương thích với các thư viện hiện có (đặc biệt là Firebase SDK và AndroidX).
- Thư viện ưu tiên: - Image Loading: Glide (phải cấu hình DiskCacheStrategy.ALL để tối ưu dữ liệu).
  - Network: Retrofit2 (nếu cần gọi API ngoài Firestore).
