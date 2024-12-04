package com.example.talkoloco.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;


/**
 * Class that manages theme preferences and application.
 * Handles dark mode toggling and persistence.
 */
public class ThemeManager {
    private static final String PREF_DARK_MODE = "dark_mode";
    private static ThemeManager instance;
    private final SharedPreferences prefs;

    /**
     * Initializes preferences for theme storage.
     *
     * @param context Application context
     */
    private ThemeManager(Context context) {
        prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE);
    }

    /**
     * Returns instance of ThemeManager.
     *
     * @param context Application context
     * @return ThemeManager instance
     */
    public static ThemeManager getInstance(Context context) {
        if (instance == null) {
            instance = new ThemeManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Checks if dark mode is currently enabled.
     *
     * @return true if dark mode is enabled, false otherwise
     */
    public boolean isDarkMode() {
        return prefs.getBoolean(PREF_DARK_MODE, false);
    }

    /**
     * Sets dark mode preference.
     *
     * @param darkMode true to enable dark mode, false to disable
     */
    public void setDarkMode(boolean darkMode) {
        prefs.edit().putBoolean(PREF_DARK_MODE, darkMode).apply();
    }

    /**
     * Applies the current theme setting to the application.
     */
    public void apply() {
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode() ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }
}