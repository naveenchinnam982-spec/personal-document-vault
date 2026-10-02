package com.example.ui.screens.lock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.security.AppLockType
import com.example.ui.components.PatternLockView
import com.example.ui.components.PatternState
import com.example.ui.components.PinPadView
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultDarkBg
import com.example.ui.theme.VaultRed
import com.example.ui.theme.VaultSurfaceElevated
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.VaultViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainLockScreen(
    viewModel: VaultViewModel,
    onBiometricRequested: (() -> Unit) -> Unit
) {
    val authManager = viewModel.authManager
    val scope = rememberCoroutineScope()
    val appLockType = authManager.appLockType

    var enteredPin by remember { mutableStateOf("") }
    var pinErrorMessage by remember { mutableStateOf<String?>(null) }
    var shakeTrigger by remember { mutableIntStateOf(0) }
    var lockoutSeconds by remember { mutableIntStateOf(0) }

    var patternState by remember { mutableStateOf(PatternState.DEFAULT) }
    var patternErrorMessage by remember { mutableStateOf<String?>(null) }

    var showFingerprintFallbackPin by remember { mutableStateOf(false) }

    var showForgotDialog by remember { mutableStateOf(false) }
    var recoveryCodeInput by remember { mutableStateOf("") }
    var recoveryError by remember { mutableStateOf<String?>(null) }

    val pinLength = authManager.getMainPinLength()
    val canUseBiometric = authManager.isBiometricEnabledForMain && authManager.canAuthenticateWithBiometrics()

    // Lockout countdown timer
    LaunchedEffect(Unit) {
        while (true) {
            val remaining = authManager.getMainLockoutSecondsRemaining()
            lockoutSeconds = remaining
            if (remaining > 0) {
                pinErrorMessage = "Too many failed attempts. Wait ${remaining}s"
                patternErrorMessage = "Too many failed attempts. Wait ${remaining}s"
            } else {
                if (pinErrorMessage?.startsWith("Too many") == true) pinErrorMessage = null
                if (patternErrorMessage?.startsWith("Too many") == true) patternErrorMessage = null
            }
            delay(1000L)
        }
    }

    // Auto-prompt biometric when lock screen appears for all lock types if biometric is enabled
    LaunchedEffect(appLockType, canUseBiometric) {
        if (canUseBiometric && !showFingerprintFallbackPin && lockoutSeconds == 0) {
            delay(250L)
            onBiometricRequested {
                viewModel.unlockMainVault()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultDarkBg)
            .padding(24.dp)
            .testTag("main_lock_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Lock Badge
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(EmeraldContainer)
                    .border(2.dp, EmeraldPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Personal Vault",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Personal Document Vault",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ==========================================
            // CONDITIONAL VIEW: ONLY SELECTED METHOD APPEARS
            // ==========================================
            when {
                // FINGERPRINT METHOD
                appLockType == AppLockType.FINGERPRINT && !showFingerprintFallbackPin -> {
                    Text(
                        text = "App Locked",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary.copy(alpha = 0.12f))
                            .border(2.dp, EmeraldPrimary.copy(alpha = 0.6f), CircleShape)
                            .testTag("fingerprint_lock_view"),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                if (lockoutSeconds == 0) {
                                    onBiometricRequested {
                                        viewModel.unlockMainVault()
                                    }
                                }
                            },
                            modifier = Modifier.size(120.dp).testTag("fingerprint_unlock_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Fingerprint Sensor",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(68.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Use Fingerprint",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (lockoutSeconds == 0) {
                                onBiometricRequested {
                                    viewModel.unlockMainVault()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("scan_fingerprint_button")
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan Fingerprint", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Fallback PIN Button
                    OutlinedButton(
                        onClick = {
                            showFingerprintFallbackPin = true
                            enteredPin = ""
                            pinErrorMessage = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp).testTag("use_pin_fallback_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dialpad,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Use Fallback PIN", color = TextSecondary, fontSize = 13.sp)
                    }
                }

                // PATTERN METHOD
                appLockType == AppLockType.PATTERN -> {
                    Text(
                        text = "Draw your pattern",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(VaultSurfaceElevated)
                            .border(1.dp, VaultBorder, RoundedCornerShape(20.dp))
                            .padding(16.dp)
                            .testTag("pattern_lock_view"),
                        contentAlignment = Alignment.Center
                    ) {
                        PatternLockView(
                            patternState = patternState,
                            enabled = lockoutSeconds == 0,
                            activeColor = EmeraldPrimary,
                            successColor = EmeraldLight,
                            errorColor = VaultRed,
                            onPatternComplete = { pattern ->
                                if (lockoutSeconds > 0) return@PatternLockView
                                if (authManager.verifyMainPattern(pattern)) {
                                    patternState = PatternState.SUCCESS
                                    patternErrorMessage = null
                                    scope.launch {
                                        delay(300L)
                                        viewModel.unlockMainVault()
                                    }
                                } else {
                                    patternState = PatternState.ERROR
                                    if (authManager.isMainLockedOut()) {
                                        val secs = authManager.getMainLockoutSecondsRemaining()
                                        patternErrorMessage = "Too many failed attempts. Wait ${secs}s"
                                    } else {
                                        patternErrorMessage = "Incorrect Pattern"
                                    }
                                    scope.launch {
                                        delay(1200L)
                                        patternState = PatternState.DEFAULT
                                    }
                                }
                            }
                        )
                    }

                    if (patternErrorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = patternErrorMessage!!,
                            color = VaultRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (canUseBiometric) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (lockoutSeconds == 0) {
                                    onBiometricRequested {
                                        viewModel.unlockMainVault()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .height(46.dp)
                                .testTag("main_pattern_fingerprint_button")
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Unlock with Biometrics", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = {
                            recoveryCodeInput = ""
                            recoveryError = null
                            showForgotDialog = true
                        },
                        modifier = Modifier.testTag("forgot_pattern_button")
                    ) {
                        Text(text = "Forgot Pattern?", color = TextSecondary, fontSize = 13.sp)
                    }
                }

                // PIN METHOD (OR FINGERPRINT FALLBACK TO PIN)
                else -> {
                    Text(
                        text = if (showFingerprintFallbackPin) "Enter Fallback PIN" else "Enter PIN",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    PinPadView(
                        enteredPin = enteredPin,
                        pinLength = pinLength,
                        onPinChange = { newPin ->
                            if (lockoutSeconds == 0) {
                                enteredPin = newPin
                                pinErrorMessage = null
                                if (newPin.length == pinLength) {
                                    if (authManager.verifyMainPin(newPin)) {
                                        viewModel.unlockMainVault()
                                    } else {
                                        shakeTrigger++
                                        enteredPin = ""
                                        if (authManager.isMainLockedOut()) {
                                            val secs = authManager.getMainLockoutSecondsRemaining()
                                            pinErrorMessage = "Too many failed attempts. Wait ${secs}s"
                                        } else {
                                            pinErrorMessage = "Incorrect PIN"
                                        }
                                    }
                                }
                            }
                        },
                        onBiometricClick = if (canUseBiometric) {
                            {
                                if (lockoutSeconds == 0) {
                                    onBiometricRequested {
                                        viewModel.unlockMainVault()
                                    }
                                }
                            }
                        } else null,
                        showBiometric = canUseBiometric,
                        errorMessage = pinErrorMessage,
                        shakeTrigger = shakeTrigger,
                        modifier = Modifier.testTag("pin_lock_view")
                    )

                    if (canUseBiometric && !showFingerprintFallbackPin) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                if (lockoutSeconds == 0) {
                                    onBiometricRequested {
                                        viewModel.unlockMainVault()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .height(46.dp)
                                .testTag("main_pin_fingerprint_button")
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Unlock with Biometrics", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (showFingerprintFallbackPin) {
                        TextButton(
                            onClick = {
                                showFingerprintFallbackPin = false
                                enteredPin = ""
                                pinErrorMessage = null
                            },
                            modifier = Modifier.testTag("back_to_fingerprint_button")
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Use Biometrics instead", color = EmeraldPrimary, fontSize = 13.sp)
                        }
                    } else {
                        TextButton(
                            onClick = {
                                recoveryCodeInput = ""
                                recoveryError = null
                                showForgotDialog = true
                            },
                            modifier = Modifier.testTag("forgot_pin_button")
                        ) {
                            Text(text = "Forgot PIN?", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Forgot PIN / Pattern recovery dialog
        if (showForgotDialog) {
            AlertDialog(
                onDismissRequest = { showForgotDialog = false },
                containerColor = VaultSurfaceElevated,
                title = {
                    Text(
                        text = "Reset Vault Security",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Enter your Emergency Master Key or recovery token to reset your lock credentials.\n(Default master key: SECURE2026)",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = recoveryCodeInput,
                            onValueChange = { recoveryCodeInput = it },
                            placeholder = { Text("Master Key") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = TextMuted
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("recovery_input")
                        )
                        if (recoveryError != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = recoveryError!!,
                                color = VaultRed,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (recoveryCodeInput.trim().equals("SECURE2026", ignoreCase = true) ||
                                authManager.verifyRecoveryCode(recoveryCodeInput)
                            ) {
                                showForgotDialog = false
                                viewModel.navigateTo(Screen.CHOOSE_APP_LOCK_METHOD)
                            } else {
                                recoveryError = "Invalid master key"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color(0xFF003822)
                        )
                    ) {
                        Text("Reset Lock")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showForgotDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }
    }
}
