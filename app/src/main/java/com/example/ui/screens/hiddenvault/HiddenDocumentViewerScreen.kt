package com.example.ui.screens.hiddenvault

import android.content.Intent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Warning
import com.example.ui.components.PdfEmbeddedViewer
import com.example.data.export.FileDownloadHelper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.security.VaultType
import com.example.ui.components.VideoPlayerComponent
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
import com.example.ui.viewmodel.VaultViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HiddenDocumentViewerScreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val item by viewModel.selectedHiddenItem.collectAsState()
    val bitmap by viewModel.decryptedHiddenBitmap.collectAsState()
    val pdfState by viewModel.pdfViewerState.collectAsState()

    var isEditingNotes by remember { mutableStateOf(false) }
    var notesText by remember(item?.notes) { mutableStateOf(item?.notes ?: "") }

    var showExportWarningDialog by remember { mutableStateOf(false) }
    var showReAuthPinDialog by remember { mutableStateOf(false) }
    var enteredReAuthPin by remember { mutableStateOf("") }
    var reAuthError by remember { mutableStateOf<String?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadMessage by remember { mutableStateOf<String?>(null) }
    var downloadedUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val currentItem = item ?: run {
        Box(
            modifier = Modifier.fillMaxSize().background(HiddenVaultDarkBg),
            contentAlignment = Alignment.Center
        ) {
            Text("No document selected", color = TextSecondary)
        }
        return
    }

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HiddenVaultDarkBg)
            .testTag("hidden_document_viewer_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = currentItem.name,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "Hidden Vault • ${currentItem.category}",
                        color = HiddenPrimaryLight,
                        fontSize = 11.sp
                    )
                }

                Row {
                    IconButton(onClick = { viewModel.toggleHiddenFavorite(currentItem) }) {
                        Icon(
                            imageVector = if (currentItem.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (currentItem.isFavorite) HiddenGold else TextSecondary
                        )
                    }

                    IconButton(
                        onClick = {
                            if (isDownloading) return@IconButton
                            isDownloading = true
                            downloadMessage = null
                            viewModel.downloadHiddenItem(currentItem) { success, msg, uri ->
                                isDownloading = false
                                if (success && uri != null) {
                                    downloadedUri = uri
                                    downloadMessage = msg
                                    viewModel.showToast(msg)
                                } else {
                                    viewModel.showToast(msg)
                                }
                            }
                        },
                        enabled = !isDownloading
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = HiddenGold, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Download, contentDescription = "Download", tint = HiddenGold)
                        }
                    }

                    IconButton(onClick = { showExportWarningDialog = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = TextPrimary)
                    }

                    IconButton(onClick = { viewModel.moveHiddenItemToTrash(currentItem) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Move to Trash", tint = VaultRed)
                    }
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // Document / PDF / Video Preview Card
                val isVideoItem = currentItem.itemType == "VIDEO" || currentItem.encryptedPath.endsWith("enc_vid") ||
                        currentItem.encryptedPath.contains("vid", ignoreCase = true) ||
                        currentItem.name.lowercase().let {
                            it.endsWith(".mp4") || it.endsWith(".mov") || it.endsWith(".mkv") || it.endsWith(".webm") ||
                                    it.endsWith(".3gp") || it.endsWith(".m4v") || it.endsWith(".avi") || it.endsWith(".flv")
                        }
                val isPdfItem = !isVideoItem && (currentItem.itemType in listOf("DOCUMENT", "PDF", "SCAN") || currentItem.encryptedPath.endsWith("enc_pdf") || pdfState != null)

                if (isVideoItem) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Black),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        VideoPlayerComponent(
                            encryptedPath = currentItem.encryptedPath,
                            vaultType = VaultType.HIDDEN,
                            videoTitle = currentItem.name,
                            onBack = onBack,
                            modifier = Modifier.fillMaxSize(),
                            accentColor = HiddenPrimary
                        )
                    }
                } else if (isPdfItem && pdfState != null) {
                    PdfEmbeddedViewer(
                        state = pdfState!!,
                        onPageSelected = { viewModel.goToPdfPage(it) },
                        onNextPage = { viewModel.nextPdfPage() },
                        onPrevPage = { viewModel.prevPdfPage() },
                        onDownload = {
                            viewModel.downloadHiddenItem(currentItem) { _, msg, uri ->
                                downloadedUri = uri
                                downloadMessage = msg
                                viewModel.showToast(msg)
                            }
                        },
                        onShare = { showExportWarningDialog = true }
                    )
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        colors = CardDefaults.cardColors(containerColor = HiddenVaultSurface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap!!.asImageBitmap(),
                                    contentDescription = currentItem.name,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(14.dp))
                                )
                            } else {
                                CircularProgressIndicator(color = HiddenPrimary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Security Status Badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(HiddenPrimaryContainer)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = HiddenPrimaryLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Top-Tier Isolated Cryptographic Storage", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("AES-256 GCM • Android Keystore Isolated Key", color = HiddenPrimaryLight, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Metadata Details
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = HiddenVaultSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Document Details", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        DetailRow("Category", currentItem.category)
                        DetailRow("Type", currentItem.itemType)
                        if (currentItem.tags.isNotBlank()) {
                            DetailRow("Tags", currentItem.tags)
                        }
                        DetailRow("Added Date", dateFormat.format(Date(currentItem.createdAt)))
                        if (currentItem.description.isNotBlank()) {
                            DetailRow("Description", currentItem.description)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // OCR Extracted Information
                if (currentItem.ocrText.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = HiddenVaultSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Extracted OCR Content", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentItem.ocrText,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Confidential Notes Editor
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = HiddenVaultSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Confidential Notes", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (isEditingNotes) {
                                IconButton(onClick = {
                                    viewModel.updateHiddenItemNotes(currentItem, notesText)
                                    isEditingNotes = false
                                }) {
                                    Icon(Icons.Default.Check, contentDescription = "Save Notes", tint = HiddenPrimary)
                                }
                            } else {
                                IconButton(onClick = { isEditingNotes = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Notes", tint = TextSecondary)
                                }
                            }
                        }

                        if (isEditingNotes) {
                            OutlinedTextField(
                                value = notesText,
                                onValueChange = { notesText = it },
                                placeholder = { Text("Write encrypted notes here...", color = TextMuted) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = HiddenPrimary,
                                    unfocusedBorderColor = HiddenBorder
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = if (currentItem.notes.isBlank()) "No notes added yet. Tap edit to write private notes." else currentItem.notes,
                                color = if (currentItem.notes.isBlank()) TextMuted else TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Step 1: Warning Modal
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
                        text = "Enter your Hidden Vault PIN to authorize sharing.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = enteredReAuthPin,
                        onValueChange = { enteredReAuthPin = it },
                        placeholder = { Text("PIN", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = HiddenPrimary,
                            unfocusedBorderColor = HiddenBorder
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (reAuthError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = reAuthError ?: "", color = VaultRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (viewModel.authManager.verifyHiddenPin(enteredReAuthPin)) {
                            showReAuthPinDialog = false
                            enteredReAuthPin = ""
                            reAuthError = null

                            // Secure share execution
                            scope.launch {
                                val ext = if (currentItem.itemType == "PDF") "pdf" else "jpg"
                                val exportName = "${currentItem.name.replace(" ", "_")}.$ext"
                                val exported = viewModel.prepareDecryptedFileForSharing(
                                    filePath = currentItem.encryptedPath,
                                    exportName = exportName,
                                    vaultType = VaultType.HIDDEN
                                )
                                if (exported != null) {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        exported
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = if (currentItem.itemType == "PDF") "application/pdf" else "image/*"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    try {
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Protected File"))
                                    } catch (e: Exception) {
                                        viewModel.showToast("No sharing app available")
                                    }
                                }
                            }
                        } else {
                            reAuthError = "Incorrect PIN"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HiddenPrimary)
                ) {
                    Text("Verify", color = Color.White)
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
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary, fontSize = 12.sp)
        Text(text = value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
