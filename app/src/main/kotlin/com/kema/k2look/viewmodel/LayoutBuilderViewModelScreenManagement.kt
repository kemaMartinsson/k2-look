package com.kema.k2look.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.kema.k2look.model.DataField
import com.kema.k2look.model.IconSize
import com.kema.k2look.model.LayoutDataField
import com.kema.k2look.model.LayoutScreen
import com.kema.k2look.model.VisualizationType
import kotlinx.coroutines.launch

/**
 * Screen and field management extension functions for [LayoutBuilderViewModel].
 *
 * Covers: add/update/remove data fields, add/remove screens, change template, and the zone-based
 * API aliases. All profile mutations go through [updateProfile].
 */
private const val TAG = "LayoutBuilderViewModel"

// ── Field management ───────────────────────────────────────────────────────

/** Add a datafield to a specific zone in the active profile's current screen */
fun LayoutBuilderViewModel.addFieldToScreen(screenId: Int, zoneId: String, dataField: DataField) {
    val profile = _uiState.value.activeProfile ?: return

    val updatedScreens =
            profile.screens.map { screen ->
                if (screen.id == screenId) {
                    val template = screen.getTemplate()

                    if (template.zones.none { it.id == zoneId }) {
                        Log.w(TAG, "Zone $zoneId is not valid for template ${template.id}")
                        return@map screen
                    }

                    if (screen.dataFields.any { it.zoneId == zoneId }) {
                        Log.w(TAG, "Zone $zoneId is already occupied")
                        return@map screen
                    }

                    if (screen.dataFields.size >= template.maxFields) {
                        Log.w(TAG, "Screen already has ${template.maxFields} fields")
                        return@map screen
                    }

                    val newField =
                            LayoutDataField(
                                    dataField = dataField,
                                    zoneId = zoneId,
                                    showLabel = true,
                                    showUnit = true,
                                    showIcon = dataField.icon28 != null || dataField.icon40 != null,
                                    iconSize = IconSize.SMALL
                            )

                    screen.copy(dataFields = screen.dataFields + newField)
                } else {
                    screen
                }
            }

    updateProfile(profile.copy(screens = updatedScreens, modifiedAt = System.currentTimeMillis()))
    Log.i(TAG, "Added field ${dataField.name} to screen $screenId in zone $zoneId")
}

/** Update a datafield in the active profile */
fun LayoutBuilderViewModel.updateField(screenId: Int, updatedField: LayoutDataField) {
    val profile = _uiState.value.activeProfile ?: return

    Log.i(TAG, "🔄 updateField called: screenId=$screenId, zoneId=${updatedField.zoneId}")

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
        Log.w(
                TAG,
                "  ⚠️ WARNING: No existing field in zone ${updatedField.zoneId} on screen $screenId"
        )
    }

    val updatedScreens =
            profile.screens.map { screen ->
                if (screen.id == screenId) {
                    screen.copy(
                            dataFields =
                                    screen.dataFields.map { field ->
                                        if (field.zoneId == updatedField.zoneId) {
                                            Log.i(
                                                    TAG,
                                                    "  ✅ Found matching zone ${field.zoneId}, updating field"
                                            )
                                            updatedField
                                        } else field
                                    }
                    )
                } else screen
            }

    Log.i(TAG, "  📝 Calling updateProfile to save changes")
    updateProfile(profile.copy(screens = updatedScreens, modifiedAt = System.currentTimeMillis()))
    Log.i(TAG, "✅ Updated field in zone ${updatedField.zoneId} in screen $screenId")
}

/** Remove a datafield from the active profile */
fun LayoutBuilderViewModel.removeField(screenId: Int, zoneId: String) {
    val profile = _uiState.value.activeProfile ?: return
    val updatedScreens =
            profile.screens.map { screen ->
                if (screen.id == screenId)
                        screen.copy(dataFields = screen.dataFields.filter { it.zoneId != zoneId })
                else screen
            }
    updateProfile(profile.copy(screens = updatedScreens, modifiedAt = System.currentTimeMillis()))
    Log.i(TAG, "Removed field at zone $zoneId from screen $screenId")
}

