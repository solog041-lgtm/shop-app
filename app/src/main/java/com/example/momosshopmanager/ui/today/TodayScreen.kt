package com.example.momosshopmanager.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.example.momosshopmanager.data.UserRole
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.momosshopmanager.data.MenuDefaults
import com.example.momosshopmanager.data.MenuItem
import com.example.momosshopmanager.data.PaymentMethod
import com.example.momosshopmanager.data.Sale
import com.example.momosshopmanager.data.SaleItem
import com.example.momosshopmanager.data.SalesViewModel
import com.example.momosshopmanager.theme.ChartBlue
import com.example.momosshopmanager.theme.ChartPurple
import com.example.momosshopmanager.theme.DarkCard
import com.example.momosshopmanager.theme.DarkCardElevated
import com.example.momosshopmanager.theme.DarkSurface
import com.example.momosshopmanager.theme.DarkSurfaceVariant
import com.example.momosshopmanager.theme.ErrorRed
import com.example.momosshopmanager.theme.GradientOrangeEnd
import com.example.momosshopmanager.theme.GradientOrangeStart
import com.example.momosshopmanager.theme.MomosOrange
import com.example.momosshopmanager.theme.MomosOrangeLight
import com.example.momosshopmanager.theme.SuccessGreen
import com.example.momosshopmanager.theme.TextMuted
import com.example.momosshopmanager.theme.TextPrimary
import com.example.momosshopmanager.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(viewModel: SalesViewModel) {
    val context = LocalContext.current
    val todaySales by viewModel.todaySales.collectAsState()
    val todayTotal by viewModel.todayTotal.collectAsState()
    val todayOrderCount by viewModel.todayOrderCount.collectAsState()
    val menuItems by viewModel.menuItems.collectAsState()

    var showBottomSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val filteredSales by remember(todaySales, searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                todaySales
            } else {
                val query = searchQuery.lowercase()
                todaySales.filter { sale ->
                    sale.customerName.lowercase().contains(query) ||
                        sale.items.any { it.menuItem.name.lowercase().contains(query) }
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showBottomSheet = true },
                containerColor = MomosOrange,
                contentColor = Color.White,
                shape = CircleShape,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add Sale",
                    modifier = Modifier.size(28.dp),
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ── Summary Bar ──
            SummaryBar(
                total = todayTotal,
                orderCount = todayOrderCount,
                isSearchVisible = isSearchVisible,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onToggleSearch = {
                    isSearchVisible = !isSearchVisible
                    if (!isSearchVisible) searchQuery = ""
                },
            )

            // ── Stock Alert Button (Visible to Employees) ──
            val userRole by viewModel.userRole.collectAsState()
            if (userRole == UserRole.EMPLOYEE) {
                var showStockAlertConfirm by remember { mutableStateOf(false) }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { showStockAlertConfirm = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = "Stock Alert",
                            tint = ErrorRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🚨 Alert Owner: Low Stock",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = ErrorRed
                        )
                    }
                }
                
                if (showStockAlertConfirm) {
                    AlertDialog(
                        onDismissRequest = { showStockAlertConfirm = false },
                        title = { Text("🚨 Low Stock Alert") },
                        text = { Text("Are you sure you want to ring the owner that stock is running low?") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.raiseLowStockAlert()
                                    showStockAlertConfirm = false
                                    Toast.makeText(context, "Stock alarm sent to Owner!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                            ) {
                                Text("Ring Owner")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showStockAlertConfirm = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }

            // ── Sales List ──
            if (filteredSales.isEmpty()) {
                EmptyState(isSearchActive = searchQuery.isNotBlank())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 88.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(
                        items = filteredSales,
                        key = { _, sale -> sale.id },
                    ) { index, sale ->
                        AnimatedVisibility(
                            visible = true,
                            enter = slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessLow,
                                ),
                            ) + fadeIn(),
                        ) {
                            SwipeableSaleCard(
                                sale = sale,
                                onDelete = { viewModel.deleteSale(sale.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Add Sale Bottom Sheet ──
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = DarkSurface,
            contentColor = TextPrimary,
            dragHandle = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(TextMuted),
                    )
                }
            },
        ) {
            AddSaleSheetContent(
                menuItems = menuItems,
                onAddSale = { sale ->
                    viewModel.addSale(sale)
                    scope.launch {
                        sheetState.hide()
                        showBottomSheet = false
                    }
                },
                onDismiss = {
                    scope.launch {
                        sheetState.hide()
                        showBottomSheet = false
                    }
                },
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════
//  Summary Bar
// ════════════════════════════════════════════════════════════════════

@Composable
private fun SummaryBar(
    total: Double,
    orderCount: Int,
    isSearchVisible: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(GradientOrangeStart, GradientOrangeEnd),
                    ),
                )
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Today's Sales",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹%.0f".format(total),
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    IconButton(onClick = onToggleSearch) {
                        Icon(
                            imageVector = if (isSearchVisible) Icons.Rounded.Close else Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = Color.White,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "$orderCount orders",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        // ── Search Bar ──
        AnimatedVisibility(
            visible = isSearchVisible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = fadeOut(),
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = {
                    Text(
                        "Search by customer or item…",
                        color = TextMuted,
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                        tint = TextSecondary,
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Clear",
                                tint = TextSecondary,
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MomosOrange,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    cursorColor = MomosOrange,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkCard,
                    unfocusedContainerColor = DarkCard,
                ),
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════
//  Empty State
// ════════════════════════════════════════════════════════════════════

@Composable
private fun EmptyState(isSearchActive: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isSearchActive) "🔍" else "🥟",
                style = MaterialTheme.typography.displayLarge,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isSearchActive) "No matching sales" else "No sales yet today",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isSearchActive) "Try a different search term" else "Tap + to add your first sale",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════
//  Swipeable Sale Card
// ════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableSaleCard(
    sale: Sale,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                    Color(0xFFD32F2F)
                else
                    Color.Transparent,
                label = "swipeColor",
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        },
        enableDismissFromStartToEnd = false,
    ) {
        SaleCard(sale = sale)
    }
}

@Composable
private fun SaleCard(sale: Sale) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = remember(sale.timestamp) {
        timeFormat.format(Date(sale.timestamp))
    }
    val itemsSummary = remember(sale.items) {
        sale.items.joinToString(", ") { "${it.menuItem.name} ×${it.quantity}" }
    }

    val paymentColor = when (sale.paymentMethod) {
        PaymentMethod.CASH -> SuccessGreen
        PaymentMethod.UPI -> ChartBlue
        PaymentMethod.CARD -> ChartPurple
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Time pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkCardElevated)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.labelLarge,
                    color = MomosOrangeLight,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = itemsSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val createdByText = if (sale.createdBy.isNotBlank()) "by " + sale.createdBy.takeLast(10) else ""
                if (sale.customerName.isNotBlank() || createdByText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (sale.customerName.isNotBlank()) {
                            Text(
                                text = sale.customerName,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                            )
                            if (createdByText.isNotBlank()) {
                                Text(
                                    text = " • ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                )
                            }
                        }
                        if (createdByText.isNotBlank()) {
                            Text(
                                text = createdByText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MomosOrangeLight,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Amount & Payment badge
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹%.0f".format(sale.total),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(paymentColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = sale.paymentMethod.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = paymentColor,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
//  Add Sale Bottom Sheet Content
// ════════════════════════════════════════════════════════════════════

@Composable
private fun AddSaleSheetContent(
    menuItems: List<MenuItem>,
    onAddSale: (Sale) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedCategory by remember { mutableStateOf(MenuDefaults.categories.first()) }
    val quantities = remember { mutableStateMapOf<String, Int>() }
    var selectedPayment by remember { mutableStateOf(PaymentMethod.CASH) }
    var customerName by remember { mutableStateOf("") }

    val filteredMenuItems by remember(menuItems, selectedCategory) {
        derivedStateOf {
            menuItems.filter { it.category == selectedCategory && it.isAvailable }
        }
    }

    val orderTotal by remember {
        derivedStateOf {
            quantities.entries.sumOf { (id, qty) ->
                val item = menuItems.find { it.id == id }
                (item?.price ?: 0.0) * qty
            }
        }
    }

    val hasSelection by remember {
        derivedStateOf { quantities.values.any { it > 0 } }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        // ── Header ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "New Sale",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Close",
                    tint = TextSecondary,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Category Chips ──
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(MenuDefaults.categories) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MomosOrange,
                        selectedLabelColor = Color.White,
                        containerColor = DarkCardElevated,
                        labelColor = TextSecondary,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = Color.Transparent,
                        selectedBorderColor = Color.Transparent,
                        enabled = true,
                        selected = selectedCategory == category,
                    ),
                    shape = RoundedCornerShape(12.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = DarkSurfaceVariant, thickness = 0.5.dp)

        // ── Menu Items ──
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(filteredMenuItems, key = { it.id }) { item ->
                val qty = quantities[item.id] ?: 0
                MenuItemRow(
                    item = item,
                    quantity = qty,
                    onIncrement = { quantities[item.id] = qty + 1 },
                    onDecrement = {
                        if (qty > 0) {
                            if (qty == 1) quantities.remove(item.id)
                            else quantities[item.id] = qty - 1
                        }
                    },
                )
            }
        }

        HorizontalDivider(color = DarkSurfaceVariant, thickness = 0.5.dp)

        // ── Bottom Controls ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkCard)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            // Payment method selector
            Text(
                text = "Payment Method",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PaymentMethod.entries.forEach { method ->
                    val isSelected = selectedPayment == method
                    val methodColor = when (method) {
                        PaymentMethod.CASH -> SuccessGreen
                        PaymentMethod.UPI -> ChartBlue
                        PaymentMethod.CARD -> ChartPurple
                    }
                    FilledTonalButton(
                        onClick = { selectedPayment = method },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isSelected)
                                methodColor.copy(alpha = 0.2f)
                            else
                                DarkCardElevated,
                            contentColor = if (isSelected)
                                methodColor
                            else
                                TextSecondary,
                        ),
                    ) {
                        Text(
                            text = method.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Customer name
            OutlinedTextField(
                value = customerName,
                onValueChange = { customerName = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Customer name (optional)", color = TextMuted)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MomosOrange,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    cursorColor = MomosOrange,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Total + Add button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Order Total",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                    Text(
                        text = "₹%.0f".format(orderTotal),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MomosOrange,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Button(
                    onClick = {
                        val saleItems = quantities.mapNotNull { (id, qty) ->
                            val item = menuItems.find { it.id == id }
                            if (item != null && qty > 0) SaleItem(menuItem = item, quantity = qty)
                            else null
                        }
                        if (saleItems.isNotEmpty()) {
                            onAddSale(
                                Sale(
                                    items = saleItems,
                                    paymentMethod = selectedPayment,
                                    customerName = customerName.trim(),
                                ),
                            )
                        }
                    },
                    enabled = hasSelection,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MomosOrange,
                        contentColor = Color.White,
                        disabledContainerColor = DarkCardElevated,
                        disabledContentColor = TextMuted,
                    ),
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp),
                ) {
                    Text(
                        text = "Add Sale",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
//  Menu Item Row (in bottom sheet)
// ════════════════════════════════════════════════════════════════════

@Composable
private fun MenuItemRow(
    item: MenuItem,
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    val isActive = quantity > 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isActive) TextPrimary else TextSecondary,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            )
            Text(
                text = "₹%.0f".format(item.price),
                style = MaterialTheme.typography.bodySmall,
                color = if (isActive) MomosOrangeLight else TextMuted,
            )
        }

        // Quantity stepper
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Minus button
            IconButton(
                onClick = onDecrement,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) DarkCardElevated else Color.Transparent,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = "Decrease",
                    tint = if (isActive) TextPrimary else TextMuted,
                    modifier = Modifier.size(18.dp),
                )
            }

            // Count
            Text(
                text = "$quantity",
                style = MaterialTheme.typography.titleMedium,
                color = if (isActive) MomosOrange else TextMuted,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(28.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            // Plus button
            IconButton(
                onClick = onIncrement,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MomosOrange.copy(alpha = 0.15f)),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Increase",
                    tint = MomosOrange,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
