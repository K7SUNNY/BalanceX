package com.k7sunny.balancex;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {
    private static final String TAG = "HistoryActivity";

    // Data lists
    private final List<Transaction> allTransactions = new ArrayList<>();
    private final List<Transaction> filteredTransactions = new ArrayList<>();
    private TransactionAdapter adapter;

    // Views
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton btnScrollToTop;
    private TextView loadingText;
    private LinearLayout noTransactionsText;
    private TextView textEmptySub;

    // Summary Widgets
    private TextView textSummaryIncome, textCountIncome;
    private TextView textSummaryExpense, textCountExpense;

    // Search & Filter
    private EditText editSearchTransactions;
    private ImageView btnClearSearch;
    private TextView tabFilterAll, tabFilterIncome, tabFilterExpenses;
    private TextView textResultsCount, btnResetFilters;

    // Active Filter State
    private String currentTypeFilter = "All"; // "All", "Credit", "Debit"
    private String currentDateFilter = "All"; // "All", "Today", "Month", "Custom"
    private String customSelectedDate = "";
    private String currentSearchQuery = "";
    private int currentSortOption = R.id.option_newest_to_oldest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingsManager.applyTheme(this);
        setContentView(R.layout.activity_history);

        setupWindowInsets();
        initializeViews();
        setupNavigationDock();
        setupSegmentedFilters();
        setupSearch();
        setupSwipeRefresh();
        setupScrollListener();

        loadTransactionsFromFile();
    }

    private void setupWindowInsets() {
        View coordinator = findViewById(R.id.coordinator);
        if (coordinator != null) {
            ViewCompat.setOnApplyWindowInsetsListener(coordinator, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(insets.left, insets.top, insets.right, 0);

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
        }
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        btnScrollToTop = findViewById(R.id.btnScrollToTop);
        loadingText = findViewById(R.id.loadingText);
        noTransactionsText = findViewById(R.id.noTransactionsText);
        textEmptySub = findViewById(R.id.textEmptySub);

        textSummaryIncome = findViewById(R.id.textSummaryIncome);
        textCountIncome = findViewById(R.id.textCountIncome);
        textSummaryExpense = findViewById(R.id.textSummaryExpense);
        textCountExpense = findViewById(R.id.textCountExpense);

        editSearchTransactions = findViewById(R.id.editSearchTransactions);
        btnClearSearch = findViewById(R.id.btnClearSearch);

        tabFilterAll = findViewById(R.id.tabFilterAll);
        tabFilterIncome = findViewById(R.id.tabFilterIncome);
        tabFilterExpenses = findViewById(R.id.tabFilterExpenses);

        textResultsCount = findViewById(R.id.textResultsCount);
        btnResetFilters = findViewById(R.id.btnResetFilters);

        // Header Actions
//        ImageView btnBack = findViewById(R.id.btnBack);
//        if (btnBack != null) {
//            btnBack.setOnClickListener(v -> {
//                vibrateDevice();
//                finish();
//            });
//        }

        ImageView filterByDate = findViewById(R.id.filter_by_date);
        if (filterByDate != null) {
            filterByDate.setOnClickListener(this::showFilterSortMenu);
        }

        findViewById(R.id.btnAddTransaction).setOnClickListener(v -> {
            startActivity(new Intent(this, EntryActivity.class));
            vibrateDevice();
        });

        btnResetFilters.setOnClickListener(v -> resetAllFilters());

        // Setup RecyclerView Adapter
        adapter = new TransactionAdapter(this, filteredTransactions);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupSegmentedFilters() {
        tabFilterAll.setOnClickListener(v -> {
            vibrateDevice();
            selectSegmentTab("All");
        });

        tabFilterIncome.setOnClickListener(v -> {
            vibrateDevice();
            selectSegmentTab("Credit");
        });

        tabFilterExpenses.setOnClickListener(v -> {
            vibrateDevice();
            selectSegmentTab("Debit");
        });
    }

    private void selectSegmentTab(String type) {
        currentTypeFilter = type;

        int activeBg = R.drawable.bg_segmented_active;
        int activeText = ContextCompat.getColor(this, R.color.text_primary);
        int inactiveText = ContextCompat.getColor(this, R.color.text_secondary);

        if (type.equals("All")) {
            tabFilterAll.setBackgroundResource(activeBg);
            tabFilterAll.setTextColor(activeText);
            tabFilterAll.setTypeface(null, android.graphics.Typeface.BOLD);

            tabFilterIncome.setBackground(null);
            tabFilterIncome.setTextColor(inactiveText);
            tabFilterIncome.setTypeface(null, android.graphics.Typeface.NORMAL);

            tabFilterExpenses.setBackground(null);
            tabFilterExpenses.setTextColor(inactiveText);
            tabFilterExpenses.setTypeface(null, android.graphics.Typeface.NORMAL);
        } else if (type.equals("Credit")) {
            tabFilterAll.setBackground(null);
            tabFilterAll.setTextColor(inactiveText);
            tabFilterAll.setTypeface(null, android.graphics.Typeface.NORMAL);

            tabFilterIncome.setBackgroundResource(activeBg);
            tabFilterIncome.setTextColor(activeText);
            tabFilterIncome.setTypeface(null, android.graphics.Typeface.BOLD);

            tabFilterExpenses.setBackground(null);
            tabFilterExpenses.setTextColor(inactiveText);
            tabFilterExpenses.setTypeface(null, android.graphics.Typeface.NORMAL);
        } else {
            tabFilterAll.setBackground(null);
            tabFilterAll.setTextColor(inactiveText);
            tabFilterAll.setTypeface(null, android.graphics.Typeface.NORMAL);

            tabFilterIncome.setBackground(null);
            tabFilterIncome.setTextColor(inactiveText);
            tabFilterIncome.setTypeface(null, android.graphics.Typeface.NORMAL);

            tabFilterExpenses.setBackgroundResource(activeBg);
            tabFilterExpenses.setTextColor(activeText);
            tabFilterExpenses.setTypeface(null, android.graphics.Typeface.BOLD);
        }

        applyFiltersAndSort();
    }

    private void setupSearch() {
        editSearchTransactions.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString().trim() : "";
                if (btnClearSearch != null) {
                    btnClearSearch.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                }
                applyFiltersAndSort();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> {
            editSearchTransactions.setText("");
            currentSearchQuery = "";
            vibrateDevice();
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.color_primary));
        swipeRefreshLayout.setOnRefreshListener(() -> {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                loadTransactionsFromFile();
                swipeRefreshLayout.setRefreshing(false);
            }, 800);
        });
    }

    private void setupScrollListener() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) rv.getLayoutManager();
                if (layoutManager != null) {
                    int firstVisible = layoutManager.findFirstVisibleItemPosition();
                    if (firstVisible > 4) {
                        if (btnScrollToTop.getVisibility() != View.VISIBLE) {
                            btnScrollToTop.show();
                        }
                    } else {
                        if (btnScrollToTop.getVisibility() == View.VISIBLE) {
                            btnScrollToTop.hide();
                        }
                    }
                }
            }
        });

        btnScrollToTop.setOnClickListener(v -> {
            vibrateDevice();
            recyclerView.smoothScrollToPosition(0);
        });
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
                Toast.makeText(this, "Already on History Page", Toast.LENGTH_SHORT).show();
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
                startActivity(new Intent(this, ReportsActivity.class));
                vibrateDevice();
                finish();
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
    }

    private void loadTransactionsFromFile() {
        loadingText.setVisibility(View.VISIBLE);
        allTransactions.clear();

        File file = com.k7sunny.balancex.data.db.DatabaseMigrator.findJsonFile(this);
        if (file == null) {
            file = new File("/storage/emulated/0/Documents/Accounting/transactions.json");
        }
        if (!file.exists()) {
            loadingText.setVisibility(View.GONE);
            applyFiltersAndSort();
            return;
        }

        try (FileInputStream fis = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {

            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }

            JSONArray jsonArray = new JSONArray(jsonBuilder.toString());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                allTransactions.add(new Transaction(
                        obj.optString("date", "N/A"),
                        obj.optString("amount", "0"),
                        obj.optString("receiver", "Unknown"),
                        obj.optString("description", ""),
                        obj.optString("utr", "Unknown"),
                        obj.optString("comments", ""),
                        obj.optString("category", "General"),
                        obj.optString("transactionId", "N/A"),
                        obj.optString("paymentMethod", "Cash"),
                        obj.optString("textType", "Unknown"),
                        obj.optLong("entryId", System.currentTimeMillis() - i * 1000L)
                ));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading transactions from JSON file", e);
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
                runOnUiThread(() -> {
                    loadingText.setVisibility(View.GONE);
                    applyFiltersAndSort();
                });
            });
            return;
        }

        loadingText.setVisibility(View.GONE);
        applyFiltersAndSort();
    }

    private void applyFiltersAndSort() {
        filteredTransactions.clear();

        String query = currentSearchQuery.toLowerCase(Locale.getDefault());
        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String thisMonthPrefix = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());

        for (Transaction t : allTransactions) {
            // 1. Type Filter (All, Credit, Debit)
            if (!currentTypeFilter.equals("All") && !currentTypeFilter.equalsIgnoreCase(t.getTransactionType())) {
                continue;
            }

            // 2. Date Range Filter
            if (currentDateFilter.equals("Today")) {
                if (t.getDate() == null || !t.getDate().startsWith(todayStr)) {
                    continue;
                }
            } else if (currentDateFilter.equals("Month")) {
                if (t.getDate() == null || !t.getDate().startsWith(thisMonthPrefix)) {
                    continue;
                }
            } else if (currentDateFilter.equals("Custom")) {
                if (t.getDate() == null || !t.getDate().startsWith(customSelectedDate)) {
                    continue;
                }
            }

            // 3. Search Query Filter
            if (!query.isEmpty()) {
                boolean matchesReceiver = t.getReceiverName() != null && t.getReceiverName().toLowerCase().contains(query);
                boolean matchesCategory = t.getCategory() != null && t.getCategory().toLowerCase().contains(query);
                boolean matchesMethod = t.getPaymentMethod() != null && t.getPaymentMethod().toLowerCase().contains(query);
                boolean matchesUtr = t.getUtr() != null && t.getUtr().toLowerCase().contains(query);
                boolean matchesTxnId = t.getTransactionID() != null && t.getTransactionID().toLowerCase().contains(query);
                boolean matchesAmount = t.getAmount() != null && t.getAmount().contains(query);
                boolean matchesComments = t.getComments() != null && t.getComments().toLowerCase().contains(query);

                if (!matchesReceiver && !matchesCategory && !matchesMethod &&
                        !matchesUtr && !matchesTxnId && !matchesAmount && !matchesComments) {
                    continue;
                }
            }

            filteredTransactions.add(t);
        }

        // Apply Sorting
        sortList(filteredTransactions, currentSortOption);

        // Update Summary Cards & Count Header
        updateFinancialSummary(filteredTransactions);

        // Update Adapter
        adapter.updateList(new ArrayList<>(filteredTransactions));

        // Update Visibility & Empty State
        if (filteredTransactions.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            noTransactionsText.setVisibility(View.VISIBLE);
            if (!currentSearchQuery.isEmpty()) {
                textEmptySub.setText("No transactions match \"" + currentSearchQuery + "\"");
            } else {
                textEmptySub.setText("No transactions found for the selected filter.");
            }
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            noTransactionsText.setVisibility(View.GONE);
            animateListEntrance();
        }

        // Show/Hide Reset Button
        boolean isFiltered = !currentTypeFilter.equals("All") || !currentDateFilter.equals("All") || !currentSearchQuery.isEmpty();
        btnResetFilters.setVisibility(isFiltered ? View.VISIBLE : View.GONE);

        String countText = filteredTransactions.size() + (filteredTransactions.size() == 1 ? " Transaction" : " Transactions");
        if (isFiltered) {
            textResultsCount.setText("Showing " + countText);
        } else {
            textResultsCount.setText("All " + countText);
        }
    }

    private void sortList(List<Transaction> list, int sortOptionId) {
        if (list == null || list.isEmpty()) return;

        if (sortOptionId == R.id.option_newest_to_oldest) {
            Collections.sort(list, (t1, t2) -> Long.compare(t2.getEntryId(), t1.getEntryId()));
        } else if (sortOptionId == R.id.option_oldest_to_newest) {
            Collections.sort(list, (t1, t2) -> Long.compare(t1.getEntryId(), t2.getEntryId()));
        } else if (sortOptionId == R.id.option_amount_high) {
            Collections.sort(list, (t1, t2) -> {
                double a1 = parseAmount(t1.getAmount());
                double a2 = parseAmount(t2.getAmount());
                return Double.compare(a2, a1);
            });
        } else if (sortOptionId == R.id.option_amount_low) {
            Collections.sort(list, (t1, t2) -> {
                double a1 = parseAmount(t1.getAmount());
                double a2 = parseAmount(t2.getAmount());
                return Double.compare(a1, a2);
            });
        }
    }

    private void updateFinancialSummary(List<Transaction> list) {
        double totalInflow = 0;
        int countInflow = 0;
        double totalOutflow = 0;
        int countOutflow = 0;

        for (Transaction t : list) {
            double amt = parseAmount(t.getAmount());
            if (t.getTransactionType() != null && t.getTransactionType().equalsIgnoreCase("Credit")) {
                totalInflow += amt;
                countInflow++;
            } else {
                totalOutflow += amt;
                countOutflow++;
            }
        }

        String symbol = SettingsManager.getCurrencySymbol(this);
        textSummaryIncome.setText("+" + symbol + String.format(Locale.getDefault(), "%,.2f", totalInflow));
        textCountIncome.setText(countInflow + (countInflow == 1 ? " entry" : " entries"));

        textSummaryExpense.setText("-" + symbol + String.format(Locale.getDefault(), "%,.2f", totalOutflow));
        textCountExpense.setText(countOutflow + (countOutflow == 1 ? " entry" : " entries"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        SettingsManager.applyTheme(this);
        loadTransactionsFromFile();
    }

    private void showFilterSortMenu(View anchor) {
        vibrateDevice();
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.filter_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            vibrateDevice();
            int itemId = item.getItemId();

            if (itemId == R.id.option_newest_to_oldest ||
                    itemId == R.id.option_oldest_to_newest ||
                    itemId == R.id.option_amount_high ||
                    itemId == R.id.option_amount_low) {
                currentSortOption = itemId;
                applyFiltersAndSort();
                return true;
            } else if (itemId == R.id.option_filter_all) {
                currentDateFilter = "All";
                applyFiltersAndSort();
                return true;
            } else if (itemId == R.id.option_filter_today) {
                currentDateFilter = "Today";
                applyFiltersAndSort();
                return true;
            } else if (itemId == R.id.option_filter_month) {
                currentDateFilter = "Month";
                applyFiltersAndSort();
                return true;
            } else if (itemId == R.id.option_filter_custom_date) {
                pickCustomDate();
                return true;
            }

            return false;
        });

        popup.show();
    }

    private void pickCustomDate() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            customSelectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            currentDateFilter = "Custom";
            applyFiltersAndSort();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void resetAllFilters() {
        vibrateDevice();
        currentTypeFilter = "All";
        currentDateFilter = "All";
        customSelectedDate = "";
        currentSearchQuery = "";
        editSearchTransactions.setText("");
        currentSortOption = R.id.option_newest_to_oldest;

        selectSegmentTab("All");
    }

    private void animateListEntrance() {
        recyclerView.post(() -> {
            for (int i = 0; i < Math.min(recyclerView.getChildCount(), 8); i++) {
                View child = recyclerView.getChildAt(i);
                if (child != null) {
                    child.setTranslationY(40f);
                    child.setAlpha(0f);
                    child.animate()
                            .translationY(0f)
                            .alpha(1f)
                            .setStartDelay(i * 30L)
                            .setDuration(250)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                }
            }
        });
    }

    private double parseAmount(String amtStr) {
        if (amtStr == null || amtStr.trim().isEmpty()) return 0;
        try {
            return Double.parseDouble(amtStr.replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    public void vibrateDevice() {
        SettingsManager.vibrate(this);
    }
}