package com.example.momosshopmanager.ui.analytics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momosshopmanager.data.PaymentMethod
import com.example.momosshopmanager.data.SalesViewModel
import com.example.momosshopmanager.theme.*

@Composable
fun AnalyticsScreen(viewModel: SalesViewModel) {
    val dailyAvg by viewModel.dailyAvg.collectAsState()
    val weeklyAvg by viewModel.weeklyAvg.collectAsState()
    val monthlyAvg by viewModel.monthlyAvg.collectAsState()
    val bestSellers by viewModel.bestSellers.collectAsState()
    val peakHours by viewModel.peakHours.collectAsState()
    val paymentDistribution by viewModel.paymentDistribution.collectAsState()

    // Animation trigger
    var animationStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animationStarted = true }

    val animationProgress by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "chartAnimation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ── Section 1: Sales Averages ──────────────────────────────────
        SectionHeader(
            title = "Sales Averages",
            icon = Icons.Rounded.TrendingUp,
            iconTint = MomosOrange
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AverageCard(
                modifier = Modifier.weight(1f),
                label = "Daily Avg",
                value = "₹%,.0f".format(dailyAvg),
                gradient = Brush.linearGradient(
                    colors = listOf(MomosOrange.copy(alpha = 0.25f), MomosOrangeDark.copy(alpha = 0.10f))
                ),
                accentColor = MomosOrange
            )
            AverageCard(
                modifier = Modifier.weight(1f),
                label = "Weekly Avg",
                value = "₹%,.0f".format(weeklyAvg),
                gradient = Brush.linearGradient(
                    colors = listOf(ChartBlue.copy(alpha = 0.25f), ChartBlue.copy(alpha = 0.10f))
                ),
                accentColor = ChartBlue
            )
            AverageCard(
                modifier = Modifier.weight(1f),
                label = "Monthly Avg",
                value = "₹%,.0f".format(monthlyAvg),
                gradient = Brush.linearGradient(
                    colors = listOf(SuccessGreen.copy(alpha = 0.25f), SuccessGreen.copy(alpha = 0.10f))
                ),
                accentColor = SuccessGreen
            )
        }

        // ── Section 2: Best Sellers ────────────────────────────────────
        SectionHeader(
            title = "Best Sellers",
            icon = Icons.Rounded.Star,
            iconTint = Golden
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val topItems = bestSellers.take(5)
                val maxQuantity = topItems.maxOfOrNull { it.second } ?: 1

                if (topItems.isEmpty()) {
                    Text(
                        text = "No sales data yet",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        textAlign = TextAlign.Center
                    )
                }

                topItems.forEachIndexed { index, (name, quantity) ->
                    BestSellerRow(
                        rank = index + 1,
                        name = name,
                        quantity = quantity,
                        maxQuantity = maxQuantity,
                        animationProgress = animationProgress
                    )
                }
            }
        }

        // ── Section 3: Peak Hours ──────────────────────────────────────
        SectionHeader(
            title = "Peak Hours",
            icon = Icons.Rounded.AccessTime,
            iconTint = ChartTeal
        )

        val peakSlots = listOf(
            "Morning" to "6 AM – 12 PM",
            "Lunch" to "12 PM – 3 PM",
            "Evening" to "3 PM – 7 PM",
            "Night" to "7 PM – 11 PM"
        )
        val maxOrders = peakHours.values.maxOrNull() ?: 0

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            peakSlots.forEach { (label, timeRange) ->
                val count = peakHours[label] ?: 0
                val isHighest = count > 0 && count == maxOrders
                PeakHourCard(
                    modifier = Modifier.weight(1f),
                    label = label,
                    timeRange = timeRange,
                    count = count,
                    isHighest = isHighest
                )
            }
        }

        // ── Section 4: Payment Methods (Donut Chart) ───────────────────
        SectionHeader(
            title = "Payment Methods",
            icon = Icons.Rounded.Payment,
            iconTint = ChartPurple
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                val segments = buildPaymentSegments(paymentDistribution)

                if (segments.isEmpty()) {
                    Text(
                        text = "No payment data yet",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    // Donut chart
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(200.dp)
                    ) {
                        Canvas(modifier = Modifier.size(200.dp)) {
                            val strokeWidth = 36.dp.toPx()
                            val radius = (size.minDimension - strokeWidth) / 2f
                            val topLeft = Offset(
                                (size.width - radius * 2) / 2f,
                                (size.height - radius * 2) / 2f
                            )
                            val arcSize = Size(radius * 2, radius * 2)
                            var startAngle = -90f

                            segments.forEach { segment ->
                                val sweep = (segment.percentage / 100.0 * 360.0 * animationProgress).toFloat()
                                drawArc(
                                    color = segment.color,
                                    startAngle = startAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(
                                        width = strokeWidth,
                                        cap = StrokeCap.Round
                                    )
                                )
                                startAngle += (segment.percentage / 100.0 * 360.0).toFloat()
                            }
                        }

                        // Center label
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Total",
                                color = TextMuted,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "100%",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Legend
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        segments.forEach { segment ->
                            LegendItem(
                                color = segment.color,
                                label = segment.label,
                                percentage = segment.percentage
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp)) // bottom nav clearance
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Composable helpers
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector,
    iconTint: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AverageCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    gradient: Brush,
    accentColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .padding(14.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.TrendingUp,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = value,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = label,
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun BestSellerRow(
    rank: Int,
    name: String,
    quantity: Int,
    maxQuantity: Int,
    animationProgress: Float
) {
    val medalColor = when (rank) {
        1 -> Golden
        2 -> TextSecondary          // silver
        3 -> MomosOrange             // bronze-like warm tone
        else -> TextMuted
    }
    val barColor = when (rank) {
        1 -> Golden
        2 -> GoldenLight
        3 -> MomosOrangeLight
        4 -> ChartTeal
        5 -> ChartBlue
        else -> ChartBlue
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Rank badge
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(medalColor.copy(alpha = 0.20f))
        ) {
            Text(
                text = "$rank",
                color = medalColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Name
        Text(
            text = name,
            color = TextPrimary,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Horizontal bar (Canvas)
        val fraction = if (maxQuantity > 0) quantity.toFloat() / maxQuantity else 0f
        Canvas(
            modifier = Modifier
                .weight(1.2f)
                .height(14.dp)
        ) {
            // Track
            drawRoundRect(
                color = DarkSurfaceVariant,
                cornerRadius = CornerRadius(7.dp.toPx()),
                size = Size(size.width, size.height)
            )
            // Filled bar
            val filledWidth = size.width * fraction * animationProgress
            if (filledWidth > 0f) {
                drawRoundRect(
                    color = barColor,
                    cornerRadius = CornerRadius(7.dp.toPx()),
                    size = Size(filledWidth, size.height)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Quantity label
        Text(
            text = "$quantity",
            color = TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun PeakHourCard(
    modifier: Modifier = Modifier,
    label: String,
    timeRange: String,
    count: Int,
    isHighest: Boolean
) {
    val containerColor = if (isHighest) MomosOrange.copy(alpha = 0.18f) else DarkCard
    val borderColor = if (isHighest) MomosOrange else Color.Transparent

    OutlinedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        border = BorderStroke(
            width = if (isHighest) 1.5.dp else 1.dp,
            color = if (isHighest) borderColor else DarkSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = if (isHighest) MomosOrange else TextSecondary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$count",
                color = if (isHighest) MomosOrange else TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "orders",
                color = TextMuted,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    percentage: Double
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            color = TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "%.1f%%".format(percentage),
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Data helpers
// ═══════════════════════════════════════════════════════════════════════════

private data class DonutSegment(
    val label: String,
    val percentage: Double,
    val color: Color
)

private fun buildPaymentSegments(
    distribution: Map<PaymentMethod, Double>
): List<DonutSegment> {
    if (distribution.isEmpty()) return emptyList()

    val colorMap = mapOf(
        PaymentMethod.CASH to SuccessGreen,
        PaymentMethod.UPI to ChartBlue,
        PaymentMethod.CARD to ChartPurple
    )

    return distribution.map { (method, pct) ->
        DonutSegment(
            label = method.displayName,
            percentage = pct,
            color = colorMap[method] ?: ChartBlue
        )
    }
}
