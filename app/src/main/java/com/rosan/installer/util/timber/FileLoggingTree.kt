package com.rosan.installer.util.timber

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A Timber Tree that logs to files asynchronously using Coroutines and Channels.
 * Combines high performance (non-blocking) with robust file rotation logic.
 */
@Suppress("LogNotTimber")
class FileLoggingTree(
    private val context: Context
) : Timber.DebugTree() {

    companion object {
        const val LOG_DIR_NAME = "logs"
        const val LOG_SUFFIX = ".log"
        private const val MAX_LOG_FILES = 2
        private const val MAX_FILE_SIZE = 4 * 1024 * 1024L
        private const val MAX_LOG_AGE_MS = 24 * 60 * 60 * 1000L

        // 调试用的 TAG
        private const val DEBUG_TAG = "FileLoggingTree_Internal"
    }

    private val logDir: File by lazy { File(context.filesDir, LOG_DIR_NAME) }
    private val entryDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val fileNameDateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
    private val logChannel = Channel<String>(Channel.UNLIMITED)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentLogFile: File? = null

    init {
        Log.v(DEBUG_TAG, "🚀 FileLoggingTree 初始化启动！")

        if (!logDir.exists()) {
            val created = logDir.mkdirs()
            Log.v(DEBUG_TAG, "📁 创建日志目录: ${logDir.absolutePath} -> 结果: $created")
        } else {
            Log.v(DEBUG_TAG, "📁 日志目录已存在: ${logDir.absolutePath}")
        }

        scope.launch {
            Log.v(DEBUG_TAG, "🧵 消费者协程启动，准备写入文件...")
            initializeFromExistingFile()

            logChannel.consumeEach { logContent ->
                // 这里是真正的写入操作
                writeToFile(logContent)
            }
        }
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // 1. 收到 Timber 的日志
        Log.v(DEBUG_TAG, "📥 收到 Timber 日志: $message")

        if (priority < Log.DEBUG) {
            Log.v(DEBUG_TAG, "🚫 优先级太低，忽略")
            return
        }

        val timestamp = getCurrentTimestamp()
        val priorityStr = priorityToString(priority)
        val finalTag = tag ?: "Unknown"
        var logLine = "$timestamp $priorityStr/$finalTag: $message\n"
        if (t != null) {
            logLine += Log.getStackTraceString(t) + "\n"
        }

        // 2. 发送到通道
        val result = logChannel.trySend(logLine)
        Log.v(DEBUG_TAG, "📤 发送至写入通道结果: ${result.isSuccess}")
    }

    private fun writeToFile(content: String) {
        try {
            ensureLogFileReady()
            currentLogFile?.appendText(content)
            // 调试：告诉我们写进去了
            Log.v(DEBUG_TAG, "💾 [写入成功] >> ${currentLogFile?.name}")
        } catch (e: Exception) {
            Log.e(DEBUG_TAG, "❌ [写入失败]", e)
        }
    }

    private fun ensureLogFileReady() {
        val now = System.currentTimeMillis()
        var file = currentLogFile
        var needNewFile = false

        if (!logDir.exists()) {
            logDir.mkdirs()
            needNewFile = true
        }

        if (file == null || !file.exists()) {
            needNewFile = true
            Log.v(DEBUG_TAG, "📝 需要新文件：当前文件不存在")
        } else {
            if (file.length() > MAX_FILE_SIZE) {
                needNewFile = true
                Log.v(DEBUG_TAG, "📝 需要新文件：当前文件过大")
            } else if (now - file.lastModified() > MAX_LOG_AGE_MS) {
                needNewFile = true
                Log.v(DEBUG_TAG, "📝 需要新文件：当前文件过期")
            }
        }

        if (needNewFile) {
            createNewLogFile()
            cleanOldLogFiles()
        }
    }

    private fun initializeFromExistingFile() {
        try {
            val lastFile = logDir.listFiles { _, name -> name.endsWith(LOG_SUFFIX) }
                ?.maxByOrNull { it.lastModified() }

            if (lastFile != null && lastFile.exists()) {
                val now = System.currentTimeMillis()
                // Resume writing if file is fresh and small enough
                if (now - lastFile.lastModified() < MAX_LOG_AGE_MS && lastFile.length() < MAX_FILE_SIZE) {
                    currentLogFile = lastFile
                }
            }
        } catch (e: Exception) {
            Log.e("FileLoggingTree", "Failed to init from existing file", e)
        }
    }

    private fun createNewLogFile() {
        try {
            val fileName = fileNameDateFormat.format(Date()) + LOG_SUFFIX
            val newFile = File(logDir, fileName)
            if (newFile.exists()) {
                val uniqueName = fileNameDateFormat.format(Date()) + "_" + System.currentTimeMillis() + LOG_SUFFIX
                currentLogFile = File(logDir, uniqueName)
            } else {
                currentLogFile = newFile
            }
            // 只有这里创建了，文件才会真正出现在磁盘上
            currentLogFile?.createNewFile()
            Log.v(DEBUG_TAG, "🆕 创建新日志文件: ${currentLogFile?.absolutePath}")
        } catch (e: Exception) {
            Log.e(DEBUG_TAG, "❌ 创建文件失败", e)
        }
    }

    /**
     * Maintains only the most recent MAX_LOG_FILES.
     */
    private fun cleanOldLogFiles() {
        try {
            val files = logDir.listFiles { _, name -> name.endsWith(LOG_SUFFIX) } ?: return
            if (files.size > MAX_LOG_FILES) {
                // Sort by modification time (descending)
                files.sortByDescending { it.lastModified() }

                // Delete everything after the Nth file
                for (i in MAX_LOG_FILES until files.size) {
                    files[i].delete()
                }
            }
        } catch (e: Exception) {
            Log.e("FileLoggingTree", "Failed to clean old logs", e)
        }
    }

    // Helper to format date safely
    private fun getCurrentTimestamp(): String {
        return synchronized(entryDateFormat) {
            entryDateFormat.format(Date())
        }
    }

    private fun priorityToString(priority: Int) = when (priority) {
        Log.VERBOSE -> "V"
        Log.DEBUG -> "D"
        Log.INFO -> "I"
        Log.WARN -> "W"
        Log.ERROR -> "E"
        Log.ASSERT -> "A"
        else -> "?"
    }

    /**
     * Call this when you want to stop logging (e.g. user disabled feature).
     */
    fun release() {
        scope.cancel()
        logChannel.close()
    }
}