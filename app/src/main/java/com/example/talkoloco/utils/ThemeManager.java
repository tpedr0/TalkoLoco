package com.example.talkoloco.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;


public class ThemeManager {
    private static final String PREF_DARK_MODE = "dark_mode";
    private static ThemeManager instance;
    private final SharedPreferences prefs;

    private ThemeManager(Context context) {
        prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE);
    }

    public static ThemeManager getInstance(Context context) {
        if (instance == null) {
            instance = new ThemeManager(context.getApplicationContext());
        }
        return instance;
    }

    public boolean isDarkMode() {
        return prefs.getBoolean(PREF_DARK_MODE, false);
    }

    public void setDarkMode(boolean darkMode) {
        prefs.edit().putBoolean(PREF_DARK_MODE, darkMode).apply();
    }

    public void apply() {
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode() ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }
}