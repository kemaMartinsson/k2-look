package com.kema.k2look.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kema.k2look.model.DataField
import com.kema.k2look.model.IconSize
import com.kema.k2look.model.LayoutDataField
import com.kema.k2look.model.VisualizationType

/**
 * Dialog for configuring datafield display options
 */
@Composable
fun FieldConfigurationDialog(
    field: LayoutDataField,
    onDismiss: () -> Unit,
    onSave: (LayoutDataField) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDataField by remember { mutableStateOf(field.dataField) }
    var showLabel by remember { mutableStateOf(field.showLabel) }
    var showUnit by remember { mutableStateOf(field.showUnit) }
    var showIcon by remember { mutableStateOf(field.showIcon) }
    var iconSize by remember { mutableStateOf(field.iconSize) }
    var visualizationType by remember {
        mutableStateOf(
            field.visualizationType ?: VisualizationType.TEXT
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .padding(8.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Header
                Text(
                    text = "Configure Field",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = selectedDataField.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Metric Selection
                    Text(
                        text = "Select Metric",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    MetricSelector(
                        selectedDataField = selectedDataField,
                        onDataFieldSelected = { newField ->
                            selectedDataField = newField
                            // Reset visualization if new metric doesn't support current type
                            if (visualizationType == VisualizationType.GAUGE && !canShowAsGauge(
                                    newField
                                )
                            ) {
                                visualizationType = VisualizationType.TEXT
                            }
                            if (visualizationType == VisualizationType.BAR && !canShowAsBar(newField)) {
                                visualizationType = VisualizationType.TEXT
                            }
                            if (visualizationType == VisualizationType.ZONED_BAR && !canShowAsZonedBar(
                                    newField
                                )
                            ) {
                                visualizationType = VisualizationType.TEXT
                            }
                        }
                    )

                    HorizontalDivider()

                    // Info: Font size determined by zone
                    Text(
                        text = "ℹ️ Font size is automatically set based on the zone position",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    HorizontalDivider()

                    // Visualization Type Section
                    Text(
                        text = "Visualization Style",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    VisualizationTypeSelector(
                        dataField = selectedDataField,
                        currentType = visualizationType,
                        onTypeSelected = { newType ->
                            visualizationType = newType
                        }
                    )

                    HorizontalDivider()

                    // Display Options Section (only for TEXT visualization)
                    if (visualizationType == com.kema.k2look.model.VisualizationType.TEXT) {
                        Text(
                            text = "Display Options",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Show Label Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Show Label",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Display field name",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Switch(
                                checked = showLabel,
                                onCheckedChange = { showLabel = it }
                            )
                        }

                        // Show Unit Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Show Unit",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Display unit (${selectedDataField.unit})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Switch(
                                checked = showUnit,
                                onCheckedChange = { showUnit = it }
                            )
                        }

                        HorizontalDivider()

                        // Icon Options Section (only for TEXT visualization)
                        if (visualizationType == com.kema.k2look.model.VisualizationType.TEXT) {
                            Text(
                                text = "Icon Options",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            // Show Icon Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Show Icon",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = if (selectedDataField.icon28 != null || selectedDataField.icon40 != null) {
                                            val currentIconSize =
                                                if (iconSize == IconSize.SMALL) "28×28px" else "40×40px"
                                            "Display $currentIconSize icon"
                                        } else {
                                            "No icon available for this metric"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Switch(
                                    checked = showIcon,
                                    onCheckedChange = {
                                        showIcon = it
                                        // When enabling icon, keep current size (default is SMALL)
                                        // User can toggle to LARGE if they want
                                    },
                                    enabled = selectedDataField.icon28 != null || selectedDataField.icon40 != null
                                )
                            }

                            // Large Icon Toggle (show if large icon is available)
                            if (selectedDataField.icon40 != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Large Icon",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = if (iconSize == IconSize.LARGE) "40×40px" else "28×28px",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                    Switch(
                                        checked = iconSize == IconSize.LARGE,
                                        onCheckedChange = { isLarge ->
                                            iconSize =
                                                if (isLarge) IconSize.LARGE else IconSize.SMALL
                                        },
                                        enabled = showIcon && selectedDataField.icon40 != null
                                    )
                                }
                            }
                        } // End TEXT visualization options (icon options)
                    } // End TEXT conditional
                } // End scrollable content

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Button(
                        onClick = {
                            android.util.Log.i("FieldConfig", "💾 SAVE clicked")
                            android.util.Log.i(
                                "FieldConfig",
                                "  Selected metric: ${selectedDataField.name} (id=${selectedDataField.id})"
                            )
                            android.util.Log.i(
                                "FieldConfig",
                                "  Visualization type: $visualizationType"
                            )
                            android.util.Log.i("FieldConfig", "  Zone ID: ${field.zoneId}")

                            // Get default visualization for the selected type
                            val (gauge, progressBar, zonedBar) = when (visualizationType) {
                                com.kema.k2look.model.VisualizationType.GAUGE -> {
                                    android.util.Log.i("FieldConfig", "  Creating GAUGE...")
                                    // Get metric-specific template or create generic one
                                    val baseGauge = when (selectedDataField.id) {
                                        7 -> {
                                            android.util.Log.i(
                                                "FieldConfig",
                                                "    Using Power gauge template"
                                            )
                                            com.kema.k2look.data.DefaultVisualizations.createPowerGauge()
                                        }

                                        4 -> {
                                            android.util.Log.i(
                                                "FieldConfig",
                                                "    Using HR gauge template"
                                            )
                                            com.kema.k2look.data.DefaultVisualizations.createHeartRateGauge()
                                        }

                                        8 -> {
                                            android.util.Log.i(
                                                "FieldConfig",
                                                "    Using Cadence gauge template"
                                            )
                                            com.kema.k2look.data.DefaultVisualizations.createCadenceGauge()
                                        }

                                        else -> {
                                            android.util.Log.i(
                                                "FieldConfig",
                                                "    Creating GENERIC gauge for ${selectedDataField.name}"
                                            )
                                            // Create generic gauge for other metrics
                                            com.kema.k2look.model.Gauge(
                                                id = 1,
                                                centerX = 152,
                                                centerY = 128,
                                                radiusOuter = 70,
                                                radiusInner = 55,
                                                startPortion = 3,
                                                endPortion = 14,
                                                clockwise = true,
                                                minValue = 0f,
                                                maxValue = 100f, // Generic max, adjust as needed
                                                dataField = selectedDataField
                                            )
                                        }
                                    }
                                    // Update the gauge's dataField to use the selected one
                                    val updatedGauge = baseGauge.copy(dataField = selectedDataField)
                                    android.util.Log.i(
                                        "FieldConfig",
                                        "    ✅ Gauge created with metric: ${updatedGauge.dataField.name}"
                                    )
                                    Triple(updatedGauge, null, null)
                                }

                                com.kema.k2look.model.VisualizationType.BAR -> {
                                    android.util.Log.i("FieldConfig", "  Creating BAR...")
                                    // Get metric-specific template or create generic one
                                    val baseBar = when (selectedDataField.id) {
                                        7 -> {
                                            android.util.Log.i(
                                                "FieldConfig",
                                                "    Using Power bar template"
                                            )
                                            com.kema.k2look.data.DefaultVisualizations.createPowerBar()
                                        }

                                        12 -> {
                                            android.util.Log.i(
                                                "FieldConfig",
                                                "    Using Speed bar template"
                                            )
                                            com.kema.k2look.data.DefaultVisualizations.createSpeedBar()
                                        }

                                        else -> {
                                            android.util.Log.i(
                                                "FieldConfig",
                                                "    Creating GENERIC bar for ${selectedDataField.name}"
                                            )
                                            // Create generic horizontal bar for other metrics
                                            com.kema.k2look.model.ProgressBar(
                                                id = 1,
                                                x = 30,
                                                y = 110,
                                                width = 244,
                                                height = 20,
                                                orientation = com.kema.k2look.model.Orientation.HORIZONTAL,
                                                minValue = 0f,
                                                maxValue = 100f, // Generic max, adjust as needed
                                                showBorder = true,
                                                dataField = selectedDataField
                                            )
                                        }
                                    }
                                    // Update the bar's dataField to use the selected one
                                    val updatedBar = baseBar.copy(dataField = selectedDataField)
                                    android.util.Log.i(
                                        "FieldConfig",
                                        "    ✅ Bar created with metric: ${updatedBar.dataField.name}"
                                    )
                                    Triple(null, updatedBar, null)
                                }

                                com.kema.k2look.model.VisualizationType.ZONED_BAR -> {
                                    android.util.Log.i("FieldConfig", "  Creating ZONED_BAR...")
                                    // HR and Power have zoned bar support
                                    val zonedBar = when (selectedDataField.id) {
                                        4 -> {
                                            // Heart Rate zones
                                            android.util.Log.i(
                                                "FieldConfig",
                                                "    Using HR zone bar template"
                                            )
                                            com.kema.k2look.data.DefaultVisualizations.createHRZoneBar()
                                                .copy(
                                                    bar = com.kema.k2look.data.DefaultVisualizations.createHRZoneBar().bar.copy(
                                                        dataField = selectedDataField
                                                    )
                                                )
                                        }

                                        7, 48 -> {
                                            // Power or Power Zone
                                            android.util.Log.i(
                                                "FieldConfig",
                                                "    Using Power zone bar template"
                                            )
                                            com.kema.k2look.data.DefaultVisualizations.createPowerZoneBar()
                                                .copy(
                                                    bar = com.kema.k2look.data.DefaultVisualizations.createPowerZoneBar().bar.copy(
                                                        dataField = selectedDataField
                                                    )
                                                )
                                        }

                                        else -> {
                                            android.util.Log.w(
                                                "FieldConfig",
                                                "    ⚠️ No zoned bar for ${selectedDataField.name}"
                                            )
                                            null
                                        }
                                    }
                                    Triple(null, null, zonedBar)
                                }

                                com.kema.k2look.model.VisualizationType.TEXT -> {
                                    android.util.Log.i(
                                        "FieldConfig",
                                        "  TEXT visualization (no gauge/bar)"
                                    )
                                    Triple(null, null, null)
                                }
                            }

                            android.util.Log.i(
                                "FieldConfig",
                                "  Result: gauge=${gauge != null}, bar=${progressBar != null}, zonedBar=${zonedBar != null}"
                            )

                            val updatedField = field.copy(
                                dataField = selectedDataField,
                                visualizationType = visualizationType,
                                showLabel = showLabel,
                                showUnit = showUnit,
                                showIcon = showIcon,
                                iconSize = iconSize,
                                gauge = gauge,
                                progressBar = progressBar,
                                zonedBar = zonedBar
                            )

                            android.util.Log.i(
                                "FieldConfig",
                                "  ✅ Calling onSave with updatedField"
                            )
                            android.util.Log.i(
                                "FieldConfig",
                                "     - dataField: ${updatedField.dataField.name}"
                            )
                            android.util.Log.i(
                                "FieldConfig",
                                "     - visualizationType: ${updatedField.visualizationType}"
                            )
                            android.util.Log.i(
                                "FieldConfig",
                                "     - gauge: ${updatedField.gauge?.dataField?.name}"
                            )
                            android.util.Log.i(
                                "FieldConfig",
                                "     - bar: ${updatedField.progressBar?.dataField?.name}"
                            )

                            onSave(updatedField)
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

/**
 * Check if a metric can be displayed as a gauge
 * Now supports ALL metrics with generic gauge templates
 */
private fun canShowAsGauge(dataField: DataField): Boolean {
    // Gauges work for numeric metrics
    // Time/distance metrics are constantly increasing, but can still show as gauge
    return when (dataField.id) {
        1 -> false   // Elapsed Time - not suitable
        else -> true // All other numeric metrics can use gauge
    }
}

/**
 * Check if a metric can be displayed as a simple bar
 * Now supports ALL metrics with generic bar templates
 */
private fun canShowAsBar(dataField: DataField): Boolean {
    // Bars work for most numeric metrics
    return when (dataField.id) {
        1 -> false   // Elapsed Time - not suitable
        else -> true // All other numeric metrics can use bar
    }
}

/**
 * Check if a metric can be displayed as a zoned bar
 * Only metrics with defined zones work
 */
private fun canShowAsZonedBar(dataField: DataField): Boolean {
    return when (dataField.id) {
        4 -> true   // Heart Rate (has HR zones Z1-Z5)
        47 -> true  // HR Zone (categorical)
        48 -> true  // Power Zone (has power zones based on FTP)
        // Future: More zoned metrics could be added
        else -> false
    }
}

