package com.example.beechats.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Chế độ sáng/tối: lưu trong SharedPreferences (cùng file với MainActivity)
 * và đồng bộ với {@code users/{uid}.settings.darkMode} trên Firestore khi đã đăng nhập.
 */
public final class ThemeHelper {

    public static final String PREFS_NAME = "BeeChatsPrefs";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_PENDING_BOTTOM_NAV = "pending_bottom_nav_id";

    private ThemeHelper() {}

    /** Áp dụng chế độ đã lưu — gọi sớm trong {@link android.app.Application#onCreate()}. */
    public static void applyStoredNightMode(@NonNull Context context) {
        boolean dark = getStoredDarkMode(context);
        AppCompatDelegate.setDefaultNightMode(
                dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    public static boolean getStoredDarkMode(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_DARK_MODE, false);
    }

    /**
     * Bật/tắt chế độ tối: ghi prefs + kích hoạt DayNight (Activity có thể recreate).
     */
    public static void setDarkModeEnabled(@NonNull Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_DARK_MODE, enabled).apply();
        AppCompatDelegate.setDefaultNightMode(
                enabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Trước khi recreate do đổi DayNight, lưu tab bottom nav cần mở lại (ví dụ Bee-Menu).
     */
    public static void savePendingBottomNavItem(@NonNull Context context, int menuItemId) {
        prefs(context).edit().putInt(KEY_PENDING_BOTTOM_NAV, menuItemId).apply();
    }

    /** Đọc và xóa id menu đã lưu; trả về 0 nếu không có. */
    public static int consumePendingBottomNavItem(@NonNull Context context) {
        SharedPreferences p = prefs(context);
        int id = p.getInt(KEY_PENDING_BOTTOM_NAV, 0);
        if (id != 0) {
            p.edit().remove(KEY_PENDING_BOTTOM_NAV).apply();
        }
        return id;
    }
}
