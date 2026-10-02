package com.example.ui.screens.hiddenhiddenvault

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.HiddenHiddenItem
import com.example.data.security.VaultType
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultDarkBg
import com.example.ui.theme.VaultRed
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.VaultViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HiddenHiddenVaultHomeScreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val allItems by viewModel.allHiddenHiddenItems.collectAsStateWithLifecycle()
    val photos by viewModel.hiddenHiddenPhotos.collectAsStateWithLifecycle()
    val videos by viewModel.hiddenHiddenVideos.collectAsStateWithLifecycle()
    val photosCount by viewModel.hiddenHiddenPhotosCount.collectAsStateWithLifecycle()
    val videosCount by viewModel.hiddenHiddenVideosCount.collectAsStateWithLifecycle()
    val totalCount by viewModel.hiddenHiddenTotalCount.collectAsStateWithLifecycle()
    val totalSizeBytes by viewModel.hiddenHiddenTotalSizeBytes.collectAsStateWithLifecycle()

    val selectedIds by viewModel.selectedHiddenHiddenIds.collectAsStateWithLifecycle()
    val isSelectMode by viewModel.isHiddenHiddenSelectMode.collectAsStateWithLifecycle()
    val searchQuery by viewModel.hiddenHiddenSearchQuery.collectAsStateWithLifecycle()
    val bulkImportState by viewModel.bulkImportState.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: All, 1: Photos, 2: Videos
    var isSearchVisible by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    val authManager = viewModel.authManager
    var isHiddenHiddenBiometric by remember { mutableStateOf(authManager.isBiometricEnabledForHiddenHidden) }
    var showChangePatternScreen by remember { mutableStateOf(false) }
    var isExportingMultiple by remember { mutableStateOf(false) }
    var exportProgressText by remember { mutableStateOf("") }

    // Multi-photo picker (allows selecting 1, 10, 50, 100+ photos at once)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 100)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importHiddenHiddenPhotos(uris)
        }
    }

    // Multi-video picker
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 100)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importHiddenHiddenVideos(uris)
        }
    }

    // Multi-file picker (Photos and Videos simultaneously)
    val combinedMediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 100)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importHiddenHiddenFiles(uris)
        }
    }

    // Document/Storage Fallback Multi-Picker
    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importHiddenHiddenFiles(uris)
        }
    }

    BackHandler {
        if (isSelectMode) {
            viewModel.clearHiddenHiddenSelection()
        } else if (isSearchVisible) {
            isSearchVisible = false
            viewModel.setHiddenHiddenSearchQuery("")
        } else {
            viewModel.lockHiddenHiddenVault()
            onBack()
        }
    }

    // Filter items according to tab & search query
    val currentTabItems = remember(selectedTab, allItems, photos, videos) {
        when (selectedTab) {
            1 -> photos
            2 -> videos
            else -> allItems
        }
    }

    val displayedItems = remember(currentTabItems, searchQuery) {
        if (searchQuery.isBlank()) {
            currentTabItems
        } else {
            val q = searchQuery.trim().lowercase()
            currentTabItems.filter { it.name.lowercase().contains(q) }
        }
    }

    // Formatted storage size
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val mb = bytes / (1024.0 * 1024.0)
        val gb = mb / 1024.0
        return if (gb >= 1.0) "%.1f GB".format(gb) else "%.1f MB".format(mb)
    }

    // Multi-share helper
    fun shareSelectedItems() {
        val selectedItemsList = allItems.filter { selectedIds.contains(it.id) }
        if (selectedItemsList.isEmpty()) return

        scope.launch {
            val sharedUris = arrayListOf<Uri>()
            for (item in selectedItemsList) {
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
                    sharedUris.add(uri)
                }
            }

            if (sharedUris.isNotEmpty()) {
                val shareIntent = if (sharedUris.size == 1) {
                    Intent(Intent.ACTION_SEND).apply {
                        val isVid = selectedItemsList.first().itemType == "VIDEO"
                        type = if (isVid) "video/*" else "image/*"
                        putExtra(Intent.EXTRA_STREAM, sharedUris.first())
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                } else {
                    Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                        type = "*/*"
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, sharedUris)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
                try {
                    context.startActivity(Intent.createChooser(shareIntent, "Share Secure Files"))
                } catch (e: Exception) {
                    viewModel.showToast("No sharing app available")
                }
            } else {
                viewModel.showToast("Unable to prepare files for sharing")
            }
        }
    }

    if (showChangePatternScreen) {
        HiddenHiddenLockScreen(
            viewModel = viewModel,
            initialMode = PatternScreenMode.CHANGE_CURRENT,
            onBack = { showChangePatternScreen = false }
        )
        return
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultDarkBg)
            .testTag("hidden_hidden_vault_home_screen"),
        containerColor = VaultDarkBg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F0714))
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                viewModel.lockHiddenHiddenVault()
                                onBack()
                            },
                            modifier = Modifier.testTag("hh_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Hidden Vault",
                                tint = TextPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2E101E))
                                .border(1.dp, Color(0xFFF43F5E), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFF43F5E),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "HIDDEN HIDDEN VAULT",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Text(
                                text = "Ultra-Private • Pattern Protected",
                                fontSize = 11.sp,
                                color = Color(0xFFF43F5E)
                            )
                        }
                    }

                    // Header Action Buttons: Search, Settings, Lock
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                isSearchVisible = !isSearchVisible
                                if (!isSearchVisible) viewModel.setHiddenHiddenSearchQuery("")
                            },
                            modifier = Modifier.testTag("hh_search_button")
                        ) {
                            Icon(
                                imageVector = if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (isSearchVisible) Color(0xFFF43F5E) else TextPrimary
                            )
                        }

                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.testTag("hh_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TextSecondary
                            )
                        }

                        IconButton(
                            onClick = {
                                viewModel.lockHiddenHiddenVault()
                                onBack()
                            },
                            modifier = Modifier.testTag("hh_lock_vault_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock Vault",
                                tint = Color(0xFFFBBF24)
                            )
                        }
                    }
                }

                // Search Bar Input Field
                AnimatedVisibility(visible = isSearchVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setHiddenHiddenSearchQuery(it) },
                            placeholder = { Text("Search photos and videos by name...", color = TextMuted, fontSize = 13.sp) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setHiddenHiddenSearchQuery("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFF43F5E),
                                unfocusedBorderColor = Color(0xFF332030),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = Color(0xFF160A1A),
                                unfocusedContainerColor = Color(0xFF160A1A)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("hh_search_input")
                        )
                    }
                }

                // Category Tabs (All, Photos, Videos)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF0F0714),
                    contentColor = Color(0xFFF43F5E),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFFF43F5E),
                            height = 3.dp
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = "All ($totalCount)",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) TextPrimary else TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "Photos ($photosCount)",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) TextPrimary else TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                text = "Videos ($videosCount)",
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 2) TextPrimary else TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Action Buttons Bar (+ Add Photos, + Add Videos, + Add Files, Select)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Upload options
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp).testTag("hh_add_photos_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("+ Photos", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp).testTag("hh_add_videos_button")
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("+ Videos", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            combinedMediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF43F5E)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp).testTag("hh_add_files_button")
                    ) {
                        Text("+ Files", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Select Mode Toggle Button
                if (allItems.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            viewModel.setHiddenHiddenSelectMode(!isSelectMode)
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelectMode) Color(0xFF2A101C) else Color.Transparent,
                            contentColor = if (isSelectMode) Color(0xFFF43F5E) else TextSecondary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp).testTag("hh_select_mode_button")
                    ) {
                        Text(if (isSelectMode) "Done" else "Select", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Multi-Select Banner & Action Bar
            AnimatedVisibility(visible = isSelectMode) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF200C16),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF43F5E).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${selectedIds.size} selected",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    if (selectedIds.size == displayedItems.size) {
                                        viewModel.clearHiddenHiddenSelection()
                                    } else {
                                        viewModel.selectAllHiddenHiddenItems(displayedItems.map { it.id })
                                    }
                                }
                            ) {
                                Text(
                                    text = if (selectedIds.size == displayedItems.size) "Deselect All" else "Select All",
                                    fontSize = 12.sp,
                                    color = Color(0xFFF43F5E)
                                )
                            }

                            // Share Selected
                            IconButton(
                                onClick = { shareSelectedItems() },
                                enabled = selectedIds.isNotEmpty()
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Share Selected",
                                    tint = if (selectedIds.isNotEmpty()) TextPrimary else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Download / Export Selected
                            IconButton(
                                onClick = {
                                    isExportingMultiple = true
                                    viewModel.downloadSelectedHiddenHiddenItems(
                                        onProgress = { cur, total ->
                                            exportProgressText = "Exporting $cur of $total..."
                                        },
                                        onComplete = { success, fail ->
                                            isExportingMultiple = false
                                            viewModel.showToast("Exported $success files to Downloads" + if (fail > 0) " ($fail failed)" else "")
                                        }
                                    )
                                },
                                enabled = selectedIds.isNotEmpty()
                            ) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = "Export Selected",
                                    tint = if (selectedIds.isNotEmpty()) TextPrimary else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Delete Selected
                            IconButton(
                                onClick = { showDeleteConfirmDialog = true },
                                enabled = selectedIds.isNotEmpty(),
                                modifier = Modifier.testTag("hh_batch_delete_button")
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Selected",
                                    tint = if (selectedIds.isNotEmpty()) VaultRed else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Exporting progress indicator
            if (isExportingMultiple) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Column {
                        Text(text = exportProgressText, color = Color(0xFFF43F5E), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFFF43F5E))
                    }
                }
            }

            // Bulk import progress indicator
            if (bulkImportState.isImporting) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1B0F19),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF43F5E).copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Securing ${bulkImportState.totalCount} files into Hidden Hidden Vault...",
                                fontSize = 12.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${(bulkImportState.currentProgressRatio * 100).toInt()}%",
                                fontSize = 12.sp,
                                color = Color(0xFFF43F5E),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { bulkImportState.currentProgressRatio },
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFF43F5E),
                            trackColor = Color(0xFF331525)
                        )
                    }
                }
            }

            // Vault Content Grid or Professional Empty State
            if (displayedItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF240E1B))
                                .border(1.5.dp, Color(0xFFF43F5E).copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFFF43F5E),
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = if (searchQuery.isNotEmpty()) "No files match \"$searchQuery\"" else "Your Hidden Hidden Vault is Empty",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (searchQuery.isNotEmpty()) "Try a different search term" else "Keep your private photos and videos securely here.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                combinedMediaPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp).testTag("hh_empty_state_add_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+ Add Photos & Videos",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                // Responsive 3-column media grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayedItems, key = { it.id }) { item ->
                        HiddenHiddenMediaCard(
                            item = item,
                            isSelected = selectedIds.contains(item.id),
                            isSelectMode = isSelectMode,
                            viewModel = viewModel,
                            onClick = {
                                if (isSelectMode) {
                                    viewModel.toggleHiddenHiddenSelection(item.id)
                                } else {
                                    viewModel.setSelectedHiddenHiddenItem(item, displayedItems)
                                    viewModel.navigateTo(Screen.HIDDEN_HIDDEN_MEDIA_VIEWER)
                                }
                            },
                            onLongClick = {
                                if (!isSelectMode) {
                                    viewModel.setHiddenHiddenSelectMode(true)
                                }
                                viewModel.toggleHiddenHiddenSelection(item.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Multi-Delete
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Delete ${selectedIds.size} selected item${if (selectedIds.size > 1) "s" else ""}?",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "These files will be permanently deleted from the Hidden Hidden Vault. This action cannot be undone.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteSelectedHiddenHiddenItems()
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

    // Settings Dialog
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFFF43F5E), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hidden Hidden Vault Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Total files: $totalCount • Storage: ${formatBytes(totalSizeBytes)}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showSettingsDialog = false
                                showChangePatternScreen = true
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF2A101C)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFF43F5E), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Change Pattern", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }
                    }

                    if (authManager.canAuthenticateWithBiometrics()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF2A101C)
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
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = Color(0xFFF43F5E),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Fingerprint Unlock",
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Unlock this vault with fingerprint",
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Switch(
                                    checked = isHiddenHiddenBiometric,
                                    onCheckedChange = {
                                        isHiddenHiddenBiometric = it
                                        authManager.isBiometricEnabledForHiddenHidden = it
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFFF43F5E)
                                    )
                                )
                            }
                        }
                    }

                    Text(
                        text = "Encrypted with isolated Keystore AES-GCM key. Protected by an independent security pattern separate from your Main and Hidden Vault PINs.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 15.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Close", color = Color(0xFFF43F5E))
                }
            },
            containerColor = Color(0xFF1B0C15),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HiddenHiddenMediaCard(
    item: HiddenHiddenItem,
    isSelected: Boolean,
    isSelectMode: Boolean,
    viewModel: VaultViewModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    var thumbnailBitmap by remember(item.id) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isLoading by remember(item.id) { mutableStateOf(true) }
    val isVideo = item.itemType == "VIDEO" || item.encryptedPath.endsWith("enc_vid")

    LaunchedEffect(item.encryptedPath) {
        isLoading = true
        thumbnailBitmap = viewModel.getThumbnail(item.encryptedPath, VaultType.HIDDEN_HIDDEN)
        isLoading = false
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("hh_item_${item.id}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0E18)),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(3.dp, Color(0xFFF43F5E)) else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF331526))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (thumbnailBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = thumbnailBitmap!!.asImageBitmap(),
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xFF160A14)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFFF43F5E),
                        strokeWidth = 2.dp
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF281120), Color(0xFF160A14))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isVideo) Icons.Default.Videocam else Icons.Outlined.Photo,
                        contentDescription = null,
                        tint = Color(0xFFF43F5E).copy(alpha = 0.7f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Highlight overlay when selected
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF43F5E).copy(alpha = 0.28f))
                )
            }

            // Dark gradient overlay at bottom for name and badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )

            // Video Play Badge
            if (isVideo) {
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .align(Alignment.BottomStart)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("VIDEO", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Filename label
            Text(
                text = item.name,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .width(if (isVideo) 60.dp else 100.dp),
                textAlign = TextAlign.End
            )

            // Selection Checkbox
            if (isSelectMode) {
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color(0xFFF43F5E) else Color.Black.copy(alpha = 0.5f))
                        .border(1.5.dp, if (isSelected) Color(0xFFF43F5E) else Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
