package com.example.ui.screens.hiddenhiddenvault

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PatternLockView
import com.example.ui.components.PatternState
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultDarkBg
import com.example.ui.theme.VaultRed
import com.example.ui.viewmodel.VaultViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class PatternScreenMode {
    CREATE,
    CONFIRM_CREATE,
    UNLOCK,
    CHANGE_CURRENT,
    CHANGE_NEW,
    CHANGE_CONFIRM
}

@Composable
fun HiddenHiddenLockScreen(
    viewModel: VaultViewModel,
    initialMode: PatternScreenMode? = null,
    onBack: () -> Unit
) {
    val authManager = viewModel.authManager
    val scope = rememberCoroutineScope()

    val isPatternAlreadySet = remember { authManager.isHiddenHiddenPatternSet() }

    var mode by remember {
        mutableStateOf(
            initialMode ?: if (isPatternAlreadySet) PatternScreenMode.UNLOCK else PatternScreenMode.CREATE
        )
    }

    var firstCreatedPattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var newChangedPattern by remember { mutableStateOf<List<Int>>(emptyList()) }

    var patternState by remember { mutableStateOf(PatternState.DEFAULT) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isErrorMessage by remember { mutableStateOf(false) }

    var isLockedOut by remember { mutableStateOf(authManager.isHiddenHiddenLockedOut()) }
    var lockoutSeconds by remember { mutableIntStateOf(authManager.getHiddenHiddenLockoutSecondsRemaining()) }

    // Lockout countdown timer
    LaunchedEffect(isLockedOut) {
        while (isLockedOut) {
            val remaining = authManager.getHiddenHiddenLockoutSecondsRemaining()
            lockoutSeconds = remaining
            if (remaining <= 0) {
                isLockedOut = false
                authManager.resetHiddenHiddenAttempts()
                feedbackMessage = null
                break
            }
            delay(1000L)
        }
    }

    BackHandler {
        onBack()
    }

    val title = when (mode) {
        PatternScreenMode.CREATE -> "Create Pattern"
        PatternScreenMode.CONFIRM_CREATE -> "Confirm Pattern"
        PatternScreenMode.UNLOCK -> "Hidden Hidden Vault"
        PatternScreenMode.CHANGE_CURRENT -> "Verify Current Pattern"
        PatternScreenMode.CHANGE_NEW -> "Draw New Pattern"
        PatternScreenMode.CHANGE_CONFIRM -> "Confirm New Pattern"
    }

    val subtitle = when (mode) {
        PatternScreenMode.CREATE -> "Draw a pattern connecting at least 4 dots to secure your ultra-private vault"
        PatternScreenMode.CONFIRM_CREATE -> "Draw the exact same pattern again to confirm"
        PatternScreenMode.UNLOCK -> "Draw your security pattern to unlock"
        PatternScreenMode.CHANGE_CURRENT -> "Draw your current pattern before changing"
        PatternScreenMode.CHANGE_NEW -> "Draw the new pattern (at least 4 dots)"
        PatternScreenMode.CHANGE_CONFIRM -> "Draw the new pattern again to confirm"
    }

    fun handlePattern(pattern: List<Int>) {
        if (isLockedOut) return

        if (pattern.size < 4 && mode in listOf(PatternScreenMode.CREATE, PatternScreenMode.CHANGE_NEW)) {
            patternState = PatternState.ERROR
            feedbackMessage = "Connect at least 4 dots"
            isErrorMessage = true
            scope.launch {
                delay(1200L)
                patternState = PatternState.DEFAULT
            }
            return
        }

        when (mode) {
            PatternScreenMode.CREATE -> {
                firstCreatedPattern = pattern
                patternState = PatternState.SUCCESS
                feedbackMessage = "Pattern recorded! Now confirm it"
                isErrorMessage = false
                scope.launch {
                    delay(500L)
                    mode = PatternScreenMode.CONFIRM_CREATE
                    patternState = PatternState.DEFAULT
                    feedbackMessage = null
                }
            }

            PatternScreenMode.CONFIRM_CREATE -> {
                if (pattern == firstCreatedPattern) {
                    patternState = PatternState.SUCCESS
                    feedbackMessage = "Pattern confirmed! Securing vault..."
                    isErrorMessage = false
                    val saved = authManager.setHiddenHiddenPattern(pattern)
                    if (saved) {
                        scope.launch {
                            delay(600L)
                            viewModel.unlockHiddenHiddenVault()
                        }
                    } else {
                        patternState = PatternState.ERROR
                        feedbackMessage = "Failed to save pattern securely"
                        isErrorMessage = true
                    }
                } else {
                    patternState = PatternState.ERROR
                    feedbackMessage = "Patterns do not match. Try again."
                    isErrorMessage = true
                    scope.launch {
                        delay(1200L)
                        mode = PatternScreenMode.CREATE
                        firstCreatedPattern = emptyList()
                        patternState = PatternState.DEFAULT
                    }
                }
            }

            PatternScreenMode.UNLOCK -> {
                val matches = authManager.verifyHiddenHiddenPattern(pattern)
                if (matches) {
                    patternState = PatternState.SUCCESS
                    feedbackMessage = "Access Granted"
                    isErrorMessage = false
                    scope.launch {
                        delay(400L)
                        viewModel.unlockHiddenHiddenVault()
                    }
                } else {
                    patternState = PatternState.ERROR
                    if (authManager.isHiddenHiddenLockedOut()) {
                        isLockedOut = true
                        feedbackMessage = "Too many failed attempts. Locked out."
                    } else {
                        feedbackMessage = "Incorrect pattern. Try again."
                    }
                    isErrorMessage = true
                    scope.launch {
                        delay(1200L)
                        patternState = PatternState.DEFAULT
                    }
                }
            }

            PatternScreenMode.CHANGE_CURRENT -> {
                val matches = authManager.verifyHiddenHiddenPattern(pattern)
                if (matches) {
                    patternState = PatternState.SUCCESS
                    feedbackMessage = "Current pattern verified"
                    isErrorMessage = false
                    scope.launch {
                        delay(500L)
                        mode = PatternScreenMode.CHANGE_NEW
                        patternState = PatternState.DEFAULT
                        feedbackMessage = null
                    }
                } else {
                    patternState = PatternState.ERROR
                    feedbackMessage = "Incorrect current pattern"
                    isErrorMessage = true
                    scope.launch {
                        delay(1200L)
                        patternState = PatternState.DEFAULT
                    }
                }
            }

            PatternScreenMode.CHANGE_NEW -> {
                newChangedPattern = pattern
                patternState = PatternState.SUCCESS
                feedbackMessage = "New pattern recorded. Now confirm it"
                isErrorMessage = false
                scope.launch {
                    delay(500L)
                    mode = PatternScreenMode.CHANGE_CONFIRM
                    patternState = PatternState.DEFAULT
                    feedbackMessage = null
                }
            }

            PatternScreenMode.CHANGE_CONFIRM -> {
                if (pattern == newChangedPattern) {
                    patternState = PatternState.SUCCESS
                    feedbackMessage = "Pattern changed successfully!"
                    isErrorMessage = false
                    authManager.setHiddenHiddenPattern(pattern)
                    scope.launch {
                        delay(800L)
                        viewModel.showToast("Security pattern updated")
                        if (viewModel.isHiddenHiddenUnlocked.value) {
                            onBack()
                        } else {
                            viewModel.unlockHiddenHiddenVault()
                        }
                    }
                } else {
                    patternState = PatternState.ERROR
                    feedbackMessage = "New patterns do not match. Try again."
                    isErrorMessage = true
                    scope.launch {
                        delay(1200L)
                        mode = PatternScreenMode.CHANGE_NEW
                        newChangedPattern = emptyList()
                        patternState = PatternState.DEFAULT
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F0714),
                        Color(0xFF08040C),
                        VaultDarkBg
                    )
                )
            )
            .testTag("hidden_hidden_lock_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar with Back and Cancel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("pattern_lock_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                TextButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("pattern_lock_cancel_button")
                ) {
                    Text(
                        text = "Cancel",
                        color = TextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Lock Header Icon and Titles
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2A0F1D))
                        .border(1.5.dp, Color(0xFFF43F5E), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (mode == PatternScreenMode.UNLOCK) Icons.Default.Lock else Icons.Default.Security,
                        contentDescription = "Security Lock",
                        tint = Color(0xFFF43F5E),
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isLockedOut) "Security lockout active. Try again in $lockoutSeconds s" else subtitle,
                    fontSize = 13.sp,
                    color = if (isLockedOut) VaultRed else TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Feedback message
                AnimatedVisibility(
                    visible = feedbackMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    feedbackMessage?.let { msg ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isErrorMessage) Color(0xFF3B1219) else Color(0xFF0F382A),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isErrorMessage) VaultRed.copy(alpha = 0.6f) else Color(0xFF10B981).copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = msg,
                                color = if (isErrorMessage) Color(0xFFFF8A9E) else Color(0xFFA7F3D0),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Interactive Pattern Lock View
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                PatternLockView(
                    modifier = Modifier.fillMaxSize(),
                    patternState = patternState,
                    enabled = !isLockedOut,
                    dotColor = Color(0xFF4B5563),
                    activeColor = Color(0xFFF43F5E),
                    successColor = Color(0xFF10B981),
                    errorColor = VaultRed,
                    onPatternComplete = { pattern ->
                        handlePattern(pattern)
                    }
                )
            }

            // Bottom Actions / Helper Controls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                if (mode == PatternScreenMode.CONFIRM_CREATE || mode == PatternScreenMode.CHANGE_CONFIRM) {
                    OutlinedButton(
                        onClick = {
                            if (mode == PatternScreenMode.CONFIRM_CREATE) {
                                mode = PatternScreenMode.CREATE
                                firstCreatedPattern = emptyList()
                            } else {
                                mode = PatternScreenMode.CHANGE_NEW
                                newChangedPattern = emptyList()
                            }
                            patternState = PatternState.DEFAULT
                            feedbackMessage = null
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset & Redraw")
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFFF43F5E).copy(alpha = 0.8f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ultra-Isolated Keystore • Independent Pattern",
                            fontSize = 11.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
