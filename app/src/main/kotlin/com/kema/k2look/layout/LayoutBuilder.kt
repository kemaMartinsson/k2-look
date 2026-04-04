package com.kema.k2look.layout

import android.util.Log
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

        /**
         * Official text positions from ActiveLook Visual Assets README.
         * Key = font ID. Values = (txtX, txtY) relative to clipping region.
         * With rotation 4 (TOP_LR), txtX is the RIGHT edge of the rendered text.
         */
        private data class TextPosition(val txtX: Int, val txtY: Int)

        private val officialTextPositions = mapOf(
            1 to TextPosition(62, 22),    // Font 1 (24px) — e.g. battery, time
            2 to TextPosition(87, 38),    // Font 2 (38px) — half-width zones
            3 to TextPosition(194, 64),   // Font 3 (64px) — full-width zones
            4 to TextPosition(172, 75),   // Font 4 (75px) — two-data zones
            5 to TextPosition(187, 106)   // Font 5 (82px) — one-data zones
        )
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
            textConfig = run {
                val pos = officialTextPositions[zone.font]
                if (pos != null) {
                    TextConfig(x = pos.txtX, y = pos.txtY, rotation = ROTATION_TOP_LR, opacity = true)
                } else {
                    Log.w(TAG, "No official text position for font ${zone.font}, using fallback")
                    TextConfig(x = zone.width - 10, y = zone.height / 2, rotation = ROTATION_TOP_LR, opacity = true)
                }
            },
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
     * Build additional graphic commands saved with the layout definition.
     *
     * Currently returns an empty list. Labels and icons are NOT included as saved
     * sub-commands because:
     *  1. Saved sub-commands render BEFORE the main text value on the glasses.
     *  2. The main text with opacity=true draws a black background behind each
     *     character, overwriting any previously drawn sub-commands (icons, labels).
     *  3. Official ActiveLook layouts do not use saved sub-commands for labels.
     *
     * TODO: Implement labels/icons via LayoutExtraCmd sent at display time
     *       (layoutClearAndDisplayExtended). ExtraCmd draws AFTER the main text,
     *       so labels would remain visible. See reference/android-sdk/debugapp
     *       DebugActivity.java for the pattern.
     */
    private fun buildAdditionalCommands(
        @Suppress("UNUSED_PARAMETER") field: LayoutDataField,
        @Suppress("UNUSED_PARAMETER") zone: com.kema.k2look.model.LayoutZone
    ): List<GraphicCommand> {
        return emptyList()
    }
}

