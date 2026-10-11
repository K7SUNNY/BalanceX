package com.k7sunny.balancex.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions")
public class TransactionEntity {
    @PrimaryKey(autoGenerate = true)
    public long entryId;

    public String date;
    public String amount; // Kept for rollback safety, new code uses amountMinor
    public long amountMinor; // Paise/cents (minor currency units)
    public Long dateEpochDay; // Epoch day (LocalDate.toEpochDay()), null if invalid
    public int dateValid; // 1 = valid date, 0 = unparseable / undated
    public String receiver;
    public String description;
    public String utr;
    public String transactionId;
    public String comments;
    public String category;
    public String paymentMethod;
    public String textType;
}
