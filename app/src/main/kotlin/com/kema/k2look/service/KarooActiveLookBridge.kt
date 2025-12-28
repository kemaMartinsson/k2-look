package com.kema.k2look.service

import android.content.Context
import android.graphics.Point
import android.util.Log
import com.activelook.activelooksdk.DiscoveredGlasses
import com.activelook.activelooksdk.types.Rotation
import com.kema.k2look.util.PreferencesManager
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.StreamState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Bridge service that connects Karoo data stream to ActiveLook display output.
 *
 * This service handles:
 * - Data transformation from Karoo to ActiveLook format
 * - Hold/flush pattern for efficient updates
 * - Update throttling (~1 update/second)
 * - Coordinated lifecycle management of both services
 * - Display layout management
 */
class KarooActiveLookBridge(context: Context) {

    private val karooDataService = KarooDataService(context)
    private val activeLookService = ActiveLookService(context)
    private val preferencesManager = PreferencesManager(context)

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var updateJob: Job? = null
    private var reconnectJob: Job? = null
    private var scanJob: Job? = null
    private var scanTimeoutJob: Job? = null
    private var statusLogJob: Job? = null

    // Simulator mode (for Debug tab)
    private var simulatorJob: Job? = null

    // Bridge state
    private val _bridgeState = MutableStateFlow<BridgeState>(BridgeState.Idle)
    val bridgeState: StateFlow<BridgeState> = _bridgeState.asStateFlow()

    // Accumulated data for hold/flush pattern
    private val currentData = CurrentData()

    // Active DataField profile for dynamic layouts
    private var activeProfile: com.kema.k2look.model.DataFieldProfile? = null

    // Update throttling
    private var lastUpdateTime = 0L
    private val updateIntervalMs = 1000L // 1 second

    // Reconnect tracking
    private var reconnectStartTime = 0L
    private var lastConnectedGlassesAddress: String? = null
    private var isInActiveRide = false
    private val reconnectIntervalMs = 15000L // Try reconnect every 15 seconds

    // Add a lightweight heartbeat for idle logging (prevents logcat spam)
    private var lastNoDataLogTimeMs: Long = 0L
    private val noDataLogIntervalMs: Long = 30_000L

    /**
     * Bridge state enum
     */
    sealed class BridgeState {
        data object Idle : BridgeState()
        data object KarooConnecting : BridgeState()
        data object KarooConnected : BridgeState()
        data object ActiveLookScanning : BridgeState()
        data object ActiveLookConnecting : BridgeState()
        data object FullyConnected : BridgeState() // Both Karoo and ActiveLook connected
        data class Error(val message: String) : BridgeState()
        data object Streaming : BridgeState() // Actively streaming data to glasses
    }

    /**
     * Data holder for current metric values
     */
    private data class CurrentData(
        var speed: String = "--",
        var heartRate: String = "--",
        var cadence: String = "--",
        var power: String = "--",
        var distance: String = "--",
        var time: String = "--",
        var rideState: RideState = RideState.Idle,
        var isDirty: Boolean = false // Track if data has changed since last flush
    )

    /**
     * Set the active DataField profile for display layout
     */
    fun setActiveProfile(profile: com.kema.k2look.model.DataFieldProfile) {
        activeProfile = profile
        Log.i(TAG, "📋 Active profile set: ${profile.name} (${profile.screens.size} screens)")

        // If streaming, force immediate update with new layout
        if (_bridgeState.value == BridgeState.Streaming) {
            currentData.isDirty = true
            flushToGlasses()
        }
    }

