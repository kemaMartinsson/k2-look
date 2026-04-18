package com.kema.k2look.layout

import com.activelook.activelooksdk.Glasses
import com.activelook.activelooksdk.types.LayoutExtraCmd
import com.activelook.activelooksdk.types.LayoutParameters

/**
 * Stateless renderer for dynamic `[icon][value][unit]` ActiveLook layouts.
 *
 * Provides helpers to:
 * - Build `LayoutParameters` for saving a layout in a writable config context.
 * - Build a `LayoutExtraCmd` overlay for unit labels and elapsed-time seconds.
 * - Queue icons for batch `Glasses.imgDisplay` rendering under ALooK config.
 *
 * All coordinate values are derived from hardware-calibrated constants in `LayoutPositionDefaults`
 * (sessions 6–10).
 *
 * Typical call order: cfgWrite → `buildLayoutParams` + layoutSave → `buildExtraCmd` +
 * layoutClearAndDisplayExtended → `queueIcon` → cfgSet("ALooK") → `renderPendingIcons`.
 */
object DynamicLayoutRenderer {

    private const val TAG = "DynamicLayoutRenderer"

    /** An icon queued for batch rendering in the ALooK config pass. */
    data class PendingIcon(val iconId: Int, val absX: Short, val absY: Short, val iconPx: Int)

    // ── Layout save helper ────────────────────────────────────────────────

    /**
     * Builds `LayoutParameters` for saving a layout in a writable config context (cfgWrite).
     *
     * The vertical text position is adjusted when `zoneHeight` differs from the font's
     * `CalibratedFontConfig.refHeight`: `adjustedTxtY = txtY + (zoneHeight - refHeight) / 2`
     *
     * @param layoutId Layout ID (globally accessible across configs).
     * @param x0 Zone left edge in display coordinates.
     * @param y0 Zone top edge in display coordinates.
     * @param width Zone width in pixels.
     * @param zoneHeight Zone height in pixels.
     * @param font ActiveLook font number (1–3).
     * @param hasIcon True when a 28/40 px icon will be rendered viewer-left of the value.
     */
    fun buildLayoutParams(
            layoutId: Int,
            x0: Int,
            y0: Int,
            width: Int,
            zoneHeight: Int,
            font: Int,
            hasIcon: Boolean,
    ): LayoutParameters {
        val fp =
                LayoutPositionDefaults.fontConfigs[font]
                        ?: error("No CalibratedFontConfig for font $font (valid: 1, 2, 3)")
        val yAdjust = (zoneHeight - fp.refHeight) / 2
        val adjustedTxtY = (fp.txtY + yAdjust).toByte()
        val txtX = if (hasIcon) fp.txtXWithIcon else fp.txtXNoIcon
        return LayoutParameters(
                layoutId.toByte(),
                x0.toShort(),
                y0.toByte(),
                width.toShort(),
                zoneHeight.toByte(),
                15.toByte(), // foreColor WHITE
                0.toByte(), // backColor BLACK
                fp.fontId,
                true, // enable sub-command slot
                txtX,
                adjustedTxtY,
                fp.rotation,
                true, // opacity (main text draws a black bg, erasing previous frame)
        )
    }

    // ── ExtraCmd builder ─────────────────────────────────────────────────

