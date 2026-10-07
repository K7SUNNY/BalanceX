package com.accounting.balancex.data.repository;

import android.content.Context;

import com.accounting.balancex.data.dao.SubscriptionDao;
import com.accounting.balancex.data.db.AppDatabase;
import com.accounting.balancex.data.entity.SubscriptionEntity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SubscriptionRepository {

    private final SubscriptionDao subscriptionDao;
    private final ExecutorService executorService;

    public SubscriptionRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        this.subscriptionDao = db.subscriptionDao();
        this.executorService = Executors.newFixedThreadPool(4);

        android.content.SharedPreferences sp = context.getSharedPreferences("balancex_phase3_prefs", Context.MODE_PRIVATE);
        if (!sp.getBoolean("has_purged_phase3_mock_v1", false)) {
            executorService.execute(() -> {
                List<SubscriptionEntity> subs = subscriptionDao.getAllSubscriptions();
                for (SubscriptionEntity s : subs) {
                    if ("Netflix Premium".equalsIgnoreCase(s.name) ||
                        "Spotify Duo".equalsIgnoreCase(s.name) ||
                        "Google One Cloud (100GB)".equalsIgnoreCase(s.name)) {
                        subscriptionDao.deleteSubscription(s);
                    }
                }
                sp.edit().putBoolean("has_purged_phase3_mock_v1", true).apply();
            });
        }
    }

    public interface OnSubscriptionsLoaded {
        void onLoaded(List<SubscriptionEntity> subscriptions);
    }

    public void insert(SubscriptionEntity subscription, Runnable onSuccess) {
        executorService.execute(() -> {
            subscriptionDao.insertSubscription(subscription);
            if (onSuccess != null) onSuccess.run();
        });
    }

    public void update(SubscriptionEntity subscription, Runnable onSuccess) {
        executorService.execute(() -> {
            subscriptionDao.updateSubscription(subscription);
            if (onSuccess != null) onSuccess.run();
        });
    }

    public void delete(long id, Runnable onSuccess) {
        executorService.execute(() -> {
            subscriptionDao.deleteById(id);
            if (onSuccess != null) onSuccess.run();
        });
    }

    public void getAllSubscriptions(OnSubscriptionsLoaded callback) {
        executorService.execute(() -> {
            List<SubscriptionEntity> list = subscriptionDao.getAllSubscriptions();
            if (callback != null) callback.onLoaded(list);
        });
    }

    public void getActiveSubscriptions(OnSubscriptionsLoaded callback) {
        executorService.execute(() -> {
            List<SubscriptionEntity> list = subscriptionDao.getActiveSubscriptions();
            if (callback != null) callback.onLoaded(list);
        });
    }
}
