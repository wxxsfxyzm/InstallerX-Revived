package com.rosan.installer.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.util.timber.FileLoggingTree.Companion.LOG_DIR_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// Minimum time to show the "clearing" state to avoid UI flickering
private const val MIN_FEEDBACK_DURATION_MS = 500L

/**
 * State holder for cache information and clearing logic.
 */
data class CacheInfoState(
    val description: String,
    val inProgress: Boolean,
    val onClear: () -> Unit
)

@Composable
fun rememberCacheInfo(): CacheInfoState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Internal state management
    var inProgress by remember { mutableStateOf(false) }
    var cacheSize by remember { mutableLongStateOf(0L) }
    var calculationTrigger by remember { mutableIntStateOf(0) }

    // Recalculate cache size when triggered or initialized
    LaunchedEffect(calculationTrigger) {
        withContext(Dispatchers.IO) {
            fun calculateSize(file: File): Long {
                if (!file.exists()) return 0L
                if (file.isDirectory) {
                    // if directory is logs, return 0
                    if (file.name == LOG_DIR_NAME) return 0L
                    return file.listFiles()?.sumOf { calculateSize(it) } ?: 0L
                }
                return file.length()
            }

            val internalSize = context.cacheDir?.let { calculateSize(it) } ?: 0L
            val externalSize = context.externalCacheDir?.let { calculateSize(it) } ?: 0L
            cacheSize = internalSize + externalSize
        }
    }

    // Prepare the description text based on the current state
    val description = when {
        inProgress -> stringResource(R.string.clearing_cache)
        cacheSize == 0L -> stringResource(R.string.no_cache)
        else -> stringResource(R.string.cache_size, cacheSize.formatSize())
    }

    val onClear = {
        if (!inProgress) {
            scope.launch {
                inProgress = true
                val startTime = System.currentTimeMillis()

                // Perform file deletion on IO dispatcher
                withContext(Dispatchers.IO) {
                    val paths = listOfNotNull(
                        context.cacheDir,
                        context.externalCacheDir
                    )

                    // Blacklist configuration for critical files
                    val blacklistExtensions = listOf(".lck", ".lock")
                    val blacklistNames = listOf(LOG_DIR_NAME)

                    /**
                     * Recursively clears a file or directory while respecting the blacklist.
                     */
                    fun clearFile(file: File): Boolean {
                        if (!file.exists()) return true
                        if (blacklistNames.contains(file.name)) return false
                        if (blacklistExtensions.any { file.name.endsWith(it) }) return false

                        if (file.isDirectory) {
                            val children = file.listFiles()
                            var allChildrenDeleted = true
                            children?.forEach { child ->
                                if (!clearFile(child)) {
                                    allChildrenDeleted = false
                                }
                            }
                            if (!allChildrenDeleted) return false
                        }
                        return file.delete()
                    }

                    // Clear children of cache roots instead of deleting roots themselves
                    paths.forEach { root ->
                        root.listFiles()?.forEach { child ->
                            clearFile(child)
                        }
                    }
                }

                // Ensure a smooth transition if deletion was too fast
                val elapsedTime = System.currentTimeMillis() - startTime
                if (elapsedTime < MIN_FEEDBACK_DURATION_MS) {
                    delay(MIN_FEEDBACK_DURATION_MS - elapsedTime)
                }

                inProgress = false
                // Increment trigger to refresh the cache size display
                calculationTrigger++
            }
        }
    }

    return remember(description, inProgress) {
        CacheInfoState(description, inProgress, onClear)
    }
}