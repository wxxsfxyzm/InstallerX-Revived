package com.rosan.installer.util.timber

import android.content.Context
import com.rosan.installer.domain.settings.repository.AppSettingsRepo
import com.rosan.installer.domain.settings.repository.BooleanSetting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

class LogController(
    private val context: Context,
    private val appSettingsRepo: AppSettingsRepo
) {
    private var fileLoggingTree: FileLoggingTree? = null

    // Use Main scope for collecting flow, file IO is handled internally by the Tree on IO dispatcher
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun init() {
        scope.launch {
            appSettingsRepo.getBoolean(BooleanSetting.EnableFileLogging, true)
                .collectLatest { enabled ->
                    updateLoggingState(enabled)
                }
        }
    }

    private fun updateLoggingState(enabled: Boolean) {
        if (enabled) {
            if (fileLoggingTree == null) {
                val tree = FileLoggingTree(context)
                Timber.plant(tree)
                fileLoggingTree = tree
            }
        } else {
            fileLoggingTree?.let { tree ->
                Timber.uproot(tree)
                tree.release()
                fileLoggingTree = null
            }
        }
    }
}