package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.StatFs
import com.example.data.local.AppDatabase
import com.example.data.model.Album
import com.example.data.model.Document
import com.example.data.model.HiddenAlbum
import com.example.data.model.HiddenItem
import com.example.data.model.HiddenTrashItem
import com.example.data.model.PrivateMedia
import com.example.data.model.SecurityActivityLog
import com.example.data.model.TrashItem
import com.example.data.security.CryptoManager
import com.example.data.security.MediaPrivacyHelper
import com.example.data.security.OcrHelper
import com.example.data.security.ThumbnailManager
import com.example.data.security.VaultType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class StorageInfo(
    val docsSizeBytes: Long = 0L,
    val pdfsSizeBytes: Long = 0L,
    val photosSizeBytes: Long = 0L,
    val videosSizeBytes: Long = 0L,
    val hiddenDocsSizeBytes: Long = 0L,
    val hiddenPhotosSizeBytes: Long = 0L,
    val hiddenVideosSizeBytes: Long = 0L,
    val thumbnailsSizeBytes: Long = 0L,
    val cacheSizeBytes: Long = 0L,
    val totalVaultSizeBytes: Long = 0L,
    val totalDeviceSizeBytes: Long = 0L,
    val availableDeviceSizeBytes: Long = 0L
) {
    val totalSizeBytes: Long get() = totalVaultSizeBytes
    val hiddenSizeBytes: Long get() = hiddenDocsSizeBytes + hiddenPhotosSizeBytes + hiddenVideosSizeBytes
    val hiddenTotalSizeBytes: Long get() = hiddenSizeBytes
    val privateSizeBytes: Long get() = photosSizeBytes + videosSizeBytes

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

class VaultRepository(
    private val context: Context,
    private val database: AppDatabase,
    val cryptoManager: CryptoManager
) {
    private val docDao = database.documentDao()
    private val mediaDao = database.privateMediaDao()
    private val albumDao = database.albumDao()
    private val trashDao = database.trashDao()
    private val hiddenDao = database.hiddenDao()
    private val securityLogDao = database.securityLogDao()
    val thumbnailManager = ThumbnailManager(context, cryptoManager)

    private val docsDir = File(context.filesDir, "vault_docs").apply { mkdirs(); try { File(this, ".nomedia").createNewFile() } catch (_: Throwable) {} }
    private val privateDir = File(context.filesDir, "vault_private").apply { mkdirs(); try { File(this, ".nomedia").createNewFile() } catch (_: Throwable) {} }
    private val hiddenDir = File(context.filesDir, "vault_hidden").apply { mkdirs(); try { File(this, ".nomedia").createNewFile() } catch (_: Throwable) {} }
    private val hiddenHiddenDir = File(context.filesDir, "vault_hidden_hidden").apply { mkdirs(); try { File(this, ".nomedia").createNewFile() } catch (_: Throwable) {} }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                MediaPrivacyHelper.sanitizeExistingHiddenVaultStorage(context, hiddenDao)
            } catch (e: Exception) {
                android.util.Log.e("VaultRepository", "Sanitization error: ${e.message}")
            }
        }
    }

    // ==========================================
    // 1. MAIN VAULT FLOWS
    // ==========================================
    val allDocuments: Flow<List<Document>> = docDao.getAllDocuments()
    val favoriteDocuments: Flow<List<Document>> = docDao.getFavoriteDocuments()
    val totalDocumentsCount: Flow<Int> = docDao.getDocumentsCount()
    val pdfCount: Flow<Int> = docDao.getPdfCount()
    val imageCount: Flow<Int> = docDao.getImageCount()

    // ==========================================
    // 2. PRIVATE PHOTO VAULT FLOWS
    // ==========================================
    val allPrivateMedia: Flow<List<PrivateMedia>> = mediaDao.getAllMedia()
    val favoriteMedia: Flow<List<PrivateMedia>> = mediaDao.getFavoriteMedia()
    val totalPhotosCount: Flow<Int> = mediaDao.getPhotosCount()
    val totalVideosCount: Flow<Int> = mediaDao.getVideosCount()
    val allAlbums: Flow<List<Album>> = albumDao.getAllAlbums()
    val allTrashItems: Flow<List<TrashItem>> = trashDao.getAllTrashItems()

    // ==========================================
    // 3. HIDDEN VAULT FLOWS (ISOLATED)
    // ==========================================
    val allHiddenItems: Flow<List<HiddenItem>> = hiddenDao.getAllHiddenItems()
    val hiddenPhotos: Flow<List<HiddenItem>> = hiddenDao.getHiddenPhotos()
    val hiddenVideos: Flow<List<HiddenItem>> = hiddenDao.getHiddenVideos()
    val hiddenDocuments: Flow<List<HiddenItem>> = hiddenDao.getHiddenDocuments()
    val favoriteHiddenItems: Flow<List<HiddenItem>> = hiddenDao.getFavoriteHiddenItems()
    val recentlyOpenedItems: Flow<List<HiddenItem>> = hiddenDao.getRecentlyOpenedItems()
    val allHiddenAlbums: Flow<List<HiddenAlbum>> = hiddenDao.getAllHiddenAlbums()
    val allHiddenTrashItems: Flow<List<HiddenTrashItem>> = hiddenDao.getAllHiddenTrashItems()

    val hiddenTotalCount: Flow<Int> = hiddenDao.getHiddenTotalCount()
    val hiddenPhotosCount: Flow<Int> = hiddenDao.getHiddenPhotosCount()
    val hiddenVideosCount: Flow<Int> = hiddenDao.getHiddenVideosCount()
    val hiddenDocsCount: Flow<Int> = hiddenDao.getHiddenDocsCount()
    val hiddenAlbumsCount: Flow<Int> = hiddenDao.getHiddenAlbumsCount()
    val hiddenTrashCount: Flow<Int> = hiddenDao.getHiddenTrashCount()

    val hiddenPhotosSizeBytes: Flow<Long> = hiddenDao.getHiddenPhotosSizeBytes()
    val hiddenVideosSizeBytes: Flow<Long> = hiddenDao.getHiddenVideosSizeBytes()
    val hiddenDocsSizeBytes: Flow<Long> = hiddenDao.getHiddenDocsSizeBytes()

    // Security Activity Logs
    val allSecurityLogs: Flow<List<SecurityActivityLog>> = securityLogDao.getRecentLogs()

    suspend fun logSecurityEvent(action: String, details: String) = withContext(Dispatchers.IO) {
        securityLogDao.insertLog(
            SecurityActivityLog(
                action = action,
                details = details,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearSecurityLogs() = withContext(Dispatchers.IO) {
        securityLogDao.clearAllLogs()
    }

    // ==========================================
    // MAIN VAULT METHODS
    // ==========================================
    fun getDocumentsByCategory(category: String): Flow<List<Document>> =
        docDao.getDocumentsByCategory(category)

    fun searchDocuments(query: String): Flow<List<Document>> =
        docDao.searchDocuments(query)

    fun getExpiringSoonDocuments(now: Long, futureLimit: Long): Flow<List<Document>> =
        docDao.getExpiringSoonDocuments(now, futureLimit)

    fun getExpiredDocuments(now: Long): Flow<List<Document>> =
        docDao.getExpiredDocuments(now)

    fun getMediaByAlbum(albumId: Long): Flow<List<PrivateMedia>> =
        mediaDao.getMediaByAlbum(albumId)

    fun getUnorganizedMedia(): Flow<List<PrivateMedia>> =
        mediaDao.getMediaUnorganized()

    suspend fun getDocumentById(id: Long): Document? = docDao.getDocumentById(id)
    suspend fun getMediaById(id: Long): PrivateMedia? = mediaDao.getMediaById(id)

    suspend fun addDocument(
        name: String,
        category: String,
        description: String,
        fileType: String,
        tags: String,
        expiryDate: Long?,
        isFavorite: Boolean,
        sourceUri: Uri?,
        rawBytes: ByteArray? = null,
        extractedOcr: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val fileName = "doc_${UUID.randomUUID()}.${if (fileType == "PDF") "enc_pdf" else "enc_img"}"
        val destFile = File(docsDir, fileName)

        var finalOcr = extractedOcr ?: ""
        var bytesLength = 0L

        if (rawBytes != null) {
            bytesLength = cryptoManager.encryptBytesToFile(rawBytes, destFile, VaultType.MAIN)
            if (finalOcr.isBlank()) {
                val ocrResult = OcrHelper.analyzeDocument(name, category)
                finalOcr = ocrResult.rawText
            }
        } else if (sourceUri != null) {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                bytesLength = cryptoManager.encryptToFile(input, destFile, VaultType.MAIN)
            }
            if (finalOcr.isBlank()) {
                val ocrResult = OcrHelper.analyzeDocument(name, category)
                finalOcr = ocrResult.rawText
            }
        } else {
            throw IllegalArgumentException("Cannot add document without valid file data or URI")
        }

        val doc = Document(
            name = name,
            category = category,
            description = description,
            filePath = destFile.absolutePath,
            fileType = fileType,
            tags = tags,
            expiryDate = expiryDate,
            isFavorite = isFavorite,
            ocrText = finalOcr,
            fileSizeBytes = bytesLength
        )
        val id = docDao.insertDocument(doc)
        logSecurityEvent("FILE_IMPORTED", "Document added to Main Vault: $name")
        id
    }

    suspend fun updateDocument(document: Document) = withContext(Dispatchers.IO) {
        docDao.updateDocument(document.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun toggleDocumentFavorite(document: Document) = withContext(Dispatchers.IO) {
        docDao.updateDocument(document.copy(isFavorite = !document.isFavorite))
    }

    suspend fun moveDocumentToTrash(document: Document) = withContext(Dispatchers.IO) {
        trashDao.insertTrashItem(
            TrashItem(
                originalId = document.id,
                name = document.name,
                itemType = "DOCUMENT",
                filePath = document.filePath,
                metadataJson = "Category: ${document.category}, Tags: ${document.tags}"
            )
        )
        docDao.deleteDocument(document)
        logSecurityEvent("FILE_DELETED", "Moved document to Trash: ${document.name}")
    }

    suspend fun restoreTrashItem(trashItem: TrashItem) = withContext(Dispatchers.IO) {
        if (trashItem.itemType == "DOCUMENT") {
            val restored = Document(
                id = trashItem.originalId,
                name = trashItem.name,
                category = "Restored",
                filePath = trashItem.filePath,
                fileType = if (trashItem.filePath.endsWith("enc_pdf")) "PDF" else "IMAGE"
            )
            docDao.insertDocument(restored)
        } else if (trashItem.itemType == "PRIVATE_MEDIA") {
            val restored = PrivateMedia(
                id = trashItem.originalId,
                name = trashItem.name,
                encryptedPath = trashItem.filePath,
                mediaType = "PHOTO"
            )
            mediaDao.insertMedia(restored)
        }
        trashDao.deleteTrashItem(trashItem)
        logSecurityEvent("FILE_RESTORED", "Restored from Trash: ${trashItem.name}")
    }

    suspend fun permanentlyDeleteTrashItem(trashItem: TrashItem) = withContext(Dispatchers.IO) {
        val file = File(trashItem.filePath)
        cryptoManager.secureDeleteFile(file)
        trashDao.deleteTrashItem(trashItem)
        logSecurityEvent("PERMANENT_DELETE", "Permanently shredded file: ${trashItem.name}")
    }

    suspend fun clearAllTrashPermanently() = withContext(Dispatchers.IO) {
        trashDao.clearAllTrash()
        logSecurityEvent("EMPTY_TRASH", "All Trash bin items erased")
    }

    // ==========================================
    // PRIVATE PHOTO VAULT METHODS
    // ==========================================
    suspend fun addPrivateMedia(
        name: String,
        mediaType: String,
        albumId: Long?,
        sourceUri: Uri? = null,
        rawBytes: ByteArray? = null
    ): Long = withContext(Dispatchers.IO) {
        val mimeType = if (sourceUri != null) try { context.contentResolver.getType(sourceUri)?.lowercase() } catch (_: Exception) { null } else null
        val isVideo = mediaType == "VIDEO" || mimeType?.startsWith("video/") == true || name.lowercase().let {
            it.endsWith(".mp4") || it.endsWith(".mov") || it.endsWith(".mkv") || it.endsWith(".webm") || it.endsWith(".3gp")
        }
        val finalMediaType = if (isVideo) "VIDEO" else mediaType
        val finalName = if (isVideo && !name.lowercase().let { it.endsWith(".mp4") || it.endsWith(".mov") || it.endsWith(".mkv") || it.endsWith(".webm") }) "$name.mp4" else name
        val ext = if (isVideo) "enc_vid" else "enc_img"
        val fileName = "priv_${UUID.randomUUID()}.$ext"
        val destFile = File(privateDir, fileName)
        var bytesLength = 0L

        if (rawBytes != null) {
            bytesLength = cryptoManager.encryptBytesToFile(rawBytes, destFile, VaultType.PRIVATE)
        } else if (sourceUri != null) {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                bytesLength = cryptoManager.encryptToFile(input, destFile, VaultType.PRIVATE)
            }
            try {
                thumbnailManager.saveThumbnailDirectly(destFile.absolutePath, sourceUri, isVideo = isVideo)
            } catch (_: Exception) {}
        } else {
            throw IllegalArgumentException("Cannot add private media without valid file data or URI")
        }

        val media = PrivateMedia(
            name = finalName,
            encryptedPath = destFile.absolutePath,
            mediaType = finalMediaType,
            albumId = albumId,
            fileSizeBytes = bytesLength
        )
        val id = mediaDao.insertMedia(media)
        logSecurityEvent("FILE_IMPORTED", "Private Media imported: $finalName ($finalMediaType)")
        id
    }

    suspend fun toggleMediaFavorite(media: PrivateMedia) = withContext(Dispatchers.IO) {
        mediaDao.updateMedia(media.copy(isFavorite = !media.isFavorite))
    }

    suspend fun renameMedia(media: PrivateMedia, newName: String) = withContext(Dispatchers.IO) {
        mediaDao.updateMedia(media.copy(name = newName))
    }

    suspend fun moveMediaToAlbum(mediaIds: List<Long>, albumId: Long?) = withContext(Dispatchers.IO) {
        mediaDao.moveMediaToAlbum(mediaIds, albumId)
    }

    suspend fun moveMediaToTrash(media: PrivateMedia) = withContext(Dispatchers.IO) {
        trashDao.insertTrashItem(
            TrashItem(
                originalId = media.id,
                name = media.name,
                itemType = "PRIVATE_MEDIA",
                filePath = media.encryptedPath,
                metadataJson = "Type: ${media.mediaType}"
            )
        )
        mediaDao.deleteMedia(media)
        logSecurityEvent("FILE_DELETED", "Moved Private Media to Trash: ${media.name}")
    }

    suspend fun createAlbum(name: String, iconName: String = "folder"): Long = withContext(Dispatchers.IO) {
        albumDao.insertAlbum(Album(name = name, iconName = iconName))
    }

    suspend fun deleteAlbum(album: Album) = withContext(Dispatchers.IO) {
        albumDao.deleteAlbum(album)
    }

    // ==========================================
    // 3. HIDDEN VAULT METHODS (STRICTLY ISOLATED)
    // ==========================================
    suspend fun checkDuplicateHiddenItem(name: String): HiddenItem? = withContext(Dispatchers.IO) {
        hiddenDao.findItemByName(name.trim())
    }

    fun searchHiddenItems(query: String): Flow<List<HiddenItem>> =
        hiddenDao.searchHiddenItems(query)

    suspend fun getHiddenItemById(id: Long): HiddenItem? = hiddenDao.getHiddenItemById(id)

    suspend fun addHiddenItem(
        name: String,
        itemType: String, // "PHOTO", "VIDEO", "DOCUMENT", "PDF", "SCAN"
        category: String = "Confidential",
        albumId: Long? = null,
        description: String = "",
        tags: String = "",
        expiryDate: Long? = null,
        isFavorite: Boolean = false,
        notes: String = "",
        sourceUri: Uri? = null,
        rawBytes: ByteArray? = null,
        extractedOcr: String? = null,
        deleteSourceAfterVerification: Boolean = false
    ): Long = withContext(Dispatchers.IO) {
        val mimeType = if (sourceUri != null) try { context.contentResolver.getType(sourceUri)?.lowercase() } catch (_: Exception) { null } else null
        val isVideo = itemType == "VIDEO" || mimeType?.startsWith("video/") == true || name.lowercase().let {
            it.endsWith(".mp4") || it.endsWith(".mov") || it.endsWith(".mkv") || it.endsWith(".webm") || it.endsWith(".3gp")
        }
        val isPdf = itemType == "PDF" || mimeType == "application/pdf" || name.endsWith(".pdf", ignoreCase = true)
        val finalItemType = when {
            isVideo -> "VIDEO"
            isPdf -> "PDF"
            itemType in listOf("DOCUMENT", "SCAN") -> itemType
            itemType == "PHOTO" || mimeType?.startsWith("image/") == true -> "PHOTO"
            else -> itemType
        }
        val finalName = if (isVideo && !name.lowercase().let { it.endsWith(".mp4") || it.endsWith(".mov") || it.endsWith(".mkv") || it.endsWith(".webm") }) "$name.mp4" else name
        val ext = when (finalItemType) {
            "PDF" -> "enc_pdf"
            "VIDEO" -> "enc_vid"
            "PHOTO" -> "enc_img"
            else -> "enc_doc"
        }
        val fileName = "hidden_${UUID.randomUUID()}.$ext"
        val destFile = File(hiddenDir, fileName)

        var finalOcr = extractedOcr ?: ""
        var bytesLength = 0L

        if (rawBytes != null) {
            bytesLength = cryptoManager.encryptBytesToFile(rawBytes, destFile, VaultType.HIDDEN)
            if (finalOcr.isBlank() && finalItemType in listOf("DOCUMENT", "PDF", "SCAN")) {
                val ocrResult = OcrHelper.analyzeDocument(finalName, category)
                finalOcr = ocrResult.rawText
            }
        } else if (sourceUri != null) {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                bytesLength = cryptoManager.encryptToFile(input, destFile, VaultType.HIDDEN)
            }
            if (finalOcr.isBlank() && finalItemType in listOf("DOCUMENT", "PDF", "SCAN")) {
                val ocrResult = OcrHelper.analyzeDocument(finalName, category)
                finalOcr = ocrResult.rawText
            }
        } else {
            throw IllegalArgumentException("Cannot add hidden item without valid file data or URI")
        }

        if (sourceUri != null) {
            thumbnailManager.saveThumbnailDirectly(destFile.absolutePath, sourceUri, isVideo = isVideo)
        }

        val item = HiddenItem(
            name = finalName,
            itemType = finalItemType,
            encryptedPath = destFile.absolutePath,
            category = category,
            albumId = albumId,
            description = if (isVideo) "Confidential Video in Hidden Vault" else description,
            tags = if (isVideo) "Video, Confidential, Encrypted" else tags,
            isFavorite = isFavorite,
            fileSizeBytes = bytesLength,
            ocrText = finalOcr,
            expiryDate = expiryDate,
            notes = notes
        )
        val id = hiddenDao.insertHiddenItem(item)
        logSecurityEvent("HIDDEN_FILE_IMPORTED", "Imported into Hidden Vault: $finalName ($finalItemType)")

        // If user chose "Move to Hidden Vault", verify private copy exists and has content before deleting source
        if (deleteSourceAfterVerification && sourceUri != null) {
            if (destFile.exists() && destFile.length() > 0 && id > 0) {
                try {
                    val removed = MediaPrivacyHelper.deletePublicMediaAndRefresh(context, sourceUri)
                    if (removed) {
                        logSecurityEvent("PUBLIC_MEDIA_REMOVED", "Original public media deleted after verified move: $finalName")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("VaultRepository", "Could not delete original source: ${e.message}")
                }
            }
        }

        id
    }

    // Move single media from Private Photo Vault (normal gallery) to Hidden Vault
    suspend fun movePrivateMediaToHiddenVault(media: PrivateMedia): Boolean = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(media.encryptedPath)
            if (!sourceFile.exists()) return@withContext false

            val ext = if (media.mediaType == "VIDEO") "enc_vid" else "enc_img"
            val destFile = File(hiddenDir, "hidden_${UUID.randomUUID()}.$ext")

            // Re-encrypt from private vault key to isolated hidden vault key
            val decryptedBytes = cryptoManager.decryptFileToBytes(sourceFile, VaultType.PRIVATE)
            if (decryptedBytes == null || decryptedBytes.isEmpty()) return@withContext false

            val bytesWritten = cryptoManager.encryptBytesToFile(decryptedBytes, destFile, VaultType.HIDDEN)

            // Step 2: VERIFY that the private copy was successfully created
            if (!destFile.exists() || destFile.length() <= 0 || bytesWritten <= 0) {
                destFile.delete()
                return@withContext false
            }

            // Copy thumbnail directly if exists
            thumbnailManager.saveThumbnailDirectly(destFile.absolutePath, Uri.fromFile(destFile), isVideo = media.mediaType == "VIDEO")

            val hiddenItem = HiddenItem(
                name = media.name,
                itemType = if (media.mediaType == "VIDEO") "VIDEO" else "PHOTO",
                encryptedPath = destFile.absolutePath,
                category = "Moved from Gallery",
                albumId = null,
                description = "Moved from Normal Gallery to Isolated Hidden Vault",
                tags = "Private, Moved, Encrypted",
                isFavorite = media.isFavorite,
                fileSizeBytes = bytesWritten
            )
            val newId = hiddenDao.insertHiddenItem(hiddenItem)

            if (newId <= 0) {
                destFile.delete()
                return@withContext false
            }

            // Step 3: ONLY THEN remove the normal/public copy from Private Gallery
            mediaDao.deleteMedia(media)
            sourceFile.delete()
            logSecurityEvent("MEDIA_MOVED_TO_HIDDEN", "Moved from normal gallery to Hidden Vault: ${media.name}")
            true
        } catch (e: Exception) {
            android.util.Log.e("VaultRepository", "Error moving media to hidden vault: ${e.message}")
            false
        }
    }

    // Batch move media from Private Photo Vault (normal gallery) to Hidden Vault
    suspend fun batchMovePrivateMediaToHiddenVault(mediaList: List<PrivateMedia>): Int = withContext(Dispatchers.IO) {
        var count = 0
        for (media in mediaList) {
            if (movePrivateMediaToHiddenVault(media)) {
                count++
            }
        }
        count
    }

    // Move Document from Main Vault to Hidden Vault
    suspend fun moveDocumentToHiddenVault(document: Document): Boolean = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(document.filePath)
            if (!sourceFile.exists()) return@withContext false

            val isVideo = document.fileType == "VIDEO"
            val isPdf = document.fileType == "PDF"
            val isImage = document.fileType == "IMAGE"
            val ext = when {
                isVideo -> "enc_vid"
                isPdf -> "enc_pdf"
                isImage -> "enc_img"
                else -> "enc_doc"
            }
            val destFile = File(hiddenDir, "hidden_${UUID.randomUUID()}.$ext")

            val decryptedBytes = cryptoManager.decryptFileToBytes(sourceFile, VaultType.MAIN)
            if (decryptedBytes == null || decryptedBytes.isEmpty()) return@withContext false

            val bytesWritten = cryptoManager.encryptBytesToFile(decryptedBytes, destFile, VaultType.HIDDEN)

            // Step 2: VERIFY private copy exists
            if (!destFile.exists() || destFile.length() <= 0 || bytesWritten <= 0) {
                destFile.delete()
                return@withContext false
            }

            val hiddenItem = HiddenItem(
                name = document.name,
                itemType = when {
                    isVideo -> "VIDEO"
                    isPdf -> "PDF"
                    isImage -> "PHOTO"
                    else -> "DOCUMENT"
                },
                encryptedPath = destFile.absolutePath,
                category = document.category,
                description = document.description,
                tags = document.tags,
                isFavorite = document.isFavorite,
                fileSizeBytes = bytesWritten,
                ocrText = document.ocrText
            )
            val newId = hiddenDao.insertHiddenItem(hiddenItem)

            if (newId <= 0) {
                destFile.delete()
                return@withContext false
            }

            // Step 3: ONLY THEN remove copy from Main Vault
            docDao.deleteDocument(document)
            sourceFile.delete()
            logSecurityEvent("DOCUMENT_MOVED_TO_HIDDEN", "Moved document to Hidden Vault: ${document.name}")
            true
        } catch (e: Exception) {
            android.util.Log.e("VaultRepository", "Error moving document to hidden vault: ${e.message}")
            false
        }
    }

    suspend fun updateHiddenItem(item: HiddenItem) = withContext(Dispatchers.IO) {
        hiddenDao.updateHiddenItem(item.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun recordHiddenItemOpened(item: HiddenItem) = withContext(Dispatchers.IO) {
        hiddenDao.updateHiddenItem(item.copy(lastOpenedAt = System.currentTimeMillis()))
    }

    suspend fun toggleHiddenFavorite(item: HiddenItem) = withContext(Dispatchers.IO) {
        hiddenDao.updateHiddenItem(item.copy(isFavorite = !item.isFavorite))
    }

    suspend fun moveHiddenItemToTrash(item: HiddenItem) = withContext(Dispatchers.IO) {
        hiddenDao.insertHiddenTrashItem(
            HiddenTrashItem(
                originalId = item.id,
                name = item.name,
                itemType = item.itemType,
                encryptedPath = item.encryptedPath,
                metadataJson = "Category: ${item.category}, Tags: ${item.tags}"
            )
        )
        hiddenDao.deleteHiddenItem(item)
        logSecurityEvent("HIDDEN_FILE_TRASHED", "Moved to Hidden Trash: ${item.name}")
    }

    suspend fun restoreHiddenTrashItem(trashItem: HiddenTrashItem) = withContext(Dispatchers.IO) {
        val restored = HiddenItem(
            id = trashItem.originalId,
            name = trashItem.name,
            itemType = trashItem.itemType,
            encryptedPath = trashItem.encryptedPath,
            category = "Restored"
        )
        hiddenDao.insertHiddenItem(restored)
        hiddenDao.deleteHiddenTrashItem(trashItem)
        logSecurityEvent("HIDDEN_FILE_RESTORED", "Restored from Hidden Trash: ${trashItem.name}")
    }

    suspend fun permanentlyDeleteHiddenTrashItem(trashItem: HiddenTrashItem) = withContext(Dispatchers.IO) {
        val file = File(trashItem.encryptedPath)
        cryptoManager.secureDeleteFile(file)
        hiddenDao.deleteHiddenTrashItem(trashItem)
        logSecurityEvent("HIDDEN_PERMANENT_DELETE", "Cryptographically shredded hidden file: ${trashItem.name}")
    }

    suspend fun clearAllHiddenTrashPermanently() = withContext(Dispatchers.IO) {
        hiddenDao.clearAllHiddenTrash()
        logSecurityEvent("EMPTY_HIDDEN_TRASH", "All Hidden Trash erased")
    }

    // Hidden Batch Actions
    suspend fun batchDeleteHiddenItems(items: List<HiddenItem>) = withContext(Dispatchers.IO) {
        items.forEach { moveHiddenItemToTrash(it) }
    }

    suspend fun batchFavoriteHiddenItems(items: List<HiddenItem>, favorite: Boolean) = withContext(Dispatchers.IO) {
        items.forEach { hiddenDao.updateHiddenItem(it.copy(isFavorite = favorite)) }
    }

    suspend fun batchMoveHiddenItems(ids: List<Long>, albumId: Long?) = withContext(Dispatchers.IO) {
        ids.forEach { id ->
            val item = hiddenDao.getHiddenItemById(id)
            if (item != null) {
                hiddenDao.updateHiddenItem(item.copy(albumId = albumId))
            }
        }
    }

    // Hidden Albums
    suspend fun createHiddenAlbum(name: String, category: String = "Personal"): Long = withContext(Dispatchers.IO) {
        hiddenDao.insertHiddenAlbum(HiddenAlbum(name = name, category = category))
    }

    suspend fun deleteHiddenAlbum(album: HiddenAlbum) = withContext(Dispatchers.IO) {
        hiddenDao.deleteHiddenAlbum(album)
    }

    // Real Storage Breakdown Helper
    suspend fun getStorageBreakdown(): StorageInfo = withContext(Dispatchers.IO) {
        var docsBytes = 0L
        var pdfsBytes = 0L
        docsDir.listFiles()?.forEach { f ->
            val len = f.length()
            if (f.name.endsWith("enc_pdf")) {
                pdfsBytes += len
            }
            docsBytes += len
        }

        var privPhotosBytes = 0L
        var privVideosBytes = 0L
        privateDir.listFiles()?.forEach { f ->
            val len = f.length()
            if (f.name.endsWith("enc_vid")) {
                privVideosBytes += len
            } else {
                privPhotosBytes += len
            }
        }

        var hDocsBytes = 0L
        var hPhotosBytes = 0L
        var hVideosBytes = 0L
        hiddenDir.listFiles()?.forEach { f ->
            val len = f.length()
            when {
                f.name.endsWith("enc_vid") -> hVideosBytes += len
                f.name.endsWith("enc_img") -> hPhotosBytes += len
                else -> hDocsBytes += len
            }
        }

        val thumbBytes = thumbnailManager.getThumbnailSizeBytes()
        val cacheBytes = context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

        var totalDeviceBytes = 0L
        var availableDeviceBytes = 0L
        try {
            val stat = StatFs(context.filesDir.absolutePath)
            totalDeviceBytes = stat.totalBytes
            availableDeviceBytes = stat.availableBytes
        } catch (_: Exception) {}

        val vaultTotal = docsBytes + privPhotosBytes + privVideosBytes + hDocsBytes + hPhotosBytes + hVideosBytes

        StorageInfo(
            docsSizeBytes = docsBytes,
            pdfsSizeBytes = pdfsBytes,
            photosSizeBytes = privPhotosBytes,
            videosSizeBytes = privVideosBytes,
            hiddenDocsSizeBytes = hDocsBytes,
            hiddenPhotosSizeBytes = hPhotosBytes,
            hiddenVideosSizeBytes = hVideosBytes,
            thumbnailsSizeBytes = thumbBytes,
            cacheSizeBytes = cacheBytes,
            totalVaultSizeBytes = vaultTotal,
            totalDeviceSizeBytes = totalDeviceBytes,
            availableDeviceSizeBytes = availableDeviceBytes
        )
    }

    /**
     * Checks if device has enough available storage before large imports.
     */
    fun hasAvailableStorage(bytesNeeded: Long): Boolean {
        return try {
            val stat = StatFs(context.filesDir.absolutePath)
            stat.availableBytes >= bytesNeeded
        } catch (e: Exception) {
            true
        }
    }

    /**
     * Clears temporary exports and thumbnail cache.
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        cryptoManager.clearTempExports()
        thumbnailManager.clearAllThumbnails()
        context.cacheDir.listFiles()?.forEach { f ->
            try { f.deleteRecursively() } catch (_: Exception) {}
        }
        logSecurityEvent("CACHE_PURGED", "Temporary export cache and thumbnail cache cleared")
    }
}
