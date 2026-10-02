package com.example.ui.screens.lock

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import com.example.ui.theme.CyanAccent
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
import com.example.ui.viewmodel.VaultViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SetupAppLockScreen(
    viewModel: VaultViewModel,
    lockType: AppLockType,
    isChangingMethod: Boolean = false,
    onBiometricRequested: (() -> Unit) -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    val authManager = viewModel.authManager
    val scope = rememberCoroutineScope()

    BackHandler {
        onBack()
    }

    when (lockType) {
        AppLockType.PIN -> {
            SetupPinFlow(
                onPinConfigured = { pin ->
                    authManager.setMainPin(pin)
                    authManager.appLockType = AppLockType.PIN
                    viewModel.showToast("PIN Lock successfully configured")
                    onComplete()
                },
                onBack = onBack
            )
        }

        AppLockType.PATTERN -> {
            SetupPatternFlow(
                onPatternConfigured = { pattern ->
                    authManager.setMainPattern(pattern)
                    authManager.appLockType = AppLockType.PATTERN
                    viewModel.showToast("Pattern Lock successfully configured")
                    onComplete()
                },
                onBack = onBack
            )
        }

        AppLockType.FINGERPRINT -> {
            SetupFingerprintFlow(
                onBiometricRequested = onBiometricRequested,
                onFingerprintConfigured = { fallbackPin ->
                    authManager.setMainPin(fallbackPin)
                    authManager.setFingerprintConfigured(true)
                    authManager.appLockType = AppLockType.FINGERPRINT
                    viewModel.showToast("Fingerprint Lock successfully configured")
                    onComplete()
                },
                onBack = onBack
            )
        }
    }
}

// ==========================================
// 1. PIN SETUP FLOW
// ==========================================
@Composable
private fun SetupPinFlow(
    onPinConfigured: (String) -> Unit,
    onBack: () -> Unit
) {
    var selectedLength by remember { mutableIntStateOf(4) }
    var step by remember { mutableIntStateOf(1) } // 1: Enter, 2: Confirm
    var firstEnteredPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultDarkBg)
            .padding(20.dp)
            .testTag("setup_pin_flow")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("setup_pin_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (step == 1) "Step 1 of 2" else "Step 2 of 2",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(EmeraldContainer)
                    .border(2.dp, EmeraldPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Dialpad,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (step == 1) "Create Your PIN" else "Confirm Your PIN",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (step == 1) "Enter a $selectedLength-digit passcode for your vault" else "Re-enter your $selectedLength-digit passcode to confirm",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (step == 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = selectedLength == 4,
                        onClick = {
                            selectedLength = 4
                            firstEnteredPin = ""
                            errorMessage = null
                        },
                        label = { Text("4 Digits") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.Black
                        )
                    )
                    FilterChip(
                        selected = selectedLength == 6,
                        onClick = {
                            selectedLength = 6
                            firstEnteredPin = ""
                            errorMessage = null
                        },
                        label = { Text("6 Digits") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val currentPin = if (step == 1) firstEnteredPin else confirmPin
            PinPadView(
                enteredPin = currentPin,
                pinLength = selectedLength,
                onPinChange = { newPin ->
                    errorMessage = null
                    if (step == 1) {
                        firstEnteredPin = newPin
                        if (newPin.length == selectedLength) {
                            step = 2
                        }
                    } else {
                        confirmPin = newPin
                        if (newPin.length == selectedLength) {
                            if (newPin == firstEnteredPin) {
                                onPinConfigured(newPin)
                            } else {
                                errorMessage = "PINs do not match. Please try again."
                                confirmPin = ""
                            }
                        }
                    }
                }
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage!!,
                    color = VaultRed,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }

            if (step == 2) {
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = {
                        step = 1
                        confirmPin = ""
                        errorMessage = null
                    }
                ) {
                    Text("Re-enter original PIN", color = EmeraldPrimary, fontSize = 13.sp)
                }
            }
        }
    }
}

