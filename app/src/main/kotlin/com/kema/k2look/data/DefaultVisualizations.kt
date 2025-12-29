package com.kema.k2look.data

import com.kema.k2look.model.DataField
import com.kema.k2look.model.Gauge
import com.kema.k2look.model.Orientation
import com.kema.k2look.model.ProgressBar
import com.kema.k2look.model.VisualizationType
import com.kema.k2look.model.ZonedProgressBar

/**
 * Pre-configured gauges and bars for common use cases
 *
 * These defaults provide ready-to-use visualizations that users can apply
 * without needing to configure parameters manually.
 */
object DefaultVisualizations {

    /**
     * Power Gauge - 3/4 circle showing power as percentage of 400W FTP
     *
     * Visual: Arc from 3 o'clock around to 6 o'clock (270°)
     * Center display area for text value
     */
    fun createPowerGauge(): Gauge {
        return Gauge(
            id = 1,
            centerX = 152,      // Center of 304px display
            centerY = 128,      // Center of 256px display
            radiusOuter = 70,   // Outer arc
            radiusInner = 55,   // Inner arc (thickness = 15px)
            startPortion = 3,   // Start at 3 o'clock
            endPortion = 14,    // End near 12 o'clock (270°)
            clockwise = true,
            minValue = 0f,
            maxValue = 400f,    // Typical FTP range
            dataField = DataFieldRegistry.getById(7)!! // Power
        )
    }

    /**
     * Heart Rate Gauge - 3/4 circle showing HR zones
     *
     * Visual: Arc showing HR relative to max HR
     * Can be combined with text showing actual BPM
     */
    fun createHeartRateGauge(): Gauge {
        return Gauge(
            id = 2,
            centerX = 152,
            centerY = 128,
            radiusOuter = 70,
            radiusInner = 55,
            startPortion = 3,
            endPortion = 14,
            clockwise = true,
            minValue = 40f,      // Resting HR
            maxValue = 200f,     // Max HR
            dataField = DataFieldRegistry.getById(4)!! // Heart Rate
        )
    }

    /**
     * Cadence Gauge - Full circle showing cadence range
     *
     * Visual: Complete circle for full 0-120 rpm range
     */
    fun createCadenceGauge(): Gauge {
        return Gauge(
            id = 3,
            centerX = 152,
            centerY = 128,
            radiusOuter = 60,
            radiusInner = 45,
            startPortion = 0,    // Full circle
            endPortion = 15,
            clockwise = true,
            minValue = 0f,
            maxValue = 120f,     // Typical cadence range
            dataField = DataFieldRegistry.getById(8)!! // Cadence
        )
    }

    /**
     * HR Zone Bar - Horizontal bar with 5 colored zones
     *
     * Visual: Horizontal bar showing all HR zones with different grey levels
     * Current HR position highlighted
     *
     * Default zones based on typical percentages of max HR (190 bpm example):
     * Z1: 40-114 bpm (50-60%)  - Recovery
     * Z2: 114-133 bpm (60-70%) - Endurance
     * Z3: 133-152 bpm (70-80%) - Tempo
     * Z4: 152-171 bpm (80-90%) - Threshold
     * Z5: 171-200 bpm (90-100%) - Max
     */
    fun createHRZoneBar(): ZonedProgressBar {
        val bar = ProgressBar(
            id = 1,
            x = 30,
            y = 110,
            width = 244,         // Full width minus margins
            height = 25,
            orientation = Orientation.HORIZONTAL,
            minValue = 40f,
            maxValue = 200f,
            showBorder = true,
            dataField = DataFieldRegistry.getById(4)!! // Heart Rate
        )

        return ZonedProgressBar(
            bar = bar,
            zones = listOf(
                ZonedProgressBar.Zone("Z1", 40f, 114f, 3),   // Light grey
                ZonedProgressBar.Zone("Z2", 114f, 133f, 5),  // Light-mid grey
                ZonedProgressBar.Zone("Z3", 133f, 152f, 8),  // Mid grey
                ZonedProgressBar.Zone("Z4", 152f, 171f, 11), // Mid-bright grey
                ZonedProgressBar.Zone("Z5", 171f, 200f, 14)  // Bright grey
            )
        )
    }

