package com.example.ui.screens.lock

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.security.AppLockType
import com.example.data.security.BiometricStatus
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultDarkBg
import com.example.ui.theme.VaultSurfaceElevated
import com.example.ui.viewmodel.VaultViewModel

@Composable
fun ChooseAppLockScreen(
    viewModel: VaultViewModel,
    isChangingMethod: Boolean = false,
    onBack: (() -> Unit)? = null
) {
    val authManager = viewModel.authManager
    val biometricStatus = remember { authManager.getBiometricStatus() }
    var biometricUnavailableDialog by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultDarkBg)
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .testTag("choose_app_lock_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar with optional Back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isChangingMethod && onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("choose_lock_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Vault Security Shield Icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(EmeraldContainer)
                    .border(2.dp, EmeraldPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = if (isChangingMethod) "Change App Lock Method" else "Choose your App Lock Method",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Select ONE preferred lock method to protect your Personal Document Vault on startup.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Option 1: Fingerprint & Facial Recognition
            val isFingerprintAvailable = biometricStatus == BiometricStatus.AVAILABLE
            LockMethodCard(
                icon = Icons.Default.Fingerprint,
                title = "Biometrics (Fingerprint / Face)",
                subtitle = when (biometricStatus) {
                    BiometricStatus.AVAILABLE -> "Unlock quickly and securely with fingerprint or face recognition as an alternative to PIN"
                    BiometricStatus.NOT_ENROLLED -> "No fingerprint or face enrolled on device. Tap for setup details or choose PIN/Pattern"
                    BiometricStatus.NO_HARDWARE -> "Biometric hardware not detected on this device"
                    BiometricStatus.UNAVAILABLE -> "Biometric sensors currently unavailable"
                },
                badgeText = if (isFingerprintAvailable) "BIOMETRIC" else "UNAVAILABLE",
                badgeColor = if (isFingerprintAvailable) EmeraldPrimary else VaultAmber,
                accentColor = EmeraldPrimary,
                testTag = "lock_option_fingerprint",
                onClick = {
                    if (isFingerprintAvailable) {
                        viewModel.startAppLockSetup(AppLockType.FINGERPRINT, isChangingMethod)
                    } else {
                        biometricUnavailableDialog = when (biometricStatus) {
                            BiometricStatus.NOT_ENROLLED ->
                                "No fingerprints or facial recognition credentials are currently enrolled on this device.\n\nPlease register a biometric in your Android System Settings (Security > Biometrics), or choose PIN or Pattern Lock."
                            BiometricStatus.NO_HARDWARE ->
                                "This device does not have biometric hardware (fingerprint or face recognition).\n\nPlease select PIN or Pattern Lock to protect your vault."
                            else ->
                                "Biometric authentication is not currently available.\n\nPlease select PIN or Pattern Lock to protect your vault."
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Option 2: PIN / Number Lock
            LockMethodCard(
                icon = Icons.Default.Dialpad,
                title = "PIN / Number Lock",
                subtitle = "Enter a 4 or 6-digit numeric passcode with hardware-grade salted hash encryption",
                badgeText = "RECOMMENDED",
                badgeColor = CyanAccent,
                accentColor = CyanAccent,
                testTag = "lock_option_pin",
                onClick = {
                    viewModel.startAppLockSetup(AppLockType.PIN, isChangingMethod)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Option 3: Pattern Lock
            LockMethodCard(
                icon = Icons.Default.GridOn,
                title = "Pattern Lock",
                subtitle = "Draw a custom connecting gesture pattern across a 3x3 security dot matrix",
                badgeText = "GESTURE",
                badgeColor = EmeraldLight,
                accentColor = EmeraldLight,
                testTag = "lock_option_pattern",
                onClick = {
                    viewModel.startAppLockSetup(AppLockType.PATTERN, isChangingMethod)
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Explanation footnote
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(VaultSurfaceElevated)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "The selected lock method will be your primary startup screen. You can change this anytime in Settings.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }

        // Biometric Unavailable Information Dialog
        if (biometricUnavailableDialog != null) {
            AlertDialog(
                onDismissRequest = { biometricUnavailableDialog = null },
                containerColor = VaultSurfaceElevated,
                title = {
                    Text(
                        text = "Fingerprint Unavailable",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                text = {
                    Text(
                        text = biometricUnavailableDialog!!,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { biometricUnavailableDialog = null },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("OK", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@Composable
private fun LockMethodCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(VaultSurfaceElevated)
            .border(1.dp, VaultBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(18.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.dp, accentColor.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
