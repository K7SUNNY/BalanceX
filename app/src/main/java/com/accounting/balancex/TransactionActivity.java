package com.accounting.balancex;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class TransactionActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private List<Transaction> transactionList;
    private List<Transaction> filteredList;
    private TextView textAll, textCredits, textDebits;
    private SearchView searchBox;
    private LinearLayout navHome, navTransactions, navEntry;
    private TextView filterByDateTextView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private String currentFilterType = "All"; // Default to All


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        textAll = findViewById(R.id.textAll);
        textCredits = findViewById(R.id.textCredits);
        textDebits = findViewById(R.id.textDebits);
        searchBox = findViewById(R.id.searchBox);
        filterByDateTextView = findViewById(R.id.filter_by_date);

        navHome = findViewById(R.id.navHome);
        navTransactions = findViewById(R.id.navTransactions);
        navEntry = findViewById(R.id.navEntry);

        transactionList = new ArrayList<>();
        filteredList = new ArrayList<>();

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        loadTransactionsFromFile();

        adapter = new TransactionAdapter(this, filteredList);
        recyclerView.setAdapter(adapter);

        filterTransactions("All");

        textAll.setOnClickListener(v -> filterTransactions("All"));
        textCredits.setOnClickListener(v -> filterTransactions("Credit"));
        textDebits.setOnClickListener(v -> filterTransactions("Debit"));

        searchBox.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchTransaction(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchTransaction(newText);
                return true;
            }
        });

        navHome.setOnClickListener(v -> startActivity(new Intent(TransactionActivity.this, MainActivity.class)));
        navTransactions.setOnClickListener(v -> {}); // No action needed
        navEntry.setOnClickListener(v -> startActivity(new Intent(TransactionActivity.this, EntryActivity.class)));

        filterByDateTextView.setOnClickListener(v -> showFilterPopup(v));

        // Pull-to-Refresh Listener
        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshTransactionList();
        });
    }
    private void showFilterPopup(View v) {
        PopupMenu popupMenu = new PopupMenu(TransactionActivity.this, v);
        // Inflate the menu with sorting options
        getMenuInflater().inflate(R.menu.filter_menu, popupMenu.getMenu());

        // Handle menu item selection using if-else instead of switch
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.option_newest_to_oldest) {
                    sortTransactions(false); // Newest to oldest
                } else if (item.getItemId() == R.id.option_oldest_to_newest) {
                    sortTransactions(true); // Oldest to newest
                }
                return false;
            }
        });

        // Show the popup menu
        popupMenu.show();
    }

    private void loadTransactionsFromFile() {
        transactionList.clear(); // Clear existing transactions

        TextView loadingText = findViewById(R.id.loadingText);
        TextView textNoTransactions = findViewById(R.id.noTransactionsText);
        recyclerView.setVisibility(View.GONE);
        loadingText.setVisibility(View.VISIBLE);
        textNoTransactions.setVisibility(View.GONE);

        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "Accounting/transactions.json");

        if (!file.exists()) {
            Log.e("TransactionDebug", "File not found: " + file.getAbsolutePath());
            loadingText.setVisibility(View.GONE);
            textNoTransactions.setVisibility(View.VISIBLE);
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
            transactionList.clear();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);

                transactionList.add(new Transaction(
                        obj.optString("date", "N/A"),
                        obj.optString("amount", "0"),
                        obj.optString("receiver", "Unknown"),
                        obj.optString("description", ""),
                        obj.optString("utr", "Unknown"),
                        obj.optString("comments", ""),
                        obj.optString("category", ""),
                        obj.optString("paymentMethod", "Cash"),
                        obj.optString("textType", "Unknown"),
                        obj.optLong("entryId", System.currentTimeMillis())
                ));
            }

            // Sort transactions by date (newest first)
            Collections.sort(transactionList, (t1, t2) -> Long.compare(t2.getEntryId(), t1.getEntryId()));

            runOnUiThread(() -> {
                // Apply the last selected filter type
                filterTransactions(currentFilterType);

                // Hide loading message
                loadingText.setVisibility(View.GONE);
            });

        } catch (Exception e) {
            Log.e("TransactionDebug", "Error loading transactions", e);
            loadingText.setVisibility(View.GONE);
            textNoTransactions.setVisibility(View.VISIBLE);
        }

        Log.d("TransactionDebug", "Loaded Transactions: " + transactionList.size());
    }
    private void filterTransactions(String type) {
        currentFilterType = type; // Store the selected type
        filteredList.clear();

        if (type.equals("All")) {
            filteredList.addAll(transactionList);
        } else {
            for (Transaction t : transactionList) {
                if (t.getTransactionType().equals(type)) {
                    filteredList.add(t);
                }
            }
        }

        adapter.updateList(filteredList);

        TextView textNoTransactions = findViewById(R.id.noTransactionsText);
        if (filteredList.isEmpty()) {
            textNoTransactions.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            textNoTransactions.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        recyclerView.post(() -> {
            for (int i = 0; i < recyclerView.getChildCount(); i++) {
                View child = recyclerView.getChildAt(i);
                if (child != null) {
                    child.setTranslationY(200f);
                    child.setAlpha(0f);

                    child.animate()
                            .translationY(0f)
                            .alpha(1f)
                            .setStartDelay(i * 150L)
                            .setDuration(500)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                }
            }
        });
        // Animate category selection with a subtle scale effect
        animateSelection(textAll, type.equals("All"));
        animateSelection(textCredits, type.equals("Credit"));
        animateSelection(textDebits, type.equals("Debit"));
    }
    // Helper method to apply smooth text size and scaling animation
    private void animateSelection(TextView textView, boolean isSelected) {
        float scaleFactor = isSelected ? 1.1f : 1f;
        long duration = isSelected ? 300 : 250;

        textView.animate()
                .scaleX(scaleFactor)
                .scaleY(scaleFactor)
                .setDuration(duration)
                .setInterpolator(new OvershootInterpolator()) // Smooth effect
                .start();

        textView.setPadding(isSelected ? 10 : 5, 5, isSelected ? 10 : 5, 5); // Adjust padding instead of text size

        // Keep background size fixed while changing color
        textView.setTextColor(isSelected ? Color.parseColor("#1e90ff") : Color.BLACK);
        textView.setBackgroundResource(isSelected ? R.drawable.selected_title : android.R.color.transparent);
    }
    private void sortTransactions(boolean isOldestToNewest) {
        if (!filteredList.isEmpty() && filteredList.get(0).getEntryId() != 0) { // Check if entryId exists
            if (isOldestToNewest) {
                Collections.sort(filteredList, (t1, t2) -> Long.compare(t1.getEntryId(), t2.getEntryId()));
            } else {
                Collections.sort(filteredList, (t1, t2) -> Long.compare(t2.getEntryId(), t1.getEntryId()));
            }
            adapter.notifyDataSetChanged();
        } else {
            Log.e("TransactionDebug", "Sorting skipped: entryId missing.");
        }
    }
    private void searchTransaction(String query) {
        List<Transaction> searchResults = new ArrayList<>();

        for (Transaction transaction : transactionList) {
            if (transaction.getReceiverName().toLowerCase().contains(query.toLowerCase()) ||
                    transaction.getUtr().toLowerCase().contains(query.toLowerCase())) {
                searchResults.add(transaction);
            }
        }

        adapter.updateList(searchResults);
    }
    // Refresh Transactions
    private void refreshTransactionList() {
        // Simulating a small delay for a natural feel
        new Handler().postDelayed(() -> {
            // Reload transactions (Use your existing method)
            loadTransactionsFromFile();

            // Stop refresh animation
            swipeRefreshLayout.setRefreshing(false);
        }, 1500); // Delay to make it smooth
    }
}