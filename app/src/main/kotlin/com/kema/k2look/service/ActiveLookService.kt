package com.kema.k2look.service

import android.bluetooth.BluetoothManager
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import com.activelook.activelooksdk.DiscoveredGlasses
import com.activelook.activelooksdk.Glasses
import com.activelook.activelooksdk.Sdk
import com.activelook.activelooksdk.types.ImgStreamFormat
import com.activelook.activelooksdk.types.holdFlushAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var sdk: Sdk? = null
    private var connectedGlasses: Glasses? = null

    // Tracks the DiscoveredGlasses object currently being connected, for timeout cancellation
    private var connectingGlasses: DiscoveredGlasses? = null
    private var connectionTimeoutJob: Job? = null

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

    /** Connection state enum */
    sealed class ConnectionState {
        data object Disconnected : ConnectionState()
        data object Scanning : ConnectionState()
        data object Connecting : ConnectionState()
        data class Connected(val glasses: Glasses) : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    /** Initialize the ActiveLook SDK */
    fun isSdkInitialized(): Boolean = sdk != null

    fun initializeSdk() {
        if (sdk != null) {
            Log.w(TAG, "SDK already initialized")
            return
        }

        Log.i(TAG, "Initializing ActiveLook SDK...")

        try {
            sdk =
                    Sdk.init(
                            context.applicationContext,
                            { update -> Log.i(TAG, "Firmware update started") },
                            { pair ->
                                Log.i(TAG, "Firmware update available")
                                // For now, don't auto-update during rides
                                // User can manually update through settings later
                            },
                            { update -> Log.d(TAG, "Firmware update progress") },
                            { update -> Log.i(TAG, "Firmware update successful") },
                            { update -> Log.e(TAG, "Firmware update error") }
                    )

            Log.i(TAG, "✓ ActiveLook SDK initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ActiveLook SDK: ${e.message}", e)
            _connectionState.value = ConnectionState.Error("Failed to initialize SDK: ${e.message}")
        }
    }

    /** Start scanning for ActiveLook glasses */
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

        // Pre-check: is the BLE adapter actually enabled?
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = btManager?.adapter
        if (adapter == null || !adapter.isEnabled) {
            Log.e(
                    TAG,
                    "❌ BLE adapter is ${if (adapter == null) "unavailable" else "disabled"} — scan will likely find 0 devices"
            )
            _connectionState.value = ConnectionState.Error("Bluetooth is not enabled")
            return
        }
        Log.d(TAG, "✅ BLE adapter enabled, state=${adapter.state}")

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
                    Log.d(TAG, "⏭️ Device already in list, skipping: ${discoveredGlasses.name}")
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

    /** Stop scanning for glasses */
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

    /** Connect to discovered glasses */
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

        // Watchdog: if neither onConnected nor onConnectionFail fires within the timeout,
        // the device was likely discovered from the Android BLE bonded cache rather than a
        // live advertisement.  Cancel the attempt and surface an error so the caller can retry.
        connectingGlasses = glasses
        connectionTimeoutJob?.cancel()
        connectionTimeoutJob =
                serviceScope.launch {
                    delay(CONNECTION_TIMEOUT_MS)
                    if (_connectionState.value is ConnectionState.Connecting) {
                        Log.w(
                                TAG,
                                "⏱️ Connection timed out for ${glasses.name} (${glasses.address}) — " +
                                        "device may not be advertising (stale BLE cache entry?)"
                        )
                        try {
                            connectingGlasses?.cancelConnection()
                        } catch (e: Exception) {
                            Log.w(TAG, "cancelConnection threw: ${e.message}")
                        }
                        connectingGlasses = null
                        connectedGlasses = null
                        _connectionState.value =
                                ConnectionState.Error(
                                        "Connection timed out — glasses not found advertising. " +
                                                "Try scanning again when glasses are powered on."
                                )
                    }
                }

        try {
            Log.d(TAG, "Calling glasses.connect()...")
            glasses.connect(
                    { connectedGlasses ->
                        connectionTimeoutJob?.cancel()
                        connectingGlasses = null

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

                        // Post-connect initialisation is done off the BLE callback thread.
                        // Blocking the callback thread (Thread.sleep / BLE commands) prevents
                        // the BLE stack from processing GATT responses and can cause the
                        // firmware to consider the connection lost, leading to a spurious
                        // disconnect while the logo is still displayed.
                        serviceScope.launch(Dispatchers.IO) {
                            try {
                                // Brief delay for firmware startup, then clear boot screen.
                                delay(500)
                                connectedGlasses.clear()
                                Log.i(TAG, "✓ Display cleared after connect")

                                // Play K2Look logo animation.
                                displayLogoAnimation(frameDelayMs = 300L)
                                delay(500)
                                connectedGlasses.clear()

                                Log.i(TAG, "✓ Connection established successfully")
                            } catch (e: Exception) {
                                Log.w(TAG, "Post-connect init failed: ${e.message}")
                            }
                        }
                    },
                    { failedGlasses ->
                        connectionTimeoutJob?.cancel()
                        connectingGlasses = null

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

                        // Clear the display so the last frame (e.g. logo) doesn't persist
                        // while the app shows "Searching for glasses".
                        try {
                            disconnectedGlasses.clear()
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not clear display on disconnect: ${e.message}")
                        }

                        _connectionState.value = ConnectionState.Disconnected
                        this.connectedGlasses = null

                        Log.w(TAG, "Connection lost - glasses disconnected")
                    }
            )
            Log.d(TAG, "glasses.connect() call completed, waiting for callbacks...")
        } catch (e: Exception) {
            connectionTimeoutJob?.cancel()
            connectingGlasses = null

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

    /** Disconnect from glasses */
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

    /** Delete a layout from glasses memory (Phase 4.2) */
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

    /** Delete all layouts in the current config namespace */
    fun layoutDeleteAll() {
        layoutDelete(0xFF.toByte())
    }

    /** Check if connected to glasses */
    val isConnected: Boolean
        get() = connectedGlasses != null && _connectionState.value is ConnectionState.Connected

    /** Get the currently connected glasses */
    fun getConnectedGlasses(): Glasses? = connectedGlasses

    /** Clean up resources */
    fun cleanup() {
        Log.i(TAG, "Cleaning up ActiveLook service...")

        // Cancel any in-flight connection attempt
        connectionTimeoutJob?.cancel()
        connectionTimeoutJob = null
        connectingGlasses = null

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

        // Cancel the service-internal coroutine scope
        serviceScope.cancel()

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
            glasses.gaugeDisplay(gaugeId.toByte(), clampedPercentage.toByte())
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
            } catch (_: Exception) {}
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
            } catch (_: Exception) {}
            false
        }
    }

    /** Setup listeners for gesture and touch events from glasses */
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

    /** Handle sensor interface events (gesture or touch) */
    private fun handleSensorEvent() {
        Log.i(TAG, "🖐️ Sensor event detected (gesture or touch)")
        _gestureEvents.value += 1
        Log.d(TAG, "Total sensor events: ${_gestureEvents.value}")
    }

    /** Enable or disable the gesture sensor on the glasses */
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

    /**
     * Display the K2Look logo animation on the glasses using the 7 pre-extracted frames. Streams
     * each frame directly; no glasses memory is consumed.
     *
     * @param x Left edge of the image on the display
     * @param y Top edge of the image on the display
     * @param frameDelayMs Delay between frames in milliseconds (default 100 ms → ~10 fps)
     */
    fun displayLogoAnimation(x: Short = 58, y: Short = 16, frameDelayMs: Long = 100L) {
        val glasses = connectedGlasses
        if (glasses == null) {
            Log.w(TAG, "Cannot display logo animation: No glasses connected")
            return
        }

        // Log.i(TAG, "▶ Playing K2Look logo animation")

        var currentDelay = frameDelayMs
        for (index in 0..6) {
            val filename = "frame_%02d.png".format(index)
            try {
                context.assets.open(filename).use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    glasses.imgStream(bitmap, ImgStreamFormat.MONO_4BPP_HEATSHRINK, x, y)
                }
                if (currentDelay > 0L && index < 6) {
                    Thread.sleep(currentDelay)
                    currentDelay = (currentDelay * 0.9).toLong()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stream frame $filename: ${e.message}", e)
            }
        }

        Log.i(TAG, "✓ Logo animation complete")
    }

    companion object {
        private const val TAG = "ActiveLookService"
        /** How long to wait for a connect() callback before giving up (ms) */
        private const val CONNECTION_TIMEOUT_MS = 15_000L
    }
}
