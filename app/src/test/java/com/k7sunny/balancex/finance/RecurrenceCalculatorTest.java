package com.k7sunny.balancex.finance;

import org.junit.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RecurrenceCalculatorTest {

    @Test
    public void testJan31AnchorMonthlyNoDrift() {
        // In a non-leap year (e.g. 2023)
        LocalDate anchor = LocalDate.of(2023, 1, 31);

        LocalDate occ0 = RecurrenceCalculator.getOccurrence(anchor, RecurrenceCalculator.Cycle.MONTHLY, 1, 0);
        assertEquals(LocalDate.of(2023, 1, 31), occ0);

        LocalDate occ1 = RecurrenceCalculator.getOccurrence(anchor, RecurrenceCalculator.Cycle.MONTHLY, 1, 1);
        assertEquals(LocalDate.of(2023, 2, 28), occ1);

        // Crucial test: March must be 31, NOT 28!
        LocalDate occ2 = RecurrenceCalculator.getOccurrence(anchor, RecurrenceCalculator.Cycle.MONTHLY, 1, 2);
        assertEquals(LocalDate.of(2023, 3, 31), occ2);

        LocalDate occ3 = RecurrenceCalculator.getOccurrence(anchor, RecurrenceCalculator.Cycle.MONTHLY, 1, 3);
        assertEquals(LocalDate.of(2023, 4, 30), occ3);

        LocalDate occ4 = RecurrenceCalculator.getOccurrence(anchor, RecurrenceCalculator.Cycle.MONTHLY, 1, 4);
        assertEquals(LocalDate.of(2023, 5, 31), occ4);
    }

    @Test
    public void testJan31LeapYearFebruaryClamping() {
        // 2024 is a leap year
        LocalDate anchor = LocalDate.of(2024, 1, 31);

        LocalDate feb = RecurrenceCalculator.getOccurrence(anchor, RecurrenceCalculator.Cycle.MONTHLY, 1, 1);
        assertEquals(LocalDate.of(2024, 2, 29), feb);

        LocalDate mar = RecurrenceCalculator.getOccurrence(anchor, RecurrenceCalculator.Cycle.MONTHLY, 1, 2);
        assertEquals(LocalDate.of(2024, 3, 31), mar);
    }

    @Test
    public void testLeapYearFeb29YearlyAnchorPreservesLeapDay() {
        LocalDate anchor = LocalDate.of(2024, 2, 29);

        // Year 1 (2025): non-leap year -> Feb 28
        LocalDate y1 = RecurrenceCalculator.getOccurrence(anchor, RecurrenceCalculator.Cycle.YEARLY, 1, 1);
        assertEquals(LocalDate.of(2025, 2, 28), y1);

        // Year 2 (2026): non-leap year -> Feb 28
        LocalDate y2 = RecurrenceCalculator.getOccurrence(anchor, RecurrenceCalculator.Cycle.YEARLY, 1, 2);
        assertEquals(LocalDate.of(2026, 2, 28), y2);

        // Year 3 (2027): non-leap year -> Feb 28
        LocalDate y3 = RecurrenceCalculator.getOccurrence(anchor, RecurrenceCalculator.Cycle.YEARLY, 1, 3);
        assertEquals(LocalDate.of(2027, 2, 28), y3);

        // Year 4 (2028): leap year again -> Feb 29!
        LocalDate y4 = RecurrenceCalculator.getOccurrence(anchor, RecurrenceCalculator.Cycle.YEARLY, 1, 4);
        assertEquals(LocalDate.of(2028, 2, 29), y4);
    }

    @Test
    public void testWeeklyAnchor() {
        LocalDate anchor = LocalDate.of(2026, 10, 1); // Thursday

        LocalDate w1 = RecurrenceCalculator.getOccurrence(anchor, RecurrenceCalculator.Cycle.WEEKLY, 1, 1);
        assertEquals(LocalDate.of(2026, 10, 8), w1);

        LocalDate w2 = RecurrenceCalculator.getOccurrence(anchor, RecurrenceCalculator.Cycle.WEEKLY, 1, 2);
        assertEquals(LocalDate.of(2026, 10, 15), w2);
    }

    @Test
    public void testQuarterlyAnchorClamping() {
        LocalDate anchor = LocalDate.of(2026, 8, 31);

        // Q1: 3 months later is Nov 30 (Nov has 30 days)
        LocalDate q1 = RecurrenceCalculator.getOccurrence(anchor, RecurrenceCalculator.Cycle.QUARTERLY, 1, 1);
        assertEquals(LocalDate.of(2026, 11, 30), q1);

        // Q2: 6 months later is Feb 28, 2027
        LocalDate q2 = RecurrenceCalculator.getOccurrence(anchor, RecurrenceCalculator.Cycle.QUARTERLY, 1, 2);
        assertEquals(LocalDate.of(2027, 2, 28), q2);

        // Q3: 9 months later is May 31, 2027 (May has 31 days)
        LocalDate q3 = RecurrenceCalculator.getOccurrence(anchor, RecurrenceCalculator.Cycle.QUARTERLY, 1, 3);
        assertEquals(LocalDate.of(2027, 5, 31), q3);
    }

    @Test
    public void testCustomDaysInterval() {
        LocalDate anchor = LocalDate.of(2026, 1, 1);
        LocalDate occ = RecurrenceCalculator.getOccurrence(anchor, RecurrenceCalculator.Cycle.CUSTOM_DAYS, 10, 3);
        assertEquals(LocalDate.of(2026, 1, 31), occ);
    }

    @Test
    public void testNextOccurrenceAfter() {
        LocalDate anchor = LocalDate.of(2026, 1, 31);

        // After Jan 15 -> should return anchor (Jan 31)
        LocalDate next1 = RecurrenceCalculator.nextOccurrenceAfter(anchor, RecurrenceCalculator.Cycle.MONTHLY, 1, LocalDate.of(2026, 1, 15));
        assertEquals(LocalDate.of(2026, 1, 31), next1);

        // After Jan 31 -> should return Feb 28
        LocalDate next2 = RecurrenceCalculator.nextOccurrenceAfter(anchor, RecurrenceCalculator.Cycle.MONTHLY, 1, LocalDate.of(2026, 1, 31));
        assertEquals(LocalDate.of(2026, 2, 28), next2);

        // After Feb 28 -> should return Mar 31
        LocalDate next3 = RecurrenceCalculator.nextOccurrenceAfter(anchor, RecurrenceCalculator.Cycle.MONTHLY, 1, LocalDate.of(2026, 2, 28));
        assertEquals(LocalDate.of(2026, 3, 31), next3);
    }

    @Test
    public void testOccurrencesBetweenAndCap() {
        LocalDate anchor = LocalDate.of(2024, 1, 1);
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2028, 1, 1); // 4 years = 48 months

        // Default cap of 24 should apply when limit is 24
        List<LocalDate> list = RecurrenceCalculator.occurrencesBetween(
                anchor, RecurrenceCalculator.Cycle.MONTHLY, 1, from, to, 24
        );
        assertEquals(24, list.size());
        assertEquals(LocalDate.of(2024, 1, 1), list.get(0));
        assertEquals(LocalDate.of(2025, 12, 1), list.get(23));
    }

    @Test
    public void testDaysUntil() {
        LocalDate today = LocalDate.of(2026, 10, 15);

        assertEquals(0, RecurrenceCalculator.daysUntil(LocalDate.of(2026, 10, 15), today));
        assertEquals(1, RecurrenceCalculator.daysUntil(LocalDate.of(2026, 10, 16), today));
        assertEquals(7, RecurrenceCalculator.daysUntil(LocalDate.of(2026, 10, 22), today));
        assertEquals(-1, RecurrenceCalculator.daysUntil(LocalDate.of(2026, 10, 14), today));
        assertEquals(-10, RecurrenceCalculator.daysUntil(LocalDate.of(2026, 10, 5), today));
    }

    @Test
    public void testToMonthlyAmountExactMultipliers() {
        // Weekly: amount * (52 / 12)
        double weeklyAmt = 100.0;
        double monthlyFromWeekly = RecurrenceCalculator.toMonthlyAmount(weeklyAmt, RecurrenceCalculator.Cycle.WEEKLY, 1);
        assertEquals(100.0 * (52.0 / 12.0), monthlyFromWeekly, 0.0001);

        // Yearly: amount / 12
        double yearlyAmt = 1200.0;
        double monthlyFromYearly = RecurrenceCalculator.toMonthlyAmount(yearlyAmt, RecurrenceCalculator.Cycle.YEARLY, 1);
        assertEquals(100.0, monthlyFromYearly, 0.0001);

        // Quarterly: amount / 3
        double quarterlyAmt = 300.0;
        double monthlyFromQuarterly = RecurrenceCalculator.toMonthlyAmount(quarterlyAmt, RecurrenceCalculator.Cycle.QUARTERLY, 1);
        assertEquals(100.0, monthlyFromQuarterly, 0.0001);
    }
}