    /**
     * Power Zone Bar - Horizontal bar with 7 power zones
     *
     * Visual: Horizontal bar showing all power zones based on FTP
     * Current power position highlighted
     *
     * Power zones based on typical FTP percentage ranges:
     * Z1: 0-140W (0-55% FTP)    - Active Recovery
     * Z2: 140-190W (55-75% FTP) - Endurance
     * Z3: 190-220W (75-90% FTP) - Tempo
     * Z4: 220-260W (90-105% FTP)- Lactate Threshold
     * Z5: 260-300W (105-120% FTP)- VO2 Max
     * Z6: 300-360W (120-150% FTP)- Anaerobic Capacity
     * Z7: 360-400W (150%+ FTP)  - Neuromuscular Power
     *
     * Note: These zones assume FTP ≈ 250W. Karoo automatically adjusts based on user's actual FTP.
     */
    fun createPowerZoneBar(): ZonedProgressBar {
        val bar = ProgressBar(
            id = 2,
            x = 30,
            y = 110,
            width = 244,
            height = 25,
            orientation = Orientation.HORIZONTAL,
            minValue = 0f,
            maxValue = 400f,     // Max power range
            showBorder = true,
            dataField = DataFieldRegistry.getById(7)!! // Power
        )

        return ZonedProgressBar(
            bar = bar,
            zones = listOf(
                ZonedProgressBar.Zone("Z1", 0f, 140f, 2),     // Recovery - lightest
                ZonedProgressBar.Zone("Z2", 140f, 190f, 4),   // Endurance - light
                ZonedProgressBar.Zone("Z3", 190f, 220f, 6),   // Tempo - light-mid
                ZonedProgressBar.Zone("Z4", 220f, 260f, 8),   // Threshold - mid
                ZonedProgressBar.Zone("Z5", 260f, 300f, 10),  // VO2 Max - mid-bright
                ZonedProgressBar.Zone("Z6", 300f, 360f, 12),  // Anaerobic - bright
                ZonedProgressBar.Zone("Z7", 360f, 400f, 14)   // Neuromuscular - brightest
            )
        )
    }

    /**
     * Power Bar - Simple horizontal bar for power percentage
     *
     * Visual: Clean horizontal bar showing power as percentage of FTP
     */
    fun createPowerBar(): ProgressBar {
        return ProgressBar(
            id = 2,
            x = 30,
            y = 150,
            width = 244,
            height = 20,
            orientation = Orientation.HORIZONTAL,
            minValue = 0f,
            maxValue = 400f,
            showBorder = true,
            dataField = DataFieldRegistry.getById(7)!! // Power
        )
    }

    /**
     * Speed Bar - Horizontal bar for speed visualization
     *
     * Visual: Progress bar showing current speed relative to max
     */
    fun createSpeedBar(): ProgressBar {
        return ProgressBar(
            id = 3,
            x = 30,
            y = 190,
            width = 244,
            height = 15,
            orientation = Orientation.HORIZONTAL,
            minValue = 0f,
            maxValue = 60f,      // 60 km/h max
            showBorder = true,
            dataField = DataFieldRegistry.getById(12)!! // Speed
        )
    }

    /**
     * Cadence Vertical Bar - Vertical bar for cadence
     *
     * Visual: Vertical bar on side of display (useful for multi-metric layouts)
     */
    fun createCadenceVerticalBar(): ProgressBar {
        return ProgressBar(
            id = 4,
            x = 270,
            y = 30,
            width = 20,
            height = 200,
            orientation = Orientation.VERTICAL,
            minValue = 0f,
            maxValue = 120f,
            showBorder = true,
            dataField = DataFieldRegistry.getById(8)!! // Cadence
        )
    }

    /**
     * Get default visualization for a data field
     *
     * Returns the most appropriate default gauge or bar for the given metric
     */
    fun getDefaultVisualizationFor(dataField: DataField): Pair<VisualizationType, Any?> {
        return when (dataField.id) {
            4 -> VisualizationType.ZONED_BAR to createHRZoneBar() // Heart Rate
            7 -> VisualizationType.GAUGE to createPowerGauge()     // Power
            8 -> VisualizationType.GAUGE to createCadenceGauge()   // Cadence
            12 -> VisualizationType.BAR to createSpeedBar()        // Speed
            else -> VisualizationType.TEXT to null                 // Default to text
        }
    }
}

