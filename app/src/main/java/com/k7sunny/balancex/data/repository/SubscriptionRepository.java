package com.k7sunny.balancex.data.repository;

import android.content.Context;

import com.k7sunny.balancex.data.dao.SubscriptionDao;
import com.k7sunny.balancex.data.db.AppDatabase;
import com.k7sunny.balancex.data.entity.SubscriptionEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class SubscriptionRepository {

    private final SubscriptionDao subscriptionDao;
    private final ExecutorService executorService;

    public SubscriptionRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        this.subscriptionDao = db.subscriptionDao();
        this.executorService = AppDatabase.databaseWriteExecutor;
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

    public void deleteAll(Runnable onSuccess) {
        executorService.execute(() -> {
            subscriptionDao.deleteAll();
            if (onSuccess != null) onSuccess.run();
        });
    }
}
