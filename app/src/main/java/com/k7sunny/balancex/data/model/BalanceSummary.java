package com.k7sunny.balancex.data.model;

import androidx.room.Ignore;

public class BalanceSummary {
    public long totalCreditMinor;
    public long totalDebitMinor;

    public BalanceSummary() {}

    @Ignore
    public BalanceSummary(long totalCreditMinor, long totalDebitMinor) {
        this.totalCreditMinor = totalCreditMinor;
        this.totalDebitMinor = totalDebitMinor;
    }

    public long getNetBalanceMinor() {
        return totalCreditMinor - totalDebitMinor;
    }
}
