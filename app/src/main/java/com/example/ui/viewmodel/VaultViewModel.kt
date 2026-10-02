package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Album
import com.example.data.model.Document
import com.example.data.model.HiddenAlbum
import com.example.data.model.HiddenItem
import com.example.data.model.HiddenTrashItem
import com.example.data.model.PrivateMedia
import com.example.data.model.SecurityActivityLog
import com.example.data.model.TrashItem
import com.example.data.repository.StorageInfo
import com.example.data.repository.VaultRepository
import com.example.data.security.AuthManager
import com.example.data.security.CryptoManager
import com.example.data.security.VaultType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.example.data.pdf.PdfDocumentManager
import com.example.data.export.FileDownloadHelper
import com.example.data.security.AppLockType

data class PdfViewerState(
    val isLoading: Boolean = false,
    val totalPages: Int = 0,
    val currentPageIndex: Int = 0,
    val currentPageBitmap: Bitmap? = null,
    val pageThumbnails: List<Bitmap?> = emptyList(),
    val errorMessage: String? = null,
    val currentFileName: String = "Document.pdf"
)

enum class Screen {
    SPLASH,
    CHOOSE_APP_LOCK_METHOD,
    SETUP_APP_LOCK,
    CHANGE_APP_LOCK_AUTH,
    SETUP_MAIN_PIN,
    MAIN_LOCK,
    DASHBOARD,
    DOCUMENTS,
    ADD_DOCUMENT,
    DOCUMENT_DETAIL,
    DOCUMENT_SCANNER,
    PRIVATE_LOCK,
    SETUP_PRIVATE_PIN,
    PRIVATE_VAULT_HOME,
    PHOTO_VIEWER,
    ALBUMS,
    SEARCH,
    FAVORITES,
    TRASH,
    SETTINGS,
    STORAGE_MANAGER,
    // --- Hidden Vault & High Security Screens ---
    SETUP_HIDDEN_PIN,
    HIDDEN_LOCK,
    HIDDEN_VAULT_HOME,
    HIDDEN_MEDIA_VIEWER,
    HIDDEN_DOCUMENT_VIEWER,
    HIDDEN_ADD_ITEM,
    HIDDEN_SEARCH,
    HIDDEN_FAVORITES,
    HIDDEN_TRASH,
    PRIVACY_CENTER,
    PERMISSION_MANAGER,
    SECURITY_LOGS,
    BACKUP_RESTORE,
    // --- Hidden Hidden Vault (Pattern Lock) ---
    HIDDEN_HIDDEN_LOCK,
    HIDDEN_HIDDEN_VAULT_HOME,
    HIDDEN_HIDDEN_MEDIA_VIEWER
}

data class DocumentFilter(
    val category: String? = null,
    val isExpiringSoonOnly: Boolean = false,
    val isExpiredOnly: Boolean = false,
    val sortBy: SortBy = SortBy.DATE_DESC
)

