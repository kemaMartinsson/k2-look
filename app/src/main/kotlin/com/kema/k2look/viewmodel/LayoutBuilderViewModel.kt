package com.kema.k2look.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kema.k2look.data.SeedProfile
import com.kema.k2look.data.ProfileRepository
import com.kema.k2look.data.SettingsRepository
import com.kema.k2look.model.DataFieldProfile
import com.kema.k2look.model.VisualizationType
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
 * All profiles are equal — there are no system or read-only profiles.
 * The only restriction is that you cannot delete the last remaining profile.
 */
class LayoutBuilderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProfileRepository(application)
    private val settingsRepository = SettingsRepository(application)
    private var bridge: com.kema.k2look.service.KarooActiveLookBridge? = null

    // UI State
    private val _uiState = MutableStateFlow(UiState())
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
        val karooSyncEnabled: Boolean = true
    )

    init {
        Log.i(TAG, "LayoutBuilderViewModel initialized")
        _uiState.value = _uiState.value.copy(karooSyncEnabled = settingsRepository.karooSyncEnabled.value)
        settingsRepository.karooSyncEnabled
            .onEach { enabled -> _uiState.value = _uiState.value.copy(karooSyncEnabled = enabled) }
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
     * - Empty storage  → seed the Default profile and save it.
     * - No profile with id "default" found (old install that never stored the
     *   hardcoded default) → save the seed and prepend it so it's discoverable.
     */
    private fun reloadAllProfiles(): List<DataFieldProfile> {
        val profiles = repository.loadProfiles()
        return when {
            profiles.isEmpty() -> {
                val seed = SeedProfile.build()
                repository.saveProfile(seed)
                listOf(seed)
            }
            profiles.none { it.id == SeedProfile.SEED_PROFILE_ID } -> {
                // Migration: user has custom profiles but the starter profile was never stored
                val seed = SeedProfile.build()
                repository.saveProfile(seed)
                listOf(seed) + profiles
            }
            else -> profiles
        }
    }

    // -------------------------------------------------------------------------
    // Bridge setup
    // -------------------------------------------------------------------------

    /**
     * Set the bridge instance for applying profiles to glasses
     */
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
            val matchingProfile = _uiState.value.profiles.find { profile ->
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
        bridge.getActiveLookService().connectionState
            .onEach { state ->
                val connected = state is com.kema.k2look.service.ActiveLookService.ConnectionState.Connected
                _uiState.value = _uiState.value.copy(isGlassesConnected = connected)
                Log.d(TAG, "Glasses connection state changed: $state (connected=$connected)")
            }
            .launchIn(viewModelScope)

        // Observe Karoo ride state
        bridge.getKarooDataService().rideState
            .onEach { state ->
                _uiState.value = _uiState.value.copy(isRiding = state !is RideState.Idle)
            }
            .launchIn(viewModelScope)

        // Observe active Karoo ride profile for import suggestions
        bridge.getKarooDataService().activeRideProfile
            .onEach { rideProfile ->
                _uiState.value = _uiState.value.copy(activeRideProfile = rideProfile)
            }
            .launchIn(viewModelScope)

        // Auto-apply current active profile if one is selected
        _uiState.value.activeProfile?.let { profile ->
            applyProfileToGlasses(profile)
        }
    }

    // -------------------------------------------------------------------------
    // Profile loading
    // -------------------------------------------------------------------------

    /**
     * Load all profiles including the default profile
     */
    private fun loadProfiles() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val allProfiles = reloadAllProfiles()

                val activeProfile = _uiState.value.activeProfile?.let { current ->
                    allProfiles.find { it.id == current.id }
                } ?: allProfiles.first()

                _uiState.value = _uiState.value.copy(
                    profiles = allProfiles,
                    activeProfile = activeProfile,
                    isLoading = false
                )

                Log.i(TAG, "Loaded ${allProfiles.size} profile(s), active: ${activeProfile.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading profiles", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load profiles: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    // -------------------------------------------------------------------------
    // Profile selection & apply
    // -------------------------------------------------------------------------

    /**
     * Select a profile by ID
     */
    fun selectProfile(profileId: String) {
        val profile = _uiState.value.profiles.find { it.id == profileId }
        if (profile != null) {
            _uiState.value = _uiState.value.copy(activeProfile = profile)
            Log.i(TAG, "Selected profile: ${profile.name}")

            // Apply to glasses if available
            applyProfileToGlasses(profile)
        } else {
            Log.w(TAG, "Profile not found: $profileId")
        }
    }

    /**
     * Apply the active profile to glasses for display
     */
    fun applyProfileToGlasses(profile: DataFieldProfile? = null) {
        val targetProfile = profile ?: _uiState.value.activeProfile

        if (targetProfile == null) {
            Log.w(TAG, "No profile to apply to glasses")
            return
        }

        try {
            Log.i(TAG, "Applying profile '${targetProfile.name}' to glasses")
            Log.i(TAG, "  - ${targetProfile.screens.size} screen(s)")
            targetProfile.screens.firstOrNull()?.let { screen ->
                Log.i(TAG, "  - Screen 1: ${screen.dataFields.size} field(s)")
                screen.dataFields.forEach { field ->
                    Log.i(TAG, "    - Zone ${field.zoneId}: ${field.dataField.name}")
                }
            }

            // Apply to bridge if available
            val bridgeInstance = bridge
            if (bridgeInstance != null) {
                bridgeInstance.setActiveProfile(targetProfile)
                _uiState.value = _uiState.value.copy(
                    successMessage = "Profile '${targetProfile.name}' sent to glasses ✓"
                )
                Log.i(TAG, "✅ Profile applied to bridge successfully")
            } else {
                Log.w(TAG, "⚠️ Bridge not available, profile not applied to glasses")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error applying profile to glasses", e)
            _uiState.value = _uiState.value.copy(
                error = "Failed to apply profile: ${e.message}"
            )
        }
    }

    // -------------------------------------------------------------------------
    // Profile CRUD
    // -------------------------------------------------------------------------

    /**
     * Create a new profile
     * @param name Profile name
     */
    fun createProfile(name: String) {
        viewModelScope.launch {
            try {
                if (_uiState.value.profiles.any { it.name.equals(name, ignoreCase = true) }) {
                    _uiState.value = _uiState.value.copy(error = "A profile named '$name' already exists")
                    return@launch
                }

                val newProfile = SeedProfile.build().copy(
                    id = java.util.UUID.randomUUID().toString(),
                    name = name,
                    createdAt = System.currentTimeMillis(),
                    modifiedAt = System.currentTimeMillis()
                )

                repository.saveProfile(newProfile)
                val allProfiles = reloadAllProfiles()

                _uiState.value = _uiState.value.copy(
                    profiles = allProfiles,
                    activeProfile = newProfile,
                    isLoading = false
                )

                Log.i(TAG, "Created and selected profile: $name (id: ${newProfile.id})")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating profile", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to create profile: ${e.message}"
                )
            }
        }
    }

    /**
     * Duplicate an existing profile
     */
    fun duplicateProfile(profileId: String, newName: String) {
        viewModelScope.launch {
            try {
                if (_uiState.value.profiles.any { it.name.equals(newName, ignoreCase = true) }) {
                    _uiState.value = _uiState.value.copy(error = "A profile named '$newName' already exists")
                    return@launch
                }

                val originalProfile = _uiState.value.profiles.find { it.id == profileId }
                if (originalProfile != null) {
                    val duplicatedProfile = originalProfile.copy(
                        id = java.util.UUID.randomUUID().toString(), // Generate new unique ID
                        name = newName,
                        createdAt = System.currentTimeMillis(),
                        modifiedAt = System.currentTimeMillis()
                    )

                    repository.saveProfile(duplicatedProfile)
                    val allProfiles = reloadAllProfiles()

                    _uiState.value = _uiState.value.copy(
                        profiles = allProfiles,
                        activeProfile = duplicatedProfile,
                        isLoading = false
                    )

                    Log.i(TAG, "Duplicated profile: ${originalProfile.name} → $newName (id: ${duplicatedProfile.id})")
                } else {
                    Log.w(TAG, "Profile not found for duplication: $profileId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error duplicating profile", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to duplicate profile: ${e.message}"
                )
            }
        }
    }

    /**
     * Delete a profile
     * Cannot delete default profile
     */
    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            try {
                if (_uiState.value.profiles.size <= 1) {
                    Log.w(TAG, "Cannot delete the last profile")
                    _uiState.value = _uiState.value.copy(error = "Cannot delete the last profile")
                    return@launch
                }

                repository.deleteProfile(profileId)

                val allProfiles = reloadAllProfiles()

                // If the deleted profile was active, switch to the first remaining profile
                val activeProfile = if (_uiState.value.activeProfile?.id == profileId) {
                    allProfiles.first()
                } else {
                    _uiState.value.activeProfile?.let { current ->
                        allProfiles.find { it.id == current.id }
                    } ?: allProfiles.first()
                }

                _uiState.value = _uiState.value.copy(
                    profiles = allProfiles,
                    activeProfile = activeProfile
                )

                Log.i(TAG, "Deleted profile: $profileId, switched to: ${activeProfile.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting profile", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete profile: ${e.message}"
                )
            }
        }
    }

    /**
     * Save an updated profile to the repository and refresh state
     */
    fun updateProfile(profile: DataFieldProfile) {
        viewModelScope.launch {
            try {
                repository.saveProfile(profile)
                val allProfiles = reloadAllProfiles()
                val updatedActiveProfile = allProfiles.find { it.id == profile.id } ?: profile

                _uiState.value = _uiState.value.copy(
                    profiles = allProfiles,
                    activeProfile = updatedActiveProfile,
                    isLoading = false
                )

                Log.i(TAG, "Updated profile: ${profile.name} (id: ${profile.id})")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating profile", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update profile: ${e.message}"
                )
            }
        }
    }

    // -------------------------------------------------------------------------
    // Screen management
    // -------------------------------------------------------------------------

    /**
     * Select a screen within the active profile
     */
    fun selectScreen(screenId: Int) {
        _uiState.value = _uiState.value.copy(selectedScreen = screenId)
        bridge?.setActiveScreen(screenId)
        Log.d(TAG, "Selected screen: $screenId")
    }

    /**
     * Add a datafield to a specific zone in the active profile's current screen
     */
    fun addFieldToScreen(
        screenId: Int,
        zoneId: String,
        dataField: com.kema.k2look.model.DataField
    ) {
        val profile = _uiState.value.activeProfile ?: return

        val updatedScreens = profile.screens.map { screen ->
            if (screen.id == screenId) {
                val template = screen.getTemplate()

                // Check if zone is valid for this template
                if (template.zones.none { it.id == zoneId }) {
                    Log.w(TAG, "Zone $zoneId is not valid for template ${template.id}")
                    return@map screen
                }

                // Check if zone is already occupied
                if (screen.dataFields.any { it.zoneId == zoneId }) {
                    Log.w(TAG, "Zone $zoneId is already occupied")
                    return@map screen
                }

                // Check if max fields reached
                if (screen.dataFields.size >= template.maxFields) {
                    Log.w(TAG, "Screen already has ${template.maxFields} fields")
                    return@map screen
                }

                // Add new field
                val newField = com.kema.k2look.model.LayoutDataField(
                    dataField = dataField,
                    zoneId = zoneId,
                    showLabel = true,
                    showUnit = true,
                    showIcon = dataField.icon28 != null || dataField.icon40 != null,
                    iconSize = com.kema.k2look.model.IconSize.SMALL  // Always default to SMALL
                )

                screen.copy(dataFields = screen.dataFields + newField)
            } else {
                screen
            }
        }

        updateProfile(profile.copy(screens = updatedScreens, modifiedAt = System.currentTimeMillis()))
        Log.i(TAG, "Added field ${dataField.name} to screen $screenId in zone $zoneId")
    }

    /**
     * Update a datafield in the active profile
     */
    fun updateField(screenId: Int, updatedField: com.kema.k2look.model.LayoutDataField) {
        val profile = _uiState.value.activeProfile ?: return

        Log.i(TAG, "🔄 updateField called: screenId=$screenId, zoneId=${updatedField.zoneId}")

        // Debug: Show all screens and their fields
        profile.screens.forEach { screen ->
            Log.i(TAG, "  📄 Screen ${screen.id} has ${screen.dataFields.size} fields:")
            screen.dataFields.forEach { field ->
                Log.i(TAG, "    - Zone ${field.zoneId}: ${field.dataField.name}")
            }
        }

        val targetScreen = profile.screens.find { it.id == screenId }
        if (targetScreen == null) {
            Log.e(TAG, "  ❌ ERROR: Screen $screenId not found!")
            return
        }

        val oldField = targetScreen.dataFields.find { it.zoneId == updatedField.zoneId }
        Log.i(TAG, "  OLD field name: ${oldField?.dataField?.name ?: "NOT FOUND"}")
        Log.i(TAG, "  NEW field name: ${updatedField.dataField.name}")

        if (oldField == null) {
            Log.w(TAG, "  ⚠️ WARNING: No existing field in zone ${updatedField.zoneId} on screen $screenId")
        }

        val updatedScreens = profile.screens.map { screen ->
            if (screen.id == screenId) {
                screen.copy(dataFields = screen.dataFields.map { field ->
                    if (field.zoneId == updatedField.zoneId) {
                        Log.i(TAG, "  ✅ Found matching zone ${field.zoneId}, updating field")
                        updatedField
                    } else field
                })
            } else screen
        }

        Log.i(TAG, "  📝 Calling updateProfile to save changes")
        updateProfile(profile.copy(screens = updatedScreens, modifiedAt = System.currentTimeMillis()))
        Log.i(TAG, "✅ Updated field in zone ${updatedField.zoneId} in screen $screenId")
    }

    /**
     * Remove a datafield from the active profile
     */
    fun removeField(screenId: Int, zoneId: String) {
        val profile = _uiState.value.activeProfile ?: return
        val updatedScreens = profile.screens.map { screen ->
            if (screen.id == screenId)
                screen.copy(dataFields = screen.dataFields.filter { it.zoneId != zoneId })
            else screen
        }
        updateProfile(profile.copy(screens = updatedScreens, modifiedAt = System.currentTimeMillis()))
        Log.i(TAG, "Removed field at zone $zoneId from screen $screenId")
    }

    /**
     * Add a new screen to the active profile
     */
    fun addScreen() {
        val profile = _uiState.value.activeProfile ?: return

        Log.d(TAG, "addScreen: START - current screens: ${profile.screens.map { it.id }}, selectedScreen: ${_uiState.value.selectedScreen}")

        // Find the next available screen ID
        val nextScreenId = (profile.screens.maxOfOrNull { it.id } ?: 0) + 1
        Log.d(TAG, "addScreen: Creating screen $nextScreenId")

        val newScreen = com.kema.k2look.model.LayoutScreen(
            id = nextScreenId,
            name = "Screen $nextScreenId",
            dataFields = emptyList()
        )

        val updatedProfile = profile.copy(
            screens = profile.screens + newScreen,
            modifiedAt = System.currentTimeMillis()
        )

        Log.d(TAG, "addScreen: Updated profile screens: ${updatedProfile.screens.map { it.id }}")

        // Update the profile first, which will trigger state update
        viewModelScope.launch {
            try {
                Log.d(TAG, "addScreen: Saving profile to repository")
                repository.saveProfile(updatedProfile)
                val allProfiles = reloadAllProfiles()
                val updatedActiveProfile = allProfiles.find { it.id == updatedProfile.id } ?: updatedProfile

                Log.d(TAG, "addScreen: BEFORE state update - selectedScreen=${_uiState.value.selectedScreen}")
                // Update state with new screen selected
                _uiState.value = _uiState.value.copy(
                    profiles = allProfiles,
                    activeProfile = updatedActiveProfile,
                    selectedScreen = nextScreenId,  // Select the new screen
                    isLoading = false
                )
                Log.d(TAG, "addScreen: AFTER state update - selectedScreen=${_uiState.value.selectedScreen}, profile screens=${updatedActiveProfile.screens.map { it.id }}")

                Log.i(TAG, "Added new screen: Screen $nextScreenId")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding screen", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to add screen: ${e.message}"
                )
            }
        }
    }

    /**
     * Remove a screen from the active profile
     * Cannot remove if it's the only screen
     */
    fun removeScreen(screenId: Int) {
        val profile = _uiState.value.activeProfile ?: return

        if (profile.screens.size <= 1) {
            _uiState.value = _uiState.value.copy(error = "Cannot remove the only screen")
            return
        }

        val updatedScreens = profile.screens.filter { it.id != screenId }
        if (_uiState.value.selectedScreen == screenId) {
            _uiState.value = _uiState.value.copy(selectedScreen = updatedScreens.first().id)
        }
        updateProfile(profile.copy(screens = updatedScreens, modifiedAt = System.currentTimeMillis()))
        Log.i(TAG, "Removed screen: $screenId")
    }

    /**
     * Change the layout template for a screen
     */
    fun changeScreenTemplate(screenId: Int, newTemplateId: String) {
        val profile = _uiState.value.activeProfile ?: return
        val screen = profile.screens.find { it.id == screenId } ?: return
        val newTemplate = com.kema.k2look.layout.LayoutTemplateRegistry.getTemplate(newTemplateId)

        // Preserve as many existing fields as possible
        val preservedFields = screen.dataFields.take(newTemplate.maxFields)

        // Map existing fields to new zones by index
        val mappedFields = preservedFields.mapIndexed { index, field ->
            val newZone = newTemplate.zones.getOrNull(index)
            if (newZone != null) {
                field.copy(
                    zoneId = newZone.id,
                    visualizationType = field.visualizationType ?: VisualizationType.TEXT
                )
            } else field
        }

        val updatedProfile = profile.copy(
            screens = profile.screens.map {
                if (it.id == screenId) it.copy(templateId = newTemplateId, dataFields = mappedFields) else it
            },
            modifiedAt = System.currentTimeMillis()
        )

        updateProfile(updatedProfile)
        applyProfileToGlasses(updatedProfile)

        Log.i(TAG, "Changed screen $screenId template to $newTemplateId, preserved ${mappedFields.size} fields")
    }

    // -------------------------------------------------------------------------
    // Zone-based convenience aliases
    // -------------------------------------------------------------------------

    /**
     * Assign a metric to a specific zone (zone-based API)
     */
    fun assignMetricToZone(
        screenId: Int,
        zoneId: String,
        dataField: com.kema.k2look.model.DataField
    ) {
        addFieldToScreen(screenId, zoneId, dataField)
    }

    /**
     * Remove metric from zone (zone-based API)
     */
    fun removeMetricFromZone(screenId: Int, zoneId: String) {
        removeField(screenId, zoneId)
    }

    // -------------------------------------------------------------------------
    // Gesture / screen cycling
    // -------------------------------------------------------------------------

    /**
     * Cycle to the next screen in the active profile
     * Used by gesture/touch actions for hands-free screen switching
     * @return true if screen was cycled, false if there's only one screen or no active profile
     */
    fun cycleToNextScreen(): Boolean {
        val currentProfile = _uiState.value.activeProfile ?: run {
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
        Log.i(TAG, "✓ Cycling from screen ${currentIndex + 1} to screen ${nextIndex + 1}: ${nextScreen.name}")
        selectScreen(nextScreen.id)
        applyProfileToGlasses(currentProfile)
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
                _uiState.value = _uiState.value.copy(profiles = allProfiles, activeProfile = profile)
                applyProfileToGlasses(profile)
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

    /**
     * Show/hide profile management screen
     */
    fun setShowProfileManagement(show: Boolean) {
        _uiState.value = _uiState.value.copy(showProfileManagement = show)
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    /**
     * Change the layout template for a screen
     */
    fun setKarooSyncEnabled(enabled: Boolean) {
        settingsRepository.setKarooSyncEnabled(enabled)
        Log.i(TAG, "Karoo sync ${if (enabled) "enabled" else "disabled"}")
    }

    companion object {
        private const val TAG = "LayoutBuilderViewModel"
    }
}

