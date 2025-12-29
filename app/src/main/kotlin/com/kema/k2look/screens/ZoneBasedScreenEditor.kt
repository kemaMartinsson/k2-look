package com.kema.k2look.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kema.k2look.model.DataField
import com.kema.k2look.model.LayoutDataField
import com.kema.k2look.model.LayoutScreen

/**
 * Zone-based screen editor for configuring datafields using layout templates
 */
@Composable
fun ZoneBasedScreenEditor(
    screen: LayoutScreen,
    onTemplateChange: (String) -> Unit,
    onFieldAdd: (String, DataField) -> Unit,
    onFieldEdit: (LayoutDataField) -> Unit,
    onFieldRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTemplateSelector by remember { mutableStateOf(false) }
    var showMetricSelector by remember { mutableStateOf(false) }
    var selectedZoneId by remember { mutableStateOf<String?>(null) }

    val template = screen.getTemplate()

    // Template selector dialog
    if (showTemplateSelector) {
        LayoutTemplateSelectorDialog(
            currentTemplateId = screen.templateId ?: "3D_FULL",
            onTemplateSelected = { newTemplate ->
                onTemplateChange(newTemplate.id)
                showTemplateSelector = false
            },
            onDismiss = { showTemplateSelector = false }
        )
    }

    // Metric selector dialog
    if (showMetricSelector && selectedZoneId != null) {
        MetricSelectorDialog(
            onDismiss = {
                showMetricSelector = false
                selectedZoneId = null
            },
            onSelect = { dataField ->
                onFieldAdd(selectedZoneId!!, dataField)
                showMetricSelector = false
                selectedZoneId = null
            }
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp) // Limit height to enable scrolling
                .verticalScroll(rememberScrollState())
                .padding(12.dp), // Reduced from 16.dp
            verticalArrangement = Arrangement.spacedBy(8.dp) // Reduced from 12.dp
        ) {
            // Screen header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = screen.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${screen.dataFields.size}/${template.maxFields} fields",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (screen.dataFields.size == template.maxFields)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Layout template selector button
            Button(
                onClick = { showTemplateSelector = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Layout: ${template.name}")
            }

            // Reduced spacer - no need for extra spacing
            // Zone-based datafield slots
            template.zones.forEach { zone ->
                val field = screen.dataFields.find { it.zoneId == zone.id }
                ZoneDataFieldSlot(
                    zone = zone,
                    field = field,
                    onAdd = {
                        selectedZoneId = zone.id
                        showMetricSelector = true
                    },
                    onEdit = { onFieldEdit(it) },
                    onRemove = { onFieldRemove(zone.id) }
                )
            }
        }
    }
}

/**
 * Individual datafield slot for a specific zone
 */
@Composable
fun ZoneDataFieldSlot(
    zone: com.kema.k2look.model.LayoutZone,
    field: LayoutDataField?,
    onAdd: () -> Unit,
    onEdit: (LayoutDataField) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (field != null)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else
                    MaterialTheme.colorScheme.surface
            )
            .border(
                width = 1.dp,
                color = if (field != null)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(enabled = field == null) { onAdd() }
            .padding(12.dp)
    ) {
        if (field != null) {
            // Filled slot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = zone.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = field.dataField.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Font: ${zone.fontSize.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        if (field.showIcon) {
                            Text(
                                text = "• Icon: ${field.iconSize.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { onEdit(field) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit field",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove field",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        } else {
            // Empty slot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add field",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = zone.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Tap to add field • ${zone.fontSize.name} font",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

