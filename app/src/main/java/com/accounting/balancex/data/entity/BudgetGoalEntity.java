package com.accounting.balancex.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import androidx.room.Ignore;

@Entity(tableName = "budget_goals")
public class BudgetGoalEntity {
    public static final String TYPE_BUDGET = "BUDGET";
    public static final String TYPE_GOAL = "GOAL";

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String title;
    public double targetAmount;
    public double currentAmount;
    public String category;     // e.g. "Food", "Shopping", "Savings"
    public String type;         // TYPE_BUDGET or TYPE_GOAL
    public String period;       // "MONTHLY", "ONE_TIME"
    public String colorHex;     // e.g. "#2563EB"
    public long createdAt;

    public BudgetGoalEntity() {
        this.createdAt = System.currentTimeMillis();
        this.type = TYPE_BUDGET;
        this.period = "MONTHLY";
        this.colorHex = "#2563EB";
    }

    @Ignore
    public BudgetGoalEntity(String title, double targetAmount, double currentAmount, String category, String type, String colorHex) {
        this();
        this.title = title;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.category = category;
        this.type = type;
        this.colorHex = colorHex;
    }
}
