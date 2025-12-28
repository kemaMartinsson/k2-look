package com.kema.k2look.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.kema.k2look.data.DataFieldRegistry
import com.kema.k2look.model.DataField
import com.kema.k2look.model.DataFieldCategory

/**
 * Dialog for selecting a metric from categorized list
 */
@Composable
fun MetricSelectorDialog(
    onDismiss: () -> Unit,
    onSelect: (DataField) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedCategories by remember { mutableStateOf(setOf<DataFieldCategory>()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Text(
                    text = "Select Metric",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Choose a data field to display",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    DataFieldCategory.entries.forEach { category ->
                        val fields = DataFieldRegistry.getByCategory(category)
                        if (fields.isNotEmpty()) {
                            CategorySection(
                                category = category,
                                fields = fields,
                                isExpanded = category in expandedCategories,
                                onToggleExpand = {
                                    expandedCategories = if (category in expandedCategories) {
                                        expandedCategories - category
                                    } else {
                                        expandedCategories + category
                                    }
                                },
                                onSelectField = { field ->
                                    onSelect(field)
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // Cancel button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

/**
 * Expandable category section
 */
@Composable
fun CategorySection(
    category: DataFieldCategory,
    fields: List<DataField>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSelectField: (DataField) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column {
            // Category header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = category.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${fields.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Fields list (when expanded)
            if (isExpanded) {
                HorizontalDivider()
                fields.forEach { field ->
                    MetricItem(
                        field = field,
                        onSelect = { onSelectField(field) }
                    )
                }
            }
        }
    }
}

/**
 * Individual metric item
 */
@Composable
fun MetricItem(
    field: DataField,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = field.name,
                style = MaterialTheme.typography.bodyLarge
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = field.unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                if (field.icon28 != null || field.icon40 != null) {
                    Text(
                        text = "• Has icon",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * Extension to get display name for category
 */
val DataFieldCategory.displayName: String
    get() = when (this) {
        DataFieldCategory.GENERAL -> "General"
        DataFieldCategory.HEART_RATE -> "Heart Rate"
        DataFieldCategory.POWER -> "Power"
        DataFieldCategory.SPEED_PACE -> "Speed & Pace"
        DataFieldCategory.CADENCE -> "Cadence"
        DataFieldCategory.CLIMBING -> "Climbing"
        DataFieldCategory.RUNNING_DYNAMICS -> "Running Dynamics"
        DataFieldCategory.ELEVATION -> "Elevation"
        DataFieldCategory.ENERGY -> "Energy"
    }

