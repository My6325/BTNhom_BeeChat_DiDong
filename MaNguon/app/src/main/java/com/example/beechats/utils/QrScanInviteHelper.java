package com.example.beechats.utils;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.beechats.R;
import com.example.beechats.data.models.User;
import com.example.beechats.data.repositories.FriendRepository;
import com.example.beechats.data.repositories.UserRepository;

/**
 * Parse nội dung QR BeeChat và gửi lời mời kết bạn — dùng chung cho {@code QRCode_Activity} và {@code ScanQrActivity}.
 */
public final class QrScanInviteHelper {

    public static final String QR_SCHEME = "beechats";
    public static final String QR_HOST = "user";

    private QrScanInviteHelper() {}

    public static String extractUserIdFromQr(String rawContent) {
        String trimmedContent = rawContent != null ? rawContent.trim() : "";
        if (trimmedContent.isEmpty()) {
            return null;
        }

        Uri uri = Uri.parse(trimmedContent);
        if (QR_SCHEME.equalsIgnoreCase(uri.getScheme())
                && QR_HOST.equalsIgnoreCase(uri.getAuthority())
                && !uri.getPathSegments().isEmpty()) {
            String userId = uri.getPathSegments().get(0);
            return userId != null && !userId.trim().isEmpty() ? userId.trim() : null;
        }

        if (trimmedContent.matches("[A-Za-z0-9_-]{10,}")) {
            return trimmedContent;
        }

        return null;
    }

    /**
     * @param onFlowFinished gọi khi xử lý xong (kể cả lỗi / hủy quét); chạy trên main thread sau Toast / callback Firestore.
     */
    public static void processQrScanForFriendInvite(
            Context context,
            @Nullable String rawContent,
            @NonNull String currentUserId,
            @NonNull UserRepository userRepository,
            @NonNull FriendRepository friendRepository,
            @Nullable Runnable onFlowFinished) {

        Context app = context.getApplicationContext();

        if (TextUtils.isEmpty(rawContent)) {
            if (onFlowFinished != null) {
                onFlowFinished.run();
            }
            return;
        }

        String scannedUserId = extractUserIdFromQr(rawContent);
        if (TextUtils.isEmpty(scannedUserId)) {
            Toast.makeText(app, app.getString(R.string.qr_invalid), Toast.LENGTH_SHORT).show();
            if (onFlowFinished != null) {
                onFlowFinished.run();
            }
            return;
        }

        if (scannedUserId.equals(currentUserId)) {
            Toast.makeText(app, app.getString(R.string.qr_self_scan_error), Toast.LENGTH_SHORT).show();
            if (onFlowFinished != null) {
                onFlowFinished.run();
            }
            return;
        }

        userRepository.getUser(scannedUserId, new UserRepository.OnUserCallback() {
            @Override
            public void onSuccess(User user) {
                String displayName = resolveTargetDisplayName(app, user);
                friendRepository.sendFriendRequest(currentUserId, scannedUserId,
                        new FriendRepository.OnFriendRequestCallback() {
                            @Override
                            public void onSuccess(String requestId) {
                                Toast.makeText(app,
                                        app.getString(R.string.qr_send_request_success, displayName),
                                        Toast.LENGTH_SHORT).show();
                                if (onFlowFinished != null) {
                                    onFlowFinished.run();
                                }
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Toast.makeText(app, errorMessage, Toast.LENGTH_SHORT).show();
                                if (onFlowFinished != null) {
                                    onFlowFinished.run();
                                }
                            }
                        });
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(app, app.getString(R.string.qr_user_not_found), Toast.LENGTH_SHORT).show();
                if (onFlowFinished != null) {
                    onFlowFinished.run();
                }
            }
        });
    }

    private static String resolveTargetDisplayName(Context app, User user) {
        if (user != null && !TextUtils.isEmpty(user.getDisplayName())) {
            return user.getDisplayName().trim();
        }
        if (user != null && !TextUtils.isEmpty(user.getEmail())) {
            return user.getEmail().trim();
        }
        return app.getString(R.string.app_name);
    }
}
