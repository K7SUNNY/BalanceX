package com.k7sunny.balancex.data.model;

import androidx.room.Ignore;

public class CategorySummary {
    public String category;
    public long totalAmountMinor;

    public CategorySummary() {}

    @Ignore
    public CategorySummary(String category, long totalAmountMinor) {
        this.category = category;
        this.totalAmountMinor = totalAmountMinor;
    }
}
