package com.kema.k2look.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kema.k2look.service.ActiveLookService
import com.kema.k2look.service.KarooDataService
import com.kema.k2look.viewmodel.MainViewModel

/** Karoo system service connection status row */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KarooConnectionCard(uiState: MainViewModel.UiState) {
        Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.background
                        ),
                shape = androidx.compose.ui.graphics.RectangleShape
        ) {
                Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Text(
                                text = "Karoo service:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                        )
                        Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                                Icon(
                                        imageVector =
                                                when (uiState.connectionState) {
                                                        is KarooDataService.ConnectionState.Connected ->
                                                                Icons.Default.CheckCircle
                                                        else -> Icons.Default.Close
                                                },
                                        contentDescription = null,
                                        tint =
                                                when (uiState.connectionState) {
                                                        is KarooDataService.ConnectionState.Connected ->
                                                                MaterialTheme.colorScheme.primary
                                                        is KarooDataService.ConnectionState.Connecting,
                                                        is KarooDataService.ConnectionState.Reconnecting ->
                                                                MaterialTheme.colorScheme.secondary
                                                        else -> MaterialTheme.colorScheme.error
                                                },
                                        modifier = Modifier.height(16.dp)
                                )
                                Text(
                                        text =
                                                when (uiState.connectionState) {
                                                        is KarooDataService.ConnectionState.Connected ->
                                                                "Connected"
                                                        is KarooDataService.ConnectionState.Connecting ->
                                                                "Connecting..."
                                                        is KarooDataService.ConnectionState.Reconnecting ->
                                                                "Reconnecting..."
                                                        is KarooDataService.ConnectionState.Disconnected ->
                                                                "Disconnected"
                                                        is KarooDataService.ConnectionState.Error ->
                                                                "Error"
                                                },
                                        style = MaterialTheme.typography.bodySmall,
                                        color =
                                                when (uiState.connectionState) {
                                                        is KarooDataService.ConnectionState.Connected ->
                                                                MaterialTheme.colorScheme.primary
                                                        is KarooDataService.ConnectionState.Connecting,
                                                        is KarooDataService.ConnectionState.Reconnecting ->
                                                                MaterialTheme.colorScheme.secondary
                                                        else -> MaterialTheme.colorScheme.error
                                                },
                                        fontWeight = FontWeight.Bold
                                )
                        }
                }
        }
}

/** ActiveLook glasses connection status card — shows all pairing/connected/disconnected states */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GlassesConnectionCard(uiState: MainViewModel.UiState, viewModel: MainViewModel) {
        Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.background
                        ),
                shape = androidx.compose.ui.graphics.RectangleShape
        ) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        when (val state = uiState.activeLookState) {
                                is ActiveLookService.ConnectionState.Connected -> {
                                        // Connected state
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Text(
                                                        text = "Glasses: ${state.glasses.name}",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Medium
                                                )
                                                Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = "Connected",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.height(20.dp)
                                                )
                                        }
                                }
                                is ActiveLookService.ConnectionState.Connecting -> {
                                        // Connecting state - show 3 stages
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                                Text(
                                                        text = "Pairing with glasses:",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Medium,
                                                        modifier = Modifier.padding(bottom = 8.dp)
                                                )

                                                // Stage 1: Scanning (completed)
                                                Row(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(vertical = 2.dp),
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Icon(
                                                                imageVector =
                                                                        Icons.Default.CheckCircle,
                                                                contentDescription = null,
                                                                tint =
                                                                        MaterialTheme.colorScheme
                                                                                .primary,
                                                                modifier = Modifier.height(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                                text = "1. Scanning",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodySmall,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        )
                                                }

                                                // Stage 2: Found (completed)
                                                Row(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(vertical = 2.dp),
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Icon(
                                                                imageVector =
                                                                        Icons.Default.CheckCircle,
                                                                contentDescription = null,
                                                                tint =
                                                                        MaterialTheme.colorScheme
                                                                                .primary,
                                                                modifier = Modifier.height(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                                text = "2. Found glasses",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodySmall,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        )
                                                }

                                                // Stage 3: Connecting (in progress)
                                                Row(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(vertical = 2.dp),
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        androidx.compose.material3
                                                                .CircularProgressIndicator(
                                                                        modifier =
                                                                                Modifier.height(
                                                                                                16.dp
                                                                                        )
                                                                                        .width(
                                                                                                16.dp
                                                                                        ),
                                                                        strokeWidth = 2.dp,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .secondary
                                                                )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                                text = "3. Connecting...",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodySmall,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .secondary,
                                                                fontWeight = FontWeight.Bold
                                                        )
                                                }
                                        }
                                }
                                is ActiveLookService.ConnectionState.Scanning -> {
                                        // Scanning state - show scanning in progress
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                                Text(
                                                        text = "Pairing with glasses:",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Medium,
                                                        modifier = Modifier.padding(bottom = 8.dp)
                                                )

                                                // Stage 1: Scanning (in progress)
                                                Row(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(vertical = 2.dp),
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        androidx.compose.material3
                                                                .CircularProgressIndicator(
                                                                        modifier =
                                                                                Modifier.height(
                                                                                                16.dp
                                                                                        )
                                                                                        .width(
                                                                                                16.dp
                                                                                        ),
                                                                        strokeWidth = 2.dp,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .secondary
                                                                )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                                text = "1. Scanning for glasses...",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodySmall,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .secondary,
                                                                fontWeight = FontWeight.Bold
                                                        )
                                                }

                                                // Stage 2: Found (pending)
                                                Row(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(vertical = 2.dp),
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = null,
                                                                tint =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                                                .copy(alpha = 0.3f),
                                                                modifier = Modifier.height(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                                text = "2. Find glasses",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodySmall,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                                                .copy(alpha = 0.5f)
                                                        )
                                                }

                                                // Stage 3: Connecting (pending)
                                                Row(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(vertical = 2.dp),
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = null,
                                                                tint =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                                                .copy(alpha = 0.3f),
                                                                modifier = Modifier.height(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                                text = "3. Connect",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodySmall,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                                                .copy(alpha = 0.5f)
                                                        )
                                                }

                                                // Show discovered glasses count if any
                                                if (uiState.discoveredGlasses.isNotEmpty()) {
                                                        Text(
                                                                text =
                                                                        "Found ${uiState.discoveredGlasses.size} device(s)",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodySmall,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .primary,
                                                                modifier =
                                                                        Modifier.padding(
                                                                                top = 4.dp,
                                                                                start = 24.dp
                                                                        )
                                                        )
                                                }
                                        }
                                }
                                else -> {
                                        // Disconnected state
                                        Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement =
                                                                Arrangement.SpaceBetween,
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Text(
                                                                text = "No glasses connected",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyMedium,
                                                                fontWeight = FontWeight.Medium
                                                        )
                                                        Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = "Disconnected",
                                                                tint =
                                                                        MaterialTheme.colorScheme
                                                                                .error,
                                                                modifier = Modifier.height(20.dp)
                                                        )
                                                }
                                                Button(
                                                        onClick = { viewModel.startGlassesScan() },
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(top = 8.dp)
                                                ) {
                                                        Text(
                                                                "Connect glasses",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodySmall
                                                        )
                                                }
                                        }
                                }
                        }
                }
        }
}

