package com.k7sunny.balancex.data.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "reminder_log",
    indices = {
        @Index(value = {"subscriptionId", "occurrenceDate", "kind"}, unique = true)
    }
)
public class ReminderLogEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long subscriptionId;
    public String occurrenceDate; // yyyy-MM-dd
    public String kind; // e.g. "REMINDER", "DUE_TODAY", "OVERDUE", "AUTO_BILLED", "PAID"
    public long loggedAt;

    public ReminderLogEntity() {
        this.loggedAt = System.currentTimeMillis();
    }

    @androidx.room.Ignore
    public ReminderLogEntity(long subscriptionId, String occurrenceDate, String kind) {
        this();
        this.subscriptionId = subscriptionId;
        this.occurrenceDate = occurrenceDate;
        this.kind = kind;
    }
}
