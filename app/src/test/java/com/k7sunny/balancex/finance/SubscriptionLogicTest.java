package com.k7sunny.balancex.finance;

import com.k7sunny.balancex.notifications.ReminderPlanner;
import com.k7sunny.balancex.ui.subscriptions.SubscriptionsActivity;

import org.junit.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SubscriptionLogicTest {

    @Test
    public void testRelativeDueDateFormatting() {
        LocalDate today = LocalDate.of(2026, 10, 15);

        assertEquals("Due today", SubscriptionsActivity.formatRelativeDueDate("2026-10-15", today));
        assertEquals("Due tomorrow", SubscriptionsActivity.formatRelativeDueDate("2026-10-16", today));
        assertEquals("Due in 5 days", SubscriptionsActivity.formatRelativeDueDate("2026-10-20", today));
        assertEquals("Overdue by 1d", SubscriptionsActivity.formatRelativeDueDate("2026-10-14", today));
        assertEquals("Overdue by 10d", SubscriptionsActivity.formatRelativeDueDate("2026-10-05", today));
        assertEquals("Due 25 Nov", SubscriptionsActivity.formatRelativeDueDate("2026-11-25", today));
    }

    @Test
    public void testNotificationIdStabilityAndUniqueness() {
        // Same sub ID and occurrence date must produce the exact same notification ID
        int id1 = ReminderPlanner.getNotificationId(42, "2026-10-15");
        int id2 = ReminderPlanner.getNotificationId(42, "2026-10-15");
        assertEquals(id1, id2);

        // Different occurrence dates for same sub must produce different notification IDs
        int id3 = ReminderPlanner.getNotificationId(42, "2026-11-15");
        assertNotEquals(id1, id3);

        // Different sub IDs on same date must produce different notification IDs
        int id4 = ReminderPlanner.getNotificationId(99, "2026-10-15");
        assertNotEquals(id1, id4);
    }

    @Test
    public void testCatchUpOccurrencesCappedAt24() {
        LocalDate anchor = LocalDate.of(2020, 1, 1);
        LocalDate from = LocalDate.of(2020, 1, 1);
        LocalDate today = LocalDate.of(2026, 10, 15); // > 6 years = > 72 months

        List<LocalDate> occurrences = RecurrenceCalculator.occurrencesBetween(
                anchor,
                RecurrenceCalculator.Cycle.MONTHLY,
                1,
                from,
                today,
                24
        );

        assertNotNull(occurrences);
        assertEquals(24, occurrences.size());
        assertEquals(LocalDate.of(2020, 1, 1), occurrences.get(0));
        assertEquals(LocalDate.of(2021, 12, 1), occurrences.get(23));
    }

    @Test
    public void testQuarterlyRecurrencePreservesAnchor() {
        LocalDate anchor = LocalDate.of(2026, 1, 31);

        LocalDate q1 = RecurrenceCalculator.getOccurrence(anchor, RecurrenceCalculator.Cycle.QUARTERLY, 1, 1);
        assertEquals(LocalDate.of(2026, 4, 30), q1); // April has 30 days

        LocalDate q2 = RecurrenceCalculator.getOccurrence(anchor, RecurrenceCalculator.Cycle.QUARTERLY, 1, 2);
        assertEquals(LocalDate.of(2026, 7, 31), q2); // July has 31 days

        LocalDate q3 = RecurrenceCalculator.getOccurrence(anchor, RecurrenceCalculator.Cycle.QUARTERLY, 1, 3);
        assertEquals(LocalDate.of(2026, 10, 31), q3); // October has 31 days
    }
}