    /**
     * Initialize both services and auto-connect based on preferences
     */
    fun initialize() {
        android.util.Log.i(TAG, "🚀 === Initializing KarooActiveLookBridge ===")

        // Initialize ActiveLook SDK
        activeLookService.initializeSdk()

        // Auto-connect to Karoo System if enabled (default: true)
        if (preferencesManager.isAutoConnectKarooEnabled()) {
            android.util.Log.i(TAG, "✅ Auto-connecting to Karoo System (enabled in preferences)...")
            connectKaroo()
        } else {
            android.util.Log.i(TAG, "⏭️ Auto-connect to Karoo disabled in preferences")
        }

        // Auto-connect to last paired glasses if enabled
        if (preferencesManager.isAutoConnectActiveLookEnabled()) {
            val lastGlassesAddress = preferencesManager.getLastConnectedGlassesAddress()
            if (lastGlassesAddress != null) {
                android.util.Log.i(
                    TAG,
                    "👓 Auto-connect to glasses enabled, will attempt connection to: $lastGlassesAddress"
                )
                // Start scanning to find the previously connected glasses
                attemptAutoConnectToGlasses(lastGlassesAddress)
            } else {
                android.util.Log.i(
                    TAG,
                    "⚠️ Auto-connect to glasses enabled, but no previous connection found"
                )
            }
        } else {
            android.util.Log.i(TAG, "⏭️ Auto-connect to glasses disabled in preferences")
        }

        startPeriodicStatusLogging()

        android.util.Log.i(TAG, "✅ Bridge initialized")
    }

    private fun startPeriodicStatusLogging() {
        if (statusLogJob?.isActive == true) return

        statusLogJob = scope.launch {
            while (true) {
                delay(15_000)

                val activeLookState = activeLookService.connectionState.value
                val scanning = activeLookService.isScanning.value
                val connectedAddr = try {
                    activeLookService.getConnectedGlasses()?.address
                } catch (_: Exception) {
                    null
                }

                Log.i(
                    TAG,
                    "Status: bridge=${_bridgeState.value}, karooConnected=${karooDataService.isConnected}, " +
                            "activeLookState=$activeLookState, scanning=$scanning, connectedAddr=${connectedAddr ?: "-"}"
                )
            }
        }
    }

    /**
     * Attempt to auto-connect to previously connected glasses
     */
    private fun attemptAutoConnectToGlasses(targetAddress: String) {
        val timeoutMinutes = preferencesManager.getStartupTimeoutMinutes()
        val timeoutMs = timeoutMinutes * 60 * 1000L // Convert to milliseconds

        android.util.Log.i(
            TAG,
            "🔍 Scanning for previously connected glasses: $targetAddress (timeout: ${timeoutMinutes}min)"
        )

        // Start scanning
        activeLookService.startScanning()

        var glassesFound = false

        // Observe discovered glasses and connect when found
        val collectionJob = scope.launch {
            activeLookService.discoveredGlasses.collect { glassesList ->
                android.util.Log.d(
                    TAG,
                    "📋 Discovered glasses list updated: ${glassesList.size} devices"
                )
                glassesList.forEach {
                    android.util.Log.d(TAG, "  - ${it.name} (${it.address})")
                }

                // Look for the target glasses
                val targetGlasses = glassesList.find { it.address == targetAddress }
                if (targetGlasses != null && !glassesFound) {
                    glassesFound = true
                    android.util.Log.i(
                        TAG,
                        "✅ Found previously connected glasses: ${targetGlasses.name}"
                    )
                    // Stop scanning
                    activeLookService.stopScanning()
                    // Connect to the glasses
                    connectActiveLook(targetGlasses)
                } else if (glassesList.isNotEmpty() && !glassesFound) {
                    android.util.Log.w(
                        TAG,
                        "⚠️ Found glasses but not matching target address $targetAddress"
                    )
                }
            }
        }

        // Timeout after configured minutes
        scope.launch {
            delay(timeoutMs)
            if (!glassesFound && activeLookService.isScanning.value) {
                android.util.Log.w(
                    TAG,
                    "⏱️ Startup auto-connect timeout (${timeoutMinutes}min): Could not find glasses with address $targetAddress"
                )
                android.util.Log.i(
                    TAG,
                    "🔄 Service will continue running and will attempt reconnect when ride starts"
                )
                activeLookService.stopScanning()
                collectionJob.cancel()
            }
        }
    }

