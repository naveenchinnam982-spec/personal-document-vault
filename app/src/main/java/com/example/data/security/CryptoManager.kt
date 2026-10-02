package com.example.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

enum class VaultType {
    MAIN,
    PRIVATE,
    HIDDEN,
    HIDDEN_HIDDEN
}

class CryptoManager(private val context: Context) {

    private val keyStore: KeyStore? = try {
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
            load(null)
        }
    } catch (e: Throwable) {
        null
    }

    private val memoryKeyCache = mutableMapOf<String, SecretKey>()

    init {
        // Do NOT block app startup with synchronous KeyStore operations.
        // Keys will be initialized lazily on-demand when operations require them.
    }

    private fun getAliasForVault(vaultType: VaultType): String = when (vaultType) {
        VaultType.MAIN -> KEY_ALIAS_MAIN
        VaultType.PRIVATE -> KEY_ALIAS_PRIVATE
        VaultType.HIDDEN -> KEY_ALIAS_HIDDEN
        VaultType.HIDDEN_HIDDEN -> KEY_ALIAS_HIDDEN_HIDDEN
    }

    private fun getOrCreateKey(alias: String): SecretKey {
        memoryKeyCache[alias]?.let { return it }

        if (keyStore != null) {
            try {
                if (keyStore.containsAlias(alias)) {
                    val existingKey = (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.secretKey
                    if (existingKey != null) {
                        try {
                            val testCipher = Cipher.getInstance(TRANSFORMATION)
                            testCipher.init(Cipher.ENCRYPT_MODE, existingKey)
                            memoryKeyCache[alias] = existingKey
                            return existingKey
                        } catch (_: Throwable) {
                            try {
                                keyStore.deleteEntry(alias)
                            } catch (_: Throwable) {}
                        }
                    }
                }

                val newKey = createKey(alias)
                if (newKey != null) {
                    try {
                        val testCipher = Cipher.getInstance(TRANSFORMATION)
                        testCipher.init(Cipher.ENCRYPT_MODE, newKey)
                        memoryKeyCache[alias] = newKey
                        return newKey
                    } catch (_: Throwable) {
                        try {
                            keyStore.deleteEntry(alias)
                        } catch (_: Throwable) {}
                    }
                }
            } catch (_: Throwable) {
                try {
                    keyStore.deleteEntry(alias)
                } catch (_: Throwable) {}
            }
        }

        val fallbackKey = getOrCreateFallbackKey(alias)
        memoryKeyCache[alias] = fallbackKey
        return fallbackKey
    }

    private fun createKey(alias: String): SecretKey? {
        return try {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            val spec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        } catch (_: Throwable) {
            null
        }
    }

    private fun getOrCreateFallbackKey(alias: String): SecretKey {
        return try {
            val keyDir = File(context.filesDir, ".vault_keys")
            if (!keyDir.exists()) {
                keyDir.mkdirs()
            }
            val keyFile = File(keyDir, "$alias.bin")
            if (keyFile.exists() && keyFile.length() == 32L) {
                try {
                    val bytes = keyFile.readBytes()
                    SecretKeySpec(bytes, "AES")
                } catch (_: Throwable) {
                    generateAndStoreFallbackKey(keyFile)
                }
            } else {
                generateAndStoreFallbackKey(keyFile)
            }
        } catch (_: Throwable) {
            val randomBytes = ByteArray(32)
            SecureRandom().nextBytes(randomBytes)
            SecretKeySpec(randomBytes, "AES")
        }
    }

    private fun generateAndStoreFallbackKey(keyFile: File): SecretKey {
        val randomBytes = ByteArray(32)
        SecureRandom().nextBytes(randomBytes)
        try {
            keyFile.writeBytes(randomBytes)
        } catch (_: Throwable) {}
        return SecretKeySpec(randomBytes, "AES")
    }

    /**
     * Encrypts the provided input stream and writes it to the destination file.
     * File layout: [12 bytes IV] + [AES-GCM Ciphertext with Auth Tag]
     */
    fun encryptToFile(
        inputStream: InputStream,
        destinationFile: File,
        vaultType: VaultType = VaultType.MAIN
    ): Long {
        destinationFile.parentFile?.mkdirs()
        val key = getOrCreateKey(getAliasForVault(vaultType))
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv // 12 bytes for GCM

        FileOutputStream(destinationFile).use { fos ->
            fos.write(iv)
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                val output = cipher.update(buffer, 0, bytesRead)
                if (output != null && output.isNotEmpty()) {
                    fos.write(output)
                }
            }
            val finalBytes = cipher.doFinal()
            if (finalBytes != null && finalBytes.isNotEmpty()) {
                fos.write(finalBytes)
            }
        }
        return destinationFile.length()
    }

    fun encryptToFile(inputStream: InputStream, destinationFile: File, isPrivateVault: Boolean): Long {
        return encryptToFile(inputStream, destinationFile, if (isPrivateVault) VaultType.PRIVATE else VaultType.MAIN)
    }

    /**
     * Encrypts raw byte array and writes to file.
     */
    fun encryptBytesToFile(
        bytes: ByteArray,
        destinationFile: File,
        vaultType: VaultType = VaultType.MAIN
    ): Long {
        destinationFile.parentFile?.mkdirs()
        val key = getOrCreateKey(getAliasForVault(vaultType))
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv
        val ciphertext = cipher.doFinal(bytes)

        FileOutputStream(destinationFile).use { fos ->
            fos.write(iv)
            fos.write(ciphertext)
        }
        return destinationFile.length()
    }

    fun encryptBytesToFile(bytes: ByteArray, destinationFile: File, isPrivateVault: Boolean): Long {
        return encryptBytesToFile(bytes, destinationFile, if (isPrivateVault) VaultType.PRIVATE else VaultType.MAIN)
    }

    /**
     * Decrypts an encrypted file to memory.
     */
    fun decryptFileToBytes(sourceFile: File, vaultType: VaultType = VaultType.MAIN): ByteArray? {
        if (!sourceFile.exists() || sourceFile.length() < GCM_IV_LENGTH) return null
        return try {
            val key = getOrCreateKey(getAliasForVault(vaultType))
            FileInputStream(sourceFile).use { fis ->
                val iv = ByteArray(GCM_IV_LENGTH)
                val ivRead = fis.read(iv)
                if (ivRead != GCM_IV_LENGTH) return null

                val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, key, spec)

                val ciphertext = fis.readBytes()
                cipher.doFinal(ciphertext)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun decryptFileToBytes(sourceFile: File, isPrivateVault: Boolean): ByteArray? {
        return decryptFileToBytes(sourceFile, if (isPrivateVault) VaultType.PRIVATE else VaultType.MAIN)
    }

    /**
     * Decrypts an encrypted file directly to a destination file in a memory-efficient stream.
     * Ideal for media files such as videos and large documents.
     */
    fun decryptToFile(
        sourceFile: File,
        destinationFile: File,
        vaultType: VaultType = VaultType.MAIN
    ): Boolean {
        if (!sourceFile.exists() || sourceFile.length() < GCM_IV_LENGTH) return false
        return try {
            destinationFile.parentFile?.mkdirs()
            val key = getOrCreateKey(getAliasForVault(vaultType))
            FileInputStream(sourceFile).use { fis ->
                val iv = ByteArray(GCM_IV_LENGTH)
                val ivRead = fis.read(iv)
                if (ivRead != GCM_IV_LENGTH) return false

                val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, key, spec)

                FileOutputStream(destinationFile).use { fos ->
                    val buffer = ByteArray(16384)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        val output = cipher.update(buffer, 0, bytesRead)
                        if (output != null && output.isNotEmpty()) {
                            fos.write(output)
                        }
                    }
                    val finalBytes = cipher.doFinal()
                    if (finalBytes != null && finalBytes.isNotEmpty()) {
                        fos.write(finalBytes)
                    }
                }
            }
            destinationFile.exists() && destinationFile.length() > 0
        } catch (e: Exception) {
            destinationFile.delete()
            false
        }
    }

    fun decryptToFile(sourceFile: File, destinationFile: File, isPrivateVault: Boolean): Boolean {
        return decryptToFile(sourceFile, destinationFile, if (isPrivateVault) VaultType.PRIVATE else VaultType.MAIN)
    }

    /**
     * Creates a temporary decrypted copy in cacheDir/shared_export/ for sharing.
     */
    fun createDecryptedTempCopy(
        sourceFile: File,
        exportName: String,
        vaultType: VaultType = VaultType.MAIN
    ): File? {
        val decryptedBytes = decryptFileToBytes(sourceFile, vaultType) ?: return null
        val exportDir = File(context.cacheDir, "shared_export").apply {
            mkdirs()
            try { File(this, ".nomedia").createNewFile() } catch (_: Throwable) {}
        }
        val tempFile = File(exportDir, exportName)
        tempFile.writeBytes(decryptedBytes)
        return tempFile
    }

    fun createDecryptedTempCopy(sourceFile: File, exportName: String, isPrivateVault: Boolean): File? {
        return createDecryptedTempCopy(sourceFile, exportName, if (isPrivateVault) VaultType.PRIVATE else VaultType.MAIN)
    }

    /**
     * Purges temporary exported files.
     */
    fun clearTempExports() {
        try {
            val exportDir = File(context.cacheDir, "shared_export")
            if (exportDir.exists()) {
                exportDir.listFiles()?.forEach { it.delete() }
            }
        } catch (_: Exception) {}
    }

    /**
     * Performs secure shredding of sensitive file:
     * Overwrites file contents with random bytes before deletion.
     */
    fun secureDeleteFile(file: File): Boolean {
        if (!file.exists()) return true
        return try {
            val length = file.length()
            if (length > 0) {
                FileOutputStream(file).use { fos ->
                    val random = SecureRandom()
                    val buffer = ByteArray(4096)
                    var written = 0L
                    while (written < length) {
                        random.nextBytes(buffer)
                        val toWrite = minOf(buffer.size.toLong(), length - written).toInt()
                        fos.write(buffer, 0, toWrite)
                        written += toWrite
                    }
                    fos.flush()
                }
            }
            file.delete()
        } catch (e: Exception) {
            file.delete()
        }
    }

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS_MAIN = "SecureVault_MainKey"
        private const val KEY_ALIAS_PRIVATE = "SecureVault_PrivateKey"
        private const val KEY_ALIAS_HIDDEN = "SecureVault_HiddenKey"
        private const val KEY_ALIAS_HIDDEN_HIDDEN = "SecureVault_HiddenHiddenKey"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH_BITS = 128
    }
}
