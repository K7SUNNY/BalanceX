package com.accounting.balancex;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import android.content.SharedPreferences;

public class MainActivity extends AppCompatActivity {
    private TextView totalBalanceText, totalCreditText, totalDebitText;
    private FrameLayout graphContainer;
    private TextView months, days, weeks, years;
    private List<Transaction> transactions;
    private TextView textmonth1, textmonth2, textmonth3, textmonth4;
    private LinearLayout navHome, navTransactions, navEntry;
    private LinearLayout slideMenu;
    private ImageView lineChartButton, barChartButton;
    private CustomBarGraphView barGraphView;
    private CustomLineGraphView lineGraphView;
    private boolean isMenuOpen = false;
    private double totalBalance = 0, totalCredit = 0, totalDebit = 0;
    private List<String> transactionDates;
    private long appInstallTime;
    private List<RecentTransactionModel> recentTransactions;
    private RecentTransactionsAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        totalBalanceText = findViewById(R.id.textTotalBalance);
        totalCreditText = findViewById(R.id.textNetCredit);
        totalDebitText = findViewById(R.id.textNetDebit);
        graphContainer = findViewById(R.id.graphContainer);

        navHome = findViewById(R.id.navHome);
        navTransactions = findViewById(R.id.navTransactions);
        navEntry = findViewById(R.id.navEntry);

        loadTransactionData();

