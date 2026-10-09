package com.k7sunny.balancex;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ReportsActivity extends AppCompatActivity {

    private static final String TAG = "ReportsActivity";

    // Period Filter Chips
    private TextView chipThisMonth, chipLastMonth, chipLast30Days, chipLastQuarter, chipFinancialYear, chipAllTime;
    private LinearLayout chipCustomRange;
    private TextView textCustomRangeLabel, textSelectedDateRange, textReportRecordCount;

    // Overview Hero Card
    private TextView textNetFlowAmount, textNetFlowStatus;
    private TextView textTotalInflow, textInflowCount;
    private TextView textTotalOutflow, textOutflowCount;
    private TextView textRatioLabel, textSavingsPercent;
    private ProgressBar progressFlowRatio;

    // KPI Mini Tiles
    private TextView textDailyBurn, textAverageTicket;
    private TextView textHighestExpense, textHighestExpenseParty;
    private TextView textActiveDays, textTransactionVelocity;

    // Breakdown Containers
    private LinearLayout layoutCategoryBreakdownContainer;
    private LinearLayout layoutPayeesContainer;
    private LinearLayout layoutMethodBreakdownContainer;

    // Export Statement Controls
    private TextView tabScopeAll, tabScopeDebit, tabScopeCredit;
    private MaterialCardView cardFormatPdf, cardFormatCsv, cardFormatJson;
    private RadioButton radioPdf, radioCsv, radioJson;
    private MaterialCheckBox checkIncludeUtr, checkIncludeNotes;
    private TextView btnSortToggle;

    private MaterialButton btnSaveDownloads, btnShareStatement;
    private TextView textExportSummaryNote;
    private View btnQuickShare;

    // Filter State
    private enum PeriodType { THIS_MONTH, LAST_MONTH, LAST_30_DAYS, LAST_QUARTER, FINANCIAL_YEAR, ALL_TIME, CUSTOM }
    private PeriodType currentPeriod = PeriodType.THIS_MONTH;
    private Calendar customStartCal, customEndCal;

    private enum ExportFormat { PDF, CSV, JSON }
    private ExportFormat currentFormat = ExportFormat.PDF;

    // Scope for the exported statement: "All", "Debit" (Expenses), "Credit" (Income)
    private String exportScope = "All";

    // Sort order: true = newest first, false = highest amount first
    private boolean isSortNewestFirst = true;

    // Raw & Filtered Data
    private final List<Transaction> allTransactions = new ArrayList<>();
    private final List<Transaction> periodTransactions = new ArrayList<>();

    // Aggregates
    private double totalInflowSum = 0;
    private double totalOutflowSum = 0;
    private int inflowCount = 0;
    private int outflowCount = 0;

    private String currencySymbol = "₹";
    private boolean isHapticEnabled = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingsManager.applyTheme(this);
        setContentView(R.layout.activity_reports);

        loadPreferences();
        setupWindowInsets();
        initializeViews();
        setupNavigationDock();
        setupPeriodFilters();
        setupScopeTabs();
        setupFormatSelection();
        setupExportConfig();
        setupActionButtons();

        loadTransactions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SettingsManager.applyTheme(this);
        loadPreferences();
        loadTransactions();
    }

    private void loadPreferences() {
        currencySymbol = SettingsManager.getCurrencySymbol(this);
        isHapticEnabled = SettingsManager.isHapticsEnabled(this);
    }

    private void setupWindowInsets() {
        View coordinator = findViewById(R.id.coordinator);
        if (coordinator != null) {
            ViewCompat.setOnApplyWindowInsetsListener(coordinator, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(insets.left, insets.top, insets.right, 0);

                View navbar = findViewById(R.id.navbar);
                if (navbar != null) {
                    int baseBottom = (int) (6 * getResources().getDisplayMetrics().density);
                    navbar.setPadding(
                            navbar.getPaddingLeft(),
                            navbar.getPaddingTop(),
                            navbar.getPaddingRight(),
                            baseBottom + insets.bottom
                    );
                }
                return windowInsets;
            });
        }
    }

    private void initializeViews() {
        btnQuickShare = findViewById(R.id.btnQuickShare);

        chipThisMonth = findViewById(R.id.chipThisMonth);
        chipLastMonth = findViewById(R.id.chipLastMonth);
        chipLast30Days = findViewById(R.id.chipLast30Days);
        chipLastQuarter = findViewById(R.id.chipLastQuarter);
        chipFinancialYear = findViewById(R.id.chipFinancialYear);
        chipAllTime = findViewById(R.id.chipAllTime);
        chipCustomRange = findViewById(R.id.chipCustomRange);
        textCustomRangeLabel = findViewById(R.id.textCustomRangeLabel);

        textSelectedDateRange = findViewById(R.id.textSelectedDateRange);
        textReportRecordCount = findViewById(R.id.textReportRecordCount);

        textNetFlowAmount = findViewById(R.id.textNetFlowAmount);
        textNetFlowStatus = findViewById(R.id.textNetFlowStatus);
        textTotalInflow = findViewById(R.id.textTotalInflow);
        textInflowCount = findViewById(R.id.textInflowCount);
        textTotalOutflow = findViewById(R.id.textTotalOutflow);
        textOutflowCount = findViewById(R.id.textOutflowCount);
        textRatioLabel = findViewById(R.id.textRatioLabel);
        textSavingsPercent = findViewById(R.id.textSavingsPercent);
        progressFlowRatio = findViewById(R.id.progressFlowRatio);

        textDailyBurn = findViewById(R.id.textDailyBurn);
        textAverageTicket = findViewById(R.id.textAverageTicket);
        textHighestExpense = findViewById(R.id.textHighestExpense);
        textHighestExpenseParty = findViewById(R.id.textHighestExpenseParty);
        textActiveDays = findViewById(R.id.textActiveDays);
        textTransactionVelocity = findViewById(R.id.textTransactionVelocity);

        layoutCategoryBreakdownContainer = findViewById(R.id.layoutCategoryBreakdownContainer);
        layoutPayeesContainer = findViewById(R.id.layoutPayeesContainer);
        layoutMethodBreakdownContainer = findViewById(R.id.layoutMethodBreakdownContainer);

        tabScopeAll = findViewById(R.id.tabScopeAll);
        tabScopeDebit = findViewById(R.id.tabScopeDebit);
        tabScopeCredit = findViewById(R.id.tabScopeCredit);

        cardFormatPdf = findViewById(R.id.cardFormatPdf);
        cardFormatCsv = findViewById(R.id.cardFormatCsv);
        cardFormatJson = findViewById(R.id.cardFormatJson);
        radioPdf = findViewById(R.id.radioPdf);
        radioCsv = findViewById(R.id.radioCsv);
        radioJson = findViewById(R.id.radioJson);

        checkIncludeUtr = findViewById(R.id.checkIncludeUtr);
        checkIncludeNotes = findViewById(R.id.checkIncludeNotes);
        btnSortToggle = findViewById(R.id.btnSortToggle);

        btnSaveDownloads = findViewById(R.id.btnSaveDownloads);
        btnShareStatement = findViewById(R.id.btnShareStatement);
        textExportSummaryNote = findViewById(R.id.textExportSummaryNote);
    }

    private void setupNavigationDock() {
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                startActivity(new Intent(this, MainActivity.class));
                vibrateDevice();
                finish();
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
                finish();
            });
        }

        View navExport = findViewById(R.id.navExport);
        if (navExport != null) {
            navExport.setOnClickListener(v -> {
                Toast.makeText(this, "Already on Reports", Toast.LENGTH_SHORT).show();
            });
        }

        View navSettings = findViewById(R.id.navSettings);
        if (navSettings != null) {
            navSettings.setOnClickListener(v -> {
                startActivity(new Intent(this, SettingsActivity.class));
                vibrateDevice();
                finish();
            });
        }
    }

    private void setupPeriodFilters() {
        chipThisMonth.setOnClickListener(v -> selectPeriod(PeriodType.THIS_MONTH));
        chipLastMonth.setOnClickListener(v -> selectPeriod(PeriodType.LAST_MONTH));
        chipLast30Days.setOnClickListener(v -> selectPeriod(PeriodType.LAST_30_DAYS));
        chipLastQuarter.setOnClickListener(v -> selectPeriod(PeriodType.LAST_QUARTER));
        chipFinancialYear.setOnClickListener(v -> selectPeriod(PeriodType.FINANCIAL_YEAR));
        chipAllTime.setOnClickListener(v -> selectPeriod(PeriodType.ALL_TIME));
        chipCustomRange.setOnClickListener(v -> showCustomDateRangePicker());
        updatePeriodChipsUI();
    }

    private void selectPeriod(PeriodType period) {
        currentPeriod = period;
        vibrateDevice();
        updatePeriodChipsUI();
        applyFiltersAndCalculate();
    }

    private void updatePeriodChipsUI() {
        int activeBg = R.drawable.rounded_corner_container_fliter_section;
        int inactiveBg = R.drawable.rounded_corner_unselected;

        int activeColor = ContextCompat.getColor(this, R.color.text_primary);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_secondary);

        chipThisMonth.setBackgroundResource(currentPeriod == PeriodType.THIS_MONTH ? activeBg : inactiveBg);
        chipThisMonth.setTextColor(currentPeriod == PeriodType.THIS_MONTH ? activeColor : inactiveColor);
        chipThisMonth.setTypeface(null, currentPeriod == PeriodType.THIS_MONTH ? Typeface.BOLD : Typeface.NORMAL);

        chipLastMonth.setBackgroundResource(currentPeriod == PeriodType.LAST_MONTH ? activeBg : inactiveBg);
        chipLastMonth.setTextColor(currentPeriod == PeriodType.LAST_MONTH ? activeColor : inactiveColor);
        chipLastMonth.setTypeface(null, currentPeriod == PeriodType.LAST_MONTH ? Typeface.BOLD : Typeface.NORMAL);

        chipLast30Days.setBackgroundResource(currentPeriod == PeriodType.LAST_30_DAYS ? activeBg : inactiveBg);
        chipLast30Days.setTextColor(currentPeriod == PeriodType.LAST_30_DAYS ? activeColor : inactiveColor);
        chipLast30Days.setTypeface(null, currentPeriod == PeriodType.LAST_30_DAYS ? Typeface.BOLD : Typeface.NORMAL);

        chipLastQuarter.setBackgroundResource(currentPeriod == PeriodType.LAST_QUARTER ? activeBg : inactiveBg);
        chipLastQuarter.setTextColor(currentPeriod == PeriodType.LAST_QUARTER ? activeColor : inactiveColor);
        chipLastQuarter.setTypeface(null, currentPeriod == PeriodType.LAST_QUARTER ? Typeface.BOLD : Typeface.NORMAL);

        chipFinancialYear.setBackgroundResource(currentPeriod == PeriodType.FINANCIAL_YEAR ? activeBg : inactiveBg);
        chipFinancialYear.setTextColor(currentPeriod == PeriodType.FINANCIAL_YEAR ? activeColor : inactiveColor);
        chipFinancialYear.setTypeface(null, currentPeriod == PeriodType.FINANCIAL_YEAR ? Typeface.BOLD : Typeface.NORMAL);

        chipAllTime.setBackgroundResource(currentPeriod == PeriodType.ALL_TIME ? activeBg : inactiveBg);
        chipAllTime.setTextColor(currentPeriod == PeriodType.ALL_TIME ? activeColor : inactiveColor);
        chipAllTime.setTypeface(null, currentPeriod == PeriodType.ALL_TIME ? Typeface.BOLD : Typeface.NORMAL);

        chipCustomRange.setBackgroundResource(currentPeriod == PeriodType.CUSTOM ? activeBg : inactiveBg);
        textCustomRangeLabel.setTextColor(currentPeriod == PeriodType.CUSTOM ? activeColor : inactiveColor);
        textCustomRangeLabel.setTypeface(null, currentPeriod == PeriodType.CUSTOM ? Typeface.BOLD : Typeface.NORMAL);
    }

    private void showCustomDateRangePicker() {
        final Calendar now = Calendar.getInstance();
        DatePickerDialog startDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    customStartCal = Calendar.getInstance();
                    customStartCal.set(year, month, dayOfMonth, 0, 0, 0);
                    customStartCal.set(Calendar.MILLISECOND, 0);

                    DatePickerDialog endDialog = new DatePickerDialog(
                            this,
                            (endView, endYear, endMonth, endDay) -> {
                                customEndCal = Calendar.getInstance();
                                customEndCal.set(endYear, endMonth, endDay, 23, 59, 59);
                                customEndCal.set(Calendar.MILLISECOND, 999);

                                SimpleDateFormat shortFmt = new SimpleDateFormat("dd MMM", Locale.getDefault());
                                textCustomRangeLabel.setText(shortFmt.format(customStartCal.getTime()) + " - " + shortFmt.format(customEndCal.getTime()));
                                selectPeriod(PeriodType.CUSTOM);
                            },
                            now.get(Calendar.YEAR),
                            now.get(Calendar.MONTH),
                            now.get(Calendar.DAY_OF_MONTH)
                    );
                    endDialog.setTitle("Select End Date");
                    endDialog.show();
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        );
        startDialog.setTitle("Select Start Date");
        startDialog.show();
    }

    private void setupScopeTabs() {
        tabScopeAll.setOnClickListener(v -> setExportScope("All"));
        tabScopeDebit.setOnClickListener(v -> setExportScope("Debit"));
        tabScopeCredit.setOnClickListener(v -> setExportScope("Credit"));
        updateScopeTabsUI();
    }

    private void setExportScope(String scope) {
        exportScope = scope;
        vibrateDevice();
        updateScopeTabsUI();
        updateExportButtonBadges();
    }

    private void updateScopeTabsUI() {
        int activeBg = R.drawable.bg_segmented_active;
        int activeColor = ContextCompat.getColor(this, R.color.text_primary);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_secondary);

        tabScopeAll.setBackgroundResource("All".equals(exportScope) ? activeBg : android.R.color.transparent);
        tabScopeAll.setTextColor("All".equals(exportScope) ? activeColor : inactiveColor);
        tabScopeAll.setTypeface(null, "All".equals(exportScope) ? Typeface.BOLD : Typeface.NORMAL);

        tabScopeDebit.setBackgroundResource("Debit".equals(exportScope) ? activeBg : android.R.color.transparent);
        tabScopeDebit.setTextColor("Debit".equals(exportScope) ? activeColor : inactiveColor);
        tabScopeDebit.setTypeface(null, "Debit".equals(exportScope) ? Typeface.BOLD : Typeface.NORMAL);

        tabScopeCredit.setBackgroundResource("Credit".equals(exportScope) ? activeBg : android.R.color.transparent);
        tabScopeCredit.setTextColor("Credit".equals(exportScope) ? activeColor : inactiveColor);
        tabScopeCredit.setTypeface(null, "Credit".equals(exportScope) ? Typeface.BOLD : Typeface.NORMAL);
    }

    private void setupFormatSelection() {
        View.OnClickListener listener = v -> {
            if (v == cardFormatPdf) {
                currentFormat = ExportFormat.PDF;
            } else if (v == cardFormatCsv) {
                currentFormat = ExportFormat.CSV;
            } else if (v == cardFormatJson) {
                currentFormat = ExportFormat.JSON;
            }
            vibrateDevice();
            updateFormatUI();
        };

        cardFormatPdf.setOnClickListener(listener);
        cardFormatCsv.setOnClickListener(listener);
        cardFormatJson.setOnClickListener(listener);

        updateFormatUI();
    }

    private void updateFormatUI() {
        int primaryColor = ContextCompat.getColor(this, R.color.color_primary);
        int subtleBorder = ContextCompat.getColor(this, R.color.border_subtle);

        cardFormatPdf.setStrokeColor(currentFormat == ExportFormat.PDF ? primaryColor : subtleBorder);
        cardFormatPdf.setStrokeWidth(currentFormat == ExportFormat.PDF ? 2 : 1);
        radioPdf.setChecked(currentFormat == ExportFormat.PDF);

        cardFormatCsv.setStrokeColor(currentFormat == ExportFormat.CSV ? primaryColor : subtleBorder);
        cardFormatCsv.setStrokeWidth(currentFormat == ExportFormat.CSV ? 2 : 1);
        radioCsv.setChecked(currentFormat == ExportFormat.CSV);

        cardFormatJson.setStrokeColor(currentFormat == ExportFormat.JSON ? primaryColor : subtleBorder);
        cardFormatJson.setStrokeWidth(currentFormat == ExportFormat.JSON ? 2 : 1);
        radioJson.setChecked(currentFormat == ExportFormat.JSON);
    }

    private void setupExportConfig() {
        btnSortToggle.setOnClickListener(v -> {
            isSortNewestFirst = !isSortNewestFirst;
            btnSortToggle.setText(isSortNewestFirst ? "Newest First ▾" : "Highest Amount ▾");
            vibrateDevice();
        });
    }

    private void setupActionButtons() {
        btnSaveDownloads.setOnClickListener(v -> {
            vibrateDevice();
            performExport(false);
        });

        btnShareStatement.setOnClickListener(v -> {
            vibrateDevice();
            performExport(true);
        });

        btnQuickShare.setOnClickListener(v -> {
            vibrateDevice();
            performExport(true);
        });
    }

    private void loadTransactions() {
        allTransactions.clear();
        try {
            File file = com.k7sunny.balancex.data.db.DatabaseMigrator.findJsonFile(this);
            if (file == null) {
                file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Accounting/transactions.json");
            }
            if (!file.exists()) {
                file = new File(getFilesDir(), "transactions.json");
            }

            if (file.exists() && file.length() > 2) {
                String json;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                } else {
                    FileInputStream fis = new FileInputStream(file);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    json = sb.toString();
                }
                JSONArray array = new JSONArray(json.trim());
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    allTransactions.add(new Transaction(
                            obj.optString("date", "N/A"),
                            obj.optString("amount", "0"),
                            obj.optString("receiver", "Unknown"),
                            obj.optString("description", ""),
                            obj.optString("utr", "N/A"),
                            obj.optString("comments", ""),
                            obj.optString("category", "General"),
                            obj.optString("transactionId", "N/A"),
                            obj.optString("paymentMethod", "Cash"),
                            obj.optString("textType", "Debit"),
                            obj.optLong("entryId", System.currentTimeMillis() - i * 1000L)
                    ));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading transactions", e);
        }

        if (allTransactions.isEmpty()) {
            com.k7sunny.balancex.data.repository.TransactionRepository repo =
                    new com.k7sunny.balancex.data.repository.TransactionRepository(this);
            repo.getAllTransactions(entities -> {
                if (entities != null && !entities.isEmpty()) {
                    for (com.k7sunny.balancex.data.entity.TransactionEntity e : entities) {
                        allTransactions.add(new Transaction(
                                e.date, e.amount, e.receiver, e.description,
                                e.utr, e.comments, e.category, e.transactionId,
                                e.paymentMethod, e.textType, e.entryId
                        ));
                    }
                }
                runOnUiThread(this::applyFiltersAndCalculate);
            });
            return;
        }

        applyFiltersAndCalculate();
    }

    private void applyFiltersAndCalculate() {
        periodTransactions.clear();
        totalInflowSum = 0;
        totalOutflowSum = 0;
        inflowCount = 0;
        outflowCount = 0;

        SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar now = Calendar.getInstance();

        Calendar startRange = null;
        Calendar endRange = null;
        String rangeSubtitle = "";

        switch (currentPeriod) {
            case THIS_MONTH: {
                startRange = Calendar.getInstance();
                startRange.set(Calendar.DAY_OF_MONTH, 1);
                startRange.set(Calendar.HOUR_OF_DAY, 0);
                startRange.set(Calendar.MINUTE, 0);
                startRange.set(Calendar.SECOND, 0);
                startRange.set(Calendar.MILLISECOND, 0);

                endRange = Calendar.getInstance();
                endRange.set(Calendar.DAY_OF_MONTH, endRange.getActualMaximum(Calendar.DAY_OF_MONTH));
                endRange.set(Calendar.HOUR_OF_DAY, 23);
                endRange.set(Calendar.MINUTE, 59);
                endRange.set(Calendar.SECOND, 59);
                endRange.set(Calendar.MILLISECOND, 999);

                SimpleDateFormat monthFmt = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                rangeSubtitle = "Current Month • " + monthFmt.format(now.getTime());
                break;
            }
            case LAST_MONTH: {
                startRange = Calendar.getInstance();
                startRange.add(Calendar.MONTH, -1);
                startRange.set(Calendar.DAY_OF_MONTH, 1);
                startRange.set(Calendar.HOUR_OF_DAY, 0);
                startRange.set(Calendar.MINUTE, 0);
                startRange.set(Calendar.SECOND, 0);
                startRange.set(Calendar.MILLISECOND, 0);

                endRange = Calendar.getInstance();
                endRange.add(Calendar.MONTH, -1);
                endRange.set(Calendar.DAY_OF_MONTH, endRange.getActualMaximum(Calendar.DAY_OF_MONTH));
                endRange.set(Calendar.HOUR_OF_DAY, 23);
                endRange.set(Calendar.MINUTE, 59);
                endRange.set(Calendar.SECOND, 59);
                endRange.set(Calendar.MILLISECOND, 999);

                SimpleDateFormat monthFmt = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                rangeSubtitle = "Last Month • " + monthFmt.format(startRange.getTime());
                break;
            }
            case LAST_30_DAYS: {
                startRange = Calendar.getInstance();
                startRange.add(Calendar.DAY_OF_YEAR, -30);
                startRange.set(Calendar.HOUR_OF_DAY, 0);
                startRange.set(Calendar.MINUTE, 0);
                startRange.set(Calendar.SECOND, 0);
                startRange.set(Calendar.MILLISECOND, 0);

                endRange = Calendar.getInstance();
                endRange.set(Calendar.HOUR_OF_DAY, 23);
                endRange.set(Calendar.MINUTE, 59);
                endRange.set(Calendar.SECOND, 59);
                endRange.set(Calendar.MILLISECOND, 999);

                SimpleDateFormat shortFmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                rangeSubtitle = "Last 30 Days • " + shortFmt.format(startRange.getTime()) + " - " + shortFmt.format(endRange.getTime());
                break;
            }
            case LAST_QUARTER: {
                startRange = Calendar.getInstance();
                startRange.add(Calendar.DAY_OF_YEAR, -90);
                startRange.set(Calendar.HOUR_OF_DAY, 0);
                startRange.set(Calendar.MINUTE, 0);
                startRange.set(Calendar.SECOND, 0);
                startRange.set(Calendar.MILLISECOND, 0);

                endRange = Calendar.getInstance();
                endRange.set(Calendar.HOUR_OF_DAY, 23);
                endRange.set(Calendar.MINUTE, 59);
                endRange.set(Calendar.SECOND, 59);
                endRange.set(Calendar.MILLISECOND, 999);

                SimpleDateFormat shortFmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                rangeSubtitle = "Last 90 Days • " + shortFmt.format(startRange.getTime()) + " - " + shortFmt.format(endRange.getTime());
                break;
            }
            case FINANCIAL_YEAR: {
                int year = now.get(Calendar.YEAR);
                if (now.get(Calendar.MONTH) < Calendar.APRIL) {
                    year -= 1;
                }
                startRange = Calendar.getInstance();
                startRange.set(year, Calendar.APRIL, 1, 0, 0, 0);
                startRange.set(Calendar.MILLISECOND, 0);

                endRange = Calendar.getInstance();
                endRange.set(year + 1, Calendar.MARCH, 31, 23, 59, 59);
                endRange.set(Calendar.MILLISECOND, 999);

                rangeSubtitle = "FY " + year + "-" + (year + 1) + " (Apr - Mar)";
                break;
            }
            case ALL_TIME: {
                startRange = null;
                endRange = null;
                rangeSubtitle = "Complete Transaction History (All Time)";
                break;
            }
            case CUSTOM: {
                if (customStartCal != null && customEndCal != null) {
                    startRange = customStartCal;
                    endRange = customEndCal;
                    SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                    rangeSubtitle = df.format(startRange.getTime()) + " - " + df.format(endRange.getTime());
                } else {
                    rangeSubtitle = "Custom Range (Select Dates)";
                }
                break;
            }
        }

        textSelectedDateRange.setText(rangeSubtitle);

        Map<String, Double> categoryTotals = new HashMap<>();
        Map<String, Integer> categoryCounts = new HashMap<>();

        Map<String, Double> payeeTotals = new HashMap<>();
        Map<String, Integer> payeeCounts = new HashMap<>();

        Map<String, Double> methodTotals = new HashMap<>();

        Set<String> uniqueDates = new HashSet<>();
        double highestExpenseAmount = 0;
        String highestExpenseParty = "None";

        for (Transaction t : allTransactions) {
            // Filter by date range for the overview
            if (startRange != null && endRange != null) {
                Date date = parseTransactionDate(t.getDate());
                if (date != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    if (cal.before(startRange) || cal.after(endRange)) {
                        continue;
                    }
                }
            }

            periodTransactions.add(t);

            double amount = 0;
            try {
                amount = Double.parseDouble(t.getAmount().replaceAll("[^0-9.]", ""));
            } catch (Exception ignored) {}

            boolean isCredit = "Credit".equalsIgnoreCase(t.getTransactionType());
            if (isCredit) {
                totalInflowSum += amount;
                inflowCount++;
            } else {
                totalOutflowSum += amount;
                outflowCount++;

                if (amount > highestExpenseAmount) {
                    highestExpenseAmount = amount;
                    highestExpenseParty = t.getReceiverName() != null ? t.getReceiverName() : "Unknown";
                }

                // Category breakdown for expenses
                String cat = t.getCategory();
                if (cat == null || cat.trim().isEmpty()) cat = "General";
                categoryTotals.put(cat, categoryTotals.getOrDefault(cat, 0.0) + amount);
                categoryCounts.put(cat, categoryCounts.getOrDefault(cat, 0) + 1);
            }

            // Top Payees / Recipients (overall volume)
            String receiver = t.getReceiverName();
            if (receiver == null || receiver.trim().isEmpty()) receiver = "Unknown";
            payeeTotals.put(receiver, payeeTotals.getOrDefault(receiver, 0.0) + amount);
            payeeCounts.put(receiver, payeeCounts.getOrDefault(receiver, 0) + 1);

            // Payment Methods
            String method = t.getPaymentMethod();
            if (method == null || method.trim().isEmpty()) method = "Other";
            methodTotals.put(method, methodTotals.getOrDefault(method, 0.0) + amount);

            if (t.getDate() != null && !t.getDate().isEmpty()) {
                uniqueDates.add(t.getDate());
            }
        }

        // Record Count Badge
        textReportRecordCount.setText(periodTransactions.size() + " records");

        // Format Currency
        NumberFormat currencyFormat = NumberFormat.getNumberInstance(Locale.getDefault());
        currencyFormat.setMinimumFractionDigits(2);
        currencyFormat.setMaximumFractionDigits(2);

        textTotalInflow.setText(currencySymbol + " " + currencyFormat.format(totalInflowSum));
        textInflowCount.setText(inflowCount + " " + (inflowCount == 1 ? "entry" : "entries"));

        textTotalOutflow.setText(currencySymbol + " " + currencyFormat.format(totalOutflowSum));
        textOutflowCount.setText(outflowCount + " " + (outflowCount == 1 ? "entry" : "entries"));

        double netFlow = totalInflowSum - totalOutflowSum;
        String sign = netFlow >= 0 ? "+ " : "- ";
        textNetFlowAmount.setText(currencySymbol + " " + sign + currencyFormat.format(Math.abs(netFlow)));

        int colorIncome = ContextCompat.getColor(this, R.color.finance_income);
        int colorExpense = ContextCompat.getColor(this, R.color.finance_expense);

        if (netFlow >= 0) {
            textNetFlowAmount.setTextColor(colorIncome);
            textNetFlowStatus.setText("Positive Net Savings");
            textNetFlowStatus.setTextColor(colorIncome);
        } else {
            textNetFlowAmount.setTextColor(colorExpense);
            textNetFlowStatus.setText("Deficit Outflow");
            textNetFlowStatus.setTextColor(colorExpense);
        }

        // Savings percentage & ratio bar
        if (totalInflowSum > 0) {
            double savingsRate = ((totalInflowSum - totalOutflowSum) / totalInflowSum) * 100.0;
            int progress = (int) Math.max(0, Math.min(100, Math.round((totalInflowSum / (totalInflowSum + totalOutflowSum)) * 100)));
            progressFlowRatio.setProgress(progress);
            if (savingsRate >= 0) {
                textSavingsPercent.setText(String.format(Locale.getDefault(), "%.1f%% Saved", savingsRate));
                textSavingsPercent.setTextColor(colorIncome);
            } else {
                textSavingsPercent.setText(String.format(Locale.getDefault(), "%.1f%% Deficit", Math.abs(savingsRate)));
                textSavingsPercent.setTextColor(colorExpense);
            }
        } else {
            progressFlowRatio.setProgress(totalOutflowSum > 0 ? 0 : 50);
            textSavingsPercent.setText("0.0% Saved");
            textSavingsPercent.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }

        // 3. Render KPIs
        int activeDaysCount = Math.max(1, uniqueDates.size());
        double dailyBurn = totalOutflowSum / activeDaysCount;
        textDailyBurn.setText(currencySymbol + " " + currencyFormat.format(dailyBurn) + " / day");

        int totalTxnCount = periodTransactions.size();
        double avgTicket = totalTxnCount > 0 ? (totalInflowSum + totalOutflowSum) / totalTxnCount : 0;
        textAverageTicket.setText(currencySymbol + " " + currencyFormat.format(avgTicket));

        textHighestExpense.setText(currencySymbol + " " + currencyFormat.format(highestExpenseAmount));
        textHighestExpenseParty.setText(highestExpenseParty);

        textActiveDays.setText(uniqueDates.size() + " days");
        double velocity = uniqueDates.isEmpty() ? 0 : (double) totalTxnCount / uniqueDates.size();
        textTransactionVelocity.setText(String.format(Locale.getDefault(), "%.1f txn / active day", velocity));

        // 4. Render Dynamic Sections
        renderBreakdownList(layoutCategoryBreakdownContainer, categoryTotals, categoryCounts, totalOutflowSum, "No expense categories in this period.");
        renderBreakdownList(layoutPayeesContainer, payeeTotals, payeeCounts, totalInflowSum + totalOutflowSum, "No payee data in this period.");
        renderMethodBreakdown(methodTotals, totalInflowSum + totalOutflowSum);

        updateExportButtonBadges();
    }

    private void renderBreakdownList(LinearLayout container, Map<String, Double> totals, Map<String, Integer> counts, double grandTotal, String emptyMessage) {
        container.removeAllViews();

        if (totals.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(emptyMessage);
            empty.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            empty.setTextSize(12);
            empty.setPadding(0, 10, 0, 10);
            container.addView(empty);
            return;
        }

        NumberFormat fmt = NumberFormat.getNumberInstance(Locale.getDefault());
        fmt.setMinimumFractionDigits(2);
        fmt.setMaximumFractionDigits(2);

        List<Map.Entry<String, Double>> sorted = new ArrayList<>(totals.entrySet());
        Collections.sort(sorted, (a, b) -> Double.compare(b.getValue(), a.getValue()));

        int maxItems = Math.min(5, sorted.size());
        for (int i = 0; i < maxItems; i++) {
            Map.Entry<String, Double> entry = sorted.get(i);
            String label = entry.getKey();
            double val = entry.getValue();
            int percent = grandTotal > 0 ? (int) Math.round((val / grandTotal) * 100) : 0;
            int count = counts != null ? counts.getOrDefault(label, 0) : 0;

            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setPadding(0, 6, 0, 6);

            LinearLayout topRow = new LinearLayout(this);
            topRow.setOrientation(LinearLayout.HORIZONTAL);

            TextView labelView = new TextView(this);
            String subtitle = count > 0 ? " (" + count + " txn • " + percent + "%)" : " (" + percent + "%)";
            labelView.setText(label + subtitle);
            labelView.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            labelView.setTextSize(13);
            labelView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView amountView = new TextView(this);
            amountView.setText(currencySymbol + " " + fmt.format(val));
            amountView.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            amountView.setTextSize(13);
            amountView.setTypeface(null, android.graphics.Typeface.BOLD);

            topRow.addView(labelView);
            topRow.addView(amountView);

            ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            bar.setIndeterminate(false);
            bar.setMax(100);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (int) (4 * getResources().getDisplayMetrics().density)
            );
            lp.topMargin = (int) (6 * getResources().getDisplayMetrics().density);
            bar.setLayoutParams(lp);
            Drawable progressDrawable = ContextCompat.getDrawable(this, R.drawable.custom_progress_bar_credit);
            if (progressDrawable != null) {
                bar.setProgressDrawable(progressDrawable.mutate());
            }
            bar.setProgress(percent);

            itemLayout.addView(topRow);
            itemLayout.addView(bar);

            container.addView(itemLayout);
        }
    }

    private void renderMethodBreakdown(Map<String, Double> methodTotals, double grandTotal) {
        layoutMethodBreakdownContainer.removeAllViews();

        if (methodTotals.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No payment channel data in this period.");
            empty.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            empty.setTextSize(12);
            empty.setPadding(0, 10, 0, 10);
            layoutMethodBreakdownContainer.addView(empty);
            return;
        }

        NumberFormat fmt = NumberFormat.getNumberInstance(Locale.getDefault());
        fmt.setMinimumFractionDigits(2);
        fmt.setMaximumFractionDigits(2);

        List<Map.Entry<String, Double>> sorted = new ArrayList<>(methodTotals.entrySet());
        Collections.sort(sorted, (a, b) -> Double.compare(b.getValue(), a.getValue()));

        for (Map.Entry<String, Double> entry : sorted) {
            String method = entry.getKey();
            double val = entry.getValue();
            int percent = grandTotal > 0 ? (int) Math.round((val / grandTotal) * 100) : 0;

            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setPadding(0, 6, 0, 6);

            LinearLayout topRow = new LinearLayout(this);
            topRow.setOrientation(LinearLayout.HORIZONTAL);

            TextView label = new TextView(this);
            label.setText(method + " (" + percent + "%)");
            label.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            label.setTextSize(13);
            label.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView amount = new TextView(this);
            amount.setText(currencySymbol + " " + fmt.format(val));
            amount.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            amount.setTextSize(13);
            amount.setTypeface(null, android.graphics.Typeface.BOLD);

            topRow.addView(label);
            topRow.addView(amount);

            ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            bar.setIndeterminate(false);
            bar.setMax(100);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (int) (4 * getResources().getDisplayMetrics().density)
            );
            lp.topMargin = (int) (6 * getResources().getDisplayMetrics().density);
            bar.setLayoutParams(lp);
            Drawable progressDrawable = ContextCompat.getDrawable(this, R.drawable.custom_progress_bar_credit);
            if (progressDrawable != null) {
                bar.setProgressDrawable(progressDrawable.mutate());
            }
            bar.setProgress(percent);

            itemLayout.addView(topRow);
            itemLayout.addView(bar);

            layoutMethodBreakdownContainer.addView(itemLayout);
        }
    }

    private List<Transaction> getExportTransactions() {
        List<Transaction> exportList = new ArrayList<>();
        for (Transaction t : periodTransactions) {
            if ("Debit".equalsIgnoreCase(exportScope) && !"Debit".equalsIgnoreCase(t.getTransactionType())) {
                continue;
            }
            if ("Credit".equalsIgnoreCase(exportScope) && !"Credit".equalsIgnoreCase(t.getTransactionType())) {
                continue;
            }
            exportList.add(t);
        }

        // Sort
        if (isSortNewestFirst) {
            Collections.sort(exportList, (a, b) -> {
                String d1 = a.getDate() != null ? a.getDate() : "";
                String d2 = b.getDate() != null ? b.getDate() : "";
                return d2.compareTo(d1);
            });
        } else {
            Collections.sort(exportList, (a, b) -> {
                double valA = 0, valB = 0;
                try { valA = Double.parseDouble(a.getAmount().replaceAll("[^0-9.]", "")); } catch (Exception ignored) {}
                try { valB = Double.parseDouble(b.getAmount().replaceAll("[^0-9.]", "")); } catch (Exception ignored) {}
                return Double.compare(valB, valA);
            });
        }

        return exportList;
    }

    private void updateExportButtonBadges() {
        List<Transaction> toExport = getExportTransactions();
        int count = toExport.size();
        String recordWord = count == 1 ? "transaction" : "transactions";
        if (textExportSummaryNote != null) {
            textExportSummaryNote.setText(count + " " + recordWord + " selected for export");
        }
        btnSaveDownloads.setText("Save File");
        btnShareStatement.setText("Share Statement");
    }

    private void performExport(boolean triggerShare) {
        List<Transaction> exportList = getExportTransactions();
        if (exportList.isEmpty()) {
            Toast.makeText(this, "No records match the selected scope to export", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File exportFile;
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());

            if (currentFormat == ExportFormat.PDF) {
                exportFile = generatePdfStatement(exportList, timeStamp);
            } else if (currentFormat == ExportFormat.CSV) {
                exportFile = generateCsvStatement(exportList, timeStamp);
            } else {
                exportFile = generateJsonStatement(exportList, timeStamp);
            }

            if (exportFile == null || !exportFile.exists()) {
                Toast.makeText(this, "Failed to create export file", Toast.LENGTH_SHORT).show();
                return;
            }

            if (triggerShare) {
                shareFile(exportFile);
            } else {
                Toast.makeText(this, "Saved to Downloads: " + exportFile.getName(), Toast.LENGTH_LONG).show();
            }
            com.k7sunny.balancex.notifications.AppNotificationManager.postReportExported(this, currentFormat.name(), exportFile.getName());

        } catch (Exception e) {
            Log.e(TAG, "Export failed", e);
            Toast.makeText(this, "Export error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private File generatePdfStatement(List<Transaction> exportList, String timeStamp) throws Exception {
        PdfDocument document = new PdfDocument();
        int pageWidth = 595; // A4 standard width in points
        int pageHeight = 842; // A4 standard height

        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);

        Paint bgPaint = new Paint();
        bgPaint.setAntiAlias(true);

        int pageNumber = 1;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        int margin = 36;
        int currentY = 50;

        String statementTitle = "BalanceX - Financial Statement";
        if ("Debit".equalsIgnoreCase(exportScope)) statementTitle = "BalanceX - Expense Statement";
        if ("Credit".equalsIgnoreCase(exportScope)) statementTitle = "BalanceX - Income Statement";

        // 1. Header & Title
        textPaint.setTextSize(20);
        textPaint.setFakeBoldText(true);
        textPaint.setColor(Color.parseColor("#1B4B82"));
        canvas.drawText(statementTitle, margin, currentY, textPaint);

        currentY += 20;
        textPaint.setTextSize(11);
        textPaint.setFakeBoldText(false);
        textPaint.setColor(Color.parseColor("#64748B"));
        String genTime = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date());
        canvas.drawText("Generated on: " + genTime + " | Period: " + textSelectedDateRange.getText().toString(), margin, currentY, textPaint);

        SharedPreferences userPrefs = getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
        boolean includeInPdf = userPrefs.getBoolean("includeInPdf", true);
        String profileName = userPrefs.getString("userName", "").trim();
        String profileCompany = userPrefs.getString("companyName", "").trim();

        if (includeInPdf && (!profileName.isEmpty() || !profileCompany.isEmpty())) {
            StringBuilder profileHeader = new StringBuilder();
            if (!profileName.isEmpty()) {
                profileHeader.append("Account Holder: ").append(profileName);
            }
            if (!profileCompany.isEmpty()) {
                if (profileHeader.length() > 0) profileHeader.append("  •  ");
                profileHeader.append("Entity: ").append(profileCompany);
            }
            currentY += 16;
            textPaint.setTextSize(10);
            textPaint.setColor(Color.parseColor("#475569"));
            canvas.drawText(profileHeader.toString(), margin, currentY, textPaint);
        }

        currentY += 24;

        // Calculate totals for the export subset
        double exportInflow = 0;
        double exportOutflow = 0;
        for (Transaction t : exportList) {
            double amt = 0;
            try { amt = Double.parseDouble(t.getAmount().replaceAll("[^0-9.]", "")); } catch (Exception ignored) {}
            if ("Credit".equalsIgnoreCase(t.getTransactionType())) exportInflow += amt;
            else exportOutflow += amt;
        }

        // 2. Executive Summary Box
        bgPaint.setColor(Color.parseColor("#F1F5F9"));
        RectF summaryBox = new RectF(margin, currentY, pageWidth - margin, currentY + 68);
        canvas.drawRoundRect(summaryBox, 8, 8, bgPaint);

        NumberFormat numFmt = NumberFormat.getNumberInstance(Locale.getDefault());
        numFmt.setMinimumFractionDigits(2);
        numFmt.setMaximumFractionDigits(2);

        textPaint.setTextSize(10);
        textPaint.setColor(Color.parseColor("#475569"));
        canvas.drawText("Total Inflow", margin + 16, currentY + 24, textPaint);
        canvas.drawText("Total Outflow", margin + 180, currentY + 24, textPaint);
        canvas.drawText("Net Balance", margin + 340, currentY + 24, textPaint);

        textPaint.setTextSize(14);
        textPaint.setFakeBoldText(true);
        textPaint.setColor(Color.parseColor("#137A4B"));
        canvas.drawText(currencySymbol + " " + numFmt.format(exportInflow), margin + 16, currentY + 48, textPaint);

        textPaint.setColor(Color.parseColor("#B91C1C"));
        canvas.drawText(currencySymbol + " " + numFmt.format(exportOutflow), margin + 180, currentY + 48, textPaint);

        double net = exportInflow - exportOutflow;
        textPaint.setColor(net >= 0 ? Color.parseColor("#137A4B") : Color.parseColor("#B91C1C"));
        canvas.drawText(currencySymbol + " " + (net >= 0 ? "+" : "-") + numFmt.format(Math.abs(net)), margin + 340, currentY + 48, textPaint);

        currentY += 88;

        // 3. Table Headers
        bgPaint.setColor(Color.parseColor("#1B4B82"));
        RectF tableHeader = new RectF(margin, currentY, pageWidth - margin, currentY + 24);
        canvas.drawRoundRect(tableHeader, 4, 4, bgPaint);

        textPaint.setTextSize(10);
        textPaint.setFakeBoldText(true);
        textPaint.setColor(Color.WHITE);
        canvas.drawText("Date", margin + 8, currentY + 16, textPaint);
        canvas.drawText("Party / Description", margin + 80, currentY + 16, textPaint);
        canvas.drawText("Category", margin + 260, currentY + 16, textPaint);
        canvas.drawText("Method", margin + 340, currentY + 16, textPaint);
        canvas.drawText("Amount (" + currencySymbol + ")", pageWidth - margin - 90, currentY + 16, textPaint);

        currentY += 32;

        // 4. Rows
        textPaint.setFakeBoldText(false);
        textPaint.setTextSize(9);

        boolean incUtr = checkIncludeUtr.isChecked();
        boolean incNotes = checkIncludeNotes.isChecked();

        for (int i = 0; i < exportList.size(); i++) {
            Transaction t = exportList.get(i);

            if (i % 2 == 1) {
                bgPaint.setColor(Color.parseColor("#F8FAFC"));
                canvas.drawRect(margin, currentY - 12, pageWidth - margin, currentY + 10, bgPaint);
            }

            textPaint.setColor(Color.parseColor("#1E293B"));
            canvas.drawText(t.getDate() != null ? t.getDate() : "N/A", margin + 8, currentY, textPaint);

            String receiver = t.getReceiverName() != null ? t.getReceiverName() : "Unknown";
            if (incNotes && t.getDescription() != null && !t.getDescription().isEmpty()) {
                receiver += " (" + t.getDescription() + ")";
            }
            if (receiver.length() > 28) receiver = receiver.substring(0, 25) + "...";
            canvas.drawText(receiver, margin + 80, currentY, textPaint);

            String cat = t.getCategory() != null ? t.getCategory() : "General";
            if (cat.length() > 14) cat = cat.substring(0, 12) + "..";
            canvas.drawText(cat, margin + 260, currentY, textPaint);

            String method = t.getPaymentMethod() != null ? t.getPaymentMethod() : "Cash";
            if (incUtr && t.getUtr() != null && !t.getUtr().equals("N/A") && !t.getUtr().isEmpty()) {
                method += " | " + t.getUtr();
            }
            if (method.length() > 16) method = method.substring(0, 14) + "..";
            textPaint.setColor(Color.parseColor("#475569"));
            canvas.drawText(method, margin + 340, currentY, textPaint);

            boolean isCredit = "Credit".equalsIgnoreCase(t.getTransactionType());
            textPaint.setColor(isCredit ? Color.parseColor("#137A4B") : Color.parseColor("#0F172A"));
            textPaint.setFakeBoldText(true);
            canvas.drawText(currencySymbol + " " + t.getAmount(), pageWidth - margin - 90, currentY, textPaint);
            textPaint.setFakeBoldText(false);

            currentY += 20;

            if (currentY > pageHeight - 50) {
                textPaint.setColor(Color.parseColor("#94A3B8"));
                textPaint.setTextSize(8);
                canvas.drawText("BalanceX Statement • Page " + pageNumber, margin, pageHeight - 24, textPaint);

                document.finishPage(page);
                pageNumber++;
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                currentY = 40;

                bgPaint.setColor(Color.parseColor("#1B4B82"));
                tableHeader = new RectF(margin, currentY, pageWidth - margin, currentY + 24);
                canvas.drawRoundRect(tableHeader, 4, 4, bgPaint);

                textPaint.setTextSize(10);
                textPaint.setFakeBoldText(true);
                textPaint.setColor(Color.WHITE);
                canvas.drawText("Date", margin + 8, currentY + 16, textPaint);
                canvas.drawText("Party / Description", margin + 80, currentY + 16, textPaint);
                canvas.drawText("Category", margin + 260, currentY + 16, textPaint);
                canvas.drawText("Method", margin + 340, currentY + 16, textPaint);
                canvas.drawText("Amount (" + currencySymbol + ")", pageWidth - margin - 90, currentY + 16, textPaint);
                currentY += 32;
                textPaint.setFakeBoldText(false);
                textPaint.setTextSize(9);
            }
        }

        textPaint.setColor(Color.parseColor("#94A3B8"));
        textPaint.setTextSize(8);
        canvas.drawText("BalanceX Statement • Page " + pageNumber + " • End of Report", margin, pageHeight - 24, textPaint);

        document.finishPage(page);

        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!downloadsDir.exists()) downloadsDir.mkdirs();

        File pdfFile = new File(downloadsDir, "BalanceX_Statement_" + timeStamp + ".pdf");
        FileOutputStream fos = new FileOutputStream(pdfFile);
        document.writeTo(fos);
        fos.flush();
        fos.close();
        document.close();

        return pdfFile;
    }

    private File generateCsvStatement(List<Transaction> exportList, String timeStamp) throws Exception {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!downloadsDir.exists()) downloadsDir.mkdirs();

        File csvFile = new File(downloadsDir, "BalanceX_Report_" + timeStamp + ".csv");
        FileWriter writer = new FileWriter(csvFile);

        writer.append("\"Date\",\"Receiver / Party\",\"Type\",\"Category\",\"Payment Method\",\"Amount\",\"UTR\",\"Description\",\"Comments\"\n");

        for (Transaction t : exportList) {
            writer.append("\"").append(escapeCsv(t.getDate())).append("\",");
            writer.append("\"").append(escapeCsv(t.getReceiverName())).append("\",");
            writer.append("\"").append(escapeCsv(t.getTransactionType())).append("\",");
            writer.append("\"").append(escapeCsv(t.getCategory())).append("\",");
            writer.append("\"").append(escapeCsv(t.getPaymentMethod())).append("\",");
            writer.append("\"").append(escapeCsv(t.getAmount())).append("\",");
            writer.append("\"").append(escapeCsv(t.getUtr())).append("\",");
            writer.append("\"").append(escapeCsv(t.getDescription())).append("\",");
            writer.append("\"").append(escapeCsv(t.getComments())).append("\"\n");
        }

        writer.flush();
        writer.close();
        return csvFile;
    }

    private File generateJsonStatement(List<Transaction> exportList, String timeStamp) throws Exception {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!downloadsDir.exists()) downloadsDir.mkdirs();

        File jsonFile = new File(downloadsDir, "BalanceX_Backup_" + timeStamp + ".json");
        JSONArray array = new JSONArray();

        for (Transaction t : exportList) {
            JSONObject obj = new JSONObject();
            obj.put("date", t.getDate());
            obj.put("amount", t.getAmount());
            obj.put("receiver", t.getReceiverName());
            obj.put("type", t.getTransactionType());
            obj.put("textType", t.getTransactionType());
            obj.put("category", t.getCategory());
            obj.put("paymentMethod", t.getPaymentMethod());
            obj.put("utr", t.getUtr());
            obj.put("description", t.getDescription());
            obj.put("comments", t.getComments());
            obj.put("entryId", t.getEntryId());
            array.put(obj);
        }

        FileWriter writer = new FileWriter(jsonFile);
        writer.write(array.toString(2));
        writer.flush();
        writer.close();
        return jsonFile;
    }

    private String escapeCsv(String str) {
        if (str == null) return "";
        return str.replace("\"", "\"\"");
    }

    private void shareFile(File file) {
        try {
            Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            if (file.getName().endsWith(".pdf")) {
                shareIntent.setType("application/pdf");
            } else if (file.getName().endsWith(".csv")) {
                shareIntent.setType("text/csv");
            } else {
                shareIntent.setType("application/json");
            }

            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "BalanceX Financial Report");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Attached is the financial statement generated via BalanceX.");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share Statement via"));
        } catch (Exception e) {
            Log.e(TAG, "Share failed", e);
            Toast.makeText(this, "Failed to share file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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

    private void vibrateDevice() {
        SettingsManager.vibrate(this);
    }
}
