package com.kema.k2look.service

import android.content.Context
import android.graphics.Point
import android.util.Log
import com.activelook.activelooksdk.DiscoveredGlasses
import com.activelook.activelooksdk.types.Rotation
import com.kema.k2look.model.VisualizationType
import com.kema.k2look.util.PreferencesManager
import io.hammerhead.karooext.models.DataType
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

    // Accumulated data for hold/flush pattern (see BridgeCurrentData.kt)
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
     * Observe Karoo data streams and update [currentData].
     * Delegated to focused sub-functions by metric category.
     */
    private fun observeKarooData() {
        observeSystemState()          // connection, ride state, profile auto-switch
        observeCoreMetrics()          // speed, HR, cadence, power, distance, time, VAM
        observeRadar()                // multi-field radar stream
        observeGeneralMetrics()
        observeHeartRateMetrics()
        observePowerMetrics()
        observeEnergyMetrics()
        observeSpeedCadenceMetrics()
        observeElevationMetrics()
        observeLapMetrics()
        observeLastLapMetrics()
        observeShiftingMetrics()
        observeNavigationMetrics()
        observeEBikeMetrics()
    }

    /**
     * Convenience: collect [flow] in a new coroutine, apply [update] to [currentData],
     * and mark the frame dirty. Eliminates 3-line boilerplate per metric.
     */
    private fun observe(
        flow: kotlinx.coroutines.flow.Flow<StreamState?>,
        update: CurrentData.(StreamState?) -> Unit
    ) {
        scope.launch {
            flow.collect { state ->
                currentData.update(state)
                currentData.isDirty = true
            }
        }
    }

    // ── System state: connection, ride lifecycle, auto-profile switch ──────
    private fun observeSystemState() {
        scope.launch {
            karooDataService.connectionState.collect { state ->
                Log.d(TAG, "Karoo connection state: $state")
                when (state) {
                    is KarooDataService.ConnectionState.Connected -> {
                        _bridgeState.value = BridgeState.KarooConnected
                        updateBridgeState()
                    }
                    is KarooDataService.ConnectionState.Error ->
                        _bridgeState.value = BridgeState.Error("Karoo: ${state.message}")
                    else -> updateBridgeState()
                }
            }
        }
        scope.launch {
            karooDataService.rideState.collect { state ->
                currentData.rideState = state
                currentData.isDirty = true
                val wasInActiveRide = isInActiveRide
                isInActiveRide = state is RideState.Recording
                when (state) {
                    is RideState.Recording -> {
                        if (_bridgeState.value == BridgeState.FullyConnected) startStreaming()
                        if (!wasInActiveRide) {
                            Log.i(TAG, "Entered active ride - starting continuous reconnect monitoring")
                            startContinuousReconnect()
                        }
                    }
                    else -> if (wasInActiveRide) {
                        Log.i(TAG, "Exited active ride - stopping continuous reconnect monitoring")
                        stopContinuousReconnect()
                        resetAutoSwitch()
                    }
                }
            }
        }
        scope.launch {
            karooDataService.activeRideProfile.collect { rideProfile ->
                val profileName = rideProfile?.name
                if (profileName != null && profileName != lastKarooProfileName) {
                    Log.i(TAG, "Karoo profile changed: '$lastKarooProfileName' → '$profileName'")
                    if (!hasAutoSwitchedProfile && isInActiveRide) {
                        Log.i(TAG, "Ride starting with Karoo profile '$profileName', checking for matching K2Look profile...")
                        tryAutoSwitchProfile(profileName)
                    } else if (hasAutoSwitchedProfile && isInActiveRide) {
                        Log.i(TAG, "Karoo profile changed mid-ride to '$profileName', keeping current K2Look profile (no auto-switch)")
                    }
                    lastKarooProfileName = profileName
                } else if (profileName == null && lastKarooProfileName != null) {
                    Log.d(TAG, "Karoo profile cleared")
                    lastKarooProfileName = null
                }
            }
        }
    }

    // ── Core cycling metrics ───────────────────────────────────────────────
    private fun observeCoreMetrics() {
        observe(karooDataService.speedData)              { speed         = formatStreamData(it, "km/h") }
        observe(karooDataService.maxSpeedData)           { maxSpeed      = formatStreamData(it, "km/h") }
        observe(karooDataService.averageSpeedData)       { avgSpeed      = formatStreamData(it, "km/h") }
        observe(karooDataService.heartRateData)          { heartRate     = formatStreamData(it, "bpm")  }
        observe(karooDataService.maxHeartRateData)       { maxHeartRate  = formatStreamData(it, "bpm")  }
        observe(karooDataService.averageHeartRateData)   { avgHeartRate  = formatStreamData(it, "bpm")  }
        observe(karooDataService.hrZoneData)             { hrZone        = formatHRZoneData(it)          }
        observe(karooDataService.cadenceData)            { cadence       = formatStreamData(it, "rpm")  }
        observe(karooDataService.maxCadenceData)         { maxCadence    = formatStreamData(it, "rpm")  }
        observe(karooDataService.averageCadenceData)     { avgCadence    = formatStreamData(it, "rpm")  }
        observe(karooDataService.powerData)              { power         = formatStreamData(it, "w")    }
        observe(karooDataService.maxPowerData)           { maxPower      = formatStreamData(it, "w")    }
        observe(karooDataService.averagePowerData)       { avgPower      = formatStreamData(it, "w")    }
        observe(karooDataService.smoothed3sPowerData)    { power3s       = formatStreamData(it, "w")    }
        observe(karooDataService.distanceData)           { distance      = formatStreamData(it, "km")   }
        observe(karooDataService.timeData)               { time          = formatTimeData(it)            }
        observe(karooDataService.vamData)                { vam           = formatStreamData(it, "m/h")  }
        observe(karooDataService.avgVamData)             { avgVam        = formatStreamData(it, "m/h")  }
    }

    // ── Radar (multi-field DataPoint — handled separately) ─────────────────
    private fun observeRadar() {
        scope.launch {
            karooDataService.radarData.collect { streamState ->
                when (streamState) {
                    is StreamState.Streaming -> {
                        val v = streamState.dataPoint.values
                        val threat = v[DataType.Field.RADAR_THREAT_LEVEL]?.toInt() ?: 0
                        currentData.radarThreatLevel = threat.toString()
                        val ranges = listOfNotNull(
                            v[DataType.Field.RADAR_TARGET_1_RANGE], v[DataType.Field.RADAR_TARGET_2_RANGE],
                            v[DataType.Field.RADAR_TARGET_3_RANGE], v[DataType.Field.RADAR_TARGET_4_RANGE],
                            v[DataType.Field.RADAR_TARGET_5_RANGE], v[DataType.Field.RADAR_TARGET_6_RANGE],
                            v[DataType.Field.RADAR_TARGET_7_RANGE], v[DataType.Field.RADAR_TARGET_8_RANGE]
                        ).filter { it > 0.0 }
                        currentData.radarTargetCount = ranges.size.toString()
                        val closest = ranges.minOrNull()
                        currentData.radarClosestRange = if (closest != null) "${formatValue(closest)} m" else "--"
                        Log.d(TAG, "Radar: threat=$threat, targets=${ranges.size}, closest=${currentData.radarClosestRange}")
                    }
                    is StreamState.Searching -> {
                        currentData.radarThreatLevel = "..."; currentData.radarTargetCount = "..."; currentData.radarClosestRange = "..."
                    }
                    else -> {
                        currentData.radarThreatLevel = "--"; currentData.radarTargetCount = "--"; currentData.radarClosestRange = "--"
                    }
                }
                currentData.isDirty = true
            }
        }
    }

    // ── General additions ──────────────────────────────────────────────────
    private fun observeGeneralMetrics() {
        observe(karooDataService.clockTimeData)       { clockTime      = formatClockTime(it)       }
        observe(karooDataService.temperatureData)     { temperature    = formatStreamData(it, "°C") }
        observe(karooDataService.batteryPercentData)  { batteryPercent = formatPercent(it)          }
        observe(karooDataService.rideTimeData)        { rideTime       = formatTimeData(it)         }
    }

    // ── Heart Rate additions ───────────────────────────────────────────────
    private fun observeHeartRateMetrics() {
        observe(karooDataService.percentMaxHrData) { percentMaxHr = formatPercent(it) }
        observe(karooDataService.percentHrrData)   { percentHrr   = formatPercent(it) }
    }

    // ── Power additions ────────────────────────────────────────────────────
    private fun observePowerMetrics() {
        observe(karooDataService.powerZoneData)           { powerZone       = formatZoneData(it, 7)        }
        observe(karooDataService.smoothed5sPowerData)     { power5s         = formatStreamData(it, "w")    }
        observe(karooDataService.smoothed10sPowerData)    { power10s        = formatStreamData(it, "w")    }
        observe(karooDataService.smoothed30sPowerData)    { power30s        = formatStreamData(it, "w")    }
        observe(karooDataService.normalizedPowerData)     { normalizedPower = formatStreamData(it, "w")    }
        observe(karooDataService.percentFtpData)          { percentFtp      = formatPercent(it)            }
        observe(karooDataService.intensityFactorData)     { intensityFactor = formatStreamData(it, "")     }
        observe(karooDataService.trainingStressScoreData) { tss             = formatStreamData(it, "")     }
        observe(karooDataService.powerToWeightData)       { wPerKg          = formatStreamData(it, "w/kg") }
    }

    // ── Energy ────────────────────────────────────────────────────────────
    private fun observeEnergyMetrics() {
        observe(karooDataService.energyOutputData)    { energyOutput    = formatStreamData(it, "kJ")     }
        observe(karooDataService.caloriesData)        { calories        = formatStreamData(it, "kcal")   }
        observe(karooDataService.caloriesPerHourData) { caloriesPerHour = formatStreamData(it, "kcal/h") }
    }

    // ── Speed and Cadence additions ────────────────────────────────────────
    private fun observeSpeedCadenceMetrics() {
        observe(karooDataService.smoothed3sSpeedData)   { speed3s   = formatStreamData(it, "km/h") }
        observe(karooDataService.smoothed3sCadenceData) { cadence3s = formatStreamData(it, "rpm")  }
    }

    // ── Elevation ─────────────────────────────────────────────────────────
    private fun observeElevationMetrics() {
        observe(karooDataService.elevationGradeData) { elevationGrade = formatGrade(it)             }
        observe(karooDataService.elevationGainData)  { elevationGain  = formatStreamData(it, "m")   }
        observe(karooDataService.elevationLossData)  { elevationLoss  = formatStreamData(it, "m")   }
        observe(karooDataService.altitudeData)       { altitude       = formatStreamData(it, "m")   }
        observe(karooDataService.vam30sData)         { vam30s         = formatStreamData(it, "m/h") }
    }

    // ── Lap ───────────────────────────────────────────────────────────────
    private fun observeLapMetrics() {
        observe(karooDataService.lapNumberData)   { lapNumber   = formatInteger(it)            }
        observe(karooDataService.lapTimeData)     { lapTime     = formatLapTime(it)            }
        observe(karooDataService.lapDistanceData) { lapDistance = formatStreamData(it, "km")  }
        observe(karooDataService.lapSpeedData)    { lapSpeed    = formatStreamData(it, "km/h") }
        observe(karooDataService.lapHrData)       { lapHr       = formatStreamData(it, "bpm") }
        observe(karooDataService.lapPowerData)    { lapPower    = formatStreamData(it, "w")   }
        observe(karooDataService.lapNpData)       { lapNp       = formatStreamData(it, "w")   }
        observe(karooDataService.lapCadenceData)  { lapCadence  = formatStreamData(it, "rpm") }
        observe(karooDataService.lapAscentData)   { lapAscent   = formatStreamData(it, "m")   }
    }

    // ── Last Lap ──────────────────────────────────────────────────────────
    private fun observeLastLapMetrics() {
        observe(karooDataService.lastLapTimeData)     { lastLapTime     = formatLapTime(it)            }
        observe(karooDataService.lastLapDistanceData) { lastLapDistance = formatStreamData(it, "km")   }
        observe(karooDataService.lastLapSpeedData)    { lastLapSpeed    = formatStreamData(it, "km/h") }
        observe(karooDataService.lastLapHrData)       { lastLapHr       = formatStreamData(it, "bpm")  }
        observe(karooDataService.lastLapPowerData)    { lastLapPower    = formatStreamData(it, "w")    }
        observe(karooDataService.lastLapNpData)       { lastLapNp       = formatStreamData(it, "w")    }
    }

    // ── Shifting (multi-field for gears, simple for count) ─────────────────
    private fun observeShiftingMetrics() {
        scope.launch {
            karooDataService.shiftingFrontGearData.collect {
                currentData.shiftingFrontGear = formatGear(it, DataType.Field.SHIFTING_FRONT_GEAR, DataType.Field.SHIFTING_FRONT_GEAR_MAX)
                currentData.isDirty = true
            }
        }
        scope.launch {
            karooDataService.shiftingRearGearData.collect {
                currentData.shiftingRearGear = formatGear(it, DataType.Field.SHIFTING_REAR_GEAR, DataType.Field.SHIFTING_REAR_GEAR_MAX)
                currentData.isDirty = true
            }
        }
        scope.launch {
            karooDataService.shiftingBatteryData.collect {
                currentData.shiftingBattery = formatShiftingBattery(it)
                currentData.isDirty = true
            }
        }
        observe(karooDataService.shiftingCountData) { shiftingCount = formatInteger(it) }
    }

    // ── Navigation ────────────────────────────────────────────────────────
    private fun observeNavigationMetrics() {
        observe(karooDataService.distanceToTurnData) { distanceToTurn = formatDistanceToTurn(it)  }
        observe(karooDataService.distanceToDestData) { distanceToDest = formatStreamData(it, "km") }
        observe(karooDataService.timeOfArrivalData)  { timeOfArrival  = formatClockTime(it)        }
        observe(karooDataService.timeToDestData)     { timeToDest     = formatDuration(it)          }
        observe(karooDataService.headingData)        { heading        = formatHeading(it)           }
    }

    // ── eBike ─────────────────────────────────────────────────────────────
    private fun observeEBikeMetrics() {
        observe(karooDataService.levBatteryData)    { levBattery    = formatPercent(it)         }
        observe(karooDataService.levRangeData)      { levRange      = formatStreamData(it, "km") }
        observe(karooDataService.levAssistModeData) { levAssistMode = formatInteger(it)          }
        observe(karooDataService.levMotorPowerData) { levMotorPower = formatStreamData(it, "w")  }
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

    private fun flushWithEfficientMode(screen: com.kema.k2look.model.LayoutScreen) {
        screen.dataFields.forEach { field ->
            val value = currentData.valueFor(field.dataField.id)
            updateVisualization(field, value)
        }
    }

    private fun flushWithBasicMode(screen: com.kema.k2look.model.LayoutScreen) {
        val template = screen.getTemplate()
        screen.dataFields.forEach { field ->
            val value = currentData.valueFor(field.dataField.id)
            when (field.visualizationType ?: VisualizationType.TEXT) {
                com.kema.k2look.model.VisualizationType.TEXT -> {
                    val zone = template.zones.find { it.id == field.zoneId }
                    if (zone == null) {
                        Log.w(TAG, "Zone ${field.zoneId} not found in template ${template.id}")
                        return@forEach
                    }
                    activeLookService.displayField(field, value, zone.y)
                }
                else -> updateVisualization(field, value)
            }
        }
    }

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

    // Formatter methods live in BridgeMetricFormatters.kt (package-level functions)

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

