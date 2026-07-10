package com.example.momosshopmanager

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey
@Serializable data object DashboardDest : NavKey
@Serializable data object TodayDest : NavKey
@Serializable data object MonthlyDest : NavKey
@Serializable data object AnalyticsDest : NavKey
