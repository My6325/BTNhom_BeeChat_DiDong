package com.example.beechats.data.repositories;

import android.net.Uri;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import java.util.Map;

/**
 * Repository xử lý upload media lên Cloudinary.
 * Dùng unsigned upload với presets để đảm bảo bảo mật (không cần api_secret trên client).
 *
 * Upload presets cần tạo trên Cloudinary Console:
 * - beechat_profile: ảnh đại diện (jpg/png/webp, max 10MB)
 * - beechat_group: ảnh nhóm (jpg/png/webp, max 10MB)
 * - beechat_chat: media chat (jpg/png/webp/mp4/mp3/ogg/m4a, max 100MB)
 */
public class CloudinaryRepository {

    private static final String PRESET_PROFILE = "beechat_profile";
    private static final String PRESET_GROUP = "beechat_group";
    private static final String PRESET_CHAT = "beechat_chat";

    public interface OnUploadCallback {
        void onSuccess(String secureUrl);
        void onError(String errorMessage);
    }

    /**
     * Upload ảnh đại diện user.
     * Folder: beechat/profiles/{userId}
     */
    public void uploadProfilePhoto(String userId, Uri fileUri, OnUploadCallback callback) {
        MediaManager.get().upload(fileUri)
                .unsigned(PRESET_PROFILE)
                .option("folder", "beechat/profiles/" + userId)
                .option("public_id", "avatar")
                .option("overwrite", true)
                .callback(createUploadCallback(callback))
                .dispatch();
    }

    /**
     * Upload ảnh đại diện nhóm.
     * Folder: beechat/groups/{conversationId}
     */
    public void uploadGroupPhoto(String conversationId, Uri fileUri, OnUploadCallback callback) {
        MediaManager.get().upload(fileUri)
                .unsigned(PRESET_GROUP)
                .option("folder", "beechat/groups/" + conversationId)
                .option("public_id", "avatar")
                .option("overwrite", true)
                .callback(createUploadCallback(callback))
                .dispatch();
    }

    /**
     * Upload media trong chat (ảnh, video, voice).
     * Folder: beechat/chat/{conversationId}/{messageId}
     */
    public void uploadChatMedia(String conversationId, String messageId, Uri fileUri, OnUploadCallback callback) {
        MediaManager.get().upload(fileUri)
                .unsigned(PRESET_CHAT)
                .option("folder", "beechat/chat/" + conversationId + "/" + messageId)
                .callback(createUploadCallback(callback))
                .dispatch();
    }

    private UploadCallback createUploadCallback(OnUploadCallback callback) {
        return new UploadCallback() {
            @Override
            public void onStart(String requestId) {}

            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {}

            @Override
            public void onSuccess(String requestId, Map resultData) {
                String secureUrl = (String) resultData.get("secure_url");
                if (secureUrl != null) {
                    callback.onSuccess(secureUrl);
                } else {
                    callback.onError("Upload thành công nhưng không nhận được URL");
                }
            }

            @Override
            public void onError(String requestId, ErrorInfo error) {
                callback.onError(error.getDescription());
            }

            @Override
            public void onReschedule(String requestId, ErrorInfo error) {
                callback.onError("Upload bị hoãn: " + error.getDescription());
            }
        };
    }
}
