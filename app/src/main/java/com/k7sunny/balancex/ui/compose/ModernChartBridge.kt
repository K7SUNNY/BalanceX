package com.k7sunny.balancex.ui.compose

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.k7sunny.balancex.Transaction
import com.k7sunny.balancex.data.db.DatabaseMigrator
import java.text.SimpleDateFormat
import java.util.*

class ModernChartBridge(
    private val context: Context,
    private val composeView: ComposeView
) {
    private var currentTimeline: String = "M"
    private var currentGraphType: String = "line"

    private var chartState by mutableStateOf(ChartUiState())

    private data class ChartUiState(
        val dataPoints: List<FinancialDataPoint> = emptyList(),
        val timeline: String = "M",
        val graphType: String = "line"
    )

    init {
        composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )

        composeView.setContent {
            ModernFinancialChart(
                dataPoints = chartState.dataPoints,
                timeline = chartState.timeline,
                graphType = chartState.graphType
            )
        }
    }

    /**
     * Java-friendly method called by MainActivity
     */
    fun updateGraph(timeline: String, graphType: String) {
        this.currentTimeline = timeline
        this.currentGraphType = graphType
        refresh()
    }

    /**
     * Reload transactions from disk/database and update compose state
     */
    fun refresh() {
        val transactions = DatabaseMigrator.loadTransactionsFromJson(context)
        val points = processTransactions(transactions, currentTimeline)

        chartState = ChartUiState(
            dataPoints = points,
            timeline = currentTimeline,
            graphType = currentGraphType
        )
    }

    private fun processTransactions(
        transactions: List<Transaction>,
        timeline: String
    ): List<FinancialDataPoint> {
        if (transactions.isEmpty()) {
            return emptyList()
        }

        val creditMap = LinkedHashMap<Int, Float>()
        val debitMap = LinkedHashMap<Int, Float>()
        val labelMap = LinkedHashMap<Int, String>()

        // 1. Determine anchor date (current time, or latest transaction if in future or if all data is historical)
        val anchorCal = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsedDates = transactions.mapNotNull { txn ->
            txn.date?.let {
                try { dateFormat.parse(it) } catch (e: Exception) { null }
            }
        }
        val latestTxnDate = parsedDates.maxOrNull()

        if (latestTxnDate != null) {
            val latestTxnCal = Calendar.getInstance().apply { time = latestTxnDate }
            if (latestTxnCal.after(anchorCal)) {
                anchorCal.time = latestTxnDate
            } else {
                // Check if any transaction falls within the default recent window ending today.
                // If not (e.g., imported historical dataset), anchor to latest transaction so data is visible.
                val diffDays = (anchorCal.timeInMillis - latestTxnDate.time) / (1000 * 60 * 60 * 24)
                val windowDays = when (timeline) {
                    "D" -> 7
                    "W" -> 56 // 8 weeks
                    "M" -> 210 // 7 months
                    else -> 365
                }
                if (diffDays > windowDays) {
                    anchorCal.time = latestTxnDate
                }
            }
        }

        // 2. Initialize strictly 6 to 8 periods (never overcrowded)
        when (timeline) {
            "M" -> {
                // Show last 6 months (half-year window)
                val monthNames = arrayOf(
                    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                )
                for (i in 5 downTo 0) {
                    val c = Calendar.getInstance().apply { time = anchorCal.time }
                    c.add(Calendar.MONTH, -i)
                    val y = c.get(Calendar.YEAR)
                    val m = c.get(Calendar.MONTH) + 1
                    val key = (y * 100) + m
                    creditMap[key] = 0f
                    debitMap[key] = 0f
                    labelMap[key] = monthNames[m - 1]
                }
            }
            "D" -> {
                // Show last 7 days (1 full week)
                val dayFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
                for (i in 6 downTo 0) {
                    val c = Calendar.getInstance().apply { time = anchorCal.time }
                    c.add(Calendar.DAY_OF_YEAR, -i)
                    val y = c.get(Calendar.YEAR)
                    val m = c.get(Calendar.MONTH) + 1
                    val d = c.get(Calendar.DAY_OF_MONTH)
                    val key = (y * 10000) + (m * 100) + d
                    creditMap[key] = 0f
                    debitMap[key] = 0f
                    labelMap[key] = dayFormat.format(c.time)
                }
            }
            "W" -> {
                // Show last 7 weeks (e.g. W35..W41)
                for (i in 6 downTo 0) {
                    val c = Calendar.getInstance().apply { time = anchorCal.time }
                    c.add(Calendar.WEEK_OF_YEAR, -i)
                    val y = c.get(Calendar.YEAR)
                    val w = c.get(Calendar.WEEK_OF_YEAR)
                    val key = (y * 100) + w
                    creditMap[key] = 0f
                    debitMap[key] = 0f
                    labelMap[key] = "W$w"
                }
            }
            "Y" -> {
                // Show only years with data up to current year, capped at last 6-7 years
                val currentYear = anchorCal.get(Calendar.YEAR)
                val txnYears = transactions.mapNotNull { txn ->
                    txn.date?.split("-")?.firstOrNull()?.toIntOrNull()
                }
                val distinctYears = (txnYears + currentYear).distinct().sorted()
                val displayedYears = distinctYears.takeLast(7)
                for (y in displayedYears) {
                    creditMap[y] = 0f
                    debitMap[y] = 0f
                    labelMap[y] = y.toString()
                }
            }
        }

        // 3. Aggregate transactions strictly into the displayed periods
        for (txn in transactions) {
            val dateStr = txn.date ?: continue
            val type = txn.transactionType ?: "Debit"
            val amtStr = txn.amount?.replace("[^0-9.]".toRegex(), "") ?: "0"
            val amt = amtStr.toFloatOrNull() ?: 0f
            if (amt <= 0f) continue

            val periodKey = extractPeriod(dateStr, timeline) ?: continue

            // Only accumulate into the displayed periods to prevent cramping
            if (labelMap.containsKey(periodKey)) {
                if (type.equals("Credit", ignoreCase = true)) {
                    creditMap[periodKey] = (creditMap[periodKey] ?: 0f) + amt
                } else {
                    debitMap[periodKey] = (debitMap[periodKey] ?: 0f) + amt
                }
            }
        }

        val sortedKeys = labelMap.keys.sorted()
        return sortedKeys.map { key ->
            FinancialDataPoint(
                label = labelMap[key] ?: key.toString(),
                periodKey = key,
                income = creditMap[key] ?: 0f,
                expense = debitMap[key] ?: 0f
            )
        }
    }

    private fun extractPeriod(date: String, timeline: String): Int? {
        val parts = date.split("-")
        if (parts.size < 3) return null

        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].toIntOrNull() ?: return null

        return when (timeline) {
            "M" -> (year * 100) + month
            "Y" -> year
            "D" -> (year * 10000) + (month * 100) + day
            "W" -> {
                val cal = Calendar.getInstance()
                cal.set(year, month - 1, day)
                val w = cal.get(Calendar.WEEK_OF_YEAR)
                (year * 100) + w
            }
            else -> (year * 100) + month
        }
    }

    private fun formatPeriodLabel(period: Int, timeline: String): String {
        return when (timeline) {
            "M" -> {
                val m = period % 100
                val monthNames = arrayOf(
                    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                )
                if (m in 1..12) monthNames[m - 1] else "$m"
            }
            "Y" -> period.toString()
            "D" -> {
                val d = period % 100
                val m = (period / 100) % 100
                "$d/$m"
            }
            "W" -> "W${period % 100}"
            else -> period.toString()
        }
    }
}
