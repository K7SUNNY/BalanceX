# BalanceX - Modern Fintech Color Scheme & Design System

This document provides a complete reference for the Material 3-aligned modern fintech design system in BalanceX.

---

## 1. Theme Configuration Overview

- **Base Theme:** `Theme.Material3.DayNight.NoActionBar`
- **Configuration Files:**
  - Day Colors: [`app/src/main/res/values/colors.xml`](file:///c:/Users/sunny/StudioProjects/BalanceX/app/src/main/res/values/colors.xml)
  - Night Colors: [`app/src/main/res/values-night/colors.xml`](file:///c:/Users/sunny/StudioProjects/BalanceX/app/src/main/res/values-night/colors.xml)
  - Day Theme Mappings: [`app/src/main/res/values/themes.xml`](file:///c:/Users/sunny/StudioProjects/BalanceX/app/src/main/res/values/themes.xml)
  - Night Theme Mappings: [`app/src/main/res/values-night/themes.xml`](file:///c:/Users/sunny/StudioProjects/BalanceX/app/src/main/res/values-night/themes.xml)
  - Component Styles: [`app/src/main/res/values/styles.xml`](file:///c:/Users/sunny/StudioProjects/BalanceX/app/src/main/res/values/styles.xml)

---

## 2. Canonical Color Token Palette

| Token / Resource Name | Day Mode (Light) | Night Mode (Dark) | Role / Usage |
| :--- | :--- | :--- | :--- |
| **`color_primary`** | `#1B4B82` | `#A1C9FF` | Primary brand accent; app headers, primary actions |
| **`color_primary_dark`** | `#0B2545` | `#081A33` | Deep brand shade for status bars / high-contrast elements |
| **`color_primary_container`** | `#D5E3FF` | `#00315B` | Tonal containers for primary action cards & badges |
| **`color_on_primary`** | `#FFFFFF` | `#00315B` | High-contrast text/icon color on `color_primary` |
| **`color_on_primary_container`** | `#001C3B` | `#D5E3FF` | Text/icon color on `color_primary_container` |
| **`color_accent`** | `#2B6CB0` | `#70B2FF` | Interactive links, active filter pills, buttons |
| **`color_secondary`** | `#51606F` | `#B9C8DA` | Secondary UI controls and neutral badges |
| **`color_tertiary`** | `#B45309` | `#FBBF24` | Warm tertiary accent for profile / alert highlights |
| **`color_tertiary_container`** | `#FEF3C7` | `#3D2406` | Tonal container for tertiary action items |
| **`color_on_tertiary`** | `#FFFFFF` | `#451A03` | High-contrast text/icon color on tertiary |
| **`color_on_tertiary_container`** | `#451A03` | `#FEF3C7` | High-contrast text/icon color on tertiary container |
| **`surface_background`** | `#F7F9FC` | `#0E1217` | Window & root view background |
| **`surface_card`** | `#FFFFFF` | `#171D25` | Elevated cards, dialog boxes, bottom sheets |
| **`surface_input`** | `#EEF2F6` | `#222933` | Text input fills, search fields, unselected pills |
| **`border_subtle`** | `#DDE3EA` | `#2D3542` | Hairline card borders, list dividers, subtle strokes |
| **`text_primary`** | `#111827` | `#F1F5F9` | Primary headings, transaction titles, amounts |
| **`text_secondary`** | `#64748B` | `#94A3B8` | Body labels, dates, secondary descriptions |
| **`text_tertiary`** | `#94A3B8` | `#64748B` | Input hints, disabled placeholders, timestamps |

---

## 3. Financial Semantics & Data Visualization

| Token / Role | Day Mode | Night Mode | Usage Context |
| :--- | :--- | :--- | :--- |
| **`finance_income`** | `#137A4B` | `#5CD892` | Positive balances, credit badges, income chart bars |
| **`finance_income_container`** | `#DCFCE7` | `#0A331D` | Pill background for credit transaction tags |
| **`finance_expense`** | `#B91C1C` | `#F87171` | Negative balances, debit badges, expense chart bars |
| **`finance_expense_container`** | `#FEE2E2` | `#3C1010` | Pill background for debit transaction tags |
| **`chart_grid_line`** | `#E2E8F0` | `#252D3B` | Chart grid lines, axis guidelines |
| **`chart_axis_label`** | `#475569` | `#94A3B8` | Chart axis labels and data markers |
| **`selection_tab_selected_bg`** | `#FFFFFF` | `#2C3647` | Selected pill background in timeline selectors |
| **`selection_tab_selected_text`** | `#1B4B82` | `#F1F5F9` | Selected tab text in timeline selectors |
| **`selection_tab_unselected_text`** | `#64748B` | `#94A3B8` | Unselected tab text in timeline selectors |
| **`navbar_background`** | `#0B2545` | `#131922` | Solid continuous background for bottom nav and system bar |
| **`header_gradient_start`** | `#1B4B82` | `#1A2330` | Top toolbar header start gradient |
| **`header_gradient_end`** | `#0D2D52` | `#111720` | Top toolbar header end gradient |

---

## 4. Material 3 Theme Attributes Mapping

The base application theme maps system attributes directly to canonical tokens:

```xml
<style name="Theme.BalanceX" parent="Theme.Material3.DayNight.NoActionBar">
    <!-- Brand / Primary -->
    <item name="colorPrimary">@color/color_primary</item>
    <item name="colorOnPrimary">@color/color_on_primary</item>
    <item name="colorPrimaryContainer">@color/color_primary_container</item>
    <item name="colorOnPrimaryContainer">@color/color_on_primary_container</item>
    
    <!-- Secondary -->
    <item name="colorSecondary">@color/color_secondary</item>
    
    <!-- Surfaces & Windows -->
    <item name="android:windowBackground">@color/surface_background</item>
    <item name="colorSurface">@color/surface_card</item>
    <item name="colorOnSurface">@color/text_primary</item>
    <item name="colorOutline">@color/border_subtle</item>

    <!-- System Bars (Seamless Edge-to-Edge) -->
    <item name="android:statusBarColor">@color/surface_background</item>
    <item name="android:windowLightStatusBar">true</item> <!-- Day: true | Night: false -->
    <item name="android:navigationBarColor">@color/navbar_background</item>
    <item name="android:windowLightNavigationBar">false</item>
</style>
```

---

## 5. Token Migration Reference

| Legacy Token | New Semantic Token | Note |
| :--- | :--- | :--- |
| `@color/colorPrimary` | `@color/color_primary` | Standardized to snake_case |
| `@color/colorPrimaryDark` | `@color/color_primary_dark` | Darker contrast container |
| `@color/colorAccent` | `@color/color_accent` | Brand interactive accent |
| `@color/colorSecondary` | `@color/color_secondary` | Muted secondary controls |
| `@color/app_background` | `@color/surface_background` | Surface background semantic |
| `@color/card_bg` | `@color/surface_card` | Card & dialog elevation surface |
| `@color/input_bg` | `@color/surface_input` | Text field & pill input background |
| `@color/divider`, `@color/filter_stroke` | `@color/border_subtle` | Unified subtle borders |
| `@color/text_hint` | `@color/text_tertiary` | Tertiary text hierarchy |
| `@color/text_on_primary` | `@color/color_on_primary` | Contrast text on primary |
| `@color/green`, `@color/chart_credit` | `@color/finance_income` | Income semantic |
| `@color/red`, `@color/chart_debit` | `@color/finance_expense` | Expense semantic |
| `@color/chart_grid` | `@color/chart_grid_line` | Chart grid line |
| `@color/chart_text` | `@color/chart_axis_label` | Chart axis label |
| `@color/see_all_link`, `@color/blue` | `@color/color_accent` / `@color/color_primary` | Unified accent/primary |
