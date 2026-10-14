package com.example.cipherspend.service

import com.example.cipherspend.data.model.TransactionEntity
import java.security.MessageDigest
import java.util.regex.Pattern

object SmsParser {
    private val AMOUNT_PATTERNS = listOf(
        // Pattern 1: Currency symbol/prefix followed by amount (e.g. "Rs. 1,234.50", "INR 500", "₹ 450")
        Pattern.compile("""(?:rs\.?|inr|₹)\s*[:\-\s]*([0-9]+(?:,[0-9]+)*(?:\.[0-9]{1,2})?)""", Pattern.CASE_INSENSITIVE),
        // Pattern 2: Action word followed by amount (e.g. "debited for 500", "spent of Rs. 400", "paid by INR 250", "sent of 100")
        Pattern.compile("""(?:debited|spent|paid|sent)(?:\s+(?:by|for|of|with))?\s+(?:rs\.?|inr|₹)?\s*([0-9]+(?:,[0-9]+)*(?:\.[0-9]{1,2})?)""", Pattern.CASE_INSENSITIVE),
        // Pattern 3: Amount followed by action (e.g. "500.00 debited", "1,200 spent")
        Pattern.compile("""(?:rs\.?|inr|₹)?\s*([0-9]+(?:,[0-9]+)*(?:\.[0-9]{1,2})?)\s+(?:debited|spent)""", Pattern.CASE_INSENSITIVE),
        // Pattern 4: Fallback for "amount:", "txn of", "refund of"
        Pattern.compile("""(?:amount|txn of|refund of)\s*[:\-\s]*(?:rs\.?|inr|₹)?\s*([0-9]+(?:,[0-9]+)*(?:\.[0-9]{1,2})?)""", Pattern.CASE_INSENSITIVE)
    )

    private val NOTIF_AMOUNT_REGEX = Pattern.compile(
        """(?:₹|Rs\.?)\s*([0-9]+(?:,[0-9]+)*(?:\.[0-9]{1,2})?)""",
        Pattern.CASE_INSENSITIVE
    )

    private val UPI_SLASH_REGEX = Pattern.compile(
        """(?i)UPI/(?:P2M|P2A|DR|CR)/[0-9]+/([A-Za-z0-9\s.&_\-]+?)(?:/|\s+|$)"""
    )

    private val STANDARD_MERCHANT_REGEX = Pattern.compile(
        """(?i)(?:paid to|sent to|transfer to|trf to|purchase at|spent at|txn at|to vpa|at|to)\s+([A-Za-z0-9][A-Za-z0-9\s.&@_\-]{1,35}?)(?=\s+(?:on|at|from|via|successful|ref|refno|upi|txn|dated|bal|avl|not you|call|sms|info)|[.\-,;]|$)"""
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

    private val ACCOUNT_LAST4_REGEX = Pattern.compile(
        """(?i)(?:a/c|acct|account|card)\s*(?:no\.?|ending|xx+|\*+|\s)*[xX*]*([0-9]{3,4})\b"""
    )

    fun formatAccountSource(sender: String, body: String): String {
        val sUpper = sender.uppercase()
        val bUpper = body.uppercase()

        val bank: String? = when {
            sUpper.contains("HDFC") || bUpper.contains("HDFC") -> "HDFC"
            sUpper.contains("SBI") || sUpper.contains("SBIN") || bUpper.contains("SBI ") || bUpper.contains("STATE BANK") -> "SBI"
            sUpper.contains("ICICI") || bUpper.contains("ICICI") -> "ICICI"
            sUpper.contains("AXIS") || bUpper.contains("AXIS") -> "Axis"
            sUpper.contains("KOTAK") || bUpper.contains("KOTAK") -> "Kotak"
            sUpper.contains("BOB") || sUpper.contains("BAROD") || bUpper.contains("BANK OF BARODA") || bUpper.contains("BOB ") -> "BOB"
            sUpper.contains("PNB") || bUpper.contains("PUNJAB NATIONAL") || bUpper.contains("PNB ") -> "PNB"
            sUpper.contains("CANARA") || sUpper.contains("CANBNK") || bUpper.contains("CANARA") -> "Canara"
            sUpper.contains("INDUS") || bUpper.contains("INDUSIND") -> "IndusInd"
            sUpper.contains("IDFC") || bUpper.contains("IDFC") -> "IDFC"
            sUpper.contains("YESB") || bUpper.contains("YES BANK") || bUpper.contains("YESBANK") -> "Yes Bank"
            sUpper.contains("FED") || bUpper.contains("FEDERAL BANK") -> "Federal"
            sUpper.contains("AMEX") || bUpper.contains("AMERICAN EXPRESS") -> "AMEX"
            sUpper.contains("ONECARD") || bUpper.contains("ONECARD") -> "OneCard"
            sUpper.contains("SLICE") || bUpper.contains("SLICE") -> "Slice"
            sUpper.contains("GPAY") || bUpper.contains("GOOGLE PAY") -> "GPay"
            sUpper.contains("PHONEPE") || bUpper.contains("PHONEPE") -> "PhonePe"
            sUpper.contains("PAYTM") || bUpper.contains("PAYTM") -> "Paytm"
            sUpper.contains("BHIM") || bUpper.contains("BHIM") -> "BHIM"
            sUpper.contains("CRED") || bUpper.contains("CRED") -> "Cred"
            else -> null
        }

        val matcher = ACCOUNT_LAST4_REGEX.matcher(body)
        val last4: String? = if (matcher.find()) {
            matcher.group(1)
        } else {
            null
        }

        return when {
            bank != null && last4 != null -> "$bank ••$last4"
            last4 != null -> "A/c ••$last4"
            bank != null -> bank
            else -> sender
        }
    }

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
        predictionService: PredictionService? = null,
        merchantOverrides: Map<String, String> = emptyMap()
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
        } else {
            if (isSpam(cleanBody)) {
                return null
            }

            val debitIndicators = listOf(
                "debited", "spent", "paid", "sent", "withdrawn", "withdrew",
                "purchase", "deducted", "auto-debit", "mandate", "dr ", "txn of", "trf to", "transfer to"
            )
            val hasDebit = debitIndicators.any { lowerBody.contains(it) }

            val creditPhrases = listOf(
                "credited to", "credited in", "received in", "deposited", "salary credited", "refund of", "cashback of"
            )
            val hasCredit = creditPhrases.any { lowerBody.contains(it) }

            if (!hasDebit || (hasCredit && !hasDebit)) {
                return null
            }
        }

