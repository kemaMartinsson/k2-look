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
import com.kema.k2look.model.IconSize
import com.kema.k2look.model.LayoutDataField

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
    var showLabel by remember { mutableStateOf(field.showLabel) }
    var showUnit by remember { mutableStateOf(field.showUnit) }
    var showIcon by remember { mutableStateOf(field.showIcon) }
    var iconSize by remember { mutableStateOf(field.iconSize) }
    var visualizationType by remember { mutableStateOf(field.visualizationType) }

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
                    text = field.dataField.name,
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
                        dataField = field.dataField,
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
                                    text = "Display unit (${field.dataField.unit})",
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
                                        text = if (field.dataField.icon28 != null || field.dataField.icon40 != null) {
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
                                    enabled = field.dataField.icon28 != null || field.dataField.icon40 != null
                                )
                            }

                            // Large Icon Toggle (show if large icon is available)
                            if (field.dataField.icon40 != null) {
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
                                        enabled = showIcon && field.dataField.icon40 != null
                                    )
                                }
                            }
                        } // End TEXT visualization options (icon options)
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
                                // Get default visualization for the selected type
                                val (gauge, progressBar, zonedBar) = when (visualizationType) {
                                    com.kema.k2look.model.VisualizationType.GAUGE -> {
                                        val defaultGauge = when (field.dataField.id) {
                                            7 -> com.kema.k2look.data.DefaultVisualizations.createPowerGauge()
                                            4 -> com.kema.k2look.data.DefaultVisualizations.createHeartRateGauge()
                                            8 -> com.kema.k2look.data.DefaultVisualizations.createCadenceGauge()
                                            else -> null
                                        }
                                        Triple(defaultGauge, null, null)
                                    }

                                    com.kema.k2look.model.VisualizationType.BAR -> {
                                        val defaultBar = when (field.dataField.id) {
                                            7 -> com.kema.k2look.data.DefaultVisualizations.createPowerBar()
                                            12 -> com.kema.k2look.data.DefaultVisualizations.createSpeedBar()
                                            else -> null
                                        }
                                        Triple(null, defaultBar, null)
                                    }

                                    com.kema.k2look.model.VisualizationType.ZONED_BAR -> {
                                        val defaultZonedBar = when (field.dataField.id) {
                                            4 -> com.kema.k2look.data.DefaultVisualizations.createHRZoneBar()
                                            else -> null
                                        }
                                        Triple(null, null, defaultZonedBar)
                                    }

                                    com.kema.k2look.model.VisualizationType.TEXT -> {
                                        Triple(null, null, null)
                                    }
                                }

                                val updatedField = field.copy(
                                    visualizationType = visualizationType,
                                    showLabel = showLabel,
                                    showUnit = showUnit,
                                    showIcon = showIcon,
                                    iconSize = iconSize,
                                    gauge = gauge,
                                    progressBar = progressBar,
                                    zonedBar = zonedBar
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
}

