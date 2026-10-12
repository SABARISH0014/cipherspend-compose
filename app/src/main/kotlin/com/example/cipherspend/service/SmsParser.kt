package com.example.cipherspend.service

import com.example.cipherspend.data.model.CustomRuleEntity
import com.example.cipherspend.data.model.TransactionEntity
import java.security.MessageDigest
import java.util.regex.Pattern

object SmsParser {
    private val AMOUNT_REGEX = Pattern.compile(
        """(?:rs\.?|inr|₹|amount|debited(?: for| by| with)?|spent|spent of|refund of|txn of|credited(?: for| with)?)\s*[:\-\s]*(?:rs\.?|inr|₹)?\s*([0-9]+(?:,[0-9]+)*(?:\.[0-9]{1,2})?)""",
        Pattern.CASE_INSENSITIVE
    )

    private val NOTIF_AMOUNT_REGEX = Pattern.compile(
        """(?:₹|Rs\.?)\s*([0-9]+(?:,[0-9]+)*(?:\.[0-9]{1,2})?)""",
        Pattern.CASE_INSENSITIVE
    )

    private val MERCHANT_REGEX = Pattern.compile(
        """(?:paid to|sent to|transfer to|trf to|transfer from|purchase at|txn at|to vpa|for|from|in|to)\s+([a-zA-Z0-9\s\.@\-_]+)""",
        Pattern.CASE_INSENSITIVE
    )

    private val UPI_REGEX = Pattern.compile(
        """(upi|vpa|paytm|gpay|phonepe|bhim|qr)""",
        Pattern.CASE_INSENSITIVE
    )

    private val VPA_REGEX = Pattern.compile(
        """([a-zA-Z0-9._\-]+@[a-zA-Z]+)""",
        Pattern.CASE_INSENSITIVE
    )

    private val SPAM_PATTERNS = listOf(
        Pattern.compile("""(?:data|internet).*(?:consumed|used|exhausted|left|remaining)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:recharge|plan|pack|offer|validity|expired|quota|benefit)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:get|claim).*(?:gb|mb).*(?:at|@)\s*(?:rs|₹)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:otp|verification code|auth code|login)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:http|www|bit\.ly|goo\.gl|\.in/|\.com/)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:congratulations|winner|lucky|prize|reward)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:loan|pre-approved|limit increase)""", Pattern.CASE_INSENSITIVE)
    )

    fun isSpam(cleanBody: String): Boolean {
        for (pattern in SPAM_PATTERNS) {
            if (pattern.matcher(cleanBody).find()) return true
        }
        return false
    }

    fun parseMessage(
        sender: String,
        body: String,
        timestamp: Long,
        customRules: List<CustomRuleEntity> = emptyList(),
        predictionService: PredictionService? = null
    ): TransactionEntity? {
        val cleanBody = body.replace("\\s+".toRegex(), " ").trim()
        val lowerBody = cleanBody.lowercase()

        val isNotification = listOf("GPay", "PhonePe", "Paytm", "BHIM", "Cred", "SBI YONO", "HDFC Bank", "ICICI iMobile")
            .any { sender.contains(it, ignoreCase = true) }

        if (isNotification) {
            val isExpense = lowerBody.contains("paid") ||
                    lowerBody.contains("sent") ||
                    lowerBody.contains("debited") ||
                    lowerBody.contains("payment") ||
                    lowerBody.contains("successful")

            val isIncomeOrPromo = lowerBody.contains("received") ||
                    lowerBody.contains("credited") ||
                    lowerBody.contains("cashback") ||
                    lowerBody.contains("won") ||
                    lowerBody.contains("reward")

            if (!isExpense || isIncomeOrPromo) return null
        } else if (isSpam(cleanBody)) {
            return null
        }

        // Check custom rules first
        var customMerchant: String? = null
        for (rule in customRules) {
            try {
                val rulePattern = Pattern.compile(rule.regexPattern, Pattern.CASE_INSENSITIVE)
                val matcher = rulePattern.matcher(cleanBody)
                if (matcher.find()) {
                    customMerchant = rule.merchantName
                    break
                }
            } catch (_: Exception) {}
        }

        val amount = extractAmount(cleanBody, isNotification)
        if (amount <= 0.0) return null

        val merchant = customMerchant ?: extractMerchant(cleanBody)
        val type = determineType(lowerBody)
        var category = predictionService?.predictCategory(cleanBody) ?: "Uncategorized"

        val lowerCat = category.lowercase()
        if (isNotification && (lowerCat == "ignore" || lowerCat == "spam" || lowerCat == "uncategorized")) {
            category = "Transaction"
        } else if (!isNotification && (lowerCat == "ignore" || lowerCat == "spam")) {
            return null
        }

        val hash = generateHash(sender, cleanBody, timestamp)

        return TransactionEntity(
            hash = hash,
            sender = sender,
            body = cleanBody,
            amount = amount,
            category = category,
            type = type,
            merchant = merchant,
            timestamp = timestamp
        )
    }

    fun extractAmount(body: String, isNotif: Boolean): Double {
        val matcher = AMOUNT_REGEX.matcher(body)
        if (matcher.find()) {
            val raw = matcher.group(1)?.replace(",", "")
            raw?.toDoubleOrNull()?.let { return it }
        }
        if (isNotif) {
            val notifMatcher = NOTIF_AMOUNT_REGEX.matcher(body)
            if (notifMatcher.find()) {
                val raw = notifMatcher.group(1)?.replace(",", "")
                raw?.toDoubleOrNull()?.let { return it }
            }
        }
        return 0.0
    }

    fun extractMerchant(body: String): String {
        val matcher = MERCHANT_REGEX.matcher(body)
        if (matcher.find()) {
            var name = matcher.group(1)?.trim() ?: ""
            name = name.split("\\s+(?:on|at|from|via|successful|ref|refno|txn)(?:\\s+|$)".toRegex(RegexOption.IGNORE_CASE))[0]
            name = name.replace("[.\\-_]+$".toRegex(), "").trim()
            if (name.isNotEmpty()) return name
        }

        val vpaMatcher = VPA_REGEX.matcher(body)
        if (vpaMatcher.find()) {
            return vpaMatcher.group(1) ?: "Unknown"
        }

        if (body.contains("atm", ignoreCase = true)) return "ATM"
        return "Unknown Merchant"
    }

    fun determineType(lowerBody: String): String {
        if (UPI_REGEX.matcher(lowerBody).find()) return "UPI"
        if (lowerBody.contains("card") || lowerBody.contains("visa") || lowerBody.contains("mastercard")) return "Card"
        if (lowerBody.contains("atm") || lowerBody.contains("cash")) return "Cash"
        return "Bank Transfer"
    }

    fun generateHash(sender: String, body: String, timestamp: Long): String {
        val clean = body.trim().replace("\\s+".toRegex(), "")
        val raw = "$sender$clean$timestamp"
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Synthesizes a pattern silently in the background when user edits a merchant name.
     */
    fun synthesizeRuleForCorrection(sender: String, body: String, correctedMerchant: String): CustomRuleEntity {
        // Find if old merchant or common payment tokens exist in body and build a regex
        val escapedMerchant = Pattern.quote(correctedMerchant)
        // If the body already contains a distinct segment, anchor around it
        val words = body.split("\\s+".toRegex()).take(6).joinToString(" ") { Pattern.quote(it) }
        val pattern = ".*($escapedMerchant|paid to|sent to).*"
        return CustomRuleEntity(
            sender = sender,
            regexPattern = pattern,
            merchantName = correctedMerchant
        )
    }
}
