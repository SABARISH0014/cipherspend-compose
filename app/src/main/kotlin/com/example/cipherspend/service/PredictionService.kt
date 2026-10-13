package com.example.cipherspend.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.cipherspend.data.db.DatabaseProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.Calendar
import java.time.YearMonth
import java.time.ZoneId

data class SpendingForecast(
    val budget: Double,
    val spent: Double,
    val projected: Double,
    val dailyAverage: Double,
    val progress: Float,
    val topCategory: String,
    val topCategoryAmount: Double
)

class PredictionService private constructor(private val context: Context) {
    private val inferenceLock = Any()
    private var interpreter: Interpreter? = null
    private var modelByteBuffer: ByteBuffer? = null
    private var cachedInputBuffer: ByteBuffer? = null
    private var cachedOutputBuffer: Array<FloatArray>? = null
    private var vocab: Map<String, Int> = emptyMap()
    private var labels: List<String> = emptyList()
    private var sequenceLength: Int = 20

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            // Read TFLite model completely into an in-memory direct ByteBuffer
            // This prevents file descriptor closure, APK offset bugs, and GC unmapping crashes
            val modelBytes = context.assets.open("sms_model.tflite").use { it.readBytes() }
            val buffer = ByteBuffer.allocateDirect(modelBytes.size).apply {
                order(ByteOrder.nativeOrder())
                put(modelBytes)
                rewind()
            }
            modelByteBuffer = buffer

            val options = Interpreter.Options().apply {
                setNumThreads(2)
            }
            interpreter = Interpreter(buffer, options)

            // Dynamic input & output shape detection
            val inputShape = interpreter?.getInputTensor(0)?.shape()
            if (inputShape != null && inputShape.size > 1) {
                sequenceLength = inputShape[1]
            }
            val outputShape = interpreter?.getOutputTensor(0)?.shape()
            val numClasses = if (outputShape != null && outputShape.size > 1) outputShape[1] else 11

            // Pre-allocate cached direct input and output buffers to eliminate repeated allocations
            cachedInputBuffer = ByteBuffer.allocateDirect(4 * sequenceLength).order(ByteOrder.nativeOrder())
            cachedOutputBuffer = Array(1) { FloatArray(numClasses) }

            // Load Vocab JSON
            val vocabString = context.assets.open("vocab.json").bufferedReader().use { it.readText() }
            val mapType = object : TypeToken<Map<String, Int>>() {}.type
            vocab = Gson().fromJson(vocabString, mapType)

