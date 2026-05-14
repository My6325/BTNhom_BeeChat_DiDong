package com.example.beechats.ui.setting;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beechats.R;
import com.example.beechats.data.models.User;
import com.example.beechats.data.repositories.UserRepository;
import com.example.beechats.ui.onboarding.QRCode_Activity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsFragment extends Fragment {

    private ImageView imgAvatar;
    private TextView txtName, txtEmail;
    private SwitchCompat switchDarkMode, switchStatus;
    private RecyclerView rvAccount;
    private UserRepository userRepository;
    private FirebaseAuth mAuth;

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

        // Khởi tạo Repository và Auth
        userRepository = new UserRepository();
        mAuth = FirebaseAuth.getInstance();

        // Thiết lập RecyclerView cho danh sách tài khoản
        rvAccount.setLayoutManager(new LinearLayoutManager(getContext()));
        // TODO: Cài đặt Adapter cho recyclerAccount nếu cần thiết
        loadUserProfile();
        return view;
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
                        // load avatar if using Glide/Picasso
                        // Glide.with(getContext()).load(user.getAvatarUrl()).into(imgAvatar);
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
}
