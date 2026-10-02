package com.example.data.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

object FileDownloadHelper {

    /**
     * Saves decrypted bytes directly to the public Downloads folder.
     * Complies with modern Android Scoped Storage without broad storage permissions.
     */
    fun saveToDownloads(
        context: Context,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): Uri? {
        return try {
            val cleanName = sanitizeFileName(fileName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, cleanName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { stream ->
                        stream.write(bytes)
                        stream.flush()
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                    uri
                } else {
                    null
                }
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                downloadsDir.mkdirs()
                var targetFile = File(downloadsDir, cleanName)
                var counter = 1
                val baseName = cleanName.substringBeforeLast(".")
                val ext = cleanName.substringAfterLast(".", "")
                while (targetFile.exists()) {
                    val candidateName = if (ext.isNotEmpty()) "$baseName ($counter).$ext" else "$baseName ($counter)"
                    targetFile = File(downloadsDir, candidateName)
                    counter++
                }
                targetFile.writeBytes(bytes)
                Uri.fromFile(targetFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Writes decrypted bytes into a URI chosen by the user via ActivityResultContracts.CreateDocument.
     */
    fun saveToChosenUri(
        context: Context,
        targetUri: Uri,
        bytes: ByteArray
    ): Boolean {
        return try {
            context.contentResolver.openOutputStream(targetUri)?.use { stream ->
                stream.write(bytes)
                stream.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Opens a downloaded file using the user's preferred viewer app.
     */
    fun openFile(context: Context, fileUri: Uri, mimeType: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open with..."))
        } catch (_: Exception) {}
    }

    fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[/\\\\:*?\"<>|]"), "_").trim().ifBlank { "downloaded_file" }
    }
}
