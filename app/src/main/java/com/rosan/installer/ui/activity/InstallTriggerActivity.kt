package com.rosan.installer.ui.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.repo.InstallerRepo
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import timber.log.Timber

/**
 * 这是一个透明的代理Activity，其唯一作用是：
 * 被通知栏的PendingIntent启动，将App进程提升到前台优先级。
 * 立即触发安装指令。
 * 保持在前台，直到安装完成或失败。
 * 如果系统未响应，兜底超时退出。
 * */
class InstallTriggerActivity : ComponentActivity(), KoinComponent {
    companion object {
        private const val HYPEROS_USB_INSTALL_TIMEOUT_MS = 10000L
        private const val INSTALL_WATCHDOG_TIMEOUT_MS = 10000L
    }

    private var finishJob: Job? = null
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {
        if (finishJob?.isActive == true) {
            Timber.tag("InstallTriggerActivity").w("Force-finishing due to timeout.")
            finishJob?.cancel()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val installerId = intent?.getStringExtra(InstallerActivity.KEY_ID)

        if (installerId == null) {
            Timber.tag("InstallTriggerActivity").e("Installer ID is null, cannot trigger install.")
            finish()
            return
        }

        val installer: InstallerRepo = get { parametersOf(installerId) }

        Timber.tag("InstallTriggerActivity").d("Triggering install for ID: $installerId")
        installer.install()

        // 启动监听器，等待安装完成或失败再退出
        finishJob = lifecycleScope.launch {
            installer.progress.collect { progress ->
                when (progress) {
                    is ProgressEntity.InstallSuccess,
                    is ProgressEntity.InstallFailed,
                    is ProgressEntity.AnalysedUnsupported -> {
                        Timber.tag("InstallTriggerActivity").d("Install ended: $progress")
                        finish()
                        return@collect
                    }

                    else -> {
                        // 不处理其他状态
                    }
                }
            }
        }

        timeoutHandler.postDelayed(timeoutRunnable, HYPEROS_USB_INSTALL_TIMEOUT_MS + INSTALL_WATCHDOG_TIMEOUT_MS)
    }

    override fun onDestroy() {
        super.onDestroy()
        timeoutHandler.removeCallbacks(timeoutRunnable)
        if (finishJob?.isActive == true) {
            finishJob?.cancel()
        }
        Timber.tag("InstallTriggerActivity").d("onDestroy, timeout callback removed.")
    }
}