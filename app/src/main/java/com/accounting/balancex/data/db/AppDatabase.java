package com.accounting.balancex.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.accounting.balancex.data.dao.BudgetGoalDao;
import com.accounting.balancex.data.dao.SubscriptionDao;
import com.accounting.balancex.data.dao.TransactionDao;
import com.accounting.balancex.data.entity.BudgetGoalEntity;
import com.accounting.balancex.data.entity.SubscriptionEntity;
import com.accounting.balancex.data.entity.TransactionEntity;

import net.zetetic.database.sqlcipher.SupportOpenHelperFactory;

import java.nio.charset.StandardCharsets;

@Database(entities = {TransactionEntity.class, SubscriptionEntity.class, BudgetGoalEntity.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract TransactionDao transactionDao();
    public abstract SubscriptionDao subscriptionDao();
    public abstract BudgetGoalDao budgetGoalDao();

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    // Initialize SQLCipher factory with a passphrase
                    System.loadLibrary("sqlcipher");
                    final byte[] passphrase = "BalanceX_Secure_Passphrase_123!".getBytes(StandardCharsets.UTF_8);
                    final SupportOpenHelperFactory factory = new SupportOpenHelperFactory(passphrase);

                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "balancex_encrypted_database.db")
                            .openHelperFactory(factory)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
