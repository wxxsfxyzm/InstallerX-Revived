// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.rosan.installer.R
import com.rosan.installer.core.env.AppConfig
import com.rosan.installer.data.session.manager.InstallerSessionManager
import com.rosan.installer.domain.device.model.Level
import com.rosan.installer.domain.device.model.PermissionType
import com.rosan.installer.domain.device.provider.PermissionChecker
import com.rosan.installer.domain.session.model.ProgressEntity
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.domain.settings.model.ThemeState
import com.rosan.installer.domain.settings.provider.ThemeStateProvider
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.ui.common.auth.BiometricAuthBridge
import com.rosan.installer.ui.common.permission.PermissionRequester
import com.rosan.installer.ui.page.main.installer.InstallerPage
import com.rosan.installer.ui.page.miuix.installer.MiuixInstallerPage
import com.rosan.installer.ui.theme.InstallerTheme
import com.rosan.installer.util.hasFlag
import com.rosan.installer.util.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class InstallerActivity : ComponentActivity(), KoinComponent {
    companion object {
        const val KEY_ID = "installer_id"
        private const val ACTION_CONFIRM_INSTALL = "android.content.pm.action.CONFIRM_INSTALL"
        private const val ACTION_CONFIRM_PERMISSIONS = "android.content.pm.action.CONFIRM_PERMISSIONS"
    }

    private val appSettingsRepo by inject<AppSettingsRepository>()
    private val themeStateProvider: ThemeStateProvider by inject()
    private var disableNotificationOnDismiss = false

    private val sessionManager: InstallerSessionManager by inject()
    private var session by mutableStateOf<InstallerSessionRepository?>(null)
    private var job: Job? = null

    private var latestProgress: ProgressEntity = ProgressEntity.Ready

    private val permissionChecker: PermissionChecker by inject()
    private lateinit var permissionRequester: PermissionRequester

    // Flag to track if the activity is stopped due to a permission request
    private var isRequestingPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        if (AppConfig.isDebug && AppConfig.LEVEL == Level.UNSTABLE) logIntentDetails("onNewIntent", intent)
        enableEdgeToEdge()
        // Compat Navigation Bar color for Xiaomi Devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            window.isNavigationBarContrastEnforced = false
        super.onCreate(savedInstanceState)
        Timber.d("onCreate. SavedInstanceState is ${if (savedInstanceState == null) "null" else "not null"}")

        lifecycleScope.launch {
            // Collect disable notification on dismiss state
            appSettingsRepo.getBoolean(BooleanSetting.DialogDisableNotificationOnDismiss).collect {
                disableNotificationOnDismiss = it
            }
        }

        permissionRequester = PermissionRequester(this, permissionChecker)
        // Set up the callback to intercept the settings launch event
        permissionRequester.onBeforeLaunchSettings = {
            Timber.d("Launching settings for permission, preventing session closure in onStop.")
            isRequestingPermission = true
        }

        val originalSessionId = if (savedInstanceState == null) {
            intent?.getStringExtra(KEY_ID)
        } else {
            savedInstanceState.getString(KEY_ID)
        }

        restoreInstaller(savedInstanceState)
        if (originalSessionId == null) {
            Timber.d("onCreate: This is a fresh launch (originalId is null). Starting permission and resolve process.")
            checkPermissionsAndStartProcess()
        } else {
            Timber.d("onCreate: Re-attaching to existing installer ($originalSessionId).")
        }

        showContent()
    }

    private fun checkPermissionsAndStartProcess() {
        if (intent.flags.hasFlag(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)) {
            Timber.d("checkPermissionsAndStartProcess: Launched from history, skipping permission checks.")
            return
        }
        Timber.d("checkPermissionsAndStartProcess: Starting permission check flow.")

        // Call the manager to request permissions and handle the results in the callbacks.
        permissionRequester.requestEssentialPermissions(
            onGranted = {
                Timber.d("All essential permissions are granted.")
                when (intent.action) {
                    ACTION_CONFIRM_INSTALL,
                    ACTION_CONFIRM_PERMISSIONS -> resolveConfirm(intent)

                    else -> {
                        Timber.d("onCreate: Dispatching resolveInstall")
                        session?.resolveInstall(this)
                    }
                }
            },
            onDenied = { reason ->
                // This is called if any permission is denied.
                // The 'reason' enum tells you which one failed.
                when (reason) {
                    PermissionType.NOTIFICATION -> {
                        Timber.w("Notification permission was denied.")
                        this.toast(R.string.enable_notification_hint)
                    }

                    PermissionType.STORAGE -> {
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
        val currentId = session?.id
        outState.putString(KEY_ID, currentId)
        Timber.d("onSaveInstanceState: Saving id: $currentId")
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        Timber.d("onNewIntent: Received new intent.")
        if (AppConfig.isDebug && AppConfig.LEVEL == Level.UNSTABLE)
            logIntentDetails("onNewIntent", intent)
        // Fix for Microsoft Edge
        if (this.session != null && intent.flags.hasFlag(Intent.FLAG_ACTIVITY_NEW_TASK)) {
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
                session?.resolveConfirmInstall(this, sessionId)
            } else {
                Timber.e("onNewIntent: CONFIRM_INSTALL intent missing EXTRA_SESSION_ID")
                finish()
            }
        } else {
            Timber.d("onNewIntent: Dispatching resolveInstall")
            session?.resolveInstall(this)
        }
    }

    override fun onStop() {
        super.onStop()

        if (BiometricAuthBridge.isAuthenticating) {
            Timber.d("onStop: Ignored background trigger due to active biometric authentication.")
            return
        }
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
        if (!isFinishing && !isChangingConfigurations && !isRequestingPermission) {
            session?.let { session ->
                // If using session install, we don't hide UI since oems have different package installer impls
                // if (session.config.authorizer == ConfigEntity.Authorizer.None) return

                val currentProgress = latestProgress

                val isRunning = currentProgress is ProgressEntity.InstallResolving ||
                        currentProgress is ProgressEntity.InstallAnalysing ||
                        currentProgress is ProgressEntity.InstallPreparing ||
                        currentProgress is ProgressEntity.Installing ||
                        currentProgress is ProgressEntity.InstallConfirming ||
                        currentProgress is ProgressEntity.InstallingModule

                // If the task is still running and hasn't finished or errored
                if (isRunning) {
                    Timber.d("onStop: User left activity while running. Triggering background mode.")
                    session.background(true)
                } else { // If the task has finished or errored
                    if (disableNotificationOnDismiss) {
                        Timber.d("onStop: Task finished and disableNotificationOnDismiss is true. Closing.")
                        session.close()
                    } else {
                        Timber.d("onStop: Task finished and disableNotificationOnDismiss is false. Triggering background mode.")
                        session.background(true)
                    }
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
        val sessionId =
            if (savedInstanceState == null) intent?.getStringExtra(KEY_ID) else savedInstanceState.getString(KEY_ID)
        Timber.d("restoreInstaller: Attempting to restore with id: $sessionId")

        if (this.session != null && this.session?.id == sessionId) {
            Timber.d("restoreInstaller: Current installer already matches id $sessionId. Skipping.")
            return
        }

        job?.cancel()
        Timber.d("restoreInstaller: Old job cancelled. Getting new installer instance.")

        val session = sessionManager.getOrCreate(sessionId)
        session.background(false)
        this.session = session
        intent?.putExtra(KEY_ID, session.id)

        Timber.d("restoreInstaller: New installer instance [id=${session.id}] set. Starting collectors.")

        val scope = CoroutineScope(Dispatchers.Main.immediate)
        job = scope.launch {
            launch {
                session.progress.collect { progress ->
                    Timber.d("[id=${session.id}] Activity collected progress: ${progress::class.simpleName}")
                    latestProgress = progress
                    if (progress is ProgressEntity.Finish) {
                        Timber.d("[id=${session.id}] Finish progress detected, finishing activity.")
                        if (!this@InstallerActivity.isFinishing) this@InstallerActivity.finish()
                    }
                }
            }
            launch {
                session.background.collect { isBackground ->
                    Timber.d("[id=${session.id}] Activity collected background: $isBackground")
                    if (isBackground) {
                        Timber.d("[id=${session.id}] Background mode detected, finishing activity.")
                        this@InstallerActivity.finish()
                    }
                }
            }
        }
    }

    private fun resolveConfirm(intent: Intent) {
        val sessionId = intent.getIntExtra(
            PackageInstaller.EXTRA_SESSION_ID,
            -1
        )

        if (sessionId == -1) {
            Timber.e("CONFIRM_INSTALL intent missing EXTRA_SESSION_ID")
            finish()
            return
        }

        Timber.d("onCreate: Dispatching resolveConfirmInstall for session $sessionId")
        session?.resolveConfirmInstall(this, sessionId)
    }


    private fun showContent() {
        setContent {
            val uiState by themeStateProvider.themeStateFlow.collectAsState(initial = ThemeState())
            if (!uiState.isLoaded) return@setContent

            val session = session ?: return@setContent
            val background by session.background.collectAsState(false)
            val progress by session.progress.collectAsState(ProgressEntity.Ready)

            if (background || progress is ProgressEntity.Ready || progress is ProgressEntity.InstallResolving || progress is ProgressEntity.Finish)
                return@setContent

            InstallerTheme(
                isExpressive = uiState.isExpressive,
                useMiuix = uiState.useMiuix,
                themeMode = uiState.themeMode,
                paletteStyle = uiState.paletteStyle,
                colorSpec = uiState.colorSpec,
                useDynamicColor = uiState.useDynamicColor,
                useMiuixMonet = uiState.useMiuixMonet,
                seedColor = uiState.seedColor
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (uiState.useMiuix) {
                        MiuixInstallerPage(session)
                    } else {
                        InstallerPage(session)
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
                val value = @Suppress("DEPRECATION") extras.get(key)
                Timber.d("$tag: Extra: $key = $value")
            }
        }
    }
}
