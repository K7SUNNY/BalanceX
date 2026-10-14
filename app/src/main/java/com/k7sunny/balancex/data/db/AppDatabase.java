package com.k7sunny.balancex.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.k7sunny.balancex.data.dao.BudgetGoalDao;
import com.k7sunny.balancex.data.dao.NotificationDao;
import com.k7sunny.balancex.data.dao.SubscriptionDao;
import com.k7sunny.balancex.data.dao.TransactionDao;
import com.k7sunny.balancex.data.entity.BudgetGoalEntity;
import com.k7sunny.balancex.data.entity.NotificationEntity;
import com.k7sunny.balancex.data.entity.SubscriptionEntity;
import com.k7sunny.balancex.data.entity.TransactionEntity;

import net.zetetic.database.sqlcipher.SupportOpenHelperFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {TransactionEntity.class, SubscriptionEntity.class, BudgetGoalEntity.class, NotificationEntity.class}, version = 4, exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public abstract TransactionDao transactionDao();
    public abstract SubscriptionDao subscriptionDao();
    public abstract BudgetGoalDao budgetGoalDao();
    public abstract NotificationDao notificationDao();

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
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
