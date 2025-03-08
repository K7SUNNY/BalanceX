package com.accounting.balancex;
//MainActivity recent transaction handler class( model)
public class RecentTransactionModel {
    private String receiver;
    private String date;
    private String amount;
    private String type;
    private long entryId;

    // Constructor
    public RecentTransactionModel(String receiver, String date, String amount, String type, long entryId) {
        this.receiver = receiver;
        this.date = date;
        this.amount = amount;
        this.type = type;
        this.entryId = entryId;
    }

    // Getters
    public String getReceiver() { return receiver; }
    public String getDate() { return date; }
    public String getAmount() { return amount; }
    public String getType() { return type; }
    public long getEntryId() { return entryId; }
}