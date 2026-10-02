package com.example.ui.screens.privatevault

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.security.VaultType
import com.example.ui.components.VideoPlayerComponent
import com.example.ui.components.ZoomablePhotoComponent
import com.example.ui.screens.documents.outlinedFieldColors
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultDarkBg
import com.example.ui.theme.VaultRed
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.VaultSurfaceElevated
import com.example.ui.viewmodel.VaultViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoViewerScreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val rawMediaList by viewModel.viewerMediaList.collectAsStateWithLifecycle()
    val allPrivateMedia by viewModel.allPrivateMedia.collectAsStateWithLifecycle()
    val mediaList = if (rawMediaList.isNotEmpty()) rawMediaList else allPrivateMedia
    val selectedMedia by viewModel.selectedMedia.collectAsStateWithLifecycle()
    val albums by viewModel.allAlbums.collectAsStateWithLifecycle()

    var showMoveMenu by remember { mutableStateOf(false) }
    var showMoveToHiddenDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isSharing by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var areControlsVisible by remember { mutableStateOf(true) }
    var isCurrentPageZoomed by remember { mutableStateOf(false) }

    BackHandler {
        onBack()
    }

    if (mediaList.isEmpty() || selectedMedia == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(VaultDarkBg),
            contentAlignment = Alignment.Center
        ) {
            Text("No media selected", color = TextSecondary)
        }
        LaunchedEffect(Unit) {
            onBack()
        }
        return
    }

    // Preserve the exact selected photo index when opened from gallery
    val initialPage = remember {
        val targetId = selectedMedia?.id ?: 0L
        val found = mediaList.indexOfFirst { it.id == targetId }
        if (found >= 0) found else 0
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, (mediaList.size - 1).coerceAtLeast(0)),
        pageCount = { mediaList.size }
    )

    // Handle photo deletion or list count updates safely
    LaunchedEffect(mediaList.size) {
        if (mediaList.isEmpty()) {
            onBack()
        } else if (pagerState.currentPage >= mediaList.size) {
            pagerState.scrollToPage(mediaList.size - 1)
        }
    }

    // Reset zoom state and sync selectedMedia whenever swiping to an adjacent photo
    LaunchedEffect(pagerState.currentPage) {
        isCurrentPageZoomed = false
        val pageItem = mediaList.getOrNull(pagerState.currentPage)
        if (pageItem != null && pageItem.id != selectedMedia?.id) {
            viewModel.updateSelectedMediaWithoutNavigating(pageItem)
        }
    }

    val currentMedia = mediaList.getOrNull(pagerState.currentPage) ?: selectedMedia!!
    val isCurrentItemVideo = currentMedia.mediaType == "VIDEO" ||
            currentMedia.encryptedPath.endsWith("enc_vid") ||
            currentMedia.encryptedPath.contains("vid", ignoreCase = true) ||
            currentMedia.name.contains("Video", ignoreCase = true) ||
            currentMedia.name.lowercase().let {
                it.endsWith(".mp4") || it.endsWith(".mov") || it.endsWith(".mkv") || it.endsWith(".webm") ||
                        it.endsWith(".3gp") || it.endsWith(".m4v") || it.endsWith(".avi") || it.endsWith(".flv")
            }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("photo_viewer_screen")
    ) {
        // Horizontal Pager for smooth side-swipe between photos
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !isCurrentPageZoomed,
            key = { mediaList[it].id },
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val pageMedia = mediaList[page]
            val isPageVideo = pageMedia.mediaType == "VIDEO" ||
                    pageMedia.encryptedPath.endsWith("enc_vid") ||
                    pageMedia.encryptedPath.contains("vid", ignoreCase = true) ||
                    pageMedia.name.contains("Video", ignoreCase = true) ||
                    pageMedia.name.lowercase().let {
                        it.endsWith(".mp4") || it.endsWith(".mov") || it.endsWith(".mkv") || it.endsWith(".webm") ||
                                it.endsWith(".3gp") || it.endsWith(".m4v") || it.endsWith(".avi") || it.endsWith(".flv")
                    }

            if (isPageVideo) {
                VideoPlayerComponent(
                    encryptedPath = pageMedia.encryptedPath,
                    vaultType = VaultType.PRIVATE,
                    videoTitle = pageMedia.name,
                    onBack = onBack,
                    accentColor = EmeraldPrimary,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ZoomablePhotoComponent(
                    filePath = pageMedia.encryptedPath,
                    vaultType = VaultType.PRIVATE,
                    contentDescription = pageMedia.name,
                    accentColor = EmeraldPrimary,
                    onZoomChanged = { scale ->
                        if (pagerState.currentPage == page) {
                            isCurrentPageZoomed = scale > 1.05f
                        }
                    },
                    onTap = {
                        areControlsVisible = !areControlsVisible
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Left (Previous) Navigation Arrow Button
        if (areControlsVisible && !isCurrentItemVideo && pagerState.currentPage > 0) {
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .testTag("photo_viewer_prev_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Photo",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Right (Next) Navigation Arrow Button
        if (areControlsVisible && !isCurrentItemVideo && pagerState.currentPage < mediaList.size - 1) {
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .testTag("photo_viewer_next_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Photo",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Top App Bar with Title, Counter (e.g. 4 / 25), and Actions
        AnimatedVisibility(
            visible = areControlsVisible && !isCurrentItemVideo,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.70f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f))
                                .testTag("photo_viewer_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = currentMedia.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${pagerState.currentPage + 1} / ${mediaList.size}",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    }

                    // Rename button
                    IconButton(
                        onClick = {
                            renameInput = currentMedia.name
                            showRenameDialog = true
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f))
                            .testTag("rename_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Bottom Action Bar: Favorite, Move to Album, Share, Download/Export, Delete
        AnimatedVisibility(
            visible = areControlsVisible && !isCurrentItemVideo,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = VaultSurface.copy(alpha = 0.92f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Favorite Toggle
                    IconButton(
                        onClick = { viewModel.toggleMediaFavorite(currentMedia) },
                        modifier = Modifier.testTag("photo_fav_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (currentMedia.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (currentMedia.isFavorite) VaultAmber else Color.White
                            )
                            Text(
                                text = "Favorite",
                                color = if (currentMedia.isFavorite) VaultAmber else TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Move to Album
                    Box {
                        IconButton(
                            onClick = { showMoveMenu = true },
                            modifier = Modifier.testTag("photo_move_album_button")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                                    contentDescription = "Move to Album",
                                    tint = Color.White
                                )
                                Text("Album", color = TextMuted, fontSize = 10.sp)
                            }
                        }

                        DropdownMenu(
                            expanded = showMoveMenu,
                            onDismissRequest = { showMoveMenu = false },
                            modifier = Modifier.background(VaultSurfaceElevated)
                        ) {
                            DropdownMenuItem(
                                text = { Text("🔒 Move to Hidden Vault (Private)", color = com.example.ui.theme.HiddenPrimary, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    showMoveMenu = false
                                    showMoveToHiddenDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Unorganized (No Album)", color = TextPrimary) },
                                onClick = {
                                    viewModel.moveMediaToAlbum(listOf(currentMedia.id), null)
                                    showMoveMenu = false
                                }
                            )
                            albums.forEach { alb ->
                                DropdownMenuItem(
                                    text = { Text(alb.name, color = TextPrimary) },
                                    onClick = {
                                        viewModel.moveMediaToAlbum(listOf(currentMedia.id), alb.id)
                                        showMoveMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Share
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                isSharing = true
                                val safeExportName = "${currentMedia.name.replace(" ", "_")}.png"
                                val tempFile = viewModel.prepareDecryptedFileForSharing(
                                    currentMedia.encryptedPath,
                                    safeExportName,
                                    isPrivate = true
                                )
                                if (tempFile != null) {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        tempFile
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/*"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    try {
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Private Photo"))
                                    } catch (e: Exception) {
                                        viewModel.showToast("No sharing app available")
                                    }
                                } else {
                                    viewModel.showToast("Could not export encrypted file")
                                }
                                isSharing = false
                            }
                        },
                        modifier = Modifier.testTag("photo_share_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (isSharing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = EmeraldPrimary, strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = Color.White
                                )
                            }
                            Text("Share", color = TextMuted, fontSize = 10.sp)
                        }
                    }

                    // Download / Export to Device
                    IconButton(
                        onClick = {
                            isDownloading = true
                            viewModel.downloadPrivateMedia(currentMedia) { success, msg, _ ->
                                isDownloading = false
                                viewModel.showToast(msg)
                            }
                        },
                        modifier = Modifier.testTag("photo_download_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (isDownloading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = EmeraldPrimary, strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download",
                                    tint = Color.White
                                )
                            }
                            Text("Download", color = TextMuted, fontSize = 10.sp)
                        }
                    }

                    // Move to Trash / Delete
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.testTag("photo_delete_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = VaultRed
                            )
                            Text("Delete", color = VaultRed, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }

    // Rename Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = VaultSurfaceElevated,
            title = { Text("Rename Photo", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    colors = outlinedFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("rename_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            viewModel.renameMedia(currentMedia, renameInput.trim())
                            showRenameDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White)
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = VaultSurfaceElevated,
            title = { Text("Move to Trash?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Photo \"${currentMedia.name}\" will be moved to the Trash bin and can be restored anytime.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.moveMediaToTrash(currentMedia, fromViewer = true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultRed, contentColor = Color.White)
                ) {
                    Text("Move to Trash")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Move to Hidden Vault Dialog
    if (showMoveToHiddenDialog) {
        AlertDialog(
            onDismissRequest = { showMoveToHiddenDialog = false },
            containerColor = VaultSurfaceElevated,
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = com.example.ui.theme.HiddenPrimary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Move to Hidden Vault?",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "\"${currentMedia.name}\" will be securely encrypted in your isolated Hidden Vault and removed from your normal gallery.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMoveToHiddenDialog = false
                        viewModel.movePrivateMediaToHiddenVault(currentMedia) { success ->
                            if (success && mediaList.size <= 1) {
                                onBack()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.HiddenPrimary)
                ) {
                    Text("Move to Hidden Vault", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMoveToHiddenDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
