package com.kema.k2look.service

import com.kema.k2look.service.AppLog as Log
import com.activelook.activelooksdk.types.LayoutParameters
import com.activelook.activelooksdk.types.Rotation
import com.activelook.activelooksdk.types.holdFlushAction

private const val TAG_DBG = "DisplayDebug"

// ════════════════════════════════════════════════════════════════════════════
//  Tests 1–5: Geometry, coordinate system, clipping, text-X calibration
// ════════════════════════════════════════════════════════════════════════════

/**
 * Draws the display boundary, safe-area rectangle, center crosshair, and coordinate labels at the
 * four corners and center.
 *
 * Answers: Where is (0,0)? Which direction does Y increase?
 */
fun DisplayDebugService.testDisplayBounds() {
    val g = activeLookService.getConnectedGlasses() ?: return logNoGlasses()
    Log.i(TAG_DBG, "▶ Test 1: Display bounds & coordinate grid")

    try {
        g.holdFlush(holdFlushAction.HOLD)
        g.clear()

        // — Outer display border (full pixel extent) —
        g.color(DisplayDebugService.DIM)
        g.rect(
                0,
                0,
                (DisplayDebugService.DISPLAY_W - 1).toShort(),
                (DisplayDebugService.DISPLAY_H - 1).toShort()
        )

        // — Safe-area rectangle —
        g.color(DisplayDebugService.MID_GREY)
        g.rect(
                DisplayDebugService.SAFE_LEFT.toShort(),
                DisplayDebugService.SAFE_TOP.toShort(),
                DisplayDebugService.SAFE_RIGHT.toShort(),
                DisplayDebugService.SAFE_BOTTOM.toShort()
        )

        // — Center crosshair —
        val cx = (DisplayDebugService.DISPLAY_W / 2).toShort() // 152
        val cy = (DisplayDebugService.DISPLAY_H / 2).toShort() // 128
        g.color(DisplayDebugService.WHITE)
        g.line((cx - 20).toShort(), cy, (cx + 20).toShort(), cy)
        g.line(cx, (cy - 20).toShort(), cx, (cy + 20).toShort())

        // — Corner markers (5×5 filled squares) —
        drawCornerMarker(g, 0, 0)
        drawCornerMarker(g, DisplayDebugService.DISPLAY_W - 6, 0)
        drawCornerMarker(g, 0, DisplayDebugService.DISPLAY_H - 6)
        drawCornerMarker(g, DisplayDebugService.DISPLAY_W - 6, DisplayDebugService.DISPLAY_H - 6)

        // — Coordinate labels (font 1 = 24px, smallest default font) —
        val f: Byte = 1
        g.txt(8.toShort(), 8.toShort(), Rotation.TOP_LR, f, DisplayDebugService.WHITE, "0,0")
        g.txt(
                (DisplayDebugService.DISPLAY_W - 60).toShort(),
                8.toShort(),
                Rotation.TOP_LR,
                f,
                DisplayDebugService.WHITE,
                "303,0"
        )
        g.txt(
                8.toShort(),
                (DisplayDebugService.DISPLAY_H - 30).toShort(),
                Rotation.TOP_LR,
                f,
                DisplayDebugService.WHITE,
                "0,255"
        )
        g.txt(
                (DisplayDebugService.DISPLAY_W - 80).toShort(),
                (DisplayDebugService.DISPLAY_H - 30).toShort(),
                Rotation.TOP_LR,
                f,
                DisplayDebugService.WHITE,
                "303,255"
        )
        g.txt(
                (cx - 20).toShort(),
                (cy + 8).toShort(),
                Rotation.TOP_LR,
                f,
                DisplayDebugService.WHITE,
                "152,128"
        )

        g.holdFlush(holdFlushAction.FLUSH)
        Log.i(TAG_DBG, "✓ Test 1 complete — check glasses for corner labels & crosshair")
    } catch (e: Exception) {
        Log.e(TAG_DBG, "Test 1 failed: ${e.message}", e)
        safeFlush(g)
    }
}

/**
 * Renders the SAME string ("Abc") at the SAME (x,y) with every rotation value (0-7). A small dot
 * marks the exact (x,y) anchor point.
 *
 * Answers: For each rotation, where is the anchor relative to the text?
 */
