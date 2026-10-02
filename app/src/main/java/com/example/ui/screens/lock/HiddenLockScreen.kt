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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.ui.theme.HiddenPrimary
import com.example.ui.theme.HiddenPrimaryContainer
import com.example.ui.theme.HiddenPrimaryLight
import com.example.ui.theme.HiddenVaultDarkBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultSurfaceElevated
import com.example.ui.viewmodel.VaultViewModel
import kotlinx.coroutines.delay

@Composable
fun HiddenLockScreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit,
    onTriggerBiometric: () -> Unit = {}
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lockoutSeconds by remember { mutableIntStateOf(0) }
    var shakeTrigger by remember { mutableIntStateOf(0) }
    var showDiscreetDialog by remember { mutableStateOf(false) }
    var discreetInput by remember { mutableStateOf("") }

    val authManager = viewModel.authManager
    val pinLength = authManager.getHiddenPinLength()
    val canUseBiometric = authManager.isBiometricEnabledForHidden && authManager.canAuthenticateWithBiometrics()

    // Auto-prompt biometric on entry if enabled
    LaunchedEffect(Unit) {
        if (canUseBiometric && lockoutSeconds == 0) {
            delay(250L)
            onTriggerBiometric()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val remaining = authManager.getHiddenLockoutSecondsRemaining()
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
            .background(HiddenVaultDarkBg)
            .padding(24.dp)
            .testTag("hidden_lock_screen")
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .testTag("hidden_lock_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary
            )
        }

        IconButton(
            onClick = { showDiscreetDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .testTag("discreet_code_button")
        ) {
            Icon(
                imageVector = Icons.Default.Key,
                contentDescription = "Discreet Code",
                tint = HiddenPrimaryLight
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Shield Icon
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(HiddenPrimaryContainer)
                    .border(2.dp, HiddenPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = HiddenPrimaryLight,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "HIDDEN VAULT",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = 2.sp
            )

            Text(
                text = "Isolated Cryptographic Security Zone",
                fontSize = 12.sp,
                color = HiddenPrimaryLight,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            PinPadView(
                enteredPin = enteredPin,
                pinLength = pinLength,
                onPinChange = { newPin ->
                    if (lockoutSeconds == 0) {
                        enteredPin = newPin
                        errorMessage = null
                        if (newPin.length == pinLength) {
                            if (authManager.verifyHiddenPin(newPin)) {
                                viewModel.unlockHiddenVault()
                            } else {
                                shakeTrigger++
                                enteredPin = ""
                                val rem = authManager.getHiddenLockoutSecondsRemaining()
                                errorMessage = if (rem > 0) "Too many failed attempts. Wait ${rem}s" else "Incorrect Hidden Vault PIN"
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
                    colors = ButtonDefaults.buttonColors(containerColor = HiddenPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(46.dp)
                        .testTag("hidden_fingerprint_button")
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

        // Discreet Code Dialog
        if (showDiscreetDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDiscreetDialog = false
                    discreetInput = ""
                },
                containerColor = VaultSurfaceElevated,
                title = { Text("Discreet Access", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            text = "Enter emergency access code to unlock Hidden Vault directly:",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = discreetInput,
                            onValueChange = { discreetInput = it },
                            placeholder = { Text("Access Code", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = HiddenPrimary,
                                unfocusedBorderColor = VaultBorder
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("discreet_code_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (authManager.verifyDiscreetCode(discreetInput.trim())) {
                                showDiscreetDialog = false
                                viewModel.unlockHiddenVault()
                            } else {
                                viewModel.showToast("Invalid discreet access code")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HiddenPrimary)
                    ) {
                        Text("Authenticate", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDiscreetDialog = false
                        discreetInput = ""
                    }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }
    }
}
