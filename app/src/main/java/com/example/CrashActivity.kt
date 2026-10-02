package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

class CrashActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CRASH_INFO = "extra_crash_info"
        const val EXTRA_ERROR_MESSAGE = "extra_error_message"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val crashInfo = intent.getStringExtra(EXTRA_CRASH_INFO) ?: loadLastCrashLog()
        val errorMessage = intent.getStringExtra(EXTRA_ERROR_MESSAGE) ?: "Unexpected Application Exception"

        setContent {
            CrashScreen(
                errorMessage = errorMessage,
                crashInfo = crashInfo,
                onCopy = { copyToClipboard(crashInfo) },
                onRestart = { restartApp() },
                onClearDataAndRestart = { clearDataAndRestart() }
            )
        }
    }

    private fun loadLastCrashLog(): String {
        return try {
            val file = File(filesDir, "last_crash.log")
            if (file.exists()) file.readText() else "No diagnostic crash log available on device storage."
        } catch (t: Throwable) {
            "Failed reading crash log: ${t.message}"
        }
    }

    private fun copyToClipboard(text: String) {
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Crash Diagnostic Log", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Crash details copied to clipboard", Toast.LENGTH_SHORT).show()
        } catch (_: Throwable) {}
    }

    private fun restartApp() {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            if (intent != null) {
                startActivity(intent)
            }
        } catch (_: Throwable) {}
        finish()
    }

    private fun clearDataAndRestart() {
        try {
            deleteDatabase("secure_vault.db")
            File(filesDir, ".vault_keys").deleteRecursively()
            File(filesDir, "vault_files").deleteRecursively()
            File(filesDir, "private_vault").deleteRecursively()
            File(filesDir, "hidden_vault").deleteRecursively()
            File(filesDir, "last_crash.log").delete()
            val sharedPrefsDir = File(applicationInfo.dataDir, "shared_prefs")
            sharedPrefsDir.deleteRecursively()
            Toast.makeText(this, "Vault data cleared. Restarting...", Toast.LENGTH_SHORT).show()
        } catch (_: Throwable) {}
        restartApp()
    }
}

@Composable
fun CrashScreen(
    errorMessage: String,
    crashInfo: String,
    onCopy: () -> Unit,
    onRestart: () -> Unit,
    onClearDataAndRestart: () -> Unit
) {
    val hScroll = rememberScrollState()
    val vScroll = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F0B0B)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .systemBarsPadding()
        ) {
            // Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("crash_header_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4A1010)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Crash Alert",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Diagnostic Crash Reporter",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "An unhandled exception occurred. Details captured below:",
                            color = Color(0xFFFFCDD2),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Error summary badge
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF1E1414),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6B1E1E))
            ) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFFF8A80),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stack trace monospace display
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF070404), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF332222), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(vScroll)
                        .horizontalScroll(hScroll)
                ) {
                    Text(
                        text = crashInfo,
                        color = Color(0xFFE0E0E0),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCopy,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("crash_copy_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Log", fontSize = 12.sp)
                }

                Button(
                    onClick = onRestart,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("crash_restart_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Restart", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restart", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onClearDataAndRestart,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("crash_clear_data_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8A80)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6B1E1E)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset Vault Data & Restart", fontSize = 12.sp)
            }
        }
    }
}
