package com.kema.k2look.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for ProgressBar model
 */
class ProgressBarTest {

    @Test
    fun `calculateFillAmount returns correct pixels for horizontal bar`() {
        val bar = ProgressBar(
            id = 1,
            x = 30,
            y = 100,
            width = 244,
            height = 20,
            orientation = Orientation.HORIZONTAL,
            minValue = 0f,
            maxValue = 400f,
            showBorder = true,
            dataField = createTestDataField()
        )

        assertEquals(0, bar.calculateFillAmount(0f))
        assertEquals(61, bar.calculateFillAmount(100f))
        assertEquals(122, bar.calculateFillAmount(200f))
        assertEquals(149, bar.calculateFillAmount(245f)) // 61% of 244px
        assertEquals(244, bar.calculateFillAmount(400f))
    }

    @Test
    fun `calculateFillAmount returns correct pixels for vertical bar`() {
        val bar = ProgressBar(
            id = 1,
            x = 270,
            y = 30,
            width = 20,
            height = 200,
            orientation = Orientation.VERTICAL,
            minValue = 0f,
            maxValue = 120f,
            showBorder = true,
            dataField = createTestDataField()
        )

        assertEquals(0, bar.calculateFillAmount(0f))
        assertEquals(100, bar.calculateFillAmount(60f))
        assertEquals(141, bar.calculateFillAmount(85f)) // Real cadence
        assertEquals(200, bar.calculateFillAmount(120f))
    }

    @Test
    fun `calculatePercentage returns correct percentage`() {
        val bar = ProgressBar(
            id = 1,
            x = 30,
            y = 100,
            width = 244,
            height = 20,
            orientation = Orientation.HORIZONTAL,
            minValue = 0f,
            maxValue = 400f,
            showBorder = true,
            dataField = createTestDataField()
        )

        assertEquals(0, bar.calculatePercentage(0f))
        assertEquals(25, bar.calculatePercentage(100f))
        assertEquals(50, bar.calculatePercentage(200f))
        assertEquals(61, bar.calculatePercentage(245f))
        assertEquals(100, bar.calculatePercentage(400f))
    }

    @Test
    fun `calculatePercentage clamps values outside range`() {
        val bar = ProgressBar(
            id = 1,
            x = 30,
            y = 100,
            width = 244,
            height = 20,
            orientation = Orientation.HORIZONTAL,
            minValue = 0f,
            maxValue = 400f,
            showBorder = true,
            dataField = createTestDataField()
        )

        assertEquals(0, bar.calculatePercentage(-50f))
        assertEquals(100, bar.calculatePercentage(500f))
    }

    @Test
    fun `calculateFillAmount works with non-zero minimum`() {
        val bar = ProgressBar(
            id = 1,
            x = 30,
            y = 100,
            width = 200,
            height = 20,
            orientation = Orientation.HORIZONTAL,
            minValue = 40f,
            maxValue = 200f,
            showBorder = true,
            dataField = createTestDataField()
        )

        assertEquals(0, bar.calculateFillAmount(40f))
        assertEquals(100, bar.calculateFillAmount(120f)) // Midpoint
        assertEquals(156, bar.calculateFillAmount(165f)) // 78% of 200px
        assertEquals(200, bar.calculateFillAmount(200f))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `bar rejects zero width`() {
        ProgressBar(
            id = 1,
            x = 30,
            y = 100,
            width = 0,
            height = 20,
            orientation = Orientation.HORIZONTAL,
            minValue = 0f,
            maxValue = 400f,
            showBorder = true,
            dataField = createTestDataField()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `bar rejects zero height`() {
        ProgressBar(
            id = 1,
            x = 30,
            y = 100,
            width = 244,
            height = 0,
            orientation = Orientation.HORIZONTAL,
            minValue = 0f,
            maxValue = 400f,
            showBorder = true,
            dataField = createTestDataField()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `bar rejects max value less than min value`() {
        ProgressBar(
            id = 1,
            x = 30,
            y = 100,
            width = 244,
            height = 20,
            orientation = Orientation.HORIZONTAL,
            minValue = 400f,
            maxValue = 0f,
            showBorder = true,
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
