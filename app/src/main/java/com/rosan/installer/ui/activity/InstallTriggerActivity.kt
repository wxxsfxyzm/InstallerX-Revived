package com.rosan.installer.ui.activity

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.rosan.installer.data.installer.repo.InstallerRepo
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

/**
 * 这是一个透明的代理Activity，其唯一作用是：
 * 1. 被通知栏的PendingIntent启动，将App进程提升到前台优先级。
 * 2. 立即触发安装指令。
 * 3. 立即自我销毁。
 */
class InstallTriggerActivity : ComponentActivity(), KoinComponent {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 从启动它的 Intent 中获取 installerId
        val installerId = intent?.getStringExtra(InstallerActivity.KEY_ID)

        if (installerId == null) {
            Log.e("InstallTriggerActivity", "Installer ID is null, cannot trigger install.")
            finish() // 如果没有ID，直接销毁
            return
        }

        // 2. 使用 Koin 获取对应的 InstallerRepo 实例
        val installer: InstallerRepo = get { parametersOf(installerId) }

        // 3. 触发安装指令
        Log.d("InstallTriggerActivity", "Triggering install for ID: $installerId")
        installer.install()

        // 4. 立即销毁自己，用户不会看到任何界面
        finish()
    }
}