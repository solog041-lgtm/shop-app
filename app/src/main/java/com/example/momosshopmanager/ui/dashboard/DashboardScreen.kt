package com.example.momosshopmanager.ui.dashboard

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momosshopmanager.data.PaymentMethod
import com.example.momosshopmanager.data.Sale
import com.example.momosshopmanager.data.SalesViewModel
import com.example.momosshopmanager.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(viewModel: SalesViewModel) {
    val todayTotal by viewModel.todayTotal.collectAsState()
    val monthlyTotal by viewModel.monthlyTotal.collectAsState()
    val averageSale by viewModel.averageSale.collectAsState()
    val todayOrderCount by viewModel.todayOrderCount.collectAsState()
    val last7DaysSales by viewModel.last7DaysSales.collectAsState()
    val recentSales by viewModel.recentSales.collectAsState()

    val currentDate = remember {
        SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ─── Greeting Header ───────────────────────────────────
        item {
            GreetingHeader(currentDate)
        }

        // ─── KPI Grid (2 × 2) ─────────────────────────────────
        item {
            KpiGrid(
                todayTotal = todayTotal,
                monthlyTotal = monthlyTotal,
                averageSale = averageSale,
                todayOrderCount = todayOrderCount
            )
        }

        // ─── Last 7 Days Bar Chart ─────────────────────────────
        item {
            Last7DaysChart(data = last7DaysSales)
        }

        // ─── Recent Orders Header ──────────────────────────────
        item {
            Text(
                text = "Recent Orders",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // ─── Recent Orders List ────────────────────────────────
        if (recentSales.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No sales recorded yet",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            items(recentSales.take(10), key = { it.id }) { sale ->
                RecentOrderCard(sale)
            }
        }

        // Bottom spacer so content doesn't hide behind nav bar
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

// ═══════════════════════════════════════════════════════════
// Greeting Header
// ═══════════════════════════════════════════════════════════

@Composable
private fun GreetingHeader(currentDate: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "🥟 Momos Shop",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = currentDate,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

// ═══════════════════════════════════════════════════════════
// KPI Grid
// ═══════════════════════════════════════════════════════════

@Composable
private fun KpiGrid(
    todayTotal: Double,
    monthlyTotal: Double,
    averageSale: Double,
    todayOrderCount: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiCard(
                modifier = Modifier.weight(1f),
                label = "Today's Sales",
                value = "₹%,.0f".format(todayTotal),
                icon = Icons.Rounded.CurrencyRupee,
                gradientStart = GradientOrangeStart,
                gradientEnd = GradientOrangeEnd,
                iconTint = MomosOrange
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                label = "Monthly Sales",
                value = "₹%,.0f".format(monthlyTotal),
                icon = Icons.Rounded.CalendarMonth,
                gradientStart = GradientRedStart,
                gradientEnd = GradientRedEnd,
                iconTint = ChiliRed
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiCard(
                modifier = Modifier.weight(1f),
                label = "Avg Sale Value",
                value = "₹%,.0f".format(averageSale),
                icon = Icons.Rounded.TrendingUp,
                gradientStart = GoldenDark,
                gradientEnd = Golden,
                iconTint = Golden
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                label = "Today's Orders",
                value = todayOrderCount.toString(),
                icon = Icons.Rounded.Receipt,
                gradientStart = ChartTeal.copy(alpha = 0.9f),
                gradientEnd = ChartTeal.copy(alpha = 0.5f),
                iconTint = ChartTeal
            )
        }
    }
}

@Composable
private fun KpiCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    gradientStart: Color,
    gradientEnd: Color,
    iconTint: Color
) {
    // Animate the value appearance
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "kpiAlpha"
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            gradientStart.copy(alpha = 0.18f),
                            gradientEnd.copy(alpha = 0.08f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                // Icon circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Label
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Value (animated)
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = animatedAlpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// Last 7 Days Bar Chart
// ═══════════════════════════════════════════════════════════

@Composable
private fun Last7DaysChart(data: List<Pair<String, Double>>) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Last 7 Days",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Daily sales overview",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (data.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No data yet", color = TextMuted)
                }
            } else {
                BarChart(data = data)
            }
        }
    }
}

