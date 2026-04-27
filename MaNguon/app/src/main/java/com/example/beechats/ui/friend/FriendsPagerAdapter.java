package com.example.beechats.ui.friend;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * Adapter cho ViewPager2 trong FriendsFragment.
 * Quản lý 2 màn hình: FriendListFragment (Bạn bè) và FriendInviteFragment (Yêu cầu kết bạn).
 */
public class FriendsPagerAdapter extends FragmentStateAdapter {

    public FriendsPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Vị trí 0: Tab Bạn bè
        if (position == 0) {
            return new FriendListFragment();
        } 
        // Vị trí 1: Tab Yêu cầu kết bạn
        return new FriendInviteFragment();
    }

    @Override
    public int getItemCount() {
        return 2; // Số lượng tab là 2
    }
}
