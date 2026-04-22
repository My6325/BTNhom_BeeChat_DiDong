package com.example.beechats.ui.chat;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * Adapter kết nối ViewPager2 với 3 Fragment con (Tất cả, Chưa đọc, Nhóm)
 */
public class ChatPagerAdapter extends FragmentStateAdapter {

    private static final int TAB_COUNT = 3;

    public ChatPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new AllChatFragment();     // Tab "Tất cả"
            case 1:
                return new UnreadChatFragment();  // Tab "Chưa đọc"
            case 2:
                return new GroupChatFragment();    // Tab "Nhóm"
            default:
                return new AllChatFragment();
        }
    }

    @Override
    public int getItemCount() {
        return TAB_COUNT;
    }
}
