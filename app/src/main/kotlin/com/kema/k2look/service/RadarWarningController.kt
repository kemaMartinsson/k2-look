package com.kema.k2look.service

import android.util.Log

/**
 * State machine that drives the radar warning overlay on the ActiveLook display.
 *
 * Receives radar data updates from [KarooActiveLookBridge] and transitions between:
 * - [State.HIDDEN] — no vehicle detected
 * - [State.VISIBLE_SMALL] — vehicle detected; warning_white_28 shown
 * - [State.VISIBLE_LARGE] — vehicle within [TTA_THRESHOLD_S] seconds; warning_white_40 shown
 *
 * Render/erase is delegated to injected lambdas — no direct BLE or asset access here. All
 * transitions are synchronous within the calling coroutine; no background job is needed.
 */
class RadarWarningController(
        private val renderSmall: () -> Unit,
        private val eraseSmall: () -> Unit,
        private val renderLarge: () -> Unit,
        private val eraseLarge: () -> Unit
) {
    private enum class State {
        HIDDEN,
        VISIBLE_SMALL,
        VISIBLE_LARGE
    }

    private var state = State.HIDDEN
    private var enabled = true

    // Velocity estimation — last sample
    private var prevRangeM: Float = 0f
    private var prevTimeMs: Long = 0L

    /**
     * Called on every radar packet while streaming.
     *
     * @param threatLevel Value of RADAR_THREAT_LEVEL. 0 = no threat.
     * @param closestRangeM Closest target range in metres, or null if no targets present.
     * @param elapsedMs Override for elapsed time since last sample (used in tests). Pass 0 to
     * ```
     *                   use wall clock (default).
     * ```
     */
    fun onRadarUpdate(threatLevel: Int, closestRangeM: Float?, elapsedMs: Long = 0L) {
        if (!enabled) return

        val nowMs = if (elapsedMs > 0L) prevTimeMs + elapsedMs else System.currentTimeMillis()
        val tta = computeTta(closestRangeM, nowMs)

        // Update velocity sample for next call
        if (closestRangeM != null && closestRangeM > 0f) {
            prevRangeM = closestRangeM
            prevTimeMs = nowMs
        }

        val targetState =
                when {
                    threatLevel == 0 || closestRangeM == null -> State.HIDDEN
                    tta <= TTA_THRESHOLD_S -> State.VISIBLE_LARGE
                    else -> State.VISIBLE_SMALL
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
            clearVelocitySample()
        }
        Log.i(TAG, "Radar warning ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Called on glasses disconnect. Resets state without attempting to erase (glasses are gone).
     * Safe to call when already HIDDEN.
     */
    fun reset() {
        state = State.HIDDEN
        clearVelocitySample()
        Log.d(TAG, "RadarWarningController reset")
    }

    // ── Private ──────────────────────────────────────────────────────────────

    private fun transition(target: State) {
        if (target == state) return
        Log.d(TAG, "Radar warning: $state → $target")
        when (state to target) {
            State.HIDDEN to State.VISIBLE_SMALL -> renderSmall()
            State.HIDDEN to State.VISIBLE_LARGE -> renderLarge()
            State.VISIBLE_SMALL to State.VISIBLE_LARGE -> {
                eraseSmall()
                renderLarge()
            }
            State.VISIBLE_LARGE to State.VISIBLE_SMALL -> {
                eraseLarge()
                renderSmall()
            }
            State.VISIBLE_SMALL to State.HIDDEN -> eraseSmall()
            State.VISIBLE_LARGE to State.HIDDEN -> eraseLarge()
        }
        state = target
    }

    private fun eraseCurrentIcon() {
        when (state) {
            State.VISIBLE_SMALL -> eraseSmall()
            State.VISIBLE_LARGE -> eraseLarge()
            State.HIDDEN -> {
                /* nothing to erase */
            }
        }
    }

    private fun computeTta(closestRangeM: Float?, nowMs: Long): Float {
        if (closestRangeM == null || closestRangeM <= 0f) return Float.MAX_VALUE
        if (prevTimeMs == 0L) return Float.MAX_VALUE // no prior sample
        val elapsedS = (nowMs - prevTimeMs) / 1000f
        if (elapsedS <= 0f) return Float.MAX_VALUE
        val velocityMps = (prevRangeM - closestRangeM) / elapsedS
        if (velocityMps <= 0f) return Float.MAX_VALUE // not closing
        return closestRangeM / velocityMps
    }

    private fun clearVelocitySample() {
        prevRangeM = 0f
        prevTimeMs = 0L
    }

    private companion object {
        private const val TAG = "RadarWarningController"
        private const val TTA_THRESHOLD_S = 5.0f
    }
}
