package com.kema.k2look.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.activelook.activelooksdk.DiscoveredGlasses
import com.kema.k2look.service.ActiveLookService
import com.kema.k2look.service.KarooActiveLookBridge
import com.kema.k2look.service.KarooDataService
import com.kema.k2look.util.PreferencesManager
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing Karoo data, ActiveLook connection, and UI state
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val bridge = KarooActiveLookBridge(application)
    private val karooDataService = bridge.getKarooDataService()
    private val activeLookService = bridge.getActiveLookService()

    // Public access to preferences for UI
    val preferencesManager = PreferencesManager(application)

    // Reference to LayoutBuilderViewModel for gesture actions
    private var layoutBuilderViewModel: LayoutBuilderViewModel? = null

    /**
     * Set the LayoutBuilderViewModel instance for gesture screen cycling
     */
    fun setLayoutBuilderViewModel(viewModel: LayoutBuilderViewModel) {
        this.layoutBuilderViewModel = viewModel
        Log.i(TAG, "LayoutBuilderViewModel reference set for gesture actions")
    }

    /**
     * Get the bridge for use by other components (e.g., LayoutBuilderViewModel)
     */
    fun getBridge(): KarooActiveLookBridge = bridge

    /**
     * Get the layout service for Phase 4.2 operations
     */
    fun getLayoutService() = bridge.getLayoutService()

    /**
     * Find a profile by name (for auto-switching based on Karoo profile)
     * This will be called by the bridge, which gets the callback from LayoutBuilderViewModel
     */
    fun getProfileByName(name: String): com.kema.k2look.model.DataFieldProfile? {
        // This is a placeholder - actual lookup happens in LayoutBuilderViewModel
        // The bridge will get the callback directly from LayoutBuilderViewModel
        return null
    }

    // UI State
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    data class UiState(
        val connectionState: KarooDataService.ConnectionState = KarooDataService.ConnectionState.Disconnected,
        val bridgeState: KarooActiveLookBridge.BridgeState = KarooActiveLookBridge.BridgeState.Idle,
        val activeLookState: ActiveLookService.ConnectionState = ActiveLookService.ConnectionState.Disconnected,
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
        val gestureAction: com.kema.k2look.model.GestureAction = com.kema.k2look.model.GestureAction.CYCLE_SCREENS,
        val touchAction: com.kema.k2look.model.TouchAction = com.kema.k2look.model.TouchAction.SHOW_HIDE_DISPLAY,
        // Forget glasses warning dialog
        val showForgetWarningDialog: Boolean = false,
    )

    private var simulatorJob: kotlinx.coroutines.Job? = null

    private val gesturePreferences = com.kema.k2look.data.GesturePreferencesRepository(application)

    init {
        Log.i(TAG, "MainViewModel initialized")
        bridge.initialize()
        observeKarooData()
        observeActiveLookData()
        observeBridgeState()
        observeUserProfile()
        loadReconnectTimeout()
        observeGestureEvents()
        observeGesturePreferences()
    }

    /**
     * Connect to Karoo System
     */
    fun connectKaroo() {
        Log.i(TAG, "User requested connection to Karoo")
        bridge.connectKaroo()
    }

    /**
     * Disconnect from Karoo System
     */
    fun disconnectKaroo() {
        Log.i(TAG, "User requested disconnect from Karoo")
        bridge.disconnectKaroo()
    }

    /**
     * Start scanning for ActiveLook glasses
     */
    fun startActiveLookScan() {
        Log.i(TAG, "User requested ActiveLook scan")
        bridge.startActiveLookScan()
    }

    /**
     * Start scanning for glasses (alias for startActiveLookScan)
     */
    fun startGlassesScan() {
        startActiveLookScan()
    }

    /**
     * Stop scanning for ActiveLook glasses
     */
    fun stopActiveLookScan() {
        Log.i(TAG, "User requested stop ActiveLook scan")
        bridge.stopActiveLookScan()
    }

    /**
     * Connect to ActiveLook glasses
     */
    fun connectActiveLook(glasses: DiscoveredGlasses) {
        Log.i(TAG, "User requested connection to ActiveLook glasses: ${glasses.name}")
        bridge.connectActiveLook(glasses)
    }

    /**
     * Disconnect from ActiveLook glasses
     */
    fun disconnectActiveLook() {
        Log.i(TAG, "User requested disconnect from ActiveLook")
        bridge.disconnectActiveLook()
    }

    /**
     * Set auto-connect to glasses on startup
     */
    fun setAutoConnectGlasses(enabled: Boolean) {
        Log.i(TAG, "Setting auto-connect glasses to: $enabled")
        preferencesManager.setAutoConnectActiveLook(enabled)
    }

    /**
     * Set disconnect glasses when ride ends (idle state)
     */
    fun setDisconnectWhenIdle(enabled: Boolean) {
        Log.i(TAG, "Setting disconnect when idle to: $enabled")
        preferencesManager.setDisconnectWhenIdle(enabled)
    }

    /**
     * Forget saved glasses and disconnect if connected
     * Best practice: Clean up resources (layouts, gauges) before disconnecting
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
     * Force forget glasses without cleanup (when not connected)
     * Resources (layouts, gauges) will remain on glasses
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

    /**
     * Dismiss forget glasses warning dialog
     */
    fun dismissForgetWarning() {
        Log.d(TAG, "Dismissed forget glasses warning")
        _uiState.value = _uiState.value.copy(showForgetWarningDialog = false)
    }

    /**
     * Load reconnect timeout from preferences
     */
    private fun loadReconnectTimeout() {
        val timeout = preferencesManager.getReconnectTimeoutMinutes()
        _uiState.value = _uiState.value.copy(reconnectTimeoutMinutes = timeout)
        Log.d(TAG, "Loaded reconnect timeout: ${timeout}min")
    }

    /**
     * Observe gesture/touch events from ActiveLook service
     */
    private fun observeGestureEvents() {
        viewModelScope.launch {
            activeLookService.gestureEvents.collect { count ->
                Log.i(TAG, "🖐️ Gesture event #$count received")
                _uiState.value = _uiState.value.copy(gestureEventCount = count)

                // Execute the configured gesture action
                executeGestureAction(_uiState.value.gestureAction)
            }
        }

        viewModelScope.launch {
            activeLookService.touchEvents.collect { count ->
                Log.i(TAG, "👆 Touch event #$count received")
                _uiState.value = _uiState.value.copy(touchEventCount = count)

                // Execute the configured touch action
                executeTouchAction(_uiState.value.touchAction)
            }
        }
    }

    /**
     * Observe gesture/touch action preferences
     */
    private fun observeGesturePreferences() {
        viewModelScope.launch {
            gesturePreferences.gestureAction.collect { action ->
                Log.d(TAG, "Gesture action preference changed: ${action.displayName}")
                _uiState.value = _uiState.value.copy(gestureAction = action)
            }
        }
        viewModelScope.launch {
            gesturePreferences.touchAction.collect { action ->
                Log.d(TAG, "Touch action preference changed: ${action.displayName}")
                _uiState.value = _uiState.value.copy(touchAction = action)
            }
        }
    }

    /**
     * Execute the configured gesture action
     */
    private fun executeGestureAction(action: com.kema.k2look.model.GestureAction) {
        Log.i(TAG, "🖐️ Executing gesture action: ${action.displayName}")

        when (action) {
            com.kema.k2look.model.GestureAction.CYCLE_SCREENS -> {
                cycleToNextScreen()
            }

            com.kema.k2look.model.GestureAction.ADJUST_BRIGHTNESS -> {
                adjustBrightness()
            }

            com.kema.k2look.model.GestureAction.TOGGLE_DISPLAY -> {
                toggleDisplay()
            }
        }
    }

    /**
     * Execute the configured touch action
     */
    private fun executeTouchAction(action: com.kema.k2look.model.TouchAction) {
        Log.i(TAG, "👆 Executing touch action: ${action.displayName}")

        when (action) {
            com.kema.k2look.model.TouchAction.SHOW_HIDE_DISPLAY -> {
                toggleDisplay()
            }

            com.kema.k2look.model.TouchAction.CYCLE_SCREENS -> {
                cycleToNextScreen()
            }

            com.kema.k2look.model.TouchAction.ADJUST_BRIGHTNESS -> {
                adjustBrightness()
            }
        }
    }

    // Current brightness level (0-15)
    private var currentBrightness = 8 // Default mid-level

    // Display power state
    private var displayPowerOn = true

    /**
     * Cycle to the next screen in the current profile
     */
    private fun cycleToNextScreen() {
        val layoutViewModel = layoutBuilderViewModel
        if (layoutViewModel == null) {
            Log.w(TAG, "Cannot cycle screens - LayoutBuilderViewModel not set")
            return
        }

        val success = layoutViewModel.cycleToNextScreen()
        if (!success) {
            Log.d(TAG, "Screen cycling not performed (single screen or no profile)")
        }
    }

    /**
     * Cycle to the next profile (REMOVED - not useful during rides)
     */
    private fun cycleToNextProfile() {
        // Removed - cycling K2Look profiles during a ride doesn't make sense
        Log.d(TAG, "Profile cycling removed - not needed during rides")
    }

    /**
     * Adjust brightness (cycle through levels: 8 -> 12 -> 15 -> 4 -> 8)
     * Using common brightness levels for cycling
     */
    private fun adjustBrightness() {
        viewModelScope.launch {
            try {
                // Cycle through useful brightness levels
                currentBrightness = when (currentBrightness) {
                    in 0..7 -> 8    // Low -> Medium
                    8 -> 12          // Medium -> High
                    in 9..12 -> 15   // High -> Max
                    in 13..15 -> 4   // Max -> Low
                    else -> 8        // Default to medium
                }

                Log.i(TAG, "✓ Adjusting brightness to level $currentBrightness (0=min, 15=max)")

                // Send luma command to glasses (command 0x10)
                val activeLookService = bridge.getActiveLookService()
                activeLookService.setLuminance(currentBrightness)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to adjust brightness: ${e.message}", e)
            }
        }
    }

    /**
     * Toggle display on/off
     */
    private fun toggleDisplay() {
        viewModelScope.launch {
            try {
                displayPowerOn = !displayPowerOn

                Log.i(TAG, "✓ Toggling display ${if (displayPowerOn) "ON" else "OFF"}")

                // Send display power command to glasses (command 0x00)
                val activeLookService = bridge.getActiveLookService()
                activeLookService.setDisplayPower(displayPowerOn)

                if (displayPowerOn) {
                    // TODO: Redraw current layout when turning back on
                    // Will need bridge method to refresh display
                    Log.d(TAG, "Display turned on - layout refresh needed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle display: ${e.message}", e)
            }
        }
    }

    /**
     * Set gesture action preference
     */
    fun setGestureAction(action: com.kema.k2look.model.GestureAction) {
        Log.i(TAG, "Setting gesture action to: ${action.displayName}")
        gesturePreferences.setGestureAction(action)
    }

    /**
     * Set touch action preference
     */
    fun setTouchAction(action: com.kema.k2look.model.TouchAction) {
        Log.i(TAG, "Setting touch action to: ${action.displayName}")
        gesturePreferences.setTouchAction(action)
    }

    /**
     * Update reconnect timeout
     */
    fun setReconnectTimeout(minutes: Int) {
        if (minutes < 1 || minutes > 60) {
            Log.w(TAG, "Invalid reconnect timeout: $minutes (must be 1-60)")
            return
        }

        Log.i(TAG, "Setting reconnect timeout to ${minutes}min")
        preferencesManager.setReconnectTimeoutMinutes(minutes)
        _uiState.value = _uiState.value.copy(reconnectTimeoutMinutes = minutes)
    }

    /**
     * Toggle debug mode on/off
     */
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

    /**
     * Start debug logging to file
     */
    private fun startDebugLogging() {
        // TODO: Implement file logging
        Log.i(TAG, "Debug logging started - logs will be written to /sdcard/k2look_debug.log")
    }

    /**
     * Stop debug logging
     */
    private fun stopDebugLogging() {
        Log.i(TAG, "Debug logging stopped")
    }

    /**
     * Start simulator - sends test data to glasses
     */
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
        simulatorJob = viewModelScope.launch {
            var counter = 0
            Log.i(TAG, "🔁 Simulator UI update loop starting")
            while (_uiState.value.debugModeEnabled) {
                counter++
                _uiState.value = _uiState.value.copy(
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

    /**
     * Stop simulator
     */
    fun stopSimulator() {
        Log.i(TAG, "Stopping simulator...")
        simulatorJob?.cancel()
        simulatorJob = null
        bridge.stopSimulator()
    }

    /**
     * Format simulated time
     */
    private fun formatSimulatedTime(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }

    /**
     * Observe bridge state
     */
    private fun observeBridgeState() {
        viewModelScope.launch {
            bridge.bridgeState.collect { state ->
                Log.d(TAG, "Bridge state changed: $state")
                _uiState.value = _uiState.value.copy(bridgeState = state)
            }
        }
    }

    /**
     * Observe ActiveLook data
     */
    private fun observeActiveLookData() {
        // Observe connection state
        viewModelScope.launch {
            activeLookService.connectionState.collect { state ->
                Log.d(TAG, "ActiveLook connection state changed: $state")
                _uiState.value = _uiState.value.copy(activeLookState = state)
            }
        }

        // Observe discovered glasses
        viewModelScope.launch {
            activeLookService.discoveredGlasses.collect { glasses ->
                Log.d(TAG, "Discovered glasses updated: ${glasses.size} devices")
                _uiState.value = _uiState.value.copy(discoveredGlasses = glasses)
            }
        }

        // Observe scanning state
        viewModelScope.launch {
            activeLookService.isScanning.collect { scanning ->
                Log.d(TAG, "Scanning state: $scanning")
                _uiState.value = _uiState.value.copy(isScanning = scanning)
            }
        }
    }

    /**
     * Observe user profile preferences from Karoo (including unit system)
     */
    private fun observeUserProfile() {
        viewModelScope.launch {
            karooDataService.getKarooSystem().addConsumer<UserProfile> { profile ->
                Log.d(
                    TAG,
                    "User profile updated: distance=${profile.preferredUnit.distance}, elevation=${profile.preferredUnit.elevation}"
                )
                val useImperial =
                    profile.preferredUnit.distance == UserProfile.PreferredUnit.UnitType.IMPERIAL
                _uiState.value = _uiState.value.copy(
                    userProfile = profile,
                    useImperialUnits = useImperial
                )
            }
        }
    }

    /**
     * Observe data from KarooDataService and update UI state
     */
    private fun observeKarooData() {
        // Observe connection state
        viewModelScope.launch {
            karooDataService.connectionState.collect { state ->
                Log.d(TAG, "Connection state changed: $state")
                _uiState.value = _uiState.value.copy(connectionState = state)
            }
        }

        // Observe ride state
        viewModelScope.launch {
            karooDataService.rideState.collect { state ->
                Log.d(TAG, "Ride state changed: $state")
                _uiState.value = _uiState.value.copy(rideState = state)
            }
        }

        // Observe speed data
        viewModelScope.launch {
            karooDataService.speedData.collect { streamState ->
                val speedStr = formatStreamData(streamState, "km/h")
                _uiState.value = _uiState.value.copy(speed = speedStr)
            }
        }

        // Observe average speed data
        viewModelScope.launch {
            karooDataService.averageSpeedData.collect { streamState ->
                val avgSpeedStr = formatStreamData(streamState, "km/h")
                _uiState.value = _uiState.value.copy(avgSpeed = avgSpeedStr)
            }
        }

        // Observe max speed data
        viewModelScope.launch {
            karooDataService.maxSpeedData.collect { streamState ->
                val maxSpeedStr = formatStreamData(streamState, "km/h")
                _uiState.value = _uiState.value.copy(maxSpeed = maxSpeedStr)
            }
        }

        // Observe heart rate data
        viewModelScope.launch {
            karooDataService.heartRateData.collect { streamState ->
                val hrStr = formatStreamData(streamState, "bpm")
                _uiState.value = _uiState.value.copy(heartRate = hrStr)
            }
        }

        // Observe average heart rate data
        viewModelScope.launch {
            karooDataService.averageHeartRateData.collect { streamState ->
                val avgHrStr = formatStreamData(streamState, "bpm")
                _uiState.value = _uiState.value.copy(avgHeartRate = avgHrStr)
            }
        }

        // Observe max heart rate data
        viewModelScope.launch {
            karooDataService.maxHeartRateData.collect { streamState ->
                val maxHrStr = formatStreamData(streamState, "bpm")
                _uiState.value = _uiState.value.copy(maxHeartRate = maxHrStr)
            }
        }

        // Observe cadence data
        viewModelScope.launch {
            karooDataService.cadenceData.collect { streamState ->
                val cadenceStr = formatStreamData(streamState, "rpm")
                _uiState.value = _uiState.value.copy(cadence = cadenceStr)
            }
        }

        // Observe average cadence data
        viewModelScope.launch {
            karooDataService.averageCadenceData.collect { streamState ->
                val avgCadenceStr = formatStreamData(streamState, "rpm")
                _uiState.value = _uiState.value.copy(avgCadence = avgCadenceStr)
            }
        }

        // Observe max cadence data
        viewModelScope.launch {
            karooDataService.maxCadenceData.collect { streamState ->
                val maxCadenceStr = formatStreamData(streamState, "rpm")
                _uiState.value = _uiState.value.copy(maxCadence = maxCadenceStr)
            }
        }

        // Observe power data
        viewModelScope.launch {
            karooDataService.powerData.collect { streamState ->
                val powerStr = formatStreamData(streamState, "w")
                _uiState.value = _uiState.value.copy(power = powerStr)
            }
        }

        // Observe average power data
        viewModelScope.launch {
            karooDataService.averagePowerData.collect { streamState ->
                val avgPowerStr = formatStreamData(streamState, "w")
                _uiState.value = _uiState.value.copy(avgPower = avgPowerStr)
            }
        }

        // Observe max power data
        viewModelScope.launch {
            karooDataService.maxPowerData.collect { streamState ->
                val maxPowerStr = formatStreamData(streamState, "w")
                _uiState.value = _uiState.value.copy(maxPower = maxPowerStr)
            }
        }

        // Observe distance data
        viewModelScope.launch {
            karooDataService.distanceData.collect { streamState ->
                val distanceStr = formatStreamData(streamState, "km")
                _uiState.value = _uiState.value.copy(distance = distanceStr)
            }
        }

        // Observe time data
        viewModelScope.launch {
            karooDataService.timeData.collect { streamState ->
                val timeStr = formatTimeData(streamState)
                _uiState.value = _uiState.value.copy(time = timeStr)
            }
        }

        // Observe HR zone data
        viewModelScope.launch {
            karooDataService.hrZoneData.collect { streamState ->
                val zoneStr = when (streamState) {
                    is StreamState.Streaming -> {
                        val zoneValue = streamState.dataPoint.singleValue?.toInt()
                        if (zoneValue != null && zoneValue > 0) "Z$zoneValue" else "--"
                    }

                    else -> "--"
                }
                _uiState.value = _uiState.value.copy(hrZone = zoneStr)
            }
        }

        // Observe 3s power data
        viewModelScope.launch {
            karooDataService.smoothed3sPowerData.collect { streamState ->
                val power3sStr = formatStreamData(streamState, "w")
                _uiState.value = _uiState.value.copy(power3s = power3sStr)
            }
        }

        // Observe 10s power data
        viewModelScope.launch {
            karooDataService.smoothed10sPowerData.collect { streamState ->
                val power10sStr = formatStreamData(streamState, "w")
                _uiState.value = _uiState.value.copy(power10s = power10sStr)
            }
        }

        // Observe 30s power data
        viewModelScope.launch {
            karooDataService.smoothed30sPowerData.collect { streamState ->
                val power30sStr = formatStreamData(streamState, "w")
                _uiState.value = _uiState.value.copy(power30s = power30sStr)
            }
        }

        // Observe VAM data
        viewModelScope.launch {
            karooDataService.vamData.collect { streamState ->
                val vamStr = formatStreamData(streamState, "m/h")
                _uiState.value = _uiState.value.copy(vam = vamStr)
            }
        }

        // Observe average VAM data
        viewModelScope.launch {
            karooDataService.avgVamData.collect { streamState ->
                val avgVamStr = formatStreamData(streamState, "m/h")
                _uiState.value = _uiState.value.copy(avgVam = avgVamStr)
            }
        }
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

            is StreamState.Searching -> "Searching..."
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
            else -> "%.2f".format(value)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.i(TAG, "MainViewModel cleared, cleaning up bridge")
        bridge.cleanup()
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}

