package com.kema.k2look.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kema.k2look.data.DataFieldRegistry
import com.kema.k2look.model.DataFieldProfile
import io.hammerhead.karooext.models.RideProfile

/**
 * Screen for managing DataField profiles (create, delete, duplicate)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileManagementScreen(
    profiles: List<DataFieldProfile>,
    activeRideProfile: RideProfile?,
    isRiding: Boolean,
    karooSyncEnabled: Boolean,
    onBack: () -> Unit,
    onCreateProfile: (name: String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onDuplicateProfile: (String, String) -> Unit,
    onToggleKarooSync: (Boolean) -> Unit,
    onImportFromKaroo: (RideProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var showDuplicateDialog by remember { mutableStateOf<String?>(null) }
    var showImportPreview by remember { mutableStateOf(false) }

    // Dismiss import preview if a ride starts while it is open
    LaunchedEffect(isRiding) {
        if (isRiding) showImportPreview = false
    }

    // Show Karoo suggestion when sync is on, not riding, profile exists, and no name match
    val hasKarooSuggestion = karooSyncEnabled
        && !isRiding
        && activeRideProfile != null
        && profiles.none { it.name.equals(activeRideProfile.name, ignoreCase = true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Profiles") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true }
            ) {
                Icon(Icons.Default.Add, "Create Profile")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Karoo Sync toggle
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Karoo Sync",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Auto-switch profiles when Karoo profile changes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Switch(
                            checked = karooSyncEnabled,
                            onCheckedChange = onToggleKarooSync
                        )
                    }
                }
            }

            // From Karoo suggestion
            if (hasKarooSuggestion && activeRideProfile != null) {
                item {
                    KarooSuggestionCard(
                        rideProfile = activeRideProfile,
                        onImport = { showImportPreview = true }
                    )
                }
            }
            // User profiles (exclude default)
            val userProfiles = profiles.filter { !it.isDefault }

            if (userProfiles.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No custom profiles yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.padding(vertical = 8.dp))
                            Text(
                                text = "Tap + to create your first profile",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                items(userProfiles) { profile ->
                    ProfileCard(
                        profile = profile,
                        onDuplicate = { showDuplicateDialog = profile.id },
                        onDelete = { showDeleteDialog = profile.id }
                    )
                }
            }

            // Default profile (shown at bottom)
            item {
                Spacer(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "System Profiles",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            items(profiles.filter { it.isDefault }) { profile ->
                ProfileCard(
                    profile = profile,
                    onDuplicate = { showDuplicateDialog = profile.id },
                    onDelete = null // Cannot delete default
                )
            }
        }
    }

    // Import Preview Dialog
    if (showImportPreview && activeRideProfile != null) {
        ImportPreviewDialog(
            rideProfile = activeRideProfile,
            onDismiss = { showImportPreview = false },
            onConfirm = {
                onImportFromKaroo(activeRideProfile)
                showImportPreview = false
            }
        )
    }

    // Create Profile Dialog
    if (showCreateDialog) {
        CreateProfileDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                onCreateProfile(name)
                showCreateDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    showDeleteDialog?.let { profileId ->
        val profile = profiles.find { it.id == profileId }
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Profile") },
            text = { Text("Are you sure you want to delete '${profile?.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProfile(profileId)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Duplicate Profile Dialog
    showDuplicateDialog?.let { profileId ->
        val profile = profiles.find { it.id == profileId }
        DuplicateProfileDialog(
            originalName = profile?.name ?: "",
            onDismiss = { showDuplicateDialog = null },
            onDuplicate = { newName ->
                onDuplicateProfile(profileId, newName)
                showDuplicateDialog = null
            }
        )
    }
}

@Composable
private fun ProfileCard(
    profile: DataFieldProfile,
    onDuplicate: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.padding(vertical = 2.dp))
                Text(
                    text = "${profile.screens.size} screen(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                if (profile.isReadOnly) {
                    Text(
                        text = "Read-only",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Row {
                IconButton(onClick = onDuplicate) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Duplicate Profile",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Profile",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateProfileDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String) -> Unit
) {
    var profileName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Profile") },
        text = {
            Column {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("Profile Name") },
                    placeholder = { Text("e.g., Training, Race, Recovery") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "Starts with default metrics (Speed, Distance, Time). Customize after creation!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(profileName) },
                enabled = profileName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


@Composable
private fun DuplicateProfileDialog(
    originalName: String,
    onDismiss: () -> Unit,
    onDuplicate: (String) -> Unit
) {
    var newName by remember { mutableStateOf("$originalName (Copy)") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Duplicate Profile") },
        text = {
            Column {
                Text("Enter a name for the duplicated profile:")
                Spacer(modifier = Modifier.padding(vertical = 8.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Profile Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onDuplicate(newName) },
                enabled = newName.isNotBlank()
            ) {
                Text("Duplicate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun KarooSuggestionCard(
    rideProfile: RideProfile,
    onImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val screenCount = remember(rideProfile) {
        rideProfile.pages.count { page ->
            !page.mapPage && page.elements.any { el ->
                DataFieldRegistry.ALL_FIELDS.any { f -> f.karooStreamType == el.dataTypeId }
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Import from Karoo",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rideProfile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "$screenCount screen${if (screenCount != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                Button(onClick = onImport) {
                    Text("Import")
                }
            }
        }
    }
}

@Composable
private fun ImportPreviewDialog(
    rideProfile: RideProfile,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val screenSummaries = remember(rideProfile) {
        rideProfile.pages
            .filter { !it.mapPage }
            .mapNotNull { page ->
                val fieldNames = page.elements.mapNotNull { element ->
                    DataFieldRegistry.ALL_FIELDS
                        .find { it.karooStreamType == element.dataTypeId }?.name
                }
                if (fieldNames.isEmpty()) null
                else fieldNames.take(6).joinToString(" · ")
            }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import '${rideProfile.name}'?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (screenSummaries.isNotEmpty()) {
                    Text(
                        text = "Generates ${screenSummaries.size} screen${if (screenSummaries.size != 1) "s" else ""} from your Karoo configuration:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.padding(vertical = 2.dp))
                    screenSummaries.forEachIndexed { index, summary ->
                        Text(
                            text = "Screen ${index + 1}: $summary",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                } else {
                    Text(
                        text = "No recognisable fields found in this Karoo profile.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = screenSummaries.isNotEmpty()
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

