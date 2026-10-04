package com.accounting.balancex.data.db;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.accounting.balancex.Transaction;
import com.accounting.balancex.data.entity.TransactionEntity;
import com.accounting.balancex.data.repository.TransactionRepository;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DatabaseMigrator {

    private static final String PREF_NAME = "MigrationPrefs";
    private static final String KEY_MIGRATED = "is_json_to_room_migrated_v2"; // Changed key to force retry

    public static void migrateJsonToRoomIfNeeded(Context context, TransactionRepository repository) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean isMigrated = prefs.getBoolean(KEY_MIGRATED, false);

        if (isMigrated) {
            return; // Already migrated
        }

        List<Transaction> oldTransactions = loadTransactionsFromJson(context);
        if (oldTransactions == null || oldTransactions.isEmpty()) {
            // Nothing to migrate, mark as done
            prefs.edit().putBoolean(KEY_MIGRATED, true).apply();
            return;
        }

        List<TransactionEntity> entities = new ArrayList<>();
        for (Transaction old : oldTransactions) {
            TransactionEntity entity = new TransactionEntity();
            // Let's use old entry id if it is non-zero, else let room generate.
            if(old.getEntryId() > 0) {
                entity.entryId = old.getEntryId();
            }
            entity.date = old.getDate();
            entity.amount = old.getAmount();
            entity.receiver = old.getReceiverName();
            entity.description = old.getDescription();
            entity.utr = old.getUtr();
            entity.transactionId = old.getTransactionID();
            entity.comments = old.getComments();
            entity.category = old.getCategory();
            entity.paymentMethod = old.getPaymentMethod();
            entity.textType = old.getTransactionType();
            
            entities.add(entity);
        }

        repository.insertAll(entities, () -> {
            Log.d("DatabaseMigrator", "Successfully migrated " + entities.size() + " transactions to Room.");
            prefs.edit().putBoolean(KEY_MIGRATED, true).apply();
        });
    }

    private static List<Transaction> loadTransactionsFromJson(Context context) {
        File file = new File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS), "Accounting/transactions.json");
        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader reader = new BufferedReader(isr)) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            Gson gson = new Gson();
            Type type = new TypeToken<List<Transaction>>() {}.getType();
            return gson.fromJson(sb.toString(), type);

        } catch (Exception e) {
            Log.e("DatabaseMigrator", "Error reading JSON for migration", e);
            return new ArrayList<>();
        }
    }
}
