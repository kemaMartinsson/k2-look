package com.kema.k2look.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.activelook.activelooksdk.DiscoveredGlasses
import com.kema.k2look.service.ActiveLookService
import com.kema.k2look.service.KarooActiveLookBridge
import com.kema.k2look.service.KarooDataService
import com.kema.k2look.viewmodel.MainViewModel
import io.hammerhead.karooext.models.RideState

@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header Section
        Text(
            text = "K2-Look",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        // Divider
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )

        // Tab Row
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Status") }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Dashboard") }
            )
        }

        // Content based on selected tab
        when (selectedTabIndex) {
            0 -> StatusTab(viewModel, uiState, onBack)
            1 -> DashboardTab(viewModel, uiState, onBack)
        }
    }
}

@Composable
fun StatusTab(viewModel: MainViewModel, uiState: MainViewModel.UiState, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Connection Status Section
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Karoo Connection Status
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Karoo service:",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = when (uiState.connectionState) {
                                is KarooDataService.ConnectionState.Connected -> "Connected"
                                is KarooDataService.ConnectionState.Connecting -> "Connecting..."
                                is KarooDataService.ConnectionState.Reconnecting -> "Reconnecting..."
                                is KarooDataService.ConnectionState.Disconnected -> "Disconnected"
                                is KarooDataService.ConnectionState.Error -> "Error"
                            },
                            color = when (uiState.connectionState) {
                                is KarooDataService.ConnectionState.Connected -> MaterialTheme.colorScheme.primary
                                is KarooDataService.ConnectionState.Connecting,
                                is KarooDataService.ConnectionState.Reconnecting -> MaterialTheme.colorScheme.secondary

                                else -> MaterialTheme.colorScheme.error
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // ActiveLook Glasses Status
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        when (val state = uiState.activeLookState) {
                            is ActiveLookService.ConnectionState.Connected -> {
                                // Show connected glasses with brand/model
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Glasses ${state.glasses.name}:",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "connected",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            is ActiveLookService.ConnectionState.Connecting -> {
                                // Show connecting status
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ActiveLook Glasses:",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "connecting...",
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            is ActiveLookService.ConnectionState.Scanning -> {
                                // Show scanning status
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ActiveLook Glasses:",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "scanning...",
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            else -> {
                                // Show connect button when no glasses connected
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No glasses connected",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                    Button(
                                        onClick = { viewModel.startGlassesScan() }
                                    ) {
                                        Text("Connect glasses")
                                    }
                                }
                            }
                        }
                    }
                }

                // Reconnect Timeout Configuration
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Auto-Reconnect Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "During an active ride, continuously attempt to reconnect to glasses if disconnected.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        var timeoutText by remember { mutableStateOf(uiState.reconnectTimeoutMinutes.toString()) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Timeout:",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = timeoutText,
                                    onValueChange = { newValue ->
                                        // Only allow digits
                                        if (newValue.all { it.isDigit() } && newValue.length <= 2) {
                                            timeoutText = newValue
                                            // Update the preference if valid
                                            val minutes = newValue.toIntOrNull()
                                            if (minutes != null && minutes in 1..60) {
                                                viewModel.setReconnectTimeout(minutes)
                                            }
                                        }
                                    },
                                    label = { Text("Minutes") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )

                                Text(
                                    text = "min",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "Valid range: 1-60 minutes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Build Info Section at bottom
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
                Text(
                    text = "Version: ${com.kema.k2look.BuildConfig.VERSION_NAME} (${com.kema.k2look.BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Build: ${com.kema.k2look.BuildConfig.BUILD_DATE}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Floating Action Buttons
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back Button
            FloatingActionButton(
                onClick = onBack,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            // Add/Connect Glasses Button - only show when not connected
            if (uiState.activeLookState !is ActiveLookService.ConnectionState.Connected) {
                FloatingActionButton(
                    onClick = { viewModel.startGlassesScan() },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Connect Glasses",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardTab(viewModel: MainViewModel, uiState: MainViewModel.UiState, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Ride State Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Ride State",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = getRideStateText(uiState.rideState),
                        color = getRideStateColor(uiState.rideState),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 1: Speed, Heart Rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    label = "Speed",
                    value = uiState.speed,
                    avgValue = uiState.avgSpeed,
                    maxValue = uiState.maxSpeed,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Heart Rate",
                    value = uiState.heartRate,
                    avgValue = uiState.avgHeartRate,
                    maxValue = uiState.maxHeartRate,
                    extraInfo = if (uiState.hrZone != "--") uiState.hrZone else null,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Cadence, Power
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    label = "Cadence",
                    value = uiState.cadence,
                    avgValue = uiState.avgCadence,
                    maxValue = uiState.maxCadence,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Power",
                    value = uiState.power,
                    avgValue = uiState.avgPower,
                    maxValue = uiState.maxPower,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 3: Distance, Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    label = "Distance",
                    value = uiState.distance,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Time",
                    value = uiState.time,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 4: Smoothed Power (3s/10s/30s)
            Text(
                text = "Smoothed Power",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    label = "3s Power",
                    value = uiState.power3s,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "10s Power",
                    value = uiState.power10s,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "30s Power",
                    value = uiState.power30s,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 5: VAM (Climbing metrics)
            Text(
                text = "Climbing",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    label = "VAM",
                    value = uiState.vam,
                    avgValue = uiState.avgVam,
                    modifier = Modifier.weight(1f)
                )
                // Placeholder for future grade% or elevation
                MetricCard(
                    label = "Grade",
                    value = "--",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Floating Action Buttons
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back Button
            FloatingActionButton(
                onClick = onBack,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            // Add/Connect Glasses Button - only show when not connected
            if (uiState.activeLookState !is ActiveLookService.ConnectionState.Connected) {
                FloatingActionButton(
                    onClick = { viewModel.startGlassesScan() },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Connect Glasses",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    avgValue: String? = null,
    maxValue: String? = null,
    extraInfo: String? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
            if (avgValue != null || maxValue != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (avgValue != null) {
                        Text(
                            text = "Avg: $avgValue",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    if (avgValue != null && maxValue != null) {
                        Text(
                            text = " | ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                        )
                    }
                    if (maxValue != null) {
                        Text(
                            text = "Max: $maxValue",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            if (extraInfo != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = extraInfo,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun getConnectionStatusText(state: KarooDataService.ConnectionState): String {
    return when (state) {
        is KarooDataService.ConnectionState.Disconnected -> "Disconnected"
        is KarooDataService.ConnectionState.Connecting -> "Connecting..."
        is KarooDataService.ConnectionState.Connected -> "Connected"
        is KarooDataService.ConnectionState.Error -> "Error: ${state.message}"
        is KarooDataService.ConnectionState.Reconnecting -> "Reconnecting (${state.attempt})..."
    }
}

@Composable
fun getConnectionStatusColor(state: KarooDataService.ConnectionState): androidx.compose.ui.graphics.Color {
    return when (state) {
        is KarooDataService.ConnectionState.Connected -> MaterialTheme.colorScheme.primary
        is KarooDataService.ConnectionState.Connecting,
        is KarooDataService.ConnectionState.Reconnecting -> MaterialTheme.colorScheme.tertiary

        is KarooDataService.ConnectionState.Error -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
fun getRideStateText(state: RideState): String {
    return when (state) {
        is RideState.Idle -> "Idle"
        is RideState.Recording -> "Recording"
        is RideState.Paused -> if (state.auto) "Auto-Paused" else "Paused"
    }
}

@Composable
fun getRideStateColor(state: RideState): androidx.compose.ui.graphics.Color {
    return when (state) {
        is RideState.Recording -> MaterialTheme.colorScheme.primary
        is RideState.Paused -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
fun getBridgeStatusText(state: KarooActiveLookBridge.BridgeState): String {
    return when (state) {
        is KarooActiveLookBridge.BridgeState.Idle -> "Not Connected"
        is KarooActiveLookBridge.BridgeState.KarooConnecting -> "Connecting to Karoo..."
        is KarooActiveLookBridge.BridgeState.KarooConnected -> "Karoo Connected"
        is KarooActiveLookBridge.BridgeState.ActiveLookScanning -> "Scanning for Glasses..."
        is KarooActiveLookBridge.BridgeState.ActiveLookConnecting -> "Connecting to Glasses..."
        is KarooActiveLookBridge.BridgeState.FullyConnected -> "Connected"
        is KarooActiveLookBridge.BridgeState.Streaming -> "Streaming to Glasses ✓"
        is KarooActiveLookBridge.BridgeState.Error -> "Error: ${state.message}"
    }
}

@Composable
fun getBridgeStatusColor(state: KarooActiveLookBridge.BridgeState): androidx.compose.ui.graphics.Color {
    return when (state) {
        is KarooActiveLookBridge.BridgeState.FullyConnected,
        is KarooActiveLookBridge.BridgeState.Streaming -> MaterialTheme.colorScheme.primary

        is KarooActiveLookBridge.BridgeState.KarooConnecting,
        is KarooActiveLookBridge.BridgeState.ActiveLookScanning,
        is KarooActiveLookBridge.BridgeState.ActiveLookConnecting -> MaterialTheme.colorScheme.tertiary

        is KarooActiveLookBridge.BridgeState.Error -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
fun GlassesListItem(
    glasses: DiscoveredGlasses,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = glasses.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = glasses.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
            Text(
                text = "Connect →",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ============================================================================
// Previews - Shows UI in different states
// ============================================================================

@Preview(showBackground = true, showSystemUi = true, name = "Status Tab")
@Composable
fun MainScreenPreviewStatus() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header
            Text(
                text = "K2-Look",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally)
            )

            // Divider
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )

            // Tabs (Status selected)
            TabRow(selectedTabIndex = 0) {
                Tab(
                    selected = true,
                    onClick = { },
                    text = { Text("Status") }
                )
                Tab(
                    selected = false,
                    onClick = { },
                    text = { Text("Dashboard") }
                )
            }

            // Status Tab Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    StatusCard("Karoo service:", "connected", MaterialTheme.colorScheme.primary)
                    StatusCard("Glasses ENGO-2:", "connected", MaterialTheme.colorScheme.primary)
                }

                // Build info
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Text(
                        text = "Version: 1.0 (1)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Build: 2025-01-23 14:30",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Dashboard Tab")
@Composable
fun MainScreenPreviewDashboard() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header
            Text(
                text = "K2-Look",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally)
            )

            // Divider
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )

            // Tabs (Dashboard selected)
            TabRow(selectedTabIndex = 1) {
                Tab(
                    selected = false,
                    onClick = { },
                    text = { Text("Status") }
                )
                Tab(
                    selected = true,
                    onClick = { },
                    text = { Text("Dashboard") }
                )
            }

            // Dashboard Tab Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Ride State
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Ride State",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Recording",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PreviewMetricCard("Speed", "25,5 km/h", Modifier.weight(1f))
                    PreviewMetricCard("Heart Rate", "142 bpm", Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PreviewMetricCard("Cadence", "85 rpm", Modifier.weight(1f))
                    PreviewMetricCard("Power", "220 w", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun StatusCard(title: String, status: String, color: androidx.compose.ui.graphics.Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = status,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PreviewMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}