    /**
     * Connect to Karoo System
     */
    fun connectKaroo() {
        Log.i(TAG, "Connecting to Karoo System...")
        _bridgeState.value = BridgeState.KarooConnecting

        karooDataService.connect()

        // Start observing Karoo data
        observeKarooData()
    }

    /**
     * Disconnect from Karoo System
     */
    fun disconnectKaroo() {
        Log.i(TAG, "Disconnecting from Karoo System...")
        karooDataService.disconnect()
        stopStreaming()
        updateBridgeState()
    }

    /**
     * Start scanning for ActiveLook glasses
     * When called from UI, will auto-connect to first discovered glasses
     */
    fun startActiveLookScan() {
        // Cancel any previous scan jobs
        scanJob?.cancel()
        scanTimeoutJob?.cancel()

        Log.i(TAG, "Starting ActiveLook scan with auto-connect...")
        _bridgeState.value = BridgeState.ActiveLookScanning
        activeLookService.startScanning()

        // Auto-connect to first discovered glasses
        var glassesFound = false
        scanJob = scope.launch {
            activeLookService.discoveredGlasses.collect { glassesList ->
                if (glassesList.isNotEmpty() && !glassesFound) {
                    val firstGlasses = glassesList.first()
                    glassesFound = true
                    Log.i(TAG, "Auto-connecting to discovered glasses: ${firstGlasses.name}")
                    activeLookService.stopScanning()

                    // Cancel scan jobs
                    scanJob?.cancel()
                    scanTimeoutJob?.cancel()

                    // Connect to the glasses
                    connectActiveLook(firstGlasses)
                }
            }
        }

        // Timeout after 30 seconds
        scanTimeoutJob = scope.launch {
            delay(30000)
            if (!glassesFound && activeLookService.isScanning.value) {
                Log.w(TAG, "Scan timeout - no glasses found")
                activeLookService.stopScanning()
                scanJob?.cancel()
                scanTimeoutJob = null
                scanJob = null
                updateBridgeState()
            }
        }
    }

    /**
     * Stop scanning for ActiveLook glasses
     */
    fun stopActiveLookScan() {
        Log.i(TAG, "Stopping ActiveLook scan...")
        activeLookService.stopScanning()
        updateBridgeState()
    }

    /**
     * Connect to ActiveLook glasses
     */
    fun connectActiveLook(glasses: DiscoveredGlasses) {
        Log.i(TAG, "Connecting to ActiveLook glasses: ${glasses.name} (${glasses.address})...")
        _bridgeState.value = BridgeState.ActiveLookConnecting

        activeLookService.connect(glasses)

        // Save glasses address for future auto-connect
        preferencesManager.setLastConnectedGlasses(glasses.address)
        Log.i(TAG, "Saved glasses address for auto-connect: ${glasses.address}")

        // Start observing ActiveLook connection state
        observeActiveLookState()
    }

    /**
     * Disconnect from ActiveLook glasses
     */
    fun disconnectActiveLook() {
        Log.i(TAG, "Disconnecting from ActiveLook glasses...")
        activeLookService.disconnect()
        stopStreaming()
        updateBridgeState()
    }

