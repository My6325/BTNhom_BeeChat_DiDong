package com.example.beechats.ui.friend;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.beechats.R;

/**
 * Fragment hiển thị danh sách người dùng đã bị chặn.
 * Được nạp vào thông qua FriendsPagerAdapter.
 */
public class BlockedListFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Nạp giao diện cho danh sách bị chặn
        return inflater.inflate(R.layout.frag_blocked_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Khởi tạo các View và Logic xử lý dữ liệu ở đây trong tương lai
        // Ví dụ: set Adapter cho RecyclerView hiển thị danh sách bị chặn
    }
}
