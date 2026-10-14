package com.k7sunny.balancex.security;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.k7sunny.balancex.data.entity.BudgetGoalEntity;
import com.k7sunny.balancex.data.entity.NotificationEntity;
import com.k7sunny.balancex.data.entity.SubscriptionEntity;
import com.k7sunny.balancex.data.entity.TransactionEntity;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BackupManagerTest {

    @Test
    public void testEncryptedBackupRoundTrip() throws Exception {
        BackupManager.BackupPayload originalPayload = new BackupManager.BackupPayload();

        TransactionEntity tx = new TransactionEntity();
        tx.entryId = 1;
        tx.amount = "1,500.50";
        tx.amountMinor = 150050;
        tx.date = "2026-03-15";
        tx.dateEpochDay = 20527L;
        tx.receiver = "Coffee Roasters";
        tx.textType = "Debit";
        tx.category = "Food";
        originalPayload.transactions.add(tx);

        SubscriptionEntity sub = new SubscriptionEntity();
        sub.id = 10;
        sub.name = "Cloud Storage";
        sub.amount = 199.0;
        sub.billingCycle = "MONTHLY";
        originalPayload.subscriptions.add(sub);

        BudgetGoalEntity goal = new BudgetGoalEntity();
        goal.id = 5;
        goal.title = "Emergency Fund";
        goal.targetAmount = 50000;
        goal.currentAmount = 10000;
        goal.type = "GOAL";
        originalPayload.budgetGoals.add(goal);

        NotificationEntity notif = new NotificationEntity("Welcome", "Test notif", "ALERT", null, null);
        originalPayload.notifications.add(notif);

        originalPayload.settings.put("app_theme", "dark");

        // Manually build container as if exported
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        byte[] payloadBytes = gson.toJson(originalPayload).getBytes(StandardCharsets.UTF_8);

        BackupManager.BackupContainer container = new BackupManager.BackupContainer();
        container.schemaVersion = BackupManager.CURRENT_SCHEMA_VERSION;
        container.isEncrypted = true;
        container.kdf = "PBKDF2WithHmacSHA256";
        container.iterations = BackupManager.PBKDF2_ITERATIONS;

        byte[] salt = new byte[BackupManager.SALT_LENGTH_BYTES];
        byte[] iv = new byte[BackupManager.IV_LENGTH_BYTES];
        for (int i = 0; i < salt.length; i++) salt[i] = (byte) i;
        for (int i = 0; i < iv.length; i++) iv[i] = (byte) (i * 2);

        // Access private crypt via reflection or extractPayload
        java.lang.reflect.Method encryptMethod = BackupManager.class.getDeclaredMethod(
                "encryptGcm", byte[].class, String.class, byte[].class, byte[].class, int.class);
        encryptMethod.setAccessible(true);
        byte[] cipherBytes = (byte[]) encryptMethod.invoke(null, payloadBytes, "StrongP@ssw0rd!", salt, iv, container.iterations);

        java.lang.reflect.Method base64EncodeMethod = BackupManager.class.getDeclaredMethod("base64Encode", byte[].class);
        base64EncodeMethod.setAccessible(true);
        container.salt = (String) base64EncodeMethod.invoke(null, (Object) salt);
        container.iv = (String) base64EncodeMethod.invoke(null, (Object) iv);
        container.cipherText = (String) base64EncodeMethod.invoke(null, (Object) cipherBytes);

        // 1. Decrypt with correct password
        BackupManager.BackupPayload decryptedPayload = BackupManager.extractPayload(container, "StrongP@ssw0rd!");
        assertNotNull(decryptedPayload);
        assertEquals(1, decryptedPayload.transactions.size());
        assertEquals("Coffee Roasters", decryptedPayload.transactions.get(0).receiver);
        assertEquals(150050, decryptedPayload.transactions.get(0).amountMinor);
        assertEquals("Cloud Storage", decryptedPayload.subscriptions.get(0).name);
        assertEquals("Emergency Fund", decryptedPayload.budgetGoals.get(0).title);
        assertEquals("dark", decryptedPayload.settings.get("app_theme"));

        // 2. Decrypt with wrong password fails
        try {
            BackupManager.extractPayload(container, "WrongPassword");
            fail("Expected InvalidPasswordException on incorrect password");
        } catch (BackupManager.InvalidPasswordException expected) {
            assertTrue(expected.getMessage().contains("Incorrect password"));
        }

        // 3. Decrypt without password fails
        try {
            BackupManager.extractPayload(container, null);
            fail("Expected PasswordRequiredException when password is null");
        } catch (BackupManager.PasswordRequiredException expected) {
            assertTrue(expected.getMessage().contains("password-protected"));
        }
    }

    @Test
    public void testUnencryptedBackupRoundTrip() throws Exception {
        BackupManager.BackupContainer container = new BackupManager.BackupContainer();
        container.isEncrypted = false;
        container.payload = new BackupManager.BackupPayload();

        TransactionEntity tx = new TransactionEntity();
        tx.entryId = 42;
        tx.receiver = "Metro Rail";
        container.payload.transactions.add(tx);

        BackupManager.BackupPayload extracted = BackupManager.extractPayload(container, null);
        assertNotNull(extracted);
        assertEquals(1, extracted.transactions.size());
        assertEquals("Metro Rail", extracted.transactions.get(0).receiver);
    }

    @Test
    public void testRejectFutureSchemaVersion() {
        BackupManager.BackupContainer container = new BackupManager.BackupContainer();
        container.schemaVersion = BackupManager.CURRENT_SCHEMA_VERSION + 99;
        container.isEncrypted = false;
        container.payload = new BackupManager.BackupPayload();

        try {
            BackupManager.restoreContainer(null, container, null, BackupManager.RestoreMode.MERGE);
            fail("Expected exception for future schema version");
        } catch (BackupManager.BackupException expected) {
            assertTrue(expected.getMessage().contains("newer version"));
        }
    }
}
