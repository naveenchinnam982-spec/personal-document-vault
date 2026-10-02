package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.bulk.BulkImportStatus
import com.example.ui.components.BulkImportConfirmDialog
import com.example.ui.components.BulkImportProgressDialog
import com.example.ui.components.ImportProgressDialog
import com.example.ui.components.VaultBottomBar
import com.example.ui.components.WhatsAppShareImportDialog
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.documents.AddDocumentScreen
import com.example.ui.screens.documents.DocumentDetailScreen
import com.example.ui.screens.documents.DocumentsScreen
import com.example.ui.screens.favorites.FavoritesScreen
import com.example.ui.screens.hiddenvault.HiddenAddItemScreen
import com.example.ui.screens.hiddenvault.HiddenDocumentViewerScreen
import com.example.ui.screens.hiddenvault.HiddenMediaViewerScreen
import com.example.ui.screens.hiddenvault.HiddenSearchScreen
import com.example.ui.screens.hiddenvault.HiddenTrashScreen
import com.example.ui.screens.hiddenvault.HiddenVaultHomeScreen
import com.example.ui.screens.hiddenhiddenvault.HiddenHiddenLockScreen
import com.example.ui.screens.hiddenhiddenvault.HiddenHiddenVaultHomeScreen
import com.example.ui.screens.hiddenhiddenvault.HiddenHiddenMediaViewerScreen
import com.example.ui.screens.lock.ChangeAppLockAuthScreen
import com.example.ui.screens.lock.ChooseAppLockScreen
import com.example.ui.screens.lock.HiddenLockScreen
import com.example.ui.screens.lock.MainLockScreen
import com.example.ui.screens.lock.PrivateLockScreen
import com.example.ui.screens.lock.SetupAppLockScreen
import com.example.ui.screens.lock.SetupHiddenPinScreen
import com.example.ui.screens.lock.SetupPinScreen
import com.example.ui.screens.privatevault.PhotoViewerScreen
import com.example.ui.screens.privatevault.PrivateVaultHomeScreen
import com.example.ui.screens.scanner.DocumentScannerScreen
import com.example.ui.screens.search.SearchScreen
import com.example.ui.screens.settings.BackupRestoreScreen
import com.example.ui.screens.settings.PermissionManagerScreen
import com.example.ui.screens.settings.PrivacyCenterScreen
import com.example.ui.screens.settings.SecurityLogsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.StorageManagerScreen
import com.example.ui.screens.splash.SplashScreen
import com.example.ui.screens.trash.TrashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VaultDarkBg
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.VaultViewModel

class MainActivity : FragmentActivity() {

    private val viewModel: VaultViewModel by viewModels()
    private var lastBackgroundTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
            updateScreenshotProtection()

            setContent {
                val currentTheme by viewModel.appTheme.collectAsStateWithLifecycle()
                MyApplicationTheme(themeStyle = currentTheme) {
                    VaultApp(
                        viewModel = viewModel,
                        onBiometricRequested = { onSuccess ->
                            promptBiometricUnlock(onSuccess)
                        }
                    )
                }
            }

