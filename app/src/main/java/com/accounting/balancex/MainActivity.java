package com.accounting.balancex;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import java.util.Calendar;
import androidx.appcompat.widget.PopupMenu;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import androidx.compose.ui.platform.ComposeView;
import com.accounting.balancex.ui.compose.ModernChartBridge;
import com.accounting.balancex.ui.compose.ModernPieChartBridge;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import android.database.Cursor;
import android.provider.OpenableColumns;
import android.os.Looper;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
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

public class MainActivity extends AppCompatActivity {
    private TextView textTotalBalance, textNetCredit, textNetDebit, seeAllButton;
    private TextView textBalancePeriod;
    private View btnBalancePeriodFilter;
    private String currentBalancePeriod = SettingsManager.PERIOD_MONTH;
    private double totalBalance = 0, totalCredit = 0, totalDebit = 0;
    private ArrayList<String> transactionDates;
    private Handler handler = new Handler();
    private Runnable scrollRunnable;
    private int scrollPosition = 0;
    private RecyclerView recyclerView;
    private RecentTransactionsAdapter adapter;
    private List<RecentTransactionModel> recentTransactions;
    private String selectedTimeline = "M"; // Default to Months
    private String graphType = "bar"; // Default to Bar Graph
    private ModernChartBridge modernChartBridge;
    private ModernPieChartBridge modernPieChartBridge;
    private DrawerLayout drawerLayout;
    private ImageView menuButton, notificationButton;
    // Back Press Handling
    private boolean backPressedOnce = false;

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
        com.accounting.balancex.data.repository.TransactionRepository repo = new com.accounting.balancex.data.repository.TransactionRepository(
                this);

        // 2. Trigger the JSON to Room migration with live stats update
        com.accounting.balancex.data.db.DatabaseMigrator.migrateJsonToRoomIfNeeded(this, repo, () -> {
            runOnUiThread(this::updateDrawerStats);
        });

        // 3. Query the encrypted database to verify data is there
        repo.getAllTransactions(transactions -> {
            android.util.Log.d("RoomTest", "Total transactions in Room: " + transactions.size());
            for (int i = 0; i < Math.min(transactions.size(), 3); i++) {
                android.util.Log.d("RoomTest", "Transaction: " + transactions.get(i).description +
                        " | Amount: " + transactions.get(i).amount);
            }
        });


        // Quick Actions Listeners
        findViewById(R.id.action_add_transaction).setOnClickListener(v -> {
            startActivity(new Intent(this, EntryActivity.class));
            vibrateDevice();
        });

