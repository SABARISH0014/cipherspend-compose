package com.example.cipherspend.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cipherspend.data.dao.CategorySpend
import com.example.cipherspend.data.db.DatabaseProvider
import com.example.cipherspend.data.model.AppNotificationEntity
import com.example.cipherspend.data.model.IgnoredHashEntity
import com.example.cipherspend.data.model.TransactionEntity
import com.example.cipherspend.service.PredictionService
import com.example.cipherspend.service.SmsParser
import com.example.cipherspend.service.SpendingForecast
import com.example.cipherspend.service.getSalaryCycleBounds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId
import java.util.Calendar

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val db get() = DatabaseProvider.getDatabase(getApplication())
    private val predictionService = PredictionService.getInstance(application)

    private val _dbEpoch = MutableStateFlow(0)

    fun onDatabaseReset() {
        _dbEpoch.value++
        refreshForecast()
    }

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    private val _startDate = MutableStateFlow<Long?>(null)
    val startDate: StateFlow<Long?> = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow<Long?>(null)
    val endDate: StateFlow<Long?> = _endDate.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow<String?>(null)
    val selectedTypeFilter: StateFlow<String?> = _selectedTypeFilter.asStateFlow()

    private val _forecast = MutableStateFlow<SpendingForecast?>(null)
    val forecast: StateFlow<SpendingForecast?> = _forecast.asStateFlow()

    private val generalPrefs = application.getSharedPreferences("CipherSpendPrefs", android.content.Context.MODE_PRIVATE)

    private val _isDemoMode = MutableStateFlow(generalPrefs.getBoolean("is_demo_mode", false))

    @OptIn(ExperimentalCoroutinesApi::class)
    private val rawTransactions = _dbEpoch.flatMapLatest {
        db.transactionDao().getAllTransactionsFlow()
    }

    val isDemoMode: StateFlow<Boolean> = combine(_isDemoMode, rawTransactions) { flag, list ->
        flag || list.any { it.sender == "DEMO_MODE" || it.hash.startsWith("DEMO_") }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val transactions: StateFlow<List<TransactionEntity>> = combine(
        rawTransactions,
        _searchQuery,
        _selectedCategoryFilter,
        _selectedMonth
    ) { list, query, cat, ym ->
        val cycleDay = generalPrefs.getInt("salary_cycle_day", 1)
        val (start, end) = getSalaryCycleBounds(ym, cycleDay)
        var filtered = list.filter { it.timestamp in start until end }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            filtered = filtered.filter {
                it.merchant.lowercase().contains(q) ||
                it.body.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.sender.lowercase().contains(q)
            }
        }
        if (cat != null && cat != "All") {
            filtered = filtered.filter { it.category.equals(cat, ignoreCase = true) }
        }
        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchResults: StateFlow<List<TransactionEntity>> = combine(
        rawTransactions,
        _searchQuery,
        _selectedCategoryFilter,
        combine(_selectedTypeFilter, _startDate, _endDate) { type, start, end ->
            Triple(type, start, end)
        }
    ) { list, query, cat, (type, start, end) ->
        var filtered = list

        if (start != null) {
            filtered = filtered.filter { it.timestamp >= start }
        }
        if (end != null) {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = end
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val endOfDayMillis = calendar.timeInMillis
            filtered = filtered.filter { it.timestamp <= endOfDayMillis }
        }

        if (!type.isNullOrBlank() && type != "All") {
            filtered = filtered.filter { it.type.equals(type, ignoreCase = true) }
        }

        if (!cat.isNullOrBlank() && cat != "All") {
            filtered = filtered.filter { it.category.equals(cat, ignoreCase = true) }
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            filtered = filtered.filter {
                it.merchant.lowercase().contains(q) ||
                it.body.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.sender.lowercase().contains(q)
            }
        }

        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionEntity>> = rawTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val notifications: StateFlow<List<AppNotificationEntity>> = _dbEpoch.flatMapLatest {
        db.appNotificationDao().getAllNotificationsFlow()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshForecast()
        viewModelScope.launch {
            rawTransactions.collect {
                refreshForecast()
            }
        }
    }

    fun selectPreviousMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
        refreshForecast()
    }

    fun selectNextMonth() {
        _selectedMonth.value = _selectedMonth.value.plusMonths(1)
        refreshForecast()
    }

    fun selectMonth(yearMonth: YearMonth) {
        _selectedMonth.value = yearMonth
        refreshForecast()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategoryFilterChanged(category: String?) {
        _selectedCategoryFilter.value = category
    }

    fun setDateRange(start: Long?, end: Long?) {
        _startDate.value = start
        _endDate.value = end
    }

    fun setTypeFilter(type: String?) {
        _selectedTypeFilter.value = type
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedCategoryFilter.value = null
        _selectedTypeFilter.value = null
        _startDate.value = null
        _endDate.value = null
    }

    fun syncDemoModeState() {
        _isDemoMode.value = generalPrefs.getBoolean("is_demo_mode", false)
    }

    fun refreshForecast() {
        syncDemoModeState()
        viewModelScope.launch(Dispatchers.IO) {
            val fc = predictionService.getSpendingForecast(_selectedMonth.value)
            _forecast.value = fc
        }
    }

    fun insertManualTransaction(
        amount: Double,
        merchant: String,
        category: String,
        type: String = "Manual",
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanMerchant = merchant.ifBlank { "Manual Entry" }
            val cleanCat = category.ifBlank { "Others" }
            val hash = SmsParser.generateHash("MANUAL", "$cleanMerchant$amount", timestamp)
            val entity = TransactionEntity(
                hash = hash,
                sender = "MANUAL",
                body = "Manual transaction at $cleanMerchant of ₹$amount",
                amount = amount,
                category = cleanCat,
                type = type,
                merchant = cleanMerchant,
                timestamp = timestamp
            )
            db.transactionDao().insert(entity)
            predictionService.checkSpendingLimits()
            refreshForecast()
        }
    }

    /**
     * Error Prevention (One-Tap Corrections):
     * Updates the merchant name in Room across matching messages.
     */
    fun updateMerchantCorrection(transactionId: Long, newMerchant: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = db.transactionDao().getTransactionById(transactionId) ?: return@launch
            val cleanNewMerchant = newMerchant.trim()
            val oldMerchant = existing.merchant
            val updated = existing.copy(merchant = cleanNewMerchant)
            db.transactionDao().update(updated)
            db.transactionDao().updateMerchantByBodyAndSender(
                body = existing.body,
                sender = existing.sender,
                merchant = cleanNewMerchant
            )
            if (oldMerchant.isNotBlank()) {
                db.transactionDao().updateMerchantGlobally(
                    oldMerchant = oldMerchant,
                    newMerchant = cleanNewMerchant
                )
                val overridePrefs = getApplication<Application>().getSharedPreferences("cipher_merchant_overrides", android.content.Context.MODE_PRIVATE)
                overridePrefs.edit().putString(oldMerchant.lowercase(), cleanNewMerchant).apply()
            }
            refreshForecast()
        }
    }

    fun loadDemoTransactions() {
        viewModelScope.launch(Dispatchers.IO) {
            _isDemoMode.value = true
            generalPrefs.edit().putBoolean("is_demo_mode", true).apply()

            val now = System.currentTimeMillis()
            val dayMillis = 86_400_000L

            val demoList = listOf(
                TransactionEntity(
                    hash = "DEMO_swiggy",
                    sender = "HDFC ••6334",
                    body = "Paid Rs 420.00 to Swiggy via UPI",
                    amount = 420.0,
                    category = "Food & Dining",
                    type = "UPI",
                    merchant = "Swiggy",
                    timestamp = now - (1 * dayMillis)
                ),
                TransactionEntity(
                    hash = "DEMO_amazon",
                    sender = "SBI ••1209",
                    body = "Spent Rs 1899.00 on Amazon with Card ending 4412",
                    amount = 1899.0,
                    category = "Shopping",
                    type = "Card",
                    merchant = "Amazon",
                    timestamp = now - (2 * dayMillis)
                ),
                TransactionEntity(
                    hash = "DEMO_uber",
                    sender = "ICICI ••4510",
                    body = "Paid Rs 265.00 to Uber via UPI",
                    amount = 265.0,
                    category = "Travel",
                    type = "UPI",
                    merchant = "Uber",
                    timestamp = now - (3 * dayMillis)
                ),
                TransactionEntity(
                    hash = "DEMO_airtel",
                    sender = "Axis ••8841",
                    body = "Paid Rs 999.00 to Airtel Broadband via UPI",
                    amount = 999.0,
                    category = "Bills & Utilities",
                    type = "UPI",
                    merchant = "Airtel Broadband",
                    timestamp = now - (4 * dayMillis)
                ),
                TransactionEntity(
                    hash = "DEMO_zerodha",
                    sender = "Kotak ••3321",
                    body = "Transferred Rs 5000.00 to Zerodha SIP via Bank Transfer",
                    amount = 5000.0,
                    category = "Investment",
                    type = "Bank Transfer",
                    merchant = "Zerodha SIP",
                    timestamp = now - (5 * dayMillis)
                ),
                TransactionEntity(
                    hash = "DEMO_starbucks",
                    sender = "AMEX ••1004",
                    body = "Spent Rs 380.00 at Starbucks with Card ending 8911",
                    amount = 380.0,
                    category = "Food & Dining",
                    type = "Card",
                    merchant = "Starbucks",
                    timestamp = now - (6 * dayMillis)
                ),
                TransactionEntity(
                    hash = "DEMO_zomato",
                    sender = "OneCard ••9215",
                    body = "Paid Rs 540.00 to Zomato via UPI",
                    amount = 540.0,
                    category = "Food & Dining",
                    type = "UPI",
                    merchant = "Zomato",
                    timestamp = now - (7 * dayMillis)
                ),
                TransactionEntity(
                    hash = "DEMO_hpcl",
                    sender = "HDFC ••6334",
                    body = "Spent Rs 1200.00 at HPCL Fuel with Card ending 4412",
                    amount = 1200.0,
                    category = "Travel",
                    type = "Card",
                    merchant = "HPCL Fuel",
                    timestamp = now - (8 * dayMillis)
                )
            )

            db.transactionDao().insertAll(demoList)
            refreshForecast()
        }
    }

    fun exitDemoMode() {
        viewModelScope.launch(Dispatchers.IO) {
            _isDemoMode.value = false
            generalPrefs.edit().putBoolean("is_demo_mode", false).apply()
            db.transactionDao().deleteDemoTransactions()
            refreshForecast()
        }
    }

    fun updateTransactionCategory(transactionId: Long, newCategory: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = db.transactionDao().getTransactionById(transactionId) ?: return@launch
            val updated = existing.copy(category = newCategory.trim())
            db.transactionDao().update(updated)
            refreshForecast()
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            db.ignoredHashDao().insertIgnoredHash(IgnoredHashEntity(transaction.hash))
            db.transactionDao().delete(transaction)
            refreshForecast()
        }
    }

    fun restoreTransaction(transaction: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            db.ignoredHashDao().removeIgnoredHash(transaction.hash)
            db.transactionDao().insert(transaction)
            refreshForecast()
        }
    }

    fun deleteNotification(notification: AppNotificationEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            db.appNotificationDao().deleteNotification(notification)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            db.appNotificationDao().clearAllNotifications()
        }
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            db.transactionDao().deleteAll()
            refreshForecast()
        }
    }

    fun setMonthlyBudget(newBudget: Float) {
        val prefs = getApplication<Application>().getSharedPreferences("CipherSpendPrefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putFloat("monthly_budget", newBudget).apply()
        _dbEpoch.value++
        refreshForecast()
    }

    suspend fun getTransactionById(id: Long): TransactionEntity? {
        return db.transactionDao().getTransactionById(id)
    }
}
