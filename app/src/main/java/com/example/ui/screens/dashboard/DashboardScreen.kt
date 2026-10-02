package com.example.ui.screens.dashboard

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.DocumentCard
import com.example.ui.components.StatCard
import com.example.ui.components.ThemeSelectorDialog
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.HiddenPrimary
import com.example.ui.theme.HiddenPrimaryContainer
import com.example.ui.theme.HiddenPrimaryLight
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultDarkBg
import com.example.ui.theme.VaultRed
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.VaultSurfaceElevated
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.VaultViewModel
import java.util.Calendar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val documents by viewModel.allDocuments.collectAsStateWithLifecycle()
    val totalDocs by viewModel.totalDocumentsCount.collectAsStateWithLifecycle()
    val photosCount by viewModel.photosCount.collectAsStateWithLifecycle()
    val videosCount by viewModel.videosCount.collectAsStateWithLifecycle()
    val favoriteDocs by viewModel.favoriteDocuments.collectAsStateWithLifecycle()
    val favoriteMedia by viewModel.favoriteMedia.collectAsStateWithLifecycle()
    val favoritesCount = favoriteDocs.size + favoriteMedia.size
    val currentTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val storageInfo by viewModel.storageInfo.collectAsStateWithLifecycle()
    val userName = viewModel.authManager.userName

    var showThemeDialog by remember { mutableStateOf(false) }
    val greeting = rememberGreeting()
    val isVaultCompletelyEmpty = totalDocs == 0 && photosCount == 0

    // Impressive 1-Tap Upload Launchers to Secure Vault
    val uploadPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importMultiplePhotos(uris, null)
            viewModel.showToast("${uris.size} photo(s) encrypting to Vault")
        }
    }

    val uploadFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importMultipleDocuments(uris, "General")
            viewModel.showToast("${uris.size} document(s) encrypting to Vault")
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(currentTheme.bgColor)
                .testTag("dashboard_screen"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Header Section: Greeting & Security Pill & Theme Switcher
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$greeting 👋",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        // Secret stealth trigger: Long press on security text opens Hidden Vault
                        Text(
                            text = "Your Vault is Secure 🔐",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = currentTheme.primaryColor,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .combinedClickable(
                                    onClick = { viewModel.showToast("Vault Protected with Hardware AES-256") },
                                    onLongClick = {
                                        viewModel.showToast("Stealth Vault Access")
                                        viewModel.navigateTo(Screen.HIDDEN_LOCK)
                                    }
                                )
                                .padding(vertical = 2.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Quick Theme Switcher Button
                        IconButton(
                            onClick = { showThemeDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(currentTheme.surfaceElevatedColor)
                                .border(1.dp, currentTheme.borderColor, CircleShape)
                                .testTag("theme_switcher_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Themes",
                                tint = currentTheme.primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Panic Lock Button (Long press also acts as stealth secret trigger)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(currentTheme.primaryColor.copy(alpha = 0.2f))
                                .border(1.dp, currentTheme.primaryColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                .combinedClickable(
                                    onClick = { viewModel.lockAll() },
                                    onLongClick = {
                                        viewModel.showToast("Stealth Vault Access")
                                        viewModel.navigateTo(Screen.HIDDEN_LOCK)
                                    }
                                )
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                .testTag("panic_lock_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(currentTheme.primaryColor)
                                )
                                Text(
                                    text = "Lock Vault",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = currentTheme.primaryColor
                                )
                            }
                        }
                    }
                }
            }

            // 2. 4 Stat Cards in 2x2 Grid (Documents, Photos, Videos, Storage Used)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Documents",
                            count = totalDocs,
                            icon = Icons.Default.Description,
                            accentColor = currentTheme.primaryColor,
                            onClick = { viewModel.navigateTo(Screen.DOCUMENTS) },
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Photos",
                            count = photosCount,
                            icon = Icons.Default.Image,
                            accentColor = currentTheme.accentColor,
                            onClick = { viewModel.navigateTo(Screen.PRIVATE_VAULT_HOME) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Videos",
                            count = videosCount,
                            icon = Icons.Default.Videocam,
                            accentColor = CyanAccent,
                            onClick = { viewModel.navigateTo(Screen.PRIVATE_VAULT_HOME) },
                            modifier = Modifier.weight(1f)
                        )
                        StorageStatCard(
                            title = "Storage Used",
                            value = storageInfo.formatBytes(storageInfo.totalVaultSizeBytes),
                            icon = Icons.Default.Storage,
                            accentColor = TealAccent,
                            onClick = { viewModel.navigateTo(Screen.STORAGE_MANAGER) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

        // 3. Impressive Clean Upload Hero Card (Uploads directly to Vault)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .testTag("upload_to_vault_card"),
                colors = CardDefaults.cardColors(containerColor = currentTheme.surfaceElevatedColor),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, currentTheme.primaryColor.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(currentTheme.primaryColor.copy(alpha = 0.15f))
                                .border(1.5.dp, currentTheme.primaryColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Upload",
                                tint = currentTheme.primaryColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Quick Secure Upload",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Hardware Encrypted • Isolated Local Storage",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                uploadPhotoLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = currentTheme.primaryColor,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("dashboard_upload_photos_button")
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Photos", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                uploadFileLauncher.launch("*/*")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = currentTheme.surfaceColor,
                                contentColor = currentTheme.primaryColor
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, currentTheme.primaryColor.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("dashboard_upload_files_button")
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Files", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 4. Important Vault Actions Only (Cleaned from clutter boxes)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Vault Actions",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        title = "Documents",
                        subtitle = "Browse & Organize",
                        icon = Icons.Default.Description,
                        accentColor = currentTheme.primaryColor,
                        onClick = { viewModel.navigateTo(Screen.DOCUMENTS) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        title = "Photos",
                        subtitle = "Photo Gallery",
                        icon = Icons.Default.Image,
                        accentColor = currentTheme.accentColor,
                        onClick = { viewModel.navigateTo(Screen.PRIVATE_VAULT_HOME) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        title = "Videos",
                        subtitle = "Video Collection",
                        icon = Icons.Default.Videocam,
                        accentColor = CyanAccent,
                        onClick = { viewModel.navigateTo(Screen.PRIVATE_VAULT_HOME) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        title = "Search",
                        subtitle = "Find Any File",
                        icon = Icons.Default.Search,
                        accentColor = TealAccent,
                        onClick = { viewModel.navigateTo(Screen.SEARCH) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. Initial Empty Vault Welcome Card (shown when user has no files yet)
        if (isVaultCompletelyEmpty) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .testTag("empty_vault_welcome_card"),
                    colors = CardDefaults.cardColors(containerColor = VaultSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "SecureVault",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Your private digital vault",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Row 1: No documents yet
                        EmptyVaultActionRow(
                            label = "No documents yet.",
                            buttonText = "+ Add Document",
                            onClick = { viewModel.navigateTo(Screen.ADD_DOCUMENT) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Row 2: No private photos yet
                        EmptyVaultActionRow(
                            label = "No private photos yet.",
                            buttonText = "+ Add Photo",
                            onClick = { viewModel.navigateTo(Screen.PRIVATE_VAULT_HOME) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Row 3: No hidden files yet
                        EmptyVaultActionRow(
                            label = "No hidden files yet.",
                            buttonText = "Open Hidden Vault",
                            onClick = { viewModel.navigateTo(Screen.HIDDEN_VAULT_HOME) }
                        )
                    }
                }
            }
        }

        // 5. Recent Files Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Files",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (documents.isNotEmpty()) {
                    Text(
                        text = "View all",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = EmeraldPrimary,
                        modifier = Modifier
                            .clickable { viewModel.navigateTo(Screen.DOCUMENTS) }
                            .padding(4.dp)
                    )
                }
            }
        }

        if (documents.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(VaultSurfaceElevated)
                        .border(1.dp, VaultBorder, RoundedCornerShape(14.dp))
                        .padding(24.dp)
                        .testTag("no_recent_files_box"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recent files",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            items(documents.take(5)) { doc ->
                DocumentCard(
                    document = doc,
                    onClick = { viewModel.selectDocument(doc) },
                    onFavoriteToggle = { viewModel.toggleDocumentFavorite(doc) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showThemeDialog) {
        ThemeSelectorDialog(
            currentTheme = currentTheme,
            onThemeSelected = { viewModel.setAppTheme(it) },
            onDismiss = { showThemeDialog = false }
        )
    }
    }
}

@Composable
private fun EmptyVaultActionRow(
    label: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextSecondary
        )
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldPrimary,
                contentColor = Color(0xFF003822)
            ),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                text = buttonText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StorageStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("storage_stat_card"),
        colors = CardDefaults.cardColors(containerColor = VaultSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(VaultSurfaceElevated)
            .border(1.dp, VaultBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("quick_action_${title.lowercase().replace(" ", "_")}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun rememberGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }
}
