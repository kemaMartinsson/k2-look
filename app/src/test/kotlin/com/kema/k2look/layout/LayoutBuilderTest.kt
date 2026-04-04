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

    // ──────────────────────────────────────────────────────────────────────
    // Regression: 3D_FULL zone geometry must match official ActiveLook values
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Locks in the official zone coordinates for the 3D_FULL template.
     *
     * If LayoutTemplateRegistry or LayoutBuilder.officialTextPositions is edited,
     * this test will fail — a prompt to also update DisplayDebugService.testThreeFieldLayout.
     *
     * Official source: ActiveLook Visual Assets README (full-width layouts):
     *   width=244, height=50, font=3, txtX=194, txtY=64
     *
     * txtY (64) > height (50) is intentional: the top 14 px of SourceSansPro
     * SemiBold 64 px are ascender headroom above the clip boundary.
     * Digit bodies are fully visible. Any additional cropping is a bug.
     *
     * Zone Y positions (bottom-edge, Y increases upward):
     *   H (top):    y=153  → absolute clip [153, 203]  — 14-px gap →
     *   M (middle): y= 89  → absolute clip [ 89, 139]  — 14-px gap →
     *   L (bottom): y= 25  → absolute clip [ 25,  75]
     */
    @Test
    fun test3DFullZoneGeometryMatchesOfficialValues() {
        val template = LayoutTemplateRegistry.getTemplate("3D_FULL")
        assertEquals("3D_FULL", template.id)
        assertEquals(3, template.zones.size)

        val h = template.zones.first { it.id == "3D_FULL_H" }
        assertEquals("3D_FULL_H x",       30, h.x)
        assertEquals("3D_FULL_H y",      153, h.y)
        assertEquals("3D_FULL_H width",  244, h.width)
        assertEquals("3D_FULL_H height",  50, h.height)
        assertEquals("3D_FULL_H font",     3, h.font)

        val m = template.zones.first { it.id == "3D_FULL_M" }
        assertEquals("3D_FULL_M x",       30, m.x)
        assertEquals("3D_FULL_M y",       89, m.y)
        assertEquals("3D_FULL_M width",  244, m.width)
        assertEquals("3D_FULL_M height",  50, m.height)
        assertEquals("3D_FULL_M font",     3, m.font)

        val l = template.zones.first { it.id == "3D_FULL_L" }
        assertEquals("3D_FULL_L x",       30, l.x)
        assertEquals("3D_FULL_L y",       25, l.y)
        assertEquals("3D_FULL_L width",  244, l.width)
        assertEquals("3D_FULL_L height",  50, l.height)
        assertEquals("3D_FULL_L font",     3, l.font)

        // LayoutBuilder must produce the official text position for font 3
        val field = LayoutDataField(
            dataField = DataFieldRegistry.getById(12)!!,
            zoneId = "3D_FULL_L",
            visualizationType = VisualizationType.TEXT,
            showLabel = false,
            showUnit = false,
            showIcon = false
        )
        val screen = LayoutScreen(id = 1, name = "T", templateId = "3D_FULL", dataFields = listOf(field))
        val layout = LayoutBuilder().buildLayout(10, field, screen)

        assertEquals("clipping x",       30, layout.clippingRegion.x)
        assertEquals("clipping y",       25, layout.clippingRegion.y)
        assertEquals("clipping width",  244, layout.clippingRegion.width)
        assertEquals("clipping height",  50, layout.clippingRegion.height)
        assertEquals("font",              3,  layout.font)
        // Official txtX for font 3 = 194 (right edge of text within clip)
        assertEquals("txtX", 194, layout.textConfig.x)
        // Official txtY for font 3 = 64  (top of text, measured upward from clip bottom)
        assertEquals("txtY",  64, layout.textConfig.y)
    }
}
