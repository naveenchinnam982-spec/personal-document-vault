package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultDarkBg
import com.example.ui.theme.VaultRed
import com.example.ui.theme.VaultSurfaceElevated
import com.example.ui.viewmodel.PdfViewerState
import kotlin.math.roundToInt

@Composable
fun PdfEmbeddedViewer(
    state: PdfViewerState,
    onPageSelected: (Int) -> Unit,
    onNextPage: () -> Unit,
    onPrevPage: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showFullscreenDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // PDF Controls Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(VaultSurfaceElevated)
                .border(1.dp, VaultBorder, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = VaultRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(VaultDarkBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (state.totalPages > 0) "Page ${state.currentPageIndex + 1} of ${state.totalPages}" else "Loading PDF...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onPrevPage,
                    enabled = state.currentPageIndex > 0,
                    modifier = Modifier.size(32.dp).testTag("pdf_prev_page_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Page",
                        tint = if (state.currentPageIndex > 0) EmeraldPrimary else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onNextPage,
                    enabled = state.currentPageIndex < state.totalPages - 1,
                    modifier = Modifier.size(32.dp).testTag("pdf_next_page_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Page",
                        tint = if (state.currentPageIndex < state.totalPages - 1) EmeraldPrimary else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = { showFullscreenDialog = true },
                    modifier = Modifier.size(32.dp).testTag("pdf_fullscreen_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Expand Fullscreen",
                        tint = CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Active Page Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(Color(0xFF1E222B))
                .border(1.dp, VaultBorder)
                .clickable { showFullscreenDialog = true },
            contentAlignment = Alignment.Center
        ) {
            if (state.isLoading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = EmeraldPrimary, strokeWidth = 2.5.dp, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Rendering PDF page...", fontSize = 12.sp, color = TextSecondary)
                }
            } else if (state.currentPageBitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = state.currentPageBitmap.asImageBitmap(),
                        contentDescription = "PDF Page ${state.currentPageIndex + 1}",
                        modifier = Modifier
                            .fillMaxSize()
                            .shadow(8.dp, RoundedCornerShape(4.dp))
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            } else if (state.errorMessage != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = VaultRed, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(state.errorMessage, fontSize = 13.sp, color = VaultRed)
                }
            }
        }

        // Page Thumbnails Strip
        if (state.totalPages > 1) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
                .background(VaultSurfaceElevated)
                .border(1.dp, VaultBorder, RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(state.totalPages) { index ->
                    val isSelected = index == state.currentPageIndex
                    val thumb = state.pageThumbnails.getOrNull(index)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) EmeraldPrimary else VaultBorder,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .background(if (isSelected) EmeraldPrimary.copy(alpha = 0.15f) else VaultDarkBg)
                            .clickable { onPageSelected(index) }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(52.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            if (thumb != null) {
                                Image(
                                    bitmap = thumb.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "p.${index + 1}",
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) EmeraldLight else TextSecondary
                        )
                    }
                }
            }
        }
    }

    if (showFullscreenDialog) {
        FullScreenPdfReaderDialog(
            state = state,
            onDismiss = { showFullscreenDialog = false },
            onPageSelected = onPageSelected,
            onNextPage = onNextPage,
            onPrevPage = onPrevPage,
            onDownload = onDownload,
            onShare = onShare
        )
    }
}

@Composable
fun FullScreenPdfReaderDialog(
    state: PdfViewerState,
    onDismiss: () -> Unit,
    onPageSelected: (Int) -> Unit,
    onNextPage: () -> Unit,
    onPrevPage: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    fun resetZoom() {
        scale = 1f
        offset = Offset.Zero
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF10141A)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Interactive Zoomable PDF Page
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (scale * zoom).coerceIn(0.8f, 5.0f)
                                scale = newScale
                                if (newScale > 1f) {
                                    offset = Offset(
                                        x = offset.x + pan.x,
                                        y = offset.y + pan.y
                                    )
                                } else {
                                    offset = Offset.Zero
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1.2f) {
                                        resetZoom()
                                    } else {
                                        scale = 2.5f
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(color = EmeraldPrimary, strokeWidth = 3.dp)
                    } else if (state.currentPageBitmap != null) {
                        Image(
                            bitmap = state.currentPageBitmap.asImageBitmap(),
                            contentDescription = "PDF Page ${state.currentPageIndex + 1}",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                )
                                .padding(16.dp)
                        )
                    }
                }

                // Top Floating App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = state.currentFileName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                            Text(
                                text = "Page ${state.currentPageIndex + 1} of ${state.totalPages} • ${(scale * 100).roundToInt()}%",
                                fontSize = 11.sp,
                                color = EmeraldLight
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (scale != 1f) {
                            IconButton(onClick = { resetZoom() }) {
                                Icon(Icons.Default.RestartAlt, contentDescription = "Reset Zoom", tint = CyanAccent)
                            }
                        }

                        IconButton(onClick = onDownload, modifier = Modifier.testTag("fullscreen_pdf_download_button")) {
                            Icon(Icons.Default.Download, contentDescription = "Download PDF", tint = EmeraldPrimary)
                        }

                        IconButton(onClick = onShare) {
                            Icon(Icons.Default.Share, contentDescription = "Share PDF", tint = Color.White)
                        }
                    }
                }

                // Bottom Floating Page Controls
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(Color.Black.copy(alpha = 0.85f))
                        .border(1.dp, VaultBorder, RoundedCornerShape(30.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = {
                            resetZoom()
                            onPrevPage()
                        },
                        enabled = state.currentPageIndex > 0,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Page",
                            tint = if (state.currentPageIndex > 0) EmeraldPrimary else TextMuted
                        )
                    }

                    Text(
                        text = "${state.currentPageIndex + 1} / ${state.totalPages}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    IconButton(
                        onClick = {
                            resetZoom()
                            onNextPage()
                        },
                        enabled = state.currentPageIndex < state.totalPages - 1,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Page",
                            tint = if (state.currentPageIndex < state.totalPages - 1) EmeraldPrimary else TextMuted
                        )
                    }
                }
            }
        }
    }
}
