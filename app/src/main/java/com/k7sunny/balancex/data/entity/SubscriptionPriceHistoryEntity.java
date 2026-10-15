package com.k7sunny.balancex.data.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "subscription_price_history",
    indices = {
        @Index(value = {"subscriptionId"})
    }
)
public class SubscriptionPriceHistoryEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long subscriptionId;
    public String effectiveDate; // yyyy-MM-dd
    public long amountMinor;
    public double amount;
    public long createdAt;

    public SubscriptionPriceHistoryEntity() {
        this.createdAt = System.currentTimeMillis();
    }

    @androidx.room.Ignore
    public SubscriptionPriceHistoryEntity(long subscriptionId, String effectiveDate, long amountMinor, double amount) {
        this();
        this.subscriptionId = subscriptionId;
        this.effectiveDate = effectiveDate;
        this.amountMinor = amountMinor;
        this.amount = amount;
    }
}
