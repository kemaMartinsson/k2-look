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
    internal val karooDataService = KarooDataService(context)
    internal val activeLookService = ActiveLookService(context)
    private val layoutService = ActiveLookLayoutService(activeLookService)
    private val preferencesManager = PreferencesManager(context)
    private val profileRepository = ProfileRepository(context)

    internal val scope = CoroutineScope(Dispatchers.Main + Job())
    private var updateJob: Job? = null
    private var reconnectJob: Job? = null
    private var scanJob: Job? = null
    private var scanTimeoutJob: Job? = null
    private var statusLogJob: Job? = null
    private var autoConnectCollectionJob: Job? = null
    private var autoConnectTimeoutJob: Job? = null
    private var bluetoothRequested = false
    private var pendingUserScanUntilKarooReady = false

    // Simulator mode (for Debug tab)
    internal var simulatorJob: Job? = null

    // Bridge state
    private val _bridgeState = MutableStateFlow<BridgeState>(BridgeState.Idle)
    val bridgeState: StateFlow<BridgeState> = _bridgeState.asStateFlow()

    // Accumulated data for hold/flush pattern (see BridgeCurrentData.kt)
    internal val currentData = CurrentData()

    // Active DataField profile for dynamic layouts
    internal var activeProfile: com.kema.k2look.model.DataFieldProfile? = null

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

    // ── Radar warning ────────────────────────────────────────────────
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
                    // Battery layout is saved inside saveAndActivateProfile (slow path) and
                    // always needs a redraw after the display was cleared.
                    layoutService.updateBatteryDisplay(currentBatteryLevel)
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
                        "👓 Auto-connect enabled but no previous address — waiting for user to tap Connect"
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

        // ActiveLook glasses stop advertising after 3 minutes without a connection (API §2.2)
        val timeoutMs = 3 * 60 * 1000L

        android.util.Log.i(
                TAG,
                "🔍 Scanning for previously connected glasses: $targetAddress (timeout: 3min)"
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
                                "⏱️ Startup auto-connect timeout (3min): Could not find glasses with address $targetAddress"
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

        when (resolveScanStartAction(
                        karooConnected = karooDataService.isConnected,
                        pendingScanUntilKarooReady = pendingUserScanUntilKarooReady
                )
        ) {
            ScanStartAction.WAIT_FOR_KAROO -> {
                pendingUserScanUntilKarooReady = true
                Log.i(
                        TAG,
                        "⏳ Deferring glasses scan until Karoo service is connected (first-launch race protection)"
                )
                if (karooDataService.connectionState.value !is
                                KarooDataService.ConnectionState.Connecting
                ) {
                    connectKaroo()
                }
                return
            }
            ScanStartAction.NO_OP_ALREADY_PENDING -> {
                Log.i(TAG, "⏳ Glasses scan already queued; waiting for Karoo connection")
                return
            }
            ScanStartAction.START_NOW -> {
                // Continue below
            }
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
                    // Wait for the BLE adapter to become enabled after RequestBluetooth dispatch.
                    // The Karoo system enables BLE asynchronously — polling until ready (max 5s).
                    val btManager =
                            context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as?
                                    android.bluetooth.BluetoothManager
                    var bleWaitMs = 0
                    while (btManager?.adapter?.isEnabled != true && bleWaitMs < 5000) {
                        delay(250)
                        bleWaitMs += 250
                    }
                    if (btManager?.adapter?.isEnabled == true) {
                        Log.i(TAG, "✅ BLE adapter ready after ${bleWaitMs}ms")
                    } else {
                        Log.w(
                                TAG,
                                "⚠️ BLE adapter still not enabled after ${bleWaitMs}ms — scanning anyway"
                        )
                    }

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

    private fun resumePendingUserScanIfNeeded() {
        if (!pendingUserScanUntilKarooReady) return
        pendingUserScanUntilKarooReady = false
        scope.launch {
            // Give the Karoo system a short moment to process RequestBluetooth.
            delay(250)
            startActiveLookScan()
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
        observeGlassesBattery()
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
    internal fun observe(
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
                        // Request BLE now that the Karoo system service is connected and can handle
                        // it
                        requestBluetooth()
                        updateBridgeState()
                        resumePendingUserScanIfNeeded()
                    }
                    is KarooDataService.ConnectionState.Error ->
                            _bridgeState.value = BridgeState.Error("Karoo: ${state.message}")
                    is KarooDataService.ConnectionState.Disconnected -> {
                        bluetoothRequested = false // Reset so we re-request on next connection
                        updateBridgeState()
                    }
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
                                // Battery display is gated by batteryDisplayEnabled, not ride
                                // state.
                                // Nothing to do here.
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

    // ── Core cycling metrics → KarooActiveLookBridgeMetrics.kt ─────────────

    // ── Radar (multi-field DataPoint — handled separately) ─────────────────
    private fun getWarningBitmapSmall(): android.graphics.Bitmap =
            warningBitmapSmall
                    ?: android.graphics.BitmapFactory.decodeStream(
                                    context.assets.open("warning_white_28.png")
                            )
                            .also { warningBitmapSmall = it }

    private fun getWarningBitmapLarge(): android.graphics.Bitmap =
            warningBitmapLarge
                    ?: android.graphics.BitmapFactory.decodeStream(
                                    context.assets.open("warning_white_40.png")
                            )
                            .also { warningBitmapLarge = it }

    private val renderWarningSmall: () -> Unit = {
        try {
            activeLookService
                    .getConnectedGlasses()
                    ?.imgStream(
                            getWarningBitmapSmall(),
                            com.activelook.activelooksdk.types.ImgStreamFormat.MONO_4BPP_HEATSHRINK,
                            30,
                            25
                    )
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
            activeLookService
                    .getConnectedGlasses()
                    ?.imgStream(
                            getWarningBitmapLarge(),
                            com.activelook.activelooksdk.types.ImgStreamFormat.MONO_4BPP_HEATSHRINK,
                            30,
                            25
                    )
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

    private val radarWarningController =
            RadarWarningController(
                    renderSmall = renderWarningSmall,
                    eraseSmall = eraseWarningSmall,
                    renderLarge = renderWarningLarge,
                    eraseLarge = eraseWarningLarge
            )

    /** Enable or disable the radar warning overlay. */
    fun setRadarWarningEnabled(enabled: Boolean) {
        radarWarningEnabled = enabled
        radarWarningController.setEnabled(enabled)
        Log.d(TAG, "Radar warning: ${if (enabled) "enabled" else "disabled"}")
    }

    /** Trigger a simulated radar warning (debug/test use only). */
    fun simulateRadarWarning(threatLevel: Int, closestRangeM: Float?) {
        radarWarningController.onRadarUpdate(threatLevel, closestRangeM)
    }

    /** Directly render the large radar warning icon — bypasses TTA logic (debug/test use only). */
    fun renderRadarWarningLargeNow() {
        renderWarningLarge()
    }

    /** Enable or disable the glasses battery. */
    fun setBatteryDisplayEnabled(enabled: Boolean) {
        layoutService.batteryDisplayEnabled = enabled
        layoutService.updateBatteryDisplay(if (enabled) currentBatteryLevel else -1)
        Log.d(TAG, "Battery overlay: ${if (enabled) "enabled" else "disabled"}")
    }

    private var currentBatteryLevel: Int = -1

    private fun observeGlassesBattery() {
        scope.launch {
            activeLookService.glassesBatteryLevel.collect { level ->
                currentBatteryLevel = level
                layoutService.updateBatteryDisplay(level)
            }
        }
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

    // ── General/HR/Power/Energy/Speed/Cadence/Elevation/Lap/Shifting/Navigation/eBike
    // ── metric observers → KarooActiveLookBridgeMetrics.kt ───────────────────────────

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
    internal fun flushToGlasses() {
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
            if ((field.visualizationType
                            ?: VisualizationType.TEXT) ==
                            com.kema.k2look.model.VisualizationType.ZONED_BAR &&
                            field.zonedBar == null
            ) {
                Log.w(
                        TAG,
                        "ZONED_BAR field has null zonedBar model: metric=${field.dataField.name} id=${field.dataField.id} zone=${field.zoneId}. Re-save this field in builder."
                )
            }
            when (field.visualizationType ?: VisualizationType.TEXT) {
                com.kema.k2look.model.VisualizationType.TEXT -> fields[field.zoneId] = value
                else -> updateVisualization(field, value, screen.id)
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

    // startSimulator() / stopSimulator() → KarooActiveLookBridgeSimulator.kt

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
    private fun updateVisualization(
            field: com.kema.k2look.model.LayoutDataField,
            value: String,
            screenId: Int
    ) {
        when (field.visualizationType ?: VisualizationType.TEXT) {
            com.kema.k2look.model.VisualizationType.GAUGE -> updateGauge(field, value)
            com.kema.k2look.model.VisualizationType.BAR -> updateProgressBar(field, value, screenId)
            com.kema.k2look.model.VisualizationType.ZONED_BAR -> updateZonedBar(field, screenId)
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
    private fun updateProgressBar(
            field: com.kema.k2look.model.LayoutDataField,
            value: String,
            screenId: Int
    ) {
        val bar = field.progressBar ?: return

        // Parse numeric value
        val numericValue = parseNumericValue(value)
        if (numericValue == null) {
            Log.d(TAG, "Cannot update bar: non-numeric value '$value'")
            return
        }

        // Render bar at the field's actual template zone position
        scope.launch { layoutService.displayBarAtZone(bar, numericValue, field.zoneId, screenId) }

        Log.d(TAG, "Bar ${bar.id}: ${field.dataField.name} = $numericValue")
    }

    /** Update zoned progress bar with current metric value */
    private fun updateZonedBar(field: com.kema.k2look.model.LayoutDataField, screenId: Int) {
        val zonedBar =
                field.zonedBar
                        ?: run {
                            Log.w(
                                    TAG,
                                    "Skipping zoned render: zonedBar is null for metric=${field.dataField.name} id=${field.dataField.id} zone=${field.zoneId}"
                            )
                            return
                        }

        val sourceMetricId = resolveZonedBarSourceMetricId(field, zonedBar)
        val sourceValue = currentData.valueFor(sourceMetricId)
        val activeZoneIndex = resolveZonedBarActiveZoneIndex(field, sourceValue)
        val overlayValue = resolveZonedBarOverlayValue(field)

        // Parse numeric value
        val numericValue = parseNumericValue(sourceValue)
        if (numericValue == null) {
            Log.d(
                    TAG,
                    "Cannot update zoned bar: non-numeric value '$sourceValue' for metric $sourceMetricId (field=${field.dataField.name} id=${field.dataField.id} zone=${field.zoneId})"
            )
            return
        }

        // Render zone circles at the field's actual template zone position.
        // Respect showIcon so toggling the icon off also removes it from zone circles.
        val iconId =
                if (field.showIcon && sourceMetricId != 4 && sourceMetricId != 47)
                        field.dataField.icon28
                else null
        scope.launch {
            layoutService.displayZoneCircles(
                    zonedBar,
                    numericValue,
                    field.zoneId,
                    screenId,
                    iconId,
                    sourceMetricId,
                    activeZoneIndex,
                    overlayValue
            )
        }

        val currentZone =
                if (activeZoneIndex != null) zonedBar.zones.getOrNull(activeZoneIndex - 1)
                else zonedBar.findZone(numericValue)
        Log.d(
                TAG,
                "Zoned bar ${zonedBar.bar.id}: ${field.dataField.name} source=$sourceMetricId = $sourceValue active=${currentZone?.name ?: "?"} overlay=$overlayValue"
        )
    }

    private fun resolveZonedBarActiveZoneIndex(
            field: com.kema.k2look.model.LayoutDataField,
            sourceValue: String
    ): Int? {
        return when (field.dataField.id) {
            47 -> sourceValue.removePrefix("Z").toIntOrNull()
            48 -> sourceValue.removePrefix("Z").toIntOrNull()
            else -> null
        }
    }

    private fun resolveZonedBarOverlayValue(field: com.kema.k2look.model.LayoutDataField): String {
        return when (field.dataField.id) {
            47 -> currentData.heartRate
            48 -> currentData.power
            else -> currentData.valueFor(field.dataField.id)
        }
    }

    private fun resolveZonedBarSourceMetricId(
            field: com.kema.k2look.model.LayoutDataField,
            zonedBar: com.kema.k2look.model.ZonedProgressBar
    ): Int {
        val fieldId = field.dataField.id
        val barId = zonedBar.bar.dataField.id
        return when {
            fieldId == 47 || barId == 47 -> 47
            fieldId == 48 || barId == 48 -> 48
            barId > 0 -> barId
            else -> fieldId
        }
    }

    /** Parse numeric value from display string (removes units, handles special cases) */
    private fun parseNumericValue(value: String): Float? {
        return when {
            value == "--" || value == "..." || value == "n/a" -> null
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

internal enum class ScanStartAction {
    START_NOW,
    WAIT_FOR_KAROO,
    NO_OP_ALREADY_PENDING,
}

internal fun resolveScanStartAction(
        karooConnected: Boolean,
        pendingScanUntilKarooReady: Boolean
): ScanStartAction {
    if (karooConnected) return ScanStartAction.START_NOW
    return if (pendingScanUntilKarooReady) {
        ScanStartAction.NO_OP_ALREADY_PENDING
    } else {
        ScanStartAction.WAIT_FOR_KAROO
    }
}
