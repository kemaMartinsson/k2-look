package com.kema.k2look.viewmodel

// DisplayDebugService extension functions (split from DisplayDebugService.kt)
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.activelook.activelooksdk.DiscoveredGlasses
import com.kema.k2look.K2LookApplication
import com.kema.k2look.service.ActiveLookService
import com.kema.k2look.service.DisplayDebugService
import com.kema.k2look.service.KarooActiveLookBridge
import com.kema.k2look.service.KarooDataService
import com.kema.k2look.service.deleteGauge
import com.kema.k2look.service.startSimulator
import com.kema.k2look.service.stopSimulator
import com.kema.k2look.service.testClippingSemantics
import com.kema.k2look.service.testDisplayBounds
import com.kema.k2look.service.testDynamicLayout
import com.kema.k2look.service.testExtraCommands
import com.kema.k2look.service.testGauge270
import com.kema.k2look.service.testIconValueUnit
import com.kema.k2look.service.testK2LookVsOfficial
import com.kema.k2look.service.testProductionLayout
import com.kema.k2look.service.testRealisticLayout
import com.kema.k2look.service.testTextRotations
import com.kema.k2look.service.testTextXPosition
import com.kema.k2look.service.testThreeFieldLayout
import com.kema.k2look.service.testZoneBar
import com.kema.k2look.util.PreferencesManager
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** ViewModel for managing Karoo data, ActiveLook connection, and UI state */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    // Bridge is owned by K2LookApplication — shared with K2LookExtension.
    // The UI observes the live connection; closing the UI never tears it down.
    internal val bridge = (application as K2LookApplication).bridge
    internal val karooDataService = bridge.getKarooDataService()
    internal val activeLookService = bridge.getActiveLookService()

    /** Display debug / calibration test patterns on glasses */
    val displayDebug = DisplayDebugService(activeLookService)

    // Public access to preferences for UI
    val preferencesManager = PreferencesManager(application)

    // Reference to LayoutBuilderViewModel for gesture actions
    internal var layoutBuilderViewModel: LayoutBuilderViewModel? = null

    /** Set the LayoutBuilderViewModel instance for gesture screen cycling */
    fun setLayoutBuilderViewModel(viewModel: LayoutBuilderViewModel) {
        this.layoutBuilderViewModel = viewModel
        Log.i(TAG, "LayoutBuilderViewModel reference set for gesture actions")
    }

    /** Get the bridge for use by other components (e.g., LayoutBuilderViewModel) */
    fun getBridge(): KarooActiveLookBridge = bridge

    /** Get the layout service for Phase 4.2 operations */
    fun getLayoutService() = bridge.getLayoutService()

    /**
     * Find a profile by name (for auto-switching based on Karoo profile) This will be called by the
     * bridge, which gets the callback from LayoutBuilderViewModel
     */
    fun getProfileByName(name: String): com.kema.k2look.model.DataFieldProfile? {
        // This is a placeholder - actual lookup happens in LayoutBuilderViewModel
        // The bridge will get the callback directly from LayoutBuilderViewModel
        return null
    }

    // UI State
    internal val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    data class UiState(
            val connectionState: KarooDataService.ConnectionState =
                    KarooDataService.ConnectionState.Disconnected,
            val bridgeState: KarooActiveLookBridge.BridgeState =
                    KarooActiveLookBridge.BridgeState.Idle,
            val activeLookState: ActiveLookService.ConnectionState =
                    ActiveLookService.ConnectionState.Disconnected,
            val discoveredGlasses: List<DiscoveredGlasses> = emptyList(),
            val isScanning: Boolean = false,
            val rideState: RideState = RideState.Idle,
            val userProfile: UserProfile? = null,
            val useImperialUnits: Boolean = false,
            val reconnectTimeoutMinutes: Int = 10, // Default 10 minutes
            val debugModeEnabled: Boolean = false,
            val speed: String = "--",
            val avgSpeed: String = "--",
            val maxSpeed: String = "--",
            val heartRate: String = "--",
            val avgHeartRate: String = "--",
            val maxHeartRate: String = "--",
            val cadence: String = "--",
            val avgCadence: String = "--",
            val maxCadence: String = "--",
            val power: String = "--",
            val avgPower: String = "--",
            val maxPower: String = "--",
            val distance: String = "--",
            val time: String = "--",
            // Advanced metrics
            val hrZone: String = "--",
            val power3s: String = "--",
            val power10s: String = "--",
            val power30s: String = "--",
            val vam: String = "--",
            val avgVam: String = "--",
            // Gesture/Touch events
            val gestureEventCount: Int = 0,
            val touchEventCount: Int = 0,
            val gestureAction: com.kema.k2look.model.GestureAction =
                    com.kema.k2look.model.GestureAction.CYCLE_SCREENS,
            val touchAction: com.kema.k2look.model.TouchAction =
                    com.kema.k2look.model.TouchAction.SHOW_HIDE_DISPLAY,
            val gestureEnabled: Boolean = true,
            val touchEnabled: Boolean = true,
            // Forget glasses warning dialog
            val showForgetWarningDialog: Boolean = false,
    )

    private var simulatorJob: kotlinx.coroutines.Job? = null

    // Owned here; accessed by MainViewModelGestureHandlers.kt extension functions
    internal var currentBrightness = 8 // 0-15, default mid-level
    internal var displayPowerOn = true

    internal val gesturePreferences = com.kema.k2look.data.GesturePreferencesRepository(application)

    init {
        Log.i(TAG, "MainViewModel initialized — attaching to application-scoped bridge")
        // Bridge already initialized by K2LookApplication — do NOT call bridge.initialize() here.
        observeKarooData()
        observeActiveLookData()
        observeBridgeState()
        observeUserProfile()
        loadReconnectTimeout()
        observeGestureEvents()
        observeGesturePreferences()
    }

    /** Connect to Karoo System */
    fun connectKaroo() {
        Log.i(TAG, "User requested connection to Karoo")
        bridge.connectKaroo()
    }

    /** Disconnect from Karoo System */
    fun disconnectKaroo() {
        Log.i(TAG, "User requested disconnect from Karoo")
        bridge.disconnectKaroo()
    }

    /** Start scanning for ActiveLook glasses */
    fun startActiveLookScan() {
        Log.i(TAG, "User requested ActiveLook scan")
        bridge.startActiveLookScan()
    }

    /** Start scanning for glasses (alias for startActiveLookScan) */
    fun startGlassesScan() {
        startActiveLookScan()
    }

    /** Stop scanning for ActiveLook glasses */
    fun stopActiveLookScan() {
        Log.i(TAG, "User requested stop ActiveLook scan")
        bridge.stopActiveLookScan()
    }

    /** Connect to ActiveLook glasses */
    fun connectActiveLook(glasses: DiscoveredGlasses) {
        Log.i(TAG, "User requested connection to ActiveLook glasses: ${glasses.name}")
        bridge.connectActiveLook(glasses)
    }

    /** Disconnect from ActiveLook glasses */
    fun disconnectActiveLook() {
        Log.i(TAG, "User requested disconnect from ActiveLook")
        bridge.disconnectActiveLook()
    }

    /** Set auto-connect to glasses on startup */
    fun setAutoConnectGlasses(enabled: Boolean) {
        Log.i(TAG, "Setting auto-connect glasses to: $enabled")
        preferencesManager.setAutoConnectActiveLook(enabled)
    }

    /** Set disconnect glasses when ride ends (idle state) */
    fun setDisconnectWhenIdle(enabled: Boolean) {
        Log.i(TAG, "Setting disconnect when idle to: $enabled")
        preferencesManager.setDisconnectWhenIdle(enabled)
    }

    /**
     * Forget saved glasses and disconnect if connected Best practice: Clean up resources (layouts,
     * gauges) before disconnecting
     */
    fun forgetGlasses() {
        Log.i(TAG, "Forgetting saved glasses")

        val isConnected = activeLookService.isConnected

        if (isConnected) {
            // Connected: Clean up resources before disconnecting
            viewModelScope.launch {
                try {
                    Log.i(TAG, "Cleaning up resources on glasses before disconnect...")

                    // Delete all layouts (using 0xFF for all)
                    bridge.getLayoutService().clearLayouts()

                    // Delete all gauges (using 0xFF for all)
                    activeLookService.deleteGauge(0xFF)

                    Log.i(TAG, "✓ Resources cleaned from glasses")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clean resources: ${e.message}", e)
                } finally {
                    // Always disconnect and clear preferences
                    bridge.disconnectActiveLook()
                    preferencesManager.clearLastConnectedGlasses()
                    Log.i(TAG, "Saved glasses cleared")
                }
            }
        } else {
            // Not connected: Show warning dialog
            Log.w(TAG, "Glasses not connected - cannot clean resources, showing warning")
            _uiState.value = _uiState.value.copy(showForgetWarningDialog = true)
        }
    }

    /**
     * Force forget glasses without cleanup (when not connected) Resources (layouts, gauges) will
     * remain on glasses
     */
    fun forceForgetGlasses() {
        Log.i(TAG, "Force forgetting glasses (not connected, resources may remain on glasses)")

        // Disconnect if somehow connected
        bridge.disconnectActiveLook()

        // Clear saved glasses address
        preferencesManager.clearLastConnectedGlasses()

        // Dismiss warning dialog
        _uiState.value = _uiState.value.copy(showForgetWarningDialog = false)

        Log.i(TAG, "Force forget complete (resources not cleaned)")
    }

    /** Dismiss forget glasses warning dialog */
    fun dismissForgetWarning() {
        Log.d(TAG, "Dismissed forget glasses warning")
        _uiState.value = _uiState.value.copy(showForgetWarningDialog = false)
    }

    /** Load reconnect timeout from preferences */
    private fun loadReconnectTimeout() {
        val timeout = preferencesManager.getReconnectTimeoutMinutes()
        _uiState.value = _uiState.value.copy(reconnectTimeoutMinutes = timeout)
        Log.d(TAG, "Loaded reconnect timeout: ${timeout}min")
    }

    // observeGestureEvents / observeGesturePreferences / executeGestureAction /
    // executeTouchAction / cycleToNextScreen / adjustBrightness / toggleDisplay /
    // setGestureAction / setTouchAction / setGestureEnabled / setTouchEnabled
    // → MainViewModelGestureHandlers.kt as extension functions

    /** Update reconnect timeout */
    fun setReconnectTimeout(minutes: Int) {
        if (minutes < 1 || minutes > 60) {
            Log.w(TAG, "Invalid reconnect timeout: $minutes (must be 1-60)")
            return
        }

        Log.i(TAG, "Setting reconnect timeout to ${minutes}min")
        preferencesManager.setReconnectTimeoutMinutes(minutes)
        _uiState.value = _uiState.value.copy(reconnectTimeoutMinutes = minutes)
    }

    /** Toggle debug mode on/off */
    fun setDebugMode(enabled: Boolean) {
        Log.i(TAG, "Debug mode ${if (enabled) "enabled" else "disabled"}")
        _uiState.value = _uiState.value.copy(debugModeEnabled = enabled)

        if (enabled) {
            startDebugLogging()
        } else {
            stopDebugLogging()

            // Ensure simulator doesn't keep running if user disables debug mode.
            stopSimulator()
        }
    }

    /** Start debug logging to file */
    private fun startDebugLogging() {
        // TODO: Implement file logging
        Log.i(TAG, "Debug logging started - logs will be written to /sdcard/k2look_debug.log")
    }

    /** Stop debug logging */
    private fun stopDebugLogging() {
        Log.i(TAG, "Debug logging stopped")
    }

    /** Start simulator - sends test data to glasses */
    fun startSimulator() {
        Log.i(TAG, "🎮 START SIMULATOR REQUESTED")
        Log.i(TAG, "  Debug mode: ${_uiState.value.debugModeEnabled}")
        Log.i(TAG, "  ActiveLook state: ${_uiState.value.activeLookState}")
        Log.i(TAG, "  Bridge state: ${_uiState.value.bridgeState}")

        if (!_uiState.value.debugModeEnabled) {
            Log.w(TAG, "❌ Simulator requires Debug Mode enabled")
            return
        }

        // Push values to glasses via the bridge.
        Log.i(TAG, "📤 Calling bridge.startSimulator()")
        bridge.startSimulator()

        // Also mirror the same values in the UI for visibility.
        simulatorJob?.cancel()
        simulatorJob =
                viewModelScope.launch {
                    var counter = 0
                    Log.i(TAG, "🔁 Simulator UI update loop starting")
                    while (_uiState.value.debugModeEnabled) {
                        counter++
                        _uiState.value =
                                _uiState.value.copy(
                                        speed = "${20 + (counter % 20)} km/h",
                                        heartRate = "${140 + (counter % 30)} bpm",
                                        cadence = "${80 + (counter % 20)} rpm",
                                        power = "${200 + (counter % 100)} w",
                                        distance = "${counter / 10}.${counter % 10} km",
                                        time = formatSimulatedTime(counter * 2)
                                )
                        if (counter % 5 == 0) {
                            Log.d(
                                    TAG,
                                    "Simulator tick $counter: SPD=${_uiState.value.speed}, HR=${_uiState.value.heartRate}"
                            )
                        }
                        kotlinx.coroutines.delay(2000)
                    }
                    Log.i(TAG, "⏹ Simulator UI update loop stopped")
                }
    }

    /** Stop simulator */
    fun stopSimulator() {
        Log.i(TAG, "Stopping simulator...")
        simulatorJob?.cancel()
        simulatorJob = null
        bridge.stopSimulator()
    }

    /**
     * Run a numbered display debug test (1–6) on the connected glasses. Requires debug mode ON and
     * glasses connected.
     *
     * 1 = Display bounds & coordinate grid 2 = Text anchor / rotation behaviour 3 = Clipping region
     * semantics (width = SIZE vs COORD) 4 = Text X position with rotation 4 5 = K2Look current vs
     * Official ActiveLook params 6 = Full 3-field layout with official positions 7 =
     * LayoutExtraCmd: unit text / bitmap icon / label drawn AFTER main value 8 = icon | value |
     * unit: speed icon (id=26) to the viewer-left of value 9 = imgList: log all stored bitmap ids /
     * dimensions in ALooK config to find correct icon IDs
     */
    fun runDisplayDebugTest(testNumber: Int) {
        if (!_uiState.value.debugModeEnabled) {
            Log.w(TAG, "Display debug requires debug mode")
            return
        }
        if (_uiState.value.activeLookState !is ActiveLookService.ConnectionState.Connected) {
            Log.w(TAG, "Display debug requires glasses connected")
            return
        }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            when (testNumber) {
                1 -> displayDebug.testDisplayBounds()
                2 -> displayDebug.testTextRotations()
                3 -> displayDebug.testClippingSemantics()
                4 -> displayDebug.testTextXPosition()
                5 -> displayDebug.testK2LookVsOfficial()
                6 -> displayDebug.testThreeFieldLayout()
                7 -> displayDebug.testExtraCommands()
                8 -> displayDebug.testIconValueUnit()
                9 -> displayDebug.testRealisticLayout()
                10 -> displayDebug.testDynamicLayout()
                11 -> displayDebug.testGauge270()
                12 -> displayDebug.testProductionLayout()
                13 -> displayDebug.testZoneBar()
                else -> Log.w(TAG, "Unknown debug test: $testNumber")
            }
        }
    }

    /** Clear all debug patterns from the glasses display. */
    fun clearGlassesDisplay() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { displayDebug.clearDisplay() }
    }

    /** Format simulated time */
    private fun formatSimulatedTime(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", h, m, s)
    }

    // observeBridgeState / observeActiveLookData / observeUserProfile / observeKarooData
    // formatStreamData / formatTimeData / formatValue / formatStreamDataInt
    // → MainViewModelDataObservers.kt as extension functions

    override fun onCleared() {
        super.onCleared()
        // Bridge is owned by K2LookApplication — do NOT call cleanup here.
        // The glasses connection stays alive when the user closes the UI.
        Log.i(TAG, "MainViewModel cleared (bridge kept alive in background)")
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}
