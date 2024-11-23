package com.example.talkoloco.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Utility class for managing shared preferences in the application.
 */
public class PreferenceManager {
    private final SharedPreferences sharedPrefrences;

    /**
     * Initializes the shared preferences with a specific name.
     *
     * @param context The application context.
     */
    public PreferenceManager(Context context) {
        sharedPrefrences = context.getSharedPreferences(Constants.KEY_PREFERENCE_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Stores a boolean value in the shared preferences.
     *
     * @param key   The key for the preference.
     * @param value The boolean value to store.
     */
    public void putBoolean(String key, Boolean value) {
        SharedPreferences.Editor editor = sharedPrefrences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    /**
     * Retrieves a boolean value from the shared preferences.
     *
     * @param key The key for the preference.
     * @return The boolean value associated with the key, or false if not found.
     */
    public Boolean getBoolean(String key) {
        return sharedPrefrences.getBoolean(key, false);
    }

    /**
     * Stores a string value in the shared preferences.
     *
     * @param key   The key for the preference.
     * @param value The string value to store.
     */
    public void putString(String key, String value) {
        SharedPreferences.Editor editor = sharedPrefrences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    /**
     * Retrieves a string value from the shared preferences.
     *
     * @param key The key for the preference.
     * @return The string value associated with the key, or null if not found.
     */
    public String getString(String key) {
        return sharedPrefrences.getString(key, null);
    }

    /**
     * Clears all values in the shared preferences.
     */
    public void clear() {
        SharedPreferences.Editor editor = sharedPrefrences.edit();
        editor.clear();
        editor.apply();
    }
}