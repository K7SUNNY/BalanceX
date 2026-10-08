package com.k7sunny.balancex.ui.compose

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.*

data class CategoryBreakdownItem(
    val category: String,
    val amount: Float,
    val color: Color
)

@Composable
fun ModernPieChart(
    items: List<CategoryBreakdownItem>,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    val textColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val textMutedColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val tooltipBgColor = if (isDark) Color(0xFF0F172A) else Color(0xFF1E293B)
    val barTrackColor = if (isDark) Color(0x1FFFFFFF) else Color(0x12000000)

    val totalExpenses = remember(items) { items.sumOf { it.amount.toDouble() }.toFloat() }

    var selectedIndex by remember(items) { mutableStateOf<Int?>(null) }

    var animTrigger by remember { mutableStateOf(0f) }
    LaunchedEffect(items) {
        animTrigger = 0f
        animTrigger = 1f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = animTrigger,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "donutAnim"
    )

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build()).apply {
            maximumFractionDigits = 0
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (items.isEmpty() || totalExpenses <= 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "No category data available",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = textMutedColor
                    )
                    Text(
                        text = "Add transactions to view category breakdowns",
                        fontSize = 11.sp,
                        color = textMutedColor.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            // Donut Chart Container
            val activeItem = selectedIndex?.let { if (it in items.indices) items[it] else null }
            val activePercent = if (activeItem != null && totalExpenses > 0f) {
                round((activeItem.amount / totalExpenses) * 100f).toInt()
            } else null

            Box(
                modifier = Modifier
                    .size(240.dp)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                // Interactive Donut Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(items) {
                            detectTapGestures(
                                onTap = { tapOffset ->
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val dx = tapOffset.x - center.x
                                    val dy = tapOffset.y - center.y
                                    val distance = sqrt(dx * dx + dy * dy)

                                    val outerRadius = size.width / 2f
                                    val strokeWidth = 34.dp.toPx()
                                    val innerRadius = outerRadius - strokeWidth

                                    if (distance in (innerRadius - 20f)..(outerRadius + 20f)) {
                                        var touchAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                        if (touchAngle < 0) touchAngle += 360f

                                        // Find corresponding slice
                                        var currentAngle = 270f // starts at 12 o'clock
                                        var found = false
                                        for (i in items.indices) {
                                            val sweep = (items[i].amount / totalExpenses) * 360f
                                            val start = currentAngle % 360f
                                            val end = (currentAngle + sweep) % 360f

                                            val inSlice = if (start < end) {
                                                touchAngle in start..end
                                            } else {
                                                touchAngle >= start || touchAngle <= end
                                            }

                                            if (inSlice) {
                                                selectedIndex = if (selectedIndex == i) null else i
                                                found = true
                                                break
                                            }
                                            currentAngle += sweep
                                        }
                                        if (!found) selectedIndex = null
                                    } else {
                                        selectedIndex = null
                                    }
                                }
                            )
                        }
                ) {
                    val strokeWidth = 34.dp.toPx()
                    val arcPadding = strokeWidth / 2f
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(arcPadding, arcPadding)

                    var currentStartAngle = 270f // Start at 12 o'clock
                    val minSweepForGap = 16f

                    items.forEachIndexed { idx, item ->
                        val rawSweep = (item.amount / totalExpenses) * 360f * animatedProgress
                        val isSelected = selectedIndex == idx
                        val hasSelection = selectedIndex != null

                        // Leave clean angular gap between segments like in reference UI
                        val gap = if (items.size > 1 && rawSweep > minSweepForGap) 8f else 1f
                        val actualSweep = (rawSweep - gap).coerceAtLeast(1f)
                        val startAngle = currentStartAngle + (gap / 2f)

                        val sliceStrokeWidth = if (isSelected) strokeWidth + 6.dp.toPx() else strokeWidth
                        val sliceAlpha = if (!hasSelection || isSelected) 1f else 0.40f

                        drawArc(
                            color = item.color.copy(alpha = sliceAlpha),
                            startAngle = startAngle,
                            sweepAngle = actualSweep,
                            useCenter = false,
                            topLeft = if (isSelected) Offset(arcPadding - 3.dp.toPx(), arcPadding - 3.dp.toPx()) else topLeft,
                            size = if (isSelected) Size(arcSize.width + 6.dp.toPx(), arcSize.height + 6.dp.toPx()) else arcSize,
                            style = Stroke(
                                width = sliceStrokeWidth,
                                cap = StrokeCap.Round
                            )
                        )

                        currentStartAngle += rawSweep
                    }
                }

                // Center Information (Total Expenses / Selected Category)
                Column(
                    modifier = Modifier
                        .clickable { selectedIndex = null }
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (activeItem != null) activeItem.category else "Total Expenses",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textMutedColor,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currencyFormatter.format(activeItem?.amount ?: totalExpenses),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                    if (activePercent != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = tooltipBgColor,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "$activePercent%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category Breakdown Rows (matching Cards_UI / Card_List_Overview.webp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items.take(4).forEachIndexed { index, item ->
                    val isSelected = selectedIndex == index
                    val percent = if (totalExpenses > 0f) round((item.amount / totalExpenses) * 100f).toInt() else 0

                    Surface(
                        color = if (isSelected) item.color.copy(alpha = if (isDark) 0.16f else 0.08f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedIndex = if (selectedIndex == index) null else index
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .background(item.color, CircleShape)
                                    )
                                    Text(
                                        text = item.category,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = textColor
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = item.color.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "$percent%",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = item.color,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = currencyFormatter.format(item.amount),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Subtle progress bar under category row
                            LinearProgressIndicator(
                                progress = { (item.amount / totalExpenses) * animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = item.color,
                                trackColor = barTrackColor,
                                strokeCap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }
    }
}
