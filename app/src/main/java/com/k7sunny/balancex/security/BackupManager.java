package com.k7sunny.balancex.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.k7sunny.balancex.ProfileHelper;
import com.k7sunny.balancex.SettingsManager;
import com.k7sunny.balancex.data.db.AppDatabase;
import com.k7sunny.balancex.data.entity.BudgetGoalEntity;
import com.k7sunny.balancex.data.entity.NotificationEntity;
import com.k7sunny.balancex.data.entity.SubscriptionEntity;
import com.k7sunny.balancex.data.entity.TransactionEntity;
import com.k7sunny.balancex.util.AppLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class BackupManager {

    private static final String TAG = "BackupManager";
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final int APP_VERSION = 1;
    public static final int PBKDF2_ITERATIONS = 200_000;
    public static final int KEY_LENGTH_BITS = 256;
    public static final int GCM_TAG_LENGTH_BITS = 128;
    public static final int SALT_LENGTH_BYTES = 16;
    public static final int IV_LENGTH_BYTES = 12;

    public enum RestoreMode {
        MERGE,
        REPLACE
    }

    public static class BackupContainer {
        public int schemaVersion = CURRENT_SCHEMA_VERSION;
        public int appVersion = APP_VERSION;
        public long createdAt = System.currentTimeMillis();
        public boolean isEncrypted = false;
        public String kdf;
        public int iterations;
        public String salt;
        public String iv;
        public String cipherText;
        public BackupPayload payload;
    }

    public static class BackupPayload {
        public List<TransactionEntity> transactions = new ArrayList<>();
        public List<SubscriptionEntity> subscriptions = new ArrayList<>();
        public List<BudgetGoalEntity> budgetGoals = new ArrayList<>();
        public List<NotificationEntity> notifications = new ArrayList<>();
        public Map<String, Object> settings = new HashMap<>();
        public Map<String, Object> profile = new HashMap<>();
    }

    public static class RestoreResult {
        public int transactionsRestored = 0;
        public int subscriptionsRestored = 0;
        public int budgetGoalsRestored = 0;
        public int notificationsRestored = 0;
    }

    public static class BackupException extends Exception {
        public BackupException(String message) {
            super(message);
        }
        public BackupException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class PasswordRequiredException extends BackupException {
        public PasswordRequiredException(String message) {
            super(message);
        }
    }

    public static class InvalidPasswordException extends BackupException {
        public InvalidPasswordException(String message) {
            super(message);
        }
    }

    // ==================== EXPORT BACKUP ====================

    public static BackupContainer createBackupContainer(Context context, @Nullable String password) throws BackupException {
        AppDatabase db = AppDatabase.getDatabase(context);
        BackupPayload payload = new BackupPayload();

        payload.transactions = db.transactionDao().getAllTransactions();
        payload.subscriptions = db.subscriptionDao().getAllSubscriptions();
        payload.budgetGoals = db.budgetGoalDao().getAll();
        payload.notifications = db.notificationDao().getAll();

        // Export non-sensitive user settings
        SharedPreferences settingsPrefs = context.getSharedPreferences(SettingsManager.PREFS_NAME, Context.MODE_PRIVATE);
        for (Map.Entry<String, ?> entry : settingsPrefs.getAll().entrySet()) {
            payload.settings.put(entry.getKey(), entry.getValue());
        }

        // Export user profile preferences
        SharedPreferences profilePrefs = context.getSharedPreferences(ProfileHelper.PREF_NAME, Context.MODE_PRIVATE);
        for (Map.Entry<String, ?> entry : profilePrefs.getAll().entrySet()) {
            payload.profile.put(entry.getKey(), entry.getValue());
        }

        BackupContainer container = new BackupContainer();
        container.schemaVersion = CURRENT_SCHEMA_VERSION;
        container.appVersion = APP_VERSION;
        container.createdAt = System.currentTimeMillis();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        if (password != null && !password.trim().isEmpty()) {
            // AES-256-GCM + PBKDF2 (200k iterations)
            container.isEncrypted = true;
            container.kdf = "PBKDF2WithHmacSHA256";
            container.iterations = PBKDF2_ITERATIONS;

            try {
                byte[] salt = new byte[SALT_LENGTH_BYTES];
                byte[] iv = new byte[IV_LENGTH_BYTES];
                SecureRandom random = new SecureRandom();
                random.nextBytes(salt);
                random.nextBytes(iv);

                byte[] payloadBytes = gson.toJson(payload).getBytes(StandardCharsets.UTF_8);
                byte[] cipherBytes = encryptGcm(payloadBytes, password, salt, iv, container.iterations);

                container.salt = base64Encode(salt);
                container.iv = base64Encode(iv);
                container.cipherText = base64Encode(cipherBytes);
                container.payload = null;
            } catch (Exception e) {
                AppLogger.e(TAG, "Failed to encrypt backup container", e);
                throw new BackupException("Encryption failed: " + e.getMessage(), e);
            }
        } else {
            container.isEncrypted = false;
            container.payload = payload;
        }

        return container;
    }

    public static void exportBackup(Context context, OutputStream outputStream, @Nullable String password) throws BackupException {
        try {
            BackupContainer container = createBackupContainer(context, password);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(container);
            outputStream.write(json.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        } catch (BackupException be) {
            throw be;
        } catch (Exception e) {
            AppLogger.e(TAG, "Failed to write backup output stream", e);
            throw new BackupException("Failed to write backup: " + e.getMessage(), e);
        }
    }

    // ==================== RESTORE BACKUP ====================

    public static BackupContainer parseContainer(InputStream inputStream) throws BackupException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            Gson gson = new Gson();
            BackupContainer container = gson.fromJson(reader, BackupContainer.class);
            if (container == null) {
                throw new BackupException("Backup file is empty or corrupted.");
            }
            return container;
        } catch (Exception e) {
            AppLogger.e(TAG, "Failed to parse backup container JSON", e);
            throw new BackupException("Invalid backup file: " + e.getMessage(), e);
        }
    }

    public static BackupPayload extractPayload(BackupContainer container, @Nullable String password) throws BackupException {
        if (!container.isEncrypted) {
            if (container.payload == null) {
                throw new BackupException("Backup container missing payload data.");
            }
            return container.payload;
        }

        if (password == null || password.trim().isEmpty()) {
            throw new PasswordRequiredException("This backup is password-protected. Please enter the password.");
        }

        try {
            byte[] salt = base64Decode(container.salt);
            byte[] iv = base64Decode(container.iv);
            byte[] cipherBytes = base64Decode(container.cipherText);
            int iterations = container.iterations > 0 ? container.iterations : PBKDF2_ITERATIONS;

            byte[] decryptedBytes = decryptGcm(cipherBytes, password, salt, iv, iterations);
            String json = new String(decryptedBytes, StandardCharsets.UTF_8);

            Gson gson = new Gson();
            BackupPayload payload = gson.fromJson(json, BackupPayload.class);
            if (payload == null) {
                throw new BackupException("Decrypted payload is empty or malformed.");
            }
            return payload;
        } catch (javax.crypto.AEADBadTagException e) {
            AppLogger.e(TAG, "Bad tag during backup decryption (incorrect password)", e);
            throw new InvalidPasswordException("Incorrect password or corrupted backup file.");
        } catch (BackupException be) {
            throw be;
        } catch (Exception e) {
            AppLogger.e(TAG, "Failed to decrypt backup", e);
            throw new BackupException("Decryption failed: " + e.getMessage(), e);
        }
    }

    public static RestoreResult restoreBackup(Context context, InputStream inputStream, @Nullable String password, RestoreMode mode) throws BackupException {
        BackupContainer container = parseContainer(inputStream);
        return restoreContainer(context, container, password, mode);
    }

    public static RestoreResult restoreContainer(Context context, BackupContainer container, @Nullable String password, RestoreMode mode) throws BackupException {
        if (container.schemaVersion > CURRENT_SCHEMA_VERSION) {
            throw new BackupException("This backup requires a newer version of BalanceX (Schema v"
                    + container.schemaVersion + ", current is v" + CURRENT_SCHEMA_VERSION + ").");
        }

        BackupPayload payload = extractPayload(container, password);

        // 1. Automatic pre-restore backup in app-private storage
        createPreRestoreBackup(context);

        AppDatabase db = AppDatabase.getDatabase(context);
        RestoreResult result = new RestoreResult();

        // 2. Atomic Room transaction covering all restored tables
        db.runInTransaction(() -> {
            if (mode == RestoreMode.REPLACE) {
                db.transactionDao().deleteAll();
                db.subscriptionDao().deleteAll();
                db.budgetGoalDao().deleteAll();
                db.notificationDao().deleteAll();

                if (payload.transactions != null && !payload.transactions.isEmpty()) {
                    db.transactionDao().insertOrReplaceAll(payload.transactions);
                    result.transactionsRestored = payload.transactions.size();
                }

                if (payload.subscriptions != null && !payload.subscriptions.isEmpty()) {
                    for (SubscriptionEntity s : payload.subscriptions) {
                        if (s.amountMinor == 0 && s.amount > 0) {
                            s.amountMinor = Math.round(s.amount * 100.0);
                        }
                        if (s.anchorDate == null || s.anchorDate.isEmpty()) {
                            s.anchorDate = s.nextDueDate;
                        }
                        if (s.paymentMethod == null) {
                            s.paymentMethod = "UPI";
                        }
                    }
                    db.subscriptionDao().insertAll(payload.subscriptions);
                    result.subscriptionsRestored = payload.subscriptions.size();
                }

                if (payload.budgetGoals != null && !payload.budgetGoals.isEmpty()) {
                    db.budgetGoalDao().insertAll(payload.budgetGoals);
                    result.budgetGoalsRestored = payload.budgetGoals.size();
                }

                if (payload.notifications != null && !payload.notifications.isEmpty()) {
                    db.notificationDao().insertAll(payload.notifications);
                    result.notificationsRestored = payload.notifications.size();
                }

                // Restore preferences in REPLACE mode
                applyPreferences(context, SettingsManager.PREFS_NAME, payload.settings);
                applyPreferences(context, ProfileHelper.PREF_NAME, payload.profile);

            } else { // MERGE
                // Transactions: deduplicate by signature
                List<TransactionEntity> existing = db.transactionDao().getAllTransactions();
                Set<String> signatures = new HashSet<>();
                long maxId = 0;
                if (existing != null) {
                    for (TransactionEntity ex : existing) {
                        signatures.add(makeSignature(ex));
                        if (ex.entryId > maxId) maxId = ex.entryId;
                    }
                }

                List<TransactionEntity> toInsertTx = new ArrayList<>();
                if (payload.transactions != null) {
                    for (TransactionEntity tx : payload.transactions) {
                        String sig = makeSignature(tx);
                        if (!signatures.contains(sig)) {
                            maxId++;
                            tx.entryId = maxId;
                            signatures.add(sig);
                            toInsertTx.add(tx);
                        }
                    }
                }
                if (!toInsertTx.isEmpty()) {
                    db.transactionDao().insertOrReplaceAll(toInsertTx);
                    result.transactionsRestored = toInsertTx.size();
                }

                // Subscriptions: deduplicate by (name + billingCycle)
                List<SubscriptionEntity> existingSubs = db.subscriptionDao().getAllSubscriptions();
                Set<String> subKeys = new HashSet<>();
                if (existingSubs != null) {
                    for (SubscriptionEntity s : existingSubs) {
                        subKeys.add((s.name != null ? s.name.trim().toLowerCase() : "") + "|" + s.billingCycle);
                    }
                }
                List<SubscriptionEntity> toInsertSubs = new ArrayList<>();
                if (payload.subscriptions != null) {
                    for (SubscriptionEntity s : payload.subscriptions) {
                        String key = (s.name != null ? s.name.trim().toLowerCase() : "") + "|" + s.billingCycle;
                        if (!subKeys.contains(key)) {
                            s.id = 0; // auto-generate new id
                            if (s.amountMinor == 0 && s.amount > 0) {
                                s.amountMinor = Math.round(s.amount * 100.0);
                            }
                            if (s.anchorDate == null || s.anchorDate.isEmpty()) {
                                s.anchorDate = s.nextDueDate;
                            }
                            if (s.paymentMethod == null) {
                                s.paymentMethod = "UPI";
                            }
                            subKeys.add(key);
                            toInsertSubs.add(s);
                        }
                    }
                }
                if (!toInsertSubs.isEmpty()) {
                    db.subscriptionDao().insertAll(toInsertSubs);
                    result.subscriptionsRestored = toInsertSubs.size();
                }

                // Budget Goals: deduplicate by (category + type)
                List<BudgetGoalEntity> existingGoals = db.budgetGoalDao().getAll();
                Set<String> goalKeys = new HashSet<>();
                if (existingGoals != null) {
                    for (BudgetGoalEntity g : existingGoals) {
                        goalKeys.add((g.category != null ? g.category.trim().toLowerCase() : "") + "|" + g.type);
                    }
                }
                List<BudgetGoalEntity> toInsertGoals = new ArrayList<>();
                if (payload.budgetGoals != null) {
                    for (BudgetGoalEntity g : payload.budgetGoals) {
                        String key = (g.category != null ? g.category.trim().toLowerCase() : "") + "|" + g.type;
                        if (!goalKeys.contains(key)) {
                            g.id = 0;
                            goalKeys.add(key);
                            toInsertGoals.add(g);
                        }
                    }
                }
                if (!toInsertGoals.isEmpty()) {
                    db.budgetGoalDao().insertAll(toInsertGoals);
                    result.budgetGoalsRestored = toInsertGoals.size();
                }

                // Notifications
                if (payload.notifications != null && !payload.notifications.isEmpty()) {
                    List<NotificationEntity> existingNotifs = db.notificationDao().getAll();
                    Set<String> notifKeys = new HashSet<>();
                    if (existingNotifs != null) {
                        for (NotificationEntity n : existingNotifs) {
                            notifKeys.add((n.title != null ? n.title : "") + "|" + (n.message != null ? n.message : ""));
                        }
                    }
                    List<NotificationEntity> toInsertNotifs = new ArrayList<>();
                    for (NotificationEntity n : payload.notifications) {
                        String key = (n.title != null ? n.title : "") + "|" + (n.message != null ? n.message : "");
                        if (!notifKeys.contains(key)) {
                            n.id = 0;
                            notifKeys.add(key);
                            toInsertNotifs.add(n);
                        }
                    }
                    if (!toInsertNotifs.isEmpty()) {
                        db.notificationDao().insertAll(toInsertNotifs);
                        result.notificationsRestored = toInsertNotifs.size();
                    }
                }
            }
        });

        return result;
    }

    private static String makeSignature(TransactionEntity e) {
        if (e == null) return "";
        return (e.dateEpochDay != null ? e.dateEpochDay : e.date) + "|"
                + e.amountMinor + "|"
                + (e.receiver != null ? e.receiver.trim().toLowerCase() : "") + "|"
                + (e.textType != null ? e.textType.trim().toLowerCase() : "") + "|"
                + (e.description != null ? e.description.trim() : "");
    }

    private static void applyPreferences(Context context, String prefName, Map<String, Object> prefsMap) {
        if (prefsMap == null || prefsMap.isEmpty()) return;
        SharedPreferences.Editor editor = context.getSharedPreferences(prefName, Context.MODE_PRIVATE).edit();
        for (Map.Entry<String, Object> entry : prefsMap.entrySet()) {
            Object val = entry.getValue();
            if (val instanceof Boolean) {
                editor.putBoolean(entry.getKey(), (Boolean) val);
            } else if (val instanceof Integer) {
                editor.putInt(entry.getKey(), (Integer) val);
            } else if (val instanceof Long) {
                editor.putLong(entry.getKey(), (Long) val);
            } else if (val instanceof Float) {
                editor.putFloat(entry.getKey(), (Float) val);
            } else if (val instanceof String) {
                editor.putString(entry.getKey(), (String) val);
            }
        }
        editor.apply();
    }

    public static void createPreRestoreBackup(Context context) {
        try {
            File backupDir = new File(context.getFilesDir(), "backups");
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
            File preRestoreFile = new File(backupDir, "pre_restore_backup.json");
            BackupContainer container = createBackupContainer(context, null);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileOutputStream fos = new FileOutputStream(preRestoreFile)) {
                fos.write(gson.toJson(container).getBytes(StandardCharsets.UTF_8));
            }
            AppLogger.d(TAG, "Created automatic pre-restore backup at " + preRestoreFile.getAbsolutePath());
        } catch (Exception e) {
            AppLogger.e(TAG, "Failed to create pre-restore backup", e);
        }
    }

    // ==================== CRYPTOGRAPHIC HELPERS ====================

    private static byte[] encryptGcm(byte[] plaintext, String password, byte[] salt, byte[] iv, int iterations) throws Exception {
        SecretKey secretKey = deriveKey(password, salt, iterations);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
        return cipher.doFinal(plaintext);
    }

    private static byte[] decryptGcm(byte[] ciphertext, String password, byte[] salt, byte[] iv, int iterations) throws Exception {
        SecretKey secretKey = deriveKey(password, salt, iterations);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
        return cipher.doFinal(ciphertext);
    }

    private static SecretKey deriveKey(String password, byte[] salt, int iterations) throws Exception {
        char[] chars = password.toCharArray();
        SecretKeyFactory factory;
        try {
            factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        } catch (Exception e) {
            factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        }
        PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, KEY_LENGTH_BITS);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    private static String base64Encode(byte[] data) {
        try {
            return java.util.Base64.getEncoder().encodeToString(data);
        } catch (Throwable t) {
            return android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP);
        }
    }

    private static byte[] base64Decode(String str) {
        try {
            return java.util.Base64.getDecoder().decode(str);
        } catch (Throwable t) {
            return android.util.Base64.decode(str, android.util.Base64.DEFAULT);
        }
    }
}
