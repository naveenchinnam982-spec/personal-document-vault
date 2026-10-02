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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultDarkBg
import com.example.ui.theme.VaultSurfaceElevated
import com.example.ui.viewmodel.VaultViewModel

@Composable
fun SetupPinScreen(
    viewModel: VaultViewModel,
    isPrivateVaultSetup: Boolean = false,
    onSetupComplete: () -> Unit
) {
    var selectedLength by remember { mutableIntStateOf(4) }
    var step by remember { mutableIntStateOf(1) } // 1: Enter, 2: Confirm, 3: Biometric
    var firstEnteredPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var shakeTrigger by remember { mutableIntStateOf(0) }

    val authManager = viewModel.authManager
    val canBiometric = authManager.canAuthenticateWithBiometrics()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultDarkBg)
            .padding(24.dp)
            .testTag(if (isPrivateVaultSetup) "setup_private_pin_screen" else "setup_main_pin_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(EmeraldContainer)
                    .border(2.dp, EmeraldPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPrivateVaultSetup) Icons.Default.Lock else Icons.Default.Shield,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isPrivateVaultSetup) "Set Private Vault PIN" else "Welcome to SecureVault",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = when (step) {
                    1 -> if (isPrivateVaultSetup) "This area has a separate lock.\nCreate your Private Vault PIN" else "Protect your personal documents & IDs with hardware-grade encryption"
                    2 -> "Confirm your $selectedLength-digit PIN"
                    else -> "Enable Biometric Unlock"
                },
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (step == 1) {
                // Pin length selector
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                            selectedLabelColor = Color(0xFF003822),
                            containerColor = VaultSurfaceElevated,
                            labelColor = TextSecondary
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
                            selectedLabelColor = Color(0xFF003822),
                            containerColor = VaultSurfaceElevated,
                            labelColor = TextSecondary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                PinPadView(
                    enteredPin = firstEnteredPin,
                    pinLength = selectedLength,
                    onPinChange = { newPin ->
                        firstEnteredPin = newPin
                        errorMessage = null
                        if (newPin.length == selectedLength) {
                            step = 2
                        }
                    },
                    errorMessage = errorMessage,
                    shakeTrigger = shakeTrigger
                )
            } else if (step == 2) {
                PinPadView(
                    enteredPin = confirmPin,
                    pinLength = selectedLength,
                    onPinChange = { newPin ->
                        confirmPin = newPin
                        errorMessage = null
                        if (newPin.length == selectedLength) {
                            if (newPin == firstEnteredPin) {
                                // Match!
                                if (isPrivateVaultSetup) {
                                    authManager.setPrivatePin(newPin)
                                    onSetupComplete()
                                } else {
                                    authManager.setMainPin(newPin)
                                    if (canBiometric) {
                                        step = 3
                                    } else {
                                        onSetupComplete()
                                    }
                                }
                            } else {
                                errorMessage = "PINs do not match. Try again."
                                shakeTrigger++
                                confirmPin = ""
                            }
                        }
                    },
                    errorMessage = errorMessage,
                    shakeTrigger = shakeTrigger
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        step = 1
                        firstEnteredPin = ""
                        confirmPin = ""
                        errorMessage = null
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("Re-enter PIN")
                }
            } else if (step == 3) {
                // Biometric Choice Screen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(VaultSurfaceElevated)
                        .border(1.dp, VaultBorder, RoundedCornerShape(20.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(EmeraldContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Enable Fingerprint / Face Unlock?",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Unlock your main vault instantly using device biometrics while keeping your PIN as backup.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                authManager.isBiometricEnabled = true
                                onSetupComplete()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color(0xFF003822)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("enable_biometrics_button")
                        ) {
                            Text("Enable Biometrics", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                authManager.isBiometricEnabled = false
                                onSetupComplete()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("skip_biometrics_button")
                        ) {
                            Text("Skip for Now")
                        }
                    }
                }
            }
        }
    }
}
