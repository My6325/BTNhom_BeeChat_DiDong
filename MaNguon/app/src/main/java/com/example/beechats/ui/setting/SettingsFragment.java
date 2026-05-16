package com.example.beechats.ui.setting;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.beechats.R;
import com.example.beechats.data.local.SavedAccountManager;
import com.example.beechats.data.models.SavedAccount;
import com.example.beechats.data.models.User;
import com.example.beechats.data.repositories.UserRepository;
import com.example.beechats.ui.auth.LoginActivity;
import com.example.beechats.ui.onboarding.QRCode_Activity;
import com.example.beechats.utils.ThemeHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class SettingsFragment extends Fragment {

    private ImageView imgAvatar;
    private TextView txtName, txtEmail;
    private SwitchCompat switchDarkMode, switchStatus;
    private RecyclerView rvAccount;
    private UserRepository userRepository;
    private FirebaseAuth mAuth;

    private final CompoundButton.OnCheckedChangeListener darkModeListener =
            (buttonView, isChecked) -> {
                ThemeHelper.savePendingBottomNavItem(requireContext(), R.id.menu_settings);
                ThemeHelper.setDarkModeEnabled(requireContext(), isChecked);
                FirebaseUser firebaseUser = mAuth.getCurrentUser();
                if (firebaseUser == null) {
                    return;
                }
                userRepository.updateDarkMode(firebaseUser.getUid(), isChecked,
                        new UserRepository.OnCompleteCallback() {
                            @Override
                            public void onSuccess() { }

                            @Override
                            public void onError(String errorMessage) {
                                if (!isAdded()) {
                                    return;
                                }
                                Toast.makeText(getContext(),
                                        getString(R.string.dark_mode_save_error, errorMessage),
                                        Toast.LENGTH_SHORT).show();
                                setDarkSwitchWithoutEvent(!isChecked);
                                ThemeHelper.setDarkModeEnabled(requireContext(), !isChecked);
                            }
                        });
            };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.menu_setting_user, container, false);

        // Ánh xạ View
        imgAvatar = view.findViewById(R.id.img_avatar);
        txtName = view.findViewById(R.id.txt_setting_name);
        txtEmail = view.findViewById(R.id.txt_Email);
        switchDarkMode = view.findViewById(R.id.switch_dark_mode);
        switchStatus = view.findViewById(R.id.switch_status);
        rvAccount = view.findViewById(R.id.rv_Account);

        view.findViewById(R.id.btn_menu_qr).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), QRCode_Activity.class)));

        view.findViewById(R.id.row_change_password).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ChangePasswordActivity.class)));

        view.findViewById(R.id.btn_exit_account).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });

        // Khởi tạo Repository và Auth
        userRepository = new UserRepository();
        mAuth = FirebaseAuth.getInstance();

        setDarkSwitchWithoutEvent(ThemeHelper.getStoredDarkMode(requireContext()));
        switchDarkMode.setOnCheckedChangeListener(darkModeListener);

        rvAccount.setLayoutManager(new LinearLayoutManager(getContext()));
        loadSavedAccounts();
        loadUserProfile();
        // Thiết lập RecyclerView cho danh sách tài khoản
        return view;
    }

    private void loadSavedAccounts() {
        List<SavedAccount> accounts = SavedAccountManager.getSavedAccounts(requireContext());
        AccountAdapter adapter = new AccountAdapter(accounts, new AccountAdapter.OnAccountClickListener() {
            @Override
            public void onAccountClick(SavedAccount account) {
                // Đăng xuất tài khoản hiện tại trên Firebase
                FirebaseAuth.getInstance().signOut();

                // Intent chuyển về màn hình Đăng nhập
                Intent intent = new Intent(requireContext(), LoginActivity.class);

                // Đính kèm email của tài khoản được click để truyền sang LoginActivity
                if (account.getEmail() != null) {
                    intent.putExtra("SWITCH_ACCOUNT_EMAIL", account.getEmail());
                }

                //Xóa lịch sử các màn hình cũ --> giúp app không bị xếp chồng quá nhiều gây loú khi sử dụng back
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
        rvAccount.setAdapter(adapter);
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            
            // Lấy dữ liệu từ Firestore thông qua UserRepository
            userRepository.getUser(uid, new UserRepository.OnUserCallback() {
                @Override
                public void onSuccess(User user) {
                    if (isAdded()) { // Kiểm tra Fragment còn gắn vào Activity không
                        txtName.setText(user.getDisplayName());
                        txtEmail.setText(user.getEmail());
<<<<<<< HEAD
                        // load avatar if using Glide/Picasso
                        // Glide.with(getContext()).load(user.getAvatarUrl()).into(imgAvatar);

                        boolean darkFromCloud = user.getSettings() != null
                                && user.getSettings().isDarkMode();
                        if (ThemeHelper.getStoredDarkMode(requireContext()) != darkFromCloud) {
                            ThemeHelper.setDarkModeEnabled(requireContext(), darkFromCloud);
                        }
                        setDarkSwitchWithoutEvent(darkFromCloud);
=======
                        // Kiểm tra xem user có link avatar hay chưa
                        if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                            Glide.with(requireContext())
                                    .load(user.getPhotoUrl())
                                    .placeholder(R.drawable.bee_pollen) // Ảnh chờ khi đang tải mạng
                                    .error(R.drawable.bee_pollen)       // Ảnh thay thế nếu link bị lỗi
                                    .into(imgAvatar);
                        } else {
                            // Nếu tài khoản chưa cài avatar, set thẳng ảnh mặc định
                            imgAvatar.setImageResource(R.drawable.bee_pollen);
                        }
>>>>>>> feature/chat-ui
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Lỗi: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void setDarkSwitchWithoutEvent(boolean checked) {
        switchDarkMode.setOnCheckedChangeListener(null);
        switchDarkMode.setChecked(checked);
        switchDarkMode.setOnCheckedChangeListener(darkModeListener);
    }
}
