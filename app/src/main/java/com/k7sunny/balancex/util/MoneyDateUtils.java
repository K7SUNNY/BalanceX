package com.k7sunny.balancex.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Robust utility for currency (minor units) and date (epoch day) conversions.
 * Ensures storage is locale-independent (Locale.ROOT) while display respects user locale.
 */
public final class MoneyDateUtils {

    private static final DateTimeFormatter[] DATE_FORMATTERS = new DateTimeFormatter[] {
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT),
            DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ROOT),
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT),
            DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.ROOT),
            DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ROOT),
            DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ROOT),
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    };

    private MoneyDateUtils() {}

    /**
     * Parse amount string into minor units (cents / paise).
     * Accepts '.' and ',' decimal separators, thousands separators, and currency symbols.
     * E.g. "12.50" -> 1250, "12,50" -> 1250, "1,250.50" -> 125050, "₹ 500" -> 50000.
     */
    public static long parseAmountToMinor(String rawAmount) {
        if (rawAmount == null) return 0L;
        String s = rawAmount.trim();
        if (s.isEmpty()) return 0L;

        // Determine sign
        boolean isNegative = s.startsWith("-") || s.contains("-");

        // Keep only digits, dots, commas
        s = s.replaceAll("[^0-9.,]", "");
        if (s.isEmpty()) return 0L;

        int dotIdx = s.lastIndexOf('.');
        int commaIdx = s.lastIndexOf(',');

        String normalized;
        if (dotIdx != -1 && commaIdx != -1) {
            if (commaIdx > dotIdx) {
                // e.g. "1.250,50" -> dot is thousands, comma is decimal
                normalized = s.replace(".", "").replace(',', '.');
            } else {
                // e.g. "1,250.50" -> comma is thousands, dot is decimal
                normalized = s.replace(",", "");
            }
        } else if (commaIdx != -1) {
            // Only comma present
            int charsAfterComma = s.length() - 1 - commaIdx;
            if (charsAfterComma <= 2) {
                // e.g. "12,50" or "12,5" -> decimal comma
                normalized = s.substring(0, commaIdx).replace(",", "") + "." + s.substring(commaIdx + 1);
            } else {
                // e.g. "1,000" -> thousand separator
                normalized = s.replace(",", "");
            }
        } else {
            // Only dot or pure integer
            normalized = s;
        }

        try {
            BigDecimal bd = new BigDecimal(normalized);
            BigDecimal minor = bd.multiply(new BigDecimal(100)).setScale(0, RoundingMode.HALF_UP);
            long val = minor.longValueExact();
            return isNegative ? -Math.abs(val) : Math.abs(val);
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Convert minor units to major double (e.g. 1250 -> 12.50).
     */
    public static double minorToMajor(long amountMinor) {
        return amountMinor / 100.0;
    }

    /**
     * Convert major double to minor units (e.g. 12.50 -> 1250).
     */
    public static long majorToMinor(double major) {
        return Math.round(major * 100.0);
    }

    /**
     * Format minor units for display using user locale and currency symbol.
     */
    public static String formatDisplayAmount(long amountMinor, String currencySymbol) {
        double major = Math.abs(amountMinor) / 100.0;
        String sym = (currencySymbol != null) ? currencySymbol : "";
        return sym + String.format(Locale.getDefault(), "%,.2f", major);
    }

    /**
     * Format minor units for internal storage (Locale.ROOT, e.g. "12.50").
     */
    public static String formatStorageAmount(long amountMinor) {
        double major = Math.abs(amountMinor) / 100.0;
        return String.format(Locale.ROOT, "%.2f", major);
    }

    /**
     * Attempt to parse a date string into an epoch day (days since 1970-01-01).
     * Returns null if unparseable or empty.
     */
    public static Long parseDateToEpochDay(String rawDate) {
        if (rawDate == null) return null;
        String s = rawDate.trim();
        if (s.isEmpty() || s.equalsIgnoreCase("N/A") || s.equalsIgnoreCase("NA")) return null;

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate date = LocalDate.parse(s, formatter);
                return date.toEpochDay();
            } catch (Exception ignored) {}
            try {
                LocalDateTime dateTime = LocalDateTime.parse(s, formatter);
                return dateTime.toLocalDate().toEpochDay();
            } catch (Exception ignored) {}
        }

        // Additional attempt: split on ' ' or 'T' if format had extra time info
        if (s.contains(" ") || s.contains("T")) {
            String datePart = s.split("[ T]")[0].trim();
            for (DateTimeFormatter formatter : DATE_FORMATTERS) {
                try {
                    LocalDate date = LocalDate.parse(datePart, formatter);
                    return date.toEpochDay();
                } catch (Exception ignored) {}
            }
        }

        return null;
    }

    /**
     * Convert epoch day to ISO-8601 string (yyyy-MM-dd) using Locale.ROOT.
     */
    public static String epochDayToIsoDate(Long epochDay) {
        if (epochDay == null) return "N/A";
        return LocalDate.ofEpochDay(epochDay).toString();
    }

    /**
     * Convert epoch day to user display format (e.g. yyyy-MM-dd).
     */
    public static String formatDisplayDate(Long epochDay, String fallbackDate) {
        if (epochDay == null) {
            return (fallbackDate != null && !fallbackDate.trim().isEmpty()) ? fallbackDate : "Undated";
        }
        return LocalDate.ofEpochDay(epochDay).toString();
    }

    /**
     * Today's epoch day.
     */
    public static long todayEpochDay() {
        return LocalDate.now().toEpochDay();
    }
}
