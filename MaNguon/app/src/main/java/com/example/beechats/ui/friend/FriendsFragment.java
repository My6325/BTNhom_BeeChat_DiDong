package com.example.beechats.ui.friend;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.beechats.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * Fragment chính cho Tab Bạn bè.
 * Chứa TabLayout và ViewPager2 để chuyển đổi giữa danh sách Bạn bè và Chặn.
 */
public class FriendsFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    // Tên hiển thị cho từng tab
    private final String[] tabTitles = {"Bạn bè", "Yêu cầu kết bạn"};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_friends, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ View
        tabLayout = view.findViewById(R.id.tabLayoutFriends);
        viewPager = view.findViewById(R.id.viewPagerFriends);

        // Tạo Adapter và gắn vào ViewPager2
        FriendsPagerAdapter pagerAdapter = new FriendsPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Kết nối TabLayout với ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(tabTitles[position]);
        }).attach();
    }
}
