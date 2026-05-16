package com.example.beechats.ui.setting;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.beechats.R;
import com.example.beechats.data.models.SavedAccount;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.AccountViewHolder> {

    public interface OnAccountClickListener {
        void onAccountClick(SavedAccount account);
    }

    private final List<SavedAccount> accounts;
    private final OnAccountClickListener listener;
    private final String currentUid;

    public AccountAdapter(List<SavedAccount> accounts, OnAccountClickListener listener) {
        this.accounts = accounts;
        this.listener = listener;
        String uid = null;
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        this.currentUid = uid;
    }

    @NonNull
    @Override
    public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_account, parent, false);
        return new AccountViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountViewHolder holder, int position) {
        SavedAccount account = accounts.get(position);

        holder.txtName.setText(
                TextUtils.isEmpty(account.getDisplayName()) ? "User" : account.getDisplayName());
        holder.txtEmail.setText(
                TextUtils.isEmpty(account.getEmail()) ? "" : account.getEmail());
        //set avatar
        if (!TextUtils.isEmpty(account.getPhotoUrl())) {
            Glide.with(holder.itemView.getContext())
                    .load(account.getPhotoUrl())
                    .placeholder(R.drawable.bee_pollen)
                    .error(R.drawable.bee_pollen)
                    .into(holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.bee_pollen);
        }

        boolean isCurrent = account.getUid() != null && account.getUid().equals(currentUid);
        if (isCurrent) {
            // Nếu là tài khoản đang đăng nhập -> Hiện dấu tick
            holder.imgShowAccount.setVisibility(View.VISIBLE);
            holder.imgShowAccount.setImageResource(R.drawable.checkbox_terms_mark);

            // Hủy sự kiện click vào dấu tick vì nó chỉ là icon báo hiệu
            holder.imgShowAccount.setOnClickListener(null);
        } else {
            // Nếu là tài khoản cũ -> Ẩn icon đi
            // Dùng INVISIBLE để giữ nguyên không gian, tránh chữ bị xô lệch sang phải
            holder.imgShowAccount.setVisibility(View.INVISIBLE);
        }

        //sự kiện bấm vào cả dòng tài khoản (để chuyển đổi)
        holder.itemView.setOnClickListener(v -> {
            if (listener != null && !isCurrent) {
                listener.onAccountClick(account);
            }
        });
    }

    @Override
    public int getItemCount() {
        return accounts.size();
    }

    static class AccountViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgAvatar;
        final TextView txtName;
        final TextView txtEmail;
        final ImageView imgShowAccount;

        AccountViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.img_account);
            txtName = itemView.findViewById(R.id.txt_account_name);
            txtEmail = itemView.findViewById(R.id.txt_Email);
            imgShowAccount = itemView.findViewById(R.id.img_show_account);
        }
    }
}
