package com.k7sunny.balancex.receiver;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.k7sunny.balancex.data.db.AppDatabase;
import com.k7sunny.balancex.data.entity.ReminderLogEntity;
import com.k7sunny.balancex.data.repository.SubscriptionRepository;

import java.time.LocalDate;

public class SubscriptionActionReceiver extends BroadcastReceiver {

    private static final String TAG = "SubscriptionActionRec";

    public static final String ACTION_MARK_PAID = "com.k7sunny.balancex.action.MARK_PAID";
    public static final String ACTION_SNOOZE_ONE_DAY = "com.k7sunny.balancex.action.SNOOZE_ONE_DAY";

    public static final String EXTRA_SUBSCRIPTION_ID = "extra_subscription_id";
    public static final String EXTRA_OCCURRENCE_DATE = "extra_occurrence_date";
    public static final String EXTRA_NOTIFICATION_ID = "extra_notification_id";
    public static final String EXTRA_SUBSCRIPTION_NAME = "extra_subscription_name";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();

        long subId = intent.getLongExtra(EXTRA_SUBSCRIPTION_ID, -1);
        String occurrenceDate = intent.getStringExtra(EXTRA_OCCURRENCE_DATE);
        int notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1);
        String subName = intent.getStringExtra(EXTRA_SUBSCRIPTION_NAME);

        // Cancel the system notification
        if (notifId != -1) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.cancel(notifId);
            }
        }

        if (subId == -1) return;

        SubscriptionRepository repo = new SubscriptionRepository(context);

        if (ACTION_MARK_PAID.equals(action)) {
            Log.d(TAG, "Notification action: Mark Paid for subId=" + subId + ", date=" + occurrenceDate);
            repo.markAsPaid(subId, occurrenceDate, null, null);
        } else if (ACTION_SNOOZE_ONE_DAY.equals(action)) {
            Log.d(TAG, "Notification action: Snooze 1 Day for subId=" + subId + ", date=" + occurrenceDate);
            AppDatabase.databaseWriteExecutor.execute(() -> {
                try {
                    AppDatabase db = AppDatabase.getDatabase(context);
                    String date = (occurrenceDate != null) ? occurrenceDate : LocalDate.now().toString();
                    db.reminderLogDao().insert(new ReminderLogEntity(subId, date, "SNOOZED_1D"));
                } catch (Exception e) {
                    Log.e(TAG, "Failed to record snooze log", e);
                }
            });
        }
    }
}
