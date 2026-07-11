package com.example.momosshopmanager.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SalesRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("sales_data", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _sales = MutableStateFlow<List<Sale>>(emptyList())
    val sales: Flow<List<Sale>> = _sales.asStateFlow()

    private val _menuItems = MutableStateFlow(MenuDefaults.defaultMenu)
    val menuItems: Flow<List<MenuItem>> = _menuItems.asStateFlow()

    // Sync state flows for UI updates
    val _syncingState = MutableStateFlow(false)
    val _syncStatusMessage = MutableStateFlow("Idle")

    init {
        loadSales()
        loadMenu()
        SyncManager.databaseUrl = getDatabaseUrl()

        // Auto-detect database URL from google-services.json if using the default placeholder
        if (SyncManager.databaseUrl.contains("momos-shop-manager-default-rtdb")) {
            repositoryScope.launch {
                val detected = SyncManager.detectAndVerifyDatabaseUrl(context)
                if (detected != null) {
                    setDatabaseUrl(detected)
                }
            }
        }

        // Start background periodic sync loop
        repositoryScope.launch {
            while (true) {
                if (getSyncEnabled() && getSyncCode().isNotBlank()) {
                    try {
                        syncNow()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                delay(30000) // sync every 30 seconds
            }
        }
    }

    // --- Authentication & Settings Settings ---

    fun isRegistered(): Boolean = prefs.getBoolean("is_registered", false)
    fun setRegistered(value: Boolean) = prefs.edit().putBoolean("is_registered", value).apply()

    fun getUserRole(): UserRole {
        val roleStr = prefs.getString("user_role", UserRole.EMPLOYEE.name)
        return try { UserRole.valueOf(roleStr ?: UserRole.EMPLOYEE.name) } catch (e: Exception) { UserRole.EMPLOYEE }
    }
    fun setUserRole(role: UserRole) = prefs.edit().putString("user_role", role.name).apply()

    fun getUserPhone(): String = prefs.getString("user_phone", "") ?: ""
    fun setUserPhone(phone: String) = prefs.edit().putString("user_phone", phone).apply()

    fun getSyncCode(): String = prefs.getString("sync_code", "") ?: ""
    fun setSyncCode(code: String) = prefs.edit().putString("sync_code", code).apply()

    fun getDatabaseUrl(): String = prefs.getString("database_url", "https://shop-5b949-default-rtdb.asia-southeast1.firebasedatabase.app") ?: "https://shop-5b949-default-rtdb.asia-southeast1.firebasedatabase.app"
    fun setDatabaseUrl(url: String) {
        val cleanUrl = url.trim().removeSuffix("/")
        prefs.edit().putString("database_url", cleanUrl).apply()
        SyncManager.databaseUrl = cleanUrl
        
        // Trigger background sync immediately if sync is enabled
        if (getSyncEnabled() && getSyncCode().isNotBlank()) {
            repositoryScope.launch {
                syncNow()
            }
        }
    }

    fun getSyncEnabled(): Boolean = true
    fun setSyncEnabled(enabled: Boolean) {}

    fun getOwnerPin(): String = prefs.getString("owner_pin", "1234") ?: "1234"
    fun setOwnerPin(pin: String) = prefs.edit().putString("owner_pin", pin).apply()

    fun getUserPin(): String = prefs.getString("user_pin", "1234") ?: "1234"
    fun setUserPin(pin: String) = prefs.edit().putString("user_pin", pin).apply()

    fun getUserName(): String = prefs.getString("user_name", "") ?: ""
    fun setUserName(name: String) = prefs.edit().putString("user_name", name).apply()

    fun isAppLockedToday(): Boolean {
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val minute = now.get(Calendar.MINUTE)
        
        // Auto lock app after 11:15 PM (23:15)
        if (hour > 23 || (hour == 23 && minute >= 15)) {
            return true
        }
        
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastUnlock = prefs.getString("last_unlock_date", "")
        return lastUnlock != todayStr
    }

    fun unlockAppForToday() {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        prefs.edit().putString("last_unlock_date", todayStr).apply()
    }

    fun logout() {
        prefs.edit().clear().apply()
        _sales.value = emptyList()
        _menuItems.value = MenuDefaults.defaultMenu
        _syncStatusMessage.value = "Logged Out"
    }

    // --- Cloud Sync Implementation ---

    suspend fun syncNow(): Boolean {
        val code = getSyncCode()
        if (code.isBlank() || !getSyncEnabled()) {
            _syncStatusMessage.value = "Sync Disabled"
            return false
        }

        _syncingState.value = true
        _syncStatusMessage.value = "Syncing..."

        try {
            // 1. Pull sales from cloud
            val remoteSales = SyncManager.pullSales(code)
            val localSales = _sales.value

            // Combine local and remote without duplicates
            val mergedSales = (localSales + remoteSales).distinctBy { it.id }.sortedByDescending { it.timestamp }
            
            // Push any local sales that don't exist remotely
            val remoteIds = remoteSales.map { it.id }.toSet()
            val localOnly = localSales.filter { it.id !in remoteIds }
            
            var pushSucceeded = true
            localOnly.forEach { sale ->
                val ok = SyncManager.pushSale(code, sale)
                if (!ok) pushSucceeded = false
            }

            _sales.value = mergedSales
            saveSales()

            // 2. Pull menu from cloud
            val remoteMenu = SyncManager.pullMenu(code)
            if (remoteMenu != null && remoteMenu.isNotEmpty()) {
                _menuItems.value = remoteMenu
                saveMenu()
            } else {
                // If cloud menu is empty, push local menu
                SyncManager.pushMenu(code, _menuItems.value)
            }

            val timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            _syncStatusMessage.value = if (pushSucceeded) "Connected (Last: $timestamp)" else "Partial Sync (Last: $timestamp)"
            _syncingState.value = false
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            _syncStatusMessage.value = "Sync Error"
            _syncingState.value = false
            return false
        }
    }

    // --- Sales CRUD ---

    fun addSale(sale: Sale) {
        val userPhone = getUserPhone()
        val enrichedSale = if (sale.createdBy.isBlank()) sale.copy(createdBy = userPhone) else sale
        val current = _sales.value.toMutableList()
        current.add(0, enrichedSale)
        _sales.value = current
        saveSales()

        // Sync remote if enabled
        if (getSyncEnabled() && getSyncCode().isNotBlank()) {
            repositoryScope.launch {
                SyncManager.pushSale(getSyncCode(), enrichedSale)
                // Refresh status timestamp
                val timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                _syncStatusMessage.value = "Connected (Last: $timestamp)"
            }
        }
    }

    fun deleteSale(saleId: String) {
        val current = _sales.value.toMutableList()
        current.removeAll { it.id == saleId }
        _sales.value = current
        saveSales()

        // Sync remote delete if enabled
        if (getSyncEnabled() && getSyncCode().isNotBlank()) {
            repositoryScope.launch {
                SyncManager.deleteSale(getSyncCode(), saleId)
                val timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                _syncStatusMessage.value = "Connected (Last: $timestamp)"
            }
        }
    }

    fun clearAllLocalData() {
        _sales.value = emptyList()
        saveSales()
    }

    // --- Queries ---

    fun getTodaySales(): Flow<List<Sale>> = _sales.map { all ->
        val todayStart = getTodayStartMillis()
        all.filter { it.timestamp >= todayStart }
    }

    fun getMonthlySales(year: Int, month: Int): Flow<List<Sale>> = _sales.map { all ->
        val cal = Calendar.getInstance()
        all.filter {
            cal.timeInMillis = it.timestamp
            cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
        }
    }

    fun getSalesInRange(startMillis: Long, endMillis: Long): Flow<List<Sale>> = _sales.map { all ->
        all.filter { it.timestamp in startMillis..endMillis }
    }

    // --- Aggregations ---

    fun getTodayTotal(): Flow<Double> = getTodaySales().map { sales ->
        sales.sumOf { it.total }
    }

    fun getMonthlyTotal(year: Int, month: Int): Flow<Double> = getMonthlySales(year, month).map { sales ->
        sales.sumOf { it.total }
    }

    fun getAverageSaleValue(): Flow<Double> = _sales.map { all ->
        if (all.isEmpty()) 0.0 else all.sumOf { it.total } / all.size
    }

    fun getTodayOrderCount(): Flow<Int> = getTodaySales().map { it.size }

    fun getLast7DaysSales(): Flow<List<Pair<String, Double>>> = _sales.map { all ->
        val cal = Calendar.getInstance()
        val days = mutableListOf<Pair<String, Double>>()
        val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

        for (i in 6 downTo 0) {
            val dayCal = Calendar.getInstance()
            dayCal.add(Calendar.DAY_OF_YEAR, -i)
            val dayStart = getDayStartMillis(dayCal)
            val dayEnd = dayStart + 24 * 60 * 60 * 1000 - 1
            val dayTotal = all.filter { it.timestamp in dayStart..dayEnd }.sumOf { it.total }
            val dayName = dayNames[dayCal.get(Calendar.DAY_OF_WEEK) - 1]
            days.add(dayName to dayTotal)
        }
        days
    }

    fun getRecentSales(limit: Int = 10): Flow<List<Sale>> = _sales.map { all ->
        all.take(limit)
    }

    fun getBestSellingItems(limit: Int = 5): Flow<List<Pair<String, Int>>> = _sales.map { all ->
        all.flatMap { it.items }
            .groupBy { it.menuItem.name }
            .mapValues { (_, items) -> items.sumOf { it.quantity } }
            .entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key to it.value }
    }

    fun getPaymentMethodDistribution(): Flow<Map<PaymentMethod, Double>> = _sales.map { all ->
        if (all.isEmpty()) {
            PaymentMethod.entries.associateWith { 0.0 }
        } else {
            val total = all.sumOf { it.total }
            PaymentMethod.entries.associateWith { method ->
                val methodTotal = all.filter { it.paymentMethod == method }.sumOf { it.total }
                if (total > 0) (methodTotal / total) * 100.0 else 0.0
            }
        }
    }

    fun getDailyAverage(): Flow<Double> = _sales.map { all ->
        if (all.isEmpty()) return@map 0.0
        val cal = Calendar.getInstance()
        val dayTotals = all.groupBy { sale ->
            cal.timeInMillis = sale.timestamp
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
        }.values.map { daySales -> daySales.sumOf { it.total } }
        if (dayTotals.isEmpty()) 0.0 else dayTotals.average()
    }

    fun getWeeklyAverage(): Flow<Double> = _sales.map { all ->
        if (all.isEmpty()) return@map 0.0
        val cal = Calendar.getInstance()
        val weekTotals = all.groupBy { sale ->
            cal.timeInMillis = sale.timestamp
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.WEEK_OF_YEAR)}"
        }.values.map { weekSales -> weekSales.sumOf { it.total } }
        if (weekTotals.isEmpty()) 0.0 else weekTotals.average()
    }

    fun getMonthlyAverage(): Flow<Double> = _sales.map { all ->
        if (all.isEmpty()) return@map 0.0
        val cal = Calendar.getInstance()
        val monthTotals = all.groupBy { sale ->
            cal.timeInMillis = sale.timestamp
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}"
        }.values.map { monthSales -> monthSales.sumOf { it.total } }
        if (monthTotals.isEmpty()) 0.0 else monthTotals.average()
    }

    fun getPeakHours(): Flow<Map<String, Int>> = _sales.map { all ->
        val cal = Calendar.getInstance()
        val hourLabels = mapOf(
            "Afternoon" to (12..15),
            "Evening" to (16..19),
            "Night" to (20..23)
        )
        hourLabels.mapValues { (_, range) ->
            all.count { sale ->
                cal.timeInMillis = sale.timestamp
                cal.get(Calendar.HOUR_OF_DAY) in range
            }
        }
    }

    fun getDayBreakdown(year: Int, month: Int): Flow<List<Triple<Int, Int, Double>>> = getMonthlySales(year, month).map { sales ->
        val cal = Calendar.getInstance()
        sales.groupBy { sale ->
            cal.timeInMillis = sale.timestamp
            cal.get(Calendar.DAY_OF_MONTH)
        }.map { (day, daySales) ->
            Triple(day, daySales.size, daySales.sumOf { it.total })
        }.sortedBy { it.first }
    }

    // --- Menu Management ---

    fun updateMenuItem(item: MenuItem) {
        val current = _menuItems.value.toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index != -1) {
            current[index] = item
            _menuItems.value = current
            saveMenu()

            // Push updated menu to cloud
            if (getSyncEnabled() && getSyncCode().isNotBlank()) {
                repositoryScope.launch {
                    SyncManager.pushMenu(getSyncCode(), _menuItems.value)
                }
            }
        }
    }

    fun addMenuItem(item: MenuItem) {
        val current = _menuItems.value.toMutableList()
        current.add(item)
        _menuItems.value = current
        saveMenu()

        // Push updated menu to cloud
        if (getSyncEnabled() && getSyncCode().isNotBlank()) {
            repositoryScope.launch {
                SyncManager.pushMenu(getSyncCode(), _menuItems.value)
            }
        }
    }

    // --- Persistence ---

    private fun saveSales() {
        val data = json.encodeToString(_sales.value)
        prefs.edit().putString("sales", data).apply()
    }

    private fun loadSales() {
        val data = prefs.getString("sales", null)
        if (data != null) {
            try {
                _sales.value = json.decodeFromString(data)
            } catch (_: Exception) {
                _sales.value = emptyList()
            }
        }
    }

    private fun saveMenu() {
        val data = json.encodeToString(_menuItems.value)
        prefs.edit().putString("menu", data).apply()
    }

    private fun loadMenu() {
        val data = prefs.getString("menu", null)
        if (data != null) {
            try {
                _menuItems.value = json.decodeFromString(data)
            } catch (_: Exception) {
                _menuItems.value = MenuDefaults.defaultMenu
            }
        }
    }

    // --- Helpers ---

    private fun getTodayStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getDayStartMillis(calendar: Calendar): Long {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // --- Generate Sample Data for Demo ---
    fun generateSampleData() {
        val random = java.util.Random()
        val menu = _menuItems.value
        val sampleSales = _sales.value.toMutableList()

        for (daysAgo in 30 downTo 0) {
            val ordersToday = random.nextInt(8) + 3 // 3-10 orders per day
            for (orderIndex in 0 until ordersToday) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
                cal.set(Calendar.HOUR_OF_DAY, random.nextInt(14) + 8) // 8 AM to 10 PM
                cal.set(Calendar.MINUTE, random.nextInt(60))

                val itemCount = random.nextInt(3) + 1
                val items = mutableListOf<SaleItem>()
                for (i in 0 until itemCount) {
                    val item = menu[random.nextInt(menu.size)]
                    val qty = random.nextInt(2) + 1
                    items.add(SaleItem(menuItem = item, quantity = qty))
                }

                val payment = PaymentMethod.entries[random.nextInt(3)]
                sampleSales.add(
                    Sale(
                        timestamp = cal.timeInMillis,
                        items = items,
                        paymentMethod = payment,
                        customerName = if (random.nextBoolean()) "Customer ${random.nextInt(100)}" else ""
                    )
                )
            }
        }

        _sales.value = sampleSales.sortedByDescending { it.timestamp }
        saveSales()

        // Push menu and sales to cloud if syncing
        if (getSyncEnabled() && getSyncCode().isNotBlank()) {
            repositoryScope.launch {
                syncNow()
            }
        }
    }
}
