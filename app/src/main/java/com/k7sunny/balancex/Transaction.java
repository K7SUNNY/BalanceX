package com.k7sunny.balancex;

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
    private String transactionId;
    private long amountMinor;
    private Long dateEpochDay;
    private int dateValid;

    // Constructors
    public Transaction(String date, String amount, String receiver, String description,
                       String utr, String comments, String category, String transactionId,
                       String paymentMethod, String transactionType, long entryId) {
        this(date, amount, receiver, description, utr, comments, category, transactionId,
                paymentMethod, transactionType, entryId,
                com.k7sunny.balancex.util.MoneyDateUtils.parseAmountToMinor(amount),
                com.k7sunny.balancex.util.MoneyDateUtils.parseDateToEpochDay(date),
                com.k7sunny.balancex.util.MoneyDateUtils.parseDateToEpochDay(date) != null ? 1 : 0);
    }

    public Transaction(String date, String amount, String receiver, String description,
                       String utr, String comments, String category, String transactionId,
                       String paymentMethod, String transactionType, long entryId,
                       long amountMinor, Long dateEpochDay, int dateValid) {
        this.date = date;
        this.amount = amount;
        this.receiver = receiver;
        this.description = description;
        this.utr = utr;
        this.transactionId = transactionId;
        this.comments = comments;
        this.category = category;
        this.paymentMethod = paymentMethod;
        this.textType = transactionType;
        this.entryId = entryId;
        this.amountMinor = amountMinor;
        this.dateEpochDay = dateEpochDay;
        this.dateValid = dateValid;
    }

    // Getters
    public long getEntryId() { return entryId; }
    public String getDate() { return date; }
    public String getAmount() { return amount; }
    public long getAmountMinor() { return amountMinor; }
    public Long getDateEpochDay() { return dateEpochDay; }
    public int getDateValid() { return dateValid; }
    public boolean isDateValid() { return dateValid == 1; }
    public String getReceiverName() { return receiver; }
    public String getDescription() { return description; }
    public String getUtr() { return utr; }
    public String getTransactionID() { return transactionId; }
    public String getComments() { return comments; }
    public String getCategory() { return category; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getTransactionType() { return textType; }
    public boolean isAutoBilled() {
        return "Auto-Debit".equalsIgnoreCase(paymentMethod) ||
                (description != null && description.toLowerCase(java.util.Locale.ROOT).contains("auto-billed"));
    }
}
