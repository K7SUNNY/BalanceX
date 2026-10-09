package com.k7sunny.balancex.worker;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.k7sunny.balancex.MainActivity;
import com.k7sunny.balancex.R;
import com.k7sunny.balancex.SettingsManager;
import com.k7sunny.balancex.data.db.AppDatabase;
import com.k7sunny.balancex.data.entity.SubscriptionEntity;
import com.k7sunny.balancex.data.entity.TransactionEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
            SharedPreferences prefs = context.getSharedPreferences("subscription_reminders", Context.MODE_PRIVATE);

            for (SubscriptionEntity sub : activeSubs) {
                if (sub.nextDueDate == null || sub.nextDueDate.trim().isEmpty()) {
                    continue;
                }

                try {
                    Date dueDate = sdf.parse(sub.nextDueDate.trim());
                    if (dueDate == null) continue;

                    long diffMillis = dueDate.getTime() - today.getTime();
                    long daysUntilDue = TimeUnit.MILLISECONDS.toDays(diffMillis);
                    String trimmedDueDate = sub.nextDueDate.trim();

                    // 1. If due today or past and autoAddTransaction is enabled, auto-bill and notify receipt
                    if (daysUntilDue <= 0 && sub.autoAddTransaction) {
                        List<String> missedDates = calculateCatchUpDueDates(dueDate, today, sub.billingCycle);
                        int billedCount = 0;
                        double totalBilledAmount = 0;

                        for (String cycleDateStr : missedDates) {
                            String desc = "Auto-billed recurring: " + sub.name;
                            boolean alreadyExists = db.transactionDao().hasAutoBilledTransaction(sub.name, cycleDateStr, desc);

                            if (!alreadyExists) {
                                TransactionEntity tx = new TransactionEntity();
                                tx.date = cycleDateStr;
                                tx.amount = String.format(Locale.getDefault(), "%.2f", sub.amount);
                                tx.receiver = sub.name;
                                tx.description = desc;
                                tx.category = sub.category != null ? sub.category : "Subscription";
                                tx.paymentMethod = "Auto-Debit";
                                tx.textType = "debit";
                                db.transactionDao().insert(tx);
                                billedCount++;
                                totalBilledAmount += sub.amount;
                            }
                        }

                        // Advance nextDueDate to future cycle
                        sub.nextDueDate = calculateNextCycleDate(dueDate, today, sub.billingCycle);
                        db.subscriptionDao().updateSubscription(sub);

                        if (billedCount > 0) {
                            String notifMsg = (billedCount == 1)
                                    ? "Logged " + symbol + String.format(Locale.getDefault(), "%,.2f", sub.amount) + " for " + sub.name
                                    : "Logged " + billedCount + " catch-up payment(s) totaling " + symbol + String.format(Locale.getDefault(), "%,.2f", totalBilledAmount) + " for " + sub.name;
                            showNotification(context, (int) sub.id, "Auto-Subscription Logged", notifMsg);
                            com.k7sunny.balancex.notifications.AppNotificationManager.postNotification(
                                    context, "Auto-Subscription Logged", notifMsg,
                                    com.k7sunny.balancex.data.entity.NotificationEntity.TYPE_BILL,
                                    com.k7sunny.balancex.data.entity.NotificationEntity.ACTION_SUBSCRIPTIONS,
                                    sub.name, false
                            );
                        }
                    } else if (daysUntilDue > 0 && daysUntilDue <= sub.reminderDaysBefore) {
                        // 2. Pre-due reminder window: alert once per billing cycle
                        String reminderKey = "reminded_" + sub.id + "_" + trimmedDueDate;
                        if (!prefs.getBoolean(reminderKey, false)) {
                            String msg;
                            if (daysUntilDue == 1) {
                                msg = sub.name + " (" + symbol + String.format(Locale.getDefault(), "%,.2f", sub.amount) + ") is due tomorrow!";
                            } else {
                                msg = sub.name + " (" + symbol + String.format(Locale.getDefault(), "%,.2f", sub.amount) + ") is due in " + daysUntilDue + " days.";
                            }
                            showNotification(context, (int) sub.id, "Upcoming Bill Reminder", msg);
                            com.k7sunny.balancex.notifications.AppNotificationManager.postBillReminder(context, sub.name, sub.amount, daysUntilDue, false);
                            prefs.edit().putBoolean(reminderKey, true).apply();
                        }
                    } else if (daysUntilDue <= 0 && !sub.autoAddTransaction) {
                        // 3. Due day / past due reminder for manual bills: alert once per billing cycle
                        String dueKey = "due_reminded_" + sub.id + "_" + trimmedDueDate;
                        if (!prefs.getBoolean(dueKey, false)) {
                            String msg;
                            if (daysUntilDue == 0) {
                                msg = sub.name + " (" + symbol + String.format(Locale.getDefault(), "%,.2f", sub.amount) + ") is due today!";
                            } else {
                                msg = sub.name + " (" + symbol + String.format(Locale.getDefault(), "%,.2f", sub.amount) + ") is past due!";
                            }
                            showNotification(context, (int) sub.id, "Bill Payment Reminder", msg);
                            com.k7sunny.balancex.notifications.AppNotificationManager.postBillReminder(context, sub.name, sub.amount, daysUntilDue, false);
                            prefs.edit().putBoolean(dueKey, true).apply();
                        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Cannot show notification: POST_NOTIFICATIONS permission not granted");
                return;
            }
        }

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
                .setSmallIcon(R.drawable.balancex_white_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(ContextCompat.getColor(context, R.color.color_primary))
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

    public static List<String> calculateCatchUpDueDates(Date dueDate, Date today, String billingCycle) {
        List<String> dates = new ArrayList<>();
        if (dueDate == null || today == null) return dates;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cycleCal = Calendar.getInstance();
        cycleCal.setTime(dueDate);

        Calendar todayCal = Calendar.getInstance();
        todayCal.setTime(today);
        todayCal.set(Calendar.HOUR_OF_DAY, 0);
        todayCal.set(Calendar.MINUTE, 0);
        todayCal.set(Calendar.SECOND, 0);
        todayCal.set(Calendar.MILLISECOND, 0);

        int safetyLimit = 50;
        while (!cycleCal.after(todayCal) && safetyLimit-- > 0) {
            dates.add(sdf.format(cycleCal.getTime()));
            if ("Yearly".equalsIgnoreCase(billingCycle)) {
                cycleCal.add(Calendar.YEAR, 1);
            } else if ("Weekly".equalsIgnoreCase(billingCycle)) {
                cycleCal.add(Calendar.WEEK_OF_YEAR, 1);
            } else {
                cycleCal.add(Calendar.MONTH, 1);
            }
        }
        return dates;
    }

    public static String calculateNextCycleDate(Date dueDate, Date today, String billingCycle) {
        if (dueDate == null || today == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cycleCal = Calendar.getInstance();
        cycleCal.setTime(dueDate);

        Calendar todayCal = Calendar.getInstance();
        todayCal.setTime(today);
        todayCal.set(Calendar.HOUR_OF_DAY, 0);
        todayCal.set(Calendar.MINUTE, 0);
        todayCal.set(Calendar.SECOND, 0);
        todayCal.set(Calendar.MILLISECOND, 0);

        int safetyLimit = 50;
        while (!cycleCal.after(todayCal) && safetyLimit-- > 0) {
            if ("Yearly".equalsIgnoreCase(billingCycle)) {
                cycleCal.add(Calendar.YEAR, 1);
            } else if ("Weekly".equalsIgnoreCase(billingCycle)) {
                cycleCal.add(Calendar.WEEK_OF_YEAR, 1);
            } else {
                cycleCal.add(Calendar.MONTH, 1);
            }
        }
        return sdf.format(cycleCal.getTime());
    }
}
