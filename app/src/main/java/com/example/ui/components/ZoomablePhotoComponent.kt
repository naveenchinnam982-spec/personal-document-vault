package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.security.CryptoManager
import com.example.data.security.VaultType
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.HiddenPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * High-performance, memory-safe Zoomable Photo Viewer for HorizontalPager galleries.
 *
 * Supports:
 * - Lazy background decryption without UI freezing
 * - Automatic inSampleSize downsampling preventing OutOfMemoryError on 100+ images
 * - Single-finger horizontal swipe pass-through to HorizontalPager at scale == 1f
 * - Pinch-to-zoom (1x - 4x) and pan when zoomed
 * - Double-tap to smoothly zoom in (2.5x) or zoom out (1x)
 * - Single tap to toggle UI controls
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ZoomablePhotoComponent(
    filePath: String,
    vaultType: VaultType,
    contentDescription: String,
    modifier: Modifier = Modifier,
    accentColor: Color = if (vaultType == VaultType.HIDDEN) HiddenPrimary else EmeraldPrimary,
    onZoomChanged: (Float) -> Unit = {},
    onTap: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val cryptoManager = remember { CryptoManager(context) }

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableStateOf(0) }

    val scaleAnim = remember { Animatable(1f) }
    val offsetAnim = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Lazy load & decrypt image safely in background
    LaunchedEffect(filePath, retryTrigger) {
        isLoading = true
        errorMessage = null
        withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                val actualFile = if (!file.exists()) {
                    val fileName = file.name
                    val searchDirs = listOf(
                        File(context.filesDir, "vault_private"),
                        File(context.filesDir, "vault_hidden"),
                        File(context.filesDir, "vault_hidden_hidden"),
                        File(context.filesDir, "vault_documents")
                    )
                    searchDirs.map { File(it, fileName) }.firstOrNull { it.exists() } ?: file
                } else {
                    file
                }

                if (!actualFile.exists()) {
                    errorMessage = "File does not exist"
                    isLoading = false
                    return@withContext
                }

                // Decrypt file bytes using primary vault type and fallbacks
                val vaultOrder = listOf(vaultType, VaultType.PRIVATE, VaultType.HIDDEN, VaultType.HIDDEN_HIDDEN, VaultType.MAIN).distinct()
                var decryptedBytes: ByteArray? = null
                for (v in vaultOrder) {
                    decryptedBytes = cryptoManager.decryptFileToBytes(actualFile, v)
                    if (decryptedBytes != null && decryptedBytes.isNotEmpty()) break
                }

                // If not encrypted, try reading raw bytes directly
                if (decryptedBytes == null || decryptedBytes.isEmpty()) {
                    try {
                        decryptedBytes = actualFile.readBytes()
                    } catch (_: Exception) {}
                }

                if (decryptedBytes != null && decryptedBytes.isNotEmpty()) {
                    // Safe decode with inSampleSize to prevent OOM
                    val boundsOptions = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size, boundsOptions)

                    var sampleSize = 1
                    val maxDimension = 2560
                    while (boundsOptions.outWidth / sampleSize > maxDimension || boundsOptions.outHeight / sampleSize > maxDimension) {
                        sampleSize *= 2
                    }

                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    val decoded = BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size, decodeOptions)
                    if (decoded != null) {
                        bitmap = decoded
                    } else {
                        errorMessage = "Unsupported photo format"
                    }
                } else {
                    errorMessage = "Failed to decrypt photo"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load photo"
            } finally {
                isLoading = false
            }
        }
    }

    // Reset zoom and offset when moving away or disposing
    LaunchedEffect(filePath) {
        scaleAnim.snapTo(1f)
        offsetAnim.snapTo(Offset.Zero)
        onZoomChanged(1f)
    }

    // Pinch-to-zoom multi-touch handler (only activates with 2 fingers)
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val currentScale = scaleAnim.value
        val newScale = (currentScale * zoomChange).coerceIn(1f, 4.5f)
        coroutineScope.launch {
            if (newScale > 1.05f) {
                scaleAnim.snapTo(newScale)
                onZoomChanged(newScale)
                val maxOffsetX = ((containerSize.width * (newScale - 1f)) / 2f).coerceAtLeast(0f)
                val maxOffsetY = ((containerSize.height * (newScale - 1f)) / 2f).coerceAtLeast(0f)
                val newOffset = Offset(
                    x = (offsetAnim.value.x + panChange.x).coerceIn(-maxOffsetX, maxOffsetX),
                    y = (offsetAnim.value.y + panChange.y).coerceIn(-maxOffsetY, maxOffsetY)
                )
                offsetAnim.snapTo(newOffset)
            } else {
                scaleAnim.snapTo(1f)
                offsetAnim.snapTo(Offset.Zero)
                onZoomChanged(1f)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { containerSize = it }
            .transformable(
                state = transformState,
                canPan = { scaleAnim.value > 1.05f },
                lockRotationOnZoomPan = true
            )
            .then(
                if (scaleAnim.value > 1.05f) {
                    Modifier.pointerInput(scaleAnim.value) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val currentScale = scaleAnim.value
                            val maxOffsetX = ((containerSize.width * (currentScale - 1f)) / 2f).coerceAtLeast(0f)
                            val maxOffsetY = ((containerSize.height * (currentScale - 1f)) / 2f).coerceAtLeast(0f)
                            coroutineScope.launch {
                                val newX = (offsetAnim.value.x + dragAmount.x).coerceIn(-maxOffsetX, maxOffsetX)
                                val newY = (offsetAnim.value.y + dragAmount.y).coerceIn(-maxOffsetY, maxOffsetY)
                                offsetAnim.snapTo(Offset(newX, newY))
                            }
                        }
                    }
                } else {
                    Modifier
                }
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        coroutineScope.launch {
                            if (scaleAnim.value > 1.1f) {
                                // Zoom out to normal
                                launch { scaleAnim.animateTo(1f, tween(250)) }
                                launch { offsetAnim.animateTo(Offset.Zero, tween(250)) }
                                onZoomChanged(1f)
                            } else {
                                // Zoom in to 2.5x
                                launch { scaleAnim.animateTo(2.5f, tween(250)) }
                                onZoomChanged(2.5f)
                            }
                        }
                    },
                    onTap = {
                        onTap()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    color = accentColor,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(44.dp)
                )
            }
            errorMessage != null -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(VaultRed.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = null,
                            tint = VaultRed,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = errorMessage ?: "Failed to load photo",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { retryTrigger++ },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Retry", fontWeight = FontWeight.Bold)
                    }
                }
            }
            bitmap != null -> {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scaleAnim.value,
                            scaleY = scaleAnim.value,
                            translationX = offsetAnim.value.x,
                            translationY = offsetAnim.value.y
                        )
                )
            }
        }
    }
}
