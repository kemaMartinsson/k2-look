package com.kema.k2look.layout

import android.util.Log
import com.kema.k2look.layout.LayoutPositionDefaults.AVAILABLE_HEIGHT
import com.kema.k2look.layout.LayoutPositionDefaults.MIN_GAP
import com.kema.k2look.layout.LayoutPositionDefaults.ZONE_WIDTH
import com.kema.k2look.layout.LayoutPositionDefaults.ZONE_X0

/**
 * Stateless row layout geometry engine.
 *
 * Computes the y0 and height for each row in a dynamic multi-row display layout, distributing rows
 * evenly within the available vertical space with equal gaps between them.
 *
 * Row order matches the viewer perspective: row 1 is viewer-top (highest display-y), row N is
 * viewer-bottom (lowest display-y).
 */
object DynamicLayoutEngine {

    private const val TAG = "DynamicLayoutEngine"

    /**
     * Geometry for a single display row.
     *
     * @param layoutId ActiveLook layout ID assigned to this row (global across configs).
     * @param y0 Top edge of the clipping region in display coordinates.
     * @param height Height of the clipping region in pixels.
     * @param size Row size label: "large" (50 px), "medium" (35 px), or "small" (30 px).
     */
    data class RowConfig(val layoutId: Int, val y0: Int, val height: Int, val size: String)

    /**
     * Computes row geometry for a list of row size labels and assigns layout IDs.
     *
     * Rows are trimmed from the end until the remaining rows (plus [MIN_GAP] gaps between them) fit
     * within [AVAILABLE_HEIGHT]. The surviving rows are distributed edge-to-edge with equal gaps:
     * row 0 at the viewer-top (highest y0), row N-1 at the viewer-bottom (lowest y0).
     *
     * @param rows Ordered size labels. First entry = viewer-top row.
     * @param startLayoutId First layout ID to assign; incremented for each subsequent row.
     * @return Ordered [RowConfig] list, or empty if no single row fits.
     */
    fun createRowLayouts(rows: List<String>, startLayoutId: Int): List<RowConfig> {
        val heights = rows.map { sizeToHeight(it) }.toMutableList()

        // Trim trailing rows until the remaining set fits within the available height
        while (heights.isNotEmpty()) {
            val totalH = heights.sum()
            val gaps = if (heights.size > 1) (heights.size - 1) * MIN_GAP else 0
            if (totalH + gaps <= AVAILABLE_HEIGHT) break
            heights.removeAt(heights.lastIndex)
        }

        if (heights.isEmpty()) return emptyList()

        val n = heights.size
        val totalH = heights.sum()
        val configs = mutableListOf<RowConfig>()

        if (n == 1) {
            // Centre the single row vertically
            val y0 = (AVAILABLE_HEIGHT - heights[0]) / 2
            configs.add(RowConfig(startLayoutId, y0, heights[0], rows[0]))
        } else {
            // Edge-to-edge: first row at viewer-top (highest y0), last at viewer-bottom (lowest y0)
            val gap = (AVAILABLE_HEIGHT - totalH) / (n - 1)
            var currentY = AVAILABLE_HEIGHT - heights[0]
            for (i in 0 until n) {
                if (i > 0) currentY -= gap + heights[i]
                configs.add(RowConfig(startLayoutId + i, currentY, heights[i], rows[i]))
            }
        }

        Log.i(
                TAG,
                "createRowLayouts: ${configs.size} rows — ${configs.map { "id=${it.layoutId} y0=${it.y0} h=${it.height}" }}"
        )
        return configs
    }

    private fun sizeToHeight(size: String): Int =
            when (size) {
                "large" -> 50
                "medium" -> 35
                "gauge" -> GAUGE_HEIGHT
                else -> 30 // "small"
            }

    // ── Gauge row support ─────────────────────────────────────────────────

    /** Height in pixels for a gauge row (~2 stacked small rows + gap = 65 px). */
    const val GAUGE_HEIGHT = 65

    /** Arc thickness in pixels (rOuter − rInner). */
    private const val GAUGE_ARC_THICKNESS = 12

