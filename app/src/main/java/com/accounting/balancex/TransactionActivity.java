package com.accounting.balancex;

import static com.accounting.balancex.R.*;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.search.SearchBar;
import com.google.android.material.search.SearchView;

//History page
public class TransactionActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private List<Transaction> transactionList;
    private List<Transaction> filteredList;
    private ChipGroup filterChipGroup;
    private SearchBar searchBar;
    private com.google.android.material.search.SearchView searchView;
    private MaterialToolbar toolbar;
    private LinearLayout navHome, navTransactions, navEntry;
    private ImageView filterByDateIcon;
    private SwipeRefreshLayout swipeRefreshLayout;
    private String currentFilterType = "All"; // Default to All
    private FloatingActionButton btnScrollToTop;
    private Button btnAddTransaction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        View coordinator = findViewById(R.id.coordinator);
        if (coordinator != null) {
            ViewCompat.setOnApplyWindowInsetsListener(coordinator, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(insets.left, insets.top, insets.right, 0);
                View navbar = findViewById(R.id.navbar);
                if (navbar != null) {
                    navbar.setPadding(
                            navbar.getPaddingLeft(),
                            navbar.getPaddingTop(),
                            navbar.getPaddingRight(),
                            insets.bottom);
                }
                return windowInsets;
            });
        }

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        toolbar = findViewById(R.id.toolbar);
        searchBar = findViewById(R.id.search_bar);
        searchView = findViewById(R.id.search_view);
        filterChipGroup = findViewById(R.id.filter_chip_group);
        filterByDateIcon = findViewById(R.id.filter_by_date);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        navHome = findViewById(R.id.navHome);
        navTransactions = findViewById(R.id.navTransactions);
        navEntry = findViewById(R.id.navEntry);

        transactionList = new ArrayList<>();
        filteredList = new ArrayList<>();

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        adapter = new TransactionAdapter(this, filteredList);
        recyclerView.setAdapter(adapter);
        loadTransactionsFromFile();
        filterTransactions("All");

        filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chip_all) {
                filterTransactions("All");
            } else if (checkedId == R.id.chip_income) {
                filterTransactions("Credit");
            } else if (checkedId == R.id.chip_expenses) {
                filterTransactions("Debit");
            }
        });

        searchView.getEditText().setOnEditorActionListener((v, actionId, event) -> {
            String query = searchView.getText().toString();
            searchBar.setText(query);
            searchTransaction(query);
            searchView.hide();
            return false;
        });

        searchView.addTransitionListener((searchView1, previousState, newState) -> {
            if (newState == com.google.android.material.search.SearchView.TransitionState.HIDDEN) {
                String query = searchView.getText().toString();
                searchBar.setText(query);
                searchTransaction(query);
            }
        });

        hideNavText(); // Hide text initially

        // Apply animation to the correct tab
        applyNavAnimation(findViewById(R.id.navTransactions));

        // Navigation Click Listeners
        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            vibrateDevice();
            finish();
        });

        findViewById(R.id.navTransactions).setOnClickListener(v -> {
            Toast.makeText(this, "Already on History Page", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.navEntry).setOnClickListener(v -> {
            startActivity(new Intent(this, EntryActivity.class));
            vibrateDevice();
            finish();
        });

        filterByDateIcon.setOnClickListener(v -> showFilterPopup(v));

        // Pull-to-Refresh Listener
        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshTransactionList();
        });

        //scroll to top button
        btnScrollToTop = findViewById(R.id.btnScrollToTop);
        // Show or hide button based on scroll position
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                // Show button only when scrolling down
                if (!recyclerView.canScrollVertically(-1)) {
                    if (btnScrollToTop.getVisibility() == View.VISIBLE) {
                        btnScrollToTop.hide();
                    }
                } else {
                    if (btnScrollToTop.getVisibility() == View.GONE) {
                        btnScrollToTop.show();
                    }
                }
            }
        });

        // Scroll to top smoothly and hide button
        btnScrollToTop.setOnClickListener(v -> {
            recyclerView.smoothScrollToPosition(0);

            // Delay hiding the button so the ripple effect completes
            new Handler().postDelayed(() -> btnScrollToTop.hide(), 200);
        });

        //Add new transaction button
        btnAddTransaction = findViewById(R.id.btnAddTransaction);
        btnAddTransaction.setOnClickListener(v -> {
            Log.d("DEBUG BUTTON", "Button Clicked!");  // Log to check if button is clickable
            Toast.makeText(TransactionActivity.this, "Redirecting to Entry Page...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(TransactionActivity.this, EntryActivity.class);
            startActivity(intent);
        });
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        vibrateDevice(); // Optional if you want feedback on back press
        finish();
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
    //nav animation
    private void hideNavText() {
        ((TextView) ((LinearLayout) findViewById(R.id.navHome)).getChildAt(1)).setVisibility(View.INVISIBLE);
        ((TextView) ((LinearLayout) findViewById(R.id.navTransactions)).getChildAt(1)).setVisibility(View.INVISIBLE);
        ((TextView) ((LinearLayout) findViewById(R.id.navEntry)).getChildAt(1)).setVisibility(View.INVISIBLE);
    }

    private void applyNavAnimation(LinearLayout selectedNavItem) {
        // Get the TextView inside the selected navigation item
        TextView textView = (TextView) ((LinearLayout) selectedNavItem).getChildAt(1);
        // Load the animation
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up_nav);

        // Apply the animation and make it visible
        textView.setVisibility(View.VISIBLE);
        textView.startAnimation(slideUp);
    }

    private void loadTransactionsFromFile() {
        transactionList.clear(); // Clear existing transactions

        TextView loadingText = findViewById(R.id.loadingText);
        LinearLayout textNoTransactions = findViewById(R.id.noTransactionsText);
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
                        obj.optString("transactionId", "N/A"),   // ✅ Corrected
                        obj.optString("paymentMethod", "Cash"),  // ✅ Corrected
                        obj.optString("textType", "Unknown"),    // ✅ Corrected
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

        LinearLayout textNoTransactions = findViewById(R.id.noTransactionsText);
        if (filteredList.isEmpty()) {
            textNoTransactions.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            // Move noTransactionsText to front
            textNoTransactions.bringToFront();
        } else {
            textNoTransactions.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            // Move SwipeRefreshLayout to front
            swipeRefreshLayout.bringToFront();
        }

        // Animate list items (keeping the subtle animation)
        recyclerView.post(() -> {
            for (int i = 0; i < recyclerView.getChildCount(); i++) {
                View child = recyclerView.getChildAt(i);
                if (child != null) {
                    child.setTranslationY(100f);
                    child.setAlpha(0f);

                    child.animate()
                            .translationY(0f)
                            .alpha(1f)
                            .setStartDelay(i * 50L)
                            .setDuration(400)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                }
            }
        });
    }
    private void sortTransactions(boolean isOldestToNewest) {
        if (!filteredList.isEmpty() && filteredList.get(0).getEntryId() != 0) { // Check if entryId exists
            if (isOldestToNewest) {
                Collections.sort(filteredList, (t1, t2) -> Long.compare(t1.getEntryId(), t2.getEntryId()));
            } else {
                Collections.sort(filteredList, (t1, t2) -> Long.compare(t2.getEntryId(), t1.getEntryId()));
            }
            adapter.notifyDataSetChanged();

            // Animation for recyclerView items after sorting
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

        } else {
            Log.e("TransactionDebug", "Sorting skipped: entryId missing.");
        }
    }
    private void searchTransaction(String query) {
        List<Transaction> searchResults = new ArrayList<>();

        for (Transaction transaction : transactionList) {
            // Ensure search only considers transactions that match the current filter type
            if (currentFilterType.equals("All") || transaction.getTransactionType().equals(currentFilterType)) {
                if (transaction.getReceiverName().toLowerCase().contains(query.toLowerCase()) ||
                        transaction.getUtr().toLowerCase().contains(query.toLowerCase())) {
                    searchResults.add(transaction);
                }
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
    public void vibrateDevice() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(30); // Deprecated in API 26+, but works for older versions
            }
        }
    }
}