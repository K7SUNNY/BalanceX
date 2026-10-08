package com.k7sunny.balancex;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.appcompat.app.AppCompatDelegate;

import java.util.Locale;

public class SettingsManager {

    public static final String PREFS_NAME = "balancex_settings";

    public static final String KEY_THEME = "app_theme";
    public static final String KEY_HAPTICS = "pref_haptics";
    public static final String KEY_HAPTICS_LEGACY = "haptic_feedback";
    public static final String KEY_CURRENCY = "pref_currency";
    public static final String KEY_CURRENCY_SYMBOL = "currency_symbol";
    public static final String KEY_DEFAULT_PAYMENT_METHOD = "pref_default_payment_method";
    public static final String KEY_DEFAULT_TX_TYPE = "pref_default_tx_type";

    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ==================== THEME MANAGEMENT ====================

    public static String getTheme(Context context) {
        return getPrefs(context).getString(KEY_THEME, THEME_SYSTEM);
    }

    public static void setTheme(Context context, String theme) {
        getPrefs(context).edit().putString(KEY_THEME, theme).apply();
        applyTheme(context);
    }

    public static void applyTheme(Context context) {
        String theme = getTheme(context);
        if (THEME_LIGHT.equalsIgnoreCase(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (THEME_DARK.equalsIgnoreCase(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    // ==================== HAPTICS MANAGEMENT ====================

    public static boolean isHapticsEnabled(Context context) {
        SharedPreferences prefs = getPrefs(context);
        if (prefs.contains(KEY_HAPTICS)) {
            return prefs.getBoolean(KEY_HAPTICS, true);
        }
        return prefs.getBoolean(KEY_HAPTICS_LEGACY, true);
    }

    public static void setHapticsEnabled(Context context, boolean enabled) {
        getPrefs(context).edit()
                .putBoolean(KEY_HAPTICS, enabled)
                .putBoolean(KEY_HAPTICS_LEGACY, enabled)
                .apply();
    }

    public static void vibrate(Context context) {
        vibrate(context, 25);
    }

    public static void vibrate(Context context, long durationMillis) {
        if (context == null || !isHapticsEnabled(context)) return;
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(durationMillis);
                }
            }
        } catch (Exception ignored) {}
    }

    // ==================== CURRENCY MANAGEMENT ====================

    public static String getCurrencyFull(Context context) {
        return getPrefs(context).getString(KEY_CURRENCY, "₹ INR (Indian Rupee)");
    }

    public static String getCurrencySymbol(Context context) {
        SharedPreferences prefs = getPrefs(context);
        if (prefs.contains(KEY_CURRENCY_SYMBOL)) {
            String sym = prefs.getString(KEY_CURRENCY_SYMBOL, "");
            if (sym != null && !sym.trim().isEmpty()) {
                return sym.trim();
            }
        }

        String full = prefs.getString(KEY_CURRENCY, "₹ INR (Indian Rupee)");
        return extractSymbolFromFull(full);
    }

    public static void setCurrency(Context context, String fullCurrency) {
        String symbol = extractSymbolFromFull(fullCurrency);
        getPrefs(context).edit()
                .putString(KEY_CURRENCY, fullCurrency)
                .putString(KEY_CURRENCY_SYMBOL, symbol)
                .apply();
    }

    public static String extractSymbolFromFull(String full) {
        if (full == null || full.trim().isEmpty()) {
            return "₹";
        }
        String trimmed = full.trim();
        if (trimmed.startsWith("A$")) return "A$";
        if (trimmed.startsWith("C$")) return "C$";
        if (trimmed.startsWith("₹")) return "₹";
        if (trimmed.startsWith("$")) return "$";
        if (trimmed.startsWith("€")) return "€";
        if (trimmed.startsWith("£")) return "£";
        if (trimmed.startsWith("¥")) return "¥";

        int spaceIndex = trimmed.indexOf(' ');
        if (spaceIndex > 0) {
            return trimmed.substring(0, spaceIndex);
        }
        return "₹";
    }

    public static String formatAmount(Context context, double amount) {
        String symbol = getCurrencySymbol(context);
        return symbol + String.format(Locale.getDefault(), "%,.2f", amount);
    }

    public static String formatSignedAmount(Context context, double amount, boolean isCredit) {
        String symbol = getCurrencySymbol(context);
        String prefix = isCredit ? "+" : "-";
        return prefix + symbol + String.format(Locale.getDefault(), "%,.2f", amount);
    }

    // ==================== DEFAULT PAYMENT METHOD ====================

    public static String getDefaultPaymentMethod(Context context) {
        return getPrefs(context).getString(KEY_DEFAULT_PAYMENT_METHOD, "UPI");
    }

    public static void setDefaultPaymentMethod(Context context, String method) {
        getPrefs(context).edit().putString(KEY_DEFAULT_PAYMENT_METHOD, method).apply();
    }

    // ==================== DEFAULT TRANSACTION TYPE ====================

    public static String getDefaultTransactionType(Context context) {
        return getPrefs(context).getString(KEY_DEFAULT_TX_TYPE, "Debit (Expense)");
    }

    public static void setDefaultTransactionType(Context context, String type) {
        getPrefs(context).edit().putString(KEY_DEFAULT_TX_TYPE, type).apply();
    }

    // ==================== BALANCE CARD PERIOD ====================
    public static final String KEY_BALANCE_PERIOD = "balance_card_period";
    public static final String PERIOD_MONTH = "Month";
    public static final String PERIOD_FINANCIAL_YEAR = "Financial Year";
    public static final String PERIOD_ALL_TIME = "All Time";

    public static String getBalancePeriod(Context context) {
        return getPrefs(context).getString(KEY_BALANCE_PERIOD, PERIOD_MONTH);
    }

    public static void setBalancePeriod(Context context, String period) {
        getPrefs(context).edit().putString(KEY_BALANCE_PERIOD, period).apply();
    }

    // ==================== BIOMETRIC APP LOCK ====================
    public static final String KEY_BIOMETRIC_LOCK = "pref_biometric_lock";

    public static boolean isBiometricLockEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_BIOMETRIC_LOCK, false);
    }

    public static void setBiometricLockEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_BIOMETRIC_LOCK, enabled).apply();
    }
}
