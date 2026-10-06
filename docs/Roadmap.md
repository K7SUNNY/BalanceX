# BalanceX: Implementation Roadmap (Java Edition)

This roadmap outlines the step-by-step process to modernize BalanceX while preserving the core Java and XML foundation.

## Phase 1: Architecture & Data Layer Preparation
*Goal: Migrate from the fragile JSON storage to a robust, encrypted relational database.*
- [x] **Step 1:** Add **Room Database** and **SQLCipher** dependencies to Gradle.
- [x] **Step 2:** Design Database Entities (Transactions, Categories, Goals) using Java annotations (`@Entity`, `@PrimaryKey`).
- [x] **Step 3:** Implement Data Access Objects (DAOs) using interfaces in Java.
- [x] **Step 4:** Build a Java-based Repository pattern to manage data access.
- [x] **Step 5:** Write a background migration script to parse the existing user JSON files and insert them into the new Room Database seamlessly.

## Phase 2: UI/UX Modernization (Material 3 & Hybrid Compose)
*Goal: Revamp the look and feel using Material 3 and selective Compose integration.*
- [x] **Step 1:** Upgrade project theme to **Material 3** (`Theme.Material3.DayNight`).
- [x] **Step 2:** Update core XML layouts (Buttons, TextFields, Cards) to their Material 3 equivalents.
- [x] **Step 3:** Set up Jetpack Compose tooling for Java/XML interoperability.
- [x] **Step 4:** Replace the legacy MPAndroidChart with a modern Compose-based chart inside a `ComposeView` within the Java Activities/Fragments.

## Phase 3: Core Feature Expansion & Gamification
*Goal: Add smart budgeting and goals.*
- [ ] **Step 1:** Add a "Subscriptions / Recurring" tracker using Java background workers (`WorkManager`).
- [ ] **Step 2:** Build a "Goals" UI in XML to set budgets for specific categories.
- [ ] **Step 3:** Implement the "Financial Health" score logic based on user's adherence to budgets.
- [ ] **Step 4:** Implement App Lock using Android's `BiometricPrompt` API.

## Phase 4: On-Device Intelligence (The Local LLM)
*Goal: Implement offline AI to automate data entry.*
- [ ] **Step 1:** Research and integrate an inference engine (e.g., `llama.cpp` for Android via JNI or MediaPipe).
- [ ] **Step 2:** Download and bundle (or fetch post-install) a small quantized GGUF model (e.g., Qwen-0.5B / Qwen-1.5B).
- [ ] **Step 3:** Build the "Smart Input" prompt engine. When a user types text, feed it to the model with a system prompt to strictly output JSON.
- [ ] **Step 4:** (Optional Bonus) Integrate ML Kit Text Recognition to scan physical receipts, passing the OCR text into the local LLM to extract the amount and vendor.

## Phase 5: Polish & Deployment
*Goal: Ensure stability and performance.*
- [ ] **Step 1:** Thoroughly test the JSON-to-Room data migration on first launch.
- [ ] **Step 2:** Profile memory usage, especially around the LLM inference.
- [ ] **Step 3:** Refine PDF Statement generation using data straight from Room.
- [ ] **Step 4:** Beta release and feedback gathering.

### The End