package com.example.evacuationapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.evacuationapp.models.User;
import com.google.gson.Gson;

public class PreferenceManager {
    private static final String PREF_NAME = "evacuation_prefs";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_USER = "user";

    private SharedPreferences prefs;
    private Gson gson;

    public PreferenceManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }
    public void setDriverProfileFilled(boolean filled) {
        prefs.edit().putBoolean("driver_profile_filled", filled).apply();
    }

    public boolean isDriverProfileFilled() {
        return prefs.getBoolean("driver_profile_filled", false);
    }
    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public void saveUserId(long userId) {
        prefs.edit().putLong(KEY_USER_ID, userId).apply();
    }

    public long getUserId() {
        return prefs.getLong(KEY_USER_ID, 0);
    }

    public void saveUserRole(String role) {
        prefs.edit().putString(KEY_USER_ROLE, role).apply();
    }

    public String getUserRole() {
        return prefs.getString(KEY_USER_ROLE, null);
    }

    public void saveUser(User user) {
        String userJson = gson.toJson(user);
        prefs.edit()
                .putString(KEY_USER, userJson)
                .putLong(KEY_USER_ID, user.getUserId())
                .putString(KEY_USER_ROLE, user.getRole())
                .apply();
    }

    public User getUser() {
        String userJson = prefs.getString(KEY_USER, null);
        if (userJson != null) {
            return gson.fromJson(userJson, User.class);
        }
        return null;
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

    // Добавь этот метод
    public boolean isLoggedIn() {
        return getToken() != null && getUserId() != 0;
    }
}