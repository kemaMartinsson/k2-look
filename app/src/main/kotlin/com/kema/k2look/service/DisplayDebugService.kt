package com.kema.k2look.service

import com.kema.k2look.service.AppLog as Log
import com.activelook.activelooksdk.Glasses
import com.activelook.activelooksdk.types.holdFlushAction
import com.kema.k2look.layout.DynamicLayoutEngine
import com.kema.k2look.layout.DynamicLayoutRenderer
import com.kema.k2look.layout.LayoutPositionDefaults

// File-level aliases so debug tests can refer to the shared types without qualification
private typealias RowConfig = DynamicLayoutEngine.RowConfig
private typealias PendingIcon = DynamicLayoutRenderer.PendingIcon

/**
 * Renders calibration / diagnostic patterns directly on the ActiveLook glasses using low-level
 * graphics primitives (line, rect, txt) and saved layouts.
 *
 * Each "test" draws a known pattern so the developer can photograph the glasses and compare what
 * they see against the expected coordinates.
 *
 * Test methods are organised in separate files by category:
 * - [DisplayDebugGeometryTests]  — Tests 1–5: coordinate system, clipping, text-X
 * - [DisplayDebugFontTests]      — Tests 6–7: font comparison, ExtraCmd overlays
 * - [DisplayDebugIconTests]      — Tests 8–9: icon grid, realistic [icon][value][unit]
 * - [DisplayDebugDynamicTests]   — Tests 10–13: dynamic layout, gauge, zone circles
 *
 * Usage: triggered from the Debug tab in the app UI.
 */
class DisplayDebugService(internal val activeLookService: ActiveLookService) {

    companion object {
        private const val TAG = "DisplayDebug"

        // Exposed for extension test files
        internal const val TAG_DBG = "DisplayDebug"

        // ActiveLook display dimensions
        const val DISPLAY_W = 304
        const val DISPLAY_H = 256

        // Safe area (from ActiveLook docs: 30px horizontal, 25px vertical margins)
        const val SAFE_LEFT = 30
        const val SAFE_RIGHT = 274 // 304 - 30
        const val SAFE_TOP = 25
        const val SAFE_BOTTOM = 231 // 256 - 25

        // Debug layout IDs (high range to avoid conflicts)
        const val DBG_LAYOUT_BASE = 50

        // Config name used by all debug tests
        const val DBG_CFG = "K2Look"

        // Colors
        const val WHITE: Byte = 15
        const val MID_GREY: Byte = 8
        const val DIM: Byte = 4

        // Dynamic layout geometry — delegated to LayoutPositionDefaults
        val AVAILABLE_HEIGHT get() = LayoutPositionDefaults.AVAILABLE_HEIGHT
        val MIN_GAP get() = LayoutPositionDefaults.MIN_GAP
        val ZONE_X0 get() = LayoutPositionDefaults.ZONE_X0
        val ZONE_WIDTH get() = LayoutPositionDefaults.ZONE_WIDTH
        val ICON_ABS_X get() = LayoutPositionDefaults.ICON_ABS_X
    }

    // ════════════════════════════════════════════════════════════════════
    //  Dynamic Layout Data Structures
    // ════════════════════════════════════════════════════════════════════

    enum class DebugMetric(
        val displayValue: String,
        val unit: String,
        val icon28: Int?,
        val icon40: Int?,
        val maxChars: Int // max expected display length for ghost-char padding
    ) {
        SPEED("25.1", "km/h", 26, 58, 5),          // up to "125.1"
        POWER("1250", "w", 19, 51, 4),              // up to "1500"
        HEARTRATE("150", "bpm", 12, 44, 3),         // up to "220"
        CADENCE("185", "rpm", 4, 36, 3),            // up to "200"
        DISTANCE("42.5", "km", 9, 41, 5),           // up to "999.9"
        ELAPSED_TIME("1:23:45", "HH:MM:SS", 8, 40, 7) // "H:MM:SS"
    }

    private var rowConfigs: List<RowConfig> = emptyList()
    private val pendingIcons = mutableListOf<PendingIcon>()

    /**
     * Left-pads a display value with ActiveLook ghost characters to [maxChars] length. Only
     * effective for fonts 4/5 (digit-only). Fonts 1-3 render '$' as a visible dollar sign.
     * - `$` = invisible character with the width of digit `0`
     * - `&` = invisible character with the width of `:` or `.`
     */
    private fun ghostPad(value: String, maxChars: Int, font: Int): String {
        if (font < 4) return value
        if (value.length >= maxChars) return value
        val padCount = maxChars - value.length
        return "$".repeat(padCount) + value
    }

    // ════════════════════════════════════════════════════════════════════
    //  Dynamic Layout Helpers (used by Tests 10–13)
    // ════════════════════════════════════════════════════════════════════

    /**
     * Computes row geometry for N rows of given sizes, evenly spaced vertically.
     *
     * @param rows list of "large" (50px), "medium" (35px), or "small" (30px) row sizes,
     *             ordered viewer-top first
     * @return list of [RowConfig] with computed y0 positions and layout IDs.
     *   Trailing rows are dropped if they don't fit within [AVAILABLE_HEIGHT].
     *   Returns empty list if even a single row can't fit.
     */
    fun createLayouts(rows: List<String>): List<RowConfig> {
        pendingIcons.clear()
        val configs = DynamicLayoutEngine.createRowLayouts(
            rows,
            startLayoutId = DBG_LAYOUT_BASE + 3
        )
        rowConfigs = configs
        return configs
    }

