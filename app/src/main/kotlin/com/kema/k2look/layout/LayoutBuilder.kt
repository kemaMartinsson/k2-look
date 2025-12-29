package com.kema.k2look.layout

import android.util.Log
import com.kema.k2look.model.FontSize
import com.kema.k2look.model.IconSize
import com.kema.k2look.model.LayoutDataField
import com.kema.k2look.model.LayoutScreen

/**
 * Builds ActiveLook layouts from DataField configurations using zone-based positioning
 * Handles precise zone positioning, icon placement, and text formatting based on
 * ActiveLook's official layout templates
 */
class LayoutBuilder {

    companion object {
        private const val TAG = "LayoutBuilder"

        // ActiveLook display specifications
        const val DISPLAY_WIDTH = 304
        const val DISPLAY_HEIGHT = 256

        // Colors
        const val COLOR_WHITE = 15
        const val COLOR_BLACK = 0

        // Text rotation (see ActiveLook API)
        const val ROTATION_TOP_LR = 4  // Top-to-bottom, left-to-right, centered

        // Layout margins
        const val ICON_MARGIN = 5
        const val TEXT_MARGIN = 10
    }

    /**
     * Build a layout for a specific field in a zone
     * @param layoutId Layout ID (1-15) for ActiveLook glasses
     * @param field The data field configuration
     * @return ActiveLookLayout ready to be encoded and sent to glasses
     */
    fun buildLayout(
        layoutId: Int,
        field: LayoutDataField,
        screen: LayoutScreen
    ): ActiveLookLayout {
        val template = screen.getTemplate()
        val zone = template.zones.find { it.id == field.zoneId }
            ?: throw IllegalArgumentException("Zone ${field.zoneId} not found in template ${template.id}")

        Log.d(
            TAG,
            "Building layout $layoutId for zone ${zone.displayName} (${zone.id}), field: ${field.dataField.name}"
        )

        return ActiveLookLayout(
            layoutId = layoutId,
            clippingRegion = ClippingRegion(
                x = zone.x,
                y = zone.y,
                width = zone.width,
                height = zone.height
            ),
            foreColor = COLOR_WHITE,
            backColor = COLOR_BLACK,
            font = zone.font,
            textConfig = TextConfig(
                x = zone.width - TEXT_MARGIN,  // Right-aligned
                y = zone.height / 2,  // Vertically centered
                rotation = ROTATION_TOP_LR,
                opacity = true
            ),
            additionalCommands = buildAdditionalCommands(field, zone)
        )
    }

    /**
     * Build all layouts for a screen
     * @return Map of zone ID to ActiveLookLayout
     */
    fun buildScreenLayouts(
        startLayoutId: Int,
        screen: LayoutScreen
    ): Map<String, ActiveLookLayout> {
        Log.i(
            TAG,
            "Building all layouts for screen ${screen.id} (template: ${screen.templateId}), starting at layout ID $startLayoutId"
        )

        return screen.dataFields.mapIndexed { index, field ->
            field.zoneId to buildLayout(startLayoutId + index, field, screen)
        }.toMap()
    }

    /**
     * Build additional graphic commands (icons, labels)
     */
    private fun buildAdditionalCommands(
        field: LayoutDataField,
        zone: com.kema.k2look.model.LayoutZone
    ): List<GraphicCommand> {
        val commands = mutableListOf<GraphicCommand>()

        var textOffsetX = 0

        // Add icon if enabled
        if (field.showIcon) {
            val iconId = when (field.iconSize) {
                IconSize.SMALL -> field.dataField.icon28
                IconSize.LARGE -> field.dataField.icon40
            }

            if (iconId != null) {
                val iconSize = field.iconSize.pixels
                val iconX = ICON_MARGIN
                val iconY = (zone.height - iconSize) / 2  // Vertically center in zone

                Log.d(TAG, "Adding icon $iconId at ($iconX, $iconY), size: ${iconSize}px")

                commands.add(
                    GraphicCommand.Image(
                        id = iconId,
                        x = iconX,
                        y = iconY
                    )
                )

                textOffsetX = iconSize + ICON_MARGIN * 2
            }
        }

        // Add label if enabled
        if (field.showLabel) {
            val labelText = buildLabelText(field)
            val labelX = textOffsetX + 5
            val labelY = 5  // Near top of zone

            Log.d(TAG, "Adding label '$labelText' at ($labelX, $labelY)")

            commands.add(
                GraphicCommand.Text(
                    x = labelX,
                    y = labelY,
                    rotation = 0,  // Left-aligned
                    font = FontSize.SMALL.fontId,  // Labels always use small font
                    text = labelText
                )
            )
        }

        Log.d(TAG, "Built ${commands.size} additional commands for zone ${zone.id}")
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

