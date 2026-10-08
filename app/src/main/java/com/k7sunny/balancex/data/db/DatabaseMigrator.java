package com.k7sunny.balancex.data.db;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.k7sunny.balancex.Transaction;
import com.k7sunny.balancex.data.entity.TransactionEntity;
import com.k7sunny.balancex.data.repository.TransactionRepository;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DatabaseMigrator {

    private static final String TAG = "DatabaseMigrator";

    public static void migrateJsonToRoomIfNeeded(Context context, TransactionRepository repository) {
        migrateJsonToRoomIfNeeded(context, repository, null);
    }

    public static void migrateJsonToRoomIfNeeded(Context context, TransactionRepository repository, Runnable onComplete) {
        List<TransactionEntity> jsonEntities = loadEntitiesFromJson(context);
        if (jsonEntities == null || jsonEntities.isEmpty()) {
            Log.d(TAG, "No JSON transactions found to migrate.");
            if (onComplete != null) onComplete.run();
            return;
        }

        repository.getAllTransactions(roomTransactions -> {
            int roomCount = (roomTransactions != null) ? roomTransactions.size() : 0;
            Log.d(TAG, "Found " + jsonEntities.size() + " in JSON, " + roomCount + " in Room.");

            // If Room has fewer transactions than the JSON file, or if new transactions exist
            if (roomTransactions == null || roomTransactions.isEmpty()) {
                // Initial or complete migration
                repository.insertAll(jsonEntities, () -> {
                    Log.d(TAG, "Successfully inserted " + jsonEntities.size() + " transactions into Room.");
                    if (onComplete != null) onComplete.run();
                });
            } else if (jsonEntities.size() > roomTransactions.size()) {
                // Find and insert any missing transactions
                Set<String> existingSignatures = new HashSet<>();
                for (TransactionEntity r : roomTransactions) {
                    existingSignatures.add(makeSignature(r.date, r.amount, r.receiver, r.textType, r.description));
                }

                List<TransactionEntity> missingEntities = new ArrayList<>();
                for (TransactionEntity j : jsonEntities) {
                    String sig = makeSignature(j.date, j.amount, j.receiver, j.textType, j.description);
                    if (!existingSignatures.contains(sig)) {
                        missingEntities.add(j);
                    }
                }

                if (!missingEntities.isEmpty()) {
                    Log.d(TAG, "Syncing " + missingEntities.size() + " new transactions from JSON into Room.");
                    repository.insertAll(missingEntities, () -> {
                        Log.d(TAG, "Sync complete.");
                        if (onComplete != null) onComplete.run();
                    });
                } else {
                    if (onComplete != null) onComplete.run();
                }
            } else {
                Log.d(TAG, "Room is already up to date with JSON records.");
                if (onComplete != null) onComplete.run();
            }
        });
    }

    public static String makeSignature(String date, String amount, String receiver, String textType, String description) {
        return (date != null ? date : "") + "|"
                + (amount != null ? amount : "") + "|"
                + (receiver != null ? receiver : "") + "|"
                + (textType != null ? textType : "") + "|"
                + (description != null ? description : "");
    }

    public static File findJsonFile(Context context) {
        File[] candidatePaths = new File[] {
                // 1. Accounting subdirectory
                new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Accounting/transactions.json"),
                new File("/storage/emulated/0/Documents/Accounting/transactions.json"),
                new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Accounting/transacttions.json"),
                new File("/storage/emulated/0/Documents/Accounting/transacttions.json"),
                // 2. Direct Documents folder
                new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "transactions.json"),
                new File("/storage/emulated/0/Documents/transactions.json"),
                new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "transacttions.json"),
                new File("/storage/emulated/0/Documents/transacttions.json"),
                // 3. App-specific external storage
                context.getExternalFilesDir(null) != null ? new File(context.getExternalFilesDir(null), "transactions.json") : null,
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) != null ? new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "transactions.json") : null,
                // 4. Internal files directory
                new File(context.getFilesDir(), "transactions.json")
        };

        // First pass: Find any file that is populated (length > 2 bytes, ignoring empty "[]")
        for (File candidate : candidatePaths) {
            if (candidate != null && candidate.exists() && candidate.length() > 2) {
                Log.d(TAG, "Resolved valid populated transactions.json at: " + candidate.getAbsolutePath() + " (" + candidate.length() + " bytes)");
                return candidate;
            }
        }
        // Second pass: Return existing file even if empty
        for (File candidate : candidatePaths) {
            if (candidate != null && candidate.exists()) {
                Log.d(TAG, "Resolved existing transactions.json at: " + candidate.getAbsolutePath());
                return candidate;
            }
        }
        Log.w(TAG, "No transactions.json file found in any expected document path.");
        return null;
    }

    public static List<TransactionEntity> loadEntitiesFromJson(Context context) {
        File file = findJsonFile(context);
        if (file == null) {
            return new ArrayList<>();
        }

        List<TransactionEntity> entities = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            JSONArray array = new JSONArray(sb.toString().trim());
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                TransactionEntity entity = new TransactionEntity();

                if (obj.has("entryId")) {
                    entity.entryId = obj.optLong("entryId", i + 1);
                } else {
                    entity.entryId = i + 1;
                }

                entity.date = obj.optString("date", "");
                
                // Handle amount whether it is JSON number or String
                if (obj.has("amount")) {
                    entity.amount = String.valueOf(obj.get("amount"));
                } else {
                    entity.amount = "0";
                }

                // Handle receiver name variants
                if (obj.has("receiver")) {
                    entity.receiver = obj.optString("receiver");
                } else if (obj.has("receiverName")) {
                    entity.receiver = obj.optString("receiverName");
                } else {
                    entity.receiver = "Unknown";
                }

                entity.description = obj.optString("description", "");
                entity.utr = obj.optString("utr", "");
                
                if (obj.has("transactionId")) {
                    entity.transactionId = obj.optString("transactionId");
                } else if (obj.has("transactionID")) {
                    entity.transactionId = obj.optString("transactionID");
                } else {
                    entity.transactionId = "";
                }

                entity.comments = obj.optString("comments", "");
                entity.category = obj.optString("category", "General");
                entity.paymentMethod = obj.optString("paymentMethod", "Cash");
                
                // Handle transaction type variants
                if (obj.has("textType")) {
                    entity.textType = obj.optString("textType");
                } else if (obj.has("transactionType")) {
                    entity.textType = obj.optString("transactionType");
                } else {
                    entity.textType = "Debit";
                }

                entities.add(entity);
            }

            Log.d(TAG, "Successfully parsed " + entities.size() + " entities from " + file.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Error parsing transactions JSON", e);
        }

        return entities;
    }

    public static List<Transaction> loadTransactionsFromJson(Context context) {
        List<TransactionEntity> entities = loadEntitiesFromJson(context);
        List<Transaction> transactions = new ArrayList<>();
        for (TransactionEntity e : entities) {
            transactions.add(new Transaction(
                    e.date,
                    e.amount,
                    e.receiver,
                    e.description,
                    e.utr,
                    e.comments,
                    e.category,
                    e.transactionId,
                    e.paymentMethod,
                    e.textType,
                    e.entryId
            ));
        }
        return transactions;
    }

    public static boolean saveEntitiesToJson(Context context, List<TransactionEntity> entities) {
        if (context == null || entities == null) return false;
        try {
            File targetFile = findJsonFile(context);
            if (targetFile == null) {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Accounting");
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                targetFile = new File(dir, "transactions.json");
            }
            JSONArray array = new JSONArray();
            for (TransactionEntity e : entities) {
                JSONObject obj = new JSONObject();
                obj.put("entryId", e.entryId);
                obj.put("date", e.date != null ? e.date : "N/A");
                obj.put("amount", e.amount != null ? e.amount : "0");
                obj.put("receiver", e.receiver != null ? e.receiver : "Unknown");
                obj.put("description", e.description != null ? e.description : "");
                obj.put("utr", e.utr != null ? e.utr : "");
                obj.put("transactionId", e.transactionId != null ? e.transactionId : "");
                obj.put("comments", e.comments != null ? e.comments : "");
                obj.put("category", e.category != null ? e.category : "General");
                obj.put("paymentMethod", e.paymentMethod != null ? e.paymentMethod : "Cash");
                obj.put("textType", e.textType != null ? e.textType : "Debit");
                array.put(obj);
            }
            try (FileWriter writer = new FileWriter(targetFile)) {
                writer.write(array.toString(4));
            }
            Log.d(TAG, "Successfully wrote " + entities.size() + " records to " + targetFile.getAbsolutePath());
            return true;
        } catch (Exception ex) {
            Log.e(TAG, "Failed to save entities to JSON", ex);
            return false;
        }
    }
}
