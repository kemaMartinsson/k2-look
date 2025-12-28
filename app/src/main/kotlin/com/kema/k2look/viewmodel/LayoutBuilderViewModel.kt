package com.kema.k2look.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kema.k2look.data.DefaultProfiles
import com.kema.k2look.data.ProfileRepository
import com.kema.k2look.model.DataFieldProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing DataField Builder state and profile management
 */
class LayoutBuilderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProfileRepository(application)

    // UI State
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    data class UiState(
        val profiles: List<DataFieldProfile> = emptyList(),
        val activeProfile: DataFieldProfile? = null,
        val selectedScreen: Int = 1,
        val isLoading: Boolean = false,
        val error: String? = null,
        val showProfileManagement: Boolean = false
    )

    init {
        Log.i(TAG, "LayoutBuilderViewModel initialized")
        loadProfiles()
    }

    /**
     * Load all profiles including the default profile
     */
    private fun loadProfiles() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val userProfiles = repository.loadProfiles()
                val defaultProfile = DefaultProfiles.getDefaultProfile()

                // Combine default + user profiles
                val allProfiles = listOf(defaultProfile) + userProfiles

                // Refresh active profile reference if it exists, otherwise use default
                val activeProfile = _uiState.value.activeProfile?.let { current ->
                    // Find the updated version of the current active profile
                    allProfiles.find { it.id == current.id } ?: defaultProfile
                } ?: defaultProfile

                _uiState.value = _uiState.value.copy(
                    profiles = allProfiles,
                    activeProfile = activeProfile,
                    isLoading = false
                )

                Log.i(
                    TAG,
                    "Loaded ${allProfiles.size} profiles (${userProfiles.size} user + 1 default), active: ${activeProfile.name}"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading profiles", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load profiles: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Select a profile by ID
     */
    fun selectProfile(profileId: String) {
        val profile = _uiState.value.profiles.find { it.id == profileId }
        if (profile != null) {
            _uiState.value = _uiState.value.copy(activeProfile = profile)
            Log.i(TAG, "Selected profile: ${profile.name}")
        } else {
            Log.w(TAG, "Profile not found: $profileId")
        }
    }

    /**
     * Create a new profile
     * @param name Profile name
     * @param template Optional template ID ("default", "template_road", "template_gravel")
     */
    fun createProfile(name: String, template: String? = null) {
        viewModelScope.launch {
            try {
                val baseProfile = when (template) {
                    "template_road" -> DefaultProfiles.getRoadBikeTemplate()
                    "template_gravel" -> DefaultProfiles.getGravelBikeTemplate()
                    else -> DefaultProfiles.getDefaultProfile()
                }

                val newProfile = baseProfile.copy(
                    id = java.util.UUID.randomUUID().toString(), // Generate new unique ID
                    name = name,
                    isDefault = false,
                    isReadOnly = false,
                    createdAt = System.currentTimeMillis(),
                    modifiedAt = System.currentTimeMillis()
                )

                repository.saveProfile(newProfile)

                // Reload profiles
                val userProfiles = repository.loadProfiles()
                val defaultProfile = DefaultProfiles.getDefaultProfile()
                val allProfiles = listOf(defaultProfile) + userProfiles

                // Auto-select the newly created profile
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
                val originalProfile = _uiState.value.profiles.find { it.id == profileId }
                if (originalProfile != null) {
                    val duplicatedProfile = originalProfile.copy(
                        id = java.util.UUID.randomUUID().toString(), // Generate new unique ID
                        name = newName,
                        isDefault = false,
                        isReadOnly = false,
                        createdAt = System.currentTimeMillis(),
                        modifiedAt = System.currentTimeMillis()
                    )

                    repository.saveProfile(duplicatedProfile)

                    // Reload profiles
                    val userProfiles = repository.loadProfiles()
                    val defaultProfile = DefaultProfiles.getDefaultProfile()
                    val allProfiles = listOf(defaultProfile) + userProfiles

                    // Auto-select the newly duplicated profile
                    _uiState.value = _uiState.value.copy(
                        profiles = allProfiles,
                        activeProfile = duplicatedProfile,
                        isLoading = false
                    )

                    Log.i(
                        TAG,
                        "Duplicated and selected profile: ${originalProfile.name} -> $newName (id: ${duplicatedProfile.id})"
                    )
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
                val profile = _uiState.value.profiles.find { it.id == profileId }

                if (profile?.isDefault == true) {
                    Log.w(TAG, "Cannot delete default profile")
                    _uiState.value = _uiState.value.copy(
                        error = "Cannot delete default profile"
                    )
                    return@launch
                }

                repository.deleteProfile(profileId)

                // If deleted profile was active, switch to default
                if (_uiState.value.activeProfile?.id == profileId) {
                    val defaultProfile = DefaultProfiles.getDefaultProfile()
                    _uiState.value = _uiState.value.copy(activeProfile = defaultProfile)
                }

                loadProfiles()

                Log.i(TAG, "Deleted profile: $profileId")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting profile", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete profile: ${e.message}"
                )
            }
        }
    }

    /**
     * Update the active profile
     */
    fun updateProfile(profile: DataFieldProfile) {
        viewModelScope.launch {
            try {
                if (profile.isReadOnly) {
                    Log.w(TAG, "Cannot update read-only profile")
                    _uiState.value = _uiState.value.copy(
                        error = "Cannot modify read-only profile"
                    )
                    return@launch
                }

                repository.saveProfile(profile)

                // Reload profiles
                val userProfiles = repository.loadProfiles()
                val defaultProfile = DefaultProfiles.getDefaultProfile()
                val allProfiles = listOf(defaultProfile) + userProfiles

                // Find and maintain the updated profile as active
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

    /**
     * Select a screen within the active profile
     */
    fun selectScreen(screenId: Int) {
        _uiState.value = _uiState.value.copy(selectedScreen = screenId)
        Log.d(TAG, "Selected screen: $screenId")
    }

    /**
     * Add a datafield to the active profile's current screen
     */
    fun addFieldToScreen(
        screenId: Int,
        position: com.kema.k2look.model.Position,
        dataField: com.kema.k2look.model.DataField
    ) {
        val profile = _uiState.value.activeProfile ?: return

        if (profile.isReadOnly) {
            _uiState.value = _uiState.value.copy(error = "Cannot modify read-only profile")
            return
        }

        val updatedScreens = profile.screens.map { screen ->
            if (screen.id == screenId) {
                // Check if position is already occupied
                if (screen.dataFields.any { it.position == position }) {
                    Log.w(TAG, "Position $position is already occupied")
                    return@map screen
                }

                // Check if max fields reached
                if (screen.dataFields.size >= 3) {
                    Log.w(TAG, "Screen already has 3 fields")
                    return@map screen
                }

                // Add new field
                val newField = com.kema.k2look.model.LayoutDataField(
                    dataField = dataField,
                    position = position,
                    fontSize = com.kema.k2look.model.FontSize.MEDIUM,  // Default to MEDIUM
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

        val updatedProfile = profile.copy(
            screens = updatedScreens,
            modifiedAt = System.currentTimeMillis()
        )

        updateProfile(updatedProfile)
        Log.i(TAG, "Added field ${dataField.name} to screen $screenId at $position")
    }

    /**
     * Update a datafield in the active profile
     */
    fun updateField(screenId: Int, updatedField: com.kema.k2look.model.LayoutDataField) {
        val profile = _uiState.value.activeProfile ?: return

        if (profile.isReadOnly) {
            _uiState.value = _uiState.value.copy(error = "Cannot modify read-only profile")
            return
        }

        val updatedScreens = profile.screens.map { screen ->
            if (screen.id == screenId) {
                val updatedFields = screen.dataFields.map { field ->
                    if (field.position == updatedField.position) {
                        updatedField
                    } else {
                        field
                    }
                }
                screen.copy(dataFields = updatedFields)
            } else {
                screen
            }
        }

        val updatedProfile = profile.copy(
            screens = updatedScreens,
            modifiedAt = System.currentTimeMillis()
        )

        updateProfile(updatedProfile)
        Log.i(TAG, "Updated field at position ${updatedField.position} in screen $screenId")
    }

    /**
     * Remove a datafield from the active profile
     */
    fun removeField(screenId: Int, position: com.kema.k2look.model.Position) {
        val profile = _uiState.value.activeProfile ?: return

        if (profile.isReadOnly) {
            _uiState.value = _uiState.value.copy(error = "Cannot modify read-only profile")
            return
        }

        val updatedScreens = profile.screens.map { screen ->
            if (screen.id == screenId) {
                screen.copy(dataFields = screen.dataFields.filter { it.position != position })
            } else {
                screen
            }
        }

        val updatedProfile = profile.copy(
            screens = updatedScreens,
            modifiedAt = System.currentTimeMillis()
        )

        updateProfile(updatedProfile)
        Log.i(TAG, "Removed field at position $position from screen $screenId")
    }

    /**
     * Add a new screen to the active profile
     */
    fun addScreen() {
        val profile = _uiState.value.activeProfile ?: return

        Log.d(
            TAG,
            "addScreen: START - current screens: ${profile.screens.map { it.id }}, selectedScreen: ${_uiState.value.selectedScreen}"
        )

        if (profile.isReadOnly) {
            _uiState.value = _uiState.value.copy(error = "Cannot modify read-only profile")
            return
        }

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
                if (updatedProfile.isReadOnly) {
                    Log.w(TAG, "Cannot update read-only profile")
                    _uiState.value = _uiState.value.copy(
                        error = "Cannot modify read-only profile"
                    )
                    return@launch
                }

                Log.d(TAG, "addScreen: Saving profile to repository")
                repository.saveProfile(updatedProfile)

                // Reload profiles
                val userProfiles = repository.loadProfiles()
                val defaultProfile = DefaultProfiles.getDefaultProfile()
                val allProfiles = listOf(defaultProfile) + userProfiles

                // Find and maintain the updated profile as active
                val updatedActiveProfile =
                    allProfiles.find { it.id == updatedProfile.id } ?: updatedProfile

                Log.d(
                    TAG,
                    "addScreen: BEFORE state update - selectedScreen=${_uiState.value.selectedScreen}"
                )

                // Update state with new screen selected
                _uiState.value = _uiState.value.copy(
                    profiles = allProfiles,
                    activeProfile = updatedActiveProfile,
                    selectedScreen = nextScreenId,  // Select the new screen
                    isLoading = false
                )

                Log.d(
                    TAG,
                    "addScreen: AFTER state update - selectedScreen=${_uiState.value.selectedScreen}, profile screens=${updatedActiveProfile.screens.map { it.id }}"
                )

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

        if (profile.isReadOnly) {
            _uiState.value = _uiState.value.copy(error = "Cannot modify read-only profile")
            return
        }

        if (profile.screens.size <= 1) {
            _uiState.value = _uiState.value.copy(error = "Cannot remove the only screen")
            return
        }

        val updatedScreens = profile.screens.filter { it.id != screenId }

        val updatedProfile = profile.copy(
            screens = updatedScreens,
            modifiedAt = System.currentTimeMillis()
        )

        // If we're removing the active screen, switch to the first remaining screen
        if (_uiState.value.selectedScreen == screenId) {
            _uiState.value = _uiState.value.copy(selectedScreen = updatedScreens.first().id)
        }

        updateProfile(updatedProfile)
        Log.i(TAG, "Removed screen: $screenId")
    }

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

    companion object {
        private const val TAG = "LayoutBuilderViewModel"
    }
}