        navTransactions.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, TransactionActivity.class)));
        navEntry.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, EntryActivity.class)));

        slideMenu = findViewById(R.id.slideMenu);
        ImageView menuButton = findViewById(R.id.imageslidemenu_mainpage);
        menuButton.setOnClickListener(v -> { if (isMenuOpen) closeMenu(); else openMenu(); });
        findViewById(R.id.rootLayout).setOnClickListener(v -> { if (isMenuOpen) closeMenu(); });

        ImageView notificationIcon = findViewById(R.id.imagenotification);
        notificationIcon.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, NotificationActivity.class)));

        graphContainer = findViewById(R.id.graphContainer);
        lineChartButton = findViewById(R.id.linechart);
        barChartButton = findViewById(R.id.imageViewbarchart);

        barGraphView = new CustomBarGraphView(this);
        lineGraphView = new CustomLineGraphView(this);
        graphContainer.removeAllViews();
        graphContainer.addView(barGraphView);
        setSelectedGraph(barChartButton, lineChartButton);

        barChartButton.setOnClickListener(v -> switchGraphView(barGraphView, barChartButton, lineChartButton));
        lineChartButton.setOnClickListener(v -> switchGraphView(lineGraphView, lineChartButton, barChartButton));

        months = findViewById(R.id.months);
        days = findViewById(R.id.days);
        weeks = findViewById(R.id.weeks);
        years = findViewById(R.id.years);

        textmonth1 = findViewById(R.id.textmonth1);
        textmonth2 = findViewById(R.id.textmonth2);
        textmonth3 = findViewById(R.id.textmonth3);
        textmonth4 = findViewById(R.id.textmonth4);

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        if (!prefs.contains("install_time")) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong("install_time", System.currentTimeMillis());
            editor.apply();
        }
        appInstallTime = prefs.getLong("install_time", System.currentTimeMillis());

        months.setOnClickListener(view -> updateTimelineSelection(months, "months"));
        days.setOnClickListener(view -> updateTimelineSelection(days, "days"));
        weeks.setOnClickListener(view -> updateTimelineSelection(weeks, "weeks"));
        years.setOnClickListener(view -> updateTimelineSelection(years, "years"));
        updateTimelineSelection(months, "months");

        // Initialize RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recyclerViewRecentTransactions);
        recyclerView.setLayoutManager(new LinearLayoutManager(this)); // Set layout manager
        recentTransactions = new ArrayList<>();
        adapter = new RecentTransactionsAdapter(this, recentTransactions);
        recyclerView.setAdapter(adapter);

        // Load transactions from storage
        loadRecentTransactions();
    }

    private void loadTransactionData() {
        try {
            File file = new File("/storage/emulated/0/Documents/Accounting/transactions.json");
            if (!file.exists()) return;

            FileReader reader = new FileReader(file);
            char[] buffer = new char[(int) file.length()];
            reader.read(buffer);
            reader.close();

            JSONArray transactionsArray = new JSONArray(new String(buffer));
            totalBalance = totalCredit = totalDebit = 0;
            Set<String> dateSet = new HashSet<>();

            for (int i = 0; i < transactionsArray.length(); i++) {
                JSONObject transaction = transactionsArray.getJSONObject(i);
                double amount = transaction.getDouble("amount");
                String type = transaction.getString("textType");
                String date = transaction.getString("date");

                dateSet.add(date);

                if (type.equalsIgnoreCase("credit")) { totalCredit += amount; totalBalance += amount; }
                else if (type.equalsIgnoreCase("debit")) { totalDebit += amount; totalBalance -= amount; }
            }

            totalBalanceText.setText("₹" + totalBalance);
            totalCreditText.setText("₹" + totalCredit);
            totalDebitText.setText("₹" + totalDebit);

            transactionDates = new ArrayList<>(dateSet);
            Collections.sort(transactionDates, Collections.reverseOrder());
        } catch (IOException | org.json.JSONException e) { e.printStackTrace(); }
    }
    private void openMenu() {
        int menuWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.45);
        slideMenu.getLayoutParams().width = menuWidth;
        slideMenu.requestLayout();
        slideMenu.setVisibility(View.VISIBLE);
        TranslateAnimation openAnimation = new TranslateAnimation(-menuWidth, 0, 0, 0);
        openAnimation.setDuration(300);
        slideMenu.startAnimation(openAnimation);
        isMenuOpen = true;
    }

    private void closeMenu() {
        int menuWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.45);
        TranslateAnimation closeAnimation = new TranslateAnimation(0, -menuWidth, 0, 0);
        closeAnimation.setDuration(300);
        slideMenu.startAnimation(closeAnimation);
        slideMenu.setVisibility(View.GONE);
        isMenuOpen = false;
    }

    private void switchGraphView(View newGraph, ImageView selectedButton, ImageView unselectedButton) {
        graphContainer.removeAllViews();
        graphContainer.addView(newGraph);
        newGraph.invalidate();
        setSelectedGraph(selectedButton, unselectedButton);
    }

    private void setSelectedGraph(ImageView selected, ImageView unselected) {
        selected.setBackgroundResource(R.drawable.rounded_corner_selected_timeline);
        unselected.setBackgroundResource(R.drawable.rounded_corner_unselected);
    }


    private void updateTimelineSelection(TextView selectedView, String timelineType) {
        months.setBackgroundResource(R.drawable.rounded_corner_unselected);
        days.setBackgroundResource(R.drawable.rounded_corner_unselected);
        weeks.setBackgroundResource(R.drawable.rounded_corner_unselected);
        years.setBackgroundResource(R.drawable.rounded_corner_unselected);

        selectedView.setBackgroundResource(R.drawable.rounded_corner_selected_timeline);
        List<String> labels = getTimelineLabels(timelineType);

        textmonth1.setText(labels.size() > 0 ? labels.get(0) : "");
        textmonth2.setText(labels.size() > 1 ? labels.get(1) : "");
        textmonth3.setText(labels.size() > 2 ? labels.get(2) : "");
        textmonth4.setText(labels.size() > 3 ? labels.get(3) : "");
    }
    private List<String> getTimelineLabels(String type) {
        List<String> labels = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat formatter;

        switch (type) {
            case "months":
                formatter = new SimpleDateFormat("MMM", Locale.getDefault());
                for (int i = 3; i >= 0; i--) {
                    calendar.add(Calendar.MONTH, -i);
                    labels.add(formatter.format(calendar.getTime()));
                    calendar.add(Calendar.MONTH, i);
                }
                break;

            case "days":
                formatter = new SimpleDateFormat("E", Locale.getDefault());
                for (int i = 3; i >= 0; i--) {
                    calendar.add(Calendar.DAY_OF_MONTH, -i);
                    labels.add(formatter.format(calendar.getTime()));
                    calendar.add(Calendar.DAY_OF_MONTH, i);
                }
                break;

            case "years":
                formatter = new SimpleDateFormat("yyyy", Locale.getDefault());
                for (int i = 3; i >= 0; i--) {
                    calendar.add(Calendar.YEAR, -i);
                    labels.add(formatter.format(calendar.getTime()));
                    calendar.add(Calendar.YEAR, i);
                }
                break;

            case "weeks":
                long weeksSinceInstall = (System.currentTimeMillis() - appInstallTime) / (7 * 24 * 60 * 60 * 1000);
                long startWeek = Math.max(1, weeksSinceInstall - 3);
                for (long i = startWeek; i <= weeksSinceInstall; i++) {
                    labels.add("Week " + i);
                }
                break;
        }

        while (labels.size() < 4) {
            labels.add("-");
        }

        return labels;
    }
    private void loadRecentTransactions() {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "Accounting/transactions.json");

        if (!file.exists()) {
            Log.e("LoadTransactions", "Transaction file not found!");
            return;
        }

        try {
            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            reader.close();
            fis.close();

            Log.d("LoadTransactions", "JSON Read: " + sb.toString());

            JSONArray transactionsArray = new JSONArray(sb.toString());
            recentTransactions.clear();  // Clear old data before adding new

            for (int i = 0; i < transactionsArray.length(); i++) {
                JSONObject transaction = transactionsArray.getJSONObject(i);
                String receiver = transaction.getString("receiver");
                String date = transaction.getString("date");
                String amount = transaction.getString("amount");
                String type = transaction.getString("textType"); // credit/debit
                long entryId = transaction.getLong("entryId");  // Get entry ID as long

                recentTransactions.add(new RecentTransactionModel(receiver, date, amount, type, entryId));
            }

            // 🔹 SORT transactions by Entry ID in DESCENDING ORDER (latest first)
            Collections.sort(recentTransactions, (t1, t2) -> Long.compare(t2.getEntryId(), t1.getEntryId()));

            adapter.notifyDataSetChanged();  // Update RecyclerView after sorting and loading

        } catch (Exception e) {
            Log.e("LoadTransactions", "Error parsing JSON", e);
        }
    }
}