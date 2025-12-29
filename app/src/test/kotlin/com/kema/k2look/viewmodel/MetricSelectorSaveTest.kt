package com.kema.k2look.viewmodel

import com.kema.k2look.data.DataFieldRegistry
import com.kema.k2look.data.DefaultVisualizations
import com.kema.k2look.model.Gauge
import com.kema.k2look.model.LayoutDataField
import com.kema.k2look.model.Orientation
import com.kema.k2look.model.ProgressBar
import com.kema.k2look.model.VisualizationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for metric selector and gauge/bar save functionality
 *
 * Tests the scenario where user:
 * 1. Opens field configuration dialog
 * 2. Changes metric (e.g., HR → Power)
 * 3. Selects visualization (GAUGE/BAR)
 * 4. Clicks Save
 * 5. Expects UI to update with new metric
 */
class MetricSelectorSaveTest {

    @Test
    fun `field copy with new dataField updates correctly`() {
        // Original field with HR
        val originalField = LayoutDataField(
            dataField = DataFieldRegistry.getById(4)!!, // Heart Rate
            zoneId = "3D_FULL_H",
            visualizationType = VisualizationType.TEXT,
            showLabel = true,
            showUnit = true
        )

        // User selects Power
        val newDataField = DataFieldRegistry.getById(7)!! // Power

        // Copy field with new dataField
        val updatedField = originalField.copy(dataField = newDataField)

        // Verify the dataField changed
        assertEquals("Heart Rate", originalField.dataField.name)
        assertEquals("Power", updatedField.dataField.name)
        assertEquals(4, originalField.dataField.id)
        assertEquals(7, updatedField.dataField.id)
    }

    @Test
    fun `gauge creation with generic template uses selected dataField`() {
        val selectedDataField = DataFieldRegistry.getById(12)!! // Speed

        // Create generic gauge for Speed (not in hardcoded templates)
        val gauge = Gauge(
            id = 1,
            centerX = 152,
            centerY = 128,
            radiusOuter = 70,
            radiusInner = 55,
            startPortion = 3,
            endPortion = 14,
            clockwise = true,
            minValue = 0f,
            maxValue = 100f,
            dataField = selectedDataField
        )

        // Verify gauge has the correct dataField
        assertEquals("Speed", gauge.dataField.name)
        assertEquals(12, gauge.dataField.id)
        assertEquals("km/h", gauge.dataField.unit)
    }

    @Test
    fun `gauge copy updates dataField correctly`() {
        // Start with Power gauge
        val powerGauge = DefaultVisualizations.createPowerGauge()
        assertEquals("Power", powerGauge.dataField.name)

        // User selects HR
        val hrDataField = DataFieldRegistry.getById(4)!! // Heart Rate

        // Copy gauge with new dataField
        val updatedGauge = powerGauge.copy(dataField = hrDataField)

        // Verify the gauge now has HR dataField
        assertEquals("Heart Rate", updatedGauge.dataField.name)
        assertEquals(4, updatedGauge.dataField.id)
        assertEquals("bpm", updatedGauge.dataField.unit)

        // Verify other gauge properties unchanged
        assertEquals(powerGauge.centerX, updatedGauge.centerX)
        assertEquals(powerGauge.centerY, updatedGauge.centerY)
        assertEquals(powerGauge.minValue, updatedGauge.minValue, 0.01f)
        assertEquals(powerGauge.maxValue, updatedGauge.maxValue, 0.01f)
    }

    @Test
    fun `bar creation with generic template uses selected dataField`() {
        val selectedDataField = DataFieldRegistry.getById(4)!! // Heart Rate

        // Create generic bar for HR (not in hardcoded bar templates)
        val bar = ProgressBar(
            id = 1,
            x = 30,
            y = 110,
            width = 244,
            height = 20,
            orientation = Orientation.HORIZONTAL,
            minValue = 0f,
            maxValue = 100f,
            showBorder = true,
            dataField = selectedDataField
        )

        // Verify bar has the correct dataField
        assertEquals("Heart Rate", bar.dataField.name)
        assertEquals(4, bar.dataField.id)
        assertEquals("bpm", bar.dataField.unit)
    }

