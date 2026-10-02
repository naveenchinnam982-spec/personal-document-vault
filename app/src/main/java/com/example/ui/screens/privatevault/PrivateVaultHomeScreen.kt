package com.example.ui.screens.privatevault

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Album
import com.example.data.model.PrivateMedia
import com.example.ui.screens.documents.outlinedFieldColors
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultDarkBg
import com.example.ui.theme.VaultRed
import com.example.ui.theme.VaultRedContainer
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.VaultSurfaceElevated
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.VaultViewModel
import java.io.ByteArrayOutputStream

@Composable
fun PrivateVaultHomeScreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allMedia by viewModel.allPrivateMedia.collectAsStateWithLifecycle()
    val favoriteMedia by viewModel.favoriteMedia.collectAsStateWithLifecycle()
    val albums by viewModel.allAlbums.collectAsStateWithLifecycle()
    val selectedAlbumId by viewModel.selectedAlbumId.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: All, 1: Albums, 2: Favorites
    var showAddMenu by remember { mutableStateOf(false) }
    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var newAlbumName by remember { mutableStateOf("") }

    // Multi-Selection State for Bulk Operations
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    var showBatchMoveToHiddenConfirm by remember { mutableStateOf(false) }
    var isSearchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val currentTheme by viewModel.appTheme.collectAsStateWithLifecycle()

    // Pick visual photos/videos from phone (single or multi)
    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val hasVideo = uris.any { uri ->
                val mime = try { context.contentResolver.getType(uri)?.lowercase() } catch (_: Exception) { null }
                mime?.startsWith("video/") == true
            }
            if (hasVideo) {
                viewModel.importMultipleVideos(uris, selectedAlbumId)
            } else {
                viewModel.importMultiplePhotos(uris, selectedAlbumId)
            }
        }
    }

    // Direct Gallery video picker
    val galleryVideoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importMultipleVideos(uris, selectedAlbumId)
        }
    }

    // Pick videos from phone files (single or multi)
    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.size == 1) {
            viewModel.addPrivateMedia(
                name = "Private Video ${System.currentTimeMillis() % 10000}.mp4",
                mediaType = "VIDEO",
                albumId = selectedAlbumId,
                sourceUri = uris.first()
            )
        } else if (uris.size > 1) {
            viewModel.importMultipleVideos(uris, selectedAlbumId)
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            viewModel.addPrivateMedia(
                name = "Private Capture",
                mediaType = "PHOTO",
                albumId = selectedAlbumId,
                rawBytes = stream.toByteArray()
            )
        }
    }

    val baseMedia = when (selectedTabIndex) {
        0 -> if (selectedAlbumId == null) allMedia else allMedia.filter { it.albumId == selectedAlbumId }
        1 -> allMedia
        else -> favoriteMedia
    }
    val displayedMedia = remember(baseMedia, searchQuery) {
        if (searchQuery.isBlank()) {
            baseMedia
        } else {
            val q = searchQuery.trim().lowercase()
            baseMedia.filter { it.name.lowercase().contains(q) }
        }
    }

    val shareSelectedMedia: () -> Unit = {
        val itemsToShare = displayedMedia.filter { it.id in selectedIds }
        if (itemsToShare.isNotEmpty()) {
            scope.launch {
                val uris = mutableListOf<Uri>()
                for (item in itemsToShare) {
                    val isVid = item.mediaType == "VIDEO" || item.encryptedPath.endsWith("enc_vid")
                    val ext = if (isVid) "mp4" else "png"
                    val exportName = "${item.name.replace(" ", "_")}.$ext"
                    val file = viewModel.prepareDecryptedFileForSharing(item.encryptedPath, exportName, isPrivate = true)
                    if (file != null) {
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        uris.add(uri)
                    }
                }
                if (uris.isNotEmpty()) {
                    val shareIntent = if (uris.size == 1) {
                        Intent(Intent.ACTION_SEND).apply {
                            type = "image/*"
                            putExtra(Intent.EXTRA_STREAM, uris.first())
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    } else {
                        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                            type = "image/*"
                            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    }
                    try {
                        context.startActivity(Intent.createChooser(shareIntent, "Share Photos"))
                    } catch (_: Exception) {
                        viewModel.showToast("No sharing application found")
                    }
                }
            }
        }
    }

    val downloadSelectedMedia: () -> Unit = {
        val itemsToDownload = displayedMedia.filter { it.id in selectedIds }
        if (itemsToDownload.isNotEmpty()) {
            scope.launch {
                for (item in itemsToDownload) {
                    viewModel.downloadPrivateMedia(item) { _, _, _ -> }
                }
                viewModel.showToast("Exporting ${itemsToDownload.size} photo(s) to Downloads")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(currentTheme.bgColor)
            .testTag("private_vault_home_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar / Multi-Select Action Bar
            if (isSelectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(currentTheme.surfaceElevatedColor)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                isSelectionMode = false
                                selectedIds = emptySet()
                            },
                            modifier = Modifier.testTag("close_selection_mode_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Selection",
                                tint = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${selectedIds.size} ${if (selectedIds.size == 1) "photo" else "photos"} selected",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = currentTheme.primaryColor
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val allSelected = displayedMedia.isNotEmpty() && selectedIds.size == displayedMedia.size
                        Button(
                            onClick = {
                                selectedIds = if (allSelected) emptySet() else displayedMedia.map { it.id }.toSet()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (allSelected) currentTheme.primaryColor.copy(alpha = 0.2f) else currentTheme.surfaceColor,
                                contentColor = currentTheme.primaryColor
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp).testTag("select_all_media_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (allSelected) "Deselect" else "Select All (${displayedMedia.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Share Selected Button
                        IconButton(
                            onClick = { shareSelectedMedia() },
                            enabled = selectedIds.isNotEmpty(),
                            modifier = Modifier.testTag("share_selected_media_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Selected",
                                tint = if (selectedIds.isNotEmpty()) currentTheme.primaryColor else TextMuted
                            )
                        }

                        // Download Selected Button
                        IconButton(
                            onClick = { downloadSelectedMedia() },
                            enabled = selectedIds.isNotEmpty(),
                            modifier = Modifier.testTag("download_selected_media_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download Selected",
                                tint = if (selectedIds.isNotEmpty()) currentTheme.primaryColor else TextMuted
                            )
                        }

                        // Move to Hidden Vault Button
                        IconButton(
                            onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    showBatchMoveToHiddenConfirm = true
                                }
                            },
                            enabled = selectedIds.isNotEmpty(),
                            modifier = Modifier.testTag("move_to_hidden_selected_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Move to Hidden Vault",
                                tint = if (selectedIds.isNotEmpty()) com.example.ui.theme.HiddenPrimary else TextMuted
                            )
                        }

                        // Delete Selected Button
                        IconButton(
                            onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    showBatchDeleteConfirm = true
                                }
                            },
                            enabled = selectedIds.isNotEmpty(),
                            modifier = Modifier.testTag("delete_selected_media_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Selected",
                                tint = if (selectedIds.isNotEmpty()) VaultRed else TextMuted
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("private_vault_back_button")) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Private Vault",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(VaultRedContainer)
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text("ISOLATED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = VaultRed)
                                    }
                            }
                            Text(
                                text = "${allMedia.size} private items",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Search Toggle Button
                        IconButton(
                            onClick = {
                                isSearchVisible = !isSearchVisible
                                if (!isSearchVisible) searchQuery = ""
                            },
                            modifier = Modifier.size(34.dp).testTag("toggle_photo_search_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (isSearchVisible) currentTheme.primaryColor else TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        if (displayedMedia.isNotEmpty()) {
                            Button(
                                onClick = { isSelectionMode = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = currentTheme.surfaceElevatedColor,
                                    contentColor = currentTheme.primaryColor
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp).testTag("enter_select_mode_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SelectAll,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Select", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        Button(
                            onClick = {
                                try {
                                    mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = currentTheme.surfaceElevatedColor,
                                contentColor = EmeraldLight
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp).testTag("bulk_import_photos_header_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Photos", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Lock Vault Immediately button
                        Button(
                            onClick = { viewModel.lockPrivateVault() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VaultRedContainer,
                                contentColor = VaultRed
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp).testTag("lock_private_vault_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Lock", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Search input field when search is active
            if (isSearchVisible && !isSelectionMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search photos & videos...", fontSize = 13.sp, color = TextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("photo_search_input")
                    )
                }
            }

            // Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = VaultSurface,
                contentColor = VaultRed,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = VaultRed
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Photos & Videos", fontSize = 12.sp) },
                    selectedContentColor = VaultRed,
                    unselectedContentColor = TextSecondary
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Albums (${albums.size})", fontSize = 12.sp) },
                    selectedContentColor = VaultRed,
                    unselectedContentColor = TextSecondary
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Favorites (${favoriteMedia.size})", fontSize = 12.sp) },
                    selectedContentColor = VaultRed,
                    unselectedContentColor = TextSecondary
                )
            }

            // Content body
            when (selectedTabIndex) {
                0, 2 -> {
                    // Album chip bar (if in tab 0)
                    if (selectedTabIndex == 0) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedAlbumId == null,
                                    onClick = { viewModel.selectAlbum(null) },
                                    label = { Text("All Media", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = VaultRed,
                                        selectedLabelColor = Color.White,
                                        containerColor = VaultSurfaceElevated,
                                        labelColor = TextSecondary
                                    )
                                )
                            }
                            items(albums) { album ->
                                FilterChip(
                                    selected = selectedAlbumId == album.id,
                                    onClick = { viewModel.selectAlbum(album.id) },
                                    label = { Text(album.name, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = VaultRed,
                                        selectedLabelColor = Color.White,
                                        containerColor = VaultSurfaceElevated,
                                        labelColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }

                    if (displayedMedia.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "🔒", fontSize = 42.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (selectedTabIndex == 2) "No favorite media" else "Private Vault Empty",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap + to import confidential photos or captures.",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 90.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(displayedMedia, key = { it.id }) { item ->
                                val isSelected = selectedIds.contains(item.id)
                                PrivateMediaThumbnail(
                                    media = item,
                                    viewModel = viewModel,
                                    isSelected = isSelected,
                                    isSelectionMode = isSelectionMode,
                                    onToggleSelect = {
                                        if (!isSelectionMode) isSelectionMode = true
                                        selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id
                                    },
                                    onClick = {
                                        if (isSelectionMode) {
                                            selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id
                                        } else {
                                            viewModel.selectPrivateMedia(item, displayedMedia)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Albums tab
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Vault Albums",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Button(
                                onClick = {
                                    newAlbumName = ""
                                    showCreateAlbumDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VaultRed,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp).testTag("create_album_button")
                            ) {
                                Text("+ New Album", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(albums) { album ->
                                val mediaInAlbumCount = allMedia.count { it.albumId == album.id }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(VaultSurfaceElevated)
                                        .border(1.dp, VaultBorder, RoundedCornerShape(14.dp))
                                        .clickable {
                                            viewModel.selectAlbum(album.id)
                                            selectedTabIndex = 0
                                        }
                                        .padding(14.dp)
                                        .testTag("album_card_${album.id}")
                                ) {
                                    Column {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(VaultRed.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = VaultRed,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = album.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "$mediaInAlbumCount items",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            FloatingActionButton(
                onClick = { showAddMenu = true },
                containerColor = VaultRed,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("private_vault_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add to Private Vault",
                    modifier = Modifier.size(26.dp)
                )
            }

            DropdownMenu(
                expanded = showAddMenu,
                onDismissRequest = { showAddMenu = false },
                modifier = Modifier.background(VaultSurfaceElevated)
            ) {
                DropdownMenuItem(
                    text = { Text("Import Photos from Gallery", color = TextPrimary) },
                    leadingIcon = {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = EmeraldPrimary)
                    },
                    onClick = {
                        showAddMenu = false
                        try {
                            mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        } catch (_: Exception) {}
                    }
                )
                DropdownMenuItem(
                    text = { Text("Import Videos from Gallery", color = TextPrimary) },
                    leadingIcon = {
                        Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = VaultAmber)
                    },
                    onClick = {
                        showAddMenu = false
                        try {
                            galleryVideoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        } catch (_: Exception) {}
                    }
                )
                DropdownMenuItem(
                    text = { Text("Browse Video Files", color = TextPrimary) },
                    leadingIcon = {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = CyanAccent)
                    },
                    onClick = {
                        showAddMenu = false
                        try {
                            videoPicker.launch(arrayOf("video/*"))
                        } catch (_: Exception) {}
                    }
                )
                DropdownMenuItem(
                    text = { Text("Take Secret Photo", color = TextPrimary) },
                    leadingIcon = {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = CyanAccent)
                    },
                    onClick = {
                        showAddMenu = false
                        try {
                            cameraLauncher.launch(null)
                        } catch (_: Exception) {}
                    }
                )
                DropdownMenuItem(
                    text = { Text("Create New Album", color = TextPrimary) },
                    leadingIcon = {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = EmeraldLight)
                    },
                    onClick = {
                        showAddMenu = false
                        newAlbumName = ""
                        showCreateAlbumDialog = true
                    }
                )
            }
        }

        // Create Album Dialog
        if (showCreateAlbumDialog) {
            AlertDialog(
                onDismissRequest = { showCreateAlbumDialog = false },
                containerColor = VaultSurfaceElevated,
                title = { Text("Create Album", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = newAlbumName,
                        onValueChange = { newAlbumName = it },
                        placeholder = { Text("Album name (e.g. Passports, Deeds)") },
                        singleLine = true,
                        colors = outlinedFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("new_album_input")
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newAlbumName.isNotBlank()) {
                                viewModel.createAlbum(newAlbumName.trim())
                                showCreateAlbumDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VaultRed, contentColor = Color.White)
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateAlbumDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        // Floating Bottom Action Bar for Multi-Selection
        if (isSelectionMode && selectedIds.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(currentTheme.surfaceElevatedColor)
                    .border(1.dp, currentTheme.borderColor, RoundedCornerShape(30.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showBatchDeleteConfirm = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VaultRedContainer,
                            contentColor = VaultRed
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("floating_delete_selected_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete (${selectedIds.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { showBatchMoveToHiddenConfirm = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = com.example.ui.theme.HiddenPrimaryContainer,
                            contentColor = com.example.ui.theme.HiddenPrimaryLight
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("floating_move_to_hidden_btn")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("To Hidden (${selectedIds.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { shareSelectedMedia() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = currentTheme.primaryColor.copy(alpha = 0.2f),
                            contentColor = currentTheme.primaryColor
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("floating_share_selected_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { downloadSelectedMedia() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = currentTheme.primaryColor.copy(alpha = 0.2f),
                            contentColor = currentTheme.primaryColor
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("floating_download_selected_btn")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    TextButton(
                        onClick = {
                            isSelectionMode = false
                            selectedIds = emptySet()
                        }
                    ) {
                        Text("Cancel", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        // Batch Delete Confirmation Dialog
        if (showBatchDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showBatchDeleteConfirm = false },
                containerColor = VaultSurfaceElevated,
                shape = RoundedCornerShape(22.dp),
                title = {
                    Text(
                        text = "Delete ${selectedIds.size} Image(s)?",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete ${selectedIds.size} selected image(s)? They will be safely moved to Trash and can be restored anytime.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.batchDeletePrivateMedia(selectedIds.toList())
                            selectedIds = emptySet()
                            isSelectionMode = false
                            showBatchDeleteConfirm = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VaultRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("confirm_batch_delete_btn")
                    ) {
                        Text("Delete ${selectedIds.size} Images", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBatchDeleteConfirm = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        // Batch Move to Hidden Vault Dialog
        if (showBatchMoveToHiddenConfirm) {
            AlertDialog(
                onDismissRequest = { showBatchMoveToHiddenConfirm = false },
                containerColor = VaultSurfaceElevated,
                shape = RoundedCornerShape(22.dp),
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
                        text = "Move ${selectedIds.size} item(s) to Hidden Vault?",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Selected photos/videos will be securely encrypted in your isolated Hidden Vault and removed from your normal gallery.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val itemsToMove = displayedMedia.filter { it.id in selectedIds }
                            viewModel.batchMovePrivateMediaToHiddenVault(itemsToMove)
                            selectedIds = emptySet()
                            isSelectionMode = false
                            showBatchMoveToHiddenConfirm = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = com.example.ui.theme.HiddenPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("confirm_batch_move_to_hidden_btn")
                    ) {
                        Text("Move & Make Private", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBatchMoveToHiddenConfirm = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PrivateMediaThumbnail(
    media: PrivateMedia,
    viewModel: VaultViewModel,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onClick: () -> Unit
) {
    var thumbnailBitmap by remember(media.id) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(media.id) { mutableStateOf(true) }
    val currentTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val isVideo = media.mediaType == "VIDEO" || media.encryptedPath.endsWith("enc_vid")

    LaunchedEffect(media.encryptedPath) {
        isLoading = true
        thumbnailBitmap = viewModel.getThumbnail(media.encryptedPath, com.example.data.security.VaultType.PRIVATE)
        isLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(VaultSurfaceElevated)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) currentTheme.primaryColor else VaultBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onToggleSelect
            )
            .testTag("private_media_${media.id}"),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnailBitmap != null) {
            Image(
                bitmap = thumbnailBitmap!!.asImageBitmap(),
                contentDescription = media.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(VaultSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = currentTheme.primaryColor,
                    strokeWidth = 2.dp
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(6.dp)
            ) {
                Icon(
                    imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Lock,
                    contentDescription = null,
                    tint = currentTheme.primaryColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = media.name,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        // Highlight overlay when selected
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(currentTheme.primaryColor.copy(alpha = 0.28f))
            )
        }

        // Video Badge
        if (isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Video",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        // Selection Checkmark Badge
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) currentTheme.primaryColor else Color.Black.copy(alpha = 0.55f)
                    )
                    .border(
                        1.5.dp,
                        if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        if (media.isFavorite && !isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Favorite",
                    tint = VaultAmber,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
