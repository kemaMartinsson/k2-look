package io.hammerhead.karooexttemplate.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.activelook.activelooksdk.DiscoveredGlasses
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooexttemplate.service.KarooActiveLookBridge
import io.hammerhead.karooexttemplate.service.KarooDataService
import io.hammerhead.karooexttemplate.viewmodel.MainViewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = "K2-Look Gateway",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Connection Status Card
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
                    text = "Karoo Connection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = getConnectionStatusText(uiState.connectionState),
                    color = getConnectionStatusColor(uiState.connectionState),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Connect/Disconnect buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.connectKaroo() },
                        enabled = uiState.connectionState !is KarooDataService.ConnectionState.Connected
                    ) {
                        Text("Connect")
                    }
                    Button(
                        onClick = { viewModel.disconnectKaroo() },
                        enabled = uiState.connectionState is KarooDataService.ConnectionState.Connected,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Disconnect")
                    }
                }
            }
        }

        // ActiveLook Connection Card
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
                    text = "ActiveLook Glasses",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = getBridgeStatusText(uiState.bridgeState),
                    color = getBridgeStatusColor(uiState.bridgeState),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Scan/Connect buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (uiState.isScanning) {
                                viewModel.stopActiveLookScan()
                            } else {
                                viewModel.startActiveLookScan()
                            }
                        },
                        enabled = uiState.bridgeState !is KarooActiveLookBridge.BridgeState.FullyConnected &&
                                uiState.bridgeState !is KarooActiveLookBridge.BridgeState.Streaming
                    ) {
                        Text(if (uiState.isScanning) "Stop Scan" else "Scan")
                    }
                    Button(
                        onClick = { viewModel.disconnectActiveLook() },
                        enabled = uiState.bridgeState is KarooActiveLookBridge.BridgeState.FullyConnected ||
                                uiState.bridgeState is KarooActiveLookBridge.BridgeState.Streaming,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Disconnect")
                    }
                }

                // Show discovered glasses
                if (uiState.discoveredGlasses.isNotEmpty() && uiState.isScanning) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Discovered Glasses:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    uiState.discoveredGlasses.forEach { glasses ->
                        GlassesListItem(
                            glasses = glasses,
                            onClick = { viewModel.connectActiveLook(glasses) }
                        )
                    }
                }
            }
        }

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

        // Metrics Grid
        Text(
            text = "Live Metrics",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // Row 1: Speed, Heart Rate
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                label = "Speed",
                value = uiState.speed,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "Heart Rate",
                value = uiState.heartRate,
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
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "Power",
                value = uiState.power,
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
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
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

// Preview removed - MainScreen requires ViewModel with StateFlows which aren't compatible with @Preview
// To preview UI, run the app on a device or emulator
