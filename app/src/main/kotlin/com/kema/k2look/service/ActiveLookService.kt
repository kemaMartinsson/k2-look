package com.kema.k2look.service

import android.content.Context
import android.graphics.Point
import android.util.Log
import com.activelook.activelooksdk.DiscoveredGlasses
import com.activelook.activelooksdk.Glasses
import com.activelook.activelooksdk.Sdk
import com.activelook.activelooksdk.types.Rotation
import com.activelook.activelooksdk.types.holdFlushAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service responsible for managing ActiveLook glasses connection and communication.
 *
 * This service handles:
 * - SDK initialization
 * - BLE scanning for glasses
 * - Connection management (connect/disconnect)
 * - Connection state monitoring
 * - Text display on glasses
 * - Error handling and recovery
 */
class ActiveLookService(private val context: Context) {

    private var sdk: Sdk? = null
    private var connectedGlasses: Glasses? = null

    // Connection state
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Discovered glasses
    private val _discoveredGlasses = MutableStateFlow<List<DiscoveredGlasses>>(emptyList())
    val discoveredGlasses: StateFlow<List<DiscoveredGlasses>> = _discoveredGlasses.asStateFlow()

    // Scanning state
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Gesture/Touch events
    private val _gestureEvents = MutableStateFlow(0) // Counter for gesture events
    val gestureEvents: StateFlow<Int> = _gestureEvents.asStateFlow()

    private val _touchEvents = MutableStateFlow(0) // Counter for touch events
    val touchEvents: StateFlow<Int> = _touchEvents.asStateFlow()

    /**
     * Connection state enum
     */
    sealed class ConnectionState {
        data object Disconnected : ConnectionState()
        data object Scanning : ConnectionState()
        data object Connecting : ConnectionState()
        data class Connected(val glasses: Glasses) : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    /**
     * Initialize the ActiveLook SDK
     */
    fun initializeSdk() {
        if (sdk != null) {
            Log.w(TAG, "SDK already initialized")
            return
        }

        Log.i(TAG, "Initializing ActiveLook SDK...")

        try {
            sdk = Sdk.init(
                context.applicationContext,
                { update ->
                    Log.i(TAG, "Firmware update started")
                },
                { pair ->
                    Log.i(TAG, "Firmware update available")
                    // For now, don't auto-update during rides
                    // User can manually update through settings later
                },
                { update ->
                    Log.d(TAG, "Firmware update progress")
                },
                { update ->
                    Log.i(TAG, "Firmware update successful")
                },
                { update ->
                    Log.e(TAG, "Firmware update error")
                }
            )

            Log.i(TAG, "✓ ActiveLook SDK initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ActiveLook SDK: ${e.message}", e)
            _connectionState.value = ConnectionState.Error("Failed to initialize SDK: ${e.message}")
        }
    }

    /**
     * Start scanning for ActiveLook glasses
     */
    fun startScanning() {
        val sdkInstance = sdk
        if (sdkInstance == null) {
            Log.e(TAG, "❌ SDK not initialized. Call initializeSdk() first.")
            _connectionState.value = ConnectionState.Error("SDK not initialized")
            return
        }

        if (_isScanning.value) {
            Log.w(TAG, "⚠️ Already scanning")
            return
        }

        Log.i(TAG, "🔍 === Starting BLE scan for ActiveLook glasses ===")
        Log.d(TAG, "SDK instance: $sdkInstance")
        Log.i(TAG, "Current connection state: ${_connectionState.value}")
        _connectionState.value = ConnectionState.Scanning
        _isScanning.value = true
        _discoveredGlasses.value = emptyList()

        try {
            sdkInstance.startScan { discoveredGlasses ->
                Log.i(TAG, "👓 === DISCOVERED DEVICE ===")
                Log.i(TAG, "  Name: ${discoveredGlasses.name}")
                Log.i(TAG, "  Address: ${discoveredGlasses.address}")
                Log.i(TAG, "  Manufacturer: ${discoveredGlasses.manufacturer}")
                Log.i(TAG, "  toString(): $discoveredGlasses")
                Log.i(TAG, "========================")

                // Add to discovered list if not already present
                val currentList = _discoveredGlasses.value.toMutableList()
                if (currentList.none { it.address == discoveredGlasses.address }) {
                    currentList.add(discoveredGlasses)
                    _discoveredGlasses.value = currentList
                    Log.i(TAG, "✅ Added to discovered list: ${discoveredGlasses.name}")
                    Log.d(TAG, "Total discovered devices: ${currentList.size}")
                } else {
                    Log.d(
                        TAG,
                        "⏭️ Device already in list, skipping: ${discoveredGlasses.name}"
                    )
                }
            }
            Log.i(TAG, "✅ Scan started successfully, waiting for devices...")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error starting scan: ${e.message}", e)
            Log.e(TAG, "Exception type: ${e.javaClass.name}")
            Log.e(TAG, "Stack trace:", e)
            _connectionState.value = ConnectionState.Error("Scan failed: ${e.message}")
            _isScanning.value = false
        }
    }

