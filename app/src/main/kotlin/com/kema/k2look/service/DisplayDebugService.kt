package com.kema.k2look.service

import android.util.Log
import com.activelook.activelooksdk.Glasses
import com.activelook.activelooksdk.types.LayoutExtraCmd
import com.activelook.activelooksdk.types.LayoutParameters
import com.activelook.activelooksdk.types.Rotation
import com.activelook.activelooksdk.types.holdFlushAction

/**
 * Renders calibration / diagnostic patterns directly on the ActiveLook glasses using low-level
 * graphics primitives (line, rect, txt) and saved layouts.
 *
 * Each "test" draws a known pattern so the developer can photograph the glasses and compare what
 * they see against the expected coordinates.
 *
 * Usage: triggered from the Debug tab in the app UI.
 */
class DisplayDebugService(private val activeLookService: ActiveLookService) {

        companion object {
                private const val TAG = "DisplayDebug"

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

                // Colors
                const val WHITE: Byte = 15
                const val MID_GREY: Byte = 8
                const val DIM: Byte = 4
        }

        // ════════════════════════════════════════════════════════════════════
        //  Test 1 — Display bounds & coordinate grid
        // ════════════════════════════════════════════════════════════════════

        /**
         * Draws the display boundary, safe-area rectangle, center crosshair, and coordinate labels
         * at the four corners and center.
         *
         * Answers: Where is (0,0)? Which direction does Y increase?
         */
        fun testDisplayBounds() {
                val g = activeLookService.getConnectedGlasses() ?: return logNoGlasses()
                Log.i(TAG, "▶ Test 1: Display bounds & coordinate grid")

                try {
                        g.holdFlush(holdFlushAction.HOLD)
                        g.clear()

                        // — Outer display border (full pixel extent) —
                        g.color(DIM)
                        g.rect(0, 0, (DISPLAY_W - 1).toShort(), (DISPLAY_H - 1).toShort())

                        // — Safe-area rectangle —
                        g.color(MID_GREY)
                        g.rect(
                                SAFE_LEFT.toShort(),
                                SAFE_TOP.toShort(),
                                SAFE_RIGHT.toShort(),
                                SAFE_BOTTOM.toShort()
                        )

                        // — Center crosshair —
                        val cx = (DISPLAY_W / 2).toShort() // 152
                        val cy = (DISPLAY_H / 2).toShort() // 128
                        g.color(WHITE)
                        g.line((cx - 20).toShort(), cy, (cx + 20).toShort(), cy)
                        g.line(cx, (cy - 20).toShort(), cx, (cy + 20).toShort())

                        // — Corner markers (5×5 filled squares) —
                        drawCornerMarker(g, 0, 0)
                        drawCornerMarker(g, DISPLAY_W - 6, 0)
                        drawCornerMarker(g, 0, DISPLAY_H - 6)
                        drawCornerMarker(g, DISPLAY_W - 6, DISPLAY_H - 6)

                        // — Coordinate labels (font 1 = 24px, smallest default font) —
                        val f: Byte = 1
                        g.txt(8.toShort(), 8.toShort(), Rotation.TOP_LR, f, WHITE, "0,0")
                        g.txt(
                                (DISPLAY_W - 60).toShort(),
                                8.toShort(),
                                Rotation.TOP_LR,
                                f,
                                WHITE,
                                "303,0"
                        )
                        g.txt(
                                8.toShort(),
                                (DISPLAY_H - 30).toShort(),
                                Rotation.TOP_LR,
                                f,
                                WHITE,
                                "0,255"
                        )
                        g.txt(
                                (DISPLAY_W - 80).toShort(),
                                (DISPLAY_H - 30).toShort(),
                                Rotation.TOP_LR,
                                f,
                                WHITE,
                                "303,255"
                        )
                        g.txt(
                                (cx - 20).toShort(),
                                (cy + 8).toShort(),
                                Rotation.TOP_LR,
                                f,
                                WHITE,
                                "152,128"
                        )

                        g.holdFlush(holdFlushAction.FLUSH)
                        Log.i(
                                TAG,
                                "✓ Test 1 complete — check glasses for corner labels & crosshair"
                        )
                } catch (e: Exception) {
                        Log.e(TAG, "Test 1 failed: ${e.message}", e)
                        safeFlush(g)
                }
        }

        // ════════════════════════════════════════════════════════════════════
        //  Test 2 — Text anchor / rotation behaviour
        // ════════════════════════════════════════════════════════════════════

        /**
         * Renders the SAME string ("Abc") at the SAME (x,y) with every rotation value (0-7). A
         * small dot marks the exact (x,y) anchor point.
         *
         * Answers: For each rotation, where is the anchor relative to the text?
         */
        fun testTextRotations() {
                val g = activeLookService.getConnectedGlasses() ?: return logNoGlasses()
                Log.i(TAG, "▶ Test 2: Text anchor / rotation test")

                try {
                        g.holdFlush(holdFlushAction.HOLD)
                        g.clear()

                        // — Outer display border (reference frame, same as test 1) —
                        g.color(DIM)
                        g.rect(0, 0, (DISPLAY_W - 1).toShort(), (DISPLAY_H - 1).toShort())

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

                        // Lay out 8 test points in 2 columns × 4 rows
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

                        val f: Byte = 1 // font 1 = 24px

                        for (i in rotations.indices) {
                                val (ax, ay) = positions[i]

                                // Draw bright crosshair at the exact anchor point
                                g.color(MID_GREY)
                                g.line(
                                        (ax - 4).toShort(),
                                        ay.toShort(),
                                        (ax + 4).toShort(),
                                        ay.toShort()
                                )
                                g.line(
                                        ax.toShort(),
                                        (ay - 4).toShort(),
                                        ax.toShort(),
                                        (ay + 4).toShort()
                                )

                                // Draw label showing rotation number
                                g.color(WHITE)
                                g.txt(ax.toShort(), ay.toShort(), rotations[i], f, WHITE, "R$i")
                        }

                        // Legend at top
                        g.txt(
                                150.toShort(),
                                10.toShort(),
                                Rotation.TOP_LR,
                                f,
                                MID_GREY,
                                "Rot 0-7 anchor test"
                        )

                        g.holdFlush(holdFlushAction.FLUSH)
                        Log.i(
                                TAG,
                                "✓ Test 2 complete — compare anchor dot vs text position for each rotation"
                        )
                } catch (e: Exception) {
                        Log.e(TAG, "Test 2 failed: ${e.message}", e)
                        safeFlush(g)
                }
        }

