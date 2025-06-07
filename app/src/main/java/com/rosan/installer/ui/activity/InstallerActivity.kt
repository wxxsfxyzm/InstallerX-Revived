package com.rosan.installer.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rosan.installer.R
import com.rosan.installer.data.installer.model.entity.InstallerEvent
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.installer.InstallerPage
import com.rosan.installer.ui.theme.InstallerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class InstallerActivity : ComponentActivity(), KoinComponent {
    companion object {
        const val KEY_ID = "installer_id"
    }

    private var installer by mutableStateOf<InstallerRepo?>(null)

    // 通过 Koin 或其他方式注入 InstallerRepo
    private val installerRepo: InstallerRepo by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        restoreInstaller(savedInstanceState)
        observeInstallerEvents()
        showContent()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_ID, installer?.id)
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        this.intent = intent
        super.onNewIntent(intent)
        restoreInstaller()
    }

    private var job: Job? = null

    override fun finish() {
        super.finish()
    }

    override fun onDestroy() {
        job?.cancel()
        job = null
        super.onDestroy()
    }

    private fun restoreInstaller(savedInstanceState: Bundle? = null) {
        val installerId = if (savedInstanceState == null) intent?.getStringExtra(KEY_ID)
        else savedInstanceState.getString(KEY_ID)

        // 关键检查：如果当前已经有一个 installer，并且它的 ID 和将要恢复的 ID 相同，
        // 那么就什么都不做，直接返回，避免重复创建。
        if (this.installer != null && this.installer?.id == installerId) {
            android.util.Log.d(
                "InstallerDebug",
                "Installer already exists with the same ID. Skipping restore."
            )
            return
        }

        job?.cancel() // 只有在确定需要恢复新实例时才取消旧的 job

        val installer: InstallerRepo = get {
            parametersOf(installerId)
        }
        installer.background(false)
        this.installer = installer
        val scope = CoroutineScope(Dispatchers.IO)
        job = scope.launch {
            launch {
                installer.progress.collect { progress ->
                    when (progress) {
                        is ProgressEntity.Ready -> {
                            // 在调用 resolve 前加日志
                            android.util.Log.d(
                                "InstallerDebug",
                                "Installer(${installer.id}) is Ready. Calling resolve()."
                            )
                            installer.resolve(this@InstallerActivity)
                        }

                        is ProgressEntity.Finish -> {
                            val activity = this@InstallerActivity
                            if (!activity.isFinishing) activity.finish()
                        }

                        else -> {}
                    }
                }
            }
            launch {
                installer.background.collect {
                    if (it) this@InstallerActivity.finish()
                }
            }
        }
    }

    private fun showContent() {
        setContent {
            val installer = installer ?: return@setContent
            val background by installer.background.collectAsState(false)
            val progress by installer.progress.collectAsState(ProgressEntity.Ready)

            // 在 Composable 中获取 Context 和 Activity 的引用
            val context = LocalContext.current
            val activity = (context as? Activity)

            // 使用 LaunchedEffect 来监听一次性事件流
            LaunchedEffect(installer) { // 使用 installer 作为 key，如果 installer 实例变化，则重启
                installer.events.collect { event ->
                    when (event) {
                        InstallerEvent.NOTIFICATION_PERMISSION_MISSING -> {
                            Toast.makeText(
                                context,
                                R.string.enable_notification_hint, // 确保你有这个字符串资源
                                Toast.LENGTH_SHORT
                            ).show()
                            // 关闭 Activity
                            activity?.finish()
                        }
                    }
                }
            }

            if (
                background ||
                progress is ProgressEntity.Ready ||
                progress is ProgressEntity.Resolving ||
                progress is ProgressEntity.Finish
            ) return@setContent
            InstallerTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    InstallerPage(installer)
                }
            }
        }
    }

    private fun observeInstallerEvents() {
        // 使用 lifecycleScope 来确保协程在 Activity 销毁时自动取消
        lifecycleScope.launch {
            // repeatOnLifecycle 确保只有在 Activity 处于活跃状态时才收集事件
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                installerRepo.events.collect { event ->
                    when (event) {
                        InstallerEvent.NOTIFICATION_PERMISSION_MISSING -> {
                            // 在主线程上显示 Toast
                            Toast.makeText(
                                applicationContext,
                                R.string.enable_notification_hint, // 使用你的字符串资源
                                Toast.LENGTH_LONG
                            ).show()
                            // 结束 Activity
                            finish()
                        }
                        // 可以处理其他事件
                    }
                }
            }
        }
    }
}