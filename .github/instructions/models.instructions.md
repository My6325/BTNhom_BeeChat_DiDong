---
applyTo: "app/src/main/java/com/example/beechats/models/*.java"
---

# Model Generation Rules

Khi tạo hoặc sửa đổi các Java Model classes:
1. **Firestore Serialization:** Phải luôn có một constructor rỗng (no-argument constructor).
2. **Data Types:** - Sử dụng `com.google.firebase.Timestamp` cho các trường thời gian.
   - Các trường `settings`, `reactions`, phải được định nghĩa dưới dạng `Map<String, Object>`.
3. **Naming:** Tên biến phải khớp chính xác với schema trong tài liệu thiết kế database (ví dụ: `photoUrl`, `fcmToken`, `searchKeywords`).
4. **Search:** Field `searchKeywords` phải luôn là `List<String>` để hỗ trợ tìm kiếm `array-contains`.
