package com.accounting.balancex;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
    private EditText searchBox;
    private LinearLayout navHome, navTransactions, navEntry;
    private TextView filterByDateTextView;

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

        navHome = findViewById(R.id.navHome);
        navTransactions = findViewById(R.id.navTransactions);
        navEntry = findViewById(R.id.navEntry);

        transactionList = new ArrayList<>();
        filteredList = new ArrayList<>();

        loadTransactionsFromFile();

        adapter = new TransactionAdapter(this, filteredList);
        recyclerView.setAdapter(adapter);

        filterTransactions("All");

        textAll.setOnClickListener(v -> filterTransactions("All"));
        textCredits.setOnClickListener(v -> filterTransactions("Credit"));
        textDebits.setOnClickListener(v -> filterTransactions("Debit"));

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchTransaction(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        navHome.setOnClickListener(v -> startActivity(new Intent(TransactionActivity.this, MainActivity.class)));
        navTransactions.setOnClickListener(v -> {
        });
        navEntry.setOnClickListener(v -> startActivity(new Intent(TransactionActivity.this, EntryActivity.class)));

        filterByDateTextView = findViewById(R.id.filter_by_date);
        filterByDateTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFilterPopup(v);
            }
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
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "Accounting/transactions.json");

        if (!file.exists()) {
            Log.e("TransactionDebug", "File not found: " + file.getAbsolutePath());
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

            // Sort the transactions by date in descending order (newest first)
            Collections.sort(transactionList, (t1, t2) -> Long.compare(t2.getEntryId(), t1.getEntryId()));

            // Notify adapter to update UI
            adapter.notifyDataSetChanged();


            runOnUiThread(() -> {
                filteredList.clear();
                filteredList.addAll(transactionList);

                if (adapter == null) {
                    adapter = new TransactionAdapter(this, filteredList);
                    recyclerView.setAdapter(adapter);
                } else {
                    adapter.notifyDataSetChanged();
                }
            });

        } catch (Exception e) {
            Log.e("TransactionDebug", "Error loading transactions", e);
        }

        Log.d("TransactionDebug", "Loaded Transactions: " + transactionList.size());
    }

    private void filterTransactions(String type) {
        // Set the selected type to the current section
        String selectedType = type;
        filteredList.clear();

        // Filter based on the selected section type
        if (selectedType.equals("All")) {
            filteredList.addAll(transactionList);
        } else {
            for (Transaction t : transactionList) {
                if (t.getTransactionType().equals(selectedType)) {
                    filteredList.add(t);
                }
            }
        }

        // Update the adapter with filtered results
        adapter.updateList(filteredList);

        // Update the colors for the section titles
        textAll.setTextColor(selectedType.equals("All") ? Color.parseColor("#1e90ff") : Color.BLACK);
        textCredits.setTextColor(selectedType.equals("Credit") ? Color.parseColor("#1e90ff") : Color.BLACK);
        textDebits.setTextColor(selectedType.equals("Debit") ? Color.parseColor("#1e90ff") : Color.BLACK);
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
}