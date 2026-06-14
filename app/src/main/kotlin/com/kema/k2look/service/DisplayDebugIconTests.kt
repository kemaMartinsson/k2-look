package com.kema.k2look.service

import com.kema.k2look.service.AppLog as Log
import com.activelook.activelooksdk.types.LayoutExtraCmd
import com.activelook.activelooksdk.types.LayoutParameters
import com.activelook.activelooksdk.types.Rotation
import com.activelook.activelooksdk.types.holdFlushAction

private const val TAG_DBG = "DisplayDebug"

// ════════════════════════════════════════════════════════════════════════════
//  Tests 8–9: Icon grid and realistic [icon][value][unit] layout
// ════════════════════════════════════════════════════════════════════════════

/**
 * Tests the full [icon][value][unit] pattern across all three font zones. Viewer sees (left →
 * right): [speed icon 28×28] [value] [km/h]
 *
 * Icon layout logic (all relative to zone x0=30, width=244):
 * - Icon 28×28: right-flush → x_rel=216 (fills 216..243 = viewer-leftmost corner)
 * - Value: txtX=208 (8px gap from icon left-edge at 216)
 * - Unit: addSubCommandText(x, y, "unit") at calibrated x
 */
fun DisplayDebugService.testIconValueUnit() {
    val g = activeLookService.getConnectedGlasses() ?: return logNoGlasses()
    Log.i(TAG_DBG, "▶ Test 8: icon grid — viewer reads L→R: [1][2][3] / [4][5][6] / [7][8][9]")

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

        // Use ALooK config — icons are only accessible from this context
        g.cfgSet("ALooK")
        Thread.sleep(100)

        // Font 1 (24px) — top zone y=153..182, h=30
        val topLayout =
                LayoutParameters(
                        DisplayDebugService.DBG_LAYOUT_BASE.toByte(),
                        30.toShort(),
                        153.toByte(),
                        244.toShort(),
                        30.toByte(),
                        DisplayDebugService.WHITE,
                        0.toByte(),
                        1.toByte(),
                        true,
                        208.toShort(),
                        22.toByte(),
                        Rotation.TOP_LR,
                        true
                )
        g.layoutSave(topLayout)
        Thread.sleep(80)

        // Font 2 (38px) — mid zone y=89..123, h=35
        val midLayout =
                LayoutParameters(
                        (DisplayDebugService.DBG_LAYOUT_BASE + 1).toByte(),
                        30.toShort(),
                        89.toByte(),
                        244.toShort(),
                        35.toByte(),
                        DisplayDebugService.WHITE,
                        0.toByte(),
                        2.toByte(),
                        true,
                        208.toShort(),
                        38.toByte(),
                        Rotation.TOP_LR,
                        true
                )
        g.layoutSave(midLayout)
        Thread.sleep(80)

        // Font 3 (64px) — bot zone y=25..74, h=50
        val botLayout =
                LayoutParameters(
                        (DisplayDebugService.DBG_LAYOUT_BASE + 2).toByte(),
                        30.toShort(),
                        25.toByte(),
                        244.toShort(),
                        50.toByte(),
                        DisplayDebugService.WHITE,
                        0.toByte(),
                        3.toByte(),
                        true,
                        208.toShort(),
                        38.toByte(),
                        Rotation.TOP_LR,
                        true
                )
        g.layoutSave(botLayout)
        Thread.sleep(80)

        // Already in ALooK config — icons available

        // 3 icons × 28px across 244px zone: 4 equal gaps of 40px → x = 40, 108, 176
        // Row 1 (font1 zone, h=30) — ids 1,2,3 — y centred: (30-28)/2 = 1
        val topExtra =
                LayoutExtraCmd()
                        .addSubCommandBitmap(0.toByte(), 0.toShort(), 1.toShort())
                        .addSubCommandBitmap(1.toByte(), 45.toShort(), 1.toShort())
                        .addSubCommandBitmap(2.toByte(), 90.toShort(), 1.toShort())
        g.layoutClearAndDisplayExtended(
                DisplayDebugService.DBG_LAYOUT_BASE.toByte(),
                180.toShort(),
                220.toByte(),
                " ",
                topExtra
        )

        // Row 2 (font2 zone, h=35) — ids 4,5,6 — y centred: (35-28)/2 = 3
        val midExtra =
                LayoutExtraCmd()
                        .addSubCommandBitmap(58.toByte(), 0.toShort(), 3.toShort())
                        .addSubCommandBitmap(51.toByte(), 50.toShort(), 3.toShort())
                        .addSubCommandBitmap(44.toByte(), 90.toShort(), 3.toShort())
        g.layoutClearAndDisplayExtended(
                (DisplayDebugService.DBG_LAYOUT_BASE + 1).toByte(),
                180.toShort(),
                120.toByte(),
                " ",
                midExtra
        )

        // Row 3 (font3 zone, h=50) — ids 7,8,9 — y centred: (50-28)/2 = 11
        val botExtra =
                LayoutExtraCmd()
                        .addSubCommandBitmap(26.toByte(), 0.toShort(), 11.toShort())
                        .addSubCommandBitmap(19.toByte(), 45.toShort(), 11.toShort())
                        .addSubCommandBitmap(12.toByte(), 90.toShort(), 11.toShort())
        g.layoutClearAndDisplayExtended(
                (DisplayDebugService.DBG_LAYOUT_BASE + 2).toByte(),
                180.toShort(),
                10.toByte(),
                " ",
                botExtra
        )

        g.holdFlush(holdFlushAction.FLUSH)
        Log.i(TAG_DBG, "✓ Test 8 complete — icon grid: ids 1-3 (top) / 4-6 (mid) / 7-9 (bot)")
    } catch (e: Exception) {
        Log.e(TAG_DBG, "Test 8 failed: ${e.message}", e)
        safeFlush(g)
    }
}

