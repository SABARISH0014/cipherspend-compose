package com.example.cipherspend

import com.example.cipherspend.service.getSalaryCycleBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

class SalaryCycleTest {

    @Test
    fun testCycleDayOneBounds() {
        val ym = YearMonth.of(2026, 10)
        val (start, end) = getSalaryCycleBounds(ym, 1)

        val startZdt = Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault())
        val endZdt = Instant.ofEpochMilli(end).atZone(ZoneId.systemDefault())

        assertEquals(2026, startZdt.year)
        assertEquals(10, startZdt.monthValue)
        assertEquals(1, startZdt.dayOfMonth)
        assertEquals(0, startZdt.hour)

        assertEquals(2026, endZdt.year)
        assertEquals(11, endZdt.monthValue)
        assertEquals(1, endZdt.dayOfMonth)
        assertEquals(0, endZdt.hour)

        assertTrue(end > start)
    }

    @Test
    fun testCycleDayMidMonthBounds() {
        val ym = YearMonth.of(2026, 5)
        val (start, end) = getSalaryCycleBounds(ym, 15)

        val startZdt = Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault())
        val endZdt = Instant.ofEpochMilli(end).atZone(ZoneId.systemDefault())

        assertEquals(2026, startZdt.year)
        assertEquals(5, startZdt.monthValue)
        assertEquals(15, startZdt.dayOfMonth)

        assertEquals(2026, endZdt.year)
        assertEquals(6, endZdt.monthValue)
        assertEquals(15, endZdt.dayOfMonth)

        assertTrue(end > start)
    }

    @Test
    fun testCycleDayClampingForEndMonth() {
        // Cycle day 31 should be safely clamped to <= 28 to guarantee valid dates in all months (including Feb)
        val ym = YearMonth.of(2026, 2)
        val (start, end) = getSalaryCycleBounds(ym, 31)

        val startZdt = Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault())
        val endZdt = Instant.ofEpochMilli(end).atZone(ZoneId.systemDefault())

        assertEquals(28, startZdt.dayOfMonth)
        assertEquals(28, endZdt.dayOfMonth)
        assertTrue(end > start)
    }
}
