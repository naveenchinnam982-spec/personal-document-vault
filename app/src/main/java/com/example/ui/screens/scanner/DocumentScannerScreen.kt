package com.example.ui.screens.scanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DocumentCategories
import com.example.data.security.OcrHelper
import com.example.ui.screens.documents.outlinedFieldColors
import com.example.ui.theme.CyanAccent
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
import java.io.ByteArrayOutputStream

enum class ScanFilter {
    NORMAL,
    HIGH_CONTRAST,
    BLACK_WHITE
}

@Composable
fun DocumentScannerScreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit
) {
    val scannedPages = remember { mutableStateListOf<Bitmap>() }
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var rotationAngle by remember { mutableIntStateOf(0) }
    var activeFilter by remember { mutableStateOf(ScanFilter.NORMAL) }
    var documentTitle by remember { mutableStateOf("Scanned Document") }
    var selectedCategory by remember { mutableStateOf("Identity") }
    var isSavingAsPdf by remember { mutableStateOf(true) }
    var isEdgeDetectionActive by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    // Camera take picture
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            scannedPages.add(bitmap)
            currentPageIndex = scannedPages.size - 1
            rotationAngle = 0
            if (documentTitle == "Scanned Document") {
                documentTitle = "Scan_${System.currentTimeMillis() % 10000}"
            }
        }
    }

    // Retake current page
    val retakeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null && currentPageIndex in scannedPages.indices) {
            scannedPages[currentPageIndex] = bitmap
            rotationAngle = 0
        }
    }

    // Launch camera automatically if nothing captured yet
    LaunchedEffect(Unit) {
        if (scannedPages.isEmpty()) {
            cameraLauncher.launch(null)
        }
    }

    // Function to re-render processed bitmap when rotation or filter changes
    fun applyTransformations(raw: Bitmap, angle: Int, filter: ScanFilter): Bitmap {
        val matrix = Matrix()
        if (angle != 0) {
            matrix.postRotate(angle.toFloat())
        }
        val rotated = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)

        val result = Bitmap.createBitmap(rotated.width, rotated.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        when (filter) {
            ScanFilter.NORMAL -> {
                canvas.drawBitmap(rotated, 0f, 0f, paint)
            }
            ScanFilter.HIGH_CONTRAST -> {
                val cm = ColorMatrix().apply {
                    set(
                        floatArrayOf(
                            1.5f, 0f, 0f, 0f, -30f,
                            0f, 1.5f, 0f, 0f, -30f,
                            0f, 0f, 1.5f, 0f, -30f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                }
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(rotated, 0f, 0f, paint)
            }
            ScanFilter.BLACK_WHITE -> {
                val cm = ColorMatrix().apply {
                    setSaturation(0f)
                }
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(rotated, 0f, 0f, paint)
            }
        }
        return result
    }

    val currentRawBitmap = scannedPages.getOrNull(currentPageIndex)
    val currentDisplayedBitmap = currentRawBitmap?.let { applyTransformations(it, rotationAngle, activeFilter) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultDarkBg)
            .testTag("document_scanner_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Text(
                    text = "Document Scanner",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                IconButton(
                    onClick = { cameraLauncher.launch(null) },
                    modifier = Modifier.testTag("rescan_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Add Page",
                        tint = EmeraldPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Scanner Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(VaultSurfaceElevated)
                    .border(
                        2.dp,
                        if (isEdgeDetectionActive) EmeraldPrimary else VaultBorder,
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (currentDisplayedBitmap != null) {
                    Image(
                        bitmap = currentDisplayedBitmap.asImageBitmap(),
                        contentDescription = "Scanned Page ${currentPageIndex + 1}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    )

                    // Page indicator badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Page ${currentPageIndex + 1} of ${scannedPages.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Edge detection overlay simulation
                    if (isEdgeDetectionActive) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .border(1.5.dp, CyanAccent.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = "Auto-Crop Ready",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(4.dp)
                            )
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap camera to capture page",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Multi-Page Thumbnails Strip
            if (scannedPages.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scanned Pages (${scannedPages.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    OutlinedButton(
                        onClick = { cameraLauncher.launch(null) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Next Page", fontSize = 11.sp, color = EmeraldPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(scannedPages) { index, page ->
                        val isSelected = index == currentPageIndex
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    2.dp,
                                    if (isSelected) EmeraldPrimary else VaultBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    currentPageIndex = index
                                    rotationAngle = 0
                                }
                        ) {
                            Image(
                                bitmap = page.asImageBitmap(),
                                contentDescription = "Page ${index + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Action Toolbar (Rotate, Auto Crop, Retake, Delete Page)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Rotate 90
                IconButton(
                    onClick = {
                        if (currentRawBitmap != null) {
                            rotationAngle = (rotationAngle + 90) % 360
                        }
                    },
                    modifier = Modifier.testTag("scanner_rotate_button")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate", tint = TextPrimary)
                        Text("Rotate", fontSize = 10.sp, color = TextSecondary)
                    }
                }

                // Toggle Auto-Edge Detection
                IconButton(
                    onClick = { isEdgeDetectionActive = !isEdgeDetectionActive },
                    modifier = Modifier.testTag("scanner_edge_button")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Crop, contentDescription = "Auto Edge", tint = if (isEdgeDetectionActive) CyanAccent else TextSecondary)
                        Text("Crop", fontSize = 10.sp, color = if (isEdgeDetectionActive) CyanAccent else TextSecondary)
                    }
                }

                // Retake Current Page
                IconButton(
                    onClick = { retakeLauncher.launch(null) },
                    modifier = Modifier.testTag("scanner_retake_button")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Retake", tint = EmeraldPrimary)
                        Text("Retake", fontSize = 10.sp, color = EmeraldPrimary)
                    }
                }

                // Delete Current Page
                if (scannedPages.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            if (currentPageIndex in scannedPages.indices) {
                                scannedPages.removeAt(currentPageIndex)
                                if (currentPageIndex >= scannedPages.size && scannedPages.isNotEmpty()) {
                                    currentPageIndex = scannedPages.size - 1
                                }
                                rotationAngle = 0
                            }
                        },
                        modifier = Modifier.testTag("scanner_delete_page_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Page", tint = VaultRed)
                            Text("Delete", fontSize = 10.sp, color = VaultRed)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Selector
            Text(
                text = "Document Filters",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    ScanFilter.NORMAL to "Normal",
                    ScanFilter.HIGH_CONTRAST to "High Contrast",
                    ScanFilter.BLACK_WHITE to "Document B&W"
                ).forEach { (f, label) ->
                    val isSel = activeFilter == f
                    FilterChip(
                        selected = isSel,
                        onClick = { activeFilter = f },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color(0xFF003822),
                            containerColor = VaultSurfaceElevated,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Export Format Toggle (PDF vs Image)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = isSavingAsPdf,
                    onClick = { isSavingAsPdf = true },
                    label = { Text("Save as PDF (${scannedPages.size} pages)", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldPrimary,
                        selectedLabelColor = Color(0xFF003822)
                    ),
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = !isSavingAsPdf,
                    onClick = { isSavingAsPdf = false },
                    label = { Text("Save as Image", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldPrimary,
                        selectedLabelColor = Color(0xFF003822)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metadata fields
            OutlinedTextField(
                value = documentTitle,
                onValueChange = { documentTitle = it },
                label = { Text("Document Name *") },
                singleLine = true,
                colors = outlinedFieldColors(),
                modifier = Modifier.fillMaxWidth().testTag("scanner_doc_name_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category Dropdown
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
                                showCategoryDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save into Vault
            Button(
                onClick = {
                    if (documentTitle.isBlank()) {
                        viewModel.showToast("Please enter a document name")
                        return@Button
                    }
                    if (scannedPages.isEmpty()) {
                        viewModel.showToast("Please capture a document using the camera first")
                        return@Button
                    }
                    isSaving = true

                    val bytes: ByteArray
                    val fileType: String

                    if (isSavingAsPdf) {
                        fileType = "PDF"
                        val pdfDocument = PdfDocument()
                        try {
                            for (i in scannedPages.indices) {
                                val transformed = applyTransformations(scannedPages[i], rotationAngle, activeFilter)
                                val pageInfo = PdfDocument.PageInfo.Builder(transformed.width, transformed.height, i + 1).create()
                                val page = pdfDocument.startPage(pageInfo)
                                page.canvas.drawBitmap(transformed, 0f, 0f, null)
                                pdfDocument.finishPage(page)
                            }
                            val stream = ByteArrayOutputStream()
                            pdfDocument.writeTo(stream)
                            bytes = stream.toByteArray()
                        } finally {
                            pdfDocument.close()
                        }
                    } else {
                        fileType = "SCAN"
                        val finalBitmap = currentDisplayedBitmap ?: scannedPages.first()
                        val stream = ByteArrayOutputStream()
                        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                        bytes = stream.toByteArray()
                    }

                    val firstPage = currentDisplayedBitmap ?: scannedPages.first()
                    val ocrResult = OcrHelper.analyzeDocument(documentTitle, selectedCategory, firstPage)

                    viewModel.addDocument(
                        name = documentTitle.trim(),
                        category = selectedCategory,
                        description = "Scanned with Document Scanner (${scannedPages.size} pages, Filter: ${activeFilter.name})",
                        fileType = fileType,
                        tags = "Scan, Document, ${ocrResult.detectedTags.joinToString(", ")}",
                        expiryDate = null,
                        isFavorite = false,
                        rawBytes = bytes,
                        ocrText = ocrResult.rawText
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
                    .testTag("scanner_save_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color(0xFF003822), modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = if (isSavingAsPdf) "Create Encrypted PDF (${scannedPages.size} pgs)" else "Save Encrypted Document",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
