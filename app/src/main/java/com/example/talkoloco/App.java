package com.example.talkoloco;

import android.app.Application;
import com.example.talkoloco.utils.ThemeManager;

public class App extends Application {
    @Override
    public void onCreate() {
            super.onCreate();
            ThemeManager.getInstance(this).apply();
        }

    private void initializeTheme() {
        ThemeManager.getInstance(this).apply();
    }
}