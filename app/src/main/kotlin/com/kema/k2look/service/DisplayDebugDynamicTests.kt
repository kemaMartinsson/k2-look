package com.kema.k2look.service

import android.util.Log
import com.activelook.activelooksdk.types.Rotation
import com.activelook.activelooksdk.types.holdFlushAction
import com.kema.k2look.service.DisplayDebugService.DebugMetric

private const val TAG_DBG = "DisplayDebug"

// ════════════════════════════════════════════════════════════════════════════
//  Tests 10–13: Dynamic layout engine, gauge, production layout, zone circles
// ════════════════════════════════════════════════════════════════════════════

/**
 * Demonstrates the dynamic layout system with 4 rows:
 * - Row 1 (viewer-top, large): speed, font1, small icon, unit
 * - Row 2 (medium): elapsed time, font2, small icon, no unit
 * - Row 3 (small): heartrate, font1, small icon, unit
 * - Row 4 (viewer-bottom, large): power, font3, large icon, no unit
 *
 * Uses [createLayouts] for geometry and [populateLayout] for rendering.
 */
fun DisplayDebugService.testDynamicLayout() {
    val g = activeLookService.getConnectedGlasses() ?: return logNoGlasses()
    Log.i(TAG_DBG, "▶ Test 10: dynamic layout — 4 rows [large, medium, small, large]")

    try {
        g.holdFlush(holdFlushAction.HOLD)
        g.clear()

        g.color(DisplayDebugService.DIM)
        g.rect(
                0,
                0,
                (DisplayDebugService.DISPLAY_W - 1).toShort(),
                (DisplayDebugService.DISPLAY_H - 1).toShort()
        )

        g.cfgWrite("K2LDBG", 4, 0)
        Thread.sleep(100)

        val rows = createLayouts(listOf("large", "medium", "small", "large"))
        if (rows.isEmpty()) {
            Log.w(TAG_DBG, "Test 10: no rows fit — aborting")
            g.holdFlush(holdFlushAction.FLUSH)
            return
        }

        populateLayout(g, 1, 1, DebugMetric.SPEED, "small", true)
        populateLayout(g, 2, 2, DebugMetric.ELAPSED_TIME, "small", false)
        populateLayout(g, 3, 1, DebugMetric.HEARTRATE, "small", true)
        populateLayout(g, 4, 3, DebugMetric.POWER, "large", false)

        g.cfgSet("ALooK")
        Thread.sleep(50)
        renderPendingIcons(g)

        g.holdFlush(holdFlushAction.FLUSH)
        Log.i(TAG_DBG, "✓ Test 10 complete — 4 dynamic rows: speed/cadence/HR/power")
    } catch (e: Exception) {
        Log.e(TAG_DBG, "Test 10 failed: ${e.message}", e)
        safeFlush(g)
    }
}

/**
 * Saves a 270° arc gauge at the display centre and steps through fill levels: 0→20→40→60→80→100%
 * with 1.5s between each frame.
 *
 * Gauge geometry (matches DefaultVisualizations.kt power-gauge defaults):
 * - Centre: (152, 128) — display centre
 * - Outer radius: 70px, inner radius: 45px (25px thick arc)
 * - start=7, end=12, clockwise=true
 */
fun DisplayDebugService.testGauge270() {
    val g = activeLookService.getConnectedGlasses() ?: return logNoGlasses()
    Log.i(TAG_DBG, "▶ Test 11: 180° gauge — 4 fill levels (0/33/66/100 %)")

    val gaugeId: Byte = 1
    val cx: Short = 152
    val cy: Short = 128
    val txtCy: Short = (cy + 25).toShort()
    val rOuter: Char = 70.toChar()
    val rInner: Char = 45.toChar()
    val startPortion: Byte = 7
    val endPortion: Byte = 12
    val steps = listOf(0, 20, 40, 60, 80, 100)

    try {
        g.cfgWrite("K2LDBG", 4, 0)
        Thread.sleep(100)
        g.gaugeSave(gaugeId, cx, cy, rOuter, rInner, startPortion, endPortion, true)
        Thread.sleep(100)

        for (pct in steps) {
            g.holdFlush(holdFlushAction.HOLD)
            g.clear()

            g.color(DisplayDebugService.DIM)
            g.rect(
                    0,
                    0,
                    (DisplayDebugService.DISPLAY_W - 1).toShort(),
                    (DisplayDebugService.DISPLAY_H - 1).toShort()
            )

            g.gaugeDisplay(gaugeId, pct.toByte())

            g.color(DisplayDebugService.WHITE)
            g.txt(187, txtCy, Rotation.TOP_LR, 2.toByte(), DisplayDebugService.WHITE, "$pct%")

            g.holdFlush(holdFlushAction.FLUSH)
            Log.i(TAG_DBG, "  Gauge @ $pct%")
            Thread.sleep(1500)
        }

        g.holdFlush(holdFlushAction.HOLD)
        g.gaugeDelete(gaugeId)
        g.clear()
        g.holdFlush(holdFlushAction.FLUSH)

        Log.i(TAG_DBG, "✓ Test 11 complete — 180° gauge animated at 0/33/66/100 %")
    } catch (e: Exception) {
        Log.e(TAG_DBG, "Test 11 failed: ${e.message}", e)
        safeFlush(g)
    }
}

/**
 * Renders a 3-field cycling layout using the same DynamicLayoutEngine path as
 * ActiveLookLayoutService.saveProfileLayouts. Validates the wired pipeline on hardware.
 *
 * Layout (matches production large→font3 / medium→font2 / small→font1 mapping):
 * - Row 1 (viewer-top, large, font 3): speed + icon + unit
 * - Row 2 (viewer-mid, medium, font 2): heart-rate + icon + unit
 * - Row 3 (viewer-bot, small, font 1): cadence + icon + unit
 */