        findViewById(R.id.action_history).setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
            vibrateDevice();
        });

        findViewById(R.id.action_export).setOnClickListener(v -> {
            startActivity(new Intent(this, ReportsActivity.class));
            vibrateDevice();
        });

        findViewById(R.id.action_profile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            vibrateDevice();
        });

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

        recyclerView = findViewById(R.id.recyclerViewRecentTransactions);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recentTransactions = new ArrayList<>(); // ✅ Initialize empty list before passing it to adapter
        adapter = new RecentTransactionsAdapter(this, recentTransactions);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(layoutManager);
        // Load transactions from storage
        loadTransactionsFromStorage();

        // Ensures the item in the center stays in place
        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);
        startAutoScroll(); // ✅ Start auto-scrolling after setting up RecyclerView

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
        graphType = "bar"; // Default to Bar Graph
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
        updateGraphTypeSelection(barGraphImageView, lineChartImageView);

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

        TextView seeAllButton = findViewById(R.id.seeAllButton);
        if (seeAllButton != null) {
            seeAllButton.setOnClickListener(v -> {
                startActivity(new Intent(this, HistoryActivity.class));
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
        com.accounting.balancex.data.repository.TransactionRepository repo = 
                new com.accounting.balancex.data.repository.TransactionRepository(this);
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

    private void showBackupBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_backup, null);
        dialog.setContentView(view);

        View btnClose = view.findViewById(R.id.btnCloseBackup);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        TextView textRecordCount = view.findViewById(R.id.textBackupRecordCount);
        com.accounting.balancex.data.repository.TransactionRepository repo =
                new com.accounting.balancex.data.repository.TransactionRepository(this);

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
                    if (count == 0) {
                        Toast.makeText(MainActivity.this, "No transactions to backup.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    shareBackupFile(transactions);
                });
            }

            View btnSave = view.findViewById(R.id.btnSaveBackupToDevice);
            if (btnSave != null) {
                btnSave.setOnClickListener(v -> {
                    vibrateDevice();
                    dialog.dismiss();
                    if (count == 0) {
                        Toast.makeText(MainActivity.this, "No transactions to backup.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveBackupToDevice(transactions);
                });
            }
        });

        dialog.show();
    }

    private void shareBackupFile(List<com.accounting.balancex.data.entity.TransactionEntity> transactions) {
        try {
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(transactions);
            File backupDir = new File(getCacheDir(), "backups");
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date());
            File backupFile = new File(backupDir, "BalanceX_Backup_" + timeStamp + ".json");
            try (FileOutputStream fos = new FileOutputStream(backupFile)) {
                fos.write(json.getBytes(StandardCharsets.UTF_8));
            }

            Uri fileUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    backupFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/json");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "BalanceX Ledger Backup (" + timeStamp + ")");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Here is the offline JSON ledger backup from BalanceX (" + transactions.size() + " records).");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share Ledger Backup"));
        } catch (Exception e) {
            Log.e("Backup", "Error sharing backup", e);
            Toast.makeText(this, "Failed to share backup: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveBackupToDevice(List<com.accounting.balancex.data.entity.TransactionEntity> transactions) {
        try {
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(transactions);

            File targetDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Accounting/Backups");
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
            if (!targetDir.canWrite()) {
                targetDir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Backups");
                if (!targetDir.exists()) {
                    targetDir.mkdirs();
                }
            }

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "BalanceX_Backup_" + timeStamp + ".json";
            File backupFile = new File(targetDir, fileName);

            try (FileOutputStream fos = new FileOutputStream(backupFile)) {
                fos.write(json.getBytes(StandardCharsets.UTF_8));
            }

            vibrateDevice();
            Toast.makeText(this, "Backup saved to Documents/Accounting/Backups/" + fileName, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e("Backup", "Error saving backup to device", e);
            Toast.makeText(this, "Failed to save backup: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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

        com.accounting.balancex.data.repository.TransactionRepository repo =
                new com.accounting.balancex.data.repository.TransactionRepository(this);

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

            for (com.accounting.balancex.data.entity.TransactionEntity t : transactions) {
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

    private static final int RESTORE_FILE_REQUEST = 1001;

    private void openRestoreFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {"application/json", "text/plain", "application/octet-stream"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        try {
            startActivityForResult(intent, RESTORE_FILE_REQUEST);
        } catch (Exception e) {
            Toast.makeText(this, "No document picker found to select backup file.", Toast.LENGTH_SHORT).show();
        }
    }

    private void importBackupFromUri(Uri uri) {
        try (java.io.InputStream is = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String rawJson = sb.toString().trim();
            if (rawJson.isEmpty() || (!rawJson.startsWith("[") && !rawJson.startsWith("{"))) {
                Toast.makeText(this, "The selected file is not a valid JSON ledger file.", Toast.LENGTH_SHORT).show();
                return;
            }

            JSONArray array;
            if (rawJson.startsWith("{")) {
                JSONObject root = new JSONObject(rawJson);
                if (root.has("transactions")) {
                    array = root.getJSONArray("transactions");
                } else {
                    array = new JSONArray();
                    array.put(root);
                }
            } else {
                array = new JSONArray(rawJson);
            }

            List<com.accounting.balancex.data.entity.TransactionEntity> importedEntities = new ArrayList<>();
            double totalInflow = 0;
            double totalOutflow = 0;

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                com.accounting.balancex.data.entity.TransactionEntity entity = new com.accounting.balancex.data.entity.TransactionEntity();
                entity.entryId = obj.optLong("entryId", i + 1);
                entity.date = obj.optString("date", "");
                entity.amount = obj.has("amount") ? String.valueOf(obj.get("amount")) : "0";
                entity.receiver = obj.optString("receiver", obj.optString("receiverName", "Unknown"));
                entity.description = obj.optString("description", "");
                entity.utr = obj.optString("utr", "");
                entity.transactionId = obj.optString("transactionId", obj.optString("transactionID", ""));
                entity.comments = obj.optString("comments", "");
                entity.category = obj.optString("category", "General");
                entity.paymentMethod = obj.optString("paymentMethod", "Cash");
                entity.textType = obj.optString("textType", obj.optString("transactionType", "Debit"));

                double parsedAmt = 0;
                try {
                    parsedAmt = Double.parseDouble(entity.amount.replaceAll("[^0-9.]", ""));
                } catch (Exception ignored) {}

                if ("credit".equalsIgnoreCase(entity.textType)) {
                    totalInflow += parsedAmt;
                } else {
                    totalOutflow += parsedAmt;
                }

                importedEntities.add(entity);
            }

            if (importedEntities.isEmpty()) {
                Toast.makeText(this, "The selected file did not contain any valid transactions.", Toast.LENGTH_SHORT).show();
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

            showRestoreBottomSheet(importedEntities, fileName, totalInflow, totalOutflow);

        } catch (Exception e) {
            Log.e("Restore", "Failed to parse backup", e);
            Toast.makeText(this, "Failed to parse backup file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showRestoreBottomSheet(List<com.accounting.balancex.data.entity.TransactionEntity> importedEntities,
                                         String fileName, double totalInflow, double totalOutflow) {
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
        if (textCount != null) {
            textCount.setText(importedEntities.size() + (importedEntities.size() == 1 ? " Entry Ready to Import" : " Entries Ready to Import"));
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

        com.accounting.balancex.data.repository.TransactionRepository repo =
                new com.accounting.balancex.data.repository.TransactionRepository(this);

        // Action 1: Merge
        View btnMerge = view.findViewById(R.id.btnRestoreMerge);
        if (btnMerge != null) {
            btnMerge.setOnClickListener(v -> {
                dialog.dismiss();
                repo.getAllTransactions(existingList -> {
                    Set<String> existingSigs = new HashSet<>();
                    long maxId = 0;
                    if (existingList != null) {
                        for (com.accounting.balancex.data.entity.TransactionEntity ex : existingList) {
                            existingSigs.add(com.accounting.balancex.data.db.DatabaseMigrator.makeSignature(
                                    ex.date, ex.amount, ex.receiver, ex.textType, ex.description));
                            if (ex.entryId > maxId) {
                                maxId = ex.entryId;
                            }
                        }
                    }

                    List<com.accounting.balancex.data.entity.TransactionEntity> toAdd = new ArrayList<>();
                    for (com.accounting.balancex.data.entity.TransactionEntity imp : importedEntities) {
                        String sig = com.accounting.balancex.data.db.DatabaseMigrator.makeSignature(
                                imp.date, imp.amount, imp.receiver, imp.textType, imp.description);
                        if (!existingSigs.contains(sig)) {
                            maxId++;
                            imp.entryId = maxId;
                            toAdd.add(imp);
                            existingSigs.add(sig);
                        }
                    }

                    if (toAdd.isEmpty()) {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, 
                                "All " + importedEntities.size() + " records in this snapshot are already in your vault.", 
                                Toast.LENGTH_SHORT).show());
                        return;
                    }

                    repo.insertAll(toAdd, () -> {
                        repo.getAllTransactions(fullList -> {
                            com.accounting.balancex.data.db.DatabaseMigrator.saveEntitiesToJson(MainActivity.this, fullList);
                        });
                        runOnUiThread(() -> {
                            vibrateDevice();
                            Toast.makeText(MainActivity.this, 
                                    "Successfully merged " + toAdd.size() + " new entries into your vault!", 
                                    Toast.LENGTH_LONG).show();
                            updateDrawerStats();
                            loadBalanceData();
                            loadTransactionsFromStorage();
                            setupPieChart();
                        });
                    });
                });
            });
        }

        // Action 2: Replace
        View btnReplace = view.findViewById(R.id.btnRestoreReplace);
        if (btnReplace != null) {
            btnReplace.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                        .setTitle("Replace Vault?")
                        .setMessage("This will replace all your current records with the " + importedEntities.size() + " records from " + fileName + ". This cannot be undone.")
                        .setPositiveButton("Replace", (d, w) -> {
                            dialog.dismiss();
                            repo.deleteAll(() -> {
                                repo.insertAll(importedEntities, () -> {
                                    com.accounting.balancex.data.db.DatabaseMigrator.saveEntitiesToJson(MainActivity.this, importedEntities);
                                    runOnUiThread(() -> {
                                        vibrateDevice();
                                        Toast.makeText(MainActivity.this, 
                                                "Vault replaced with " + importedEntities.size() + " transactions!", 
                                                Toast.LENGTH_LONG).show();
                                        updateDrawerStats();
                                        loadBalanceData();
                                        loadTransactionsFromStorage();
                                        setupPieChart();
                                    });
                                });
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

        com.accounting.balancex.data.repository.TransactionRepository repo =
                new com.accounting.balancex.data.repository.TransactionRepository(this);

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

            for (com.accounting.balancex.data.entity.TransactionEntity t : transactions) {
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESTORE_FILE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            importBackupFromUri(data.getData());
        }
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
        String symbol = SettingsManager.getCurrencySymbol(this);
        try {
            File file = com.accounting.balancex.data.db.DatabaseMigrator.findJsonFile(this);
            if (file == null) {
                file = new File("/storage/emulated/0/Documents/Accounting/transactions.json");
            }
            if (!file.exists() || file.length() == 0) {
                loadBalanceDataFromRoom(symbol);
                return;
            }

            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            fis.close();

            String jsonStr = sb.toString().trim();
            if (jsonStr.isEmpty() || jsonStr.equals("[]")) {
                loadBalanceDataFromRoom(symbol);
                return;
            }

            JSONArray transactionsArray = new JSONArray(jsonStr);
            totalBalance = totalCredit = totalDebit = 0;
            Set<String> dateSet = new HashSet<>();

            for (int i = 0; i < transactionsArray.length(); i++) {
                JSONObject transaction = transactionsArray.getJSONObject(i);
                String date = transaction.optString("date", "N/A");
                dateSet.add(date);

                if (!isTransactionInSelectedPeriod(date, currentBalancePeriod)) {
                    continue;
                }

                double amount = 0;
                try {
                    amount = transaction.getDouble("amount");
                } catch (Exception e) {
                    try {
                        amount = Double.parseDouble(transaction.getString("amount").replaceAll("[^0-9.]", ""));
                    } catch (Exception ignored) {}
                }
                String type = transaction.optString("textType", "debit");

                if (type.equalsIgnoreCase("credit")) {
                    totalCredit += amount;
                    totalBalance += amount;
                } else if (type.equalsIgnoreCase("debit")) {
                    totalDebit += amount;
                    totalBalance -= amount;
                }
            }

            updateBalanceDisplay(symbol, totalBalance, totalCredit, totalDebit);

            transactionDates = new ArrayList<>(dateSet);
            Collections.sort(transactionDates, Collections.reverseOrder());
        } catch (Exception e) {
            e.printStackTrace();
            totalBalance = totalCredit = totalDebit = 0;
            updateBalanceDisplay(symbol, 0, 0, 0);
        }
    }

    private void loadBalanceDataFromRoom(String symbol) {
        com.accounting.balancex.data.repository.TransactionRepository repo = 
                new com.accounting.balancex.data.repository.TransactionRepository(this);
        repo.getAllTransactions(entities -> {
            if (entities == null || entities.isEmpty()) {
                runOnUiThread(() -> {
                    totalBalance = totalCredit = totalDebit = 0;
                    updateBalanceDisplay(symbol, 0, 0, 0);
                });
                return;
            }

            double credit = 0;
            double debit = 0;
            double balance = 0;
            Set<String> dateSet = new HashSet<>();

            for (com.accounting.balancex.data.entity.TransactionEntity e : entities) {
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
            });
        });
    }

    private void loadTransactionsFromStorage() {
        File file = com.accounting.balancex.data.db.DatabaseMigrator.findJsonFile(this);
        if (file == null) {
            file = new File("/storage/emulated/0/Documents/Accounting/transactions.json");
        }

        if (recentTransactions == null) {
            recentTransactions = new ArrayList<>();
        }
        recentTransactions.clear();

        if (!file.exists() || file.length() == 0) {
            if (adapter != null) adapter.notifyDataSetChanged();
            return;
        }

        try {
            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            reader.close();
            fis.close();

            String jsonStr = sb.toString().trim();
            if (jsonStr.isEmpty() || jsonStr.equals("[]")) {
                if (adapter != null) adapter.notifyDataSetChanged();
                return;
            }

            JSONArray transactionsArray = new JSONArray(jsonStr);
            int limit = Math.min(5, transactionsArray.length());

            for (int i = 0; i < limit; i++) {
                JSONObject transaction = transactionsArray.getJSONObject(i);
                String receiver = transaction.optString("receiver", "Unknown");
                String date = transaction.optString("date", "N/A");
                String amount = transaction.optString("amount", "0.00");
                String type = transaction.optString("textType", "debit");
                long entryId = transaction.optLong("entryId", i + 1);

                recentTransactions.add(new RecentTransactionModel(receiver, date, amount, type, entryId));
            }

            // 🔹 SORT transactions by Entry ID in DESCENDING ORDER (latest first)
            Collections.sort(recentTransactions, (t1, t2) -> Long.compare(t2.getEntryId(), t1.getEntryId()));

            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startAutoScroll() {
        if (scrollRunnable != null) {
            handler.removeCallbacks(scrollRunnable); // Prevent duplicate runnable
        }

        scrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (transactionDates == null || transactionDates.isEmpty())
                    return; // Prevent crashes

                // 🔹 Smooth scroll normally
                if (scrollPosition < transactionDates.size() - 1) {
                    scrollPosition++;
                    recyclerView.smoothScrollBy(0, 150); // Adjust for better smoothness
                }
                // 🔹 Handle last item (wait & reset smoothly)
                else {
                    recyclerView.smoothScrollToPosition(transactionDates.size() - 1); // Reach last item
                    new Handler().postDelayed(() -> {
                        recyclerView.scrollToPosition(0); // Reset to first item after delay
                        scrollPosition = 0; // Reset position
                    }, 1500); // Delay before resetting
                }

                handler.postDelayed(this, 4000); // Auto-scroll every 4 sec
            }
        };

        handler.postDelayed(scrollRunnable, 4000); // Start scrolling after 4 sec
    }

    @Override
    protected void onResume() {
        super.onResume();
        SettingsManager.applyTheme(this);
        startAutoScroll(); // Resume auto-scroll
        currentBalancePeriod = SettingsManager.getBalancePeriod(this);
        if (textBalancePeriod != null) {
            textBalancePeriod.setText(currentBalancePeriod);
        }
        loadProfileData();
        loadBalanceData();
        loadTransactionsFromStorage();
        if (modernChartBridge != null) {
            modernChartBridge.refresh();
        }
        if (modernPieChartBridge != null) {
            modernPieChartBridge.refresh();
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        com.accounting.balancex.data.repository.TransactionRepository repo = 
                new com.accounting.balancex.data.repository.TransactionRepository(this);
        com.accounting.balancex.data.db.DatabaseMigrator.migrateJsonToRoomIfNeeded(this, repo, () -> {
            runOnUiThread(() -> {
                loadBalanceData();
                loadTransactionsFromStorage();
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
        handler.removeCallbacks(scrollRunnable); // Stop auto-scroll to save resources
    }

    // ✅ Properly Used updateRecyclerView()
    private void updateRecyclerView() {
        adapter.notifyDataSetChanged(); // Refresh data
        scrollPosition = 0; // Reset scrolling position
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
