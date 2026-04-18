package com.kema.k2look.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Switch
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
import com.kema.k2look.viewmodel.setGestureAction
import com.kema.k2look.viewmodel.setGestureEnabled
import com.kema.k2look.viewmodel.setTouchAction
import com.kema.k2look.viewmodel.setTouchEnabled

@Composable
fun GesturesTab(viewModel: MainViewModel, uiState: MainViewModel.UiState) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        // ── Hand Gesture ──────────────────────────────────────────────────
        Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.background
                        ),
                shape = androidx.compose.ui.graphics.RectangleShape
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                // Title row with enable switch
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                            text = "Hand Gesture",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                    )
                    Switch(
                            checked = uiState.gestureEnabled,
                            onCheckedChange = { viewModel.setGestureEnabled(it) }
                    )
                }

                Text(
                        text = "Wave your hand in front of the glasses sensor.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                )

                // Options — only visible when enabled
                AnimatedVisibility(visible = uiState.gestureEnabled) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        var selectedGesture by
                                remember(uiState.gestureAction) {
                                    mutableStateOf(uiState.gestureAction)
                                }
                        GestureAction.entries.forEach { action ->
                            Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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
                                            fontWeight =
                                                    if (selectedGesture == action) FontWeight.Bold
                                                    else FontWeight.Normal
                                    )
                                    Text(
                                            text = action.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (uiState.gestureEventCount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                    text =
                                            "✓ Gesture events detected: ${uiState.gestureEventCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )

        // ── Touch Button ──────────────────────────────────────────────────
        Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.background
                        ),
                shape = androidx.compose.ui.graphics.RectangleShape
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                // Title row with enable switch
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                            text = "Touch Button",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                    )
                    Switch(
                            checked = uiState.touchEnabled,
                            onCheckedChange = { viewModel.setTouchEnabled(it) }
                    )
                }

                Text(
                        text = "Short tap (<3s) on the capacitive button on the glasses frame.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                )

                // Options — only visible when enabled
                AnimatedVisibility(visible = uiState.touchEnabled) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        var selectedTouch by
                                remember(uiState.touchAction) {
                                    mutableStateOf(uiState.touchAction)
                                }
                        TouchAction.entries.forEach { action ->
                            Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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
                                            fontWeight =
                                                    if (selectedTouch == action) FontWeight.Bold
                                                    else FontWeight.Normal
                                    )
                                    Text(
                                            text = action.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (uiState.touchEventCount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                    text = "✓ Touch events detected: ${uiState.touchEventCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
