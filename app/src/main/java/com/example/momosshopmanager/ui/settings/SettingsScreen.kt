package com.example.momosshopmanager.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momosshopmanager.data.*
import com.example.momosshopmanager.data.MenuDefaults
import com.example.momosshopmanager.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SalesViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State bindings
    val userRole by viewModel.userRole.collectAsState()
    val userPhone by viewModel.userPhone.collectAsState()
    val syncCode by viewModel.syncCode.collectAsState()
    val syncEnabled by viewModel.syncEnabled.collectAsState()
    val syncingState by viewModel.syncingState.collectAsState()
    val syncStatusMessage by viewModel.syncStatusMessage.collectAsState()
    val menuItems by viewModel.menuItems.collectAsState()

    // Screen State
    var showAddMomoDialog by remember { mutableStateOf(false) }
    var showClearWarning by remember { mutableStateOf(false) }
    var showLogoutWarning by remember { mutableStateOf(false) }
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }

    // Devices state (Owner only)
    var devices by remember { mutableStateOf<List<DeviceInfo>>(emptyList()) }
    var loadingDevices by remember { mutableStateOf(false) }
    var deviceError by remember { mutableStateOf<String?>(null) }

    val isOwner = userRole == UserRole.OWNER

    // Load registered devices when screen opens / syncCode changes
    fun refreshDevices() {
        if (isOwner && syncCode.isNotBlank()) {
            loadingDevices = true
            deviceError = null
            scope.launch {
                try {
                    val pulled = SyncManager.pullDevices(syncCode)
                    devices = pulled.sortedByDescending { it.registeredAt }
                } catch (e: Exception) {
                    deviceError = "Failed to load devices"
                } finally {
                    loadingDevices = false
                }
            }
        }
    }

    LaunchedEffect(syncCode, userRole) {
        refreshDevices()
    }

    // Group menu items by category
    val itemsByCategory = remember(menuItems) {
        menuItems.groupBy { it.category }
    }

    // Sort categories based on MenuDefaults or alphabetically
    val categoriesSorted = remember(itemsByCategory) {
        MenuDefaults.categories.filter { it in itemsByCategory.keys } +
                (itemsByCategory.keys - MenuDefaults.categories.toSet()).sorted()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Screen Header ---
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Configure connection, sync menu items, and manage local data.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        // --- Connection & Sync Status Section ---
        item {
            SectionHeader(title = "Connection & Sync Status")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Role and Phone number
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "App Access Role", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isOwner) MomosOrange.copy(alpha = 0.15f) else DarkSurfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = userRole.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isOwner) MomosOrange else TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Phone Number", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = userPhone.ifBlank { "Not configured" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = DarkSurfaceVariant, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Shop Sync Code Display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Shop Sync Code", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = syncCode.ifBlank { "No sync code" },
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Shop Sync Code", syncCode)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Sync Code copied!", Toast.LENGTH_SHORT).show()
                            },
                            enabled = syncCode.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copy Sync Code",
                                tint = if (syncCode.isNotBlank()) MomosOrange else TextMuted
                            )
                        }
                    }
                }
            }
        }

        // --- Menu Management Section ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(title = "Menu Management")
                if (isOwner) {
                    TextButton(
                        onClick = { showAddMomoDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MomosOrange)
                    ) {
                        Icon(imageVector = Icons.Rounded.Add, contentDescription = "Add Custom Momo", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Add Custom Momo", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // List categories using expandable cards
        categoriesSorted.forEach { category ->
            val items = itemsByCategory[category] ?: emptyList()
            val isExpanded = expandedCategories[category] != false // default expanded

            item(key = "cat_$category") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard)
                ) {
                    Column {
                        // Expandable Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedCategories[category] = !isExpanded }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkSurfaceVariant)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${items.size} Items",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Toggle Section",
                                tint = TextSecondary
                            )
                        }

                        // Category Items list
                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HorizontalDivider(color = DarkSurfaceVariant, thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(4.dp))
                                items.forEach { menuItem ->
                                    MenuSettingsItemRow(
                                        item = menuItem,
                                        isOwner = isOwner,
                                        onUpdate = { updatedItem ->
                                            viewModel.updateMenuItem(updatedItem)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Registered Devices Section (Owner Only) ---
        if (isOwner) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(title = "Registered Devices")
                    IconButton(onClick = { refreshDevices() }) {
                        if (loadingDevices) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MomosOrange)
                        } else {
                            Icon(imageVector = Icons.Rounded.Refresh, contentDescription = "Refresh", tint = MomosOrange)
                        }
                    }
                }
            }

            if (deviceError != null) {
                item {
                    Text(
                        text = deviceError!!,
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
            } else if (devices.isEmpty() && !loadingDevices) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard)
                    ) {
                        Text(
                            text = "No devices fetched. Please make sure Internet Sync is configured and tap Refresh.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(devices) { device ->
                    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
                    val regTimeString = remember(device.registeredAt) {
                        dateFormat.format(Date(device.registeredAt))
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCardElevated)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val displayNameText = if (device.userName.isNotBlank()) {
                                    "${device.userName} (${device.deviceName})"
                                } else {
                                    device.deviceName
                                }
                                Text(
                                    text = displayNameText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Phone: ${device.phoneNumber}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Registered: $regTimeString",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (device.role == UserRole.OWNER) MomosOrange.copy(alpha = 0.15f) else SuccessGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = device.role.displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (device.role == UserRole.OWNER) MomosOrange else SuccessGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Danger Zone Section (Owner Only) ---
        if (isOwner) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Danger Zone",
                            style = MaterialTheme.typography.titleMedium,
                            color = ErrorRed,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Actions here modify database states permanently.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Generate Sample Data
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Demo Mode", style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text(text = "Populate local DB with 30 days of sample sales.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            OutlinedButton(
                                onClick = {
                                    viewModel.loadSampleData()
                                    Toast.makeText(context, "Sample data loaded successfully!", Toast.LENGTH_SHORT).show()
                                },
                                border = BorderStroke(1.dp, TextSecondary),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                            ) {
                                Text("Generate Data", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = DarkSurfaceVariant, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Clear Local Data
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Clear Local Data", style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text(text = "Permanently wipe all local sales history.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Button(
                                onClick = { showClearWarning = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.White)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteForever,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear All", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                    }
                }
            }
        }

        // --- Session / Log Out Card (Visible to Everyone) ---
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Device Session",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Manage your login session on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Log Out", style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text(text = "Disconnect registration and lock app access.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = { showLogoutWarning = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkCardElevated, contentColor = ErrorRed),
                            border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Logout,
                                contentDescription = "Logout",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Log Out", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // Spacer at the bottom
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // --- Dialog: Add Custom Momo ---
    if (showAddMomoDialog) {
        AddMomoDialog(
            onDismiss = { showAddMomoDialog = false },
            onConfirm = { name, category, price ->
                viewModel.addMenuItem(
                    MenuItem(
                        name = name,
                        category = category,
                        price = price,
                        isAvailable = true
                    )
                )
                showAddMomoDialog = false
                Toast.makeText(context, "$name Added!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // --- Warning Dialog: Clear Local Data ---
    if (showClearWarning) {
        AlertDialog(
            onDismissRequest = { showClearWarning = false },
            containerColor = DarkCard,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = "Warning",
                    tint = ErrorRed,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Clear Local Data?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will permanently erase all local sales history. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllLocalData()
                        showClearWarning = false
                        Toast.makeText(context, "Local sales data cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearWarning = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Warning Dialog: Log Out ---
    if (showLogoutWarning) {
        AlertDialog(
            onDismissRequest = { showLogoutWarning = false },
            containerColor = DarkCard,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Logout,
                    contentDescription = "Logout",
                    tint = ErrorRed,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Confirm Log Out?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Logging out will remove device registration, clear sync configuration, and lock app access. You will be prompted to setup a PIN again.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logout()
                        showLogoutWarning = false
                        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Log Out")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutWarning = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MomosOrangeLight,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

@Composable
fun MenuSettingsItemRow(
    item: MenuItem,
    isOwner: Boolean,
    onUpdate: (MenuItem) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            if (!isOwner) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Price: ₹%.0f".format(item.price),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isOwner) {
                var priceText by remember(item.price) { mutableStateOf(item.price.toInt().toString()) }
                val focusManager = LocalFocusManager.current

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.all { it.isDigit() || it == '.' }) {
                            priceText = newValue
                        }
                    },
                    modifier = Modifier
                        .width(90.dp)
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused) {
                                val parsed = priceText.toDoubleOrNull()
                                if (parsed != null && parsed >= 0 && parsed != item.price) {
                                    onUpdate(item.copy(price = parsed))
                                } else if (parsed == null || parsed < 0) {
                                    priceText = item.price.toInt().toString()
                                }
                            }
                        },
                    singleLine = true,
                    prefix = { Text("₹", color = TextSecondary) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                        }
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MomosOrange,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }

            Switch(
                checked = item.isAvailable,
                onCheckedChange = { isChecked ->
                    onUpdate(item.copy(isAvailable = isChecked))
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MomosOrange,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = DarkSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun AddMomoDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, category: String, price: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(MenuDefaults.categories.first()) }
    var priceStr by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf(false) }
    var priceError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(
                text = "Add Custom Momo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MomosOrangeLight
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (nameError && it.isNotBlank()) nameError = false
                    },
                    label = { Text("Momo Name") },
                    placeholder = { Text("e.g. Cheese Corn Momo (8pc)", color = TextMuted) },
                    isError = nameError,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MomosOrange,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        errorBorderColor = ErrorRed,
                        focusedLabelColor = MomosOrange,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (nameError) {
                    Text(text = "Name cannot be empty", color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                }

                // Category Selector
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { dropdownExpanded = !dropdownExpanded }) {
                                    Icon(
                                        imageVector = if (dropdownExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = "Toggle Dropdown",
                                        tint = TextSecondary
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MomosOrange,
                                unfocusedBorderColor = DarkSurfaceVariant,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { dropdownExpanded = !dropdownExpanded }
                        )

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .background(DarkCardElevated)
                        ) {
                            MenuDefaults.categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category, color = TextPrimary) },
                                    onClick = {
                                        selectedCategory = category
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Price Field
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.all { it.isDigit() || it == '.' }) {
                            priceStr = newValue
                            if (priceError) priceError = false
                        }
                    },
                    label = { Text("Price") },
                    prefix = { Text("₹ ", color = TextSecondary) },
                    placeholder = { Text("0", color = TextMuted) },
                    isError = priceError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MomosOrange,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        errorBorderColor = ErrorRed,
                        focusedLabelColor = MomosOrange,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (priceError) {
                    Text(text = "Please enter a valid price", color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hasName = name.isNotBlank()
                    val priceVal = priceStr.toDoubleOrNull()
                    val hasPrice = priceVal != null && priceVal >= 0

                    if (!hasName) nameError = true
                    if (!hasPrice) priceError = true

                    if (hasName && hasPrice) {
                        onConfirm(name.trim(), selectedCategory, priceVal!!)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MomosOrange,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
            ) {
                Text("Cancel")
            }
        }
    )
}
