package com.accounting.balancex;

import android.app.Application;

public class BalanceXApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            System.loadLibrary("sqlcipher");
        } catch (Throwable ignored) {}
        // Restore user's preferred theme mode immediately upon application launch
        SettingsManager.applyTheme(this);
    }
}
