package com.example.ui.screens.lock

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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.ui.components.PinPadView
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultDarkBg
import com.example.ui.theme.VaultRed
import com.example.ui.theme.VaultRedContainer
import com.example.ui.viewmodel.VaultViewModel
import kotlinx.coroutines.delay

@Composable
fun PrivateLockScreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit,
    onTriggerBiometric: () -> Unit = {}
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var shakeTrigger by remember { mutableIntStateOf(0) }
    var lockoutSeconds by remember { mutableIntStateOf(0) }

    val authManager = viewModel.authManager
    val pinLength = authManager.getPrivatePinLength()
    val canUseBiometric = authManager.isBiometricEnabledForPrivate && authManager.canAuthenticateWithBiometrics()

    // Auto-prompt biometric on entry if enabled
    LaunchedEffect(Unit) {
        if (canUseBiometric && lockoutSeconds == 0) {
            delay(250L)
            onTriggerBiometric()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val remaining = authManager.getPrivateLockoutSecondsRemaining()
            lockoutSeconds = remaining
            if (remaining > 0) {
                errorMessage = "Too many failed attempts. Wait ${remaining}s"
            } else if (errorMessage?.startsWith("Too many") == true) {
                errorMessage = null
            }
            delay(1000L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultDarkBg)
            .padding(24.dp)
            .testTag("private_lock_screen")
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .testTag("private_lock_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Private Lock Shield
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(VaultRedContainer)
                    .border(2.dp, VaultRed, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = VaultRed,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "PRIVATE VAULT",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "This area has a separate lock.\nEnter Private Vault PIN",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            PinPadView(
                enteredPin = enteredPin,
                pinLength = pinLength,
                onPinChange = { newPin ->
                    if (lockoutSeconds == 0) {
                        enteredPin = newPin
                        errorMessage = null
                        if (newPin.length == pinLength) {
                            if (authManager.verifyPrivatePin(newPin)) {
                                viewModel.unlockPrivateVault()
                            } else {
                                shakeTrigger++
                                enteredPin = ""
                                if (authManager.isPrivateLockedOut()) {
                                    val secs = authManager.getPrivateLockoutSecondsRemaining()
                                    errorMessage = "Too many failed attempts. Wait ${secs}s"
                                } else {
                                    errorMessage = "Incorrect Private PIN"
                                }
                            }
                        }
                    }
                },
                onBiometricClick = if (canUseBiometric) {
                    {
                        if (lockoutSeconds == 0) {
                            onTriggerBiometric()
                        }
                    }
                } else null,
                showBiometric = canUseBiometric,
                errorMessage = errorMessage,
                shakeTrigger = shakeTrigger
            )

            if (canUseBiometric) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        if (lockoutSeconds == 0) {
                            onTriggerBiometric()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(46.dp)
                        .testTag("private_fingerprint_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Scan Fingerprint",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Unlock with Biometrics",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