/**
 * Displays realistic cycling data using the confirmed [icon][value][unit] layout:
 * - Top (y=153, h=30, font1): speed icon (id=26) + "25.1" + "km/h"
 * - Mid (y= 89, h=35, font2): power icon (id=19) + "250" + "W"
 * - Bot (y= 25, h=50, font3): HR icon (id=12) + "150" + "bpm"
 *
 * Confirmed layout math (viewer-LEFT = high rel-x): icon rel-x=216 (28px wide, fills 216..243 =
 * viewer-leftmost) value txtX=208 (8px gap from icon; right-anchor flows toward 0) unit rel-x=5
 * (viewer-RIGHT; font1, safe from any value width)
 */
fun DisplayDebugService.testRealisticLayout() {
    val g = activeLookService.getConnectedGlasses() ?: return logNoGlasses()
    Log.i(TAG_DBG, "▶ Test 9: realistic layout — [icon][value][unit] speed/power/HR")

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

        // Use cfgWrite — cfgSet("ALooK") is read-only, layoutSave there is silently ignored.
        // IDs 53/54/55 — distinct from test 8's 50/51/52 to avoid stale cache.
        g.cfgWrite("K2LDBG", 4, 0)
        Thread.sleep(100)

        val T9_TOP = (DisplayDebugService.DBG_LAYOUT_BASE + 3).toByte() // 53
        val T9_MID = (DisplayDebugService.DBG_LAYOUT_BASE + 4).toByte() // 54
        val T9_BOT = (DisplayDebugService.DBG_LAYOUT_BASE + 5).toByte() // 55

        // Top zone (font1, y=216, h=30) — speed
        val topLayout =
                LayoutParameters(
                        T9_TOP,
                        30.toShort(),
                        216.toByte(),
                        244.toShort(),
                        30.toByte(),
                        DisplayDebugService.WHITE,
                        0.toByte(),
                        1.toByte(),
                        true,
                        208.toShort(),
                        22.toByte(),
                        Rotation.TOP_LR,
                        true
                )
        g.layoutSave(topLayout)
        Thread.sleep(80)

        // Mid zone (font2, y=89, h=35) — power
        val midLayout =
                LayoutParameters(
                        T9_MID,
                        30.toShort(),
                        89.toByte(),
                        244.toShort(),
                        35.toByte(),
                        DisplayDebugService.WHITE,
                        0.toByte(),
                        2.toByte(),
                        true,
                        208.toShort(),
                        38.toByte(),
                        Rotation.TOP_LR,
                        true
                )
        g.layoutSave(midLayout)
        Thread.sleep(80)

        // Bot zone (font3, y=0, h=50) — heartrate
        val botLayout =
                LayoutParameters(
                        T9_BOT,
                        30.toShort(),
                        0.toByte(),
                        244.toShort(),
                        50.toByte(),
                        DisplayDebugService.WHITE,
                        0.toByte(),
                        3.toByte(),
                        true,
                        208.toShort(),
                        55.toByte(),
                        Rotation.TOP_LR,
                        true
                )
        g.layoutSave(botLayout)
        Thread.sleep(80)

        g.cfgSet("K2LDBG")
        Thread.sleep(100)

        // === PASS 1: Layouts + values + units under K2LDBG ===

        // Top (font1, h=30): speed value + "km/h" unit
        val topExtra =
                LayoutExtraCmd()
                        .addSubCommandFont(1.toByte())
                        .addSubCommandText(160.toShort(), 23.toShort(), "km/h")
        g.layoutClearAndDisplayExtended(T9_TOP, 30.toShort(), 216.toByte(), "25.1", topExtra)

        // Mid (font2, h=35): power value + "W" unit
        val midExtra =
                LayoutExtraCmd()
                        .addSubCommandFont(1.toByte())
                        .addSubCommandText(50.toShort(), 38.toShort(), "W")
        g.layoutClearAndDisplayExtended(T9_MID, 30.toShort(), 89.toByte(), "250", midExtra)

        // Bot (font3, h=50): HR value + "bpm" unit
        val botExtra =
                LayoutExtraCmd()
                        .addSubCommandFont(1.toByte())
                        .addSubCommandText(130.toShort(), 50.toShort(), "bpm")
        g.layoutClearAndDisplayExtended(T9_BOT, 30.toShort(), 0.toByte(), "150", botExtra)

        // === PASS 2: Icons via imgDisplay under ALooK ===
        g.cfgSet("ALooK")
        Thread.sleep(50)
        g.imgDisplay(26.toByte(), 260.toShort(), 216.toShort()) // speed: y=216+1
        g.imgDisplay(19.toByte(), 260.toShort(), 93.toShort()) // power: y=89+4
        g.imgDisplay(44.toByte(), 260.toShort(), 15.toShort()) // heart: y=25+11

        g.holdFlush(holdFlushAction.FLUSH)
        Log.i(TAG_DBG, "✓ Test 9 complete — speed(25.1 km/h) / power(250 W) / HR(150 bpm)")
    } catch (e: Exception) {
        Log.e(TAG_DBG, "Test 9 failed: ${e.message}", e)
        safeFlush(g)
    }
}

