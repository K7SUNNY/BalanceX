# BalanceX

BalanceX is a comprehensive personal accounting application for Android designed to facilitate autonomous financial management. The application emphasizes simplicity and data privacy, enabling users to record transactions, analyze spending patterns, and manage budgets within a secure environment where data remains exclusively on the local device.

## Core Philosophy

The fundamental principle of BalanceX is "Your Money, Your Control." In contrast to financial applications that rely on cloud synchronization and potentially compromise user privacy, BalanceX utilizes a 100% local data storage architecture. This ensures that sensitive financial history is preserved securely on the user's hardware without external transmission.

## Key Features

*   **Financial Dashboard**: Provides an immediate overview of Net Balance, Total Credits, and Total Debits upon application launch.
*   **Data Visualization**:
    *   **Bar and Line Charts**: Facilitates longitudinal analysis of income and expenditure.
    *   **Pie Charts**: Offers a categorical distribution of spending for rapid assessment.
*   **Temporal Analysis**: Supports Daily, Weekly, Monthly, and Yearly views for granular tracking of financial trends.
*   **Transaction Management**: Includes robust tools for recording, searching, and filtering transaction records.
*   **Statement Generation**: Enables the export of professional PDF statements via the integrated iText7 engine.
*   **User Interface and Experience**:
    *   Material Design compliant interface for consistent user experience.
    *   Motion design elements powered by Lottie-Android.
    *   Optimized layout for a broad range of screen dimensions, including devices with 6.4-inch displays and larger.
    *   Edge-to-Edge display support using the fitsSystemWindows attribute.
*   **Profile Personalization**: Customizable user profiles including biographical information.

## Technology Stack

*   **Platform**: Android
*   **Primary Language**: Java
*   **UI Framework**: Android XML with Material Design Components
*   **Visualization Library**: [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart)
*   **Animation Engine**: [Lottie-Android](https://github.com/airbnb/lottie-android)
*   **PDF Engine**: [iText7](https://itextpdf.com/en/products/itext-7)
*   **Persistence Layer**: Local JSON-based storage for maximum data sovereignty.

## Installation and Deployment

1.  **Clone the Repository**:
    ```bash
    git clone https://github.com/[your-username]/BalanceX.git
    ```
2.  **Import Project**: Open Android Studio and import the directory as a Gradle-based project.
3.  **Execution**: Build and deploy the application to an emulator or a physical device running Android 7.0 (API 24) or higher.

---
Developed for financial clarity and data sovereignty.
