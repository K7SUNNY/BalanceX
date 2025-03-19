package com.accounting.balancex;

public class PieModel {
    private String category;
    private float amount;

    public PieModel(String category, float amount) {
        this.category = category;
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public float getAmount() {
        return amount;
    }
}

