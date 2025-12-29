package com.kema.k2look.layout

import com.kema.k2look.data.DataFieldRegistry
import com.kema.k2look.model.IconSize
import com.kema.k2look.model.LayoutDataField
import com.kema.k2look.model.LayoutScreen
import com.kema.k2look.model.VisualizationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for zone-based layouts
 * Tests layout generation logic with new zone-based API
 */
class LayoutBuilderTest {

    @Test
    fun testBuildLayoutForSingleField() {
        val speedField = LayoutDataField(
            dataField = DataFieldRegistry.getById(12)!!, // Speed
            zoneId = "3D_FULL_H",
            visualizationType = VisualizationType.TEXT,
            showLabel = true,
            showUnit = true,
            showIcon = true,
            iconSize = IconSize.SMALL
        )

        val screen = LayoutScreen(
            id = 1,
            name = "Main",
            templateId = "3D_FULL",
            dataFields = listOf(speedField)
        )

        assertNotNull(screen)
        assertEquals(1, screen.dataFields.size)
        assertEquals("Speed", screen.dataFields[0].dataField.name)
    }

    @Test
    fun testBuildScreenWithMultipleFields() {
        val speedField = LayoutDataField(
            dataField = DataFieldRegistry.getById(12)!!, // Speed
            zoneId = "3D_FULL_H",
            visualizationType = VisualizationType.TEXT,
            showLabel = true,
            showUnit = true,
            showIcon = true,
            iconSize = IconSize.SMALL
        )

        val hrField = LayoutDataField(
            dataField = DataFieldRegistry.getById(4)!!, // Heart Rate
            zoneId = "3D_FULL_M",
            visualizationType = VisualizationType.TEXT,
            showLabel = true,
            showUnit = true,
            showIcon = true,
            iconSize = IconSize.LARGE
        )

        val powerField = LayoutDataField(
            dataField = DataFieldRegistry.getById(7)!!, // Power
            zoneId = "3D_FULL_L",
            visualizationType = VisualizationType.TEXT,
            showLabel = false,
            showUnit = true,
            showIcon = true,
            iconSize = IconSize.SMALL
        )

        val screen = LayoutScreen(
            id = 1,
            name = "Multi-Field",
            templateId = "3D_FULL",
            dataFields = listOf(speedField, hrField, powerField)
        )

        assertEquals(3, screen.dataFields.size)
        assertEquals("3D_FULL_H", screen.dataFields[0].zoneId)
        assertEquals("3D_FULL_M", screen.dataFields[1].zoneId)
        assertEquals("3D_FULL_L", screen.dataFields[2].zoneId)
    }

    @Test
    fun testDifferentIconSizes() {
        val fieldSmall = LayoutDataField(
            dataField = DataFieldRegistry.getById(12)!!, // Speed
            zoneId = "3D_FULL_H",
            visualizationType = VisualizationType.TEXT,
            showLabel = true,
            showUnit = true,
            showIcon = true,
            iconSize = IconSize.SMALL
        )

        val fieldLarge = LayoutDataField(
            dataField = DataFieldRegistry.getById(12)!!, // Speed
            zoneId = "3D_FULL_H",
            visualizationType = VisualizationType.TEXT,
            showLabel = true,
            showUnit = true,
            showIcon = true,
            iconSize = IconSize.LARGE
        )

        assertEquals(IconSize.SMALL, fieldSmall.iconSize)
        assertEquals(IconSize.LARGE, fieldLarge.iconSize)
    }

    @Test
    fun testAllMetricTypes() {
        val metricIds = listOf(1, 2, 4, 7, 12, 18, 24)

        metricIds.forEach { metricId ->
            val dataField = DataFieldRegistry.getById(metricId)
            assertNotNull("Metric $metricId should exist", dataField)

            val field = LayoutDataField(
                dataField = dataField!!,
                zoneId = "3D_FULL_H",
                visualizationType = VisualizationType.TEXT,
                showLabel = true,
                showUnit = true,
                showIcon = true,
                iconSize = IconSize.SMALL
            )

            assertNotNull("Field for metric $metricId should be created", field)
            assertEquals(dataField.name, field.dataField.name)
        }
    }

    @Test
    fun testDifferentZones() {
        val field = LayoutDataField(
            dataField = DataFieldRegistry.getById(12)!!, // Speed
            zoneId = "3D_FULL_H",
            visualizationType = VisualizationType.TEXT,
            showLabel = true,
            showUnit = true,
            showIcon = true,
            iconSize = IconSize.SMALL
        )

        val topField = field.copy(zoneId = "3D_FULL_H")
        val middleField = field.copy(zoneId = "3D_FULL_M")
        val bottomField = field.copy(zoneId = "3D_FULL_L")

        assertEquals("3D_FULL_H", topField.zoneId)
        assertEquals("3D_FULL_M", middleField.zoneId)
        assertEquals("3D_FULL_L", bottomField.zoneId)
    }

    @Test
    fun testVisualizationTypes() {
        val textField = LayoutDataField(
            dataField = DataFieldRegistry.getById(7)!!, // Power
            zoneId = "3D_FULL_H",
            visualizationType = VisualizationType.TEXT,
            showLabel = true,
            showUnit = true
        )

        val gaugeField = LayoutDataField(
            dataField = DataFieldRegistry.getById(7)!!, // Power
            zoneId = "3D_FULL_H",
            visualizationType = VisualizationType.GAUGE,
            gauge = com.kema.k2look.data.DefaultVisualizations.createPowerGauge()
        )

        val barField = LayoutDataField(
            dataField = DataFieldRegistry.getById(7)!!, // Power
            zoneId = "3D_FULL_H",
            visualizationType = VisualizationType.BAR,
            progressBar = com.kema.k2look.data.DefaultVisualizations.createPowerBar()
        )

        assertEquals(VisualizationType.TEXT, textField.visualizationType)
        assertEquals(VisualizationType.GAUGE, gaugeField.visualizationType)
        assertEquals(VisualizationType.BAR, barField.visualizationType)

        assertNotNull(gaugeField.gauge)
        assertNotNull(barField.progressBar)
    }

    @Test
    fun testScreenTemplateId() {
        val screen = LayoutScreen(
            id = 1,
            name = "Template Test",
            templateId = "3D_FULL",
            dataFields = emptyList()
        )

        assertEquals("3D_FULL", screen.templateId)

        val template = screen.getTemplate()
        assertNotNull(template)
        assertEquals("3D_FULL", template.id)
        assertTrue(template.zones.size >= 3)
    }
}

