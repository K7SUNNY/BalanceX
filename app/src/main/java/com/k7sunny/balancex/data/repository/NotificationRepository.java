package com.k7sunny.balancex.data.repository;

import android.content.Context;

import com.k7sunny.balancex.data.dao.NotificationDao;
import com.k7sunny.balancex.data.db.AppDatabase;
import com.k7sunny.balancex.data.entity.NotificationEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class NotificationRepository {

    private final NotificationDao notificationDao;
    private final ExecutorService executorService;

    public NotificationRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        this.notificationDao = db.notificationDao();
        this.executorService = AppDatabase.databaseWriteExecutor;
    }

    public interface OnNotificationsLoaded {
        void onLoaded(List<NotificationEntity> items);
    }

    public interface OnUnreadCountLoaded {
        void onLoaded(int count);
    }

    public void insert(NotificationEntity entity, Runnable onSuccess) {
        executorService.execute(() -> {
            notificationDao.insertNotification(entity);
            if (onSuccess != null) onSuccess.run();
        });
    }

    public void insertAll(List<NotificationEntity> entities, Runnable onSuccess) {
        executorService.execute(() -> {
            notificationDao.insertAll(entities);
            if (onSuccess != null) onSuccess.run();
        });
    }

    public void markAsRead(long id, Runnable onSuccess) {
        executorService.execute(() -> {
            notificationDao.markAsRead(id);
            if (onSuccess != null) onSuccess.run();
        });
    }

    public void markAllAsRead(Runnable onSuccess) {
        executorService.execute(() -> {
            notificationDao.markAllAsRead();
            if (onSuccess != null) onSuccess.run();
        });
    }

    public void delete(long id, Runnable onSuccess) {
        executorService.execute(() -> {
            notificationDao.deleteById(id);
            if (onSuccess != null) onSuccess.run();
        });
    }

    public void deleteAll(Runnable onSuccess) {
        executorService.execute(() -> {
            notificationDao.deleteAll();
            if (onSuccess != null) onSuccess.run();
        });
    }

    public void getAll(OnNotificationsLoaded callback) {
        executorService.execute(() -> {
            List<NotificationEntity> list = notificationDao.getAll();
            if (callback != null) callback.onLoaded(list);
        });
    }

    public void getUnread(OnNotificationsLoaded callback) {
        executorService.execute(() -> {
            List<NotificationEntity> list = notificationDao.getUnread();
            if (callback != null) callback.onLoaded(list);
        });
    }

    public void getByType(String type, OnNotificationsLoaded callback) {
        executorService.execute(() -> {
            List<NotificationEntity> list = notificationDao.getByType(type);
            if (callback != null) callback.onLoaded(list);
        });
    }

    public void getByTypes(List<String> types, OnNotificationsLoaded callback) {
        executorService.execute(() -> {
            List<NotificationEntity> list = notificationDao.getByTypes(types);
            if (callback != null) callback.onLoaded(list);
        });
    }

    public void getUnreadCount(OnUnreadCountLoaded callback) {
        executorService.execute(() -> {
            int count = notificationDao.getUnreadCount();
            if (callback != null) callback.onLoaded(count);
        });
    }
}
