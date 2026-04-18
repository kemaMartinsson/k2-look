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
}
