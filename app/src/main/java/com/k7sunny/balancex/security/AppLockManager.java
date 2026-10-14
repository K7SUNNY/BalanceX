package com.k7sunny.balancex.security;

import android.app.Activity;
import android.content.Context;

import com.k7sunny.balancex.SettingsManager;

public class AppLockManager {

    private static volatile AppLockManager sInstance;

    private boolean isSessionAuthenticated = false;
    private long lastBackgroundTimestamp = 0;
    private boolean isLaunchingSystemPicker = false;
    private int startedActivityCount = 0;
    private boolean isChangingConfig = false;

    private AppLockManager() {}

    public static AppLockManager getInstance() {
        if (sInstance == null) {
            synchronized (AppLockManager.class) {
                if (sInstance == null) {
                    sInstance = new AppLockManager();
                }
            }
        }
        return sInstance;
    }

    public synchronized void onActivityStarted(Activity activity) {
        startedActivityCount++;
        isChangingConfig = false;

        if (startedActivityCount == 1) {
            // App transitioning from background to foreground
            if (isLaunchingSystemPicker) {
                // Return from system picker (SAF, Camera, Contacts) does not trigger lock
                isLaunchingSystemPicker = false;
            } else if (lastBackgroundTimestamp > 0) {
                long elapsed = System.currentTimeMillis() - lastBackgroundTimestamp;
                long gracePeriod = SettingsManager.getLockGracePeriodMillis(activity);
                if (elapsed > gracePeriod) {
                    isSessionAuthenticated = false;
                }
            }
        }
    }

    public synchronized void onActivityStopped(Activity activity) {
        if (activity != null && activity.isChangingConfigurations()) {
            isChangingConfig = true;
        }
        startedActivityCount = Math.max(0, startedActivityCount - 1);
        if (startedActivityCount == 0 && !isChangingConfig) {
            lastBackgroundTimestamp = System.currentTimeMillis();
        }
    }

    public synchronized boolean isLockRequired(Context context) {
        if (context == null) return false;
        if (!SettingsManager.isBiometricLockEnabled(context)) {
            return false;
        }
        return !isSessionAuthenticated;
    }

    public synchronized void setSessionAuthenticated(boolean authenticated) {
        this.isSessionAuthenticated = authenticated;
        if (authenticated) {
            this.lastBackgroundTimestamp = System.currentTimeMillis();
        }
    }

    public synchronized boolean isSessionAuthenticated() {
        return this.isSessionAuthenticated;
    }

    public synchronized void lockSession() {
        this.isSessionAuthenticated = false;
    }

    public synchronized void resetSession() {
        lockSession();
    }

    public synchronized void setLaunchingSystemPicker(boolean launching) {
        this.isLaunchingSystemPicker = launching;
    }

    public synchronized boolean isLaunchingSystemPicker() {
        return this.isLaunchingSystemPicker;
    }
}
