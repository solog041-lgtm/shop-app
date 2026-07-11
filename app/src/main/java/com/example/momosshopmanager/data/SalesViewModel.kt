package com.example.momosshopmanager.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SalesViewModel(application: Application) : AndroidViewModel(application) {

    val repository = SalesRepository(application)

    // Menu
    val menuItems: StateFlow<List<MenuItem>> = repository.menuItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard flows
    val todayTotal: StateFlow<Double> = repository.getTodayTotal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlyTotal: StateFlow<Double> = run {
        val cal = java.util.Calendar.getInstance()
        repository.getMonthlyTotal(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val averageSale: StateFlow<Double> = repository.getAverageSaleValue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayOrderCount: StateFlow<Int> = repository.getTodayOrderCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val last7DaysSales: StateFlow<List<Pair<String, Double>>> = repository.getLast7DaysSales()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSales: StateFlow<List<Sale>> = repository.getRecentSales(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Today
    val todaySales: StateFlow<List<Sale>> = repository.getTodaySales()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Analytics
    val bestSellers: StateFlow<List<Pair<String, Int>>> = repository.getBestSellingItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val paymentDistribution: StateFlow<Map<PaymentMethod, Double>> = repository.getPaymentMethodDistribution()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val dailyAvg: StateFlow<Double> = repository.getDailyAverage()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val weeklyAvg: StateFlow<Double> = repository.getWeeklyAverage()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlyAvg: StateFlow<Double> = repository.getMonthlyAverage()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val peakHours: StateFlow<Map<String, Int>> = repository.getPeakHours()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Monthly Settings
    private val _selectedYear = MutableStateFlow(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _selectedMonth = MutableStateFlow(java.util.Calendar.getInstance().get(java.util.Calendar.MONTH))
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    val selectedMonthSales: StateFlow<List<Sale>> = combine(_selectedYear, _selectedMonth) { y, m -> y to m }
        .flatMapLatest { (y, m) -> repository.getMonthlySales(y, m) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedMonthDayBreakdown: StateFlow<List<Triple<Int, Int, Double>>> =
        combine(_selectedYear, _selectedMonth) { y, m -> y to m }
            .flatMapLatest { (y, m) -> repository.getDayBreakdown(y, m) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val previousMonthTotal: StateFlow<Double> = combine(_selectedYear, _selectedMonth) { y, m ->
        if (m == 0) (y - 1) to 11 else y to (m - 1)
    }.flatMapLatest { (y, m) -> repository.getMonthlyTotal(y, m) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Sync State
    val syncingState: StateFlow<Boolean> = repository._syncingState.asStateFlow()
    val syncStatusMessage: StateFlow<String> = repository._syncStatusMessage.asStateFlow()

    // Auth & Locking States (MutableStateFlow to handle dynamic locking in session)
    private val _isRegistered = MutableStateFlow(repository.isRegistered())
    val isRegistered: StateFlow<Boolean> = _isRegistered.asStateFlow()

    private val _userRole = MutableStateFlow(repository.getUserRole())
    val userRole: StateFlow<UserRole> = _userRole.asStateFlow()

    private val _userPhone = MutableStateFlow(repository.getUserPhone())
    val userPhone: StateFlow<String> = _userPhone.asStateFlow()

    private val _syncCode = MutableStateFlow(repository.getSyncCode())
    val syncCode: StateFlow<String> = _syncCode.asStateFlow()

    private val _syncEnabled = MutableStateFlow(repository.getSyncEnabled())
    val syncEnabled: StateFlow<Boolean> = _syncEnabled.asStateFlow()

    private val _isAppLocked = MutableStateFlow(repository.isAppLockedToday())
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _databaseUrl = MutableStateFlow(repository.getDatabaseUrl())
    val databaseUrl: StateFlow<String> = _databaseUrl.asStateFlow()

    private val _activeAlert = MutableStateFlow<Alert?>(null)
    val activeAlert: StateFlow<Alert?> = _activeAlert.asStateFlow()

    init {
        // 1. Polling for Low Stock Alerts (Owners only)
        viewModelScope.launch {
            while (true) {
                if (_isRegistered.value && _userRole.value == UserRole.OWNER && _syncCode.value.isNotBlank()) {
                    try {
                        val alert = SyncManager.pullAlert(_syncCode.value)
                        if (alert != null) {
                            if (_activeAlert.value?.id != alert.id) {
                                playAlertSound()
                            }
                            _activeAlert.value = alert
                        } else {
                            _activeAlert.value = null
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                kotlinx.coroutines.delay(5000)
            }
        }

        // 2. Real-time Auto Lock verification (Locks app after 11:15 PM)
        viewModelScope.launch {
            while (true) {
                if (_isRegistered.value && !_isAppLocked.value) {
                    if (repository.isAppLockedToday()) {
                        _isAppLocked.value = true
                    }
                }
                kotlinx.coroutines.delay(10000)
            }
        }
    }

    private fun playAlertSound() {
        try {
            val notificationUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = android.media.RingtoneManager.getRingtone(getApplication(), notificationUri)
            ringtone.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun raiseLowStockAlert() {
        viewModelScope.launch {
            if (_syncCode.value.isNotBlank()) {
                val alert = Alert(
                    message = "Stock is getting over. Send more momos!",
                    senderName = repository.getUserName(),
                    senderPhone = repository.getUserPhone()
                )
                SyncManager.pushAlert(_syncCode.value, alert)
            }
        }
    }

    fun acknowledgeAlert() {
        viewModelScope.launch {
            if (_syncCode.value.isNotBlank()) {
                SyncManager.clearAlert(_syncCode.value)
                _activeAlert.value = null
            }
        }
    }

    // --- Authentication Actions ---

    fun registerDevice(
        dbUrl: String,
        code: String,
        phone: String,
        role: UserRole,
        pin: String,
        userName: String,
        ownerPasswordInput: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            repository._syncingState.value = true
            repository._syncStatusMessage.value = "Verifying..."
            
            // Set DB URL first so SyncManager resolves correctly
            repository.setDatabaseUrl(dbUrl)
            _databaseUrl.value = repository.getDatabaseUrl()

            // Check Gated Safety Protocol for Owner
            if (role == UserRole.OWNER) {
                val dbPassword = SyncManager.getOwnerPassword(code)
                if (dbPassword == null) {
                    if (ownerPasswordInput.isBlank()) {
                        repository._syncingState.value = false
                        onResult(false, "First Owner: Please define a new Owner Setup Password!")
                        return@launch
                    }
                    val created = SyncManager.setOwnerPassword(code, ownerPasswordInput.trim())
                    if (!created) {
                        repository._syncingState.value = false
                        onResult(false, "Failed to initialize Owner Setup Password in cloud.")
                        return@launch
                    }
                } else {
                    if (ownerPasswordInput.trim() != dbPassword) {
                        repository._syncingState.value = false
                        onResult(false, "Incorrect Owner Setup Password for this shop!")
                        return@launch
                    }
                }
            }

            // Register device to cloud
            val ok = SyncManager.registerDevice(code, phone, role, pin, userName)
            if (ok) {
                // Save settings locally
                repository.setSyncCode(code)
                repository.setUserPhone(phone)
                repository.setUserRole(role)
                repository.setUserPin(pin)
                repository.setUserName(userName)
                repository.setRegistered(true)
                repository.unlockAppForToday()

                // Update view model state
                _syncCode.value = code
                _userPhone.value = phone
                _userRole.value = role
                _syncEnabled.value = true
                _isRegistered.value = true
                _isAppLocked.value = false

                // Pull down any remote data
                repository.syncNow()

                onResult(true, "Registration Successful!")
            } else {
                repository._syncStatusMessage.value = "Registration Failed"
                onResult(false, "Failed to connect to server. Check your Sync Code & Network.")
            }
            repository._syncingState.value = false
        }
    }

    fun setDatabaseUrl(url: String) {
        repository.setDatabaseUrl(url)
        _databaseUrl.value = repository.getDatabaseUrl()
    }

    fun verifyDailyPin(pin: String): Boolean {
        val correctPin = repository.getUserPin()
        return if (pin == correctPin) {
            repository.unlockAppForToday()
            _isAppLocked.value = false
            true
        } else {
            false
        }
    }

    fun logout() {
        repository.logout()
        _isRegistered.value = false
        _userPhone.value = ""
        _userRole.value = UserRole.EMPLOYEE
        _syncCode.value = ""
        _syncEnabled.value = false
        _isAppLocked.value = true
    }

    // --- Sync Settings ---

    fun setSyncEnabled(enabled: Boolean) {
        repository.setSyncEnabled(enabled)
        _syncEnabled.value = enabled
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            repository.syncNow()
        }
    }

    // --- Menu Management ---

    fun updateMenuItem(item: MenuItem) {
        repository.updateMenuItem(item)
    }

    fun addMenuItem(item: MenuItem) {
        repository.addMenuItem(item)
    }

    fun clearAllLocalData() {
        repository.clearAllLocalData()
    }

    fun setSelectedMonth(year: Int, month: Int) {
        _selectedYear.value = year
        _selectedMonth.value = month
    }

    // --- Sales Actions ---

    fun addSale(sale: Sale) {
        repository.addSale(sale)
    }

    fun deleteSale(saleId: String) {
        repository.deleteSale(saleId)
    }

    fun loadSampleData() {
        repository.generateSampleData()
    }

    // --- Resources & Expenses flows ---
    val resources: StateFlow<List<ShopResource>> = repository.resources
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<Expense>> = repository.expenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalExpenses: StateFlow<Double> = repository.expenses
        .map { list -> list.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val netProfit: StateFlow<Double> = combine(monthlyTotal, totalExpenses) { sales, expensesVal ->
        sales - expensesVal
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // --- Resources Actions ---
    fun addResource(resource: ShopResource) {
        repository.addResource(resource)
    }

    fun deleteResource(resourceId: String) {
        repository.deleteResource(resourceId)
    }

    fun raiseResourceAlarm(resourceName: String) {
        viewModelScope.launch {
            if (_syncCode.value.isNotBlank()) {
                val alert = Alert(
                    message = "$resourceName is out of stock!",
                    senderName = repository.getUserName(),
                    senderPhone = repository.getUserPhone(),
                    resourceName = resourceName
                )
                SyncManager.pushAlert(_syncCode.value, alert)
            }
        }
    }

    // --- Expenses Actions ---
    fun addExpense(expense: Expense) {
        repository.addExpense(expense)
    }

    fun deleteExpense(expenseId: String) {
        repository.deleteExpense(expenseId)
    }
}
