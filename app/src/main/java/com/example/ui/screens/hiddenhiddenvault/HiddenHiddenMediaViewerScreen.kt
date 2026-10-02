package com.example.ui.screens.hiddenhiddenvault

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import com.example.ui.components.ZoomablePhotoComponent
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.HiddenHiddenItem
import com.example.data.security.VaultType
import com.example.ui.components.VideoPlayerComponent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultRed
import com.example.ui.viewmodel.VaultViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HiddenHiddenMediaViewerScreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val rawList by viewModel.viewerHiddenHiddenItemList.collectAsStateWithLifecycle()
    val allItemsState by viewModel.allHiddenHiddenItems.collectAsStateWithLifecycle()
    val allItems = if (rawList.isNotEmpty()) rawList else allItemsState
    val initialItem by viewModel.selectedHiddenHiddenItem.collectAsStateWithLifecycle()

    if (allItems.isEmpty() || initialItem == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val initialPage = remember {
        val targetId = initialItem?.id ?: 0L
        val found = allItems.indexOfFirst { it.id == targetId }
        if (found >= 0) found else 0
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, (allItems.size - 1).coerceAtLeast(0)),
        pageCount = { allItems.size }
    )

    // Handle item deletion or list updates safely
    LaunchedEffect(allItems.size) {
        if (allItems.isEmpty()) {
            onBack()
        } else if (pagerState.currentPage >= allItems.size) {
            pagerState.scrollToPage(allItems.size - 1)
        }
    }

    val currentItem = allItems.getOrNull(pagerState.currentPage) ?: initialItem!!
    val isVideo = currentItem.itemType == "VIDEO" || currentItem.encryptedPath.endsWith("enc_vid")

    var areControlsVisible by remember { mutableStateOf(true) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isCurrentPageZoomed by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        isCurrentPageZoomed = false
        val pageItem = allItems.getOrNull(pagerState.currentPage)
        if (pageItem != null && pageItem.id != initialItem?.id) {
            viewModel.updateSelectedHiddenHiddenItemWithoutNavigating(pageItem)
        }
    }

    BackHandler {
        onBack()
    }

    fun shareItem(item: HiddenHiddenItem) {
        scope.launch {
            val ext = if (item.itemType == "VIDEO") "mp4" else "jpg"
            val exportName = "${item.name.replace(" ", "_")}.$ext"
            val exported = viewModel.prepareDecryptedFileForSharing(
                filePath = item.encryptedPath,
                exportName = exportName,
                vaultType = VaultType.HIDDEN_HIDDEN
            )
            if (exported != null) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    exported
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = if (item.itemType == "VIDEO") "video/*" else "image/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    context.startActivity(Intent.createChooser(shareIntent, "Share Secure File"))
                } catch (e: Exception) {
                    viewModel.showToast("No app available to share this file")
                }
            } else {
                viewModel.showToast("Unable to prepare file for sharing")
            }
        }
    }

    fun downloadItem(item: HiddenHiddenItem) {
        viewModel.downloadHiddenHiddenItem(item) { success, msg, _ ->
            viewModel.showToast(msg)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("hidden_hidden_media_viewer")
    ) {
        // Horizontal Pager between items
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !isCurrentPageZoomed,
            modifier = Modifier.fillMaxSize(),
            key = { allItems[it].id }
        ) { page ->
            val pageItem = allItems[page]
            val pageIsVideo = pageItem.itemType == "VIDEO" || pageItem.encryptedPath.endsWith("enc_vid")

            if (pageIsVideo) {
                VideoPlayerComponent(
                    encryptedPath = pageItem.encryptedPath,
                    vaultType = VaultType.HIDDEN_HIDDEN,
                    videoTitle = pageItem.name,
                    onBack = onBack,
                    accentColor = Color(0xFFF43F5E),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ZoomablePhotoComponent(
                    filePath = pageItem.encryptedPath,
                    vaultType = VaultType.HIDDEN_HIDDEN,
                    contentDescription = pageItem.name,
                    accentColor = Color(0xFFF43F5E),
                    onZoomChanged = { scale ->
                        if (pagerState.currentPage == page) {
                            isCurrentPageZoomed = scale > 1.05f
                        }
                    },
                    onTap = { areControlsVisible = !areControlsVisible },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Left (Previous) Navigation Arrow Button
        if (areControlsVisible && !isVideo && pagerState.currentPage > 0) {
            IconButton(
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .testTag("hh_viewer_prev_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Item",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Right (Next) Navigation Arrow Button
        if (areControlsVisible && !isVideo && pagerState.currentPage < allItems.size - 1) {
            IconButton(
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .testTag("hh_viewer_next_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Item",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Top Overlay Bar (Filename, counter, back)
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("viewer_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Column {
                            Text(
                                text = currentItem.name,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${pagerState.currentPage + 1} / ${allItems.size}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Share button in top bar
                    IconButton(
                        onClick = { shareItem(currentItem) },
                        modifier = Modifier.testTag("viewer_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Bottom Overlay Bar (Download, Delete, Close)
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Download / Export
                    IconButton(
                        onClick = { downloadItem(currentItem) },
                        modifier = Modifier.testTag("viewer_download_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.White)
                            Text("Download", color = Color.White, fontSize = 10.sp)
                        }
                    }

                    // Share
                    IconButton(
                        onClick = { shareItem(currentItem) }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                            Text("Share", color = Color.White, fontSize = 10.sp)
                        }
                    }

                    // Delete
                    IconButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.testTag("viewer_delete_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = VaultRed)
                            Text("Delete", color = VaultRed, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete this item?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text(
                    "This file will be permanently deleted from the Hidden Hidden Vault.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteHiddenHiddenItem(currentItem, fromViewer = true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Color(0xFF1E1018),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun ZoomablePhotoViewer(
    encryptedPath: String,
    onTap: () -> Unit
) {
    ZoomablePhotoComponent(
        filePath = encryptedPath,
        vaultType = VaultType.HIDDEN_HIDDEN,
        contentDescription = "Decrypted photo",
        accentColor = Color(0xFFF43F5E),
        onTap = onTap,
        modifier = Modifier.fillMaxSize()
    )
}