    /**
     * Observe Karoo data streams and accumulate changes
     */
    private fun observeKarooData() {
        // Observe connection state
        scope.launch {
            karooDataService.connectionState.collect { state ->
                Log.d(TAG, "Karoo connection state: $state")
                when (state) {
                    is KarooDataService.ConnectionState.Connected -> {
                        _bridgeState.value = BridgeState.KarooConnected
                        updateBridgeState()
                    }

                    is KarooDataService.ConnectionState.Error -> {
                        _bridgeState.value = BridgeState.Error("Karoo: ${state.message}")
                    }

                    else -> updateBridgeState()
                }
            }
        }

        // Observe ride state
        scope.launch {
            karooDataService.rideState.collect { state ->
                currentData.rideState = state
                currentData.isDirty = true

                // Track if we're in an active ride
                val wasInActiveRide = isInActiveRide
                isInActiveRide = state is RideState.Recording

                // Start/stop streaming based on ride state
                when (state) {
                    is RideState.Recording -> {
                        if (_bridgeState.value == BridgeState.FullyConnected) {
                            startStreaming()
                        }

                        // Start continuous reconnect if we just entered a ride
                        if (!wasInActiveRide) {
                            Log.i(
                                TAG,
                                "Entered active ride - starting continuous reconnect monitoring"
                            )
                            startContinuousReconnect()
                        }
                    }

                    else -> {
                        // Stop continuous reconnect when ride ends
                        if (wasInActiveRide) {
                            Log.i(
                                TAG,
                                "Exited active ride - stopping continuous reconnect monitoring"
                            )
                            stopContinuousReconnect()
                        }
                    }
                }
            }
        }

        // Observe speed
        scope.launch {
            karooDataService.speedData.collect { streamState ->
                currentData.speed = formatStreamData(streamState, "km/h")
                currentData.isDirty = true
            }
        }

        // Observe heart rate
        scope.launch {
            karooDataService.heartRateData.collect { streamState ->
                currentData.heartRate = formatStreamData(streamState, "bpm")
                currentData.isDirty = true
            }
        }

        // Observe cadence
        scope.launch {
            karooDataService.cadenceData.collect { streamState ->
                currentData.cadence = formatStreamData(streamState, "rpm")
                currentData.isDirty = true
            }
        }

        // Observe power
        scope.launch {
            karooDataService.powerData.collect { streamState ->
                currentData.power = formatStreamData(streamState, "w")
                currentData.isDirty = true
            }
        }

        // Observe distance
        scope.launch {
            karooDataService.distanceData.collect { streamState ->
                currentData.distance = formatStreamData(streamState, "km")
                currentData.isDirty = true
            }
        }

        // Observe time
        scope.launch {
            karooDataService.timeData.collect { streamState ->
                currentData.time = formatTimeData(streamState)
                currentData.isDirty = true
            }
        }
    }

    /**
     * Observe ActiveLook connection state
     */
    private fun observeActiveLookState() {
        scope.launch {
            activeLookService.connectionState.collect { state ->
                Log.d(TAG, "ActiveLook connection state: $state")
                when (state) {
                    is ActiveLookService.ConnectionState.Connected -> {
                        // Save the connected glasses address for reconnect attempts
                        lastConnectedGlassesAddress = state.glasses.address
                        Log.d(
                            TAG,
                            "Tracking connected glasses address: $lastConnectedGlassesAddress"
                        )

                        updateBridgeState()
                        // If Karoo is also connected and riding, start streaming
                        if (karooDataService.isConnected) {
                            startStreaming()
                        }
                    }

                    is ActiveLookService.ConnectionState.Disconnected -> {
                        // If we're in an active ride and glasses disconnected, trigger reconnect
                        if (isInActiveRide && lastConnectedGlassesAddress != null) {
                            Log.w(
                                TAG,
                                "Glasses disconnected during active ride - will attempt reconnect"
                            )
                        }
                        updateBridgeState()
                    }

                    is ActiveLookService.ConnectionState.Error -> {
                        _bridgeState.value = BridgeState.Error("ActiveLook: ${state.message}")
                    }

                    else -> updateBridgeState()
                }
            }
        }
    }

    /**
     * Update bridge state based on both service states
     */
    private fun updateBridgeState() {
        val karooConnected = karooDataService.isConnected
        val activeLookConnected = activeLookService.isConnected

        if (karooConnected && activeLookConnected) {
            if (_bridgeState.value != BridgeState.Streaming) {
                _bridgeState.value = BridgeState.FullyConnected
            }
        } else if (karooConnected) {
            _bridgeState.value = BridgeState.KarooConnected
        } else if (activeLookConnected) {
            // ActiveLook connected but not Karoo
            _bridgeState.value = BridgeState.Idle
        } else {
            _bridgeState.value = BridgeState.Idle
        }
    }

