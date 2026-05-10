package com.kema.k2look.layout

import com.activelook.activelooksdk.types.Rotation

/**
 * Hardware-calibrated display constants and font configuration for ActiveLook glasses.
 *
 * All values are empirically verified on physical hardware (sessions 6–10). Used by both the
 * production render pipeline ([DynamicLayoutRenderer]) and debug tests (
 * [com.kema.k2look.service.DisplayDebugService]).
 */
object LayoutPositionDefaults {

        // ── Display geometry ──────────────────────────────────────────────────

        /** Total vertical space available for dynamic row layout (display y=0..246). */
        const val AVAILABLE_HEIGHT = 246

        /** Minimum pixel gap between adjacent rows when trimming overflow. */
        const val MIN_GAP = 2

        /** Left edge of the safe render area (x0 for all standard zones). */
        const val ZONE_X0 = 30

        /** Width of the safe render area in pixels. */
        const val ZONE_WIDTH = 244

        /**
         * Absolute display-x coordinate for [com.activelook.activelooksdk.Glasses.imgDisplay].
         * Icons are placed at the viewer-left end of the display (high display-x). Calibrated in
         * session 9.
         */
        val ICON_ABS_X: Short = 260

        // ── Font configuration ────────────────────────────────────────────────

        /**
         * Hardware-calibrated parameters for a single ActiveLook font (1–3).
         *
         * @param fontId ActiveLook font number.
         * @param txtY Vertical text anchor (relative to zone top) for the main value.
         * @param rotation Text rotation. Always [Rotation.TOP_LR] for cycling metrics.
         * @param unitY Vertical anchor for a font-1 unit/annotation overlay.
         * @param refHeight Zone height at which [txtY] and [unitY] were calibrated.
         * ```
         *                       A vertical adjustment is applied when rendering in zones of
         *                       different heights: `yAdjust = (actualHeight - refHeight) / 2`.
         * @param txtXWithIcon
         * ```
         * Horizontal anchor when a 28/40px icon is present viewer-left.
         * @param txtXNoIcon Horizontal anchor when no icon is present (right edge = full zone).
         */
        data class CalibratedFontConfig(
                val fontId: Byte,
                val txtY: Byte,
                val rotation: Rotation,
                val unitY: Short,
                val refHeight: Int,
                val txtXWithIcon: Short,
                val txtXNoIcon: Short = 244,
        )

        /**
         * Calibrated font configs for fonts 1–3. Fonts 4/5 are digit-only (no letter support) and
         * are excluded from this map.
         */
        val fontConfigs: Map<Int, CalibratedFontConfig> =
                mapOf(
                        1 to
                                CalibratedFontConfig(
                                        fontId = 1.toByte(),
                                        txtY = 25.toByte(),
                                        rotation = Rotation.TOP_LR,
                                        unitY = 25.toShort(),
                                        refHeight = 30,
                                        txtXWithIcon = 178,
                                ),
                        2 to
                                CalibratedFontConfig(
                                        fontId = 2.toByte(),
                                        txtY = 35.toByte(),
                                        rotation = Rotation.TOP_LR,
                                        unitY = 40.toShort(),
                                        refHeight = 35,
                                        txtXWithIcon = 208,
                                ),
                        3 to
                                CalibratedFontConfig(
                                        fontId = 3.toByte(),
                                        txtY = 47.toByte(),
                                        rotation = Rotation.TOP_LR,
                                        unitY = 46.toShort(),
                                        refHeight = 50,
                                        txtXWithIcon = 220,
                                ),
                )

        // ── Elapsed-time seconds x positions ─────────────────────────────────

        /**
         * Relative-x for the ":SS" seconds component in elapsed-time split rendering. Keyed by zone
         * font number. Calibrated on hardware (session 10). Font 1 has no entry because the split
         * is only needed for larger fonts (2 and 3).
         */
        private val secondsXByFont: Map<Int, Short> =
                mapOf(
                        2 to 153.toShort(),
                        3 to 145.toShort(),
                )

        /** Returns the ":SS" overlay x position for [font], defaulting to 153 if not found. */
        fun secondsXFor(font: Int): Short = secondsXByFont[font] ?: 153.toShort()

