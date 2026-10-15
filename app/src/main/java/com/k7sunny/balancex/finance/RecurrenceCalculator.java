package com.k7sunny.balancex.finance;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Pure calculation engine for recurring bills and subscriptions.
 *
 * All recurrence calculations are anchored to the original starting date to
 * guarantee zero day-of-month drift across varying month lengths and leap years.
 */
public final class RecurrenceCalculator {

    public enum Cycle {
        WEEKLY,
        MONTHLY,
        QUARTERLY,
        YEARLY,
        CUSTOM_DAYS,
        CUSTOM_MONTHS;

        public static Cycle fromString(String raw) {
            if (raw == null) return MONTHLY;
            String normalized = raw.trim().toUpperCase(Locale.ROOT);
            switch (normalized) {
                case "WEEKLY":
                    return WEEKLY;
                case "QUARTERLY":
                    return QUARTERLY;
                case "YEARLY":
                case "ANNUAL":
                case "ANNUALLY":
                    return YEARLY;
                case "CUSTOM_DAYS":
                case "CUSTOM_DAYS_INTERVAL":
                    return CUSTOM_DAYS;
                case "CUSTOM_MONTHS":
                case "CUSTOM_MONTHS_INTERVAL":
                    return CUSTOM_MONTHS;
                case "MONTHLY":
                default:
                    return MONTHLY;
            }
        }
    }

    private RecurrenceCalculator() {
        // Utility class
    }

    /**
     * Computes the k-th occurrence (k >= 0) from the given anchor date.
     * The original anchor's day-of-month is preserved and clamped to the target month's length.
     *
     * @param anchor Date from which cycles originate
     * @param cycle Frequency cycle
     * @param customInterval Interval step for CUSTOM_DAYS or CUSTOM_MONTHS (must be >= 1)
     * @param k Occurrence index (0 = anchor date itself)
     */
    public static LocalDate getOccurrence(LocalDate anchor, Cycle cycle, int customInterval, long k) {
        if (anchor == null) {
            throw new IllegalArgumentException("Anchor date cannot be null");
        }
        if (k < 0) {
            throw new IllegalArgumentException("Occurrence index k must be >= 0");
        }
        if (cycle == null) {
            cycle = Cycle.MONTHLY;
        }
        int interval = Math.max(1, customInterval);

        switch (cycle) {
            case WEEKLY:
                return anchor.plusWeeks(k);

            case YEARLY: {
                long targetYear = anchor.getYear() + k;
                int anchorDay = anchor.getDayOfMonth();
                int anchorMonth = anchor.getMonthValue();
                int maxDays = YearMonth.of((int) targetYear, anchorMonth).lengthOfMonth();
                int clampedDay = Math.min(anchorDay, maxDays);
                return LocalDate.of((int) targetYear, anchorMonth, clampedDay);
            }

            case QUARTERLY: {
                long totalMonthsToAdd = k * 3L;
                return addMonthsPreservingAnchor(anchor, totalMonthsToAdd);
            }

            case CUSTOM_DAYS:
                return anchor.plusDays(k * interval);

            case CUSTOM_MONTHS: {
                long totalMonthsToAdd = k * (long) interval;
                return addMonthsPreservingAnchor(anchor, totalMonthsToAdd);
            }

            case MONTHLY:
            default:
                return addMonthsPreservingAnchor(anchor, k);
        }
    }

    private static LocalDate addMonthsPreservingAnchor(LocalDate anchor, long monthsToAdd) {
        // Calculate new year and month
        long totalMonths = (anchor.getYear() * 12L + (anchor.getMonthValue() - 1)) + monthsToAdd;
        int targetYear = (int) Math.floorDiv(totalMonths, 12L);
        int targetMonth = (int) (Math.floorMod(totalMonths, 12L) + 1);

        int maxDaysInTargetMonth = YearMonth.of(targetYear, targetMonth).lengthOfMonth();
        int clampedDay = Math.min(anchor.getDayOfMonth(), maxDaysInTargetMonth);
        return LocalDate.of(targetYear, targetMonth, clampedDay);
    }

