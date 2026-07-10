package com.example.momosshopmanager.ui.monthly

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momosshopmanager.data.*
import com.example.momosshopmanager.theme.*

@Composable
fun MonthlyScreen(viewModel: SalesViewModel) {

    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedMonthSales by viewModel.selectedMonthSales.collectAsState()
    val dayBreakdown by viewModel.selectedMonthDayBreakdown.collectAsState()
    val previousMonthTotal by viewModel.previousMonthTotal.collectAsState()

    val monthNames = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    val currentMonthTotal = selectedMonthSales.sumOf { it.total }
    val totalOrders = selectedMonthSales.size
    val avgOrderValue = if (totalOrders > 0) currentMonthTotal / totalOrders else 0.0

    val percentChange = if (previousMonthTotal > 0.0) {
        (currentMonthTotal - previousMonthTotal) / previousMonthTotal * 100
    } else {
        0.0
    }
    val isPositiveChange = percentChange >= 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ── Month/Year Selector ──────────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (selectedMonth == 0) {
                            viewModel.setSelectedMonth(selectedYear - 1, 11)
                        } else {
                            viewModel.setSelectedMonth(selectedYear, selectedMonth - 1)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronLeft,
                            contentDescription = "Previous Month",
                            tint = MomosOrange
                        )
                    }

                    Text(
                        text = "${monthNames[selectedMonth]} $selectedYear",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    IconButton(onClick = {
                        if (selectedMonth == 11) {
                            viewModel.setSelectedMonth(selectedYear + 1, 0)
                        } else {
                            viewModel.setSelectedMonth(selectedYear, selectedMonth + 1)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = "Next Month",
                            tint = MomosOrange
                        )
                    }
                }
            }
        }

        // ── Monthly Summary Card (Gradient) ──────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(GradientOrangeStart, GradientOrangeEnd)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Monthly Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkBackground
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Total Revenue
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Total Revenue",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DarkBackground.copy(alpha = 0.75f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "₹%,.0f".format(currentMonthTotal),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkBackground
                                )
                            }

                            // Total Orders
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Total Orders",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DarkBackground.copy(alpha = 0.75f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$totalOrders",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkBackground
                                )
                            }

                            // Avg Order Value
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Avg Order",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DarkBackground.copy(alpha = 0.75f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "₹%,.0f".format(avgOrderValue),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkBackground
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Month-over-Month Comparison Card ─────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Trend icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isPositiveChange) SuccessGreen.copy(alpha = 0.15f)
                                else ErrorRed.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPositiveChange) Icons.Rounded.TrendingUp
                            else Icons.Rounded.TrendingDown,
                            contentDescription = if (isPositiveChange) "Trending Up" else "Trending Down",
                            tint = if (isPositiveChange) SuccessGreen else ErrorRed,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "vs Previous Month",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))

                        val previousMonthIndex = if (selectedMonth == 0) 11 else selectedMonth - 1
                        Text(
                            text = "${monthNames[previousMonthIndex]}: ₹%,.0f".format(previousMonthTotal),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }

                    // Percentage change
                    Text(
                        text = "${if (isPositiveChange) "+" else ""}%.1f%%".format(percentChange),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isPositiveChange) SuccessGreen else ErrorRed
                    )
                }
            }
        }

        // ── Day-by-Day Breakdown Header ──────────────────────────────────
        item {
            Text(
                text = "Day-by-Day Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // ── Column Headers ───────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MomosOrangeDark
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Day",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = "Orders",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Revenue",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.End
                    )
                }
            }
        }

        // ── Day Rows ─────────────────────────────────────────────────────
        itemsIndexed(dayBreakdown) { index, (day, orderCount, revenue) ->
            val isEvenRow = index % 2 == 0
            val rowColor = if (isEvenRow) {
                MaterialTheme.colorScheme.surfaceContainerLow
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }

            val shape = if (index == dayBreakdown.lastIndex) {
                RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
            } else {
                RoundedCornerShape(0.dp)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = shape,
                colors = CardDefaults.cardColors(containerColor = rowColor)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$day",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            textAlign = TextAlign.Start
                        )
                        Text(
                            text = "$orderCount",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "₹%,.0f".format(revenue),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Golden,
                            textAlign = TextAlign.End
                        )
                    }

                    if (index != dayBreakdown.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