fun DisplayDebugService.testTextRotations() {
    val g = activeLookService.getConnectedGlasses() ?: return logNoGlasses()
    Log.i(TAG_DBG, "▶ Test 2: Text anchor / rotation test")

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

        val rotations =
                arrayOf(
                        Rotation.BOTTOM_RL, // 0
                        Rotation.BOTTOM_LR, // 1
                        Rotation.LEFT_BT, // 2
                        Rotation.LEFT_TB, // 3
                        Rotation.TOP_LR, // 4
                        Rotation.TOP_RL, // 5
                        Rotation.RIGHT_TB, // 6
                        Rotation.RIGHT_BT // 7
                )

        val positions =
                listOf(
                        60 to 40,
                        100 to 40,
                        60 to 100,
                        100 to 100,
                        60 to 160,
                        100 to 160,
                        60 to 220,
                        100 to 220
                )

        val f: Byte = 1

        for (i in rotations.indices) {
            val (ax, ay) = positions[i]

            g.color(DisplayDebugService.MID_GREY)
            g.line((ax - 4).toShort(), ay.toShort(), (ax + 4).toShort(), ay.toShort())
            g.line(ax.toShort(), (ay - 4).toShort(), ax.toShort(), (ay + 4).toShort())

            g.color(DisplayDebugService.WHITE)
            g.txt(ax.toShort(), ay.toShort(), rotations[i], f, DisplayDebugService.WHITE, "R$i")
        }

        g.txt(
                150.toShort(),
                10.toShort(),
                Rotation.TOP_LR,
                f,
                DisplayDebugService.MID_GREY,
                "Rot 0-7 anchor test"
        )

        g.holdFlush(holdFlushAction.FLUSH)
        Log.i(TAG_DBG, "✓ Test 2 complete — compare anchor dot vs text position for each rotation")
    } catch (e: Exception) {
        Log.e(TAG_DBG, "Test 2 failed: ${e.message}", e)
        safeFlush(g)
    }
}

/**
 * Saves two layouts with IDENTICAL text but DIFFERENT clipping params to determine whether "width"
 * means size or right-edge X coordinate.
 *
 * Answers: Is "width" a pixel size or a right-edge X coordinate?
 */
fun DisplayDebugService.testClippingSemantics() {
    val g = activeLookService.getConnectedGlasses() ?: return logNoGlasses()
    Log.i(TAG_DBG, "▶ Test 3: Clipping region semantics (width=SIZE vs width=COORD)")

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

        val layoutA: Byte = DisplayDebugService.DBG_LAYOUT_BASE.toByte()
        val layoutB: Byte = (DisplayDebugService.DBG_LAYOUT_BASE + 1).toByte()

        g.cfgWrite("K2LDBG", 1, 0)
        Thread.sleep(100)

        val paramsA =
                LayoutParameters(
                        layoutA,
                        30.toShort(),
                        160.toByte(),
                        244.toShort(),
                        60.toByte(),
                        DisplayDebugService.WHITE,
                        0.toByte(),
                        2.toByte(),
                        true,
                        200.toShort(),
                        38.toByte(),
                        Rotation.TOP_LR,
                        true
                )
        g.layoutSave(paramsA)
        Thread.sleep(100)

        val paramsB =
                LayoutParameters(
                        layoutB,
                        30.toShort(),
                        80.toByte(),
                        273.toShort(),
                        60.toByte(),
                        DisplayDebugService.WHITE,
                        0.toByte(),
                        2.toByte(),
                        true,
                        200.toShort(),
                        38.toByte(),
                        Rotation.TOP_LR,
                        true
                )
        g.layoutSave(paramsB)
        Thread.sleep(100)

        g.cfgSet("K2LDBG")
        Thread.sleep(100)

        g.color(DisplayDebugService.MID_GREY)
        g.txt(
                30.toShort(),
                55.toShort(),
                Rotation.TOP_LR,
                1.toByte(),
                DisplayDebugService.MID_GREY,
                "A:w=244 B:w=273 txt@200"
        )

        g.layoutClearAndDisplay(layoutA, "ABCDEFGH")
        g.layoutClearAndDisplay(layoutB, "ABCDEFGH")

        g.color(DisplayDebugService.DIM)
        g.rect(30.toShort(), 160.toShort(), 273.toShort(), 220.toShort())
        g.rect(30.toShort(), 80.toShort(), 273.toShort(), 140.toShort())

        g.holdFlush(holdFlushAction.FLUSH)
        Log.i(
                TAG_DBG,
                "✓ Test 3 complete — compare which layout shows more text:\n" +
                        "  • If A shows LESS text than B → width is interpreted as SIZE\n" +
                        "  • If both show SAME text → width is interpreted as COORD (right edge)"
        )
    } catch (e: Exception) {
        Log.e(TAG_DBG, "Test 3 failed: ${e.message}", e)
        safeFlush(g)
    }
}

/**
 * Saves 4 layouts in the SAME clipping zone but with different txtX values: 50, 120, 194, 234. All
 * use rotation 4 (TOP_LR), font 2, same text "12.5".
 *
 * Answers: What does txtX mean with rotation 4?
 */
