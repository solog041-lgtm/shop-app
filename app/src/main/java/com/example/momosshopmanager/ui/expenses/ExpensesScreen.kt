package com.example.momosshopmanager.ui.expenses

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momosshopmanager.data.Expense
import com.example.momosshopmanager.data.SalesViewModel
import com.example.momosshopmanager.data.UserRole
import com.example.momosshopmanager.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(viewModel: SalesViewModel) {
    val expenses by viewModel.expenses.collectAsState()
    val totalExpenses by viewModel.totalExpenses.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val isOwner = userRole == UserRole.OWNER
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    
    // Form fields state
    var expenseAmount by remember { mutableStateOf("") }
    var expenseCategory by remember { mutableStateOf("Ingredients") }
    var expenseDesc by remember { mutableStateOf("") }

    val categories = listOf("Ingredients", "Wages", "Gas", "Rent", "Other")

    Scaffold(
        containerColor = DarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    expenseAmount = ""
                    expenseCategory = "Ingredients"
                    expenseDesc = ""
                    showAddDialog = true
                },
                containerColor = MomosOrange,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = "Add Expense")
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
                text = "Expenses Log",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Track raw material purchases, utilities, wages, and other costs.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Total Expenses Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Total Logged Expenses",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹%,.2f".format(totalExpenses),
                        color = ChiliRedLight,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (expenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Receipt,
                            contentDescription = "Empty",
                            tint = TextMuted,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No expenses recorded yet",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(expenses.sortedByDescending { it.timestamp }, key = { it.id }) { expense ->
                        ExpenseCard(
                            expense = expense,
                            isOwner = isOwner,
                            onDelete = {
                                viewModel.deleteExpense(expense.id)
                                Toast.makeText(context, "Expense deleted", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = DarkSurface,
            title = { Text("Log Expense", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = expenseAmount,
                        onValueChange = { expenseAmount = it },
                        label = { Text("Amount (₹)", color = TextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MomosOrange,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    
                    Text("Category", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.take(3).forEach { cat ->
                            FilterChip(
                                selected = expenseCategory == cat,
                                onClick = { expenseCategory = cat },
                                label = { Text(cat, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MomosOrange,
                                    selectedLabelColor = Color.White,
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.drop(3).forEach { cat ->
                            FilterChip(
                                selected = expenseCategory == cat,
                                onClick = { expenseCategory = cat },
                                label = { Text(cat, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MomosOrange,
                                    selectedLabelColor = Color.White,
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = expenseDesc,
                        onValueChange = { expenseDesc = it },
                        label = { Text("Description / Notes", color = TextSecondary) },
                        placeholder = { Text("e.g. Bought vegetable supply, Gas Cylinders", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MomosOrange,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountVal = expenseAmount.toDoubleOrNull()
                        if (amountVal != null && amountVal > 0) {
                            val newExpense = Expense(
                                amount = amountVal,
                                category = expenseCategory,
                                description = expenseDesc.trim(),
                                recordedBy = if (isOwner) "Owner" else "Employee"
                            )
                            viewModel.addExpense(newExpense)
                            showAddDialog = false
                            Toast.makeText(context, "Expense logged successfully!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MomosOrange),
                    enabled = expenseAmount.toDoubleOrNull() != null
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun ExpenseCard(
    expense: Expense,
    isOwner: Boolean,
    onDelete: () -> Unit
) {
    val categoryColor = when (expense.category) {
        "Ingredients" -> SuccessGreen
        "Wages" -> ChartBlue
        "Gas" -> MomosOrange
        "Rent" -> ChartPurple
        else -> TextSecondary
    }

    val expenseDate = remember(expense.timestamp) {
        SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(expense.timestamp))
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(categoryColor.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = expense.category,
                            color = categoryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "By ${expense.recordedBy}",
                        color = TextMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (expense.description.isNotBlank()) {
                    Text(
                        text = expense.description,
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = expenseDate,
                    color = TextMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "₹%,.0f".format(expense.amount),
                    color = ChiliRedLight,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                if (isOwner) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(imageVector = Icons.Rounded.Delete, contentDescription = "Delete", tint = ErrorRed, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = DarkSurface,
            title = { Text("Delete Log", color = TextPrimary) },
            text = { Text("Delete this expense entry of ₹${expense.amount}?", color = TextSecondary) },
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
}