        // ════════════════════════════════════════════════════════════════════
        //  Test 3 — Layout clipping region semantics
        // ════════════════════════════════════════════════════════════════════

        /**
         * Saves two layouts with IDENTICAL text but DIFFERENT clipping params to determine whether
         * "width" means size or right-edge X coordinate.
         *
         * Layout A: x=30, y=160, width=244, height=60 (width = SIZE) → If SIZE: clip spans x 30–273
         * → If COORD: clip spans x 30–244
         *
         * Layout B: x=30, y=80, width=273, height=60 (width = RIGHT EDGE if coord) → If SIZE: clip
         * spans x 30–302 (too wide) → If COORD: clip spans x 30–273
         *
         * We place text at txtX = 200 (relative). Whichever layout shows MORE text tells us width =
         * SIZE. If they look the same, width = COORD.
         *
         * Answers: Is "width" a pixel size or a right-edge X coordinate?
         */
        fun testClippingSemantics() {
                val g = activeLookService.getConnectedGlasses() ?: return logNoGlasses()
                Log.i(TAG, "▶ Test 3: Clipping region semantics (width=SIZE vs width=COORD)")

                try {
                        g.holdFlush(holdFlushAction.HOLD)
                        g.clear()

                        // — Outer display border (reference frame, same as test 1) —
                        g.color(DIM)
                        g.rect(0, 0, (DISPLAY_W - 1).toShort(), (DISPLAY_H - 1).toShort())

                        val layoutA: Byte = DBG_LAYOUT_BASE.toByte() // 50
                        val layoutB: Byte = (DBG_LAYOUT_BASE + 1).toByte() // 51

                        // —— Layout A: width=244 (matches official zone size) ——
                        g.cfgWrite("K2LDBG", 1, 0)
                        Thread.sleep(100)

                        val paramsA =
                                LayoutParameters(
                                        layoutA,
                                        30.toShort(),
                                        160.toByte(), // clip origin
                                        244.toShort(),
                                        60.toByte(), // width=244 (the value K2Look currently uses)
                                        WHITE,
                                        0.toByte(), // fg/bg
                                        2.toByte(), // font 2 = 38px
                                        true,
                                        200.toShort(),
                                        38.toByte(), // text at (200, 38) — well to the right
                                        Rotation.TOP_LR,
                                        true
                                )
                        g.layoutSave(paramsA)
                        Thread.sleep(100)

                        // —— Layout B: width=273 (= x + size - 1 = 30 + 244 - 1) ——
                        val paramsB =
                                LayoutParameters(
                                        layoutB,
                                        30.toShort(),
                                        80.toByte(), // different Y so they don't overlap
                                        273.toShort(),
                                        60.toByte(), // width=273
                                        WHITE,
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

                        // Draw reference text (low-level) above the layouts
                        g.color(MID_GREY)
                        g.txt(
                                30.toShort(),
                                55.toShort(),
                                Rotation.TOP_LR,
                                1.toByte(),
                                MID_GREY,
                                "A:w=244 B:w=273 txt@200"
                        )

                        // Display the SAME text through both layouts
                        g.layoutClearAndDisplay(layoutA, "ABCDEFGH")
                        g.layoutClearAndDisplay(layoutB, "ABCDEFGH")

                        // Draw zone boundary lines for visual reference
                        g.color(DIM)
                        // Layout A expected boundaries
                        g.rect(
                                30.toShort(),
                                160.toShort(),
                                273.toShort(),
                                220.toShort()
                        ) // if width=SIZE
                        g.rect(
                                30.toShort(),
                                80.toShort(),
                                273.toShort(),
                                140.toShort()
                        ) // Layout B same boundary

                        g.holdFlush(holdFlushAction.FLUSH)
                        Log.i(
                                TAG,
                                "✓ Test 3 complete — compare which layout shows more text:\n" +
                                        "  • If A shows LESS text than B → width is interpreted as SIZE\n" +
                                        "  • If both show SAME text → width is interpreted as COORD (right edge)"
                        )
                } catch (e: Exception) {
                        Log.e(TAG, "Test 3 failed: ${e.message}", e)
                        safeFlush(g)
                }
        }

        // ════════════════════════════════════════════════════════════════════
        //  Test 4 — Text X position with rotation 4 (TOP_LR)
        // ════════════════════════════════════════════════════════════════════

        /**
         * Saves 4 layouts in the SAME clipping zone but with different txtX values: 50, 120, 194,
         * 234.
         *
         * All use rotation 4 (TOP_LR), font 2, same text "12.5". By observing where the text
         * appears, we determine whether txtX is the LEFT edge, RIGHT edge, or CENTER of the text.
         *
         * Answers: What does txtX mean with rotation 4?
         */
        fun testTextXPosition() {
                val g = activeLookService.getConnectedGlasses() ?: return logNoGlasses()
                Log.i(TAG, "▶ Test 4: Text X position semantics with rotation 4 (TOP_LR)")

                try {
                        g.holdFlush(holdFlushAction.HOLD)
                        g.clear()

                        // — Outer display border (reference frame, same as test 1) —
                        g.color(DIM)
                        g.rect(0, 0, (DISPLAY_W - 1).toShort(), (DISPLAY_H - 1).toShort())

                        g.cfgWrite("K2LDBG", 2, 0)
                        Thread.sleep(100)

                        data class TestCase(
                                val id: Byte,
                                val y: Int,
                                val txtX: Int,
                                val label: String
                        )

                        val tests =
                                listOf(
                                        TestCase(DBG_LAYOUT_BASE.toByte(), 190, 50, "X=50"),
                                        TestCase((DBG_LAYOUT_BASE + 1).toByte(), 145, 120, "X=120"),
                                        TestCase((DBG_LAYOUT_BASE + 2).toByte(), 100, 194, "X=194"),
                                        TestCase((DBG_LAYOUT_BASE + 3).toByte(), 55, 234, "X=234")
                                )

                        for (tc in tests) {
                                val params =
                                        LayoutParameters(
                                                tc.id,
                                                30.toShort(),
                                                tc.y.toByte(),
                                                244.toShort(),
                                                40.toByte(),
                                                WHITE,
                                                0.toByte(),
                                                2.toByte(), // font 2 = 38px
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

                        // Display same value through each layout
                        for (tc in tests) {
                                g.layoutClearAndDisplay(tc.id, "12.5")
                        }

                        // Draw vertical reference line at each txtX position
                        g.color(DIM)
                        for (tc in tests) {
                                val absX =
                                        (30 + tc.txtX)
                                                .toShort() // absolute X if txtX is relative to clip
                                g.line(absX, tc.y.toShort(), absX, (tc.y + 40).toShort())
                        }

                        // Labels
                        g.color(MID_GREY)
                        val f: Byte = 1
                        for (tc in tests) {
                                g.txt(
                                        SAFE_LEFT.toShort(),
                                        (tc.y + 2).toShort(),
                                        Rotation.TOP_LR,
                                        f,
                                        MID_GREY,
                                        tc.label
                                )
                        }

                        g.holdFlush(holdFlushAction.FLUSH)
                        Log.i(
                                TAG,
                                "✓ Test 4 complete — observe where '12.5' appears relative to the vertical line:\n" +
                                        "  • Text starts AT the line → txtX is LEFT edge\n" +
                                        "  • Text ends AT the line → txtX is RIGHT edge\n" +
                                        "  • Text centered ON the line → txtX is CENTER"
                        )
                } catch (e: Exception) {
                        Log.e(TAG, "Test 4 failed: ${e.message}", e)
                        safeFlush(g)
                }
        }

        // ════════════════════════════════════════════════════════════════════
        //  Test 5 — Reproduce exact K2Look layout (current vs official)
        // ════════════════════════════════════════════════════════════════════

        /**
         * Side-by-side comparison: renders two layouts for a 2D_H-type zone.
         *
         * Layout A (top half): OLD K2Look formula — txtX=234, txtY=30 (pre-fix) Layout B (bottom
         * half): Official ActiveLook params — txtX=172, txtY=75
         *
         * After the FIX_DISPLAY fix, K2Look now uses the official params for all zones. Both
         * layouts should render "20" identically (Layout A should no longer clip).
         *
         * ⚠ If Layout A (top) is still clipped/missing, the LayoutBuilder lookup table has
         * regressed — check officialTextPositions for font 4.
         */
        fun testK2LookVsOfficial() {
                val g = activeLookService.getConnectedGlasses() ?: return logNoGlasses()
                Log.i(TAG, "▶ Test 5: K2Look current vs Official ActiveLook text position")

                try {
                        g.holdFlush(holdFlushAction.HOLD)
                        g.clear()

                        // — Outer display border (reference frame, same as test 1) —
                        g.color(DIM)
                        g.rect(0, 0, (DISPLAY_W - 1).toShort(), (DISPLAY_H - 1).toShort())

                        g.cfgWrite("K2LDBG", 3, 0)
                        Thread.sleep(100)

                        val idCurrent: Byte = DBG_LAYOUT_BASE.toByte()
                        val idOfficial: Byte = (DBG_LAYOUT_BASE + 1).toByte()

                        // ── Layout A: K2Look current (top half) ──
                        // Mirrors what LayoutBuilder currently produces for a 2D_H-like zone
                        // font=4, txtX = zone.width - 10 = 234, txtY = zone.height / 2 = 30
                        val paramsCurrent =
                                LayoutParameters(
                                        idCurrent,
                                        30.toShort(),
                                        160.toByte(), // clip origin
                                        244.toShort(),
                                        60.toByte(), // clip size
                                        WHITE,
                                        0.toByte(),
                                        4.toByte(), // font 4 = 75px
                                        true,
                                        234.toShort(),
                                        30.toByte(), // K2Look: zone.width-10, zone.height/2
                                        Rotation.TOP_LR,
                                        true
                                )
                        g.layoutSave(paramsCurrent)
                        Thread.sleep(100)

                        // ── Layout B: Official ActiveLook (bottom half) ──
                        // From Visual Assets: chrono_min_sec_two_data → txtX=172, txtY=75
                        val paramsOfficial =
                                LayoutParameters(
                                        idOfficial,
                                        30.toShort(),
                                        80.toByte(),
                                        244.toShort(),
                                        60.toByte(),
                                        WHITE,
                                        0.toByte(),
                                        4.toByte(),
                                        true,
                                        172.toShort(),
                                        75.toByte(), // Official: from Visual Assets README
                                        Rotation.TOP_LR,
                                        true
                                )
                        g.layoutSave(paramsOfficial)
                        Thread.sleep(100)

                        g.cfgSet("K2LDBG")
                        Thread.sleep(100)

                        // Display same text in both
                        g.layoutClearAndDisplay(idCurrent, "20")
                        g.layoutClearAndDisplay(idOfficial, "20")

                        // Labels
                        g.color(MID_GREY)
                        val f: Byte = 1
                        g.txt(
                                SAFE_LEFT.toShort(),
                                152.toShort(),
                                Rotation.TOP_LR,
                                f,
                                MID_GREY,
                                "K2Look: x=234 y=30"
                        )
                        g.txt(
                                SAFE_LEFT.toShort(),
                                72.toShort(),
                                Rotation.TOP_LR,
                                f,
                                MID_GREY,
                                "Official: x=172 y=75"
                        )

                        g.holdFlush(holdFlushAction.FLUSH)
                        Log.i(
                                TAG,
                                "✓ Test 5 complete — compare the two '20' values:\n" +
                                        "  • Both should now render identically (K2Look uses official params post-fix)\n" +
                                        "  • Top (A, old formula x=234 y=30): should look the same as B after fix\n" +
                                        "  • Bottom (B, official x=172 y=75): reference — must render correctly\n" +
                                        "  • If A is still clipped, LayoutBuilder.officialTextPositions[4] has regressed"
                        )
                } catch (e: Exception) {
                        Log.e(TAG, "Test 5 failed: ${e.message}", e)
                        safeFlush(g)
                }
        }

        // ════════════════════════════════════════════════════════════════════
        //  Test 6 — One zone per font (font 1 top, font 2 mid, font 3 bot)
        // ════════════════════════════════════════════════════════════════════

        /**
         * Displays the same value "14.1" in three zones, one per font: Top (y=153): font 1 (24px) —
         * h=30, txtX=62, txtY=22 (official) Mid (y= 89): font 2 (38px) — h=35, txtX=87, txtY=38
         * (official) Bot (y= 25): font 3 (64px) — h=50, txtX=194, txtY=38 (calibrated: dead center)
         *
         * Goal: verify txtY centering for fonts 1 and 2. Adjust if needed.
         */
        fun testThreeFieldLayout() {
                val g = activeLookService.getConnectedGlasses() ?: return logNoGlasses()
                Log.i(
                        TAG,
                        "▶ Test 6: font comparison — font1 top, font2 mid, font3 bot, value='14.1'"
                )

                try {
                        g.holdFlush(holdFlushAction.HOLD)
                        g.clear()

                        // — Outer display border (reference frame, same as test 1) —
                        g.color(DIM)
                        g.rect(0, 0, (DISPLAY_W - 1).toShort(), (DISPLAY_H - 1).toShort())

                        g.cfgWrite("K2LDBG", 4, 0)
                        Thread.sleep(100)

                        // Font 1 (24px) — top zone y=153, h=30, txtX=62, txtY=22 (official)
                        val topLayout =
                                LayoutParameters(
                                        DBG_LAYOUT_BASE.toByte(),
                                        30.toShort(),
                                        153.toByte(),
                                        244.toShort(),
                                        30.toByte(),
                                        WHITE,
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
                                        (DBG_LAYOUT_BASE + 1).toByte(),
                                        30.toShort(),
                                        89.toByte(),
                                        244.toShort(),
                                        35.toByte(),
                                        WHITE,
                                        0.toByte(),
                                        2.toByte(),
                                        true,
                                        244.toShort(), // 87.toShort(),
                                        38.toByte(),
                                        Rotation.TOP_LR,
                                        true
                                )
                        g.layoutSave(midLayout)
                        Thread.sleep(80)

                        // Font 3 (64px) — bot zone y=25, h=50, txtX=194, txtY=38 (calibrated)
                        val botLayout =
                                LayoutParameters(
                                        (DBG_LAYOUT_BASE + 2).toByte(),
                                        30.toShort(),
                                        25.toByte(),
                                        244.toShort(),
                                        50.toByte(),
                                        WHITE,
                                        0.toByte(),
                                        3.toByte(),
                                        true,
                                        244.toShort(), // 194.toShort(),
                                        38.toByte(),
                                        Rotation.TOP_LR,
                                        true
                                )
                        g.layoutSave(botLayout)
                        Thread.sleep(80)

                        g.cfgSet("K2LDBG")
                        Thread.sleep(100)

                        // Draw zone boundary rectangles
                        g.color(DIM)
                        g.rect(
                                30.toShort(),
                                153.toShort(),
                                273.toShort(),
                                182.toShort()
                        ) // font 1 zone
                        g.rect(
                                30.toShort(),
                                89.toShort(),
                                273.toShort(),
                                123.toShort()
                        ) // font 2 zone
                        g.rect(
                                30.toShort(),
                                25.toShort(),
                                273.toShort(),
                                74.toShort()
                        ) // font 3 zone

                        g.layoutClearAndDisplay(DBG_LAYOUT_BASE.toByte(), "14.1")
                        g.layoutClearAndDisplay((DBG_LAYOUT_BASE + 1).toByte(), "14.1")
                        g.layoutClearAndDisplay((DBG_LAYOUT_BASE + 2).toByte(), "14.1")

                        g.holdFlush(holdFlushAction.FLUSH)
                        Log.i(
                                TAG,
                                "✓ Test 6 complete — font1(top), font2(mid), font3(bot), all showing '14.1'"
                        )
                } catch (e: Exception) {
                        Log.e(TAG, "Test 6 failed: ${e.message}", e)
                        safeFlush(g)
                }
        }

        // ════════════════════════════════════════════════════════════════════
        //  Test 7 — LayoutExtraCmd: unit / icon / label drawn AFTER main value
        // ════════════════════════════════════════════════════════════════════

        /**
         * Tests `LayoutExtraCmd` + `layoutClearAndDisplayExtended` — the correct way to draw
         * labels/icons ON TOP of the main value (so they are not overwritten).
         *
         * - Font 1 top row (y=153): value "14.1" + unit "km/h" drawn via ExtraCmd
         * - Font 2 mid row (y=89): value "14.1" + speed icon (id=26, 28×28) via ExtraCmd
         * - Font 3 bot row (y=25): value "14.1" + label "Speed" in font 1 via ExtraCmd
         *
         * ExtraCmd coordinates are relative to the clipping region origin (same as sub-commands).
         * Extra elements at zone-x ≈ 40 should appear to the viewer's RIGHT of the value (higher
         * coord-x = viewer-left due to lens mirroring).
         */
        fun testExtraCommands() {
                val g = activeLookService.getConnectedGlasses() ?: return logNoGlasses()
                Log.i(TAG, "▶ Test 7: LayoutExtraCmd — unit(top) / icon(mid) / label(bot)")

                try {
                        g.holdFlush(holdFlushAction.HOLD)
                        g.clear()

                        // — Outer display border (reference frame, same as test 1) —
                        g.color(DIM)
                        g.rect(0, 0, (DISPLAY_W - 1).toShort(), (DISPLAY_H - 1).toShort())

                        g.cfgWrite("K2LDBG", 4, 0)
                        Thread.sleep(100)

                        // Font 1 (24px) — top zone y=153..182, h=30, txtX=244, txtY=22
                        val topLayout =
                                LayoutParameters(
                                        DBG_LAYOUT_BASE.toByte(),
                                        30.toShort(),
                                        153.toByte(),
                                        244.toShort(),
                                        30.toByte(),
                                        WHITE,
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
                                        (DBG_LAYOUT_BASE + 1).toByte(),
                                        30.toShort(),
                                        89.toByte(),
                                        244.toShort(),
                                        35.toByte(),
                                        WHITE,
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
                                        (DBG_LAYOUT_BASE + 2).toByte(),
                                        30.toShort(),
                                        25.toByte(),
                                        244.toShort(),
                                        50.toByte(),
                                        WHITE,
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
                        g.color(DIM)
                        g.rect(
                                30.toShort(),
                                153.toShort(),
                                273.toShort(),
                                182.toShort()
                        ) // font 1 zone
                        g.rect(
                                30.toShort(),
                                89.toShort(),
                                273.toShort(),
                                123.toShort()
                        ) // font 2 zone
                        g.rect(
                                30.toShort(),
                                25.toShort(),
                                273.toShort(),
                                74.toShort()
                        ) // font 3 zone

                        // Font 1 top: "14.1" + unit "km/h" — x=170 anchors to viewer's right (flows
                        // toward
                        // x=100)
                        val topExtra =
                                LayoutExtraCmd()
                                        .addSubCommandFont(1.toByte())
                                        .addSubCommandText(200.toShort(), 22.toShort(), "km/h")
                        g.layoutClearAndDisplayExtended(
                                DBG_LAYOUT_BASE.toByte(),
                                30.toShort(),
                                153.toByte(),
                                "14.1",
                                topExtra
                        )

                        // Font 2 mid: "14.1" + text "speed" — bitmap id=26 likely not pre-loaded on
                        // device
                        val midExtra =
                                LayoutExtraCmd()
                                        .addSubCommandFont(1.toByte())
                                        .addSubCommandText(185.toShort(), 32.toShort(), "km/h")
                        g.layoutClearAndDisplayExtended(
                                (DBG_LAYOUT_BASE + 1).toByte(),
                                30.toShort(),
                                89.toByte(),
                                "14.1",
                                midExtra
                        )

                        // Font 3 bot: "14.1" + label "Speed" — x=170 anchors to viewer's right
                        // (flows toward
                        // x=90)
                        val botExtra =
                                LayoutExtraCmd()
                                        .addSubCommandFont(1.toByte())
                                        .addSubCommandText(168.toShort(), 35.toShort(), "km/h")
                        g.layoutClearAndDisplayExtended(
                                (DBG_LAYOUT_BASE + 2).toByte(),
                                30.toShort(),
                                25.toByte(),
                                "14.1",
                                botExtra
                        )

                        g.holdFlush(holdFlushAction.FLUSH)
                        Log.i(
                                TAG,
                                "✓ Test 7 complete — ExtraCmd unit/icon/label rendered on top of values"
                        )
                } catch (e: Exception) {
                        Log.e(TAG, "Test 7 failed: ${e.message}", e)
                        safeFlush(g)
                }
        }

        // ════════════════════════════════════════════════════════════════════
        //  Test 8 — icon | value | unit: speed icon (id=26) left of value
        // ════════════════════════════════════════════════════════════════════

        /**
         * Tests the full [icon][value][unit] pattern across all three font zones.
         *
         * Viewer sees (left → right): [speed icon 28×28] [value] [km/h]
         *
         * Coordinate logic:
         * - Icon: `addSubCommandBitmap(id=26, x=216, y)` — left-edge at x=216, extends to x=243
         * ```
         *    (rightmost 28px of the 244-wide zone = viewer’s leftmost position)
         * ```
         * - Value: `txtX=208` (right anchor, 8px gap from icon start at 216)
         * ```
         *    Shifted from the calibrated 244 to make room for the icon.
         * ```
         * - Unit: `addSubCommandText(x, y, "km/h")` at same relative offset as test 7
         * ```
         *    (unit x = test7_unit_x − 36 to match the 36px value shift)
         * ```
         * Values: row1="50" (font1), row2="20" (font2), row3="30" (font3). Different numbers test
         * proportional font spacing per row.
         */
        fun testIconValueUnit() {
                val g = activeLookService.getConnectedGlasses() ?: return logNoGlasses()
                Log.i(
                        TAG,
                        "▶ Test 8: icon grid — viewer reads L→R: [1][2][3] / [4][5][6] / [7][8][9]"
                )

                try {
                        g.holdFlush(holdFlushAction.HOLD)
                        g.clear()

                        // — Outer display border (reference frame, same as test 1) —
                        g.color(DIM)
                        g.rect(0, 0, (DISPLAY_W - 1).toShort(), (DISPLAY_H - 1).toShort())

                        // Use ALooK config — icons are only accessible from this context
                        g.cfgSet("ALooK")
                        Thread.sleep(100)

                        // Font 1 (24px) — top zone y=153..182, h=30
                        // Icon 28×28 fills relative x=216..243; value right-anchor at x=208 (8px
                        // gap from icon)
                        val topLayout =
                                LayoutParameters(
                                        DBG_LAYOUT_BASE.toByte(),
                                        30.toShort(),
                                        153.toByte(),
                                        244.toShort(),
                                        30.toByte(),
                                        WHITE,
                                        0.toByte(),
                                        1.toByte(),
                                        true,
                                        208.toShort(), // shifted from 244 to make room for icon
                                        22.toByte(),
                                        Rotation.TOP_LR,
                                        true
                                )
                        g.layoutSave(topLayout)
                        Thread.sleep(80)

                        // Font 2 (38px) — mid zone y=89..123, h=35
                        val midLayout =
                                LayoutParameters(
                                        (DBG_LAYOUT_BASE + 1).toByte(),
                                        30.toShort(),
                                        89.toByte(),
                                        244.toShort(),
                                        35.toByte(),
                                        WHITE,
                                        0.toByte(),
                                        2.toByte(),
                                        true,
                                        208.toShort(), // shifted from 244
                                        38.toByte(),
                                        Rotation.TOP_LR,
                                        true
                                )
                        g.layoutSave(midLayout)
                        Thread.sleep(80)

                        // Font 3 (64px) — bot zone y=25..74, h=50
                        val botLayout =
                                LayoutParameters(
                                        (DBG_LAYOUT_BASE + 2).toByte(),
                                        30.toShort(),
                                        25.toByte(),
                                        244.toShort(),
                                        50.toByte(),
                                        WHITE,
                                        0.toByte(),
                                        3.toByte(),
                                        true,
                                        208.toShort(), // shifted from 244
                                        38.toByte(),
                                        Rotation.TOP_LR,
                                        true
                                )
                        g.layoutSave(botLayout)
                        Thread.sleep(80)

                        // Already in ALooK config — icons available, no cfgSet needed

                        // Draw zone boundary rectangles (dim)
                        g.color(DIM)
                        // g.rect(
                        //        30.toShort(),
                        //        153.toShort(),
                        //        273.toShort(),
                        //        182.toShort()
                        // ) // font 1 zone
                        // g.rect(
                        //        30.toShort(),
                        //        89.toShort(),
                        //        273.toShort(),
                        //        123.toShort()
                        // ) // font 2 zone
                        // g.rect(
                        //        30.toShort(),
                        //        25.toShort(),
                        //        273.toShort(),
                        //        74.toShort()
                        // ) // font 3 zone

                        // 3 icons × 28px across 244px zone: 4 equal gaps of 40px → x = 40, 108, 176
                        // Row 1 (font1 zone, h=30) — ids 1,2,3 — y centred: (30-28)/2 = 1
                        val topExtra =
                                LayoutExtraCmd()
                                        .addSubCommandBitmap(0.toByte(), 0.toShort(), 1.toShort())
                                        .addSubCommandBitmap(1.toByte(), 45.toShort(), 1.toShort())
                                        .addSubCommandBitmap(2.toByte(), 90.toShort(), 1.toShort())
                        g.layoutClearAndDisplayExtended(
                                DBG_LAYOUT_BASE.toByte(),
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
                                (DBG_LAYOUT_BASE + 1).toByte(),
                                180.toShort(),
                                120.toByte(),
                                " ",
                                midExtra
                        )

                        // Row 3 (font3 zone, h=50) — ids 7,8,9 — y centred: (50-28)/2 = 11
                        val botExtra =
                                LayoutExtraCmd()
                                        .addSubCommandBitmap(26.toByte(), 0.toShort(), 11.toShort())
                                        .addSubCommandBitmap(
                                                19.toByte(),
                                                45.toShort(),
                                                11.toShort()
                                        )
                                        .addSubCommandBitmap(
                                                12.toByte(),
                                                90.toShort(),
                                                11.toShort()
                                        )
                        g.layoutClearAndDisplayExtended(
                                (DBG_LAYOUT_BASE + 2).toByte(),
                                180.toShort(),
                                10.toByte(),
                                " ",
                                botExtra
                        )

                        g.holdFlush(holdFlushAction.FLUSH)
                        Log.i(
                                TAG,
                                "✓ Test 8 complete — icon grid: ids 1-3 (top) / 4-6 (mid) / 7-9 (bot)"
                        )
                } catch (e: Exception) {
                        Log.e(TAG, "Test 8 failed: ${e.message}", e)
                        safeFlush(g)
                }
        }

        // ════════════════════════════════════════════════════════════════════
        //  Test 9 — [icon][value][unit]: realistic cycling data, all three zones
        // ════════════════════════════════════════════════════════════════════

        /**
         * Displays realistic cycling data using the confirmed [icon][value][unit] layout:
         * - Top (y=153, h=30, font1): speed icon (id=26) + "25.1" + "km/h"
         * - Mid (y= 89, h=35, font2): power icon (id=19) + "250" + "W"
         * - Bot (y= 25, h=50, font3): HR icon (id=12) + "150" + "bpm"
         *
         * Confirmed layout math (viewer-LEFT = high rel-x): icon rel-x=216 (28px wide, fills
         * 216..243 = viewer-leftmost) value txtX=208 (8px gap from icon; right-anchor flows toward
         * 0) unit rel-x=5 (viewer-RIGHT; font1, safe from any value width)
         *
         * Icon y-center: (h−28)/2 → top=1, mid=3+1=4 (power +1 offset), bot=11 Unit y-center:
         * (h−24)/2 → top=3, mid=5, bot=13
         */
        fun testRealisticLayout() {
                val g = activeLookService.getConnectedGlasses() ?: return logNoGlasses()
                Log.i(TAG, "▶ Test 9: realistic layout — [icon][value][unit] speed/power/HR")

                try {
                        g.holdFlush(holdFlushAction.HOLD)
                        g.clear()

                        g.color(DIM)
                        g.rect(0, 0, (DISPLAY_W - 1).toShort(), (DISPLAY_H - 1).toShort())

                        // FIX: use cfgWrite to create a writable config for layout saves.
                        // cfgSet("ALooK") is the SYSTEM config — layoutSave there is silently
                        // ignored (read-only). Saves must go into a user-owned config (K2LDBG).
                        // IDs 53/54/55 — distinct from test 8's 50/51/52 to avoid stale cache.
                        g.cfgWrite("K2LDBG", 4, 0)
                        Thread.sleep(100)

                        // ZONE GEOMETRY — identical to Test 7 (hardware-verified):
                        //   x0=30, width=244 (official safe-area margins)
                        //   Top y=153 h=30 font1 | Mid y=89 h=35 font2 | Bot y=25 h=50 font3
                        //
                        // LAYOUT MATH (Session 6 analytics, now applied against hw-verified zones):
                        //   Icon 28×28 right-flush to zone width:  x_rel = 244-28 = 216
                        //   Value txtX = 208  (8px gap from icon left edge at 216)
                        //   Unit  x_rel = 5   (viewer-RIGHT, safely left of any value text)
                        //   Icon  y_rel = (h-28)/2  — centred vertically in zone
                        //   Unit  y_rel = txtY       — top-aligns with value baseline (font1 unit)
                        //   Power icon (id=19): +1 to y_rel for 2px internal drawable offset

                        val T9_TOP = (DBG_LAYOUT_BASE + 3).toByte() // 53
                        val T9_MID = (DBG_LAYOUT_BASE + 4).toByte() // 54
                        val T9_BOT = (DBG_LAYOUT_BASE + 5).toByte() // 55

                        // Top zone (font1, y=163, h=30) — speed
                        val topLayout =
                                LayoutParameters(
                                        T9_TOP,
                                        30.toShort(), // x0 (safe area left)
                                        216.toByte(), // y0 (shifted up from 153 for viewer)
                                        244.toShort(), // width (official safe area)
                                        30.toByte(), // height
                                        WHITE,
                                        0.toByte(),
                                        1.toByte(), // font1
                                        true,
                                        208.toShort(), // txtX: right-anchor (high x = viewer LEFT)
                                        22.toByte(), // txtY: official font1 position
                                        Rotation.TOP_LR,
                                        true
                                )
                        g.layoutSave(topLayout)
                        Thread.sleep(80)

                        // Mid zone (font2, y=89, h=35) — power
                        val midLayout =
                                LayoutParameters(
                                        T9_MID,
                                        30.toShort(), // x0
                                        89.toByte(), // y0
                                        244.toShort(), // width
                                        35.toByte(), // height
                                        WHITE,
                                        0.toByte(),
                                        2.toByte(), // font2
                                        true,
                                        208.toShort(), // txtX: right-anchor (high x = viewer LEFT)
                                        38.toByte(), // txtY: official font2 position
                                        Rotation.TOP_LR,
                                        true
                                )
                        g.layoutSave(midLayout)
                        Thread.sleep(80)

                        // Bot zone (font3, y=25, h=50) — heartrate
                        val botLayout =
                                LayoutParameters(
                                        T9_BOT,
                                        30.toShort(), // x0
                                        0.toByte(), // y0
                                        244.toShort(), // width
                                        50.toByte(), // height
                                        WHITE,
                                        0.toByte(),
                                        3.toByte(), // font3
                                        true,
                                        208.toShort(), // txtX: right-anchor (high x = viewer LEFT)
                                        55.toByte(), // txtY: official font3 position
                                        Rotation.TOP_RL,
                                        true
                                )
                        g.layoutSave(botLayout)
                        Thread.sleep(80)

                        // Render under the SAME config where layouts were saved.
                        // cfgSet("ALooK") caused stale layouts — IDs are per-config.
                        g.cfgSet("K2LDBG")
                        Thread.sleep(100)

                        // Draw zone boundaries for reference
                        g.color(DIM)
                        // g.rect(30.toShort(), 216.toShort(), 273.toShort(), 245.toShort())
                        // g.rect(30.toShort(), 89.toShort(), 273.toShort(), 123.toShort())
                        // g.rect(30.toShort(), 25.toShort(), 273.toShort(), 74.toShort())

                        // === PASS 1: Layouts + values + units under K2LDBG ===
                        // (No bitmap sub-commands — icons only render under ALooK)

                        // Top (font1, h=30): speed value + "km/h" unit
                        val topExtra =
                                LayoutExtraCmd()
                                        .addSubCommandFont(1.toByte())
                                        .addSubCommandText(160.toShort(), 23.toShort(), "km/h")
                        g.layoutClearAndDisplayExtended(
                                T9_TOP,
                                30.toShort(),
                                216.toByte(),
                                "25.1",
                                topExtra
                        )

                        // Mid (font2, h=35): power value + "W" unit
                        val midExtra =
                                LayoutExtraCmd()
                                        .addSubCommandFont(1.toByte())
                                        .addSubCommandText(50.toShort(), 38.toShort(), "W")
                        g.layoutClearAndDisplayExtended(
                                T9_MID,
                                30.toShort(),
                                89.toByte(),
                                "250",
                                midExtra
                        )

                        // Bot (font3, h=50): HR value + "bpm" unit
                        val botExtra =
                                LayoutExtraCmd()
                                        .addSubCommandFont(1.toByte())
                                        .addSubCommandText(130.toShort(), 50.toShort(), "bpm")
                        g.layoutClearAndDisplayExtended(
                                T9_BOT,
                                30.toShort(),
                                0.toByte(),
                                "150",
                                botExtra
                        )

                        // === PASS 2: Icons via imgDisplay under ALooK ===
                        // Bitmaps are stored in the ALooK system config.
                        // imgDisplay uses ABSOLUTE display coordinates.
                        // abs-x = x0 + icon_rel_x = 30 + 216 = 246
                        // abs-y = y0 + icon_rel_y
                        g.cfgSet("ALooK")
                        Thread.sleep(50)

                        g.imgDisplay(26.toByte(), 260.toShort(), 216.toShort()) // speed: y=216+1
                        g.imgDisplay(19.toByte(), 260.toShort(), 93.toShort()) // power: y=89+4
                        g.imgDisplay(44.toByte(), 260.toShort(), 15.toShort()) // heart: y=25+11

                        g.holdFlush(holdFlushAction.FLUSH)
                        Log.i(
                                TAG,
                                "✓ Test 9 complete — speed(25.1 km/h) / power(250 W) / HR(150 bpm)"
                        )
                } catch (e: Exception) {
                        Log.e(TAG, "Test 9 failed: ${e.message}", e)
                        safeFlush(g)
                }
        }

        // ════════════════════════════════════════════════════════════════════
        //  Test 10 — Visual styles: GAUGE / BAR / ZONE VIEW
        // ════════════════════════════════════════════════════════════════════

        /**
         * Three visual styles using rect/rectf ExtraCmd primitives:
         * - Top (y=190, h=40): GAUGE — 7 power-zone segments. Z5 active (filled), rest dim.
         * - Mid (y=110, h=40): BAR — horizontal effort % progress bar. "68%" text + border + fill.
         * - Bot (y= 25, h=40): ZONE VIEW — 5 HR-zone segments. Z4 active, rest dim.
         *
         * All zones use width=264 to leave 40px on the right for an icon. Segment colors set via
         * addSubCommandColor (WHITE=active, DIM=inactive).
         *
         * Translated from tools/test10.json.
         */
        fun testVisualStyles() {
                val g = activeLookService.getConnectedGlasses() ?: return logNoGlasses()
                Log.i(TAG, "▶ Test 10: visual styles — GAUGE (top) / BAR (mid) / ZONE VIEW (bot)")

                try {
                        g.holdFlush(holdFlushAction.HOLD)
                        g.clear()

                        g.color(DIM)
                        g.rect(0, 0, (DISPLAY_W - 1).toShort(), (DISPLAY_H - 1).toShort())

                        // ALooK config required for icon bitmaps
                        g.cfgSet("ALooK")
                        Thread.sleep(100)

                        // GAUGE — top zone (y=190, h=40). No main value text.
                        val gaugeLayout =
                                LayoutParameters(
                                        DBG_LAYOUT_BASE.toByte(),
                                        0.toShort(),
                                        190.toByte(),
                                        264.toShort(),
                                        40.toByte(),
                                        WHITE,
                                        0.toByte(),
                                        1.toByte(), // font 1 (required param; no main text
                                        // displayed)
                                        true,
                                        0.toShort(), // txtX — unused
                                        0.toByte(), // txtY — unused
                                        Rotation.TOP_LR,
                                        true
                                )
                        g.layoutSave(gaugeLayout)
                        Thread.sleep(80)

                        // BAR — mid zone (y=110, h=40). Shows effort % as text + filled bar.
                        val barLayout =
                                LayoutParameters(
                                        (DBG_LAYOUT_BASE + 1).toByte(),
                                        0.toShort(),
                                        110.toByte(),
                                        264.toShort(),
                                        40.toByte(),
                                        WHITE,
                                        0.toByte(),
                                        1.toByte(), // font 1
                                        true,
                                        260.toShort(), // txtX — viewer-right side (text "68%")
                                        35.toByte(), // txtY
                                        Rotation.TOP_LR,
                                        true
                                )
                        g.layoutSave(barLayout)
                        Thread.sleep(80)

                        // ZONE VIEW — bot zone (y=25, h=40). No main value text.
                        val zoneLayout =
                                LayoutParameters(
                                        (DBG_LAYOUT_BASE + 2).toByte(),
                                        0.toShort(),
                                        25.toByte(),
                                        264.toShort(),
                                        40.toByte(),
                                        WHITE,
                                        0.toByte(),
                                        1.toByte(), // font 1 (required param; no main text)
                                        true,
                                        0.toShort(), // txtX — unused
                                        0.toByte(), // txtY — unused
                                        Rotation.TOP_LR,
                                        true
                                )
                        g.layoutSave(zoneLayout)
                        Thread.sleep(80)

                        // ── GAUGE: 7 power-zone segments, Z5 active ──
                        // Segments at x=2..230 (30px each, 3px gap). Z5 = x=134..164 = active.
                        // Power small icon (id=19, 28×28) at x=270 (right of zone, in display
                        // space).
                        val gaugeExtra =
                                LayoutExtraCmd()
                                        .addSubCommandColor(DIM)
                                        .addSubCommandRectf(
                                                2.toShort(),
                                                5.toShort(),
                                                32.toShort(),
                                                35.toShort()
                                        ) // Z1
                                        .addSubCommandRectf(
                                                35.toShort(),
                                                5.toShort(),
                                                65.toShort(),
                                                35.toShort()
                                        ) // Z2
                                        .addSubCommandRectf(
                                                68.toShort(),
                                                5.toShort(),
                                                98.toShort(),
                                                35.toShort()
                                        ) // Z3
                                        .addSubCommandRectf(
                                                101.toShort(),
                                                5.toShort(),
                                                131.toShort(),
                                                35.toShort()
                                        ) // Z4
                                        .addSubCommandColor(WHITE)
                                        .addSubCommandRectf(
                                                134.toShort(),
                                                5.toShort(),
                                                164.toShort(),
                                                35.toShort()
                                        ) // Z5 active
                                        .addSubCommandColor(DIM)
                                        .addSubCommandRectf(
                                                167.toShort(),
                                                5.toShort(),
                                                197.toShort(),
                                                35.toShort()
                                        ) // Z6
                                        .addSubCommandRectf(
                                                200.toShort(),
                                                5.toShort(),
                                                230.toShort(),
                                                35.toShort()
                                        ) // Z7
                                        .addSubCommandColor(WHITE)
                                        .addSubCommandBitmap(
                                                19.toByte(),
                                                270.toShort(),
                                                5.toShort()
                                        ) // power icon
                        g.layoutClearAndDisplayExtended(
                                DBG_LAYOUT_BASE.toByte(),
                                0.toShort(),
                                190.toByte(),
                                " ",
                                gaugeExtra
                        )

                        // ── BAR: border + 68% fill from viewer-left, effort icon ──
                        // Bar spans x=5..260 (255px). Fill = 68% = ~173px from right in display
                        // coords → fill x=87..260. Icon id=46 (large effort) at x=264.
                        val barExtra =
                                LayoutExtraCmd()
                                        .addSubCommandColor(WHITE)
                                        .addSubCommandRect(
                                                5.toShort(),
                                                6.toShort(),
                                                260.toShort(),
                                                34.toShort()
                                        ) // border
                                        .addSubCommandRectf(
                                                99.toShort(),
                                                7.toShort(),
                                                260.toShort(),
                                                33.toShort()
                                        ) // fill (68%)
                                        .addSubCommandBitmap(
                                                46.toByte(),
                                                264.toShort(),
                                                2.toShort()
                                        ) // effort icon
                        g.layoutClearAndDisplayExtended(
                                (DBG_LAYOUT_BASE + 1).toByte(),
                                0.toShort(),
                                110.toByte(),
                                "68%",
                                barExtra
                        )

                        // ── ZONE VIEW: 5 HR-zone segments, Z4 active ──
                        // 5 segments × ~50px (2..261), 3px gap. Active = Z4 at x=158..208.
                        // Heartbeat large icon (id=44) at x=264.
                        val zoneExtra =
                                LayoutExtraCmd()
                                        .addSubCommandColor(DIM)
                                        .addSubCommandRectf(
                                                2.toShort(),
                                                5.toShort(),
                                                50.toShort(),
                                                35.toShort()
                                        ) // Z1
                                        .addSubCommandRectf(
                                                53.toShort(),
                                                5.toShort(),
                                                103.toShort(),
                                                35.toShort()
                                        ) // Z2
                                        .addSubCommandRectf(
                                                105.toShort(),
                                                5.toShort(),
                                                155.toShort(),
                                                35.toShort()
                                        ) // Z3
                                        .addSubCommandColor(WHITE)
                                        .addSubCommandRectf(
                                                158.toShort(),
                                                5.toShort(),
                                                208.toShort(),
                                                35.toShort()
                                        ) // Z4 active
                                        .addSubCommandColor(DIM)
                                        .addSubCommandRectf(
                                                211.toShort(),
                                                5.toShort(),
                                                261.toShort(),
                                                35.toShort()
                                        ) // Z5
                                        .addSubCommandColor(WHITE)
                                        .addSubCommandBitmap(
                                                44.toByte(),
                                                264.toShort(),
                                                2.toShort()
                                        ) // heartbeat icon
                        g.layoutClearAndDisplayExtended(
                                (DBG_LAYOUT_BASE + 2).toByte(),
                                0.toShort(),
                                25.toByte(),
                                " ",
                                zoneExtra
                        )

                        g.holdFlush(holdFlushAction.FLUSH)
                        Log.i(
                                TAG,
                                "✓ Test 10 complete — GAUGE (Z5 active/7 zones) / BAR (68%) / ZONE VIEW (Z4 active/5 zones)"
                        )
                } catch (e: Exception) {
                        Log.e(TAG, "Test 10 failed: ${e.message}", e)
                        safeFlush(g)
                }
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
                        try {
                                g.cfgDelete("K2LDBG")
                        } catch (_: Exception) {}
                        Log.i(TAG, "Display cleared + all layouts/pages/gauges/config deleted")
                } catch (e: Exception) {
                        Log.e(TAG, "Clear failed: ${e.message}", e)
                }
        }

        private fun drawCornerMarker(g: Glasses, x: Int, y: Int) {
                g.color(WHITE)
                g.rectf(x.toShort(), y.toShort(), (x + 5).toShort(), (y + 5).toShort())
        }

        private fun safeFlush(g: Glasses) {
                try {
                        g.holdFlush(holdFlushAction.FLUSH)
                } catch (_: Exception) {}
        }

        private fun logNoGlasses() {
                Log.w(TAG, "Cannot run test — glasses not connected")
        }
}
