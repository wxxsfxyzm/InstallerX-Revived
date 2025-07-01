package com.rosan.installer.ui.activity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
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
import androidx.core.net.toUri
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
import timber.log.Timber

class InstallerActivity : ComponentActivity(), KoinComponent {
    companion object {
        const val KEY_ID = "installer_id"
    }

    private var installer by mutableStateOf<InstallerRepo?>(null)

    // 使用官方推荐的方式，在 Activity 顶部注册一个权限请求结果的“启动器”
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            // 这是权限请求完成后的回调
            // 检查通知权限是否被授予 (Tiramisu及以上版本)
            val allGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions[Manifest.permission.POST_NOTIFICATIONS] == true
            } else {
                true // 低版本系统没有这个权限，视为已授予
            }

            if (allGranted) {
                Timber.tag("InstallerDebug")
                    .d("Notification permission GRANTED. Proceeding to check storage permission.")
                checkStoragePermissionAndProceed()
            } else {
                Timber.tag("InstallerDebug").d("Native permission DENIED.")
                Toast.makeText(this, R.string.enable_notification_hint, Toast.LENGTH_LONG).show()
                finish()
            }
        }

    private val requestStoragePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // 从设置页面返回后，再次检查权限是否已被授予
            if (Environment.isExternalStorageManager()) {
                Timber.tag("InstallerDebug").d("Storage permission GRANTED. Calling resolve().")
                installer?.resolve(this)
            } else {
                Timber.tag("InstallerDebug").d("Storage permission DENIED.")
                Toast.makeText(this, R.string.enable_storage_permission_hint, Toast.LENGTH_LONG)
                    .show()
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
        checkPermissionsAndStartProcess()
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

    private fun checkPermissionsAndStartProcess() {
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) {
            return
        }
        // 总是先从检查通知权限开始
        checkNotificationPermissionAndProceed()
    }

    private fun checkNotificationPermissionAndProceed() {
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyArray()
        }

        if (permissionsToRequest.isEmpty()) {
            // 无需通知权限，直接去检查存储权限
            checkStoragePermissionAndProceed()
            return
        }

        // 检查是否已有权限
        var allGranted = true
        for (permission in permissionsToRequest) {
            if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                allGranted = false
                break
            }
        }

        if (allGranted) {
            checkStoragePermissionAndProceed()
        } else {
            requestNotificationPermissionLauncher.launch(permissionsToRequest)
        }
    }

    // 5. 新增：检查并请求存储权限的方法
    private fun checkStoragePermissionAndProceed() {
        // MANAGE_EXTERNAL_STORAGE 权限只在 Android 11 (R) 及以上版本需要
        if (Environment.isExternalStorageManager()) {
            // 已有权限，直接开始最终的 resolve 流程
            Timber.tag("InstallerDebug").d("Storage permission already granted. Calling resolve().")
            installer?.resolve(this)
        } else {
            // 没有权限，跳转到系统设置页面
            Timber.tag("InstallerDebug").d("Requesting storage permission by opening settings.")
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = "package:$packageName".toUri()
            requestStoragePermissionLauncher.launch(intent)
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