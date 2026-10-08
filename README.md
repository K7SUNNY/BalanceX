# BalanceX

**BalanceX** is a modern, privacy-first personal accounting and financial wellness application for Android. Built with a 100% offline-first philosophy, BalanceX puts users in complete control of their money with bank-grade local encryption, interactive financial charts, automated subscription tracking, smart category budgeting, and gamified health scoring—without any external servers or cloud dependencies.

---

## Core Philosophy: "Your Money, Your Control"

Most financial trackers demand cloud synchronizations, server-side data aggregation, or account linking. BalanceX takes the opposite approach:

- **100% Offline & Sovereign**: All data resides strictly on the local device hardware.
- **Bank-Grade Local Encryption**: Powered by **SQLCipher** for Android with full database encryption (`AES-256`).
- **Zero Third-Party Tracking**: No telemetry, no cloud analytics, and no external data transmission.
- **Portability**: Complete JSON backup and restore capabilities for easy data migration and user custody.

---

## Key Features

### 1. Financial Dashboard & Analytics

- **Live Balances**: Real-time overview of Net Balance, Total Income (Credits), and Total Expenses (Debits).
- **Flexible Balance Timeframes**: Quick toggle between Monthly, Financial Year, and All-Time balance calculations.
- **Hybrid Jetpack Compose Spending Chart**:
  - Native Compose Canvas visualization with smooth quadratic Bézier curves and fill gradients.
  - Interactive touch scrubbing with haptic feedback to inspect individual data points.
  - Quick toggle between **Line Chart** (default) and **Bar Chart**.
  - Timeline filtering across **Days (D)**, **Weeks (W)**, **Months (M - default)**, and **Years (Y)**.
- **Category Breakdown Donut Chart**: Dynamic Jetpack Compose Donut chart visualizing expenditure distribution across top categories.

### 2. Subscriptions & Recurring Commitments Tracker

- **Automated WorkManager Service**: Daily background worker (`SubscriptionWorker`) manages recurring services offline.
- **Predictive Due Alerts**: Local notifications fired according to customizable advance reminder windows (Same day, 1 day, 2 days, 3 days before due date).
- **Smart Auto-Billing**: Automatically records recurring debit transactions on the due date with built-in deduplication.
- **Natural Relative Due Dates**: Displays intuitive, contextual deadlines (`Due today`, `Due tomorrow`, `Due in 3 days`, `Overdue by 2d`, `Due 15 Oct`).
- **Complete Management**: Pause/resume recurring subscriptions, adjust billing cycles (Monthly, Yearly, Weekly), and edit details anytime.

### 3. Category Budgets & Savings Goals

- **Monthly Spending Limits**: Set strict budget caps on individual spending categories (e.g., Food & Dining, Shopping, Fuel, Groceries).
- **Live Transaction Spend Synchronization**: Spends are dynamically computed from monthly ledger debits and fed in real time.
- **Visual Budget Adherence**: Linear progress indicators with dynamic threshold color coding (Green `<80%`, Amber `80-99%`, Red `≥100%` / Over Budget).
- **Savings Target Goals**: Set long-term financial targets with initial deposits, percentage achieved tracking, and quick incremental deposit dialogs.
- **Full Edit Affordance**: Tap any budget or goal item to adjust limits, titles, or deposits.

### 4. Financial Health Score Engine

- **Algorithmic Wellness Score (0–100)**: Evaluates monthly financial performance across three core pillars:
  - **Savings Rate Pillar (40 pts)**: Proportion of net income retained as savings.
  - **Budget Adherence Pillar (35 pts)**: Percentage of active category budgets maintained within target caps.
  - **Commitment Ratio Pillar (25 pts)**: Percentage of cashflow absorbed by recurring subscriptions (`<15%`, `15-30%`, `>30%`).
- **First-Run Guard**: Displays a clean "Not enough data" neutral state until genuine transaction data is logged, avoiding false scores.
- **Detailed Breakdown Bottom Sheet**: Transparent inspection of points earned per pillar with actionable tips.

### 5. Vault Security & App Lock

- **Biometric Authentication**: Integrated with Android `BiometricPrompt` supporting Fingerprint, Face Unlock, and Device PIN/Pattern fallback.
- **Auto-Lock on Background**: Automatically secures the app whenever backgrounded or minimized.
- **Anti-Tamper Drawer Lock**: Completely closes and locks the navigation drawer while locked to prevent ledger exfiltration or unauthorized backup exports.
- **Permanent Lockout Recovery**: Proactive hardware capability checks (`canAuthenticate()`) with secure recovery options.

### 6. Reports & PDF Statements

- **Financial Flow Analytics**: Income vs. Expense cashflow ratios, category breakdowns, top payees, and payment method statistics.
- **Professional PDF Export**: Generate formatted, auditable PDF account statements via the embedded **iText7** engine.
- **Granular Filters**: Filter transactions by date range, type (Credit / Debit), category, payment mode, or text search.

### 7. Modern UI/UX & Design System

- **Material 3 Theming**: Full Light and Dark mode compliance with automated Day/Night switching and tailored contrast palettes.
- **Edge-to-Edge Experience**: Dynamic status bar and navigation bar insets across Android 7.0 through Android 15 (API 35).
- **Haptic Micro-Interactions**: Subtle vibrational tactile feedback on key actions and navigation items.

---

## Technology Stack & Architecture

| Component                 | Technology                       | Role                                                                        |
| ------------------------- | -------------------------------- | --------------------------------------------------------------------------- |
| **Platform**              | Android (API 24 to API 35)       | Native mobile application                                                   |
| **Languages**             | Java 17 & Kotlin 2.0             | Hybrid Java XML foundations + Kotlin Compose components                     |
| **Encrypted Database**    | Room 2.6.1 + SQLCipher 4.6.1     | AES-256 encrypted relational persistence (`balancex_encrypted_database.db`) |
| **Migrations**            | Room `Migration` (MIGRATION_1_2) | Non-destructive schema evolution preserving Phase 3 user records            |
| **Background Processing** | AndroidX WorkManager 2.9.1       | Offline recurring task scheduler for subscription reminders                 |
| **Modern Visualization**  | Jetpack Compose BOM 2024.10.01   | Compose Canvas line graphs, bar charts, and donut distributions             |
| **Bridge Layer**          | ComposeView Interoperability     | `ModernChartBridge` and `ModernPieChartBridge` embedded in XML layouts      |
| **Biometrics**            | AndroidX Biometric 1.2.0         | Hardware biometric prompt with device credential fallback                   |
| **PDF Generation**        | iText7 Core 9.6.0                | Offline accounting statement rendering                                      |
| **Package Namespace**     | `com.k7sunny.balancex`           | Standardized application namespace                                          |

---

## Installation & Building

### Prerequisites

- Android Studio Ladybug (2024.2.1) or newer
- JDK 17
- Android SDK Platform 35

### Steps

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/K7SUNNY/BalanceX.git
   cd BalanceX
   ```
2. **Build the Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```
3. **Run Unit Tests**:
   ```bash
   ./gradlew testDebugUnitTest
   ```
4. **Deploy to Device / Emulator**:
   Run via Android Studio or execute:
   ```bash
   ./gradlew installDebug
   ```

---

## License & Sovereignty

Developed for personal financial autonomy and uncompromising privacy. All transaction data, credentials, and settings remain strictly on your device.
