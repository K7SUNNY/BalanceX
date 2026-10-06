package com.accounting.balancex.ui.compose

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max

data class FinancialDataPoint(
    val label: String,
    val periodKey: Int,
    val income: Float,
    val expense: Float
)

@Composable
fun ModernFinancialChart(
    dataPoints: List<FinancialDataPoint>,
    timeline: String,
    graphType: String,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    // Blend seamlessly with parent card background
    val textColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val textMutedColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val gridLineColor = if (isDark) Color(0x18FFFFFF) else Color(0x10000000)
    val tooltipBgColor = if (isDark) Color(0xFF0F172A) else Color(0xFF1E293B)

    // Vibrant modern finance colors
    val incomeColor = Color(0xFF10B981) // Emerald Green
    val incomeGradientBottom = Color(0xFF059669)
    val expenseColor = Color(0xFFF43F5E) // Coral Rose Red
    val expenseGradientBottom = Color(0xFFE11D48)

    var selectedIndex by remember(dataPoints, timeline, graphType) { mutableStateOf<Int?>(null) }

    var animTrigger by remember { mutableStateOf(0f) }
    LaunchedEffect(dataPoints, timeline, graphType) {
        animTrigger = 0f
        animTrigger = 1f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = animTrigger,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "chartAnim"
    )

    val totalIncome = remember(dataPoints) { dataPoints.sumOf { it.income.toDouble() } }
    val totalExpense = remember(dataPoints) { dataPoints.sumOf { it.expense.toDouble() } }
    val netBalance = totalIncome - totalExpense

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build()).apply {
            maximumFractionDigits = 0
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(horizontal = 4.dp)
    ) {
        // Summary & Legend Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val activePoint = selectedIndex?.let { idx ->
                if (idx in dataPoints.indices) dataPoints[idx] else null
            }

            if (activePoint != null) {
                // Interactive selected point pill
                Surface(
                    color = tooltipBgColor.copy(alpha = if (isDark) 0.9f else 0.85f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = activePoint.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "+${currencyFormatter.format(activePoint.income)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = incomeColor
                        )
                        Text(
                            text = "-${currencyFormatter.format(activePoint.expense)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = expenseColor
                        )
                    }
                }
            } else {
                // Default Net summary
                val netColor = if (netBalance >= 0) incomeColor else expenseColor
                val netPrefix = if (netBalance >= 0) "+" else ""
                Surface(
                    color = netColor.copy(alpha = if (isDark) 0.20f else 0.12f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Net $netPrefix${currencyFormatter.format(netBalance)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = netColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Legend indicators
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(incomeColor, CircleShape)
                    )
                    Text(
                        text = "Income",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = textMutedColor
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(expenseColor, CircleShape)
                    )
                    Text(
                        text = "Expense",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = textMutedColor
                    )
                }
            }
        }

        // Chart Canvas Area
        val isNoData = dataPoints.isEmpty() || (totalIncome == 0.0 && totalExpense == 0.0)
        val isLineChartInsufficient = !isNoData && graphType.equals("line", ignoreCase = true) && dataPoints.size < 2

        if (isNoData) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(185.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(textMutedColor.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "📊", fontSize = 17.sp)
                    }
                    Text(
                        text = "No transactions recorded for this period",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                    Text(
                        text = "Switch timeline filter or add new transactions",
                        fontSize = 11.sp,
                        color = textMutedColor
                    )
                }
            }
        } else if (isLineChartInsufficient) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(185.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(incomeColor.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "📈", fontSize = 17.sp)
                    }
                    Text(
                        text = "Not enough data to display line trend",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                    Text(
                        text = "Line charts require at least 2 periods to plot trends.\nSwitch to Bar view above to view this period's total.",
                        fontSize = 11.sp,
                        color = textMutedColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        } else {
            val maxAmount = remember(dataPoints) {
                val peak = dataPoints.maxOfOrNull { max(it.income, it.expense) } ?: 100f
                if (peak <= 0f) 100f else peak * 1.18f
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .pointerInput(dataPoints, graphType) {
                        detectTapGestures(
                            onTap = { offset ->
                                val count = dataPoints.size
                                if (count > 0) {
                                    val index = if (graphType.equals("line", ignoreCase = true) && count > 1) {
                                        val slot = size.width / (count - 1)
                                        ((offset.x + slot / 2f) / slot).toInt().coerceIn(0, count - 1)
                                    } else {
                                        val slot = size.width / count
                                        (offset.x / slot).toInt().coerceIn(0, count - 1)
                                    }
                                    selectedIndex = if (selectedIndex == index) null else index
                                }
                            }
                        )
                    }
                    .pointerInput(dataPoints, graphType) {
                        detectDragGestures(
                            onDrag = { change, _ ->
                                change.consume()
                                val count = dataPoints.size
                                if (count > 0) {
                                    val index = if (graphType.equals("line", ignoreCase = true) && count > 1) {
                                        val slot = size.width / (count - 1)
                                        ((change.position.x + slot / 2f) / slot).toInt().coerceIn(0, count - 1)
                                    } else {
                                        val slot = size.width / count
                                        (change.position.x / slot).toInt().coerceIn(0, count - 1)
                                    }
                                    selectedIndex = index
                                }
                            },
                            onDragEnd = {
                                // keep selected index visible
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val bottomPadding = 24.dp.toPx()
                    val chartHeight = height - bottomPadding

                    // Draw subtle dashed horizontal grid lines
                    val gridLines = 3
                    for (i in 0..gridLines) {
                        val y = chartHeight * (i.toFloat() / gridLines)
                        drawLine(
                            color = gridLineColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                        )
                    }

                    if (graphType.equals("bar", ignoreCase = true)) {
                        drawModernBarChart(
                            dataPoints = dataPoints,
                            maxAmount = maxAmount,
                            progress = animatedProgress,
                            selectedIndex = selectedIndex,
                            chartHeight = chartHeight,
                            width = width,
                            incomeColor = incomeColor,
                            incomeGradientBottom = incomeGradientBottom,
                            expenseColor = expenseColor,
                            expenseGradientBottom = expenseGradientBottom,
                            labelColor = textMutedColor,
                            isDark = isDark
                        )
                    } else {
                        drawModernLineChart(
                            dataPoints = dataPoints,
                            maxAmount = maxAmount,
                            progress = animatedProgress,
                            selectedIndex = selectedIndex,
                            chartHeight = chartHeight,
                            width = width,
                            incomeColor = incomeColor,
                            expenseColor = expenseColor,
                            gridLineColor = gridLineColor,
                            labelColor = textMutedColor,
                            isDark = isDark
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawModernBarChart(
    dataPoints: List<FinancialDataPoint>,
    maxAmount: Float,
    progress: Float,
    selectedIndex: Int?,
    chartHeight: Float,
    width: Float,
    incomeColor: Color,
    incomeGradientBottom: Color,
    expenseColor: Color,
    expenseGradientBottom: Color,
    labelColor: Color,
    isDark: Boolean
) {
    val count = dataPoints.size
    if (count == 0) return

    val slotWidth = width / count

    // Generous, premium bar widths adapted to number of bars
    val maxBarWidth = when {
        count <= 3 -> 24.dp.toPx()
        count <= 6 -> 18.dp.toPx()
        else -> 12.dp.toPx()
    }
    val barWidth = (slotWidth * 0.30f).coerceIn(8.dp.toPx(), maxBarWidth)
    val barSpacing = 3.dp.toPx()
    val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
    val minBarHeight = 3.dp.toPx()

    dataPoints.forEachIndexed { i, point ->
        val centerX = (i * slotWidth) + (slotWidth / 2f)
        val isSelected = selectedIndex == null || selectedIndex == i
        val alpha = if (isSelected) 1f else 0.35f

        // Income Bar (Left of center)
        val incomeCalculatedHeight = (point.income / maxAmount) * chartHeight * progress
        val incomeHeight = if (point.income > 0) incomeCalculatedHeight.coerceAtLeast(minBarHeight) else minBarHeight
        val incomeLeft = centerX - barWidth - (barSpacing / 2f)
        val incomeTop = chartHeight - incomeHeight

        val actualIncomeAlpha = if (point.income > 0) alpha else 0.15f
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    incomeColor.copy(alpha = actualIncomeAlpha),
                    incomeGradientBottom.copy(alpha = actualIncomeAlpha)
                ),
                startY = incomeTop,
                endY = chartHeight
            ),
            topLeft = Offset(incomeLeft, incomeTop),
            size = Size(barWidth, incomeHeight),
            cornerRadius = cornerRadius
        )

        // Expense Bar (Right of center)
        val expenseCalculatedHeight = (point.expense / maxAmount) * chartHeight * progress
        val expenseHeight = if (point.expense > 0) expenseCalculatedHeight.coerceAtLeast(minBarHeight) else minBarHeight
        val expenseLeft = centerX + (barSpacing / 2f)
        val expenseTop = chartHeight - expenseHeight

        val actualExpenseAlpha = if (point.expense > 0) alpha else 0.15f
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    expenseColor.copy(alpha = actualExpenseAlpha),
                    expenseGradientBottom.copy(alpha = actualExpenseAlpha)
                ),
                startY = expenseTop,
                endY = chartHeight
            ),
            topLeft = Offset(expenseLeft, expenseTop),
            size = Size(barWidth, expenseHeight),
            cornerRadius = cornerRadius
        )

        // Selected indicator dot under label
        if (selectedIndex == i) {
            val indicatorColor = if (point.income >= point.expense) incomeColor else expenseColor
            drawCircle(
                color = indicatorColor,
                radius = 3.dp.toPx(),
                center = Offset(centerX, chartHeight + 20.dp.toPx())
            )
        }

        // Draw Axis Label
        val shouldDrawLabel = when {
            count > 16 -> i % 4 == 0 || i == count - 1
            count > 8 -> i % 2 == 0 || i == count - 1
            else -> true
        }

        if (shouldDrawLabel) {
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(
                    (if (selectedIndex == i) 255 else 165),
                    (labelColor.red * 255).toInt(),
                    (labelColor.green * 255).toInt(),
                    (labelColor.blue * 255).toInt()
                )
                textSize = 10.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                typeface = if (selectedIndex == i) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
            }
            drawContext.canvas.nativeCanvas.drawText(
                point.label,
                centerX,
                chartHeight + 14.dp.toPx(),
                textPaint
            )
        }
    }
}

private fun DrawScope.drawModernLineChart(
    dataPoints: List<FinancialDataPoint>,
    maxAmount: Float,
    progress: Float,
    selectedIndex: Int?,
    chartHeight: Float,
    width: Float,
    incomeColor: Color,
    expenseColor: Color,
    gridLineColor: Color,
    labelColor: Color,
    isDark: Boolean
) {
    val count = dataPoints.size
    if (count < 2) return

    val slotWidth = width / (count - 1)
    val incomePoints = mutableListOf<Offset>()
    val expensePoints = mutableListOf<Offset>()

    dataPoints.forEachIndexed { i, point ->
        val x = i * slotWidth
        val yIncome = chartHeight - ((point.income / maxAmount) * chartHeight * progress)
        val yExpense = chartHeight - ((point.expense / maxAmount) * chartHeight * progress)
        incomePoints.add(Offset(x, yIncome.coerceIn(0f, chartHeight)))
        expensePoints.add(Offset(x, yExpense.coerceIn(0f, chartHeight)))
    }

    // 1. Draw Smooth Gradient Area & Line for Income
    val incomePath = createSmoothPath(incomePoints)
    val incomeAreaPath = Path().apply {
        addPath(incomePath)
        lineTo(width, chartHeight)
        lineTo(0f, chartHeight)
        close()
    }

    drawPath(
        path = incomeAreaPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                incomeColor.copy(alpha = 0.28f * progress),
                incomeColor.copy(alpha = 0.0f)
            ),
            startY = 0f,
            endY = chartHeight
        )
    )

    drawPath(
        path = incomePath,
        color = incomeColor,
        style = Stroke(
            width = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    )

    // 2. Draw Smooth Gradient Area & Line for Expense
    val expensePath = createSmoothPath(expensePoints)
    val expenseAreaPath = Path().apply {
        addPath(expensePath)
        lineTo(width, chartHeight)
        lineTo(0f, chartHeight)
        close()
    }

    drawPath(
        path = expenseAreaPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                expenseColor.copy(alpha = 0.22f * progress),
                expenseColor.copy(alpha = 0.0f)
            ),
            startY = 0f,
            endY = chartHeight
        )
    )

    drawPath(
        path = expensePath,
        color = expenseColor,
        style = Stroke(
            width = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    )

    // 3. Interactive touch scrubber guide line and active indicator dot ONLY when selected
    if (selectedIndex != null && selectedIndex in dataPoints.indices) {
        val selIdx = selectedIndex
        val selectedX = selIdx * slotWidth
        drawLine(
            color = gridLineColor.copy(alpha = 0.85f),
            start = Offset(selectedX, 0f),
            end = Offset(selectedX, chartHeight),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        )

        // Draw indicator dot ONLY for the actively selected point during touch scrub
        val incPt = incomePoints[selIdx]
        drawCircle(
            color = if (isDark) Color(0xFF0F172A) else Color.White,
            radius = 5.dp.toPx(),
            center = incPt
        )
        drawCircle(
            color = incomeColor,
            radius = 3.5.dp.toPx(),
            center = incPt
        )

        val expPt = expensePoints[selIdx]
        drawCircle(
            color = if (isDark) Color(0xFF0F172A) else Color.White,
            radius = 5.dp.toPx(),
            center = expPt
        )
        drawCircle(
            color = expenseColor,
            radius = 3.5.dp.toPx(),
            center = expPt
        )
    }

    // 5. Draw Axis Labels
    dataPoints.forEachIndexed { i, point ->
        val x = i * slotWidth
        val shouldDrawLabel = when {
            count > 16 -> i % 4 == 0 || i == count - 1
            count > 8 -> i % 2 == 0 || i == count - 1
            else -> true
        }

        if (shouldDrawLabel) {
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(
                    (if (selectedIndex == i) 255 else 165),
                    (labelColor.red * 255).toInt(),
                    (labelColor.green * 255).toInt(),
                    (labelColor.blue * 255).toInt()
                )
                textSize = 10.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                typeface = if (selectedIndex == i) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
            }
            drawContext.canvas.nativeCanvas.drawText(
                point.label,
                x,
                chartHeight + 14.dp.toPx(),
                textPaint
            )
        }
    }
}

private fun createSmoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path

    path.moveTo(points.first().x, points.first().y)
    for (i in 0 until points.size - 1) {
        val p0 = points[i]
        val p1 = points[i + 1]
        val controlPointX1 = p0.x + (p1.x - p0.x) / 2f
        val controlPointY1 = p0.y
        val controlPointX2 = p0.x + (p1.x - p0.x) / 2f
        val controlPointY2 = p1.y

        path.cubicTo(
            controlPointX1, controlPointY1,
            controlPointX2, controlPointY2,
            p1.x, p1.y
        )
    }
    return path
}
