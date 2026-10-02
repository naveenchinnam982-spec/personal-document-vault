package com.example.ui.screens.hiddenvault

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyColumn
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.data.security.VaultType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HiddenAlbum
import com.example.data.model.HiddenItem
import com.example.ui.theme.HiddenBorder
import com.example.ui.theme.HiddenGold
import com.example.ui.theme.HiddenPrimary
import com.example.ui.theme.HiddenPrimaryContainer
import com.example.ui.theme.HiddenPrimaryLight
import com.example.ui.theme.HiddenVaultDarkBg
import com.example.ui.theme.HiddenVaultSurface
import com.example.ui.theme.HiddenVaultSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultRed
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.VaultViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HiddenVaultHomeScreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Photos", "Videos", "Documents", "Albums", "Favorites", "Recent")

    val hiddenPhotos by viewModel.hiddenPhotos.collectAsState()
    val hiddenVideos by viewModel.hiddenVideos.collectAsState()
    val hiddenDocs by viewModel.hiddenDocuments.collectAsState()
    val hiddenAlbums by viewModel.allHiddenAlbums.collectAsState()
    val favoriteItems by viewModel.favoriteHiddenItems.collectAsState()
    val recentItems by viewModel.recentlyOpenedHiddenItems.collectAsState()

    val photosCount by viewModel.hiddenPhotosCount.collectAsState()
    val videosCount by viewModel.hiddenVideosCount.collectAsState()
    val docsCount by viewModel.hiddenDocsCount.collectAsState()
    val albumsCount by viewModel.hiddenAlbumsCount.collectAsState()

    val selectedHiddenIds by viewModel.selectedHiddenIds.collectAsState()
    val isMultiSelect by viewModel.isMultiSelectMode.collectAsState()
    val selectedHiddenAlbumId by viewModel.selectedHiddenAlbumId.collectAsState()

    // Direct Gallery Launcher for Photos & Videos (allows direct selection from device gallery)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val itemType = if (selectedTab == 1) "VIDEO" else "PHOTO"
            viewModel.importMultipleHiddenItems(uris, itemType, selectedHiddenAlbumId, isMoveOperation = true)
        }
    }

    // Direct Document / File Picker Launcher
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importMultipleHiddenItems(uris, "DOCUMENT", selectedHiddenAlbumId)
            viewModel.showToast("${uris.size} document(s) imported")
        }
    }

    fun openGalleryUpload() {
        if (selectedTab == 1) {
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
        } else {
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        }
    }

    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var newAlbumName by remember { mutableStateOf("") }
    var showMoveAlbumDialog by remember { mutableStateOf(false) }
    var selectedMoveAlbumId by remember { mutableStateOf<Long?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HiddenVaultDarkBg)
            .testTag("hidden_vault_home_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // High Security Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(HiddenPrimaryContainer)
                            .border(1.dp, HiddenPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = HiddenPrimaryLight,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "HIDDEN VAULT",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Private & Protected • Isolated Keystore",
                            fontSize = 11.sp,
                            color = HiddenPrimaryLight
                        )
                    }
                }

                // Header actions: Search, Trash, Lock
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.HIDDEN_SEARCH) },
                        modifier = Modifier.testTag("hidden_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Hidden Vault",
                            tint = TextPrimary
                        )
                    }

                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.HIDDEN_TRASH) },
                        modifier = Modifier.testTag("hidden_trash_button")
                    ) {
                        val trashCount by viewModel.hiddenTrashCount.collectAsState()
                        BadgedBox(
                            badge = {
                                if (trashCount > 0) {
                                    Badge(containerColor = VaultRed) {
                                        Text(text = "$trashCount", color = Color.White)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hidden Trash",
                                tint = TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            viewModel.lockHiddenVault()
                            onBack()
                        },
                        modifier = Modifier.testTag("hidden_lock_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Hidden Vault",
                            tint = HiddenGold
                        )
                    }
                }
            }

            // 🔐 Hidden Hidden Vault Option at the top of the page
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clickable {
                        // User must NOT be taken directly into vault. Navigates to Lock Screen!
                        viewModel.openHiddenHiddenVault()
                    }
                    .testTag("hidden_hidden_vault_button"),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF200C1B)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFFF43F5E), Color(0xFFE11D48), Color(0xFFF59E0B))
                    )
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF381228))
                                .border(1.5.dp, Color(0xFFF43F5E), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🔐", fontSize = 22.sp)
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Hidden Hidden Vault",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFF43F5E))
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "NEW",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Ultra-Private Vault • Pattern Lock Protected",
                                fontSize = 11.sp,
                                color = Color(0xFFFF859B)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Pattern Lock Screen Required",
                        tint = Color(0xFFF43F5E),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Statistics Header Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Photos",
                    count = photosCount,
                    accentColor = HiddenPrimary,
                    onClick = { selectedTab = 0 }
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Videos",
                    count = videosCount,
                    accentColor = Color(0xFFF43F5E),
                    onClick = { selectedTab = 1 }
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Docs",
                    count = docsCount,
                    accentColor = Color(0xFF06B6D4),
                    onClick = { selectedTab = 2 }
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Albums",
                    count = albumsCount,
                    accentColor = HiddenGold,
                    onClick = { selectedTab = 3 }
                )
            }

            // Impressive Quick Upload Banner (Directly opens device gallery or files)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .testTag("hidden_vault_upload_banner"),
                colors = CardDefaults.cardColors(containerColor = HiddenPrimaryContainer.copy(alpha = 0.5f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, HiddenPrimary.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(14.dp)
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(HiddenPrimary)
                                .clickable { openGalleryUpload() }
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Upload to Hidden Vault",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Tap to import directly from Gallery",
                                fontSize = 11.sp,
                                color = HiddenPrimaryLight
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { openGalleryUpload() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HiddenPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("hidden_open_gallery_button")
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gallery", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { documentPickerLauncher.launch("*/*") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HiddenVaultSurface,
                                contentColor = HiddenPrimaryLight
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, HiddenPrimary.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("hidden_open_files_button")
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Files", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Multi-Select Action Bar (If Active)
            if (isMultiSelect) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HiddenVaultSurfaceElevated)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${selectedHiddenIds.size} Selected",
                        color = HiddenPrimaryLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { showMoveAlbumDialog = true }) {
                            Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = "Move to Album", tint = TextPrimary)
                        }
                        IconButton(onClick = {
                            val allItems = when (selectedTab) {
                                0 -> hiddenPhotos
                                1 -> hiddenVideos
                                2 -> hiddenDocs
                                else -> favoriteItems
                            }
                            viewModel.batchFavoriteHiddenSelection(allItems, true)
                        }) {
                            Icon(Icons.Default.Star, contentDescription = "Favorite Selected", tint = HiddenGold)
                        }
                        IconButton(onClick = {
                            val allItems = when (selectedTab) {
                                0 -> hiddenPhotos
                                1 -> hiddenVideos
                                2 -> hiddenDocs
                                else -> favoriteItems
                            }
                            viewModel.batchDeleteHiddenSelection(allItems)
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Move to Trash", tint = VaultRed)
                        }
                        TextButton(onClick = { viewModel.clearHiddenSelection() }) {
                            Text("Clear", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Navigation Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = HiddenVaultDarkBg,
                contentColor = HiddenPrimary,
                edgePadding = 20.dp,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                color = if (selectedTab == index) HiddenPrimaryLight else TextMuted,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tab Content
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                when (selectedTab) {
                    0 -> PhotosGrid(
                        items = hiddenPhotos,
                        selectedIds = selectedHiddenIds,
                        viewModel = viewModel,
                        onUploadClick = { openGalleryUpload() },
                        onItemClick = { item ->
                            if (isMultiSelect) {
                                viewModel.toggleHiddenSelection(item.id)
                            } else {
                                viewModel.selectHiddenItem(item, hiddenPhotos)
                            }
                        },
                        onItemLongClick = { item ->
                            viewModel.toggleHiddenSelection(item.id)
                        }
                    )
                    1 -> VideosGrid(
                        items = hiddenVideos,
                        selectedIds = selectedHiddenIds,
                        viewModel = viewModel,
                        onUploadClick = {
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        },
                        onItemClick = { item ->
                            if (isMultiSelect) {
                                viewModel.toggleHiddenSelection(item.id)
                            } else {
                                viewModel.selectHiddenItem(item, hiddenVideos)
                            }
                        },
                        onItemLongClick = { item ->
                            viewModel.toggleHiddenSelection(item.id)
                        }
                    )
                    2 -> DocumentsList(
                        items = hiddenDocs,
                        selectedIds = selectedHiddenIds,
                        onUploadClick = { documentPickerLauncher.launch("*/*") },
                        onItemClick = { item ->
                            if (isMultiSelect) {
                                viewModel.toggleHiddenSelection(item.id)
                            } else {
                                viewModel.selectHiddenItem(item, hiddenDocs)
                            }
                        },
                        onItemLongClick = { item ->
                            viewModel.toggleHiddenSelection(item.id)
                        }
                    )
                    3 -> AlbumsSection(
                        albums = hiddenAlbums,
                        onCreateAlbumClick = { showCreateAlbumDialog = true },
                        onAlbumClick = { album ->
                            viewModel.selectHiddenAlbum(album.id)
                        }
                    )
                    4 -> FavoritesSection(
                        items = favoriteItems,
                        onItemClick = { item -> viewModel.selectHiddenItem(item, favoriteItems) }
                    )
                    5 -> RecentSection(
                        items = recentItems,
                        onItemClick = { item -> viewModel.selectHiddenItem(item, recentItems) }
                    )
                }
            }
        }

        // Impressive Extended Floating Action Button for Direct Gallery Upload
        ExtendedFloatingActionButton(
            onClick = {
                if (selectedTab == 2) {
                    documentPickerLauncher.launch("*/*")
                } else {
                    openGalleryUpload()
                }
            },
            containerColor = HiddenPrimary,
            contentColor = Color.White,
            icon = {
                Icon(
                    imageVector = if (selectedTab == 2) Icons.Default.Description else Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            text = {
                Text(
                    text = if (selectedTab == 2) "Upload Docs" else "Upload from Gallery",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_hidden_item_fab")
        )
    }

    // Create Album Dialog
    if (showCreateAlbumDialog) {
        AlertDialog(
            onDismissRequest = { showCreateAlbumDialog = false },
            title = { Text("New Hidden Album", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Create a confidential album to organize secret files.", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newAlbumName,
                        onValueChange = { newAlbumName = it },
                        placeholder = { Text("e.g. Legal Trust, Crypto Wallets", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = HiddenPrimary,
                            unfocusedBorderColor = HiddenBorder
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newAlbumName.isNotBlank()) {
                            viewModel.createHiddenAlbum(newAlbumName.trim())
                            newAlbumName = ""
                            showCreateAlbumDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HiddenPrimary)
                ) {
                    Text("Create", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateAlbumDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = HiddenVaultDarkBg
        )
    }

    // Move to Album Dialog
    if (showMoveAlbumDialog) {
        AlertDialog(
            onDismissRequest = { showMoveAlbumDialog = false },
            title = { Text("Move to Hidden Album", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedMoveAlbumId == null,
                                onClick = { selectedMoveAlbumId = null },
                                colors = RadioButtonDefaults.colors(selectedColor = HiddenPrimary)
                            )
                            Text("Unorganized (No Album)", color = TextPrimary, fontSize = 14.sp)
                        }
                    }
                    items(hiddenAlbums) { album ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedMoveAlbumId == album.id,
                                onClick = { selectedMoveAlbumId = album.id },
                                colors = RadioButtonDefaults.colors(selectedColor = HiddenPrimary)
                            )
                            Text(album.name, color = TextPrimary, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.batchMoveHiddenSelection(selectedMoveAlbumId)
                        showMoveAlbumDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HiddenPrimary)
                ) {
                    Text("Move", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMoveAlbumDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = HiddenVaultDarkBg
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    count: Int,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = HiddenVaultSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 10.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotosGrid(
    items: List<HiddenItem>,
    selectedIds: Set<Long>,
    viewModel: VaultViewModel,
    onUploadClick: () -> Unit,
    onItemClick: (HiddenItem) -> Unit,
    onItemLongClick: (HiddenItem) -> Unit
) {
    if (items.isEmpty()) {
        EmptyHiddenState("No hidden photos yet", "Tap below to import photos directly from your gallery", "Open Gallery", onUploadClick)
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            items(items, key = { it.id }) { item ->
                val isSelected = selectedIds.contains(item.id)
                HiddenPhotoThumbnailItem(
                    item = item,
                    isSelected = isSelected,
                    viewModel = viewModel,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HiddenPhotoThumbnailItem(
    item: HiddenItem,
    isSelected: Boolean,
    viewModel: VaultViewModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var thumbnail by remember(item.id) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(item.id) { mutableStateOf(true) }

    LaunchedEffect(item.encryptedPath) {
        isLoading = true
        thumbnail = viewModel.getThumbnail(item.encryptedPath, VaultType.HIDDEN)
        isLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(HiddenVaultSurface)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) HiddenPrimary else HiddenBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail!!.asImageBitmap(),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = item.name,
                    fontSize = 10.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(HiddenVaultDarkBg),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = HiddenPrimary,
                    strokeWidth = 2.dp
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = HiddenPrimaryLight,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.name,
                    fontSize = 11.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (item.isFavorite) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = HiddenGold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(16.dp)
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(HiddenPrimary.copy(alpha = 0.28f)),
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(HiddenPrimary)
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideosGrid(
    items: List<HiddenItem>,
    selectedIds: Set<Long>,
    viewModel: VaultViewModel,
    onUploadClick: () -> Unit,
    onItemClick: (HiddenItem) -> Unit,
    onItemLongClick: (HiddenItem) -> Unit
) {
    if (items.isEmpty()) {
        EmptyHiddenState("No hidden videos yet", "Tap below to import videos directly from your gallery", "Open Gallery", onUploadClick)
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            items(items, key = { it.id }) { item ->
                val isSelected = selectedIds.contains(item.id)
                var thumbnail by remember(item.id) { mutableStateOf<Bitmap?>(null) }

                LaunchedEffect(item.encryptedPath) {
                    thumbnail = viewModel.getThumbnail(item.encryptedPath, VaultType.HIDDEN)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .combinedClickable(
                            onClick = { onItemClick(item) },
                            onLongClick = { onItemLongClick(item) }
                        ),
                    colors = CardDefaults.cardColors(containerColor = HiddenVaultSurface)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (thumbnail != null) {
                            Image(
                                bitmap = thumbnail!!.asImageBitmap(),
                                contentDescription = item.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = item.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(HiddenPrimary.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.TopEnd
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = HiddenPrimary,
                                    modifier = Modifier.padding(6.dp).size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocumentsList(
    items: List<HiddenItem>,
    selectedIds: Set<Long>,
    onUploadClick: () -> Unit,
    onItemClick: (HiddenItem) -> Unit,
    onItemLongClick: (HiddenItem) -> Unit
) {
    if (items.isEmpty()) {
        EmptyHiddenState("No hidden documents", "Tap below to store confidential PDFs and records", "Upload Documents", onUploadClick)
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            items(items, key = { it.id }) { item ->
                val isSelected = selectedIds.contains(item.id)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onItemClick(item) },
                            onLongClick = { onItemLongClick(item) }
                        ),
                    colors = CardDefaults.cardColors(containerColor = HiddenVaultSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(HiddenPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = HiddenPrimaryLight,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = item.category,
                                    fontSize = 11.sp,
                                    color = HiddenPrimaryLight
                                )
                                Text(text = "•", fontSize = 11.sp, color = TextMuted)
                                Text(
                                    text = item.itemType,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        if (item.isFavorite) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = HiddenGold, modifier = Modifier.size(18.dp))
                        }

                        if (isSelected) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = HiddenPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumsSection(
    albums: List<HiddenAlbum>,
    onCreateAlbumClick: () -> Unit,
    onAlbumClick: (HiddenAlbum) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Confidential Albums", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            TextButton(onClick = onCreateAlbumClick) {
                Text("+ New Album", color = HiddenPrimaryLight)
            }
        }

        if (albums.isEmpty()) {
            EmptyHiddenState("No albums created", "Organize files by personal trust, legal or finance")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(albums) { album ->
                    Card(
                        onClick = { onAlbumClick(album) },
                        colors = CardDefaults.cardColors(containerColor = HiddenVaultSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = HiddenPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = album.name,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = album.category,
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

@Composable
fun FavoritesSection(
    items: List<HiddenItem>,
    onItemClick: (HiddenItem) -> Unit
) {
    if (items.isEmpty()) {
        EmptyHiddenState("No favorites yet", "Star important hidden files to access them quickly")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items) { item ->
                Card(
                    onClick = { onItemClick(item) },
                    colors = CardDefaults.cardColors(containerColor = HiddenVaultSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = HiddenGold, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(text = item.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "${item.category} • ${item.itemType}", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentSection(
    items: List<HiddenItem>,
    onItemClick: (HiddenItem) -> Unit
) {
    if (items.isEmpty()) {
        EmptyHiddenState("No recent activity", "Items you open inside Hidden Vault will appear here")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items) { item ->
                Card(
                    onClick = { onItemClick(item) },
                    colors = CardDefaults.cardColors(containerColor = HiddenVaultSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = HiddenPrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(text = item.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "Opened recently • ${item.category}", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyHiddenState(title: String, subtitle: String, buttonText: String = "Open Gallery", onUploadClick: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(HiddenPrimaryContainer)
                .border(1.dp, HiddenPrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = HiddenPrimaryLight, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(text = title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(text = subtitle, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))

        if (onUploadClick != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onUploadClick,
                colors = ButtonDefaults.buttonColors(containerColor = HiddenPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (buttonText.contains("Doc", ignoreCase = true)) Icons.Default.Description else Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(buttonText, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