            handleIncomingShareIntent(intent)
        } catch (t: Throwable) {
            android.util.Log.e("MainActivity", "Fatal startup error in MainActivity: ${t.message}", t)
            handleStartupCrash(t)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingShareIntent(intent)
    }

    private fun handleIncomingShareIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return
        if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) return

        try {
            if (action == Intent.ACTION_SEND) {
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    ?: intent.clipData?.getItemAt(0)?.uri
                    ?: intent.data
                if (uri != null) {
                    viewModel.handleIncomingSharedUris(listOf(uri))
                }
            } else if (action == Intent.ACTION_SEND_MULTIPLE) {
                val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                if (!uris.isNullOrEmpty()) {
                    viewModel.handleIncomingSharedUris(uris)
                } else if (intent.clipData != null) {
                    val list = mutableListOf<Uri>()
                    for (i in 0 until intent.clipData!!.itemCount) {
                        intent.clipData!!.getItemAt(i).uri?.let { list.add(it) }
                    }
                    if (list.isNotEmpty()) {
                        viewModel.handleIncomingSharedUris(list)
                    }
                }
            }
        } catch (t: Throwable) {
            android.util.Log.e("MainActivity", "Error handling incoming share: ${t.message}", t)
        }
    }

    private fun handleStartupCrash(t: Throwable) {
        try {
            val sw = java.io.StringWriter()
            t.printStackTrace(java.io.PrintWriter(sw))
            val intent = Intent(this, CrashActivity::class.java).apply {
                putExtra(CrashActivity.EXTRA_CRASH_INFO, "MainActivity.onCreate Crash:\n${t.message}\n\n$sw")
                putExtra(CrashActivity.EXTRA_ERROR_MESSAGE, t.message ?: t.javaClass.simpleName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
            finish()
        } catch (_: Throwable) {
            // Last resort view
            val tv = android.widget.TextView(this).apply {
                setBackgroundColor(0xFF4A1010.toInt())
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(32, 64, 32, 64)
                text = "Startup Exception:\n${t.message}\n\n${t.stackTraceToString()}"
            }
            setContentView(tv)
        }
    }

    override fun onResume() {
        super.onResume()
        updateScreenshotProtection()

        try {
            if (lastBackgroundTime > 0L) {
                val elapsedSecs = (System.currentTimeMillis() - lastBackgroundTime) / 1000
                val timeoutSecs = viewModel.authManager.autoLockTimeoutSeconds
                if (timeoutSecs in 0..elapsedSecs) {
                    viewModel.lockAllVaults()
                }
                lastBackgroundTime = 0L
            }
        } catch (_: Throwable) {}
    }

    override fun onStop() {
        super.onStop()
        lastBackgroundTime = System.currentTimeMillis()
    }

    private fun updateScreenshotProtection() {
        try {
            // Avoid blocking emulator screencast / streaming preview
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } catch (_: Throwable) {}
    }

    private fun promptBiometricUnlock(onSuccess: () -> Unit) {
        try {
            val biometricManager = BiometricManager.from(this)
            val canAuth = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            if (canAuth == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ||
                canAuth == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE) {
                Toast.makeText(this, "Biometric sensor unavailable on this device", Toast.LENGTH_SHORT).show()
                return
            }
            if (canAuth == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                Toast.makeText(this, "No fingerprint or face enrolled. Please enroll biometrics in Settings or use PIN", Toast.LENGTH_LONG).show()
                return
            }

            val executor = ContextCompat.getMainExecutor(this)
            val biometricPrompt = BiometricPrompt(
                this,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        runOnUiThread {
                            onSuccess()
                        }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                            errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                            errorCode != BiometricPrompt.ERROR_CANCELED) {
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, errString, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Secure Vault")
                .setSubtitle("Confirm fingerprint or face to continue")
                .setDescription("Biometric authentication as a secure alternative to your PIN")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .setNegativeButtonText("Use PIN")
                .build()

            biometricPrompt.authenticate(promptInfo)
        } catch (t: Throwable) {
            android.util.Log.w("MainActivity", "Biometric unlock not available: ${t.message}")
        }
    }
}

@Composable
fun VaultApp(
    viewModel: VaultViewModel,
    onBiometricRequested: (() -> Unit) -> Unit
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val isMainUnlocked by viewModel.isMainUnlocked.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val multiImportState by viewModel.multiImportState.collectAsStateWithLifecycle()
    val bulkImportState by viewModel.bulkImportState.collectAsStateWithLifecycle()
    val currentTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val showIncomingShareDialog by viewModel.showIncomingShareDialog.collectAsStateWithLifecycle()
    val incomingSharedUris by viewModel.incomingSharedUris.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            snackbarHostState.showSnackbar(toastMessage!!)
            viewModel.clearToast()
        }
    }

    val showBottomBar = isMainUnlocked && currentScreen in listOf(
        Screen.DASHBOARD,
        Screen.DOCUMENTS,
        Screen.SEARCH,
        Screen.SETTINGS
    )

    BackHandler(enabled = isMainUnlocked && currentScreen != Screen.DASHBOARD) {
        viewModel.navigateBack()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(currentTheme.bgColor),
        bottomBar = {
            if (showBottomBar) {
                VaultBottomBar(
                    currentScreen = currentScreen,
                    onNavigate = { screen -> viewModel.navigateTo(screen) }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = currentTheme.bgColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.SPLASH -> {
                    SplashScreen()
                }

                // App Lock Setup & Change
                Screen.CHOOSE_APP_LOCK_METHOD -> {
                    val isChanging by viewModel.isChangingAppLockMethod.collectAsStateWithLifecycle()
                    ChooseAppLockScreen(
                        viewModel = viewModel,
                        isChangingMethod = isChanging,
                        onBack = {
                            if (isChanging) {
                                viewModel.navigateTo(Screen.SETTINGS)
                            }
                        }
                    )
                }

                Screen.SETUP_APP_LOCK -> {
                    val lockType by viewModel.setupAppLockType.collectAsStateWithLifecycle()
                    val isChanging by viewModel.isChangingAppLockMethod.collectAsStateWithLifecycle()
                    SetupAppLockScreen(
                        viewModel = viewModel,
                        lockType = lockType,
                        isChangingMethod = isChanging,
                        onBiometricRequested = { onAuthSuccess ->
                            onBiometricRequested { onAuthSuccess() }
                        },
                        onBack = {
                            viewModel.navigateTo(Screen.CHOOSE_APP_LOCK_METHOD)
                        },
                        onComplete = {
                            viewModel.onAppLockSetupComplete()
                        }
                    )
                }

                Screen.CHANGE_APP_LOCK_AUTH -> {
                    ChangeAppLockAuthScreen(
                        viewModel = viewModel,
                        onBiometricRequested = { onAuthSuccess ->
                            onBiometricRequested { onAuthSuccess() }
                        },
                        onAuthenticated = {
                            viewModel.navigateTo(Screen.CHOOSE_APP_LOCK_METHOD)
                        },
                        onBack = {
                            viewModel.navigateTo(Screen.SETTINGS)
                        }
                    )
                }

                // Main Vault Auth
                Screen.SETUP_MAIN_PIN -> {
                    SetupPinScreen(
                        viewModel = viewModel,
                        isPrivateVaultSetup = false,
                        onSetupComplete = { viewModel.unlockMainVault() }
                    )
                }

                Screen.MAIN_LOCK -> {
                    MainLockScreen(
                        viewModel = viewModel,
                        onBiometricRequested = {
                            onBiometricRequested { viewModel.unlockMainVault() }
                        }
                    )
                }

                // Private Vault Auth
                Screen.SETUP_PRIVATE_PIN -> {
                    SetupPinScreen(
                        viewModel = viewModel,
                        isPrivateVaultSetup = true,
                        onSetupComplete = { viewModel.unlockPrivateVault() }
                    )
                }

                Screen.PRIVATE_LOCK -> {
                    PrivateLockScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() },
                        onTriggerBiometric = {
                            onBiometricRequested { viewModel.unlockPrivateVault() }
                        }
                    )
                }

                // Hidden Vault Auth
                Screen.SETUP_HIDDEN_PIN -> {
                    SetupHiddenPinScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }

                Screen.HIDDEN_LOCK -> {
                    HiddenLockScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() },
                        onTriggerBiometric = {
                            onBiometricRequested { viewModel.unlockHiddenVault() }
                        }
                    )
                }

                // Main Vault Screens
                Screen.DASHBOARD -> {
                    DashboardScreen(viewModel = viewModel)
                }

                Screen.DOCUMENTS -> {
                    DocumentsScreen(viewModel = viewModel)
                }

                Screen.ADD_DOCUMENT -> {
                    AddDocumentScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }

                Screen.DOCUMENT_DETAIL -> {
                    DocumentDetailScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }

                Screen.DOCUMENT_SCANNER -> {
                    DocumentScannerScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }

                // Private Vault Screens
                Screen.PRIVATE_VAULT_HOME -> {
                    PrivateVaultHomeScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }

                Screen.PHOTO_VIEWER -> {
                    PhotoViewerScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }

                // Hidden Vault Screens
                Screen.HIDDEN_VAULT_HOME -> {
                    HiddenVaultHomeScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }

                Screen.HIDDEN_MEDIA_VIEWER -> {
                    HiddenMediaViewerScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }

                Screen.HIDDEN_DOCUMENT_VIEWER -> {
                    HiddenDocumentViewerScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }

                Screen.HIDDEN_ADD_ITEM -> {
                    HiddenAddItemScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }

                Screen.HIDDEN_TRASH -> {
                    HiddenTrashScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }

                Screen.HIDDEN_SEARCH -> {
                    HiddenSearchScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }

                // Hidden Hidden Vault Screens (Pattern Lock Protected - No Fingerprint)
                Screen.HIDDEN_HIDDEN_LOCK -> {
                    HiddenHiddenLockScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }

                Screen.HIDDEN_HIDDEN_VAULT_HOME -> {
                    HiddenHiddenVaultHomeScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }

                Screen.HIDDEN_HIDDEN_MEDIA_VIEWER -> {
                    HiddenHiddenMediaViewerScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }

                // General Screens
                Screen.SEARCH -> {
                    SearchScreen(viewModel = viewModel)
                }

                Screen.FAVORITES -> {
                    FavoritesScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }

                Screen.TRASH -> {
                    TrashScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }

                Screen.SETTINGS -> {
                    SettingsScreen(viewModel = viewModel)
                }

                Screen.STORAGE_MANAGER -> {
                    StorageManagerScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }

                // Security & Privacy Screens
                Screen.PRIVACY_CENTER -> {
                    PrivacyCenterScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }

                Screen.SECURITY_LOGS -> {
                    SecurityLogsScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }

                Screen.BACKUP_RESTORE -> {
                    BackupRestoreScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }

                Screen.PERMISSION_MANAGER -> {
                    PermissionManagerScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }

                Screen.ALBUMS -> {
                    PrivateVaultHomeScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }

                Screen.HIDDEN_FAVORITES -> {
                    HiddenVaultHomeScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }
            }

            ImportProgressDialog(
                state = multiImportState,
                onDismiss = { viewModel.dismissMultiImport() },
                onRetry = { viewModel.retryFailedImports() }
            )

            BulkImportConfirmDialog(
                plan = bulkImportState.plan,
                isPreparing = bulkImportState.status == BulkImportStatus.PREPARING,
                onConfirm = { mode, skipDuplicates, isMove ->
                    viewModel.confirmAndStartBulkImport(mode, skipDuplicates, isMove)
                },
                onDismiss = { viewModel.dismissBulkImport() }
            )

            BulkImportProgressDialog(
                state = bulkImportState,
                onPause = { viewModel.pauseBulkImport() },
                onResume = { viewModel.resumeBulkImport() },
                onCancel = { viewModel.cancelBulkImport() },
                onRetryFailed = { viewModel.retryFailedBulkImport() },
                onDismiss = { viewModel.dismissBulkImport() }
            )

            if (showIncomingShareDialog && incomingSharedUris.isNotEmpty()) {
                WhatsAppShareImportDialog(
                    uris = incomingSharedUris,
                    onImportToVault = { targetVault ->
                        viewModel.importSharedUris(targetVault)
                    },
                    onDismiss = {
                        viewModel.dismissIncomingShareDialog()
                    }
                )
            }
        }
    }
}
