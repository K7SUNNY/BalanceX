package com.accounting.balancex.data.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import net.sqlcipher.database.SupportFactory;
import net.sqlcipher.database.SQLiteDatabase;

import com.accounting.balancex.data.entity.TransactionEntity;
import com.accounting.balancex.data.dao.TransactionDao;

@Database(entities = {TransactionEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract TransactionDao transactionDao();

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    // Initialize SQLCipher factory with a passphrase
                    final byte[] passphrase = SQLiteDatabase.getBytes("BalanceX_Secure_Passphrase_123!".toCharArray());
                    final SupportFactory factory = new SupportFactory(passphrase);

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
