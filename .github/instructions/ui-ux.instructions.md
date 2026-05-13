---
applyTo: "app/src/main/res/layout/*.xml, app/src/main/java/com/example/beechats/ui/**/*.java"
---

# UI & UX Standards (Material Design 3)

## 1. Kiến trúc Giao diện
- **ViewBinding:** Bắt buộc sử dụng ViewBinding cho tất cả Activity và Fragment. 
- **Memory Management:** Trong Fragment, biến binding phải được gán về `null` trong `onDestroyView()` để tránh leak bộ nhớ[cite: 2].
- **Responsive:** Sử dụng `ConstraintLayout` làm root view để đảm bảo giao diện hiển thị tốt trên nhiều kích thước màn hình.

## 2. Quản lý Tài nguyên (Resources)
- **Hardcoded Strings:** Tuyệt đối không viết trực tiếp văn bản vào file XML hoặc Java; tất cả phải sử dụng `R.string`[cite: 3].
- **Màu sắc & Kích thước:** - Sử dụng hệ thống màu từ `colors.xml` (dựa trên Material 3 palette).

## 3. Trải nghiệm người dùng (UX)
- **Loading State:** Mọi tác vụ bất đồng bộ (Auth, Firestore, Upload) phải có trạng thái chờ (ProgressBar hoặc Shimmer).
- **Feedback:** Sử dụng `Snackbar` thay vì `Toast` cho các thông báo quan trọng để không làm gián đoạn trải nghiệm người dùng.
- **Navigation:** Sử dụng `Jetpack Navigation Component` để quản lý luồng chuyển màn hình.