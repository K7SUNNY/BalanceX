# BalanceX - Color Scheme & Theme Guide

This document provides a complete reference for the current Day (Light) and Night (Dark) color palettes, Material 3 theme mappings, and UI component styling in BalanceX.

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

## 2. Master Color Palette Comparison

| Token / Resource Name | Day (Light Mode) | Night (Dark Mode) | Purpose / Visual Description |
| :--- | :--- | :--- | :--- |
| **`colorPrimary`** | `#0D47A1` | `#90CAF9` | Primary brand blue; top bars, key callouts |
| **`colorPrimaryDark`** | `#002171` | `#002171` | Deep midnight navy (legacy status bar) |
| **`colorAccent`** | `#1E90FF` | `#64B5F6` | Interactive links, focus rings, highlights |
| **`colorSecondary`** | `#03DAC5` | `#03DAC6` | Teal accent color |
| **`colorSecondaryVariant`** | `#018786` | `#018786` | Darker secondary teal variant |
| **`app_background`** | `#F8FCFF` | `#121212` | Main screen and window background |
| **`card_bg`** | `#FFFFFF` | `#1E1E1E` | Card surfaces, dialog backgrounds, sheets |
| **`text_primary`** | `#000000` | `#E1E1E1` | Main headings, title text, prominent figures |
| **`text_secondary`** | `#757575` | `#B0B0B0` | Subtitles, body labels, secondary descriptions |
| **`text_on_primary`** | `#FFFFFF` | `#000000` | Text/icons displayed over `colorPrimary` |
| **`text_on_surface`** | `#000000` | `#E1E1E1` | Text displayed over card/dialog surfaces |
| **`text_hint`** | `#A6A6A6` | `#757575` | Placeholder and inactive input text |
| **`divider`** | `#BDBDBD` | `#323232` | Line dividers, outline borders (`colorOutline`) |

---

## 3. Financial & Transaction Colors

| Category | Token | Day Mode | Night Mode | Usage Context |
| :--- | :--- | :--- | :--- | :--- |
| **Income / Credit** | `chart_credit` | `#1E90FF` | `#64B5F6` | Chart credit bars & graph fill |
| | `green` | `#2E7D32` | `#81C784` | Income amount chips & badges |
| **Expense / Debit** | `chart_debit` | `#3700B3` | `#BB86FC` | Chart debit bars & graph fill |
| | `red` | `#C62828` | `#E57373` | Expense amount chips & badges |
| **Chart Elements** | `chart_text` | `#000000` | `#E1E1E1` | Axis labels, graph values |
| | `chart_grid` | `#E0E0E0` | `#323232` | Gridlines and axis ticks |

---

## 4. UI Components, Controls & Gradients

| Component Area | Token Name | Day Mode | Night Mode | Usage |
| :--- | :--- | :--- | :--- | :--- |
| **Header Gradient** | `header_gradient_start`<br>`header_gradient_end` | `#00B7EB`<br>`#0D47A1` | `#1F1F1F`<br>`#121212` | Gradient background used on top dashboard headers |
| **Navbar Gradient** | `navbar_gradient_start`<br>`navbar_gradient_end` | `#2196F3`<br>`#00B7EB` | `#1F1F1F`<br>`#121212` | Bottom / Top navigation bar backgrounds |
| **Navbar Solid** | `navbar_bg` | `#0D47A1` | `#1F1F1F` | Fallback solid navbar color |
| **Input Fields** | `input_bg` | `#F0EFEE` | `#2C2C2C` | Background for text inputs and search bars |
| **Selection Tabs** | `selection_tab_selected_bg`<br>`selection_tab_selected_text`<br>`selection_tab_unselected_text` | `#FFFFFF`<br>`#1E90FF`<br>`#000000` | `#3D3D3D`<br>`#64B5F6`<br>`#E1E1E1` | Segmented controls and filter buttons |
| **Filters & Timeline** | `filter_stroke`<br>`timeline_bg` | `#A9A9A9`<br>`#E9E9E9` | `#444444`<br>`#2C2C2C` | Chip border strokes and timeline dividers |
| **Navigation Links** | `see_all_link` | `#1E90FF` | `#64B5F6` | "See All", "View More" interactive text links |

---

## 5. Material 3 Theme Attributes Mapping

Defined in [`themes.xml`](file:///c:/Users/sunny/StudioProjects/BalanceX/app/src/main/res/values/themes.xml) and [`values-night/themes.xml`](file:///c:/Users/sunny/StudioProjects/BalanceX/app/src/main/res/values-night/themes.xml):

```xml
<style name="Theme.BalanceX" parent="Theme.Material3.DayNight.NoActionBar">
    <!-- Brand colors -->
    <item name="colorPrimary">@color/colorPrimary</item>
    <item name="colorOnPrimary">@color/text_on_primary</item>
    <item name="colorSecondary">@color/colorSecondary</item>
    <item name="colorOnSecondary">@color/black (Day) / @color/white (Night)</item>

    <!-- Surfaces & Windows -->
    <item name="android:windowBackground">@color/app_background</item>
    <item name="colorSurface">@color/card_bg</item>
    <item name="colorOnSurface">@color/text_on_surface</item>
    <item name="colorOutline">@color/divider</item>

    <!-- System Bars -->
    <item name="android:statusBarColor">?attr/colorSurface</item>
    <item name="android:windowLightStatusBar">true (Day) / false (Night)</item>
</style>
```

---

## 6. Recommendations for Modernization (Phase 2)

1. **Unify Semantic Status Colors:**
   - Consolidate legacy colors (`green`, `red`, `blue`, `lime`) and chart tokens into standard semantic aliases:
     - `@color/income` (Day: `#2E7D32`, Night: `#81C784`)
     - `@color/expense` (Day: `#C62828`, Night: `#E57373`)
2. **Material 3 Container Tokens:**
   - Leverage `colorSurfaceContainer`, `colorSurfaceContainerHigh`, and `colorSurfaceContainerLow` for layered elevation instead of flat `#1E1E1E` in dark mode.
3. **High Contrast Accessibility:**
   - Ensure text contrast ratios on all card backgrounds meet WCAG AA standards (4.5:1 for body, 3:1 for large headers).
