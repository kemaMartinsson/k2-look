package com.kema.k2look.data

import com.kema.k2look.model.Orientation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for DefaultVisualizations
 */
class DefaultVisualizationsTest {

    @Test
    fun `createPowerGauge returns valid gauge`() {
        val gauge = DefaultVisualizations.createPowerGauge()

        assertEquals(1, gauge.id)
        assertEquals(152, gauge.centerX)
        assertEquals(128, gauge.centerY)
        assertEquals(70, gauge.radiusOuter)
        assertEquals(55, gauge.radiusInner)
        assertEquals(3, gauge.startPortion)
        assertEquals(14, gauge.endPortion)
        assertTrue(gauge.clockwise)
        assertEquals(0f, gauge.minValue, 0.01f)
        assertEquals(400f, gauge.maxValue, 0.01f)
        assertEquals("Power", gauge.dataField.name)
    }

    @Test
    fun `createHeartRateGauge returns valid gauge`() {
        val gauge = DefaultVisualizations.createHeartRateGauge()

        assertEquals(2, gauge.id)
        assertEquals(40f, gauge.minValue, 0.01f)
        assertEquals(200f, gauge.maxValue, 0.01f)
        assertEquals("Heart Rate", gauge.dataField.name)
    }

    @Test
    fun `createCadenceGauge returns full circle gauge`() {
        val gauge = DefaultVisualizations.createCadenceGauge()

        assertEquals(3, gauge.id)
        assertEquals(0, gauge.startPortion) // Full circle
        assertEquals(15, gauge.endPortion)
        assertEquals(0f, gauge.minValue, 0.01f)
        assertEquals(120f, gauge.maxValue, 0.01f)
        assertEquals("Cadence", gauge.dataField.name)
    }

    @Test
    fun `createHRZoneBar returns valid zoned bar with 5 zones`() {
        val zonedBar = DefaultVisualizations.createHRZoneBar()

        assertEquals(1, zonedBar.bar.id)
        assertEquals(Orientation.HORIZONTAL, zonedBar.bar.orientation)
        assertEquals(40f, zonedBar.bar.minValue, 0.01f)
        assertEquals(200f, zonedBar.bar.maxValue, 0.01f)
        assertEquals(5, zonedBar.zones.size)

        // Check zone names
        assertEquals("Z1", zonedBar.zones[0].name)
        assertEquals("Z2", zonedBar.zones[1].name)
        assertEquals("Z3", zonedBar.zones[2].name)
        assertEquals("Z4", zonedBar.zones[3].name)
        assertEquals("Z5", zonedBar.zones[4].name)

        // Check zones are contiguous
        assertEquals(zonedBar.zones[0].maxValue, zonedBar.zones[1].minValue, 0.01f)
        assertEquals(zonedBar.zones[1].maxValue, zonedBar.zones[2].minValue, 0.01f)
        assertEquals(zonedBar.zones[2].maxValue, zonedBar.zones[3].minValue, 0.01f)
        assertEquals(zonedBar.zones[3].maxValue, zonedBar.zones[4].minValue, 0.01f)
    }

    @Test
    fun `createPowerBar returns valid horizontal bar`() {
        val bar = DefaultVisualizations.createPowerBar()

        assertEquals(2, bar.id)
        assertEquals(Orientation.HORIZONTAL, bar.orientation)
        assertEquals(0f, bar.minValue, 0.01f)
        assertEquals(400f, bar.maxValue, 0.01f)
        assertTrue(bar.showBorder)
        assertEquals("Power", bar.dataField.name)
    }

    @Test
    fun `createSpeedBar returns valid horizontal bar`() {
        val bar = DefaultVisualizations.createSpeedBar()

        assertEquals(3, bar.id)
        assertEquals(Orientation.HORIZONTAL, bar.orientation)
        assertEquals(0f, bar.minValue, 0.01f)
        assertEquals(60f, bar.maxValue, 0.01f)
        assertEquals("Speed", bar.dataField.name)
    }

    @Test
    fun `createCadenceVerticalBar returns valid vertical bar`() {
        val bar = DefaultVisualizations.createCadenceVerticalBar()

        assertEquals(4, bar.id)
        assertEquals(Orientation.VERTICAL, bar.orientation)
        assertEquals(0f, bar.minValue, 0.01f)
        assertEquals(120f, bar.maxValue, 0.01f)
        assertEquals("Cadence", bar.dataField.name)
    }

    @Test
    fun `all gauge IDs are unique`() {
        val gaugeIds = setOf(
            DefaultVisualizations.createPowerGauge().id,
            DefaultVisualizations.createHeartRateGauge().id,
            DefaultVisualizations.createCadenceGauge().id
        )

        assertEquals(3, gaugeIds.size) // All unique
    }

    @Test
    fun `all bar IDs are unique`() {
        val barIds = setOf(
            DefaultVisualizations.createHRZoneBar().bar.id,
            DefaultVisualizations.createPowerBar().id,
            DefaultVisualizations.createSpeedBar().id,
            DefaultVisualizations.createCadenceVerticalBar().id
        )

        assertEquals(4, barIds.size) // All unique
    }

    @Test
    fun `all gauges have valid portion ranges`() {
        val gauges = listOf(
            DefaultVisualizations.createPowerGauge(),
            DefaultVisualizations.createHeartRateGauge(),
            DefaultVisualizations.createCadenceGauge()
        )

        gauges.forEach { gauge ->
            assertTrue(
                "Start portion ${gauge.startPortion} should be 0-15",
                gauge.startPortion in 0..15
            )
            assertTrue(
                "End portion ${gauge.endPortion} should be 0-15",
                gauge.endPortion in 0..15
            )
        }
    }

    @Test
    fun `all bars have positive dimensions`() {
        val bars = listOf(
            DefaultVisualizations.createHRZoneBar().bar,
            DefaultVisualizations.createPowerBar(),
            DefaultVisualizations.createSpeedBar(),
            DefaultVisualizations.createCadenceVerticalBar()
        )

        bars.forEach { bar ->
            assertTrue("Bar ${bar.id} width should be > 0", bar.width > 0)
            assertTrue("Bar ${bar.id} height should be > 0", bar.height > 0)
        }
    }

    @Test
    fun `all visualizations have valid min max ranges`() {
        val gauges = listOf(
            DefaultVisualizations.createPowerGauge(),
            DefaultVisualizations.createHeartRateGauge(),
            DefaultVisualizations.createCadenceGauge()
        )

        val bars = listOf(
            DefaultVisualizations.createHRZoneBar().bar,
            DefaultVisualizations.createPowerBar(),
            DefaultVisualizations.createSpeedBar(),
            DefaultVisualizations.createCadenceVerticalBar()
        )

        gauges.forEach { gauge ->
            assertTrue(
                "Gauge ${gauge.id} max should be > min",
                gauge.maxValue > gauge.minValue
            )
        }

        bars.forEach { bar ->
            assertTrue(
                "Bar ${bar.id} max should be > min",
                bar.maxValue > bar.minValue
            )
        }
    }

    @Test
    fun `HR zone bar zones have increasing grey levels`() {
        val zonedBar = DefaultVisualizations.createHRZoneBar()

        for (i in 0 until zonedBar.zones.size - 1) {
            val currentColor = zonedBar.zones[i].color
            val nextColor = zonedBar.zones[i + 1].color
            assertTrue(
                "Zone ${zonedBar.zones[i].name} color ($currentColor) should be < ${zonedBar.zones[i + 1].name} color ($nextColor)",
                currentColor < nextColor
            )
        }
    }
}

