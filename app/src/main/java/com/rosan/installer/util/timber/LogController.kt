package com.rosan.installer.util.timber

import android.content.Context
import android.util.Log
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

class LogController(
    private val context: Context,
    private val appDataStore: AppDataStore
) {
    private var fileLoggingTree: FileLoggingTree? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // 添加 init 块，确保类被加载时就有日志
    init {
        Log.e("LogController", "⚠️ 实例已被 Koin 创建！")
    }

    fun init() {
        Log.e("LogController", "⚠️ init() 方法被调用，开始监听 DataStore...")

        scope.launch {
            appDataStore.getBoolean(AppDataStore.ENABLE_FILE_LOGGING, true)
                .collectLatest { enabled ->
                    Log.e("LogController", "⚠️ 收到配置变化: enabled = $enabled")
                    updateLoggingState(enabled)
                }
        }
    }

    private fun updateLoggingState(enabled: Boolean) {
        if (enabled) {
            if (fileLoggingTree == null) {
                Log.e("LogController", "✅ 开启文件日志: Planting tree...")
                val tree = FileLoggingTree(context)
                Timber.plant(tree)
                fileLoggingTree = tree
            } else {
                Log.e("LogController", "ℹ️ 文件日志已开启，无需重复操作")
            }
        } else {
            Log.e("LogController", "🛑 关闭文件日志")
            fileLoggingTree?.let { tree ->
                Timber.uproot(tree)
                tree.release()
                fileLoggingTree = null
            }
        }
    }
}