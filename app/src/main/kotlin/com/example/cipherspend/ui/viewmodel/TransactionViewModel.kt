package com.example.cipherspend.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cipherspend.data.dao.CategorySpend
import com.example.cipherspend.data.db.DatabaseProvider
import com.example.cipherspend.data.model.AppNotificationEntity
import com.example.cipherspend.data.model.TransactionEntity
import com.example.cipherspend.service.PredictionService
import com.example.cipherspend.service.SmsParser
import com.example.cipherspend.service.SpendingForecast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val db = DatabaseProvider.getDatabase(application)
    private val predictionService = PredictionService.getInstance(application)

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    private val _forecast = MutableStateFlow<SpendingForecast?>(null)
    val forecast: StateFlow<SpendingForecast?> = _forecast.asStateFlow()

    private val rawTransactions = db.transactionDao().getAllTransactionsFlow()

    val transactions: StateFlow<List<TransactionEntity>> = combine(
        rawTransactions,
        _searchQuery,
        _selectedCategoryFilter,
        _selectedMonth
    ) { list, query, cat, ym ->
        val start = ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = ym.plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
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

    val allTransactions: StateFlow<List<TransactionEntity>> = rawTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<AppNotificationEntity>> = db.appNotificationDao()
        .getAllNotificationsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshForecast()
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

    fun refreshForecast() {
        viewModelScope.launch(Dispatchers.IO) {
            val fc = predictionService.getSpendingForecast(_selectedMonth.value)
            _forecast.value = fc
        }
    }

    fun insertManualTransaction(
        amount: Double,
        merchant: String,
        category: String,
        type: String = "Manual"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val cleanMerchant = merchant.ifBlank { "Manual Entry" }
            val cleanCat = category.ifBlank { "Others" }
            val hash = SmsParser.generateHash("MANUAL", "$cleanMerchant$amount", now)
            val entity = TransactionEntity(
                hash = hash,
                sender = "MANUAL",
                body = "Manual transaction at $cleanMerchant of ₹$amount",
                amount = amount,
                category = cleanCat,
                type = type,
                merchant = cleanMerchant,
                timestamp = now
            )
            db.transactionDao().insert(entity)
            predictionService.checkSpendingLimits()
            refreshForecast()
        }
    }

    /**
     * Error Prevention (One-Tap Corrections):
     * Updates the merchant name in Room and synthesizes regex pattern silently in background.
     */
    fun updateMerchantCorrection(transactionId: Long, newMerchant: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = db.transactionDao().getTransactionById(transactionId) ?: return@launch
            val updated = existing.copy(merchant = newMerchant.trim())
            db.transactionDao().update(updated)

            // Synthesize regex rule silently in the background
            val rule = SmsParser.synthesizeRuleForCorrection(
                sender = existing.sender,
                body = existing.body,
                correctedMerchant = newMerchant.trim()
            )
            db.customRuleDao().insertRule(rule)
            refreshForecast()
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            db.transactionDao().delete(transaction)
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
        refreshForecast()
    }

    suspend fun getTransactionById(id: Long): TransactionEntity? {
        return db.transactionDao().getTransactionById(id)
    }
}
