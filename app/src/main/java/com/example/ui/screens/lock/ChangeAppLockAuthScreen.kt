package com.example.ui.screens.lock

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
fun ChangeAppLockAuthScreen(
    viewModel: VaultViewModel,
    onBiometricRequested: (() -> Unit) -> Unit,
    onAuthenticated: () -> Unit,
    onBack: () -> Unit
) {
    val authManager = viewModel.authManager
    val scope = rememberCoroutineScope()
    val appLockType = authManager.appLockType

    var enteredPin by remember { mutableStateOf("") }
    var pinErrorMessage by remember { mutableStateOf<String?>(null) }
    var shakeTrigger by remember { mutableIntStateOf(0) }

    var patternState by remember { mutableStateOf(PatternState.DEFAULT) }
    var patternErrorMessage by remember { mutableStateOf<String?>(null) }
    var showFingerprintFallbackPin by remember { mutableStateOf(false) }

    val pinLength = authManager.getMainPinLength()

    BackHandler {
        onBack()
    }

    LaunchedEffect(appLockType) {
        if (appLockType == AppLockType.FINGERPRINT && !showFingerprintFallbackPin) {
            delay(200L)
            onBiometricRequested {
                onAuthenticated()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultDarkBg)
            .padding(20.dp)
            .testTag("change_app_lock_auth_screen")
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
                IconButton(onClick = onBack, modifier = Modifier.testTag("auth_change_lock_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
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
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Verify Current Lock",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Authenticate using your current lock method before choosing a new one",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            when {
                appLockType == AppLockType.FINGERPRINT && !showFingerprintFallbackPin -> {
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
                                    onAuthenticated()
                                }
                            },
                            modifier = Modifier.size(110.dp).testTag("trigger_fingerprint_auth_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Scan Fingerprint",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            onBiometricRequested {
                                onAuthenticated()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Scan Fingerprint", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            showFingerprintFallbackPin = true
                            enteredPin = ""
                            pinErrorMessage = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Icon(Icons.Default.Dialpad, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Use Fallback PIN instead", color = TextSecondary, fontSize = 13.sp)
                    }
                }

                appLockType == AppLockType.PATTERN -> {
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(VaultSurfaceElevated)
                            .border(1.dp, VaultBorder, RoundedCornerShape(20.dp))
                            .padding(16.dp)
                            .testTag("pattern_auth_view"),
                        contentAlignment = Alignment.Center
                    ) {
                        PatternLockView(
                            patternState = patternState,
                            activeColor = EmeraldPrimary,
                            successColor = EmeraldLight,
                            errorColor = VaultRed,
                            onPatternComplete = { pattern ->
                                if (authManager.verifyMainPattern(pattern)) {
                                    patternState = PatternState.SUCCESS
                                    patternErrorMessage = null
                                    scope.launch {
                                        delay(300L)
                                        onAuthenticated()
                                    }
                                } else {
                                    patternState = PatternState.ERROR
                                    patternErrorMessage = "Incorrect Pattern"
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
                }

                else -> {
                    // PIN
                    PinPadView(
                        enteredPin = enteredPin,
                        pinLength = pinLength,
                        onPinChange = { newPin ->
                            enteredPin = newPin
                            pinErrorMessage = null
                            if (newPin.length == pinLength) {
                                if (authManager.verifyMainPin(newPin)) {
                                    onAuthenticated()
                                } else {
                                    shakeTrigger++
                                    enteredPin = ""
                                    pinErrorMessage = "Incorrect PIN"
                                }
                            }
                        },
                        errorMessage = pinErrorMessage,
                        shakeTrigger = shakeTrigger,
                        modifier = Modifier.testTag("pin_auth_view")
                    )

                    if (showFingerprintFallbackPin) {
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = {
                                showFingerprintFallbackPin = false
                                enteredPin = ""
                                pinErrorMessage = null
                            }
                        ) {
                            Text("Use Fingerprint instead", color = EmeraldPrimary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
