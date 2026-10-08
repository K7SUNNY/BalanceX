package com.accounting.balancex.finance;

import com.accounting.balancex.data.entity.BudgetGoalEntity;
import com.accounting.balancex.data.entity.SubscriptionEntity;
import com.accounting.balancex.data.entity.TransactionEntity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FinancialHealthEngine {

    public static class HealthScoreResult {
        public final int score;
        public final String ratingLabel;
        public final int savingsRatePercent;
        public final String adviceTip;
        public final String badgeColorHex;
        public final boolean hasData;
        public final int savingsPoints;
        public final String savingsDesc;
        public final int budgetPoints;
        public final String budgetDesc;
        public final int recurringPoints;
        public final String recurringDesc;

        public HealthScoreResult(
                int score,
                String ratingLabel,
                int savingsRatePercent,
                String adviceTip,
                String badgeColorHex,
                boolean hasData,
                int savingsPoints,
                String savingsDesc,
                int budgetPoints,
                String budgetDesc,
                int recurringPoints,
                String recurringDesc
        ) {
            this.score = score;
            this.ratingLabel = ratingLabel;
            this.savingsRatePercent = savingsRatePercent;
            this.adviceTip = adviceTip;
            this.badgeColorHex = badgeColorHex;
            this.hasData = hasData;
            this.savingsPoints = savingsPoints;
            this.savingsDesc = savingsDesc;
            this.budgetPoints = budgetPoints;
            this.budgetDesc = budgetDesc;
            this.recurringPoints = recurringPoints;
            this.recurringDesc = recurringDesc;
        }

        public HealthScoreResult(int score, String ratingLabel, int savingsRatePercent, String adviceTip, String badgeColorHex, boolean hasData) {
            this(score, ratingLabel, savingsRatePercent, adviceTip, badgeColorHex, hasData, 0, "", 0, "", 0, "");
        }

        public HealthScoreResult(int score, String ratingLabel, int savingsRatePercent, String adviceTip, String badgeColorHex) {
            this(score, ratingLabel, savingsRatePercent, adviceTip, badgeColorHex, true);
        }
    }

    public static HealthScoreResult calculateHealthScore(
            double monthlyIncome,
            double monthlyExpense,
            List<BudgetGoalEntity> budgets,
            List<SubscriptionEntity> subscriptions
    ) {
        if (monthlyIncome <= 0 && monthlyExpense <= 0) {
            return new HealthScoreResult(
                    0,
                    "Not Enough Data",
                    0,
                    "Log your income and expenses to unlock your personalized Financial Health Score.",
                    "#64748B",
                    false,
                    0,
                    "Log income and expenses to track your savings rate.",
                    0,
                    "Set category caps in Budgets to measure adherence.",
                    0,
                    "Add recurring subscriptions to monitor fixed costs."
            );
        }

        int savingsPoints;
        int savingsRate = 0;
        String savingsDesc;

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

            if (savingsRate > 0) {
                savingsDesc = "Saved " + savingsRate + "% of incoming credit this period.";
            } else if (savingsRate == 0) {
                savingsDesc = "100% of incoming credit was spent this period.";
            } else {
                savingsDesc = "Debits exceeded credits by " + Math.abs(savingsRate) + "% this period.";
            }
        } else {
            // No income logged yet this period, but expenses exist
            savingsPoints = 5;
            savingsDesc = "Debits recorded without logged income.";
        }

        // Budget Adherence (35 points max)
        int budgetPoints = 28;
        int overBudgetCount = 0;
        String budgetDesc = "No category spending caps configured.";
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
                if (overBudgetCount == 0) {
                    budgetDesc = "All " + budgetCount + " active category spending caps are within limits.";
                } else {
                    budgetDesc = overBudgetCount + " of " + budgetCount + " category budgets exceeded limit.";
                }
            }
        }

        // Recurring Commitment (25 points max)
        int recurringPoints = 25;
        double totalSubs = 0;
        String recurringDesc = "No active recurring subscription commitments.";
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
                recurringDesc = String.format(Locale.getDefault(), "Recurring bills make up %.1f%% of your cashflow.", subRatio);
            } else if (subRatio <= 30) {
                recurringPoints = 18;
                recurringDesc = String.format(Locale.getDefault(), "Recurring bills make up %.1f%% of cashflow (moderate).", subRatio);
            } else {
                recurringPoints = 10;
                recurringDesc = String.format(Locale.getDefault(), "Recurring bills take %.1f%% of cashflow (high fixed costs).", subRatio);
            }
        } else if (totalSubs > 0) {
            recurringPoints = 15;
            recurringDesc = "Active subscriptions detected with no logged income.";
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

        return new HealthScoreResult(
                totalScore,
                label,
                savingsRate,
                tip,
                colorHex,
                true,
                savingsPoints,
                savingsDesc,
                budgetPoints,
                budgetDesc,
                recurringPoints,
                recurringDesc
        );
    }

    public static HealthScoreResult calculateHealthScore(
            double monthlyIncome,
            double monthlyExpense,
            List<BudgetGoalEntity> budgets,
            List<SubscriptionEntity> subscriptions,
            List<TransactionEntity> transactions
    ) {
        if (budgets != null && transactions != null) {
            Map<String, Double> categoryMap = calculateMonthlyCategoryExpenses(transactions);
            applyMonthlySpendToBudgets(budgets, categoryMap);
        }
        return calculateHealthScore(monthlyIncome, monthlyExpense, budgets, subscriptions);
    }

    public static boolean isTransactionInMonth(String dateStr, Calendar referenceMonth) {
        if (dateStr == null || dateStr.trim().isEmpty() || dateStr.equalsIgnoreCase("N/A")) {
            return false;
        }
        String[] formats = new String[]{"yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", "dd-MM-yyyy", "yyyy/MM/dd"};
        Date date = null;
        for (String fmt : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.getDefault());
                sdf.setLenient(false);
                date = sdf.parse(dateStr.trim());
                if (date != null) break;
            } catch (Exception ignored) {}
        }
        if (date == null) {
            return false;
        }

        Calendar txCal = Calendar.getInstance();
        txCal.setTime(date);

        Calendar ref = referenceMonth != null ? referenceMonth : Calendar.getInstance();
        return txCal.get(Calendar.YEAR) == ref.get(Calendar.YEAR) &&
               txCal.get(Calendar.MONTH) == ref.get(Calendar.MONTH);
    }

    public static boolean isTransactionInCurrentMonth(String dateStr) {
        return isTransactionInMonth(dateStr, Calendar.getInstance());
    }

    public static Map<String, Double> calculateMonthlyCategoryExpenses(List<TransactionEntity> transactions) {
        return calculateMonthlyCategoryExpenses(transactions, Calendar.getInstance());
    }

    public static Map<String, Double> calculateMonthlyCategoryExpenses(
            List<TransactionEntity> transactions,
            Calendar referenceMonth
    ) {
        Map<String, Double> map = new HashMap<>();
        if (transactions != null) {
            for (TransactionEntity tx : transactions) {
                if ("debit".equalsIgnoreCase(tx.textType) && isTransactionInMonth(tx.date, referenceMonth)) {
                    String cat = tx.category != null ? tx.category.trim().toLowerCase() : "general";
                    double amt = 0;
                    if (tx.amount != null) {
                        try {
                            amt = Double.parseDouble(tx.amount.replaceAll("[^0-9.]", ""));
                        } catch (Exception ignored) {}
                    }
                    Double existing = map.get(cat);
                    map.put(cat, (existing != null ? existing : 0.0) + amt);
                }
            }
        }
        return map;
    }

    public static void applyMonthlySpendToBudgets(
            List<BudgetGoalEntity> budgets,
            Map<String, Double> categoryExpenseMap
    ) {
        if (budgets == null || categoryExpenseMap == null) return;
        for (BudgetGoalEntity b : budgets) {
            if (BudgetGoalEntity.TYPE_BUDGET.equalsIgnoreCase(b.type)) {
                String catKey = (b.category != null ? b.category : b.title).trim().toLowerCase();
                b.currentAmount = categoryExpenseMap.containsKey(catKey) ? categoryExpenseMap.get(catKey) : 0.0;
            }
        }
    }
}