    /**
     * Saves a layout and renders a metric value (with optional icon and unit) for one row.
     *
     * Must be called AFTER [createLayouts] and while in a writable config context (cfgWrite).
     * Icons are queued for batch rendering in PASS 2 (under ALooK config).
     *
     * @param g connected glasses instance
     * @param rowIndex 1-based row index (1 = viewer-top)
     * @param font font number: 1 (24px), 2 (38px), or 3 (64px)
     * @param metric the [DebugMetric] to display
     * @param iconSize "small" (28px), "large" (40px), or null for no icon
     * @param showUnit whether to render the unit string via ExtraCmd
     */
    fun populateLayout(
        g: Glasses,
        rowIndex: Int,
        font: Int,
        metric: DebugMetric,
        iconSize: String?,
        showUnit: Boolean
    ) {
        val row = rowConfigs.getOrNull(rowIndex - 1) ?: run {
            Log.w(TAG, "populateLayout: invalid rowIndex=$rowIndex")
            return
        }

        val iconPx = if (iconSize == "large") 40 else 28
        val iconId = if (iconSize != null) {
            if (iconSize == "large") metric.icon40 else metric.icon28
        } else null
        val hasIcon = iconId != null

        val params = DynamicLayoutRenderer.buildLayoutParams(
            layoutId = row.layoutId,
            x0 = ZONE_X0,
            y0 = row.y0,
            width = ZONE_WIDTH,
            zoneHeight = row.height,
            font = font,
            hasIcon = hasIcon,
        )
        g.layoutSave(params)
        Thread.sleep(80)

        val paddedValue = ghostPad(metric.displayValue, metric.maxChars, font)

        val (extra, renderValue) = DynamicLayoutRenderer.buildExtraCmd(
            value = paddedValue,
            unit = metric.unit,
            font = font,
            zoneHeight = row.height,
            showUnit = showUnit,
        )

        g.layoutClearAndDisplayExtended(
            row.layoutId.toByte(),
            ZONE_X0.toShort(),
            row.y0.toByte(),
            renderValue,
            extra,
        )

        if (hasIcon) {
            DynamicLayoutRenderer.queueIcon(pendingIcons, iconId!!, iconPx, row.y0, row.height)
        }

        Log.i(
            TAG,
            "populateLayout: row=$rowIndex font=$font metric=${metric.name} " +
                    "icon=$iconSize unit=$showUnit y0=${row.y0} h=${row.height}"
        )
    }

    /**
     * Renders all queued icons under ALooK config. Must be called after all [populateLayout]
     * calls and after switching to ALooK config.
     */
    fun renderPendingIcons(g: Glasses) {
        DynamicLayoutRenderer.renderPendingIcons(g, pendingIcons)
        pendingIcons.clear()
    }

    // ════════════════════════════════════════════════════════════════════
    //  Utilities
    // ════════════════════════════════════════════════════════════════════

    /** Clears everything the debug tests may have left on the display and glasses storage. */
    fun clearDisplay() {
        val g = activeLookService.getConnectedGlasses() ?: return logNoGlasses()
        try {
            g.clear()
            g.layoutDeleteAll()
            g.pageDeleteAll()
            g.gaugeDeleteAll()
            try { g.cfgDelete("K2LDBG") } catch (_: Exception) {}
            Log.i(TAG, "Display cleared + all layouts/pages/gauges/config deleted")
        } catch (e: Exception) {
            Log.e(TAG, "Clear failed: ${e.message}", e)
        }
    }

    internal fun drawCornerMarker(g: Glasses, x: Int, y: Int) {
        g.color(WHITE)
        g.rectf(x.toShort(), y.toShort(), (x + 5).toShort(), (y + 5).toShort())
    }

    internal fun safeFlush(g: Glasses) {
        try { g.holdFlush(holdFlushAction.FLUSH) } catch (_: Exception) {}
    }

    internal fun logNoGlasses() {
        Log.w(TAG, "Cannot run test — glasses not connected")
    }

    /**
     * Draws a filled circle at ([cx], [cy]) with radius [r] using horizontal scan-lines.
     *
     * The caller must set the desired colour via `g.color()` before calling this.
     */
    internal fun filledCircle(g: Glasses, cx: Int, cy: Int, r: Int) {
        for (dy in -r..r) {
            val dx = Math.sqrt((r * r - dy * dy).toDouble()).toInt()
            if (dx == 0) continue
            g.line((cx - dx).toShort(), (cy + dy).toShort(), (cx + dx).toShort(), (cy + dy).toShort())
        }
    }

    /**
     * Draws an outline circle at ([cx], [cy]) with radius [r] using a [steps]-vertex polyline.
     *
     * The caller must set the desired colour via `g.color()` before calling this.
     */
    internal fun outlineCircle(g: Glasses, cx: Int, cy: Int, r: Int, steps: Int = 24) {
        val pts = ShortArray((steps + 1) * 2)
        for (i in 0..steps) {
            val angle = 2 * Math.PI * i / steps
            pts[i * 2] = (cx + (r * Math.cos(angle)).toInt()).toShort()
            pts[i * 2 + 1] = (cy + (r * Math.sin(angle)).toInt()).toShort()
        }
        g.polyline(pts)
    }
}