    /**
     * Specification for a gauge arc visualisation row.
     *
     * @param steps Number of discrete fill levels. Examples:
     * ```
     *   steps=4 → [0, 33, 66, 100]  (original test-11 behaviour)
     *   steps=5 → [0, 25, 50, 75, 100]  (HR/power zones)
     *   steps=7 → [0, 17, 33, 50, 67, 83, 100]  (7-zone power model)
     * ```
     * @param columns Number of gauges per row (1 = full-width, 2 = side-by-side half-width).
     * @param startPortion Arc start portion (1–15; 0 is a firmware sentinel and must not be used).
     * @param endPortion Arc end portion (1–15).
     * @param clockwise Fill direction on the display.
     */
    data class GaugeSpec(
            val steps: Int = 5,
            val columns: Int = 1,
            val startPortion: Byte = 4,
            val endPortion: Byte = 12,
            val clockwise: Boolean = true,
    )

    /**
     * Pre-computed geometry for a single gauge arc within a row.
     *
     * @param gaugeId ActiveLook gauge ID.
     * @param cx Absolute display-x of the gauge centre.
     * @param cy Absolute display-y of the gauge centre.
     * @param textY Absolute display-y for the value/unit label overlay (cy + 25, calibrated).
     * @param rOuter Outer radius in pixels.
     * @param rInner Inner radius in pixels.
     * @param startPortion Arc start portion.
     * @param endPortion Arc end portion.
     * @param clockwise Fill direction.
     * @param fillLevels Ordered fill percentages derived from [GaugeSpec.steps].
     */
    data class GaugeConfig(
            val gaugeId: Byte,
            val cx: Short,
            val cy: Short,
            val textY: Short,
            val rOuter: Int,
            val rInner: Int,
            val startPortion: Byte,
            val endPortion: Byte,
            val clockwise: Boolean,
            val fillLevels: List<Int>,
    )

    /**
     * Geometry for a row containing one or two side-by-side gauges.
     *
     * @param y0 Top edge of the row in display coordinates.
     * @param height Row height in pixels (always [GAUGE_HEIGHT]).
     * @param gauges One or two [GaugeConfig] entries.
     */
    data class GaugeRowConfig(
            val y0: Int,
            val height: Int,
            val gauges: List<GaugeConfig>,
    )

    /**
     * Computes gauge geometry for the given [spec] placed at [y0].
     *
     * The arc is sized to fill [GAUGE_HEIGHT] with 2 px margins top and bottom, and centred
     * horizontally within each column of the 244-wide safe zone (x0=30).
     *
     * For [GaugeSpec.columns]=2 the two gauge centres are at one-quarter and three-quarter of the
     * zone width — each gauge spans its own 122 px half-column.
     *
     * @param y0 Top edge of the row in display coordinates.
     * @param spec Gauge visualisation specification.
     * @param startGaugeId First ActiveLook gauge ID to assign; incremented per column.
     */
    /** Vertical offset from gauge centre to the value/unit text label (calibrated in test 11). */
    private const val GAUGE_TEXT_Y_OFFSET = 25

    fun createGaugeRow(
            y0: Int,
            spec: GaugeSpec,
            startGaugeId: Int = 1,
    ): GaugeRowConfig {
        val height = GAUGE_HEIGHT
        val rOuter = height / 2 - 2
        val rInner = maxOf(rOuter - GAUGE_ARC_THICKNESS, 1)
        val cy = (y0 + height / 2).toShort()
        val textY = (cy + GAUGE_TEXT_Y_OFFSET).toShort()
        val fillLevels = computeFillLevels(spec.steps)
        val colWidth = ZONE_WIDTH / spec.columns

        val gauges =
                (0 until spec.columns).map { col ->
                    val cxRel = colWidth / 2 + col * colWidth
                    GaugeConfig(
                            gaugeId = (startGaugeId + col).toByte(),
                            cx = (ZONE_X0 + cxRel).toShort(),
                            cy = cy,
                            textY = textY,
                            rOuter = rOuter,
                            rInner = rInner,
                            startPortion = spec.startPortion,
                            endPortion = spec.endPortion,
                            clockwise = spec.clockwise,
                            fillLevels = fillLevels,
                    )
                }

        Log.i(
                TAG,
                "createGaugeRow: y0=$y0 h=$height cols=${spec.columns} rOuter=$rOuter " +
                        "gauges=${gauges.map { "id=${it.gaugeId} cx=${it.cx} cy=${it.cy}" }}"
        )
        return GaugeRowConfig(y0 = y0, height = height, gauges = gauges)
    }

    /**
     * Computes evenly distributed fill percentages for [steps] animation steps.
     *
     * - steps=4 → [0, 33, 66, 100]
     * - steps=5 → [0, 25, 50, 75, 100]
     * - steps=7 → [0, 17, 33, 50, 67, 83, 100]
     */
    fun computeFillLevels(steps: Int): List<Int> {
        if (steps <= 1) return listOf(0)
        return (0 until steps).map { i -> (i * 100) / (steps - 1) }
    }

