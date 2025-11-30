package com.kema.k2look.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

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

@Composable
fun UpdateDialog(
    update: AppUpdate,
    isDownloading: Boolean,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = { if (!isDownloading) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .heightIn(max = 500.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Update Available",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Version ${update.version}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (update.releaseNotes.isNotBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "What's New:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = parseSimpleMarkdown(update.releaseNotes),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Downloading...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Later")
                        }

                        Button(
                            onClick = onDownload,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Download")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CheckingUpdateDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Checking for updates...",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun NoUpdateDialog(
    currentVersion: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "You're up to date!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "You have the latest version ($currentVersion)",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(onClick = onDismiss) {
                    Text("OK")
                }
            }
        }
    }
}

