package com.accounting.balancex;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.icu.util.Calendar;
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

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.bottomsheet.BottomSheetDialog;

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
    private double totalBalance = 0, totalCredit = 0, totalDebit = 0;
    private ArrayList<String> transactionDates;
    private Handler handler = new Handler();
    private Runnable scrollRunnable;
    private int scrollPosition = 0;
    private RecyclerView recyclerView;
    private RecentTransactionsAdapter adapter;
    private List<RecentTransactionModel> recentTransactions;
    private PieChart pieChart;
    private String selectedTimeline = "M"; // Default to Months
    private String graphType = "bar"; // Default to Bar Graph
    private BarChart barChart;
    private LineChart lineChart;
    private DrawerLayout drawerLayout;
    private ImageView menuButton, notificationButton;
    // Back Press Handling
    private boolean backPressedOnce = false;
    private static final int EDIT_PROFILE_REQUEST = 1;

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

        // Initialize PieChart
        pieChart = findViewById(R.id.pieChart);
        setupPieChart();

        // Initialize Graph
        BarChart barChart = findViewById(R.id.barChart);
        LineChart lineChart = findViewById(R.id.lineChart);

        GraphManager graphManager = new GraphManager(this, barChart, lineChart);
        selectedTimeline = "M"; // Default to Months
        graphType = "bar"; // Default to Bar Graph
        graphManager.updateGraph(selectedTimeline, graphType);

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
            graphManager.updateGraph(selectedTimeline, graphType);
        });

        daysTextView.setOnClickListener(view -> {
            selectedTimeline = "D";
            updateTimelineSelection(daysTextView, monthsTextView, weeksTextView, yearsTextView);
            graphManager.updateGraph(selectedTimeline, graphType);
        });

        weeksTextView.setOnClickListener(view -> {
            selectedTimeline = "W";
            updateTimelineSelection(weeksTextView, monthsTextView, daysTextView, yearsTextView);
            graphManager.updateGraph(selectedTimeline, graphType);
        });

        yearsTextView.setOnClickListener(view -> {
            selectedTimeline = "Y";
            updateTimelineSelection(yearsTextView, monthsTextView, daysTextView, weeksTextView);
            graphManager.updateGraph(selectedTimeline, graphType);
        });

        // Graph Type Selection
        lineChartImageView.setOnClickListener(view -> {
            Log.d("GraphManager", "Line Chart button clicked!");
            graphType = "line";
            updateGraphTypeSelection(lineChartImageView, barGraphImageView);
            graphManager.updateGraph(selectedTimeline, graphType);
        });

        barGraphImageView.setOnClickListener(view -> {
            graphType = "bar";
            updateGraphTypeSelection(barGraphImageView, lineChartImageView);
            graphManager.updateGraph(selectedTimeline, graphType);
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

        // 2. Backup Ledger -> Share Encrypted JSON Snapshot
        View btnBackup = findViewById(R.id.btnDrawerBackup);
        if (btnBackup != null) {
            btnBackup.setOnClickListener(v -> {
                vibrateDevice();
                closeDrawerIfOpen();
                backupLedgerData();
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

        // 4. Categories & Budgets -> Categories Dialog
        View btnCategories = findViewById(R.id.btnDrawerCategories);
        if (btnCategories != null) {
            btnCategories.setOnClickListener(v -> {
                vibrateDevice();
                closeDrawerIfOpen();
                showCategoriesOverviewDialog();
            });
        }

        // 5. Payment Accounts -> Accounts Dialog
        View btnAccounts = findViewById(R.id.btnDrawerAccounts);
        if (btnAccounts != null) {
            btnAccounts.setOnClickListener(v -> {
                vibrateDevice();
                closeDrawerIfOpen();
                showPaymentAccountsDialog();
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
        SharedPreferences sharedPreferences = getSharedPreferences("UserProfile", MODE_PRIVATE);

        TextView userNameText = findViewById(R.id.drawerUserName);
        TextView bioText = findViewById(R.id.drawerUserBio);
        ImageView profileImage = findViewById(R.id.drawerProfileImage);

        if (userNameText != null) {
            userNameText.setText(sharedPreferences.getString("userName", "User Name"));
        }
        if (bioText != null) {
            bioText.setText(sharedPreferences.getString("bio", "Personal Ledger"));
        }

        if (profileImage != null) {
            String imageUriString = sharedPreferences.getString("profileImageUri", "");
            if (!imageUriString.isEmpty()) {
                try {
                    Uri imageUri = Uri.parse(imageUriString);
                    profileImage.setImageURI(imageUri);
                } catch (SecurityException e) {
                    Log.e("ProfileImage", "Permission denied for URI: " + imageUriString, e);
                    profileImage.setImageResource(R.drawable.ic_account);
                }
            } else {
                profileImage.setImageResource(R.drawable.ic_account);
            }
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

    private void backupLedgerData() {
        com.accounting.balancex.data.repository.TransactionRepository repo = 
                new com.accounting.balancex.data.repository.TransactionRepository(this);
        repo.getAllTransactions(transactions -> {
            if (transactions == null || transactions.isEmpty()) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "No transactions to backup.", Toast.LENGTH_SHORT).show());
                return;
            }
            try {
                com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
                String json = gson.toJson(transactions);
                File backupDir = new File(getCacheDir(), "backups");
                if (!backupDir.exists()) {
                    backupDir.mkdirs();
                }
                File backupFile = new File(backupDir, "BalanceX_Ledger_Backup.json");
                try (FileOutputStream fos = new FileOutputStream(backupFile)) {
                    fos.write(json.getBytes(StandardCharsets.UTF_8));
                }

                Uri fileUri = FileProvider.getUriForFile(
                        MainActivity.this,
                        getPackageName() + ".provider",
                        backupFile);

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("application/json");
                shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "BalanceX Ledger Backup");
                shareIntent.putExtra(Intent.EXTRA_TEXT, "Here is the JSON ledger backup from BalanceX (" + transactions.size() + " records).");
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                runOnUiThread(() -> startActivity(Intent.createChooser(shareIntent, "Share Ledger Backup")));
            } catch (Exception e) {
                Log.e("Backup", "Error creating backup", e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to create backup: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
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
            JSONArray array = new JSONArray(sb.toString().trim());
            List<com.accounting.balancex.data.entity.TransactionEntity> entities = new ArrayList<>();
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
                entities.add(entity);
            }

            if (!entities.isEmpty()) {
                com.accounting.balancex.data.repository.TransactionRepository repo =
                        new com.accounting.balancex.data.repository.TransactionRepository(this);
                repo.insertAll(entities, () -> runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Successfully restored " + entities.size() + " transactions into local vault!", Toast.LENGTH_LONG).show();
                    updateDrawerStats();
                    loadBalanceData();
                    loadTransactionsFromStorage();
                    setupPieChart();
                }));
            } else {
                Toast.makeText(this, "The selected file did not contain any valid transactions.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("Restore", "Failed to import backup", e);
            Toast.makeText(this, "Failed to restore backup: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showCategoriesOverviewDialog() {
        com.accounting.balancex.data.repository.TransactionRepository repo =
                new com.accounting.balancex.data.repository.TransactionRepository(this);
        repo.getAllTransactions(transactions -> runOnUiThread(() -> {
            if (transactions == null || transactions.isEmpty()) {
                Toast.makeText(this, "No transactions recorded yet.", Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, Integer> categoryCount = new HashMap<>();
            for (com.accounting.balancex.data.entity.TransactionEntity t : transactions) {
                String cat = (t.category != null && !t.category.trim().isEmpty()) ? t.category : "General";
                categoryCount.put(cat, categoryCount.getOrDefault(cat, 0) + 1);
            }
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
                sb.append("• ").append(entry.getKey()).append(": ").append(entry.getValue()).append(" transactions\n");
            }
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Categories & Budgets")
                    .setMessage(sb.toString().trim())
                    .setPositiveButton("Close", null)
                    .show();
        }));
    }

    private void showPaymentAccountsDialog() {
        com.accounting.balancex.data.repository.TransactionRepository repo =
                new com.accounting.balancex.data.repository.TransactionRepository(this);
        repo.getAllTransactions(transactions -> runOnUiThread(() -> {
            if (transactions == null || transactions.isEmpty()) {
                Toast.makeText(this, "No transactions recorded yet.", Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, Double> methodTotals = new HashMap<>();
            for (com.accounting.balancex.data.entity.TransactionEntity t : transactions) {
                String method = (t.paymentMethod != null && !t.paymentMethod.trim().isEmpty()) ? t.paymentMethod : "Cash";
                double amt = 0;
                try {
                    amt = Double.parseDouble(t.amount.replaceAll("[^0-9.]", ""));
                } catch (Exception ignored) {}
                methodTotals.put(method, methodTotals.getOrDefault(method, 0.0) + amt);
            }
            StringBuilder sb = new StringBuilder();
            String symbol = SettingsManager.getCurrencySymbol(this);
            for (Map.Entry<String, Double> entry : methodTotals.entrySet()) {
                sb.append("• ").append(entry.getKey()).append(": ").append(symbol).append(String.format(Locale.getDefault(), "%.2f", entry.getValue())).append("\n");
            }
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Payment Accounts")
                    .setMessage(sb.toString().trim())
                    .setPositiveButton("Close", null)
                    .show();
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
        if (requestCode == EDIT_PROFILE_REQUEST && resultCode == RESULT_OK) {
            loadProfileData(); // Reload updated data
        } else if (requestCode == RESTORE_FILE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
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

    private void loadBalanceData() {
        String symbol = SettingsManager.getCurrencySymbol(this);
        try {
            File file = com.accounting.balancex.data.db.DatabaseMigrator.findJsonFile(this);
            if (file == null) {
                file = new File("/storage/emulated/0/Documents/Accounting/transactions.json");
            }
            if (!file.exists() || file.length() == 0) {
                totalBalance = totalCredit = totalDebit = 0;
                textTotalBalance.setText(symbol + "0.00");
                textNetCredit.setText("+" + symbol + "0.00");
                textNetDebit.setText("-" + symbol + "0.00");
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
                totalBalance = totalCredit = totalDebit = 0;
                textTotalBalance.setText(symbol + "0.00");
                textNetCredit.setText("+" + symbol + "0.00");
                textNetDebit.setText("-" + symbol + "0.00");
                return;
            }

            JSONArray transactionsArray = new JSONArray(jsonStr);
            totalBalance = totalCredit = totalDebit = 0;
            Set<String> dateSet = new HashSet<>();

            for (int i = 0; i < transactionsArray.length(); i++) {
                JSONObject transaction = transactionsArray.getJSONObject(i);
                double amount = 0;
                try {
                    amount = transaction.getDouble("amount");
                } catch (Exception e) {
                    try {
                        amount = Double.parseDouble(transaction.getString("amount").replaceAll("[^0-9.]", ""));
                    } catch (Exception ignored) {}
                }
                String type = transaction.optString("textType", "debit");
                String date = transaction.optString("date", "N/A");

                dateSet.add(date);

                if (type.equalsIgnoreCase("credit")) {
                    totalCredit += amount;
                    totalBalance += amount;
                } else if (type.equalsIgnoreCase("debit")) {
                    totalDebit += amount;
                    totalBalance -= amount;
                }
            }

            textTotalBalance.setText(symbol + String.format(Locale.getDefault(), "%,.2f", totalBalance));
            textNetCredit.setText("+" + symbol + String.format(Locale.getDefault(), "%,.2f", totalCredit));
            textNetDebit.setText("-" + symbol + String.format(Locale.getDefault(), "%,.2f", totalDebit));

            transactionDates = new ArrayList<>(dateSet);
            Collections.sort(transactionDates, Collections.reverseOrder());
        } catch (Exception e) {
            e.printStackTrace();
            totalBalance = totalCredit = totalDebit = 0;
            textTotalBalance.setText(symbol + "0.00");
            textNetCredit.setText("+" + symbol + "0.00");
            textNetDebit.setText("-" + symbol + "0.00");
        }
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
        loadProfileData();
        loadBalanceData();
        loadTransactionsFromStorage();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        com.accounting.balancex.data.repository.TransactionRepository repo = 
                new com.accounting.balancex.data.repository.TransactionRepository(this);
        com.accounting.balancex.data.db.DatabaseMigrator.migrateJsonToRoomIfNeeded(this, repo, () -> {
            runOnUiThread(this::updateDrawerStats);
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

    // Initialize PieChart
    private ArrayList<PieModel> loadPieChartData() {
        ArrayList<PieModel> pieDataList = new ArrayList<>();
        HashMap<String, Float> categoryTotals = new HashMap<>();

        try {
            File file = new File("/storage/emulated/0/Documents/Accounting/transactions.json");
            if (!file.exists())
                return pieDataList; // Return empty if no data

            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder jsonContent = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
            reader.close();

            JSONArray transactionsArray = new JSONArray(jsonContent.toString());

            for (int i = 0; i < transactionsArray.length(); i++) {
                JSONObject transaction = transactionsArray.getJSONObject(i);

                String category = transaction.getString("category");
                float amount = Float.parseFloat(transaction.getString("amount"));

                if (categoryTotals.containsKey(category)) {
                    categoryTotals.put(category, categoryTotals.get(category) + amount);
                } else {
                    categoryTotals.put(category, amount);
                }
            }

            for (Map.Entry<String, Float> entry : categoryTotals.entrySet()) {
                pieDataList.add(new PieModel(entry.getKey(), entry.getValue()));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return pieDataList;
    }

    private void setupPieChart() {
        ArrayList<PieModel> pieDataList = loadPieChartData(); // Load data

        ArrayList<PieEntry> entries = new ArrayList<>();
        for (PieModel pieData : pieDataList) {
            entries.add(new PieEntry(pieData.getAmount(), pieData.getCategory()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Categories");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(14f);
        int textColor = ContextCompat.getColor(this, R.color.text_primary);
        dataSet.setValueTextColor(textColor);

        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);
        pieChart.getDescription().setEnabled(false);
        pieChart.setHoleColor(ContextCompat.getColor(this, R.color.surface_card));
        pieChart.getLegend().setTextColor(textColor);
        pieChart.setEntryLabelColor(textColor);
        pieChart.invalidate(); // Refresh chart
    }

    public void vibrateDevice() {
        SettingsManager.vibrate(this);
    }





}
