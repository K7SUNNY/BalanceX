package com.accounting.balancex;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {
    private TextView textTotalBalance, textNetCredit, textNetDebit;
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
    private ImageView menuButton;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_drawer);

        hideNavText(); // Hide text initially

        // Apply animation to the correct tab
        applyNavAnimation(findViewById(R.id.navHome));

        // Navigation Click Listeners
        findViewById(R.id.navHome).setOnClickListener(v -> {
            Toast.makeText(this, "Already on Home Page", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.navTransactions).setOnClickListener(v -> {
            startActivity(new Intent(this, TransactionActivity.class));
            vibrateDevice();
            finish();
        });

        findViewById(R.id.navEntry).setOnClickListener(v -> {
            startActivity(new Intent(this, EntryActivity.class));
            vibrateDevice();
            finish();
        });

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

        drawerLayout = findViewById(R.id.drawerlayout);
        menuButton = findViewById(R.id.menu_button);
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.open();
            }
        });
        navigationView = findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.my_profile) {
                    Log.d("navigationView", "onNavigationItemSelected: my profile");
                } else if (itemId == R.id.about_BalanceX) {
                    Log.d("navigationView", "onNavigationItemSelected: about balanceX");
                }
                return false;
            }
        });
    }
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
    private void updateTimelineSelection(TextView selected, TextView... others) {
        selected.setBackgroundResource(R.drawable.selected_title);
        selected.setTextColor(Color.BLACK);

        for (TextView other : others) {
            other.setBackgroundColor(Color.TRANSPARENT);
            other.setTextColor(Color.BLACK);
        }
    }
    private void updateGraphTypeSelection(ImageView selected, ImageView other) {
        selected.setBackgroundResource(R.drawable.selected_title);
        other.setBackgroundColor(Color.TRANSPARENT);
    }


    private void loadBalanceData() {
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

            textTotalBalance.setText("₹" + totalBalance);
            textNetCredit.setText("₹" + totalCredit);
            textNetDebit.setText("₹" + totalDebit);

            transactionDates = new ArrayList<>(dateSet);
            Collections.sort(transactionDates, Collections.reverseOrder());
        } catch (IOException | org.json.JSONException e) { e.printStackTrace(); }
        Log.e("LoadTransactions", "Transaction file not found!");
    }
    private void loadTransactionsFromStorage() {
        File file = new File("/storage/emulated/0/Documents/Accounting/transactions.json");

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

            JSONArray transactionsArray = new JSONArray(sb.toString());
            if (recentTransactions == null) {
                recentTransactions = new ArrayList<>(); // ✅ Initialize if null
            }
            recentTransactions.clear();

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
            Log.d("Transactions", "Loaded Transactions: " + recentTransactions.size());


        } catch (Exception e) {
            Log.e("LoadTransactions", "Error parsing JSON", e);
        }
    }

    private void startAutoScroll() {
        if (scrollRunnable != null) {
            handler.removeCallbacks(scrollRunnable); // Prevent duplicate runnables
        }

        scrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (transactionDates == null || transactionDates.isEmpty()) return; // Prevent crashes

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
        startAutoScroll(); // Resume auto-scroll
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
            if (!file.exists()) return pieDataList; // Return empty if no data

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

        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);
        pieChart.invalidate(); // Refresh chart
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
