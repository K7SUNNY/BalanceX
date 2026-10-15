package com.k7sunny.balancex.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.k7sunny.balancex.data.entity.SubscriptionPriceHistoryEntity;

import java.util.List;

@Dao
public interface SubscriptionPriceHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(SubscriptionPriceHistoryEntity entity);

    @Query("SELECT * FROM subscription_price_history WHERE subscriptionId = :subscriptionId ORDER BY effectiveDate DESC")
    List<SubscriptionPriceHistoryEntity> getHistoryForSubscription(long subscriptionId);

    @Query("DELETE FROM subscription_price_history WHERE subscriptionId = :subscriptionId")
    void deleteBySubscriptionId(long subscriptionId);
}
