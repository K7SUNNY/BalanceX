# BalanceX Theme Plan: Light & Dark Mode

This document outlines the color system and implementation strategy for the BalanceX Android application to support both Light and Dark themes, following the device system settings.

## 1. Color Palette

### Light Theme (Day)
| Purpose | Color | Hex Code |
|---------|-------|----------|
| Primary | Deep Blue | `#0D47A1` |
| Primary Light | Sky Blue | `#00B7EB` |
| Secondary | Teal | `#03DAC5` |
| Background | Off-White/Light Blue Tint | `#F8FCFF` |
| Surface (Cards) | White | `#FFFFFF` |
| Text (Primary) | Black | `#000000` |
| Text (Secondary) | Dark Gray | `#757575` |
| Text (On Primary) | White | `#FFFFFF` |
| Accent/Link | Bright Blue | `#1E90FF` |

### Dark Theme (Night)
| Purpose | Color | Hex Code |
|---------|-------|----------|
| Primary | Soft Blue | `#90CAF9` |
| Primary Variant | Darker Blue | `#1565C0` |
| Secondary | Soft Teal | `#03DAC6` |
| Background | Deep Gray/Black | `#121212` |
| Surface (Cards) | Dark Gray | `#1E1E1E` |
| Text (Primary) | White/High Emphasis | `#E1E1E1` |
| Text (Secondary) | Light Gray | `#B0B0B0` |
| Text (On Primary) | Dark Gray/Black | `#000000` |
| Accent/Link | Light Sky Blue | `#64B5F6` |

## 2. Implementation Strategy

### A. Semantic Color Naming
Instead of using hardcoded hex values or generic color names (like `blue`), we will use semantic names in `colors.xml` and `res/values-night/colors.xml`.

**Standardized Names:**
- `app_background`
- `card_background`
- `text_primary`
- `text_secondary`
- `text_on_primary`
- `header_gradient_start`
- `header_gradient_end`

### B. Theme Attribute Mapping
We will map these colors to Material Design theme attributes in `themes.xml`.
- `android:windowBackground` -> `@color/app_background`
- `colorSurface` -> `@color/card_background`
- `android:textColorPrimary` -> `@color/text_primary`
- `android:textColorSecondary` -> `@color/text_secondary`

### C. Refactoring Layouts
All hardcoded colors in XML layouts (e.g., `android:textColor="#fff"`) will be replaced with:
1.  Semantic color references: `@color/text_primary`
2.  Theme attributes: `?android:attr/textColorPrimary` or `?attr/colorSurface`

### D. Dynamic Drawables
Gradient backgrounds (like `home_page_gradient_background.xml`) will be updated to use theme-aware color references instead of hardcoded hex codes.

## 3. Implementation Status
- [x] Primary and Semantic colors defined in `values/colors.xml` and `values-night/colors.xml`.
- [x] `themes.xml` and `values-night/themes.xml` updated to use `DayNight` parent and semantic colors.
- [x] Global refactor of layouts to use `@color/text_primary`, `@color/text_secondary`, etc.
- [x] Custom drawables (gradients, rounded rectangles) updated to use theme-aware colors.
- [x] Programmatic chart coloring in `GraphManager.java` and `MainActivity.java` updated to use `ContextCompat.getColor()`.

## 4. Verification
The app now follows the system theme automatically. To verify:
1. Switch device to Dark Mode.
2. Observe background changes to `#121212`.
3. Observe cards changing to `#1E1E1E`.
4. Observe text changing to light gray/white.
5. Observe charts (Bar, Line, Pie) updating their colors and text for better visibility.
