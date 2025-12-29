package com.kema.k2look.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for Gauge model
 */
class GaugeTest {

    @Test
    fun `calculatePercentage returns correct percentage for value in range`() {
        val gauge = Gauge(
            id = 1,
            centerX = 152,
            centerY = 128,
            radiusOuter = 60,
            radiusInner = 45,
            startPortion = 0,
            endPortion = 15,
            clockwise = true,
            minValue = 0f,
            maxValue = 400f,
            dataField = createTestDataField()
        )

        // Test various values
        assertEquals(0, gauge.calculatePercentage(0f))
        assertEquals(25, gauge.calculatePercentage(100f))
        assertEquals(50, gauge.calculatePercentage(200f))
        assertEquals(61, gauge.calculatePercentage(245f)) // Real-world example
        assertEquals(75, gauge.calculatePercentage(300f))
        assertEquals(100, gauge.calculatePercentage(400f))
    }

    @Test
    fun `calculatePercentage clamps values below minimum to 0 percent`() {
        val gauge = Gauge(
            id = 1,
            centerX = 152,
            centerY = 128,
            radiusOuter = 60,
            radiusInner = 45,
            startPortion = 0,
            endPortion = 15,
            clockwise = true,
            minValue = 0f,
            maxValue = 400f,
            dataField = createTestDataField()
        )

        assertEquals(0, gauge.calculatePercentage(-50f))
        assertEquals(0, gauge.calculatePercentage(-1f))
    }

    @Test
    fun `calculatePercentage clamps values above maximum to 100 percent`() {
        val gauge = Gauge(
            id = 1,
            centerX = 152,
            centerY = 128,
            radiusOuter = 60,
            radiusInner = 45,
            startPortion = 0,
            endPortion = 15,
            clockwise = true,
            minValue = 0f,
            maxValue = 400f,
            dataField = createTestDataField()
        )

        assertEquals(100, gauge.calculatePercentage(450f))
        assertEquals(100, gauge.calculatePercentage(1000f))
    }

    @Test
    fun `calculatePercentage works with non-zero minimum`() {
        val gauge = Gauge(
            id = 1,
            centerX = 152,
            centerY = 128,
            radiusOuter = 60,
            radiusInner = 45,
            startPortion = 3,
            endPortion = 14,
            clockwise = true,
            minValue = 40f, // HR min
            maxValue = 200f, // HR max
            dataField = createTestDataField()
        )

        assertEquals(0, gauge.calculatePercentage(40f))
        assertEquals(50, gauge.calculatePercentage(120f))
        assertEquals(78, gauge.calculatePercentage(165f)) // Real-world HR
        assertEquals(100, gauge.calculatePercentage(200f))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `gauge rejects invalid ID above 255`() {
        Gauge(
            id = 256,
            centerX = 152,
            centerY = 128,
            radiusOuter = 60,
            radiusInner = 45,
            startPortion = 0,
            endPortion = 15,
            clockwise = true,
            minValue = 0f,
            maxValue = 400f,
            dataField = createTestDataField()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `gauge rejects invalid start portion above 15`() {
        Gauge(
            id = 1,
            centerX = 152,
            centerY = 128,
            radiusOuter = 60,
            radiusInner = 45,
            startPortion = 16,
            endPortion = 15,
            clockwise = true,
            minValue = 0f,
            maxValue = 400f,
            dataField = createTestDataField()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `gauge rejects outer radius smaller than inner radius`() {
        Gauge(
            id = 1,
            centerX = 152,
            centerY = 128,
            radiusOuter = 40,
            radiusInner = 50,
            startPortion = 0,
            endPortion = 15,
            clockwise = true,
            minValue = 0f,
            maxValue = 400f,
            dataField = createTestDataField()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `gauge rejects max value less than min value`() {
        Gauge(
            id = 1,
            centerX = 152,
            centerY = 128,
            radiusOuter = 60,
            radiusInner = 45,
            startPortion = 0,
            endPortion = 15,
            clockwise = true,
            minValue = 400f,
            maxValue = 0f,
            dataField = createTestDataField()
        )
    }

    private fun createTestDataField(): DataField {
        return DataField(
            id = 7,
            name = "Power",
            unit = "W",
            category = DataFieldCategory.POWER,
            karooStreamType = "power",
            icon28 = null,
            icon40 = null
        )
    }
}
