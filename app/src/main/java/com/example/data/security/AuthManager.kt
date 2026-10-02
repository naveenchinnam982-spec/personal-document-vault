package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

enum class AppLockType {
    FINGERPRINT,
    PIN,
    PATTERN
}

enum class BiometricStatus {
    AVAILABLE,
    NOT_ENROLLED,
    NO_HARDWARE,
    UNAVAILABLE
}

class AuthManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("secure_vault_auth", Context.MODE_PRIVATE)

    // ==========================================
    // 1. MAIN VAULT AUTH STATE & LOCK TYPE
    // ==========================================
    var appLockType: AppLockType
        get() {
            val typeStr = prefs.getString(KEY_APP_LOCK_TYPE, null)
            return when (typeStr) {
                AppLockType.FINGERPRINT.name -> AppLockType.FINGERPRINT
                AppLockType.PATTERN.name -> AppLockType.PATTERN
                AppLockType.PIN.name -> AppLockType.PIN
                else -> {
                    if (isMainPatternSet()) AppLockType.PATTERN
                    else if (isMainPinSet()) AppLockType.PIN
                    else AppLockType.PIN
                }
            }
        }
        set(value) {
            prefs.edit().putString(KEY_APP_LOCK_TYPE, value.name).apply()
        }

    fun isAppLockConfigured(): Boolean {
        if (!prefs.contains(KEY_APP_LOCK_TYPE)) {
            // Check legacy
            return isMainPinSet() || isMainPatternSet()
        }
        return when (appLockType) {
            AppLockType.PIN -> isMainPinSet()
            AppLockType.PATTERN -> isMainPatternSet()
            AppLockType.FINGERPRINT -> isFingerprintConfigured() && isMainPinSet()
        }
    }

    // --- MAIN PIN ---
    fun isMainPinSet(): Boolean = prefs.contains(KEY_MAIN_PIN_HASH)

    fun setMainPin(pin: String): Boolean {
        if (!isValidPin(pin)) return false
        val salt = generateSalt()
        val hash = hashPin(pin, salt)
        prefs.edit()
            .putString(KEY_MAIN_PIN_HASH, hash)
            .putString(KEY_MAIN_PIN_SALT, salt)
            .putInt(KEY_MAIN_PIN_LENGTH, pin.length)
            .apply()
        resetMainAttempts()
        return true
    }

    fun verifyMainPin(pin: String): Boolean {
        if (isMainLockedOut()) return false
        val savedHash = prefs.getString(KEY_MAIN_PIN_HASH, null) ?: return false
        val salt = prefs.getString(KEY_MAIN_PIN_SALT, null) ?: return false
        val computedHash = hashPin(pin, salt)
        val matches = computedHash == savedHash
        if (matches) {
            resetMainAttempts()
        } else {
            recordMainFailedAttempt()
        }
        return matches
    }

    fun getMainPinLength(): Int = prefs.getInt(KEY_MAIN_PIN_LENGTH, 4)

    // --- MAIN PATTERN ---
    fun isMainPatternSet(): Boolean = prefs.contains(KEY_MAIN_PATTERN_HASH)

    fun setMainPattern(pattern: List<Int>): Boolean {
        if (pattern.size < 4) return false
        val patternString = pattern.joinToString(separator = "-")
        val salt = generateSalt()
        val hash = hashPin(patternString, salt)
        prefs.edit()
            .putString(KEY_MAIN_PATTERN_HASH, hash)
            .putString(KEY_MAIN_PATTERN_SALT, salt)
            .putInt(KEY_MAIN_PATTERN_NODES, pattern.size)
            .apply()
        resetMainAttempts()
        return true
    }

    fun verifyMainPattern(pattern: List<Int>): Boolean {
        if (isMainLockedOut()) return false
        val savedHash = prefs.getString(KEY_MAIN_PATTERN_HASH, null) ?: return false
        val salt = prefs.getString(KEY_MAIN_PATTERN_SALT, null) ?: return false
        val patternString = pattern.joinToString(separator = "-")
        val computedHash = hashPin(patternString, salt)
        val matches = computedHash == savedHash
        if (matches) {
            resetMainAttempts()
        } else {
            recordMainFailedAttempt()
        }
        return matches
    }

    fun resetMainPattern() {
        prefs.edit()
            .remove(KEY_MAIN_PATTERN_HASH)
            .remove(KEY_MAIN_PATTERN_SALT)
            .remove(KEY_MAIN_PATTERN_NODES)
            .apply()
    }

    // --- MAIN FINGERPRINT ---
    fun isFingerprintConfigured(): Boolean = prefs.getBoolean(KEY_FINGERPRINT_CONFIGURED, false)

    fun setFingerprintConfigured(configured: Boolean) {
        prefs.edit()
            .putBoolean(KEY_FINGERPRINT_CONFIGURED, configured)
            .putBoolean(KEY_BIOMETRIC_MAIN, configured)
            .apply()
    }

    fun getBiometricStatus(): BiometricStatus {
        return try {
            val biometricManager = BiometricManager.from(context)
            when (biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
            )) {
                BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
                else -> BiometricStatus.UNAVAILABLE
            }
        } catch (_: Throwable) {
            BiometricStatus.UNAVAILABLE
        }
    }

    private fun recordMainFailedAttempt() {
        val attempts = prefs.getInt(KEY_MAIN_ATTEMPTS, 0) + 1
        val editor = prefs.edit().putInt(KEY_MAIN_ATTEMPTS, attempts)
        if (attempts >= MAX_ATTEMPTS) {
            editor.putLong(KEY_MAIN_LOCKOUT_TIME, System.currentTimeMillis() + LOCKOUT_DURATION_MS)
        }
        editor.apply()
    }

    private fun resetMainAttempts() {
        prefs.edit().remove(KEY_MAIN_ATTEMPTS).remove(KEY_MAIN_LOCKOUT_TIME).apply()
    }

    fun isMainLockedOut(): Boolean {
        val lockoutUntil = prefs.getLong(KEY_MAIN_LOCKOUT_TIME, 0L)
        return System.currentTimeMillis() < lockoutUntil
    }

    fun getMainLockoutSecondsRemaining(): Int {
        val lockoutUntil = prefs.getLong(KEY_MAIN_LOCKOUT_TIME, 0L)
        val diff = lockoutUntil - System.currentTimeMillis()
        return if (diff > 0) (diff / 1000).toInt() + 1 else 0
    }

    // ==========================================
    // 2. PRIVATE PHOTO VAULT AUTH STATE
    // ==========================================
    fun isPrivatePinSet(): Boolean = prefs.contains(KEY_PRIVATE_PIN_HASH)

    fun setPrivatePin(pin: String): Boolean {
        if (!isValidPin(pin)) return false
        val salt = generateSalt()
        val hash = hashPin(pin, salt)
        prefs.edit()
            .putString(KEY_PRIVATE_PIN_HASH, hash)
            .putString(KEY_PRIVATE_PIN_SALT, salt)
            .putInt(KEY_PRIVATE_PIN_LENGTH, pin.length)
            .apply()
        resetPrivateAttempts()
        return true
    }

    fun verifyPrivatePin(pin: String): Boolean {
        if (isPrivateLockedOut()) return false
        val savedHash = prefs.getString(KEY_PRIVATE_PIN_HASH, null) ?: return false
        val salt = prefs.getString(KEY_PRIVATE_PIN_SALT, null) ?: return false
        val computedHash = hashPin(pin, salt)
        val matches = computedHash == savedHash
        if (matches) {
            resetPrivateAttempts()
        } else {
            recordPrivateFailedAttempt()
        }
        return matches
    }

    fun getPrivatePinLength(): Int = prefs.getInt(KEY_PRIVATE_PIN_LENGTH, 4)

    private fun recordPrivateFailedAttempt() {
        val attempts = prefs.getInt(KEY_PRIVATE_ATTEMPTS, 0) + 1
        val editor = prefs.edit().putInt(KEY_PRIVATE_ATTEMPTS, attempts)
        if (attempts >= MAX_ATTEMPTS) {
            editor.putLong(KEY_PRIVATE_LOCKOUT_TIME, System.currentTimeMillis() + LOCKOUT_DURATION_MS)
        }
        editor.apply()
    }

    private fun resetPrivateAttempts() {
        prefs.edit().remove(KEY_PRIVATE_ATTEMPTS).remove(KEY_PRIVATE_LOCKOUT_TIME).apply()
    }

    fun isPrivateLockedOut(): Boolean {
        val lockoutUntil = prefs.getLong(KEY_PRIVATE_LOCKOUT_TIME, 0L)
        return System.currentTimeMillis() < lockoutUntil
    }

    fun getPrivateLockoutSecondsRemaining(): Int {
        val lockoutUntil = prefs.getLong(KEY_PRIVATE_LOCKOUT_TIME, 0L)
        val diff = lockoutUntil - System.currentTimeMillis()
        return if (diff > 0) (diff / 1000).toInt() + 1 else 0
    }

    // ==========================================
    // 3. HIDDEN VAULT AUTH STATE (SEPARATE PIN & LOCKOUT)
    // ==========================================
    fun isHiddenPinSet(): Boolean = prefs.contains(KEY_HIDDEN_PIN_HASH)
    val isPinSetForHidden: Boolean get() = isHiddenPinSet()

    fun setHiddenPin(pin: String): Boolean {
        if (!isValidPin(pin)) return false
        val salt = generateSalt()
        val hash = hashPin(pin, salt)
        prefs.edit()
            .putString(KEY_HIDDEN_PIN_HASH, hash)
            .putString(KEY_HIDDEN_PIN_SALT, salt)
            .putInt(KEY_HIDDEN_PIN_LENGTH, pin.length)
            .apply()
        resetHiddenAttempts()
        return true
    }

    fun verifyHiddenPin(pin: String): Boolean {
        if (isHiddenLockedOut()) return false
        val savedHash = prefs.getString(KEY_HIDDEN_PIN_HASH, null) ?: return false
        val salt = prefs.getString(KEY_HIDDEN_PIN_SALT, null) ?: return false
        val computedHash = hashPin(pin, salt)
        val matches = computedHash == savedHash
        if (matches) {
            resetHiddenAttempts()
        } else {
            recordHiddenFailedAttempt()
        }
        return matches
    }

    fun getHiddenPinLength(): Int = prefs.getInt(KEY_HIDDEN_PIN_LENGTH, 4)

    private fun recordHiddenFailedAttempt() {
        val attempts = prefs.getInt(KEY_HIDDEN_ATTEMPTS, 0) + 1
        val editor = prefs.edit().putInt(KEY_HIDDEN_ATTEMPTS, attempts)
        if (attempts >= MAX_ATTEMPTS) {
            editor.putLong(KEY_HIDDEN_LOCKOUT_TIME, System.currentTimeMillis() + LOCKOUT_DURATION_MS)
        }
        editor.apply()
    }

    private fun resetHiddenAttempts() {
        prefs.edit().remove(KEY_HIDDEN_ATTEMPTS).remove(KEY_HIDDEN_LOCKOUT_TIME).apply()
    }

    fun isHiddenLockedOut(): Boolean {
        val lockoutUntil = prefs.getLong(KEY_HIDDEN_LOCKOUT_TIME, 0L)
        return System.currentTimeMillis() < lockoutUntil
    }

    fun getHiddenLockoutSecondsRemaining(): Int {
        val lockoutUntil = prefs.getLong(KEY_HIDDEN_LOCKOUT_TIME, 0L)
        val diff = lockoutUntil - System.currentTimeMillis()
        return if (diff > 0) (diff / 1000).toInt() + 1 else 0
    }

    // Hidden Vault System Feature Flags
    var isHiddenVaultEnabled: Boolean
        get() = prefs.getBoolean(KEY_HIDDEN_VAULT_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDDEN_VAULT_ENABLED, value).apply()

    var hiddenVaultAutoLockTimeoutSeconds: Int
        get() = prefs.getInt(KEY_HIDDEN_AUTO_LOCK, 30) // default 30s for high security
        set(value) = prefs.edit().putInt(KEY_HIDDEN_AUTO_LOCK, value).apply()

    // Discreet Access Code / Sequence
    var discreetAccessCode: String
        get() = prefs.getString(KEY_DISCREET_CODE, "7777") ?: "7777"
        set(value) = prefs.edit().putString(KEY_DISCREET_CODE, value).apply()

    fun verifyDiscreetCode(code: String): Boolean = code.trim() == discreetAccessCode.trim()

    var isDiscreetGestureEnabled: Boolean
        get() = prefs.getBoolean(KEY_DISCREET_GESTURE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_DISCREET_GESTURE_ENABLED, value).apply()

    // ==========================================
    // 4. HIDDEN HIDDEN VAULT AUTH STATE (PATTERN LOCK)
    // ==========================================
    fun isHiddenHiddenPatternSet(): Boolean = prefs.contains(KEY_HIDDEN_HIDDEN_PATTERN_HASH)
    val isPatternSetForHiddenHidden: Boolean get() = isHiddenHiddenPatternSet()

    fun setHiddenHiddenPattern(pattern: List<Int>): Boolean {
        if (pattern.size < 4) return false
        val patternString = pattern.joinToString(separator = "-")
        val salt = generateSalt()
        val hash = hashPin(patternString, salt)
        prefs.edit()
            .putString(KEY_HIDDEN_HIDDEN_PATTERN_HASH, hash)
            .putString(KEY_HIDDEN_HIDDEN_PATTERN_SALT, salt)
            .putInt(KEY_HIDDEN_HIDDEN_PATTERN_NODES, pattern.size)
            .apply()
        resetHiddenHiddenAttempts()
        return true
    }

    fun verifyHiddenHiddenPattern(pattern: List<Int>): Boolean {
        if (isHiddenHiddenLockedOut()) return false
        val savedHash = prefs.getString(KEY_HIDDEN_HIDDEN_PATTERN_HASH, null) ?: return false
        val salt = prefs.getString(KEY_HIDDEN_HIDDEN_PATTERN_SALT, null) ?: return false
        val patternString = pattern.joinToString(separator = "-")
        val computedHash = hashPin(patternString, salt)
        val matches = computedHash == savedHash
        if (matches) {
            resetHiddenHiddenAttempts()
        } else {
            recordHiddenHiddenFailedAttempt()
        }
        return matches
    }

    fun resetHiddenHiddenPattern() {
        prefs.edit()
            .remove(KEY_HIDDEN_HIDDEN_PATTERN_HASH)
            .remove(KEY_HIDDEN_HIDDEN_PATTERN_SALT)
            .remove(KEY_HIDDEN_HIDDEN_PATTERN_NODES)
            .remove(KEY_HIDDEN_HIDDEN_ATTEMPTS)
            .remove(KEY_HIDDEN_HIDDEN_LOCKOUT_TIME)
            .apply()
    }

    private fun recordHiddenHiddenFailedAttempt() {
        val attempts = prefs.getInt(KEY_HIDDEN_HIDDEN_ATTEMPTS, 0) + 1
        val editor = prefs.edit().putInt(KEY_HIDDEN_HIDDEN_ATTEMPTS, attempts)
        if (attempts >= MAX_ATTEMPTS) {
            editor.putLong(KEY_HIDDEN_HIDDEN_LOCKOUT_TIME, System.currentTimeMillis() + LOCKOUT_DURATION_MS)
        }
        editor.apply()
    }

    fun resetHiddenHiddenAttempts() {
        prefs.edit().remove(KEY_HIDDEN_HIDDEN_ATTEMPTS).remove(KEY_HIDDEN_HIDDEN_LOCKOUT_TIME).apply()
    }

    fun isHiddenHiddenLockedOut(): Boolean {
        val lockoutUntil = prefs.getLong(KEY_HIDDEN_HIDDEN_LOCKOUT_TIME, 0L)
        return System.currentTimeMillis() < lockoutUntil
    }

    fun getHiddenHiddenLockoutSecondsRemaining(): Int {
        val lockoutUntil = prefs.getLong(KEY_HIDDEN_HIDDEN_LOCKOUT_TIME, 0L)
        val diff = lockoutUntil - System.currentTimeMillis()
        return if (diff > 0) (diff / 1000).toInt() + 1 else 0
    }

    // ==========================================
    // BIOMETRICS (PER-VAULT SETTINGS)
    // ==========================================
    fun canAuthenticateWithBiometrics(): Boolean {
        return try {
            val biometricManager = BiometricManager.from(context)
            biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
            ) == BiometricManager.BIOMETRIC_SUCCESS
        } catch (_: Throwable) {
            false
        }
    }

    var isBiometricEnabled: Boolean
        get() = isBiometricEnabledForMain
        set(value) {
            isBiometricEnabledForMain = value
        }

    var isBiometricEnabledForMain: Boolean
        get() = if (prefs.contains(KEY_BIOMETRIC_MAIN)) prefs.getBoolean(KEY_BIOMETRIC_MAIN, false) else canAuthenticateWithBiometrics()
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_MAIN, value).apply()

    var isBiometricEnabledForPrivate: Boolean
        get() = if (prefs.contains(KEY_BIOMETRIC_PRIVATE)) prefs.getBoolean(KEY_BIOMETRIC_PRIVATE, false) else canAuthenticateWithBiometrics()
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_PRIVATE, value).apply()

    var isBiometricEnabledForHidden: Boolean
        get() = if (prefs.contains(KEY_BIOMETRIC_HIDDEN)) prefs.getBoolean(KEY_BIOMETRIC_HIDDEN, false) else canAuthenticateWithBiometrics()
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_HIDDEN, value).apply()

    var isBiometricEnabledForHiddenHidden: Boolean
        get() = false
        set(_) {
            prefs.edit().putBoolean(KEY_BIOMETRIC_HIDDEN_HIDDEN, false).apply()
        }

    // ==========================================
    // RECOVERY
    // ==========================================
    fun getOrCreateRecoveryCode(): String {
        var code = prefs.getString(KEY_RECOVERY_CODE, null)
        if (code == null) {
            code = UUID.randomUUID().toString().substring(0, 8).uppercase()
            prefs.edit().putString(KEY_RECOVERY_CODE, code).apply()
        }
        return code
    }

    fun verifyRecoveryCode(code: String): Boolean {
        val saved = prefs.getString(KEY_RECOVERY_CODE, null) ?: return false
        return saved.equals(code.trim(), ignoreCase = true)
    }

    fun setSecurityQuestion(question: String, answer: String) {
        val salt = generateSalt()
        val answerHash = hashPin(answer.trim().lowercase(), salt)
        prefs.edit()
            .putString(KEY_SEC_QUESTION, question)
            .putString(KEY_SEC_ANSWER_HASH, answerHash)
            .putString(KEY_SEC_SALT, salt)
            .apply()
    }

    fun getSecurityQuestion(): String? = prefs.getString(KEY_SEC_QUESTION, null)

    fun verifySecurityAnswer(answer: String): Boolean {
        val savedHash = prefs.getString(KEY_SEC_ANSWER_HASH, null) ?: return false
        val salt = prefs.getString(KEY_SEC_SALT, null) ?: return false
        return hashPin(answer.trim().lowercase(), salt) == savedHash
    }

    // ==========================================
    // SYSTEM SETTINGS
    // ==========================================
    var autoLockTimeoutSeconds: Int
        get() = prefs.getInt(KEY_AUTO_LOCK_TIMEOUT, 60)
        set(value) = prefs.edit().putInt(KEY_AUTO_LOCK_TIMEOUT, value).apply()

    var isScreenshotProtectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_SCREENSHOT_PROTECT, false)
        set(value) = prefs.edit().putBoolean(KEY_SCREENSHOT_PROTECT, value).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "Naveen") ?: "Naveen"
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, "DARK") ?: "DARK" // "DARK", "LIGHT", "SYSTEM"
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value).apply()

    var appThemeStyle: String
        get() = prefs.getString(KEY_APP_THEME_STYLE, "EMERALD") ?: "EMERALD"
        set(value) = prefs.edit().putString(KEY_APP_THEME_STYLE, value).apply()

    var isDarkMode: Boolean
        get() = themeMode != "LIGHT"
        set(value) {
            themeMode = if (value) "DARK" else "LIGHT"
        }

    var isBackupEnabled: Boolean
        get() = prefs.getBoolean(KEY_BACKUP_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BACKUP_ENABLED, value).apply()

    var lastBackupTimestamp: Long
        get() = prefs.getLong(KEY_LAST_BACKUP_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_BACKUP_TIME, value).apply()

    // Helper hashing methods
    private fun hashPin(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt.toByteArray(Charsets.UTF_8))
        val digest = md.digest(pin.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun generateSalt(): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        return saltBytes.joinToString("") { "%02x".format(it) }
    }

    private fun isValidPin(pin: String): Boolean {
        return (pin.length == 4 || pin.length == 6) && pin.all { it.isDigit() }
    }

    companion object {
        private const val KEY_APP_LOCK_TYPE = "app_lock_type"
        private const val KEY_MAIN_PIN_HASH = "main_vault_pin_hash"
        private const val KEY_MAIN_PIN_SALT = "main_vault_pin_salt"
        private const val KEY_MAIN_PIN_LENGTH = "main_vault_pin_len"
        private const val KEY_MAIN_PATTERN_HASH = "main_pattern_hash"
        private const val KEY_MAIN_PATTERN_SALT = "main_pattern_salt"
        private const val KEY_MAIN_PATTERN_NODES = "main_pattern_nodes"
        private const val KEY_FINGERPRINT_CONFIGURED = "fingerprint_configured"

        private const val KEY_PRIVATE_PIN_HASH = "private_vault_pin_hash"
        private const val KEY_PRIVATE_PIN_SALT = "private_vault_pin_salt"
        private const val KEY_PRIVATE_PIN_LENGTH = "private_vault_pin_len"

        private const val KEY_HIDDEN_PIN_HASH = "hidden_vault_pin_hash"
        private const val KEY_HIDDEN_PIN_SALT = "hidden_vault_pin_salt"
        private const val KEY_HIDDEN_PIN_LENGTH = "hidden_vault_pin_len"

        private const val KEY_HIDDEN_HIDDEN_PATTERN_HASH = "hidden_hidden_pattern_hash"
        private const val KEY_HIDDEN_HIDDEN_PATTERN_SALT = "hidden_hidden_pattern_salt"
        private const val KEY_HIDDEN_HIDDEN_PATTERN_NODES = "hidden_hidden_pattern_nodes"
        private const val KEY_HIDDEN_HIDDEN_ATTEMPTS = "hidden_hidden_attempts"
        private const val KEY_HIDDEN_HIDDEN_LOCKOUT_TIME = "hidden_hidden_lockout_time"

        private const val KEY_MAIN_ATTEMPTS = "main_attempts"
        private const val KEY_MAIN_LOCKOUT_TIME = "main_lockout_time"
        private const val KEY_PRIVATE_ATTEMPTS = "private_attempts"
        private const val KEY_PRIVATE_LOCKOUT_TIME = "private_lockout_time"
        private const val KEY_HIDDEN_ATTEMPTS = "hidden_attempts"
        private const val KEY_HIDDEN_LOCKOUT_TIME = "hidden_lockout_time"

        private const val KEY_BIOMETRIC_MAIN = "biometric_main"
        private const val KEY_BIOMETRIC_PRIVATE = "biometric_private"
        private const val KEY_BIOMETRIC_HIDDEN = "biometric_hidden"
        private const val KEY_BIOMETRIC_HIDDEN_HIDDEN = "biometric_hidden_hidden"

        private const val KEY_HIDDEN_VAULT_ENABLED = "hidden_vault_enabled"
        private const val KEY_HIDDEN_AUTO_LOCK = "hidden_auto_lock"
        private const val KEY_DISCREET_CODE = "discreet_code"
        private const val KEY_DISCREET_GESTURE_ENABLED = "discreet_gesture_enabled"

        private const val KEY_RECOVERY_CODE = "recovery_code"
        private const val KEY_SEC_QUESTION = "sec_question"
        private const val KEY_SEC_ANSWER_HASH = "sec_answer_hash"
        private const val KEY_SEC_SALT = "sec_salt"
        private const val KEY_AUTO_LOCK_TIMEOUT = "auto_lock_timeout"
        private const val KEY_SCREENSHOT_PROTECT = "screenshot_protect"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_APP_THEME_STYLE = "app_theme_style"
        private const val KEY_BACKUP_ENABLED = "backup_enabled"
        private const val KEY_LAST_BACKUP_TIME = "last_backup_time"

        const val MAX_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MS = 30_000L
    }
}