    /**
     * Stop scanning for glasses
     */
    fun stopScanning() {
        val sdkInstance = sdk
        if (sdkInstance == null) {
            Log.w(TAG, "SDK not initialized")
            return
        }

        if (!_isScanning.value) {
            return
        }

        Log.i(TAG, "Stopping BLE scan...")

        try {
            sdkInstance.stopScan()
            _isScanning.value = false

            if (_connectionState.value is ConnectionState.Scanning) {
                _connectionState.value = ConnectionState.Disconnected
            }

            Log.i(TAG, "✓ Scan stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping scan: ${e.message}", e)
        }
    }

    /**
     * Connect to discovered glasses
     */
    fun connect(glasses: DiscoveredGlasses) {
        Log.i(TAG, "🔌 === INITIATING CONNECTION ===")
        Log.i(TAG, "  Target Name: ${glasses.name}")
        Log.i(TAG, "  Target Address: ${glasses.address}")
        Log.i(TAG, "  Target Manufacturer: ${glasses.manufacturer}")
        Log.i(TAG, "  Current State: ${_connectionState.value}")
        Log.i(TAG, "  Is Scanning: ${_isScanning.value}")

        // Stop scanning if active
        if (_isScanning.value) {
            Log.d(TAG, "Stopping scan before connection...")
            stopScanning()
        }

        Log.d(TAG, "Setting state to Connecting...")
        _connectionState.value = ConnectionState.Connecting

        try {
            Log.d(TAG, "Calling glasses.connect()...")
            glasses.connect(
                { connectedGlasses ->
                    Log.i(TAG, "✅ === CONNECTION SUCCESS ===")
                    Log.i(TAG, "  Connected Name: ${connectedGlasses.name}")
                    Log.i(TAG, "  Connected Address: ${connectedGlasses.address}")
                    Log.i(TAG, "  Manufacturer: ${connectedGlasses.manufacturer}")
                    Log.i(TAG, "=========================")

                    this.connectedGlasses = connectedGlasses
                    _connectionState.value = ConnectionState.Connected(connectedGlasses)

                    // Subscribe to sensor interface notifications (gesture & touch events)
                    setupGestureAndTouchListeners(connectedGlasses)

                    // Enable gesture sensor on glasses
                    enableGestureSensor(true)

                    Log.i(TAG, "✓ Connection established successfully")
                },
                { failedGlasses ->
                    Log.e(TAG, "=== CONNECTION FAILED ===")
                    Log.e(TAG, "  Failed Name: ${failedGlasses.name}")
                    Log.e(TAG, "  Failed Address: ${failedGlasses.address}")
                    Log.e(TAG, "  Manufacturer: ${failedGlasses.manufacturer}")
                    Log.e(TAG, "  Connection State: Failed")
                    Log.e(TAG, "=========================")

                    _connectionState.value = ConnectionState.Error("Connection failed")
                    this.connectedGlasses = null

                    Log.e(TAG, "✗ Connection failed - check glasses power, BLE, and proximity")
                },
                { disconnectedGlasses ->
                    Log.w(TAG, "=== DISCONNECTION EVENT ===")
                    Log.w(TAG, "  Disconnected Name: ${disconnectedGlasses.name}")
                    Log.w(TAG, "  Disconnected Address: ${disconnectedGlasses.address}")
                    Log.w(TAG, "  Previous State: ${_connectionState.value}")
                    Log.w(TAG, "===========================")

                    _connectionState.value = ConnectionState.Disconnected
                    this.connectedGlasses = null

                    Log.w(TAG, "Connection lost - glasses disconnected")
                }
            )
            Log.d(TAG, "glasses.connect() call completed, waiting for callbacks...")
        } catch (e: Exception) {
            Log.e(TAG, "=== CONNECTION EXCEPTION ===")
            Log.e(TAG, "  Exception Type: ${e.javaClass.name}")
            Log.e(TAG, "  Message: ${e.message}")
            Log.e(TAG, "  Stack trace:", e)
            Log.e(TAG, "============================")

            _connectionState.value = ConnectionState.Error("Connection error: ${e.message}")
            this.connectedGlasses = null

            Log.e(TAG, "✗ Exception during connection attempt")
        }
    }

