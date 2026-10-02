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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultSurfaceElevated
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.VaultViewModel

@Composable
fun SetupHiddenPinScreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) } // 1: Enter, 2: Confirm
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinLength by remember { mutableIntStateOf(4) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var shakeTrigger by remember { mutableIntStateOf(0) }

    val currentInput = if (step == 1) firstPin else confirmPin

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HiddenVaultDarkBg)
            .padding(24.dp)
            .testTag("setup_hidden_pin_screen")
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart)
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
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(HiddenPrimaryContainer)
                    .border(2.dp, HiddenPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LockReset,
                    contentDescription = null,
                    tint = HiddenPrimaryLight,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = if (step == 1) "Create Hidden Vault PIN" else "Confirm Hidden Vault PIN",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (step == 1)
                    "Separate from your Main & Private Vault PINs"
                else
                    "Re-enter the same $pinLength-digit code to confirm",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // PIN Length selector in step 1
            if (step == 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = pinLength == 4,
                        onClick = {
                            pinLength = 4
                            firstPin = ""
                            errorMessage = null
                        },
                        label = { Text("4 Digits") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HiddenPrimary,
                            selectedLabelColor = TextPrimary,
                            containerColor = VaultSurfaceElevated,
                            labelColor = TextSecondary
                        )
                    )
                    FilterChip(
                        selected = pinLength == 6,
                        onClick = {
                            pinLength = 6
                            firstPin = ""
                            errorMessage = null
                        },
                        label = { Text("6 Digits") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HiddenPrimary,
                            selectedLabelColor = TextPrimary,
                            containerColor = VaultSurfaceElevated,
                            labelColor = TextSecondary
                        )
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            PinPadView(
                enteredPin = currentInput,
                pinLength = pinLength,
                onPinChange = { newPin ->
                    errorMessage = null
                    if (step == 1) {
                        firstPin = newPin
                        if (newPin.length == pinLength) {
                            step = 2
                            confirmPin = ""
                        }
                    } else {
                        confirmPin = newPin
                        if (newPin.length == pinLength) {
                            if (newPin == firstPin) {
                                val success = viewModel.authManager.setHiddenPin(newPin)
                                if (success) {
                                    viewModel.authManager.isHiddenVaultEnabled = true
                                    viewModel.unlockHiddenVault()
                                } else {
                                    errorMessage = "Failed to save Hidden PIN"
                                }
                            } else {
                                shakeTrigger++
                                errorMessage = "PINs do not match. Try again"
                                confirmPin = ""
                            }
                        }
                    }
                },
                errorMessage = errorMessage,
                shakeTrigger = shakeTrigger
            )

            if (step == 2) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = {
                        step = 1
                        firstPin = ""
                        confirmPin = ""
                        errorMessage = null
                    }
                ) {
                    Text("Change PIN length / Start Over", color = HiddenPrimaryLight)
                }
            }
        }
    }
}
