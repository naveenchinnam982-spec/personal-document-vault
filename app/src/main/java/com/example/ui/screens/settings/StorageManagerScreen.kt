package com.example.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.bulk.PhotoStorageMode
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.HiddenPrimary
import com.example.ui.theme.HiddenPrimaryLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultDarkBg
import com.example.ui.theme.VaultRed
import com.example.ui.theme.VaultSurfaceElevated
import com.example.ui.viewmodel.VaultViewModel

@Composable
fun StorageManagerScreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit
) {
    val storageInfo by viewModel.storageInfo.collectAsStateWithLifecycle()
    val currentStorageMode by viewModel.photoStorageMode.collectAsStateWithLifecycle()

    var showClearThumbnailsDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showClearTempDialog by remember { mutableStateOf(false) }

    val usedDeviceBytes = (storageInfo.totalDeviceSizeBytes - storageInfo.availableDeviceSizeBytes).coerceAtLeast(0L)
    val deviceUsedRatio = if (storageInfo.totalDeviceSizeBytes > 0L) {
        (usedDeviceBytes.toFloat() / storageInfo.totalDeviceSizeBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val vaultTotal = storageInfo.totalVaultSizeBytes + storageInfo.thumbnailsSizeBytes + storageInfo.cacheSizeBytes

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultDarkBg)
            .testTag("storage_manager_screen")
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(VaultSurfaceElevated)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Storage Dashboard & Manager",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Real-time hardware encrypted metrics",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Low Storage Warning if available < 2 GB
            if (storageInfo.availableDeviceSizeBytes < 2L * 1024 * 1024 * 1024 && storageInfo.availableDeviceSizeBytes > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(VaultAmber.copy(alpha = 0.15f))
                        .border(1.dp, VaultAmber.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = VaultAmber,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Device Storage is Low",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = VaultAmber
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Available: ${storageInfo.formatBytes(storageInfo.availableDeviceSizeBytes)}. Consider freeing device space or enabling Storage Saver mode.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 1. DEVICE STORAGE CARD
            Text(
                text = "DEVICE STORAGE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(VaultSurfaceElevated)
                    .border(1.dp, VaultBorder, RoundedCornerShape(18.dp))
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Device Storage", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                    Text(
                        text = "${(deviceUsedRatio * 100).toInt()}% Used",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (deviceUsedRatio > 0.9f) VaultRed else CyanAccent
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { deviceUsedRatio },
                    color = if (deviceUsedRatio > 0.9f) VaultRed else CyanAccent,
                    trackColor = VaultBorder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total", fontSize = 11.sp, color = TextSecondary)
                        Text(storageInfo.formatBytes(storageInfo.totalDeviceSizeBytes), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Used", fontSize = 11.sp, color = TextSecondary)
                        Text(storageInfo.formatBytes(usedDeviceBytes), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Available", fontSize = 11.sp, color = TextSecondary)
                        Text(storageInfo.formatBytes(storageInfo.availableDeviceSizeBytes), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EmeraldLight)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. SECUREVAULT BREAKDOWN CARD
            Text(
                text = "SECUREVAULT BREAKDOWN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(VaultSurfaceElevated)
                    .border(1.dp, VaultBorder, RoundedCornerShape(18.dp))
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Total SecureVault Space", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                    Text(
                        text = storageInfo.formatBytes(vaultTotal),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                StorageMetricRow(label = "Documents", bytes = storageInfo.docsSizeBytes, color = TextPrimary, storageInfo = storageInfo)
                StorageMetricRow(label = "PDFs", bytes = storageInfo.pdfsSizeBytes, color = EmeraldLight, storageInfo = storageInfo)
                StorageMetricRow(label = "Photos", bytes = storageInfo.photosSizeBytes, color = CyanAccent, storageInfo = storageInfo)
                StorageMetricRow(label = "Videos", bytes = storageInfo.videosSizeBytes, color = VaultAmber, storageInfo = storageInfo)
                StorageMetricRow(label = "Hidden Vault", bytes = storageInfo.hiddenSizeBytes, color = HiddenPrimaryLight, storageInfo = storageInfo)
                StorageMetricRow(label = "Thumbnails", bytes = storageInfo.thumbnailsSizeBytes, color = TextSecondary, storageInfo = storageInfo)
                StorageMetricRow(label = "Temporary / Cache", bytes = storageInfo.cacheSizeBytes, color = TextMuted, storageInfo = storageInfo)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. PHOTO STORAGE MODE SETTING
            Text(
                text = "PHOTO STORAGE MODE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(VaultSurfaceElevated)
                    .border(1.dp, VaultBorder, RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                PhotoStorageMode.values().forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.setPhotoStorageMode(mode) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentStorageMode == mode,
                            onClick = { viewModel.setPhotoStorageMode(mode) },
                            colors = RadioButtonDefaults.colors(selectedColor = EmeraldPrimary, unselectedColor = TextMuted)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = mode.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (currentStorageMode == mode) TextPrimary else TextSecondary
                            )
                            Text(
                                text = mode.description,
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. STORAGE CLEANUP ACTIONS
            Text(
                text = "SAFE STORAGE CLEANUP",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(VaultSurfaceElevated)
                    .border(1.dp, VaultBorder, RoundedCornerShape(18.dp))
            ) {
                CleanupActionRow(
                    icon = Icons.Default.CleaningServices,
                    title = "Clear Cache & Temporary Exports",
                    subtitle = "Safe cleanup. Recovers ${storageInfo.formatBytes(storageInfo.cacheSizeBytes)}. Never deletes vault files.",
                    tint = CyanAccent,
                    onClick = { showClearCacheDialog = true }
                )
                CleanupActionRow(
                    icon = Icons.Default.Image,
                    title = "Clear Thumbnail Cache",
                    subtitle = "Recovers ${storageInfo.formatBytes(storageInfo.thumbnailsSizeBytes)}. Thumbnails regenerate automatically on demand.",
                    tint = VaultAmber,
                    onClick = { showClearThumbnailsDialog = true }
                )
                CleanupActionRow(
                    icon = Icons.Default.DeleteSweep,
                    title = "Clear Temporary Import Buffer",
                    subtitle = "Purges leftover staging files from interrupted imports.",
                    tint = EmeraldLight,
                    onClick = { showClearTempDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Confirmation Dialogs
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear Temporary Cache?", color = TextPrimary) },
            text = {
                Text(
                    "This safely removes decrypted temporary files used during sharing. Your actual encrypted documents, photos, and videos will NEVER be deleted.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearStorageCache()
                        showClearCacheDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
                ) {
                    Text("Clear Cache", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = VaultSurfaceElevated
        )
    }

    if (showClearThumbnailsDialog) {
        AlertDialog(
            onDismissRequest = { showClearThumbnailsDialog = false },
            title = { Text("Clear Thumbnail Cache?", color = TextPrimary) },
            text = {
                Text(
                    "This frees up ${storageInfo.formatBytes(storageInfo.thumbnailsSizeBytes)}. Thumbnails will automatically rebuild when you view items.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearThumbnailCache()
                        showClearThumbnailsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultAmber, contentColor = Color(0xFF332000))
                ) {
                    Text("Clear Thumbnails", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearThumbnailsDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = VaultSurfaceElevated
        )
    }

    if (showClearTempDialog) {
        AlertDialog(
            onDismissRequest = { showClearTempDialog = false },
            title = { Text("Clear Temporary Import Files?", color = TextPrimary) },
            text = {
                Text(
                    "This cleans up incomplete temporary staging files from any interrupted imports.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearTempImports()
                        showClearTempDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color(0xFF003822))
                ) {
                    Text("Clean Up", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearTempDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = VaultSurfaceElevated
        )
    }
}

@Composable
private fun StorageMetricRow(
    label: String,
    bytes: Long,
    color: Color,
    storageInfo: com.example.data.repository.StorageInfo
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = label, fontSize = 13.sp, color = TextSecondary)
        }
        Text(
            text = storageInfo.formatBytes(bytes),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun CleanupActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp)
        }
    }
}
