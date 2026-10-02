package com.example.data.security

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ThumbnailManager(
    private val context: Context,
    private val cryptoManager: CryptoManager
) {
    private val thumbDir = File(context.filesDir, ".vault_thumbnails").apply {
        mkdirs()
        try { File(this, ".nomedia").createNewFile() } catch (_: Throwable) {}
    }

    init {
        try {
            val legacy = File(context.filesDir, "vault_thumbnails")
            if (legacy.exists()) {
                File(legacy, ".nomedia").createNewFile()
            }
        } catch (_: Throwable) {}
    }

    // In-memory cache for fast display (bounded by runtime heap)
    private val memoryCache: LruCache<String, Bitmap> = run {
        val maxMemory = Runtime.getRuntime().maxMemory()
        val cacheSize = if (maxMemory > 0) {
            (maxMemory / 8).toInt().coerceIn(2 * 1024 * 1024, 20 * 1024 * 1024)
        } else {
            10 * 1024 * 1024
        }
        object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, value: Bitmap): Int {
                return value.byteCount
            }
        }
    }

    suspend fun getThumbnail(
        encryptedFilePath: String,
        vaultType: VaultType,
        isPdf: Boolean = false
    ): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = File(encryptedFilePath).nameWithoutExtension
        
        // 1. Check memory cache
        memoryCache.get(cacheKey)?.let { return@withContext it }

        // 2. Check disk thumbnail
        val diskFile = File(thumbDir, "${cacheKey}_thumb.png")
        if (diskFile.exists() && diskFile.length() > 0) {
            val bmp = BitmapFactory.decodeFile(diskFile.absolutePath)
            if (bmp != null) {
                memoryCache.put(cacheKey, bmp)
                return@withContext bmp
            }
        }

        // 3. Generate from encrypted source
        val encFile = File(encryptedFilePath)
        if (!encFile.exists() || encFile.length() == 0L) return@withContext null

        try {
            val decryptedBytes = cryptoManager.decryptFileToBytes(encFile, vaultType) ?: return@withContext null
            
            val isVideo = encryptedFilePath.endsWith("enc_vid") ||
                    encryptedFilePath.endsWith(".mp4", ignoreCase = true) ||
                    encryptedFilePath.endsWith(".mov", ignoreCase = true) ||
                    encryptedFilePath.endsWith(".mkv", ignoreCase = true)

            val generatedBitmap: Bitmap? = if (isPdf || encryptedFilePath.endsWith("enc_pdf")) {
                generatePdfThumbnail(decryptedBytes)
            } else if (isVideo) {
                generateVideoThumbnail(decryptedBytes)
            } else {
                generateImageThumbnail(decryptedBytes)
            }

            if (generatedBitmap != null) {
                // Save to disk thumbnail cache
                FileOutputStream(diskFile).use { fos ->
                    generatedBitmap.compress(Bitmap.CompressFormat.PNG, 85, fos)
                }
                memoryCache.put(cacheKey, generatedBitmap)
            }
            generatedBitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun generateImageThumbnail(bytes: ByteArray): Bitmap? {
        // Decode bounds to sample down
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

        val targetWidth = 240
        val targetHeight = 240
        var sampleSize = 1
        if (options.outHeight > targetHeight || options.outWidth > targetWidth) {
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while ((halfHeight / sampleSize) >= targetHeight && (halfWidth / sampleSize) >= targetWidth) {
                sampleSize *= 2
            }
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
    }

    private fun generatePdfThumbnail(pdfBytes: ByteArray): Bitmap? {
        val tempPdf = File(context.cacheDir, "temp_thumb_${System.currentTimeMillis()}.pdf")
        return try {
            tempPdf.writeBytes(pdfBytes)
            val pfd = ParcelFileDescriptor.open(tempPdf, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            if (renderer.pageCount > 0) {
                val page = renderer.openPage(0)
                val width = 240
                val height = (width * (page.height.toFloat() / page.width.toFloat())).toInt().coerceIn(160, 360)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                pfd.close()
                bitmap
            } else {
                renderer.close()
                pfd.close()
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            tempPdf.delete()
        }
    }

    private fun generateVideoThumbnail(videoBytes: ByteArray): Bitmap? {
        val tempVid = File(context.cacheDir, "temp_vid_thumb_${System.currentTimeMillis()}.mp4")
        return try {
            tempVid.writeBytes(videoBytes)
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(tempVid.absolutePath)
                val frame = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.frameAtTime
                if (frame != null) {
                    val maxDim = 320
                    val scale = minOf(maxDim.toFloat() / frame.width, maxDim.toFloat() / frame.height, 1f)
                    val w = (frame.width * scale).toInt().coerceAtLeast(1)
                    val h = (frame.height * scale).toInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(frame, w, h, true)
                } else null
            } finally {
                try { retriever.release() } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            null
        } finally {
            tempVid.delete()
        }
    }

    /**
     * Extracts and saves thumbnail directly from source URI during import
     * without requiring later decryption.
     */
    suspend fun saveThumbnailDirectly(
        encryptedFilePath: String,
        sourceUri: Uri,
        isVideo: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        val cacheKey = File(encryptedFilePath).nameWithoutExtension
        val diskFile = File(thumbDir, "${cacheKey}_thumb.png")
        if (diskFile.exists() && diskFile.length() > 0) return@withContext true

        try {
            val bitmap: Bitmap? = if (isVideo) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, sourceUri)
                    val frame = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.frameAtTime
                    if (frame != null) {
                        val maxDim = 320
                        val scale = minOf(maxDim.toFloat() / frame.width, maxDim.toFloat() / frame.height, 1f)
                        val w = (frame.width * scale).toInt().coerceAtLeast(1)
                        val h = (frame.height * scale).toInt().coerceAtLeast(1)
                        Bitmap.createScaledBitmap(frame, w, h, true)
                    } else null
                } finally {
                    try { retriever.release() } catch (_: Exception) {}
                }
            } else {
                context.contentResolver.openInputStream(sourceUri)?.use { isBounds ->
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(isBounds, null, options)
                    val targetDim = 320
                    var sample = 1
                    while ((options.outWidth / sample) >= targetDim && (options.outHeight / sample) >= targetDim) {
                        sample *= 2
                    }
                    context.contentResolver.openInputStream(sourceUri)?.use { isData ->
                        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
                        BitmapFactory.decodeStream(isData, null, decodeOpts)
                    }
                }
            }

            if (bitmap != null) {
                FileOutputStream(diskFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos)
                }
                memoryCache.put(cacheKey, bitmap)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun deleteThumbnail(encryptedFilePath: String) {
        val cacheKey = File(encryptedFilePath).nameWithoutExtension
        memoryCache.remove(cacheKey)
        val diskFile = File(thumbDir, "${cacheKey}_thumb.png")
        if (diskFile.exists()) {
            diskFile.delete()
        }
    }

    fun clearAllThumbnails() {
        memoryCache.evictAll()
        thumbDir.listFiles()?.forEach { it.delete() }
    }

    fun getThumbnailSizeBytes(): Long {
        return thumbDir.listFiles()?.sumOf { it.length() } ?: 0L
    }
}
