package com.k7sunny.balancex.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.k7sunny.balancex.data.entity.NotificationEntity;

import java.util.List;

@Dao
public interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertNotification(NotificationEntity notification);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<NotificationEntity> notifications);

    @Update
    void updateNotification(NotificationEntity notification);

    @Delete
    void deleteNotification(NotificationEntity notification);

    @Query("DELETE FROM notifications WHERE id = :id")
    void deleteById(long id);

    @Query("DELETE FROM notifications")
    void deleteAll();

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    List<NotificationEntity> getAll();

    @Query("SELECT * FROM notifications WHERE isRead = 0 ORDER BY timestamp DESC")
    List<NotificationEntity> getUnread();

    @Query("SELECT * FROM notifications WHERE type = :type ORDER BY timestamp DESC")
    List<NotificationEntity> getByType(String type);

    @Query("SELECT * FROM notifications WHERE type IN (:types) ORDER BY timestamp DESC")
    List<NotificationEntity> getByTypes(List<String> types);

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    int getUnreadCount();

    @Query("SELECT COUNT(*) FROM notifications")
    int getTotalCount();

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    void markAsRead(long id);

    @Query("UPDATE notifications SET isRead = 1 WHERE isRead = 0")
    void markAllAsRead();
}
