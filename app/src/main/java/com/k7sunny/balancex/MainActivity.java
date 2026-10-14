package com.k7sunny.balancex;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import androidx.appcompat.widget.PopupMenu;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.Manifest;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import androidx.compose.ui.platform.ComposeView;
import com.k7sunny.balancex.data.db.AppDatabase;
import com.k7sunny.balancex.security.BackupManager;
import com.k7sunny.balancex.ui.compose.ModernChartBridge;
import com.k7sunny.balancex.ui.compose.ModernPieChartBridge;
import com.k7sunny.balancex.util.AppLogger;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import android.database.Cursor;
import android.provider.OpenableColumns;
import android.os.Looper;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends BaseSecureActivity {
    private TextView textTotalBalance, textNetCredit, textNetDebit;
    private TextView textBalancePeriod;
    private View btnBalancePeriodFilter;
    private String currentBalancePeriod = SettingsManager.PERIOD_MONTH;
    private double totalBalance = 0, totalCredit = 0, totalDebit = 0;
    private ArrayList<String> transactionDates;
    private com.k7sunny.balancex.data.repository.SubscriptionRepository subscriptionRepository;
    private com.k7sunny.balancex.data.repository.BudgetGoalRepository budgetGoalRepository;
    private com.k7sunny.balancex.data.repository.TransactionRepository transactionRepository;
    private String selectedTimeline = "M"; // Default to Months
    private String graphType = "line"; // Default to Line Chart
    private ModernChartBridge modernChartBridge;
    private ModernPieChartBridge modernPieChartBridge;
    private DrawerLayout drawerLayout;
    private ImageView menuButton, notificationButton;
    // Back Press Handling
    private boolean backPressedOnce = false;

    private String pendingBackupPassword = null;

    private final ActivityResultLauncher<String> createBackupLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"),
            uri -> {
                if (uri != null) {
                    final String pwd = pendingBackupPassword;
                    pendingBackupPassword = null;
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        try (java.io.OutputStream os = getContentResolver().openOutputStream(uri)) {
                            if (os != null) {
                                BackupManager.exportBackup(MainActivity.this, os, pwd);
                                runOnUiThread(() -> {
                                    vibrateDevice();
                                    Toast.makeText(MainActivity.this, "Backup saved to storage successfully!", Toast.LENGTH_LONG).show();
                                });
                            }
                        } catch (Exception e) {
                            AppLogger.e("MainActivity", "Error saving backup via SAF", e);
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to save backup: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    });
                } else {
                    pendingBackupPassword = null;
                }
            }
    );

    private final ActivityResultLauncher<String[]> openRestoreLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    handleRestoreUri(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingsManager.applyTheme(this);
        setContentView(R.layout.activity_main_drawer);

        drawerLayout = findViewById(R.id.drawerlayout);
        if (drawerLayout != null) {
            drawerLayout.setStatusBarBackgroundColor(ContextCompat.getColor(this, R.color.surface_background));
            ViewCompat.setOnApplyWindowInsetsListener(drawerLayout, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                View mainView = findViewById(R.id.main);
                if (mainView != null) {
                    mainView.setPadding(insets.left, insets.top, insets.right, 0);
                }
                View drawerHeader = findViewById(R.id.drawerHeaderProfile);
                if (drawerHeader != null) {
                    int baseTop = (int) (20 * getResources().getDisplayMetrics().density);
                    drawerHeader.setPadding(
                            drawerHeader.getPaddingLeft(),
                            baseTop + insets.top,
                            drawerHeader.getPaddingRight(),
                            drawerHeader.getPaddingBottom());
                }
                View drawerFooter = findViewById(R.id.drawerFooter);
                if (drawerFooter != null) {
                    int baseBottom = (int) (12 * getResources().getDisplayMetrics().density);
                    drawerFooter.setPadding(
                            drawerFooter.getPaddingLeft(),
                            drawerFooter.getPaddingTop(),
                            drawerFooter.getPaddingRight(),
                            baseBottom + insets.bottom);
                }
                View navbar = findViewById(R.id.navbar);
                if (navbar != null) {
                    int baseBottomPadding = (int) (6 * getResources().getDisplayMetrics().density);
                    navbar.setPadding(
                            navbar.getPaddingLeft(),
                            navbar.getPaddingTop(),
                            navbar.getPaddingRight(),
                            baseBottomPadding + insets.bottom);
                }
                return windowInsets;
            });
            ViewCompat.requestApplyInsets(drawerLayout);
        }

        // 1. Initialize the Repository
        com.k7sunny.balancex.data.repository.TransactionRepository repo = new com.k7sunny.balancex.data.repository.TransactionRepository(
                this);

        // 2. Trigger the JSON to Room migration with live stats update
        com.k7sunny.balancex.data.db.DatabaseMigrator.migrateJsonToRoomIfNeeded(this, repo, () -> {
            runOnUiThread(this::updateDrawerStats);
        });

        // Phase 3 Quick Actions Listeners
        View actionSub = findViewById(R.id.action_subscriptions);
        if (actionSub != null) {
            actionSub.setOnClickListener(v -> {
                vibrateDevice();
                startActivity(new Intent(this, com.k7sunny.balancex.ui.subscriptions.SubscriptionsActivity.class));
            });
        }

        View actionGoals = findViewById(R.id.action_goals);
        if (actionGoals != null) {
            actionGoals.setOnClickListener(v -> {
                vibrateDevice();
                startActivity(new Intent(this, com.k7sunny.balancex.ui.goals.GoalsBudgetsActivity.class));
            });
        }

        View actionHealth = findViewById(R.id.action_health);
        if (actionHealth != null) {
            actionHealth.setOnClickListener(v -> {
                vibrateDevice();
                showHealthScoreSheet();
            });
        }

        View actionSec = findViewById(R.id.action_security);
        if (actionSec != null) {
            actionSec.setOnClickListener(v -> {
                vibrateDevice();
                showSecurityLockSheet();
            });
        }

        // Bottom Navigation Bar (5 Items: Home, History, Center Add FAB, Export, Profile)
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Toast.makeText(this, "Already on Home Page", Toast.LENGTH_SHORT).show();
            });
        }

        View navTransactions = findViewById(R.id.navTransactions);
        if (navTransactions != null) {
            navTransactions.setOnClickListener(v -> {
                startActivity(new Intent(this, HistoryActivity.class));
                vibrateDevice();
                finish();
            });
        }

        View navAdd = findViewById(R.id.navAdd);
        if (navAdd != null) {
            navAdd.setOnClickListener(v -> {
                startActivity(new Intent(this, EntryActivity.class));
                vibrateDevice();
            });
        }

        View navExport = findViewById(R.id.navExport);
        if (navExport != null) {
            navExport.setOnClickListener(v -> {
                startActivity(new Intent(this, ReportsActivity.class));
                vibrateDevice();
            });
        }

        View navSettings = findViewById(R.id.navSettings);
