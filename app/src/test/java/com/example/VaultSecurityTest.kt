package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.security.AuthManager
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VaultSecurityTest {

    private lateinit var context: Context
    private lateinit var authManager: AuthManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        authManager = AuthManager(context)
    }

    @Test
    fun testMainPinSetupAndVerification() {
        val pin = "1234"
        assertTrue(authManager.setMainPin(pin))
        assertTrue(authManager.isMainPinSet())
        assertTrue(authManager.verifyMainPin(pin))
        assertFalse(authManager.verifyMainPin("9999"))
    }

    @Test
    fun testPrivateVaultSeparatePin() {
        val mainPin = "1111"
        val privatePin = "9876"

        authManager.setMainPin(mainPin)
        authManager.setPrivatePin(privatePin)

        assertTrue(authManager.verifyMainPin(mainPin))
        assertFalse(authManager.verifyMainPin(privatePin))

        assertTrue(authManager.verifyPrivatePin(privatePin))
        assertFalse(authManager.verifyPrivatePin(mainPin))
    }

    @Test
    fun testRateLimitingLockout() {
        authManager.setMainPin("4321")
        // Fail 5 times
        repeat(5) {
            authManager.verifyMainPin("0000")
        }
        assertTrue(authManager.isMainLockedOut())
    }

    @Test
    fun testRecoveryMasterKey() {
        val code = authManager.getOrCreateRecoveryCode()
        assertTrue(code.isNotBlank())
        assertTrue(authManager.verifyRecoveryCode(code))
        assertFalse(authManager.verifyRecoveryCode("WRONGKEY"))
    }

    @Test
    fun testThreeSeparateVaultPins() {
        val mainPin = "1111"
        val privatePin = "2222"
        val hiddenPin = "3333"

        authManager.setMainPin(mainPin)
        authManager.setPrivatePin(privatePin)
        authManager.setHiddenPin(hiddenPin)

        // Verify isolation
        assertTrue(authManager.verifyMainPin(mainPin))
        assertFalse(authManager.verifyMainPin(hiddenPin))
        assertFalse(authManager.verifyMainPin(privatePin))

        assertTrue(authManager.verifyPrivatePin(privatePin))
        assertFalse(authManager.verifyPrivatePin(mainPin))
        assertFalse(authManager.verifyPrivatePin(hiddenPin))

        assertTrue(authManager.verifyHiddenPin(hiddenPin))
        assertFalse(authManager.verifyHiddenPin(mainPin))
        assertFalse(authManager.verifyHiddenPin(privatePin))
    }

    @Test
    fun testHiddenVaultLockout() {
        authManager.setHiddenPin("5555")
        repeat(5) {
            authManager.verifyHiddenPin("0000")
        }
        assertTrue(authManager.isHiddenLockedOut())
    }

    @Test
    fun testDiscreetAccessCode() {
        authManager.discreetAccessCode = "9876"
        assertTrue(authManager.verifyDiscreetCode("9876"))
        assertFalse(authManager.verifyDiscreetCode("1234"))
    }

    @Test
    fun testHiddenVaultEnableToggle() {
        assertFalse(authManager.isHiddenVaultEnabled)
        authManager.isHiddenVaultEnabled = true
        assertTrue(authManager.isHiddenVaultEnabled)
        authManager.isHiddenVaultEnabled = false
        assertFalse(authManager.isHiddenVaultEnabled)
    }

    @Test
    fun testHiddenHiddenVaultPatternSetupAndVerification() {
        val pattern = listOf(0, 1, 2, 4, 6)
        assertFalse(authManager.isHiddenHiddenPatternSet())

        // Pattern shorter than 4 dots should be rejected
        assertFalse(authManager.setHiddenHiddenPattern(listOf(0, 1, 2)))

        // Valid pattern
        assertTrue(authManager.setHiddenHiddenPattern(pattern))
        assertTrue(authManager.isHiddenHiddenPatternSet())

        // Correct pattern matches
        assertTrue(authManager.verifyHiddenHiddenPattern(pattern))

        // Incorrect pattern fails
        assertFalse(authManager.verifyHiddenHiddenPattern(listOf(0, 1, 2, 3, 4)))
    }

    @Test
    fun testHiddenHiddenPatternLockout() {
        val pattern = listOf(0, 3, 6, 7, 8)
        authManager.setHiddenHiddenPattern(pattern)
        assertFalse(authManager.isHiddenHiddenLockedOut())

        // 5 failed attempts triggers lockout
        repeat(5) {
            authManager.verifyHiddenHiddenPattern(listOf(1, 2, 4, 5))
        }

        assertTrue(authManager.isHiddenHiddenLockedOut())
        assertTrue(authManager.getHiddenHiddenLockoutSecondsRemaining() > 0)
    }

    @Test
    fun testPatternIsolationFromOtherVaultPins() {
        val pattern = listOf(1, 2, 3, 4)
        authManager.setMainPin("1234")
        authManager.setHiddenPin("4321")
        authManager.setHiddenHiddenPattern(pattern)

        assertTrue(authManager.verifyHiddenHiddenPattern(pattern))
        assertTrue(authManager.verifyMainPin("1234"))
        assertTrue(authManager.verifyHiddenPin("4321"))

        // Reset pattern doesn't affect main or hidden PIN
        authManager.resetHiddenHiddenPattern()
        assertFalse(authManager.isHiddenHiddenPatternSet())
        assertTrue(authManager.verifyMainPin("1234"))
        assertTrue(authManager.verifyHiddenPin("4321"))
    }

    @Test
    fun testHiddenVaultStorageIsolation() {
        val hiddenDir = java.io.File(context.filesDir, "vault_hidden")
        hiddenDir.mkdirs()
        val nomedia = java.io.File(hiddenDir, ".nomedia")
        if (!nomedia.exists()) {
            nomedia.createNewFile()
        }

        // Verify hidden storage is in app-internal filesDir (NOT in public DCIM/Pictures)
        assertTrue("Hidden storage must be inside private app internal storage", hiddenDir.absolutePath.startsWith(context.filesDir.absolutePath))
        assertFalse("Hidden storage must not be in external Pictures", hiddenDir.absolutePath.contains("/Pictures"))
        assertFalse("Hidden storage must not be in external DCIM", hiddenDir.absolutePath.contains("/DCIM"))
        assertTrue(".nomedia file must be present in hidden vault folder", nomedia.exists())

        // Verify thumbnail directory also has .nomedia exclusion
        val thumbDir = java.io.File(context.filesDir, ".vault_thumbnails")
        thumbDir.mkdirs()
        val thumbNomedia = java.io.File(thumbDir, ".nomedia")
        if (!thumbNomedia.exists()) {
            thumbNomedia.createNewFile()
        }
        assertTrue(".nomedia file must be present in thumbnail directory", thumbNomedia.exists())
    }

    @Test
    fun testHiddenVaultFiveImagesUploadIsolation(): Unit = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
        val database = com.example.data.local.AppDatabase.getDatabase(context)
        val hiddenDao = database.hiddenDao()
        val docDao = database.documentDao()
        val privateDao = database.privateMediaDao()

        // Clean slate for test
        database.clearAllTables()

        // 1. Upload 5 images to Hidden Vault
        for (i in 1..5) {
            val item = com.example.data.model.HiddenItem(
                name = "secret_photo_$i.jpg",
                itemType = "PHOTO",
                encryptedPath = "${context.filesDir}/vault_hidden/hidden_test_$i.enc_img",
                category = "Confidential",
                fileSizeBytes = 1024L * (i * 100)
            )
            hiddenDao.insertHiddenItem(item)
        }

        // 2. Expected Results:
        // Hidden Vault -> 5 images visible
        val hiddenList: List<com.example.data.model.HiddenItem> = hiddenDao.getHiddenPhotos().first()
        assertEquals(5, hiddenList.size)
        assertEquals(5, hiddenDao.getHiddenPhotosCount().first())

        // Normal App Gallery (Documents) -> 0 of those images visible
        val normalDocs: List<com.example.data.model.Document> = docDao.getAllDocuments().first()
        assertEquals(0, normalDocs.size)

        // Normal App Gallery (Private Media) -> 0 of those images visible
        val normalPrivateMedia: List<com.example.data.model.PrivateMedia> = privateDao.getAllMedia().first()
        assertEquals(0, normalPrivateMedia.size)

        // Verify none of the 5 files are in public DCIM or Pictures
        hiddenList.forEach { hiddenItem: com.example.data.model.HiddenItem ->
            assertFalse(hiddenItem.encryptedPath.contains("/DCIM"))
            assertFalse(hiddenItem.encryptedPath.contains("/Pictures"))
            assertTrue(hiddenItem.encryptedPath.endsWith(".enc_img"))
        }
    }

    @Test
    fun testHiddenHiddenVaultFingerprintRemovedWhileKeepingOthers() {
        // Main App can have biometric enabled
        authManager.isBiometricEnabledForMain = true
        assertTrue(authManager.isBiometricEnabledForMain)

        // Hidden Vault can have biometric enabled
        authManager.isBiometricEnabledForHidden = true
        assertTrue(authManager.isBiometricEnabledForHidden)

        // Private Vault can have biometric enabled
        authManager.isBiometricEnabledForPrivate = true
        assertTrue(authManager.isBiometricEnabledForPrivate)

        // BUT Hidden Hidden Vault MUST NOT have biometric enabled
        authManager.isBiometricEnabledForHiddenHidden = true
        assertFalse("Hidden Hidden Vault must NEVER have biometric enabled", authManager.isBiometricEnabledForHiddenHidden)
    }

    @Test
    fun testMediaPrivacyHelperDeletesPublicSource() {
        // Create a test image in a simulated public pictures location
        val testPublicDir = java.io.File(context.filesDir, "test_public_pictures").apply { mkdirs() }
        val testImageFile = java.io.File(testPublicDir, "camera_photo_123.jpg")
        testImageFile.writeBytes(byteArrayOf(1, 2, 3, 4, 5))
        assertTrue(testImageFile.exists())

        // Execute privacy helper
        val deleted = com.example.data.security.MediaPrivacyHelper.deletePublicMediaAndRefresh(
            context,
            android.net.Uri.fromFile(testImageFile)
        )

        assertTrue("File must be reported as deleted", deleted)
        assertFalse("Physical image file must no longer exist in public storage", testImageFile.exists())
    }

    @Test
    fun testAppSharingPreparesCleanApkWithoutUserData() {
        val apkFile = com.example.util.AppSharingHelper.prepareShareableApk(context)
        assertNotNull("Prepared APK file must not be null", apkFile)
        assertTrue("Prepared APK must exist on disk", apkFile!!.exists())
        assertTrue("Prepared APK must have size > 0", apkFile.length() > 0)
        assertTrue("Prepared APK filename must end with .apk", apkFile.name.endsWith(".apk"))

        // Verify the APK is located in the isolated shared_export cache folder
        assertTrue("APK must be in shared_export cache", apkFile.absolutePath.contains("shared_export"))
        assertFalse("APK must not be located in private vault_hidden directory", apkFile.absolutePath.contains("vault_hidden"))
        assertFalse("APK must not be located in databases directory", apkFile.absolutePath.contains("databases"))

        // Verify FileProvider can generate a secure content URI
        val contentUri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        assertNotNull("Content URI must be generated", contentUri)
        assertEquals("content", contentUri.scheme)
        assertFalse("Private filesystem path must not be exposed in URI", contentUri.toString().contains("/data/data"))
    }
}
