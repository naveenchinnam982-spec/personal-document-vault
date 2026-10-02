package com.example.ui.components

import android.net.Uri
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.security.CryptoManager
import com.example.data.security.VaultType
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.HiddenPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Production-ready Video Player Component using modern AndroidX Media3 ExoPlayer.
 *
 * Replaces legacy MediaPlayer / TextureView to eliminate IllegalStateException on Samsung / Android 15/16,
 * while safely decoupling hardware surfaces from recomposition cycles.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerComponent(
    encryptedPath: String,
    vaultType: VaultType,
    videoTitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = if (vaultType == VaultType.HIDDEN) HiddenPrimary else EmeraldPrimary,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val cryptoManager = remember { CryptoManager(context) }

    var playbackUri by remember { mutableStateOf<Uri?>(null) }
    var tempFileToDelete by remember { mutableStateOf<File?>(null) }
    var isDecrypting by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(false) }
    var decryptionError by remember { mutableStateOf<String?>(null) }

    var isPlaying by remember { mutableStateOf(false) }
    var isVideoCompleted by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(0) }
    var isUserSeeking by remember { mutableStateOf(false) }
    var seekPositionMs by remember { mutableFloatStateOf(0f) }
    var areControlsVisible by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }

    // Initialize ExoPlayer once with remember and context
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    // Helper to decrypt file to a temporary file
    fun decryptFileToTemp(src: File, primaryVault: VaultType): File? {
        val playbackDir = File(context.cacheDir, "temp_playback").apply { mkdirs() }
        // Clean up any stale playback files older than 3 minutes
        playbackDir.listFiles()?.forEach {
            if (System.currentTimeMillis() - it.lastModified() > 180_000) {
                try { it.delete() } catch (_: Exception) {}
            }
        }
        val ext = when {
            src.name.endsWith(".mov", ignoreCase = true) || encryptedPath.endsWith(".mov", ignoreCase = true) -> ".mov"
            src.name.endsWith(".mkv", ignoreCase = true) || encryptedPath.endsWith(".mkv", ignoreCase = true) -> ".mkv"
            src.name.endsWith(".webm", ignoreCase = true) || encryptedPath.endsWith(".webm", ignoreCase = true) -> ".webm"
            else -> ".mp4"
        }
        val tempFile = File(playbackDir, "play_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}$ext")

        // Try decryptToFile with primary vault and other vaults as fallback
        val vaultSequence = listOf(primaryVault, VaultType.HIDDEN_HIDDEN, VaultType.PRIVATE, VaultType.HIDDEN, VaultType.MAIN).distinct()
        for (vType in vaultSequence) {
            val ok = cryptoManager.decryptToFile(src, tempFile, vType)
            if (ok && tempFile.exists() && tempFile.length() > 0) {
                tempFile.deleteOnExit()
                return tempFile
            }
        }

        // Fallback: decryptFileToBytes
        for (vType in vaultSequence) {
            val bytes = cryptoManager.decryptFileToBytes(src, vType)
            if (bytes != null && bytes.isNotEmpty()) {
                tempFile.writeBytes(bytes)
                tempFile.deleteOnExit()
                return tempFile
            }
        }

        // If source is already a readable plain video file
        if (src.exists() && src.length() > 0) {
            try {
                src.inputStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (tempFile.exists() && tempFile.length() > 0) {
                    tempFile.deleteOnExit()
                    return tempFile
                }
            } catch (_: Exception) {}
        }
        return null
    }

    // 1. Decrypt video to temporary private cache file or prepare content URI
    fun prepareVideo() {
        scope.launch {
            isDecrypting = true
            decryptionError = null
            playbackUri = null

            val result = withContext(Dispatchers.IO) {
                try {
                    // Check if encryptedPath is a content:// URI
                    if (encryptedPath.startsWith("content://")) {
                        val parsedUri = Uri.parse(encryptedPath)
                        // If it's a content URI, try reading stream into temp file or play directly
                        val playbackDir = File(context.cacheDir, "temp_playback").apply { mkdirs() }
                        val tempFile = File(playbackDir, "play_content_${System.currentTimeMillis()}.mp4")
                        context.contentResolver.openInputStream(parsedUri)?.use { input ->
                            FileOutputStream(tempFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        if (tempFile.exists() && tempFile.length() > 0) {
                            tempFile.deleteOnExit()
                            Pair(Uri.fromFile(tempFile), tempFile)
                        } else {
                            Pair(parsedUri, null)
                        }
                    } else {
                        val src = File(encryptedPath)
                        val actualFile = if (!src.exists()) {
                            val fileName = src.name
                            val altDirs = listOf(
                                File(context.filesDir, "vault_hidden_hidden"),
                                File(context.filesDir, "vault_hidden"),
                                File(context.filesDir, "vault_private"),
                                File(context.filesDir, "vault_documents")
                            )
                            altDirs.map { File(it, fileName) }.firstOrNull { it.exists() } ?: src
                        } else {
                            src
                        }

                        val decrypted = decryptFileToTemp(actualFile, vaultType)
                        if (decrypted != null && decrypted.exists() && decrypted.length() > 0) {
                            Pair(Uri.fromFile(decrypted), decrypted)
                        } else {
                            null
                        }
                    }
                } catch (e: Exception) {
                    null
                }
            }

            isDecrypting = false
            if (result != null) {
                playbackUri = result.first
                tempFileToDelete = result.second
                // Load MediaItem into ExoPlayer
                val mediaItem = MediaItem.fromUri(result.first)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.play()
            } else {
                decryptionError = "Unable to decrypt and prepare video file"
            }
        }
    }

    LaunchedEffect(encryptedPath) {
        prepareVideo()
    }

    // Attach ExoPlayer event listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        isBuffering = true
                    }
                    Player.STATE_READY -> {
                        isBuffering = false
                        val dur = exoPlayer.duration
                        if (dur > 0) {
                            durationMs = dur.toInt()
                        }
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                        isPlaying = false
                        isVideoCompleted = true
                        areControlsVisible = true
                    }
                    Player.STATE_IDLE -> {
                        isBuffering = false
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                isPlaying = false
                decryptionError = "Video playback error: ${error.localizedMessage ?: error.errorCodeName}"
            }
        }
        exoPlayer.addListener(listener)

        // Lifecycle observer for pause/resume handling
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
            try {
                tempFileToDelete?.delete()
            } catch (_: Exception) {}
        }
    }

    // Continuous time tracker loop while playing
    LaunchedEffect(isPlaying, isUserSeeking) {
        while (isPlaying && !isUserSeeking) {
            val pos = exoPlayer.currentPosition
            if (pos >= 0) {
                currentPositionMs = pos.toInt()
            }
            val dur = exoPlayer.duration
            if (dur > 0) {
                durationMs = dur.toInt()
            }
            delay(250L)
        }
    }

    // Auto-hide controls timer
    LaunchedEffect(areControlsVisible, isPlaying) {
        if (areControlsVisible && isPlaying) {
            delay(3500L)
            areControlsVisible = false
        }
    }

    fun formatTime(ms: Int): String {
        val totalSecs = (ms / 1000).coerceAtLeast(0)
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return "%02d:%02d".format(mins, secs)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("encrypted_video_player")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                areControlsVisible = !areControlsVisible
            }
    ) {
        // Main Video Playback View using Media3 PlayerView
        if (playbackUri != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                update = { playerView ->
                    if (playerView.player != exoPlayer) {
                        playerView.player = exoPlayer
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Decrypting Loading Spinner
        if (isDecrypting) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = accentColor,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Decrypting Secure Video...",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Hardware Key AES-256 Decryption",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Playback Buffering Spinner
        if (isBuffering && !isDecrypting && decryptionError == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = accentColor,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Decryption / Playback Error Screen
        if (decryptionError != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = VaultRed,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Playback Failed",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = decryptionError ?: "Unknown error",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { prepareVideo() },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retry Decryption", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Overlay Controls
        AnimatedVisibility(
            visible = areControlsVisible && !isDecrypting && decryptionError == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .testTag("video_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Column(modifier = Modifier.padding(start = 4.dp)) {
                            Text(
                                text = videoTitle,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "Secure Video",
                                    color = accentColor,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        actions()

                        // Mute / Unmute Button
                        IconButton(
                            onClick = {
                                isMuted = !isMuted
                                exoPlayer.volume = if (isMuted) 0f else 1f
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .testTag("video_mute_button")
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Center Play / Pause / Replay Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rewind 10s
                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                            exoPlayer.seekTo(newPos)
                            currentPositionMs = newPos.toInt()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .testTag("video_rewind_10s")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Main Center Action (Play, Pause, or Replay)
                    IconButton(
                        onClick = {
                            if (isVideoCompleted) {
                                exoPlayer.seekTo(0)
                                exoPlayer.play()
                                isPlaying = true
                                isVideoCompleted = false
                            } else if (isPlaying) {
                                exoPlayer.pause()
                                isPlaying = false
                            } else {
                                exoPlayer.play()
                                isPlaying = true
                            }
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                            .testTag("video_main_play_pause")
                    ) {
                        Icon(
                            imageVector = when {
                                isVideoCompleted -> Icons.Default.Replay
                                isPlaying -> Icons.Default.Pause
                                else -> Icons.Default.PlayArrow
                            },
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    // Forward 10s
                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(durationMs.toLong())
                            exoPlayer.seekTo(newPos)
                            currentPositionMs = newPos.toInt()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .testTag("video_forward_10s")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Bottom Timeline & Time Overlay
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    val progressValue = if (isUserSeeking) {
                        seekPositionMs
                    } else if (durationMs > 0) {
                        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Slider(
                        value = progressValue,
                        onValueChange = {
                            isUserSeeking = true
                            seekPositionMs = it
                        },
                        onValueChangeFinished = {
                            val targetMs = (seekPositionMs * durationMs).toLong()
                            exoPlayer.seekTo(targetMs)
                            currentPositionMs = targetMs.toInt()
                            isUserSeeking = false
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .testTag("video_seek_slider")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val displayCurrent = if (isUserSeeking) {
                            (seekPositionMs * durationMs).toInt()
                        } else currentPositionMs

                        Text(
                            text = formatTime(displayCurrent),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = formatTime(durationMs),
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
