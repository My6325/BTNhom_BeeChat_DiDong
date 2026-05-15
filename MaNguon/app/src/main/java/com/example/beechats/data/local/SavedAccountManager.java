package com.example.beechats.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.beechats.data.models.SavedAccount;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SavedAccountManager {

    private static final String PREFS_NAME = "BeeChatsPrefs";
    private static final String KEY_SAVED_ACCOUNTS = "saved_accounts";

    public static void saveAccount(Context context, String uid, String displayName,
                                   String email, String photoUrl) {
        List<SavedAccount> accounts = getSavedAccounts(context);

        for (int i = 0; i < accounts.size(); i++) {
            if (accounts.get(i).getUid().equals(uid)) {
                accounts.get(i).setDisplayName(displayName);
                accounts.get(i).setEmail(email);
                accounts.get(i).setPhotoUrl(photoUrl);
                persist(context, accounts);
                return;
            }
        }

        accounts.add(0, new SavedAccount(uid, displayName, email, photoUrl));
        persist(context, accounts);
    }

    public static List<SavedAccount> getSavedAccounts(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_SAVED_ACCOUNTS, "[]");
        List<SavedAccount> list = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                list.add(new SavedAccount(
                        obj.optString("uid", ""),
                        obj.optString("displayName", ""),
                        obj.optString("email", ""),
                        obj.optString("photoUrl", null)
                ));
            }
        } catch (JSONException ignored) {}
        return list;
    }

    public static void removeAccount(Context context, String uid) {
        List<SavedAccount> accounts = getSavedAccounts(context);
        for (int i = 0; i < accounts.size(); i++) {
            if (accounts.get(i).getUid().equals(uid)) {
                accounts.remove(i);
                break;
            }
        }
        persist(context, accounts);
    }

    private static void persist(Context context, List<SavedAccount> accounts) {
        JSONArray array = new JSONArray();
        for (SavedAccount a : accounts) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("uid", a.getUid());
                obj.put("displayName", a.getDisplayName());
                obj.put("email", a.getEmail());
                obj.put("photoUrl", a.getPhotoUrl() != null ? a.getPhotoUrl() : JSONObject.NULL);
                array.put(obj);
            } catch (JSONException ignored) {}
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SAVED_ACCOUNTS, array.toString())
                .apply();
    }
}