    /**
     * Builds a `LayoutExtraCmd` overlay and computes the display value string.
     *
     * **Elapsed time** ("HH:MM:SS" unit, font ≥ 2): Strips the seconds component from the main
     * value. The main font renders "H:MM"; a font-1 overlay at the viewer-right renders ":SS"
     * top-aligned with the main value. A "MM:SS" input is normalised to "0:MM:SS" before splitting
     * to maintain consistent layout.
     *
     * **Unit label** (all other units when `showUnit` is true): Rendered in font 1 at the
     * viewer-right end of the zone.
     *
     * @param value Raw metric value string (e.g. "25.1", "1:23:45").
     * @param unit Metric unit string matching `LayoutPositionDefaults.unitXLookup` keys.
     * @param font Zone font number (1–3).
     * @param zoneHeight Actual zone height in pixels. Used to compute vertical adjustment.
     * @param showUnit Whether to add a unit label overlay.
     * @return Pair(extraCmd, renderValue). `LayoutExtraCmd` may have 0 sub-commands.
     */
    fun buildExtraCmd(
            value: String,
            unit: String,
            font: Int,
            zoneHeight: Int,
            showUnit: Boolean,
            iconPx: Int = 0,
    ): Pair<LayoutExtraCmd, String> {
        val fp = LayoutPositionDefaults.fontConfigs[font] ?: return Pair(LayoutExtraCmd(), value)
        val yAdjust = (zoneHeight - fp.refHeight) / 2
        val adjustedTxtY = fp.txtY + yAdjust // Int — convert to Short at addSubCommandText()
        val adjustedUnitY = (fp.unitY + yAdjust).toShort()

        val extra = LayoutExtraCmd()
        var renderValue = value
        var fontSet = false

        // Elapsed time: split "H:MM:SS" → main value "H:MM", overlay ":SS" in font 1
        if (unit == "HH:MM:SS" && font >= 2) {
            val normalized =
                    value.let { v ->
                        // Normalise "MM:SS" → "0:MM:SS" so the split always yields 3 parts
                        if (v.split(":").size == 2) "0:$v" else v
                    }
            val parts = normalized.split(":")
            if (parts.size == 3) {
                val secondsStr = ":${parts[2]}"
                val secondsX = LayoutPositionDefaults.secondsXFor(font)
                extra.addSubCommandFont(1.toByte())
                extra.addSubCommandText(secondsX, adjustedTxtY.toShort(), secondsStr)
                fontSet = true
                renderValue = "${parts[0]}:${parts[1]}"
            }
        }

        // Unit label — skip for "HH:MM:SS" (seconds overlay handles the split; unit is implied)
        if (showUnit && unit.isNotEmpty() && unit != "HH:MM:SS") {
            val unitX = LayoutPositionDefaults.unitXFor(unit, font, iconPx)
            if (!fontSet) extra.addSubCommandFont(1.toByte())
            extra.addSubCommandText(unitX, adjustedUnitY, unit)
        }

        return Pair(extra, renderValue)
    }

    // ── Icon queuing ──────────────────────────────────────────────────────

    /**
     * Queues an icon for batch rendering in the ALooK config pass.
     *
     * Icons cannot be rendered inline with value updates because `Glasses.imgDisplay` requires the
     * ALooK system config to be active, which must be set *after* all
     * `layoutClearAndDisplayExtended` calls.
     *
     * Icon y is centred vertically in the zone: `absY = y0 + (zoneHeight - iconPx) / 2` A +6 px
     * calibrated offset is applied for 28 px icons in 35 px zones (medium rows).
     *
     * @param pendingIcons Mutable list to append the queued icon to.
     * @param iconId ActiveLook icon ID (see Visual Assets README — ground truth only).
     * @param iconPx Icon size in pixels: 28 (small) or 40 (large).
     * @param y0 Zone top edge in display coordinates.
     * @param zoneHeight Zone height in pixels.
     */
    fun queueIcon(
            pendingIcons: MutableList<PendingIcon>,
            iconId: Int,
            iconPx: Int,
            y0: Int,
            zoneHeight: Int,
    ) {
        val baseY = y0 + (zoneHeight - iconPx) / 2
        // Empirically verified +6 px offset for 28 px icons in 35 px (medium) zones (session 10)
        val iconYOffset = if (iconPx == 28 && zoneHeight == 35) 6 else 0
        val absY = (baseY + iconYOffset).toShort()
        // Small icons (28px) shifted 5px viewer-left (higher X) for visual alignment
        val iconAbsX = (LayoutPositionDefaults.ICON_ABS_X + if (iconPx == 28) 15 else 0).toShort()
        pendingIcons.add(PendingIcon(iconId, iconAbsX, absY, iconPx))
        // Log.v(TAG, "queueIcon: id=$iconId iconPx=$iconPx y0=$y0 zoneH=$zoneHeight → absY=$absY")
    }

    // ── Icon rendering ────────────────────────────────────────────────────

    /**
     * Renders all queued icons via `Glasses.imgDisplay`.
     *
     * Must be called while the glasses are in the ALooK system config context (after
     * `glasses.cfgSet("ALooK")`). The `pendingIcons` list is NOT cleared by this function — the
     * caller is responsible for lifecycle management.
     *
     * @param g Connected glasses instance.
     * @param pendingIcons Icons to render.
     */
    fun renderPendingIcons(g: Glasses, pendingIcons: List<PendingIcon>) {
        pendingIcons.forEach { icon -> g.imgDisplay(icon.iconId.toByte(), icon.absX, icon.absY) }
        // Log.i(TAG, "renderPendingIcons: ${pendingIcons.size} icons rendered")
    }
}
