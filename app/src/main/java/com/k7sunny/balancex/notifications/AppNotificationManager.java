package com.k7sunny.balancex.notifications;

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

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.k7sunny.balancex.NotificationActivity;
import com.k7sunny.balancex.R;
import com.k7sunny.balancex.SettingsManager;
import com.k7sunny.balancex.data.db.AppDatabase;
import com.k7sunny.balancex.data.entity.BudgetGoalEntity;
import com.k7sunny.balancex.data.entity.NotificationEntity;
import com.k7sunny.balancex.data.entity.SubscriptionEntity;
import com.k7sunny.balancex.data.entity.TransactionEntity;
import com.k7sunny.balancex.data.repository.NotificationRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AppNotificationManager {

    private static final String TAG = "AppNotificationManager";
    public static final String CHANNEL_ID = "balancex_app_notifications";
    private static final String PREF_NOTIF_TRACKER = "balancex_notif_dedup";

    public static void postNotification(Context context, String title, String message, String type,
            String actionTarget, String extraData, boolean showSystemBar) {
        if (context == null)
            return;
        Context appContext = context.getApplicationContext();

        NotificationRepository repo = new NotificationRepository(appContext);
        NotificationEntity entity = new NotificationEntity(title, message, type, actionTarget, extraData);
        repo.insert(entity, null);

        if (showSystemBar) {
            triggerSystemNotification(appContext, title, message, (int) (System.currentTimeMillis() % 100000));
        }
    }

    public static void postTransactionNotification(Context context, TransactionEntity tx) {
        if (context == null || tx == null)
            return;
        Context appContext = context.getApplicationContext();
        String symbol = SettingsManager.getCurrencySymbol(appContext);

        boolean isDebit = "debit".equalsIgnoreCase(tx.textType);
        String title = isDebit ? "Expense Recorded" : "Income Received";
        double amt = 0;
        try {
            amt = Double.parseDouble(tx.amount.replace(",", "").trim());
        } catch (Exception ignored) {
        }

        String category = (tx.category != null && !tx.category.isEmpty()) ? tx.category : "General";
        String message = (isDebit ? "Spent " : "Received ") + symbol + String.format(Locale.getDefault(), "%,.2f", amt)
                + " for " + category + (tx.receiver != null && !tx.receiver.isEmpty() ? " (" + tx.receiver + ")" : "");

        postNotification(appContext, title, message, NotificationEntity.TYPE_TRANSACTION,
                NotificationEntity.ACTION_HISTORY, tx.category, false);

        // Check if this expense pushed a category budget over limit
        if (isDebit) {
            evaluateBudgetThreshold(appContext, category);
        }
    }

    public static void postGoalMilestone(Context context, String goalTitle, double current, double target) {
        if (context == null)
            return;
        Context appContext = context.getApplicationContext();
        String symbol = SettingsManager.getCurrencySymbol(appContext);

        int pct = target > 0 ? (int) Math.round((current / target) * 100) : 0;
        String title;
        String message;

        if (pct >= 100) {
            title = "🎉 Goal Achieved!";
            message = "Congratulations! You reached 100% of your savings goal for " + goalTitle + " (" + symbol
                    + String.format(Locale.getDefault(), "%,.2f", target) + ")!";
        } else {
            title = "Savings Progress: " + pct + "%";
            message = "Added savings to " + goalTitle + ". Current: " + symbol
                    + String.format(Locale.getDefault(), "%,.2f", current) + " of " + symbol
                    + String.format(Locale.getDefault(), "%,.2f", target);
        }

        postNotification(appContext, title, message, NotificationEntity.TYPE_GOAL,
                NotificationEntity.ACTION_GOALS, goalTitle, true);
    }

    public static void postBillReminder(Context context, String billName, double amount, long daysUntilDue,
            boolean isAutoDebit) {
        if (context == null)
            return;
        Context appContext = context.getApplicationContext();
        String symbol = SettingsManager.getCurrencySymbol(appContext);

        String title;
        String message;
        if (isAutoDebit) {
            title = "Auto-Subscription Logged";
            message = billName + " (" + symbol + String.format(Locale.getDefault(), "%,.2f", amount)
                    + ") was automatically recorded in your ledger.";
        } else if (daysUntilDue == 0) {
            title = "Bill Due Today";
            message = billName + " (" + symbol + String.format(Locale.getDefault(), "%,.2f", amount)
                    + ") is due for payment today!";
        } else if (daysUntilDue < 0) {
            title = "Overdue Bill Reminder";
            message = billName + " (" + symbol + String.format(Locale.getDefault(), "%,.2f", amount)
                    + ") is past due date!";
        } else {
            title = "Upcoming Bill Reminder";
            message = billName + " (" + symbol + String.format(Locale.getDefault(), "%,.2f", amount) + ") is due in "
                    + daysUntilDue + " day" + (daysUntilDue > 1 ? "s" : "") + ".";
        }

        postNotification(appContext, title, message, NotificationEntity.TYPE_BILL,
                NotificationEntity.ACTION_SUBSCRIPTIONS, billName, true);
    }

    public static void postReportExported(Context context, String format, String filename) {
        if (context == null)
            return;
        Context appContext = context.getApplicationContext();

        String title = "Statement Exported (" + format + ")";
        String message = "Your " + format + " financial report was generated successfully. Saved to Downloads: "
                + filename;

        postNotification(appContext, title, message, NotificationEntity.TYPE_REPORT,
                NotificationEntity.ACTION_REPORTS, filename, false);
    }

    public static void evaluateBudgetThreshold(Context context, String category) {
        if (context == null || category == null)
            return;
        Context appContext = context.getApplicationContext();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(appContext);
                BudgetGoalEntity budget = db.budgetGoalDao().getBudgetByCategory(category);
                if (budget == null || budget.targetAmount <= 0)
                    return;

                // Calculate current month's spend for this category
                Calendar cal = Calendar.getInstance();
                int currentMonth = cal.get(Calendar.MONTH);
                int currentYear = cal.get(Calendar.YEAR);

                List<TransactionEntity> transactions = db.transactionDao().getAllTransactions();
                double totalSpent = 0;
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                SimpleDateFormat sdfAlt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                if (transactions != null) {
                    for (TransactionEntity t : transactions) {
                        if (!"debit".equalsIgnoreCase(t.textType))
                            continue;
                        if (t.category == null || !t.category.trim().equalsIgnoreCase(category.trim()))
                            continue;

                        Date d = null;
                        try {
                            d = sdf.parse(t.date);
                        } catch (Exception e1) {
                            try {
                                d = sdfAlt.parse(t.date);
                            } catch (Exception ignored) {
                            }
                        }

                        if (d != null) {
                            Calendar txCal = Calendar.getInstance();
                            txCal.setTime(d);
                            if (txCal.get(Calendar.MONTH) == currentMonth && txCal.get(Calendar.YEAR) == currentYear) {
                                try {
                                    totalSpent += Double.parseDouble(t.amount.replace(",", "").trim());
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }
                }

                double pct = (totalSpent / budget.targetAmount) * 100.0;
                String symbol = SettingsManager.getCurrencySymbol(appContext);

                // Dedup key for this category this month
                String dedupKey = "budget_alert_" + category.toLowerCase().trim() + "_" + currentYear + "_"
                        + currentMonth;
                SharedPreferences prefs = appContext.getSharedPreferences(PREF_NOTIF_TRACKER, Context.MODE_PRIVATE);
                int lastAlertLevel = prefs.getInt(dedupKey, 0); // 0 = none, 1 = 80%, 2 = 100%

                if (pct >= 100 && lastAlertLevel < 2) {
                    String title = "⚠️ Budget Exceeded: " + category;
                    String msg = "You've exceeded your monthly " + category + " limit (" + symbol
                            + String.format(Locale.getDefault(), "%,.2f", budget.targetAmount) + ")! Total spent: "
                            + symbol + String.format(Locale.getDefault(), "%,.2f", totalSpent);
                    postNotification(appContext, title, msg, NotificationEntity.TYPE_BUDGET,
                            NotificationEntity.ACTION_GOALS, category, true);
                    prefs.edit().putInt(dedupKey, 2).apply();
                } else if (pct >= 80 && pct < 100 && lastAlertLevel < 1) {
                    String title = "⚠️ Budget Warning: " + category;
                    String msg = "You have used " + Math.round(pct) + "% of your " + category + " budget (" + symbol
                            + String.format(Locale.getDefault(), "%,.2f", totalSpent) + " / " + symbol
                            + String.format(Locale.getDefault(), "%,.2f", budget.targetAmount) + ").";
                    postNotification(appContext, title, msg, NotificationEntity.TYPE_BUDGET,
                            NotificationEntity.ACTION_GOALS, category, true);
                    prefs.edit().putInt(dedupKey, 1).apply();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error evaluating budget threshold", e);
            }
        });
    }

    public static void syncSystemAndActiveAlerts(Context context, Runnable onComplete) {
        if (context == null)
            return;
        Context appContext = context.getApplicationContext();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(appContext);
                SharedPreferences prefs = appContext.getSharedPreferences(PREF_NOTIF_TRACKER, Context.MODE_PRIVATE);

                // Seed initial welcome & intro notifications only once on initial setup, never
                // on clear/dismiss
                boolean hasSeededWelcome = prefs.getBoolean("has_seeded_welcome_notifs", false);
                if (!hasSeededWelcome) {
                    prefs.edit().putBoolean("has_seeded_welcome_notifs", true).apply();
                    NotificationEntity w1 = new NotificationEntity(
                            "Welcome to BalanceX",
                            "Track your daily cashflows, manage recurring bills, and hit your savings goals securely on-device.",
                            NotificationEntity.TYPE_SYSTEM,
                            NotificationEntity.ACTION_ENTRY);
                    NotificationEntity w2 = new NotificationEntity(
                            "Smart Budget Monitoring",
                            "Set monthly spending limits for categories to receive real-time threshold warnings before overspending.",
                            NotificationEntity.TYPE_BUDGET,
                            NotificationEntity.ACTION_GOALS);
                    NotificationEntity w3 = new NotificationEntity(
                            "Bill & Due Reminders",
                            "Track monthly subscriptions with automatic payment logging and proactive due date alerts.",
                            NotificationEntity.TYPE_BILL,
                            NotificationEntity.ACTION_SUBSCRIPTIONS);
                    db.notificationDao().insertNotification(w3);
                    db.notificationDao().insertNotification(w2);
                    db.notificationDao().insertNotification(w1);
                }

                // Check active subscriptions due soon
                List<SubscriptionEntity> subs = db.subscriptionDao().getActiveSubscriptions();
                if (subs != null) {
                    SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Calendar today = Calendar.getInstance();
                    today.set(Calendar.HOUR_OF_DAY, 0);
                    today.set(Calendar.MINUTE, 0);
                    today.set(Calendar.SECOND, 0);
                    today.set(Calendar.MILLISECOND, 0);

                    for (SubscriptionEntity s : subs) {
                        if (s.nextDueDate == null || s.nextDueDate.trim().isEmpty())
                            continue;
                        try {
                            Date dDate = fmt.parse(s.nextDueDate.trim());
                            if (dDate == null)
                                continue;
                            long diff = dDate.getTime() - today.getTimeInMillis();
                            int days = (int) (diff / (1000L * 60 * 60 * 24));

                            String billKey = "bill_notif_" + s.id + "_" + s.nextDueDate;
                            if (days <= s.reminderDaysBefore && !prefs.getBoolean(billKey, false)) {
                                postBillReminder(appContext, s.name, s.amount, days, s.autoAddTransaction);
                                prefs.edit().putBoolean(billKey, true).apply();
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error syncing active alerts", e);
            } finally {
                if (onComplete != null)
                    onComplete.run();
            }
        });
    }

    private static void triggerSystemNotification(Context context, String title, String message, int id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null)
            return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "BalanceX Alerts & Reminders",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Threshold warnings, bill reminders, and financial updates");
            manager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, NotificationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.balancex_white_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(ContextCompat.getColor(context, R.color.color_primary))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        manager.notify(id, builder.build());
    }
}
