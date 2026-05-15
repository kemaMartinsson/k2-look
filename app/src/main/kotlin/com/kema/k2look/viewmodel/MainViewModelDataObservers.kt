package com.kema.k2look.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.kema.k2look.service.ActiveLookService
import com.kema.k2look.service.formatDistanceDataKm
import com.kema.k2look.service.formatSpeedDataKmh
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UserProfile
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Data observation and stream formatting extension functions for [MainViewModel].
 *
 * Covers: Karoo data streams (speed, HR, cadence, power, distance, time, zones), ActiveLook
 * connection state, bridge state, and user profile. Format helpers are file-private — only called
 * from within this file.
 */
private const val TAG = "MainViewModel"

// ── State observers ────────────────────────────────────────────────────────

/** Observe bridge state */
internal fun MainViewModel.observeBridgeState() {
    viewModelScope.launch {
        bridge.bridgeState.collect { state ->
            Log.d(TAG, "Bridge state changed: $state")
            _uiState.value = _uiState.value.copy(bridgeState = state)
        }
    }
}

/** Observe ActiveLook data */
internal fun MainViewModel.observeActiveLookData() {
    // Observe connection state
    viewModelScope.launch {
        activeLookService.connectionState.collect { state ->
            Log.d(TAG, "ActiveLook connection state changed: $state")
            _uiState.value = _uiState.value.copy(activeLookState = state)
            // Re-apply gesture preference on every (re)connect.
            // onConnected always calls enableGestureSensor(true); correct it here if the
            // user has gesture disabled so the preference is respected after reconnects.
            if (state is ActiveLookService.ConnectionState.Connected) {
                val gestureEnabled = gesturePreferences.gestureEnabled.value
                Log.d(
                        TAG,
                        "Re-applying gesture preference on connect: gestureEnabled=$gestureEnabled"
                )
                activeLookService.enableGestureSensor(gestureEnabled)
            }
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

/** Observe user profile preferences from Karoo (including unit system) */
internal fun MainViewModel.observeUserProfile() {
    viewModelScope.launch {
        karooDataService.getKarooSystem().addConsumer<UserProfile> { profile ->
            Log.d(
                    TAG,
                    "User profile updated: distance=${profile.preferredUnit.distance}, elevation=${profile.preferredUnit.elevation}"
            )
            val useImperial =
                    profile.preferredUnit.distance == UserProfile.PreferredUnit.UnitType.IMPERIAL
            _uiState.value =
                    _uiState.value.copy(userProfile = profile, useImperialUnits = useImperial)
        }
    }
}

/** Observe data from KarooDataService and update UI state */
internal fun MainViewModel.observeKarooData() {
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
            val previousRideState = _uiState.value.rideState
            val shouldDisableDebug =
                    shouldDisableDebugModeForRideTransition(
                            previousRideState = previousRideState,
                            newRideState = state,
                            debugModeEnabled = _uiState.value.debugModeEnabled,
                    )

            _uiState.value =
                    _uiState.value.copy(
                            rideState = state,
                            debugModeEnabled =
                                    if (shouldDisableDebug) false
                                    else _uiState.value.debugModeEnabled,
                    )

            if (shouldDisableDebug) {
                Log.w(TAG, "Ride started with debug mode enabled; disabling debug mode")
                setDebugMode(false)
            }
        }
    }

    // Observe speed data
    viewModelScope.launch {
        karooDataService.speedData.collect { streamState ->
            _uiState.value = _uiState.value.copy(speed = formatUiSpeedDataKmh(streamState))
        }
    }

    // Observe average speed data
    viewModelScope.launch {
        karooDataService.averageSpeedData.collect { streamState ->
            _uiState.value = _uiState.value.copy(avgSpeed = formatUiSpeedDataKmh(streamState))
        }
    }

    // Observe max speed data
    viewModelScope.launch {
        karooDataService.maxSpeedData.collect { streamState ->
            _uiState.value = _uiState.value.copy(maxSpeed = formatUiSpeedDataKmh(streamState))
        }
    }

    // Observe heart rate data
    viewModelScope.launch {
        karooDataService.heartRateData.collect { streamState ->
            _uiState.value = _uiState.value.copy(heartRate = formatStreamData(streamState, "bpm"))
        }
    }

    // Observe average heart rate data
    viewModelScope.launch {
        karooDataService.averageHeartRateData.collect { streamState ->
            _uiState.value =
                    _uiState.value.copy(avgHeartRate = formatStreamData(streamState, "bpm"))
        }
    }

    // Observe max heart rate data
    viewModelScope.launch {
        karooDataService.maxHeartRateData.collect { streamState ->
            _uiState.value =
                    _uiState.value.copy(maxHeartRate = formatStreamData(streamState, "bpm"))
        }
    }

    // Observe cadence data
    viewModelScope.launch {
        karooDataService.cadenceData.collect { streamState ->
            _uiState.value = _uiState.value.copy(cadence = formatStreamDataInt(streamState, "rpm"))
        }
    }

    // Observe average cadence data
    viewModelScope.launch {
        karooDataService.averageCadenceData.collect { streamState ->
            _uiState.value =
                    _uiState.value.copy(avgCadence = formatStreamDataInt(streamState, "rpm"))
        }
    }

    // Observe max cadence data
    viewModelScope.launch {
        karooDataService.maxCadenceData.collect { streamState ->
            _uiState.value =
                    _uiState.value.copy(maxCadence = formatStreamDataInt(streamState, "rpm"))
        }
    }

    // Observe power data
    viewModelScope.launch {
        karooDataService.powerData.collect { streamState ->
            _uiState.value = _uiState.value.copy(power = formatStreamDataInt(streamState, "w"))
        }
    }

    // Observe average power data
    viewModelScope.launch {
        karooDataService.averagePowerData.collect { streamState ->
            _uiState.value = _uiState.value.copy(avgPower = formatStreamDataInt(streamState, "w"))
        }
    }

    // Observe max power data
    viewModelScope.launch {
        karooDataService.maxPowerData.collect { streamState ->
            _uiState.value = _uiState.value.copy(maxPower = formatStreamDataInt(streamState, "w"))
        }
    }

    // Observe distance data
    viewModelScope.launch {
        karooDataService.distanceData.collect { streamState ->
            _uiState.value = _uiState.value.copy(distance = formatUiDistanceDataKm(streamState))
        }
    }

    // Observe time data
    viewModelScope.launch {
        karooDataService.timeData.collect { streamState ->
            _uiState.value = _uiState.value.copy(time = formatTimeData(streamState))
        }
    }

    // Observe HR zone data
    viewModelScope.launch {
        karooDataService.hrZoneData.collect { streamState ->
            val zoneStr =
                    when (streamState) {
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
            _uiState.value = _uiState.value.copy(power3s = formatStreamDataInt(streamState, "w"))
        }
    }

    // Observe 10s power data
    viewModelScope.launch {
        karooDataService.smoothed10sPowerData.collect { streamState ->
            _uiState.value = _uiState.value.copy(power10s = formatStreamDataInt(streamState, "w"))
        }
    }

    // Observe 30s power data
    viewModelScope.launch {
        karooDataService.smoothed30sPowerData.collect { streamState ->
            _uiState.value = _uiState.value.copy(power30s = formatStreamDataInt(streamState, "w"))
        }
    }

    // Observe VAM data
    viewModelScope.launch {
        karooDataService.vamData.collect { streamState ->
            _uiState.value = _uiState.value.copy(vam = formatStreamData(streamState, "m/h"))
        }
    }

    // Observe average VAM data
    viewModelScope.launch {
        karooDataService.avgVamData.collect { streamState ->
            _uiState.value = _uiState.value.copy(avgVam = formatStreamData(streamState, "m/h"))
        }
    }
}

internal fun shouldDisableDebugModeForRideTransition(
        previousRideState: RideState,
        newRideState: RideState,
        debugModeEnabled: Boolean,
): Boolean {
    return debugModeEnabled &&
            previousRideState is RideState.Idle &&
            newRideState !is RideState.Idle
}

// ── Format helpers (file-private) ─────────────────────────────────────────

/** Format stream data for display */
private fun formatStreamData(streamState: StreamState?, unit: String): String {
    return when (streamState) {
        is StreamState.Streaming -> {
            val value = streamState.dataPoint.singleValue
            if (value != null) "${formatValue(value)} $unit" else "-- $unit"
        }
        is StreamState.Searching -> "Searching..."
        is StreamState.Idle -> "-- $unit"
        is StreamState.NotAvailable -> "n/a"
        null -> "-- $unit"
    }
}

internal fun formatUiSpeedDataKmh(streamState: StreamState?): String =
        formatSpeedDataKmh(streamState)

internal fun formatUiDistanceDataKm(streamState: StreamState?): String =
        formatDistanceDataKm(streamState)

/** Format time data (convert ms to HH:MM:SS) */
private fun formatTimeData(streamState: StreamState?): String {
    return when (streamState) {
        is StreamState.Streaming -> {
            val ms = streamState.dataPoint.singleValue?.toLong()
            if (ms != null) {
                val seconds = (ms / 1000) % 60
                val minutes = (ms / (1000 * 60)) % 60
                val hours = ms / (1000 * 60 * 60)
                String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                "--:--:--"
            }
        }
        is StreamState.Searching -> "--:--:--"
        is StreamState.Idle -> "--:--:--"
        is StreamState.NotAvailable -> "n/a"
        null -> "--:--:--"
    }
}

/** Format numeric value for display */
private fun formatValue(value: Double): String {
    return when {
        value >= 100 -> "%.0f".format(value)
        value >= 10 -> "%.1f".format(value)
        else -> "%.2f".format(value)
    }
}

/** Format an integer-only metric (cadence, power, calories, energy) — no decimals. */
private fun formatStreamDataInt(streamState: StreamState?, unit: String): String {
    return when (streamState) {
        is StreamState.Streaming ->
                streamState.dataPoint.singleValue?.let { "%.0f $unit".format(it) } ?: "-- $unit"
        is StreamState.Searching -> "Searching..."
        is StreamState.Idle -> "-- $unit"
        is StreamState.NotAvailable -> "n/a"
        null -> "-- $unit"
    }
}
