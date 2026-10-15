package com.k7sunny.balancex.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.k7sunny.balancex.data.dao.BudgetGoalDao;
import com.k7sunny.balancex.data.dao.NotificationDao;
import com.k7sunny.balancex.data.dao.ReminderLogDao;
import com.k7sunny.balancex.data.dao.SubscriptionDao;
import com.k7sunny.balancex.data.dao.SubscriptionPriceHistoryDao;
import com.k7sunny.balancex.data.dao.TransactionDao;
import com.k7sunny.balancex.data.entity.BudgetGoalEntity;
import com.k7sunny.balancex.data.entity.NotificationEntity;
import com.k7sunny.balancex.data.entity.ReminderLogEntity;
import com.k7sunny.balancex.data.entity.SubscriptionEntity;
import com.k7sunny.balancex.data.entity.SubscriptionPriceHistoryEntity;
import com.k7sunny.balancex.data.entity.TransactionEntity;

import net.zetetic.database.sqlcipher.SupportOpenHelperFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
        entities = {
                TransactionEntity.class,
                SubscriptionEntity.class,
                BudgetGoalEntity.class,
                NotificationEntity.class,
                SubscriptionPriceHistoryEntity.class,
                ReminderLogEntity.class
        },
        version = 5,
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public abstract TransactionDao transactionDao();
    public abstract SubscriptionDao subscriptionDao();
    public abstract BudgetGoalDao budgetGoalDao();
    public abstract NotificationDao notificationDao();
    public abstract SubscriptionPriceHistoryDao subscriptionPriceHistoryDao();
    public abstract ReminderLogDao reminderLogDao();

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `subscriptions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `amount` REAL NOT NULL, `billingCycle` TEXT, `nextDueDate` TEXT, `category` TEXT, `reminderDaysBefore` INTEGER NOT NULL, `autoAddTransaction` INTEGER NOT NULL, `isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)");
            database.execSQL("CREATE TABLE IF NOT EXISTS `budget_goals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT, `targetAmount` REAL NOT NULL, `currentAmount` REAL NOT NULL, `category` TEXT, `type` TEXT, `period` TEXT, `colorHex` TEXT, `createdAt` INTEGER NOT NULL)");
        }
    };

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `notifications` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT, `message` TEXT, `timestamp` INTEGER NOT NULL, `type` TEXT, `isRead` INTEGER NOT NULL, `actionTarget` TEXT, `extraData` TEXT)");
        }
    };

    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `transactions` ADD COLUMN `amountMinor` INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE `transactions` ADD COLUMN `dateEpochDay` INTEGER DEFAULT NULL");
            database.execSQL("ALTER TABLE `transactions` ADD COLUMN `dateValid` INTEGER NOT NULL DEFAULT 1");

            android.database.Cursor cursor = database.query("SELECT `entryId`, `amount`, `date` FROM `transactions`");
            if (cursor != null) {
                try {
                    while (cursor.moveToNext()) {
                        long entryId = cursor.getLong(0);
                        String rawAmount = cursor.getString(1);
                        String rawDate = cursor.getString(2);

                        long amountMinor = com.k7sunny.balancex.util.MoneyDateUtils.parseAmountToMinor(rawAmount);
                        Long epochDay = com.k7sunny.balancex.util.MoneyDateUtils.parseDateToEpochDay(rawDate);
                        int dateValid = (epochDay != null) ? 1 : 0;

                        database.execSQL("UPDATE `transactions` SET `amountMinor` = ?, `dateEpochDay` = ?, `dateValid` = ? WHERE `entryId` = ?",
                                new Object[]{amountMinor, epochDay, dateValid, entryId});
                    }
                } finally {
                    cursor.close();
                }
            }
        }
    };

    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // 1. Subscriptions table additions
            database.execSQL("ALTER TABLE `subscriptions` ADD COLUMN `amountMinor` INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE `subscriptions` ADD COLUMN `customInterval` INTEGER NOT NULL DEFAULT 1");
            database.execSQL("ALTER TABLE `subscriptions` ADD COLUMN `anchorDate` TEXT DEFAULT NULL");
            database.execSQL("ALTER TABLE `subscriptions` ADD COLUMN `lastPaidDate` TEXT DEFAULT NULL");
            database.execSQL("ALTER TABLE `subscriptions` ADD COLUMN `endDate` TEXT DEFAULT NULL");
            database.execSQL("ALTER TABLE `subscriptions` ADD COLUMN `pausedUntil` TEXT DEFAULT NULL");
            database.execSQL("ALTER TABLE `subscriptions` ADD COLUMN `trialEndsOn` TEXT DEFAULT NULL");
            database.execSQL("ALTER TABLE `subscriptions` ADD COLUMN `paymentMethod` TEXT DEFAULT NULL");

            // Backfill subscriptions
            database.execSQL("UPDATE `subscriptions` SET `amountMinor` = CAST(ROUND(`amount` * 100) AS INTEGER) WHERE `amountMinor` = 0");
            database.execSQL("UPDATE `subscriptions` SET `anchorDate` = `nextDueDate` WHERE `anchorDate` IS NULL");
            database.execSQL("UPDATE `subscriptions` SET `paymentMethod` = 'UPI' WHERE `paymentMethod` IS NULL");

            // 2. Transactions table additions
            database.execSQL("ALTER TABLE `transactions` ADD COLUMN `subscriptionId` INTEGER DEFAULT NULL");
            database.execSQL("ALTER TABLE `transactions` ADD COLUMN `occurrenceDate` TEXT DEFAULT NULL");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_transactions_subscriptionId_occurrenceDate` ON `transactions` (`subscriptionId`, `occurrenceDate`)");

            // 3. New table: subscription_price_history
            database.execSQL("CREATE TABLE IF NOT EXISTS `subscription_price_history` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`subscriptionId` INTEGER NOT NULL, " +
                    "`effectiveDate` TEXT, " +
                    "`amountMinor` INTEGER NOT NULL, " +
                    "`amount` REAL NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_subscription_price_history_subscriptionId` ON `subscription_price_history` (`subscriptionId`)");

            // 4. New table: reminder_log
            database.execSQL("CREATE TABLE IF NOT EXISTS `reminder_log` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`subscriptionId` INTEGER NOT NULL, " +
                    "`occurrenceDate` TEXT, " +
                    "`kind` TEXT, " +
                    "`loggedAt` INTEGER NOT NULL)");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_reminder_log_subscriptionId_occurrenceDate_kind` ON `reminder_log` (`subscriptionId`, `occurrenceDate`, `kind`)");
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    // Initialize SQLCipher factory with Android Keystore backed key
                    System.loadLibrary("sqlcipher");
                    final byte[] passphrase = com.k7sunny.balancex.security.DatabaseKeyManager.getDatabaseKey(context);
                    final SupportOpenHelperFactory factory = new SupportOpenHelperFactory(passphrase);

                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "balancex_encrypted_database.db")
                            .openHelperFactory(factory)
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
