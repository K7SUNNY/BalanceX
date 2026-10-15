package com.k7sunny.balancex.data.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.sqlite.db.SupportSQLiteDatabase;

import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

public class Migration4To5Test {

    @Test
    public void testMigration4To5ExecutesAllDDLAndBackfillStatements() {
        List<String> executedStatements = new ArrayList<>();

        SupportSQLiteDatabase fakeDatabase = (SupportSQLiteDatabase) Proxy.newProxyInstance(
                SupportSQLiteDatabase.class.getClassLoader(),
                new Class<?>[]{SupportSQLiteDatabase.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("execSQL".equals(name)) {
                        String sql = (String) args[0];
                        executedStatements.add(sql);
                    }
                    return null;
                }
        );

        // Execute MIGRATION_4_5
        AppDatabase.MIGRATION_4_5.migrate(fakeDatabase);

        // Verify statement count
        assertEquals(18, executedStatements.size());

        // Verify specific DDL and backfill queries
        assertTrue(executedStatements.stream().anyMatch(s -> s.contains("ALTER TABLE `subscriptions` ADD COLUMN `amountMinor`")));
        assertTrue(executedStatements.stream().anyMatch(s -> s.contains("ALTER TABLE `subscriptions` ADD COLUMN `anchorDate`")));
        assertTrue(executedStatements.stream().anyMatch(s -> s.contains("ALTER TABLE `subscriptions` ADD COLUMN `paymentMethod`")));
        assertTrue(executedStatements.stream().anyMatch(s -> s.contains("UPDATE `subscriptions` SET `amountMinor` = CAST(ROUND(`amount` * 100) AS INTEGER)")));
        assertTrue(executedStatements.stream().anyMatch(s -> s.contains("UPDATE `subscriptions` SET `anchorDate` = `nextDueDate`")));
        assertTrue(executedStatements.stream().anyMatch(s -> s.contains("ALTER TABLE `transactions` ADD COLUMN `subscriptionId`")));
        assertTrue(executedStatements.stream().anyMatch(s -> s.contains("ALTER TABLE `transactions` ADD COLUMN `occurrenceDate`")));
        assertTrue(executedStatements.stream().anyMatch(s -> s.contains("CREATE UNIQUE INDEX IF NOT EXISTS `index_transactions_subscriptionId_occurrenceDate`")));
        assertTrue(executedStatements.stream().anyMatch(s -> s.contains("CREATE TABLE IF NOT EXISTS `subscription_price_history`")));
        assertTrue(executedStatements.stream().anyMatch(s -> s.contains("CREATE TABLE IF NOT EXISTS `reminder_log`")));
        assertTrue(executedStatements.stream().anyMatch(s -> s.contains("CREATE UNIQUE INDEX IF NOT EXISTS `index_reminder_log_subscriptionId_occurrenceDate_kind`")));
    }
}