    // ── Zone-based gauge support ──────────────────────────────────────────

    /**
     * A single named threshold zone for a gauge visualisation (e.g. HR Zone 2 or Power Zone 3).
     *
     * The zone is active when the current metric value is in the range [minValue, maxValue). The
     * last zone should have [maxValue] equal to the maximum expected metric value.
     *
     * @param name Display label shown in the DataFieldBuilder zone list (e.g. "Z1", "Z2 —
     * Endurance").
     * @param minValue Inclusive lower bound of the zone.
     * @param maxValue Exclusive upper bound of the zone (inclusive for the final zone).
     */
    data class ZoneDefinition(
            val name: String,
            val minValue: Float,
            val maxValue: Float,
    )

    /**
     * A [GaugeSpec] extended with named threshold zones for use in the DataFieldBuilder.
     *
     * When a metric value arrives at runtime, call [zoneValueToFillPercent] with the value and this
     * spec's [zones] list to obtain the fill percentage to pass to `gaugeDisplay()`.
     *
     * Example — 5-zone HR gauge (max HR = 200 bpm):
     * ```kotlin
     * ZonedGaugeSpec(
     *     zones = listOf(
     *         ZoneDefinition("Z1 Recovery",  0f,  120f),
     *         ZoneDefinition("Z2 Endurance", 120f, 140f),
     *         ZoneDefinition("Z3 Tempo",     140f, 160f),
     *         ZoneDefinition("Z4 Threshold", 160f, 180f),
     *         ZoneDefinition("Z5 VO2max",    180f, 200f),
     *     )
     * )
     * ```
     *
     * Example — 7-zone power gauge (FTP = 250 W):
     * ```kotlin
     * ZonedGaugeSpec(
     *     zones = listOf(
     *         ZoneDefinition("Z1 Active Recovery", 0f,   150f),
     *         ZoneDefinition("Z2 Endurance",       150f, 200f),
     *         ZoneDefinition("Z3 Tempo",           200f, 225f),
     *         ZoneDefinition("Z4 Threshold",       225f, 275f),
     *         ZoneDefinition("Z5 VO2max",          275f, 325f),
     *         ZoneDefinition("Z6 Anaerobic",       325f, 400f),
     *         ZoneDefinition("Z7 Neuromuscular",   400f, 600f),
     *     )
     * )
     * ```
     *
     * @param zones Ordered zone definitions from lowest to highest value. Must not be empty.
     * @param gaugeSpec Arc geometry and direction defaults.
     */
    data class ZonedGaugeSpec(
            val zones: List<ZoneDefinition>,
            val gaugeSpec: GaugeSpec = GaugeSpec(),
    ) {
        init {
            require(zones.isNotEmpty()) { "ZonedGaugeSpec requires at least one zone" }
        }

        /** Number of fill steps equals the number of zones. */
        val steps: Int
            get() = zones.size
    }

    /**
     * Maps a live metric [value] to a gauge fill percentage (0–100) using [zones].
     *
     * The fill percentage is the fractional position of [value] within its zone, offset to place
     * the zone at its proportional position on the arc.
     *
     * - Below the first zone min → 0 %
     * - Above the last zone max → 100 %
     * - Within zone i (0-indexed of N): fill = (i / N + fraction_within_zone / N) × 100
     *
     * @param value Current metric value (e.g. bpm or watts).
     * @param zones Zone definitions, ordered low → high, matching a [ZonedGaugeSpec].
     */
    fun zoneValueToFillPercent(value: Float, zones: List<ZoneDefinition>): Int {
        if (zones.isEmpty()) return 0
        val n = zones.size
        val overall_min = zones.first().minValue
        val overall_max = zones.last().maxValue
        if (value <= overall_min) return 0
        if (value >= overall_max) return 100
        val activeZoneIndex = zones.indexOfFirst { value < it.maxValue }
        if (activeZoneIndex < 0) return 100
        val zone = zones[activeZoneIndex]
        val fractionWithinZone =
                if (zone.maxValue > zone.minValue) {
                    ((value - zone.minValue) / (zone.maxValue - zone.minValue)).coerceIn(0f, 1f)
                } else 0f
        val fillFraction = (activeZoneIndex + fractionWithinZone) / n
        return (fillFraction * 100).toInt().coerceIn(0, 100)
    }
}
