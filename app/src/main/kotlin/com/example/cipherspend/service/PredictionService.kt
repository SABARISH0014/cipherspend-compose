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
    private var interpreter: Interpreter? = null
    private var vocab: Map<String, Int> = emptyMap()
    private var labels: List<String> = emptyList()
    private var sequenceLength: Int = 20

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            // Load TFLite model from assets
            val assetFileDescriptor = context.assets.openFd("sms_model.tflite")
            val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = fileInputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

            val options = Interpreter.Options()
            interpreter = Interpreter(modelBuffer, options)

            // Dynamic input shape detection
            val inputShape = interpreter?.getInputTensor(0)?.shape()
            if (inputShape != null && inputShape.size > 1) {
                sequenceLength = inputShape[1]
            }

            // Load Vocab JSON
            val vocabString = context.assets.open("vocab.json").bufferedReader().use { it.readText() }
            val mapType = object : TypeToken<Map<String, Int>>() {}.type
            vocab = Gson().fromJson(vocabString, mapType)

            // Load Labels JSON
            val labelsString = context.assets.open("labels.json").bufferedReader().use { it.readText() }
            val labelMapType = object : TypeToken<Map<String, String>>() {}.type
            val labelMap: Map<String, String> = Gson().fromJson(labelsString, labelMapType)
            val sortedList = mutableListOf<String>()
            for (i in 0 until labelMap.size) {
                sortedList.add(labelMap[i.toString()] ?: "Others")
            }
            labels = sortedList
            Log.d("PredictionService", "TFLite Model loaded successfully. Input length: $sequenceLength, Classes: ${labels.size}")
        } catch (e: Exception) {
            Log.e("PredictionService", "Failed to initialize TFLite model: ${e.message}", e)
        }
    }

    fun predictCategory(text: String): String {
        val activeInterpreter = interpreter ?: return fallbackCategorize(text)
        if (labels.isEmpty()) return fallbackCategorize(text)

        try {
            val inputFeatures = tokenizeAndPad(text, sequenceLength)
            val inputBuffer = ByteBuffer.allocateDirect(4 * sequenceLength).apply {
                order(ByteOrder.nativeOrder())
                for (v in inputFeatures) {
                    putFloat(v)
                }
                rewind()
            }

            val numClasses = labels.size
            val outputBuffer = Array(1) { FloatArray(numClasses) }

            activeInterpreter.run(inputBuffer, outputBuffer)

            val probabilities = outputBuffer[0]
            var bestIdx = 0
            var maxProb = -1.0f
            for (i in probabilities.indices) {
                if (probabilities[i] > maxProb) {
                    maxProb = probabilities[i]
                    bestIdx = i
                }
            }

            val result = if (bestIdx in labels.indices) labels[bestIdx] else fallbackCategorize(text)
            Log.d("PredictionService", "Predicted: $result with confidence: $maxProb")
            return result
        } catch (e: Exception) {
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

        for (i in 0 until minOf(targetLength, words.size)) {
            val word = words[i]
            val token = vocab[word]?.toFloat() ?: oovToken
            sequence[i] = token
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

    suspend fun getSpendingForecast(): SpendingForecast {
        val prefs = context.getSharedPreferences("CipherSpendPrefs", Context.MODE_PRIVATE)
        val budget = prefs.getFloat("monthly_budget", 25000f).toDouble()

        val calendar = Calendar.getInstance()
        val nowDay = calendar.get(Calendar.DAY_OF_MONTH)
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        val end = calendar.timeInMillis

        val db = DatabaseProvider.getDatabase(context)
        val spent = db.transactionDao().getMonthlySpend(start, end) ?: 0.0
        val catSpends = db.transactionDao().getCategorySpends(start, end)
        val topCategory = catSpends.firstOrNull()?.category ?: "None"
        val topAmount = catSpends.firstOrNull()?.total ?: 0.0

        val daysRemaining = maxDays - nowDay
        val dailyAverage = if (nowDay > 0) spent / nowDay else 0.0
        val projected = spent + (dailyAverage * daysRemaining)
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