//        if (navSettings == null) {
//            navSettings = findViewById(R.id.navProfile);
//        }
        if (navSettings != null) {
            navSettings.setOnClickListener(v -> {
                startActivity(new Intent(this, SettingsActivity.class));
                vibrateDevice();
            });
        }

        // Initialize Views
        textTotalBalance = findViewById(R.id.textTotalBalance);
        textNetCredit = findViewById(R.id.textNetCredit);
        textNetDebit = findViewById(R.id.textNetDebit);
        textBalancePeriod = findViewById(R.id.textBalancePeriod);
        btnBalancePeriodFilter = findViewById(R.id.btnBalancePeriodFilter);

        currentBalancePeriod = SettingsManager.getBalancePeriod(this);
        if (textBalancePeriod != null) {
            textBalancePeriod.setText(currentBalancePeriod);
        }
        if (btnBalancePeriodFilter != null) {
            btnBalancePeriodFilter.setOnClickListener(this::showBalancePeriodMenu);
        }

        // Load initial balance
        loadBalanceData();

        // Initialize Phase 3 Repositories & WorkManager
        subscriptionRepository = new com.k7sunny.balancex.data.repository.SubscriptionRepository(this);
        budgetGoalRepository = new com.k7sunny.balancex.data.repository.BudgetGoalRepository(this);
        transactionRepository = new com.k7sunny.balancex.data.repository.TransactionRepository(this);
        com.k7sunny.balancex.worker.SubscriptionWorker.scheduleDailyCheck(this);

        // Snapshot Card click listeners
        View btnManageSubs = findViewById(R.id.btnManageSubscriptions);
        if (btnManageSubs != null) {
            btnManageSubs.setOnClickListener(v -> {
                vibrateDevice();
                startActivity(new Intent(this, com.k7sunny.balancex.ui.subscriptions.SubscriptionsActivity.class));
            });
        }

        View btnViewGoals = findViewById(R.id.btnViewAllGoals);
        if (btnViewGoals != null) {
            btnViewGoals.setOnClickListener(v -> {
                vibrateDevice();
                startActivity(new Intent(this, com.k7sunny.balancex.ui.goals.GoalsBudgetsActivity.class));
            });
        }

        View cardHealth = findViewById(R.id.cardHealthScore);
        if (cardHealth != null) {
            cardHealth.setOnClickListener(v -> {
                vibrateDevice();
                showHealthScoreSheet();
            });
        }

        // Vault Security Overlay click listeners
        View btnUnlockVault = findViewById(R.id.btnUnlockVaultBiometric);
        if (btnUnlockVault != null) {
            btnUnlockVault.setOnClickListener(v -> {
                vibrateDevice();
                checkSessionLock();
            });
        }
        View overlayLock = findViewById(R.id.layoutBiometricLockOverlay);
        if (overlayLock != null) {
            overlayLock.setOnClickListener(v -> {
                vibrateDevice();
                checkSessionLock();
            });
        }

        // Initialize Modern Compose Pie/Donut Chart
        ComposeView composePieChartView = findViewById(R.id.composePieChartView);
        if (composePieChartView != null) {
            modernPieChartBridge = new ModernPieChartBridge(this, composePieChartView);
        }

        // Initialize Modern Compose Graph
        ComposeView composeChartView = findViewById(R.id.composeChartView);
        if (composeChartView != null) {
            modernChartBridge = new ModernChartBridge(this, composeChartView);
        }
        selectedTimeline = "M"; // Default to Months
        graphType = "line"; // Default to Line Chart
        if (modernChartBridge != null) {
            modernChartBridge.updateGraph(selectedTimeline, graphType);
        }

        // Timeline Selection Buttons
        TextView monthsTextView = findViewById(R.id.months);
        TextView daysTextView = findViewById(R.id.days);
        TextView weeksTextView = findViewById(R.id.weeks);
        TextView yearsTextView = findViewById(R.id.years);

        // Graph Type Buttons
        ImageView lineChartImageView = findViewById(R.id.ImageViewlineChart);
        ImageView barGraphImageView = findViewById(R.id.ImageViewbarGraph);

        // Apply Default Selection
        updateTimelineSelection(monthsTextView, daysTextView, weeksTextView, yearsTextView);
        updateGraphTypeSelection(lineChartImageView, barGraphImageView);

        // Timeline Selection
        monthsTextView.setOnClickListener(view -> {
            selectedTimeline = "M";
            updateTimelineSelection(monthsTextView, daysTextView, weeksTextView, yearsTextView);
            if (modernChartBridge != null) {
                modernChartBridge.updateGraph(selectedTimeline, graphType);
            }
        });

        daysTextView.setOnClickListener(view -> {
            selectedTimeline = "D";
            updateTimelineSelection(daysTextView, monthsTextView, weeksTextView, yearsTextView);
            if (modernChartBridge != null) {
                modernChartBridge.updateGraph(selectedTimeline, graphType);
            }
        });

        weeksTextView.setOnClickListener(view -> {
            selectedTimeline = "W";
            updateTimelineSelection(weeksTextView, monthsTextView, daysTextView, yearsTextView);
            if (modernChartBridge != null) {
                modernChartBridge.updateGraph(selectedTimeline, graphType);
            }
        });

        yearsTextView.setOnClickListener(view -> {
            selectedTimeline = "Y";
            updateTimelineSelection(yearsTextView, monthsTextView, daysTextView, weeksTextView);
            if (modernChartBridge != null) {
                modernChartBridge.updateGraph(selectedTimeline, graphType);
            }
        });

        // Graph Type Selection
        lineChartImageView.setOnClickListener(view -> {
            Log.d("ModernChart", "Line Chart button clicked!");
            graphType = "line";
            updateGraphTypeSelection(lineChartImageView, barGraphImageView);
            if (modernChartBridge != null) {
                modernChartBridge.updateGraph(selectedTimeline, graphType);
            }
        });

        barGraphImageView.setOnClickListener(view -> {
            graphType = "bar";
            updateGraphTypeSelection(barGraphImageView, lineChartImageView);
            if (modernChartBridge != null) {
                modernChartBridge.updateGraph(selectedTimeline, graphType);
            }
        });

        setupCustomDrawer();
        loadProfileData();

        notificationButton = findViewById(R.id.notificationButton);
        if (notificationButton != null) {
            notificationButton.setOnClickListener(v -> {
                startActivity(new Intent(this, NotificationActivity.class));
                vibrateDevice();
            });
        }
    }

    private void setupCustomDrawer() {
        drawerLayout = findViewById(R.id.drawerlayout);
        menuButton = findViewById(R.id.menu_button);
        if (menuButton != null) {
            menuButton.setOnClickListener(v -> {
                vibrateDevice();
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }

        // 1. Profile Header -> ProfileActivity
        View drawerProfile = findViewById(R.id.drawerHeaderProfile);
        if (drawerProfile != null) {
            drawerProfile.setOnClickListener(v -> {
                vibrateDevice();
                closeDrawerIfOpen();
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
            });
        }

        // 2. Backup Ledger -> Vault Backup Bottom Sheet
        View btnBackup = findViewById(R.id.btnDrawerBackup);
        if (btnBackup != null) {
            btnBackup.setOnClickListener(v -> {
                vibrateDevice();
                closeDrawerIfOpen();
                showBackupBottomSheet();
            });
        }

        // 3. Restore / Import Ledger -> File Picker
        View btnRestore = findViewById(R.id.btnDrawerRestore);
        if (btnRestore != null) {
            btnRestore.setOnClickListener(v -> {
                vibrateDevice();
                closeDrawerIfOpen();
                openRestoreFilePicker();
            });
        }

        // 4. Categories & Budgets -> Categories Bottom Sheet
        View btnCategories = findViewById(R.id.btnDrawerCategories);
        if (btnCategories != null) {
            btnCategories.setOnClickListener(v -> {
                vibrateDevice();
                closeDrawerIfOpen();
                showCategoriesBottomSheet();
            });
        }

        // 5. Payment Accounts -> Accounts Bottom Sheet
        View btnAccounts = findViewById(R.id.btnDrawerAccounts);
        if (btnAccounts != null) {
            btnAccounts.setOnClickListener(v -> {
                vibrateDevice();
                closeDrawerIfOpen();
                showPaymentAccountsBottomSheet();
            });
        }

        // 6. Vault Security & Privacy -> Security Dialog
        View btnSecurity = findViewById(R.id.btnDrawerSecurity);
        if (btnSecurity != null) {
            btnSecurity.setOnClickListener(v -> {
                vibrateDevice();
                closeDrawerIfOpen();
                showVaultSecurityDialog();
            });
        }

        // 7. Help & Support -> Email
        View btnHelp = findViewById(R.id.btnDrawerHelp);
        if (btnHelp != null) {
            btnHelp.setOnClickListener(v -> {
                vibrateDevice();
                closeDrawerIfOpen();
                sendEmail("Help & Support - BalanceX");
            });
        }

        // 8. Share BalanceX -> System Share Chooser
        View btnShare = findViewById(R.id.btnDrawerShare);
        if (btnShare != null) {
            btnShare.setOnClickListener(v -> {
                vibrateDevice();
                closeDrawerIfOpen();
                shareAppRecommendation();
            });
        }

        // 9. About BalanceX -> AboutActivity
        View btnAbout = findViewById(R.id.btnDrawerAbout);
        if (btnAbout != null) {
            btnAbout.setOnClickListener(v -> {
                vibrateDevice();
                closeDrawerIfOpen();
                startActivity(new Intent(MainActivity.this, AboutActivity.class));
            });
        }

        // 10. Dynamic App Version
        TextView drawerVersion = findViewById(R.id.drawerVersionName);
        if (drawerVersion != null) {
            try {
                String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                drawerVersion.setText("BalanceX v" + versionName);
            } catch (Exception e) {
                drawerVersion.setText("BalanceX v1.0");
            }
        }

        // 11. Ensure drawer container consumes all touch events so main screen buttons behind it are not clicked
        View drawerContainer = findViewById(R.id.customDrawerContainer);
        if (drawerContainer != null) {
            drawerContainer.setClickable(true);
            drawerContainer.setFocusable(true);
        }
        View drawerInner = findViewById(R.id.drawerInnerLayout);
        if (drawerInner != null) {
            drawerInner.setClickable(true);
            drawerInner.setFocusable(true);
        }
    }

    private void closeDrawerIfOpen() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    private void loadProfileData() {
        SharedPreferences sharedPreferences = ProfileHelper.getPrefs(this);

        TextView userNameText = findViewById(R.id.drawerUserName);
        TextView bioText = findViewById(R.id.drawerUserBio);
        ImageView profileImage = findViewById(R.id.drawerProfileImage);

        if (userNameText != null) {
            userNameText.setText(sharedPreferences.getString(ProfileHelper.KEY_USER_NAME, "User Name"));
        }
        if (bioText != null) {
            bioText.setText(sharedPreferences.getString(ProfileHelper.KEY_BIO, "Personal Ledger"));
        }

        if (profileImage != null) {
            ProfileHelper.loadAvatar(this, profileImage);
        }

        updateDrawerStats();
    }

    private void updateDrawerStats() {
        com.k7sunny.balancex.data.repository.TransactionRepository repo =
                new com.k7sunny.balancex.data.repository.TransactionRepository(this);
        repo.getAllTransactions(transactions -> runOnUiThread(() -> {
            int count = transactions != null ? transactions.size() : 0;
            TextView statTrans = findViewById(R.id.drawerStatTransactions);
            if (statTrans != null) {
                statTrans.setText(count + (count == 1 ? " Entry" : " Entries"));
            }
            TextView ledgerCount = findViewById(R.id.drawerLedgerRecordCount);
            if (ledgerCount != null) {
                if (count > 0) {
                    ledgerCount.setText(count + " records available to backup");
                } else {
                    ledgerCount.setText("Share encrypted JSON snapshot");
                }
            }
        }));
    }

    private interface PasswordChosenCallback {
        void onPasswordChosen(@Nullable String password);
    }

    private void promptBackupPassword(PasswordChosenCallback callback) {
        vibrateDevice();
        android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("Password (leave blank for unencrypted)");
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout containerView = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.leftMargin = pad;
        params.rightMargin = pad;
        containerView.addView(input, params);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Protect Backup? (Optional)")
                .setMessage("Enter a password to encrypt this backup with AES-256-GCM. Or leave blank to create an unencrypted backup.")
                .setView(containerView)
                .setPositiveButton("Proceed", (dialog, which) -> {
                    String pwd = input.getText().toString().trim();
                    callback.onPasswordChosen(pwd.isEmpty() ? null : pwd);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showBackupBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_backup, null);
        dialog.setContentView(view);

        View btnClose = view.findViewById(R.id.btnCloseBackup);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        TextView textRecordCount = view.findViewById(R.id.textBackupRecordCount);
        com.k7sunny.balancex.data.repository.TransactionRepository repo =
                new com.k7sunny.balancex.data.repository.TransactionRepository(this);

        repo.getAllTransactions(transactions -> {
            int count = (transactions != null) ? transactions.size() : 0;
            runOnUiThread(() -> {
                if (textRecordCount != null) {
                    textRecordCount.setText(count + (count == 1 ? " Transaction Recorded" : " Transactions Recorded"));
                }
            });

            View btnShare = view.findViewById(R.id.btnShareBackupSnapshot);
            if (btnShare != null) {
                btnShare.setOnClickListener(v -> {
                    vibrateDevice();
                    dialog.dismiss();
                    promptBackupPassword(this::shareBackupFile);
                });
            }

            View btnSave = view.findViewById(R.id.btnSaveBackupToDevice);
            if (btnSave != null) {
                btnSave.setOnClickListener(v -> {
                    vibrateDevice();
                    dialog.dismiss();
                    promptBackupPassword(this::startSaveBackupViaSaf);
                });
            }
        });

        dialog.show();
    }

    private void shareBackupFile(@Nullable String password) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                File backupDir = new File(getCacheDir(), "backups");
                if (!backupDir.exists()) {
                    backupDir.mkdirs();
                }
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date());
                File backupFile = new File(backupDir, "BalanceX_Backup_" + timeStamp + ".json");
                try (FileOutputStream fos = new FileOutputStream(backupFile)) {
                    BackupManager.exportBackup(MainActivity.this, fos, password);
                }

                Uri fileUri = FileProvider.getUriForFile(
                        MainActivity.this,
                        getPackageName() + ".provider",
                        backupFile);

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("application/json");
                shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "BalanceX Ledger Backup (" + timeStamp + ")");
                shareIntent.putExtra(Intent.EXTRA_TEXT, "Here is the offline JSON ledger backup from BalanceX.");
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                setLaunchingSystemPicker(true);
                runOnUiThread(() -> startActivity(Intent.createChooser(shareIntent, "Share Ledger Backup")));
            } catch (Exception e) {
                AppLogger.e("MainActivity", "Error sharing backup", e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to share backup: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void startSaveBackupViaSaf(@Nullable String password) {
        pendingBackupPassword = password;
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date());
        setLaunchingSystemPicker(true);
        createBackupLauncher.launch("BalanceX_Backup_" + timeStamp + ".json");
    }

    private void showCategoriesBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_categories, null);
        dialog.setContentView(view);

        View btnClose = view.findViewById(R.id.btnCloseCategories);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        View layoutEmpty = view.findViewById(R.id.layoutEmptyCategories);
        View scrollCategories = view.findViewById(R.id.scrollCategories);
        android.widget.LinearLayout container = view.findViewById(R.id.containerCategoryItems);
        TextView subtitle = view.findViewById(R.id.textCategoriesTotalSubtitle);

        com.k7sunny.balancex.data.repository.TransactionRepository repo =
                new com.k7sunny.balancex.data.repository.TransactionRepository(this);

        repo.getAllTransactions(transactions -> runOnUiThread(() -> {
            if (transactions == null || transactions.isEmpty()) {
                if (layoutEmpty != null) layoutEmpty.setVisibility(View.VISIBLE);
                if (scrollCategories != null) scrollCategories.setVisibility(View.GONE);
                if (subtitle != null) subtitle.setText("No expense records recorded");
                dialog.show();
                return;
            }

            if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
            if (scrollCategories != null) scrollCategories.setVisibility(View.VISIBLE);

            class CatStat {
                String name;
                double total = 0;
                int count = 0;
            }

            Map<String, CatStat> map = new HashMap<>();
            double grandTotalOutflow = 0;

            for (com.k7sunny.balancex.data.entity.TransactionEntity t : transactions) {
                String cat = (t.category != null && !t.category.trim().isEmpty() && !t.category.equalsIgnoreCase("NA")) 
                        ? t.category.trim() : "General";
                double amt = 0;
                try {
                    amt = Double.parseDouble(t.amount.replaceAll("[^0-9.]", ""));
                } catch (Exception ignored) {}

                CatStat stat = map.get(cat);
                if (stat == null) {
                    stat = new CatStat();
                    stat.name = cat;
                    map.put(cat, stat);
                }
                stat.count++;
                stat.total += amt;
                grandTotalOutflow += amt;
            }

            List<CatStat> list = new ArrayList<>(map.values());
            Collections.sort(list, (a, b) -> Double.compare(b.total, a.total));

            String symbol = SettingsManager.getCurrencySymbol(MainActivity.this);
            if (subtitle != null) {
                subtitle.setText("Total Outflow: " + symbol + String.format(Locale.getDefault(), "%,.2f", grandTotalOutflow));
            }

            if (container != null) {
                container.removeAllViews();
                for (CatStat stat : list) {
                    View item = getLayoutInflater().inflate(R.layout.item_category_breakdown, container, false);
                    TextView textName = item.findViewById(R.id.textCatName);
                    TextView textCount = item.findViewById(R.id.textCatCount);
                    TextView textAmount = item.findViewById(R.id.textCatAmount);
                    LinearProgressIndicator progress = item.findViewById(R.id.progressCat);
                    ImageView imgIcon = item.findViewById(R.id.imgCatIcon);

                    int pct = grandTotalOutflow > 0 ? (int) Math.round((stat.total / grandTotalOutflow) * 100) : 0;
                    if (pct < 1 && stat.total > 0) pct = 1;

                    if (textName != null) textName.setText(stat.name);
                    if (textCount != null) {
                        textCount.setText(stat.count + (stat.count == 1 ? " entry" : " entries") + " • " + pct + "%");
                    }
                    if (textAmount != null) {
                        textAmount.setText(symbol + String.format(Locale.getDefault(), "%,.2f", stat.total));
                    }
                    if (progress != null) {
                        progress.setProgress(pct);
                    }
                    if (imgIcon != null) {
                        String lower = stat.name.toLowerCase();
                        if (lower.contains("food") || lower.contains("dine") || lower.contains("meal") || lower.contains("snack")) {
                            imgIcon.setImageResource(R.drawable.ic_cash);
                        } else if (lower.contains("bill") || lower.contains("recharge") || lower.contains("rent") || lower.contains("electric")) {
                            imgIcon.setImageResource(R.drawable.ic_payment);
                        } else if (lower.contains("shop") || lower.contains("market") || lower.contains("cloth") || lower.contains("buy")) {
                            imgIcon.setImageResource(R.drawable.ic_wallet);
                        } else if (lower.contains("salary") || lower.contains("income") || lower.contains("profit")) {
                            imgIcon.setImageResource(R.drawable.ic_arrow_down_left);
                        } else if (lower.contains("transfer") || lower.contains("send") || lower.contains("upi")) {
                            imgIcon.setImageResource(R.drawable.ic_arrow_up_right);
                        } else {
                            imgIcon.setImageResource(R.drawable.ic_filter);
                        }
                    }

                    container.addView(item);
                }
            }

            dialog.show();
        }));
    }

    private void shareAppRecommendation() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "BalanceX - Private Offline Finance");
        shareIntent.putExtra(Intent.EXTRA_TEXT, 
                "Manage your personal finances with BalanceX — 100% offline, private, and secure expense manager.\nTrack expenses, generate PDF statements, and keep complete control of your financial data.");
        startActivity(Intent.createChooser(shareIntent, "Share BalanceX"));
    }

    private void openRestoreFilePicker() {
        setLaunchingSystemPicker(true);
        try {
            openRestoreLauncher.launch(new String[]{"application/json", "application/octet-stream", "*/*"});
        } catch (Exception e) {
            Toast.makeText(this, "No document picker found to select backup file.", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleRestoreUri(Uri uri) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                if (is == null) {
                    runOnUiThread(() -> Toast.makeText(this, "Could not open selected file.", Toast.LENGTH_SHORT).show());
                    return;
                }

                // Extract file name
                String fileName = "backup.json";
                try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (nameIndex >= 0) {
                            String name = cursor.getString(nameIndex);
                            if (name != null && !name.trim().isEmpty()) {
                                fileName = name;
                            }
                        }
                    }
                } catch (Exception ignored) {}

                BackupManager.BackupContainer container = BackupManager.parseContainer(is);
                final String finalFileName = fileName;

                if (container.isEncrypted) {
                    runOnUiThread(() -> promptRestorePassword(container, finalFileName));
                } else {
                    BackupManager.BackupPayload payload = BackupManager.extractPayload(container, null);
                    runOnUiThread(() -> showRestoreBottomSheet(container, payload, null, finalFileName));
                }
            } catch (Exception e) {
                AppLogger.e("MainActivity", "Failed to parse backup file", e);
                runOnUiThread(() -> Toast.makeText(this, "Failed to parse backup: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void promptRestorePassword(BackupManager.BackupContainer container, String fileName) {
        vibrateDevice();
        android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("Enter backup password");
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout containerView = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.leftMargin = pad;
        params.rightMargin = pad;
        containerView.addView(input, params);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Encrypted Backup")
                .setMessage("This backup file is encrypted with AES-256-GCM. Please enter your password to unlock it:")
                .setView(containerView)
                .setPositiveButton("Unlock", (dialog, which) -> {
                    String password = input.getText().toString();
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        try {
                            BackupManager.BackupPayload payload = BackupManager.extractPayload(container, password);
                            runOnUiThread(() -> showRestoreBottomSheet(container, payload, password, fileName));
                        } catch (BackupManager.InvalidPasswordException ipe) {
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, "Incorrect password. Please try again.", Toast.LENGTH_SHORT).show();
                                promptRestorePassword(container, fileName);
                            });
                        } catch (Exception e) {
                            AppLogger.e("MainActivity", "Failed to decrypt backup", e);
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Decryption failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRestoreBottomSheet(BackupManager.BackupContainer container,
                                         BackupManager.BackupPayload payload,
                                         @Nullable String password,
                                         String fileName) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_restore, null);
        dialog.setContentView(view);

        View btnClose = view.findViewById(R.id.btnCloseRestore);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        TextView textCount = view.findViewById(R.id.textRestoreRecordCount);
        TextView textFile = view.findViewById(R.id.textRestoreFileName);
        TextView textInflow = view.findViewById(R.id.textRestoreInflow);
        TextView textOutflow = view.findViewById(R.id.textRestoreOutflow);

        String symbol = SettingsManager.getCurrencySymbol(this);
        int txCount = payload.transactions != null ? payload.transactions.size() : 0;
        int subCount = payload.subscriptions != null ? payload.subscriptions.size() : 0;
        int bgCount = payload.budgetGoals != null ? payload.budgetGoals.size() : 0;

        double totalInflow = 0;
        double totalOutflow = 0;
        if (payload.transactions != null) {
            for (com.k7sunny.balancex.data.entity.TransactionEntity entity : payload.transactions) {
                double parsedAmt = 0;
                try {
                    parsedAmt = Double.parseDouble(entity.amount.replaceAll("[^0-9.]", ""));
                } catch (Exception ignored) {}
                if ("credit".equalsIgnoreCase(entity.textType)) {
                    totalInflow += parsedAmt;
                } else {
                    totalOutflow += parsedAmt;
                }
            }
        }

        if (textCount != null) {
            String summaryText = txCount + (txCount == 1 ? " Entry" : " Entries");
            if (subCount > 0 || bgCount > 0) {
                summaryText += " (" + subCount + " subs, " + bgCount + " budgets)";
            }
            summaryText += " Ready to Import";
            textCount.setText(summaryText);
        }
        if (textFile != null) {
            textFile.setText(fileName);
        }
        if (textInflow != null) {
            textInflow.setText("+" + symbol + String.format(Locale.getDefault(), "%,.2f", totalInflow));
        }
        if (textOutflow != null) {
            textOutflow.setText("-" + symbol + String.format(Locale.getDefault(), "%,.2f", totalOutflow));
        }

        // Action 1: Merge
        View btnMerge = view.findViewById(R.id.btnRestoreMerge);
        if (btnMerge != null) {
            btnMerge.setOnClickListener(v -> {
                dialog.dismiss();
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    try {
                        BackupManager.RestoreResult res = BackupManager.restoreContainer(
                                MainActivity.this, container, password, BackupManager.RestoreMode.MERGE
                        );
                        runOnUiThread(() -> {
                            vibrateDevice();
                            Toast.makeText(MainActivity.this, 
                                    "Successfully merged " + res.transactionsRestored + " transactions, " 
                                    + res.subscriptionsRestored + " subscriptions, " 
                                    + res.budgetGoalsRestored + " budgets!", 
                                    Toast.LENGTH_LONG).show();
                            updateDrawerStats();
                            loadBalanceData();
                            loadSnapshotCards();
                            setupPieChart();
                        });
                    } catch (Exception e) {
                        AppLogger.e("MainActivity", "Merge restore failed", e);
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Restore failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }
                });
            });
        }

        // Action 2: Replace
        View btnReplace = view.findViewById(R.id.btnRestoreReplace);
        if (btnReplace != null) {
            btnReplace.setOnClickListener(v -> {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle("Replace Vault?")
                        .setMessage("This will replace all your current records, budgets, and subscriptions with this backup (" + txCount + " transactions). An automatic pre-restore backup will be created in app storage before replacing.")
                        .setPositiveButton("Replace", (d, w) -> {
                            dialog.dismiss();
                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                try {
                                    BackupManager.RestoreResult res = BackupManager.restoreContainer(
                                            MainActivity.this, container, password, BackupManager.RestoreMode.REPLACE
                                    );
                                    runOnUiThread(() -> {
                                        vibrateDevice();
                                        Toast.makeText(MainActivity.this, 
                                                "Vault replaced successfully! Loaded " + res.transactionsRestored + " transactions, " 
                                                + res.subscriptionsRestored + " subscriptions, " 
                                                + res.budgetGoalsRestored + " budgets.", 
                                                Toast.LENGTH_LONG).show();
                                        updateDrawerStats();
                                        loadBalanceData();
                                        loadSnapshotCards();
                                        setupPieChart();
                                    });
                                } catch (Exception e) {
                                    AppLogger.e("MainActivity", "Replace restore failed", e);
                                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Restore failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                                }
                            });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        dialog.show();
    }

    private void showPaymentAccountsBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_accounts, null);
        dialog.setContentView(view);

        View btnClose = view.findViewById(R.id.btnCloseAccounts);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        View layoutEmpty = view.findViewById(R.id.layoutEmptyAccounts);
        View scrollAccounts = view.findViewById(R.id.scrollAccounts);
        android.widget.LinearLayout container = view.findViewById(R.id.containerAccountItems);
        TextView subtitle = view.findViewById(R.id.textAccountsTotalSubtitle);

        com.k7sunny.balancex.data.repository.TransactionRepository repo =
                new com.k7sunny.balancex.data.repository.TransactionRepository(this);

        repo.getAllTransactions(transactions -> runOnUiThread(() -> {
            if (transactions == null || transactions.isEmpty()) {
                if (layoutEmpty != null) layoutEmpty.setVisibility(View.VISIBLE);
                if (scrollAccounts != null) scrollAccounts.setVisibility(View.GONE);
                if (subtitle != null) subtitle.setText("No account records recorded");
                dialog.show();
                return;
            }

            if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
            if (scrollAccounts != null) scrollAccounts.setVisibility(View.VISIBLE);

            class AccountStat {
                String name;
                double totalVolume = 0;
                double creditVolume = 0;
                double debitVolume = 0;
                int count = 0;
            }

            Map<String, AccountStat> map = new HashMap<>();
            double grandTotalVolume = 0;

            for (com.k7sunny.balancex.data.entity.TransactionEntity t : transactions) {
                String method = (t.paymentMethod != null && !t.paymentMethod.trim().isEmpty() && !t.paymentMethod.equalsIgnoreCase("NA"))
                        ? t.paymentMethod.trim() : "Cash";
                double amt = 0;
                try {
                    amt = Double.parseDouble(t.amount.replaceAll("[^0-9.]", ""));
                } catch (Exception ignored) {}

                AccountStat stat = map.get(method);
                if (stat == null) {
                    stat = new AccountStat();
                    stat.name = method;
                    map.put(method, stat);
                }
                stat.count++;
                stat.totalVolume += amt;
                grandTotalVolume += amt;
                if ("credit".equalsIgnoreCase(t.textType)) {
                    stat.creditVolume += amt;
                } else {
                    stat.debitVolume += amt;
                }
            }

            List<AccountStat> list = new ArrayList<>(map.values());
            Collections.sort(list, (a, b) -> Double.compare(b.totalVolume, a.totalVolume));

            String symbol = SettingsManager.getCurrencySymbol(MainActivity.this);
            if (subtitle != null) {
                subtitle.setText("Total Channel Volume: " + symbol + String.format(Locale.getDefault(), "%,.2f", grandTotalVolume));
            }

            if (container != null) {
                container.removeAllViews();
                for (AccountStat stat : list) {
                    View item = getLayoutInflater().inflate(R.layout.item_account_breakdown, container, false);
                    TextView textName = item.findViewById(R.id.textAccName);
                    TextView textCount = item.findViewById(R.id.textAccCount);
                    TextView textAmount = item.findViewById(R.id.textAccAmount);
                    TextView textFlow = item.findViewById(R.id.textAccFlowTag);
                    LinearProgressIndicator progress = item.findViewById(R.id.progressAcc);
                    ImageView imgIcon = item.findViewById(R.id.imgAccIcon);

                    int pct = grandTotalVolume > 0 ? (int) Math.round((stat.totalVolume / grandTotalVolume) * 100) : 0;
                    if (pct < 1 && stat.totalVolume > 0) pct = 1;

                    if (textName != null) textName.setText(stat.name);
                    if (textCount != null) {
                        textCount.setText(stat.count + (stat.count == 1 ? " transaction" : " transactions") + " • " + pct + "% volume");
                    }
                    if (textAmount != null) {
                        textAmount.setText(symbol + String.format(Locale.getDefault(), "%,.2f", stat.totalVolume));
                    }
                    if (textFlow != null) {
                        textFlow.setText("+" + symbol + String.format(Locale.getDefault(), "%,.0f", stat.creditVolume) 
                                + " / -" + symbol + String.format(Locale.getDefault(), "%,.0f", stat.debitVolume));
                    }
                    if (progress != null) {
                        progress.setProgress(pct);
                    }
                    if (imgIcon != null) {
                        String lower = stat.name.toLowerCase();
                        if (lower.contains("upi") || lower.contains("gpay") || lower.contains("phonepe") || lower.contains("paytm")) {
                            imgIcon.setImageResource(R.drawable.ic_arrow_up_right);
                        } else if (lower.contains("cash")) {
                            imgIcon.setImageResource(R.drawable.ic_cash);
                        } else if (lower.contains("bank") || lower.contains("net banking") || lower.contains("neft") || lower.contains("rtgs") || lower.contains("imps")) {
                            imgIcon.setImageResource(R.drawable.ic_wallet);
                        } else if (lower.contains("card") || lower.contains("credit") || lower.contains("debit")) {
                            imgIcon.setImageResource(R.drawable.ic_payment);
                        } else {
                            imgIcon.setImageResource(R.drawable.ic_payment);
                        }
                    }

                    container.addView(item);
                }
            }

            dialog.show();
        }));
    }

    private void showVaultSecurityDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("100% Offline Vault Guarantee")
                .setMessage("BalanceX is built from the ground up for strict personal financial privacy.\n\n"
                        + "🔒 All data is stored locally in an encrypted SQLite database.\n"
                        + "🚫 No cloud servers, no account logins, and no tracking.\n"
                        + "📦 You have 100% ownership of your ledger backups at all times.")
                .setPositiveButton("Understood", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

        if (backPressedOnce) {
            super.onBackPressed(); // Close the app
            return;
        }

        this.backPressedOnce = true;
        Toast.makeText(this, "Press back again to exit app", Toast.LENGTH_SHORT).show();

        // Reset flag after 2 seconds
        new Handler(Looper.getMainLooper()).postDelayed(() -> backPressedOnce = false, 2000);
    }

    private void sendEmail(String subject) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822"); // Ensures only email apps handle it
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] { "sunnyk7rajput@gmail.com" });
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);

        try {
            startActivity(Intent.createChooser(intent, "Choose Email Client"));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "No email apps installed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTimelineSelection(TextView selected, TextView... others) {
        selected.setBackgroundResource(R.drawable.selected_title);
        selected.setTextColor(ContextCompat.getColor(this, R.color.selection_tab_selected_text));
        selected.setTypeface(null, Typeface.BOLD);

        for (TextView other : others) {
            other.setBackgroundColor(Color.TRANSPARENT);
            other.setTextColor(ContextCompat.getColor(this, R.color.selection_tab_unselected_text));
            other.setTypeface(null, Typeface.NORMAL);
        }
    }

    private void updateGraphTypeSelection(ImageView selected, ImageView other) {
        selected.setBackgroundResource(R.drawable.selected_title);
        selected.setColorFilter(ContextCompat.getColor(this, R.color.text_primary));
        other.setBackgroundResource(android.R.color.transparent);
        other.setColorFilter(ContextCompat.getColor(this, R.color.text_tertiary));
    }

    private void showBalancePeriodMenu(View anchor) {
        vibrateDevice();
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_balance_period, popup.getMenu());

        Menu menu = popup.getMenu();
        if (SettingsManager.PERIOD_MONTH.equals(currentBalancePeriod)) {
            MenuItem item = menu.findItem(R.id.menu_period_month);
            if (item != null) item.setChecked(true);
        } else if (SettingsManager.PERIOD_FINANCIAL_YEAR.equals(currentBalancePeriod)) {
            MenuItem item = menu.findItem(R.id.menu_period_financial_year);
            if (item != null) item.setChecked(true);
        } else if (SettingsManager.PERIOD_ALL_TIME.equals(currentBalancePeriod)) {
            MenuItem item = menu.findItem(R.id.menu_period_all_time);
            if (item != null) item.setChecked(true);
        }

        popup.setOnMenuItemClickListener(item -> {
            vibrateDevice();
            int itemId = item.getItemId();
            String newPeriod = currentBalancePeriod;
            if (itemId == R.id.menu_period_month) {
                newPeriod = SettingsManager.PERIOD_MONTH;
            } else if (itemId == R.id.menu_period_financial_year) {
                newPeriod = SettingsManager.PERIOD_FINANCIAL_YEAR;
            } else if (itemId == R.id.menu_period_all_time) {
                newPeriod = SettingsManager.PERIOD_ALL_TIME;
            }

            currentBalancePeriod = newPeriod;
            SettingsManager.setBalancePeriod(MainActivity.this, currentBalancePeriod);
            if (textBalancePeriod != null) {
                textBalancePeriod.setText(currentBalancePeriod);
            }
            loadBalanceData();
            return true;
        });

        popup.show();
    }

    private boolean isTransactionInSelectedPeriod(String dateStr, String period) {
        if (SettingsManager.PERIOD_ALL_TIME.equals(period)) {
            return true;
        }

        Date date = parseTransactionDate(dateStr);
        if (date == null) {
            return false;
        }

        Calendar now = Calendar.getInstance();

        if (SettingsManager.PERIOD_MONTH.equals(period)) {
            // Month: 1st day of current month (00:00:00.000) up to today (23:59:59.999)
            Calendar startRange = Calendar.getInstance();
            startRange.set(Calendar.DAY_OF_MONTH, 1);
            startRange.set(Calendar.HOUR_OF_DAY, 0);
            startRange.set(Calendar.MINUTE, 0);
            startRange.set(Calendar.SECOND, 0);
            startRange.set(Calendar.MILLISECOND, 0);

            Calendar endRange = Calendar.getInstance();
            endRange.set(Calendar.HOUR_OF_DAY, 23);
            endRange.set(Calendar.MINUTE, 59);
            endRange.set(Calendar.SECOND, 59);
            endRange.set(Calendar.MILLISECOND, 999);

            return !date.before(startRange.getTime()) && !date.after(endRange.getTime());
        } else if (SettingsManager.PERIOD_FINANCIAL_YEAR.equals(period)) {
            // Financial Year: 1st day of current financial year (April 1st) up to today (23:59:59.999)
            int year = now.get(Calendar.YEAR);
            if (now.get(Calendar.MONTH) < Calendar.APRIL) {
                year -= 1;
            }
            Calendar startRange = Calendar.getInstance();
            startRange.set(year, Calendar.APRIL, 1, 0, 0, 0);
            startRange.set(Calendar.MILLISECOND, 0);

            Calendar endRange = Calendar.getInstance();
            endRange.set(Calendar.HOUR_OF_DAY, 23);
            endRange.set(Calendar.MINUTE, 59);
            endRange.set(Calendar.SECOND, 59);
            endRange.set(Calendar.MILLISECOND, 999);

            return !date.before(startRange.getTime()) && !date.after(endRange.getTime());
        }

        return true;
    }

    private Date parseTransactionDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty() || dateStr.equalsIgnoreCase("N/A")) {
            return null;
        }
        String[] formats = new String[]{"yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", "dd-MM-yyyy", "yyyy/MM/dd"};
        for (String fmt : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.getDefault());
                sdf.setLenient(false);
                return sdf.parse(dateStr.trim());
            } catch (Exception ignored) {}
        }
        return null;
    }

    private void updateBalanceDisplay(String symbol, double balance, double credit, double debit) {
        if (balance < 0) {
            textTotalBalance.setText("-" + symbol + String.format(Locale.getDefault(), "%,.2f", Math.abs(balance)));
        } else {
            textTotalBalance.setText(symbol + String.format(Locale.getDefault(), "%,.2f", balance));
        }
        textNetCredit.setText("+" + symbol + String.format(Locale.getDefault(), "%,.2f", credit));
        textNetDebit.setText("-" + symbol + String.format(Locale.getDefault(), "%,.2f", debit));
    }

    private void loadBalanceData() {
        loadBalanceData(this::loadHealthScoreSnapshot);
    }

    private void loadBalanceData(Runnable onComplete) {
        String symbol = SettingsManager.getCurrencySymbol(this);
        loadBalanceDataFromRoom(symbol, onComplete);
    }

    private void loadBalanceDataFromRoom(String symbol, Runnable onComplete) {
        com.k7sunny.balancex.data.repository.TransactionRepository repo =
                transactionRepository != null ? transactionRepository : new com.k7sunny.balancex.data.repository.TransactionRepository(this);
        repo.getAllTransactions(entities -> {
            if (entities == null || entities.isEmpty()) {
                runOnUiThread(() -> {
                    totalBalance = totalCredit = totalDebit = 0;
                    updateBalanceDisplay(symbol, 0, 0, 0);
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
                return;
            }

            double credit = 0;
            double debit = 0;
            double balance = 0;
            Set<String> dateSet = new HashSet<>();

            for (com.k7sunny.balancex.data.entity.TransactionEntity e : entities) {
                String date = e.date != null ? e.date : "N/A";
                dateSet.add(date);

                if (!isTransactionInSelectedPeriod(date, currentBalancePeriod)) {
                    continue;
                }

                double amount = 0;
                try {
                    amount = Double.parseDouble(e.amount.replaceAll("[^0-9.]", ""));
                } catch (Exception ignored) {}

                String type = e.textType != null ? e.textType : "debit";
                if (type.equalsIgnoreCase("credit")) {
                    credit += amount;
                    balance += amount;
                } else if (type.equalsIgnoreCase("debit")) {
                    debit += amount;
                    balance -= amount;
                }
            }

            final double finalBalance = balance;
            final double finalCredit = credit;
            final double finalDebit = debit;

            runOnUiThread(() -> {
                totalBalance = finalBalance;
                totalCredit = finalCredit;
                totalDebit = finalDebit;
                updateBalanceDisplay(symbol, totalBalance, totalCredit, totalDebit);

                transactionDates = new ArrayList<>(dateSet);
                Collections.sort(transactionDates, Collections.reverseOrder());
                if (onComplete != null) {
                    onComplete.run();
                }
            });
        });
    }

    // ==================== PHASE 3 SNAPSHOT FEED ====================

    private void loadSnapshotCards() {
        runOnUiThread(() -> {
            loadUpcomingSubscriptionsSnapshot();
            loadBudgetsGoalsSnapshot();
            loadHealthScoreSnapshot();
        });
    }

    private void loadUpcomingSubscriptionsSnapshot() {
        if (subscriptionRepository == null) return;
        subscriptionRepository.getActiveSubscriptions(subscriptions -> runOnUiThread(() -> {
            LinearLayout container = findViewById(R.id.layoutSubscriptionsContainer);
            TextView emptyText = findViewById(R.id.textEmptySubscriptions);
            if (container == null) return;

            for (int i = container.getChildCount() - 1; i >= 0; i--) {
                View child = container.getChildAt(i);
                if (child.getId() != R.id.textEmptySubscriptions) {
                    container.removeViewAt(i);
                }
            }

            if (subscriptions == null || subscriptions.isEmpty()) {
                if (emptyText != null) emptyText.setVisibility(View.VISIBLE);
                return;
            }

            if (emptyText != null) emptyText.setVisibility(View.GONE);
            String symbol = SettingsManager.getCurrencySymbol(this);
            int displayLimit = Math.min(2, subscriptions.size());

            for (int i = 0; i < displayLimit; i++) {
                com.k7sunny.balancex.data.entity.SubscriptionEntity sub = subscriptions.get(i);
                View item = getLayoutInflater().inflate(R.layout.item_home_subscription, container, false);

                TextView textName = item.findViewById(R.id.textHomeSubName);
                TextView textAmount = item.findViewById(R.id.textHomeSubAmount);
                TextView textDueBadge = item.findViewById(R.id.textHomeSubDueBadge);
                TextView textCycle = item.findViewById(R.id.textHomeSubCycle);
                View divider = item.findViewById(R.id.dividerHomeSub);

                if (textName != null) textName.setText(sub.name);
                if (textAmount != null) textAmount.setText(symbol + String.format(Locale.getDefault(), "%,.2f", sub.amount));
                if (textDueBadge != null) {
                    textDueBadge.setText(com.k7sunny.balancex.ui.subscriptions.SubscriptionsActivity.formatRelativeDueDate(sub.nextDueDate));
                }
                if (textCycle != null) textCycle.setText("• " + sub.billingCycle);

                if (divider != null && i == displayLimit - 1) {
                    divider.setVisibility(View.GONE);
                }

                item.setOnClickListener(v -> {
                    vibrateDevice();
                    startActivity(new Intent(this, com.k7sunny.balancex.ui.subscriptions.SubscriptionsActivity.class));
                });

                container.addView(item);
            }
        }));
    }

    private void loadBudgetsGoalsSnapshot() {
        if (budgetGoalRepository == null) return;
        budgetGoalRepository.getAll(items -> {
            com.k7sunny.balancex.data.repository.TransactionRepository txRepo =
                    transactionRepository != null ? transactionRepository : new com.k7sunny.balancex.data.repository.TransactionRepository(this);
            txRepo.getAllTransactions(txs -> {
                if (items != null && txs != null) {
                    java.util.Map<String, Double> monthlyExpenses =
                            com.k7sunny.balancex.finance.FinancialHealthEngine.calculateMonthlyCategoryExpenses(txs);
                    com.k7sunny.balancex.finance.FinancialHealthEngine.applyMonthlySpendToBudgets(items, monthlyExpenses);
                    budgetGoalRepository.syncBudgetSpends(monthlyExpenses, null);
                }

                runOnUiThread(() -> {
                    LinearLayout container = findViewById(R.id.layoutBudgetsGoalsContainer);
                    TextView emptyText = findViewById(R.id.textEmptyBudgets);
                    if (container == null) return;

                    for (int i = container.getChildCount() - 1; i >= 0; i--) {
                        View child = container.getChildAt(i);
                        if (child.getId() != R.id.textEmptyBudgets) {
                            container.removeViewAt(i);
                        }
                    }

                    if (items == null || items.isEmpty()) {
                        if (emptyText != null) emptyText.setVisibility(View.VISIBLE);
                        return;
                    }

                    if (emptyText != null) emptyText.setVisibility(View.GONE);
                    String symbol = SettingsManager.getCurrencySymbol(this);
                    int displayLimit = Math.min(2, items.size());

                    for (int i = 0; i < displayLimit; i++) {
                        com.k7sunny.balancex.data.entity.BudgetGoalEntity bg = items.get(i);
                        View item = getLayoutInflater().inflate(R.layout.item_home_budget_goal, container, false);

                        TextView title = item.findViewById(R.id.textHomeBudgetTitle);
                        TextView pctText = item.findViewById(R.id.textHomeBudgetProgressPct);
                        TextView spentText = item.findViewById(R.id.textHomeBudgetSpent);
                        TextView limitText = item.findViewById(R.id.textHomeBudgetTarget);
                        LinearProgressIndicator progress = item.findViewById(R.id.progressHomeBudget);
                        View divider = item.findViewById(R.id.dividerHomeBudget);

                        boolean isBudget = com.k7sunny.balancex.data.entity.BudgetGoalEntity.TYPE_BUDGET.equalsIgnoreCase(bg.type);
                        int pct = bg.targetAmount > 0 ? (int) Math.round((bg.currentAmount / bg.targetAmount) * 100) : 0;

                        if (title != null) {
                            title.setText(bg.title + (isBudget ? " (Budget)" : " (Goal)"));
                        }
                        if (pctText != null) {
                            pctText.setText(pct + "%");
                        }
                        if (spentText != null) {
                            spentText.setText((isBudget ? "Spent: " : "Saved: ") + symbol + String.format(Locale.getDefault(), "%,.2f", bg.currentAmount));
                        }
                        if (limitText != null) {
                            limitText.setText((isBudget ? "Limit: " : "Target: ") + symbol + String.format(Locale.getDefault(), "%,.2f", bg.targetAmount));
                        }
                        if (progress != null) {
                            progress.setProgress(Math.min(100, pct));
                            if (isBudget) {
                                if (pct >= 100) {
                                    progress.setIndicatorColor(ContextCompat.getColor(this, R.color.finance_expense));
                                } else if (pct >= 80) {
                                    progress.setIndicatorColor(ContextCompat.getColor(this, R.color.color_tertiary));
                                } else {
                                    progress.setIndicatorColor(ContextCompat.getColor(this, R.color.finance_income));
                                }
                            } else {
                                progress.setIndicatorColor(ContextCompat.getColor(this, R.color.color_primary));
                            }
                        }

                        if (divider != null && i == displayLimit - 1) {
                            divider.setVisibility(View.GONE);
                        }

                        item.setOnClickListener(v -> {
                            vibrateDevice();
                            startActivity(new Intent(this, com.k7sunny.balancex.ui.goals.GoalsBudgetsActivity.class));
                        });

                        container.addView(item);
                    }
                });
            });
        });
    }

    private void loadHealthScoreSnapshot() {
        if (budgetGoalRepository == null || subscriptionRepository == null) return;
        budgetGoalRepository.getBudgets(budgets -> {
            subscriptionRepository.getActiveSubscriptions(subs -> {
                com.k7sunny.balancex.data.repository.TransactionRepository txRepo =
                        new com.k7sunny.balancex.data.repository.TransactionRepository(this);
                txRepo.getAllTransactions(txs -> runOnUiThread(() -> {
                    com.k7sunny.balancex.finance.FinancialHealthEngine.HealthScoreResult result =
                            com.k7sunny.balancex.finance.FinancialHealthEngine.calculateHealthScore(
                                    totalCredit,
                                    totalDebit,
                                    budgets,
                                    subs,
                                    txs
                            );

                    TextView scoreNum = findViewById(R.id.textHealthScoreNumber);
                    TextView rating = findViewById(R.id.textHealthScoreRating);
                    TextView subDesc = findViewById(R.id.textHealthSavingsRate);
                    TextView tip = findViewById(R.id.textHealthTip);

                    if (result.hasData) {
                        if (scoreNum != null) scoreNum.setText(String.valueOf(result.score));
                        if (rating != null) rating.setText(result.ratingLabel);
                        if (subDesc != null) {
                            if (result.savingsRatePercent > 0) {
                                subDesc.setText(result.savingsRatePercent + "% monthly savings rate • Disciplined spending");
                            } else {
                                subDesc.setText("Discipline & cashflow tracking");
                            }
                        }
                        if (tip != null) tip.setText(result.adviceTip);
                    } else {
                        if (scoreNum != null) scoreNum.setText("—");
                        if (rating != null) rating.setText(result.ratingLabel);
                        if (subDesc != null) subDesc.setText("Log income & expenses to unlock score");
                        if (tip != null) tip.setText(result.adviceTip);
                    }
                }));
            });
        });
    }

    private void showHealthScoreSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_health_score, null);
        dialog.setContentView(view);

        TextView scoreVal = view.findViewById(R.id.textSheetHealthScore);
        TextView ratingVal = view.findViewById(R.id.textSheetHealthRating);
        TextView savingsPts = view.findViewById(R.id.textSheetSavingsPts);
        TextView savingsDesc = view.findViewById(R.id.textSheetSavingsDesc);
        TextView budgetPts = view.findViewById(R.id.textSheetBudgetPts);
        TextView budgetDesc = view.findViewById(R.id.textSheetBudgetDesc);
        TextView recurringPts = view.findViewById(R.id.textSheetRecurringPts);
        TextView recurringDesc = view.findViewById(R.id.textSheetRecurringDesc);
        View btnClose = view.findViewById(R.id.btnCloseHealthSheet);

        if (budgetGoalRepository != null && subscriptionRepository != null) {
            budgetGoalRepository.getBudgets(budgets -> {
                subscriptionRepository.getActiveSubscriptions(subs -> {
                    com.k7sunny.balancex.data.repository.TransactionRepository txRepo =
                            new com.k7sunny.balancex.data.repository.TransactionRepository(this);
                    txRepo.getAllTransactions(txs -> runOnUiThread(() -> {
                        com.k7sunny.balancex.finance.FinancialHealthEngine.HealthScoreResult res =
                                com.k7sunny.balancex.finance.FinancialHealthEngine.calculateHealthScore(
                                         totalCredit, totalDebit, budgets, subs, txs
                                );
                        if (res.hasData) {
                            if (scoreVal != null) scoreVal.setText(String.valueOf(res.score));
                            if (ratingVal != null) ratingVal.setText(res.ratingLabel);
                            if (savingsPts != null) savingsPts.setText(res.savingsPoints + " / 40 pts");
                            if (savingsDesc != null) savingsDesc.setText(res.savingsDesc);
                            if (budgetPts != null) budgetPts.setText(res.budgetPoints + " / 35 pts");
                            if (budgetDesc != null) budgetDesc.setText(res.budgetDesc);
                            if (recurringPts != null) recurringPts.setText(res.recurringPoints + " / 25 pts");
                            if (recurringDesc != null) recurringDesc.setText(res.recurringDesc);
                        } else {
                            if (scoreVal != null) scoreVal.setText("—");
                            if (ratingVal != null) ratingVal.setText(res.ratingLabel);
                            if (savingsPts != null) savingsPts.setText("— / 40 pts");
                            if (savingsDesc != null) savingsDesc.setText(res.savingsDesc);
                            if (budgetPts != null) budgetPts.setText("— / 35 pts");
                            if (budgetDesc != null) budgetDesc.setText(res.budgetDesc);
                            if (recurringPts != null) recurringPts.setText("— / 25 pts");
                            if (recurringDesc != null) recurringDesc.setText(res.recurringDesc);
                        }
                    }));
                });
            });
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void setSwitchSilently(com.google.android.material.materialswitch.MaterialSwitch switchView,
                                   boolean checked,
                                   boolean[] isProgrammaticFlag) {
        if (switchView == null) return;
        isProgrammaticFlag[0] = true;
        try {
            switchView.setChecked(checked);
        } finally {
            isProgrammaticFlag[0] = false;
        }
    }

    private void showSecurityLockSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_security_lock, null);
        dialog.setContentView(view);

        com.google.android.material.materialswitch.MaterialSwitch switchLock =
                view.findViewById(R.id.switchBiometricLock);
        View btnClose = view.findViewById(R.id.btnCloseSecuritySheet);

        if (switchLock != null) {
            final boolean[] isProgrammaticChange = new boolean[]{false};
            setSwitchSilently(switchLock, SettingsManager.isBiometricLockEnabled(this), isProgrammaticChange);
            switchLock.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isProgrammaticChange[0]) {
                    return;
                }
                vibrateDevice();
                if (isChecked) {
                    if (com.k7sunny.balancex.security.BiometricLockHelper.canAuthenticate(this)) {
                        com.k7sunny.balancex.security.BiometricLockHelper.promptAuthentication(
                                this,
                                "Enable Biometric Lock",
                                "Verify biometric or device credentials",
                                new com.k7sunny.balancex.security.BiometricLockHelper.AuthCallback() {
                                    @Override
                                    public void onSuccess() {
                                        SettingsManager.setBiometricLockEnabled(MainActivity.this, true);
                                        com.k7sunny.balancex.security.AppLockManager.getInstance().setSessionAuthenticated(true);
                                        Toast.makeText(MainActivity.this, "App lock enabled!", Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onFailure(String errorMsg) {
                                        setSwitchSilently(switchLock, false, isProgrammaticChange);
                                        Toast.makeText(MainActivity.this, "Authentication failed: " + errorMsg, Toast.LENGTH_SHORT).show();
                                    }
                                }
                        );
                    } else {
                        setSwitchSilently(switchLock, false, isProgrammaticChange);
                        Toast.makeText(this, "Biometrics or device security not set up on device", Toast.LENGTH_LONG).show();
                    }
                } else {
                    com.k7sunny.balancex.security.BiometricLockHelper.promptAuthentication(
                            this,
                            "Disable Biometric Lock",
                            "Verify credentials to turn off App Lock",
                            new com.k7sunny.balancex.security.BiometricLockHelper.AuthCallback() {
                                @Override
                                public void onSuccess() {
                                    SettingsManager.setBiometricLockEnabled(MainActivity.this, false);
                                    com.k7sunny.balancex.security.AppLockManager.getInstance().setSessionAuthenticated(true);
                                    unlockDashboard();
                                    loadUnlockedDashboardData();
                                    Toast.makeText(MainActivity.this, "App lock disabled", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onFailure(String errorMsg) {
                                    setSwitchSilently(switchLock, true, isProgrammaticChange);
                                    Toast.makeText(MainActivity.this, "Authentication required to disable lock", Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                }
            });
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void lockDashboardForBiometrics() {
        if (drawerLayout == null) {
            drawerLayout = findViewById(R.id.drawerlayout);
        }
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START, false);
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
        applyScreenBlur(true);
        View lockOverlay = findViewById(R.id.layoutBiometricLockOverlay);
        if (lockOverlay != null) {
            lockOverlay.setVisibility(View.VISIBLE);
        }
        View balanceCard = findViewById(R.id.balanceCard);
        if (balanceCard != null) {
            balanceCard.setVisibility(View.INVISIBLE);
        }
        if (textTotalBalance != null) textTotalBalance.setText("••••••");
        if (textNetCredit != null) textNetCredit.setText("••••••");
        if (textNetDebit != null) textNetDebit.setText("••••••");
    }

    private void unlockDashboard() {
        if (drawerLayout == null) {
            drawerLayout = findViewById(R.id.drawerlayout);
        }
        if (drawerLayout != null) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
        applyScreenBlur(false);
        View lockOverlay = findViewById(R.id.layoutBiometricLockOverlay);
        if (lockOverlay != null) {
            lockOverlay.setVisibility(View.GONE);
        }
        View balanceCard = findViewById(R.id.balanceCard);
        if (balanceCard != null) {
            balanceCard.setVisibility(View.VISIBLE);
        }
    }

    private void applyScreenBlur(boolean blur) {
        View contentRoot = findViewById(R.id.main_content_root);
        if (contentRoot != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (blur) {
                contentRoot.setRenderEffect(RenderEffect.createBlurEffect(32f, 32f, Shader.TileMode.CLAMP));
            } else {
                contentRoot.setRenderEffect(null);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SettingsManager.applyTheme(this);
        if (SettingsManager.isBiometricLockEnabled(this) && com.k7sunny.balancex.security.AppLockManager.getInstance().isLockRequired(this)) {
            lockDashboardForBiometrics();
        } else {
            unlockDashboard();
            loadUnlockedDashboardData();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public static void resetSessionAuth() {
        com.k7sunny.balancex.security.AppLockManager.getInstance().resetSession();
    }

    private void loadUnlockedDashboardData() {
        currentBalancePeriod = SettingsManager.getBalancePeriod(this);
        if (textBalancePeriod != null) {
            textBalancePeriod.setText(currentBalancePeriod);
        }
        loadProfileData();
        loadUpcomingSubscriptionsSnapshot();
        loadBudgetsGoalsSnapshot();
        loadBalanceData(this::loadHealthScoreSnapshot);
        if (modernChartBridge != null) {
            modernChartBridge.refresh();
        }
        if (modernPieChartBridge != null) {
            modernPieChartBridge.refresh();
        }
        View viewNotifBadge = findViewById(R.id.viewNotificationBadge);
        new com.k7sunny.balancex.data.repository.NotificationRepository(this).getUnreadCount(count -> runOnUiThread(() -> {
            if (viewNotifBadge != null) {
                viewNotifBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
            }
        }));
        com.k7sunny.balancex.notifications.AppNotificationManager.syncSystemAndActiveAlerts(this, null);
        com.k7sunny.balancex.data.repository.TransactionRepository repo =
                transactionRepository != null ? transactionRepository : new com.k7sunny.balancex.data.repository.TransactionRepository(this);
        com.k7sunny.balancex.data.db.DatabaseMigrator.migrateJsonToRoomIfNeeded(this, repo, () -> {
            runOnUiThread(() -> {
                loadBalanceData(() -> {
                    loadSnapshotCards();
                });
                setupPieChart();
                updateDrawerStats();
                if (modernChartBridge != null) {
                    modernChartBridge.refresh();
                }
            });
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void setupPieChart() {
        if (modernPieChartBridge != null) {
            modernPieChartBridge.refresh();
        }
    }

    public void vibrateDevice() {
        SettingsManager.vibrate(this);
    }
}
