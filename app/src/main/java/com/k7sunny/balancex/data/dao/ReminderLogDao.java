package com.k7sunny.balancex.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.k7sunny.balancex.data.entity.ReminderLogEntity;

@Dao
public interface ReminderLogDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(ReminderLogEntity log);

    @Query("SELECT EXISTS(SELECT 1 FROM reminder_log WHERE subscriptionId = :subscriptionId AND occurrenceDate = :occurrenceDate AND kind = :kind LIMIT 1)")
    boolean hasLog(long subscriptionId, String occurrenceDate, String kind);

    @Query("DELETE FROM reminder_log WHERE subscriptionId = :subscriptionId AND occurrenceDate = :occurrenceDate")
    void deleteForOccurrence(long subscriptionId, String occurrenceDate);

    @Query("DELETE FROM reminder_log WHERE loggedAt < :thresholdMillis")
    int pruneOlderThan(long thresholdMillis);

    @Query("DELETE FROM reminder_log WHERE subscriptionId = :subscriptionId")
    void deleteBySubscriptionId(long subscriptionId);
}
