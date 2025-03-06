package com.accounting.balancex;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomBarGraphView extends View {
    private Paint creditPaint, debitPaint, textPaint;
    private List<Integer> creditTransactions;
    private List<Integer> debitTransactions;

    public CustomBarGraphView(Context context) {
        super(context);
        init();
    }

    public CustomBarGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        creditPaint = new Paint();
        creditPaint.setColor(Color.parseColor("#1e90ff")); // Credit bar color
        creditPaint.setStyle(Paint.Style.FILL);
        creditPaint.setAntiAlias(true); // Smooth edges

        debitPaint = new Paint();
        debitPaint.setColor(Color.parseColor("#3700B3")); // Debit bar color
        debitPaint.setStyle(Paint.Style.FILL);
        debitPaint.setAntiAlias(true); // Smooth edges

        textPaint = new Paint();
        textPaint.setColor(Color.parseColor("#808080")); // Label color
        textPaint.setTextSize(30); // Label size
        textPaint.setAntiAlias(true); // Smooth edges

        // Sample transaction counts for 4 months
        Integer year_new_value4c = 0;
        Integer year_new_value3c = 0;
        Integer year_new_value2c = 0;
        Integer year_new_value1c = 0;
        creditTransactions = Arrays.asList(year_new_value1c, year_new_value2c, year_new_value3c, year_new_value4c);
        debitTransactions = Arrays.asList(3, 4, 5, 6);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        int numBars = creditTransactions.size(); // 4 months (default)
        int barGroupWidth = width / numBars; // Space allocated for each month
        int barWidth = barGroupWidth / 5; // Thin bars
        int spacing = 5; // Reduced spacing between Credit & Debit bars
        float cornerRadius = 12 * getResources().getDisplayMetrics().density; // 12dp rounded corners

        for (int i = 0; i < numBars; i++) {
            int groupLeft = i * barGroupWidth;

            // Draw Credit Bar
            int creditLeft = groupLeft + (barGroupWidth / 2) - (barWidth + spacing);
            int creditTop = height - (creditTransactions.get(i) * 20); // Multiply by a factor to adjust height
            RectF creditRect = new RectF(creditLeft, creditTop, creditLeft + barWidth, height);
            canvas.drawRoundRect(creditRect, cornerRadius, cornerRadius, creditPaint);

            // Draw Credit Label
            String creditLabel = String.valueOf(creditTransactions.get(i));
            float creditLabelX = creditLeft + (barWidth / 2) - (textPaint.measureText(creditLabel) / 2);
            float creditLabelY = creditTop - 10; // Slightly above the bar
            canvas.drawText(creditLabel, creditLabelX, creditLabelY, textPaint);

            // Draw Debit Bar
            int debitLeft = creditLeft + barWidth + spacing;
            int debitTop = height - (debitTransactions.get(i) * 20); // Multiply by a factor to adjust height
            RectF debitRect = new RectF(debitLeft, debitTop, debitLeft + barWidth, height);
            canvas.drawRoundRect(debitRect, cornerRadius, cornerRadius, debitPaint);

            // Draw Debit Label
            String debitLabel = String.valueOf(debitTransactions.get(i));
            float debitLabelX = debitLeft + (barWidth / 2) - (textPaint.measureText(debitLabel) / 2);
            float debitLabelY = debitTop - 10; // Slightly above the bar
            canvas.drawText(debitLabel, debitLabelX, debitLabelY, textPaint);
        }
    }

    public void updateData(List<Integer> creditCounts, List<Integer> debitCounts) {
        this.creditTransactions = creditCounts;
        this.debitTransactions = debitCounts;
        invalidate(); // Redraw graph
    }
    public void readTransactionFile() {
        try {
            File file = new File(Environment.getExternalStorageDirectory() + "/Documents/Accounting/transaction.json");
            InputStream is = new FileInputStream(file);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String json = new String(buffer, "UTF-8");
            JSONArray jsonArray = new JSONArray(json);

            List<String> entryIds = new ArrayList<>();
            List<String> textTypes = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                entryIds.add(jsonObject.getString("entryId"));
                textTypes.add(jsonObject.getString("textType"));
            }

            // Do something with the data (e.g., update graph or store it for later use)
            // For example: updateData(entryIds, textTypes); if you want to update graph data
            Map<String, Integer> creditYearMap = new HashMap<>();
            Map<String, Integer> debitYearMap = new HashMap<>();
            Map<String, Integer> creditMonthMap = new HashMap<>();
            Map<String, Integer> debitMonthMap = new HashMap<>();
            Map<String, Integer> creditDateMap = new HashMap<>();
            Map<String, Integer> debitDateMap = new HashMap<>();
            Map<String, Integer> creditWeekMap = new HashMap<>();
            Map<String, Integer> debitWeekMap = new HashMap<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String entryId = jsonObject.getString("entryId");
                String textType = jsonObject.getString("textType");

                // Parse the entryId
                String year = entryId.substring(0, 4);
                String month = entryId.substring(4, 6);
                String date = entryId.substring(6, 8);

                // Update maps
                if (textType.equals("Credit")) {
                    creditYearMap.put(year, creditYearMap.getOrDefault(year, 0) + 1);
                    creditMonthMap.put(year + "-" + month, creditMonthMap.getOrDefault(year + "-" + month, 0) + 1);
                    creditDateMap.put(year + "-" + month + "-" + date, creditDateMap.getOrDefault(year + "-" + month + "-" + date, 0) + 1);
                    // Add week logic here if needed
                } else if (textType.equals("Debit")) {
                    debitYearMap.put(year, debitYearMap.getOrDefault(year, 0) + 1);
                    debitMonthMap.put(year + "-" + month, debitMonthMap.getOrDefault(year + "-" + month, 0) + 1);
                    debitDateMap.put(year + "-" + month + "-" + date, debitDateMap.getOrDefault(year + "-" + month + "-" + date, 0) + 1);
                    // Add week logic here if needed
                }
            }

            // Extract recent 4 periods for years, months, dates, weeks
            List<Integer> recentCreditYears = new ArrayList<>(creditYearMap.values()).subList(Math.max(0, creditYearMap.size() - 4), creditYearMap.size());
            List<Integer> recentDebitYears = new ArrayList<>(debitYearMap.values()).subList(Math.max(0, debitYearMap.size() - 4), debitYearMap.size());

            List<Integer> recentCreditMonths = new ArrayList<>(creditMonthMap.values()).subList(Math.max(0, creditMonthMap.size() - 4), creditMonthMap.size());
            List<Integer> recentDebitMonths = new ArrayList<>(debitMonthMap.values()).subList(Math.max(0, debitMonthMap.size() - 4), debitMonthMap.size());

            List<Integer> recentCreditDates = new ArrayList<>(creditDateMap.values()).subList(Math.max(0, creditDateMap.size() - 4), creditDateMap.size());
            List<Integer> recentDebitDates = new ArrayList<>(debitDateMap.values()).subList(Math.max(0, debitDateMap.size() - 4), debitDateMap.size());

            // Similarly handle weeks if needed

            // Save recent periods counts (Example, you can adjust it to your use)
            int year_new_value1c = recentCreditYears.size() > 0 ? recentCreditYears.get(0) : 0;
            int year_new_value2c = recentCreditYears.size() > 1 ? recentCreditYears.get(1) : 0;
            int year_new_value3c = recentCreditYears.size() > 2 ? recentCreditYears.get(2) : 0;
            int year_new_value4c = recentCreditYears.size() > 3 ? recentCreditYears.get(3) : 0;

            int year_new_value1d = recentDebitYears.size() > 0 ? recentDebitYears.get(0) : 0;
            int year_new_value2d = recentDebitYears.size() > 1 ? recentDebitYears.get(1) : 0;
            int year_new_value3d = recentDebitYears.size() > 2 ? recentDebitYears.get(2) : 0;
            int year_new_value4d = recentDebitYears.size() > 3 ? recentDebitYears.get(3) : 0;

            // Similarly save for months, dates, weeks...

            // Do something with the data (e.g., update graph or store it for later use)
            // For example: updateData(entryIds, textTypes); if you want to update graph data
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}