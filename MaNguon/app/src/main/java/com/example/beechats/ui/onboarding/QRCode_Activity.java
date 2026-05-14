package com.example.beechats.ui.onboarding;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.beechats.R;
import com.example.beechats.data.models.User;
import com.example.beechats.data.repositories.FriendRepository;
import com.example.beechats.data.repositories.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;

public class QRCode_Activity extends AppCompatActivity {
    private static final String QR_SCHEME = "beechats";
    private static final String QR_HOST = "user";
    private static final int QR_SIZE_DP = 256;

    private ImageView btnBack;
    private ImageView btnScanQr;
    private ImageView imgAvatar;
    private ImageView imgMyQrCode;
    private TextView tvUserName;

    private UserRepository userRepository;
    private FriendRepository friendRepository;
    private FirebaseUser currentUser;
    private String currentUserId;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchQrScannerInternal();
                } else {
                    showToast(getString(R.string.qr_camera_denied));
                }
            });

    private final ActivityResultLauncher<ScanOptions> qrScannerLauncher =
            registerForActivityResult(new ScanContract(), this::handleScanResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_qrcode);

        initViews();
        initRepositories();

        if (!ensureLoggedIn()) {
            return;
        }

        bindEvents();
        loadCurrentUserProfile();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnScanQr = findViewById(R.id.btn_scan_qr);
        imgAvatar = findViewById(R.id.img_avatar);
        imgMyQrCode = findViewById(R.id.img_my_qr_code);
        tvUserName = findViewById(R.id.tv_user_name);
    }

    private void initRepositories() {
        userRepository = new UserRepository();
        friendRepository = new FriendRepository();
    }

    private boolean ensureLoggedIn() {
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showToast(getString(R.string.qr_login_required));
            finish();
            return false;
        }

        currentUserId = currentUser.getUid();
        return true;
    }

    private void bindEvents() {
        btnBack.setOnClickListener(v -> {
            onBackPressed();
            overridePendingTransition(0, 0);
        });

        btnScanQr.setOnClickListener(v -> launchQrScanner());
    }

    private void loadCurrentUserProfile() {
        tvUserName.setText(resolveCurrentUserDisplayName(null));
        loadAvatar(null);
        renderMyQrCode();

        userRepository.getUser(currentUserId, new UserRepository.OnUserCallback() {
            @Override
            public void onSuccess(User user) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                tvUserName.setText(resolveCurrentUserDisplayName(user));
                loadAvatar(user != null ? user.getPhotoUrl() : null);
            }

            @Override
            public void onError(String errorMessage) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                showToast(getString(R.string.qr_loading_error));
            }
        });
    }
    

    private void renderMyQrCode() {
        try {
            Bitmap qrBitmap = generateQrBitmap(buildQrPayload(currentUserId), dpToPx(QR_SIZE_DP));
            imgMyQrCode.setImageBitmap(qrBitmap);
        } catch (WriterException e) {
            imgMyQrCode.setImageResource(R.drawable.my_qr);
            showToast(getString(R.string.qr_generate_error));
        }
    }

    private void loadAvatar(String photoUrl) {
        Glide.with(this)
                .load(TextUtils.isEmpty(photoUrl) ? R.drawable.bee_pollen : photoUrl)
                .placeholder(R.drawable.bee_pollen)
                .error(R.drawable.bee_pollen)
                .into(imgAvatar);
    }

    private void launchQrScanner() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchQrScannerInternal();
            return;
        }

        cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void launchQrScannerInternal() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt(getString(R.string.qr_scan_prompt));
        options.setBeepEnabled(false);
        options.setOrientationLocked(true);
        options.setBarcodeImageEnabled(false);
        qrScannerLauncher.launch(options);
    }

    private void handleScanResult(ScanIntentResult result) {
        String rawContent = result.getContents();
        if (TextUtils.isEmpty(rawContent)) {
            return;
        }

        String scannedUserId = extractUserIdFromQr(rawContent);
        if (TextUtils.isEmpty(scannedUserId)) {
            showToast(getString(R.string.qr_invalid));
            return;
        }

        if (scannedUserId.equals(currentUserId)) {
            showToast(getString(R.string.qr_self_scan_error));
            return;
        }

        userRepository.getUser(scannedUserId, new UserRepository.OnUserCallback() {
            @Override
            public void onSuccess(User user) {
                sendFriendRequest(scannedUserId, resolveTargetDisplayName(user));
            }

            @Override
            public void onError(String errorMessage) {
                showToast(getString(R.string.qr_user_not_found));
            }
        });
    }

    private void sendFriendRequest(String targetUserId, String targetDisplayName) {
        friendRepository.sendFriendRequest(currentUserId, targetUserId,
                new FriendRepository.OnFriendRequestCallback() {
                    @Override
                    public void onSuccess(String requestId) {
                        showToast(getString(R.string.qr_send_request_success, targetDisplayName));
                    }

                    @Override
                    public void onError(String errorMessage) {
                        showToast(errorMessage);
                    }
                });
    }

    private String buildQrPayload(String userId) {
        return new Uri.Builder()
                .scheme(QR_SCHEME)
                .authority(QR_HOST)
                .appendPath(userId)
                .build()
                .toString();
    }

    private String extractUserIdFromQr(String rawContent) {
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

        // Fallback cho trường hợp QR chỉ chứa trực tiếp UID.
        if (trimmedContent.matches("[A-Za-z0-9_-]{10,}")) {
            return trimmedContent;
        }

        return null;
    }

    private String resolveCurrentUserDisplayName(User user) {
        if (user != null && !TextUtils.isEmpty(user.getDisplayName())) {
            return user.getDisplayName().trim();
        }
        if (currentUser != null && !TextUtils.isEmpty(currentUser.getDisplayName())) {
            return currentUser.getDisplayName().trim();
        }
        if (currentUser != null && !TextUtils.isEmpty(currentUser.getEmail())) {
            return currentUser.getEmail().trim();
        }
        return getString(R.string.app_name);
    }

    private String resolveTargetDisplayName(User user) {
        if (user != null && !TextUtils.isEmpty(user.getDisplayName())) {
            return user.getDisplayName().trim();
        }
        if (user != null && !TextUtils.isEmpty(user.getEmail())) {
            return user.getEmail().trim();
        }
        return getString(R.string.app_name);
    }

    private Bitmap generateQrBitmap(@NonNull String content, int sizePx) throws WriterException {
        BitMatrix bitMatrix = new MultiFormatWriter()
                .encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx);

        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        int[] pixels = new int[width * height];

        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
