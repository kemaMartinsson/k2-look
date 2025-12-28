package com.kema.k2look.layout

import android.util.Log
import com.kema.k2look.model.FontSize
import com.kema.k2look.model.IconSize
import com.kema.k2look.model.LayoutDataField
import com.kema.k2look.model.LayoutScreen
import com.kema.k2look.model.Position

/**
 * Builds ActiveLook layouts from DataField configurations
 * Handles position calculations, icon placement, and text formatting
 */
class LayoutBuilder {

    companion object {
        private const val TAG = "LayoutBuilder"

        // ActiveLook display specifications
        const val DISPLAY_WIDTH = 304
        const val DISPLAY_HEIGHT = 256
        const val SECTION_HEIGHT = 85  // 256 / 3 ≈ 85px per section

        // Layout margins and offsets
        const val MARGIN_X = 10
        const val MARGIN_Y = 5
        const val LABEL_OFFSET_Y = 15
        const val VALUE_OFFSET_Y = 45
        const val ICON_LEFT_MARGIN = 10
        const val ICON_LABEL_SPACING = 10

        // Colors
        const val COLOR_WHITE = 15
        const val COLOR_BLACK = 0

        // Text rotation (see ActiveLook API)
        const val ROTATION_TOP_LR = 4  // Top-to-bottom, left-to-right, centered
    }

    /**
     * Build a layout for a specific field position
     * @param layoutId Layout ID (1-15) for ActiveLook glasses
     * @param screen The screen containing the field
     * @param position Which position to build (TOP, MIDDLE, BOTTOM)
     * @return ActiveLookLayout ready to be encoded and sent to glasses
     */
    fun buildLayout(
        layoutId: Int,
        screen: LayoutScreen,
        position: Position
    ): ActiveLookLayout {
        Log.d(TAG, "Building layout $layoutId for position $position on screen ${screen.id}")

        val sectionY = when (position) {
            Position.TOP -> 0
            Position.MIDDLE -> SECTION_HEIGHT
            Position.BOTTOM -> SECTION_HEIGHT * 2
        }

        val field = screen.dataFields.find { it.position == position }

        if (field == null) {
            Log.d(TAG, "No field at position $position, creating empty layout")
            return createEmptyLayout(layoutId, sectionY)
        }

        Log.d(
            TAG,
            "Building layout for field: ${field.dataField.name}, font: ${field.fontSize}, icon: ${field.showIcon}"
        )

        return ActiveLookLayout(
            layoutId = layoutId,
            clippingRegion = ClippingRegion(
                x = 0,
                y = sectionY,
                width = DISPLAY_WIDTH,
                height = SECTION_HEIGHT
            ),
            foreColor = COLOR_WHITE,
            backColor = COLOR_BLACK,
            font = field.fontSize.fontId,
            textConfig = TextConfig(
                x = DISPLAY_WIDTH / 2,  // Center horizontally
                y = VALUE_OFFSET_Y,
                rotation = ROTATION_TOP_LR,
                opacity = true
            ),
            additionalCommands = buildAdditionalCommands(field, sectionY)
        )
    }

    /**
     * Build all layouts for a screen (3 layouts: TOP, MIDDLE, BOTTOM)
     * @return Map of Position to ActiveLookLayout
     */
    fun buildScreenLayouts(
        startLayoutId: Int,
        screen: LayoutScreen
    ): Map<Position, ActiveLookLayout> {
        Log.i(
            TAG,
            "Building all layouts for screen ${screen.id}, starting at layout ID $startLayoutId"
        )

        return mapOf(
            Position.TOP to buildLayout(startLayoutId, screen, Position.TOP),
            Position.MIDDLE to buildLayout(startLayoutId + 1, screen, Position.MIDDLE),
            Position.BOTTOM to buildLayout(startLayoutId + 2, screen, Position.BOTTOM)
        )
    }

    /**
     * Create an empty layout for unused positions
     */
    private fun createEmptyLayout(layoutId: Int, sectionY: Int): ActiveLookLayout {
        return ActiveLookLayout(
            layoutId = layoutId,
            clippingRegion = ClippingRegion(
                x = 0,
                y = sectionY,
                width = DISPLAY_WIDTH,
                height = SECTION_HEIGHT
            ),
            foreColor = COLOR_WHITE,
            backColor = COLOR_BLACK,
            font = FontSize.MEDIUM.fontId,
            textConfig = TextConfig(
                x = DISPLAY_WIDTH / 2,
                y = VALUE_OFFSET_Y,
                rotation = ROTATION_TOP_LR,
                opacity = true
            ),
            additionalCommands = emptyList()
        )
    }

    /**
     * Build additional graphic commands (icons, labels, separators)
     */
    private fun buildAdditionalCommands(
        field: LayoutDataField,
        sectionY: Int
    ): List<GraphicCommand> {
        val commands = mutableListOf<GraphicCommand>()

        var labelX = MARGIN_X + 5  // Start from left by default

        // Add icon if enabled
        if (field.showIcon) {
            val iconId = when (field.iconSize) {
                IconSize.SMALL -> field.dataField.icon28
                IconSize.LARGE -> field.dataField.icon40
            }

            if (iconId != null) {
                val iconSize = field.iconSize.pixels
                val iconX = ICON_LEFT_MARGIN
                val iconY = (SECTION_HEIGHT - iconSize) / 2  // Vertically center in section

                Log.d(TAG, "Adding icon $iconId at ($iconX, $iconY), size: ${iconSize}px")

                commands.add(
                    GraphicCommand.Image(
                        id = iconId,
                        x = iconX,
                        y = iconY
                    )
                )

                // Shift label right to make space for icon
                labelX = iconX + iconSize + ICON_LABEL_SPACING
            }
        }

        // Add label if enabled
        if (field.showLabel) {
            val labelText = buildLabelText(field)
            Log.d(TAG, "Adding label '$labelText' at ($labelX, $LABEL_OFFSET_Y)")

            commands.add(
                GraphicCommand.Text(
                    x = labelX,
                    y = LABEL_OFFSET_Y,
                    rotation = 0,  // Left-aligned
                    font = FontSize.SMALL.fontId,  // Labels always use small font
                    text = labelText
                )
            )
        }

        // Add separator line between sections (except for BOTTOM)
        commands.add(
            GraphicCommand.Line(
                x0 = MARGIN_X,
                y0 = SECTION_HEIGHT - 1,
                x1 = DISPLAY_WIDTH - MARGIN_X,
                y1 = SECTION_HEIGHT - 1
            )
        )

        Log.d(TAG, "Built ${commands.size} additional commands")
        return commands
    }

    /**
     * Build label text (name + optional unit)
     */
    private fun buildLabelText(field: LayoutDataField): String {
        val name = field.dataField.name.uppercase()
        return if (field.showUnit && field.dataField.unit.isNotEmpty()) {
            "$name (${field.dataField.unit})"
        } else {
            name
        }
    }
}

