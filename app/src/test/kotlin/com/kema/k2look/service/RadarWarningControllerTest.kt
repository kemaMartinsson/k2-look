package com.kema.k2look.service

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RadarWarningControllerTest {

    private val renderSmallCalls = mutableListOf<String>()
    private val eraseSmallCalls = mutableListOf<String>()
    private val renderLargeCalls = mutableListOf<String>()
    private val eraseLargeCalls = mutableListOf<String>()
    private val renderCriticalCalls = mutableListOf<String>()
    private val eraseCriticalCalls = mutableListOf<String>()

    private lateinit var controller: RadarWarningController

    @Before
    fun setUp() {
        controller =
                RadarWarningController(
                    renderSmall = { renderSmallCalls += "render_small" },
                    eraseSmall = { eraseSmallCalls += "erase_small" },
                    renderLarge = { renderLargeCalls += "render_large" },
                    eraseLarge = { eraseLargeCalls += "erase_large" },
                    renderCritical = { renderCriticalCalls += "render_critical" },
                    eraseCritical = { eraseCriticalCalls += "erase_critical" }
                )
    }

    // ── Threat mapping ───────────────────────────────────────────────────────

    @Test
    fun `threat level 2 renders small icon`() {
        controller.onRadarUpdate(threatLevel = 2, closestRangeM = 200f)
        assertEquals(listOf("render_small"), renderSmallCalls)
        assertEquals(emptyList<String>(), renderLargeCalls)
        assertEquals(emptyList<String>(), renderCriticalCalls)
    }

    @Test
    fun `threat levels below 2 stay hidden`() {
        controller.onRadarUpdate(threatLevel = 0, closestRangeM = null)
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 200f)
        assertEquals(emptyList<String>(), renderSmallCalls)
        assertEquals(emptyList<String>(), renderLargeCalls)
        assertEquals(emptyList<String>(), renderCriticalCalls)
        assertEquals(emptyList<String>(), eraseSmallCalls)
        assertEquals(emptyList<String>(), eraseLargeCalls)
        assertEquals(emptyList<String>(), eraseCriticalCalls)
    }

    @Test
    fun `threat level 3 renders large icon`() {
        controller.onRadarUpdate(threatLevel = 3, closestRangeM = 200f)
        assertEquals(emptyList<String>(), renderSmallCalls)
        assertEquals(listOf("render_large"), renderLargeCalls)
        assertEquals(emptyList<String>(), renderCriticalCalls)
    }

    @Test
    fun `threat level 4 renders critical icon`() {
        controller.onRadarUpdate(threatLevel = 4, closestRangeM = 200f)
        assertEquals(emptyList<String>(), renderSmallCalls)
        assertEquals(emptyList<String>(), renderLargeCalls)
        assertEquals(listOf("render_critical"), renderCriticalCalls)
    }

    // ── Escalation and de-escalation ────────────────────────────────────────

    @Test
    fun `escalates from small to large when threat level rises from 2 to 3`() {
        controller.onRadarUpdate(threatLevel = 2, closestRangeM = 200f)
        renderSmallCalls.clear()

        controller.onRadarUpdate(threatLevel = 3, closestRangeM = 160f, elapsedMs = 1000L)
        assertEquals(listOf("erase_small"), eraseSmallCalls)
        assertEquals(listOf("render_large"), renderLargeCalls)
    }

    @Test
    fun `escalates from large to critical when threat level rises from 3 to 4`() {
        controller.onRadarUpdate(threatLevel = 3, closestRangeM = 200f)
        eraseLargeCalls.clear()
        renderCriticalCalls.clear()

        controller.onRadarUpdate(threatLevel = 4, closestRangeM = 180f, elapsedMs = 1000L)
        assertEquals(listOf("erase_large"), eraseLargeCalls)
        assertEquals(listOf("render_critical"), renderCriticalCalls)
    }

    @Test
    fun `de-escalates from critical to large when threat level drops from 4 to 3`() {
        controller.onRadarUpdate(threatLevel = 4, closestRangeM = 200f)
        eraseCriticalCalls.clear()
        renderLargeCalls.clear()

        controller.onRadarUpdate(threatLevel = 3, closestRangeM = 180f, elapsedMs = 1000L)
        assertEquals(listOf("erase_critical"), eraseCriticalCalls)
        assertEquals(listOf("render_large"), renderLargeCalls)
    }

    // ── Clearance: any VISIBLE → HIDDEN ──────────────────────────────────────

    @Test
    fun `threat level 1 from VISIBLE_SMALL erases small and returns to HIDDEN`() {
        controller.onRadarUpdate(threatLevel = 2, closestRangeM = 200f)
        renderSmallCalls.clear()

        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 190f)
        assertEquals(listOf("erase_small"), eraseSmallCalls)
        assertEquals(emptyList<String>(), renderSmallCalls)
    }

    @Test
    fun `threat level 1 from VISIBLE_LARGE erases large and returns to HIDDEN`() {
        controller.onRadarUpdate(threatLevel = 3, closestRangeM = 160f)
        eraseLargeCalls.clear()

        controller.onRadarUpdate(threatLevel = 1, closestRangeM = null)
        assertEquals(listOf("erase_large"), eraseLargeCalls)
    }

    @Test
    fun `threat level 1 from VISIBLE_CRITICAL erases critical and returns to HIDDEN`() {
        controller.onRadarUpdate(threatLevel = 4, closestRangeM = 160f)
        eraseCriticalCalls.clear()

        controller.onRadarUpdate(threatLevel = 1, closestRangeM = null)
        assertEquals(listOf("erase_critical"), eraseCriticalCalls)
    }

    // ── setEnabled ───────────────────────────────────────────────────────────

    @Test
    fun `setEnabled false from VISIBLE_SMALL erases and suppresses future updates`() {
        controller.onRadarUpdate(threatLevel = 2, closestRangeM = 200f)
        eraseSmallCalls.clear()
        renderSmallCalls.clear()

        controller.setEnabled(false)
        assertEquals(listOf("erase_small"), eraseSmallCalls)

        // Next update should be suppressed
        controller.onRadarUpdate(threatLevel = 2, closestRangeM = 180f, elapsedMs = 1000L)
        assertEquals(emptyList<String>(), renderSmallCalls)
    }

    @Test
    fun `setEnabled true allows subsequent updates`() {
        controller.setEnabled(false)
        controller.onRadarUpdate(threatLevel = 2, closestRangeM = 200f)
        assertEquals(emptyList<String>(), renderSmallCalls)

        controller.setEnabled(true)
        controller.onRadarUpdate(threatLevel = 2, closestRangeM = 200f)
        assertEquals(listOf("render_small"), renderSmallCalls)
    }

    // ── reset ─────────────────────────────────────────────────────────────────

    @Test
    fun `reset from VISIBLE_CRITICAL transitions to HIDDEN without erasing`() {
        controller.onRadarUpdate(threatLevel = 4, closestRangeM = 160f)
        eraseCriticalCalls.clear()

        controller.reset()
        // reset does NOT erase (glasses may be disconnected)
        assertEquals(emptyList<String>(), eraseCriticalCalls)

        // After reset, a fresh detection starts from HIDDEN
        renderSmallCalls.clear()
        controller.onRadarUpdate(threatLevel = 2, closestRangeM = 200f)
        assertEquals(listOf("render_small"), renderSmallCalls)
    }

    // ── No-op transitions ────────────────────────────────────────────────────

    @Test
    fun `repeated updates in same state produce no render calls`() {
        controller.onRadarUpdate(threatLevel = 2, closestRangeM = 200f) // → VISIBLE_SMALL
        renderSmallCalls.clear()

        // Same state again
        controller.onRadarUpdate(threatLevel = 2, closestRangeM = 195f, elapsedMs = 1000L)
        assertEquals(emptyList<String>(), renderSmallCalls)
        assertEquals(emptyList<String>(), renderLargeCalls)
        assertEquals(emptyList<String>(), renderCriticalCalls)
    }

    @Test
    fun `closest range and elapsed inputs do not affect threat-level mapping`() {
        controller.onRadarUpdate(threatLevel = 2, closestRangeM = null)
        assertEquals(listOf("render_small"), renderSmallCalls)

        renderSmallCalls.clear()
        controller.onRadarUpdate(threatLevel = 4, closestRangeM = 999f, elapsedMs = 1L)
        assertEquals(listOf("render_critical"), renderCriticalCalls)
    }

    // ── End-to-end scenarios ─────────────────────────────────────────────────

    /**
     * Scenario A (full ride-by):
     * 1. Threat level 2 -> small icon shown
     * 2. Threat level 3 -> large icon shown
     * 3. Threat level 4 -> critical icon shown
     * 4. Threat drops to level 1 -> icon removed
     */
    @Test
    fun `scenario A - level 2 escalates to 3 then 4 then clears`() {
        // Step 1: level 2 -> small icon
        controller.onRadarUpdate(threatLevel = 2, closestRangeM = 200f)
        assertEquals(listOf("render_small"), renderSmallCalls)
        assertEquals(emptyList<String>(), renderLargeCalls)
        assertEquals(emptyList<String>(), renderCriticalCalls)

        // Step 2: level 3 -> large icon
        controller.onRadarUpdate(threatLevel = 3, closestRangeM = 160f, elapsedMs = 1000L)
        assertEquals(listOf("erase_small"), eraseSmallCalls)
        assertEquals(listOf("render_large"), renderLargeCalls)

        // Step 3: level 4 -> critical icon
        controller.onRadarUpdate(threatLevel = 4, closestRangeM = 120f, elapsedMs = 1000L)
        assertEquals(listOf("erase_large"), eraseLargeCalls)
        assertEquals(listOf("render_critical"), renderCriticalCalls)

        // Step 4: level drops below warning threshold -> hidden
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = null)
        assertEquals(listOf("erase_critical"), eraseCriticalCalls)

        // Nothing else rendered after clearing
        assertEquals(1, renderSmallCalls.size) // only from step 1
        assertEquals(1, renderLargeCalls.size) // only from step 2
        assertEquals(1, renderCriticalCalls.size) // only from step 3
    }

    /**
     * Scenario B (threat de-escalates):
     * 1. Level 4 critical is shown
     * 2. Level drops to 3 -> large shown
     * 3. Level drops to 2 -> small shown
     * 4. Level drops to 1 -> hidden
     */
    @Test
    fun `scenario B - level 4 de-escalates to 3 then 2 then 1`() {
        controller.onRadarUpdate(threatLevel = 4, closestRangeM = 150f)
        assertEquals(listOf("render_critical"), renderCriticalCalls)

        controller.onRadarUpdate(threatLevel = 3, closestRangeM = 170f)
        assertEquals(listOf("erase_critical"), eraseCriticalCalls)
        assertEquals(listOf("render_large"), renderLargeCalls)

        controller.onRadarUpdate(threatLevel = 2, closestRangeM = 190f)
        assertEquals(listOf("erase_large"), eraseLargeCalls)
        assertEquals(listOf("render_small"), renderSmallCalls)

        controller.onRadarUpdate(threatLevel = 1, closestRangeM = null)
        assertEquals(listOf("erase_small"), eraseSmallCalls)
    }
}
