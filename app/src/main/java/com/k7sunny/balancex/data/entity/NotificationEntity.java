package com.k7sunny.balancex.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "notifications")
public class NotificationEntity {

    public static final String TYPE_TRANSACTION = "TRANSACTION";
    public static final String TYPE_BUDGET = "BUDGET";
    public static final String TYPE_GOAL = "GOAL";
    public static final String TYPE_BILL = "BILL";
    public static final String TYPE_REMINDER = "REMINDER";
    public static final String TYPE_REPORT = "REPORT";
    public static final String TYPE_SYSTEM = "SYSTEM";

    // Action Deep Links / Navigation Targets
    public static final String ACTION_HISTORY = "ACTION_HISTORY";
    public static final String ACTION_GOALS = "ACTION_GOALS";
    public static final String ACTION_SUBSCRIPTIONS = "ACTION_SUBSCRIPTIONS";
    public static final String ACTION_REPORTS = "ACTION_REPORTS";
    public static final String ACTION_ENTRY = "ACTION_ENTRY";

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String title;
    public String message;
    public long timestamp;
    public String type;
    public boolean isRead;
    public String actionTarget;
    public String extraData;

    public NotificationEntity() {
        this.timestamp = System.currentTimeMillis();
        this.isRead = false;
        this.type = TYPE_SYSTEM;
    }

    @Ignore
    public NotificationEntity(String title, String message, String type, String actionTarget, String extraData) {
        this();
        this.title = title;
        this.message = message;
        this.type = type != null ? type : TYPE_SYSTEM;
        this.actionTarget = actionTarget;
        this.extraData = extraData;
        this.isRead = false;
    }

    @Ignore
    public NotificationEntity(String title, String message, String type, String actionTarget) {
        this(title, message, type, actionTarget, null);
    }
}