        val amount = extractAmount(cleanBody, isNotification)
        if (amount <= 0.0) return null

        var merchant = extractMerchant(cleanBody)
        merchantOverrides[merchant.lowercase()]?.let { customName ->
            if (customName.isNotBlank()) {
                merchant = customName
            }
        }

        val type = determineType(lowerBody)
        var category = predictionService?.predictCategory(cleanBody) ?: "Uncategorized"

        val lowerCat = category.lowercase()
        if (isNotification && (lowerCat == "ignore" || lowerCat == "spam" || lowerCat == "uncategorized")) {
            category = "Transaction"
        } else if (!isNotification && (lowerCat == "ignore" || lowerCat == "spam")) {
            return null
        }

        val hash = generateHash(sender, cleanBody, timestamp)
        val finalSender = if (sender == "DEMO_MODE" || sender == "MANUAL") {
            sender
        } else {
            formatAccountSource(sender, cleanBody)
        }

        return TransactionEntity(
            hash = hash,
            sender = finalSender,
            body = cleanBody,
            amount = amount,
            category = category,
            type = type,
            merchant = merchant,
            timestamp = timestamp
        )
    }

    fun extractAmount(body: String, isNotif: Boolean): Double {
        for (pattern in AMOUNT_PATTERNS) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                val raw = matcher.group(1)?.replace(",", "")
                val parsed = raw?.toDoubleOrNull()
                if (parsed != null && parsed > 0.0) {
                    return parsed
                }
            }
        }
        if (isNotif) {
            val notifMatcher = NOTIF_AMOUNT_REGEX.matcher(body)
            if (notifMatcher.find()) {
                val raw = notifMatcher.group(1)?.replace(",", "")
                val parsed = raw?.toDoubleOrNull()
                if (parsed != null && parsed > 0.0) {
                    return parsed
                }
            }
        }
        return 0.0
    }

    fun extractMerchant(body: String): String {
        val lower = body.lowercase()

        // 1. Known Indian brand fast-match
        when {
            lower.contains("swiggy instamart") -> return "Swiggy Instamart"
            lower.contains("swiggy") -> return "Swiggy"
            lower.contains("zomato") -> return "Zomato"
            lower.contains("uber") -> return "Uber"
            lower.contains("ola") -> return "Ola"
            lower.contains("rapido") -> return "Rapido"
            lower.contains("amazon") -> return "Amazon"
            lower.contains("flipkart") -> return "Flipkart"
            lower.contains("myntra") -> return "Myntra"
            lower.contains("zepto") -> return "Zepto"
            lower.contains("blinkit") -> return "Blinkit"
            lower.contains("bigbasket") -> return "BigBasket"
            lower.contains("dmart") -> return "DMart"
            lower.contains("airtel") -> return "Airtel"
            lower.contains("jio") -> return "Jio"
            lower.contains("zerodha") -> return "Zerodha"
            lower.contains("groww") -> return "Groww"
            lower.contains("irctc") -> return "IRCTC"
            lower.contains("netflix") -> return "Netflix"
            lower.contains("spotify") -> return "Spotify"
            lower.contains("bookmyshow") -> return "BookMyShow"
            lower.contains("meesho") -> return "Meesho"
            lower.contains("mcdonald") -> return "McDonald's"
            lower.contains("kfc") -> return "KFC"
            lower.contains("domino") -> return "Domino's"
            lower.contains("pizza hut") -> return "Pizza Hut"
            lower.contains("starbucks") -> return "Starbucks"
        }

        // 2. UPI slash-delimited format
        val upiSlashMatcher = UPI_SLASH_REGEX.matcher(body)
        if (upiSlashMatcher.find()) {
            val candidate = upiSlashMatcher.group(1)?.trim()
            if (!candidate.isNullOrBlank()) {
                val cleaned = cleanMerchantName(candidate)
                if (cleaned.isNotEmpty() && cleaned.length <= 40) {
                    return cleaned
                }
            }
        }

        // 3. Standard "paid to / sent to / at / to" format (excluding "from" and "in")
        val standardMatcher = STANDARD_MERCHANT_REGEX.matcher(body)
        if (standardMatcher.find()) {
            val candidate = standardMatcher.group(1)?.trim()
            if (!candidate.isNullOrBlank()) {
                val cleaned = cleanMerchantName(candidate)
                if (cleaned.isNotEmpty() && cleaned.length <= 40) {
                    return cleaned
                }
            }
        }

        // 4. VPA fallback
        val vpaMatcher = VPA_REGEX.matcher(body)
        if (vpaMatcher.find()) {
            return vpaMatcher.group(1) ?: "Unknown Merchant"
        }

        if (body.contains("atm", ignoreCase = true)) return "ATM"
        return "Unknown Merchant"
    }

    private fun cleanMerchantName(raw: String): String {
        return raw.split("\\s+(?:on|at|from|via|successful|ref|refno|upi|txn|dated|bal|avl|inr|rs)(?:\\s+|$)".toRegex(RegexOption.IGNORE_CASE))[0]
            .replace("[.\\-_,;]+$".toRegex(), "")
            .trim()
    }

    fun determineType(lowerBody: String): String {
        // 1. UPI Payment
        if (UPI_REGEX.matcher(lowerBody).find()) return "UPI"

        // 2. Card Payment
        if (lowerBody.contains("card") || lowerBody.contains("visa") ||
            lowerBody.contains("mastercard") || lowerBody.contains("rupay") ||
            lowerBody.contains("pos") || lowerBody.contains("credit card") ||
            lowerBody.contains("debit card")
        ) return "Card"

        // 3. ATM / Cash Withdrawal (Strictly verified)
        val isAtmOrCashWithdrawal = (lowerBody.contains("atm") ||
                lowerBody.contains("cash withdrawal") ||
                lowerBody.contains("withdrawn cash") ||
                lowerBody.contains("cash w/d") ||
                Regex("""\b(withdrew|withdrawn)\s+(?:rs\.?|inr|₹)?\s*\d+""").containsMatchIn(lowerBody)) &&
                !lowerBody.contains("cash limit") &&
                !lowerBody.contains("cashback") &&
                !lowerBody.contains("cash back") &&
                !lowerBody.contains("available cash")

        if (isAtmOrCashWithdrawal) return "Cash"

        // 4. Bank Transfer / Account Debit
        if (lowerBody.contains("neft") || lowerBody.contains("rtgs") ||
            lowerBody.contains("imps") || lowerBody.contains("transfer") ||
            lowerBody.contains("trf") || lowerBody.contains("debited") ||
            lowerBody.contains("netbanking") || lowerBody.contains("a/c") ||
            lowerBody.contains("account")
        ) return "Bank Transfer"

        return "Other"
    }

    fun generateHash(sender: String, body: String, timestamp: Long): String {
        val clean = body.trim().replace("\\s+".toRegex(), "")
        val raw = "$sender$clean$timestamp"
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
