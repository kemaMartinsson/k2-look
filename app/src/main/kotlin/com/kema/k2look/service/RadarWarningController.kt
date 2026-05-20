package com.kema.k2look.service

import android.util.Log

/**
 * State machine that drives the radar warning overlay on the ActiveLook display.
 *
 * Receives radar data updates from [KarooActiveLookBridge] and transitions between:
 * - [State.HIDDEN] — no vehicle detected
 * - [State.VISIBLE_SMALL] — threat level 2; radar_white_24 shown
 * - [State.VISIBLE_LARGE] — threat level 3; radar_white_40 shown
 * - [State.VISIBLE_CRITICAL] — threat level 4; alert_white_40 shown
 *
 * Render/erase is delegated to injected lambdas — no direct BLE or asset access here. All
 * transitions are synchronous within the calling coroutine; no background job is needed. * ⚠️ NOT
 * thread-safe. All calls must be made from the same coroutine/thread context.
 */
class RadarWarningController(
    private val renderSmall: () -> Unit,
    private val eraseSmall: () -> Unit,
    private val renderLarge: () -> Unit,
    private val eraseLarge: () -> Unit,
    private val renderCritical: () -> Unit,
    private val eraseCritical: () -> Unit
) {
    private enum class State {
        HIDDEN,
        VISIBLE_SMALL,
        VISIBLE_LARGE,
        VISIBLE_CRITICAL
    }

    private var state = State.HIDDEN
    private var enabled = true

    /**
     * Called on every radar packet while streaming.
     *
     * @param threatLevel Value of RADAR_THREAT_LEVEL.
    * - 1: hidden
    * - 2: small warning icon
    * - 3: large warning icon
    * - 4: critical warning icon
     * @param closestRangeM Unused by threat-level mapping; kept for API compatibility.
     * @param elapsedMs Unused by threat-level mapping; kept for API compatibility.
     */
    fun onRadarUpdate(threatLevel: Int, closestRangeM: Float?, elapsedMs: Long = 0L) {
        if (!enabled) return

        val targetState =
                when {
                    threatLevel >= 4 -> State.VISIBLE_CRITICAL
                    threatLevel == 3 -> State.VISIBLE_LARGE
                    threatLevel == 2 -> State.VISIBLE_SMALL
                    else -> State.HIDDEN
                }

        transition(targetState)
    }

    /** Live-toggle the feature. Erases current icon immediately if disabling. */
    fun setEnabled(enabled: Boolean) {
        if (this.enabled == enabled) return
        this.enabled = enabled
        if (!enabled) {
            eraseCurrentIcon()
            state = State.HIDDEN
        }
        Log.i(TAG, "Radar warning ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Called on glasses disconnect. Resets state without attempting to erase (glasses are gone).
     * Safe to call when already HIDDEN.
     */
    fun reset() {
        state = State.HIDDEN
        Log.d(TAG, "RadarWarningController reset")
    }

    // ── Private ──────────────────────────────────────────────────────────────

    private fun transition(target: State) {
        if (target == state) return
        Log.d(TAG, "Radar warning: $state → $target")
        when (state to target) {
            State.HIDDEN to State.VISIBLE_SMALL -> renderSmall()
            State.HIDDEN to State.VISIBLE_LARGE -> renderLarge()
            State.HIDDEN to State.VISIBLE_CRITICAL -> renderCritical()
            State.VISIBLE_SMALL to State.VISIBLE_LARGE -> {
                eraseSmall()
                renderLarge()
            }
            State.VISIBLE_SMALL to State.VISIBLE_CRITICAL -> {
                eraseSmall()
                renderCritical()
            }
            State.VISIBLE_LARGE to State.VISIBLE_SMALL -> {
                eraseLarge()
                renderSmall()
            }
            State.VISIBLE_LARGE to State.VISIBLE_CRITICAL -> {
                eraseLarge()
                renderCritical()
            }
            State.VISIBLE_CRITICAL to State.VISIBLE_LARGE -> {
                eraseCritical()
                renderLarge()
            }
            State.VISIBLE_CRITICAL to State.VISIBLE_SMALL -> {
                eraseCritical()
                renderSmall()
            }
            State.VISIBLE_SMALL to State.HIDDEN -> eraseSmall()
            State.VISIBLE_LARGE to State.HIDDEN -> eraseLarge()
            State.VISIBLE_CRITICAL to State.HIDDEN -> eraseCritical()
        }
        state = target
    }

    private fun eraseCurrentIcon() {
        when (state) {
            State.VISIBLE_SMALL -> eraseSmall()
            State.VISIBLE_LARGE -> eraseLarge()
            State.VISIBLE_CRITICAL -> eraseCritical()
            State.HIDDEN -> {
                /* nothing to erase */
            }
        }
    }

    private companion object {
        private const val TAG = "RadarWarningController"
    }
}
