package com.rosan.installer.ui.activity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.rosan.installer.R
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
import org.koin.core.parameter.parametersOf

class InstallerActivity : ComponentActivity(), KoinComponent {
    companion object {
        const val KEY_ID = "installer_id"
    }

    private var installer by mutableStateOf<InstallerRepo?>(null)

    // 1. 使用官方推荐的方式，在 Activity 顶部注册一个权限请求结果的“启动器”
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            // 这是权限请求完成后的回调
            // 检查通知权限是否被授予 (Tiramisu及以上版本)
            val allGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions[Manifest.permission.POST_NOTIFICATIONS] == true
            } else {
                true // 低版本系统没有这个权限，视为已授予
            }

            if (allGranted) {
                Log.d("InstallerDebug", "Native permission GRANTED. Calling resolve().")
                installer?.resolve(this)
            } else {
                Log.d("InstallerDebug", "Native permission DENIED.")
                Toast.makeText(this, R.string.enable_notification_hint, Toast.LENGTH_LONG).show()
                finish()
            }
        }

    // 通过 Koin 注入 InstallerRepo
    // private val installerRepo: InstallerRepo by inject()

    // 增加一个标志位，防止重复请求权限
    private var permissionCheckTriggered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        restoreInstaller(savedInstanceState)
        checkPermissionAndStartProcess()
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
                        /*is ProgressEntity.Ready -> {
                            installer.resolve(this@InstallerActivity)
                        }*/

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

    private fun checkPermissionAndStartProcess() {
        // 仅在 Activity 第一次创建时检查权限
        // 如果是从 onNewIntent 进来，则不重复检查
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) {
            return
        }

        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyArray()
        }

        if (permissionsToRequest.isEmpty()) {
            // 无需请求权限，直接开始流程
            installer?.resolve(this)
            return
        }

        // 使用启动器来发起权限请求，这是最安全的方式
        Log.d("InstallerDebug", "Launching native permission request...")
        requestPermissionLauncher.launch(permissionsToRequest)
    }

    private fun showContent() {
        setContent {
            val installer = installer ?: return@setContent
            val background by installer.background.collectAsState(false)
            val progress by installer.progress.collectAsState(ProgressEntity.Ready)

            // 在 Composable 中获取 Context 和 Activity 的引用
            val context = LocalContext.current
            val activity = (context as? Activity)

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
}