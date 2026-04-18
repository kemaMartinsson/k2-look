package com.kema.k2look.service

import android.util.Log
import com.activelook.activelooksdk.types.LayoutExtraCmd
import com.activelook.activelooksdk.types.LayoutParameters
import com.activelook.activelooksdk.types.Rotation
import com.activelook.activelooksdk.types.holdFlushAction

private const val TAG_DBG = "DisplayDebug"

// ════════════════════════════════════════════════════════════════════════════
//  Tests 6–7: Font comparison and LayoutExtraCmd overlay calibration
// ════════════════════════════════════════════════════════════════════════════

/**
 * Displays the same value "14.1" in three zones, one per font:
 * - Top (y=153): font 1 (24px) — h=30, txtX=62, txtY=22 (official)
 * - Mid (y= 89): font 2 (38px) — h=35, txtX=87, txtY=38 (official)
 * - Bot (y= 25): font 3 (64px) — h=50, txtX=194, txtY=38 (calibrated)
 *
 * Goal: verify txtY centering for fonts 1 and 2.
 */
fun DisplayDebugService.testThreeFieldLayout() {
    val g = activeLookService.getConnectedGlasses() ?: return logNoGlasses()
    Log.i(TAG_DBG, "▶ Test 6: font comparison — font1 top, font2 mid, font3 bot, value='14.1'")

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

        // Font 1 (24px) — top zone y=153, h=30, txtX=62, txtY=22 (official)
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
                        244.toShort(),
                        22.toByte(),
                        Rotation.TOP_LR,
                        true
                )
        g.layoutSave(topLayout)
        Thread.sleep(80)

        // Font 2 (38px) — mid zone y=89, h=35, txtX=87, txtY=38 (official)
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
                        244.toShort(),
                        38.toByte(),
                        Rotation.TOP_LR,
                        true
                )
        g.layoutSave(midLayout)
        Thread.sleep(80)

        // Font 3 (64px) — bot zone y=25, h=50, txtX=194, txtY=38 (calibrated)
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
                        244.toShort(),
                        38.toByte(),
                        Rotation.TOP_LR,
                        true
                )
        g.layoutSave(botLayout)
        Thread.sleep(80)

        g.cfgSet("K2LDBG")
        Thread.sleep(100)

        // Draw zone boundary rectangles
        g.color(DisplayDebugService.DIM)
        g.rect(30.toShort(), 153.toShort(), 273.toShort(), 182.toShort()) // font 1 zone
        g.rect(30.toShort(), 89.toShort(), 273.toShort(), 123.toShort()) // font 2 zone
        g.rect(30.toShort(), 25.toShort(), 273.toShort(), 74.toShort()) // font 3 zone

        g.layoutClearAndDisplay(DisplayDebugService.DBG_LAYOUT_BASE.toByte(), "14.1")
        g.layoutClearAndDisplay((DisplayDebugService.DBG_LAYOUT_BASE + 1).toByte(), "14.1")
        g.layoutClearAndDisplay((DisplayDebugService.DBG_LAYOUT_BASE + 2).toByte(), "14.1")

        g.holdFlush(holdFlushAction.FLUSH)
        Log.i(TAG_DBG, "✓ Test 6 complete — font1(top), font2(mid), font3(bot), all showing '14.1'")
    } catch (e: Exception) {
        Log.e(TAG_DBG, "Test 6 failed: ${e.message}", e)
        safeFlush(g)
    }
}

/**
 * Tests `LayoutExtraCmd` + `layoutClearAndDisplayExtended` — the correct way to draw labels/icons
 * ON TOP of the main value (so they are not overwritten).
 *
 * - Font 1 top row (y=153): value "14.1" + unit "km/h" drawn via ExtraCmd
 * - Font 2 mid row (y= 89): value "14.1" + "km/h" via ExtraCmd
 * - Font 3 bot row (y= 25): value "14.1" + "km/h" via ExtraCmd
 *
 * ExtraCmd coordinates are relative to the clipping region origin. Extra elements at zone-x ≈ 40
 * should appear to the viewer's RIGHT of the value (higher coord-x = viewer-left due to lens
 * mirroring).
 */
fun DisplayDebugService.testExtraCommands() {
    val g = activeLookService.getConnectedGlasses() ?: return logNoGlasses()
    Log.i(TAG_DBG, "▶ Test 7: LayoutExtraCmd — unit(top) / icon(mid) / label(bot)")

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

        // Font 1 (24px) — top zone y=153..182, h=30, txtX=244, txtY=22
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
                        244.toShort(),
                        22.toByte(),
                        Rotation.TOP_LR,
                        true
                )
        g.layoutSave(topLayout)
        Thread.sleep(80)

        // Font 2 (38px) — mid zone y=89..123, h=35, txtX=244, txtY=38
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
                        244.toShort(),
                        38.toByte(),
                        Rotation.TOP_LR,
                        true
                )
        g.layoutSave(midLayout)
        Thread.sleep(80)

        // Font 3 (64px) — bot zone y=25..74, h=50, txtX=244, txtY=38
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
                        244.toShort(),
                        38.toByte(),
                        Rotation.TOP_LR,
                        true
                )
        g.layoutSave(botLayout)
        Thread.sleep(80)

        g.cfgSet("K2LDBG")
        Thread.sleep(100)

        // Draw zone boundary rectangles (dim)
        g.color(DisplayDebugService.DIM)
        g.rect(30.toShort(), 153.toShort(), 273.toShort(), 182.toShort())
        g.rect(30.toShort(), 89.toShort(), 273.toShort(), 123.toShort())
        g.rect(30.toShort(), 25.toShort(), 273.toShort(), 74.toShort())

        // Font 1 top: "14.1" + unit "km/h"
        val topExtra =
                LayoutExtraCmd()
                        .addSubCommandFont(1.toByte())
                        .addSubCommandText(200.toShort(), 22.toShort(), "km/h")
        g.layoutClearAndDisplayExtended(
                DisplayDebugService.DBG_LAYOUT_BASE.toByte(),
                30.toShort(),
                153.toByte(),
                "14.1",
                topExtra
        )

        // Font 2 mid: "14.1" + "km/h"
        val midExtra =
                LayoutExtraCmd()
                        .addSubCommandFont(1.toByte())
                        .addSubCommandText(185.toShort(), 32.toShort(), "km/h")
        g.layoutClearAndDisplayExtended(
                (DisplayDebugService.DBG_LAYOUT_BASE + 1).toByte(),
                30.toShort(),
                89.toByte(),
                "14.1",
                midExtra
        )

        // Font 3 bot: "14.1" + "km/h"
        val botExtra =
                LayoutExtraCmd()
                        .addSubCommandFont(1.toByte())
                        .addSubCommandText(168.toShort(), 35.toShort(), "km/h")
        g.layoutClearAndDisplayExtended(
                (DisplayDebugService.DBG_LAYOUT_BASE + 2).toByte(),
                30.toShort(),
                25.toByte(),
                "14.1",
                botExtra
        )

        g.holdFlush(holdFlushAction.FLUSH)
        Log.i(TAG_DBG, "✓ Test 7 complete — ExtraCmd unit/icon/label rendered on top of values")
    } catch (e: Exception) {
        Log.e(TAG_DBG, "Test 7 failed: ${e.message}", e)
        safeFlush(g)
    }
}
