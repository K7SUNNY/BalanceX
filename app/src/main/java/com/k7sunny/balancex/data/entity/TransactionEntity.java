package com.k7sunny.balancex.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions")
public class TransactionEntity {
    @PrimaryKey(autoGenerate = true)
    public long entryId;

    public String date;
    public String amount;
    public String receiver;
    public String description;
    public String utr;
    public String transactionId;
    public String comments;
    public String category;
    public String paymentMethod;
    public String textType;
}