    /**
     * Start streaming data to ActiveLook glasses
     */
    private fun startStreaming() {
        if (updateJob?.isActive == true) {
            Log.d(TAG, "Already streaming")
            return
        }

        if (!karooDataService.isConnected || !activeLookService.isConnected) {
            Log.w(TAG, "Cannot start streaming: Not fully connected")
            return
        }

        Log.i(TAG, "Starting data streaming to glasses...")
        _bridgeState.value = BridgeState.Streaming

        // Start periodic flush job
        updateJob = scope.launch {
            while (true) {
                try {
                    // Wait for the update interval
                    delay(updateIntervalMs)

                    // Flush accumulated data to glasses
                    flushToGlasses()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in streaming loop: ${e.message}", e)
                }
            }
        }

        Log.i(TAG, "✓ Streaming started (1 update/second)")
    }

    /**
     * Stop streaming data to glasses
     */
    private fun stopStreaming() {
        updateJob?.cancel()
        updateJob = null

        if (_bridgeState.value == BridgeState.Streaming) {
            Log.i(TAG, "Streaming stopped")
            updateBridgeState()
        }
    }

    /**
     * Flush accumulated data to ActiveLook glasses (hold/flush pattern)
     */
    private fun flushToGlasses() {
        // Only update if data has changed
        if (!currentData.isDirty) {
            val now = System.currentTimeMillis()
            if (now - lastNoDataLogTimeMs >= noDataLogIntervalMs) {
                Log.v(TAG, "No data changes, skipping update")
                lastNoDataLogTimeMs = now
            }
            return
        }

        // Throttle updates
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTime < updateIntervalMs) {
            Log.v(TAG, "Throttling update")
            return
        }

