package com.kannada.kavi.features.settings.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.kannada.kavi.features.voice.manager.TTSManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import android.os.Bundle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kannada.kavi.features.settings.ui.components.SettingsCard
import com.kannada.kavi.features.settings.ui.components.SettingsSection
import com.kannada.kavi.features.settings.ui.components.SettingsSwitchItem
import com.kannada.kavi.features.themes.tokens.SpacingTokens
import java.util.*

/**
 * TextToSpeechScreen - Text-to-Speech Settings and Testing
 *
 * Features:
 * - Enable/disable TTS
 * - Voice selection (Kannada voices)
 * - Speech rate adjustment
 * - Pitch adjustment
 * - Test TTS with sample text
 * - Custom text testing
 * - Voice download help
 *
 * Material You Design:
 * - Semantic colors
 * - Proper elevation
 * - Spacing tokens
 * - Typography scale
 * - Interactive components
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextToSpeechScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current

    // TTS Manager
    var ttsManager by remember { mutableStateOf<TTSManager?>(null) }
    var isTtsInitialized by remember { mutableStateOf(false) }
    var isTtsEnabled by remember { mutableStateOf(true) }
    var useBhashini by remember { mutableStateOf(true) }
    var isBhashiniAvailable by remember { mutableStateOf(false) }
    var isAndroidTtsAvailable by remember { mutableStateOf(false) }
    var availableVoices by remember { mutableStateOf<List<Voice>>(emptyList()) }
    var selectedVoice by remember { mutableStateOf<Voice?>(null) }
    var speechRate by remember { mutableStateOf(1.0f) }
    var pitch by remember { mutableStateOf(1.0f) }
    var testText by remember { mutableStateOf("ನಮಸ್ಕಾರ, ಕಾವಿ ಕನ್ನಡ ಕೀಬೋರ್ಡ್‌ಗೆ ಸ್ವಾಗತ") }
    var isSpeaking by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }


    // Permission launcher for storage access
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, download can proceed
            Toast.makeText(context, "Storage permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                context,
                "Storage permission required to download audio files",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Check storage permission
    fun checkStoragePermission(): Boolean {
        // Android 13+ doesn't need WRITE_EXTERNAL_STORAGE for MediaStore/Downloads
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        // Android 10-12 - check permission
        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    // Initialize TTS Manager
    DisposableEffect(Unit) {
        val manager = TTSManager(context)
        ttsManager = manager

        manager.initialize { success ->
            isTtsInitialized = success
            isBhashiniAvailable = manager.isBhashiniAvailable()
            isAndroidTtsAvailable = manager.isAndroidTtsAvailable()
            availableVoices = manager.getAvailableVoices()
            selectedVoice = availableVoices.firstOrNull()

            if (!success) {
                Toast.makeText(
                    context,
                    "No TTS engine available. Please install Kannada voice data to enable text-to-speech.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        onDispose {
            manager.shutdown()
        }
    }

    // Scroll behavior
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Speak function
    val speak: (String) -> Unit = { text ->
        if (isTtsInitialized && isTtsEnabled && text.isNotBlank()) {
            ttsManager?.setSpeechRate(speechRate)
            ttsManager?.setPitch(pitch)
            selectedVoice?.let { ttsManager?.setVoice(it) }
            ttsManager?.speak(
                text = text,
                useBhashini = useBhashini && isBhashiniAvailable,
                onStart = { isSpeaking = true },
                onComplete = { isSpeaking = false },
                onError = { error ->
                    isSpeaking = false
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    // Stop speaking
    val stopSpeaking: () -> Unit = {
        ttsManager?.stop()
        isSpeaking = false
    }

    // Download audio function
    fun downloadAudio(text: String) {
        if (text.isBlank()) {
            Toast.makeText(context, "Please enter text first", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isBhashiniAvailable) {
            Toast.makeText(
                context,
                "Amita Engine not available. Download requires online TTS.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Check permission first
        if (!checkStoragePermission()) {
            // Request permission for Android 10-12
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            return
        }

        isDownloading = true
        android.util.Log.d("TTS", "Starting download for text: $text")

        ttsManager?.downloadAudio(
            text = text,
            fileName = "kavi_tts_${System.currentTimeMillis()}.wav",
            saveToDownloads = true
        ) { success, filePath ->
            isDownloading = false
            android.util.Log.d("TTS", "Download result: success=$success, path=$filePath")

            if (success && filePath != null) {
                Toast.makeText(
                    context,
                    "Audio saved to Downloads folder",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    context,
                    "Download failed. Check internet connection and try again.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Text-to-Speech",
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
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                horizontal = SpacingTokens.base,
                vertical = SpacingTokens.base
            ),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.base)
        ) {
            // Status Card
            item {
                StatusCard(
                    isInitialized = isTtsInitialized,
                    isBhashiniAvailable = isBhashiniAvailable,
                    isAndroidTtsAvailable = isAndroidTtsAvailable,
                    isEnabled = isTtsEnabled,
                    onInstallClick = {
                        // Open TTS settings to download voices
                        val intent = Intent()
                        intent.action = "com.android.settings.TTS_SETTINGS"
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    }
                )
            }

            // Enable TTS
            item {
                SettingsSection(title = "Settings")
            }

            item {
                SettingsCard {
                    Column {
                        SettingsSwitchItem(
                            title = "Enable Text-to-Speech",
                            description = "Read out typed text and suggestions",
                            icon = Icons.Default.VolumeUp,
                            checked = isTtsEnabled,
                            enabled = isTtsInitialized,
                            onCheckedChange = { isTtsEnabled = it }
                        )

                        if (isBhashiniAvailable && isTtsEnabled) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            SettingsSwitchItem(
                                title = "Use Amita Engine",
                                description = "High-quality cloud-based text-to-speech (requires internet)",
                                icon = Icons.Default.Cloud,
                                checked = useBhashini,
                                enabled = true,
                                onCheckedChange = { useBhashini = it }
                            )
                        }
                    }
                }
            }

            // Voice Selection (Android TTS only)
            if (isTtsInitialized && isTtsEnabled && isAndroidTtsAvailable &&
                (!useBhashini || !isBhashiniAvailable) && availableVoices.isNotEmpty()) {
                item {
                    SettingsSection(title = "Voice (Android TTS)")
                }

                items(availableVoices) { voice ->
                    VoiceSelectionCard(
                        voice = voice,
                        isSelected = selectedVoice == voice,
                        onSelect = {
                            selectedVoice = voice
                            ttsManager?.setVoice(voice)
                        }
                    )
                }
            }

            // Speech Rate
            if (isTtsInitialized && isTtsEnabled) {
                item {
                    SettingsSection(title = "Speech Rate")
                }

                item {
                    SliderCard(
                        title = "Speed",
                        value = speechRate,
                        valueRange = 0.5f..2.0f,
                        onValueChange = { speechRate = it },
                        valueLabel = String.format("%.1fx", speechRate),
                        icon = Icons.Default.Speed
                    )
                }
            }

            // Pitch
            if (isTtsInitialized && isTtsEnabled) {
                item {
                    SettingsSection(title = "Pitch")
                }

                item {
                    SliderCard(
                        title = "Voice Pitch",
                        value = pitch,
                        valueRange = 0.5f..2.0f,
                        onValueChange = { pitch = it },
                        valueLabel = String.format("%.1fx", pitch),
                        icon = Icons.Default.MusicNote
                    )
                }
            }

            // Test Section
            if (isTtsInitialized && isTtsEnabled) {
                item {
                    SettingsSection(title = "Test")
                }

                item {
                    TestCard(
                        text = testText,
                        onTextChange = { testText = it },
                        isSpeaking = isSpeaking,
                        isDownloading = isDownloading,
                        isBhashiniAvailable = isBhashiniAvailable,
                        onSpeak = { speak(testText) },
                        onStop = stopSpeaking,
                        onDownload = { downloadAudio(testText) }
                    )
                }
            }

        }
    }
}

@Composable
private fun StatusCard(
    isInitialized: Boolean,
    isBhashiniAvailable: Boolean,
    isAndroidTtsAvailable: Boolean,
    isEnabled: Boolean,
    onInstallClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isInitialized) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(SpacingTokens.base),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isInitialized) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (isInitialized) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    },
                    modifier = Modifier.size(32.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isInitialized) "TTS Ready" else "TTS Not Available",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isInitialized) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                    Text(
                        text = when {
                            isBhashiniAvailable && isAndroidTtsAvailable ->
                                "Amita Engine (cloud) and Android TTS (offline) available"
                            isBhashiniAvailable ->
                                "Amita Engine available (cloud-based)"
                            isAndroidTtsAvailable ->
                                "Android TTS available (offline)"
                            else ->
                                "Please install Kannada voice data to enable text-to-speech"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isInitialized) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
                if (!isInitialized) {
                    OutlinedButton(onClick = onInstallClick) {
                        Text("Install")
                    }
                }
            }

            // Show detailed status for each engine
            if (isInitialized) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.base)
                ) {
                    // Amita Engine Status
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isBhashiniAvailable) Icons.Default.Cloud else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Amita: ${if (isBhashiniAvailable) "✓" else "✗"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }

                    // Android TTS Status
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isAndroidTtsAvailable) Icons.Default.PhoneAndroid else Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Android: ${if (isAndroidTtsAvailable) "✓" else "✗"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceSelectionCard(
    voice: Voice,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    SettingsCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSelect)
                .padding(SpacingTokens.base),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = voice.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = "Quality: ${voice.quality}/500 • ${voice.locale.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SliderCard(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueLabel: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    SettingsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingTokens.base),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Text(
                    text = valueLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Slow",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Fast",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TestCard(
    text: String,
    onTextChange: (String) -> Unit,
    isSpeaking: Boolean,
    isDownloading: Boolean,
    isBhashiniAvailable: Boolean,
    onSpeak: () -> Unit,
    onStop: () -> Unit,
    onDownload: () -> Unit
) {
    SettingsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingTokens.base),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm)
        ) {
            Text(
                text = "Test TTS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Enter text to speak") },
                minLines = 3,
                maxLines = 5,
                placeholder = { Text("Type Kannada text here...") }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                // Download button (only for Bhashini)
                if (isBhashiniAvailable && !isSpeaking) {
                    OutlinedButton(
                        onClick = onDownload,
                        enabled = text.isNotBlank() && !isDownloading
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isDownloading) "Downloading..." else "Download")
                    }
                }

                // Speak/Stop button
                if (isSpeaking) {
                    FilledTonalButton(
                        onClick = onStop,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop")
                    }
                } else {
                    FilledTonalButton(
                        onClick = onSpeak,
                        enabled = text.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Speak")
                    }
                }
            }
        }
    }
}
