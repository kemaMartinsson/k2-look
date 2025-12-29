package com.kema.k2look.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import com.kema.k2look.model.GestureAction
import com.kema.k2look.model.TouchAction
import com.kema.k2look.viewmodel.MainViewModel

@Composable
fun GesturesTab(viewModel: MainViewModel, uiState: MainViewModel.UiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Hand Gesture Configuration
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.background
            ),
            shape = androidx.compose.ui.graphics.RectangleShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = "Hand Gesture Action",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Wave your hand in front of the glasses to trigger the selected action.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                var selectedGesture by remember { mutableStateOf(uiState.gestureAction) }

                // Gesture Action Options
                GestureAction.entries.forEach { action ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedGesture == action,
                            onClick = {
                                selectedGesture = action
                                viewModel.setGestureAction(action)
                            }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = action.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedGesture == action) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = action.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Event counter (for debugging)
                if (uiState.gestureEventCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "✓ Gesture events detected: ${uiState.gestureEventCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Divider
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )

        // Touch Button Configuration
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.background
            ),
            shape = androidx.compose.ui.graphics.RectangleShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = "Touch Button Action",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Tap the capacitive button on the glasses to trigger the selected action.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                var selectedTouch by remember { mutableStateOf(uiState.touchAction) }

                // Touch Action Options
                TouchAction.entries.forEach { action ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedTouch == action,
                            onClick = {
                                selectedTouch = action
                                viewModel.setTouchAction(action)
                            }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = action.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedTouch == action) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = action.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Event counter (for debugging)
                if (uiState.touchEventCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "✓ Touch events detected: ${uiState.touchEventCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Divider
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )

        // Information Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            ),
            shape = androidx.compose.ui.graphics.RectangleShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = "ℹ️ How It Works",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Hand Gesture:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Wave your hand near the glasses sensor to trigger the action. Works while riding!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
                )

                Text(
                    text = "Touch Button:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Short tap (<3s) on the capacitive button on the glasses frame.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

