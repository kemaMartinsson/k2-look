package com.kema.k2look.model

/**
 * Individual zone within a layout template
 *
 * Represents a specific positioning area on the glasses display where a data field
 * can be placed. Each zone has precise X/Y coordinates and dimensions based on
 * ActiveLook's official layout specifications.
 *
 * @param id Zone identifier (e.g., "1D", "2D_H", "3D_HALF_L1")
 * @param displayName Human-readable zone name (e.g., "Top", "Bottom Left")
 * @param x X position in pixels (0-304)
 * @param y Y position in pixels (0-256)
 * @param width Zone width in pixels
 * @param height Zone height in pixels
 * @param font ActiveLook font ID (1-5)
 * @param fontSize FontSize enum for UI display
 * @param isChrono Whether this zone needs special time/chrono handling
 * @param chronoHourX X position for hour component (if chrono)
 * @param chronoHourY Y position for hour component (if chrono)
 */
data class LayoutZone(
    val id: String,
    val displayName: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val font: Int,
    val fontSize: FontSize,
    val isChrono: Boolean = false,
    val chronoHourX: Int? = null,
    val chronoHourY: Int? = null
) {
    init {
        require(x in 0..304) { "X position must be between 0 and 304" }
        require(y in 0..256) { "Y position must be between 0 and 256" }
        require(width > 0) { "Width must be positive" }
        require(height > 0) { "Height must be positive" }
        require(font in 1..5) { "Font ID must be between 1 and 5" }
        if (isChrono) {
            require(chronoHourX != null && chronoHourY != null) {
                "Chrono zones must have chronoHourX and chronoHourY"
            }
        }
    }
}

