package com.kema.k2look.data

import android.content.Context
import com.kema.k2look.model.GestureAction
import com.kema.k2look.model.TouchAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for managing gesture and touch action preferences using SharedPreferences
 */
class GesturePreferencesRepository(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "gesture_preferences"
        private const val KEY_GESTURE_ACTION = "gesture_action"
        private const val KEY_TOUCH_ACTION = "touch_action"
        private const val KEY_GESTURE_ENABLED = "gesture_enabled"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // StateFlows for reactive preferences
    private val _gestureAction = MutableStateFlow(loadGestureAction())
    val gestureAction: StateFlow<GestureAction> = _gestureAction.asStateFlow()

    private val _touchAction = MutableStateFlow(loadTouchAction())
    val touchAction: StateFlow<TouchAction> = _touchAction.asStateFlow()

    private val _gestureEnabled = MutableStateFlow(loadGestureEnabled())
    val gestureEnabled: StateFlow<Boolean> = _gestureEnabled.asStateFlow()

    /**
     * Load gesture action from preferences
     */
    private fun loadGestureAction(): GestureAction {
        val actionName = prefs.getString(KEY_GESTURE_ACTION, GestureAction.CYCLE_SCREENS.name)
            ?: GestureAction.CYCLE_SCREENS.name
        return try {
            GestureAction.valueOf(actionName)
        } catch (e: IllegalArgumentException) {
            GestureAction.CYCLE_SCREENS // Default if invalid
        }
    }

    /**
     * Load touch action from preferences
     */
    private fun loadTouchAction(): TouchAction {
        val actionName = prefs.getString(KEY_TOUCH_ACTION, TouchAction.SHOW_HIDE_DISPLAY.name)
            ?: TouchAction.SHOW_HIDE_DISPLAY.name
        return try {
            TouchAction.valueOf(actionName)
        } catch (e: IllegalArgumentException) {
            TouchAction.SHOW_HIDE_DISPLAY // Default if invalid
        }
    }

    /**
     * Load gesture enabled state
     */
    private fun loadGestureEnabled(): Boolean {
        return prefs.getBoolean(KEY_GESTURE_ENABLED, true)
    }

    /**
     * Set the gesture action preference
     */
    fun setGestureAction(action: GestureAction) {
        prefs.edit().putString(KEY_GESTURE_ACTION, action.name).apply()
        _gestureAction.value = action
    }

    /**
     * Set the touch action preference
     */
    fun setTouchAction(action: TouchAction) {
        prefs.edit().putString(KEY_TOUCH_ACTION, action.name).apply()
        _touchAction.value = action
    }

    /**
     * Set whether gesture sensor is enabled
     */
    fun setGestureEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GESTURE_ENABLED, enabled).apply()
        _gestureEnabled.value = enabled
    }
}