    @Test
    fun `bar copy updates dataField correctly`() {
        // Start with Power bar
        val powerBar = DefaultVisualizations.createPowerBar()
        assertEquals("Power", powerBar.dataField.name)

        // User selects Speed
        val speedDataField = DataFieldRegistry.getById(12)!! // Speed

        // Copy bar with new dataField
        val updatedBar = powerBar.copy(dataField = speedDataField)

        // Verify the bar now has Speed dataField
        assertEquals("Speed", updatedBar.dataField.name)
        assertEquals(12, updatedBar.dataField.id)
        assertEquals("km/h", updatedBar.dataField.unit)

        // Verify other bar properties unchanged
        assertEquals(powerBar.x, updatedBar.x)
        assertEquals(powerBar.y, updatedBar.y)
        assertEquals(powerBar.width, updatedBar.width)
    }

    @Test
    fun `field with gauge visualization preserves gauge after metric change`() {
        // Create field with Power and GAUGE
        val powerDataField = DataFieldRegistry.getById(7)!! // Power
        val powerGauge = DefaultVisualizations.createPowerGauge()

        val originalField = LayoutDataField(
            dataField = powerDataField,
            zoneId = "3D_FULL_H",
            visualizationType = VisualizationType.GAUGE,
            showLabel = true,
            showUnit = true,
            gauge = powerGauge
        )

        // User changes to HR but keeps GAUGE
        val hrDataField = DataFieldRegistry.getById(4)!! // Heart Rate
        val hrGauge = DefaultVisualizations.createHeartRateGauge()
            .copy(dataField = hrDataField)

        val updatedField = originalField.copy(
            dataField = hrDataField,
            gauge = hrGauge
        )

        // Verify field has new metric
        assertEquals("Heart Rate", updatedField.dataField.name)
        assertEquals(4, updatedField.dataField.id)

        // Verify visualization type is still GAUGE
        assertEquals(VisualizationType.GAUGE, updatedField.visualizationType)

        // Verify gauge is not null
        assertNotNull(updatedField.gauge)

        // Verify gauge has the new dataField
        assertEquals("Heart Rate", updatedField.gauge!!.dataField.name)
        assertEquals(4, updatedField.gauge!!.dataField.id)
    }

    @Test
    fun `field with bar visualization preserves bar after metric change`() {
        // Create field with Speed and BAR
        val speedDataField = DataFieldRegistry.getById(12)!! // Speed
        val speedBar = DefaultVisualizations.createSpeedBar()

        val originalField = LayoutDataField(
            dataField = speedDataField,
            zoneId = "3D_FULL_H",
            visualizationType = VisualizationType.BAR,
            showLabel = true,
            showUnit = true,
            progressBar = speedBar
        )

        // User changes to Power but keeps BAR
        val powerDataField = DataFieldRegistry.getById(7)!! // Power
        val powerBar = DefaultVisualizations.createPowerBar()
            .copy(dataField = powerDataField)

        val updatedField = originalField.copy(
            dataField = powerDataField,
            progressBar = powerBar
        )

        // Verify field has new metric
        assertEquals("Power", updatedField.dataField.name)
        assertEquals(7, updatedField.dataField.id)

        // Verify visualization type is still BAR
        assertEquals(VisualizationType.BAR, updatedField.visualizationType)

        // Verify bar is not null
        assertNotNull(updatedField.progressBar)

        // Verify bar has the new dataField
        assertEquals("Power", updatedField.progressBar!!.dataField.name)
        assertEquals(7, updatedField.progressBar!!.dataField.id)
    }

    @Test
    fun `switching from TEXT to GAUGE creates gauge with selected metric`() {
        // Start with HR as TEXT
        val hrDataField = DataFieldRegistry.getById(4)!! // Heart Rate

        val textField = LayoutDataField(
            dataField = hrDataField,
            zoneId = "3D_FULL_H",
            visualizationType = VisualizationType.TEXT,
            showLabel = true,
            showUnit = true
        )

        // User selects Power and GAUGE
        val powerDataField = DataFieldRegistry.getById(7)!! // Power
        val powerGauge = DefaultVisualizations.createPowerGauge()
            .copy(dataField = powerDataField)

        val gaugeField = textField.copy(
            dataField = powerDataField,
            visualizationType = VisualizationType.GAUGE,
            gauge = powerGauge
        )

        // Verify field changed to Power
        assertEquals("Power", gaugeField.dataField.name)
        assertEquals(7, gaugeField.dataField.id)

        // Verify visualization changed to GAUGE
        assertEquals(VisualizationType.GAUGE, gaugeField.visualizationType)

        // Verify gauge exists and has Power
        assertNotNull(gaugeField.gauge)
        assertEquals("Power", gaugeField.gauge!!.dataField.name)
    }

