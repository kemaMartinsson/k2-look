package com.kema.k2look.service

import android.content.Context
import android.graphics.Point
import android.util.Log
import com.activelook.activelooksdk.DiscoveredGlasses
import com.activelook.activelooksdk.types.Rotation
import com.kema.k2look.model.VisualizationType
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
    private val layoutService = ActiveLookLayoutService(activeLookService)
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

    // Currently displayed screen ID (updated by gesture cycling / profile selection)
    private var activeScreenId: Int? = null

    // Use efficient layout system (layoutSave/layoutDisplay vs individual txt commands)
    // Reduces BLE traffic by 80% and improves battery life by 50%
    private var useEfficientLayouts = true // Can be toggled for testing/fallback

    // Auto profile switching on ride start
    private var hasAutoSwitchedProfile = false  // Track if we've auto-switched this ride
    private var lastKarooProfileName: String? = null  // Track last seen Karoo profile

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
        // Speed metrics
        var speed: String = "--",
        var maxSpeed: String = "--",
        var avgSpeed: String = "--",

        // Heart Rate metrics
        var heartRate: String = "--",
        var maxHeartRate: String = "--",
        var avgHeartRate: String = "--",
        var hrZone: String = "--",

        // Cadence metrics
        var cadence: String = "--",
        var maxCadence: String = "--",
        var avgCadence: String = "--",

        // Power metrics
        var power: String = "--",
        var maxPower: String = "--",
        var avgPower: String = "--",
        var power3s: String = "--",

        // General metrics
        var distance: String = "--",
        var time: String = "--",

        // Climbing metrics
        var vam: String = "--",
        var avgVam: String = "--",

        // Radar metrics
        var radarThreatLevel: String = "--",
        var radarTargetCount: String = "--",
        var radarClosestRange: String = "--",

        // General additions
        var clockTime: String = "--",
        var temperature: String = "--",
        var batteryPercent: String = "--",
        var rideTime: String = "--",

        // Heart Rate additions
        var percentMaxHr: String = "--",
        var percentHrr: String = "--",

        // Power additions
        var powerZone: String = "--",
        var power5s: String = "--",
        var power10s: String = "--",
        var power30s: String = "--",
        var normalizedPower: String = "--",
        var percentFtp: String = "--",
        var intensityFactor: String = "--",
        var tss: String = "--",
        var wPerKg: String = "--",

        // Energy
        var energyOutput: String = "--",
        var calories: String = "--",
        var caloriesPerHour: String = "--",

        // Speed / Cadence additions
        var speed3s: String = "--",
        var cadence3s: String = "--",

        // Elevation
        var elevationGrade: String = "--",
        var elevationGain: String = "--",
        var elevationLoss: String = "--",
        var altitude: String = "--",
        var vam30s: String = "--",

        // Lap
        var lapNumber: String = "--",
        var lapTime: String = "--",
        var lapDistance: String = "--",
        var lapSpeed: String = "--",
        var lapHr: String = "--",
        var lapPower: String = "--",
        var lapNp: String = "--",
        var lapCadence: String = "--",
        var lapAscent: String = "--",

        // Last Lap
        var lastLapTime: String = "--",
        var lastLapDistance: String = "--",
        var lastLapSpeed: String = "--",
        var lastLapHr: String = "--",
        var lastLapPower: String = "--",
        var lastLapNp: String = "--",

        // Shifting
        var shiftingFrontGear: String = "--",
        var shiftingRearGear: String = "--",
        var shiftingBattery: String = "--",
        var shiftingCount: String = "--",

        // Navigation
        var distanceToTurn: String = "--",
        var distanceToDest: String = "--",
        var timeOfArrival: String = "--",
        var timeToDest: String = "--",
        var heading: String = "--",

        // eBike
        var levBattery: String = "--",
        var levRange: String = "--",
        var levAssistMode: String = "--",
        var levMotorPower: String = "--",

        // State
        var rideState: RideState = RideState.Idle,
        var isDirty: Boolean = false // Track if data has changed since last flush
    )

    /**
     * Set the active DataField profile for display layout
     */
    fun setActiveProfile(profile: com.kema.k2look.model.DataFieldProfile) {
        activeProfile = profile
        // Reset to first screen when profile changes
        activeScreenId = profile.screens.firstOrNull()?.id
        Log.i(TAG, "📋 Active profile set: ${profile.name} (${profile.screens.size} screens), active screen: $activeScreenId")

        // Save layouts/gauges then flush to glasses — always when connected.
        // When streaming this shows live values; when idle it shows "--" placeholders
        // so the user gets immediate visual confirmation of their layout (Build & Send).
        if (activeLookService.isConnected) {
            scope.launch {
                // Save text layouts (if using efficient mode)
                if (useEfficientLayouts) {
                    val layoutsSuccess = layoutService.saveProfileLayouts(profile)
                    if (layoutsSuccess) {
                        Log.i(TAG, "✅ Efficient layouts saved to glasses memory")
                    } else {
                        Log.w(TAG, "⚠️ Failed to save layouts, falling back to basic mode")
                        useEfficientLayouts = false
                    }
                }

                // Save gauges (always needed for gauge visualization)
                val gaugesSuccess = initializeGauges(profile)
                if (gaugesSuccess) {
                    Log.i(TAG, "✅ Gauges initialized")
                }

                // Force a display update now that layouts are saved.
                // flushWithProfile falls back to basic mode if efficient layouts aren't
                // ready yet, so the ordering here is always safe.
                currentData.isDirty = true
                flushToGlasses()
                Log.i(TAG, "✅ Display updated after profile apply")
            }
        }
    }

    /**
     * Set the currently displayed screen (called by gesture cycling / LayoutBuilderViewModel)
     */
    fun setActiveScreen(screenId: Int) {
        activeScreenId = screenId
        Log.d(TAG, "Active screen changed to: $screenId")
        // Force flush so the new screen is displayed immediately
        currentData.isDirty = true
    }

    /**
     * Toggle efficient layout mode (for testing/debugging)
     */
    fun setUseEfficientLayouts(enabled: Boolean) {
        useEfficientLayouts = enabled
        Log.i(
            TAG,
            "Efficient layouts: ${if (enabled) "ENABLED (80% less BLE traffic)" else "DISABLED (fallback mode)"}"
        )
    }

    /**
     * Check if efficient layout mode is active
     */
    fun isEfficientLayoutsEnabled(): Boolean = useEfficientLayouts

    /**
     * Callback to find K2Look profile by name
     * Set by LayoutBuilderViewModel to enable auto-switching
     */
    private var profileLookup: ((String) -> com.kema.k2look.model.DataFieldProfile?)? = null

    /**
     * Set the profile lookup callback for auto-switching
     */
    fun setProfileLookup(lookup: (String) -> com.kema.k2look.model.DataFieldProfile?) {
        profileLookup = lookup
        Log.i(TAG, "Profile lookup callback registered for auto-switching")
    }

    /**
     * Attempt to auto-switch profile based on Karoo profile name
     * Only happens once at ride start, not mid-ride
     */
    private fun tryAutoSwitchProfile(karooProfileName: String) {
        // Don't switch if we already auto-switched this ride
        if (hasAutoSwitchedProfile) {
            Log.d(TAG, "Already auto-switched this ride, ignoring Karoo profile change")
            return
        }

        // Look up matching K2Look profile
        val matchingProfile = profileLookup?.invoke(karooProfileName)

        if (matchingProfile != null) {
            Log.i(
                TAG,
                "🎯 Auto-switching to K2Look profile '${matchingProfile.name}' (matches Karoo profile '$karooProfileName')"
            )
            setActiveProfile(matchingProfile)
            hasAutoSwitchedProfile = true
        } else {
            Log.d(TAG, "No matching K2Look profile found for Karoo profile '$karooProfileName'")
        }
    }

    /**
     * Reset auto-switch flag when ride ends (allows auto-switch on next ride)
     */
    private fun resetAutoSwitch() {
        hasAutoSwitchedProfile = false
        lastKarooProfileName = null
        Log.d(TAG, "Auto-switch reset - ready for next ride")
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

                            // Reset auto-switch flag for next ride
                            resetAutoSwitch()
                        }
                    }
                }
            }
        }

        // Observe active ride profile (for auto-switching on ride start)
        scope.launch {
            karooDataService.activeRideProfile.collect { rideProfile ->
                val profileName = rideProfile?.name

                if (profileName != null && profileName != lastKarooProfileName) {
                    Log.i(TAG, "Karoo profile changed: '$lastKarooProfileName' → '$profileName'")

                    // Only auto-switch if this is ride start (not mid-ride change)
                    if (!hasAutoSwitchedProfile && isInActiveRide) {
                        Log.i(
                            TAG,
                            "Ride starting with Karoo profile '$profileName', checking for matching K2Look profile..."
                        )
                        tryAutoSwitchProfile(profileName)
                    } else if (hasAutoSwitchedProfile && isInActiveRide) {
                        Log.i(
                            TAG,
                            "Karoo profile changed mid-ride to '$profileName', keeping current K2Look profile (no auto-switch)"
                        )
                    }

                    lastKarooProfileName = profileName
                } else if (profileName == null && lastKarooProfileName != null) {
                    Log.d(TAG, "Karoo profile cleared")
                    lastKarooProfileName = null
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

        // Observe max speed
        scope.launch {
            karooDataService.maxSpeedData.collect { streamState ->
                currentData.maxSpeed = formatStreamData(streamState, "km/h")
                currentData.isDirty = true
            }
        }

        // Observe average speed
        scope.launch {
            karooDataService.averageSpeedData.collect { streamState ->
                currentData.avgSpeed = formatStreamData(streamState, "km/h")
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

        // Observe max heart rate
        scope.launch {
            karooDataService.maxHeartRateData.collect { streamState ->
                currentData.maxHeartRate = formatStreamData(streamState, "bpm")
                currentData.isDirty = true
            }
        }

        // Observe average heart rate
        scope.launch {
            karooDataService.averageHeartRateData.collect { streamState ->
                currentData.avgHeartRate = formatStreamData(streamState, "bpm")
                currentData.isDirty = true
            }
        }

        // Observe HR zone
        scope.launch {
            karooDataService.hrZoneData.collect { streamState ->
                currentData.hrZone = formatHRZoneData(streamState)
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

        // Observe max cadence
        scope.launch {
            karooDataService.maxCadenceData.collect { streamState ->
                currentData.maxCadence = formatStreamData(streamState, "rpm")
                currentData.isDirty = true
            }
        }

        // Observe average cadence
        scope.launch {
            karooDataService.averageCadenceData.collect { streamState ->
                currentData.avgCadence = formatStreamData(streamState, "rpm")
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

        // Observe max power
        scope.launch {
            karooDataService.maxPowerData.collect { streamState ->
                currentData.maxPower = formatStreamData(streamState, "w")
                currentData.isDirty = true
            }
        }

        // Observe average power
        scope.launch {
            karooDataService.averagePowerData.collect { streamState ->
                currentData.avgPower = formatStreamData(streamState, "w")
                currentData.isDirty = true
            }
        }

        // Observe smoothed 3s power
        scope.launch {
            karooDataService.smoothed3sPowerData.collect { streamState ->
                currentData.power3s = formatStreamData(streamState, "w")
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

        // Observe VAM (Vertical Ascent Meters)
        scope.launch {
            karooDataService.vamData.collect { streamState ->
                currentData.vam = formatStreamData(streamState, "m/h")
                currentData.isDirty = true
            }
        }

        // Observe average VAM
        scope.launch {
            karooDataService.avgVamData.collect { streamState ->
                currentData.avgVam = formatStreamData(streamState, "m/h")
                currentData.isDirty = true
            }
        }

        // Observe radar — one stream carries threat level + up to 8 target ranges
        scope.launch {
            karooDataService.radarData.collect { streamState ->
                when (streamState) {
                    is StreamState.Streaming -> {
                        val values = streamState.dataPoint.values

                        // Threat level (required field — 0 = clear, higher = more threat)
                        val threat = values[io.hammerhead.karooext.models.DataType.Field.RADAR_THREAT_LEVEL]?.toInt() ?: 0
                        currentData.radarThreatLevel = threat.toString()

                        // Count detected targets (non-zero ranges)
                        val ranges = listOfNotNull(
                            values[io.hammerhead.karooext.models.DataType.Field.RADAR_TARGET_1_RANGE],
                            values[io.hammerhead.karooext.models.DataType.Field.RADAR_TARGET_2_RANGE],
                            values[io.hammerhead.karooext.models.DataType.Field.RADAR_TARGET_3_RANGE],
                            values[io.hammerhead.karooext.models.DataType.Field.RADAR_TARGET_4_RANGE],
                            values[io.hammerhead.karooext.models.DataType.Field.RADAR_TARGET_5_RANGE],
                            values[io.hammerhead.karooext.models.DataType.Field.RADAR_TARGET_6_RANGE],
                            values[io.hammerhead.karooext.models.DataType.Field.RADAR_TARGET_7_RANGE],
                            values[io.hammerhead.karooext.models.DataType.Field.RADAR_TARGET_8_RANGE]
                        ).filter { it > 0.0 }
                        currentData.radarTargetCount = ranges.size.toString()

                        // Distance to closest target
                        val closest = ranges.minOrNull()
                        currentData.radarClosestRange = if (closest != null)
                            "${formatValue(closest)} m" else "--"

                        Log.d(TAG, "Radar: threat=$threat, targets=${ranges.size}, closest=${currentData.radarClosestRange}")
                    }
                    is StreamState.Searching -> {
                        currentData.radarThreatLevel = "..."
                        currentData.radarTargetCount = "..."
                        currentData.radarClosestRange = "..."
                    }
                    else -> {
                        currentData.radarThreatLevel = "--"
                        currentData.radarTargetCount = "--"
                        currentData.radarClosestRange = "--"
                    }
                }
                currentData.isDirty = true
            }
        }

        // ── General additions ──────────────────────────────────────────────────
        scope.launch { karooDataService.clockTimeData.collect      { currentData.clockTime       = formatClockTime(it);            currentData.isDirty = true } }
        scope.launch { karooDataService.temperatureData.collect    { currentData.temperature     = formatStreamData(it, "°C");     currentData.isDirty = true } }
        scope.launch { karooDataService.batteryPercentData.collect { currentData.batteryPercent  = formatPercent(it);              currentData.isDirty = true } }
        scope.launch { karooDataService.rideTimeData.collect       { currentData.rideTime        = formatTimeData(it);             currentData.isDirty = true } }

        // ── Heart Rate additions ───────────────────────────────────────────────
        scope.launch { karooDataService.percentMaxHrData.collect   { currentData.percentMaxHr    = formatPercent(it);              currentData.isDirty = true } }
        scope.launch { karooDataService.percentHrrData.collect     { currentData.percentHrr      = formatPercent(it);              currentData.isDirty = true } }

        // ── Power additions ────────────────────────────────────────────────────
        scope.launch { karooDataService.powerZoneData.collect      { currentData.powerZone       = formatZoneData(it, 7);          currentData.isDirty = true } }
        scope.launch { karooDataService.smoothed5sPowerData.collect{ currentData.power5s         = formatStreamData(it, "w");      currentData.isDirty = true } }
        scope.launch { karooDataService.smoothed10sPowerData.collect{currentData.power10s        = formatStreamData(it, "w");      currentData.isDirty = true } }
        scope.launch { karooDataService.smoothed30sPowerData.collect{currentData.power30s        = formatStreamData(it, "w");      currentData.isDirty = true } }
        scope.launch { karooDataService.normalizedPowerData.collect{ currentData.normalizedPower = formatStreamData(it, "w");      currentData.isDirty = true } }
        scope.launch { karooDataService.percentFtpData.collect     { currentData.percentFtp      = formatPercent(it);              currentData.isDirty = true } }
        scope.launch { karooDataService.intensityFactorData.collect{ currentData.intensityFactor = formatStreamData(it, "");       currentData.isDirty = true } }
        scope.launch { karooDataService.trainingStressScoreData.collect{ currentData.tss         = formatStreamData(it, "");       currentData.isDirty = true } }
        scope.launch { karooDataService.powerToWeightData.collect  { currentData.wPerKg          = formatStreamData(it, "w/kg");   currentData.isDirty = true } }

        // ── Energy ────────────────────────────────────────────────────────────
        scope.launch { karooDataService.energyOutputData.collect   { currentData.energyOutput    = formatStreamData(it, "kJ");     currentData.isDirty = true } }
        scope.launch { karooDataService.caloriesData.collect       { currentData.calories        = formatStreamData(it, "kcal");   currentData.isDirty = true } }
        scope.launch { karooDataService.caloriesPerHourData.collect{ currentData.caloriesPerHour = formatStreamData(it, "kcal/h"); currentData.isDirty = true } }

        // ── Speed / Cadence additions ──────────────────────────────────────────
        scope.launch { karooDataService.smoothed3sSpeedData.collect  { currentData.speed3s    = formatStreamData(it, "km/h"); currentData.isDirty = true } }
        scope.launch { karooDataService.smoothed3sCadenceData.collect{ currentData.cadence3s  = formatStreamData(it, "rpm");  currentData.isDirty = true } }

        // ── Elevation ─────────────────────────────────────────────────────────
        scope.launch { karooDataService.elevationGradeData.collect { currentData.elevationGrade = formatGrade(it);                 currentData.isDirty = true } }
        scope.launch { karooDataService.elevationGainData.collect  { currentData.elevationGain  = formatStreamData(it, "m");       currentData.isDirty = true } }
        scope.launch { karooDataService.elevationLossData.collect  { currentData.elevationLoss  = formatStreamData(it, "m");       currentData.isDirty = true } }
        scope.launch { karooDataService.altitudeData.collect       { currentData.altitude        = formatStreamData(it, "m");       currentData.isDirty = true } }
        scope.launch { karooDataService.vam30sData.collect         { currentData.vam30s          = formatStreamData(it, "m/h");     currentData.isDirty = true } }

        // ── Lap ───────────────────────────────────────────────────────────────
        scope.launch { karooDataService.lapNumberData.collect   { currentData.lapNumber   = formatInteger(it);          currentData.isDirty = true } }
        scope.launch { karooDataService.lapTimeData.collect     { currentData.lapTime     = formatLapTime(it);          currentData.isDirty = true } }
        scope.launch { karooDataService.lapDistanceData.collect { currentData.lapDistance = formatStreamData(it, "km"); currentData.isDirty = true } }
        scope.launch { karooDataService.lapSpeedData.collect    { currentData.lapSpeed    = formatStreamData(it, "km/h"); currentData.isDirty = true } }
        scope.launch { karooDataService.lapHrData.collect       { currentData.lapHr       = formatStreamData(it, "bpm"); currentData.isDirty = true } }
        scope.launch { karooDataService.lapPowerData.collect    { currentData.lapPower    = formatStreamData(it, "w");  currentData.isDirty = true } }
        scope.launch { karooDataService.lapNpData.collect       { currentData.lapNp       = formatStreamData(it, "w");  currentData.isDirty = true } }
        scope.launch { karooDataService.lapCadenceData.collect  { currentData.lapCadence  = formatStreamData(it, "rpm"); currentData.isDirty = true } }
        scope.launch { karooDataService.lapAscentData.collect   { currentData.lapAscent   = formatStreamData(it, "m");  currentData.isDirty = true } }

        // ── Last Lap ──────────────────────────────────────────────────────────
        scope.launch { karooDataService.lastLapTimeData.collect     { currentData.lastLapTime     = formatLapTime(it);           currentData.isDirty = true } }
        scope.launch { karooDataService.lastLapDistanceData.collect { currentData.lastLapDistance = formatStreamData(it, "km");  currentData.isDirty = true } }
        scope.launch { karooDataService.lastLapSpeedData.collect    { currentData.lastLapSpeed    = formatStreamData(it, "km/h"); currentData.isDirty = true } }
        scope.launch { karooDataService.lastLapHrData.collect       { currentData.lastLapHr       = formatStreamData(it, "bpm"); currentData.isDirty = true } }
        scope.launch { karooDataService.lastLapPowerData.collect    { currentData.lastLapPower    = formatStreamData(it, "w");   currentData.isDirty = true } }
        scope.launch { karooDataService.lastLapNpData.collect       { currentData.lastLapNp       = formatStreamData(it, "w");   currentData.isDirty = true } }

        // ── Shifting ──────────────────────────────────────────────────────────
        scope.launch {
            karooDataService.shiftingFrontGearData.collect { streamState ->
                currentData.shiftingFrontGear = formatGear(
                    streamState,
                    io.hammerhead.karooext.models.DataType.Field.SHIFTING_FRONT_GEAR,
                    io.hammerhead.karooext.models.DataType.Field.SHIFTING_FRONT_GEAR_MAX
                )
                currentData.isDirty = true
            }
        }
        scope.launch {
            karooDataService.shiftingRearGearData.collect { streamState ->
                currentData.shiftingRearGear = formatGear(
                    streamState,
                    io.hammerhead.karooext.models.DataType.Field.SHIFTING_REAR_GEAR,
                    io.hammerhead.karooext.models.DataType.Field.SHIFTING_REAR_GEAR_MAX
                )
                currentData.isDirty = true
            }
        }
        scope.launch {
            karooDataService.shiftingBatteryData.collect { streamState ->
                currentData.shiftingBattery = formatShiftingBattery(streamState)
                currentData.isDirty = true
            }
        }
        scope.launch { karooDataService.shiftingCountData.collect { currentData.shiftingCount = formatInteger(it); currentData.isDirty = true } }

        // ── Navigation ────────────────────────────────────────────────────────
        scope.launch { karooDataService.distanceToTurnData.collect { currentData.distanceToTurn = formatDistanceToTurn(it); currentData.isDirty = true } }
        scope.launch { karooDataService.distanceToDestData.collect { currentData.distanceToDest = formatStreamData(it, "km"); currentData.isDirty = true } }
        scope.launch { karooDataService.timeOfArrivalData.collect  { currentData.timeOfArrival  = formatClockTime(it);       currentData.isDirty = true } }
        scope.launch { karooDataService.timeToDestData.collect     { currentData.timeToDest      = formatDuration(it);        currentData.isDirty = true } }
        scope.launch { karooDataService.headingData.collect        { currentData.heading          = formatHeading(it);         currentData.isDirty = true } }

        // ── eBike ─────────────────────────────────────────────────────────────
        scope.launch { karooDataService.levBatteryData.collect    { currentData.levBattery    = formatPercent(it);         currentData.isDirty = true } }
        scope.launch { karooDataService.levRangeData.collect      { currentData.levRange      = formatStreamData(it, "km"); currentData.isDirty = true } }
        scope.launch { karooDataService.levAssistModeData.collect { currentData.levAssistMode = formatInteger(it);          currentData.isDirty = true } }
        scope.launch { karooDataService.levMotorPowerData.collect { currentData.levMotorPower = formatStreamData(it, "w");  currentData.isDirty = true } }
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
        // Use the currently selected screen index (driven by gesture cycling)
        val selectedScreenId = activeScreenId
        val screen = if (selectedScreenId != null)
            profile.screens.find { it.id == selectedScreenId } ?: profile.screens.first()
        else
            profile.screens.first()

        Log.v(
            TAG,
            "Flushing with profile: ${profile.name}, screen: ${screen.id}, fields: ${screen.dataFields.size}, efficient: $useEfficientLayouts"
        )

        if (useEfficientLayouts && layoutService.isProfileSaved(profile.id)) {
            // Efficient mode: Use layoutDisplay (3 commands, 80% less traffic!)
            flushWithEfficientMode(screen)
        } else {
            // Basic mode: Use displayField (12 commands, works but less efficient)
            flushWithBasicMode(screen)
        }
    }

    /**
     * Efficient mode: Updates using layoutDisplay for text, direct updates for gauges/bars
     * Only sends values - layouts already saved in glasses
     */
    private fun flushWithEfficientMode(screen: com.kema.k2look.model.LayoutScreen) {
        screen.dataFields.forEach { field ->
            val value = getMetricValue(field.dataField)

            // Use appropriate update method based on visualization type
            updateVisualization(field, value)
        }
    }

    /**
     * Basic mode: Full rendering based on visualization type
     * Sends all positioning/icons/labels every update (for text)
     * Or renders gauges/bars directly
     */
    private fun flushWithBasicMode(screen: com.kema.k2look.model.LayoutScreen) {
        val template = screen.getTemplate()

        screen.dataFields.forEach { field ->
            val value = getMetricValue(field.dataField)

            when (field.visualizationType ?: VisualizationType.TEXT) {
                com.kema.k2look.model.VisualizationType.TEXT -> {
                    // Get zone for text display
                    val zone = template.zones.find { it.id == field.zoneId }
                    if (zone == null) {
                        Log.w(TAG, "Zone ${field.zoneId} not found in template ${template.id}")
                        return@forEach
                    }
                    activeLookService.displayField(field, value, zone.y)
                }

                else -> {
                    // Gauges and bars use direct updates
                    updateVisualization(field, value)
                }
            }
        }
    }

    /**
     * Get the current value for a specific metric
     * Note: Karoo already provides data in user's preferred units (metric/imperial)
     */
    private fun getMetricValue(dataField: com.kema.k2look.model.DataField): String {
        return when (dataField.id) {
            // General metrics
            1 -> currentData.time                                    // Elapsed Time
            2 -> currentData.distance                                // Distance

            // Heart Rate metrics
            4 -> currentData.heartRate                               // Heart Rate (current)
            5 -> currentData.maxHeartRate                            // Max Heart Rate
            6 -> currentData.avgHeartRate                            // Avg Heart Rate
            47 -> currentData.hrZone                                 // HR Zone (Z1-Z5)

            // Power metrics
            7 -> currentData.power                                   // Power (current)
            8 -> currentData.maxPower                                // Max Power
            9 -> currentData.avgPower                                // Avg Power
            10 -> currentData.power3s                                // Power 3s smoothed

            // Speed metrics
            12 -> currentData.speed                                  // Speed (current)
            13 -> currentData.maxSpeed                               // Max Speed
            14 -> currentData.avgSpeed                               // Avg Speed

            // Cadence metrics
            18 -> currentData.cadence                                // Cadence (current)
            19 -> currentData.maxCadence                             // Max Cadence
            20 -> currentData.avgCadence                             // Avg Cadence

            // Climbing metrics
            24 -> currentData.vam                                    // VAM (vertical ascent meters)
            25 -> currentData.avgVam                                 // Avg VAM

            // Radar metrics
            50 -> currentData.radarThreatLevel
            51 -> currentData.radarTargetCount
            52 -> currentData.radarClosestRange

            // General additions
            53 -> currentData.clockTime
            54 -> currentData.temperature
            55 -> currentData.batteryPercent
            56 -> currentData.rideTime

            // Heart Rate additions
            57 -> currentData.percentMaxHr
            58 -> currentData.percentHrr

            // Power additions
            48 -> currentData.powerZone
            59 -> currentData.power5s
            60 -> currentData.power10s
            61 -> currentData.power30s
            62 -> currentData.normalizedPower
            63 -> currentData.percentFtp
            64 -> currentData.intensityFactor
            65 -> currentData.tss
            66 -> currentData.wPerKg

            // Energy
            67 -> currentData.energyOutput
            68 -> currentData.calories
            69 -> currentData.caloriesPerHour

            // Speed / Cadence additions
            70 -> currentData.speed3s
            71 -> currentData.cadence3s

            // Elevation
            72 -> currentData.elevationGrade
            73 -> currentData.elevationGain
            74 -> currentData.elevationLoss
            75 -> currentData.altitude
            76 -> currentData.vam30s

            // Lap
            77 -> currentData.lapNumber
            78 -> currentData.lapTime
            79 -> currentData.lapDistance
            80 -> currentData.lapSpeed
            81 -> currentData.lapHr
            82 -> currentData.lapPower
            83 -> currentData.lapNp
            84 -> currentData.lapCadence
            85 -> currentData.lapAscent

            // Last Lap
            86 -> currentData.lastLapTime
            87 -> currentData.lastLapDistance
            88 -> currentData.lastLapSpeed
            89 -> currentData.lastLapHr
            90 -> currentData.lastLapPower
            91 -> currentData.lastLapNp

            // Shifting
            92 -> currentData.shiftingFrontGear
            93 -> currentData.shiftingRearGear
            94 -> currentData.shiftingBattery
            95 -> currentData.shiftingCount

            // Navigation
            96 -> currentData.distanceToTurn
            97 -> currentData.distanceToDest
            98 -> currentData.timeOfArrival
            99 -> currentData.timeToDest
            100 -> currentData.heading

            // eBike
            101 -> currentData.levBattery
            102 -> currentData.levRange
            103 -> currentData.levAssistMode
            104 -> currentData.levMotorPower

            else -> {
                Log.w(
                    TAG,
                    "Unknown metric ID: ${dataField.id} (${dataField.name})"
                )
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
     * Format HR zone data (Z1-Z5) — delegates to generic formatZoneData
     */
    private fun formatHRZoneData(streamState: StreamState?): String = formatZoneData(streamState, 5)

    /**
     * Format zone data generically (Z1..Zmax)
     */
    private fun formatZoneData(streamState: StreamState?, max: Int): String {
        return when (streamState) {
            is StreamState.Streaming -> {
                val zone = streamState.dataPoint.singleValue?.toInt()
                if (zone != null && zone in 1..max) "Z$zone" else "--"
            }
            is StreamState.Searching -> "..."
            is StreamState.Idle -> "--"
            is StreamState.NotAvailable -> "N/A"
            null -> "--"
        }
    }

    /** Format a percentage value (0–100) as "72%" */
    private fun formatPercent(streamState: StreamState?): String {
        return when (streamState) {
            is StreamState.Streaming -> {
                val v = streamState.dataPoint.singleValue
                if (v != null) "%.0f%%".format(v) else "--%"
            }
            is StreamState.Searching -> "...%"
            is StreamState.Idle, null -> "--%"
            is StreamState.NotAvailable -> "N/A"
        }
    }

    /** Format elevation grade with sign: "+5.2%" / "-3.1%" */
    private fun formatGrade(streamState: StreamState?): String {
        return when (streamState) {
            is StreamState.Streaming -> {
                val v = streamState.dataPoint.singleValue
                if (v != null) {
                    val sign = if (v > 0) "+" else ""
                    "$sign${"%.1f".format(v)}%"
                } else "--%"
            }
            is StreamState.Searching -> "...%"
            is StreamState.Idle, null -> "--%"
            is StreamState.NotAvailable -> "N/A"
        }
    }

    /** Format an integer metric (lap #, shift count, assist mode) */
    private fun formatInteger(streamState: StreamState?): String {
        return when (streamState) {
            is StreamState.Streaming -> {
                val v = streamState.dataPoint.singleValue?.toInt()
                v?.toString() ?: "--"
            }
            is StreamState.Searching -> "..."
            is StreamState.Idle, null -> "--"
            is StreamState.NotAvailable -> "N/A"
        }
    }

    /** Format lap time (ms) as "MM:SS" */
    private fun formatLapTime(streamState: StreamState?): String {
        return when (streamState) {
            is StreamState.Streaming -> {
                val ms = streamState.dataPoint.singleValue?.toLong()
                if (ms != null) {
                    val s = (ms / 1000) % 60
                    val m = (ms / (1000 * 60)) % 60
                    val h = ms / (1000 * 60 * 60)
                    if (h > 0)
                        String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, s)
                    else
                        String.format(java.util.Locale.US, "%02d:%02d", m, s)
                } else "--:--"
            }
            is StreamState.Searching -> "--:--"
            is StreamState.Idle, null -> "--:--"
            is StreamState.NotAvailable -> "N/A"
        }
    }

    /** Format a clock/arrival time (ms epoch or ms-of-day) as "HH:MM" */
    private fun formatClockTime(streamState: StreamState?): String {
        return when (streamState) {
            is StreamState.Streaming -> {
                val ms = streamState.dataPoint.singleValue?.toLong()
                if (ms != null) {
                    val totalMin = ms / 60000
                    val h = (totalMin / 60) % 24
                    val m = totalMin % 60
                    String.format(java.util.Locale.US, "%02d:%02d", h, m)
                } else "--:--"
            }
            is StreamState.Searching -> "--:--"
            is StreamState.Idle, null -> "--:--"
            is StreamState.NotAvailable -> "N/A"
        }
    }

    /** Format a duration (seconds) as "H:MM" or "MM:SS" */
    private fun formatDuration(streamState: StreamState?): String {
        return when (streamState) {
            is StreamState.Streaming -> {
                val sec = streamState.dataPoint.singleValue?.toLong()
                if (sec != null) {
                    val h = sec / 3600
                    val m = (sec % 3600) / 60
                    if (h > 0)
                        String.format(java.util.Locale.US, "%d:%02d", h, m)
                    else
                        String.format(java.util.Locale.US, "%d min", m)
                } else "--"
            }
            is StreamState.Searching -> "..."
            is StreamState.Idle, null -> "--"
            is StreamState.NotAvailable -> "N/A"
        }
    }

    /** Format distance-to-next-turn: show metres when < 1 km, else km */
    private fun formatDistanceToTurn(streamState: StreamState?): String {
        return when (streamState) {
            is StreamState.Streaming -> {
                val m = streamState.dataPoint.singleValue
                if (m != null) {
                    if (m < 1000) "%.0f m".format(m) else "%.1f km".format(m / 1000.0)
                } else "-- m"
            }
            is StreamState.Searching -> "..."
            is StreamState.Idle, null -> "-- m"
            is StreamState.NotAvailable -> "N/A"
        }
    }

    /** Format gear as "current/max" (e.g. "3/11") using multi-field DataPoint */
    private fun formatGear(streamState: StreamState?, gearField: String, maxField: String): String {
        return when (streamState) {
            is StreamState.Streaming -> {
                val values = streamState.dataPoint.values
                val gear = values[gearField]?.toInt()
                val max  = values[maxField]?.toInt()
                when {
                    gear == null -> "--"
                    max  != null -> "$gear/$max"
                    else         -> "$gear"
                }
            }
            is StreamState.Searching -> "..."
            is StreamState.Idle, null -> "--"
            is StreamState.NotAvailable -> "N/A"
        }
    }

    /** Format drivetrain battery status (ordinal of BatteryStatus enum) */
    private fun formatShiftingBattery(streamState: StreamState?): String {
        // BatteryStatus ordinals: 0=NEW, 1=GOOD, 2=OK, 3=LOW, 4=CRITICAL, 5=INVALID
        val labels = arrayOf("New", "Good", "OK", "Low", "Critical", "?")
        return when (streamState) {
            is StreamState.Streaming -> {
                val values = streamState.dataPoint.values
                // Prefer rear derailleur; fall back to overall status
                val raw = (values[io.hammerhead.karooext.models.DataType.Field.SHIFTING_BATTERY_STATUS_REAR_DERAILLEUR]
                    ?: values[io.hammerhead.karooext.models.DataType.Field.SHIFTING_BATTERY_STATUS])
                    ?.toInt()
                labels.getOrElse(raw ?: 5) { "?" }
            }
            is StreamState.Searching -> "..."
            is StreamState.Idle, null -> "--"
            is StreamState.NotAvailable -> "N/A"
        }
    }

    /** Format compass heading index (0=N, 1=NE … 7=NW) */
    private fun formatHeading(streamState: StreamState?): String {
        val dirs = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        return when (streamState) {
            is StreamState.Streaming -> {
                val idx = streamState.dataPoint.singleValue?.toInt()
                dirs.getOrElse(idx ?: 8) { "--" }
            }
            is StreamState.Searching -> "..."
            is StreamState.Idle, null -> "--"
            is StreamState.NotAvailable -> "N/A"
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
     * Get ActiveLookLayoutService for Phase 4.2 layout management
     */
    fun getLayoutService(): ActiveLookLayoutService = layoutService

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
        return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", h, m, s)
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

    // ========== GAUGE & BAR VISUALIZATION ==========

    /**
     * Initialize gauges for all fields in the profile that use gauge visualization
     */
    private suspend fun initializeGauges(profile: com.kema.k2look.model.DataFieldProfile): Boolean {
        var allSuccess = true

        profile.screens.forEach { screen ->
            screen.dataFields.forEach { field ->
                when (field.visualizationType ?: VisualizationType.TEXT) {
                    com.kema.k2look.model.VisualizationType.GAUGE -> {
                        field.gauge?.let { gauge ->
                            val success = activeLookService.saveGauge(gauge)
                            if (!success) {
                                Log.w(TAG, "Failed to save gauge for ${field.dataField.name}")
                                allSuccess = false
                            }
                        }
                    }

                    else -> {
                        // TEXT, BAR, ZONED_BAR don't need pre-initialization
                    }
                }
            }
        }

        return allSuccess
    }

    /**
     * Update visualization (gauge or bar) with current metric value
     */
    private fun updateVisualization(field: com.kema.k2look.model.LayoutDataField, value: String) {
        when (field.visualizationType ?: VisualizationType.TEXT) {
            com.kema.k2look.model.VisualizationType.GAUGE -> {
                updateGauge(field, value)
            }

            com.kema.k2look.model.VisualizationType.BAR -> {
                updateProgressBar(field, value)
            }

            com.kema.k2look.model.VisualizationType.ZONED_BAR -> {
                updateZonedBar(field, value)
            }

            com.kema.k2look.model.VisualizationType.TEXT -> {
                // Text handled by existing displayFieldValue
                layoutService.displayFieldValue(field.zoneId, value)
            }
        }
    }

    /**
     * Update gauge with current metric value
     */
    private fun updateGauge(field: com.kema.k2look.model.LayoutDataField, value: String) {
        val gauge = field.gauge ?: return

        // Parse numeric value from string (remove units, etc.)
        val numericValue = parseNumericValue(value)
        if (numericValue == null) {
            Log.d(TAG, "Cannot update gauge: non-numeric value '$value'")
            return
        }

        // Calculate percentage
        val percentage = gauge.calculatePercentage(numericValue)

        // Update gauge on glasses
        scope.launch {
            activeLookService.displayGauge(gauge.id, percentage)
        }

        Log.d(TAG, "Gauge ${gauge.id}: ${field.dataField.name} = $numericValue ($percentage%)")
    }

    /**
     * Update progress bar with current metric value
     */
    private fun updateProgressBar(field: com.kema.k2look.model.LayoutDataField, value: String) {
        val bar = field.progressBar ?: return

        // Parse numeric value
        val numericValue = parseNumericValue(value)
        if (numericValue == null) {
            Log.d(TAG, "Cannot update bar: non-numeric value '$value'")
            return
        }

        // Calculate percentage
        val percentage = bar.calculatePercentage(numericValue)

        // Update bar on glasses
        scope.launch {
            activeLookService.displayProgressBar(bar, percentage)
        }

        Log.d(TAG, "Bar ${bar.id}: ${field.dataField.name} = $numericValue ($percentage%)")
    }

    /**
     * Update zoned progress bar with current metric value
     */
    private fun updateZonedBar(field: com.kema.k2look.model.LayoutDataField, value: String) {
        val zonedBar = field.zonedBar ?: return

        // Parse numeric value
        val numericValue = parseNumericValue(value)
        if (numericValue == null) {
            Log.d(TAG, "Cannot update zoned bar: non-numeric value '$value'")
            return
        }

        // Update zoned bar on glasses (uses actual value, not percentage)
        scope.launch {
            activeLookService.displayZonedBar(zonedBar, numericValue)
        }

        val currentZone = zonedBar.findZone(numericValue)
        Log.d(
            TAG,
            "Zoned bar ${zonedBar.bar.id}: ${field.dataField.name} = $numericValue (${currentZone?.name ?: "?"})"
        )
    }

    /**
     * Parse numeric value from display string (removes units, handles special cases)
     */
    private fun parseNumericValue(value: String): Float? {
        return when {
            value == "--" || value == "..." || value == "N/A" -> null
            else -> {
                // Remove common units and parse
                val cleaned = value.replace(Regex("[^0-9.-]"), "")
                cleaned.toFloatOrNull()
            }
        }
    }

    companion object {
        private const val TAG = "KarooActiveLookBridge"
    }
}

