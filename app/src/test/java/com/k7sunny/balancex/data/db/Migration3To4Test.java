package com.k7sunny.balancex.data.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.database.Cursor;

import androidx.sqlite.db.SupportSQLiteDatabase;

import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Migration3To4Test {

    static class RowData {
        long entryId;
        String amount;
        String date;

        RowData(long entryId, String amount, String date) {
            this.entryId = entryId;
            this.amount = amount;
            this.date = date;
        }
    }

    static class UpdateExecution {
        long amountMinor;
        Long dateEpochDay;
        int dateValid;
        long entryId;

        UpdateExecution(long amountMinor, Long dateEpochDay, int dateValid, long entryId) {
            this.amountMinor = amountMinor;
            this.dateEpochDay = dateEpochDay;
            this.dateValid = dateValid;
            this.entryId = entryId;
        }
    }

    @Test
    public void testMigration3To4BackfillsAmountsAndDatesAccurately() {
        // Pre-insert simulated rows across multiple amount formats and date formats
        List<RowData> testRows = new ArrayList<>();
        testRows.add(new RowData(1L, "1,234.50", "2026-04-12"));
        testRows.add(new RowData(2L, "₹450", "12/04/2026"));
        testRows.add(new RowData(3L, "25", "April 12, 2026"));
        testRows.add(new RowData(4L, "-12.00", "InvalidDate"));
        testRows.add(new RowData(5L, "0.00", ""));
        testRows.add(new RowData(6L, null, null));

        List<String> ddlStatements = new ArrayList<>();
        Map<Long, UpdateExecution> executedUpdates = new HashMap<>();

        // Create Cursor dynamic proxy
        Cursor fakeCursor = (Cursor) Proxy.newProxyInstance(
                Cursor.class.getClassLoader(),
                new Class<?>[]{Cursor.class},
                new InvocationHandler() {
                    private int currentIndex = -1;

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        String name = method.getName();
                        if ("moveToNext".equals(name)) {
                            currentIndex++;
                            return currentIndex < testRows.size();
                        } else if ("getLong".equals(name)) {
                            int col = (int) args[0];
                            if (col == 0) return testRows.get(currentIndex).entryId;
                        } else if ("getString".equals(name)) {
                            int col = (int) args[0];
                            if (col == 1) return testRows.get(currentIndex).amount;
                            if (col == 2) return testRows.get(currentIndex).date;
                        } else if ("close".equals(name)) {
                            return null;
                        }
                        return null;
                    }
                }
        );

        // Create SupportSQLiteDatabase dynamic proxy
        SupportSQLiteDatabase fakeDatabase = (SupportSQLiteDatabase) Proxy.newProxyInstance(
                SupportSQLiteDatabase.class.getClassLoader(),
                new Class<?>[]{SupportSQLiteDatabase.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("execSQL".equals(name)) {
                        String sql = (String) args[0];
                        if (args.length == 1) {
                            ddlStatements.add(sql);
                        } else if (args.length == 2 && args[1] instanceof Object[]) {
                            Object[] bindArgs = (Object[]) args[1];
                            long amountMinor = (long) bindArgs[0];
                            Long epochDay = (Long) bindArgs[1];
                            int dateValid = (int) bindArgs[2];
                            long entryId = (long) bindArgs[3];
                            executedUpdates.put(entryId, new UpdateExecution(amountMinor, epochDay, dateValid, entryId));
                        }
                        return null;
                    } else if ("query".equals(name)) {
                        return fakeCursor;
                    }
                    return null;
                }
        );

        // Run the actual migration
        AppDatabase.MIGRATION_3_4.migrate(fakeDatabase);

        // Verify Schema DDL alterations were executed
        assertEquals(3, ddlStatements.size());
        assertTrue(ddlStatements.get(0).contains("amountMinor"));
        assertTrue(ddlStatements.get(1).contains("dateEpochDay"));
        assertTrue(ddlStatements.get(2).contains("dateValid"));

        // Verify all 6 rows were backfilled
        assertEquals(6, executedUpdates.size());

        long expectedEpochDay = LocalDate.of(2026, 4, 12).toEpochDay();

        // Row 1: "1,234.50", "2026-04-12"
        UpdateExecution r1 = executedUpdates.get(1L);
        assertNotNull(r1);
        assertEquals(123450L, r1.amountMinor);
        assertEquals(Long.valueOf(expectedEpochDay), r1.dateEpochDay);
        assertEquals(1, r1.dateValid);

        // Row 2: "₹450", "12/04/2026"
        UpdateExecution r2 = executedUpdates.get(2L);
        assertNotNull(r2);
        assertEquals(45000L, r2.amountMinor);
        assertEquals(Long.valueOf(expectedEpochDay), r2.dateEpochDay);
        assertEquals(1, r2.dateValid);

        // Row 3: "25", "April 12, 2026"
        UpdateExecution r3 = executedUpdates.get(3L);
        assertNotNull(r3);
        assertEquals(2500L, r3.amountMinor);
        assertEquals(Long.valueOf(expectedEpochDay), r3.dateEpochDay);
        assertEquals(1, r3.dateValid);

        // Row 4: "-12.00", "InvalidDate"
        UpdateExecution r4 = executedUpdates.get(4L);
        assertNotNull(r4);
        assertEquals(-1200L, r4.amountMinor);
        assertNull(r4.dateEpochDay);
        assertEquals(0, r4.dateValid);

        // Row 5: "0.00", ""
        UpdateExecution r5 = executedUpdates.get(5L);
        assertNotNull(r5);
        assertEquals(0L, r5.amountMinor);
        assertNull(r5.dateEpochDay);
        assertEquals(0, r5.dateValid);

        // Row 6: null, null
        UpdateExecution r6 = executedUpdates.get(6L);
        assertNotNull(r6);
        assertEquals(0L, r6.amountMinor);
        assertNull(r6.dateEpochDay);
        assertEquals(0, r6.dateValid);
    }
}
