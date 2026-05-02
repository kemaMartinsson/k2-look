package com.kema.k2look.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.kema.k2look.BuildConfig
import com.kema.k2look.service.ActiveLookService
import com.kema.k2look.viewmodel.MainViewModel

@Composable
fun DebugTab(viewModel: MainViewModel, uiState: MainViewModel.UiState) {
        // Hoist debugEnabled so it can be used across the whole screen
        var debugEnabled by remember { mutableStateOf(uiState.debugModeEnabled) }

        // Keep local switch state in sync with ViewModel state (e.g., after process recreation)
        androidx.compose.runtime.LaunchedEffect(uiState.debugModeEnabled) {
                debugEnabled = uiState.debugModeEnabled
        }

        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                                .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
                // Debug Toggle Card
                Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.background
                                ),
                        shape = androidx.compose.ui.graphics.RectangleShape
                ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Text(
                                        text = "Debug Mode",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                        text = "Enable debug logging and simulator features",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                )

                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Text(
                                                text = "Debug Mode:",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium
                                        )

                                        androidx.compose.material3.Switch(
                                                checked = debugEnabled,
                                                onCheckedChange = {
                                                        debugEnabled = it
                                                        viewModel.setDebugMode(it)
                                                }
                                        )
                                }

                                if (debugEnabled) {
                                        Text(
                                                text = "✓ Logging to: /sdcard/k2look_debug.log",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(top = 8.dp)
                                        )
                                }
                        }
                }

                // Divider
                HorizontalDivider(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )

                // Simulator Card (always show, but functionality depends on Debug Mode)
                Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.background
                                ),
                        shape = androidx.compose.ui.graphics.RectangleShape
                ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Text(
                                        text = "Display Simulator",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                        text = "Test glasses display with simulated values",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                )

                                // Simulator controls
                                var simulatorActive by remember { mutableStateOf(false) }

                                // Reset simulator state when debug mode is disabled
                                androidx.compose.runtime.LaunchedEffect(uiState.debugModeEnabled) {
                                        if (!uiState.debugModeEnabled) {
                                                simulatorActive = false
                                        }
                                }

                                if (!uiState.debugModeEnabled) {
                                        // Show disabled state when debug mode is off
                                        Button(
                                                onClick = {},
                                                modifier = Modifier.fillMaxWidth(),
                                                enabled = false
                                        ) { Text("Start Simulator") }
                                        Text(
                                                text =
                                                        "⚠️ Enable Debug Mode above to use simulator",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.padding(top = 8.dp)
                                        )
                                } else if (!simulatorActive) {
                                        Button(
                                                onClick = {
                                                        simulatorActive = true
                                                        viewModel.startSimulator()
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                enabled =
                                                        uiState.activeLookState is
                                                                ActiveLookService.ConnectionState.Connected
                                        ) { Text("Start Simulator") }

                                        if (uiState.activeLookState !is
                                                        ActiveLookService.ConnectionState.Connected
                                        ) {
                                                Text(
                                                        text = "Connect glasses first",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.padding(top = 8.dp)
                                                )
                                        }
                                } else {
                                        Button(
                                                onClick = {
                                                        simulatorActive = false
                                                        viewModel.stopSimulator()
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors =
                                                        androidx.compose.material3.ButtonDefaults
                                                                .buttonColors(
                                                                        containerColor =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .error
                                                                )
                                        ) { Text("Stop Simulator") }

                                        Text(
                                                text = "✓ Sending test data to glasses...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(top = 12.dp)
                                        )
                                }
                        }
                }

                // ── Display Debug Tests (debug builds only) ─────────────────────
                if (BuildConfig.DEBUG) {
                        HorizontalDivider(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                thickness = 1.dp,
                                color =
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.3f
                                        )
                        )

                        Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                colors =
                                        CardDefaults.cardColors(
                                                containerColor =
                                                        MaterialTheme.colorScheme.background
                                        ),
                                shape = androidx.compose.ui.graphics.RectangleShape
                        ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                        Text(
                                                text = "Display Debug Tests",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        Text(
                                                text =
                                                        "Render calibration patterns on the glasses to diagnose layout / clipping issues",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(bottom = 12.dp)
                                        )

                                        val glassesConnected =
                                                uiState.activeLookState is
                                                        ActiveLookService.ConnectionState.Connected
                                        val testsEnabled = debugEnabled && glassesConnected

                                        data class DebugTest(
                                                val num: Int,
                                                val label: String,
                                                val desc: String
                                        )

                                        val tests =
                                                listOf(
                                                        DebugTest(
                                                                1,
                                                                "1 · Bounds",
                                                                "Corner markers, safe area, center crosshair"
                                                        ),
                                                        DebugTest(
                                                                2,
                                                                "2 · Rotations",
                                                                "Same anchor, 8 rotation values"
                                                        ),
                                                        DebugTest(
                                                                3,
                                                                "3 · Clip w/h",
                                                                "width=244 vs width=273 → SIZE or COORD?"
                                                        ),
                                                        DebugTest(
                                                                4,
                                                                "4 · Text X",
                                                                "txtX at 50 / 120 / 194 / 234"
                                                        ),
                                                        DebugTest(
                                                                5,
                                                                "5 · K2L vs Official",
                                                                "Current params vs ActiveLook defaults"
                                                        ),
                                                        DebugTest(
                                                                6,
                                                                "6 · 3-Field",
                                                                "Full 3D_FULL layout, official positions"
                                                        ),
                                                        DebugTest(
                                                                7,
                                                                "7 · ExtraCmd",
                                                                "unit / icon / label drawn on top via LayoutExtraCmd"
                                                        ),
                                                        DebugTest(
                                                                8,
                                                                "8 · Icon+Value+Unit",
                                                                "[speed icon][50/20/30][km/h] all fonts"
                                                        ),
                                                        DebugTest(
                                                                9,
                                                                "9 · Realistic Layout",
                                                                "[speed icon][25.1][km/h] / [power icon][250][W] / [HR icon][150][bpm]"
                                                        ),
                                                        DebugTest(
                                                                10,
                                                                "10 · Dynamic Layout (4 rows)",
                                                                "[speed][time][HR][power] via DynamicLayoutEngine"
                                                        ),
                                                        DebugTest(
                                                                11,
                                                                "11 · Gauge 270°",
                                                                "270° arc gauge at 0 / 33 / 66 / 100 %"
                                                        ),
                                                        DebugTest(
                                                                12,
                                                                "12 · Production Layout (3 rows)",
                                                                "[speed font3][HR font2][cadence font1] — production sizeToFont mapping"
                                                        ),
                                                        DebugTest(
                                                                13,
                                                                "13 · HR Zone Bar",
                                                                "5-segment horizontal bar, cycles no zone → Z1 → Z5"
                                                        ),
                                                )

                                        tests.forEach { t ->
                                                Row(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(vertical = 3.dp),
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                        text = t.label,
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .bodyMedium,
                                                                        fontWeight =
                                                                                FontWeight.Medium
                                                                )
                                                                Text(
                                                                        text = t.desc,
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .labelSmall,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurfaceVariant
                                                                )
                                                        }
                                                        Button(
                                                                onClick = {
                                                                        viewModel
                                                                                .runDisplayDebugTest(
                                                                                        t.num
                                                                                )
                                                                },
                                                                enabled = testsEnabled,
                                                                modifier =
                                                                        Modifier.padding(
                                                                                start = 8.dp
                                                                        ),
                                                                contentPadding =
                                                                        androidx.compose.foundation
                                                                                .layout
                                                                                .PaddingValues(
                                                                                        horizontal =
                                                                                                12.dp,
                                                                                        vertical =
                                                                                                4.dp
                                                                                )
                                                        ) {
                                                                Text(
                                                                        "Run",
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .labelMedium
                                                                )
                                                        }
                                                }
                                        }

                                        // Clear button
                                        HorizontalDivider(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                thickness = 0.5.dp,
                                                color =
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                                .copy(alpha = 0.2f)
                                        )
                                        Button(
                                                onClick = { viewModel.clearGlassesDisplay() },
                                                enabled = testsEnabled,
                                                modifier = Modifier.fillMaxWidth(),
                                                colors =
                                                        androidx.compose.material3.ButtonDefaults
                                                                .buttonColors(
                                                                        containerColor =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .secondary
                                                                )
                                        ) { Text("Clear Glasses Display") }

                                        if (!testsEnabled) {
                                                Text(
                                                        text =
                                                                when {
                                                                        !debugEnabled ->
                                                                                "⚠️ Enable Debug Mode above"
                                                                        !glassesConnected ->
                                                                                "⚠️ Connect glasses first"
                                                                        else -> ""
                                                                },
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.padding(top = 8.dp)
                                                )
                                        }
                                }
                        }
                } // end BuildConfig.DEBUG

                // ── Production Simulation (debug builds only) ──────────────────
                if (BuildConfig.DEBUG) {
                        HorizontalDivider(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                thickness = 1.dp,
                                color =
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.3f
                                        )
                        )

                        Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                colors =
                                        CardDefaults.cardColors(
                                                containerColor =
                                                        MaterialTheme.colorScheme.background
                                        ),
                                shape = androidx.compose.ui.graphics.RectangleShape
                        ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                        Text(
                                                text = "Production Simulation",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        Text(
                                                text =
                                                        "3-row 3D_FULL layout via saveAndActivateProfile + displayAllFieldValues — same code path as a real ride",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        Text(
                                                text =
                                                        "Speed 25.1 km/h · Cadence 185 rpm · HR 150 bpm · Battery 75% · Radar alert",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(bottom = 12.dp)
                                        )

                                        val simEnabled =
                                                debugEnabled &&
                                                        uiState.activeLookState is
                                                                ActiveLookService.ConnectionState.Connected

                                        Button(
                                                onClick = { viewModel.runProductionSimulation() },
                                                enabled = simEnabled,
                                                modifier = Modifier.fillMaxWidth()
                                        ) { Text("Run Production Simulation") }

                                        if (!simEnabled) {
                                                Text(
                                                        text =
                                                                when {
                                                                        !debugEnabled ->
                                                                                "⚠️ Enable Debug Mode above"
                                                                        else ->
                                                                                "⚠️ Connect glasses first"
                                                                },
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.padding(top = 8.dp)
                                                )
                                        }
                                }
                        }
                } // end Production Simulation
                Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Text(
                                        text = "Current Values on Glasses",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                )

                                // Display current values in compact format
                                DebugValueRow("Speed", uiState.speed)
                                DebugValueRow("Heart Rate", uiState.heartRate)
                                DebugValueRow("Cadence", uiState.cadence)
                                DebugValueRow("Power", uiState.power)
                                DebugValueRow("Distance", uiState.distance)
                                DebugValueRow("Time", uiState.time)
                        }
                }
        }
}

@Composable
fun DebugValueRow(label: String, value: String) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
        ) {
                Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                )
        }
}
