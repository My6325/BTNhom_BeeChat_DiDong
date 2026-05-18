package com.example.beechats.ui.onboarding;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.beechats.R;
import com.example.beechats.data.repositories.FriendRepository;
import com.example.beechats.data.repositories.UserRepository;
import com.example.beechats.utils.QrScanInviteHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;

/**
 * Activity trong suốt: chỉ mở camera quét QR để kết bạn (từ thanh tìm kiếm BeeChat).
 */
public class ScanQrActivity extends AppCompatActivity {

    private UserRepository userRepository;
    private FriendRepository friendRepository;
    private String currentUserId;
    private boolean isProcessingScan = false;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchQrScannerInternal();
                } else {
                    Toast.makeText(ScanQrActivity.this,
                            getString(R.string.qr_camera_denied), Toast.LENGTH_SHORT).show();
                    finish();
                }
            });

    private final ActivityResultLauncher<ScanOptions> qrScannerLauncher =
            registerForActivityResult(new ScanContract(), this::handleScanResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qr);

        userRepository = new UserRepository();
        friendRepository = new FriendRepository();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        currentUserId = user.getUid();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            getWindow().getDecorView().post(this::launchQrScannerInternal);
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
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
        if (isProcessingScan) {
            return;
        }
        isProcessingScan = true;
        QrScanInviteHelper.processQrScanForFriendInvite(
                this,
                result.getContents(),
                currentUserId,
                userRepository,
                friendRepository,
                () -> {
                    isProcessingScan = false;
                    if (!isFinishing()) {
                        finish();
                    }
                });
    }
}