    /**
     * Disconnect from glasses
     */
    fun disconnect() {
        val glasses = connectedGlasses
        if (glasses == null) {
            Log.w(TAG, "No glasses connected")
            return
        }

        Log.i(TAG, "Disconnecting from glasses: ${glasses.name}...")

        try {
            glasses.disconnect()
            connectedGlasses = null
            _connectionState.value = ConnectionState.Disconnected
            Log.i(TAG, "✓ Disconnected successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect: ${e.message}", e)
            _connectionState.value = ConnectionState.Error("Disconnect error: ${e.message}")
        }
    }

    /**
     * Display text on glasses
     */
    fun displayText(text: String, x: Int = 0, y: Int = 0) {
        val glasses = connectedGlasses
        if (glasses == null) {
            Log.w(TAG, "Cannot display text: No glasses connected")
            return
        }

        try {
            // Clear display first
            glasses.clear()

            // Display text at position with default rotation, font size, and color
            val position = Point(x, y)
            val rotation = Rotation.TOP_LR
            val fontSize: Byte = 3
            val color: Byte = 15  // White/Full brightness (0-15)

            glasses.txt(position, rotation, fontSize, color, text)

            Log.d(TAG, "Displayed text: '$text' at ($x, $y)")
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying text: ${e.message}", e)
        }
    }

    /**
     * Clear the glasses display
     */
    fun clearDisplay() {
        val glasses = connectedGlasses
        if (glasses == null) {
            Log.w(TAG, "Cannot clear display: No glasses connected")
            return
        }

        try {
            glasses.clear()
            Log.d(TAG, "Display cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing display: ${e.message}", e)
        }
    }