@Composable
private fun BarChart(data: List<Pair<String, Double>>) {
    val maxValue = remember(data) { data.maxOfOrNull { it.second } ?: 1.0 }

    // Animate bars growing from 0 → 1
    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(data) { animationTriggered = true }

    val animationProgress by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "barChartAnim"
    )

    val textMeasurer = rememberTextMeasurer()
    val barColor = MomosOrange
    val barColorEnd = GradientOrangeEnd
    val labelColor = TextSecondary
    val valueColor = TextPrimary

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val barCount = data.size
        val totalSpacing = 10.dp.toPx() * (barCount - 1)
        val barWidth = (size.width - totalSpacing) / barCount
        val bottomLabelHeight = 28.dp.toPx()
        val topValueHeight = 20.dp.toPx()
        val chartHeight = size.height - bottomLabelHeight - topValueHeight

        data.forEachIndexed { index, (dayLabel, amount) ->
            val barFraction = if (maxValue > 0) (amount / maxValue).toFloat() else 0f
            val animatedBarHeight = chartHeight * barFraction * animationProgress

            val left = index * (barWidth + 10.dp.toPx())
            val top = topValueHeight + chartHeight - animatedBarHeight

            // Bar with gradient
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(barColor, barColorEnd),
                    start = Offset(left, top),
                    end = Offset(left, top + animatedBarHeight)
                ),
                topLeft = Offset(left, top),
                size = Size(barWidth, animatedBarHeight),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                style = Fill
            )

            // Day label (below bar)
            val dayLabelLayout = textMeasurer.measure(
                text = dayLabel,
                style = TextStyle(
                    color = labelColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            drawText(
                textLayoutResult = dayLabelLayout,
                topLeft = Offset(
                    x = left + (barWidth - dayLabelLayout.size.width) / 2f,
                    y = size.height - bottomLabelHeight + 8.dp.toPx()
                )
            )

            // Value label (above bar) — only show when animation nearly done
            if (animationProgress > 0.6f) {
                val valueTxt = if (amount >= 1000) {
                    "₹%.0fk".format(amount / 1000.0)
                } else {
                    "₹%.0f".format(amount)
                }
                val valueLayout = textMeasurer.measure(
                    text = valueTxt,
                    style = TextStyle(
                        color = valueColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                drawText(
                    textLayoutResult = valueLayout,
                    topLeft = Offset(
                        x = left + (barWidth - valueLayout.size.width) / 2f,
                        y = top - valueLayout.size.height - 4.dp.toPx()
                    )
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// Recent Order Card
// ═══════════════════════════════════════════════════════════

@Composable
private fun RecentOrderCard(sale: Sale) {
    val timeFormatted = remember(sale.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(sale.timestamp))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top row: items summary & total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Item names (scrollable horizontally is overkill; ellipsize)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sale.items.joinToString(", ") {
                            "${it.menuItem.name} ×${it.quantity}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "₹%,.0f".format(sale.total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MomosOrange
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = DarkSurfaceVariant, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Bottom row: payment badge, customer, time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PaymentBadge(paymentMethod = sale.paymentMethod)
                    if (sale.customerName.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = sale.customerName,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun PaymentBadge(paymentMethod: PaymentMethod) {
    val (bgColor, textColor) = when (paymentMethod) {
        PaymentMethod.CASH -> SuccessGreen.copy(alpha = 0.15f) to SuccessGreenLight
        PaymentMethod.UPI -> ChartBlue.copy(alpha = 0.15f) to ChartBlue
        PaymentMethod.CARD -> ChartPurple.copy(alpha = 0.15f) to ChartPurple
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = paymentMethod.displayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
