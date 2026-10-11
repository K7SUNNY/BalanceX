package com.k7sunny.balancex.util;

import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.*;

public class MoneyDateUtilsTest {

    @Test
    public void testParseAmountToMinor_standardDot() {
        assertEquals(1250L, MoneyDateUtils.parseAmountToMinor("12.50"));
        assertEquals(100L, MoneyDateUtils.parseAmountToMinor("1.00"));
        assertEquals(50L, MoneyDateUtils.parseAmountToMinor("0.50"));
    }

    @Test
    public void testParseAmountToMinor_commaDecimal() {
        // e.g. from German/French locales or String.format in comma locales
        assertEquals(1250L, MoneyDateUtils.parseAmountToMinor("12,50"));
        assertEquals(1250L, MoneyDateUtils.parseAmountToMinor("12,5"));
        assertEquals(99900L, MoneyDateUtils.parseAmountToMinor("999,00"));
    }

    @Test
    public void testParseAmountToMinor_thousandsAndCurrency() {
        assertEquals(125075L, MoneyDateUtils.parseAmountToMinor("1,250.75"));
        assertEquals(125075L, MoneyDateUtils.parseAmountToMinor("1.250,75"));
        assertEquals(50000L, MoneyDateUtils.parseAmountToMinor("₹ 500"));
        assertEquals(50000L, MoneyDateUtils.parseAmountToMinor("₹500.00"));
        assertEquals(1099L, MoneyDateUtils.parseAmountToMinor("$10.99"));
        assertEquals(2500000L, MoneyDateUtils.parseAmountToMinor("25,000"));
    }

    @Test
    public void testParseAmountToMinor_blankAndInvalid() {
        assertEquals(0L, MoneyDateUtils.parseAmountToMinor(null));
        assertEquals(0L, MoneyDateUtils.parseAmountToMinor(""));
        assertEquals(0L, MoneyDateUtils.parseAmountToMinor("   "));
        assertEquals(0L, MoneyDateUtils.parseAmountToMinor("N/A"));
    }

    @Test
    public void testParseDateToEpochDay_knownFormats() {
        LocalDate expected = LocalDate.of(2026, 10, 11);
        long epochDay = expected.toEpochDay();

        assertEquals(Long.valueOf(epochDay), MoneyDateUtils.parseDateToEpochDay("2026-10-11"));
        assertEquals(Long.valueOf(epochDay), MoneyDateUtils.parseDateToEpochDay("2026-10-11 14:30:00"));
        assertEquals(Long.valueOf(epochDay), MoneyDateUtils.parseDateToEpochDay("11-10-2026"));
        assertEquals(Long.valueOf(epochDay), MoneyDateUtils.parseDateToEpochDay("11/10/2026"));
        assertEquals(Long.valueOf(epochDay), MoneyDateUtils.parseDateToEpochDay("2026/10/11"));
        assertEquals(Long.valueOf(epochDay), MoneyDateUtils.parseDateToEpochDay("20261011"));
    }

    @Test
    public void testParseDateToEpochDay_invalidDates() {
        assertNull(MoneyDateUtils.parseDateToEpochDay(null));
        assertNull(MoneyDateUtils.parseDateToEpochDay(""));
        assertNull(MoneyDateUtils.parseDateToEpochDay("N/A"));
        assertNull(MoneyDateUtils.parseDateToEpochDay("invalid-date-string"));
        assertNull(MoneyDateUtils.parseDateToEpochDay("2026-99-99"));
    }

    @Test
    public void testFormatDisplayAndStorage() {
        assertEquals("12.50", MoneyDateUtils.formatStorageAmount(1250L));
        assertEquals(12.50, MoneyDateUtils.minorToMajor(1250L), 0.001);
        assertEquals(1250L, MoneyDateUtils.majorToMinor(12.50));
    }
}
