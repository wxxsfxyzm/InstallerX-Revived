package com.rosan.installer.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.rosan.installer.data.installer.model.impl.installer.BroadcastHandler
import com.rosan.installer.data.installer.repo.InstallerRepo
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import timber.log.Timber

/**
 * 这是一个透明的代理Activity，其唯一作用是：
 * 1. 被通知栏的PendingIntent启动，将App进程提升到前台优先级。
 * 2. 立即触发安装指令。
 * 3. 立即自我销毁。
 */
class InstallTriggerActivity : ComponentActivity(), KoinComponent {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 核心区别：检查 Intent 中是否包含“动作名称”
        val keyName = intent?.getStringExtra(BroadcastHandler.KEY_NAME) // 建议将 KEY_NAME 设为 public
        val installerId = intent?.getStringExtra(BroadcastHandler.KEY_ID) // 两个逻辑都需要 ID

        if (installerId == null) {
            Timber.tag("InstallTriggerActivity").e("Installer ID is null, aborting.")
            finish()
            return
        }

        if (keyName != null) {
            // --- 新增的“蹦床/代理”逻辑 ---
            // 如果 keyName 存在，说明这是来自通知的动作代理请求

            Timber.tag("InstallTriggerActivity")
                .d("Proxying action '$keyName' for ID: $installerId")

            val installer: InstallerRepo = get { parametersOf(installerId) }
            installer.close()

        } else {
            // --- 保留的“触发安装”原始逻辑 ---
            // 如果 keyName 不存在，说明这是原始的触发安装请求

            Timber.tag("InstallTriggerActivity").d("Triggering install for ID: $installerId")

            // 1. 使用 Koin 获取对应的 InstallerRepo 实例
            // 注意：如果你的 Koin 定义不需要参数，请使用 val installer: InstallerRepo = get()
            val installer: InstallerRepo = get { parametersOf(installerId) }

            // 2. 触发安装指令
            installer.install()
        }

        // 无论执行哪个逻辑，最后都立即销毁自己
        finish()
    }
}