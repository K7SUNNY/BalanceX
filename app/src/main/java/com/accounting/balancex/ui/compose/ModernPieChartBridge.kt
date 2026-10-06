package com.accounting.balancex.ui.compose

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.accounting.balancex.data.db.DatabaseMigrator
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

class ModernPieChartBridge(
    private val context: Context,
    private val composeView: ComposeView
) {
    private var chartState by mutableStateOf<List<CategoryBreakdownItem>>(emptyList())

    // Palette strictly inspired by UI_referene/Cards_UI/Pie_Char_UI.webp
    private val modernPalette = listOf(
        Color(0xFF8B5CF6), // Purple / Violet
        Color(0xFF38BDF8), // Sky Blue
        Color(0xFF22C55E), // Emerald Green
        Color(0xFFF97316), // Coral Orange
        Color(0xFFA78BFA), // Lavender / Soft Violet
        Color(0xFFF59E0B), // Amber Yellow
        Color(0xFFEC4899), // Rose Pink
        Color(0xFF06B6D4), // Cyan
        Color(0xFF6366F1)  // Indigo
    )

    init {
        composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )

        composeView.setContent {
            ModernPieChart(items = chartState)
        }

        refresh()
    }

    /**
     * Reload category expense totals and update compose state
     */
    fun refresh() {
        val categoryTotals = LinkedHashMap<String, Float>()

        try {
            val file = DatabaseMigrator.findJsonFile(context)
                ?: File("/storage/emulated/0/Documents/Accounting/transactions.json")

            if (file.exists() && file.length() > 2) {
                val reader = BufferedReader(FileReader(file))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()

                val array = JSONArray(sb.toString().trim())
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val cat = obj.optString("category", "General")
                    val amtStr = obj.optString("amount", "0")
                    val amt = amtStr.replace("[^0-9.]".toRegex(), "").toFloatOrNull() ?: 0f

                    // Count expense / debit transactions primarily, or fallback to all
                    val type = obj.optString("textType", obj.optString("transactionType", "Debit"))
                    if (type.equals("Debit", ignoreCase = true) || !type.equals("Credit", ignoreCase = true)) {
                        categoryTotals[cat] = (categoryTotals[cat] ?: 0f) + amt
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Sort descending by amount
        val sortedList = categoryTotals.entries
            .filter { it.value > 0f }
            .sortedByDescending { it.value }

        val items = sortedList.mapIndexed { index, entry ->
            CategoryBreakdownItem(
                category = entry.key,
                amount = entry.value,
                color = modernPalette[index % modernPalette.size]
            )
        }

        chartState = items
    }
}
