package com.rosan.installer.ui.activity

import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.rosan.installer.data.installer.model.entity.UnarchiveInfo
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.ui.activity.themestate.ThemeUiState
import com.rosan.installer.ui.activity.themestate.createThemeUiStateFlow
import com.rosan.installer.ui.theme.InstallerTheme
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class UnarchiveActivity : ComponentActivity() {
    companion object {
        // 系统定义的 Action 常量（AOSP 内部常量，我们需要手动定义）
        const val ACTION_UNARCHIVE_DIALOG = "com.android.intent.action.UNARCHIVE_DIALOG"
        const val ACTION_UNARCHIVE_ERROR_DIALOG = "com.android.intent.action.UNARCHIVE_ERROR_DIALOG"

        // 错误弹窗特有的 Extra Keys
        const val EXTRA_REQUIRED_BYTES = "com.android.content.pm.extra.UNARCHIVE_EXTRA_REQUIRED_BYTES"
        const val EXTRA_INSTALLER_TITLE = "com.android.content.pm.extra.UNARCHIVE_INSTALLER_TITLE"
    }

    private val appDataStore: AppDataStore by inject()
    private var uiState by mutableStateOf(ThemeUiState())

    // 依然获取 Repo，用于处理“确认恢复”时的逻辑
    private val installer: InstallerRepo by inject { parametersOf(null) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            window.isNavigationBarContrastEnforced = false

        lifecycleScope.launch {
            createThemeUiStateFlow(appDataStore).collect { newState ->
                uiState = newState
            }
        }
        // 1. 解析 Intent，构建 UI 状态
        val unarchiveUiState = parseIntent(intent)

        if (unarchiveUiState == UnarchiveUiState.Invalid) {
            Timber.e("Invalid intent action or extras: ${intent.action}")
            finish()
            return
        }

        setContent {
            if (!uiState.isLoaded) return@setContent
            InstallerTheme(
                useMiuix = uiState.useMiuix,
                themeMode = uiState.themeMode,
                paletteStyle = uiState.paletteStyle,
                useDynamicColor = uiState.useDynamicColor,
                useMiuixMonet = uiState.useMiuixMonet,
                seedColor = uiState.seedColor
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // 2. 将状态和操作回调传递给 Compose 页面
                    UnarchivePage(
                        state = unarchiveUiState,
                        onConfirmRestore = { info ->
                            // 用户点击“确认恢复” -> 调用系统 API
                            installer.performUnarchive(info)
                            finish()
                        },
                        onErrorAction = { action ->
                            // 用户点击错误弹窗的按钮（如“清理空间”或“继续”）
                            handleErrorAction(action)
                        },
                        onDismiss = {
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun parseIntent(intent: Intent): UnarchiveUiState {
        return when (intent.action) {
            ACTION_UNARCHIVE_DIALOG -> {
                // --- 场景 A：确认弹窗 ---
                Timber.d("ACTION_UNARCHIVE_DIALOG")
                val packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME)
                val intentSender =
                    intent.getParcelableExtra<IntentSender>("android.content.pm.extra.UNARCHIVE_INTENT_SENDER") // 注意这个Key

                if (packageName != null && intentSender != null) {
                    // 简单的封装，这里为了展示 App 名字，你可能需要用 PackageManager 查一下 label
                    // 为了简化，这里先用包名代替 label，或者你在 Compose 里异步加载 Label
                    val info = UnarchiveInfo(
                        packageName = packageName,
                        appLabel = packageName, // 建议：在此处或 UI 层异步获取应用名
                        installerLabel = "InstallerX",
                        intentSender = intentSender
                    )
                    UnarchiveUiState.Confirmation(info)
                } else {
                    UnarchiveUiState.Invalid
                }
            }

            ACTION_UNARCHIVE_ERROR_DIALOG -> {
                // --- 场景 B：错误弹窗 ---
                Timber.d("ACTION_UNARCHIVE_ERROR_DIALOG")
                val status = intent.getIntExtra(PackageInstaller.EXTRA_UNARCHIVE_STATUS, PackageInstaller.STATUS_FAILURE)
                val requiredBytes = intent.getLongExtra(EXTRA_REQUIRED_BYTES, 0L)
                // 有些错误需要用户操作（比如授权），系统会传一个 PendingIntent 过来让我们执行
                val userActionIntent = intent.getParcelableExtra<android.app.PendingIntent>(Intent.EXTRA_INTENT)

                UnarchiveUiState.Error(
                    status = status,
                    requiredBytes = requiredBytes,
                    userActionIntent = userActionIntent
                )
            }

            else -> UnarchiveUiState.Invalid
        }
    }

    private fun handleErrorAction(state: UnarchiveUiState.Error) {
        try {
            when (state.status) {
                PackageInstaller.UNARCHIVAL_ERROR_USER_ACTION_NEEDED -> {
                    // 需要用户授权，启动系统传入的 PendingIntent
                    state.userActionIntent?.intentSender?.let { sender ->
                        startIntentSender(sender, null, 0, 0, 0)
                    }
                }

                PackageInstaller.UNARCHIVAL_ERROR_INSUFFICIENT_STORAGE -> {
                    // 存储空间不足，跳转到清理页面
                    if (state.userActionIntent != null) {
                        startIntentSender(state.userActionIntent.intentSender, null, 0, 0, 0)
                    } else {
                        val i = Intent("android.intent.action.MANAGE_PACKAGE_STORAGE") // 或者 ACTION_INTERNAL_STORAGE_SETTINGS
                        startActivity(i)
                    }
                }

                PackageInstaller.UNARCHIVAL_ERROR_INSTALLER_DISABLED -> {
                    // 安装器被禁用，跳转到应用详情
                    // ... 逻辑同上，根据 Intent 跳转
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to handle error action")
        }
        finish()
    }
}

// 定义 UI 状态模型
sealed class UnarchiveUiState {
    data object Invalid : UnarchiveUiState()

    // 对应 UNARCHIVE_DIALOG
    data class Confirmation(val info: UnarchiveInfo) : UnarchiveUiState()

    // 对应 UNARCHIVE_ERROR_DIALOG
    data class Error(
        val status: Int,
        val requiredBytes: Long,
        val userActionIntent: android.app.PendingIntent?
    ) : UnarchiveUiState()
}