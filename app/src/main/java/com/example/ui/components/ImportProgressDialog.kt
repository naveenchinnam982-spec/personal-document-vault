package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
import com.example.ui.theme.VaultRed
import com.example.ui.theme.VaultSurfaceElevated
import com.example.ui.viewmodel.VaultViewModel

@Composable
fun ImportProgressDialog(
    state: VaultViewModel.MultiImportState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    if (!state.isImporting && !state.isComplete && state.errorMessage == null) {
        return
    }

    Dialog(onDismissRequest = {
        if (!state.isImporting) onDismiss()
    }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = VaultSurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("import_progress_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                state.errorMessage != null -> VaultRed.copy(alpha = 0.15f)
                                state.isComplete && state.failedCount > 0 -> VaultAmber.copy(alpha = 0.15f)
                                state.isComplete -> EmeraldPrimary.copy(alpha = 0.15f)
                                else -> CyanAccent.copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            state.errorMessage != null -> Icons.Default.Error
                            state.isComplete && state.failedCount == 0 -> Icons.Default.CheckCircle
                            state.isComplete -> Icons.Default.Refresh
                            else -> Icons.Default.Security
                        },
                        contentDescription = null,
                        tint = when {
                            state.errorMessage != null -> VaultRed
                            state.isComplete && state.failedCount > 0 -> VaultAmber
                            state.isComplete -> EmeraldLight
                            else -> CyanAccent
                        },
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = when {
                        state.errorMessage != null -> "Storage Alert"
                        state.isComplete && state.failedCount > 0 -> "Import Finished with Warnings"
                        state.isComplete -> "Import Successful"
                        else -> "Importing Securely..."
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Content
                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage,
                        fontSize = 14.sp,
                        color = TextSecondary,
                        lineHeight = 20.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color(0xFF003822)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text("Dismiss", fontWeight = FontWeight.Bold)
                    }
                } else if (state.isImporting) {
                    val progressFloat = if (state.totalCount > 0) state.currentProgress.toFloat() / state.totalCount else 0f
                    val percentage = (progressFloat * 100).toInt()

                    Text(
                        text = state.currentFileName,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    LinearProgressIndicator(
                        progress = { progressFloat },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = EmeraldPrimary,
                        trackColor = VaultBorder,
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$percentage%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldLight
                        )
                        Text(
                            text = "${state.currentProgress} / ${state.totalCount} files",
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    }
                } else if (state.isComplete) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(VaultDarkBg)
                            .border(1.dp, VaultBorder, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total selected", fontSize = 13.sp, color = TextSecondary)
                            Text("${state.totalCount}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Successfully encrypted", fontSize = 13.sp, color = EmeraldLight)
                            Text("${state.successCount}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldLight)
                        }
                        if (state.failedCount > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Failed", fontSize = 13.sp, color = VaultRed)
                                Text("${state.failedCount}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VaultRed)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (state.failedCount > 0) {
                            OutlinedButton(
                                onClick = onRetry,
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, VaultAmber)
                            ) {
                                Text("Retry Failed", color = VaultAmber, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color(0xFF003822))
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
