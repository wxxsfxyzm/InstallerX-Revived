package com.rosan.installer.ui.activity

import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.rosan.installer.R
import com.rosan.installer.build.RsConfig
import com.rosan.installer.build.model.entity.Level
import com.rosan.installer.build.model.impl.DeviceCapabilityChecker
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.ui.activity.themestate.ThemeUiState
import com.rosan.installer.ui.activity.themestate.createThemeUiStateFlow
import com.rosan.installer.ui.common.LocalMiPackageInstallerPresent
import com.rosan.installer.ui.page.main.installer.InstallerPage
import com.rosan.installer.ui.page.miuix.installer.MiuixInstallerPage
import com.rosan.installer.ui.theme.InstallerTheme
import com.rosan.installer.ui.util.PermissionDenialReason
import com.rosan.installer.ui.util.PermissionManager
import com.rosan.installer.util.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class InstallerActivity : ComponentActivity(), KoinComponent {
    companion object {
        const val KEY_ID = "installer_id"
        private const val ACTION_CONFIRM_INSTALL = "android.content.pm.action.CONFIRM_INSTALL"
        private const val ACTION_CONFIRM_PERMISSIONS = "android.content.pm.action.CONFIRM_PERMISSIONS"
    }

    private val appDataStore: AppDataStore by inject()
    private var uiState by mutableStateOf(ThemeUiState())

    private var installer by mutableStateOf<InstallerRepo?>(null)
    private var job: Job? = null

    private lateinit var permissionManager: PermissionManager

    // Flag to track if the activity is stopped due to a permission request
    private var isRequestingPermission = false

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
            createThemeUiStateFlow(appDataStore).collect { newState ->
                uiState = newState
            }
        }

        permissionManager = PermissionManager(this)
        // Setup the callback to intercept the settings launch event
        permissionManager.onBeforeLaunchSettings = {
            Timber.d("Launching settings for permission, preventing repo closure in onStop.")
            isRequestingPermission = true
        }

        Timber.d("onCreate: Handling all flows via restoreInstaller and action dispatch.")
        restoreInstaller(savedInstanceState)

        val installerId =
            if (savedInstanceState == null) intent?.getStringExtra(KEY_ID) else savedInstanceState.getString(KEY_ID)

        if (installerId == null) {
            Timber.d("onCreate: This is a fresh launch. Starting permission and resolve process.")
            checkPermissionsAndStartProcess()
        } else {
            Timber.d("onCreate: Re-attaching to existing installer ($installerId).")
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
                Timber.d("All essential permissions are granted.")
                if (intent.action == ACTION_CONFIRM_INSTALL || intent.action == ACTION_CONFIRM_PERMISSIONS) {
                    val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
                    if (sessionId != -1) {
                        Timber.d("onCreate: Dispatching resolveConfirmInstall for session $sessionId")
                        installer?.resolveConfirmInstall(this, sessionId)
                    } else {
                        Timber.e("CONFIRM_INSTALL intent missing EXTRA_SESSION_ID")
                        finish()
                    }
                } else {
                    Timber.d("onCreate: Dispatching resolveInstall")
                    installer?.resolveInstall(this)
                }
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
        if (this.installer != null && (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)) {
            Timber.w("onNewIntent was called with NEW_TASK, but an installer instance already exists. Ignoring re-initialization.")
            super.onNewIntent(intent)
            return
        }

        this.intent = intent
        super.onNewIntent(intent)
        restoreInstaller()

        if (intent.action == ACTION_CONFIRM_INSTALL || intent.action == ACTION_CONFIRM_PERMISSIONS) {
            val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
            if (sessionId != -1) {
                Timber.d("onNewIntent: Dispatching resolveConfirmInstall for session $sessionId")
                installer?.resolveConfirmInstall(this, sessionId)
            } else {
                Timber.e("onNewIntent: CONFIRM_INSTALL intent missing EXTRA_SESSION_ID")
                finish()
            }
        } else {
            Timber.d("onNewIntent: Dispatching resolveInstall")
            installer?.resolveInstall(this)
        }
    }

    override fun onStop() {
        super.onStop()

        if (BiometricsAuthenticationActivity.onActivityReady != null) return
        // Check if the screen is currently on.
        // If the screen is off, onStop is triggered by locking the device.
        // We explicitly want to ignore this case.
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val isScreenOn = powerManager.isInteractive

        if (!isScreenOn) {
            // The screen is turned off (locked), do nothing.
            Timber.d("onStop: Screen is turned off. Ignoring.")
            return
        }
        // Only strictly interpret as user leaving when not finishing and not changing configurations (e.g., rotation)
        // TODO add a flag to handle Session Install Confirmation
        if (!isFinishing && !isChangingConfigurations && !isRequestingPermission) {
            installer?.let { repo ->
                // If using session install, we don't hide UI since oems have different package installer impls
                // if (repo.config.authorizer == ConfigEntity.Authorizer.None) return
                // Since the interface defines 'progress' as Flow, we need to cast it to SharedFlow
                // to access the replayCache. We know InstallerRepoImpl uses MutableStateFlow.
                val sharedFlow = repo.progress as? SharedFlow<*>
                val currentProgress = sharedFlow?.replayCache?.lastOrNull()

                // If the task is still running and hasn't finished or errored
                if (currentProgress !is ProgressEntity.Finish &&
                    currentProgress !is ProgressEntity.Error
                ) {
                    Timber.d("onStop: User left activity. Triggering background mode.")

                    // Trigger background mode
                    repo.background(true)
                }
            }
        } else if (isRequestingPermission) {
            Timber.d("onStop: Ignored background trigger due to active permission request.")
            isRequestingPermission = false
        }
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
            if (!uiState.isLoaded) return@setContent

            val installer = installer ?: return@setContent
            val background by installer.background.collectAsState(false)
            val progress by installer.progress.collectAsState(ProgressEntity.Ready)
            val capabilityChecker = koinInject<DeviceCapabilityChecker>()

            if (background || progress is ProgressEntity.Ready || progress is ProgressEntity.InstallResolving || progress is ProgressEntity.Finish)
                return@setContent

            InstallerTheme(
                useMiuix = uiState.useMiuix,
                themeMode = uiState.themeMode,
                paletteStyle = uiState.paletteStyle,
                useDynamicColor = uiState.useDynamicColor,
                useMiuixMonet = uiState.useMiuixMonet,
                seedColor = uiState.seedColor
            ) {
                CompositionLocalProvider(
                    LocalMiPackageInstallerPresent provides capabilityChecker.hasMiPackageInstaller
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (uiState.useMiuix) {
                            MiuixInstallerPage(installer = installer)
                        } else {
                            InstallerPage(installer = installer)
                        }
                    }
                }
            }
        }
    }

    private fun logIntentDetails(tag: String, intent: Intent?) {
        if (intent == null) {
            Timber.d("$tag: Intent is null")
            return
        }
        Timber.d("$tag: Action: ${intent.action}")
        Timber.d("$tag: Data: ${intent.dataString}")
        Timber.d("$tag: Flags: ${Integer.toHexString(intent.flags)}")
        intent.extras?.let { extras ->
            for (key in extras.keySet()) {
                // Use get(key) instead of getString(key) to avoid ClassCastException
                // on non-string values (e.g. SESSION_ID is an Integer).
                // Although get(key) is deprecated in newer APIs, it is the only way
                // to generically log unknown types without suppressions or reflection.
                val value = extras.get(key)
                Timber.d("$tag: Extra: $key = $value")
            }
        }
    }
}
