package com.rosan.installer.ui.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.rosan.installer.R
import com.rosan.installer.build.Level
import com.rosan.installer.build.RsConfig
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.ui.page.main.installer.InstallerPage
import com.rosan.installer.ui.page.miuix.installer.MiuixInstallerPage
import com.rosan.installer.ui.theme.InstallerMaterialExpressiveTheme
import com.rosan.installer.ui.theme.InstallerMiuixTheme
import com.rosan.installer.ui.util.PermissionDenialReason
import com.rosan.installer.ui.util.PermissionManager
import com.rosan.installer.util.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class InstallerActivity : ComponentActivity(), KoinComponent {
    companion object {
        const val KEY_ID = "installer_id"
    }

    private val appDataStore: AppDataStore by inject()

    private var installer by mutableStateOf<InstallerRepo?>(null)
    private var job: Job? = null
    private lateinit var permissionManager: PermissionManager

    private enum class InstallerTheme {
        MATERIAL,
        MIUIX
    }

    // Define a data class for the UI state
    private data class InstallerUiState(
        val theme: InstallerTheme = InstallerTheme.MATERIAL, // Default theme
        val isThemeLoaded: Boolean = false // Flag to check if loading is complete
    )

    private var uiState by mutableStateOf(InstallerUiState())

    override fun onCreate(savedInstanceState: Bundle?) {
        if (RsConfig.isDebug && RsConfig.LEVEL == Level.UNSTABLE)
            logIntentDetails("onNewIntent", intent)
        enableEdgeToEdge()
        // Compat Navigation Bar color for Xiaomi Devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            window.isNavigationBarContrastEnforced = false
        super.onCreate(savedInstanceState)
        Timber.d("onCreate. SavedInstanceState is ${if (savedInstanceState == null) "null" else "not null"}")

        lifecycleScope.launch {
            val useMiuix = appDataStore.getBoolean(AppDataStore.UI_USE_MIUIX, false).first()
            uiState = uiState.copy(
                theme = if (useMiuix) InstallerTheme.MIUIX else InstallerTheme.MATERIAL,
                isThemeLoaded = true
            )
        }

        permissionManager = PermissionManager(this)
        restoreInstaller(savedInstanceState)
        val installerId =
            if (savedInstanceState == null) intent?.getStringExtra(KEY_ID) else savedInstanceState.getString(KEY_ID)

        if (installerId == null) {
            Timber.d("onCreate: This is a fresh launch for a new task. Starting permission and resolve process.")
            // Only start the process for a completely new task.
            checkPermissionsAndStartProcess()
        } else {
            Timber.d("onCreate: Re-attaching to existing installer ($installerId). Skipping resolve process.")
        }
        showContent()
    }

    private fun checkPermissionsAndStartProcess() {
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) {
            Timber.d("checkPermissionsAndStartProcess: Launched from history, skipping permission checks.")
            return
        }
        Timber.d("checkPermissionsAndStartProcess: Starting permission check flow.")

        // Call the manager to request permissions and handle the results in the callbacks.
        permissionManager.requestEssentialPermissions(
            onGranted = {
                // This is called when all permissions are successfully granted.
                Timber.d("All essential permissions are granted.")
                installer?.resolveInstall(this)
            },
            onDenied = { reason ->
                // This is called if any permission is denied.
                // The 'reason' enum tells you which one failed.
                when (reason) {
                    PermissionDenialReason.NOTIFICATION -> {
                        Timber.w("Notification permission was denied.")
                        this.toast(R.string.enable_notification_hint)
                    }

                    PermissionDenialReason.STORAGE -> {
                        Timber.w("Storage permission was denied.")
                        this.toast(R.string.enable_storage_permission_hint)
                    }
                }
                // Finish the activity if permissions are not granted.
                finish()
            }
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val currentId = installer?.id
        outState.putString(KEY_ID, currentId)
        Timber.d("onSaveInstanceState: Saving id: $currentId")
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        Timber.d("onNewIntent: Received new intent.")
        if (RsConfig.isDebug && RsConfig.LEVEL == Level.UNSTABLE)
            logIntentDetails("onNewIntent", intent)
        // Fix for Microsoft Edge
        if (this.installer != null) {
            Timber.w("onNewIntent was called, but an installer instance already exists. Ignoring re-initialization.")
            super.onNewIntent(intent) // Call super, but do not proceed further.
            return
        }
        if (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK == 0)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        this.intent = intent
        super.onNewIntent(intent)
        restoreInstaller()
    }

    override fun onDestroy() {
        job?.cancel()
        job = null
        Timber.d("onDestroy: Activity is being destroyed. Job cancelled.")
        super.onDestroy()
    }

    private fun restoreInstaller(savedInstanceState: Bundle? = null) {
        val installerId =
            if (savedInstanceState == null) intent?.getStringExtra(KEY_ID) else savedInstanceState.getString(KEY_ID)
        Timber.d("restoreInstaller: Attempting to restore with id: $installerId")

        if (this.installer != null && this.installer?.id == installerId) {
            Timber.d("restoreInstaller: Current installer already matches id $installerId. Skipping.")
            return
        }

        job?.cancel()
        Timber.d("restoreInstaller: Old job cancelled. Getting new installer instance.")

        val installer: InstallerRepo = get { parametersOf(installerId) }
        installer.background(false)
        this.installer = installer
        Timber.d("restoreInstaller: New installer instance [id=${installer.id}] set. Starting collectors.")

        val scope = CoroutineScope(Dispatchers.Main.immediate)
        job = scope.launch {
            launch {
                installer.progress.collect { progress ->
                    Timber.d("[id=${installer.id}] Activity collected progress: ${progress::class.simpleName}")
                    if (progress is ProgressEntity.Finish) {
                        Timber.d("[id=${installer.id}] Finish progress detected, finishing activity.")
                        if (!this@InstallerActivity.isFinishing) this@InstallerActivity.finish()
                    }
                }
            }
            launch {
                installer.background.collect { isBackground ->
                    Timber.d("[id=${installer.id}] Activity collected background: $isBackground")
                    if (isBackground) {
                        Timber.d("[id=${installer.id}] Background mode detected, finishing activity.")
                        this@InstallerActivity.finish()
                    }
                }
            }
        }
    }

    private fun showContent() {
        setContent {
            if (!uiState.isThemeLoaded) return@setContent

            val installer = installer ?: return@setContent
            val background by installer.background.collectAsState(false)
            val progress by installer.progress.collectAsState(ProgressEntity.Ready)

            if (background || progress is ProgressEntity.Ready || progress is ProgressEntity.InstallResolving || progress is ProgressEntity.Finish)
            // Return@setContent to show nothing, logs will explain why.
                return@setContent

            when (uiState.theme) {
                InstallerTheme.MATERIAL -> {
                    InstallerMaterialExpressiveTheme {
                        Box(modifier = Modifier.fillMaxSize()) {
                            InstallerPage(installer)
                        }
                    }
                }

                InstallerTheme.MIUIX -> {
                    InstallerMiuixTheme {
                        Box(modifier = Modifier.fillMaxSize()) {
                            MiuixInstallerPage(installer)
                        }
                    }
                }
            }
        }
    }

    private fun logIntentDetails(tag: String, intent: Intent?) {
        if (intent == null) {
            Timber.tag(tag).d("Intent is null")
            return
        }
        val flags = intent.flags
        val hexFlags = String.format("0x%08X", flags)

        Timber.tag(tag).d("---------- Intent Details Start ----------")
        Timber.tag(tag).d("Full Intent: $intent")
        Timber.tag(tag).d("Action: ${intent.action}")
        Timber.tag(tag).d("Data: ${intent.dataString}")
        Timber.tag(tag).d("Type: ${intent.type}")
        Timber.tag(tag).d("Categories: ${intent.categories?.joinToString(", ")}")
        Timber.tag(tag).d("Flags (Decimal): $flags")
        Timber.tag(tag).d("Flags (Hex): $hexFlags")
        Timber.tag(tag).d("Component: ${intent.component}")
        Timber.tag(tag).d("Extras: ${intent.extras?.keySet()?.joinToString(", ")}")
        Timber.tag(tag).d("---------- Intent Details End ----------")
    }
}