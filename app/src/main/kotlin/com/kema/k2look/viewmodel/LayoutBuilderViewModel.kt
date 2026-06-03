package com.kema.k2look.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kema.k2look.data.ProfileRepository
import com.kema.k2look.data.SeedProfile
import com.kema.k2look.data.SettingsRepository
import com.kema.k2look.model.DataFieldProfile
import com.kema.k2look.sharing.KarooProfileImporter
import io.hammerhead.karooext.models.RideProfile
import io.hammerhead.karooext.models.RideState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * ViewModel for managing DataField Builder state and profile management.
 *
 * All profiles are equal — there are no system or read-only profiles. The only restriction is that
 * you cannot delete the last remaining profile.
 */
class LayoutBuilderViewModel(application: Application) : AndroidViewModel(application) {

    internal val repository = ProfileRepository(application)
    internal val settingsRepository = SettingsRepository(application)
    internal var bridge: com.kema.k2look.service.KarooActiveLookBridge? = null

    // UI State
    internal val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    data class UiState(
            val profiles: List<DataFieldProfile> = emptyList(),
            val activeProfile: DataFieldProfile? = null,
            val selectedScreen: Int = 1,
            val isLoading: Boolean = false,
            val error: String? = null,
            val successMessage: String? = null,
            val showProfileManagement: Boolean = false,
            val isGlassesConnected: Boolean = false,
            val activeRideProfile: RideProfile? = null,
            val isRiding: Boolean = false,
            val karooSyncEnabled: Boolean = true,
            val radarWarningEnabled: Boolean = true,
            val batteryDisplayEnabled: Boolean = true
    )

    init {
        Log.i(TAG, "LayoutBuilderViewModel initialized")
        _uiState.value =
                _uiState.value.copy(karooSyncEnabled = settingsRepository.karooSyncEnabled.value)
        settingsRepository
                .karooSyncEnabled
                .onEach { enabled ->
                    _uiState.value = _uiState.value.copy(karooSyncEnabled = enabled)
                }
                .launchIn(viewModelScope)
        _uiState.value =
                _uiState.value.copy(
                        radarWarningEnabled = settingsRepository.radarWarningEnabled.value
                )
        settingsRepository
                .radarWarningEnabled
                .onEach { enabled ->
                    _uiState.value = _uiState.value.copy(radarWarningEnabled = enabled)
                    bridge?.setRadarWarningEnabled(enabled)
                }
                .launchIn(viewModelScope)
        _uiState.value =
                _uiState.value.copy(
                        batteryDisplayEnabled = settingsRepository.batteryDisplayEnabled.value
                )
        settingsRepository
                .batteryDisplayEnabled
                .onEach { enabled ->
                    _uiState.value = _uiState.value.copy(batteryDisplayEnabled = enabled)
                    bridge?.setBatteryDisplayEnabled(enabled)
                }
                .launchIn(viewModelScope)
        loadProfiles()
    }

    // -------------------------------------------------------------------------
    // Internal helper — single source of truth for loading + seeding profiles
    // -------------------------------------------------------------------------

    /**
     * Load all profiles from the repository.
     *
     * Migration rules (run once, transparent to the user):
     * - Empty storage → seed the Default profile and save it.
     * - No profile with id "default" found (old install that never stored the hardcoded default) →
     * save the seed and prepend it so it's discoverable.
     */
    internal fun reloadAllProfiles(): List<DataFieldProfile> {
        val profiles = repository.loadProfiles()
        return when {
            profiles.isEmpty() -> {
                val seed = SeedProfile.build()
                repository.saveProfile(seed)
                repository.markSeedMigrationRan()
                listOf(seed)
            }
            // One-time migration: old install had the Default profile hardcoded (never stored).
            // Only inject it if this migration hasn't run yet — otherwise the user deliberately
            // deleted it and we must respect that.
            profiles.none { it.id == SeedProfile.SEED_PROFILE_ID } &&
                    !repository.hasSeedMigrationRun() -> {
                val seed = SeedProfile.build()
                repository.saveProfile(seed)
                repository.markSeedMigrationRan()
                listOf(seed) + profiles
            }
            else -> profiles
        }
    }

    // -------------------------------------------------------------------------
    // Bridge setup
    // -------------------------------------------------------------------------

