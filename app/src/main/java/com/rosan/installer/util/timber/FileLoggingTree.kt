package com.rosan.installer.util.timber

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A Timber Tree that logs to a file in the background.
 * Optimized to maintain two most recent log files within a 24-hour window.
 */
@Suppress("LogNotTimber")
class FileLoggingTree(context: Context) : Timber.DebugTree() {

    private val logDir: File = File(context.cacheDir, LOG_DIR_NAME)
    private val backgroundHandler: Handler
    private var currentLogFile: File? = null
    private val handlerThread: HandlerThread

    private val fileNameDateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
    private val entryDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    init {
        if (!logDir.exists()) {
            logDir.mkdirs()
        }

        handlerThread = HandlerThread("AndroidFileLogger.Thread")
        handlerThread.start()
        backgroundHandler = Handler(handlerThread.looper)

        backgroundHandler.post {
            // Attempt to resume the most recent file regardless of the calendar day
            initializeFromExistingFile()
            ensureLogFileReady()
            cleanOldLogFiles()
        }
    }

    /**
     * Finds the most recent log file and checks if it's still valid for writing.
     * It ignores the "today" constraint to support late-night operations.
     */
    private fun initializeFromExistingFile() {
        try {
            val lastFile = logDir.listFiles { _, name -> name.endsWith(LOG_SUFFIX) }
                ?.maxByOrNull { it.lastModified() }

            if (lastFile != null && lastFile.exists()) {
                val lastModifiedTime = lastFile.lastModified()
                val currentTime = System.currentTimeMillis()

                // Check if the file was modified within the last 24 hours and is under the size limit
                if (currentTime - lastModifiedTime < MAX_LOG_AGE_MS && lastFile.length() < MAX_FILE_SIZE) {
                    currentLogFile = lastFile
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize from existing file", e)
        }
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        backgroundHandler.post {
            doLog(priority, tag, message, t)
        }
    }

    private fun doLog(priority: Int, tag: String?, message: String, t: Throwable?) {
        try {
            ensureLogFileReady()
            val file = currentLogFile ?: return

            val timestamp = entryDateFormat.format(Date())
            val priorityStr = priorityToString(priority)
            val logLine = "$timestamp $priorityStr/${tag ?: "Unknown"}: $message\n"

            file.appendText(logLine)
            if (t != null) {
                file.appendText(Log.getStackTraceString(t) + "\n")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing to log file", e)
        }
    }

    /**
     * Ensures a log file is available.
     * Creates a new one if the current file is missing, older than 24h, or too large.
     */
    private fun ensureLogFileReady() {
        val now = System.currentTimeMillis()
        val file = currentLogFile

        var needNewFile = false

        if (!logDir.exists()) {
            logDir.mkdirs()
            needNewFile = true
        }

        if (file == null || !file.exists()) {
            needNewFile = true
        } else {
            val lastModified = file.lastModified()
            // Check if the file has aged more than 24 hours or exceeded size limit
            if (now - lastModified > MAX_LOG_AGE_MS) {
                needNewFile = true
            } else if (file.length() > MAX_FILE_SIZE) {
                needNewFile = true
            }
        }

        if (needNewFile) {
            createNewLogFile(Date(now))
            cleanOldLogFiles()
        }
    }

    private fun createNewLogFile(date: Date) {
        try {
            val fileName = fileNameDateFormat.format(date) + LOG_SUFFIX
            val newFile = File(logDir, fileName)
            if (newFile.createNewFile()) {
                currentLogFile = newFile
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create new log file", e)
        }
    }

    private fun cleanOldLogFiles() {
        try {
            val files = logDir.listFiles { _, name -> name.endsWith(LOG_SUFFIX) } ?: return
            if (files.size > MAX_LOG_FILES) {
                // Keep only the most recently modified file
                files.sortByDescending { it.lastModified() }
                for (i in MAX_LOG_FILES until files.size) {
                    files[i].delete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clean old logs", e)
        }
    }

    private fun priorityToString(priority: Int): String = when (priority) {
        Log.VERBOSE -> "V"
        Log.DEBUG -> "D"
        Log.INFO -> "I"
        Log.WARN -> "W"
        Log.ERROR -> "E"
        Log.ASSERT -> "A"
        else -> "?"
    }

    fun release() {
        handlerThread.quitSafely()
    }

    companion object {
        private const val TAG = "FileLoggingTree"
        private const val MAX_LOG_FILES = 2
        private const val MAX_FILE_SIZE = 4 * 1024 * 1024L // 4MB
        private const val MAX_LOG_AGE_MS = 24 * 60 * 60 * 1000L // 24 Hours

        const val LOG_DIR_NAME = "logs"
        const val LOG_SUFFIX = ".log"
    }
}