package com.example.beechats.utils;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.example.beechats.data.repositories.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Lắng nghe vòng đời của toàn bộ app qua ProcessLifecycleOwner.
 * Tự động cập nhật isOnline=true khi vào foreground, isOnline=false khi ra background.
 * Đăng ký trong BeeChatsApp.onCreate() một lần duy nhất.
 */
public class AppLifecycleObserver implements DefaultLifecycleObserver {

    private static final String TAG = "BeeChat_Test";

    private final FirebaseAuth auth;
    private final UserRepository userRepository;

    public AppLifecycleObserver(FirebaseAuth auth, UserRepository userRepository) {
        this.auth = auth;
        this.userRepository = userRepository;
    }

    /** App vào foreground — cập nhật isOnline=true */
    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        Log.d(TAG, "🟢 [Lifecycle] App vào foreground");
        updateStatus(true);
    }

    /** App ra background — cập nhật isOnline=false + lastSeen */
    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        Log.d(TAG, "🔴 [Lifecycle] App ra background");
        updateStatus(false);
    }

    private void updateStatus(boolean isOnline) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "   ⚠️ Chưa đăng nhập — bỏ qua cập nhật isOnline");
            return;
        }
        String uid = currentUser.getUid();
        Log.d(TAG, "   🔄 Cập nhật users/" + uid + " → isOnline=" + isOnline);
        userRepository.updateOnlineStatus(uid, isOnline,
                new UserRepository.OnCompleteCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "   ✅ isOnline=" + isOnline + " đã ghi vào Firestore");
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "   ❌ Cập nhật isOnline thất bại: " + errorMessage);
                    }
                });
    }
}