    /**
     * Finds the earliest occurrence strictly after {@code afterDate}.
     * Searches forward from the anchor date without chaining drift.
     */
    public static LocalDate nextOccurrenceAfter(LocalDate anchor, Cycle cycle, int customInterval, LocalDate afterDate) {
        if (anchor == null) return null;
        if (afterDate == null) return anchor;

        if (anchor.isAfter(afterDate)) {
            return anchor;
        }

        // Binary search or estimated index jump to quickly locate the candidate k
        long k = estimateInitialK(anchor, cycle, customInterval, afterDate);
        while (k > 0 && !getOccurrence(anchor, cycle, customInterval, k - 1).isAfter(afterDate)) {
            LocalDate cand = getOccurrence(anchor, cycle, customInterval, k);
            if (cand.isAfter(afterDate)) {
                return cand;
            }
            k++;
        }

        while (!getOccurrence(anchor, cycle, customInterval, k).isAfter(afterDate)) {
            k++;
        }
        return getOccurrence(anchor, cycle, customInterval, k);
    }

    /**
     * Computes all occurrences between {@code from} (inclusive) and {@code to} (inclusive),
     * capped at {@code maxLimit} (default 24).
     */
    public static List<LocalDate> occurrencesBetween(
            LocalDate anchor,
            Cycle cycle,
            int customInterval,
            LocalDate from,
            LocalDate to,
            int maxLimit
    ) {
        if (anchor == null || from == null || to == null || from.isAfter(to)) {
            return Collections.emptyList();
        }
        int limit = (maxLimit <= 0) ? 24 : maxLimit;
        List<LocalDate> result = new ArrayList<>();

        long k = 0;
        if (anchor.isBefore(from)) {
            k = Math.max(0, estimateInitialK(anchor, cycle, customInterval, from) - 1);
        }

        while (result.size() < limit) {
            LocalDate occ = getOccurrence(anchor, cycle, customInterval, k);
            if (occ.isAfter(to)) {
                break;
            }
            if (!occ.isBefore(from)) {
                result.add(occ);
            }
            k++;
        }

        return result;
    }

    /**
     * Heuristic jump to avoid iterating step-by-step for subscriptions created long ago.
     */
    private static long estimateInitialK(LocalDate anchor, Cycle cycle, int customInterval, LocalDate target) {
        if (!target.isAfter(anchor)) return 0;
        int interval = Math.max(1, customInterval);

        switch (cycle) {
            case WEEKLY: {
                long days = ChronoUnit.DAYS.between(anchor, target);
                return Math.max(0, days / 7);
            }
            case YEARLY: {
                long years = ChronoUnit.YEARS.between(anchor, target);
                return Math.max(0, years);
            }
            case QUARTERLY: {
                long months = ChronoUnit.MONTHS.between(anchor, target);
                return Math.max(0, months / 3);
            }
            case CUSTOM_DAYS: {
                long days = ChronoUnit.DAYS.between(anchor, target);
                return Math.max(0, days / interval);
            }
            case CUSTOM_MONTHS: {
                long months = ChronoUnit.MONTHS.between(anchor, target);
                return Math.max(0, months / interval);
            }
            case MONTHLY:
            default: {
                long months = ChronoUnit.MONTHS.between(anchor, target);
                return Math.max(0, months);
            }
        }
    }

    /**
     * Returns days between today and target date.
     * Negative if target is in the past (overdue), 0 if today, positive if in the future.
     */
    public static long daysUntil(LocalDate target, LocalDate today) {
        if (target == null || today == null) return 0;
        return ChronoUnit.DAYS.between(today, target);
    }

    /**
     * Calculates the monthly equivalent of a recurring commitment amount.
     * Weekly uses exactly 52.0 / 12.0 (~4.3333333333).
     * Yearly uses 1.0 / 12.0.
     * Quarterly uses 4.0 / 12.0 (1.0 / 3.0).
     */
    public static double toMonthlyAmount(double amount, Cycle cycle, int customInterval) {
        if (amount <= 0) return 0.0;
        if (cycle == null) cycle = Cycle.MONTHLY;

        switch (cycle) {
            case WEEKLY:
                return amount * (52.0 / 12.0);
            case YEARLY:
                return amount / 12.0;
            case QUARTERLY:
                return amount / 3.0;
            case CUSTOM_DAYS: {
                int interval = Math.max(1, customInterval);
                // 365.0 / 12.0 days in an average month
                double daysInMonth = 365.0 / 12.0;
                return amount * (daysInMonth / (double) interval);
            }
            case CUSTOM_MONTHS: {
                int interval = Math.max(1, customInterval);
                return amount / (double) interval;
            }
            case MONTHLY:
            default:
                return amount;
        }
    }
}
