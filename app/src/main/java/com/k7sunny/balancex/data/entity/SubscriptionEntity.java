package com.k7sunny.balancex.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "subscriptions")
public class SubscriptionEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;
    public double amount; // Legacy convenience, synced with amountMinor
    public long amountMinor; // Exact minor currency units (paise/cents)
    public String billingCycle; // e.g. "Monthly", "Yearly", "Weekly", "Quarterly", "Custom_Days", "Custom_Months"
    public int customInterval; // e.g. 10 days, 2 months (used when cycle is custom)
    public String anchorDate; // Immutable starting date (yyyy-MM-dd) preventing drift
    public String nextDueDate; // Next occurrence date (yyyy-MM-dd)
    public String lastPaidDate; // Date of last recorded payment (yyyy-MM-dd)
    public String endDate; // Optional termination date (yyyy-MM-dd)
    public String pausedUntil; // Optional pause horizon (yyyy-MM-dd)
    public String trialEndsOn; // Optional free trial end date (yyyy-MM-dd)
    public String category; // e.g. "Entertainment", "Utilities", "Rent"
    public String paymentMethod; // e.g. "UPI", "Credit Card", "Debit Card", "Net Banking"
    public int reminderDaysBefore; // e.g. 2
    public boolean autoAddTransaction;
    public boolean isActive;
    public long createdAt;

    public SubscriptionEntity() {
        this.isActive = true;
        this.customInterval = 1;
        this.reminderDaysBefore = 2;
        this.createdAt = System.currentTimeMillis();
    }

    @Ignore
    public SubscriptionEntity(String name, double amount, String billingCycle, String nextDueDate, String category) {
        this();
        this.name = name;
        this.amount = amount;
        this.amountMinor = Math.round(amount * 100.0);
        this.billingCycle = billingCycle;
        this.anchorDate = nextDueDate;
        this.nextDueDate = nextDueDate;
        this.category = category;
        this.paymentMethod = "UPI";
    }
}
