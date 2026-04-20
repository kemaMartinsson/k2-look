package com.kema.k2look.service

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RadarWarningControllerTest {

    private val renderSmallCalls = mutableListOf<String>()
    private val eraseSmallCalls = mutableListOf<String>()
    private val renderLargeCalls = mutableListOf<String>()
    private val eraseLargeCalls = mutableListOf<String>()

    private lateinit var controller: RadarWarningController

    @Before
    fun setUp() {
        controller =
                RadarWarningController(
                        renderSmall = { renderSmallCalls += "render_small" },
                        eraseSmall = { eraseSmallCalls += "erase_small" },
                        renderLarge = { renderLargeCalls += "render_large" },
                        eraseLarge = { eraseLargeCalls += "erase_large" }
                )
    }

    // ── Detection: HIDDEN → VISIBLE_SMALL ────────────────────────────────────

    @Test
    fun `threat gt 0 with target renders small icon`() {
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 200f)
        assertEquals(listOf("render_small"), renderSmallCalls)
        assertEquals(emptyList<String>(), renderLargeCalls)
    }

    @Test
    fun `threat 0 from hidden stays hidden`() {
        controller.onRadarUpdate(threatLevel = 0, closestRangeM = null)
        assertEquals(emptyList<String>(), renderSmallCalls)
        assertEquals(emptyList<String>(), renderLargeCalls)
        assertEquals(emptyList<String>(), eraseSmallCalls)
        assertEquals(emptyList<String>(), eraseLargeCalls)
    }

    // ── Escalation: VISIBLE_SMALL → VISIBLE_LARGE ────────────────────────────

    @Test
    fun `escalates to large icon when TTA within 5 seconds`() {
        // First packet: car at 200m — enters VISIBLE_SMALL
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 200f)
        renderSmallCalls.clear()

        // Second packet 1s later: car at 160m → velocity = 40m/s → TTA = 160/40 = 4s ≤ 5s
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 160f, elapsedMs = 1000L)
        assertEquals(listOf("erase_small"), eraseSmallCalls)
        assertEquals(listOf("render_large"), renderLargeCalls)
    }

    @Test
    fun `stays small when TTA above 5 seconds`() {
        // 200m, then 180m in 1s → velocity = 20m/s → TTA = 180/20 = 9s > 5s
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 200f)
        renderSmallCalls.clear()

        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 180f, elapsedMs = 1000L)
        assertEquals(emptyList<String>(), eraseSmallCalls)
        assertEquals(emptyList<String>(), renderLargeCalls)
    }

    // ── De-escalation: VISIBLE_LARGE → VISIBLE_SMALL ─────────────────────────

    @Test
    fun `de-escalates to small when TTA rises above 5 seconds`() {
        // Reach VISIBLE_LARGE state
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 200f)
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 160f, elapsedMs = 1000L)
        eraseLargeCalls.clear()
        renderSmallCalls.clear()

        // Car slows: 160m → 158m in 1s → velocity = 2m/s → TTA = 79s > 5s
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 158f, elapsedMs = 1000L)
        assertEquals(listOf("erase_large"), eraseLargeCalls)
        assertEquals(listOf("render_small"), renderSmallCalls)
    }

    // ── Clearance: any VISIBLE → HIDDEN ──────────────────────────────────────

    @Test
    fun `threat 0 from VISIBLE_SMALL erases small and returns to HIDDEN`() {
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 200f)
        renderSmallCalls.clear()

        controller.onRadarUpdate(threatLevel = 0, closestRangeM = null)
        assertEquals(listOf("erase_small"), eraseSmallCalls)
        assertEquals(emptyList<String>(), renderSmallCalls)
    }

    @Test
    fun `threat 0 from VISIBLE_LARGE erases large and returns to HIDDEN`() {
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 200f)
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 160f, elapsedMs = 1000L)
        eraseLargeCalls.clear()

        controller.onRadarUpdate(threatLevel = 0, closestRangeM = null)
        assertEquals(listOf("erase_large"), eraseLargeCalls)
    }

    // ── setEnabled ───────────────────────────────────────────────────────────

    @Test
    fun `setEnabled false from VISIBLE_SMALL erases and suppresses future updates`() {
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 200f)
        eraseSmallCalls.clear()
        renderSmallCalls.clear()

        controller.setEnabled(false)
        assertEquals(listOf("erase_small"), eraseSmallCalls)

        // Next update should be suppressed
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 180f, elapsedMs = 1000L)
        assertEquals(emptyList<String>(), renderSmallCalls)
    }

    @Test
    fun `setEnabled true allows subsequent updates`() {
        controller.setEnabled(false)
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 200f)
        assertEquals(emptyList<String>(), renderSmallCalls)

        controller.setEnabled(true)
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 200f)
        assertEquals(listOf("render_small"), renderSmallCalls)
    }

    // ── reset ─────────────────────────────────────────────────────────────────

    @Test
    fun `reset from VISIBLE_LARGE transitions to HIDDEN without erasing`() {
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 200f)
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 160f, elapsedMs = 1000L)
        eraseLargeCalls.clear()

        controller.reset()
        // reset does NOT erase (glasses may be disconnected)
        assertEquals(emptyList<String>(), eraseLargeCalls)

        // After reset, a fresh detection starts from HIDDEN
        renderSmallCalls.clear()
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 200f)
        assertEquals(listOf("render_small"), renderSmallCalls)
    }

    // ── No-op transitions ────────────────────────────────────────────────────

    @Test
    fun `repeated updates in same state produce no render calls`() {
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 200f) // → VISIBLE_SMALL
        renderSmallCalls.clear()

        // Same state again (TTA still > 5s)
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 195f, elapsedMs = 1000L)
        assertEquals(emptyList<String>(), renderSmallCalls)
        assertEquals(emptyList<String>(), renderLargeCalls)
    }

    @Test
    fun `velocity sample is cleared when threat disappears so new threat starts fresh`() {
        // Establish a fast approach velocity: 200m → 160m in 1s = 40 m/s
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 200f)
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 160f, elapsedMs = 1000L)
        eraseLargeCalls.clear()
        renderLargeCalls.clear()
        renderSmallCalls.clear()

        // Threat clears
        controller.onRadarUpdate(threatLevel = 0, closestRangeM = null)
        eraseLargeCalls.clear()

        // New threat appears at 150m — with stale velocity, TTA ≈ 3.75s → would wrongly escalate
        // With cleared sample, it's the first packet → TTA = MAX_VALUE → VISIBLE_SMALL only
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 150f)
        assertEquals(listOf("render_small"), renderSmallCalls)
        assertEquals(emptyList<String>(), renderLargeCalls)
    }

    // ── End-to-end scenarios ─────────────────────────────────────────────────

    /**
     * Scenario A (full ride-by):
     * 1. Threat detected → small icon shown
     * 2. Threat closes to <5s TTA → small replaced by large icon
     * 3. Threat passes (level=0) → large icon removed
     */
    @Test
    fun `scenario A - threat detected escalates to large then clears on pass`() {
        // Step 1: threat detected → small icon
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 200f)
        assertEquals(listOf("render_small"), renderSmallCalls)
        assertEquals(emptyList<String>(), renderLargeCalls)

        // Step 2: vehicle closes fast → TTA ≤ 5s → escalate to large icon
        // 200m → 160m in 1s = 40 m/s → TTA = 160/40 = 4s ≤ 5s
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 160f, elapsedMs = 1000L)
        assertEquals(listOf("erase_small"), eraseSmallCalls)
        assertEquals(listOf("render_large"), renderLargeCalls)

        // Step 3: threat passes → large icon removed, back to HIDDEN
        controller.onRadarUpdate(threatLevel = 0, closestRangeM = null)
        assertEquals(listOf("erase_large"), eraseLargeCalls)
        // Nothing else rendered after clearing
        assertEquals(1, renderSmallCalls.size) // only from step 1
        assertEquals(1, renderLargeCalls.size) // only from step 2
    }

    /**
     * Scenario B (threat disappears without passing):
     * 1. Threat detected → small icon shown
     * 2. Threat disappears (level=0, no targets) → small icon removed
     */
    @Test
    fun `scenario B - threat detected then disappears without passing removes small icon`() {
        // Step 1: threat detected → small icon
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 200f)
        assertEquals(listOf("render_small"), renderSmallCalls)
        assertEquals(emptyList<String>(), renderLargeCalls)

        // Step 2: threat disappears (vehicle turned off, out of range, etc.) → small icon removed
        controller.onRadarUpdate(threatLevel = 0, closestRangeM = null)
        assertEquals(listOf("erase_small"), eraseSmallCalls)
        // Large icon was never shown
        assertEquals(emptyList<String>(), renderLargeCalls)
        assertEquals(emptyList<String>(), eraseLargeCalls)
    }
}
