package com.accounting.balancex;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "balancex_settings";
    private static final String KEY_THEME = "app_theme";
    private static final String KEY_HAPTICS = "pref_haptics";
    private static final String KEY_CURRENCY = "pref_currency";
    private static final String KEY_PAYMENT_METHOD = "pref_default_payment_method";
    private static final String KEY_TX_TYPE = "pref_default_tx_type";

    private SharedPreferences prefs;

    // Views
    private TextView textCurrentTheme;
    private MaterialSwitch switchHaptics;
    private TextView textCurrentCurrency;
    private TextView textCurrentPaymentMethod;
    private TextView textCurrentTxType;
    private TextView textStoragePath;
    private TextView textTransactionsCount;
    private TextView textFileSize;
    private TextView textVersionDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

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
        textCurrentCurrency = findViewById(R.id.textCurrentCurrency);
        textCurrentPaymentMethod = findViewById(R.id.textCurrentPaymentMethod);
        textCurrentTxType = findViewById(R.id.textCurrentTxType);
        textStoragePath = findViewById(R.id.textStoragePath);
        textTransactionsCount = findViewById(R.id.textTransactionsCount);
        textFileSize = findViewById(R.id.textFileSize);
        textVersionDisplay = findViewById(R.id.textVersionDisplay);

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
        String savedTheme = prefs.getString(KEY_THEME, "system");
        if ("light".equals(savedTheme)) {
            textCurrentTheme.setText("Light Mode");
        } else if ("dark".equals(savedTheme)) {
            textCurrentTheme.setText("Dark Mode");
        } else {
            textCurrentTheme.setText("System Default");
        }

        // Haptics
        boolean hapticsEnabled = prefs.getBoolean(KEY_HAPTICS, true);
        switchHaptics.setChecked(hapticsEnabled);

        // Currency
        String savedCurrency = prefs.getString(KEY_CURRENCY, "₹ INR (Indian Rupee)");
        textCurrentCurrency.setText(savedCurrency);

        // Default Payment Method
        String savedPaymentMethod = prefs.getString(KEY_PAYMENT_METHOD, "UPI");
        textCurrentPaymentMethod.setText(savedPaymentMethod);

        // Default Transaction Type
        String savedTxType = prefs.getString(KEY_TX_TYPE, "Debit (Expense)");
        textCurrentTxType.setText(savedTxType);

        // Storage Path display
        textStoragePath.setText("/storage/emulated/0/Documents/Accounting/transactions.json");

        // Version Display
        textVersionDisplay.setText("v" + getAppVersion());
    }

    private void setupListeners() {
        // Theme Dialog
        findViewById(R.id.layoutThemeSetting).setOnClickListener(v -> showThemeDialog());

        // Haptics Switch
        switchHaptics.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_HAPTICS, isChecked).apply();
            if (isChecked) {
                vibrateDevice();
            }
        });

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

        // About BalanceX
        findViewById(R.id.layoutAboutApp).setOnClickListener(v -> {
            vibrateDevice();
            startActivity(new Intent(this, AboutActivity.class));
        });
    }

    private void showThemeDialog() {
        vibrateDevice();
        final String[] themes = {"System Default", "Light Mode", "Dark Mode"};
        String currentTheme = prefs.getString(KEY_THEME, "system");
        int selectedIndex = 0;
        if ("light".equals(currentTheme)) selectedIndex = 1;
        else if ("dark".equals(currentTheme)) selectedIndex = 2;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Theme Mode")
                .setSingleChoiceItems(themes, selectedIndex, (dialog, which) -> {
                    dialog.dismiss();
                    vibrateDevice();
                    if (which == 1) {
                        prefs.edit().putString(KEY_THEME, "light").apply();
                        textCurrentTheme.setText("Light Mode");
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    } else if (which == 2) {
                        prefs.edit().putString(KEY_THEME, "dark").apply();
                        textCurrentTheme.setText("Dark Mode");
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    } else {
                        prefs.edit().putString(KEY_THEME, "system").apply();
                        textCurrentTheme.setText("System Default");
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
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
        String currentCurrency = prefs.getString(KEY_CURRENCY, currencies[0]);
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
                    prefs.edit().putString(KEY_CURRENCY, selected).apply();
                    textCurrentCurrency.setText(selected);
                    Toast.makeText(this, "Currency set to " + selected, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPaymentMethodDialog() {
        vibrateDevice();
        final String[] methods = {"UPI", "Cash", "Card", "Net Banking", "Cheque"};
        String currentMethod = prefs.getString(KEY_PAYMENT_METHOD, "UPI");
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
                    prefs.edit().putString(KEY_PAYMENT_METHOD, selected).apply();
                    textCurrentPaymentMethod.setText(selected);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showTransactionTypeDialog() {
        vibrateDevice();
        final String[] types = {"Debit (Expense)", "Credit (Income)"};
        String currentType = prefs.getString(KEY_TX_TYPE, types[0]);
        int selectedIndex = currentType.startsWith("Credit") ? 1 : 0;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Default Transaction Type")
                .setSingleChoiceItems(types, selectedIndex, (dialog, which) -> {
                    dialog.dismiss();
                    vibrateDevice();
                    String selected = types[which];
                    prefs.edit().putString(KEY_TX_TYPE, selected).apply();
                    textCurrentTxType.setText(selected);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showClearDataConfirmationDialog() {
        vibrateDevice();
        new MaterialAlertDialogBuilder(this)
                .setTitle("Clear All Transactions?")
                .setMessage("Are you sure you want to permanently delete all records stored in your ledger file? This action cannot be reversed.")
                .setPositiveButton("Clear All", (dialog, which) -> {
                    clearLedgerData();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearLedgerData() {
        try {
            File file = new File("/storage/emulated/0/Documents/Accounting/transactions.json");
            if (file.exists()) {
                FileWriter writer = new FileWriter(file, false);
                writer.write("[]");
                writer.flush();
                writer.close();
            }
            vibrateDevice();
            Toast.makeText(this, "Ledger records cleared successfully", Toast.LENGTH_SHORT).show();
            loadLedgerStats();
        } catch (Exception e) {
            Toast.makeText(this, "Error clearing records: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadLedgerStats() {
        try {
            File file = new File("/storage/emulated/0/Documents/Accounting/transactions.json");
            if (file.exists()) {
                long bytes = file.length();
                String formattedSize;
                if (bytes < 1024) {
                    formattedSize = bytes + " B";
                } else if (bytes < 1024 * 1024) {
                    formattedSize = String.format("%.1f KB", bytes / 1024.0);
                } else {
                    formattedSize = String.format("%.2f MB", bytes / (1024.0 * 1024.0));
                }
                textFileSize.setText(formattedSize);

                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                JSONArray jsonArray = new JSONArray(sb.toString().trim());
                int count = jsonArray.length();
                textTransactionsCount.setText(count + (count == 1 ? " record" : " records"));
            } else {
                textTransactionsCount.setText("0 records");
                textFileSize.setText("0 KB");
            }
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
        boolean enabled = prefs != null && prefs.getBoolean(KEY_HAPTICS, true);
        if (!enabled) return;

        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(25);
            }
        }
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
