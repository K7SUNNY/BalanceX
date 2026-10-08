# BalanceX: Project Vision & Evolution

## 1. The Present Reality: A Privacy-First Financial Powerhouse

BalanceX has evolved from a simple offline ledger into a **secure, proactive personal accounting and wellness companion**. By combining the rock-solid stability of Java and Material 3 XML with the high performance of Jetpack Compose and encrypted SQLite, BalanceX delivers a premium fintech experience with 100% data sovereignty.

### What is Live & Production-Ready Today:
- **Bank-Grade Local Encryption**: All user data is encrypted at rest using **SQLCipher (AES-256)** on top of Android **Room Database**, with non-destructive migrations (`MIGRATION_1_2`) and complete JSON export/import portability.
- **Hybrid Material 3 & Jetpack Compose UI**:
  - Material 3 Day/Night theme with automated dark mode switching.
  - Native Jetpack Compose Canvas visualizations (`ModernFinancialChart` and `ModernPieChart`) featuring smooth quadratic Bézier curves, area gradients, and haptic touch scrubbing.
  - Line Chart with Months filter as the default home spending analysis view.
- **Automated Recurring Subscriptions Tracker**:
  - Background processing via AndroidX **WorkManager** (`SubscriptionWorker`).
  - Predictive offline reminders with runtime notification permissions.
  - Relative due date formatting (`Due today`, `Due tomorrow`, `Due in 3 days`).
  - Automated auto-billing with deduplication.
- **Category Budgets & Savings Goals**:
  - Monthly spending caps per category with autocomplete & chip suggestions.
  - Real-time spend computation synchronized directly with ledger debits.
  - Goal tracking with progress indicators, fast deposit dialogs, and edit controls.
- **Algorithmic Financial Health Engine**:
  - Gamified 0–100 wellness score based on Savings Rate (40 pts), Budget Adherence (35 pts), and Recurring Commitments (25 pts).
  - Detailed breakdown sheet with dynamic pillar inspections and no-data guards.
- **Vault Security & App Lock**:
  - Hardware biometric authentication (**BiometricPrompt**) supporting Fingerprint, Face, and Device PIN/Pattern.
  - Automatic re-locking when app is minimized or backgrounded.
  - Drawer anti-tamper locking preventing unauthorized exports while locked.
- **Statement & Reports Engine**:
  - Auditable PDF statement generation via embedded **iText7**.
  - Comprehensive cashflow analytics, category breakdowns, and payee insights.

---

## 2. The Next Horizon: On-Device Intelligence (Phase 4)

To complete the transformation from a smart financial tracker to an **Autonomous Financial Wellness Coach**, BalanceX will integrate on-device local intelligence without ever sending a single byte to the cloud.

### Upcoming Key Implementations:

### 1. On-Device Small Language Model (SLM) Inference
- **What**: Integrate an embedded offline inference engine (such as `llama.cpp` Android JNI or Google MediaPipe LLM Inference) running quantized models (e.g., **Qwen-0.5B-GGUF** or a lightweight Gemma model).
- **Why**: Enable conversational "Smart Add". A user can type or dictate:
  > *"Had dinner at Olive Garden for $48.50 with friends"*
  and the local model parses this instantly into:
  ```json
  {
    "amount": 48.50,
    "category": "Food & Dining",
    "note": "Dinner at Olive Garden with friends",
    "type": "Debit"
  }
  ```
  completely offline in under 300ms.

### 2. On-Device Receipt OCR Scanner
- **What**: Integrate Google ML Kit on-device Text Recognition with the camera.
- **Why**: Scan paper receipts, extract vendor and line items, feed the text to the local parser, and auto-populate the transaction form with a single tap.

### 3. Predictive Cashflow Forecasting
- **What**: Statistical projection algorithms analyzing recurring subscriptions, average category burns, and historical income cycles to predict end-of-month liquidity.

---

## 3. Guiding Principles

1. **Zero Cloud Telemetry**: User financial records are personal and private. BalanceX never connects to external servers.
2. **Deterministic Security**: Encryption keys and biometric validation rely on hardware-backed Android Keystore and SQLCipher.
3. **Fluid Performance**: High frame-rate rendering, seamless edge-to-edge system insets, and zero UI blocking on background I/O threads.
