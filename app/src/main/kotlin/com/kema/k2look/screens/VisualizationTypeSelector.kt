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

/** Minimum zone height (px) required to host a gauge visualization. */
const val MIN_GAUGE_ZONE_HEIGHT = 140

/**
 * Selector for choosing visualization type (Text, Gauge, Zoned Bar).
 *
 * @param zoneHeight Height in pixels of the zone this field occupies. Gauge is only enabled when
 * the zone is tall enough (≥ [MIN_GAUGE_ZONE_HEIGHT]).
 */
@Composable
fun VisualizationTypeSelector(
        dataField: DataField,
        currentType: VisualizationType,
        onTypeSelected: (VisualizationType) -> Unit,
        zoneHeight: Int = 0,
        modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

            // Gauge option — disabled for v1.0, re-enable with 2D_GAUGE template
            // VisualizationTypeCard(
            //     icon = Icons.Default.CheckCircle,
            //     label = "Gauge",
            //     description = "Circular arc gauge",
            //     selected = currentType == VisualizationType.GAUGE,
            //     enabled = isGaugeAvailable(dataField, zoneHeight),
            //     onClick = { onTypeSelected(VisualizationType.GAUGE) },
            //     modifier = Modifier.weight(1f)
            // )
            Spacer(modifier = Modifier.weight(1f))
        }

        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Zoned Bar option (for HR / Power zones)
            VisualizationTypeCard(
                    icon = Icons.Default.PlayArrow,
                    label = "Zone Bar",
                    description = "Multi-zone bar",
                    selected = currentType == VisualizationType.ZONED_BAR,
                    enabled = isZonedBarAvailable(dataField),
                    onClick = { onTypeSelected(VisualizationType.ZONED_BAR) },
                    modifier = Modifier.weight(1f)
            )

            // Spacer card so the row aligns consistently with the top row
            Spacer(modifier = Modifier.weight(1f))
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
                /* Gauge removed from UI for v1.0 */
            }
            VisualizationType.BAR -> {
                /* BAR removed from UI — no description needed */
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

/** Card for a single visualization type option */
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
            colors =
                    CardDefaults.cardColors(
                            containerColor =
                                    if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                    ),
            border =
                    if (selected) {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    }
    ) {
        Column(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
            Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(24.dp),
                    tint =
                            if (selected) {
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
                    color =
                            if (enabled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            }
            )
            Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color =
                            if (enabled) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            }
            )
        }
    }
}

/**
 * Gauge is available when:
 * - The data field is numeric (not elapsed time)
 * - The zone is tall enough to render the gauge arc (≥ [MIN_GAUGE_ZONE_HEIGHT]) Use the 2D_GAUGE
 * template to get a suitable gauge zone.
 */
private fun isGaugeAvailable(dataField: DataField, zoneHeight: Int): Boolean {
    if (dataField.id == 1) return false // Elapsed Time — not suitable
    return zoneHeight >= MIN_GAUGE_ZONE_HEIGHT
}

/**
 * Check if zoned bar visualization is available for this data field Only metrics with pre-defined
 * zones
 */
private fun isZonedBarAvailable(dataField: DataField): Boolean {
    return when (dataField.id) {
        4 -> true // Heart Rate (has HR zones Z1-Z5)
        47 -> true // HR Zone (already categorical)
        48 -> true // Power Zone (has power zones based on FTP)
        // Future: More zoned metrics could be added
        else -> false
    }
}
