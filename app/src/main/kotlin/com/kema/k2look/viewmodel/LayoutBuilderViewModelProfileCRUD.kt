package com.kema.k2look.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.kema.k2look.data.SeedProfile
import com.kema.k2look.model.DataFieldProfile
import kotlinx.coroutines.launch

/**
 * Profile loading and CRUD extension functions for [LayoutBuilderViewModel].
 *
 * Covers: profile migration/seeding, loading, selection, apply-to-glasses, create, duplicate,
 * delete, update. All functions are extensions so they stay decoupled from the main class body
 * while still sharing its internal state.
 */
private const val TAG = "LayoutBuilderViewModel"

// ── Load / migrate ─────────────────────────────────────────────────────────

/**
 * Load all profiles from the repository.
 *
 * Migration rules (run once, transparent to the user):
 * - Empty storage → seed the Default profile and save it.
 * - No profile with id "default" found (old install that never stored the hardcoded default) → save
 * the seed and prepend it so it's discoverable.
 */
internal fun LayoutBuilderViewModel.loadProfiles() {
    viewModelScope.launch {
        try {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val allProfiles = reloadAllProfiles()

            val activeProfile =
                    _uiState.value.activeProfile?.let { current ->
                        allProfiles.find { it.id == current.id }
                    }
                            ?: allProfiles.first()

            _uiState.value =
                    _uiState.value.copy(
                            profiles = allProfiles,
                            activeProfile = activeProfile,
                            isLoading = false
                    )

            Log.i(TAG, "Loaded ${allProfiles.size} profile(s), active: ${activeProfile.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading profiles", e)
            _uiState.value =
                    _uiState.value.copy(
                            error = "Failed to load profiles: ${e.message}",
                            isLoading = false
                    )
        }
    }
}

// ── Selection & apply ──────────────────────────────────────────────────────

/** Select a profile by ID */
fun LayoutBuilderViewModel.selectProfile(profileId: String) {
    val profile = _uiState.value.profiles.find { it.id == profileId }
    if (profile != null) {
        _uiState.value = _uiState.value.copy(activeProfile = profile)
        Log.i(TAG, "Selected profile: ${profile.name}")
    } else {
        Log.w(TAG, "Profile not found: $profileId")
    }
}

/** Apply the active profile to glasses for display */
fun LayoutBuilderViewModel.applyProfileToGlasses(profile: DataFieldProfile? = null) {
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

        val bridgeInstance = bridge
        if (bridgeInstance != null) {
            bridgeInstance.setActiveProfile(targetProfile)
            _uiState.value =
                    _uiState.value.copy(successMessage = "'${targetProfile.name}' sent to glasses.")
            Log.i(TAG, "✅ Profile applied to bridge successfully")
        } else {
            Log.w(TAG, "⚠️ Bridge not available, profile not applied to glasses")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error applying profile to glasses", e)
        _uiState.value = _uiState.value.copy(error = "Failed to apply profile: ${e.message}")
    }
}

// ── Create / duplicate / delete / update ──────────────────────────────────

/** Create a new profile */
fun LayoutBuilderViewModel.createProfile(name: String) {
    viewModelScope.launch {
        try {
            if (_uiState.value.profiles.any { it.name.equals(name, ignoreCase = true) }) {
                _uiState.value =
                        _uiState.value.copy(error = "A profile named '$name' already exists")
                return@launch
            }

            val newProfile =
                    SeedProfile.build()
                            .copy(
                                    id = java.util.UUID.randomUUID().toString(),
                                    name = name,
                                    createdAt = System.currentTimeMillis(),
                                    modifiedAt = System.currentTimeMillis()
                            )

            repository.saveProfile(newProfile)
            val allProfiles = reloadAllProfiles()

            _uiState.value =
                    _uiState.value.copy(
                            profiles = allProfiles,
                            activeProfile = newProfile,
                            isLoading = false
                    )

            Log.i(TAG, "Created and selected profile: $name (id: ${newProfile.id})")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating profile", e)
            _uiState.value = _uiState.value.copy(error = "Failed to create profile: ${e.message}")
        }
    }
}

