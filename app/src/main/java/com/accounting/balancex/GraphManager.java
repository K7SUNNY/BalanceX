package com.accounting.balancex;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;


public class GraphManager {
    private Context context;
    private BarChart barChart;
    private LineChart lineChart;
    private String selectedTimeline = "M";
    private static final String FILE_PATH = "/storage/emulated/0/Documents/Accounting/transactions.json";

    public GraphManager(Context context, BarChart barChart, LineChart lineChart) {
        this.context = context;
        this.barChart = barChart;
        this.lineChart = lineChart;
    }

    public void updateGraph(String timeline, String graphType) {
        Log.d("GraphManager", "updateGraph called with: Timeline = " + timeline + ", Graph Type = " + graphType);
        this.selectedTimeline = timeline;

        List<BarEntry> creditEntries = new ArrayList<>();
        List<BarEntry> debitEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        try {
            File file = new File(FILE_PATH);
            if (!file.exists()) {
                Log.e("GraphManager", "transactions.json not found");
                return;
            }

            BufferedReader br = new BufferedReader(new FileReader(file));
            StringBuilder jsonString = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                jsonString.append(line);
            }
            br.close();

            JSONArray transactions = new JSONArray(jsonString.toString());
            Log.d("GraphManager", "Transactions loaded: " + transactions.length());

            LinkedHashMap<Integer, Float> creditMap = new LinkedHashMap<>();
            LinkedHashMap<Integer, Float> debitMap = new LinkedHashMap<>();

            for (int i = 0; i < transactions.length(); i++) {
                JSONObject transaction = transactions.getJSONObject(i);
                String type = transaction.getString("textType");
                float amount = Float.parseFloat(transaction.getString("amount"));
                int period = extractPeriod(transaction.getString("date"), timeline);

                if (type.equalsIgnoreCase("Credit")) {
                    creditMap.put(period, creditMap.getOrDefault(period, 0f) + amount);
                } else if (type.equalsIgnoreCase("Debit")) {
                    debitMap.put(period, debitMap.getOrDefault(period, 0f) + amount);
                }
            }

            int index = 0;
            for (int period : creditMap.keySet()) {
                creditEntries.add(new BarEntry(index, creditMap.get(period)));
                labels.add(formatPeriodLabel(period, timeline));
                index++;
            }

            index = 0;
            for (int period : debitMap.keySet()) {
                debitEntries.add(new BarEntry(index, debitMap.get(period)));
                index++;
            }

            Log.d("GraphManager", "Credit Entries: " + creditEntries.size() + ", Debit Entries: " + debitEntries.size());
            setXAxisLabels(labels);

        } catch (Exception e) {
            Log.e("GraphManager", "Error reading transactions: " + e.getMessage());
        }

