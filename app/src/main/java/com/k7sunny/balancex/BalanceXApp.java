package com.k7sunny.balancex;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.k7sunny.balancex.security.AppLockManager;

public class BalanceXApp extends Application {

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
                AppLockManager.getInstance().onActivityStarted(activity);
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {}

            @Override
            public void onActivityPaused(@NonNull Activity activity) {}

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                AppLockManager.getInstance().onActivityStopped(activity);
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {}
        });
    }
}
