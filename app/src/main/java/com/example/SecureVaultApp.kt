package com.example

import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Process
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SecureVaultApp : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            ensureMediaExclusion()
            setupGlobalExceptionHandler()
        } catch (t: Throwable) {
            Log.e("SecureVaultApp", "Failed configuring application: ${t.message}", t)
        }
    }

    private fun ensureMediaExclusion() {
        try {
            listOf(
                filesDir,
                cacheDir,
                File(filesDir, "vault_hidden"),
                File(filesDir, "vault_private"),
                File(filesDir, "vault_hidden_hidden"),
                File(filesDir, "vault_docs"),
                File(filesDir, "vault_thumbnails"),
                File(filesDir, ".vault_thumbnails"),
                File(filesDir, ".vault_keys"),
                File(cacheDir, "shared_export"),
                getExternalFilesDir(null)
            ).forEach { dir ->
                if (dir != null) {
                    try {
                        dir.mkdirs()
                        val nomedia = File(dir, ".nomedia")
                        if (!nomedia.exists()) {
                            nomedia.createNewFile()
                        }
                    } catch (_: Throwable) {}
                }
            }
        } catch (_: Throwable) {}
    }

    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e("SecureVaultApp", "Uncaught exception on thread ${thread.name}: ${throwable.message}", throwable)

                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val stackTrace = sw.toString()

                val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                val crashReport = buildString {
                    appendLine("Time: ${timeFormat.format(Date())}")
                    appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
                    appendLine("Android OS: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                    appendLine("Supported ABIs: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
                    appendLine("Thread: ${thread.name}")
                    appendLine("Exception: ${throwable.javaClass.name}")
                    appendLine("Message: ${throwable.message}")
                    appendLine()
                    appendLine("Stack Trace:")
                    appendLine(stackTrace)
                }

                try {
                    val crashFile = File(filesDir, "last_crash.log")
                    crashFile.writeText(crashReport)
                } catch (_: Throwable) {}

                val intent = Intent(this, CrashActivity::class.java).apply {
                    putExtra(CrashActivity.EXTRA_CRASH_INFO, crashReport)
                    putExtra(CrashActivity.EXTRA_ERROR_MESSAGE, throwable.message ?: throwable.javaClass.simpleName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)

                // Terminate main crashing process cleanly so CrashActivity in :crash process displays smoothly
                Process.killProcess(Process.myPid())
            } catch (e: Throwable) {
                Log.e("SecureVaultApp", "Exception handler failed: ${e.message}", e)
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