        if (graphType.equals("bar")) {
            barChart.setVisibility(View.VISIBLE);
            lineChart.setVisibility(View.GONE);  // Hide line chart
            showBarGraph(creditEntries, debitEntries);
        } else {
            barChart.setVisibility(View.GONE);  // Hide bar chart
            lineChart.setVisibility(View.VISIBLE);
            List<Entry> creditLineEntries = new ArrayList<>();
            List<Entry> debitLineEntries = new ArrayList<>();

            for (BarEntry entry : creditEntries) {
                creditLineEntries.add(new Entry(entry.getX(), entry.getY()));
            }
            for (BarEntry entry : debitEntries) {
                debitLineEntries.add(new Entry(entry.getX(), entry.getY()));
            }

            showLineChart(creditLineEntries, debitLineEntries);
        }
    }

    private void setXAxisLabels(List<String> labels) {
        barChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                return (index >= 0 && index < labels.size()) ? labels.get(index) : "";
            }
        });
        barChart.getXAxis().setGranularity(1f);
        barChart.getXAxis().setLabelRotationAngle(-45);
        barChart.invalidate();
    }

    private int extractPeriod(String date, String timeline) {
        String[] parts = date.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        int day = Integer.parseInt(parts[2]);

        if (timeline.equals("M")) {
            return (year * 100) + month;
        } else if (timeline.equals("Y")) {
            return year;
        } else if (timeline.equals("D")) {
            return (year * 10000) + (month * 100) + day;
        } else if (timeline.equals("W")) {
            return getWeekOfYear(year, month, day);
        }
        return month;
    }

    private String formatPeriodLabel(int period, String timeline) {
        if (timeline.equals("M")) {
            return period % 100 + "/" + (period / 100);
        } else if (timeline.equals("Y")) {
            return String.valueOf(period);
        } else if (timeline.equals("W")) {
            return "Week " + (period % 100) + ", " + (period / 100);
        }
        return "?";
    }
    private int getWeekOfYear(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, day); // Month is 0-based in Calendar
        return calendar.get(Calendar.WEEK_OF_YEAR);
    }
    private List<String> getMonths() {
        return Arrays.asList("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");
    }

    private void showBarGraph(List<BarEntry> creditEntries, List<BarEntry> debitEntries) {
        Log.d("GraphManager", "Displaying Bar Chart");

        if (creditEntries.isEmpty() && debitEntries.isEmpty()) {
            Log.e("GraphManager", "No data to show in the bar chart.");
            return;
        }

        // Data sets for Credit and Debit
        BarDataSet creditSet = new BarDataSet(creditEntries, "Credit");
        creditSet.setColor(Color.parseColor("#1E90FF")); // Dodger Blue for Credit
        creditSet.setValueTextColor(Color.WHITE);

        BarDataSet debitSet = new BarDataSet(debitEntries, "Debit");
        debitSet.setColor(Color.parseColor("#3700B3")); // Dark Purple for Debit
        debitSet.setValueTextColor(Color.WHITE);

        // Group the bars
        float groupSpace = 0.2f; // Space between groups
        float barSpace = 0.05f; // Space between bars
        float barWidth = 0.35f; // Width of each bar

        BarData barData = new BarData(creditSet, debitSet);
        barData.setBarWidth(barWidth); // Set width
        barChart.setData(barData);

        // X-Axis Customization
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(getMonths())); // Set custom labels
        xAxis.setGranularity(1f);
        xAxis.setCenterAxisLabels(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.BLACK);

        // Y-Axis Customization
        barChart.getAxisLeft().setDrawGridLines(true);
        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setEnabled(false); // Hide legend if not needed

        // Group the bars
        barChart.groupBars(0f, groupSpace, barSpace);
        barChart.setFitBars(true);
        barChart.animateY(1000, Easing.EaseInOutQuad); // Smooth animation

        // Refresh the chart
        barChart.invalidate();
    }


    private void showLineChart(List<Entry> creditEntries, List<Entry> debitEntries) {
        Log.d("GraphManager", "Displaying Line Chart with " + creditEntries.size() + " credit entries and " + debitEntries.size() + " debit entries");

        if (creditEntries.isEmpty() && debitEntries.isEmpty()) {
            Log.e("GraphManager", "No data to show in the line chart.");
            return;
        }

        // Hide Bar Chart when switching to Line Chart
        barChart.setVisibility(View.GONE);
        lineChart.setVisibility(View.VISIBLE);

        // Create Line DataSets
        LineDataSet creditSet = new LineDataSet(creditEntries, "Credit");
        creditSet.setColor(Color.parseColor("#1E90FF")); // Dodger Blue
        creditSet.setCircleColor(Color.parseColor("#1E90FF"));
        creditSet.setLineWidth(2f);
        creditSet.setCircleRadius(5f);
        creditSet.setDrawValues(true);
        creditSet.setValueTextColor(Color.BLACK);
        creditSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Smooth curve

        LineDataSet debitSet = new LineDataSet(debitEntries, "Debit");
        debitSet.setColor(Color.parseColor("#3700B3")); // Deep Purple
        debitSet.setCircleColor(Color.parseColor("#3700B3"));
        debitSet.setLineWidth(2f);
        debitSet.setCircleRadius(5f);
        debitSet.setDrawValues(true);
        debitSet.setValueTextColor(Color.BLACK);
        debitSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Smooth curve

        // Apply dataset to LineData
        LineData lineData = new LineData(creditSet, debitSet);
        lineChart.setData(lineData);

        // X-Axis Customization (Add Labels like Bar Chart)
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(getMonths())); // Same labels as Bar Chart
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setLabelRotationAngle(-45);

        // Y-Axis Customization
        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getLegend().setEnabled(true);

        // Apply animations
        lineChart.animateX(1000, Easing.EaseInOutQuad);
        lineChart.invalidate(); // 🔥 Refresh the chart
    }

}
