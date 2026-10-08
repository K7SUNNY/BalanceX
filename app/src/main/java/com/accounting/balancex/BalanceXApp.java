package com.accounting.balancex;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class BalanceXApp extends Application {

    private int startedActivityCount = 0;
    private boolean isChangingConfig = false;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            System.loadLibrary("sqlcipher");
        } catch (Throwable ignored) {}
        // Restore user's preferred theme mode immediately upon application launch
        SettingsManager.applyTheme(this);

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                startedActivityCount++;
                isChangingConfig = false;
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {}

            @Override
            public void onActivityPaused(@NonNull Activity activity) {}

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                if (activity.isChangingConfigurations()) {
                    isChangingConfig = true;
                }
                startedActivityCount--;
                if (startedActivityCount <= 0) {
                    startedActivityCount = 0;
                    if (!isChangingConfig) {
                        MainActivity.resetSessionAuth();
                    }
                }
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {}
        });
    }
}
