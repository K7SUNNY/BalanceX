package com.accounting.balancex.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.accounting.balancex.data.entity.SubscriptionEntity;

import java.util.List;

@Dao
public interface SubscriptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertSubscription(SubscriptionEntity subscription);

    @Update
    void updateSubscription(SubscriptionEntity subscription);

    @Delete
    void deleteSubscription(SubscriptionEntity subscription);

    @Query("DELETE FROM subscriptions WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM subscriptions ORDER BY nextDueDate ASC")
    List<SubscriptionEntity> getAllSubscriptions();

    @Query("SELECT * FROM subscriptions WHERE isActive = 1 ORDER BY nextDueDate ASC")
    List<SubscriptionEntity> getActiveSubscriptions();

    @Query("SELECT * FROM subscriptions WHERE isActive = 1 AND nextDueDate <= :maxDueDate ORDER BY nextDueDate ASC")
    List<SubscriptionEntity> getUpcomingActiveSubscriptions(String maxDueDate);

    @Query("SELECT COUNT(*) FROM subscriptions")
    int getSubscriptionCount();
}
