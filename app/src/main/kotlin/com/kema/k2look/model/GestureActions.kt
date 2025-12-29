package com.kema.k2look.model

/**
 * Actions that can be triggered by hand gestures
 */
enum class GestureAction {
    CYCLE_SCREENS,      // Switch to next screen in current profile
    ADJUST_BRIGHTNESS,  // Cycle through brightness levels
    TOGGLE_DISPLAY;     // Show/hide display

    val displayName: String
        get() = when (this) {
            CYCLE_SCREENS -> "Cycle Screens"
            ADJUST_BRIGHTNESS -> "Adjust Brightness"
            TOGGLE_DISPLAY -> "Toggle Display"
        }

    val description: String
        get() = when (this) {
            CYCLE_SCREENS -> "Switch to next screen in current profile"
            ADJUST_BRIGHTNESS -> "Cycle through brightness levels (0-15)"
            TOGGLE_DISPLAY -> "Show or hide the display"
        }
}

/**
 * Actions that can be triggered by capacitive touch button
 */
enum class TouchAction {
    SHOW_HIDE_DISPLAY,  // Toggle display on/off (default)
    CYCLE_SCREENS,      // Switch to next screen
    ADJUST_BRIGHTNESS;  // Cycle brightness

    val displayName: String
        get() = when (this) {
            SHOW_HIDE_DISPLAY -> "Show/Hide Display"
            CYCLE_SCREENS -> "Cycle Screens"
            ADJUST_BRIGHTNESS -> "Adjust Brightness"
        }

    val description: String
        get() = when (this) {
            SHOW_HIDE_DISPLAY -> "Toggle display visibility"
            CYCLE_SCREENS -> "Switch to next screen in profile"
            ADJUST_BRIGHTNESS -> "Cycle through brightness levels"
        }
}

