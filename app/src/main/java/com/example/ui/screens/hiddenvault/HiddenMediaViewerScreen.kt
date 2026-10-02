package com.example.ui.screens.hiddenvault

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.ui.theme.HiddenBorder
import com.example.ui.theme.HiddenGold
import com.example.ui.theme.HiddenPrimary
import com.example.ui.theme.HiddenPrimaryContainer
import com.example.ui.theme.HiddenVaultDarkBg
import com.example.ui.theme.HiddenVaultSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultRed
import com.example.ui.viewmodel.VaultViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HiddenMediaViewerScreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val rawHiddenList by viewModel.viewerHiddenItemList.collectAsStateWithLifecycle()
    val allHiddenItems by viewModel.allHiddenItems.collectAsStateWithLifecycle()
    val hiddenList = if (rawHiddenList.isNotEmpty()) rawHiddenList else allHiddenItems
    val selectedItem by viewModel.selectedHiddenItem.collectAsStateWithLifecycle()

    var isSlideshowActive by remember { mutableStateOf(false) }
    var showExportWarningDialog by remember { mutableStateOf(false) }
    var showReAuthPinDialog by remember { mutableStateOf(false) }
    var enteredReAuthPin by remember { mutableStateOf("") }
    var reAuthError by remember { mutableStateOf<String?>(null) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }

    var areControlsVisible by remember { mutableStateOf(true) }
    var isCurrentPageZoomed by remember { mutableStateOf(false) }

    BackHandler {
        onBack()
    }

    if (hiddenList.isEmpty() || selectedItem == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(HiddenVaultDarkBg),
            contentAlignment = Alignment.Center
        ) {
            Text("No item selected", color = TextSecondary)
        }
        LaunchedEffect(Unit) { onBack() }
        return
    }

    // Preserve exact selected position
    val initialPage = remember {
        val targetId = selectedItem?.id ?: 0L
        val found = hiddenList.indexOfFirst { it.id == targetId }
        if (found >= 0) found else 0
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, (hiddenList.size - 1).coerceAtLeast(0)),
        pageCount = { hiddenList.size }
    )

    // Handle item deletion or list updates safely
    LaunchedEffect(hiddenList.size) {
        if (hiddenList.isEmpty()) {
            onBack()
        } else if (pagerState.currentPage >= hiddenList.size) {
            pagerState.scrollToPage(hiddenList.size - 1)
        }
    }

    // Reset zoom state on page change and sync selected item
    LaunchedEffect(pagerState.currentPage) {
        isCurrentPageZoomed = false
        val pageItem = hiddenList.getOrNull(pagerState.currentPage)
        if (pageItem != null && pageItem.id != selectedItem?.id) {
            viewModel.updateSelectedHiddenItemWithoutNavigating(pageItem)
        }
    }

    // Slideshow auto advance loop across pages
    LaunchedEffect(isSlideshowActive) {
        while (isSlideshowActive && hiddenList.isNotEmpty()) {
            delay(3000L)
            val nextPage = (pagerState.currentPage + 1) % hiddenList.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    val currentItem = hiddenList.getOrNull(pagerState.currentPage) ?: selectedItem!!
    val isCurrentItemVideo = currentItem.itemType == "VIDEO" ||
            currentItem.encryptedPath.endsWith("enc_vid") ||
            currentItem.encryptedPath.contains("vid", ignoreCase = true) ||
            currentItem.name.contains("Video", ignoreCase = true) ||
            currentItem.name.lowercase().let {
                it.endsWith(".mp4") || it.endsWith(".mov") || it.endsWith(".mkv") || it.endsWith(".webm") ||
                        it.endsWith(".3gp") || it.endsWith(".m4v") || it.endsWith(".avi") || it.endsWith(".flv")
            }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("hidden_media_viewer_screen")
    ) {
        // Horizontal Pager for smooth side-swipe
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !isCurrentPageZoomed,
            key = { hiddenList[it].id },
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val pageItem = hiddenList[page]
            val isPageVideo = pageItem.itemType == "VIDEO" ||
                    pageItem.encryptedPath.endsWith("enc_vid") ||
                    pageItem.encryptedPath.contains("vid", ignoreCase = true) ||
                    pageItem.name.contains("Video", ignoreCase = true) ||
                    pageItem.name.lowercase().let {
                        it.endsWith(".mp4") || it.endsWith(".mov") || it.endsWith(".mkv") || it.endsWith(".webm") ||
                                it.endsWith(".3gp") || it.endsWith(".m4v") || it.endsWith(".avi") || it.endsWith(".flv")
                    }

            if (isPageVideo) {
                VideoPlayerComponent(
                    encryptedPath = pageItem.encryptedPath,
                    vaultType = VaultType.HIDDEN,
                    videoTitle = pageItem.name,
                    onBack = onBack,
                    accentColor = HiddenPrimary,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ZoomablePhotoComponent(
                    filePath = pageItem.encryptedPath,
                    vaultType = VaultType.HIDDEN,
                    contentDescription = pageItem.name,
                    accentColor = HiddenPrimary,
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
                    .testTag("hidden_viewer_prev_button")
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
        if (areControlsVisible && !isCurrentItemVideo && pagerState.currentPage < hiddenList.size - 1) {
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
                    .testTag("hidden_viewer_next_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Item",
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
                color = Color.Black.copy(alpha = 0.72f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
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
                                .testTag("hidden_viewer_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = currentItem.name,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${pagerState.currentPage + 1} / ${hiddenList.size} • Isolated Keystore",
                                color = HiddenPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row {
                        // Favorite
                        IconButton(
                            onClick = { viewModel.toggleHiddenFavorite(currentItem) },
                            modifier = Modifier.testTag("hidden_fav_button")
                        ) {
                            Icon(
                                imageVector = if (currentItem.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (currentItem.isFavorite) HiddenGold else TextSecondary
                            )
                        }

                        // Info
                        IconButton(onClick = { showInfoSheet = true }) {
                            Icon(Icons.Default.Info, contentDescription = "Info", tint = TextSecondary)
                        }
                    }
                }
            }
        }

        // Bottom Action Bar: Slideshow, Share, Download, Delete
        AnimatedVisibility(
            visible = areControlsVisible && !isCurrentItemVideo,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.85f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Slideshow toggle
                    IconButton(onClick = { isSlideshowActive = !isSlideshowActive }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Slideshow,
                                contentDescription = "Slideshow",
                                tint = if (isSlideshowActive) HiddenPrimary else TextSecondary
                            )
                            Text(
                                text = if (isSlideshowActive) "Playing" else "Slideshow",
                                color = if (isSlideshowActive) HiddenPrimary else TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Direct Download to device
                    IconButton(
                        onClick = {
                            isDownloading = true
                            viewModel.downloadHiddenItem(currentItem) { success, msg, _ ->
                                isDownloading = false
                                viewModel.showToast(msg)
                            }
                        },
                        modifier = Modifier.testTag("hidden_download_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (isDownloading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = HiddenPrimary, strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download",
                                    tint = TextPrimary
                                )
                            }
                            Text("Download", color = TextMuted, fontSize = 10.sp)
                        }
                    }

                    // Export / Share (with auth gate)
                    IconButton(
                        onClick = { showExportWarningDialog = true },
                        modifier = Modifier.testTag("hidden_share_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Protected File",
                                tint = TextPrimary
                            )
                            Text("Share", color = TextMuted, fontSize = 10.sp)
                        }
                    }

                    // Delete to Hidden Trash
                    IconButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.testTag("hidden_delete_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Move to Hidden Trash",
                                tint = VaultRed
                            )
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
            title = { Text("Move to Hidden Trash?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Item \"${currentItem.name}\" will be moved to Hidden Trash. You can restore it later.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.moveHiddenItemToTrash(currentItem, fromViewer = true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultRed, contentColor = Color.White)
                ) {
                    Text("Move to Trash")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = HiddenVaultDarkBg
        )
    }

    // Step 1: Export Warning Dialog
    if (showExportWarningDialog) {
        AlertDialog(
            onDismissRequest = { showExportWarningDialog = false },
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null, tint = VaultAmber, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("Export Outside Hidden Vault?", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Warning: Exporting or sharing this confidential file will create an unencrypted copy outside the Hidden Vault sandbox. The file will no longer be protected by the isolated Keystore encryption key.\n\nRe-authentication is strictly required to proceed.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportWarningDialog = false
                        showReAuthPinDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HiddenPrimary)
                ) {
                    Text("Authenticate & Proceed", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportWarningDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = HiddenVaultDarkBg
        )
    }

    // Step 2: Re-Authentication PIN Dialog
    if (showReAuthPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showReAuthPinDialog = false
                enteredReAuthPin = ""
                reAuthError = null
            },
            title = {
                Text("Confirm Hidden Vault PIN", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Please enter your 4-digit Hidden Vault PIN to decrypt and export this confidential item.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = enteredReAuthPin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                enteredReAuthPin = it
                                reAuthError = null
                            }
                        },
                        label = { Text("Enter PIN") },
                        isError = reAuthError != null,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HiddenPrimary,
                            unfocusedBorderColor = HiddenBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("reauth_pin_input")
                    )
                    if (reAuthError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(reAuthError!!, color = VaultRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (viewModel.verifyHiddenPin(enteredReAuthPin)) {
                            showReAuthPinDialog = false
                            enteredReAuthPin = ""
                            reAuthError = null

                            // Launch export sharing intent
                            scope.launch {
                                val isVideo = currentItem.itemType == "VIDEO" || currentItem.encryptedPath.endsWith("enc_vid")
                                val ext = if (isVideo) ".mp4" else ".png"
                                val safeExportName = "${currentItem.name.replace(" ", "_")}$ext"
                                val tempFile = viewModel.prepareDecryptedFileForSharing(
                                    filePath = currentItem.encryptedPath,
                                    exportName = safeExportName,
                                    vaultType = VaultType.HIDDEN
                                )
                                if (tempFile != null) {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        tempFile
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = if (isVideo) "video/*" else "image/*"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    try {
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Confidential File"))
                                    } catch (e: Exception) {
                                        viewModel.showToast("No sharing app available")
                                    }
                                } else {
                                    viewModel.showToast("Could not export encrypted item")
                                }
                            }
                        } else {
                            reAuthError = "Incorrect PIN. Action blocked."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HiddenPrimary)
                ) {
                    Text("Verify & Share", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showReAuthPinDialog = false
                    enteredReAuthPin = ""
                    reAuthError = null
                }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = HiddenVaultDarkBg
        )
    }

    // Modal Bottom Sheet: Item Info
    if (showInfoSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showInfoSheet = false },
            sheetState = sheetState,
            containerColor = HiddenVaultSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(HiddenPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = HiddenPrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Confidential Item Info", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                        Text(currentItem.name, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = HiddenVaultDarkBg),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        InfoRow(label = "Category", value = currentItem.category)
                        InfoRow(label = "Item Type", value = currentItem.itemType)
                        InfoRow(label = "File Size", value = "${currentItem.fileSizeBytes / 1024} KB")
                        InfoRow(label = "Date Added", value = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(currentItem.createdAt)))
                        InfoRow(label = "Encrypted Sandbox", value = currentItem.encryptedPath)
                        InfoRow(label = "Security Mode", value = "Isolated Keystore AES-256-GCM")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Text(value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
