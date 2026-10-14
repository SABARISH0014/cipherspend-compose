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

fun getSalaryCycleBounds(ym: YearMonth, cycleDay: Int): Pair<Long, Long> {
    val maxDaysThisMonth = ym.lengthOfMonth()
    val clampedDay = cycleDay.coerceIn(1, minOf(28, maxDaysThisMonth))

    val start = if (clampedDay == 1) {
        ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    } else {
        ym.atDay(clampedDay).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    val nextYm = ym.plusMonths(1)
    val maxDaysNextMonth = nextYm.lengthOfMonth()
    val clampedNextDay = cycleDay.coerceIn(1, minOf(28, maxDaysNextMonth))

    val end = if (clampedDay == 1) {
        nextYm.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    } else {
        nextYm.atDay(clampedNextDay).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    return Pair(start, end)
}

class PredictionService private constructor(private val context: Context) {
    private val inferenceLock = Any()
    private var interpreter: Interpreter? = null
    private var modelByteBuffer: ByteBuffer? = null
    private var cachedInputBuffer: ByteBuffer? = null
    private var cachedOutputBuffer: Array<FloatArray>? = null
    private var vocab: Map<String, Int> = emptyMap()
    private var labels: List<String> = emptyList()
    private var sequenceLength: Int = 60

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

    private fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.maxOrNull() ?: 0f
        var sum = 0.0
        val exps = DoubleArray(logits.size)
        for (i in logits.indices) {
            val exp = kotlin.math.exp((logits[i] - maxLogit).toDouble())
            exps[i] = exp
            sum += exp
        }
        return FloatArray(logits.size) { (exps[it] / sum).toFloat() }
    }

    /**
     * Deterministic, high-precision keyword classification for Indian banking/fintech merchants.
     */
    fun classifyByKeywords(text: String): String? {
        val lower = text.lowercase()

        // 1. Food & Dining
        if (lower.contains("swiggy") || lower.contains("zomato") || lower.contains("mcdonald") ||
            lower.contains("mcd") || lower.contains("kfc") || lower.contains("burger king") ||
            lower.contains("domino") || lower.contains("pizza hut") || lower.contains("subway") ||
            lower.contains("starbucks") || lower.contains("cafe") || lower.contains("ccd") ||
            lower.contains("chai point") || lower.contains("barbeque nation") || lower.contains("haldiram") ||
            lower.contains("blinkit") || lower.contains("zepto") || lower.contains("instamart") ||
            lower.contains("bigbasket") || lower.contains("dunzo") || lower.contains("faasos") ||
            lower.contains("behrouz") || lower.contains("eatclub") || lower.contains("restaurant") ||
            lower.contains("dining") || lower.contains("bakery") || lower.contains("sweets") ||
            lower.contains("food") || lower.contains("snacks") || lower.contains("dine") ||
            lower.contains("canteen") || lower.contains("mess") || lower.contains("biryani") ||
            lower.contains("kitchen")
        ) {
            return "Food & Dining"
        }

        // 2. Shopping
        if (lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") ||
            lower.contains("meesho") || lower.contains("nykaa") || lower.contains("ajio") ||
            lower.contains("tata cliq") || lower.contains("reliance digital") || lower.contains("croma") ||
            lower.contains("vijay sales") || lower.contains("zara") || lower.contains("h&m") ||
            lower.contains("westside") || lower.contains("pantaloons") || lower.contains("max fashion") ||
            lower.contains("decathlon") || lower.contains("ikea") || lower.contains("retail") ||
            lower.contains("mall") || lower.contains("mart") || lower.contains("superstore") ||
            lower.contains("hypermarket") || lower.contains("clothing") || lower.contains("footwear") ||
            lower.contains("lenskart") || lower.contains("purplle") || lower.contains("zudio") ||
            lower.contains("shop") || lower.contains("store")
        ) {
            return "Shopping"
        }

        // 3. Travel & Transit
        if (lower.contains("uber") || lower.contains("ola") || lower.contains("rapido") ||
            lower.contains("makemytrip") || lower.contains("yatra") || lower.contains("goibibo") ||
            lower.contains("cleartrip") || lower.contains("easemytrip") || lower.contains("irctc") ||
            lower.contains("railways") || lower.contains("redbus") || lower.contains("abhibus") ||
            lower.contains("indigo") || lower.contains("air india") || lower.contains("spicejet") ||
            lower.contains("akasa") || lower.contains("vistara") || lower.contains("flight") ||
            lower.contains("petrol") || lower.contains("fuel") || lower.contains("diesel") ||
            lower.contains("indianoil") || lower.contains("iocl") || lower.contains("hpcl") ||
            lower.contains("bpcl") || lower.contains("shell") || lower.contains("toll") ||
            lower.contains("fastag") || lower.contains("metro") || lower.contains("auto") ||
            lower.contains("cab") || lower.contains("taxi") || lower.contains("parking") ||
            lower.contains("travel")
        ) {
            return "Travel"
        }

        // 4. Bills & Utilities
        if (lower.contains("airtel") || lower.contains("jio") || lower.contains("vi") ||
            lower.contains("vodafone") || lower.contains("idea") || lower.contains("bsnl") ||
            lower.contains("mtnl") || lower.contains("electricity") || lower.contains("power") ||
            lower.contains("bescom") || lower.contains("tneb") || lower.contains("cesc") ||
            lower.contains("tata power") || lower.contains("adani electricity") || lower.contains("discom") ||
            lower.contains("water") || lower.contains("gas") || lower.contains("indane") ||
            lower.contains("bharat gas") || lower.contains("hp gas") || lower.contains("piped gas") ||
            lower.contains("broadband") || lower.contains("wifi") || lower.contains("dth") ||
            lower.contains("tata play") || lower.contains("dishtv") || lower.contains("sun direct") ||
            lower.contains("recharge") || lower.contains("utility") || lower.contains("bill") ||
            lower.contains("postpaid") || lower.contains("landline") || lower.contains("insurance") ||
            lower.contains("lic") || lower.contains("premium")
        ) {
            return "Bills & Utilities"
        }

        // 5. Investment
        if (lower.contains("zerodha") || lower.contains("groww") || lower.contains("angelone") ||
            lower.contains("upstox") || lower.contains("coin") || lower.contains("kuvera") ||
            lower.contains("indmoney") || lower.contains("paytm money") || lower.contains("mutual fund") ||
            lower.contains("sip") || lower.contains("nav") || lower.contains("stock") ||
            lower.contains("share") || lower.contains("demat") || lower.contains("nsdl") ||
            lower.contains("cdsl") || lower.contains("fixed deposit") || lower.contains("recurring deposit") ||
            lower.contains("ppf") || lower.contains("nps") || lower.contains("sovereign gold") ||
            lower.contains("sgb") || lower.contains("smallcase") || lower.contains("invest")
        ) {
            return "Investment"
        }

        // 6. ATM / Cash Withdrawal (Strictly verified)
        val isAtmCash = (lower.contains("atm") ||
                lower.contains("cash withdrawal") ||
                lower.contains("withdrawn cash") ||
                lower.contains("cash w/d") ||
                Regex("""\b(withdrew|withdrawn)\s+(?:rs\.?|inr|₹)?\s*\d+""").containsMatchIn(lower)) &&
                !lower.contains("cash limit") &&
                !lower.contains("cashback") &&
                !lower.contains("cash back")

        if (isAtmCash) {
            return "Cash"
        }

        return null
    }

    fun predictCategory(text: String): String = synchronized(inferenceLock) {
        // Step 1: Check high-precision keyword / merchant rules first
        val keywordMatch = classifyByKeywords(text)
        if (keywordMatch != null) {
            return keywordMatch
        }

        // Step 2: Machine Learning Model Inference with Softmax
        val activeInterpreter = interpreter
        if (activeInterpreter != null && labels.isNotEmpty()) {
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

                val logits = outBuf[0]
                val probs = softmax(logits)

                var bestIdx = 0
                var maxProb = -1.0f
                for (i in probs.indices) {
                    if (probs[i] > maxProb) {
                        maxProb = probs[i]
                        bestIdx = i
                    }
                }

                val predicted = if (bestIdx in labels.indices) labels[bestIdx] else "Others"

                // CRITICAL SANITY CHECK:
                // Never allow generic UPI/card/debit transactions to be labeled as "Cash"
                // unless actual cash withdrawal intent is confirmed in the SMS.
                val lower = text.lowercase()
                val isGenuineCash = (lower.contains("atm") || lower.contains("cash withdrawal") || lower.contains("withdrawn cash")) &&
                        !lower.contains("cash limit") && !lower.contains("cashback")

                if (predicted == "Cash" && !isGenuineCash) {
                    return if (lower.contains("upi") || lower.contains("transfer") || lower.contains("paid") || lower.contains("sent") || lower.contains("debited")) {
                        "Transaction"
                    } else {
                        "Others"
                    }
                }

                // Accept high-confidence predictions that are not noise
                if (maxProb >= 0.40f && predicted != "Others" && predicted != "Ignore" && predicted != "Spam" && predicted != "Uncategorized") {
                    return predicted
                }
            } catch (e: Throwable) {
                Log.e("PredictionService", "Error during ML inference: ${e.message}")
            }
        }

        // Step 3: Default fallback for general transactions
        val lower = text.lowercase()
        return if (lower.contains("upi") || lower.contains("transfer") || lower.contains("paid") ||
            lower.contains("sent") || lower.contains("debited") || lower.contains("spent") || lower.contains("txn")
        ) {
            "Transaction"
        } else {
            "Others"
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

        val cycleDay = prefs.getInt("salary_cycle_day", 1)
        val currentYm = YearMonth.now()
        val (startTimestamp, endTimestamp) = getSalaryCycleBounds(currentYm, cycleDay)

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

        val cycleDay = prefs.getInt("salary_cycle_day", 1)
        val (start, end) = getSalaryCycleBounds(ym, cycleDay)

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
