package com.k7sunny.balancex;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.view.WindowManager;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.core.content.ContextCompat;
import android.widget.FrameLayout;

import com.k7sunny.balancex.data.db.DatabaseMigrator;
import com.k7sunny.balancex.data.repository.BudgetGoalRepository;
import com.k7sunny.balancex.data.repository.SubscriptionRepository;
import com.k7sunny.balancex.data.repository.TransactionRepository;
import com.k7sunny.balancex.security.BiometricLockHelper;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class SettingsActivity extends BaseSecureActivity {

    // Views
    private TextView textCurrentTheme;
    private MaterialSwitch switchHaptics;
    private TextView iconCurrencySymbol;
    private TextView textCurrentCurrency;
    private TextView textCurrentPaymentMethod;
    private TextView textCurrentTxType;
    private TextView textStoragePath;
    private TextView textTransactionsCount;
    private TextView textFileSize;
    private TextView textVersionDisplay;

    // Security & Privacy views
    private MaterialSwitch switchBiometricLock;
    private MaterialSwitch switchFlagSecure;
    private TextView textCurrentGracePeriod;
    private MaterialSwitch switchShowNotificationAmounts;
    private View layoutLegacyFileWipe;
    private TextView textLegacyFileStatus;
    private TextView btnShredLegacy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ensure user's theme is active
        SettingsManager.applyTheme(this);
        setContentView(R.layout.activity_settings);

        setupWindowInsets();
        initViews();
        loadSavedSettings();
        setupListeners();
        setupNavigationDock();
        loadLedgerStats();
    }

    private void setupWindowInsets() {
        View coordinator = findViewById(R.id.coordinator);
        if (coordinator != null) {
            ViewCompat.setOnApplyWindowInsetsListener(coordinator, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(insets.left, insets.top, insets.right, 0);

                View navbar = findViewById(R.id.navbar);
                if (navbar != null) {
                    int baseBottomPadding = (int) (6 * getResources().getDisplayMetrics().density);
                    navbar.setPadding(
                            navbar.getPaddingLeft(),
                            navbar.getPaddingTop(),
                            navbar.getPaddingRight(),
                            baseBottomPadding + insets.bottom);
                }
                return windowInsets;
            });
        }
    }

    private void initViews() {
        textCurrentTheme = findViewById(R.id.textCurrentTheme);
        switchHaptics = findViewById(R.id.switchHaptics);
        iconCurrencySymbol = findViewById(R.id.iconCurrencySymbol);
        textCurrentCurrency = findViewById(R.id.textCurrentCurrency);
        textCurrentPaymentMethod = findViewById(R.id.textCurrentPaymentMethod);
        textCurrentTxType = findViewById(R.id.textCurrentTxType);
        textStoragePath = findViewById(R.id.textStoragePath);
        textTransactionsCount = findViewById(R.id.textTransactionsCount);
        textFileSize = findViewById(R.id.textFileSize);
        textVersionDisplay = findViewById(R.id.textVersionDisplay);

        // Security & Privacy views
        switchBiometricLock = findViewById(R.id.switchBiometricLock);
        switchFlagSecure = findViewById(R.id.switchFlagSecure);
        textCurrentGracePeriod = findViewById(R.id.textCurrentGracePeriod);
        switchShowNotificationAmounts = findViewById(R.id.switchShowNotificationAmounts);
        layoutLegacyFileWipe = findViewById(R.id.layoutLegacyFileWipe);
        textLegacyFileStatus = findViewById(R.id.textLegacyFileStatus);
        btnShredLegacy = findViewById(R.id.btnShredLegacy);

        // Header back button
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                vibrateDevice();
                finish();
            });
        }
    }

    private void loadSavedSettings() {
        // Theme
        String savedTheme = SettingsManager.getTheme(this);
        if (SettingsManager.THEME_LIGHT.equalsIgnoreCase(savedTheme)) {
            textCurrentTheme.setText("Light Mode");
        } else if (SettingsManager.THEME_DARK.equalsIgnoreCase(savedTheme)) {
            textCurrentTheme.setText("Dark Mode");
        } else {
            textCurrentTheme.setText("System Default");
        }

        // Haptics
        boolean hapticsEnabled = SettingsManager.isHapticsEnabled(this);
        if (switchHaptics != null) {
            switchHaptics.setChecked(hapticsEnabled);
        }

        // Currency
        String savedCurrency = SettingsManager.getCurrencyFull(this);
        textCurrentCurrency.setText(savedCurrency);
        if (iconCurrencySymbol != null) {
            iconCurrencySymbol.setText(SettingsManager.getCurrencySymbol(this));
        }

        // Default Payment Method
        String savedPaymentMethod = SettingsManager.getDefaultPaymentMethod(this);
        textCurrentPaymentMethod.setText(savedPaymentMethod);

        // Default Transaction Type
        String savedTxType = SettingsManager.getDefaultTransactionType(this);
        textCurrentTxType.setText(savedTxType);

        // Security & Privacy
        if (switchBiometricLock != null) {
            switchBiometricLock.setChecked(SettingsManager.isBiometricLockEnabled(this));
        }
        if (switchFlagSecure != null) {
            switchFlagSecure.setChecked(SettingsManager.isFlagSecureEnabled(this));
        }
        updateGracePeriodText();
        if (switchShowNotificationAmounts != null) {
            switchShowNotificationAmounts.setChecked(SettingsManager.isShowAmountsInNotifications(this));
        }
        updateLegacyFileUI();

        // Load Ledger & DB Stats
        loadLedgerStats();

        // Version Display
        textVersionDisplay.setText("v" + getAppVersion());
    }

    private void setupListeners() {
        // Theme Dialog
        findViewById(R.id.layoutThemeSetting).setOnClickListener(v -> showThemeDialog());

        // Haptics Switch
        if (switchHaptics != null) {
            switchHaptics.setOnCheckedChangeListener((buttonView, isChecked) -> {
                SettingsManager.setHapticsEnabled(this, isChecked);
                if (isChecked) {
                    SettingsManager.vibrate(this);
                }
            });
        }

        // Currency Dialog
        findViewById(R.id.layoutCurrencySetting).setOnClickListener(v -> showCurrencyDialog());

        // Payment Method Dialog
        findViewById(R.id.layoutDefaultPaymentMethod).setOnClickListener(v -> showPaymentMethodDialog());

        // Transaction Type Dialog
        findViewById(R.id.layoutDefaultTxType).setOnClickListener(v -> showTransactionTypeDialog());

        // Storage Path Copy
        View.OnClickListener copyPathListener = v -> {
            copyToClipboard("Storage Path", textStoragePath.getText().toString());
        };
        findViewById(R.id.layoutStoragePath).setOnClickListener(copyPathListener);
        findViewById(R.id.btnCopyPath).setOnClickListener(copyPathListener);

        // Clear Data (Danger Action)
        findViewById(R.id.layoutClearData).setOnClickListener(v -> showClearDataConfirmationDialog());

        // Legacy Plaintext File Wipe
        View.OnClickListener shredListener = v -> handleShredLegacyJson();
        if (layoutLegacyFileWipe != null) {
            layoutLegacyFileWipe.setOnClickListener(shredListener);
        }
        if (btnShredLegacy != null) {
            btnShredLegacy.setOnClickListener(shredListener);
        }

        // Biometric App Lock
        if (switchBiometricLock != null) {
            switchBiometricLock.setOnClickListener(v -> {
                boolean targetState = switchBiometricLock.isChecked();
                // Revert visual toggle until confirmed
                switchBiometricLock.setChecked(!targetState);
                if (targetState) {
                    if (!BiometricLockHelper.canAuthenticate(this)) {
                        Toast.makeText(this, "Biometrics or device lock is not set up on this device.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    BiometricLockHelper.promptAuthentication(
                            this,
                            "Enable App Lock",
                            "Authenticate to enable biometric protection",
                            new BiometricLockHelper.AuthCallback() {
                                @Override
                                public void onSuccess() {
                                    SettingsManager.setBiometricLockEnabled(SettingsActivity.this, true);
                                    switchBiometricLock.setChecked(true);
                                    Toast.makeText(SettingsActivity.this, "App lock enabled", Toast.LENGTH_SHORT).show();
                                }
                                @Override
                                public void onFailure(String errorMsg) {
                                    Toast.makeText(SettingsActivity.this, "Authentication required to enable app lock", Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                } else {
                    BiometricLockHelper.promptAuthentication(
                            this,
                            "Disable App Lock",
                            "Authenticate to disable biometric protection",
                            new BiometricLockHelper.AuthCallback() {
                                @Override
                                public void onSuccess() {
                                    SettingsManager.setBiometricLockEnabled(SettingsActivity.this, false);
                                    switchBiometricLock.setChecked(false);
                                    Toast.makeText(SettingsActivity.this, "App lock disabled", Toast.LENGTH_SHORT).show();
                                }
                                @Override
                                public void onFailure(String errorMsg) {
                                    Toast.makeText(SettingsActivity.this, "Authentication required to disable app lock", Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                }
            });
        }

        // FLAG_SECURE (Hide in recent apps)
        if (switchFlagSecure != null) {
            switchFlagSecure.setOnCheckedChangeListener((buttonView, isChecked) -> {
                SettingsManager.setFlagSecureEnabled(this, isChecked);
                if (isChecked) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
                } else {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
                }
                vibrateDevice();
            });
        }

        // Grace Period
        View layoutGrace = findViewById(R.id.layoutGracePeriod);
        if (layoutGrace != null) {
            layoutGrace.setOnClickListener(v -> showGracePeriodDialog());
        }

        // Show Amounts in Notifications
        if (switchShowNotificationAmounts != null) {
            switchShowNotificationAmounts.setOnCheckedChangeListener((buttonView, isChecked) -> {
                SettingsManager.setShowAmountsInNotifications(this, isChecked);
                vibrateDevice();
            });
        }

        // Notifications & Alerts
        View layoutNotif = findViewById(R.id.layoutNotificationSetting);
        if (layoutNotif != null) {
            layoutNotif.setOnClickListener(v -> {
                vibrateDevice();
                startActivity(new Intent(this, NotificationActivity.class));
            });
        }

        // About BalanceX
        findViewById(R.id.layoutAboutApp).setOnClickListener(v -> {
            vibrateDevice();
            startActivity(new Intent(this, AboutActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSavedSettings();
        updateNotificationBadge();
    }

    private void updateNotificationBadge() {
        TextView textSettingNotifBadge = findViewById(R.id.textSettingNotifBadge);
        if (textSettingNotifBadge != null) {
            new com.k7sunny.balancex.data.repository.NotificationRepository(this).getUnreadCount(count -> runOnUiThread(() -> {
                if (count > 0) {
                    textSettingNotifBadge.setVisibility(View.VISIBLE);
                    textSettingNotifBadge.setText(count + " New");
                } else {
                    textSettingNotifBadge.setVisibility(View.GONE);
                }
            }));
        }
    }

    private void showThemeDialog() {
        vibrateDevice();
        final String[] themes = {"System Default", "Light Mode", "Dark Mode"};
        String currentTheme = SettingsManager.getTheme(this);
        int selectedIndex = 0;
        if (SettingsManager.THEME_LIGHT.equalsIgnoreCase(currentTheme)) selectedIndex = 1;
        else if (SettingsManager.THEME_DARK.equalsIgnoreCase(currentTheme)) selectedIndex = 2;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Theme Mode")
                .setSingleChoiceItems(themes, selectedIndex, (dialog, which) -> {
                    dialog.dismiss();
                    vibrateDevice();
                    if (which == 1) {
                        SettingsManager.setTheme(this, SettingsManager.THEME_LIGHT);
                        textCurrentTheme.setText("Light Mode");
                    } else if (which == 2) {
                        SettingsManager.setTheme(this, SettingsManager.THEME_DARK);
                        textCurrentTheme.setText("Dark Mode");
                    } else {
                        SettingsManager.setTheme(this, SettingsManager.THEME_SYSTEM);
                        textCurrentTheme.setText("System Default");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCurrencyDialog() {
        vibrateDevice();
        final String[] currencies = {
                "₹ INR (Indian Rupee)",
                "$ USD (US Dollar)",
                "€ EUR (Euro)",
                "£ GBP (British Pound)",
                "¥ JPY (Japanese Yen)",
                "A$ AUD (Australian Dollar)",
                "C$ CAD (Canadian Dollar)"
        };
        String currentCurrency = SettingsManager.getCurrencyFull(this);
        int selectedIndex = 0;
        for (int i = 0; i < currencies.length; i++) {
            if (currencies[i].equalsIgnoreCase(currentCurrency)) {
                selectedIndex = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Currency Format")
                .setSingleChoiceItems(currencies, selectedIndex, (dialog, which) -> {
                    dialog.dismiss();
                    vibrateDevice();
                    String selected = currencies[which];
                    SettingsManager.setCurrency(this, selected);
                    textCurrentCurrency.setText(selected);
                    if (iconCurrencySymbol != null) {
                        iconCurrencySymbol.setText(SettingsManager.getCurrencySymbol(this));
                    }
                    Toast.makeText(this, "Currency set to " + selected, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPaymentMethodDialog() {
        vibrateDevice();
        final String[] methods = {"UPI", "Cash", "Card", "Net Banking", "Cheque"};
        String currentMethod = SettingsManager.getDefaultPaymentMethod(this);
        int selectedIndex = 0;
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].equalsIgnoreCase(currentMethod)) {
                selectedIndex = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Default Payment Method")
                .setSingleChoiceItems(methods, selectedIndex, (dialog, which) -> {
                    dialog.dismiss();
                    vibrateDevice();
                    String selected = methods[which];
                    SettingsManager.setDefaultPaymentMethod(this, selected);
                    textCurrentPaymentMethod.setText(selected);
                    Toast.makeText(this, "Default method set to " + selected, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showTransactionTypeDialog() {
        vibrateDevice();
        final String[] types = {"Debit (Expense)", "Credit (Income)"};
        String currentType = SettingsManager.getDefaultTransactionType(this);
        int selectedIndex = (currentType != null && currentType.startsWith("Credit")) ? 1 : 0;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Default Transaction Type")
                .setSingleChoiceItems(types, selectedIndex, (dialog, which) -> {
                    dialog.dismiss();
                    vibrateDevice();
                    String selected = types[which];
                    SettingsManager.setDefaultTransactionType(this, selected);
                    textCurrentTxType.setText(selected);
                    Toast.makeText(this, "Default type set to " + selected, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showClearDataConfirmationDialog() {
        vibrateDevice();

        final MaterialCheckBox checkIncludeAll = new MaterialCheckBox(this);
        checkIncludeAll.setText("Also delete budgets, goals, and subscriptions");
        checkIncludeAll.setTextSize(14f);
        checkIncludeAll.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        checkIncludeAll.setPadding(pad, pad / 2, pad, pad / 2);

        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.leftMargin = pad;
        params.rightMargin = pad;
        container.addView(checkIncludeAll, params);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Clear Transaction Records?")
                .setMessage("Are you sure you want to permanently delete all transactions from your ledger? This action cannot be reversed.")
                .setView(container)
                .setPositiveButton("Clear Data", (dialog, which) -> {
                    boolean alsoClearPhase3 = checkIncludeAll.isChecked();
                    clearLedgerData(alsoClearPhase3);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearLedgerData(boolean alsoClearPhase3) {
        try {
            // 1. Wipe Room Database atomically
            TransactionRepository repo = new TransactionRepository(this);
            repo.clearAllTransactionsAtomic(() -> {
                if (alsoClearPhase3) {
                    BudgetGoalRepository bgRepo = new BudgetGoalRepository(SettingsActivity.this);
                    bgRepo.deleteAll(null);
                    SubscriptionRepository subRepo = new SubscriptionRepository(SettingsActivity.this);
                    subRepo.deleteAll(null);
                } else {
                    BudgetGoalRepository bgRepo = new BudgetGoalRepository(SettingsActivity.this);
                    bgRepo.resetAllBudgetSpends(null);
                }
                runOnUiThread(() -> {
                    vibrateDevice();
                    String msg = alsoClearPhase3 ? "All transactions, budgets, and subscriptions cleared" : "All transactions cleared successfully";
                    Toast.makeText(SettingsActivity.this, msg, Toast.LENGTH_SHORT).show();
                    loadLedgerStats();
                });
            });
        } catch (Exception e) {
            Toast.makeText(this, "Error clearing records: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadLedgerStats() {
        try {
            File dbFile = getDatabasePath("balancex_encrypted_database.db");
            long bytes = (dbFile != null && dbFile.exists()) ? dbFile.length() : 0;
            String formattedSize;
            if (bytes < 1024) {
                formattedSize = bytes + " B";
            } else if (bytes < 1024 * 1024) {
                formattedSize = String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0);
            } else {
                formattedSize = String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024.0));
            }
            textFileSize.setText(formattedSize);

            // Legacy migration status indicator
            boolean legacyImported = getSharedPreferences("balancex_prefs", Context.MODE_PRIVATE)
                    .getBoolean("legacy_json_imported", false);
            File legacyJson = DatabaseMigrator.findJsonFile(this);
            String legacyStatus;
            if (legacyImported) {
                legacyStatus = "Legacy Data: Imported";
            } else if (legacyJson != null && legacyJson.exists()) {
                legacyStatus = "Legacy JSON: Found (Pending Import)";
            } else {
                legacyStatus = "Legacy JSON: None found";
            }
            textStoragePath.setText("SQLCipher Encrypted DB (" + legacyStatus + ")");

            TransactionRepository repo = new TransactionRepository(this);
            repo.getAllTransactions(transactions -> runOnUiThread(() -> {
                int count = transactions != null ? transactions.size() : 0;
                textTransactionsCount.setText(count + (count == 1 ? " record" : " records"));
            }));
        } catch (Exception e) {
            textTransactionsCount.setText("0 records");
            textFileSize.setText("0 KB");
        }
    }

    private void setupNavigationDock() {
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                startActivity(new Intent(this, MainActivity.class));
                vibrateDevice();
                finish();
            });
        }

        View navTransactions = findViewById(R.id.navTransactions);
        if (navTransactions != null) {
            navTransactions.setOnClickListener(v -> {
                startActivity(new Intent(this, HistoryActivity.class));
                vibrateDevice();
                finish();
            });
        }

        View navAdd = findViewById(R.id.navAdd);
        if (navAdd != null) {
            navAdd.setOnClickListener(v -> {
                startActivity(new Intent(this, EntryActivity.class));
                vibrateDevice();
                finish();
            });
        }

        View navExport = findViewById(R.id.navExport);
        if (navExport != null) {
            navExport.setOnClickListener(v -> {
                startActivity(new Intent(this, ReportsActivity.class));
                vibrateDevice();
                finish();
            });
        }

        // Settings is the current activity
        View navSettings = findViewById(R.id.navSettings);
        if (navSettings != null) {
            navSettings.setOnClickListener(v -> vibrateDevice());
        }
    }

    private void copyToClipboard(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
        }
        vibrateDevice();
        Toast.makeText(this, label + " copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void vibrateDevice() {
        SettingsManager.vibrate(this);
    }

    private void updateGracePeriodText() {
        if (textCurrentGracePeriod == null) return;
        long grace = SettingsManager.getLockGracePeriodMillis(this);
        if (grace == SettingsManager.GRACE_PERIOD_IMMEDIATE) {
            textCurrentGracePeriod.setText("Immediately");
        } else if (grace == SettingsManager.GRACE_PERIOD_1_MINUTE) {
            textCurrentGracePeriod.setText("1 minute");
        } else if (grace == SettingsManager.GRACE_PERIOD_5_MINUTES) {
            textCurrentGracePeriod.setText("5 minutes");
        } else {
            textCurrentGracePeriod.setText("30 seconds");
        }
    }

    private void showGracePeriodDialog() {
        vibrateDevice();
        final String[] options = {"Immediately", "30 seconds", "1 minute", "5 minutes"};
        final long[] values = {
                SettingsManager.GRACE_PERIOD_IMMEDIATE,
                SettingsManager.GRACE_PERIOD_30_SECONDS,
                SettingsManager.GRACE_PERIOD_1_MINUTE,
                SettingsManager.GRACE_PERIOD_5_MINUTES
        };

        long current = SettingsManager.getLockGracePeriodMillis(this);
        int selectedIndex = 1; // default 30s
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                selectedIndex = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Lock Grace Period")
                .setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                    dialog.dismiss();
                    vibrateDevice();
                    SettingsManager.setLockGracePeriodMillis(this, values[which]);
                    updateGracePeriodText();
                    Toast.makeText(this, "Grace period set to " + options[which], Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateLegacyFileUI() {
        if (textLegacyFileStatus == null) return;
        File legacy = DatabaseMigrator.findJsonFile(this);
        if (legacy != null && legacy.exists()) {
            textLegacyFileStatus.setText("Found: " + legacy.getName() + " (" + legacy.length() + " B). Tap to shred.");
            if (btnShredLegacy != null) {
                btnShredLegacy.setVisibility(View.VISIBLE);
            }
        } else {
            textLegacyFileStatus.setText("No unencrypted legacy file found. Storage is secure.");
            if (btnShredLegacy != null) {
                btnShredLegacy.setVisibility(View.GONE);
            }
        }
    }

    private void handleShredLegacyJson() {
        vibrateDevice();
        File legacy = DatabaseMigrator.findJsonFile(this);
        if (legacy == null || !legacy.exists()) {
            Toast.makeText(this, "No plaintext legacy file found.", Toast.LENGTH_SHORT).show();
            updateLegacyFileUI();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Shred Legacy Plaintext File?")
                .setMessage("This will overwrite the plaintext transactions.json with zero bytes and permanently delete it from device storage.\n\nAll your ledger data remains fully intact inside the Keystore-encrypted database.")
                .setPositiveButton("Shred Permanently", (dialog, which) -> {
                    boolean success = DatabaseMigrator.securelyWipeLegacyJson(this);
                    vibrateDevice();
                    if (success) {
                        Toast.makeText(this, "Legacy file securely shredded.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Legacy file could not be removed.", Toast.LENGTH_SHORT).show();
                    }
                    updateLegacyFileUI();
                    loadLedgerStats();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String getAppVersion() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return packageInfo.versionName != null ? packageInfo.versionName : "1.0.0";
        } catch (PackageManager.NameNotFoundException e) {
            return "1.0.0";
        }
    }
}
