package com.kema.k2look.model

/**
 * Represents a rectangular progress bar for visualizing metric values
 *
 * Uses ActiveLook rectangle commands for rendering
 *
 * @param id Bar identifier
 * @param x Top-left X coordinate
 * @param y Top-left Y coordinate
 * @param width Bar width in pixels
 * @param height Bar height in pixels
 * @param orientation Horizontal or vertical fill direction
 * @param minValue Minimum metric value
 * @param maxValue Maximum metric value
 * @param showBorder Show outline around bar
 * @param dataField Associated data field to display
 */
data class ProgressBar(
    val id: Int,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val orientation: Orientation,
    val minValue: Float = 0f,
    val maxValue: Float = 100f,
    val showBorder: Boolean = true,
    val dataField: DataField
) {
    init {
        require(width > 0) { "Width must be > 0" }
        require(height > 0) { "Height must be > 0" }
        require(maxValue > minValue) { "Max value must be > min value" }
    }

    /**
     * Calculate fill pixels for given metric value
     */
    fun calculateFillAmount(value: Float): Int {
        val range = maxValue - minValue
        val normalized = ((value - minValue) / range).coerceIn(0f, 1f)
        return when (orientation) {
            Orientation.HORIZONTAL -> (width * normalized).toInt()
            Orientation.VERTICAL -> (height * normalized).toInt()
        }
    }

    /**
     * Calculate percentage (0-100) for given metric value
     */
    fun calculatePercentage(value: Float): Int {
        val range = maxValue - minValue
        val normalized = ((value - minValue) / range).coerceIn(0f, 1f)
        return (normalized * 100).toInt()
    }
}

/**
 * Bar fill direction
 */
enum class Orientation {
    HORIZONTAL, // Left to right
    VERTICAL    // Bottom to top
}

/**
 * Progress bar with multiple colored zones (e.g., HR zones)
 *
 * @param bar Base progress bar configuration
 * @param zones List of zones with colors
 */
data class ZonedProgressBar(
    val bar: ProgressBar,
    val zones: List<Zone>
) {
    data class Zone(
        val name: String,        // e.g., "Z1", "Z2"
        val minValue: Float,     // Zone minimum
        val maxValue: Float,     // Zone maximum
        val color: Int           // Grey level (0-15)
    ) {
        init {
            require(maxValue > minValue) { "Zone max must be > min" }
            require(color in 0..15) { "Color must be 0-15" }
        }
    }

    init {
        require(zones.isNotEmpty()) { "Must have at least one zone" }
        // Validate zones are in order and don't overlap
        zones.zipWithNext().forEach { (zone1, zone2) ->
            require(zone1.maxValue <= zone2.minValue) { "Zones must not overlap" }
        }
    }

    /**
     * Find which zone the current value is in
     */
    fun findZone(value: Float): Zone? {
        return zones.find { value >= it.minValue && value < it.maxValue }
    }

    /**
     * Calculate value position in pixels
     */
    fun valueToPixel(value: Float): Int {
        val range = bar.maxValue - bar.minValue
        val normalized = ((value - bar.minValue) / range).coerceIn(0f, 1f)
        return when (bar.orientation) {
            Orientation.HORIZONTAL -> bar.x + (bar.width * normalized).toInt()
            Orientation.VERTICAL -> bar.y + bar.height - (bar.height * normalized).toInt()
        }
    }
}

