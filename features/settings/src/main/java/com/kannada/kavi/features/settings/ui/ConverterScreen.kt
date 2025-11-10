package com.kannada.kavi.features.settings.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kannada.kavi.core.common.Result
import com.kannada.kavi.features.converter.NudiToUnicodeConverter
import com.kannada.kavi.features.settings.ui.components.SettingsCard
import com.kannada.kavi.features.themes.tokens.SpacingTokens

/**
 * ConverterScreen - Nudi ↔ Unicode Converter
 *
 * Features:
 * - Nudi to Unicode conversion
 * - Unicode to Nudi conversion (Note: currently only Nudi to Unicode is implemented)
 * - Bidirectional conversion in one screen
 * - Copy to clipboard
 * - Paste from clipboard
 * - Clear buttons
 * - Swap direction
 * - Character count
 *
 * Material You Design:
 * - Semantic colors
 * - Proper elevation
 * - Spacing tokens
 * - Typography scale
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val converter = remember { NudiToUnicodeConverter() }
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    // State
    var inputText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("") }
    var isNudiToUnicode by remember { mutableStateOf(true) }

    // Scroll behavior
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Convert function
    val convert: () -> Unit = {
        if (inputText.isBlank()) {
            outputText = ""
        } else {
            val result = if (isNudiToUnicode) {
                converter.convert(inputText)
            } else {
                converter.convertReverse(inputText)
            }
            when (result) {
                is Result.Success -> outputText = result.data
                is Result.Error -> outputText = "Error: ${result.exception.message}"
            }
        }
    }

    // Auto-convert on text change
    LaunchedEffect(inputText, isNudiToUnicode) {
        convert()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Nudi ↔ Unicode Converter",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Swap direction button
                    IconButton(onClick = {
                        isNudiToUnicode = !isNudiToUnicode
                        // Swap input and output
                        val temp = inputText
                        inputText = outputText
                        outputText = temp
                    }) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Swap Direction"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(SpacingTokens.base),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.base)
        ) {
            // Direction indicator
            DirectionIndicatorCard(
                isNudiToUnicode = isNudiToUnicode,
                onSwap = {
                    isNudiToUnicode = !isNudiToUnicode
                    val temp = inputText
                    inputText = outputText
                    outputText = temp
                }
            )

            // Input section
            InputSection(
                title = if (isNudiToUnicode) "Nudi Text" else "Unicode Text",
                text = inputText,
                onTextChange = { inputText = it },
                onPaste = {
                    val clipData = clipboardManager.primaryClip
                    if (clipData != null && clipData.itemCount > 0) {
                        inputText = clipData.getItemAt(0).text?.toString() ?: ""
                        Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                    }
                },
                onClear = { inputText = "" }
            )

            // Output section
            OutputSection(
                title = if (isNudiToUnicode) "Unicode Text" else "Nudi Text",
                text = outputText,
                onCopy = {
                    if (outputText.isNotBlank()) {
                        val clip = ClipData.newPlainText("Converted Text", outputText)
                        clipboardManager.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            // Info card
            InfoCard(isNudiToUnicode = isNudiToUnicode)
        }
    }
}

@Composable
private fun DirectionIndicatorCard(
    isNudiToUnicode: Boolean,
    onSwap: () -> Unit
) {
    SettingsCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingTokens.base),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Source
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Source,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isNudiToUnicode) "Nudi" else "Unicode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Arrow with swap button
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                IconButton(onClick = onSwap) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Swap",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Target
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Output,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isNudiToUnicode) "Unicode" else "Nudi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun InputSection(
    title: String,
    text: String,
    onTextChange: (String) -> Unit,
    onPaste: () -> Unit,
    onClear: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${text.length} characters",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Input field
        SettingsCard {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter text to convert...") },
                    minLines = 6,
                    maxLines = 10,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    )
                )

                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(SpacingTokens.sm),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.End)
                ) {
                    OutlinedButton(
                        onClick = onPaste,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Paste")
                    }

                    OutlinedButton(
                        onClick = onClear,
                        enabled = text.isNotBlank(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear")
                    }
                }
            }
        }
    }
}

@Composable
private fun OutputSection(
    title: String,
    text: String,
    onCopy: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${text.length} characters",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Output field
        SettingsCard {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Converted text will appear here...") },
                    minLines = 6,
                    maxLines = 10,
                    readOnly = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                // Copy button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(SpacingTokens.sm),
                    horizontalArrangement = Arrangement.End
                ) {
                    FilledTonalButton(
                        onClick = onCopy,
                        enabled = text.isNotBlank(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy")
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(isNudiToUnicode: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(SpacingTokens.base),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "About Conversion",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = if (isNudiToUnicode) {
                        "Converts legacy Nudi font encoding to standard Unicode Kannada. " +
                        "Unicode is the universal standard and works across all apps and platforms."
                    } else {
                        "Converts standard Unicode Kannada to Nudi font encoding. " +
                        "Note: Nudi is a legacy format and may not display correctly everywhere."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