// ── Screen management ──────────────────────────────────────────────────────

/** Add a new screen to the active profile */
fun LayoutBuilderViewModel.addScreen() {
    val profile = _uiState.value.activeProfile ?: return

    Log.d(
            TAG,
            "addScreen: START - current screens: ${profile.screens.map { it.id }}, selectedScreen: ${_uiState.value.selectedScreen}"
    )

    val nextScreenId = (profile.screens.maxOfOrNull { it.id } ?: 0) + 1
    Log.d(TAG, "addScreen: Creating screen $nextScreenId")

    val newScreen =
            LayoutScreen(id = nextScreenId, name = "Screen $nextScreenId", dataFields = emptyList())

    val updatedProfile =
            profile.copy(
                    screens = profile.screens + newScreen,
                    modifiedAt = System.currentTimeMillis()
            )

    Log.d(TAG, "addScreen: Updated profile screens: ${updatedProfile.screens.map { it.id }}")

    viewModelScope.launch {
        try {
            Log.d(TAG, "addScreen: Saving profile to repository")
            repository.saveProfile(updatedProfile)
            val allProfiles = reloadAllProfiles()
            val updatedActiveProfile =
                    allProfiles.find { it.id == updatedProfile.id } ?: updatedProfile

            Log.d(
                    TAG,
                    "addScreen: BEFORE state update - selectedScreen=${_uiState.value.selectedScreen}"
            )
            _uiState.value =
                    _uiState.value.copy(
                            profiles = allProfiles,
                            activeProfile = updatedActiveProfile,
                            selectedScreen = nextScreenId,
                            isLoading = false
                    )
            Log.d(
                    TAG,
                    "addScreen: AFTER state update - selectedScreen=${_uiState.value.selectedScreen}, profile screens=${updatedActiveProfile.screens.map { it.id }}"
            )

            Log.i(TAG, "Added new screen: Screen $nextScreenId")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding screen", e)
            _uiState.value = _uiState.value.copy(error = "Failed to add screen: ${e.message}")
        }
    }
}

/** Remove a screen from the active profile. Cannot remove if it's the only screen. */
fun LayoutBuilderViewModel.removeScreen(screenId: Int) {
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

/** Change the layout template for a screen */
fun LayoutBuilderViewModel.changeScreenTemplate(screenId: Int, newTemplateId: String) {
    val profile = _uiState.value.activeProfile ?: return
    val screen = profile.screens.find { it.id == screenId } ?: return
    val newTemplate = com.kema.k2look.layout.LayoutTemplateRegistry.getTemplate(newTemplateId)

    // Preserve as many existing fields as possible
    val preservedFields = screen.dataFields.take(newTemplate.maxFields)

    val mappedFields =
            preservedFields.mapIndexed { index, field ->
                val newZone = newTemplate.zones.getOrNull(index)
                if (newZone != null) {
                    field.copy(
                            zoneId = newZone.id,
                            visualizationType = field.visualizationType ?: VisualizationType.TEXT
                    )
                } else field
            }

    val updatedProfile =
            profile.copy(
                    screens =
                            profile.screens.map {
                                if (it.id == screenId)
                                        it.copy(
                                                templateId = newTemplateId,
                                                dataFields = mappedFields
                                        )
                                else it
                            },
                    modifiedAt = System.currentTimeMillis()
            )

    updateProfile(updatedProfile)
    Log.i(
            TAG,
            "Changed screen $screenId template to $newTemplateId, preserved ${mappedFields.size} fields"
    )
}

// ── Zone-based convenience aliases ────────────────────────────────────────

/** Assign a metric to a specific zone (zone-based API) */
fun LayoutBuilderViewModel.assignMetricToZone(screenId: Int, zoneId: String, dataField: DataField) {
    addFieldToScreen(screenId, zoneId, dataField)
}

/** Remove metric from zone (zone-based API) */
fun LayoutBuilderViewModel.removeMetricFromZone(screenId: Int, zoneId: String) {
    removeField(screenId, zoneId)
}
