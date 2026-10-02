package com.example.data.bulk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.StatFs
import android.provider.OpenableColumns
import com.example.data.local.AppDatabase
import com.example.data.model.Document
import com.example.data.model.HiddenItem
import com.example.data.model.PrivateMedia
import com.example.data.security.CryptoManager
import com.example.data.security.MediaPrivacyHelper
import com.example.data.security.ThumbnailManager
import com.example.data.security.VaultType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.UUID

enum class PhotoStorageMode(val title: String, val description: String) {
    ORIGINAL("Original Quality", "Lossless bit-for-bit encryption (Highest fidelity)"),
    BALANCED("Balanced", "Optimized compression (Saves ~30% device space)"),
    SAVER("Storage Saver", "Compressed to max 1920px (Saves up to ~65% space)")
}

enum class BulkImportStatus {
    IDLE,
    PREPARING,
    CONFIRMING,
    IMPORTING,
    PAUSED,
    LOW_STORAGE_PAUSED,
    COMPLETED,
    CANCELLED
}

data class FailedImportItem(
    val uri: Uri,
    val fileName: String,
    val reason: String
)

data class BulkImportPlan(
    val uris: List<Uri>,
    val targetVault: VaultType,
    val mediaType: String, // "PHOTO", "VIDEO", "DOCUMENT"
    val albumId: Long? = null,
    val category: String = "Personal",
    val totalFiles: Int = 0,
    val totalSizeBytes: Long = 0L,
    val availableStorageBytes: Long = 0L,
    val storageAfterImportBytes: Long = 0L,
    val isStorageSufficient: Boolean = true,
    val bytesShortage: Long = 0L,
    val storageMode: PhotoStorageMode = PhotoStorageMode.ORIGINAL,
    val skipDuplicates: Boolean = true,
    val isMoveOperation: Boolean = false
) {
    fun formatBytes(bytes: Long): String {
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
}

data class BulkImportState(
    val status: BulkImportStatus = BulkImportStatus.IDLE,
    val targetVault: VaultType = VaultType.PRIVATE,
    val mediaType: String = "PHOTO",
    val totalCount: Int = 0,
    val completedCount: Int = 0,
    val successCount: Int = 0,
    val failedCount: Int = 0,
    val skippedDuplicatesCount: Int = 0,
    val remainingCount: Int = 0,
    val currentFileName: String = "",
    val currentProgressRatio: Float = 0f,
    val totalSizeBytes: Long = 0L,
    val processedBytes: Long = 0L,
    val availableDeviceBytes: Long = 0L,
    val failedItems: List<FailedImportItem> = emptyList(),
    val errorMessage: String? = null,
    val plan: BulkImportPlan? = null
) {
    val isImporting: Boolean get() = status == BulkImportStatus.IMPORTING
    val isPaused: Boolean get() = status == BulkImportStatus.PAUSED || status == BulkImportStatus.LOW_STORAGE_PAUSED
    val isComplete: Boolean get() = status == BulkImportStatus.COMPLETED

    fun formatBytes(bytes: Long): String {
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
}

class BulkImportManager(
    private val context: Context,
    private val database: AppDatabase,
    private val cryptoManager: CryptoManager,
    private val thumbnailManager: ThumbnailManager
) {
    private val tempDir = File(context.filesDir, "temp_imports").apply { mkdirs(); try { File(this, ".nomedia").createNewFile() } catch (_: Throwable) {} }
    private val docsDir = File(context.filesDir, "vault_documents").apply { mkdirs(); try { File(this, ".nomedia").createNewFile() } catch (_: Throwable) {} }
    private val privateDir = File(context.filesDir, "vault_private").apply { mkdirs(); try { File(this, ".nomedia").createNewFile() } catch (_: Throwable) {} }
    private val hiddenDir = File(context.filesDir, "vault_hidden").apply { mkdirs(); try { File(this, ".nomedia").createNewFile() } catch (_: Throwable) {} }
    private val hiddenHiddenDir = File(context.filesDir, "vault_hidden_hidden").apply { mkdirs(); try { File(this, ".nomedia").createNewFile() } catch (_: Throwable) {} }

    private val _state = MutableStateFlow(BulkImportState())
    val state: StateFlow<BulkImportState> = _state.asStateFlow()

    private var currentJob: Job? = null
    private var isPauseRequested = false
    private val pauseLock = Object()

    // Concurrency control: max 4 stream workers simultaneously
    private val semaphore = Semaphore(4)

    // Store remaining URIs for resume capability
    private val remainingUrisQueue = mutableListOf<Uri>()

    /**
     * Step 1: Prepares the import plan asynchronously.
     * Computes total size, available storage, and warns if insufficient.
     */
    suspend fun preparePlan(
        uris: List<Uri>,
        targetVault: VaultType,
        mediaType: String,
        albumId: Long? = null,
        category: String = "Personal",
        storageMode: PhotoStorageMode = PhotoStorageMode.ORIGINAL,
        skipDuplicates: Boolean = true,
        isMoveOperation: Boolean = (targetVault == VaultType.HIDDEN || targetVault == VaultType.HIDDEN_HIDDEN)
    ): BulkImportPlan = withContext(Dispatchers.IO) {
        _state.value = _state.value.copy(status = BulkImportStatus.PREPARING)

        var totalSize = 0L
        uris.forEach { uri ->
            totalSize += getFileSize(uri)
        }

        val availableStorage = getAvailableDeviceStorage()
        val safetyBuffer = 100L * 1024 * 1024 // 100 MB buffer
        val required = totalSize + safetyBuffer
        val isSufficient = availableStorage >= required
        val shortage = if (isSufficient) 0L else (required - availableStorage)

        val plan = BulkImportPlan(
            uris = uris,
            targetVault = targetVault,
            mediaType = mediaType,
            albumId = albumId,
            category = category,
            totalFiles = uris.size,
            totalSizeBytes = totalSize,
            availableStorageBytes = availableStorage,
            storageAfterImportBytes = (availableStorage - totalSize).coerceAtLeast(0L),
            isStorageSufficient = isSufficient,
            bytesShortage = shortage,
            storageMode = storageMode,
            skipDuplicates = skipDuplicates,
            isMoveOperation = isMoveOperation
        )

        _state.value = _state.value.copy(
            status = BulkImportStatus.CONFIRMING,
            targetVault = targetVault,
            mediaType = mediaType,
            totalCount = uris.size,
            totalSizeBytes = totalSize,
            availableDeviceBytes = availableStorage,
            plan = plan
        )

        plan
    }

    /**
     * Step 2: Starts the streaming import process.
     */
    fun startImport(plan: BulkImportPlan, scope: CoroutineScope) {
        if (!plan.isStorageSufficient) {
            _state.value = _state.value.copy(
                status = BulkImportStatus.IDLE,
                errorMessage = "Not enough storage. Selected files: ${plan.formatBytes(plan.totalSizeBytes)}, Available: ${plan.formatBytes(plan.availableStorageBytes)}. Free up ${plan.formatBytes(plan.bytesShortage)}."
            )
            return
        }

        currentJob?.cancel()
        isPauseRequested = false
        remainingUrisQueue.clear()
        remainingUrisQueue.addAll(plan.uris)

        _state.value = _state.value.copy(
            status = BulkImportStatus.IMPORTING,
            targetVault = plan.targetVault,
            mediaType = plan.mediaType,
            totalCount = plan.uris.size,
            completedCount = 0,
            successCount = 0,
            failedCount = 0,
            skippedDuplicatesCount = 0,
            remainingCount = plan.uris.size,
            currentProgressRatio = 0f,
            totalSizeBytes = plan.totalSizeBytes,
            processedBytes = 0L,
            failedItems = emptyList(),
            errorMessage = null,
            plan = plan
        )

        currentJob = scope.launch(Dispatchers.IO) {
            executeStreamingImport(plan)
        }
    }

    /**
     * Pauses the ongoing import gracefully.
     */
    fun pauseImport() {
        if (_state.value.status == BulkImportStatus.IMPORTING) {
            isPauseRequested = true
            _state.value = _state.value.copy(status = BulkImportStatus.PAUSED)
        }
    }

    /**
     * Resumes the paused import.
     */
    fun resumeImport(scope: CoroutineScope) {
        if (_state.value.status == BulkImportStatus.PAUSED || _state.value.status == BulkImportStatus.LOW_STORAGE_PAUSED) {
            isPauseRequested = false
            synchronized(pauseLock) {
                pauseLock.notifyAll()
            }
            if (currentJob?.isActive != true) {
                val currentPlan = _state.value.plan
                if (currentPlan != null && remainingUrisQueue.isNotEmpty()) {
                    _state.value = _state.value.copy(status = BulkImportStatus.IMPORTING)
                    currentJob = scope.launch(Dispatchers.IO) {
                        executeStreamingImport(currentPlan)
                    }
                }
            } else {
                _state.value = _state.value.copy(status = BulkImportStatus.IMPORTING)
            }
        }
    }

    /**
     * Cancels the import and cleans up any partial temporary files.
     */
    fun cancelImport() {
        isPauseRequested = false
        currentJob?.cancel()
        remainingUrisQueue.clear()
        cleanupTempFiles()
        _state.value = _state.value.copy(
            status = BulkImportStatus.CANCELLED,
            currentFileName = "Import cancelled"
        )
    }

    /**
     * Dismisses the dialog when complete or idle.
     */
    fun resetState() {
        _state.value = BulkImportState()
    }

    /**
     * Retries only the failed items from previous run.
     */
    fun retryFailed(scope: CoroutineScope) {
        val currentPlan = _state.value.plan ?: return
        val failed = _state.value.failedItems.map { it.uri }
        if (failed.isEmpty()) return

        val retryPlan = currentPlan.copy(
            uris = failed,
            totalFiles = failed.size
        )
        startImport(retryPlan, scope)
    }

    private suspend fun executeStreamingImport(plan: BulkImportPlan) = withContext(Dispatchers.IO) {
        val failedList = mutableListOf<FailedImportItem>()
        var success = _state.value.successCount
        var failed = _state.value.failedCount
        var skipped = _state.value.skippedDuplicatesCount
        var completed = _state.value.completedCount
        var processedBytes = _state.value.processedBytes

        while (remainingUrisQueue.isNotEmpty()) {
            if (!isActive) break

            // Check if user requested pause
            while (isPauseRequested) {
                synchronized(pauseLock) {
                    try {
                        pauseLock.wait()
                    } catch (_: InterruptedException) {}
                }
            }

            // Low storage safety check: if < 50MB, stop safely!
            if (getAvailableDeviceStorage() < 50L * 1024 * 1024) {
                _state.value = _state.value.copy(
                    status = BulkImportStatus.LOW_STORAGE_PAUSED,
                    errorMessage = "SecureVault paused importing because device storage is almost full (< 50MB free)."
                )
                isPauseRequested = true
                continue
            }

            val uri = remainingUrisQueue.removeAt(0)
            val fileName = getFileName(uri)

            _state.value = _state.value.copy(
                currentFileName = fileName,
                remainingCount = remainingUrisQueue.size
            )

            try {
                semaphore.withPermit {
                    val result = processSingleFileStream(
                        uri = uri,
                        fileName = fileName,
                        targetVault = plan.targetVault,
                        mediaType = plan.mediaType,
                        albumId = plan.albumId,
                        category = plan.category,
                        storageMode = plan.storageMode,
                        skipDuplicates = plan.skipDuplicates
                    )

                    when (result) {
                        is SingleFileResult.Success -> {
                            success++
                            processedBytes += result.fileSizeBytes
                            // If user explicitly chose "Move" or importing to Hidden Vault, remove original public file from device gallery/storage and refresh MediaStore
                            if (plan.isMoveOperation) {
                                try {
                                    MediaPrivacyHelper.deletePublicMediaAndRefresh(context, uri)
                                } catch (_: Exception) {}
                            }
                        }
                        is SingleFileResult.DuplicateSkipped -> {
                            skipped++
                        }
                        is SingleFileResult.Failed -> {
                            failed++
                            failedList.add(FailedImportItem(uri, fileName, result.reason))
                        }
                    }
                }
            } catch (c: CancellationException) {
                // Restore URI to queue if cancelled mid-flight
                remainingUrisQueue.add(0, uri)
                throw c
            } catch (e: Exception) {
                failed++
                failedList.add(FailedImportItem(uri, fileName, e.message ?: "Import error"))
            }

            completed++
            val ratio = if (plan.totalFiles > 0) completed.toFloat() / plan.totalFiles else 0f

            _state.value = _state.value.copy(
                completedCount = completed,
                successCount = success,
                failedCount = failed,
                skippedDuplicatesCount = skipped,
                remainingCount = remainingUrisQueue.size,
                currentProgressRatio = ratio,
                processedBytes = processedBytes,
                failedItems = failedList.toList()
            )
        }

        cleanupTempFiles()

        _state.value = _state.value.copy(
            status = BulkImportStatus.COMPLETED,
            completedCount = completed,
            successCount = success,
            failedCount = failed,
            skippedDuplicatesCount = skipped,
            remainingCount = 0,
            currentProgressRatio = 1f,
            failedItems = failedList
        )
    }

    sealed class SingleFileResult {
        data class Success(val fileSizeBytes: Long) : SingleFileResult()
        object DuplicateSkipped : SingleFileResult()
        data class Failed(val reason: String) : SingleFileResult()
    }

    /**
     * Safe transactional import for a single file.
     * 1. Validate & open stream.
     * 2. Hash calculation on-the-fly with DigestOutputStream.
     * 3. Stream encrypt directly into .tmp file.
     * 4. Check duplicate hash against database.
     * 5. Atomically move .tmp to final destination file.
     * 6. Generate thumbnail directly.
     * 7. Insert database entity.
     * 8. Clean up .tmp if any failure occurs.
     */
    private suspend fun processSingleFileStream(
        uri: Uri,
        fileName: String,
        targetVault: VaultType,
        mediaType: String,
        albumId: Long?,
        category: String,
        storageMode: PhotoStorageMode,
        skipDuplicates: Boolean
    ): SingleFileResult = withContext(Dispatchers.IO) {
        val tempFile = File(tempDir, "temp_${UUID.randomUUID()}.tmp")

        try {
            val digest = MessageDigest.getInstance("SHA-256")

            val mimeType = try { context.contentResolver.getType(uri)?.lowercase() } catch (_: Exception) { null }
            val isVideoByMime = mimeType?.startsWith("video/") == true
            val isImageByMime = mimeType?.startsWith("image/") == true
            val isPdfByMime = mimeType == "application/pdf"

            val isVideo = isVideoByMime || isVideoFileName(fileName) || (mediaType == "VIDEO" && !isImageByMime)
            val isImage = !isVideo && (isImageByMime || isImageFileName(fileName) || (mediaType == "PHOTO" && !isVideoByMime))
            val isPdf = isPdfByMime || fileName.endsWith(".pdf", ignoreCase = true)

            val resolvedFileName = if (isVideo && !isVideoFileName(fileName)) {
                if (fileName.contains(".")) "${fileName.substringBeforeLast(".")}.mp4" else "$fileName.mp4"
            } else {
                fileName
            }

            val sourceStream: InputStream = if (isImage && !isVideo && storageMode != PhotoStorageMode.ORIGINAL) {
                // Compress image to balanced or saver quality in memory stream
                compressImageToStream(uri, storageMode) ?: (context.contentResolver.openInputStream(uri)
                    ?: return@withContext SingleFileResult.Failed("Unable to open image stream"))
            } else {
                context.contentResolver.openInputStream(uri)
                    ?: return@withContext SingleFileResult.Failed("File stream unavailable")
            }

            // Stream write encrypted content while hashing
            var writtenBytes = 0L
            val digestInput = java.security.DigestInputStream(sourceStream, digest)
            digestInput.use { hashedInput ->
                writtenBytes = cryptoManager.encryptToFile(
                    inputStream = hashedInput,
                    destinationFile = tempFile,
                    vaultType = targetVault
                )
            }

            val sha256Hex = digest.digest().joinToString("") { "%02x".format(it) }

            // Step 4: Duplicate Detection
            if (skipDuplicates && isDuplicateInVault(sha256Hex, targetVault)) {
                tempFile.delete()
                return@withContext SingleFileResult.DuplicateSkipped
            }

            // Step 5: Determine final destination file
            val targetDir = when (targetVault) {
                VaultType.MAIN -> docsDir
                VaultType.PRIVATE -> privateDir
                VaultType.HIDDEN -> hiddenDir
                VaultType.HIDDEN_HIDDEN -> hiddenHiddenDir
            }
            val extension = when {
                isVideo -> "enc_vid"
                isImage -> "enc_img"
                isPdf -> "enc_pdf"
                else -> "enc_doc"
            }
            val finalFile = File(targetDir, "${UUID.randomUUID()}.$extension")

            // Atomic rename
            if (!tempFile.renameTo(finalFile)) {
                // Fallback copy
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }

            val finalSize = finalFile.length()

            // Step 6: Generate thumbnail directly from source URI
            thumbnailManager.saveThumbnailDirectly(finalFile.absolutePath, uri, isVideo = isVideo)

            // Step 7: Insert Room database record
            when (targetVault) {
                VaultType.MAIN -> {
                    val docType = if (isPdf) "PDF" else if (isVideo) "VIDEO" else if (isImage) "IMAGE" else "OTHER"
                    val doc = Document(
                        name = resolvedFileName,
                        category = category,
                        description = if (isVideo) "Secure Video in Main Vault" else "Imported securely into Main Vault",
                        filePath = finalFile.absolutePath,
                        fileType = docType,
                        tags = if (isVideo) "Video, Encrypted" else "Secure, Encrypted",
                        fileSizeBytes = finalSize,
                        fileHash = sha256Hex
                    )
                    database.documentDao().insertDocument(doc)
                }
                VaultType.PRIVATE -> {
                    val pMedia = PrivateMedia(
                        name = resolvedFileName,
                        encryptedPath = finalFile.absolutePath,
                        mediaType = if (isVideo) "VIDEO" else "PHOTO",
                        albumId = albumId,
                        fileSizeBytes = finalSize,
                        fileHash = sha256Hex
                    )
                    database.privateMediaDao().insertMedia(pMedia)
                }
                VaultType.HIDDEN -> {
                    val hItem = HiddenItem(
                        name = resolvedFileName,
                        itemType = if (isVideo) "VIDEO" else if (isImage) "PHOTO" else if (isPdf) "PDF" else "DOCUMENT",
                        encryptedPath = finalFile.absolutePath,
                        category = category,
                        albumId = albumId,
                        description = if (isVideo) "Confidential Video in Hidden Vault" else "Imported into Hidden Vault",
                        tags = if (isVideo) "Video, Confidential, Encrypted" else "Confidential, Encrypted",
                        fileSizeBytes = finalSize,
                        fileHash = sha256Hex
                    )
                    database.hiddenDao().insertHiddenItem(hItem)
                }
                VaultType.HIDDEN_HIDDEN -> {
                    val hhItem = com.example.data.model.HiddenHiddenItem(
                        name = resolvedFileName,
                        itemType = if (isVideo) "VIDEO" else "PHOTO",
                        encryptedPath = finalFile.absolutePath,
                        fileSizeBytes = finalSize,
                        fileHash = sha256Hex
                    )
                    database.hiddenHiddenDao().insertItem(hhItem)
                }
            }

            SingleFileResult.Success(finalSize)
        } catch (e: Exception) {
            tempFile.delete()
            SingleFileResult.Failed(e.message ?: "Failed writing secure file")
        }
    }

    private suspend fun isDuplicateInVault(hash: String, vaultType: VaultType): Boolean {
        return when (vaultType) {
            VaultType.MAIN -> database.documentDao().getDocumentByHash(hash) != null
            VaultType.PRIVATE -> database.privateMediaDao().getMediaByHash(hash) != null
            VaultType.HIDDEN -> database.hiddenDao().getHiddenItemByHash(hash) != null
            VaultType.HIDDEN_HIDDEN -> database.hiddenHiddenDao().getItemByHash(hash) != null
        }
    }

    private fun compressImageToStream(uri: Uri, mode: PhotoStorageMode): InputStream? {
        return try {
            val maxDimension = if (mode == PhotoStorageMode.SAVER) 1920 else 2560
            val quality = if (mode == PhotoStorageMode.SAVER) 70 else 85

            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, boundsOpts)
            }

            var sample = 1
            while ((boundsOpts.outWidth / sample) > maxDimension || (boundsOpts.outHeight / sample) > maxDimension) {
                sample *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return null

            val outStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outStream)
            bitmap.recycle()
            ByteArrayInputStream(outStream.toByteArray())
        } catch (e: Exception) {
            null
        }
    }

    private fun isImageFileName(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
                lower.endsWith(".webp") || lower.endsWith(".heic") || lower.endsWith(".gif") ||
                lower.endsWith(".bmp") || lower.endsWith(".svg")
    }

    private fun isVideoFileName(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".mkv") ||
                lower.endsWith(".avi") || lower.endsWith(".3gp") || lower.endsWith(".webm") ||
                lower.endsWith(".m4v") || lower.endsWith(".flv") || lower.endsWith(".ts") ||
                lower.endsWith(".wmv") || lower.endsWith(".mpg") || lower.endsWith(".mpeg")
    }

    private fun getFileSize(uri: Uri): Long {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1 && cursor.moveToFirst()) {
                    cursor.getLong(sizeIndex)
                } else 0L
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun getFileName(uri: Uri): String {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else null
            } ?: uri.lastPathSegment ?: "media_${System.currentTimeMillis()}"
        } catch (e: Exception) {
            "media_${System.currentTimeMillis()}"
        }
    }

    fun getAvailableDeviceStorage(): Long {
        return try {
            val stat = StatFs(context.filesDir.absolutePath)
            stat.availableBytes
        } catch (e: Exception) {
            0L
        }
    }

    fun getTotalDeviceStorage(): Long {
        return try {
            val stat = StatFs(context.filesDir.absolutePath)
            stat.totalBytes
        } catch (e: Exception) {
            0L
        }
    }

    private fun cleanupTempFiles() {
        try {
            tempDir.listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {}
    }
}
