# BalanceX: Implementation Roadmap (Hybrid Java & Compose Edition)

This roadmap tracks the architectural evolution and feature milestones of BalanceX.

---

## Phase 1: Architecture & Data Layer Modernization
*Goal: Migrate from file-based JSON storage to an encrypted, enterprise-grade relational database.*
- [x] **Step 1:** Add **Room Database** and **SQLCipher** dependencies with AES-256 local encryption.
- [x] **Step 2:** Design Database Entities (`TransactionEntity`, `SubscriptionEntity`, `BudgetGoalEntity`).
- [x] **Step 3:** Implement Data Access Objects (`TransactionDao`, `SubscriptionDao`, `BudgetGoalDao`).
- [x] **Step 4:** Build thread-safe Repository pattern using shared database write executor (`AppDatabase.databaseWriteExecutor`).
- [x] **Step 5:** Non-destructive schema migration (`MIGRATION_1_2`) and seamless one-time JSON ledger migrator.

---

## Phase 2: UI/UX Modernization (Material 3 & Hybrid Compose)
*Goal: Elevate aesthetics with Material 3 styling and interactive Jetpack Compose data visualizations.*
- [x] **Step 1:** Upgrade theme to **Material 3** (`Theme.Material3.DayNight.NoActionBar`) with automated light/dark switching.
- [x] **Step 2:** Update core XML components (TextInputs, MaterialButtons, MaterialCardViews, Chips, Segmented Buttons).
- [x] **Step 3:** Set up Jetpack Compose BOM tooling and `ComposeView` interoperability in Java Activities.
- [x] **Step 4:** Replace legacy MPAndroidChart with native Compose Canvas charts:
  - `ModernFinancialChart`: Smooth quadratic Bézier curves, gradient area fills, touch scrubbing with haptic feedback.
  - `ModernPieChart`: Donut visualization with interactive slice selection and category legends.
  - Set **Line Chart** with **Months (M)** timeline filter as default spending analysis view on Home dashboard.

---

## Phase 3: Core Feature Expansion, Gamification & Vault Security
*Goal: Smart budgeting, recurring commitments tracking, gamified wellness, and biometric protection.*
- [x] **Step 1: Subscriptions & Recurring Tracker**
  - Daily background worker (`SubscriptionWorker`) powered by AndroidX `WorkManager`.
  - Android 13+ `POST_NOTIFICATIONS` runtime permission request flow.
  - Advance reminder notifications (same-day, 1-day, 2-day, 3-day windows) with deduplication guards.
  - Automated recurring transaction logging on due dates with catch-up deduplication.
  - Contextual relative due date formatting (`Due today`, `Due tomorrow`, `Due in 3 days`, `Overdue by 2d`).
  - Full management sheet with edit, pause/resume (`isActive`), and reminder window controls.
- [x] **Step 2: Category Budgets & Savings Goals**
  - Monthly spending limit budgets for specific categories with chip & autocomplete selection.
  - Real-time spend computation dynamically filtering current-month ledger debits.
  - Visual status progress indicators with adaptive color thresholds (Green, Amber, Red/Over Budget).
  - Savings target goals with initial balance tracking, progress badges, and fast deposit dialogs.
  - Direct edit affordances (edit pencil buttons and row click) for existing budgets and goals.
- [x] **Step 3: Algorithmic Financial Health Engine**
  - 0–100 wellness score evaluating Savings Rate (40 pts), Budget Adherence (35 pts), and Recurring Commitments (25 pts).
  - First-run safety state (`NO_DATA`) to prevent premature "Excellent" ratings on empty ledgers.
  - Interactive breakdown bottom sheet dynamically populated with computed pillar points and advice.
- [x] **Step 4: Vault Security & App Lock**
  - Hardware biometric authentication via Android `BiometricPrompt` with device PIN/Pattern fallback.
  - Re-entrancy protection eliminating infinite prompt loops on cancellation.
  - Background auto-lock re-engaging when the application is minimized or resumed from background.
  - Anti-tamper drawer security: `DrawerLayout` is locked closed while the app is locked to prevent data leaks.
  - Hardware capability verification (`canAuthenticate()`) with permanent lockout recovery path.
- [x] **Step 5: Reports & Financial Analytics**
  - Cashflow flow ratios, category breakdown, top payees, and payment method statistics.
  - Horizontal progress bar clip rendering and individual drawable state isolation.

---

## Phase 4: On-Device Intelligence (Offline Local AI)
*Goal: Zero-cloud, on-device AI automation for natural language transaction logging and receipt scanning.*
- [ ] **Step 1:** Integrate lightweight on-device inference runtime (e.g., `llama.cpp` Android JNI or Google MediaPipe LLM Inference).
- [ ] **Step 2:** Bundle or download a quantized small language model (e.g., Qwen-0.5B / Qwen-1.5B GGUF).
- [ ] **Step 3:** Implement "Smart Input" natural language prompt engine (e.g., "Paid $15 for lunch at Subway" → structured JSON output).
- [ ] **Step 4:** Integrate Google ML Kit Text Recognition (OCR) for camera receipt scanning with automated parsing into Room transactions.

---

## Phase 5: Production Readiness & Release
*Goal: Final profiling, stability verification, and store distribution.*
- [x] **Step 1:** Complete 21-bug stability audit remediation (`DEBUG_REPORT.md`).
- [x] **Step 2:** Package namespace consolidation under `com.k7sunny.balancex`.
- [ ] **Step 3:** Memory and CPU profiling under high-volume transaction stress tests (10k+ records).
- [ ] **Step 4:** Release keystore configuration and signed Android App Bundle (`.aab`) generation for Google Play Store.

---
*Roadmap maintained and updated for BalanceX development.*