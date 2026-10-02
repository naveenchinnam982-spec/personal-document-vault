package com.example.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Fingerprint
import com.example.data.security.AppLockType
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.ui.platform.LocalContext
import com.example.BuildConfig
import com.example.util.AppSharingHelper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ThemeSelectorDialog
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.HiddenGold
import com.example.ui.theme.HiddenPrimary
import com.example.ui.theme.HiddenPrimaryContainer
import com.example.ui.theme.HiddenPrimaryLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultDarkBg
import com.example.ui.theme.VaultRed
import com.example.ui.theme.VaultSurfaceElevated
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.VaultViewModel

@Composable
fun SettingsScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val authManager = viewModel.authManager
    val storageInfo by viewModel.storageInfo.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var isBiometricEnabled by remember { mutableStateOf(authManager.isBiometricEnabled) }
    var isPrivateBiometric by remember { mutableStateOf(authManager.isBiometricEnabledForPrivate) }
    var isHiddenEnabled by remember { mutableStateOf(authManager.isHiddenVaultEnabled) }
    var isHiddenBiometric by remember { mutableStateOf(authManager.isBiometricEnabledForHidden) }
    var isScreenshotProtect by remember { mutableStateOf(authManager.isScreenshotProtectionEnabled) }
    var autoLockSecs by remember { mutableStateOf(authManager.autoLockTimeoutSeconds) }
    var showAutoLockMenu by remember { mutableStateOf(false) }
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var showDiscreetCodeDialog by remember { mutableStateOf(false) }
    var showShareAppDialog by remember { mutableStateOf(false) }
    var customDiscreetCode by remember { mutableStateOf(authManager.discreetAccessCode) }
    var showThemeDialog by remember { mutableStateOf(false) }
    val currentTheme by viewModel.appTheme.collectAsStateWithLifecycle()

    val autoLockOptions = listOf(
        0 to "Immediately on background",
        30 to "After 30 seconds",
        60 to "After 1 minute",
        300 to "After 5 minutes",
        -1 to "Never (Until app exit)"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VaultDarkBg)
            .testTag("settings_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vault Settings & Security",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Storage Overview Card
            Text(
                text = "ENCRYPTED STORAGE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(VaultSurfaceElevated)
                    .border(1.dp, VaultBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hardware Encrypted Space",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = storageInfo.formatBytes(storageInfo.totalSizeBytes),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val usedRatio = if (storageInfo.totalVaultSizeBytes > 0 && storageInfo.availableDeviceSizeBytes > 0) {
                        (storageInfo.totalVaultSizeBytes.toFloat() / (storageInfo.totalVaultSizeBytes + storageInfo.availableDeviceSizeBytes).toFloat()).coerceIn(0.01f, 1f)
                    } else if (storageInfo.totalVaultSizeBytes > 0) 0.05f else 0f

                    LinearProgressIndicator(
                        progress = { usedRatio },
                        color = EmeraldPrimary,
                        trackColor = VaultBorder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Free Device: ${storageInfo.formatBytes(storageInfo.availableDeviceSizeBytes)}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "Total Used: ${storageInfo.formatBytes(storageInfo.totalSizeBytes)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Docs: ${storageInfo.formatBytes(storageInfo.docsSizeBytes)}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "PDFs: ${storageInfo.formatBytes(storageInfo.pdfsSizeBytes)}",
                            fontSize = 11.sp,
                            color = EmeraldLight
                        )
                        Text(
                            text = "Photos: ${storageInfo.formatBytes(storageInfo.photosSizeBytes)}",
                            fontSize = 11.sp,
                            color = CyanAccent
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Videos: ${storageInfo.formatBytes(storageInfo.videosSizeBytes)}",
                            fontSize = 11.sp,
                            color = VaultAmber
                        )
                        Text(
                            text = "Hidden Vault: ${storageInfo.formatBytes(storageInfo.hiddenSizeBytes)}",
                            fontSize = 11.sp,
                            color = HiddenPrimaryLight
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.navigateTo(com.example.ui.viewmodel.Screen.STORAGE_MANAGER) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manage_storage_capacity_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Manage Storage & Capacity",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // APPEARANCE & THEMES
            Text(
                text = "APPEARANCE & THEMES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(VaultSurfaceElevated)
                    .border(1.dp, currentTheme.borderColor, RoundedCornerShape(16.dp))
                    .clickable { showThemeDialog = true }
                    .padding(16.dp)
                    .testTag("settings_theme_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(currentTheme.primaryColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = currentTheme.primaryColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Vibrant Themes",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${currentTheme.displayName} • ${currentTheme.iconDescription}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(currentTheme.primaryColor)
                        )
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(currentTheme.accentColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. SECURITY SECTION
            Text(
                text = "SECURITY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(VaultSurfaceElevated)
                    .border(1.dp, VaultBorder, RoundedCornerShape(16.dp))
            ) {
                // App Lock (Fingerprint / PIN / Pattern)
                val currentLockName = when (authManager.appLockType) {
                    AppLockType.FINGERPRINT -> "Fingerprint"
                    AppLockType.PIN -> "PIN (${authManager.getMainPinLength()}-digit)"
                    AppLockType.PATTERN -> "Pattern"
                }
                SettingsItemRow(
                    icon = Icons.Default.LockReset,
                    title = "App Lock",
                    subtitle = "Current: $currentLockName • Switch to Fingerprint, PIN, or Pattern",
                    onClick = { viewModel.openChooseAppLock(isChanging = true) }
                )

                // Auto-Lock Timeout
                Box {
                    SettingsItemRow(
                        icon = Icons.Default.LockClock,
                        title = "Auto Lock",
                        subtitle = autoLockOptions.firstOrNull { it.first == autoLockSecs }?.second ?: "1 minute",
                        onClick = { showAutoLockMenu = true }
                    )

                    DropdownMenu(
                        expanded = showAutoLockMenu,
                        onDismissRequest = { showAutoLockMenu = false },
                        modifier = Modifier.background(VaultSurfaceElevated)
                    ) {
                        autoLockOptions.forEach { (secs, label) ->
                            DropdownMenuItem(
                                text = { Text(label, color = TextPrimary) },
                                onClick = {
                                    autoLockSecs = secs
                                    authManager.autoLockTimeoutSeconds = secs
                                    showAutoLockMenu = false
                                }
                            )
                        }
                    }
                }

                // Biometrics toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Biometric Authentication",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Use fingerprint or facial recognition as alternative to PIN",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = {
                            isBiometricEnabled = it
                            authManager.isBiometricEnabled = it
                            authManager.isBiometricEnabledForMain = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF003822),
                            checkedTrackColor = EmeraldPrimary
                        )
                    )
                }

                // Biometrics toggle for Private Vault
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Biometric for Private Vault",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Scan fingerprint or face to access private vault",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = isPrivateBiometric,
                        onCheckedChange = {
                            isPrivateBiometric = it
                            authManager.isBiometricEnabledForPrivate = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF4A0000),
                            checkedTrackColor = Color(0xFFE53935)
                        )
                    )
                }

                // Change Main Vault PIN
                SettingsItemRow(
                    icon = Icons.Default.Shield,
                    title = "Change Main Vault PIN",
                    subtitle = "Update your primary access passcode",
                    onClick = { viewModel.navigateTo(Screen.SETUP_MAIN_PIN) }
                )

                // Change Private Vault PIN
                SettingsItemRow(
                    icon = Icons.Default.Lock,
                    title = "Change Private Vault PIN",
                    subtitle = "Separate password for private photos & videos",
                    onClick = { viewModel.navigateTo(Screen.SETUP_PRIVATE_PIN) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. PRIVACY SECTION (Hidden Vault OFF by default + Subtle Hidden Hidden Vault)
            Text(
                text = "PRIVACY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(VaultSurfaceElevated)
                    .border(1.dp, VaultBorder, RoundedCornerShape(16.dp))
            ) {
                // Hidden Vault Switch (MUST BE OFF BY DEFAULT)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Hidden Vault",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Keep selected photos and videos private",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Switch(
                        checked = isHiddenEnabled,
                        onCheckedChange = { enabled ->
                            isHiddenEnabled = enabled
                            viewModel.setHiddenVaultEnabled(enabled)
                            if (enabled && !authManager.isPinSetForHidden) {
                                viewModel.navigateTo(Screen.SETUP_HIDDEN_PIN)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF003822),
                            checkedTrackColor = EmeraldPrimary
                        ),
                        modifier = Modifier.testTag("hidden_vault_toggle")
                    )
                }

                // If user turns Hidden Vault ON, show access items:
                if (isHiddenEnabled) {
                    HorizontalDivider(color = VaultBorder.copy(alpha = 0.5f), thickness = 0.5.dp)

                    SettingsItemRow(
                        icon = Icons.Default.Lock,
                        title = "Open Hidden Vault",
                        subtitle = "Discreet entry point with isolated PIN",
                        tint = EmeraldPrimary,
                        onClick = {
                            if (!authManager.isPinSetForHidden) {
                                viewModel.navigateTo(Screen.SETUP_HIDDEN_PIN)
                            } else if (viewModel.isHiddenVaultUnlocked.value) {
                                viewModel.navigateTo(Screen.HIDDEN_VAULT_HOME)
                            } else {
                                viewModel.navigateTo(Screen.HIDDEN_LOCK)
                            }
                        }
                    )

                    SettingsItemRow(
                        icon = Icons.Default.LockReset,
                        title = "Change Hidden Vault PIN",
                        subtitle = "Set independent code for Hidden Vault",
                        tint = TextSecondary,
                        onClick = { viewModel.navigateTo(Screen.SETUP_HIDDEN_PIN) }
                    )

                    // Hidden Biometric Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Biometric for Hidden Vault",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Allow fingerprint or face unlock for hidden area",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = isHiddenBiometric,
                            onCheckedChange = {
                                isHiddenBiometric = it
                                authManager.isBiometricEnabledForHidden = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF003822),
                                checkedTrackColor = EmeraldPrimary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. STORAGE SECTION
            Text(
                text = "STORAGE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(VaultSurfaceElevated)
                    .border(1.dp, VaultBorder, RoundedCornerShape(16.dp))
            ) {
                SettingsItemRow(
                    icon = Icons.Default.Storage,
                    title = "Storage Usage",
                    subtitle = "Capacity breakdown and space cleaner",
                    onClick = { viewModel.navigateTo(Screen.STORAGE_MANAGER) }
                )

                SettingsItemRow(
                    icon = Icons.Default.Delete,
                    title = "Trash",
                    subtitle = "Recover or permanently erase deleted items",
                    onClick = { viewModel.navigateTo(Screen.TRASH) }
                )

                SettingsItemRow(
                    icon = Icons.Default.Backup,
                    title = "Backup & Restore",
                    subtitle = "Manage secure local offline backups",
                    onClick = { viewModel.navigateTo(Screen.BACKUP_RESTORE) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // PRIVACY & SYSTEM AUDIT
            Text(
                text = "SECURITY TOOLS & AUDIT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(VaultSurfaceElevated)
                    .border(1.dp, VaultBorder, RoundedCornerShape(16.dp))
            ) {
                // Privacy Center
                SettingsItemRow(
                    icon = Icons.Default.PrivacyTip,
                    title = "Privacy & Security Center",
                    subtitle = "Cryptographic status, sandboxing & cache cleaner",
                    onClick = { viewModel.navigateTo(Screen.PRIVACY_CENTER) }
                )

                // Security Activity Logs
                SettingsItemRow(
                    icon = Icons.Default.History,
                    title = "Security Activity Logs",
                    subtitle = "Audit log of authentications, imports & exports",
                    onClick = { viewModel.navigateTo(Screen.SECURITY_LOGS) }
                )

                // Permission Manager
                SettingsItemRow(
                    icon = Icons.Default.Security,
                    title = "Permission Manager",
                    subtitle = "Camera, media picker & sensor privacy",
                    onClick = { viewModel.navigateTo(Screen.PERMISSION_MANAGER) }
                )

                // Screenshot Protection toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Screenshot Protection",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Blocks screenshots & app-switcher preview",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = isScreenshotProtect,
                        onCheckedChange = {
                            isScreenshotProtect = it
                            authManager.isScreenshotProtectionEnabled = it
                            viewModel.showToast(if (it) "Screenshot protection active" else "Screenshot protection disabled")
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF003822),
                            checkedTrackColor = EmeraldPrimary
                        )
                    )
                }

                // Emergency Master Recovery Key
                SettingsItemRow(
                    icon = Icons.Default.Key,
                    title = "Emergency Master Recovery Key",
                    subtitle = "View your emergency token in case of lost PIN",
                    onClick = { showRecoveryDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ==========================================
            // ABOUT & APPLICATION SECTION
            // ==========================================
            Text(
                text = "ABOUT & APPLICATION",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(VaultSurfaceElevated)
                    .border(1.dp, VaultBorder, RoundedCornerShape(16.dp))
            ) {
                // App Information & Version
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Personal Document Vault",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Version ${BuildConfig.VERSION_NAME}",
                            fontSize = 12.sp,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                HorizontalDivider(color = VaultBorder, thickness = 0.5.dp)

                // Share App Option
                SettingsItemRow(
                    icon = Icons.Default.Share,
                    title = "Share App",
                    subtitle = "Share clean installer APK with friends via share sheet",
                    onClick = {
                        showShareAppDialog = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // PANIC LOCK BUTTON
            Button(
                onClick = {
                    viewModel.lockAllVaults()
                },
                colors = ButtonDefaults.buttonColors(containerColor = VaultRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("panic_lock_all_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Panic Lock: Lock All Vaults", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // Discreet Code Dialog
        if (showDiscreetCodeDialog) {
            AlertDialog(
                onDismissRequest = { showDiscreetCodeDialog = false },
                containerColor = VaultSurfaceElevated,
                title = { Text("Configure Discreet Access Code", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            text = "Set an emergency access code to authenticate directly into the Hidden Vault.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = customDiscreetCode,
                            onValueChange = { customDiscreetCode = it },
                            placeholder = { Text("e.g. 778899", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = HiddenPrimary,
                                unfocusedBorderColor = VaultBorder
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (customDiscreetCode.isNotBlank()) {
                                authManager.discreetAccessCode = customDiscreetCode.trim()
                                viewModel.showToast("Discreet code updated")
                            }
                            showDiscreetCodeDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HiddenPrimary)
                    ) {
                        Text("Save Code", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDiscreetCodeDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        // Recovery Dialog
        if (showRecoveryDialog) {
            val code = authManager.getOrCreateRecoveryCode()
            AlertDialog(
                onDismissRequest = { showRecoveryDialog = false },
                containerColor = VaultSurfaceElevated,
                title = { Text("Emergency Master Key", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            text = "Keep this key safe offline. If you forget your PIN, this emergency token will let you reset it:",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(VaultDarkBg)
                                .border(1.dp, EmeraldPrimary, RoundedCornerShape(10.dp))
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = code,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                letterSpacing = 3.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showRecoveryDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color(0xFF003822))
                    ) {
                        Text("Done")
                    }
                }
            )
        }

        // Share App Dialog
        if (showShareAppDialog) {
            AlertDialog(
                onDismissRequest = { showShareAppDialog = false },
                containerColor = VaultSurfaceElevated,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text(
                        text = "Share Personal Document Vault",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Send the installable app package (APK) to your friend via WhatsApp, Telegram, Quick Share, or Bluetooth.",
                            fontSize = 13.sp,
                            color = TextPrimary,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF002919))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Install the APK received from your friend. Android may ask you to allow installation from this source.",
                                fontSize = 12.sp,
                                color = EmeraldLight,
                                lineHeight = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Privacy Protected: Only the clean app installer is shared. None of your personal documents, photos, videos, PINs, or vault data are ever shared.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showShareAppDialog = false
                            val success = AppSharingHelper.shareAppApk(context)
                            if (success) {
                                viewModel.showToast("Install the APK received from your friend. Android may ask you to allow installation from this source.")
                            } else {
                                viewModel.showToast("Unable to prepare APK for sharing")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color(0xFF003822)
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share APK", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showShareAppDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        if (showThemeDialog) {
            ThemeSelectorDialog(
                currentTheme = currentTheme,
                onThemeSelected = { viewModel.setAppTheme(it) },
                onDismiss = { showThemeDialog = false }
            )
        }
    }
}

@Composable
private fun SettingsItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color = EmeraldPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
        Text(
            text = "›",
            fontSize = 20.sp,
            color = TextMuted
        )
    }
}
