package com.example.ui.screens.documents

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFolderUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DocumentCategories
import com.example.data.security.OcrHelper
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultDarkBg
import com.example.ui.theme.VaultRed
import com.example.ui.theme.VaultSurfaceElevated
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.VaultViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.Calendar

@Composable
fun AddDocumentScreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var docName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Identity") }
    var isCustomCategory by remember { mutableStateOf(false) }
    var customCategoryText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }
    var expiryDaysOffset by remember { mutableStateOf<Int?>(null) } // e.g. 30, 365, or null
    var isFavorite by remember { mutableStateOf(false) }
    var fileType by remember { mutableStateOf("IMAGE") } // PDF, IMAGE, SCAN
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rawBytesToSave by remember { mutableStateOf<ByteArray?>(null) }
    var extractedOcr by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var pdfThumbnail by remember { mutableStateOf<Bitmap?>(null) }
    var pdfPageCount by remember { mutableStateOf<Int?>(null) }

    var isBatchMode by remember { mutableStateOf(false) }
    var batchUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    fun resolveNameFromUri(uri: Uri): String {
        var name = ""
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex) ?: ""
                }
            }
        } catch (_: Exception) {}
        return name.ifBlank { "Document" }
    }

    fun getUriSize(uri: Uri): Long {
        return try {
            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (sizeIndex != -1 && cursor.moveToFirst()) cursor.getLong(sizeIndex) else 0L
            } ?: 0L
        } catch (_: Exception) { 0L }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 KB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> "%.2f GB".format(gb)
            mb >= 1.0 -> "%.1f MB".format(mb)
            else -> "%.0f KB".format(kb)
        }
    }

    // Pick Images (Single or Multi) Launcher
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.size == 1) {
            val uri = uris.first()
            selectedUri = uri
            fileType = "IMAGE"
            if (docName.isBlank()) docName = resolveNameFromUri(uri)
            val ocr = OcrHelper.analyzeDocument(docName, selectedCategory)
            extractedOcr = ocr.rawText
            if (tagsInput.isBlank()) tagsInput = ocr.detectedTags.joinToString(", ")
        } else if (uris.size > 1) {
            batchUris = (batchUris + uris).distinct()
            isBatchMode = true
            viewModel.showToast("Switched to Batch Upload (${uris.size} images selected)")
        }
    }

    // Dedicated Batch Image Picker
    val batchImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            batchUris = (batchUris + uris).distinct()
            isBatchMode = true
            viewModel.showToast("${uris.size} image(s) added to batch")
        }
    }

    // Camera Capture Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            fileType = "SCAN"
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            rawBytesToSave = stream.toByteArray()
            if (docName.isBlank()) docName = "Scanned Document"
            val ocr = OcrHelper.analyzeDocument(docName, selectedCategory, bitmap)
            extractedOcr = ocr.rawText
            if (tagsInput.isBlank()) tagsInput = ocr.detectedTags.joinToString(", ")
        }
    }

    // Open Documents (Single or Multi PDF) Launcher
    val docPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.size == 1) {
            val uri = uris.first()
            selectedUri = uri
            fileType = "PDF"
            if (docName.isBlank()) docName = resolveNameFromUri(uri)
            val ocr = OcrHelper.analyzeDocument(docName, selectedCategory)
            extractedOcr = ocr.rawText
            if (tagsInput.isBlank()) tagsInput = ocr.detectedTags.joinToString(", ")
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val meta = com.example.data.pdf.PdfDocumentManager.inspectPdfUri(context, uri)
                    if (meta != null) {
                        pdfPageCount = meta.pageCount
                        pdfThumbnail = meta.firstPageThumbnail
                    }
                } catch (_: Exception) {}
            }
        } else if (uris.size > 1) {
            batchUris = (batchUris + uris).distinct()
            isBatchMode = true
            viewModel.showToast("Switched to Batch Upload (${uris.size} PDFs selected)")
        }
    }

    // Dedicated Batch PDF Picker
    val batchDocPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            batchUris = (batchUris + uris).distinct()
            isBatchMode = true
            viewModel.showToast("${uris.size} PDF document(s) added to batch")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultDarkBg)
            .testTag("add_document_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("add_doc_back_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add Document",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Selector: Single Document vs Batch Multi-Upload
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(VaultSurfaceElevated)
                    .border(1.dp, VaultBorder, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (!isBatchMode) EmeraldPrimary.copy(alpha = 0.2f) else Color.Transparent)
                        .border(
                            1.dp,
                            if (!isBatchMode) EmeraldPrimary else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { isBatchMode = false }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Single Document",
                        fontSize = 13.sp,
                        fontWeight = if (!isBatchMode) FontWeight.Bold else FontWeight.Medium,
                        color = if (!isBatchMode) EmeraldPrimary else TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isBatchMode) CyanAccent.copy(alpha = 0.2f) else Color.Transparent)
                        .border(
                            1.dp,
                            if (isBatchMode) CyanAccent else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { isBatchMode = true }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Batch Multi-Upload",
                            fontSize = 13.sp,
                            fontWeight = if (isBatchMode) FontWeight.Bold else FontWeight.Medium,
                            color = if (isBatchMode) CyanAccent else TextSecondary
                        )
                        if (batchUris.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(CyanAccent)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${batchUris.size}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isBatchMode) {
                // ==================== BATCH MULTI-UPLOAD SECTION ====================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(VaultSurfaceElevated)
                        .border(1.dp, VaultBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Multiple Files",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyanAccent.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "High-Capacity",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Upload multiple photos or PDFs in a single operation. Files are encrypted with AES-256 GCM in the background.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SourceButton(
                            title = "Select Images",
                            icon = Icons.Default.AddPhotoAlternate,
                            accent = TealAccent,
                            isSelected = false,
                            onClick = {
                                try {
                                    batchImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.weight(1f)
                        )

                        SourceButton(
                            title = "Select PDFs",
                            icon = Icons.Default.PictureAsPdf,
                            accent = VaultRed,
                            isSelected = false,
                            onClick = {
                                try {
                                    batchDocPicker.launch(arrayOf("application/pdf"))
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Category Selector for Batch
                    Text(
                        text = "Target Category for Batch",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = {
                                IconButton(onClick = { showCategoryDropdown = true }) {
                                    Text("▼", color = EmeraldPrimary, fontSize = 12.sp)
                                }
                            },
                            colors = outlinedFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCategoryDropdown = true }
                        )

                        DropdownMenu(
                            expanded = showCategoryDropdown,
                            onDismissRequest = { showCategoryDropdown = false },
                            modifier = Modifier.background(VaultSurfaceElevated)
                        ) {
                            DocumentCategories.DEFAULT_NAMES.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat, color = TextPrimary) },
                                    onClick = {
                                        selectedCategory = cat
                                        isCustomCategory = false
                                        showCategoryDropdown = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("+ Custom Category", color = EmeraldPrimary, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    isCustomCategory = true
                                    showCategoryDropdown = false
                                }
                            )
                        }
                    }

                    if (isCustomCategory) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customCategoryText,
                            onValueChange = { customCategoryText = it },
                            label = { Text("Custom Category Name") },
                            singleLine = true,
                            colors = outlinedFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (batchUris.isNotEmpty()) {
                        val totalBatchBytes = batchUris.sumOf { getUriSize(it) }

                        // Summary pill
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(VaultDarkBg)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Ready to Import",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "${batchUris.size} files (${formatFileSize(totalBatchBytes)})",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldLight
                                )
                            }

                            Button(
                                onClick = { batchUris = emptyList() },
                                colors = ButtonDefaults.buttonColors(containerColor = VaultRed.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = VaultRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear All", fontSize = 11.sp, color = VaultRed)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Preview list of files (first 10 items shown, plus remaining indicator)
                        Text(
                            text = "Selected Files Preview:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            batchUris.take(10).forEachIndexed { index, uri ->
                                val name = resolveNameFromUri(uri)
                                val size = getUriSize(uri)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(VaultDarkBg)
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "${index + 1}.",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = name,
                                            fontSize = 12.sp,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = formatFileSize(size),
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            batchUris = batchUris.filter { it != uri }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = VaultRed, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                            if (batchUris.size > 10) {
                                Text(
                                    text = "+ ${batchUris.size - 10} more files queued",
                                    fontSize = 11.sp,
                                    color = CyanAccent,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Upload button
                        Button(
                            onClick = {
                                val finalCategory = if (isCustomCategory && customCategoryText.isNotBlank()) customCategoryText.trim() else selectedCategory
                                viewModel.importMultipleDocuments(batchUris, finalCategory)
                                batchUris = emptyList()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("batch_upload_documents_button")
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Encrypt & Upload ${batchUris.size} Files to Vault",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    } else {
                        // Empty batch state
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(VaultDarkBg)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No files selected yet",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap 'Select Images' or 'Select PDFs' above to pick 10, 50, 100, 500+ files simultaneously.",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            } else {
                // ==================== SINGLE DOCUMENT SECTION ====================

                // Source Selector Cards
                Text(
                    text = "Choose Import Source",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SourceButton(
                    title = "Import PDF",
                    icon = Icons.Default.PictureAsPdf,
                    accent = VaultRed,
                    isSelected = fileType == "PDF" && (selectedUri != null),
                    onClick = {
                        try {
                            docPicker.launch(arrayOf("application/pdf"))
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.weight(1f)
                )

                SourceButton(
                    title = "Choose Image",
                    icon = Icons.Default.Image,
                    accent = TealAccent,
                    isSelected = fileType == "IMAGE" && (selectedUri != null),
                    onClick = {
                        try {
                            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.weight(1f)
                )

                SourceButton(
                    title = "Scan Doc",
                    icon = Icons.Default.CameraAlt,
                    accent = CyanAccent,
                    isSelected = fileType == "SCAN" || capturedBitmap != null,
                    onClick = {
                        try {
                            cameraLauncher.launch(null)
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Preview thumbnail if image/scan is loaded
            if (capturedBitmap != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(VaultSurfaceElevated)
                        .border(1.dp, EmeraldPrimary, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = capturedBitmap!!.asImageBitmap(),
                        contentDescription = "Scanned preview",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else if (fileType == "PDF" && pdfThumbnail != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(VaultSurfaceElevated)
                        .border(1.dp, VaultRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = pdfThumbnail!!.asImageBitmap(),
                            contentDescription = "PDF Cover Page",
                            modifier = Modifier
                                .width(50.dp)
                                .height(65.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = VaultRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "PDF Document",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "${pdfPageCount ?: 1} Page${if ((pdfPageCount ?: 1) > 1) "s" else ""} • Ready to view & decrypt",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else if (selectedUri != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(VaultSurfaceElevated)
                        .border(1.dp, EmeraldPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "File attached: ${selectedUri!!.lastPathSegment ?: "Document"}",
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Form inputs
            Text(
                text = "Document Details",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Name
            OutlinedTextField(
                value = docName,
                onValueChange = {
                    docName = it
                    val ocr = OcrHelper.analyzeDocument(it, selectedCategory)
                    extractedOcr = ocr.rawText
                },
                label = { Text("Document Name * (e.g. Aadhaar Card, B.Tech)") },
                singleLine = true,
                colors = outlinedFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_doc_name_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category Picker
            Box {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = {
                        IconButton(onClick = { showCategoryDropdown = true }) {
                            Text("▼", color = EmeraldPrimary, fontSize = 12.sp)
                        }
                    },
                    colors = outlinedFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCategoryDropdown = true }
                        .testTag("add_doc_category_picker")
                )

                DropdownMenu(
                    expanded = showCategoryDropdown,
                    onDismissRequest = { showCategoryDropdown = false },
                    modifier = Modifier.background(VaultSurfaceElevated)
                ) {
                    DocumentCategories.DEFAULT_NAMES.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat, color = TextPrimary) },
                            onClick = {
                                selectedCategory = cat
                                isCustomCategory = false
                                showCategoryDropdown = false
                                val ocr = OcrHelper.analyzeDocument(docName, cat)
                                extractedOcr = ocr.rawText
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("+ Custom Category", color = EmeraldPrimary, fontWeight = FontWeight.Bold) },
                        onClick = {
                            isCustomCategory = true
                            showCategoryDropdown = false
                        }
                    )
                }
            }

            if (isCustomCategory) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = customCategoryText,
                    onValueChange = { customCategoryText = it },
                    label = { Text("Enter Custom Category Name") },
                    singleLine = true,
                    colors = outlinedFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_doc_custom_category_input")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                maxLines = 3,
                colors = outlinedFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_doc_description_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tags
            OutlinedTextField(
                value = tagsInput,
                onValueChange = { tagsInput = it },
                label = { Text("Tags (comma separated)") },
                singleLine = true,
                colors = outlinedFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_doc_tags_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Expiry preset buttons
            Text(
                text = "Expiry Reminder",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "None" to null,
                    "30 Days" to 30,
                    "1 Year" to 365,
                    "3 Years" to 1095
                ).forEach { (label, days) ->
                    val isSel = expiryDaysOffset == days
                    OutlinedButton(
                        onClick = { expiryDaysOffset = days },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSel) EmeraldPrimary.copy(alpha = 0.2f) else Color.Transparent,
                            contentColor = if (isSel) EmeraldPrimary else TextSecondary
                        ),
                        border = BorderStroke(1.dp, if (isSel) EmeraldPrimary else VaultBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(label, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Favorite toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = null,
                        tint = if (isFavorite) VaultAmber else TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Add to Favorites", fontSize = 14.sp, color = TextPrimary)
                }
                Switch(
                    checked = isFavorite,
                    onCheckedChange = { isFavorite = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF003822),
                        checkedTrackColor = EmeraldPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Extracted OCR text box
            if (extractedOcr.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(VaultSurfaceElevated)
                        .border(1.dp, VaultBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🧠 OCR Extracted Content",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = "Searchable",
                                fontSize = 10.sp,
                                color = TealAccent
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = extractedOcr,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Save Actions
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Primary: Save & Open Document
                Button(
                    onClick = {
                        if (docName.isBlank()) {
                            viewModel.showToast("Please enter a document name")
                            return@Button
                        }

                        if (rawBytesToSave == null && selectedUri == null) {
                            viewModel.showToast("Please select a file, PDF, or take a photo first")
                            return@Button
                        }

                        isSaving = true

                        val expiryTimestamp = expiryDaysOffset?.let { days ->
                            System.currentTimeMillis() + (days.toLong() * 24 * 60 * 60 * 1000L)
                        }

                        val finalCategory = if (isCustomCategory && customCategoryText.isNotBlank()) customCategoryText.trim() else selectedCategory

                        viewModel.addDocument(
                            name = docName.trim(),
                            category = finalCategory,
                            description = description.trim(),
                            fileType = fileType,
                            tags = tagsInput.trim(),
                            expiryDate = expiryTimestamp,
                            isFavorite = isFavorite,
                            sourceUri = selectedUri,
                            rawBytes = rawBytesToSave,
                            ocrText = extractedOcr,
                            openImmediately = true
                        )
                    },
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color(0xFF003822)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_and_open_document_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFF003822),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (fileType == "PDF") "Save & Open PDF" else "Save & Open Document",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Secondary: Save to Vault (without opening)
                OutlinedButton(
                    onClick = {
                        if (docName.isBlank()) {
                            viewModel.showToast("Please enter a document name")
                            return@OutlinedButton
                        }

                        if (rawBytesToSave == null && selectedUri == null) {
                            viewModel.showToast("Please select a file, PDF, or take a photo first")
                            return@OutlinedButton
                        }

                        isSaving = true

                        val expiryTimestamp = expiryDaysOffset?.let { days ->
                            System.currentTimeMillis() + (days.toLong() * 24 * 60 * 60 * 1000L)
                        }

                        val finalCategory = if (isCustomCategory && customCategoryText.isNotBlank()) customCategoryText.trim() else selectedCategory

                        viewModel.addDocument(
                            name = docName.trim(),
                            category = finalCategory,
                            description = description.trim(),
                            fileType = fileType,
                            tags = tagsInput.trim(),
                            expiryDate = expiryTimestamp,
                            isFavorite = isFavorite,
                            sourceUri = selectedUri,
                            rawBytes = rawBytesToSave,
                            ocrText = extractedOcr,
                            openImmediately = false
                        )
                    },
                    enabled = !isSaving,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextPrimary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_document_securely_button")
                ) {
                    Text(
                        text = "Save to Vault",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun SourceButton(
    title: String,
    icon: ImageVector,
    accent: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) accent.copy(alpha = 0.2f) else VaultSurfaceElevated)
            .border(
                1.dp,
                if (isSelected) accent else VaultBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) accent else TextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) accent else TextPrimary
            )
        }
    }
}

@Composable
fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = EmeraldPrimary,
    unfocusedBorderColor = VaultBorder,
    focusedLabelColor = EmeraldPrimary,
    unfocusedLabelColor = TextSecondary,
    cursorColor = EmeraldPrimary
)
