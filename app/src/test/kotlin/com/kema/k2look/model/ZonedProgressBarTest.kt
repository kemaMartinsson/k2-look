package com.kema.k2look.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for ZonedProgressBar model
 */
class ZonedProgressBarTest {

    @Test
    fun `findZone returns correct zone for value`() {
        val zonedBar = createHRZonedBar()

        assertEquals("Z1", zonedBar.findZone(40f)?.name)
        assertEquals("Z1", zonedBar.findZone(100f)?.name)
        assertEquals("Z2", zonedBar.findZone(114f)?.name)
        assertEquals("Z3", zonedBar.findZone(140f)?.name)
        assertEquals("Z4", zonedBar.findZone(165f)?.name) // Real-world HR
        assertEquals("Z5", zonedBar.findZone(180f)?.name)
        assertEquals("Z5", zonedBar.findZone(200f)?.name)
    }

    @Test
    fun `findZone returns null for value below all zones`() {
        val zonedBar = createHRZonedBar()

        assertNull(zonedBar.findZone(30f))
        assertNull(zonedBar.findZone(39f))
    }

    @Test
    fun `findZone returns null for value above all zones`() {
        val zonedBar = createHRZonedBar()

        assertNull(zonedBar.findZone(201f))
        assertNull(zonedBar.findZone(250f))
    }

    @Test
    fun `valueToPixel calculates correct horizontal position`() {
        val zonedBar = createHRZonedBar()

        // Bar: x=30, width=244, range=40-200
        val startX = zonedBar.valueToPixel(40f)
        val midX = zonedBar.valueToPixel(120f)
        val hrX = zonedBar.valueToPixel(165f)
        val endX = zonedBar.valueToPixel(200f)

        assertEquals(30, startX) // Min value at x=30
        assertEquals(152, midX) // Midpoint (50% of 244 + 30)
        assertEquals(220, hrX) // 78% of range
        assertEquals(274, endX) // Max value at x=274 (30+244)
    }

    @Test
    fun `valueToPixel clamps values outside range`() {
        val zonedBar = createHRZonedBar()

        val belowMin = zonedBar.valueToPixel(0f)
        val aboveMax = zonedBar.valueToPixel(300f)

        assertEquals(30, belowMin) // Clamped to min
        assertEquals(274, aboveMax) // Clamped to max (30+244)
    }

    @Test
    fun `zone colors are valid grey levels`() {
        val zonedBar = createHRZonedBar()

        zonedBar.zones.forEach { zone ->
            assertTrue("Color ${zone.color} should be 0-15", zone.color in 0..15)
        }
    }

    @Test
    fun `zones are in ascending order`() {
        val zonedBar = createHRZonedBar()

        for (i in 0 until zonedBar.zones.size - 1) {
            val currentZone = zonedBar.zones[i]
            val nextZone = zonedBar.zones[i + 1]
            assertTrue(
                "Zone ${currentZone.name} max (${currentZone.maxValue}) should be <= ${nextZone.name} min (${nextZone.minValue})",
                currentZone.maxValue <= nextZone.minValue
            )
        }
    }

    @Test
    fun `zones cover expected HR range`() {
        val zonedBar = createHRZonedBar()

        assertEquals(40f, zonedBar.zones.first().minValue, 0.01f)
        assertEquals(200f, zonedBar.zones.last().maxValue, 0.01f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zoned bar rejects empty zones list`() {
        ProgressBar(
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
        ).let { bar ->
            ZonedProgressBar(
                bar = bar,
                zones = emptyList()
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zone rejects invalid color above 15`() {
        ZonedProgressBar.Zone(
            name = "Test",
            minValue = 0f,
            maxValue = 100f,
            color = 16
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zone rejects max value less than min value`() {
        ZonedProgressBar.Zone(
            name = "Test",
            minValue = 100f,
            maxValue = 0f,
            color = 10
        )
    }

    private fun createHRZonedBar(): ZonedProgressBar {
        val bar = ProgressBar(
            id = 1,
            x = 30,
            y = 110,
            width = 244,
            height = 25,
            orientation = Orientation.HORIZONTAL,
            minValue = 40f,
            maxValue = 200f,
            showBorder = true,
            dataField = createTestDataField()
        )

        return ZonedProgressBar(
            bar = bar,
            zones = listOf(
                ZonedProgressBar.Zone("Z1", 40f, 114f, 3),
                ZonedProgressBar.Zone("Z2", 114f, 133f, 5),
                ZonedProgressBar.Zone("Z3", 133f, 152f, 8),
                ZonedProgressBar.Zone("Z4", 152f, 171f, 11),
                ZonedProgressBar.Zone("Z5", 171f, 200f, 14)
            )
        )
    }

    private fun createTestDataField(): DataField {
        return DataField(
            id = 4,
            name = "Heart Rate",
            unit = "bpm",
            category = DataFieldCategory.HEART_RATE,
            karooStreamType = "heart_rate",
            icon28 = null,
            icon40 = null
        )
    }
}
