package com.k7sunny.balancex.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import androidx.room.Ignore;

@Entity(tableName = "subscriptions")
public class SubscriptionEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;
    public double amount;
    public String billingCycle; // e.g. "Monthly", "Yearly", "Weekly"
    public String nextDueDate;   // e.g. "2026-10-15" (yyyy-MM-dd)
    public String category;      // e.g. "Entertainment", "Utilities"
    public int reminderDaysBefore; // e.g. 2
    public boolean autoAddTransaction;
    public boolean isActive;
    public long createdAt;

    public SubscriptionEntity() {
        this.isActive = true;
        this.reminderDaysBefore = 2;
        this.createdAt = System.currentTimeMillis();
    }

    @Ignore
    public SubscriptionEntity(String name, double amount, String billingCycle, String nextDueDate, String category) {
        this();
        this.name = name;
        this.amount = amount;
        this.billingCycle = billingCycle;
        this.nextDueDate = nextDueDate;
        this.category = category;
    }
}