fun DisplayDebugService.testTextXPosition() {
    val g = activeLookService.getConnectedGlasses() ?: return logNoGlasses()
    Log.i(TAG_DBG, "▶ Test 4: Text X position semantics with rotation 4 (TOP_LR)")

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

        g.cfgWrite("K2LDBG", 2, 0)
        Thread.sleep(100)

        data class TestCase(val id: Byte, val y: Int, val txtX: Int, val label: String)

        val tests =
                listOf(
                        TestCase(DisplayDebugService.DBG_LAYOUT_BASE.toByte(), 190, 50, "X=50"),
                        TestCase(
                                (DisplayDebugService.DBG_LAYOUT_BASE + 1).toByte(),
                                145,
                                120,
                                "X=120"
                        ),
                        TestCase(
                                (DisplayDebugService.DBG_LAYOUT_BASE + 2).toByte(),
                                100,
                                194,
                                "X=194"
                        ),
                        TestCase(
                                (DisplayDebugService.DBG_LAYOUT_BASE + 3).toByte(),
                                55,
                                234,
                                "X=234"
                        )
                )

        for (tc in tests) {
            val params =
                    LayoutParameters(
                            tc.id,
                            30.toShort(),
                            tc.y.toByte(),
                            244.toShort(),
                            40.toByte(),
                            DisplayDebugService.WHITE,
                            0.toByte(),
                            2.toByte(),
                            true,
                            tc.txtX.toShort(),
                            38.toByte(),
                            Rotation.TOP_LR,
                            true
                    )
            g.layoutSave(params)
            Thread.sleep(80)
        }

        g.cfgSet("K2LDBG")
        Thread.sleep(100)

        for (tc in tests) {
            g.layoutClearAndDisplay(tc.id, "12.5")
        }

        g.color(DisplayDebugService.DIM)
        for (tc in tests) {
            val absX = (30 + tc.txtX).toShort()
            g.line(absX, tc.y.toShort(), absX, (tc.y + 40).toShort())
        }

        g.color(DisplayDebugService.MID_GREY)
        val f: Byte = 1
        for (tc in tests) {
            g.txt(
                    DisplayDebugService.SAFE_LEFT.toShort(),
                    (tc.y + 2).toShort(),
                    Rotation.TOP_LR,
                    f,
                    DisplayDebugService.MID_GREY,
                    tc.label
            )
        }

        g.holdFlush(holdFlushAction.FLUSH)
        Log.i(
                TAG_DBG,
                "✓ Test 4 complete — observe where '12.5' appears relative to the vertical line:\n" +
                        "  • Text starts AT the line → txtX is LEFT edge\n" +
                        "  • Text ends AT the line → txtX is RIGHT edge\n" +
                        "  • Text centered ON the line → txtX is CENTER"
        )
    } catch (e: Exception) {
        Log.e(TAG_DBG, "Test 4 failed: ${e.message}", e)
        safeFlush(g)
    }
}

/**
 * Side-by-side comparison of K2Look layout vs Official ActiveLook params for the 2D_H zone. Both
 * layouts render "20". After the FIX_DISPLAY fix, both should look identical.
 */
fun DisplayDebugService.testK2LookVsOfficial() {
    val g = activeLookService.getConnectedGlasses() ?: return logNoGlasses()
    Log.i(TAG_DBG, "▶ Test 5: K2Look current vs Official ActiveLook text position")

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

        g.cfgWrite("K2LDBG", 3, 0)
        Thread.sleep(100)

        val idCurrent: Byte = DisplayDebugService.DBG_LAYOUT_BASE.toByte()
        val idOfficial: Byte = (DisplayDebugService.DBG_LAYOUT_BASE + 1).toByte()

        // Layout A: K2Look current (top half) — font=4, txtX=234, txtY=30
        val paramsCurrent =
                LayoutParameters(
                        idCurrent,
                        30.toShort(),
                        160.toByte(),
                        244.toShort(),
                        60.toByte(),
                        DisplayDebugService.WHITE,
                        0.toByte(),
                        4.toByte(),
                        true,
                        234.toShort(),
                        30.toByte(),
                        Rotation.TOP_LR,
                        true
                )
        g.layoutSave(paramsCurrent)
        Thread.sleep(100)

        // Layout B: Official (bottom half) — txtX=172, txtY=75
        val paramsOfficial =
                LayoutParameters(
                        idOfficial,
                        30.toShort(),
                        80.toByte(),
                        244.toShort(),
                        60.toByte(),
                        DisplayDebugService.WHITE,
                        0.toByte(),
                        4.toByte(),
                        true,
                        172.toShort(),
                        75.toByte(),
                        Rotation.TOP_LR,
                        true
                )
        g.layoutSave(paramsOfficial)
        Thread.sleep(100)

        g.cfgSet("K2LDBG")
        Thread.sleep(100)

        g.layoutClearAndDisplay(idCurrent, "20")
        g.layoutClearAndDisplay(idOfficial, "20")

        g.color(DisplayDebugService.MID_GREY)
        val f: Byte = 1
        g.txt(
                DisplayDebugService.SAFE_LEFT.toShort(),
                152.toShort(),
                Rotation.TOP_LR,
                f,
                DisplayDebugService.MID_GREY,
                "K2Look: x=234 y=30"
        )
        g.txt(
                DisplayDebugService.SAFE_LEFT.toShort(),
                72.toShort(),
                Rotation.TOP_LR,
                f,
                DisplayDebugService.MID_GREY,
                "Official: x=172 y=75"
        )

        g.holdFlush(holdFlushAction.FLUSH)
        Log.i(
                TAG_DBG,
                "✓ Test 5 complete — compare the two '20' values:\n" +
                        "  • Both should now render identically (K2Look uses official params post-fix)\n" +
                        "  • If A is still clipped, LayoutBuilder.officialTextPositions[4] has regressed"
        )
    } catch (e: Exception) {
        Log.e(TAG_DBG, "Test 5 failed: ${e.message}", e)
        safeFlush(g)
    }
}

