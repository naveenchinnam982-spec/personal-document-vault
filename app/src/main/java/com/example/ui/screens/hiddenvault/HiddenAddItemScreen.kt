package com.example.ui.screens.hiddenvault

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HiddenItem
import com.example.ui.theme.HiddenBorder
import com.example.ui.theme.HiddenPrimary
import com.example.ui.theme.HiddenPrimaryContainer
import com.example.ui.theme.HiddenPrimaryLight
import com.example.ui.theme.HiddenVaultDarkBg
import com.example.ui.theme.HiddenVaultSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultRed
import com.example.ui.viewmodel.VaultViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HiddenAddItemScreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var itemType by remember { mutableStateOf("PHOTO") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileSize by remember { mutableStateOf("") }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var duplicateItem by remember { mutableStateOf<HiddenItem?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isMoveOperation by remember { mutableStateOf(true) }

    fun detectItemType(uri: Uri, fileName: String): String {
        val lower = fileName.lowercase()
        val mime = try { context.contentResolver.getType(uri)?.lowercase() } catch (_: Exception) { "" } ?: ""
        return when {
            mime.startsWith("video/") || lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".mkv") || lower.endsWith(".webm") || lower.endsWith(".3gp") || lower.endsWith(".avi") -> "VIDEO"
            mime.startsWith("image/") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".heic") -> "PHOTO"
            mime == "application/pdf" || lower.endsWith(".pdf") -> "PDF"
            else -> "DOCUMENT"
        }
    }

    fun extractFileName(uri: Uri): String {
        var result = ""
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    result = cursor.getString(nameIndex) ?: ""
                }
            }
        } catch (_: Exception) {}
        if (result.isBlank()) {
            val path = uri.lastPathSegment ?: "Item"
            result = path.substringAfterLast("/").substringBeforeLast(".")
        }
        return result
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 KB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) "%.1f MB".format(mb) else "%.0f KB".format(kb)
    }

    // Direct Gallery Picker (Photo & Video)
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.size == 1) {
            val uri = uris.first()
            selectedUri = uri
            errorMessage = null
            val rawName = extractFileName(uri)
            name = rawName
            itemType = detectItemType(uri, rawName)

            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1 && cursor.moveToFirst()) {
                        val bytes = cursor.getLong(sizeIndex)
                        selectedFileSize = formatBytes(bytes)
                    }
                }
            } catch (_: Exception) {}

            scope.launch(Dispatchers.IO) {
                try {
                    if (itemType == "VIDEO") {
                        val retriever = android.media.MediaMetadataRetriever()
                        retriever.setDataSource(context, uri)
                        val frame = retriever.getFrameAtTime(1000000, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: retriever.frameAtTime
                        retriever.release()
                        withContext(Dispatchers.Main) {
                            previewBitmap = frame
                        }
                    } else if (itemType == "PHOTO") {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                            val bmp = BitmapFactory.decodeStream(stream, null, opts)
                            withContext(Dispatchers.Main) {
                                previewBitmap = bmp
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        } else if (uris.size > 1) {
            viewModel.importMultipleHiddenItems(uris, "PHOTO", null, isMoveOperation)
            onBack()
        }
    }

    // Media Picker (single or multi)
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.size == 1) {
            val uri = uris.first()
            selectedUri = uri
            errorMessage = null
            val rawName = extractFileName(uri)
            name = rawName
            itemType = detectItemType(uri, rawName)

            // Extract file size
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1 && cursor.moveToFirst()) {
                        val bytes = cursor.getLong(sizeIndex)
                        selectedFileSize = formatBytes(bytes)
                    }
                }
            } catch (_: Exception) {}

            scope.launch(Dispatchers.IO) {
                try {
                    if (itemType == "VIDEO") {
                        val retriever = android.media.MediaMetadataRetriever()
                        retriever.setDataSource(context, uri)
                        val frame = retriever.getFrameAtTime(1000000, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: retriever.frameAtTime
                        retriever.release()
                        withContext(Dispatchers.Main) {
                            previewBitmap = frame
                        }
                    } else if (itemType == "PHOTO") {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                            val bmp = BitmapFactory.decodeStream(stream, null, opts)
                            withContext(Dispatchers.Main) {
                                previewBitmap = bmp
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            previewBitmap = null
                        }
                    }
                } catch (_: Exception) {}
            }
        } else if (uris.size > 1) {
            viewModel.importMultipleHiddenItems(uris, "PHOTO", null, isMoveOperation)
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HiddenVaultDarkBg)
            .testTag("hidden_add_item_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Upload to Hidden Vault", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Encrypted with Isolated Hardware AES-256", color = HiddenPrimaryLight, fontSize = 11.sp)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Impressive Upload Area (Primary Content)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hidden_file_picker_card"),
                    colors = CardDefaults.cardColors(containerColor = HiddenVaultSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (selectedUri != null) HiddenPrimary else HiddenBorder
                    )
                ) {
                    if (selectedUri == null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 30.dp, horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(HiddenPrimaryContainer)
                                    .border(1.5.dp, HiddenPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = "Upload",
                                    tint = HiddenPrimaryLight,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Choose Content to Encrypt",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Select photos, videos, or confidential documents",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        galleryPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = HiddenPrimary,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(44.dp)
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("From Gallery", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Button(
                                    onClick = { mediaPickerLauncher.launch("*/*") },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = HiddenVaultSurface,
                                        contentColor = HiddenPrimaryLight
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, HiddenPrimary.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(44.dp)
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Browse Files", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    } else {
                        // File Selected Preview Card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (previewBitmap != null) {
                                Image(
                                    bitmap = previewBitmap!!.asImageBitmap(),
                                    contentDescription = "Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, HiddenPrimary, RoundedCornerShape(10.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(HiddenPrimaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (itemType) {
                                            "VIDEO" -> Icons.Default.Videocam
                                            "PDF", "DOCUMENT" -> Icons.Default.Description
                                            else -> Icons.Default.Image
                                        },
                                        contentDescription = null,
                                        tint = HiddenPrimaryLight,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name.ifBlank { "Selected File" },
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = itemType,
                                        color = HiddenPrimaryLight,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                    if (selectedFileSize.isNotBlank()) {
                                        Text("•", color = TextMuted, fontSize = 11.sp)
                                        Text(selectedFileSize, color = TextMuted, fontSize = 11.sp)
                                    }
                                }
                            }

                            Button(
                                onClick = { mediaPickerLauncher.launch("*/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = HiddenPrimaryContainer),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Change", color = HiddenPrimaryLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 2. Only Important Content: Simple Clean File Name
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("File Name", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            errorMessage = null
                        },
                        placeholder = { Text("File Name", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = HiddenPrimary,
                            unfocusedBorderColor = HiddenBorder
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("hidden_item_name_input")
                    )
                }

                // Privacy Mode Selection: Move vs Copy
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(HiddenVaultSurface)
                        .border(1.dp, HiddenBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Privacy & Storage Action",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    // Move Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isMoveOperation) HiddenPrimaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                            .clickable { isMoveOperation = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isMoveOperation,
                            onClick = { isMoveOperation = true },
                            colors = RadioButtonDefaults.colors(selectedColor = HiddenPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Move to Hidden Vault (Private)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Verifies encrypted copy, then removes public photo from device gallery",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    // Copy Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isMoveOperation) HiddenPrimaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                            .clickable { isMoveOperation = false }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !isMoveOperation,
                            onClick = { isMoveOperation = false },
                            colors = RadioButtonDefaults.colors(selectedColor = HiddenPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Copy to Hidden Vault",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Leaves original photo visible in normal device gallery",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                // Security Note
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(HiddenPrimaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = HiddenPrimaryLight, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Encrypted in isolated vault with hardware Keystore",
                        color = HiddenPrimaryLight,
                        fontSize = 12.sp
                    )
                }

                if (errorMessage != null) {
                    Text(text = errorMessage ?: "", color = VaultRed, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3. Impressive Upload & Encrypt Action Button
                Button(
                    onClick = {
                        if (selectedUri == null) {
                            errorMessage = "Please tap above to choose a photo or file"
                            return@Button
                        }
                        if (name.isBlank()) {
                            errorMessage = "Please enter a name for the file"
                            return@Button
                        }

                        scope.launch {
                            val existing = viewModel.checkDuplicateHiddenItem(name.trim())
                            if (existing != null) {
                                duplicateItem = existing
                                showDuplicateDialog = true
                            } else {
                                performSave(viewModel, name, itemType, selectedUri, isMoveOperation)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HiddenPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_hidden_item_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isMoveOperation) "Move to Hidden Vault (Make Private)" else "Copy to Hidden Vault",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }

    // Duplicate Detection Alert Dialog
    if (showDuplicateDialog) {
        AlertDialog(
            onDismissRequest = { showDuplicateDialog = false },
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null, tint = VaultAmber, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("Duplicate File Name", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "A hidden item named \"$name\" already exists in your Hidden Vault. What would you like to do?",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDuplicateDialog = false
                        val uniqueName = "$name (${System.currentTimeMillis().toString().takeLast(4)})"
                        performSave(viewModel, uniqueName, itemType, selectedUri, isMoveOperation)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HiddenPrimary)
                ) {
                    Text("Keep Both", color = Color.White)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showDuplicateDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                    TextButton(
                        onClick = {
                            showDuplicateDialog = false
                            duplicateItem?.let { viewModel.moveHiddenItemToTrash(it) }
                            performSave(viewModel, name, itemType, selectedUri, isMoveOperation)
                        }
                    ) {
                        Text("Replace", color = VaultRed)
                    }
                }
            },
            containerColor = HiddenVaultDarkBg
        )
    }
}

private fun performSave(
    viewModel: VaultViewModel,
    name: String,
    itemType: String,
    uri: Uri?,
    isMoveOperation: Boolean = true
) {
    viewModel.addHiddenItem(
        name = name,
        itemType = itemType,
        category = "Confidential",
        description = "Encrypted in Isolated Hidden Vault",
        tags = "Confidential, Encrypted",
        isFavorite = false,
        notes = "",
        sourceUri = uri,
        deleteSourceAfterVerification = isMoveOperation
    )
}