// ==========================================
// 2. PATTERN SETUP FLOW
// ==========================================
@Composable
private fun SetupPatternFlow(
    onPatternConfigured: (List<Int>) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var step by remember { mutableIntStateOf(1) } // 1: Create, 2: Confirm
    var firstPattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var patternState by remember { mutableStateOf(PatternState.DEFAULT) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultDarkBg)
            .padding(20.dp)
            .testTag("setup_pattern_flow")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("setup_pattern_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (step == 1) "Step 1 of 2" else "Step 2 of 2",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(EmeraldContainer)
                    .border(2.dp, EmeraldPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GridOn,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (step == 1) "Draw Your Pattern" else "Confirm Your Pattern",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (step == 1) "Connect at least 4 dots to create your pattern" else "Draw the exact same pattern again to confirm",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(VaultSurfaceElevated)
                    .border(1.dp, VaultBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                PatternLockView(
                    patternState = patternState,
                    activeColor = EmeraldPrimary,
                    successColor = EmeraldLight,
                    errorColor = VaultRed,
                    onPatternComplete = { pattern ->
                        if (pattern.size < 4 && step == 1) {
                            patternState = PatternState.ERROR
                            feedbackMessage = "Connect at least 4 dots"
                            isError = true
                            scope.launch {
                                delay(1200L)
                                patternState = PatternState.DEFAULT
                            }
                            return@PatternLockView
                        }

                        if (step == 1) {
                            firstPattern = pattern
                            patternState = PatternState.SUCCESS
                            feedbackMessage = "Pattern recorded! Now confirm it"
                            isError = false
                            scope.launch {
                                delay(600L)
                                step = 2
                                patternState = PatternState.DEFAULT
                                feedbackMessage = null
                            }
                        } else {
                            if (pattern == firstPattern) {
                                patternState = PatternState.SUCCESS
                                feedbackMessage = "Pattern confirmed!"
                                isError = false
                                scope.launch {
                                    delay(500L)
                                    onPatternConfigured(pattern)
                                }
                            } else {
                                patternState = PatternState.ERROR
                                feedbackMessage = "Patterns do not match. Try again."
                                isError = true
                                scope.launch {
                                    delay(1200L)
                                    patternState = PatternState.DEFAULT
                                }
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (feedbackMessage != null) {
                Text(
                    text = feedbackMessage!!,
                    fontSize = 13.sp,
                    color = if (isError) VaultRed else EmeraldLight,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            if (step == 2) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        step = 1
                        firstPattern = emptyList()
                        patternState = PatternState.DEFAULT
                        feedbackMessage = null
                    }
                ) {
                    Text("Redraw original pattern", color = EmeraldPrimary, fontSize = 13.sp)
                }
            }
        }
    }
}

// ==========================================
// 3. FINGERPRINT SETUP FLOW (WITH FALLBACK PIN)
// ==========================================
@Composable
private fun SetupFingerprintFlow(
    onBiometricRequested: (() -> Unit) -> Unit,
    onFingerprintConfigured: (String) -> Unit,
    onBack: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) } // 1: Verify Biometric, 2: Create Fallback PIN, 3: Confirm Fallback PIN
    var isFingerprintVerified by remember { mutableStateOf(false) }
    var fallbackPin by remember { mutableStateOf("") }
    var confirmFallbackPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultDarkBg)
            .padding(20.dp)
            .testTag("setup_fingerprint_flow")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("setup_fingerprint_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = when (step) {
                        1 -> "Step 1 of 3: Biometric"
                        2 -> "Step 2 of 3: Fallback PIN"
                        else -> "Step 3 of 3: Confirm PIN"
                    },
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(EmeraldContainer)
                    .border(2.dp, EmeraldPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (step == 1) Icons.Default.Fingerprint else Icons.Default.Lock,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when (step) {
                    1 -> "Test Fingerprint Sensor"
                    2 -> "Create Fallback PIN"
                    else -> "Confirm Fallback PIN"
                },
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = when (step) {
                    1 -> "Verify your fingerprint with your device sensor to ensure fast unlocking"
                    2 -> "Set a 4-digit PIN in case your fingerprint sensor is unavailable or dirty"
                    else -> "Re-enter the 4-digit fallback PIN to confirm"
                },
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (step == 1) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary.copy(alpha = 0.12f))
                        .border(2.dp, EmeraldPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            onBiometricRequested {
                                isFingerprintVerified = true
                                step = 2
                            }
                        },
                        modifier = Modifier.size(110.dp).testTag("trigger_fingerprint_test_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Test Fingerprint",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        onBiometricRequested {
                            isFingerprintVerified = true
                            step = 2
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("verify_fingerprint_btn")
                ) {
                    Text("Scan Fingerprint", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Tap the button to test your device fingerprint sensor.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            } else {
                // Steps 2 & 3: Fallback PIN
                val currentPin = if (step == 2) fallbackPin else confirmFallbackPin
                PinPadView(
                    enteredPin = currentPin,
                    pinLength = 4,
                    onPinChange = { newPin ->
                        errorMessage = null
                        if (step == 2) {
                            fallbackPin = newPin
                            if (newPin.length == 4) {
                                step = 3
                            }
                        } else {
                            confirmFallbackPin = newPin
                            if (newPin.length == 4) {
                                if (newPin == fallbackPin) {
                                    onFingerprintConfigured(newPin)
                                } else {
                                    errorMessage = "Fallback PINs do not match. Please try again."
                                    confirmFallbackPin = ""
                                }
                            }
                        }
                    }
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage!!,
                        color = VaultRed,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