/** Duplicate an existing profile */
fun LayoutBuilderViewModel.duplicateProfile(profileId: String, newName: String) {
    viewModelScope.launch {
        try {
            if (_uiState.value.profiles.any { it.name.equals(newName, ignoreCase = true) }) {
                _uiState.value =
                        _uiState.value.copy(error = "A profile named '$newName' already exists")
                return@launch
            }

            val originalProfile = _uiState.value.profiles.find { it.id == profileId }
            if (originalProfile != null) {
                val duplicatedProfile =
                        originalProfile.copy(
                                id = java.util.UUID.randomUUID().toString(),
                                name = newName,
                                createdAt = System.currentTimeMillis(),
                                modifiedAt = System.currentTimeMillis()
                        )

                repository.saveProfile(duplicatedProfile)
                val allProfiles = reloadAllProfiles()

                _uiState.value =
                        _uiState.value.copy(
                                profiles = allProfiles,
                                activeProfile = duplicatedProfile,
                                isLoading = false
                        )

                Log.i(
                        TAG,
                        "Duplicated profile: ${originalProfile.name} → $newName (id: ${duplicatedProfile.id})"
                )
            } else {
                Log.w(TAG, "Profile not found for duplication: $profileId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error duplicating profile", e)
            _uiState.value =
                    _uiState.value.copy(error = "Failed to duplicate profile: ${e.message}")
        }
    }
}

/** Delete a profile. Cannot delete the last remaining profile. */
fun LayoutBuilderViewModel.deleteProfile(profileId: String) {
    viewModelScope.launch {
        try {
            repository.deleteProfile(profileId)

            var allProfiles = reloadAllProfiles()

            // If all profiles were deleted, auto-create a fresh Default
            if (allProfiles.isEmpty()) {
                val defaultProfile =
                        SeedProfile.build()
                                .copy(
                                        id = java.util.UUID.randomUUID().toString(),
                                        name = "Default",
                                        createdAt = System.currentTimeMillis(),
                                        modifiedAt = System.currentTimeMillis()
                                )
                repository.saveProfile(defaultProfile)
                allProfiles = reloadAllProfiles()
            }

            // If the deleted profile was active, switch to the first remaining profile
            val activeProfile =
                    if (_uiState.value.activeProfile?.id == profileId) {
                        allProfiles.first()
                    } else {
                        _uiState.value.activeProfile?.let { current ->
                            allProfiles.find { it.id == current.id }
                        }
                                ?: allProfiles.first()
                    }

            _uiState.value =
                    _uiState.value.copy(profiles = allProfiles, activeProfile = activeProfile)

            Log.i(TAG, "Deleted profile: $profileId, switched to: ${activeProfile.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting profile", e)
            _uiState.value = _uiState.value.copy(error = "Failed to delete profile: ${e.message}")
        }
    }
}

/** Save an updated profile to the repository and refresh state */
fun LayoutBuilderViewModel.updateProfile(profile: DataFieldProfile) {
    viewModelScope.launch {
        try {
            repository.saveProfile(profile)
            // Invalidate cached config on glasses so next activation re-uploads
            bridge?.invalidateProfileConfig(profile.id)

            val allProfiles = reloadAllProfiles()
            val updatedActiveProfile = allProfiles.find { it.id == profile.id } ?: profile

            _uiState.value =
                    _uiState.value.copy(
                            profiles = allProfiles,
                            activeProfile = updatedActiveProfile,
                            isLoading = false
                    )

            Log.i(TAG, "Updated profile: ${profile.name} (id: ${profile.id})")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile", e)
            _uiState.value = _uiState.value.copy(error = "Failed to update profile: ${e.message}")
        }
    }
}
