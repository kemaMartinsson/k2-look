package com.kema.k2look.service

import android.content.Context
import android.util.Log
import com.activelook.activelooksdk.DiscoveredGlasses
import com.kema.k2look.data.ProfileRepository
import com.kema.k2look.model.VisualizationType
import com.kema.k2look.util.PreferencesManager
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.ReleaseBluetooth
import io.hammerhead.karooext.models.RequestBluetooth
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

    private val context = context
    private val karooDataService = KarooDataService(context)
    private val activeLookService = ActiveLookService(context)
    private val layoutService = ActiveLookLayoutService(activeLookService)
    private val preferencesManager = PreferencesManager(context)
    private val profileRepository = ProfileRepository(context)

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var updateJob: Job? = null
    private var reconnectJob: Job? = null
    private var scanJob: Job? = null
    private var scanTimeoutJob: Job? = null
    private var statusLogJob: Job? = null
    private var autoConnectCollectionJob: Job? = null
    private var autoConnectTimeoutJob: Job? = null
    private var bluetoothRequested = false

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

    // Auto profile switching on ride start
    private var hasAutoSwitchedProfile = false // Track if we've auto-switched this ride
    private var lastKarooProfileName: String? = null // Track last seen Karoo profile

    // Update throttling — 2-second interval. Radar updates bypass this (see radarFlushImmediate).
    private var lastUpdateTime = 0L
    private val updateIntervalMs = 2000L // 2 seconds

    // When false (display toggled off by user), flushToGlasses() is suppressed so
    // outgoing layout writes don't immediately power the display back on.
    private var displayOn = true

    /** Notify the bridge that the display has been toggled on or off. */
    fun setDisplayOn(on: Boolean) {
        displayOn = on
        Log.d(
                TAG,
                "Display power: ${if (on) "ON" else "OFF"} — updates ${if (on) "resumed" else "suppressed"}"
        )
    }

    // True if the active profile has at least one radar field on any screen.
    // Cached on setActiveProfile() so observeRadar() can gate the bypass cheaply.
    private var activeProfileHasRadar = false

    // ── Radar warning overlay ────────────────────────────────────────────────
    private var warningBitmapSmall: android.graphics.Bitmap? = null
    private var warningBitmapLarge: android.graphics.Bitmap? = null
    private var radarWarningEnabled: Boolean = true

    // Reconnect tracking
    private var reconnectStartTime = 0L
    private var lastConnectedGlassesAddress: String? = null
    private var isInActiveRide = false
    private val reconnectIntervalMs = 15000L // Try reconnect every 15 seconds

    // Add a lightweight heartbeat for idle logging (prevents logcat spam)
    private var lastNoDataLogTimeMs: Long = 0L
    private val noDataLogIntervalMs: Long = 30_000L

    /** Bridge state enum */
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

    /** Set the active DataField profile for display layout */
    fun setActiveProfile(profile: com.kema.k2look.model.DataFieldProfile) {
        activeProfile = profile
        activeScreenId = profile.screens.firstOrNull()?.id
        // Cache whether any screen has a radar field so observeRadar() can bypass the
        // 2-second throttle without iterating the profile on every radar packet.
        val radarIds = setOf(50, 51, 52)
        activeProfileHasRadar =
                profile.screens.any { s -> s.dataFields.any { it.dataField.id in radarIds } }
        Log.i(
                TAG,
                "📋 Active profile set: ${profile.name} (${profile.screens.size} screens), active screen: $activeScreenId"
        )

        if (activeLookService.isConnected) {
            scope.launch {
                // saveAndActivateProfile handles cfgWrite/cfgSet (fast path if already stored)
                val success = layoutService.saveAndActivateProfile(profile)
                if (success) {
                    Log.i(TAG, "✅ Profile '${profile.name}' activated on glasses")
                } else {
                    Log.w(TAG, "⚠️ Failed to activate profile '${profile.name}' on glasses")
                }

                currentData.isDirty = true
                flushToGlasses()
                Log.i(TAG, "✅ Display updated after profile apply")
            }
        }
    }

    /** Set the currently displayed screen (called by gesture cycling / LayoutBuilderViewModel) */
    fun setActiveScreen(screenId: Int) {
        activeScreenId = screenId
        Log.d(TAG, "Active screen changed to: $screenId")
        currentData.isDirty = true
    }

    /**
     * Invalidate the glasses-side config cache for [profileId]. Call this whenever a profile's
     * fields or layout are modified so the next [setActiveProfile] triggers a re-upload rather than
     * a stale cfgSet.
     */
    fun invalidateProfileConfig(profileId: String) {
        layoutService.invalidateConfig(profileId)
    }

    /**
     * Callback to find K2Look profile by name Set by LayoutBuilderViewModel to enable
     * auto-switching
     */
    private var profileLookup: ((String) -> com.kema.k2look.model.DataFieldProfile?)? = null

    /** Set the profile lookup callback for auto-switching */
    fun setProfileLookup(lookup: (String) -> com.kema.k2look.model.DataFieldProfile?) {
        profileLookup = lookup
        Log.i(TAG, "Profile lookup callback registered for auto-switching")
    }

    /**
     * Attempt to auto-switch profile based on Karoo profile name Only happens once at ride start,
     * not mid-ride
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

    /** Reset auto-switch flag when ride ends (allows auto-switch on next ride) */
    private fun resetAutoSwitch() {
        hasAutoSwitchedProfile = false
        lastKarooProfileName = null
        Log.d(TAG, "Auto-switch reset - ready for next ride")
    }

    /** Initialize both services and auto-connect based on preferences */
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

        // Request BLE radio from Karoo OS so it stays available for our scanning
        requestBluetooth()

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

        statusLogJob =
                scope.launch {
                    while (true) {
                        delay(15_000)

                        val activeLookState = activeLookService.connectionState.value
                        val scanning = activeLookService.isScanning.value
                        val connectedAddr =
                                try {
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

    /** Attempt to auto-connect to previously connected glasses */
    private fun attemptAutoConnectToGlasses(targetAddress: String) {
        // Cancel any previous auto-connect scan to avoid duplicate collectors
        autoConnectCollectionJob?.cancel()
        autoConnectTimeoutJob?.cancel()

        val timeoutMinutes = preferencesManager.getStartupTimeoutMinutes()
        val timeoutMs = timeoutMinutes * 60 * 1000L // Convert to milliseconds

        android.util.Log.i(
                TAG,
                "🔍 Scanning for previously connected glasses: $targetAddress (timeout: ${timeoutMinutes}min)"
        )

        // Stop any existing scan before starting fresh
        if (activeLookService.isScanning.value) {
            activeLookService.stopScanning()
        }

        // Start scanning
        activeLookService.startScanning()

        var glassesFound = false

        // Observe discovered glasses and connect when found
        autoConnectCollectionJob =
                scope.launch {
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
                            autoConnectTimeoutJob?.cancel()
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
        autoConnectTimeoutJob =
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
                        autoConnectCollectionJob?.cancel()
                    }
                }
    }

    /** Connect to Karoo System */
    fun connectKaroo() {
        Log.i(TAG, "Connecting to Karoo System...")
        _bridgeState.value = BridgeState.KarooConnecting

        karooDataService.connect()

        // Start observing Karoo data
        observeKarooData()
    }

    /** Disconnect from Karoo System */
    fun disconnectKaroo() {
        Log.i(TAG, "Disconnecting from Karoo System...")
        karooDataService.disconnect()
        stopStreaming()
        updateBridgeState()
    }

    /**
     * Start scanning for ActiveLook glasses When called from UI, will auto-connect to first
     * discovered glasses
     */
    fun startActiveLookScan() {
        // Guard: do not trigger a new scan if glasses are already connected or connecting.
        // Scanning while connected causes a scan→disconnect→reconnect loop that leaves the
        // GATT Commands Interface in a ghost state (battery alive, cbb/cbc dead).
        val currentConnState = activeLookService.connectionState.value
        if (currentConnState is ActiveLookService.ConnectionState.Connected ||
                        currentConnState is ActiveLookService.ConnectionState.Connecting
        ) {
            Log.w(
                    TAG,
                    "⚠️ startActiveLookScan() called while already $currentConnState — ignoring to prevent ghost GATT state"
            )
            return
        }

        // Cancel any previous scan jobs (including auto-connect jobs)
        Log.i(
                TAG,
                "🔍 startActiveLookScan() called — scanJob.active=${scanJob?.isActive}, scanTimeoutJob.active=${scanTimeoutJob?.isActive}, currentBridgeState=${_bridgeState.value}"
        )
        scanJob?.cancel()
        scanTimeoutJob?.cancel()
        autoConnectCollectionJob?.cancel()
        autoConnectTimeoutJob?.cancel()

        Log.i(TAG, "Starting ActiveLook scan with auto-connect...")
        Log.i(TAG, "  SDK initialized: ${activeLookService.isSdkInitialized()}")
        Log.i(TAG, "  isScanning before start: ${activeLookService.isScanning.value}")
        Log.i(TAG, "  connectionState before start: ${activeLookService.connectionState.value}")

        // Ensure BLE is requested before scanning
        requestBluetooth()

        _bridgeState.value = BridgeState.ActiveLookScanning

        // Scan with retry: up to MAX_SCAN_RETRIES attempts with increasing delays
        var glassesFound = false
        var scanAttempt = 0

        scanJob =
                scope.launch {
                    while (scanAttempt < MAX_SCAN_RETRIES && !glassesFound) {
                        scanAttempt++
                        Log.i(TAG, "🔍 Scan attempt $scanAttempt/$MAX_SCAN_RETRIES")

                        // Stop any lingering scan and start fresh
                        if (activeLookService.isScanning.value) {
                            activeLookService.stopScanning()
                            delay(500) // Let BLE adapter settle
                        }

                        activeLookService.startScanning()
                        Log.i(
                                TAG,
                                "  isScanning after start: ${activeLookService.isScanning.value}"
                        )

                        // Wait for SCAN_ATTEMPT_DURATION_MS, checking for results
                        val scanStartTime = System.currentTimeMillis()
                        val collectJob = launch {
                            activeLookService.discoveredGlasses.collect { glassesList ->
                                if (glassesList.isNotEmpty() && !glassesFound) {
                                    val firstGlasses = glassesList.first()
                                    glassesFound = true
                                    Log.i(
                                            TAG,
                                            "✅ Auto-connecting to discovered glasses: ${firstGlasses.name} (attempt $scanAttempt)"
                                    )
                                    activeLookService.stopScanning()
                                    connectActiveLook(firstGlasses)
                                }
                            }
                        }

                        // Wait for this attempt's duration
                        delay(SCAN_ATTEMPT_DURATION_MS)
                        collectJob.cancel()

                        if (!glassesFound) {
                            activeLookService.stopScanning()
                            if (scanAttempt < MAX_SCAN_RETRIES) {
                                val backoffMs = SCAN_RETRY_BASE_DELAY_MS * scanAttempt
                                Log.i(
                                        TAG,
                                        "⏳ No glasses found on attempt $scanAttempt, retrying in ${backoffMs}ms..."
                                )
                                delay(backoffMs)
                            }
                        }
                    }

                    if (!glassesFound) {
                        Log.w(
                                TAG,
                                "Scan failed after $MAX_SCAN_RETRIES attempts — no glasses found"
                        )
                        scanJob = null
                        updateBridgeState()
                    }
                }
    }

    /** Stop scanning for ActiveLook glasses */
    fun stopActiveLookScan() {
        Log.i(TAG, "Stopping ActiveLook scan...")
        activeLookService.stopScanning()
        updateBridgeState()
    }

    /** Connect to ActiveLook glasses */
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

    /** Disconnect from ActiveLook glasses */
    fun disconnectActiveLook() {
        Log.i(TAG, "Disconnecting from ActiveLook glasses...")
        activeLookService.disconnect()
        stopStreaming()
        updateBridgeState()
    }

    /**
     * Observe Karoo data streams and update [currentData]. Delegated to focused sub-functions by
     * metric category.
     */
    private fun observeKarooData() {
        observeSystemState() // connection, ride state, profile auto-switch
        observeCoreMetrics() // speed, HR, cadence, power, distance, time, VAM
        observeRadar() // multi-field radar stream
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
     * Convenience: collect [flow] in a new coroutine, apply [update] to [currentData], and mark the
     * frame dirty. Eliminates 3-line boilerplate per metric.
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
                            Log.i(
                                    TAG,
                                    "Entered active ride - starting continuous reconnect monitoring"
                            )
                            startContinuousReconnect()
                        }
                    }
                    else ->
                            if (wasInActiveRide) {
                                Log.i(
                                        TAG,
                                        "Exited active ride - stopping continuous reconnect monitoring"
                                )
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
    }

    // ── Core cycling metrics ───────────────────────────────────────────────
    private fun observeCoreMetrics() {
        observe(karooDataService.speedData) { speed = formatStreamData(it, "km/h") }
        observe(karooDataService.maxSpeedData) { maxSpeed = formatStreamData(it, "km/h") }
        observe(karooDataService.averageSpeedData) { avgSpeed = formatStreamData(it, "km/h") }
        observe(karooDataService.heartRateData) { heartRate = formatStreamData(it, "bpm") }
        observe(karooDataService.maxHeartRateData) { maxHeartRate = formatStreamData(it, "bpm") }
        observe(karooDataService.averageHeartRateData) {
            avgHeartRate = formatStreamData(it, "bpm")
        }
        observe(karooDataService.hrZoneData) { hrZone = formatHRZoneData(it) }
        observe(karooDataService.cadenceData) { cadence = formatStreamDataInt(it, "rpm") }
        observe(karooDataService.maxCadenceData) { maxCadence = formatStreamDataInt(it, "rpm") }
        observe(karooDataService.averageCadenceData) { avgCadence = formatStreamDataInt(it, "rpm") }
        observe(karooDataService.powerData) { power = formatStreamDataInt(it, "w") }
        observe(karooDataService.maxPowerData) { maxPower = formatStreamDataInt(it, "w") }
        observe(karooDataService.averagePowerData) { avgPower = formatStreamDataInt(it, "w") }
        observe(karooDataService.smoothed3sPowerData) { power3s = formatStreamDataInt(it, "w") }
        observe(karooDataService.distanceData) { distance = formatStreamData(it, "km") }
        observe(karooDataService.timeData) { time = formatTimeData(it) }
        observe(karooDataService.vamData) { vam = formatStreamData(it, "m/h") }
        observe(karooDataService.avgVamData) { avgVam = formatStreamData(it, "m/h") }
    }

    // ── Radar (multi-field DataPoint — handled separately) ─────────────────
    private fun getWarningBitmapSmall(): android.graphics.Bitmap =
        warningBitmapSmall ?: android.graphics.BitmapFactory.decodeStream(
            context.assets.open("warning_white_28.png")
        ).also { warningBitmapSmall = it }

    private fun getWarningBitmapLarge(): android.graphics.Bitmap =
        warningBitmapLarge ?: android.graphics.BitmapFactory.decodeStream(
            context.assets.open("warning_white_40.png")
        ).also { warningBitmapLarge = it }

    private val renderWarningSmall: () -> Unit = {
        try {
            activeLookService.getConnectedGlasses()
                ?.imgStream(getWarningBitmapSmall(), com.activelook.activelooksdk.types.ImgStreamFormat.MONO_4BPP_HEATSHRINK, 30, 25)
        } catch (e: Exception) {
            Log.e(TAG, "renderWarningSmall failed: ${e.message}", e)
        }
    }

    private val eraseWarningSmall: () -> Unit = {
        try {
            activeLookService.getConnectedGlasses()?.let { g ->
                g.holdFlush(com.activelook.activelooksdk.types.holdFlushAction.HOLD)
                g.color(0)
                g.rectf(30, 25, 57, 52)
                g.color(15)
                g.holdFlush(com.activelook.activelooksdk.types.holdFlushAction.FLUSH)
            }
        } catch (e: Exception) {
            Log.e(TAG, "eraseWarningSmall failed: ${e.message}", e)
        }
    }

    private val renderWarningLarge: () -> Unit = {
        try {
            activeLookService.getConnectedGlasses()
                ?.imgStream(getWarningBitmapLarge(), com.activelook.activelooksdk.types.ImgStreamFormat.MONO_4BPP_HEATSHRINK, 30, 25)
        } catch (e: Exception) {
            Log.e(TAG, "renderWarningLarge failed: ${e.message}", e)
        }
    }

    private val eraseWarningLarge: () -> Unit = {
        try {
            activeLookService.getConnectedGlasses()?.let { g ->
                g.holdFlush(com.activelook.activelooksdk.types.holdFlushAction.HOLD)
                g.color(0)
                g.rectf(30, 25, 69, 64)
                g.color(15)
                g.holdFlush(com.activelook.activelooksdk.types.holdFlushAction.FLUSH)
            }
        } catch (e: Exception) {
            Log.e(TAG, "eraseWarningLarge failed: ${e.message}", e)
        }
    }

    private val radarWarningController = RadarWarningController(
        renderSmall = renderWarningSmall,
        eraseSmall  = eraseWarningSmall,
        renderLarge = renderWarningLarge,
        eraseLarge  = eraseWarningLarge
    )

    /** Enable or disable the radar warning overlay. */
    fun setRadarWarningEnabled(enabled: Boolean) {
        radarWarningEnabled = enabled
        radarWarningController.setEnabled(enabled)
        Log.d(TAG, "Radar warning overlay: ${if (enabled) "enabled" else "disabled"}")
    }

    private fun observeRadar() {
        scope.launch {
            karooDataService.radarData.collect { streamState ->
                when (streamState) {
                    is StreamState.Streaming -> {
                        val v = streamState.dataPoint.values
                        val threat = v[DataType.Field.RADAR_THREAT_LEVEL]?.toInt() ?: 0
                        currentData.radarThreatLevel = threat.toString()
                        val ranges =
                                listOfNotNull(
                                                v[DataType.Field.RADAR_TARGET_1_RANGE],
                                                v[DataType.Field.RADAR_TARGET_2_RANGE],
                                                v[DataType.Field.RADAR_TARGET_3_RANGE],
                                                v[DataType.Field.RADAR_TARGET_4_RANGE],
                                                v[DataType.Field.RADAR_TARGET_5_RANGE],
                                                v[DataType.Field.RADAR_TARGET_6_RANGE],
                                                v[DataType.Field.RADAR_TARGET_7_RANGE],
                                                v[DataType.Field.RADAR_TARGET_8_RANGE]
                                        )
                                        .filter { it > 0.0 }
                        currentData.radarTargetCount = ranges.size.toString()
                        val closest = ranges.minOrNull()
                        currentData.radarClosestRange =
                                if (closest != null) "${formatValue(closest)} m" else "--"
                        // Bypass the 2s throttle for radar — it's a safety metric.
                        // Only do this when the profile actually shows radar to avoid
                        // flooding the BLE queue on profiles that don't use radar.
                        if (activeProfileHasRadar) currentData.radarFlushImmediate = true
                        radarWarningController.onRadarUpdate(threat, closest?.toFloat())
                        Log.d(
                                TAG,
                                "Radar: threat=$threat, targets=${ranges.size}, closest=${currentData.radarClosestRange}"
                        )
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
    }

    // ── General additions ──────────────────────────────────────────────────
    private fun observeGeneralMetrics() {
        observe(karooDataService.clockTimeData) { clockTime = formatClockTime(it) }
        observe(karooDataService.temperatureData) { temperature = formatStreamData(it, "°C") }
        observe(karooDataService.batteryPercentData) { batteryPercent = formatPercent(it) }
        observe(karooDataService.rideTimeData) { rideTime = formatTimeData(it) }
    }

    // ── Heart Rate additions ───────────────────────────────────────────────
    private fun observeHeartRateMetrics() {
        observe(karooDataService.percentMaxHrData) { percentMaxHr = formatPercent(it) }
        observe(karooDataService.percentHrrData) { percentHrr = formatPercent(it) }
    }

    // ── Power additions ────────────────────────────────────────────────────
    private fun observePowerMetrics() {
        observe(karooDataService.powerZoneData) { powerZone = formatZoneData(it, 7) }
        observe(karooDataService.smoothed5sPowerData) { power5s = formatStreamDataInt(it, "w") }
        observe(karooDataService.smoothed10sPowerData) { power10s = formatStreamDataInt(it, "w") }
        observe(karooDataService.smoothed30sPowerData) { power30s = formatStreamDataInt(it, "w") }
        observe(karooDataService.normalizedPowerData) {
            normalizedPower = formatStreamDataInt(it, "w")
        }
        observe(karooDataService.percentFtpData) { percentFtp = formatPercent(it) }
        observe(karooDataService.intensityFactorData) { intensityFactor = formatStreamData(it, "") }
        observe(karooDataService.trainingStressScoreData) { tss = formatStreamData(it, "") }
        observe(karooDataService.powerToWeightData) { wPerKg = formatStreamData(it, "w/kg") }
    }

    // ── Energy ────────────────────────────────────────────────────────────
    private fun observeEnergyMetrics() {
        observe(karooDataService.energyOutputData) { energyOutput = formatStreamDataInt(it, "kJ") }
        observe(karooDataService.caloriesData) { calories = formatStreamDataInt(it, "kcal") }
        observe(karooDataService.caloriesPerHourData) {
            caloriesPerHour = formatStreamDataInt(it, "kcal/h")
        }
    }

    // ── Speed and Cadence additions ────────────────────────────────────────
    private fun observeSpeedCadenceMetrics() {
        observe(karooDataService.smoothed3sSpeedData) { speed3s = formatStreamData(it, "km/h") }
        observe(karooDataService.smoothed3sCadenceData) { cadence3s = formatStreamData(it, "rpm") }
    }

    // ── Elevation ─────────────────────────────────────────────────────────
    private fun observeElevationMetrics() {
        observe(karooDataService.elevationGradeData) { elevationGrade = formatGrade(it) }
        observe(karooDataService.elevationGainData) { elevationGain = formatStreamData(it, "m") }
        observe(karooDataService.elevationLossData) { elevationLoss = formatStreamData(it, "m") }
        observe(karooDataService.altitudeData) { altitude = formatStreamData(it, "m") }
        observe(karooDataService.vam30sData) { vam30s = formatStreamData(it, "m/h") }
    }

    // ── Lap ───────────────────────────────────────────────────────────────
    private fun observeLapMetrics() {
        observe(karooDataService.lapNumberData) { lapNumber = formatInteger(it) }
        observe(karooDataService.lapTimeData) { lapTime = formatLapTime(it) }
        observe(karooDataService.lapDistanceData) { lapDistance = formatStreamData(it, "km") }
        observe(karooDataService.lapSpeedData) { lapSpeed = formatStreamData(it, "km/h") }
        observe(karooDataService.lapHrData) { lapHr = formatStreamData(it, "bpm") }
        observe(karooDataService.lapPowerData) { lapPower = formatStreamData(it, "w") }
        observe(karooDataService.lapNpData) { lapNp = formatStreamData(it, "w") }
        observe(karooDataService.lapCadenceData) { lapCadence = formatStreamData(it, "rpm") }
        observe(karooDataService.lapAscentData) { lapAscent = formatStreamData(it, "m") }
    }

    // ── Last Lap ──────────────────────────────────────────────────────────
    private fun observeLastLapMetrics() {
        observe(karooDataService.lastLapTimeData) { lastLapTime = formatLapTime(it) }
        observe(karooDataService.lastLapDistanceData) {
            lastLapDistance = formatStreamData(it, "km")
        }
        observe(karooDataService.lastLapSpeedData) { lastLapSpeed = formatStreamData(it, "km/h") }
        observe(karooDataService.lastLapHrData) { lastLapHr = formatStreamData(it, "bpm") }
        observe(karooDataService.lastLapPowerData) { lastLapPower = formatStreamData(it, "w") }
        observe(karooDataService.lastLapNpData) { lastLapNp = formatStreamData(it, "w") }
    }

    // ── Shifting (multi-field for gears, simple for count) ─────────────────
    private fun observeShiftingMetrics() {
        scope.launch {
            karooDataService.shiftingFrontGearData.collect {
                currentData.shiftingFrontGear =
                        formatGear(
                                it,
                                DataType.Field.SHIFTING_FRONT_GEAR,
                                DataType.Field.SHIFTING_FRONT_GEAR_MAX
                        )
                currentData.isDirty = true
            }
        }
        scope.launch {
            karooDataService.shiftingRearGearData.collect {
                currentData.shiftingRearGear =
                        formatGear(
                                it,
                                DataType.Field.SHIFTING_REAR_GEAR,
                                DataType.Field.SHIFTING_REAR_GEAR_MAX
                        )
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
        observe(karooDataService.distanceToTurnData) { distanceToTurn = formatDistanceToTurn(it) }
        observe(karooDataService.distanceToDestData) { distanceToDest = formatStreamData(it, "km") }
        observe(karooDataService.timeOfArrivalData) { timeOfArrival = formatClockTime(it) }
        observe(karooDataService.timeToDestData) { timeToDest = formatDuration(it) }
        observe(karooDataService.headingData) { heading = formatHeading(it) }
    }

    // ── eBike ─────────────────────────────────────────────────────────────
    private fun observeEBikeMetrics() {
        observe(karooDataService.levBatteryData) { levBattery = formatPercent(it) }
        observe(karooDataService.levRangeData) { levRange = formatStreamData(it, "km") }
        observe(karooDataService.levAssistModeData) { levAssistMode = formatInteger(it) }
        observe(karooDataService.levMotorPowerData) { levMotorPower = formatStreamData(it, "w") }
    }

    /** Observe ActiveLook connection state */
    private fun observeActiveLookState() {
        scope.launch {
            activeLookService.connectionState.collect { state ->
                Log.d(TAG, "ActiveLook connection state: $state")
                when (state) {
                    is ActiveLookService.ConnectionState.Connected -> {
                        // Stop reconnect loop now that we're connected
                        stopContinuousReconnect()

                        // Save the connected glasses address for reconnect attempts
                        lastConnectedGlassesAddress = state.glasses.address
                        Log.d(
                                TAG,
                                "Tracking connected glasses address: $lastConnectedGlassesAddress"
                        )

                        // Refresh config cache so fast-path cfgSet works immediately
                        scope.launch { layoutService.refreshConfigCache() }

                        // Re-upload active profile on (re)connect so glasses have layout data
                        activeProfile?.let { profile ->
                            scope.launch {
                                val success = layoutService.saveAndActivateProfile(profile)
                                if (success) {
                                    Log.i(
                                            TAG,
                                            "✅ Profile '${profile.name}' re-uploaded on reconnect"
                                    )
                                } else {
                                    Log.w(TAG, "⚠️ Failed to re-upload profile on reconnect")
                                }
                            }
                        }

                        updateBridgeState()
                        // If Karoo is also connected and riding, start streaming
                        if (karooDataService.isConnected) {
                            startStreaming()
                        }
                    }
                    is ActiveLookService.ConnectionState.Disconnected -> {
                        radarWarningController.reset()
                        warningBitmapSmall = null
                        warningBitmapLarge = null
                        // Auto-reconnect if we have a known glasses address
                        if (lastConnectedGlassesAddress != null) {
                            Log.w(
                                    TAG,
                                    "Glasses disconnected - will attempt reconnect to $lastConnectedGlassesAddress"
                            )
                            startContinuousReconnect()
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

    /** Update bridge state based on both service states */
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

    /** Start streaming data to ActiveLook glasses */
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
        updateJob =
                scope.launch {
                    while (true) {
                        try {
                            // Wait for the update interval
                            delay(updateIntervalMs)

                            // Flush accumulated data to glasses
                            flushToGlasses()
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            // Normal coroutine cancellation (e.g. stopStreaming / cleanup) — exit
                            // cleanly
                            throw e
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in streaming loop: ${e.message}", e)
                        }
                    }
                }

        Log.i(TAG, "✓ Streaming started (1 update/second)")
    }

    /** Stop streaming data to glasses */
    private fun stopStreaming() {
        updateJob?.cancel()
        updateJob = null

        if (_bridgeState.value == BridgeState.Streaming) {
            Log.i(TAG, "Streaming stopped")
            updateBridgeState()
        }
    }

    /** Flush accumulated data to ActiveLook glasses (hold/flush pattern) */
    private fun flushToGlasses() {
        if (!displayOn) return // display toggled off — don't re-light it with data writes
        if (!currentData.isDirty) {
            val now = System.currentTimeMillis()
            if (now - lastNoDataLogTimeMs >= noDataLogIntervalMs) {
                Log.v(TAG, "No data changes, skipping update")
                lastNoDataLogTimeMs = now
            }
            return
        }

        val currentTime = System.currentTimeMillis()
        val radarBypass = currentData.radarFlushImmediate
        if (!radarBypass && currentTime - lastUpdateTime < updateIntervalMs) {
            // Log.v(TAG, "Throttling update")
            return
        }

        try {
            var profile = activeProfile
            if (profile == null || profile.screens.isEmpty()) {
                // No profile set yet (e.g. Layout Builder tab not visited) — load latest from
                // storage
                profile = profileRepository.loadProfiles().firstOrNull()
                if (profile != null && profile.screens.isNotEmpty()) {
                    Log.i(TAG, "Auto-loaded profile '${profile.name}' from storage")
                    setActiveProfile(profile)
                } else {
                    return
                }
            }
            flushWithProfile(profile)
            currentData.isDirty = false
            currentData.radarFlushImmediate = false
            // Only advance the 2-second throttle clock on a regular flush, not a radar bypass,
            // so the next normal refresh still fires on schedule.
            if (!radarBypass) lastUpdateTime = currentTime
            // Log.d(TAG, "✓ Data flushed to glasses")
        } catch (e: Exception) {
            Log.e(TAG, "Error flushing data to glasses: ${e.message}", e)
        }
    }

    private fun flushWithProfile(profile: com.kema.k2look.model.DataFieldProfile) {
        val selectedScreenId = activeScreenId
        val screen =
                if (selectedScreenId != null)
                        profile.screens.find { it.id == selectedScreenId }
                                ?: profile.screens.first()
                else profile.screens.first()

        // Log.v(TAG, "Flushing: profile=${profile.name}, screen=${screen.id},
        // fields=${screen.dataFields.size}")

        if (layoutService.isProfileSaved(profile.id)) {
            flushWithEfficientMode(screen)
        } else {
            // Profile not yet uploaded to glasses (e.g. still in cfgWrite) — skip this frame.
            // Log.d(TAG, "Profile '${profile.name}' not yet saved on glasses, skipping frame")
        }
    }

    private fun flushWithEfficientMode(screen: com.kema.k2look.model.LayoutScreen) {
        val fields = mutableMapOf<String, String>()
        screen.dataFields.forEach { field ->
            val value = currentData.valueFor(field.dataField.id)
            when (field.visualizationType ?: VisualizationType.TEXT) {
                com.kema.k2look.model.VisualizationType.TEXT -> fields[field.zoneId] = value
                else -> updateVisualization(field, value)
            }
        }
        if (fields.isNotEmpty()) {
            layoutService.displayAllFieldValues(fields, screen)
        }
    }

    // Formatter methods live in BridgeMetricFormatters.kt (package-level functions)

    /** Get KarooDataService for direct access if needed */
    fun getKarooDataService(): KarooDataService = karooDataService

    /** Get ActiveLookService for direct access if needed */
    fun getActiveLookService(): ActiveLookService = activeLookService

    /** Get ActiveLookLayoutService for Phase 4.2 layout management */
    fun getLayoutService(): ActiveLookLayoutService = layoutService

    /**
     * Start a local simulator that periodically pushes sample values to the glasses. This uses the
     * same flush pipeline as normal streaming.
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

        Log.i(TAG, "✅ Starting simulator (profile-aware)")
        simulatorJob =
                scope.launch {
                    var tick = 0
                    Log.i(TAG, "🔁 Simulator coroutine loop started")
                    while (true) {
                        tick++
                        Log.d(
                                TAG,
                                "📊 Simulator tick $tick (profile: ${activeProfile?.name ?: "none"})"
                        )
                        pushSimulatedFrame(tick)
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
     * Simulate one frame: populate every field that appears in the active profile, then flush to
     * the glasses. Falls back to the 6 core fields when no profile is set.
     */
    private fun pushSimulatedFrame(tick: Int) {
        val fieldIds =
                activeProfile
                        ?.screens
                        ?.flatMap { it.dataFields }
                        ?.map { it.dataField.id }
                        ?.distinct()
                        ?.takeIf { it.isNotEmpty() }
                        ?: listOf(
                                1,
                                2,
                                4,
                                7,
                                12,
                                18
                        ) // fallback: time, distance, HR, power, speed, cadence

        for (id in fieldIds) {
            applySimulatedValue(id, tick)
        }
        currentData.isDirty = true
        flushToGlasses()
    }

    /** Write one simulated value into [currentData] for [id]. */
    @Suppress("ComplexMethod")
    private fun applySimulatedValue(id: Int, t: Int) {
        when (id) {
            // ── General ──────────────────────────────────────────────────
            1 -> currentData.time = formatSimulatedTime(t * 2)
            2 -> currentData.distance = "${t / 10}.${t % 10} km"
            53 -> currentData.clockTime = String.format(java.util.Locale.ROOT, "14:%02d", t % 60)
            54 -> currentData.temperature = "${18 + t % 10} °C"
            55 -> currentData.batteryPercent = "${80 - t % 30}%"
            56 -> currentData.rideTime = formatSimulatedTime(t * 2)
            // ── Heart Rate ───────────────────────────────────────────────
            4 -> currentData.heartRate = "${140 + t % 30}"
            5 -> currentData.maxHeartRate = "175"
            6 -> currentData.avgHeartRate = "145"
            47 -> currentData.hrZone = "Z${2 + (t / 10) % 3}"
            57 -> currentData.percentMaxHr = "${75 + t % 15}%"
            58 -> currentData.percentHrr = "${65 + t % 20}%"
            // ── Power ────────────────────────────────────────────────────
            7 -> currentData.power = "${200 + t % 100}"
            8 -> currentData.maxPower = "450"
            9 -> currentData.avgPower = "210"
            10 -> currentData.power3s = "${195 + t % 80}"
            48 -> currentData.powerZone = "Z${2 + (t / 15) % 4}"
            59 -> currentData.power5s = "${198 + t % 90}"
            60 -> currentData.power10s = "${205 + t % 70}"
            61 -> currentData.power30s = "${210 + t % 50}"
            62 -> currentData.normalizedPower = "${215 + t % 40}"
            63 -> currentData.percentFtp = "${85 + t % 30}%"
            64 ->
                    currentData.intensityFactor =
                            String.format(java.util.Locale.ROOT, "%.2f", 0.85 + (t % 15) * 0.01)
            65 -> currentData.tss = "${50 + t * 2}"
            66 ->
                    currentData.wPerKg =
                            String.format(java.util.Locale.ROOT, "%.1f", 3.2 + (t % 10) * 0.1)
            // ── Speed ────────────────────────────────────────────────────
            12 -> currentData.speed = "${25 + t % 15}"
            13 -> currentData.maxSpeed = "42"
            14 -> currentData.avgSpeed = "28"
            70 -> currentData.speed3s = "${24 + t % 12}"
            // ── Cadence ──────────────────────────────────────────────────
            18 -> currentData.cadence = "${85 + t % 20}"
            19 -> currentData.maxCadence = "102"
            20 -> currentData.avgCadence = "88"
            71 -> currentData.cadence3s = "${83 + t % 18}"
            // ── Energy ───────────────────────────────────────────────────
            67 -> currentData.energyOutput = "${200 + t * 5} kJ"
            68 -> currentData.calories = "${150 + t * 3}"
            69 -> currentData.caloriesPerHour = "${600 + t % 200}"
            // ── Climbing ─────────────────────────────────────────────────
            24 -> currentData.vam = "${800 + t % 400}"
            25 -> currentData.avgVam = "650"
            // ── Elevation ────────────────────────────────────────────────
            72 -> currentData.elevationGrade = "${-2 + t % 8}%"
            73 -> currentData.elevationGain = "${100 + t * 2} m"
            74 -> currentData.elevationLoss = "${30 + t} m"
            75 -> currentData.altitude = "${250 + t * 3} m"
            76 -> currentData.vam30s = "${700 + t % 300}"
            // ── Lap ──────────────────────────────────────────────────────
            77 -> currentData.lapNumber = "${1 + t / 30}"
            78 -> currentData.lapTime = formatSimulatedTime(t * 2 % 3600)
            79 ->
                    currentData.lapDistance =
                            String.format(java.util.Locale.ROOT, "%.1f km", (t % 50) * 0.1 + 0.1)
            80 -> currentData.lapSpeed = "${26 + t % 10}"
            81 -> currentData.lapHr = "${138 + t % 25}"
            82 -> currentData.lapPower = "${205 + t % 80}"
            83 -> currentData.lapNp = "${210 + t % 70}"
            84 -> currentData.lapCadence = "${87 + t % 15}"
            85 -> currentData.lapAscent = "${20 + t % 80} m"
            // ── Last Lap ─────────────────────────────────────────────────
            86 -> currentData.lastLapTime = "00:45:12"
            87 -> currentData.lastLapDistance = "22.5 km"
            88 -> currentData.lastLapSpeed = "29"
            89 -> currentData.lastLapHr = "142"
            90 -> currentData.lastLapPower = "215"
            91 -> currentData.lastLapNp = "220"
            // ── Radar ────────────────────────────────────────────────────
            50 -> currentData.radarThreatLevel = "${t % 3}"
            51 -> currentData.radarTargetCount = "${1 + t % 4}"
            52 -> currentData.radarClosestRange = "${15 + t % 50} m"
            // ── Shifting ─────────────────────────────────────────────────
            92 -> currentData.shiftingFrontGear = "3/3"
            93 -> currentData.shiftingRearGear = "${1 + t % 11}/11"
            94 -> currentData.shiftingBattery = "${85 - t % 20}%"
            95 -> currentData.shiftingCount = "${t * 3}"
            // ── Navigation ───────────────────────────────────────────────
            96 -> currentData.distanceToTurn = "${(2000 - t * 10).coerceAtLeast(0)} m"
            97 ->
                    currentData.distanceToDest =
                            String.format(
                                    java.util.Locale.ROOT,
                                    "%.1f km",
                                    (50.0 - t * 0.1).coerceAtLeast(0.0)
                            )
            98 -> currentData.timeOfArrival = "15:30"
            99 -> currentData.timeToDest = formatSimulatedTime((5400 - t * 2).coerceAtLeast(0))
            100 -> currentData.heading = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")[t % 8]
            // ── eBike ────────────────────────────────────────────────────
            101 -> currentData.levBattery = "${80 - t % 30}%"
            102 -> currentData.levRange = "${(60 - t).coerceAtLeast(0)} km"
            103 -> currentData.levAssistMode = listOf("OFF", "ECO", "TRAIL", "BOOST")[t % 4]
            104 -> currentData.levMotorPower = "${80 + t % 120} w"
        }
    }

    private fun formatSimulatedTime(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", h, m, s)
    }

    /** Clean up resources */
    fun cleanup() {
        Log.i(TAG, "Cleaning up KarooActiveLookBridge...")

        stopStreaming()
        stopContinuousReconnect()
        autoConnectCollectionJob?.cancel()
        autoConnectTimeoutJob?.cancel()
        releaseBluetooth()
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

        reconnectJob =
                scope.launch {
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

    /** Initialize gauges for all fields in the profile that use gauge visualization */

    /** Update visualization (gauge or bar) with current metric value */
    private fun updateVisualization(field: com.kema.k2look.model.LayoutDataField, value: String) {
        when (field.visualizationType ?: VisualizationType.TEXT) {
            com.kema.k2look.model.VisualizationType.GAUGE -> updateGauge(field, value)
            com.kema.k2look.model.VisualizationType.BAR -> updateProgressBar(field, value)
            com.kema.k2look.model.VisualizationType.ZONED_BAR -> updateZonedBar(field, value)
            com.kema.k2look.model.VisualizationType.TEXT -> {
                /* handled by batch in flushWithEfficientMode */
            }
        }
    }

    /** Update gauge with current metric value */
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
        scope.launch { activeLookService.displayGauge(gauge.id, percentage) }

        Log.d(TAG, "Gauge ${gauge.id}: ${field.dataField.name} = $numericValue ($percentage%)")
    }

    /** Update progress bar with current metric value */
    private fun updateProgressBar(field: com.kema.k2look.model.LayoutDataField, value: String) {
        val bar = field.progressBar ?: return

        // Parse numeric value
        val numericValue = parseNumericValue(value)
        if (numericValue == null) {
            Log.d(TAG, "Cannot update bar: non-numeric value '$value'")
            return
        }

        // Render bar at the field's actual template zone position
        scope.launch { layoutService.displayBarAtZone(bar, numericValue, field.zoneId) }

        Log.d(TAG, "Bar ${bar.id}: ${field.dataField.name} = $numericValue")
    }

    /** Update zoned progress bar with current metric value */
    private fun updateZonedBar(field: com.kema.k2look.model.LayoutDataField, value: String) {
        val zonedBar = field.zonedBar ?: return

        // Parse numeric value
        val numericValue = parseNumericValue(value)
        if (numericValue == null) {
            Log.d(TAG, "Cannot update zoned bar: non-numeric value '$value'")
            return
        }

        // Render zone circles at the field's actual template zone position.
        // Respect showIcon so toggling the icon off also removes it from zone circles.
        val iconId = if (field.showIcon) field.dataField.icon28 else null
        scope.launch {
            layoutService.displayZoneCircles(zonedBar, numericValue, field.zoneId, iconId)
        }

        val currentZone = zonedBar.findZone(numericValue)
        Log.d(
                TAG,
                "Zoned bar ${zonedBar.bar.id}: ${field.dataField.name} = $numericValue (${currentZone?.name ?: "?"})"
        )
    }

    /** Parse numeric value from display string (removes units, handles special cases) */
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

    // ========== BLUETOOTH RESOURCE MANAGEMENT ==========

    /**
     * Request the Karoo OS to keep BLE radio available for this app. Without this, the OS may
     * power-save the BLE adapter, causing scans to find 0 devices.
     */
    private fun requestBluetooth() {
        if (bluetoothRequested) return
        try {
            karooDataService.getKarooSystem().dispatch(RequestBluetooth(BT_RESOURCE_ID))
            bluetoothRequested = true
            Log.i(TAG, "📡 RequestBluetooth dispatched")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to request Bluetooth: ${e.message}")
        }
    }

    private fun releaseBluetooth() {
        if (!bluetoothRequested) return
        try {
            karooDataService.getKarooSystem().dispatch(ReleaseBluetooth(BT_RESOURCE_ID))
            bluetoothRequested = false
            Log.i(TAG, "📡 ReleaseBluetooth dispatched")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release Bluetooth: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "KarooActiveLookBridge"
        private const val BT_RESOURCE_ID = "k2look-activelook-ble"
        /** Per-attempt scan window before retrying */
        private const val SCAN_ATTEMPT_DURATION_MS = 10_000L
        /** Base delay between scan retries (multiplied by attempt number) */
        private const val SCAN_RETRY_BASE_DELAY_MS = 2_000L
        /** Maximum number of scan attempts for UI-triggered scan */
        private const val MAX_SCAN_RETRIES = 3
    }
}
