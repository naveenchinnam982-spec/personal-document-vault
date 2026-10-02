package com.example.data.security

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import com.example.data.local.HiddenDao
import java.io.File
import java.util.UUID

object MediaPrivacyHelper {

    private const val TAG = "MediaPrivacyHelper"

    /**
     * Safely deletes a public media file (image/video) after it has been securely imported/moved
     * into private app-specific storage.
     * Refreshes the phone gallery and Android MediaStore immediately.
     */
    fun deletePublicMediaAndRefresh(context: Context, uri: Uri): Boolean {
        var deleted = false
        var filePath: String? = null

        try {
            // 1. Try to resolve the underlying file path if possible
            if (uri.scheme == "content") {
                try {
                    val projection = arrayOf(
                        MediaStore.MediaColumns.DATA,
                        MediaStore.MediaColumns._ID
                    )
                    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val dataIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                            if (dataIdx != -1) {
                                filePath = cursor.getString(dataIdx)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not resolve file path from content URI: ${e.message}")
                }
            } else if (uri.scheme == "file") {
                filePath = uri.path
            }
        } catch (_: Exception) {}

        // 2. Try deleting via DocumentsContract if it is a document URI
        if (DocumentsContract.isDocumentUri(context, uri)) {
            try {
                deleted = DocumentsContract.deleteDocument(context.contentResolver, uri)
                if (deleted) Log.d(TAG, "Deleted via DocumentsContract: $uri")
            } catch (e: Exception) {
                Log.w(TAG, "DocumentsContract delete failed: ${e.message}")
            }
        }

        // 3. Try deleting via ContentResolver
        if (!deleted) {
            try {
                val rows = context.contentResolver.delete(uri, null, null)
                if (rows > 0) {
                    deleted = true
                    Log.d(TAG, "Deleted via ContentResolver: $uri ($rows rows)")
                }
            } catch (e: Exception) {
                Log.w(TAG, "ContentResolver delete failed: ${e.message}")
            }
        }

        // 4. Try deleting the underlying file on disk if path was resolved
        if (filePath != null) {
            try {
                val file = File(filePath!!)
                if (file.exists()) {
                    val fileDeleted = file.delete()
                    if (fileDeleted) {
                        deleted = true
                        Log.d(TAG, "Deleted physical file: $filePath")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Physical file delete failed: ${e.message}")
            }

            // Also clean up MediaStore entries for this file path
            try {
                context.contentResolver.delete(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    "${MediaStore.MediaColumns.DATA} = ?",
                    arrayOf(filePath!!)
                )
            } catch (_: Exception) {}
            try {
                context.contentResolver.delete(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    "${MediaStore.MediaColumns.DATA} = ?",
                    arrayOf(filePath!!)
                )
            } catch (_: Exception) {}
        }

        // 5. Force Android MediaScanner and Phone Gallery to refresh immediately
        try {
            if (filePath != null) {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(filePath!!),
                    null
                ) { path, scannedUri ->
                    Log.d(TAG, "MediaScanner refreshed: $path -> $scannedUri")
                }
            }

            // Also trigger media scanner broadcast
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                data = uri
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.w(TAG, "MediaScanner refresh broadcast failed: ${e.message}")
        }

        return deleted
    }

    /**
     * Sanitizes existing Hidden Vault storage:
     * - Ensures any existing Hidden Vault items that might have been stored in public media
     *   folders (Pictures, DCIM, Movies, Downloads) are safely relocated to internal private storage.
     * - Removes old public files and triggers MediaScanner so they disappear from Phone Gallery.
     * - Verifies .nomedia presence across all internal vault directories.
     */
    suspend fun sanitizeExistingHiddenVaultStorage(
        context: Context,
        hiddenDao: HiddenDao
    ) {
        val privateHiddenDir = File(context.filesDir, "vault_hidden").apply {
            mkdirs()
            try { File(this, ".nomedia").createNewFile() } catch (_: Throwable) {}
        }

        // 1. Audit and migrate any existing items whose path is in public storage
        try {
            val allItems = hiddenDao.getAllHiddenItemsSync()
            for (item in allItems) {
                val currentPath = item.encryptedPath
                val file = File(currentPath)

                val isInPublicStorage = currentPath.contains("/DCIM") ||
                        currentPath.contains("/Pictures") ||
                        currentPath.contains("/Movies") ||
                        currentPath.contains("/Download") ||
                        (currentPath.startsWith("/storage/emulated/0") && !currentPath.contains(context.packageName))

                if (isInPublicStorage && file.exists()) {
                    Log.w(TAG, "Migrating public hidden file to private internal storage: $currentPath")
                    val ext = when (item.itemType) {
                        "VIDEO" -> "enc_vid"
                        "PDF" -> "enc_pdf"
                        "PHOTO" -> "enc_img"
                        else -> "enc_doc"
                    }
                    val newFile = File(privateHiddenDir, "hidden_${UUID.randomUUID()}.$ext")
                    file.copyTo(newFile, overwrite = true)

                    if (newFile.exists() && newFile.length() > 0) {
                        // Update database record with safe internal path
                        hiddenDao.updateHiddenItem(item.copy(encryptedPath = newFile.absolutePath))
                        // Delete old public file
                        file.delete()
                        // Notify MediaScanner to purge old public path
                        MediaScannerConnection.scanFile(
                            context,
                            arrayOf(currentPath),
                            null,
                            null
                        )
                        Log.i(TAG, "Successfully moved to private storage: ${newFile.absolutePath}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error auditing existing hidden vault items: ${e.message}", e)
        }

        // 2. Scan public media directories for orphaned vault files (hidden_* or *.enc_*)
        try {
            val publicDirs = listOfNotNull(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            )

            for (publicDir in publicDirs) {
                if (publicDir.exists()) {
                    publicDir.listFiles()?.forEach { file ->
                        val name = file.name.lowercase()
                        if (name.startsWith("hidden_") ||
                            name.endsWith(".enc_img") ||
                            name.endsWith(".enc_vid") ||
                            name.endsWith(".enc_doc") ||
                            name.endsWith(".enc_pdf")
                        ) {
                            val path = file.absolutePath
                            file.delete()
                            MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Ensure .nomedia in all private vault folders
        listOf(
            context.filesDir,
            context.cacheDir,
            File(context.filesDir, "vault_hidden"),
            File(context.filesDir, "vault_private"),
            File(context.filesDir, "vault_hidden_hidden"),
            File(context.filesDir, "vault_docs"),
            File(context.filesDir, "vault_thumbnails"),
            File(context.filesDir, ".vault_thumbnails"),
            File(context.cacheDir, "shared_export")
        ).forEach { dir ->
            try {
                dir.mkdirs()
                val nomedia = File(dir, ".nomedia")
                if (!nomedia.exists()) nomedia.createNewFile()
            } catch (_: Throwable) {}
        }
    }
}
