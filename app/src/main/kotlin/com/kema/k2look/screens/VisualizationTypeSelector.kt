package com.kema.k2look.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kema.k2look.model.DataField
import com.kema.k2look.model.VisualizationType

/**
 * Selector for choosing visualization type (Text, Gauge, Bar, Zoned Bar)
 */
@Composable
fun VisualizationTypeSelector(
    dataField: DataField,
    currentType: VisualizationType,
    onTypeSelected: (VisualizationType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Choose how to display this metric:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Text option (always available)
            VisualizationTypeCard(
                icon = Icons.Default.Star,
                label = "Text",
                description = "Traditional text display",
                selected = currentType == VisualizationType.TEXT,
                onClick = { onTypeSelected(VisualizationType.TEXT) },
                modifier = Modifier.weight(1f)
            )

            // Gauge option (for numeric metrics)
            VisualizationTypeCard(
                icon = Icons.Default.CheckCircle,
                label = "Gauge",
                description = "Circular progress",
                selected = currentType == VisualizationType.GAUGE,
                enabled = isGaugeAvailable(dataField),
                onClick = { onTypeSelected(VisualizationType.GAUGE) },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Bar option (for numeric metrics)
            VisualizationTypeCard(
                icon = Icons.Default.PlayArrow,
                label = "Bar",
                description = "Simple progress bar",
                selected = currentType == VisualizationType.BAR,
                enabled = isBarAvailable(dataField),
                onClick = { onTypeSelected(VisualizationType.BAR) },
                modifier = Modifier.weight(1f)
            )

            // Zoned Bar option (for HR)
            VisualizationTypeCard(
                icon = Icons.Default.PlayArrow,
                label = "Zone Bar",
                description = "Multi-zone bar",
                selected = currentType == VisualizationType.ZONED_BAR,
                enabled = isZonedBarAvailable(dataField),
                onClick = { onTypeSelected(VisualizationType.ZONED_BAR) },
                modifier = Modifier.weight(1f)
            )
        }

        // Info text based on selected type
        Spacer(modifier = Modifier.height(4.dp))
        when (currentType) {
            VisualizationType.TEXT -> {
                Text(
                    text = "✓ Text display with optional icon, label, and unit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            VisualizationType.GAUGE -> {
                Text(
                    text = "✓ Circular gauge with percentage fill (e.g., 245W = 61% of 400W)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            VisualizationType.BAR -> {
                Text(
                    text = "✓ Horizontal progress bar showing percentage",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            VisualizationType.ZONED_BAR -> {
                Text(
                    text = "✓ Color-coded zone bar (e.g., HR zones Z1-Z5)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Card for a single visualization type option
 */
@Composable
private fun VisualizationTypeCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                }
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                }
            )
        }
    }
}

/**
 * Check if gauge visualization is available for this data field
 */
private fun isGaugeAvailable(dataField: DataField): Boolean {
    // Gauges work best with numeric metrics that have a meaningful range
    return when (dataField.id) {
        4 -> true   // Heart Rate
        5 -> false  // Max Heart Rate (not useful as gauge)
        6 -> false  // Avg Heart Rate (not useful as gauge)
        7 -> true   // Power
        8 -> true   // Cadence
        12 -> true  // Speed
        else -> false
    }
}

/**
 * Check if simple bar visualization is available for this data field
 */
private fun isBarAvailable(dataField: DataField): Boolean {
    // Bars work for most numeric metrics
    return when (dataField.id) {
        4 -> true   // Heart Rate
        7 -> true   // Power
        8 -> true   // Cadence
        12 -> true  // Speed
        13 -> false // Max Speed (not useful as bar)
        14 -> false // Avg Speed (not useful as bar)
        else -> false
    }
}

/**
 * Check if zoned bar visualization is available for this data field
 */
private fun isZonedBarAvailable(dataField: DataField): Boolean {
    // Zoned bars are specifically for metrics with zones
    return when (dataField.id) {
        4 -> true   // Heart Rate (has Z1-Z5)
        7 -> false  // Power (could have power zones, but not implemented yet)
        else -> false
    }
}

