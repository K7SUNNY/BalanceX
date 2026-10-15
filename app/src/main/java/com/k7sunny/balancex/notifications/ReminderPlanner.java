package com.k7sunny.balancex.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.k7sunny.balancex.R;
import com.k7sunny.balancex.SettingsManager;
import com.k7sunny.balancex.data.db.AppDatabase;
import com.k7sunny.balancex.data.entity.NotificationEntity;
import com.k7sunny.balancex.data.entity.ReminderLogEntity;
import com.k7sunny.balancex.data.entity.SubscriptionEntity;
import com.k7sunny.balancex.data.entity.TransactionEntity;
import com.k7sunny.balancex.finance.RecurrenceCalculator;
import com.k7sunny.balancex.receiver.SubscriptionActionReceiver;
import com.k7sunny.balancex.ui.subscriptions.SubscriptionsActivity;
import com.k7sunny.balancex.util.MoneyDateUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

public class ReminderPlanner {

    private static final String TAG = "ReminderPlanner";
    public static final String CHANNEL_ID = "balancex_bill_reminders";

    public static void processSubscriptions(Context context) {
        if (context == null) return;
        Context appContext = context.getApplicationContext();

        try {
            AppDatabase db = AppDatabase.getDatabase(appContext);

            // 1. Prune reminder_log older than 90 days
            long ninetyDaysAgo = System.currentTimeMillis() - (90L * 24L * 60L * 60L * 1000L);
            db.reminderLogDao().pruneOlderThan(ninetyDaysAgo);

            List<SubscriptionEntity> subs = db.subscriptionDao().getActiveSubscriptions();
            if (subs == null || subs.isEmpty()) return;

            LocalDate today = LocalDate.now();
            String currencySymbol = SettingsManager.getCurrencySymbol(appContext);

            for (SubscriptionEntity sub : subs) {
                if (sub.nextDueDate == null || sub.nextDueDate.trim().isEmpty()) {
                    continue;
                }

                // Check paused status
                if (sub.pausedUntil != null && !sub.pausedUntil.trim().isEmpty()) {
                    try {
                        LocalDate pauseDate = LocalDate.parse(sub.pausedUntil.trim());
                        if (pauseDate.isAfter(today)) {
                            continue; // Currently paused
                        }
                    } catch (Exception ignored) {}
                }

                // Check termination date
                if (sub.endDate != null && !sub.endDate.trim().isEmpty()) {
                    try {
                        LocalDate endDate = LocalDate.parse(sub.endDate.trim());
                        if (today.isAfter(endDate)) {
                            sub.isActive = false;
                            db.subscriptionDao().updateSubscription(sub);
                            continue;
                        }
                    } catch (Exception ignored) {}
                }

                LocalDate dueDate;
                try {
                    dueDate = LocalDate.parse(sub.nextDueDate.trim());
                } catch (Exception e) {
                    Log.w(TAG, "Unparseable due date for subscription " + sub.name + ": " + sub.nextDueDate);
                    continue;
                }

                long daysUntil = RecurrenceCalculator.daysUntil(dueDate, today);
                LocalDate anchor;
                try {
                    anchor = LocalDate.parse(sub.anchorDate != null ? sub.anchorDate.trim() : sub.nextDueDate.trim());
                } catch (Exception e) {
                    anchor = dueDate;
                }
                RecurrenceCalculator.Cycle cycle = RecurrenceCalculator.Cycle.fromString(sub.billingCycle);

                // ==================== AUTO-BILLING ====================
                if (sub.autoAddTransaction && daysUntil <= 0) {
                    LocalDate fromDate = dueDate;
                    if (sub.lastPaidDate != null && !sub.lastPaidDate.trim().isEmpty()) {
                        try {
                            fromDate = LocalDate.parse(sub.lastPaidDate.trim()).plusDays(1);
                        } catch (Exception ignored) {}
                    }

                    List<LocalDate> missed = RecurrenceCalculator.occurrencesBetween(
                            anchor, cycle, sub.customInterval, fromDate, today, 24
                    );
                    if (missed.isEmpty() && !dueDate.isAfter(today)) {
                        missed.add(dueDate);
                    }

                    int billedCount = 0;
                    double totalBilledAmount = 0.0;
                    LocalDate lastBilledOcc = null;

                    for (LocalDate occ : missed) {
                        String occStr = occ.toString();
                        boolean alreadyBilled = db.transactionDao().hasSubscriptionOccurrence(sub.id, occStr);
                        if (!alreadyBilled) {
                            TransactionEntity tx = new TransactionEntity();
                            tx.date = occStr;
                            tx.amount = String.format(Locale.ROOT, "%.2f", sub.amount);
                            tx.amountMinor = sub.amountMinor > 0 ? sub.amountMinor : Math.round(sub.amount * 100.0);
                            tx.dateEpochDay = occ.toEpochDay();
                            tx.dateValid = 1;
                            tx.receiver = sub.name;
                            tx.description = "Auto-billed recurring: " + sub.name;
                            tx.category = (sub.category != null && !sub.category.trim().isEmpty()) ? sub.category : "Subscription";
                            tx.paymentMethod = (sub.paymentMethod != null && !sub.paymentMethod.trim().isEmpty()) ? sub.paymentMethod : "Auto-Debit";
                            tx.textType = "Debit";
                            tx.subscriptionId = sub.id;
                            tx.occurrenceDate = occStr;

                            try {
                                db.transactionDao().insert(tx);
                                db.reminderLogDao().insert(new ReminderLogEntity(sub.id, occStr, "AUTO_BILLED"));
                                billedCount++;
                                totalBilledAmount += sub.amount;
                                lastBilledOcc = occ;
                            } catch (Exception e) {
                                Log.w(TAG, "Failed inserting auto-billed transaction for " + sub.name, e);
                            }
                        }
                    }

                    // Advance nextDueDate from anchor past today
                    LocalDate nextDue = RecurrenceCalculator.nextOccurrenceAfter(anchor, cycle, sub.customInterval, today);
                    sub.nextDueDate = nextDue.toString();
                    if (lastBilledOcc != null) {
                        sub.lastPaidDate = lastBilledOcc.toString();
                    }
                    db.subscriptionDao().updateSubscription(sub);

                    if (billedCount > 0) {
                        String notifTitle = "Auto-Subscription Logged";
                        String notifMsg = (billedCount == 1)
                                ? "Recorded " + currencySymbol + String.format(Locale.getDefault(), "%,.2f", sub.amount) + " for " + sub.name
                                : "Recorded " + billedCount + " catch-up payment(s) totaling " + currencySymbol + String.format(Locale.getDefault(), "%,.2f", totalBilledAmount) + " for " + sub.name;

                        // Insert In-app notification
                        NotificationEntity inApp = new NotificationEntity(
                                notifTitle,
                                notifMsg,
                                NotificationEntity.TYPE_BILL,
                                NotificationEntity.ACTION_SUBSCRIPTIONS,
                                sub.name
                        );
                        db.notificationDao().insertNotification(inApp);

                        // Post single system notification (no action buttons needed for auto-pay receipt)
                        int notifId = getNotificationId(sub.id, sub.nextDueDate);
                        postSystemNotification(appContext, notifId, notifTitle, notifMsg, null, null);
                    }
                    continue;
                }

                // ==================== MANUAL BILL REMINDER ====================
                if (!sub.autoAddTransaction) {
                    String kind = null;
                    if (daysUntil <= 0) {
                        kind = (daysUntil == 0) ? "DUE_TODAY" : "OVERDUE";
                    } else if (daysUntil <= sub.reminderDaysBefore) {
                        kind = "REMIND_BEFORE";
                    }

                    if (kind == null) continue;

                    String currentDueDateStr = sub.nextDueDate.trim();

                    // Check if already paid or skipped for this occurrence
                    if (db.reminderLogDao().hasLog(sub.id, currentDueDateStr, "PAID") ||
                        db.reminderLogDao().hasLog(sub.id, currentDueDateStr, "SKIPPED")) {
                        continue;
                    }

                    // Check if snoozed in the last 24 hours
                    if (db.reminderLogDao().hasLog(sub.id, currentDueDateStr, "SNOOZED_1D")) {
                        continue;
                    }

                    // Check if already notified with this kind for this occurrence
                    if (db.reminderLogDao().hasLog(sub.id, currentDueDateStr, kind)) {
                        continue;
                    }

                    // Compose notification
                    String formattedAmt = currencySymbol + String.format(Locale.getDefault(), "%,.2f", sub.amount);
                    String notifTitle;
                    String notifMsg;

                    if (daysUntil == 0) {
                        notifTitle = "Bill Due Today: " + sub.name;
                        notifMsg = sub.name + " (" + formattedAmt + ") is due for payment today.";
                    } else if (daysUntil < 0) {
                        notifTitle = "Overdue Bill: " + sub.name;
                        notifMsg = sub.name + " (" + formattedAmt + ") was due " + Math.abs(daysUntil) + " days ago!";
                    } else if (daysUntil == 1) {
                        notifTitle = "Upcoming Bill: " + sub.name;
                        notifMsg = sub.name + " (" + formattedAmt + ") is due tomorrow.";
                    } else {
                        notifTitle = "Upcoming Bill: " + sub.name;
                        notifMsg = sub.name + " (" + formattedAmt + ") is due in " + daysUntil + " days.";
                    }

                    // Record in reminder_log
                    db.reminderLogDao().insert(new ReminderLogEntity(sub.id, currentDueDateStr, kind));

                    // Insert In-App Notification
                    NotificationEntity inApp = new NotificationEntity(
                            notifTitle,
                            notifMsg,
                            NotificationEntity.TYPE_BILL,
                            NotificationEntity.ACTION_SUBSCRIPTIONS,
                            sub.name
                    );
                    db.notificationDao().insertNotification(inApp);

                    // Post system notification with Action Buttons
                    int notifId = getNotificationId(sub.id, currentDueDateStr);

                    Intent markPaidIntent = new Intent(appContext, SubscriptionActionReceiver.class);
                    markPaidIntent.setAction(SubscriptionActionReceiver.ACTION_MARK_PAID);
                    markPaidIntent.putExtra(SubscriptionActionReceiver.EXTRA_SUBSCRIPTION_ID, sub.id);
                    markPaidIntent.putExtra(SubscriptionActionReceiver.EXTRA_OCCURRENCE_DATE, currentDueDateStr);
                    markPaidIntent.putExtra(SubscriptionActionReceiver.EXTRA_NOTIFICATION_ID, notifId);
                    markPaidIntent.putExtra(SubscriptionActionReceiver.EXTRA_SUBSCRIPTION_NAME, sub.name);

                    PendingIntent markPaidPending = PendingIntent.getBroadcast(
                            appContext,
                            notifId * 3 + 1,
                            markPaidIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );

                    Intent snoozeIntent = new Intent(appContext, SubscriptionActionReceiver.class);
                    snoozeIntent.setAction(SubscriptionActionReceiver.ACTION_SNOOZE_ONE_DAY);
                    snoozeIntent.putExtra(SubscriptionActionReceiver.EXTRA_SUBSCRIPTION_ID, sub.id);
                    snoozeIntent.putExtra(SubscriptionActionReceiver.EXTRA_OCCURRENCE_DATE, currentDueDateStr);
                    snoozeIntent.putExtra(SubscriptionActionReceiver.EXTRA_NOTIFICATION_ID, notifId);

                    PendingIntent snoozePending = PendingIntent.getBroadcast(
                            appContext,
                            notifId * 3 + 2,
                            snoozeIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );

                    postSystemNotification(appContext, notifId, notifTitle, notifMsg, markPaidPending, snoozePending);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error executing ReminderPlanner", e);
        }
    }

    public static int getNotificationId(long subscriptionId, String occurrenceDate) {
        int hash = (occurrenceDate != null) ? occurrenceDate.hashCode() : 0;
        return (int) Math.abs((subscriptionId * 10007L + hash) % Integer.MAX_VALUE);
    }

    private static void postSystemNotification(
            Context context,
            int notifId,
            String title,
            String message,
            PendingIntent markPaidAction,
            PendingIntent snoozeAction
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
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
            channel.setDescription("Upcoming bill alerts and recurring payment reminders");
            manager.createNotificationChannel(channel);
        }

        Intent tapIntent = new Intent(context, SubscriptionsActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent tapPendingIntent = PendingIntent.getActivity(
                context,
                notifId,
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        boolean showAmounts = SettingsManager.isShowAmountsInNotifications(context);
        String displayTitle = showAmounts ? title : AppNotificationManager.maskAmountsInText(title);
        String displayMsg = showAmounts ? message : AppNotificationManager.maskAmountsInText(message);

        NotificationCompat.Builder publicBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.balancex_white_logo)
                .setContentTitle("BalanceX Bill Reminder")
                .setContentText("You have a scheduled payment due")
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setColor(ContextCompat.getColor(context, R.color.color_primary))
                .setAutoCancel(true)
                .setContentIntent(tapPendingIntent);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.balancex_white_logo)
                .setContentTitle(displayTitle)
                .setContentText(displayMsg)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(displayMsg))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(publicBuilder.build())
                .setColor(ContextCompat.getColor(context, R.color.color_primary))
                .setAutoCancel(true)
                .setContentIntent(tapPendingIntent);

        if (markPaidAction != null) {
            builder.addAction(R.drawable.ic_payment, "Mark Paid", markPaidAction);
        }
        if (snoozeAction != null) {
            builder.addAction(R.drawable.ic_calendar, "Snooze 1d", snoozeAction);
        }

        manager.notify(notifId, builder.build());
    }
}
