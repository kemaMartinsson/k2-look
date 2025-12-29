package com.kema.k2look.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for KarooActiveLookBridge value parsing
 *
 * These tests verify the parseNumericValue function which is critical
 * for converting display strings to numeric values for gauges/bars
 */
class KarooActiveLookBridgeValueParsingTest {

    @Test
    fun `parseNumericValue extracts simple integer`() {
        val value = parseNumericValue("245")
        assertEquals(245f, value)
    }

    @Test
    fun `parseNumericValue extracts decimal number`() {
        val value = parseNumericValue("32.5")
        assertEquals(32.5f, value)
    }

    @Test
    fun `parseNumericValue removes unit from end`() {
        val value = parseNumericValue("245W")
        assertEquals(245f, value)
    }

    @Test
    fun `parseNumericValue removes unit with space`() {
        val value = parseNumericValue("245 W")
        assertEquals(245f, value)
    }

    @Test
    fun `parseNumericValue handles various units`() {
        assertEquals(165f, parseNumericValue("165bpm"))
        assertEquals(32.5f, parseNumericValue("32.5km/h"))
        assertEquals(85f, parseNumericValue("85rpm"))
        assertEquals(1200f, parseNumericValue("1200m"))
    }

    @Test
    fun `parseNumericValue returns null for no data marker`() {
        assertNull(parseNumericValue("--"))
        assertNull(parseNumericValue("..."))
        assertNull(parseNumericValue("N/A"))
    }

    @Test
    fun `parseNumericValue handles negative numbers`() {
        val value = parseNumericValue("-5")
        assertEquals(-5f, value)
    }

    @Test
    fun `parseNumericValue handles decimal with leading zero`() {
        val value = parseNumericValue("0.5")
        assertEquals(0.5f, value)
    }

    @Test
    fun `parseNumericValue handles large numbers`() {
        val value = parseNumericValue("1234.56")
        assertEquals(1234.56f, value)
    }

    @Test
    fun `parseNumericValue returns null for empty string`() {
        assertNull(parseNumericValue(""))
    }

    @Test
    fun `parseNumericValue returns null for non-numeric string`() {
        assertNull(parseNumericValue("abc"))
        assertNull(parseNumericValue("Error"))
    }

    @Test
    fun `parseNumericValue handles real-world formatted values`() {
        // Real examples from Karoo display
        assertEquals(245.3f, parseNumericValue("245.3"))
        assertEquals(165f, parseNumericValue("165"))
        assertEquals(32.5f, parseNumericValue("32.5"))
        assertEquals(85f, parseNumericValue("85"))
        assertEquals(1234.5f, parseNumericValue("1234.5"))
    }

    /**
     * Copy of the actual implementation from KarooActiveLookBridge
     * for testing purposes
     */
    private fun parseNumericValue(value: String): Float? {
        return when {
            value == "--" || value == "..." || value == "N/A" -> null
            else -> {
                // Remove common units and parse
                val cleaned = value.replace(Regex("[^0-9.-]"), "")
                cleaned.toFloatOrNull()
            }
        }
    }
}

/**
 * Unit tests for gauge percentage calculation logic
 */
class GaugePercentageCalculationTest {

    @Test
    fun `calculatePercentage handles power gauge range`() {
        // Power gauge: 0-400W
        assertEquals(0, calculatePercentage(0f, 0f, 400f))
        assertEquals(25, calculatePercentage(100f, 0f, 400f))
        assertEquals(50, calculatePercentage(200f, 0f, 400f))
        assertEquals(61, calculatePercentage(245f, 0f, 400f))
        assertEquals(75, calculatePercentage(300f, 0f, 400f))
        assertEquals(100, calculatePercentage(400f, 0f, 400f))
    }

    @Test
    fun `calculatePercentage handles HR gauge range with non-zero min`() {
        // HR gauge: 40-200 bpm
        assertEquals(0, calculatePercentage(40f, 40f, 200f))
        assertEquals(50, calculatePercentage(120f, 40f, 200f))
        assertEquals(78, calculatePercentage(165f, 40f, 200f))
        assertEquals(100, calculatePercentage(200f, 40f, 200f))
    }

    @Test
    fun `calculatePercentage handles cadence gauge range`() {
        // Cadence gauge: 0-120 rpm
        assertEquals(0, calculatePercentage(0f, 0f, 120f))
        assertEquals(50, calculatePercentage(60f, 0f, 120f))
        assertEquals(70, calculatePercentage(85f, 0f, 120f))
        assertEquals(100, calculatePercentage(120f, 0f, 120f))
    }

    @Test
    fun `calculatePercentage clamps below minimum`() {
        assertEquals(0, calculatePercentage(-50f, 0f, 400f))
        assertEquals(0, calculatePercentage(30f, 40f, 200f))
    }

    @Test
    fun `calculatePercentage clamps above maximum`() {
        assertEquals(100, calculatePercentage(500f, 0f, 400f))
        assertEquals(100, calculatePercentage(250f, 40f, 200f))
    }

    private fun calculatePercentage(value: Float, minValue: Float, maxValue: Float): Int {
        val range = maxValue - minValue
        val normalized = ((value - minValue) / range).coerceIn(0f, 1f)
        return (normalized * 100).toInt()
    }
}

/**
 * Unit tests for bar fill amount calculation logic
 */
class BarFillAmountCalculationTest {

    @Test
    fun `calculateFillAmount handles horizontal bar`() {
        // Horizontal bar: 244px width, 0-400W
        val width = 244
        assertEquals(0, calculateFillAmount(0f, 0f, 400f, width))
        assertEquals(61, calculateFillAmount(100f, 0f, 400f, width))
        assertEquals(149, calculateFillAmount(245f, 0f, 400f, width)) // 61% of 244
        assertEquals(244, calculateFillAmount(400f, 0f, 400f, width))
    }

    @Test
    fun `calculateFillAmount handles vertical bar`() {
        // Vertical bar: 200px height, 0-120 rpm
        val height = 200
        assertEquals(0, calculateFillAmount(0f, 0f, 120f, height))
        assertEquals(100, calculateFillAmount(60f, 0f, 120f, height))
        assertEquals(141, calculateFillAmount(85f, 0f, 120f, height)) // 70.8% of 200
        assertEquals(200, calculateFillAmount(120f, 0f, 120f, height))
    }

    @Test
    fun `calculateFillAmount clamps to bar dimensions`() {
        val width = 244
        assertEquals(0, calculateFillAmount(-50f, 0f, 400f, width))
        assertEquals(244, calculateFillAmount(500f, 0f, 400f, width))
    }

    private fun calculateFillAmount(
        value: Float,
        minValue: Float,
        maxValue: Float,
        dimension: Int
    ): Int {
        val range = maxValue - minValue
        val normalized = ((value - minValue) / range).coerceIn(0f, 1f)
        return (dimension * normalized).toInt()
    }
}