/** Auto-reconnect settings card — timeout, auto-connect/disconnect switches, forget button */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReconnectSettingsCard(viewModel: MainViewModel, uiState: MainViewModel.UiState) {
        Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.background
                        ),
                shape = androidx.compose.ui.graphics.RectangleShape
        ) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(
                                text = "Auto-Reconnect Settings",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                                text =
                                        "During an active ride, the app will continuously attempt to reconnect to glasses if disconnected.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Glasses before ride - Auto-connect switch
                        var autoConnectEnabled by remember {
                                mutableStateOf(
                                        viewModel.preferencesManager
                                                .isAutoConnectActiveLookEnabled()
                                )
                        }

                        Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                                text = "Glasses before ride",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                                text = "Connect",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                }
                                androidx.compose.material3.Switch(
                                        checked = autoConnectEnabled,
                                        onCheckedChange = { enabled ->
                                                autoConnectEnabled = enabled
                                                viewModel.setAutoConnectGlasses(enabled)
                                        }
                                )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Glasses at ride end - Disconnect switch
                        var disconnectWhenIdle by remember {
                                mutableStateOf(
                                        viewModel.preferencesManager.isDisconnectWhenIdleEnabled()
                                )
                        }

                        Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                                text = "Glasses at ride end",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                                text = "Disconnect",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                }
                                androidx.compose.material3.Switch(
                                        checked = disconnectWhenIdle,
                                        onCheckedChange = { enabled ->
                                                disconnectWhenIdle = enabled
                                                viewModel.setDisconnectWhenIdle(enabled)
                                        }
                                )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Forget glasses button
                        val hasSavedGlasses =
                                viewModel.preferencesManager.getLastConnectedGlassesAddress() !=
                                        null

                        Button(
                                onClick = { viewModel.forgetGlasses() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = hasSavedGlasses
                        ) { Text("Forget glasses", style = MaterialTheme.typography.bodySmall) }

                        Text(
                                text =
                                        if (hasSavedGlasses) {
                                                "Clear saved glasses address and connection history"
                                        } else {
                                                "No glasses saved"
                                        },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                        )
                }
        }
}

/** Warning dialog shown when user tries to forget glasses while not connected */
@Composable
internal fun ForgetGlassesWarningDialog(viewModel: MainViewModel) {
        androidx.compose.material3.AlertDialog(
                onDismissRequest = { viewModel.dismissForgetWarning() },
                title = {
                        Text(
                                text = "Glasses Not Connected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                        )
                },
                text = {
                        Text(
                                text =
                                        "Cannot clean up resources on glasses when not connected.\n\n" +
                                                "Layouts and gauges will remain in glasses memory and consume space.\n\n" +
                                                "For proper cleanup, connect to glasses first, then forget.\n\n" +
                                                "Force forget anyway?",
                                style = MaterialTheme.typography.bodyMedium
                        )
                },
                confirmButton = {
                        androidx.compose.material3.TextButton(
                                onClick = { viewModel.forceForgetGlasses() }
                        ) { Text("Force Forget", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                        androidx.compose.material3.TextButton(
                                onClick = { viewModel.dismissForgetWarning() }
                        ) { Text("Cancel") }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
}