    /**
     * Display a configured field with label, icon, and value at calculated position
     * Uses zone-based positioning
     */
    fun displayField(
        field: com.kema.k2look.model.LayoutDataField,
        value: String,
        sectionY: Int
    ) {
        val glasses = connectedGlasses
        if (glasses == null) {
            Log.w(TAG, "Cannot display field: No glasses connected")
            return
        }

        try {
            // Constants for this method (localized from removed LayoutBuilder constants)
            val MARGIN_X = 10
            val ICON_LEFT_MARGIN = 10
            val ICON_LABEL_SPACING = 10
            val LABEL_OFFSET_Y = 15
            val VALUE_OFFSET_Y = 45
            val DISPLAY_WIDTH = 304
            val SECTION_HEIGHT = 85

            // Calculate label X position based on icon display
            var labelX = if (field.showIcon && field.dataField.icon28 != null) {
                ICON_LEFT_MARGIN + field.iconSize.pixels + ICON_LABEL_SPACING
            } else {
                MARGIN_X + 5
            }

            // Display icon if enabled
            if (field.showIcon) {
                val iconId = when (field.iconSize) {
                    com.kema.k2look.model.IconSize.SMALL -> field.dataField.icon28
                    com.kema.k2look.model.IconSize.LARGE -> field.dataField.icon40
                }

                if (iconId != null) {
                    val iconX = ICON_LEFT_MARGIN
                    val iconY = sectionY + (SECTION_HEIGHT - field.iconSize.pixels) / 2
                    glasses.imgDisplay(iconId.toByte(), iconX.toShort(), iconY.toShort())
                    Log.v(TAG, "  Icon $iconId at ($iconX, $iconY)")
                }
            }

            // Display label if enabled
            if (field.showLabel) {
                val labelText = buildLabelText(field)
                val labelY = sectionY + LABEL_OFFSET_Y
                glasses.txt(
                    Point(labelX, labelY),
                    Rotation.TOP_LR,
                    1, // Small font for labels
                    15, // White color
                    labelText
                )
                Log.v(TAG, "  Label '$labelText' at ($labelX, $labelY)")
            }

            // Display value (centered) - font determined by zone in layout builder
            val valueX = DISPLAY_WIDTH / 2
            val valueY = sectionY + VALUE_OFFSET_Y
            // Use medium font as default (font is now determined by zone in efficient mode)
            val fontId = 2 // Medium font
            glasses.txt(
                Point(valueX, valueY),
                Rotation.TOP_LR,
                fontId.toByte(),
                15, // White color
                value
            )
            Log.v(TAG, "  Value '$value' at ($valueX, $valueY) font=$fontId")

            // Draw separator line (except for bottom section)
            if (sectionY < SECTION_HEIGHT * 2) {
                val lineY = sectionY + SECTION_HEIGHT - 1
                glasses.line(
                    Point(MARGIN_X, lineY),
                    Point(DISPLAY_WIDTH - MARGIN_X, lineY)
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error displaying field ${field.dataField.name}: ${e.message}", e)
        }
    }

    /**
     * Build label text with optional unit
     */
    private fun buildLabelText(field: com.kema.k2look.model.LayoutDataField): String {
        val name = field.dataField.name.uppercase()
        return if (field.showUnit && field.dataField.unit.isNotEmpty()) {
            "$name (${field.dataField.unit})"
        } else {
            name
        }
    }


    /**
     * Display a value using a pre-saved layout (Phase 4.2)
     * Much more efficient than txt() - only sends the value!
     */
    fun layoutDisplay(layoutId: Byte, value: String) {
        val glasses = connectedGlasses
        if (glasses == null) {
            Log.w(TAG, "Cannot display layout: No glasses connected")
            return
        }

        try {
            glasses.layoutDisplay(layoutId, value)
            Log.v(TAG, "Layout $layoutId displayed: '$value'")
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying layout $layoutId: ${e.message}", e)
        }
    }

    /**
     * Delete a layout from glasses memory (Phase 4.2)
     */
    fun layoutDelete(layoutId: Byte) {
        val glasses = connectedGlasses
        if (glasses == null) {
            Log.w(TAG, "Cannot delete layout: No glasses connected")
            return
        }

        try {
            glasses.layoutDelete(layoutId)
            Log.d(TAG, "Layout $layoutId deleted")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting layout $layoutId: ${e.message}", e)
        }
    }

    /**
     * Check if connected to glasses
     */
    val isConnected: Boolean
        get() = connectedGlasses != null && _connectionState.value is ConnectionState.Connected

    /**
     * Get the currently connected glasses
     */
    fun getConnectedGlasses(): Glasses? = connectedGlasses

    /**
     * Clean up resources
     */
    fun cleanup() {
        Log.i(TAG, "Cleaning up ActiveLook service...")

        // Stop scanning if active
        if (_isScanning.value) {
            stopScanning()
        }

        // Disconnect if connected
        if (connectedGlasses != null) {
            disconnect()
        }

        // Clear state
        _discoveredGlasses.value = emptyList()

        Log.i(TAG, "✓ Cleanup complete")
    }

    // ========== GAUGE COMMANDS ==========

    /**
     * Save gauge configuration to glasses memory
     *
     * @param gauge Gauge configuration
     * @return true if successful
     */
    suspend fun saveGauge(gauge: com.kema.k2look.model.Gauge): Boolean {
        val glasses = connectedGlasses
        if (glasses == null) {
            Log.w(TAG, "Cannot save gauge: No glasses connected")
            return false
        }

        return try {
            glasses.gaugeSave(
                gauge.id.toByte(),
                gauge.centerX.toShort(),
                gauge.centerY.toShort(),
                gauge.radiusOuter.toChar(),
                gauge.radiusInner.toChar(),
                gauge.startPortion.toByte(),
                gauge.endPortion.toByte(),
                gauge.clockwise
            )
            Log.i(TAG, "✓ Gauge ${gauge.id} saved (${gauge.dataField.name})")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save gauge ${gauge.id}", e)
            false
        }
    }

    /**
     * Display gauge with percentage value
     *
     * @param gaugeId Gauge identifier
     * @param percentage Value 0-100
     * @return true if successful
     */
    suspend fun displayGauge(gaugeId: Int, percentage: Int): Boolean {
        val glasses = connectedGlasses
        if (glasses == null) {
            Log.w(TAG, "Cannot display gauge: No glasses connected")
            return false
        }

        return try {
            val clampedPercentage = percentage.coerceIn(0, 100)
            glasses.gaugeDisplay(
                gaugeId.toByte(),
                clampedPercentage.toByte()
            )
            Log.d(TAG, "Gauge $gaugeId: $clampedPercentage%")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to display gauge $gaugeId", e)
            false
        }
    }

    /**
     * Delete gauge from glasses memory
     *
     * @param gaugeId Gauge identifier (or 0xFF for all)
     * @return true if successful
     */
    suspend fun deleteGauge(gaugeId: Int): Boolean {
        val glasses = connectedGlasses
        if (glasses == null) {
            Log.w(TAG, "Cannot delete gauge: No glasses connected")
            return false
        }

        return try {
            glasses.gaugeDelete(gaugeId.toByte())
            Log.i(TAG, "✓ Gauge $gaugeId deleted")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to delete gauge $gaugeId", e)
            false
        }
    }

    // ========== PROGRESS BAR COMMANDS ==========

    /**
     * Display progress bar using rectangles
     *
     * @param bar Progress bar configuration
     * @param percentage Value 0-100
     * @return true if successful
     */
    suspend fun displayProgressBar(
        bar: com.kema.k2look.model.ProgressBar,
        percentage: Int
    ): Boolean {
        val glasses = connectedGlasses
        if (glasses == null) {
            Log.w(TAG, "Cannot display progress bar: No glasses connected")
            return false
        }

        return try {
            val clampedPercentage = percentage.coerceIn(0, 100)
            val fillAmount = bar.calculateFillAmount(clampedPercentage.toFloat())

            // Hold flush to prevent flickering
            glasses.holdFlush(holdFlushAction.HOLD)

            // Clear previous bar area
            glasses.color(0) // Black
            glasses.rectf(
                bar.x.toShort(),
                bar.y.toShort(),
                (bar.x + bar.width).toShort(),
                (bar.y + bar.height).toShort()
            )

            // Draw border if enabled
            if (bar.showBorder) {
                glasses.color(8) // Mid-grey border
                glasses.rect(
                    bar.x.toShort(),
                    bar.y.toShort(),
                    (bar.x + bar.width).toShort(),
                    (bar.y + bar.height).toShort()
                )
            }

            // Draw filled portion
            glasses.color(15) // White fill
            when (bar.orientation) {
                com.kema.k2look.model.Orientation.HORIZONTAL -> {
                    if (fillAmount > 0) {
                        glasses.rectf(
                            bar.x.toShort(),
                            bar.y.toShort(),
                            (bar.x + fillAmount).toShort(),
                            (bar.y + bar.height).toShort()
                        )
                    }
                }

                com.kema.k2look.model.Orientation.VERTICAL -> {
                    if (fillAmount > 0) {
                        val fillY = bar.y + bar.height - fillAmount
                        glasses.rectf(
                            bar.x.toShort(),
                            fillY.toShort(),
                            (bar.x + bar.width).toShort(),
                            (bar.y + bar.height).toShort()
                        )
                    }
                }
            }

            // Flush to display
            glasses.holdFlush(holdFlushAction.FLUSH)

            Log.d(TAG, "Progress bar ${bar.id}: $clampedPercentage% (${bar.dataField.name})")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to display progress bar ${bar.id}", e)
            // Try to flush anyway to recover
            try {
                connectedGlasses?.holdFlush(holdFlushAction.FLUSH)
            } catch (_: Exception) {
            }
            false
        }
    }

    /**
     * Display zoned progress bar with color-coded zones
     *
     * @param zonedBar Zoned bar configuration
     * @param currentValue Current metric value
     * @return true if successful
     */
    suspend fun displayZonedBar(
        zonedBar: com.kema.k2look.model.ZonedProgressBar,
        currentValue: Float
    ): Boolean {
        val glasses = connectedGlasses
        if (glasses == null) {
            Log.w(TAG, "Cannot display zoned bar: No glasses connected")
            return false
        }

        return try {
            val bar = zonedBar.bar

            // Hold flush to prevent flickering
            glasses.holdFlush(holdFlushAction.HOLD)

            // Clear previous bar area
            glasses.color(0) // Black
            glasses.rectf(
                bar.x.toShort(),
                bar.y.toShort(),
                (bar.x + bar.width).toShort(),
                (bar.y + bar.height).toShort()
            )

            // Draw border
            if (bar.showBorder) {
                glasses.color(8) // Mid-grey border
                glasses.rect(
                    bar.x.toShort(),
                    bar.y.toShort(),
                    (bar.x + bar.width).toShort(),
                    (bar.y + bar.height).toShort()
                )
            }

            // Draw each zone as background
            zonedBar.zones.forEach { zone ->
                val zoneStartPx = zonedBar.valueToPixel(zone.minValue)
                val zoneEndPx = zonedBar.valueToPixel(zone.maxValue)

                // Draw zone background
                glasses.color(zone.color.toByte())
                when (bar.orientation) {
                    com.kema.k2look.model.Orientation.HORIZONTAL -> {
                        glasses.rectf(
                            zoneStartPx.toShort(),
                            (bar.y + 2).toShort(),
                            zoneEndPx.toShort(),
                            (bar.y + bar.height - 2).toShort()
                        )
                    }

                    com.kema.k2look.model.Orientation.VERTICAL -> {
                        glasses.rectf(
                            (bar.x + 2).toShort(),
                            zoneEndPx.toShort(),
                            (bar.x + bar.width - 2).toShort(),
                            zoneStartPx.toShort()
                        )
                    }
                }
            }

            // Draw current value indicator (bright overlay)
            val currentPx = zonedBar.valueToPixel(currentValue)
            val currentZone = zonedBar.findZone(currentValue)

            glasses.color(15) // White indicator
            when (bar.orientation) {
                com.kema.k2look.model.Orientation.HORIZONTAL -> {
                    // Draw vertical line at current position
                    glasses.rectf(
                        (currentPx - 1).toShort(),
                        bar.y.toShort(),
                        (currentPx + 1).toShort(),
                        (bar.y + bar.height).toShort()
                    )
                }

                com.kema.k2look.model.Orientation.VERTICAL -> {
                    // Draw horizontal line at current position
                    glasses.rectf(
                        bar.x.toShort(),
                        (currentPx - 1).toShort(),
                        (bar.x + bar.width).toShort(),
                        (currentPx + 1).toShort()
                    )
                }
            }

            // Flush to display
            glasses.holdFlush(holdFlushAction.FLUSH)

            Log.d(
                TAG,
                "Zoned bar ${bar.id}: ${currentValue.toInt()} ${currentZone?.name ?: "?"} (${bar.dataField.name})"
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to display zoned bar ${zonedBar.bar.id}", e)
            // Try to flush anyway to recover
            try {
                connectedGlasses?.holdFlush(holdFlushAction.FLUSH)
            } catch (_: Exception) {
            }
            false
        }
    }

    /**
     * Setup listeners for gesture and touch events from glasses
     */
    private fun setupGestureAndTouchListeners(glasses: Glasses) {
        try {
            Log.i(TAG, "Setting up gesture and touch event listeners...")

            glasses.subscribeToSensorInterfaceNotifications {
                // This callback is triggered for both gesture AND touch events
                handleSensorEvent()
            }

            Log.i(TAG, "✓ Gesture/Touch event listeners enabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup gesture/touch listeners: ${e.message}", e)
        }
    }

    /**
     * Handle sensor interface events (gesture or touch)
     */
    private fun handleSensorEvent() {
        Log.i(TAG, "🖐️ Sensor event detected (gesture or touch)")
        _gestureEvents.value += 1
        Log.d(TAG, "Total sensor events: ${_gestureEvents.value}")
    }

    /**
     * Enable or disable the gesture sensor on the glasses
     */
    fun enableGestureSensor(enable: Boolean) {
        val glasses = connectedGlasses
        if (glasses == null) {
            Log.w(TAG, "Cannot enable gesture sensor - no glasses connected")
            return
        }

        try {
            Log.i(TAG, "Enabling gesture sensor: $enable")
            // The ActiveLook SDK should handle gesture sensor enabling through sensor commands
            // For now, the gesture notifications will be received once subscribed
            Log.i(TAG, "✓ Gesture sensor subscription active")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable gesture sensor: ${e.message}", e)
        }
    }

    /**
     * Set display luminance (brightness)
     * @param level Brightness level (0-15, where 0 is dimmest and 15 is brightest)
     */
    fun setLuminance(level: Int) {
        val glasses = connectedGlasses
        if (glasses == null) {
            Log.w(TAG, "Cannot set luminance - no glasses connected")
            return
        }

        try {
            val clampedLevel = level.coerceIn(0, 15)
            Log.i(TAG, "Setting luminance to level $clampedLevel")
            glasses.luma(clampedLevel.toByte())
            Log.i(TAG, "✓ Luminance set to $clampedLevel")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set luminance: ${e.message}", e)
        }
    }

    /**
     * Set display power on or off
     * @param enable True to turn display on, false to turn it off
     */
    fun setDisplayPower(enable: Boolean) {
        val glasses = connectedGlasses
        if (glasses == null) {
            Log.w(TAG, "Cannot set display power - no glasses connected")
            return
        }

        try {
            Log.i(TAG, "Setting display power: ${if (enable) "ON" else "OFF"}")
            glasses.power(enable)
            Log.i(TAG, "✓ Display power ${if (enable) "enabled" else "disabled"}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set display power: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "ActiveLookService"
    }
}

