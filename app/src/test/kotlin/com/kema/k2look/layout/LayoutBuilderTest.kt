package com.kema.k2look.layout

import com.kema.k2look.data.DataFieldRegistry
import com.kema.k2look.model.FontSize
import com.kema.k2look.model.IconSize
import com.kema.k2look.model.LayoutDataField
import com.kema.k2look.model.LayoutScreen
import com.kema.k2look.model.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for LayoutBuilder
 * Tests layout generation logic
 */
class LayoutBuilderTest {

    private lateinit var layoutBuilder: LayoutBuilder

    @Before
    fun setup() {
        layoutBuilder = LayoutBuilder()
    }

    @Test
    fun testBuildLayoutForSingleField() {
        val speedField = LayoutDataField(
            dataField = DataFieldRegistry.getById(12)!!, // Speed
            position = Position.TOP,
            fontSize = FontSize.MEDIUM,
            showLabel = true,
            showUnit = true,
            showIcon = true,
            iconSize = IconSize.SMALL
        )

        val screen = LayoutScreen(
            id = 1,
            name = "Main",
            dataFields = listOf(speedField)
        )

        val layout = layoutBuilder.buildLayout(1, screen, Position.TOP)

        assertNotNull(layout)
        assertEquals(1, layout.layoutId)
    }

    @Test
    fun testBuildScreenLayouts() {
        val speedField = LayoutDataField(
            dataField = DataFieldRegistry.getById(12)!!, // Speed
            position = Position.TOP,
            fontSize = FontSize.LARGE,
            showLabel = true,
            showUnit = true,
            showIcon = true,
            iconSize = IconSize.SMALL
        )

        val hrField = LayoutDataField(
            dataField = DataFieldRegistry.getById(4)!!, // Heart Rate
            position = Position.MIDDLE,
            fontSize = FontSize.MEDIUM,
            showLabel = true,
            showUnit = true,
            showIcon = true,
            iconSize = IconSize.LARGE
        )

        val powerField = LayoutDataField(
            dataField = DataFieldRegistry.getById(7)!!, // Power
            position = Position.BOTTOM,
            fontSize = FontSize.MEDIUM,
            showLabel = false,
            showUnit = true,
            showIcon = true,
            iconSize = IconSize.SMALL
        )

        val screen = LayoutScreen(
            id = 1,
            name = "Main",
            dataFields = listOf(speedField, hrField, powerField)
        )

        val layouts = layoutBuilder.buildScreenLayouts(1, screen)

        assertEquals(3, layouts.size)
        assertTrue(layouts.containsKey(Position.TOP))
        assertTrue(layouts.containsKey(Position.MIDDLE))
        assertTrue(layouts.containsKey(Position.BOTTOM))
    }

    @Test
    fun testDifferentFontSizes() {
        val field = LayoutDataField(
            dataField = DataFieldRegistry.getById(12)!!, // Speed
            position = Position.TOP,
            fontSize = FontSize.SMALL,
            showLabel = true,
            showUnit = true,
            showIcon = true,
            iconSize = IconSize.SMALL
        )

        val screen = LayoutScreen(id = 1, name = "Main", dataFields = listOf(field))
        val layoutSmall = layoutBuilder.buildLayout(1, screen, Position.TOP)

        val fieldLarge = field.copy(fontSize = FontSize.LARGE)
        val screenLarge = LayoutScreen(id = 1, name = "Main", dataFields = listOf(fieldLarge))
        val layoutLarge = layoutBuilder.buildLayout(1, screenLarge, Position.TOP)

        assertNotNull(layoutSmall)
        assertNotNull(layoutLarge)
        assertNotEquals(layoutSmall.font, layoutLarge.font)
    }

    @Test
    fun testEmptyPosition() {
        val speedField = LayoutDataField(
            dataField = DataFieldRegistry.getById(12)!!, // Speed
            position = Position.TOP,
            fontSize = FontSize.MEDIUM,
            showLabel = true,
            showUnit = true,
            showIcon = true,
            iconSize = IconSize.SMALL
        )

        val screen = LayoutScreen(id = 1, name = "Main", dataFields = listOf(speedField))
        val middleLayout = layoutBuilder.buildLayout(2, screen, Position.MIDDLE)

        assertNotNull(middleLayout)
        assertEquals(2, middleLayout.layoutId)
    }

    @Test
    fun testAllMetricTypes() {
        val metricIds = listOf(1, 2, 4, 7, 12, 18, 24)

        metricIds.forEach { metricId ->
            val dataField = DataFieldRegistry.getById(metricId)
            assertNotNull("Metric $metricId should exist", dataField)

            val field = LayoutDataField(
                dataField = dataField!!,
                position = Position.TOP,
                fontSize = FontSize.MEDIUM,
                showLabel = true,
                showUnit = true,
                showIcon = true,
                iconSize = IconSize.SMALL
            )

            val screen = LayoutScreen(id = 1, name = "Main", dataFields = listOf(field))
            val layout = layoutBuilder.buildLayout(1, screen, Position.TOP)

            assertNotNull("Layout for metric $metricId should be created", layout)
        }
    }

    @Test
    fun testClippingRegions() {
        val field = LayoutDataField(
            dataField = DataFieldRegistry.getById(12)!!, // Speed
            position = Position.TOP,
            fontSize = FontSize.MEDIUM,
            showLabel = true,
            showUnit = true,
            showIcon = true,
            iconSize = IconSize.SMALL
        )

        val topField = field.copy(position = Position.TOP)
        val middleField = field.copy(position = Position.MIDDLE)
        val bottomField = field.copy(position = Position.BOTTOM)

        val screenTop = LayoutScreen(id = 1, name = "Main", dataFields = listOf(topField))
        val screenMiddle = LayoutScreen(id = 1, name = "Main", dataFields = listOf(middleField))
        val screenBottom = LayoutScreen(id = 1, name = "Main", dataFields = listOf(bottomField))

        val layoutTop = layoutBuilder.buildLayout(1, screenTop, Position.TOP)
        val layoutMiddle = layoutBuilder.buildLayout(2, screenMiddle, Position.MIDDLE)
        val layoutBottom = layoutBuilder.buildLayout(3, screenBottom, Position.BOTTOM)

        assertNotNull(layoutTop)
        assertNotNull(layoutMiddle)
        assertNotNull(layoutBottom)

        assertNotEquals(layoutTop.clippingRegion.y, layoutMiddle.clippingRegion.y)
        assertNotEquals(layoutMiddle.clippingRegion.y, layoutBottom.clippingRegion.y)

        assertEquals(layoutTop.clippingRegion.height, layoutMiddle.clippingRegion.height)
    }
}

