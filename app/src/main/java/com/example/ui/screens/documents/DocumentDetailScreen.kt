package com.example.ui.screens.documents

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.export.FileDownloadHelper
import com.example.data.security.VaultType
import com.example.ui.components.FullScreenPdfReaderDialog
import com.example.ui.components.PdfEmbeddedViewer
import com.example.ui.components.VideoPlayerComponent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultDarkBg
import com.example.ui.theme.VaultRed
import com.example.ui.theme.VaultSurfaceElevated
import com.example.ui.viewmodel.VaultViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DocumentDetailScreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val doc = viewModel.selectedDocument.collectAsStateWithLifecycle().value
    val bitmap by viewModel.decryptedViewerBitmap.collectAsStateWithLifecycle()
    val pdfState by viewModel.pdfViewerState.collectAsStateWithLifecycle()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMoveToHiddenDialog by remember { mutableStateOf(false) }
    var isSharing by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadedUri by remember { mutableStateOf<Uri?>(null) }
    var downloadMessage by remember { mutableStateOf<String?>(null) }
    var showFullscreenImageViewer by remember { mutableStateOf(false) }

    if (doc == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VaultDarkBg),
            contentAlignment = Alignment.Center
        ) {
            Text("No document selected", color = TextSecondary)
        }
        return
    }

    val isVideo = doc.fileType == "VIDEO" || doc.filePath.endsWith("enc_vid") ||
            doc.filePath.contains("vid", ignoreCase = true) ||
            doc.name.lowercase().let {
                it.endsWith(".mp4") || it.endsWith(".mov") || it.endsWith(".mkv") || it.endsWith(".webm") ||
                        it.endsWith(".3gp") || it.endsWith(".m4v") || it.endsWith(".avi") || it.endsWith(".flv")
            }
    val isPdf = !isVideo && (doc.fileType == "PDF" || doc.filePath.endsWith("enc_pdf") || pdfState != null)
    val targetMimeType = if (isVideo) "video/mp4" else if (isPdf) "application/pdf" else "image/png"
    val safeExtension = if (isVideo) ".mp4" else if (isPdf) ".pdf" else ".png"
    val defaultSaveName = "${doc.name.replace(" ", "_")}$safeExtension"

    val saveAsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(targetMimeType)
    ) { uri: Uri? ->
        if (uri != null) {
            isDownloading = true
            viewModel.exportFileToChosenUri(uri, doc.filePath, VaultType.MAIN) { success ->
                isDownloading = false
                if (success) {
                    downloadedUri = uri
                    downloadMessage = "File exported successfully!"
                    viewModel.showToast("File saved to selected location")
                } else {
                    viewModel.showToast("Failed to save file")
                }
            }
        }
    }

    fun triggerDirectDownload() {
        if (isDownloading) return
        isDownloading = true
        downloadMessage = null
        viewModel.downloadDocument(doc) { success, msg, uri ->
            isDownloading = false
            if (success && uri != null) {
                downloadedUri = uri
                downloadMessage = msg
                viewModel.showToast(msg)
            } else {
                viewModel.showToast(msg)
            }
        }
    }

    fun triggerShare() {
        coroutineScope.launch {
            isSharing = true
            val tempFile = viewModel.prepareDecryptedFileForSharing(
                doc.filePath,
                defaultSaveName,
                isPrivate = false
            )
            if (tempFile != null) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = targetMimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    context.startActivity(Intent.createChooser(shareIntent, "Share Secure Document"))
                } catch (e: Exception) {
                    viewModel.showToast("No sharing app available")
                }
            } else {
                viewModel.showToast("Failed to prepare document for sharing")
            }
            isSharing = false
        }
    }

    val daysUntilExpiry = doc.daysUntilExpiry()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultDarkBg)
            .testTag("document_detail_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Top App Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("doc_detail_back_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.toggleDocumentFavorite(doc) },
                        modifier = Modifier.testTag("doc_detail_fav_button")
                    ) {
                        Icon(
                            imageVector = if (doc.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (doc.isFavorite) VaultAmber else TextMuted
                        )
                    }

                    // Direct Download Button in Header
                    IconButton(
                        onClick = { triggerDirectDownload() },
                        enabled = !isDownloading,
                        modifier = Modifier.testTag("doc_detail_download_button")
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = EmeraldPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download Document",
                                tint = EmeraldPrimary
                            )
                        }
                    }

                    // Share Button
                    IconButton(
                        onClick = { triggerShare() },
                        modifier = Modifier.testTag("doc_detail_share_button")
                    ) {
                        if (isSharing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = EmeraldPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = TextSecondary
                            )
                        }
                    }

                    // Move to Hidden Vault Button
                    IconButton(
                        onClick = { showMoveToHiddenDialog = true },
                        modifier = Modifier.testTag("doc_detail_move_to_hidden_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Move to Hidden Vault",
                            tint = com.example.ui.theme.HiddenPrimary
                        )
                    }

                    // Delete Button
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.testTag("doc_detail_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Move to Trash",
                            tint = VaultRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ==========================================
            // DOCUMENT / PDF VIEWER VIEWPORT
            // ==========================================
            if (isVideo) {
                // Video Player
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(androidx.compose.ui.graphics.Color.Black)
                ) {
                    VideoPlayerComponent(
                        encryptedPath = doc.filePath,
                        vaultType = VaultType.MAIN,
                        videoTitle = doc.name,
                        onBack = onBack,
                        modifier = Modifier.fillMaxSize(),
                        accentColor = EmeraldPrimary
                    )
                }
            } else if (isPdf && pdfState != null) {
                // Interactive Built-in Native PDF Viewer
                PdfEmbeddedViewer(
                    state = pdfState!!,
                    onPageSelected = { viewModel.goToPdfPage(it) },
                    onNextPage = { viewModel.nextPdfPage() },
                    onPrevPage = { viewModel.prevPdfPage() },
                    onDownload = { triggerDirectDownload() },
                    onShare = { triggerShare() }
                )
            } else if (bitmap != null) {
                // Image or Scanned Document Viewer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(VaultSurfaceElevated)
                        .border(1.dp, VaultBorder, RoundedCornerShape(16.dp))
                        .clickable { showFullscreenImageViewer = true },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = doc.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tap to view", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            } else {
                // Loading Placeholder while decrypting
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(VaultSurfaceElevated)
                        .border(1.dp, VaultBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = EmeraldPrimary, strokeWidth = 2.5.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isPdf) "Decrypting & Loading PDF..." else "Decrypting document...",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Download Status Banner
            AnimatedVisibility(visible = downloadMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldContainer)
                        .border(1.dp, EmeraldLight.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldLight,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = downloadMessage ?: "",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = EmeraldLight
                            )
                        }

                        if (downloadedUri != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    FileDownloadHelper.openFile(context, downloadedUri!!, targetMimeType)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldLight,
                                    contentColor = Color(0xFF003822)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Open", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // DOWNLOAD & EXPORT SECTION
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(VaultSurfaceElevated)
                    .border(1.dp, VaultBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Download & Save Options",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (isPdf) "Save or export decrypted PDF" else "Save or export decrypted file",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. One-Click Save to Downloads
                        Button(
                            onClick = { triggerDirectDownload() },
                            enabled = !isDownloading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color(0xFF003822)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("doc_detail_direct_download_action")
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color(0xFF003822),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // 2. Save As (SAF Custom Path)
                        OutlinedButton(
                            onClick = {
                                try {
                                    saveAsLauncher.launch(defaultSaveName)
                                } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("doc_detail_save_as_action")
                        ) {
                            Icon(Icons.Default.SaveAlt, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save As...", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title & Category
            Text(
                text = doc.name,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(EmeraldContainer)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = doc.category,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldLight
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(VaultSurfaceElevated)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isPdf) "PDF Document" else doc.fileType,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                if (daysUntilExpiry != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (daysUntilExpiry < 0) VaultRed.copy(alpha = 0.2f) else VaultAmber.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (daysUntilExpiry < 0) "Expired" else "Expires in $daysUntilExpiry days",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (daysUntilExpiry < 0) VaultRed else VaultAmber
                        )
                    }
                }
            }

            if (doc.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = doc.description,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            }

            if (doc.tagList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    doc.tagList.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(VaultSurfaceElevated)
                                .border(1.dp, VaultBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = "#$tag", fontSize = 11.sp, color = TextMuted)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Security Details Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(VaultSurfaceElevated)
                    .border(1.dp, VaultBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Security & Encryption Status",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = "• Keystore AES-256 GCM authenticated encryption\n• Stored in private sandbox storage (isolated from OS gallery)\n• Added: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(doc.createdAt))}",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // OCR Extracted Text Card
            if (doc.ocrText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(VaultSurfaceElevated)
                        .border(1.dp, VaultBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Extracted OCR Text",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )

                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Document OCR", doc.ocrText))
                                    viewModel.showToast("OCR text copied to clipboard")
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy OCR",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = doc.ocrText,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // Fullscreen Image Dialog for Photos/Scans
        if (showFullscreenImageViewer && bitmap != null) {
            FullScreenImageViewerDialog(
                bitmap = bitmap!!,
                title = doc.name,
                onDismiss = { showFullscreenImageViewer = false },
                onDownload = { triggerDirectDownload() },
                onShare = { triggerShare() }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = VaultSurfaceElevated,
                title = {
                    Text("Move to Trash?", color = TextPrimary, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        "This document will be moved to the Trash bin. You can restore it anytime or permanently delete it.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            viewModel.moveDocumentToTrash(doc)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VaultRed,
                            contentColor = Color.White
                        )
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
                    Text("Move to Hidden Vault?", color = TextPrimary, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        "\"${doc.name}\" will be moved into the isolated Hidden Vault and removed from your Main Vault.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showMoveToHiddenDialog = false
                            viewModel.moveDocumentToHiddenVault(doc) { success ->
                                if (success) {
                                    onBack()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.HiddenPrimary)
                    ) {
                        Text("Move & Make Private", color = Color.White, fontWeight = FontWeight.Bold)
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
}

@Composable
fun FullScreenImageViewerDialog(
    bitmap: android.graphics.Bitmap,
    title: String,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )

                // Top Controls
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
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDownload) {
                            Icon(Icons.Default.Download, contentDescription = "Download", tint = EmeraldPrimary)
                        }
                        IconButton(onClick = onShare) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}