enum class SortBy {
    DATE_DESC,
    DATE_ASC,
    NAME_ASC,
    EXPIRY_SOON,
    FILE_TYPE,
    FILE_SIZE
}

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    val cryptoManager = CryptoManager(application)
    val authManager = AuthManager(application)
    val repository = VaultRepository(application, database, cryptoManager)
    val bulkImportManager = com.example.data.bulk.BulkImportManager(application, database, cryptoManager, repository.thumbnailManager)
    val bulkImportState: StateFlow<com.example.data.bulk.BulkImportState> = bulkImportManager.state

    private val _photoStorageMode = MutableStateFlow(com.example.data.bulk.PhotoStorageMode.ORIGINAL)
    val photoStorageMode: StateFlow<com.example.data.bulk.PhotoStorageMode> = _photoStorageMode.asStateFlow()

    fun setPhotoStorageMode(mode: com.example.data.bulk.PhotoStorageMode) {
        _photoStorageMode.value = mode
    }

    // ==========================================
    // NAVIGATION & LOCK STATES
    // ==========================================
    private val _currentScreen = MutableStateFlow<Screen>(
        if (!authManager.isAppLockConfigured()) Screen.CHOOSE_APP_LOCK_METHOD else Screen.MAIN_LOCK
    )
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _isMainUnlocked = MutableStateFlow(false)
    val isMainUnlocked: StateFlow<Boolean> = _isMainUnlocked.asStateFlow()

    private val _setupAppLockType = MutableStateFlow<AppLockType>(AppLockType.PIN)
    val setupAppLockType: StateFlow<AppLockType> = _setupAppLockType.asStateFlow()

    private val _isChangingAppLockMethod = MutableStateFlow(false)
    val isChangingAppLockMethod: StateFlow<Boolean> = _isChangingAppLockMethod.asStateFlow()

    private val _isPrivateVaultUnlocked = MutableStateFlow(false)
    val isPrivateVaultUnlocked: StateFlow<Boolean> = _isPrivateVaultUnlocked.asStateFlow()

    private val _isHiddenVaultUnlocked = MutableStateFlow(false)
    val isHiddenVaultUnlocked: StateFlow<Boolean> = _isHiddenVaultUnlocked.asStateFlow()

    private val _isHiddenVaultEnabled = MutableStateFlow(authManager.isHiddenVaultEnabled)
    val isHiddenVaultEnabled: StateFlow<Boolean> = _isHiddenVaultEnabled.asStateFlow()

    fun setHiddenVaultEnabled(enabled: Boolean) {
        authManager.isHiddenVaultEnabled = enabled
        _isHiddenVaultEnabled.value = enabled
        if (!enabled) {
            lockHiddenVault()
        }
    }

    // Screen navigation stack
    private val backStack = mutableListOf<Screen>()

    // ==========================================
    // 1. MAIN VAULT DATA
    // ==========================================
    val allDocuments = repository.allDocuments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favoriteDocuments = repository.favoriteDocuments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val totalDocumentsCount = repository.totalDocumentsCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val pdfCount = repository.pdfCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val imageCount = repository.imageCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _documentFilter = MutableStateFlow(DocumentFilter())
    val documentFilter: StateFlow<DocumentFilter> = _documentFilter.asStateFlow()

    val filteredDocuments: StateFlow<List<Document>> = combine(allDocuments, _documentFilter) { docs, filter ->
        var result = docs
        if (filter.category != null) {
            result = result.filter { it.category.equals(filter.category, ignoreCase = true) }
        }
        if (filter.isExpiringSoonOnly) {
            result = result.filter { it.isExpiringSoon }
        }
        if (filter.isExpiredOnly) {
            result = result.filter { it.isExpired }
        }
        when (filter.sortBy) {
            SortBy.DATE_DESC -> result.sortedByDescending { it.updatedAt }
            SortBy.DATE_ASC -> result.sortedBy { it.updatedAt }
            SortBy.NAME_ASC -> result.sortedBy { it.name.lowercase() }
            SortBy.EXPIRY_SOON -> result.sortedBy { it.expiryDate ?: Long.MAX_VALUE }
            SortBy.FILE_TYPE -> result.sortedBy { it.fileType.lowercase() }
            SortBy.FILE_SIZE -> result.sortedByDescending { it.fileSizeBytes }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ==========================================
    // 2. PRIVATE PHOTO VAULT DATA
    // ==========================================
    val allPrivateMedia = repository.allPrivateMedia.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favoriteMedia = repository.favoriteMedia.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allAlbums = repository.allAlbums.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val photosCount = repository.totalPhotosCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val videosCount = repository.totalVideosCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _selectedAlbumId = MutableStateFlow<Long?>(null)
    val selectedAlbumId: StateFlow<Long?> = _selectedAlbumId.asStateFlow()

    val filteredPrivateMedia: StateFlow<List<PrivateMedia>> = combine(allPrivateMedia, _selectedAlbumId) { mediaList, albumId ->
        if (albumId == null) mediaList else mediaList.filter { it.albumId == albumId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Trash
    val allTrashItems = repository.allTrashItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ==========================================
    // 3. HIDDEN VAULT DATA (STRICTLY ISOLATED)
    // ==========================================
    val allHiddenItems = repository.allHiddenItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val hiddenPhotos = repository.hiddenPhotos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val hiddenVideos = repository.hiddenVideos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val hiddenDocuments = repository.hiddenDocuments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favoriteHiddenItems = repository.favoriteHiddenItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recentlyOpenedHiddenItems = repository.recentlyOpenedItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allHiddenAlbums = repository.allHiddenAlbums.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allHiddenTrashItems = repository.allHiddenTrashItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hiddenTotalCount = repository.hiddenTotalCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val hiddenPhotosCount = repository.hiddenPhotosCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val hiddenVideosCount = repository.hiddenVideosCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val hiddenDocsCount = repository.hiddenDocsCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val hiddenAlbumsCount = repository.hiddenAlbumsCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val hiddenTrashCount = repository.hiddenTrashCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allSecurityLogs = repository.allSecurityLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedHiddenAlbumId = MutableStateFlow<Long?>(null)
    val selectedHiddenAlbumId: StateFlow<Long?> = _selectedHiddenAlbumId.asStateFlow()

    // Multi-select for Hidden Vault
    private val _selectedHiddenIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedHiddenIds: StateFlow<Set<Long>> = _selectedHiddenIds.asStateFlow()

    val isMultiSelectMode: StateFlow<Boolean> = combine(_selectedHiddenIds) { ids ->
        ids[0].isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Hidden Search (Isolated)
    private val _hiddenSearchQuery = MutableStateFlow("")
    val hiddenSearchQuery: StateFlow<String> = _hiddenSearchQuery.asStateFlow()

    val hiddenSearchResults: StateFlow<List<HiddenItem>> = combine(allHiddenItems, _hiddenSearchQuery) { items, query ->
        if (query.isBlank()) emptyList()
        else {
            val q = query.trim().lowercase()
            items.filter {
                it.name.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.tags.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.ocrText.lowercase().contains(q) ||
                it.notes.lowercase().contains(q)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Items for Viewers
    private val _selectedDocument = MutableStateFlow<Document?>(null)
    val selectedDocument: StateFlow<Document?> = _selectedDocument.asStateFlow()

    private val _selectedMedia = MutableStateFlow<PrivateMedia?>(null)
    val selectedMedia: StateFlow<PrivateMedia?> = _selectedMedia.asStateFlow()

    private val _viewerMediaList = MutableStateFlow<List<PrivateMedia>>(emptyList())
    val viewerMediaList: StateFlow<List<PrivateMedia>> = _viewerMediaList.asStateFlow()

    private val _selectedHiddenItem = MutableStateFlow<HiddenItem?>(null)
    val selectedHiddenItem: StateFlow<HiddenItem?> = _selectedHiddenItem.asStateFlow()

    private val _viewerHiddenItemList = MutableStateFlow<List<HiddenItem>>(emptyList())
    val viewerHiddenItemList: StateFlow<List<HiddenItem>> = _viewerHiddenItemList.asStateFlow()

    // ==========================================
    // 4. HIDDEN HIDDEN VAULT DATA (PATTERN LOCK)
    // ==========================================
    val allHiddenHiddenItems = database.hiddenHiddenDao().getAllItems().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val hiddenHiddenPhotos = database.hiddenHiddenDao().getPhotos().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val hiddenHiddenVideos = database.hiddenHiddenDao().getVideos().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val hiddenHiddenPhotosCount = database.hiddenHiddenDao().getPhotosCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val hiddenHiddenVideosCount = database.hiddenHiddenDao().getVideosCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val hiddenHiddenTotalCount = database.hiddenHiddenDao().getTotalCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val hiddenHiddenTotalSizeBytes = database.hiddenHiddenDao().getTotalSizeBytes().map { it ?: 0L }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _isHiddenHiddenUnlocked = MutableStateFlow(false)
    val isHiddenHiddenUnlocked: StateFlow<Boolean> = _isHiddenHiddenUnlocked.asStateFlow()

    private val _selectedHiddenHiddenItem = MutableStateFlow<com.example.data.model.HiddenHiddenItem?>(null)
    val selectedHiddenHiddenItem: StateFlow<com.example.data.model.HiddenHiddenItem?> = _selectedHiddenHiddenItem.asStateFlow()

    private val _viewerHiddenHiddenItemList = MutableStateFlow<List<com.example.data.model.HiddenHiddenItem>>(emptyList())
    val viewerHiddenHiddenItemList: StateFlow<List<com.example.data.model.HiddenHiddenItem>> = _viewerHiddenHiddenItemList.asStateFlow()

    private val _selectedHiddenHiddenIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedHiddenHiddenIds: StateFlow<Set<Long>> = _selectedHiddenHiddenIds.asStateFlow()

    private val _isHiddenHiddenSelectMode = MutableStateFlow(false)
    val isHiddenHiddenSelectMode: StateFlow<Boolean> = _isHiddenHiddenSelectMode.asStateFlow()

    private val _hiddenHiddenSearchQuery = MutableStateFlow("")
    val hiddenHiddenSearchQuery: StateFlow<String> = _hiddenHiddenSearchQuery.asStateFlow()

    // Decrypted Bitmap Caches
    private val _decryptedViewerBitmap = MutableStateFlow<Bitmap?>(null)
    val decryptedViewerBitmap: StateFlow<Bitmap?> = _decryptedViewerBitmap.asStateFlow()

    private val _decryptedHiddenBitmap = MutableStateFlow<Bitmap?>(null)
    val decryptedHiddenBitmap: StateFlow<Bitmap?> = _decryptedHiddenBitmap.asStateFlow()

    // Native PDF Viewer Management & State
    val pdfDocumentManager = PdfDocumentManager(application)
    private val _pdfViewerState = MutableStateFlow<PdfViewerState?>(null)
    val pdfViewerState: StateFlow<PdfViewerState?> = _pdfViewerState.asStateFlow()

    // Re-Authentication for Export/Share
    private val _pendingReAuthAction = MutableStateFlow<(() -> Unit)?>(null)
    val pendingReAuthAction: StateFlow<(() -> Unit)?> = _pendingReAuthAction.asStateFlow()

    // Normal Search (Main Vault only)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<Document>> = combine(allDocuments, _searchQuery) { docs, query ->
        if (query.isBlank()) emptyList()
        else {
            val q = query.trim().lowercase()
            docs.filter {
                it.name.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.tags.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.ocrText.lowercase().contains(q)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Storage info
    private val _storageInfo = MutableStateFlow(StorageInfo())
    val storageInfo: StateFlow<StorageInfo> = _storageInfo.asStateFlow()

    // Multi-File Import State
    data class MultiImportState(
        val isImporting: Boolean = false,
        val totalCount: Int = 0,
        val currentProgress: Int = 0,
        val successCount: Int = 0,
        val failedCount: Int = 0,
        val currentFileName: String = "",
        val failedUris: List<Uri> = emptyList(),
        val isComplete: Boolean = false,
        val errorMessage: String? = null,
        val importType: String = "DOCUMENTS",
        val categoryOrAlbum: String = "General"
    )

    private val _multiImportState = MutableStateFlow(MultiImportState())
    val multiImportState: StateFlow<MultiImportState> = _multiImportState.asStateFlow()

    // Toast
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Dynamic App Theme
    private val _appTheme = MutableStateFlow(
        com.example.ui.theme.AppThemeStyle.fromId(authManager.appThemeStyle)
    )
    val appTheme: StateFlow<com.example.ui.theme.AppThemeStyle> = _appTheme.asStateFlow()

    fun setAppTheme(theme: com.example.ui.theme.AppThemeStyle) {
        authManager.appThemeStyle = theme.id
        _appTheme.value = theme
        showToast("Theme changed to ${theme.displayName}")
    }

    // Direct Share from WhatsApp / External Apps
    private val _incomingSharedUris = MutableStateFlow<List<Uri>>(emptyList())
    val incomingSharedUris: StateFlow<List<Uri>> = _incomingSharedUris.asStateFlow()

    private val _showIncomingShareDialog = MutableStateFlow(false)
    val showIncomingShareDialog: StateFlow<Boolean> = _showIncomingShareDialog.asStateFlow()

    fun handleIncomingSharedUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        _incomingSharedUris.value = uris
        _showIncomingShareDialog.value = true
    }

    fun dismissIncomingShareDialog() {
        _showIncomingShareDialog.value = false
        _incomingSharedUris.value = emptyList()
    }

    fun importSharedUris(targetVault: VaultType) {
        val uris = _incomingSharedUris.value
        if (uris.isEmpty()) return
        _showIncomingShareDialog.value = false
        _incomingSharedUris.value = emptyList()

        when (targetVault) {
            VaultType.PRIVATE -> {
                importMultiplePhotos(uris, selectedAlbumId.value)
            }
            VaultType.MAIN -> {
                initiateBulkImport(uris, VaultType.MAIN, "DOCUMENT", null)
            }
            VaultType.HIDDEN -> {
                importMultipleHiddenItems(uris, "PHOTO", selectedHiddenAlbumId.value)
            }
            VaultType.HIDDEN_HIDDEN -> {
                importHiddenHiddenFiles(uris)
            }
        }
    }

    init {
        checkInitialLockState()
        viewModelScope.launch {
            refreshStorageInfo()
        }
    }

    fun checkInitialLockState() {
        if (!authManager.isAppLockConfigured()) {
            _isMainUnlocked.value = false
            _currentScreen.value = Screen.CHOOSE_APP_LOCK_METHOD
        } else {
            _isMainUnlocked.value = false
            _currentScreen.value = Screen.MAIN_LOCK
        }
    }

    fun startAppLockSetup(lockType: AppLockType, isChanging: Boolean = false) {
        _setupAppLockType.value = lockType
        _isChangingAppLockMethod.value = isChanging
        _currentScreen.value = Screen.SETUP_APP_LOCK
    }

    fun openChooseAppLock(isChanging: Boolean = false) {
        _isChangingAppLockMethod.value = isChanging
        if (isChanging) {
            _currentScreen.value = Screen.CHANGE_APP_LOCK_AUTH
        } else {
            _currentScreen.value = Screen.CHOOSE_APP_LOCK_METHOD
        }
    }

    fun onAppLockSetupComplete() {
        val wasChanging = _isChangingAppLockMethod.value
        _isChangingAppLockMethod.value = false
        _isMainUnlocked.value = true
        _currentScreen.value = if (wasChanging) Screen.SETTINGS else Screen.DASHBOARD
        backStack.clear()
        viewModelScope.launch {
            refreshStorageInfo()
            repository.logSecurityEvent("APP_LOCK_CONFIGURED", "App Lock configured with ${authManager.appLockType}")
        }
    }

    fun navigateTo(screen: Screen) {
        // Enforce Private Vault authentication
        if (screen == Screen.PRIVATE_VAULT_HOME && !_isPrivateVaultUnlocked.value) {
            if (!authManager.isPrivatePinSet()) {
                backStack.add(_currentScreen.value)
                _currentScreen.value = Screen.SETUP_PRIVATE_PIN
            } else {
                backStack.add(_currentScreen.value)
                _currentScreen.value = Screen.PRIVATE_LOCK
            }
            return
        }

        // Enforce Hidden Vault authentication and enabled status
        if (screen in listOf(
                Screen.HIDDEN_VAULT_HOME,
                Screen.HIDDEN_MEDIA_VIEWER,
                Screen.HIDDEN_DOCUMENT_VIEWER,
                Screen.HIDDEN_ADD_ITEM,
                Screen.HIDDEN_SEARCH,
                Screen.HIDDEN_FAVORITES,
                Screen.HIDDEN_TRASH
            )
        ) {
            if (!authManager.isHiddenVaultEnabled) {
                showToast("Hidden Vault is disabled. Turn it ON in Settings to access.")
                return
            }
            if (!_isHiddenVaultUnlocked.value) {
                if (!authManager.isHiddenPinSet()) {
                    backStack.add(_currentScreen.value)
                    _currentScreen.value = Screen.SETUP_HIDDEN_PIN
                } else {
                    backStack.add(_currentScreen.value)
                    _currentScreen.value = Screen.HIDDEN_LOCK
                }
                return
            }
        }

        if (_currentScreen.value != screen) {
            backStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        if (backStack.isNotEmpty()) {
            val prev = backStack.removeAt(backStack.size - 1)
            // If leaving Hidden Vault, auto-lock it if configured
            if (_currentScreen.value in listOf(
                    Screen.HIDDEN_VAULT_HOME,
                    Screen.HIDDEN_MEDIA_VIEWER,
                    Screen.HIDDEN_DOCUMENT_VIEWER,
                    Screen.HIDDEN_ADD_ITEM,
                    Screen.HIDDEN_SEARCH,
                    Screen.HIDDEN_FAVORITES,
                    Screen.HIDDEN_TRASH
                ) && prev !in listOf(
                    Screen.HIDDEN_VAULT_HOME,
                    Screen.HIDDEN_MEDIA_VIEWER,
                    Screen.HIDDEN_DOCUMENT_VIEWER,
                    Screen.HIDDEN_ADD_ITEM,
                    Screen.HIDDEN_SEARCH,
                    Screen.HIDDEN_FAVORITES,
                    Screen.HIDDEN_TRASH
                )
            ) {
                lockHiddenVault()
            }

            // If leaving Hidden Hidden Vault, auto-lock it
            if (_currentScreen.value in listOf(
                    Screen.HIDDEN_HIDDEN_VAULT_HOME,
                    Screen.HIDDEN_HIDDEN_MEDIA_VIEWER
                ) && prev !in listOf(
                    Screen.HIDDEN_HIDDEN_VAULT_HOME,
                    Screen.HIDDEN_HIDDEN_MEDIA_VIEWER
                )
            ) {
                lockHiddenHiddenVault()
            }

            _currentScreen.value = prev
            return true
        }
        if (_currentScreen.value != Screen.DASHBOARD && _isMainUnlocked.value) {
            _currentScreen.value = Screen.DASHBOARD
            return true
        }
        return false
    }

    // ==========================================
    // UNLOCK / LOCK METHODS
    // ==========================================
    fun unlockMainVault() {
        _isMainUnlocked.value = true
        _currentScreen.value = Screen.DASHBOARD
        backStack.clear()
        viewModelScope.launch {
            repository.logSecurityEvent("VAULT_UNLOCKED", "Main Vault unlocked")
            refreshStorageInfo()
        }
    }

    fun unlockPrivateVault() {
        _isPrivateVaultUnlocked.value = true
        _currentScreen.value = Screen.PRIVATE_VAULT_HOME
        viewModelScope.launch {
            repository.logSecurityEvent("PRIVATE_VAULT_UNLOCKED", "Private Photo Vault unlocked")
            refreshStorageInfo()
        }
    }

    fun lockPrivateVault() {
        _isPrivateVaultUnlocked.value = false
        if (_currentScreen.value in listOf(Screen.PRIVATE_VAULT_HOME, Screen.PHOTO_VIEWER, Screen.ALBUMS)) {
            _currentScreen.value = Screen.DASHBOARD
        }
    }

    fun unlockHiddenVault() {
        _isHiddenVaultUnlocked.value = true
        _currentScreen.value = Screen.HIDDEN_VAULT_HOME
        viewModelScope.launch {
            repository.logSecurityEvent("HIDDEN_VAULT_UNLOCKED", "Highest-Security Hidden Vault unlocked")
            refreshStorageInfo()
        }
    }

    fun lockHiddenVault() {
        _isHiddenVaultUnlocked.value = false
        _selectedHiddenItem.value = null
        _decryptedHiddenBitmap.value = null
        clearHiddenSelection()
        if (_currentScreen.value in listOf(
                Screen.HIDDEN_VAULT_HOME,
                Screen.HIDDEN_MEDIA_VIEWER,
                Screen.HIDDEN_DOCUMENT_VIEWER,
                Screen.HIDDEN_ADD_ITEM,
                Screen.HIDDEN_SEARCH,
                Screen.HIDDEN_FAVORITES,
                Screen.HIDDEN_TRASH
            )
        ) {
            _currentScreen.value = Screen.SETTINGS
        }
    }

    // ==========================================
    // HIDDEN HIDDEN VAULT UNLOCK / LOCK METHODS
    // ==========================================
    fun openHiddenHiddenVault() {
        if (_isHiddenHiddenUnlocked.value) {
            navigateTo(Screen.HIDDEN_HIDDEN_VAULT_HOME)
        } else {
            navigateTo(Screen.HIDDEN_HIDDEN_LOCK)
        }
    }

    fun unlockHiddenHiddenVault() {
        _isHiddenHiddenUnlocked.value = true
        _currentScreen.value = Screen.HIDDEN_HIDDEN_VAULT_HOME
        viewModelScope.launch {
            repository.logSecurityEvent("HIDDEN_HIDDEN_UNLOCKED", "Hidden Hidden Vault unlocked with pattern")
            refreshStorageInfo()
        }
    }

    fun lockHiddenHiddenVault() {
        _isHiddenHiddenUnlocked.value = false
        _selectedHiddenHiddenItem.value = null
        _selectedHiddenHiddenIds.value = emptySet()
        _isHiddenHiddenSelectMode.value = false
        if (_currentScreen.value in listOf(
                Screen.HIDDEN_HIDDEN_VAULT_HOME,
                Screen.HIDDEN_HIDDEN_MEDIA_VIEWER
            )
        ) {
            _currentScreen.value = Screen.HIDDEN_VAULT_HOME
        }
    }

    // Panic / Quick Lock All Vaults
    fun lockAll() {
        _isMainUnlocked.value = false
        _isPrivateVaultUnlocked.value = false
        _isHiddenVaultUnlocked.value = false
        _isHiddenHiddenUnlocked.value = false
        _selectedDocument.value = null
        _selectedMedia.value = null
        _selectedHiddenItem.value = null
        _selectedHiddenHiddenItem.value = null
        _selectedHiddenHiddenIds.value = emptySet()
        _isHiddenHiddenSelectMode.value = false
        _decryptedViewerBitmap.value = null
        _decryptedHiddenBitmap.value = null
        closePdfViewer()
        clearHiddenSelection()
        backStack.clear()
        _currentScreen.value = Screen.MAIN_LOCK
        showToast("Vault locked")
        viewModelScope.launch {
            repository.logSecurityEvent("PANIC_LOCK", "Quick Lock triggered: All vaults locked instantly")
        }
    }

    fun lockAllVaults() = lockAll()

    // ==========================================
    // RE-AUTHENTICATION DIALOG
    // ==========================================
    fun promptReAuthForAction(action: () -> Unit) {
        _pendingReAuthAction.value = action
    }

    fun executePendingReAuth() {
        _pendingReAuthAction.value?.invoke()
        _pendingReAuthAction.value = null
    }

    fun dismissReAuth() {
        _pendingReAuthAction.value = null
    }

    // ==========================================
    // MAIN VAULT DOCUMENT ACTIONS
    // ==========================================
    fun selectDocument(doc: Document) {
        _selectedDocument.value = doc
        loadDocumentContent(doc)
        navigateTo(Screen.DOCUMENT_DETAIL)
    }

    fun loadDocumentContent(doc: Document) {
        viewModelScope.launch {
            _decryptedViewerBitmap.value = null
            _pdfViewerState.value = null

            val bytes = withContext(Dispatchers.IO) {
                cryptoManager.decryptFileToBytes(File(doc.filePath), VaultType.MAIN)
            }
            if (bytes != null) {
                val isPdf = doc.fileType == "PDF" ||
                        doc.filePath.endsWith(".pdf", ignoreCase = true) ||
                        doc.filePath.endsWith("enc_pdf", ignoreCase = true) ||
                        PdfDocumentManager.isPdf(bytes)

                if (isPdf) {
                    val safeName = if (doc.name.endsWith(".pdf", ignoreCase = true)) doc.name else "${doc.name}.pdf"
                    _pdfViewerState.value = PdfViewerState(isLoading = true, currentFileName = safeName)
                    val success = pdfDocumentManager.openFromBytes(bytes)
                    if (success && pdfDocumentManager.pageCount > 0) {
                        val firstPage = pdfDocumentManager.renderPage(0, densityScale = 2.0f)
                        val thumbs = mutableListOf<Bitmap?>()
                        for (i in 0 until minOf(pdfDocumentManager.pageCount, 12)) {
                            thumbs.add(pdfDocumentManager.renderThumbnail(i, 160))
                        }
                        _pdfViewerState.value = PdfViewerState(
                            isLoading = false,
                            totalPages = pdfDocumentManager.pageCount,
                            currentPageIndex = 0,
                            currentPageBitmap = firstPage,
                            pageThumbnails = thumbs,
                            currentFileName = safeName
                        )
                    } else {
                        _pdfViewerState.value = PdfViewerState(
                            isLoading = false,
                            errorMessage = "Unable to render PDF pages"
                        )
                    }
                } else {
                    _decryptedViewerBitmap.value = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
            } else {
                showToast("Failed to decrypt document")
            }
        }
    }

    fun goToPdfPage(pageIndex: Int) {
        val currentState = _pdfViewerState.value ?: return
        if (pageIndex < 0 || pageIndex >= currentState.totalPages) return
        viewModelScope.launch {
            _pdfViewerState.value = currentState.copy(isLoading = true)
            val pageBitmap = pdfDocumentManager.renderPage(pageIndex, densityScale = 2.0f)
            _pdfViewerState.value = currentState.copy(
                isLoading = false,
                currentPageIndex = pageIndex,
                currentPageBitmap = pageBitmap
            )
        }
    }

    fun nextPdfPage() {
        val current = _pdfViewerState.value?.currentPageIndex ?: return
        goToPdfPage(current + 1)
    }

    fun prevPdfPage() {
        val current = _pdfViewerState.value?.currentPageIndex ?: return
        goToPdfPage(current - 1)
    }

    fun closePdfViewer() {
        pdfDocumentManager.close()
        _pdfViewerState.value = null
    }

    // Direct Download to Device (Public Downloads Folder)
    fun downloadDocument(doc: Document, onComplete: (Boolean, String, Uri?) -> Unit) {
        viewModelScope.launch {
            val isPdf = doc.fileType == "PDF" || doc.filePath.endsWith("enc_pdf")
            val ext = if (isPdf) ".pdf" else ".png"
            val mimeType = if (isPdf) "application/pdf" else "image/png"
            val baseName = doc.name.removeSuffix(".pdf").removeSuffix(".png").removeSuffix(".jpg").replace(" ", "_")
            val fileName = "$baseName$ext"

            val bytes = withContext(Dispatchers.IO) {
                cryptoManager.decryptFileToBytes(File(doc.filePath), VaultType.MAIN)
            }

            if (bytes == null) {
                onComplete(false, "Decryption failed", null)
                return@launch
            }

            val uri = withContext(Dispatchers.IO) {
                FileDownloadHelper.saveToDownloads(
                    getApplication(),
                    fileName,
                    mimeType,
                    bytes
                )
            }

            if (uri != null) {
                repository.logSecurityEvent("FILE_DOWNLOADED", "Saved to Downloads: $fileName")
                onComplete(true, "Saved to Downloads/$fileName", uri)
            } else {
                onComplete(false, "Failed to write file to Downloads", null)
            }
        }
    }

    // Direct Download for Hidden Vault Items
    fun downloadHiddenItem(item: HiddenItem, onComplete: (Boolean, String, Uri?) -> Unit) {
        viewModelScope.launch {
            val isPdf = item.itemType in listOf("DOCUMENT", "PDF", "SCAN") || item.encryptedPath.endsWith("enc_pdf")
            val ext = if (isPdf) ".pdf" else if (item.itemType == "VIDEO") ".mp4" else ".png"
            val mimeType = if (isPdf) "application/pdf" else if (item.itemType == "VIDEO") "video/mp4" else "image/png"
            val baseName = item.name.removeSuffix(".pdf").removeSuffix(".mp4").removeSuffix(".png").removeSuffix(".jpg").replace(" ", "_")
            val fileName = "$baseName$ext"

            val bytes = withContext(Dispatchers.IO) {
                cryptoManager.decryptFileToBytes(File(item.encryptedPath), VaultType.HIDDEN)
            }

            if (bytes == null) {
                onComplete(false, "Decryption failed", null)
                return@launch
            }

            val uri = withContext(Dispatchers.IO) {
                FileDownloadHelper.saveToDownloads(
                    getApplication(),
                    fileName,
                    mimeType,
                    bytes
                )
            }

            if (uri != null) {
                repository.logSecurityEvent("HIDDEN_FILE_DOWNLOADED", "Saved to Downloads: $fileName")
                onComplete(true, "Saved to Downloads/$fileName", uri)
            } else {
                onComplete(false, "Failed to write file to Downloads", null)
            }
        }
    }

    fun exportFileToChosenUri(targetUri: Uri, filePath: String, vaultType: VaultType, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) {
                cryptoManager.decryptFileToBytes(File(filePath), vaultType)
            }
            if (bytes == null) {
                onComplete(false)
                return@launch
            }
            val success = withContext(Dispatchers.IO) {
                FileDownloadHelper.saveToChosenUri(getApplication(), targetUri, bytes)
            }
            onComplete(success)
        }
    }

    fun addDocument(
        name: String,
        category: String,
        description: String,
        fileType: String,
        tags: String,
        expiryDate: Long?,
        isFavorite: Boolean,
        sourceUri: Uri? = null,
        rawBytes: ByteArray? = null,
        ocrText: String? = null,
        openImmediately: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                val docId = repository.addDocument(
                    name = name,
                    category = category,
                    description = description,
                    fileType = fileType,
                    tags = tags,
                    expiryDate = expiryDate,
                    isFavorite = isFavorite,
                    sourceUri = sourceUri,
                    rawBytes = rawBytes,
                    extractedOcr = ocrText
                )
                refreshStorageInfo()
                showToast("Document saved to Main Vault")
                if (openImmediately) {
                    val newDoc = repository.getDocumentById(docId)
                    if (newDoc != null) {
                        selectDocument(newDoc)
                        return@launch
                    }
                }
                navigateBack()
            } catch (e: Throwable) {
                showToast(e.message ?: "Failed to save document")
            }
        }
    }

    fun toggleDocumentFavorite(doc: Document) {
        viewModelScope.launch {
            repository.toggleDocumentFavorite(doc)
        }
    }

    fun moveDocumentToTrash(doc: Document) {
        viewModelScope.launch {
            repository.moveDocumentToTrash(doc)
            _selectedDocument.value = null
            showToast("Moved to Trash")
            navigateBack()
            refreshStorageInfo()
        }
    }

    fun restoreTrashItem(item: TrashItem) {
        viewModelScope.launch {
            repository.restoreTrashItem(item)
            showToast("Item restored")
            refreshStorageInfo()
        }
    }

    fun permanentlyDeleteTrashItem(item: TrashItem) {
        viewModelScope.launch {
            repository.permanentlyDeleteTrashItem(item)
            showToast("Item permanently deleted")
            refreshStorageInfo()
        }
    }

    fun clearAllTrashPermanently() {
        viewModelScope.launch {
            repository.clearAllTrashPermanently()
            showToast("Trash emptied")
            refreshStorageInfo()
        }
    }

    // ==========================================
    // PRIVATE PHOTO VAULT ACTIONS
    // ==========================================
    fun selectPrivateMedia(media: PrivateMedia, contextList: List<PrivateMedia>? = null) {
        _selectedMedia.value = media
        _viewerMediaList.value = contextList ?: filteredPrivateMedia.value.ifEmpty { allPrivateMedia.value }
        loadMediaBitmap(media)
        navigateTo(Screen.PHOTO_VIEWER)
    }

    fun updateSelectedMediaWithoutNavigating(media: PrivateMedia) {
        _selectedMedia.value = media
    }

    fun updateSelectedHiddenItemWithoutNavigating(item: HiddenItem) {
        _selectedHiddenItem.value = item
    }

    fun updateSelectedHiddenHiddenItemWithoutNavigating(item: com.example.data.model.HiddenHiddenItem) {
        _selectedHiddenHiddenItem.value = item
    }

    fun downloadPrivateMedia(media: PrivateMedia, onComplete: (Boolean, String, Uri?) -> Unit) {
        viewModelScope.launch {
            val isVideo = media.mediaType == "VIDEO" || media.encryptedPath.endsWith("enc_vid")
            val ext = if (isVideo) ".mp4" else ".png"
            val mimeType = if (isVideo) "video/mp4" else "image/png"
            val baseName = media.name.removeSuffix(".mp4").removeSuffix(".png").removeSuffix(".jpg").replace(" ", "_")
            val fileName = "$baseName$ext"

            val bytes = withContext(Dispatchers.IO) {
                cryptoManager.decryptFileToBytes(File(media.encryptedPath), VaultType.PRIVATE)
            }

            if (bytes == null) {
                onComplete(false, "Decryption failed", null)
                return@launch
            }

            val uri = withContext(Dispatchers.IO) {
                FileDownloadHelper.saveToDownloads(
                    getApplication(),
                    fileName,
                    mimeType,
                    bytes
                )
            }

            if (uri != null) {
                repository.logSecurityEvent("PRIVATE_MEDIA_DOWNLOADED", "Saved to Downloads: $fileName")
                onComplete(true, "Saved to Downloads/$fileName", uri)
            } else {
                onComplete(false, "Failed to write file to Downloads", null)
            }
        }
    }

    private fun loadMediaBitmap(media: PrivateMedia) {
        viewModelScope.launch {
            _decryptedViewerBitmap.value = null
            val isVideo = media.mediaType == "VIDEO" ||
                    media.encryptedPath.endsWith("enc_vid") ||
                    media.encryptedPath.contains("vid", ignoreCase = true) ||
                    media.name.contains("Video", ignoreCase = true) ||
                    media.name.lowercase().let {
                        it.endsWith(".mp4") || it.endsWith(".mov") || it.endsWith(".mkv") || it.endsWith(".webm") ||
                                it.endsWith(".3gp") || it.endsWith(".m4v") || it.endsWith(".avi") || it.endsWith(".flv")
                    }
            if (isVideo) {
                // Videos are decrypted and played directly inside VideoPlayerComponent
                return@launch
            }
            val bytes = withContext(Dispatchers.IO) {
                cryptoManager.decryptFileToBytes(File(media.encryptedPath), VaultType.PRIVATE)
            }
            if (bytes != null) {
                _decryptedViewerBitmap.value = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        }
    }

    fun addPrivateMedia(
        name: String,
        mediaType: String,
        albumId: Long?,
        sourceUri: Uri? = null,
        rawBytes: ByteArray? = null
    ) {
        viewModelScope.launch {
            try {
                repository.addPrivateMedia(
                    name = name,
                    mediaType = mediaType,
                    albumId = albumId,
                    sourceUri = sourceUri,
                    rawBytes = rawBytes
                )
                refreshStorageInfo()
                showToast("Private media saved")
            } catch (e: Throwable) {
                showToast(e.message ?: "Failed to save private media")
            }
        }
    }

    fun toggleMediaFavorite(media: PrivateMedia) {
        viewModelScope.launch {
            repository.toggleMediaFavorite(media)
        }
    }

    fun renameMedia(media: PrivateMedia, newName: String) {
        viewModelScope.launch {
            repository.renameMedia(media, newName)
            _selectedMedia.value = media.copy(name = newName)
            showToast("Renamed successfully")
        }
    }

    fun moveMediaToAlbum(mediaIds: List<Long>, albumId: Long?) {
        viewModelScope.launch {
            repository.moveMediaToAlbum(mediaIds, albumId)
            showToast("Moved to album")
        }
    }

    fun moveMediaToTrash(media: PrivateMedia, fromViewer: Boolean = false) {
        viewModelScope.launch {
            val currentList = _viewerMediaList.value.ifEmpty { allPrivateMedia.value }
            val currentIndex = currentList.indexOfFirst { it.id == media.id }
            repository.moveMediaToTrash(media)
            val updatedList = currentList.filter { it.id != media.id }
            _viewerMediaList.value = updatedList
            if (fromViewer) {
                if (updatedList.isEmpty()) {
                    _selectedMedia.value = null
                    navigateBack()
                } else {
                    val nextIndex = if (currentIndex >= updatedList.size) updatedList.size - 1 else currentIndex.coerceAtLeast(0)
                    _selectedMedia.value = updatedList[nextIndex]
                }
            } else {
                _selectedMedia.value = null
                navigateBack()
            }
            showToast("Moved to Trash")
            refreshStorageInfo()
        }
    }

    fun createAlbum(name: String) {
        viewModelScope.launch {
            repository.createAlbum(name)
            showToast("Album created")
        }
    }

    fun selectAlbum(albumId: Long?) {
        _selectedAlbumId.value = albumId
    }

    // ==========================================
    // 3. HIDDEN VAULT ACTIONS
    // ==========================================
    fun selectHiddenItem(item: HiddenItem, contextList: List<HiddenItem>? = null) {
        _selectedHiddenItem.value = item
        _viewerHiddenItemList.value = contextList ?: when (item.itemType) {
            "PHOTO" -> hiddenPhotos.value.ifEmpty { allHiddenItems.value }
            "VIDEO" -> hiddenVideos.value.ifEmpty { allHiddenItems.value }
            else -> allHiddenItems.value
        }
        viewModelScope.launch {
            repository.recordHiddenItemOpened(item)
            loadHiddenItemContent(item)
        }
        val isVideo = item.itemType == "VIDEO" ||
                item.encryptedPath.endsWith("enc_vid") ||
                item.encryptedPath.contains("vid", ignoreCase = true) ||
                item.name.contains("Video", ignoreCase = true) ||
                item.name.lowercase().let {
                    it.endsWith(".mp4") || it.endsWith(".mov") || it.endsWith(".mkv") || it.endsWith(".webm") ||
                            it.endsWith(".3gp") || it.endsWith(".m4v") || it.endsWith(".avi") || it.endsWith(".flv")
                }
        if (isVideo || item.itemType in listOf("PHOTO", "VIDEO")) {
            navigateTo(Screen.HIDDEN_MEDIA_VIEWER)
        } else {
            navigateTo(Screen.HIDDEN_DOCUMENT_VIEWER)
        }
    }

    fun loadHiddenItemContent(item: HiddenItem) {
        viewModelScope.launch {
            _decryptedHiddenBitmap.value = null
            _pdfViewerState.value = null

            val isVideo = item.itemType == "VIDEO" ||
                    item.encryptedPath.endsWith("enc_vid") ||
                    item.encryptedPath.contains("vid", ignoreCase = true) ||
                    item.name.contains("Video", ignoreCase = true) ||
                    item.name.lowercase().let {
                        it.endsWith(".mp4") || it.endsWith(".mov") || it.endsWith(".mkv") || it.endsWith(".webm") ||
                                it.endsWith(".3gp") || it.endsWith(".m4v") || it.endsWith(".avi") || it.endsWith(".flv")
                    }
            if (isVideo) {
                // Videos are dynamically decrypted and played directly inside VideoPlayerComponent
                return@launch
            }

            val bytes = withContext(Dispatchers.IO) {
                cryptoManager.decryptFileToBytes(File(item.encryptedPath), VaultType.HIDDEN)
            }
            if (bytes != null) {
                val isPdf = item.itemType in listOf("DOCUMENT", "PDF", "SCAN") ||
                        item.encryptedPath.endsWith(".pdf", ignoreCase = true) ||
                        item.encryptedPath.endsWith("enc_pdf", ignoreCase = true) ||
                        PdfDocumentManager.isPdf(bytes)

                if (isPdf) {
                    val safeName = if (item.name.endsWith(".pdf", ignoreCase = true)) item.name else "${item.name}.pdf"
                    _pdfViewerState.value = PdfViewerState(isLoading = true, currentFileName = safeName)
                    val success = pdfDocumentManager.openFromBytes(bytes)
                    if (success && pdfDocumentManager.pageCount > 0) {
                        val firstPage = pdfDocumentManager.renderPage(0, densityScale = 2.0f)
                        val thumbs = mutableListOf<Bitmap?>()
                        for (i in 0 until minOf(pdfDocumentManager.pageCount, 12)) {
                            thumbs.add(pdfDocumentManager.renderThumbnail(i, 160))
                        }
                        _pdfViewerState.value = PdfViewerState(
                            isLoading = false,
                            totalPages = pdfDocumentManager.pageCount,
                            currentPageIndex = 0,
                            currentPageBitmap = firstPage,
                            pageThumbnails = thumbs,
                            currentFileName = safeName
                        )
                    } else {
                        _pdfViewerState.value = PdfViewerState(
                            isLoading = false,
                            errorMessage = "Unable to render PDF pages"
                        )
                    }
                } else {
                    _decryptedHiddenBitmap.value = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
            } else {
                showToast("Failed to decrypt hidden file")
            }
        }
    }

    suspend fun checkDuplicateHiddenItem(name: String): HiddenItem? {
        return repository.checkDuplicateHiddenItem(name)
    }

    fun addHiddenItem(
        name: String,
        itemType: String,
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
        deleteSourceAfterVerification: Boolean = true
    ) {
        viewModelScope.launch {
            try {
                repository.addHiddenItem(
                    name = name,
                    itemType = itemType,
                    category = category,
                    albumId = albumId,
                    description = description,
                    tags = tags,
                    expiryDate = expiryDate,
                    isFavorite = isFavorite,
                    notes = notes,
                    sourceUri = sourceUri,
                    rawBytes = rawBytes,
                    extractedOcr = extractedOcr,
                    deleteSourceAfterVerification = deleteSourceAfterVerification
                )
                refreshStorageInfo()
                if (deleteSourceAfterVerification) {
                    showToast("Verified & moved to Hidden Vault. Original removed from gallery.")
                } else {
                    showToast("Item saved to Hidden Vault")
                }
                navigateBack()
            } catch (e: Throwable) {
                showToast(e.message ?: "Failed to save hidden item")
            }
        }
    }

    // Move single photo/video from Private Photo Vault (normal gallery) to Hidden Vault
    fun movePrivateMediaToHiddenVault(media: com.example.data.model.PrivateMedia, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val success = repository.movePrivateMediaToHiddenVault(media)
            refreshStorageInfo()
            if (success) {
                showToast("\"${media.name}\" moved to Hidden Vault and removed from gallery")
                onComplete?.invoke(true)
            } else {
                showToast("Could not move \"${media.name}\" to Hidden Vault")
                onComplete?.invoke(false)
            }
        }
    }

    // Batch move photos/videos from Private Photo Vault (normal gallery) to Hidden Vault
    fun batchMovePrivateMediaToHiddenVault(mediaList: List<com.example.data.model.PrivateMedia>) {
        if (mediaList.isEmpty()) return
        viewModelScope.launch {
            val movedCount = repository.batchMovePrivateMediaToHiddenVault(mediaList)
            refreshStorageInfo()
            showToast("$movedCount item(s) moved to Hidden Vault and removed from gallery")
        }
    }

    // Move Document from Main Vault to Hidden Vault
    fun moveDocumentToHiddenVault(document: com.example.data.model.Document, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val success = repository.moveDocumentToHiddenVault(document)
            refreshStorageInfo()
            if (success) {
                showToast("\"${document.name}\" moved to Hidden Vault")
                onComplete?.invoke(true)
            } else {
                showToast("Could not move \"${document.name}\" to Hidden Vault")
                onComplete?.invoke(false)
            }
        }
    }

    fun updateHiddenItemNotes(item: HiddenItem, newNotes: String) {
        viewModelScope.launch {
            val updated = item.copy(notes = newNotes)
            repository.updateHiddenItem(updated)
            _selectedHiddenItem.value = updated
            showToast("Notes updated")
        }
    }

    fun toggleHiddenFavorite(item: HiddenItem) {
        viewModelScope.launch {
            repository.toggleHiddenFavorite(item)
            _selectedHiddenItem.value = _selectedHiddenItem.value?.copy(isFavorite = !item.isFavorite)
        }
    }

    fun moveHiddenItemToTrash(item: HiddenItem, fromViewer: Boolean = false) {
        viewModelScope.launch {
            val currentList = _viewerHiddenItemList.value.ifEmpty { allHiddenItems.value }
            val currentIndex = currentList.indexOfFirst { it.id == item.id }
            repository.moveHiddenItemToTrash(item)
            val updatedList = currentList.filter { it.id != item.id }
            _viewerHiddenItemList.value = updatedList
            if (fromViewer) {
                if (updatedList.isEmpty()) {
                    _selectedHiddenItem.value = null
                    navigateBack()
                } else {
                    val nextIndex = if (currentIndex >= updatedList.size) updatedList.size - 1 else currentIndex.coerceAtLeast(0)
                    _selectedHiddenItem.value = updatedList[nextIndex]
                }
            } else {
                _selectedHiddenItem.value = null
                navigateBack()
            }
            showToast("Moved to Hidden Trash")
            refreshStorageInfo()
        }
    }

    fun verifyHiddenPin(pin: String): Boolean = authManager.verifyHiddenPin(pin)

    fun restoreHiddenTrashItem(item: HiddenTrashItem) {
        viewModelScope.launch {
            repository.restoreHiddenTrashItem(item)
            showToast("Restored to Hidden Vault")
            refreshStorageInfo()
        }
    }

    fun permanentlyDeleteHiddenTrashItem(item: HiddenTrashItem) {
        viewModelScope.launch {
            repository.permanentlyDeleteHiddenTrashItem(item)
            showToast("File cryptographically shredded")
            refreshStorageInfo()
        }
    }

    fun clearAllHiddenTrashPermanently() {
        viewModelScope.launch {
            repository.clearAllHiddenTrashPermanently()
            showToast("All Hidden Trash shredded")
            refreshStorageInfo()
        }
    }

    // Multi-select for Hidden Vault
    fun toggleHiddenSelection(id: Long) {
        val current = _selectedHiddenIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _selectedHiddenIds.value = current
    }

    fun clearHiddenSelection() {
        _selectedHiddenIds.value = emptySet()
    }

    fun selectAllHidden(ids: List<Long>) {
        _selectedHiddenIds.value = ids.toSet()
    }

    fun batchDeleteHiddenSelection(items: List<HiddenItem>) {
        viewModelScope.launch {
            val toDelete = items.filter { _selectedHiddenIds.value.contains(it.id) }
            repository.batchDeleteHiddenItems(toDelete)
            clearHiddenSelection()
            showToast("${toDelete.size} items moved to Hidden Trash")
            refreshStorageInfo()
        }
    }

    fun batchFavoriteHiddenSelection(items: List<HiddenItem>, favorite: Boolean) {
        viewModelScope.launch {
            val toFav = items.filter { _selectedHiddenIds.value.contains(it.id) }
            repository.batchFavoriteHiddenItems(toFav, favorite)
            clearHiddenSelection()
            showToast("Updated favorites")
        }
    }

    fun batchMoveHiddenSelection(albumId: Long?) {
        viewModelScope.launch {
            repository.batchMoveHiddenItems(_selectedHiddenIds.value.toList(), albumId)
            clearHiddenSelection()
            showToast("Moved to album")
        }
    }

    fun createHiddenAlbum(name: String, category: String = "Personal") {
        viewModelScope.launch {
            repository.createHiddenAlbum(name, category)
            showToast("Hidden album created")
        }
    }

    fun selectHiddenAlbum(albumId: Long?) {
        _selectedHiddenAlbumId.value = albumId
    }

    fun setHiddenSearchQuery(query: String) {
        _hiddenSearchQuery.value = query
    }

    // Smart classification helper
    fun suggestCategory(title: String, ocr: String = ""): String {
        val text = "$title $ocr".lowercase()
        return when {
            text.contains("will") || text.contains("trust") || text.contains("deed") || text.contains("notary") || text.contains("court") -> "Legal"
            text.contains("tax") || text.contains("bank") || text.contains("pan") || text.contains("salary") || text.contains("income") || text.contains("account") -> "Financial"
            text.contains("hospital") || text.contains("medical") || text.contains("blood") || text.contains("prescription") || text.contains("doctor") -> "Medical"
            text.contains("degree") || text.contains("marksheet") || text.contains("certificate") || text.contains("college") || text.contains("university") -> "Education"
            text.contains("passport") || text.contains("aadhaar") || text.contains("license") || text.contains("id") || text.contains("voter") -> "Identity"
            else -> "Confidential"
        }
    }

    // ==========================================
    // SECURE SHARING & EXPORT
    // ==========================================
    suspend fun prepareDecryptedFileForSharing(
        filePath: String,
        exportName: String,
        vaultType: VaultType
    ): File? = withContext(Dispatchers.IO) {
        repository.logSecurityEvent("FILE_EXPORTED", "Decrypted temporary export prepared: $exportName ($vaultType)")
        cryptoManager.createDecryptedTempCopy(File(filePath), exportName, vaultType)
    }

    suspend fun prepareDecryptedFileForSharing(
        filePath: String,
        exportName: String,
        isPrivate: Boolean = false
    ): File? = prepareDecryptedFileForSharing(
        filePath,
        exportName,
        if (isPrivate) VaultType.PRIVATE else VaultType.MAIN
    )

    fun clearSecurityLogs() {
        viewModelScope.launch {
            repository.clearSecurityLogs()
            showToast("Security activity log cleared")
        }
    }

    // Refresh storage numbers
    fun refreshStorageInfo() {
        viewModelScope.launch {
            _storageInfo.value = repository.getStorageBreakdown()
        }
    }

    // Common search & filters
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setDocumentCategoryFilter(category: String?) {
        _documentFilter.value = _documentFilter.value.copy(category = category)
    }

    fun setDocumentCategory(category: String?) {
        setDocumentCategoryFilter(category)
    }

    fun setFilterExpiringSoon(enabled: Boolean) {
        _documentFilter.value = _documentFilter.value.copy(isExpiringSoonOnly = enabled)
    }

    fun setFilterExpired(enabled: Boolean) {
        _documentFilter.value = _documentFilter.value.copy(isExpiredOnly = enabled)
    }

    fun setSortBy(sortBy: SortBy) {
        _documentFilter.value = _documentFilter.value.copy(sortBy = sortBy)
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    // ==========================================
    // MULTI-FILE IMPORT IMPLEMENTATION
    // ==========================================
    private fun getFileNameFromUri(uri: Uri): String {
        var name = "File_${System.currentTimeMillis()}"
        try {
            getApplication<Application>().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val resolved = cursor.getString(nameIndex)
                    if (!resolved.isNullOrBlank()) {
                        name = resolved
                    }
                }
            }
        } catch (_: Exception) {}
        return name
    }

    fun dismissMultiImport() {
        _multiImportState.value = MultiImportState()
    }

    fun clearCache() {
        viewModelScope.launch {
            repository.clearCache()
            refreshStorageInfo()
            showToast("Temporary exports and cache cleared")
        }
    }

    suspend fun getThumbnail(filePath: String, vaultType: VaultType, isPdf: Boolean = false): Bitmap? {
        return repository.thumbnailManager.getThumbnail(filePath, vaultType, isPdf)
    }

    fun importMultipleDocuments(uris: List<Uri>, category: String) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            // Check storage before importing
            if (!repository.hasAvailableStorage(uris.size * 2L * 1024 * 1024)) {
                _multiImportState.value = MultiImportState(
                    errorMessage = "Not enough storage available. Free some device storage and try again."
                )
                return@launch
            }

            initiateBulkImport(uris, VaultType.MAIN, "DOCUMENT", category = category)
        }
    }

    // ==========================================
    // HIGH-CAPACITY BULK IMPORT SYSTEM
    // ==========================================
    fun initiateBulkImport(
        uris: List<Uri>,
        targetVault: VaultType,
        mediaType: String,
        albumId: Long? = null,
        category: String = "Personal",
        isMoveOperation: Boolean = (targetVault == VaultType.HIDDEN || targetVault == VaultType.HIDDEN_HIDDEN)
    ) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            bulkImportManager.preparePlan(
                uris = uris,
                targetVault = targetVault,
                mediaType = mediaType,
                albumId = albumId,
                category = category,
                storageMode = _photoStorageMode.value,
                skipDuplicates = true,
                isMoveOperation = isMoveOperation
            )
        }
    }

    fun confirmAndStartBulkImport(
        mode: com.example.data.bulk.PhotoStorageMode,
        skipDuplicates: Boolean,
        isMoveOperation: Boolean = false
    ) {
        val plan = bulkImportState.value.plan ?: return
        val updatedPlan = plan.copy(
            storageMode = mode,
            skipDuplicates = skipDuplicates,
            isMoveOperation = isMoveOperation
        )
        bulkImportManager.startImport(updatedPlan, viewModelScope)
    }

    fun pauseBulkImport() {
        bulkImportManager.pauseImport()
    }

    fun resumeBulkImport() {
        bulkImportManager.resumeImport(viewModelScope)
    }

    fun cancelBulkImport() {
        bulkImportManager.cancelImport()
    }

    fun retryFailedBulkImport() {
        bulkImportManager.retryFailed(viewModelScope)
    }

    fun dismissBulkImport() {
        bulkImportManager.resetState()
        refreshStorageInfo()
    }

    fun importMultiplePhotos(uris: List<Uri>, albumId: Long?) {
        initiateBulkImport(uris, VaultType.PRIVATE, "PHOTO", albumId)
    }

    fun importMultipleVideos(uris: List<Uri>, albumId: Long?) {
        initiateBulkImport(uris, VaultType.PRIVATE, "VIDEO", albumId)
    }

    fun importMultipleHiddenItems(uris: List<Uri>, itemType: String, albumId: Long?, isMoveOperation: Boolean = true) {
        initiateBulkImport(uris, VaultType.HIDDEN, itemType, albumId, isMoveOperation = isMoveOperation)
    }

    // ==========================================
    // HIDDEN HIDDEN VAULT BULK OPERATIONS
    // ==========================================
    fun importHiddenHiddenPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        initiateBulkImport(uris, VaultType.HIDDEN_HIDDEN, "PHOTO", null)
    }

    fun importHiddenHiddenVideos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        initiateBulkImport(uris, VaultType.HIDDEN_HIDDEN, "VIDEO", null)
    }

    fun importHiddenHiddenFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        initiateBulkImport(uris, VaultType.HIDDEN_HIDDEN, "PHOTO", null)
    }

    fun setHiddenHiddenSearchQuery(query: String) {
        _hiddenHiddenSearchQuery.value = query
    }

    fun setSelectedHiddenHiddenItem(
        item: com.example.data.model.HiddenHiddenItem?,
        contextList: List<com.example.data.model.HiddenHiddenItem>? = null
    ) {
        _selectedHiddenHiddenItem.value = item
        _viewerHiddenHiddenItemList.value = contextList ?: allHiddenHiddenItems.value
    }

    fun toggleHiddenHiddenSelection(id: Long) {
        val current = _selectedHiddenHiddenIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _selectedHiddenHiddenIds.value = current
    }

    fun selectAllHiddenHiddenItems(ids: List<Long>) {
        _selectedHiddenHiddenIds.value = ids.toSet()
    }

    fun clearHiddenHiddenSelection() {
        _selectedHiddenHiddenIds.value = emptySet()
        _isHiddenHiddenSelectMode.value = false
    }

    fun setHiddenHiddenSelectMode(enabled: Boolean) {
        _isHiddenHiddenSelectMode.value = enabled
        if (!enabled) {
            _selectedHiddenHiddenIds.value = emptySet()
        }
    }

    fun deleteHiddenHiddenItem(item: com.example.data.model.HiddenHiddenItem, fromViewer: Boolean = false) {
        viewModelScope.launch {
            try {
                val currentList = _viewerHiddenHiddenItemList.value.ifEmpty { allHiddenHiddenItems.value }
                val currentIndex = currentList.indexOfFirst { it.id == item.id }
                database.hiddenHiddenDao().deleteItem(item)
                val file = File(item.encryptedPath)
                cryptoManager.secureDeleteFile(file)
                repository.thumbnailManager.deleteThumbnail(item.encryptedPath)
                val updatedList = currentList.filter { it.id != item.id }
                _viewerHiddenHiddenItemList.value = updatedList
                if (fromViewer) {
                    if (updatedList.isEmpty()) {
                        _selectedHiddenHiddenItem.value = null
                        navigateBack()
                    } else {
                        val nextIndex = if (currentIndex >= updatedList.size) updatedList.size - 1 else currentIndex.coerceAtLeast(0)
                        _selectedHiddenHiddenItem.value = updatedList[nextIndex]
                    }
                } else {
                    if (_selectedHiddenHiddenItem.value?.id == item.id) {
                        _selectedHiddenHiddenItem.value = null
                    }
                }
                showToast("Item permanently deleted")
                refreshStorageInfo()
            } catch (e: Exception) {
                showToast("Delete failed: ${e.message}")
            }
        }
    }

    fun deleteSelectedHiddenHiddenItems() {
        val ids = _selectedHiddenHiddenIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            try {
                ids.forEach { id ->
                    val item = database.hiddenHiddenDao().getItemById(id)
                    if (item != null) {
                        database.hiddenHiddenDao().deleteItem(item)
                        val file = File(item.encryptedPath)
                        cryptoManager.secureDeleteFile(file)
                        repository.thumbnailManager.deleteThumbnail(item.encryptedPath)
                    }
                }
                val count = ids.size
                _viewerHiddenHiddenItemList.value = _viewerHiddenHiddenItemList.value.filter { !ids.contains(it.id) }
                _selectedHiddenHiddenIds.value = emptySet()
                _isHiddenHiddenSelectMode.value = false
                showToast("Deleted $count items")
                refreshStorageInfo()
            } catch (e: Exception) {
                showToast("Delete failed: ${e.message}")
            }
        }
    }

    fun downloadHiddenHiddenItem(item: com.example.data.model.HiddenHiddenItem, onComplete: (Boolean, String, Uri?) -> Unit) {
        viewModelScope.launch {
            val isVid = item.itemType == "VIDEO" || item.encryptedPath.endsWith("enc_vid")
            val ext = if (isVid) ".mp4" else ".jpg"
            val mimeType = if (isVid) "video/mp4" else "image/jpeg"
            val baseName = item.name.removeSuffix(".mp4").removeSuffix(".jpg").removeSuffix(".png").replace(" ", "_")
            val fileName = "$baseName$ext"

            val bytes = withContext(Dispatchers.IO) {
                cryptoManager.decryptFileToBytes(File(item.encryptedPath), VaultType.HIDDEN_HIDDEN)
            }

            if (bytes == null) {
                onComplete(false, "Decryption failed", null)
                return@launch
            }

            val uri = withContext(Dispatchers.IO) {
                FileDownloadHelper.saveToDownloads(
                    getApplication(),
                    fileName,
                    mimeType,
                    bytes
                )
            }

            if (uri != null) {
                repository.logSecurityEvent("HIDDEN_HIDDEN_DOWNLOAD", "Exported $fileName")
                onComplete(true, "Saved to Downloads/$fileName", uri)
            } else {
                onComplete(false, "Failed to write file to Downloads", null)
            }
        }
    }

    fun downloadSelectedHiddenHiddenItems(onProgress: (Int, Int) -> Unit, onComplete: (Int, Int) -> Unit) {
        val ids = _selectedHiddenHiddenIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            var successCount = 0
            var failCount = 0
            ids.forEachIndexed { index, id ->
                val item = database.hiddenHiddenDao().getItemById(id)
                if (item != null) {
                    val isVid = item.itemType == "VIDEO" || item.encryptedPath.endsWith("enc_vid")
                    val ext = if (isVid) ".mp4" else ".jpg"
                    val mimeType = if (isVid) "video/mp4" else "image/jpeg"
                    val baseName = item.name.removeSuffix(".mp4").removeSuffix(".jpg").removeSuffix(".png").replace(" ", "_")
                    val fileName = "${baseName}_${System.currentTimeMillis() % 10000}$ext"

                    val bytes = withContext(Dispatchers.IO) {
                        cryptoManager.decryptFileToBytes(File(item.encryptedPath), VaultType.HIDDEN_HIDDEN)
                    }

                    if (bytes != null) {
                        val uri = withContext(Dispatchers.IO) {
                            FileDownloadHelper.saveToDownloads(getApplication(), fileName, mimeType, bytes)
                        }
                        if (uri != null) successCount++ else failCount++
                    } else {
                        failCount++
                    }
                }
                onProgress(index + 1, ids.size)
            }
            _selectedHiddenHiddenIds.value = emptySet()
            _isHiddenHiddenSelectMode.value = false
            onComplete(successCount, failCount)
        }
    }

    fun retryFailedImports() {
        retryFailedBulkImport()
    }

    // ==========================================
    // BULK GALLERY & STORAGE MANAGEMENT
    // ==========================================
    fun batchDeletePrivateMedia(ids: List<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val mediaList = database.privateMediaDao().getMediaByIds(ids)
            mediaList.forEach { repository.moveMediaToTrash(it) }
            showToast("Moved ${ids.size} items to Trash")
            refreshStorageInfo()
        }
    }

    fun batchDeleteDocuments(ids: List<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val docList = database.documentDao().getDocumentsByIds(ids)
            docList.forEach { repository.moveDocumentToTrash(it) }
            showToast("Moved ${ids.size} documents to Trash")
            refreshStorageInfo()
        }
    }

    fun batchFavoritePrivateMedia(ids: List<Long>, favorite: Boolean) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            database.privateMediaDao().updateFavoriteBulk(ids, favorite)
            showToast(if (favorite) "Added ${ids.size} items to Favorites" else "Removed from Favorites")
        }
    }

    fun batchMovePrivateMediaToAlbum(ids: List<Long>, albumId: Long?) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.moveMediaToAlbum(ids, albumId)
            showToast("Moved ${ids.size} items to album")
        }
    }

    fun clearStorageCache() {
        viewModelScope.launch {
            repository.clearCache()
            showToast("Cache cleared successfully")
            refreshStorageInfo()
        }
    }

    fun clearThumbnailCache() {
        viewModelScope.launch {
            repository.thumbnailManager.clearAllThumbnails()
            showToast("Thumbnail cache cleared")
            refreshStorageInfo()
        }
    }

    fun clearTempImports() {
        viewModelScope.launch {
            val tempDir = File(getApplication<Application>().filesDir, "temp_imports")
            tempDir.listFiles()?.forEach { it.delete() }
            showToast("Temporary staging files purged")
            refreshStorageInfo()
        }
    }

    override fun onCleared() {
        super.onCleared()
        closePdfViewer()
    }
}
