package com.k7sunny.balancex.ui.compose

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.k7sunny.balancex.data.db.DatabaseMigrator
import org.json.JSONArray
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
     * Reload category expense totals from Room and update compose state
     */
    fun refresh() {
        val db = com.k7sunny.balancex.data.db.AppDatabase.getDatabase(context)
        com.k7sunny.balancex.data.db.AppDatabase.databaseWriteExecutor.execute {
            val summaries = db.transactionDao().getCategoryExpensesAllTime()
            val items = summaries
                .filter { it.totalAmountMinor > 0 }
                .mapIndexed { index, summary ->
                    CategoryBreakdownItem(
                        category = summary.category ?: "General",
                        amount = summary.totalAmountMinor / 100f,
                        color = modernPalette[index % modernPalette.size]
                    )
                }

            val updateAction = Runnable {
                chartState = items
            }
            if (context is android.app.Activity) {
                context.runOnUiThread(updateAction)
            } else {
                updateAction.run()
            }
        }
    }
}
