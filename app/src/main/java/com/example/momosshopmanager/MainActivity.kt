package com.example.momosshopmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.momosshopmanager.data.SalesViewModel
import com.example.momosshopmanager.data.UserRole
import com.example.momosshopmanager.theme.MomosShopManagerTheme
import com.example.momosshopmanager.ui.analytics.AnalyticsScreen
import com.example.momosshopmanager.ui.auth.AuthScreen
import com.example.momosshopmanager.ui.auth.LockScreen
import com.example.momosshopmanager.ui.dashboard.DashboardScreen
import com.example.momosshopmanager.ui.monthly.MonthlyScreen
import com.example.momosshopmanager.ui.today.TodayScreen
import com.example.momosshopmanager.ui.settings.SettingsScreen
import com.example.momosshopmanager.ui.resources.ResourcesScreen
import com.example.momosshopmanager.ui.expenses.ExpensesScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MomosShopManagerTheme {
                MomosAppEntry()
            }
        }
    }
}

enum class Screen(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Rounded.Home),
    TODAY("Today", Icons.Rounded.ReceiptLong),
    RESOURCES("Inventory", Icons.Rounded.Inventory),
    EXPENSES("Expenses", Icons.Rounded.Receipt),
    MONTHLY("Monthly", Icons.Rounded.CalendarMonth),
    ANALYTICS("Analytics", Icons.Rounded.Analytics),
    SETTINGS("Settings", Icons.Rounded.Settings)
}

@Composable
fun MomosAppEntry() {
    val viewModel: SalesViewModel = viewModel()
    val isRegistered by viewModel.isRegistered.collectAsState()
    val isAppLocked by viewModel.isAppLocked.collectAsState()

    when {
        !isRegistered -> {
            AuthScreen(viewModel = viewModel)
        }
        isAppLocked -> {
            LockScreen(viewModel = viewModel)
        }
        else -> {
            MomosAppContent(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomosAppContent(viewModel: SalesViewModel) {
    val userRole by viewModel.userRole.collectAsState()

    // Filter screens based on user role
    val screens = remember(userRole) {
        if (userRole == UserRole.OWNER) {
            Screen.entries
        } else {
            listOf(Screen.TODAY, Screen.RESOURCES, Screen.SETTINGS)
        }
    }

    var currentScreen by remember(userRole) {
        mutableStateOf(if (userRole == UserRole.OWNER) Screen.DASHBOARD else Screen.TODAY)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                screens.forEach { screen ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.label
                            )
                        },
                        label = { Text(screen.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    Screen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                    Screen.TODAY -> TodayScreen(viewModel = viewModel)
                    Screen.RESOURCES -> ResourcesScreen(viewModel = viewModel)
                    Screen.EXPENSES -> ExpensesScreen(viewModel = viewModel)
                    Screen.MONTHLY -> MonthlyScreen(viewModel = viewModel)
                    Screen.ANALYTICS -> AnalyticsScreen(viewModel = viewModel)
                    Screen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
