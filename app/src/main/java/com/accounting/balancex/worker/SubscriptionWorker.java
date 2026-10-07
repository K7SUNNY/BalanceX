package com.accounting.balancex.worker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.accounting.balancex.MainActivity;
import com.accounting.balancex.R;
import com.accounting.balancex.SettingsManager;
import com.accounting.balancex.data.db.AppDatabase;
import com.accounting.balancex.data.entity.SubscriptionEntity;
import com.accounting.balancex.data.entity.TransactionEntity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SubscriptionWorker extends Worker {

    private static final String TAG = "SubscriptionWorker";
    public static final String CHANNEL_ID = "balancex_bill_reminders";
    private static final String WORK_NAME = "balancex_subscription_periodic_check";

    public SubscriptionWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        Log.d(TAG, "Running SubscriptionWorker periodic check...");

        try {
            AppDatabase db = AppDatabase.getDatabase(context);
            List<SubscriptionEntity> activeSubs = db.subscriptionDao().getActiveSubscriptions();
            if (activeSubs == null || activeSubs.isEmpty()) {
                return Result.success();
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar todayCal = Calendar.getInstance();
            todayCal.set(Calendar.HOUR_OF_DAY, 0);
            todayCal.set(Calendar.MINUTE, 0);
            todayCal.set(Calendar.SECOND, 0);
            todayCal.set(Calendar.MILLISECOND, 0);
            Date today = todayCal.getTime();

            String symbol = SettingsManager.getCurrencySymbol(context);

            for (SubscriptionEntity sub : activeSubs) {
                if (sub.nextDueDate == null || sub.nextDueDate.trim().isEmpty()) {
                    continue;
                }

                try {
                    Date dueDate = sdf.parse(sub.nextDueDate.trim());
                    if (dueDate == null) continue;

                    long diffMillis = dueDate.getTime() - today.getTime();
                    long daysUntilDue = TimeUnit.MILLISECONDS.toDays(diffMillis);

                    // Check if due in <= reminderDaysBefore and not past due by more than 1 day
                    if (daysUntilDue >= 0 && daysUntilDue <= sub.reminderDaysBefore) {
                        String msg;
                        if (daysUntilDue == 0) {
                            msg = sub.name + " (" + symbol + String.format(Locale.getDefault(), "%,.2f", sub.amount) + ") is due today!";
                        } else if (daysUntilDue == 1) {
                            msg = sub.name + " (" + symbol + String.format(Locale.getDefault(), "%,.2f", sub.amount) + ") is due tomorrow!";
                        } else {
                            msg = sub.name + " (" + symbol + String.format(Locale.getDefault(), "%,.2f", sub.amount) + ") is due in " + daysUntilDue + " days.";
                        }
                        showNotification(context, (int) sub.id, "Upcoming Bill Reminder", msg);
                    }

                    // Auto-add transaction if due today or past and autoAddTransaction is enabled
                    if (daysUntilDue <= 0 && sub.autoAddTransaction) {
                        TransactionEntity tx = new TransactionEntity();
                        tx.date = sdf.format(today);
                        tx.amount = String.format(Locale.getDefault(), "%.2f", sub.amount);
                        tx.receiver = sub.name;
                        tx.description = "Auto-billed recurring: " + sub.name;
                        tx.category = sub.category != null ? sub.category : "Subscription";
                        tx.paymentMethod = "Auto-Debit";
                        tx.textType = "debit";
                        db.transactionDao().insert(tx);

                        // Advance nextDueDate
                        Calendar nextCal = Calendar.getInstance();
                        nextCal.setTime(dueDate);
                        if ("Yearly".equalsIgnoreCase(sub.billingCycle)) {
                            nextCal.add(Calendar.YEAR, 1);
                        } else if ("Weekly".equalsIgnoreCase(sub.billingCycle)) {
                            nextCal.add(Calendar.WEEK_OF_YEAR, 1);
                        } else {
                            nextCal.add(Calendar.MONTH, 1);
                        }
                        sub.nextDueDate = sdf.format(nextCal.getTime());
                        db.subscriptionDao().updateSubscription(sub);

                        showNotification(context, (int) (sub.id + 1000), "Auto-Subscription Logged",
                                "Logged " + symbol + String.format(Locale.getDefault(), "%,.2f", sub.amount) + " for " + sub.name);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing subscription: " + sub.name, e);
                }
            }

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "SubscriptionWorker failed", e);
            return Result.failure();
        }
    }

    private void showNotification(Context context, int notificationId, String title, String message) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Bill & Subscription Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for upcoming bills and recurring payments in BalanceX");
            manager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_bell)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        manager.notify(notificationId, builder.build());
    }

    public static void scheduleDailyCheck(Context context) {
        Constraints constraints = new Constraints.Builder()
                .build();

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                SubscriptionWorker.class,
                24, TimeUnit.HOURS
        )
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context.getApplicationContext()).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );
    }
}
