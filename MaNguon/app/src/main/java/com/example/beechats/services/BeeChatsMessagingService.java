package com.example.beechats.services;

import android.util.Log;

import com.example.beechats.data.repositories.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Service xử lý FCM token và tin nhắn push notification.
 * Tự động cập nhật fcmToken trong Firestore khi Firebase cấp token mới.
 */
public class BeeChatsMessagingService extends FirebaseMessagingService {

    private static final String TAG = "BeeChatsMessaging";

    /**
     * Gọi khi Firebase cấp token mới (lần đầu hoặc sau khi token bị thu hồi).
     * Chỉ lưu token nếu người dùng đang đăng nhập.
     */
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // Chưa đăng nhập — token sẽ được lưu sau khi đăng nhập
            return;
        }

        new UserRepository().updateFcmToken(currentUser.getUid(), token,
                new UserRepository.OnCompleteCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "FCM token cập nhật thành công.");
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "Lỗi cập nhật FCM token: " + errorMessage);
                    }
                });
    }

    /**
     * Xử lý tin nhắn FCM khi app đang ở foreground.
     * Phần hiển thị notification sẽ được implement trong Task 4.4.
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "FCM message nhận từ: " + remoteMessage.getFrom());
    }
}
