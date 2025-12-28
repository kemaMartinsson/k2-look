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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kema.k2look.model.DataFieldProfile

/**
 * Screen for managing DataField profiles (create, delete, duplicate)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileManagementScreen(
    profiles: List<DataFieldProfile>,
    onBack: () -> Unit,
    onCreateProfile: (name: String, template: String?) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onDuplicateProfile: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var showDuplicateDialog by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Profiles") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
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

            // Default profile (read-only, shown at bottom)
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

    // Create Profile Dialog
    if (showCreateDialog) {
        CreateProfileDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, template ->
                onCreateProfile(name, template)
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
    onCreate: (name: String, template: String?) -> Unit
) {
    var profileName by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Profile") },
        text = {
            Column {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("Profile Name") },
                    placeholder = { Text("e.g., Road Bike") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.padding(vertical = 12.dp))

                Text(
                    text = "Start from template:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.padding(vertical = 4.dp))

                // Template options
                TemplateOption(
                    label = "Blank (Default metrics)",
                    description = "Speed, Distance, Time",
                    selected = selectedTemplate == null,
                    onClick = { selectedTemplate = null }
                )
                TemplateOption(
                    label = "Road Bike (with Power)",
                    description = "Speed, Power, Heart Rate",
                    selected = selectedTemplate == "template_road",
                    onClick = { selectedTemplate = "template_road" }
                )
                TemplateOption(
                    label = "Gravel Bike (no Power)",
                    description = "Speed, Heart Rate, Distance",
                    selected = selectedTemplate == "template_gravel",
                    onClick = { selectedTemplate = "template_gravel" }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(profileName, selectedTemplate) },
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
private fun TemplateOption(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
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

