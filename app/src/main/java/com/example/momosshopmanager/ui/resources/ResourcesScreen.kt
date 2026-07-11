package com.example.momosshopmanager.ui.resources

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momosshopmanager.data.SalesViewModel
import com.example.momosshopmanager.data.ShopResource
import com.example.momosshopmanager.data.UserRole
import com.example.momosshopmanager.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourcesScreen(viewModel: SalesViewModel) {
    val resources by viewModel.resources.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val isOwner = userRole == UserRole.OWNER
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedResource by remember { mutableStateOf<ShopResource?>(null) }

    // Dialog state variables
    var resourceName by remember { mutableStateOf("") }
    var resourceQty by remember { mutableStateOf("") }
    var resourceStatus by remember { mutableStateOf("IN_STOCK") }

    Scaffold(
        containerColor = DarkBackground,
        floatingActionButton = {
            if (isOwner) {
                FloatingActionButton(
                    onClick = {
                        resourceName = ""
                        resourceQty = ""
                        resourceStatus = "IN_STOCK"
                        showAddDialog = true
                    },
                    containerColor = MomosOrange,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = "Add Resource")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Header
            Text(
                text = "Inventory Resources",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = if (isOwner) {
                    "Manage raw materials and track availability."
                } else {
                    "View raw materials and ring the owner if something is empty."
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (resources.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Inventory,
                            contentDescription = "Empty",
                            tint = TextMuted,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No resources tracked yet",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (isOwner) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MomosOrange)
                            ) {
                                Text("Add Your First Resource")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(resources, key = { it.id }) { resource ->
                        ResourceCard(
                            resource = resource,
                            isOwner = isOwner,
                            onEdit = {
                                selectedResource = resource
                                resourceName = resource.name
                                resourceQty = resource.quantity
                                resourceStatus = resource.status
                                showEditDialog = true
                            },
                            onDelete = {
                                viewModel.deleteResource(resource.id)
                                Toast.makeText(context, "${resource.name} deleted", Toast.LENGTH_SHORT).show()
                            },
                            onAlert = {
                                viewModel.raiseResourceAlarm(resource.name)
                                Toast.makeText(context, "Stock alarm sent to Owner!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    // --- Dialogs ---

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = DarkSurface,
            title = { Text("Add Resource", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = resourceName,
                        onValueChange = { resourceName = it },
                        label = { Text("Resource Name", color = TextSecondary) },
                        placeholder = { Text("e.g. Red Chutney", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MomosOrange,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = resourceQty,
                        onValueChange = { resourceQty = it },
                        label = { Text("Quantity Description (Optional)", color = TextSecondary) },
                        placeholder = { Text("e.g. 10 kg, 5 Bottles", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MomosOrange,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Text("Initial Status", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("IN_STOCK" to "In Stock", "RUNNING_LOW" to "Low", "OUT_OF_STOCK" to "Empty").forEach { (status, label) ->
                            FilterChip(
                                selected = resourceStatus == status,
                                onClick = { resourceStatus = status },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MomosOrange,
                                    selectedLabelColor = Color.White,
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resourceName.isNotBlank()) {
                            val newResource = ShopResource(
                                name = resourceName.trim(),
                                quantity = resourceQty.trim(),
                                status = resourceStatus
                            )
                            viewModel.addResource(newResource)
                            showAddDialog = false
                            Toast.makeText(context, "Resource added successfully!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MomosOrange),
                    enabled = resourceName.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showEditDialog && selectedResource != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = DarkSurface,
            title = { Text("Edit Resource", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = resourceName,
                        onValueChange = { resourceName = it },
                        label = { Text("Resource Name", color = TextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MomosOrange,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = resourceQty,
                        onValueChange = { resourceQty = it },
                        label = { Text("Quantity Description", color = TextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MomosOrange,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Text("Availability Status", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("IN_STOCK" to "In Stock", "RUNNING_LOW" to "Low", "OUT_OF_STOCK" to "Empty").forEach { (status, label) ->
                            FilterChip(
                                selected = resourceStatus == status,
                                onClick = { resourceStatus = status },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MomosOrange,
                                    selectedLabelColor = Color.White,
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedResource?.let { original ->
                            val updated = original.copy(
                                name = resourceName.trim(),
                                quantity = resourceQty.trim(),
                                status = resourceStatus,
                                lastUpdated = System.currentTimeMillis()
                            )
                            viewModel.addResource(updated)
                            showEditDialog = false
                            Toast.makeText(context, "Resource updated!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MomosOrange),
                    enabled = resourceName.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun ResourceCard(
    resource: ShopResource,
    isOwner: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAlert: () -> Unit
) {
    val statusColor = when (resource.status) {
        "IN_STOCK" -> SuccessGreen
        "RUNNING_LOW" -> WarningAmber
        "OUT_OF_STOCK" -> ErrorRed
        else -> TextMuted
    }
    val statusText = when (resource.status) {
        "IN_STOCK" -> "In Stock"
        "RUNNING_LOW" -> "Running Low"
        "OUT_OF_STOCK" -> "Out of Stock"
        else -> "Unknown"
    }

    val updatedDate = remember(resource.lastUpdated) {
        SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(resource.lastUpdated))
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAlertConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = resource.name,
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusColor.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (resource.quantity.isNotBlank()) {
                    Text(
                        text = "Quantity: ${resource.quantity}",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = "Updated: $updatedDate",
                    color = TextMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isOwner) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Rounded.Edit, contentDescription = "Edit", tint = MomosOrange)
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(imageVector = Icons.Rounded.Delete, contentDescription = "Delete", tint = ErrorRed)
                    }
                }
            } else {
                // Employee Empty stock alert warning ring button!
                Button(
                    onClick = { showAlertConfirm = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed.copy(alpha = 0.15f),
                        contentColor = ErrorRed
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = "Alert Owner",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Alert Empty", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = DarkSurface,
            title = { Text("Delete Resource", color = TextPrimary) },
            text = { Text("Are you sure you want to delete ${resource.name}?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showAlertConfirm) {
        AlertDialog(
            onDismissRequest = { showAlertConfirm = false },
            containerColor = DarkSurface,
            title = { Text("Alert Owner", color = TextPrimary) },
            text = { Text("Ring the Owner that ${resource.name} is running out of stock?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        onAlert()
                        showAlertConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Ring Owner")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAlertConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
