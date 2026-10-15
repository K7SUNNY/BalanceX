package com.k7sunny.balancex.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.k7sunny.balancex.BuildConfig;
import com.k7sunny.balancex.SettingsManager;
import com.k7sunny.balancex.notifications.ReminderPlanner;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class SubscriptionWorker extends Worker {

    private static final String TAG = "SubscriptionWorker";
    public static final String WORK_NAME = "balancex_subscription_periodic_check";

    public SubscriptionWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        Log.d(TAG, "Executing SubscriptionWorker via ReminderPlanner...");

        try {
            ReminderPlanner.processSubscriptions(context);
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "SubscriptionWorker execution failed", e);
            return Result.failure();
        }
    }

    /**
     * Schedules periodic check with ExistingPeriodicWorkPolicy.UPDATE
     * and aligns initial delay to user-configured morning reminder time (default 9:00 AM).
     */
    public static void scheduleDailyCheck(Context context) {
        Context appContext = context.getApplicationContext();

        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        int hour = SettingsManager.getBillReminderHour(appContext);
        int minute = SettingsManager.getBillReminderMinute(appContext);

        target.set(Calendar.HOUR_OF_DAY, hour);
        target.set(Calendar.MINUTE, minute);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1);
        }

        long initialDelayMillis = target.getTimeInMillis() - now.getTimeInMillis();

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                SubscriptionWorker.class,
                24, TimeUnit.HOURS
        )
                .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
        );

        Log.d(TAG, "Scheduled subscription periodic check in " + (initialDelayMillis / 1000 / 60) + " mins (Policy.UPDATE)");
    }

    /**
     * Triggers an immediate one-time check for testing or pull-to-refresh.
     */
    public static void checkNow(Context context) {
        Context appContext = context.getApplicationContext();
        OneTimeWorkRequest oneTimeWork = new OneTimeWorkRequest.Builder(SubscriptionWorker.class).build();
        WorkManager.getInstance(appContext).enqueueUniqueWork(
                "balancex_subscription_check_now",
                ExistingWorkPolicy.REPLACE,
                oneTimeWork
        );
    }
}
