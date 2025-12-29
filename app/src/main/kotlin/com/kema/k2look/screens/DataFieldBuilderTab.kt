package com.kema.k2look.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kema.k2look.model.LayoutDataField
import com.kema.k2look.viewmodel.LayoutBuilderViewModel

/**
 * Main DataField Builder tab
 */
@Composable
fun DataFieldBuilderTab(
    modifier: Modifier = Modifier,
    mainViewModel: com.kema.k2look.viewmodel.MainViewModel? = null,
    viewModel: LayoutBuilderViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFieldConfig by remember { mutableStateOf(false) }
    var editingField by remember { mutableStateOf<LayoutDataField?>(null) }
    var justAddedZone by remember { mutableStateOf<Pair<Int, String>?>(null) }

    // Set bridge when mainViewModel is available
    androidx.compose.runtime.LaunchedEffect(mainViewModel) {
        mainViewModel?.let { vm ->
            viewModel.setBridge(vm.getBridge())
            android.util.Log.d("DataFieldBuilderTab", "Bridge set in LayoutBuilderViewModel")
        }
    }

    // Auto-open config dialog when a field is added
    androidx.compose.runtime.LaunchedEffect(uiState.activeProfile?.screens) {
        justAddedZone?.let { (screenId, zoneId) ->
            val screen = uiState.activeProfile?.screens?.find { it.id == screenId }
            val field = screen?.dataFields?.find { it.zoneId == zoneId }
            if (field != null) {
                editingField = field
                showFieldConfig = true
                justAddedZone = null
            }
        }
    }

    // Show profile management screen if requested
    if (uiState.showProfileManagement) {
        ProfileManagementScreen(
            profiles = uiState.profiles,
            onBack = { viewModel.setShowProfileManagement(false) },
            onCreateProfile = { name ->
                viewModel.createProfile(name)
            },
            onDeleteProfile = { profileId ->
                viewModel.deleteProfile(profileId)
            },
            onDuplicateProfile = { profileId, newName ->
                viewModel.duplicateProfile(profileId, newName)
            }
        )
        return
    }

    // Show field configuration dialog if requested
    if (showFieldConfig && editingField != null) {
        // Find which screen this field belongs to
        val fieldScreen = uiState.activeProfile?.screens?.find { screen ->
            screen.dataFields.any { it.zoneId == editingField!!.zoneId }
        }

        FieldConfigurationDialog(
            field = editingField!!,
            onDismiss = {
                showFieldConfig = false
                editingField = null
            },
            onSave = { updatedField ->
                // Use the screen where the field actually exists, not the currently selected tab!
                val screenIdToUpdate = fieldScreen?.id ?: uiState.selectedScreen
                android.util.Log.i(
                    "DataFieldBuilder",
                    "Saving field to screen $screenIdToUpdate (currently viewing ${uiState.selectedScreen})"
                )
                viewModel.updateField(screenIdToUpdate, updatedField)
                showFieldConfig = false
                editingField = null
            }
        )
    }

    // Main builder UI
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp) // Further reduced from 8.dp
    ) {
        // Profile Selector
        ProfileSelectorCard(
            profiles = uiState.profiles,
            activeProfile = uiState.activeProfile,
            onProfileSelected = { profileId ->
                viewModel.selectProfile(profileId)
            },
            onManageProfiles = {
                viewModel.setShowProfileManagement(true)
            }
        )

        // Loading indicator
        if (uiState.isLoading) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.padding(vertical = 8.dp))
                Text("Loading profiles...")
            }
        }

        // Active profile content
        uiState.activeProfile?.let { profile ->
            Spacer(modifier = Modifier.padding(vertical = 4.dp))

            // Screen Tabs
            if (profile.screens.isNotEmpty()) {
                // Calculate validSelectedScreen once at the top to avoid race conditions
                android.util.Log.d(
                    "DataFieldBuilder",
                    "Recomposing: screens=${profile.screens.map { it.id }}, selectedScreen=${uiState.selectedScreen}"
                )

                val validSelectedScreen =
                    if (profile.screens.any { it.id == uiState.selectedScreen }) {
                        android.util.Log.d(
                            "DataFieldBuilder",
                            "Selected screen ${uiState.selectedScreen} exists"
                        )
                        uiState.selectedScreen
                    } else {
                        val fallback = profile.screens.firstOrNull()?.id ?: 1
                        android.util.Log.w(
                            "DataFieldBuilder",
                            "Selected screen ${uiState.selectedScreen} NOT FOUND, falling back to $fallback"
                        )
                        fallback
                    }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Capture tab count and selected index together to ensure consistency
                    val tabCount = profile.screens.size
                    val rawIndex = profile.screens.indexOfFirst { it.id == validSelectedScreen }
                    val safeIndex = when {
                        rawIndex < 0 -> 0
                        rawIndex >= tabCount -> tabCount - 1
                        else -> rawIndex
                    }.coerceIn(0, maxOf(0, tabCount - 1))

                    android.util.Log.d(
                        "DataFieldBuilder",
                        "rawIndex=$rawIndex, safeIndex=$safeIndex, tabCount=$tabCount"
                    )

                    if (tabCount > 0) {
                        // Use key to force recreate ScrollableTabRow when screen count changes
                        androidx.compose.runtime.key(tabCount) {
                            ScrollableTabRow(
                                selectedTabIndex = safeIndex,
                                modifier = Modifier.weight(1f)
                            ) {
                                profile.screens.forEach { screen ->
                                    Tab(
                                        selected = screen.id == validSelectedScreen,
                                        onClick = { viewModel.selectScreen(screen.id) },
                                        text = {
                                            Text(
                                                text = "Screen ${screen.id}",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Add screen button
                    if (!profile.isReadOnly) {
                        IconButton(
                            onClick = { viewModel.addScreen() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Screen",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Remove screen button (only show if more than 1 screen)
                    if (!profile.isReadOnly && profile.screens.size > 1) {
                        IconButton(
                            onClick = { viewModel.removeScreen(validSelectedScreen) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove Screen",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))

                // Screen Editor - use the same validSelectedScreen calculated above
                val currentScreen = profile.screens.find { it.id == validSelectedScreen }
                if (currentScreen != null) {
                    ZoneBasedScreenEditor(
                        screen = currentScreen,
                        onTemplateChange = { newTemplateId ->
                            viewModel.changeScreenTemplate(validSelectedScreen, newTemplateId)
                        },
                        onFieldAdd = { zoneId, dataField ->
                            // Add the field to the zone
                            viewModel.assignMetricToZone(validSelectedScreen, zoneId, dataField)
                            // Mark that we just added a field so LaunchedEffect can open config dialog
                            justAddedZone = Pair(validSelectedScreen, zoneId)
                        },
                        onFieldEdit = { field ->
                            editingField = field
                            showFieldConfig = true
                        },
                        onFieldRemove = { zoneId ->
                            viewModel.removeMetricFromZone(validSelectedScreen, zoneId)
                        }
                    )
                } else {
                    Text(
                        text = "Screen not found",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.padding(vertical = 16.dp))

            // Read-only indicator - moved above buttons for better visibility
            if (profile.isReadOnly) {
                Text(
                    text = "⚠️ This is a read-only profile. Duplicate it to make changes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Preview button - Commented out, moved to future updates
                // See docs/Future-Updates.md for implementation plan
                /*
                Button(
                    onClick = { /* TODO: Preview on glasses */ },
                    modifier = Modifier.weight(1f),
                    enabled = false
                ) {
                    Text("Preview")
                }
                */
                Button(
                    onClick = { /* TODO: Build & Send to Glasses */ },
                    modifier = Modifier.fillMaxWidth(), // Changed from weight(1f) since it's now the only button
                    enabled = false
                ) {
                    Text("Build & Send")
                }
            }
        }

        // Error message
        uiState.error?.let { error ->
            Snackbar(
                modifier = Modifier.padding(8.dp),
                action = {
                    Button(onClick = { viewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(error)
            }
        }
    }
}