        // ── Unit label x positions ────────────────────────────────────────────

        /**
         * Relative-x for unit label overlays rendered in font 1, viewer-right side of the zone.
         *
         * With TOP_LR rotation, text flows viewer-left from the anchor, so a lower x value places
         * the text further viewer-right. Values are calibrated for the standard 244px zone.
         *
         * Entries marked "Calibrated" were measured on hardware. Others are estimates based on
         * character widths and should be verified before shipping.
         */
        val unitXLookup: Map<String, Short> =
                mapOf(
                        // Calibrated on hardware — reference anchor: km/h=165 (~50px wide)
                        // Right-alignment: shorter units get higher X so their right edge
                        // aligns with km/h's right edge. Each ~12px of width difference ≈ 4-5 X.
                        "km/h" to 165.toShort(), // 4 chars, ~50px — calibrated
                        "bpm" to 165.toShort(), // 3 chars, ~42px — calibrated
                        "rpm" to 165.toShort(), // 3 chars, ~38px
                        "w" to 148.toShort(), // 1 char,  ~16px
                        "m" to 150.toShort(), // 1 char,  ~16px
                        "%" to 178.toShort(), // 1 char,  ~14px
                        "z" to 180.toShort(), // 1 char,  ~11px (zone label)
                        "km" to 173.toShort(), // 2 chars, ~29px
                        "mi" to 175.toShort(), // 2 chars, ~21px
                        "ft" to 179.toShort(), // 2 chars, ~16px
                        "kJ" to 175.toShort(), // 2 chars, ~23px
                        "°C" to 174.toShort(), // 2 chars, ~23px
                        "°F" to 174.toShort(), // 2 chars, ~21px
                        "mph" to 168.toShort(), // 3 chars, ~42px (m is wide)
                        "m/h" to 150.toShort(), // 3 chars, ~37px
                        "w/kg" to 165.toShort(), // 4 chars, ~50px — same as km/h
                        "kcal" to 150.toShort(), // 4 chars, ~41px ('l' narrow)
                        "kcal/h" to 150.toShort(), // 6 chars, ~62px
                        "HH:MM:SS" to 130.toShort(), // rarely shown — seconds rendered separately
                        "HH:MM" to 152.toShort(), // 5 chars + colon
                        "MM:SS" to 152.toShort(), // 5 chars + colon
                        "" to 0.toShort(),
                )

        private val defaultUnitX: Short = 100

        /**
         * Per-font horizontal offset applied on top of [unitXLookup] values.
         *
         * Larger fonts produce wider value text that extends further viewer-right from its anchor,
         * so the unit label must shift viewer-right (lower X) to avoid overlapping it. Font 2 is
         * the calibration baseline (offset 0). Adjust font 3 offset after hardware verification.
         */
        private val unitXFontOffset: Map<Int, Int> =
                mapOf(
                        1 to 0, // font 1 — adjust after hardware calibration
                        2 to 10, // font 2 value is wider than font 1; close the gap viewer-left
                        3 to -20, // font 3 value text is ~120px wide; shift unit viewer-right
                )

        /**
         * Per-icon-size horizontal offset applied on top of [unitXLookup] + [unitXFontOffset].
         *
         * A smaller icon leaves more horizontal room for the value text (same [txtXWithIcon]
         * anchor), so the unit must shift further viewer-right (lower X) to avoid overlap. 0 = no
         * icon (baseline). Adjust after hardware verification.
         */
        private val unitXIconOffset: Map<Int, Int> =
                mapOf(
                        0 to 28, // no icon — baseline
                        28 to 7, // small icon — value can extend further viewer-right
                        40 to 2, // large icon — baseline (row 3 calibrated here)
                )

        /** Returns the unit overlay x position for [unit] adjusted for [font] and [iconPx]. */
        fun unitXFor(unit: String, font: Int = 2, iconPx: Int = 40): Short {
                val base = unitXLookup[unit] ?: defaultUnitX
                val fontOffset = unitXFontOffset[font] ?: 0
                val iconOffset = unitXIconOffset[iconPx] ?: 0
                return (base + fontOffset + iconOffset).toShort()
        }
}
