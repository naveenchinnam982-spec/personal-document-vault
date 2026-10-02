package com.example.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class PdfQuickInfo(
    val pageCount: Int,
    val firstPageThumbnail: Bitmap?
)

class PdfDocumentManager(
    private val context: Context
) {
    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private var tempFile: File? = null

    // Cache up to 12 rendered pages
    private val pageBitmapCache = LruCache<Int, Bitmap>(12)

    val pageCount: Int
        get() = pdfRenderer?.pageCount ?: 0

    val isReady: Boolean
        get() = pdfRenderer != null && pageCount > 0

    /**
     * Initializes the PDF renderer with decrypted PDF bytes.
     */
    suspend fun openFromBytes(bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        close()
        try {
            val dir = File(context.cacheDir, "pdf_render").apply { mkdirs() }
            val file = File(dir, "pdf_${UUID.randomUUID()}.pdf")
            file.writeBytes(bytes)
            tempFile = file

            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            parcelFileDescriptor = pfd
            pdfRenderer = PdfRenderer(pfd)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            close()
            false
        }
    }

    /**
     * Initializes the PDF renderer directly from a content URI.
     */
    suspend fun openFromUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        close()
        try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return@withContext false
            parcelFileDescriptor = pfd
            pdfRenderer = PdfRenderer(pfd)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            close()
            false
        }
    }

    /**
     * Renders a specific page at high resolution.
     */
    suspend fun renderPage(pageIndex: Int, densityScale: Float = 2.0f): Bitmap? = withContext(Dispatchers.IO) {
        val renderer = pdfRenderer ?: return@withContext null
        if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null

        pageBitmapCache.get(pageIndex)?.let { return@withContext it }

        try {
            val page = renderer.openPage(pageIndex)
            val renderWidth = (page.width * densityScale).toInt().coerceAtLeast(300)
            val renderHeight = (page.height * densityScale).toInt().coerceAtLeast(400)

            val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE) // PDF background is often transparent by default
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            pageBitmapCache.put(pageIndex, bitmap)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Renders a smaller thumbnail for page previews or navigation strip.
     */
    suspend fun renderThumbnail(pageIndex: Int, targetWidth: Int = 200): Bitmap? = withContext(Dispatchers.IO) {
        val renderer = pdfRenderer ?: return@withContext null
        if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null

        try {
            val page = renderer.openPage(pageIndex)
            val targetHeight = (targetWidth * (page.height.toFloat() / page.width.toFloat())).toInt().coerceIn(120, 360)
            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Closes the renderer, descriptors, and wipes the temporary file.
     */
    fun close() {
        try {
            pageBitmapCache.evictAll()
            pdfRenderer?.close()
            pdfRenderer = null
            parcelFileDescriptor?.close()
            parcelFileDescriptor = null
            tempFile?.delete()
            tempFile = null
        } catch (_: Exception) {}
    }

    companion object {
        fun isPdf(bytes: ByteArray): Boolean {
            if (bytes.size < 4) return false
            // Magic bytes "%PDF"
            return bytes[0] == 0x25.toByte() &&
                    bytes[1] == 0x50.toByte() &&
                    bytes[2] == 0x44.toByte() &&
                    bytes[3] == 0x46.toByte()
        }

        suspend fun inspectPdfUri(context: Context, uri: Uri): PdfQuickInfo? = withContext(Dispatchers.IO) {
            try {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return@withContext null
                val renderer = PdfRenderer(pfd)
                val count = renderer.pageCount
                var thumb: Bitmap? = null
                if (count > 0) {
                    val page = renderer.openPage(0)
                    val width = 240
                    val height = (width * (page.height.toFloat() / page.width.toFloat())).toInt().coerceIn(160, 360)
                    thumb = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(thumb)
                    canvas.drawColor(Color.WHITE)
                    page.render(thumb, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                }
                renderer.close()
                pfd.close()
                PdfQuickInfo(pageCount = count, firstPageThumbnail = thumb)
            } catch (e: Exception) {
                null
            }
        }
    }
}