            // Load Labels JSON (1-based in labels.json, index 0 is baseline/Others)
            val labelsString = context.assets.open("labels.json").bufferedReader().use { it.readText() }
            val labelMapType = object : TypeToken<Map<String, String>>() {}.type
            val labelMap: Map<String, String> = Gson().fromJson(labelsString, labelMapType)
            val sortedList = ArrayList<String>(numClasses)
            for (i in 0 until numClasses) {
                val rawLabel = labelMap[i.toString()] ?: if (i == 0) "Others" else "Uncategorized"
                val formattedLabel = when (rawLabel.lowercase()) {
                    "food" -> "Food & Dining"
                    "shopping" -> "Shopping"
                    "bills" -> "Bills & Utilities"
                    "travel" -> "Travel"
                    "investment" -> "Investment"
                    "transaction" -> "Transaction"
                    "refund" -> "Refund"
                    "cash" -> "Cash"
                    "spam" -> "Spam"
                    "ignore" -> "Ignore"
                    else -> "Others"
                }
                sortedList.add(formattedLabel)
            }
            labels = sortedList
            Log.d("PredictionService", "TFLite Model loaded successfully. Input length: $sequenceLength, Classes: ${labels.size} -> $labels")
        } catch (e: Exception) {
            Log.e("PredictionService", "Failed to initialize TFLite model: ${e.message}", e)
        }
    }

    fun predictCategory(text: String): String = synchronized(inferenceLock) {
        val activeInterpreter = interpreter ?: return fallbackCategorize(text)
        if (labels.isEmpty()) return fallbackCategorize(text)

        try {
            val inputFeatures = tokenizeAndPad(text, sequenceLength)
            val inBuf = cachedInputBuffer ?: ByteBuffer.allocateDirect(4 * sequenceLength).order(ByteOrder.nativeOrder())
            val outBuf = cachedOutputBuffer ?: Array(1) { FloatArray(labels.size) }

            inBuf.clear()
            for (v in inputFeatures) {
                inBuf.putFloat(v)
            }
            inBuf.rewind()

            activeInterpreter.run(inBuf, outBuf)

            val probabilities = outBuf[0]
            var bestIdx = 0
            var maxProb = -1.0f
            for (i in probabilities.indices) {
                if (probabilities[i] > maxProb) {
                    maxProb = probabilities[i]
                    bestIdx = i
                }
            }

            val predicted = if (bestIdx in labels.indices) labels[bestIdx] else "Others"
            val result = if (predicted == "Others" || maxProb < 0.20f) {
                val fallback = fallbackCategorize(text)
                if (fallback != "Others") fallback else predicted
            } else {
                predicted
            }
            return result
        } catch (e: Throwable) {
            Log.e("PredictionService", "Error during inference: ${e.message}")
            return fallbackCategorize(text)
        }
    }

    private fun fallbackCategorize(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("food") || lower.contains("swiggy") || lower.contains("zomato") ||
                    lower.contains("cafe") || lower.contains("restaurant") || lower.contains("dine") ||
                    lower.contains("pizza") || lower.contains("burger") || lower.contains("starbucks") ||
                    lower.contains("blinkit") || lower.contains("zepto") || lower.contains("instamart") -> "Food & Dining"

            lower.contains("uber") || lower.contains("ola") || lower.contains("rapido") ||
                    lower.contains("irctc") || lower.contains("flight") || lower.contains("fuel") ||
                    lower.contains("petrol") || lower.contains("metro") || lower.contains("travel") -> "Travel"

            lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") ||
                    lower.contains("zara") || lower.contains("mall") || lower.contains("retail") ||
                    lower.contains("shop") -> "Shopping"

            lower.contains("bill") || lower.contains("recharge") || lower.contains("electricity") ||
                    lower.contains("airtel") || lower.contains("jio") || lower.contains("broadband") ||
                    lower.contains("water") || lower.contains("gas") -> "Bills & Utilities"

            lower.contains("zerodha") || lower.contains("groww") || lower.contains("mutual") ||
                    lower.contains("stock") || lower.contains("sip") || lower.contains("invest") -> "Investment"

            lower.contains("upi") || lower.contains("transfer") || lower.contains("sent") ||
                    lower.contains("paid") -> "Transaction"

            else -> "Others"
        }
    }

    private fun tokenizeAndPad(text: String, targetLength: Int): FloatArray {
        val clean = text.lowercase().replace(Regex("[^a-zA-Z0-9\\s]"), "")
        val words = clean.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val sequence = FloatArray(targetLength) { 0.0f }
        val oovToken = vocab["<OOV>"]?.toFloat() ?: 1.0f
        val maxVocabSize = 6000f

        for (i in 0 until minOf(targetLength, words.size)) {
            val word = words[i]
            val token = vocab[word]?.toFloat() ?: oovToken
            sequence[i] = if (token >= 0f && token < maxVocabSize) token else oovToken
        }
        return sequence
    }

    suspend fun checkSpendingLimits(currentSpentOffset: Double = 0.0) {
        val prefs = context.getSharedPreferences("CipherSpendPrefs", Context.MODE_PRIVATE)
        val budget = prefs.getFloat("monthly_budget", 25000f).toDouble()
        if (budget <= 0) return

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTimestamp = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        val endTimestamp = calendar.timeInMillis

        val db = DatabaseProvider.getDatabase(context)
        val spent = (db.transactionDao().getMonthlySpend(startTimestamp, endTimestamp) ?: 0.0) + currentSpentOffset

        val ratio = spent / budget
        val lastAlertPercent = prefs.getInt("last_alert_percent", 0)

        if (ratio >= 1.0 && lastAlertPercent < 100) {
            AlertNotificationManager.sendSpendingAlert(context, 100, spent, budget)
            prefs.edit().putInt("last_alert_percent", 100).apply()
        } else if (ratio >= 0.8 && ratio < 1.0 && lastAlertPercent < 80) {
            AlertNotificationManager.sendSpendingAlert(context, 80, spent, budget)
            prefs.edit().putInt("last_alert_percent", 80).apply()
        } else if (ratio < 0.8 && lastAlertPercent != 0) {
            prefs.edit().putInt("last_alert_percent", 0).apply()
        }
    }

    suspend fun getSpendingForecast(targetMonth: YearMonth? = null): SpendingForecast {
        val prefs = context.getSharedPreferences("CipherSpendPrefs", Context.MODE_PRIVATE)
        val budget = prefs.getFloat("monthly_budget", 25000f).toDouble()

        val currentYm = YearMonth.now()
        val ym = targetMonth ?: currentYm

        val start = ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = ym.plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val db = DatabaseProvider.getDatabase(context)
        val spent = db.transactionDao().getMonthlySpend(start, end) ?: 0.0
        val catSpends = db.transactionDao().getCategorySpends(start, end)
        val topCategory = catSpends.firstOrNull()?.category ?: "None"
        val topAmount = catSpends.firstOrNull()?.total ?: 0.0

        val maxDays = ym.lengthOfMonth()
        val nowDay = if (ym == currentYm) {
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        } else if (ym.isBefore(currentYm)) {
            maxDays
        } else {
            0
        }

        val daysRemaining = (maxDays - nowDay).coerceAtLeast(0)
        val dailyAverage = if (nowDay > 0) spent / nowDay else 0.0
        val projected = if (ym.isBefore(currentYm)) spent else spent + (dailyAverage * daysRemaining)
        val progress = (spent / (if (budget > 0) budget else 1.0)).coerceIn(0.0, 1.0).toFloat()

        return SpendingForecast(
            budget = budget,
            spent = spent,
            projected = projected,
            dailyAverage = dailyAverage,
            progress = progress,
            topCategory = topCategory,
            topCategoryAmount = topAmount
        )
    }

    companion object {
        @Volatile
        private var instance: PredictionService? = null

        fun getInstance(context: Context): PredictionService {
            return instance ?: synchronized(this) {
                instance ?: PredictionService(context.applicationContext).also { instance = it }
            }
        }
    }
}