        try {
            // Clear display
            activeLookService.clearDisplay()

            // Use profile-based layout if available, otherwise fallback to legacy
            val profile = activeProfile
            if (profile != null && profile.screens.isNotEmpty()) {
                flushWithProfile(profile)
            } else {
                Log.v(TAG, "No active profile, using legacy hardcoded layout")
                flushToGlassesLegacy()
            }

            // Mark data as flushed
            currentData.isDirty = false
            lastUpdateTime = currentTime

            Log.d(TAG, "✓ Data flushed to glasses")
        } catch (e: Exception) {
            Log.e(TAG, "Error flushing data to glasses: ${e.message}", e)
        }
    }

    /**
     * Flush data using configured DataField profile
     */
    private fun flushWithProfile(profile: com.kema.k2look.model.DataFieldProfile) {
        val screen = profile.screens.first() // TODO: Support multiple screens

        Log.v(TAG, "Flushing with profile: ${profile.name}, screen: ${screen.id}, fields: ${screen.dataFields.size}")

        screen.dataFields.forEach { field ->
            val sectionY = when (field.position) {
                com.kema.k2look.model.Position.TOP -> 0
                com.kema.k2look.model.Position.MIDDLE -> com.kema.k2look.layout.LayoutBuilder.SECTION_HEIGHT
                com.kema.k2look.model.Position.BOTTOM -> com.kema.k2look.layout.LayoutBuilder.SECTION_HEIGHT * 2
            }

            val value = getMetricValue(field.dataField)
            activeLookService.displayField(field, value, sectionY)
        }
    }

    /**
     * Get the current value for a specific metric
     */
    private fun getMetricValue(dataField: com.kema.k2look.model.DataField): String {
        return when (dataField.id) {
            1 -> currentData.time           // Elapsed Time
            2 -> currentData.distance       // Distance
            4 -> currentData.heartRate      // Heart Rate
            7 -> currentData.power          // Power
            12 -> currentData.speed         // Speed
            18 -> currentData.cadence       // Cadence
            // Add more mappings as needed
            else -> {
                Log.w(TAG, "Unknown metric ID: ${dataField.id} (${dataField.name})")
                "N/A"
            }
        }
    }

    /**
     * Legacy hardcoded layout (fallback when no profile is active)
     */
    private fun flushToGlassesLegacy() {
            // Display layout (4 metrics in 2x2 grid with margins)
            // Using 30px horizontal margins and 25px vertical margins
            // ActiveLook display is typically 304x256 pixels
            val leftX = 30
            val rightX = 160
            val topY = 30
            val midY = 100
            val bottomY = 170

            val glasses = activeLookService.getConnectedGlasses()
            if (glasses == null) {
                Log.w(TAG, "No glasses connected during flush")
                return
            }

            val rotation = Rotation.TOP_LR
            val labelFont: Byte = 1 // Small font for labels
            val valueFont: Byte = 3 // Large font for values
            val color: Byte = 15 // White

            // Top-left: Speed
            glasses.txt(Point(leftX, topY), rotation, labelFont, color, "SPD")
            glasses.txt(Point(leftX, topY + 15), rotation, valueFont, color, currentData.speed)

            // Top-right: Heart Rate
            glasses.txt(Point(rightX, topY), rotation, labelFont, color, "HR")
            glasses.txt(Point(rightX, topY + 15), rotation, valueFont, color, currentData.heartRate)

            // Mid-left: Power
            glasses.txt(Point(leftX, midY), rotation, labelFont, color, "PWR")
            glasses.txt(Point(leftX, midY + 15), rotation, valueFont, color, currentData.power)

            // Mid-right: Cadence
            glasses.txt(Point(rightX, midY), rotation, labelFont, color, "CAD")
            glasses.txt(Point(rightX, midY + 15), rotation, valueFont, color, currentData.cadence)

            // Bottom-left: Distance
            glasses.txt(Point(leftX, bottomY), rotation, labelFont, color, "DIST")
            glasses.txt(
                Point(leftX, bottomY + 15),
                rotation,
                valueFont,
                color,
                currentData.distance
            )

            // Bottom-right: Time
            glasses.txt(Point(rightX, bottomY), rotation, labelFont, color, "TIME")
            glasses.txt(Point(rightX, bottomY + 15), rotation, valueFont, color, currentData.time)
    }

    /**
     * Format stream data for display
     */
    private fun formatStreamData(streamState: StreamState?, unit: String): String {
        return when (streamState) {
            is StreamState.Streaming -> {
                val value = streamState.dataPoint.singleValue
                if (value != null) {
                    "${formatValue(value)} $unit"
                } else {
                    "-- $unit"
                }
            }

            is StreamState.Searching -> "..."
            is StreamState.Idle -> "-- $unit"
            is StreamState.NotAvailable -> "N/A"
            null -> "-- $unit"
        }
    }

    /**
     * Format time data (convert ms to HH:MM:SS)
     */
    private fun formatTimeData(streamState: StreamState?): String {
        return when (streamState) {
            is StreamState.Streaming -> {
                val ms = streamState.dataPoint.singleValue?.toLong()
                if (ms != null) {
                    val seconds = (ms / 1000) % 60
                    val minutes = (ms / (1000 * 60)) % 60
                    val hours = (ms / (1000 * 60 * 60))
                    String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
                } else {
                    "--:--:--"
                }
            }

            is StreamState.Searching -> "--:--:--"
            is StreamState.Idle -> "--:--:--"
            is StreamState.NotAvailable -> "N/A"
            null -> "--:--:--"
        }
    }

    /**
     * Format numeric value for display
     */
    private fun formatValue(value: Double): String {
        return when {
            value >= 100 -> "%.0f".format(value)
            value >= 10 -> "%.1f".format(value)
            else -> "%.1f".format(value)
        }
    }

    /**
     * Get KarooDataService for direct access if needed
     */
    fun getKarooDataService(): KarooDataService = karooDataService

    /**
     * Get ActiveLookService for direct access if needed
     */
    fun getActiveLookService(): ActiveLookService = activeLookService

    /**
     * Start a local simulator that periodically pushes sample values to the glasses.
     * This uses the same flush pipeline as normal streaming.
     */
    fun startSimulator() {
        Log.i(TAG, "🎮 startSimulator() called")
        Log.i(TAG, "  simulatorJob?.isActive: ${simulatorJob?.isActive}")
        Log.i(TAG, "  activeLookService.isConnected: ${activeLookService.isConnected}")

        if (simulatorJob?.isActive == true) {
            Log.d(TAG, "⚠️ Simulator already running")
            return
        }

        if (!activeLookService.isConnected) {
            Log.w(TAG, "❌ Cannot start simulator: glasses not connected")
            return
        }

        Log.i(TAG, "✅ Starting simulator (debug values)")
        simulatorJob = scope.launch {
            var counter = 0
            Log.i(TAG, "🔁 Simulator coroutine loop started")
            while (true) {
                counter++
                Log.d(TAG, "📊 Simulator tick $counter")
                setSimulatedMetrics(
                    speed = "${20 + (counter % 20)} km/h",
                    heartRate = "${140 + (counter % 30)} bpm",
                    cadence = "${80 + (counter % 20)} rpm",
                    power = "${200 + (counter % 100)} w",
                    distance = "${counter / 10}.${counter % 10} km",
                    time = formatSimulatedTime(counter * 2)
                )
                delay(2000)
            }
        }
        Log.i(TAG, "✅ Simulator job launched successfully")
    }

    /** Stop the simulator (if running). */
    fun stopSimulator() {
        if (simulatorJob?.isActive == true) {
            Log.i(TAG, "⏹ Stopping simulator")
        }
        simulatorJob?.cancel()
        simulatorJob = null
    }

    /**
     * Push a single set of simulated metric strings. This marks the frame dirty and flushes.
     */
    fun setSimulatedMetrics(
        speed: String,
        heartRate: String,
        cadence: String,
        power: String,
        distance: String,
        time: String
    ) {
        Log.v(TAG, "setSimulatedMetrics: SPD=$speed, HR=$heartRate, PWR=$power")
        currentData.speed = speed
        currentData.heartRate = heartRate
        currentData.cadence = cadence
        currentData.power = power
        currentData.distance = distance
        currentData.time = time
        currentData.isDirty = true

        Log.v(TAG, "Calling flushToGlasses() from simulator")
        // Flush immediately (still respects internal throttling), so simulator works even when
        // Karoo isn't connected and the normal 1Hz streaming job isn't running.
        flushToGlasses()
    }

    private fun formatSimulatedTime(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        Log.i(TAG, "Cleaning up KarooActiveLookBridge...")

        stopStreaming()
        stopContinuousReconnect()
        karooDataService.disconnect()
        activeLookService.cleanup()
        scope.cancel()
        statusLogJob?.cancel()
        statusLogJob = null

        Log.i(TAG, "✓ Bridge cleanup complete")
    }

    private fun startContinuousReconnect() {
        if (reconnectJob?.isActive == true) return

        val address = lastConnectedGlassesAddress
        if (address == null) {
            Log.w(TAG, "Continuous reconnect requested but no lastConnectedGlassesAddress set")
            return
        }

        reconnectStartTime = System.currentTimeMillis()
        Log.i(
            TAG,
            "🔁 Starting continuous reconnect loop (every ${reconnectIntervalMs / 1000}s) for $address"
        )

        reconnectJob = scope.launch {
            while (true) {
                delay(reconnectIntervalMs)

                if (activeLookService.isConnected) {
                    Log.d(TAG, "Reconnect loop: glasses already connected")
                    continue
                }

                Log.i(TAG, "Reconnect loop: scanning to find $address")
                attemptAutoConnectToGlasses(address)
            }
        }
    }

    private fun stopContinuousReconnect() {
        if (reconnectJob?.isActive == true) {
            Log.i(TAG, "⏹ Stopping continuous reconnect loop")
        }
        reconnectJob?.cancel()
        reconnectJob = null
    }

    companion object {
        private const val TAG = "KarooActiveLookBridge"
    }
}