fun DisplayDebugService.testProductionLayout() {
    val g = activeLookService.getConnectedGlasses() ?: return logNoGlasses()
    Log.i(TAG_DBG, "▶ Test 12: 3-field production layout — speed(font3)/HR(font2)/cadence(font1)")

    try {
        g.holdFlush(holdFlushAction.HOLD)
        g.clear()

        g.color(DisplayDebugService.DIM)
        g.rect(
                0,
                0,
                (DisplayDebugService.DISPLAY_W - 1).toShort(),
                (DisplayDebugService.DISPLAY_H - 1).toShort()
        )

        g.cfgWrite("K2LDBG", 4, 0)
        Thread.sleep(100)

        val rows = createLayouts(listOf("large", "medium", "small"))
        if (rows.isEmpty()) {
            Log.w(TAG_DBG, "Test 12: no rows fit — aborting")
            g.holdFlush(holdFlushAction.FLUSH)
            return
        }

        populateLayout(g, 1, 3, DebugMetric.SPEED, "small", true) // large → font 3
        populateLayout(g, 2, 2, DebugMetric.HEARTRATE, "small", true) // medium → font 2
        populateLayout(g, 3, 1, DebugMetric.CADENCE, "small", true) // small → font 1

        g.cfgSet("ALooK")
        Thread.sleep(50)
        renderPendingIcons(g)

        g.holdFlush(holdFlushAction.FLUSH)
        Log.i(TAG_DBG, "✓ Test 12 complete — speed(25.1 km/h) / HR(150 bpm) / cadence(185 rpm)")
    } catch (e: Exception) {
        Log.e(TAG_DBG, "Test 12 failed: ${e.message}", e)
        safeFlush(g)
    }
}

/**
 * Renders 5/7 horizontally spaced circles representing HR / power zones.
 *
 * Fill progression: all zones ≤ active are filled bright; zones above are dim outlines. Viewer
 * mapping (high display-x = viewer-LEFT): Z5/Z7 (max) → viewer-LEFT; Z1 → viewer-RIGHT.
 *
 * Test cycles: no zone → Z1 → ... → Zmax with 1.5s per step.
 */
fun DisplayDebugService.testZoneBar() {
    val g = activeLookService.getConnectedGlasses() ?: return logNoGlasses()
    Log.i(TAG_DBG, "▶ Test 13: Zone circles — row 1: 7 zones (power), row 2: 5 zones (HR)")

    data class ZoneRow(val numZones: Int, val cy: Int, val iconId: Byte)

    val rows =
            listOf(
                    ZoneRow(numZones = 7, cy = 75, iconId = 19), // power icon
                    ZoneRow(numZones = 5, cy = 175, iconId = 12), // heart-beat icon
            )

    val iconSize = 28
    val iconX: Short = (DisplayDebugService.SAFE_RIGHT - iconSize).toShort() // = 246, viewer-LEFT
    val circleAreaWidth = (iconX - 6) - DisplayDebugService.ZONE_X0 // = 210

    val maxZones = rows.maxOf { it.numZones }

    try {
        for (activeZone in 0..maxZones) {
            g.holdFlush(holdFlushAction.HOLD)
            g.clear()

            g.color(DisplayDebugService.DIM)
            g.rect(
                    0,
                    0,
                    (DisplayDebugService.DISPLAY_W - 1).toShort(),
                    (DisplayDebugService.DISPLAY_H - 1).toShort()
            )

            for (row in rows) {
                val slot = circleAreaWidth / row.numZones
                val r = slot / 2 - 1
                val txtHalfW = slot / 2
                val txtYAdjust = if (row.numZones == 7) 3 else 0
                val txtY = (row.cy - 24 + txtYAdjust).toShort()
                val txtXAdjust = if (row.numZones == 7) -2 else -8
                val rowActive = activeZone.coerceAtMost(row.numZones)

                fun zoneCx(z: Int) =
                        DisplayDebugService.ZONE_X0 + slot / 2 + (row.numZones - z) * slot

                for (z in 1..row.numZones) {
                    val cx = zoneCx(z)
                    when {
                        z < rowActive -> {
                            g.color(DisplayDebugService.DIM)
                            filledCircle(g, cx, row.cy, r)
                        }
                        z == rowActive -> {
                            g.color(DisplayDebugService.WHITE)
                            filledCircle(g, cx, row.cy, r)
                            g.txt(
                                    (cx + txtHalfW + txtXAdjust).toShort(),
                                    txtY,
                                    Rotation.TOP_LR,
                                    1.toByte(),
                                    DisplayDebugService.WHITE,
                                    "Z$z"
                            )
                        }
                        else -> {
                            /* not yet — black (no draw) */
                        }
                    }
                }
            }

            // Icons need ALooK config
            g.cfgSet("ALooK")
            for (row in rows) {
                g.imgDisplay(row.iconId, iconX, (row.cy - iconSize / 2).toShort())
            }

            val label = if (activeZone == 0) "no zone" else "Z$activeZone"
            g.holdFlush(holdFlushAction.FLUSH)
            Log.i(TAG_DBG, "  active=$label")
            Thread.sleep(1500)
        }

        Log.i(TAG_DBG, "✓ Test 13 complete")
    } catch (e: Exception) {
        Log.e(TAG_DBG, "Test 13 failed: ${e.message}", e)
        safeFlush(g)
    }
}
