package com.example.cipherspend

import com.example.cipherspend.service.SmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsParserTest {

    @Test
    fun testAmountExtractionStandardFormats() {
        val msg1 = "Dear Customer, INR 450.00 debited from A/c XX1234 on 12-Oct-26 via UPI."
        assertEquals(450.0, SmsParser.extractAmount(msg1, false), 0.001)

        val msg2 = "Your account has been debited for Rs. 1,250.75 towards purchase at DMart."
        assertEquals(1250.75, SmsParser.extractAmount(msg2, false), 0.001)

        val msg3 = "Sent Rs. 3,50,000.00 to Rakesh via NEFT ref 998811."
        assertEquals(350000.0, SmsParser.extractAmount(msg3, false), 0.001)

        val msg4 = "₹ 99 debited from card ending 4321."
        assertEquals(99.0, SmsParser.extractAmount(msg4, false), 0.001)
    }

    @Test
    fun testMerchantExtractionKnownBrandsAndUPI() {
        val msgSwiggy = "INR 349 debited from A/c XX1001 on 10-Oct for order at Swiggy."
        assertEquals("Swiggy", SmsParser.extractMerchant(msgSwiggy))

        val msgZomato = "Paid Rs 520 at Zomato via UPI."
        assertEquals("Zomato", SmsParser.extractMerchant(msgZomato))

        val msgUpiSlash = "Txn of INR 180.00 done via UPI/P2M/987654321/Chai Point/Axis Bank."
        assertEquals("Chai Point", SmsParser.extractMerchant(msgUpiSlash))

        val msgVpa = "Transferred Rs. 200 to baker@okaxis."
        assertEquals("baker@okaxis", SmsParser.extractMerchant(msgVpa))
    }

    @Test
    fun testTransactionTypeClassification() {
        assertEquals("UPI", SmsParser.determineType("paid inr 250 via upi ref 12345"))
        assertEquals("Card", SmsParser.determineType("inr 1500 debited on your visa platinum card ending 9988 at pos"))
        assertEquals("Cash", SmsParser.determineType("withdrew rs. 4000 at sbi atm branch"))
        assertEquals("Bank Transfer", SmsParser.determineType("inr 12000 debited via neft trf to acct 991122"))
    }

    @Test
    fun testAccountSourceFormatting() {
        val hdfcFormatted = SmsParser.formatAccountSource(
            "HDFCBK",
            "INR 800 debited from HDFC Bank A/c ending 6334."
        )
        assertEquals("HDFC ••6334", hdfcFormatted)

        val sbiFormatted = SmsParser.formatAccountSource(
            "SBINB",
            "Rs 2,500 debited from State Bank account no. ending 4122."
        )
        assertEquals("SBI ••4122", sbiFormatted)
    }

    @Test
    fun testSpamAndOtpRejection() {
        val spamData = "100% daily data limit consumed. Recharge your pack with 1.5GB @ Rs 19."
        assertTrue(SmsParser.isSpam(spamData))
        assertNull(SmsParser.parseMessage("JIOINF", spamData, System.currentTimeMillis()))

        val otpMsg = "Your one time password (OTP) for login is 492011. Do not share this with anyone."
        assertTrue(SmsParser.isSpam(otpMsg))
        assertNull(SmsParser.parseMessage("HDFCBK", otpMsg, System.currentTimeMillis()))

        val loanOffer = "Congratulations! You have pre-approved personal loan of Rs 5,00,000 at zero interest."
        assertTrue(SmsParser.isSpam(loanOffer))
        assertNull(SmsParser.parseMessage("AXISBK", loanOffer, System.currentTimeMillis()))
    }

    @Test
    fun testValidTransactionParsedSuccessfully() {
        val timestamp = 1729900000000L
        val tx = SmsParser.parseMessage(
            sender = "HDFCBK",
            body = "INR 350.00 debited from A/c ending 6334 on 26-Oct-26 at Starbucks via UPI.",
            timestamp = timestamp
        )
        assertNotNull(tx)
        assertEquals(350.0, tx!!.amount, 0.001)
        assertEquals("Starbucks", tx.merchant)
        assertEquals("UPI", tx.type)
        assertEquals("HDFC ••6334", tx.sender)
    }
}
