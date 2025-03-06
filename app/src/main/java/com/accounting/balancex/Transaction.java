package com.accounting.balancex;

public class Transaction {
    private String date;
    private String amount;  // Ensure it's a String to match JSON
    private String receiver;  // Match "receiver" instead of "receiverName"
    private String description;
    private String utr;
    private String comments;
    private String category;
    private String paymentMethod;
    private String textType;
    private long entryId;

    // Constructor
    public Transaction(String date, String amount, String receiver, String description,
                       String utr, String comments, String category,
                       String paymentMethod, String transactionType, long entryId) {
        this.date = date;
        this.amount = amount;
        this.receiver = receiver;
        this.description = description;
        this.utr = utr;
        this.comments = comments;
        this.category = category;
        this.paymentMethod = paymentMethod;
        this.textType = transactionType;
        this.entryId = entryId;
    }

    // Getters
    public String getDate() { return date; }
    public String getAmount() { return amount; }
    public String getReceiverName() { return receiver; }
    public String getDescription() { return description; }
    public String getUtr() { return utr; }
    public String getComments() { return comments; }
    public String getCategory() { return category; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getTransactionType() { return textType; }
    public long getEntryId() { return entryId; }
}
