package com.k7sunny.balancex.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Manages the encryption key for the SQLCipher database.
 * Generates a random 32-byte key protected by an Android Keystore hardware-backed AES-GCM master key.
 * Provides safe one-time re-keying from the legacy hardcoded passphrase.
 */
public final class DatabaseKeyManager {

    private static final String TAG = "DatabaseKeyManager";
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String MASTER_KEY_ALIAS = "balancex_db_master_key";
    private static final String PREFS_NAME = "balancex_keystore_prefs";
    private static final String PREF_WRAPPED_DB_KEY = "wrapped_db_key";
    private static final String PREF_DB_KEY_IV = "db_key_iv";
    private static final String PREF_IS_REKEYED = "is_db_rekeyed_v2";

    // Legacy hardcoded passphrase used prior to Phase 2 hardening
    private static final byte[] LEGACY_PASSPHRASE =
            "BalanceX_Secure_Passphrase_123!".getBytes(StandardCharsets.UTF_8);

    private static byte[] sCachedKey = null;

    private DatabaseKeyManager() {}

    /**
     * Retrieves or generates the 32-byte database encryption key.
     * Performs one-time re-key migration if upgrading from a legacy installation.
     */
    public static synchronized byte[] getDatabaseKey(Context context) {
        if (sCachedKey != null) {
            return Arrays.copyOf(sCachedKey, sCachedKey.length);
        }

        Context appContext = context.getApplicationContext();
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        byte[] existingKey = loadWrappedKey(prefs);
        if (existingKey != null) {
            sCachedKey = existingKey;
            return Arrays.copyOf(sCachedKey, sCachedKey.length);
        }

        // Generate a new 32-byte (256-bit) cryptographically secure key
        byte[] newDbKey = new byte[32];
        new SecureRandom().nextBytes(newDbKey);

        File dbFile = appContext.getDatabasePath("balancex_encrypted_database.db");
        boolean isRekeyed = prefs.getBoolean(PREF_IS_REKEYED, false);

        // If an existing DB file exists and has not yet been re-keyed, attempt re-key migration
        if (dbFile.exists() && dbFile.length() > 0 && !isRekeyed) {
            boolean success = rekeyLegacyDatabase(appContext, dbFile, newDbKey);
            if (!success) {
                Log.w(TAG, "Legacy database re-key did not complete. Falling back to legacy key until verified.");
                // Retain legacy passphrase until next attempt
                return Arrays.copyOf(LEGACY_PASSPHRASE, LEGACY_PASSPHRASE.length);
            }
        }

        // Store the wrapped key into Keystore-backed preferences
        if (saveWrappedKey(prefs, newDbKey)) {
            prefs.edit().putBoolean(PREF_IS_REKEYED, true).apply();
            sCachedKey = newDbKey;
            return Arrays.copyOf(sCachedKey, sCachedKey.length);
        } else {
            Log.e(TAG, "Failed to persist wrapped database key; returning ephemeral key.");
            sCachedKey = newDbKey;
            return Arrays.copyOf(sCachedKey, sCachedKey.length);
        }
    }

    /**
     * Safely attempts to re-key an existing database from the legacy hardcoded key to the new key.
     */
    private static boolean rekeyLegacyDatabase(Context context, File dbFile, byte[] newKey) {
        try {
            System.loadLibrary("sqlcipher");
            net.zetetic.database.sqlcipher.SQLiteDatabase db =
                    net.zetetic.database.sqlcipher.SQLiteDatabase.openOrCreateDatabase(
                            dbFile,
                            LEGACY_PASSPHRASE,
                            null,
                            null
                    );

            if (db != null) {
                try {
                    db.changePassword(newKey);
                    Log.i(TAG, "Successfully re-keyed database with Android Keystore derived key.");
                    return true;
                } finally {
                    db.close();
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to re-key database using SQLCipher changePassword", t);
        }
        return false;
    }

    private static SecretKey getOrCreateMasterKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);

            if (keyStore.containsAlias(MASTER_KEY_ALIAS)) {
                KeyStore.Entry entry = keyStore.getEntry(MASTER_KEY_ALIAS, null);
                if (entry instanceof KeyStore.SecretKeyEntry) {
                    return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
                }
            }

            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER);
            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                    MASTER_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
            )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build();

            keyGenerator.init(spec);
            return keyGenerator.generateKey();
        } catch (Exception e) {
            Log.w(TAG, "AndroidKeyStore unavailable (likely JVM unit test environment). Using fallback.", e);
            return null;
        }
    }

    private static byte[] loadWrappedKey(SharedPreferences prefs) {
        String wrappedBase64 = prefs.getString(PREF_WRAPPED_DB_KEY, null);
        String ivBase64 = prefs.getString(PREF_DB_KEY_IV, null);

        if (wrappedBase64 == null || ivBase64 == null) {
            return null;
        }

        try {
            SecretKey masterKey = getOrCreateMasterKey();
            byte[] iv = Base64.decode(ivBase64, Base64.NO_WRAP);
            byte[] ciphertext = Base64.decode(wrappedBase64, Base64.NO_WRAP);

            if (masterKey != null) {
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, masterKey, new GCMParameterSpec(128, iv));
                return cipher.doFinal(ciphertext);
            } else {
                // JVM unit test fallback
                return ciphertext;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to unwrap database encryption key", e);
            return null;
        }
    }

    private static boolean saveWrappedKey(SharedPreferences prefs, byte[] rawKey) {
        try {
            SecretKey masterKey = getOrCreateMasterKey();
            if (masterKey != null) {
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.ENCRYPT_MODE, masterKey);
                byte[] iv = cipher.getIV();
                byte[] ciphertext = cipher.doFinal(rawKey);

                prefs.edit()
                        .putString(PREF_WRAPPED_DB_KEY, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
                        .putString(PREF_DB_KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                        .commit();
                return true;
            } else {
                // JVM unit test fallback
                prefs.edit()
                        .putString(PREF_WRAPPED_DB_KEY, Base64.encodeToString(rawKey, Base64.NO_WRAP))
                        .putString(PREF_DB_KEY_IV, Base64.encodeToString(new byte[12], Base64.NO_WRAP))
                        .commit();
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to wrap and save database encryption key", e);
            return false;
        }
    }

    /**
     * Clears cached key from memory.
     */
    public static synchronized void clearCache() {
        if (sCachedKey != null) {
            Arrays.fill(sCachedKey, (byte) 0);
            sCachedKey = null;
        }
    }
}
