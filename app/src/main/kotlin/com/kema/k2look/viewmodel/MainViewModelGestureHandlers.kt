package com.kema.k2look.viewmodel

import com.kema.k2look.service.AppLog as Log
import androidx.lifecycle.viewModelScope
import com.kema.k2look.model.GestureAction
import com.kema.k2look.model.TouchAction
import kotlinx.coroutines.launch

/**
 * Gesture and touch event handling extension functions for [MainViewModel].
 *
 * Covers: event observation, action dispatch, screen cycling, brightness adjustment, display power
 * toggle, and preference setters. Separated to keep file sizes manageable.
 */
private const val TAG = "MainViewModel"

// ── Event observation ──────────────────────────────────────────────────────

/** Observe gesture/touch events from ActiveLook service */
internal fun MainViewModel.observeGestureEvents() {
    viewModelScope.launch {
        activeLookService.gestureEvents.collect { count ->
            _uiState.value = _uiState.value.copy(gestureEventCount = count)
            if (count == 0) return@collect // skip initial StateFlow value
            Log.i(TAG, "🖐️ Gesture event #$count received")

            if (_uiState.value.gestureEnabled) {
                executeGestureAction(_uiState.value.gestureAction)
            }
        }
    }

    viewModelScope.launch {
        activeLookService.touchEvents.collect { count ->
            _uiState.value = _uiState.value.copy(touchEventCount = count)
            if (count == 0) return@collect // skip initial StateFlow value
            Log.i(TAG, "👆 Touch event #$count received")

            if (_uiState.value.touchEnabled) {
                executeTouchAction(_uiState.value.touchAction)
            }
        }
    }
}

/** Observe gesture/touch action preferences */
internal fun MainViewModel.observeGesturePreferences() {
    viewModelScope.launch {
        gesturePreferences.gestureAction.collect { action ->
            Log.d(TAG, "Gesture action preference changed: ${action.displayName}")
            _uiState.value = _uiState.value.copy(gestureAction = action)
        }
    }
    viewModelScope.launch {
        gesturePreferences.touchAction.collect { action ->
            Log.d(TAG, "Touch action preference changed: ${action.displayName}")
            _uiState.value = _uiState.value.copy(touchAction = action)
        }
    }
    viewModelScope.launch {
        gesturePreferences.gestureEnabled.collect { enabled ->
            _uiState.value = _uiState.value.copy(gestureEnabled = enabled)
            bridge.getActiveLookService().enableGestureSensor(enabled)
        }
    }
    viewModelScope.launch {
        gesturePreferences.touchEnabled.collect { enabled ->
            _uiState.value = _uiState.value.copy(touchEnabled = enabled)
        }
    }
}

// ── Action dispatch ────────────────────────────────────────────────────────

/** Execute the configured gesture action */
private fun MainViewModel.executeGestureAction(action: GestureAction) {
    Log.i(TAG, "👋 Executing gesture action: ${action.displayName}")
    when (action) {
        GestureAction.CYCLE_SCREENS -> cycleToNextScreen()
        GestureAction.ADJUST_BRIGHTNESS -> adjustBrightness()
        GestureAction.TOGGLE_DISPLAY -> toggleDisplay()
    }
}

/** Execute the configured touch action */
private fun MainViewModel.executeTouchAction(action: TouchAction) {
    Log.i(TAG, "👆 Executing touch action: ${action.displayName}")
    when (action) {
        TouchAction.SHOW_HIDE_DISPLAY -> toggleDisplay()
        TouchAction.CYCLE_SCREENS -> cycleToNextScreen()
        TouchAction.ADJUST_BRIGHTNESS -> adjustBrightness()
    }
}

// ── Screen / display actions ───────────────────────────────────────────────

/** Cycle to the next screen in the current profile */
private fun MainViewModel.cycleToNextScreen() {
    val layoutViewModel = layoutBuilderViewModel
    if (layoutViewModel == null) {
        Log.w(TAG, "Cannot cycle screens - LayoutBuilderViewModel not set")
        return
    }
    val success = layoutViewModel.cycleToNextScreen()
    if (!success) {
        Log.d(TAG, "Screen cycling not performed (single screen or no profile)")
    }
}

/**
 * Adjust brightness (cycle through levels: 8 -> 12 -> 15 -> 4 -> 8) Using common brightness levels
 * for cycling.
 */
private fun MainViewModel.adjustBrightness() {
    viewModelScope.launch {
        try {
            // Cycle through useful brightness levels
            currentBrightness =
                    when (currentBrightness) {
                        in 0..7 -> 8 // Low -> Medium
                        8 -> 12 // Medium -> High
                        in 9..12 -> 15 // High -> Max
                        in 13..15 -> 4 // Max -> Low
                        else -> 8 // Default to medium
                    }
            Log.i(TAG, "✓ Adjusting brightness to level $currentBrightness (0=min, 15=max)")
            bridge.getActiveLookService().setLuminance(currentBrightness)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to adjust brightness: ${e.message}", e)
        }
    }
}

/** Toggle display on/off */
private fun MainViewModel.toggleDisplay() {
    viewModelScope.launch {
        try {
            displayPowerOn = !displayPowerOn
            Log.i(TAG, "✓ Toggling display ${if (displayPowerOn) "ON" else "OFF"}")

            // Tell the bridge to suppress/resume data flushes so display writes
            // don't immediately re-light the screen after the user turned it off.
            bridge.setDisplayOn(displayPowerOn)
            bridge.getActiveLookService().setDisplayPower(displayPowerOn)

            if (displayPowerOn) {
                // Redraw current layout by re-applying the active profile
                layoutBuilderViewModel?.applyProfileToGlasses()
                Log.d(TAG, "Display turned on - layout refreshed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle display: ${e.message}", e)
        }
    }
}

// ── Preference setters ─────────────────────────────────────────────────────

/** Set gesture action preference */
fun MainViewModel.setGestureAction(action: GestureAction) {
    Log.i(TAG, "Setting gesture action to: ${action.displayName}")
    gesturePreferences.setGestureAction(action)
}

/** Set touch action preference */
fun MainViewModel.setTouchAction(action: TouchAction) {
    Log.i(TAG, "Setting touch action to: ${action.displayName}")
    gesturePreferences.setTouchAction(action)
}

/** Enable or disable gesture sensor */
fun MainViewModel.setGestureEnabled(enabled: Boolean) {
    Log.i(TAG, "Setting gesture enabled: $enabled")
    gesturePreferences.setGestureEnabled(enabled)
}

/** Enable or disable touch button actions */
fun MainViewModel.setTouchEnabled(enabled: Boolean) {
    Log.i(TAG, "Setting touch enabled: $enabled")
    gesturePreferences.setTouchEnabled(enabled)
}

