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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.bulk.BulkImportState
import com.example.data.bulk.BulkImportStatus
import com.example.ui.theme.CyanAccent
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

@Composable
fun BulkImportProgressDialog(
    state: BulkImportState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetryFailed: () -> Unit,
    onDismiss: () -> Unit
) {
    if (state.status == BulkImportStatus.IDLE || state.status == BulkImportStatus.PREPARING || state.status == BulkImportStatus.CONFIRMING) {
        return
    }

    var showFailedDetails by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = {
            if (state.isComplete || state.status == BulkImportStatus.CANCELLED) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = state.isComplete || state.status == BulkImportStatus.CANCELLED,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = VaultSurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .testTag("bulk_import_progress_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status Header Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                state.status == BulkImportStatus.LOW_STORAGE_PAUSED -> VaultRed.copy(alpha = 0.15f)
                                state.status == BulkImportStatus.PAUSED -> VaultAmber.copy(alpha = 0.15f)
                                state.isComplete && state.failedCount > 0 -> VaultAmber.copy(alpha = 0.15f)
                                state.isComplete -> EmeraldPrimary.copy(alpha = 0.15f)
                                else -> CyanAccent.copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            state.status == BulkImportStatus.LOW_STORAGE_PAUSED -> Icons.Default.Warning
                            state.status == BulkImportStatus.PAUSED -> Icons.Default.Pause
                            state.isComplete && state.failedCount == 0 -> Icons.Default.CheckCircle
                            state.isComplete -> Icons.Default.Refresh
                            else -> Icons.Default.Security
                        },
                        contentDescription = null,
                        tint = when {
                            state.status == BulkImportStatus.LOW_STORAGE_PAUSED -> VaultRed
                            state.status == BulkImportStatus.PAUSED -> VaultAmber
                            state.isComplete && state.failedCount > 0 -> VaultAmber
                            state.isComplete -> EmeraldLight
                            else -> CyanAccent
                        },
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title
                Text(
                    text = when (state.status) {
                        BulkImportStatus.IMPORTING -> "Importing Photos..."
                        BulkImportStatus.PAUSED -> "Import Paused"
                        BulkImportStatus.LOW_STORAGE_PAUSED -> "Low Storage Paused"
                        BulkImportStatus.COMPLETED -> if (state.failedCount > 0) "Import Complete with Warnings" else "Import Complete"
                        BulkImportStatus.CANCELLED -> "Import Cancelled"
                        else -> "Processing..."
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // PROGRESS VIEW (while importing or paused)
                if (state.isImporting || state.isPaused) {
                    val percentage = (state.currentProgressRatio * 100).toInt().coerceIn(0, 100)

                    Text(
                        text = state.currentFileName,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { state.currentProgressRatio },
                        color = if (state.isPaused) VaultAmber else EmeraldPrimary,
                        trackColor = VaultBorder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$percentage%",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.isPaused) VaultAmber else EmeraldLight
                        )
                        Text(
                            text = "${state.completedCount} / ${state.totalCount}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Stats Grid
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(VaultDarkBg)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Completed", fontSize = 11.sp, color = TextSecondary)
                            Text("${state.successCount}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldLight)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Failed", fontSize = 11.sp, color = TextSecondary)
                            Text("${state.failedCount}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (state.failedCount > 0) VaultRed else TextMuted)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Skipped", fontSize = 11.sp, color = TextSecondary)
                            Text("${state.skippedDuplicatesCount}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Remaining", fontSize = 11.sp, color = TextSecondary)
                            Text("${state.remainingCount}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }

                    if (state.status == BulkImportStatus.LOW_STORAGE_PAUSED) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = state.errorMessage ?: "Device storage critically low. Free storage and tap Resume.",
                            fontSize = 12.sp,
                            color = VaultRed,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Controls: Pause / Resume / Cancel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder)
                        ) {
                            Text("Cancel", color = TextSecondary)
                        }

                        if (state.isPaused) {
                            Button(
                                onClick = onResume,
                                modifier = Modifier.weight(1.2f).height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color(0xFF003822))
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Resume", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = onPause,
                                modifier = Modifier.weight(1.2f).height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VaultAmber, contentColor = Color(0xFF332000))
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pause", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // COMPLETED VIEW
                if (state.isComplete || state.status == BulkImportStatus.CANCELLED) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(VaultDarkBg)
                            .border(1.dp, VaultBorder, RoundedCornerShape(14.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Processed", fontSize = 13.sp, color = TextSecondary)
                            Text("${state.totalCount}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Imported Successfully", fontSize = 13.sp, color = EmeraldLight)
                            Text("${state.successCount}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldLight)
                        }
                        if (state.skippedDuplicatesCount > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Duplicates Skipped", fontSize = 13.sp, color = TextMuted)
                                Text("${state.skippedDuplicatesCount}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                            }
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

                    if (state.failedCount > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(onClick = { showFailedDetails = !showFailedDetails }) {
                            Text(
                                text = if (showFailedDetails) "Hide Failed Items" else "View Failed Items (${state.failedCount})",
                                color = VaultAmber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (showFailedDetails) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(VaultDarkBg)
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(state.failedItems) { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = item.fileName,
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = item.reason,
                                            fontSize = 11.sp,
                                            color = VaultRed,
                                            maxLines = 1
                                        )
                                    }
                                }
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
                                onClick = onRetryFailed,
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, VaultAmber)
                            ) {
                                Text("Retry Failed", color = VaultAmber, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1.2f).height(46.dp),
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