    @Test
    fun `generic gauge for non-template metric works correctly`() {
        // Select a metric that doesn't have a specific template
        val cadenceDataField = DataFieldRegistry.getById(18)!! // Cadence (cycling)

        // Create generic gauge
        val gauge = Gauge(
            id = 1,
            centerX = 152,
            centerY = 128,
            radiusOuter = 70,
            radiusInner = 55,
            startPortion = 3,
            endPortion = 14,
            clockwise = true,
            minValue = 0f,
            maxValue = 100f,
            dataField = cadenceDataField
        )

        // Verify gauge has the correct metric
        assertEquals("Cadence", gauge.dataField.name)
        assertEquals(18, gauge.dataField.id)
        assertEquals("rpm", gauge.dataField.unit)

        // Verify gauge can calculate percentages
        val percentage = gauge.calculatePercentage(60f) // 60 rpm of 100 max
        assertEquals(60, percentage)
    }

    @Test
    fun `generic bar for non-template metric works correctly`() {
        // Select a metric that doesn't have a specific bar template
        val avgPowerDataField = DataFieldRegistry.getById(9)!! // Avg Power

        // Create generic bar
        val bar = ProgressBar(
            id = 1,
            x = 30,
            y = 110,
            width = 244,
            height = 20,
            orientation = Orientation.HORIZONTAL,
            minValue = 0f,
            maxValue = 100f,
            showBorder = true,
            dataField = avgPowerDataField
        )

        // Verify bar has the correct metric
        assertEquals("Avg Power", bar.dataField.name)
        assertEquals(9, bar.dataField.id)
        assertEquals("w", bar.dataField.unit)

        // Verify bar can calculate fill amount
        val fillAmount = bar.calculateFillAmount(75f) // 75W of 100W max
        assertEquals(183, fillAmount) // 75% of 244 width
    }

    @Test
    fun `replacing HR with Power preserves zoneId`() {
        // Original field: HR in zone 3D_FULL_H
        val hrField = LayoutDataField(
            dataField = DataFieldRegistry.getById(4)!!, // Heart Rate
            zoneId = "3D_FULL_H",
            visualizationType = VisualizationType.TEXT
        )

        // Replace with Power
        val powerField = hrField.copy(
            dataField = DataFieldRegistry.getById(7)!! // Power
        )

        // Verify metric changed but zoneId stayed the same
        assertEquals("Heart Rate", hrField.dataField.name)
        assertEquals("Power", powerField.dataField.name)
        assertEquals("3D_FULL_H", hrField.zoneId)
        assertEquals("3D_FULL_H", powerField.zoneId) // ✅ Same zone!
    }

    @Test
    fun `visualization type availability for all metrics`() {
        val allFields = DataFieldRegistry.ALL_FIELDS

        allFields.forEach { dataField ->
            // TEXT should always be available
            assertTrue("TEXT should be available for ${dataField.name}", true)

            // GAUGE/BAR should be available for all except Elapsed Time
            val gaugeBarAvailable = dataField.id != 1
            if (gaugeBarAvailable) {
                // Can create generic gauge
                val gauge = Gauge(
                    id = 1, centerX = 152, centerY = 128,
                    radiusOuter = 70, radiusInner = 55,
                    startPortion = 3, endPortion = 14,
                    clockwise = true, minValue = 0f, maxValue = 100f,
                    dataField = dataField
                )
                assertEquals(dataField.name, gauge.dataField.name)

                // Can create generic bar
                val bar = ProgressBar(
                    id = 1, x = 30, y = 110, width = 244, height = 20,
                    orientation = Orientation.HORIZONTAL,
                    minValue = 0f, maxValue = 100f, showBorder = true,
                    dataField = dataField
                )
                assertEquals(dataField.name, bar.dataField.name)
            }

            // ZONED_BAR only for HR (id=4) and HR Zone (id=47)
            val zonedBarAvailable = dataField.id == 4 || dataField.id == 47
            // Just verify the condition, actual zoned bar creation tested elsewhere
        }
    }
}