    /** Set the bridge instance for applying profiles to glasses */
    fun setBridge(bridge: com.kema.k2look.service.KarooActiveLookBridge) {
        this.bridge = bridge
        Log.i(TAG, "Bridge set, auto-applying active profile if available")

        // Register profile lookup callback for auto-switching on ride start
        bridge.setProfileLookup { karooProfileName ->
            // Respect Karoo Sync toggle
            if (!_uiState.value.karooSyncEnabled) {
                Log.d(TAG, "Karoo sync disabled, skipping auto-switch for '$karooProfileName'")
                return@setProfileLookup null
            }

            // Find K2Look profile with matching name (case-insensitive)
            val matchingProfile =
                    _uiState.value.profiles.find { profile ->
                        profile.name.equals(karooProfileName, ignoreCase = true)
                    }

            if (matchingProfile != null) {
                Log.i(
                        TAG,
                        "Found matching K2Look profile '${matchingProfile.name}' for Karoo profile '$karooProfileName'"
                )
                // Update UI state to reflect the auto-selected profile
                _uiState.value = _uiState.value.copy(activeProfile = matchingProfile)
            } else {
                Log.d(TAG, "No K2Look profile matches Karoo profile '$karooProfileName'")
            }

            matchingProfile
        }

        // Observe glasses connection state to keep isGlassesConnected up to date
        bridge.getActiveLookService()
                .connectionState
                .onEach { state ->
                    val connected =
                            state is
                                    com.kema.k2look.service.ActiveLookService.ConnectionState.Connected
                    _uiState.value = _uiState.value.copy(isGlassesConnected = connected)
                    Log.d(TAG, "Glasses connection state changed: $state (connected=$connected)")
                }
                .launchIn(viewModelScope)

        // Observe Karoo ride state
        bridge.getKarooDataService()
                .rideState
                .onEach { state ->
                    _uiState.value = _uiState.value.copy(isRiding = state !is RideState.Idle)
                }
                .launchIn(viewModelScope)

        // Observe active Karoo ride profile for import suggestions
        bridge.getKarooDataService()
                .activeRideProfile
                .onEach { rideProfile ->
                    _uiState.value = _uiState.value.copy(activeRideProfile = rideProfile)
                }
                .launchIn(viewModelScope)

        // Auto-apply current active profile if one is selected.
        // Invalidate the glasses-side cache first so a full re-upload (including
        // layoutDeleteAll) runs — this clears stale layouts left over from debug
        // tests or previous profile versions.
        _uiState.value.activeProfile?.let { profile ->
            bridge.invalidateProfileConfig(profile.id)
            applyProfileToGlasses(profile)
        }
        // Sync radar warning setting to bridge on connect
        bridge.setRadarWarningEnabled(_uiState.value.radarWarningEnabled)
        // Sync battery overlay setting to bridge on connect
        bridge.setBatteryDisplayEnabled(_uiState.value.batteryDisplayEnabled)
    }

    // selectScreen / addFieldToScreen / updateField / removeField / addScreen /
    // removeScreen / changeScreenTemplate / assignMetricToZone / removeMetricFromZone
    // → LayoutBuilderViewModelScreenManagement.kt

    /** Select a screen within the active profile */
    fun selectScreen(screenId: Int) {
        _uiState.value = _uiState.value.copy(selectedScreen = screenId)
        bridge?.setActiveScreen(screenId)
        Log.d(TAG, "Selected screen: $screenId")
    }

    // -------------------------------------------------------------------------
    // Gesture / screen cycling
    // -------------------------------------------------------------------------

    /**
     * Cycle to the next screen in the active profile Used by gesture/touch actions for hands-free
     * screen switching
     * @return true if screen was cycled, false if there's only one screen or no active profile
     */
    fun cycleToNextScreen(): Boolean {
        val currentProfile =
                _uiState.value.activeProfile
                        ?: run {
                            Log.w(TAG, "Cannot cycle screens - no active profile")
                            return false
                        }
        val screens = currentProfile.screens
        if (screens.size <= 1) {
            Log.d(TAG, "Only one screen in profile - nothing to cycle")
            return false
        }
        val currentIndex = screens.indexOfFirst { it.id == _uiState.value.selectedScreen }
        val nextIndex = (currentIndex + 1) % screens.size
        val nextScreen = screens[nextIndex]
        Log.i(
                TAG,
                "✓ Cycling from screen ${currentIndex + 1} to screen ${nextIndex + 1}: ${nextScreen.name}"
        )
        selectScreen(nextScreen.id)
        // Tell the bridge which screen to display. All screens' layouts were already uploaded
        // when the profile was last saved — no re-upload needed here.
        bridge?.setActiveScreen(nextScreen.id)
        return true
    }

    // -------------------------------------------------------------------------
    // Karoo import
    // -------------------------------------------------------------------------

    fun importFromKaroo(rideProfile: RideProfile) {
        viewModelScope.launch {
            try {
                val profile = KarooProfileImporter.import(rideProfile)
                repository.saveProfile(profile)
                val allProfiles = reloadAllProfiles()
                _uiState.value =
                        _uiState.value.copy(profiles = allProfiles, activeProfile = profile)
                Log.i(TAG, "Imported Karoo profile '${rideProfile.name}' → '${profile.name}'")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import Karoo profile", e)
                _uiState.value = _uiState.value.copy(error = "Import failed: ${e.message}")
            }
        }
    }

    // -------------------------------------------------------------------------
    // Settings
    // -------------------------------------------------------------------------

    /** Show/hide profile management screen */
    fun setShowProfileManagement(show: Boolean) {
        _uiState.value = _uiState.value.copy(showProfileManagement = show)
    }

    /** Clear error message */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /** Clear success message */
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    /** Change the layout template for a screen */
    fun setKarooSyncEnabled(enabled: Boolean) {
        settingsRepository.setKarooSyncEnabled(enabled)
        Log.i(TAG, "Karoo sync ${if (enabled) "enabled" else "disabled"}")
    }

    fun setRadarWarningEnabled(enabled: Boolean) {
        settingsRepository.setRadarWarningEnabled(enabled)
        Log.i(TAG, "Radar warning ${if (enabled) "enabled" else "disabled"}")
    }

    fun setBatteryDisplayEnabled(enabled: Boolean) {
        settingsRepository.setBatteryDisplayEnabled(enabled)
        Log.i(TAG, "Battery overlay ${if (enabled) "enabled" else "disabled"}")
    }

    companion object {
        private const val TAG = "LayoutBuilderViewModel"
    }
}
