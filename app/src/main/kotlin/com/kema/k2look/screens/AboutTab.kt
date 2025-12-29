package com.kema.k2look.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kema.k2look.BuildConfig
import com.kema.k2look.R
import com.kema.k2look.update.AppUpdate
import com.kema.k2look.update.NoUpdateDialog
import com.kema.k2look.update.UpdateChecker
import com.kema.k2look.update.UpdateDialog
import com.kema.k2look.update.UpdateDownloader
import com.kema.k2look.util.PreferencesManager
import kotlinx.coroutines.launch

/**
 * Simple markdown parser for headers (<h2>), bold (**text**), and italic (*text*)
 */
fun parseSimpleMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")

        lines.forEachIndexed { lineIndex, line ->
            // Check if line is a header (<h2>Header</h2> or <h2>Header)
            val trimmedLine = line.trimStart()
            if (trimmedLine.startsWith("<h2>")) {
                // Header - make it bold, remove <h2> and optional </h2>
                val headerText = trimmedLine
                    .removePrefix("<h2>")
                    .removeSuffix("</h2>")
                    .trim()
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(headerText)
                }
            } else {
                // Process inline markdown - handle bold and italic
                var currentPos = 0

                while (currentPos < line.length) {
                    // Check for bold (**text**)
                    if (line.substring(currentPos).startsWith("**")) {
                        val endPos = line.indexOf("**", currentPos + 2)
                        if (endPos != -1) {
                            // Found bold text
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(line.substring(currentPos + 2, endPos))
                            }
                            currentPos = endPos + 2
                            continue
                        }
                    }

                    // Check for italic (*text*) - but not if it's part of **
                    if (line.substring(currentPos).startsWith("*") &&
                        !line.substring(currentPos).startsWith("**")
                    ) {
                        val endPos = line.indexOf("*", currentPos + 1)
                        if (endPos != -1 && (endPos + 1 >= line.length || line[endPos + 1] != '*')) {
                            // Found italic text
                            withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                                append(line.substring(currentPos + 1, endPos))
                            }
                            currentPos = endPos + 1
                            continue
                        }
                    }

                    // Regular character
                    append(line[currentPos])
                    currentPos++
                }
            }

            // Add newline after each line except the last
            if (lineIndex < lines.size - 1) {
                append("\n")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutTab(
    viewModel: com.kema.k2look.viewmodel.MainViewModel,
    uiState: com.kema.k2look.viewmodel.MainViewModel.UiState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsManager = remember { PreferencesManager(context) }
    val updateChecker = remember { UpdateChecker(context) }
    val updateDownloader = remember { UpdateDownloader(context) }

    var isCheckingUpdate by remember { mutableStateOf(false) }
    var availableUpdate by remember { mutableStateOf<AppUpdate?>(null) }
    var showNoUpdateDialog by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var autoCheckEnabled by remember { mutableStateOf(prefsManager.isAutoCheckUpdatesEnabled()) }
    var showDebugDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // App Info
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.background
            ),
            shape = androidx.compose.ui.graphics.RectangleShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "K2Look Logo",
                    modifier = Modifier.height(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.app_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Developer: ${stringResource(R.string.app_developer)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Debug button
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { showDebugDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Debug & Simulator")
                }
            }
        }

        // Update Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = androidx.compose.ui.graphics.RectangleShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = "Updates",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Auto-check toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-check for updates",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Check on app start",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoCheckEnabled,
                        onCheckedChange = { enabled ->
                            autoCheckEnabled = enabled
                            prefsManager.setAutoCheckUpdates(enabled)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Check for updates button
                Button(
                    onClick = {
                        scope.launch {
                            isCheckingUpdate = true
                            val update = updateChecker.checkForUpdate()
                            isCheckingUpdate = false

                            if (update != null) {
                                availableUpdate = update
                                prefsManager.setLastUpdateCheckTime(System.currentTimeMillis())
                            } else {
                                showNoUpdateDialog = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCheckingUpdate
                ) {
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .height(20.dp)
                                .width(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isCheckingUpdate) "Checking..." else "Check for Updates")
                }
            }
        }

        // Release Notes
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = androidx.compose.ui.graphics.RectangleShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = "Release Notes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = parseSimpleMarkdown(BuildConfig.CHANGELOG),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Features
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
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
                    text = "Features",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                FeatureItem(
                    stringResource(R.string.feature_auto_reconnect),
                    stringResource(R.string.feature_auto_reconnect_desc)
                )
                FeatureItem(
                    stringResource(R.string.feature_simulator),
                    stringResource(R.string.feature_simulator_desc)
                )
                FeatureItem(
                    stringResource(R.string.feature_debug),
                    stringResource(R.string.feature_debug_desc)
                )
            }
        }

        // Help & Support
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
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
                    text = "Help & Support",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                HelpItem("Connect Glasses", stringResource(R.string.help_connect_glasses))
                HelpItem("Auto-Reconnect", stringResource(R.string.help_reconnect))
                HelpItem("Timeout Settings", stringResource(R.string.help_timeout))
            }
        }

        // Build Info Section at bottom
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
            Text(
                text = "Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Build: ${BuildConfig.BUILD_DATE}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )
        }
    }

    // Update dialogs
    if (availableUpdate != null) {
        UpdateDialog(
            update = availableUpdate!!,
            isDownloading = isDownloading,
            onDownload = {
                isDownloading = true
                updateDownloader.downloadUpdate(availableUpdate!!) { success ->
                    isDownloading = false
                    if (success) {
                        availableUpdate = null
                    }
                }
            },
            onDismiss = {
                prefsManager.setDismissedUpdateVersion(availableUpdate!!.version)
                availableUpdate = null
            }
        )
    }

    if (showNoUpdateDialog) {
        NoUpdateDialog(
            currentVersion = BuildConfig.VERSION_NAME,
            onDismiss = { showNoUpdateDialog = false }
        )
    }

    // Debug Dialog
    if (showDebugDialog) {
        Dialog(
            onDismissRequest = { showDebugDialog = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Dialog Header
                    TopAppBar(
                        title = { Text("Debug & Simulator") },
                        navigationIcon = {
                            IconButton(onClick = { showDebugDialog = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close"
                                )
                            }
                        }
                    )

                    // Debug Tab Content
                    DebugTab(viewModel, uiState)
                }
            }
        }
    }
}

@Composable
fun FeatureItem(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "• $title",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp, top = 2.dp)
        )
    }
}

@Composable
fun HelpItem(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

