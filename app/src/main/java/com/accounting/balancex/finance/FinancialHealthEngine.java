package com.accounting.balancex.finance;

import com.accounting.balancex.data.entity.BudgetGoalEntity;
import com.accounting.balancex.data.entity.SubscriptionEntity;

import java.util.List;

public class FinancialHealthEngine {

    public static class HealthScoreResult {
        public final int score;
        public final String ratingLabel;
        public final int savingsRatePercent;
        public final String adviceTip;
        public final String badgeColorHex;

        public HealthScoreResult(int score, String ratingLabel, int savingsRatePercent, String adviceTip, String badgeColorHex) {
            this.score = score;
            this.ratingLabel = ratingLabel;
            this.savingsRatePercent = savingsRatePercent;
            this.adviceTip = adviceTip;
            this.badgeColorHex = badgeColorHex;
        }
    }

    public static HealthScoreResult calculateHealthScore(
            double monthlyIncome,
            double monthlyExpense,
            List<BudgetGoalEntity> budgets,
            List<SubscriptionEntity> subscriptions
    ) {
        int savingsPoints;
        int savingsRate = 0;

        if (monthlyIncome > 0) {
            double saved = monthlyIncome - monthlyExpense;
            savingsRate = (int) Math.round((saved / monthlyIncome) * 100);

            if (savingsRate >= 30) {
                savingsPoints = 40;
            } else if (savingsRate >= 20) {
                savingsPoints = 32;
            } else if (savingsRate >= 10) {
                savingsPoints = 24;
            } else if (savingsRate >= 0) {
                savingsPoints = 15;
            } else {
                savingsPoints = 5;
            }
        } else {
            // No income logged yet this period
            savingsPoints = (monthlyExpense == 0) ? 30 : 10;
        }

        // Budget Adherence (35 points max)
        int budgetPoints = 28;
        int overBudgetCount = 0;
        if (budgets != null && !budgets.isEmpty()) {
            int budgetCount = 0;
            int adheredCount = 0;
            for (BudgetGoalEntity b : budgets) {
                if (BudgetGoalEntity.TYPE_BUDGET.equalsIgnoreCase(b.type)) {
                    budgetCount++;
                    if (b.currentAmount <= b.targetAmount) {
                        adheredCount++;
                    } else {
                        overBudgetCount++;
                    }
                }
            }
            if (budgetCount > 0) {
                budgetPoints = (int) Math.round(((double) adheredCount / budgetCount) * 35.0);
            }
        }

        // Recurring Commitment (25 points max)
        int recurringPoints = 25;
        double totalSubs = 0;
        if (subscriptions != null && !subscriptions.isEmpty()) {
            for (SubscriptionEntity s : subscriptions) {
                if (s.isActive) {
                    if ("Yearly".equalsIgnoreCase(s.billingCycle)) {
                        totalSubs += s.amount / 12.0;
                    } else if ("Weekly".equalsIgnoreCase(s.billingCycle)) {
                        totalSubs += s.amount * 4.33;
                    } else {
                        totalSubs += s.amount;
                    }
                }
            }
        }

        if (monthlyIncome > 0 && totalSubs > 0) {
            double subRatio = (totalSubs / monthlyIncome) * 100;
            if (subRatio <= 15) {
                recurringPoints = 25;
            } else if (subRatio <= 30) {
                recurringPoints = 18;
            } else {
                recurringPoints = 10;
            }
        }

        int totalScore = Math.min(100, Math.max(10, savingsPoints + budgetPoints + recurringPoints));

        String label;
        String tip;
        String colorHex;

        if (totalScore >= 80) {
            label = "Excellent • Strong Saver";
            colorHex = "#059669"; // Emerald
            if (savingsRate > 0) {
                tip = "You saved " + savingsRate + "% of your earnings! Your cashflow is in prime condition.";
            } else {
                tip = "All category budgets and recurring bills are well managed. Keep it up!";
            }
        } else if (totalScore >= 65) {
            label = "Good • Balanced";
            colorHex = "#2563EB"; // Blue
            if (overBudgetCount > 0) {
                tip = overBudgetCount + " category exceeded its target. Trim discretionary spending to push your score past 80.";
            } else {
                tip = "Healthy savings trajectory. Consider increasing your monthly emergency fund contribution.";
            }
        } else if (totalScore >= 50) {
            label = "Fair • Room to Grow";
            colorHex = "#D97706"; // Amber
            tip = "High expense ratio relative to incoming credit. Review subscriptions and dining limits.";
        } else {
            label = "Attention Needed";
            colorHex = "#DC2626"; // Red
            tip = "Monthly debits are currently outrunning credits. Set strict category caps in Budgets to regain momentum.";
        }

        return new HealthScoreResult(totalScore, label, savingsRate, tip, colorHex);
    }
}
