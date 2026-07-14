// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.ui.activity

import android.content.Intent
import android.content.IntentHidden
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstallerHidden
import android.content.pm.PackageManagerHidden
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.rosan.installer.R
import com.rosan.installer.core.app.ActivityContracts.KEY_INSTALLER_ID
import com.rosan.installer.core.bitmask.hasFlag
import com.rosan.installer.core.device.model.Level
import com.rosan.installer.core.env.AppConfig
import com.rosan.installer.data.engine.policy.UnknownSourcePermissionChecker
import com.rosan.installer.domain.device.model.PermissionType
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.device.provider.PermissionChecker
import com.rosan.installer.domain.engine.exception.InstallException
import com.rosan.installer.domain.session.model.ConfirmationRequestType
import com.rosan.installer.domain.session.model.ProgressEntity
import com.rosan.installer.domain.session.repository.InstallerSessionManager
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.ConfigModel
import com.rosan.installer.domain.settings.provider.ThemeStateProvider
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.framework.auth.BiometricAuthBridge
import com.rosan.installer.framework.packageupdate.SelfUpdateRecoveryManager
import com.rosan.installer.ui.common.permission.PermissionRequester
import com.rosan.installer.util.platformLaunchReferrer
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
        private const val KEY_RETURN_INSTALL_RESULT_REQUESTED = "return_install_result_requested"
        private const val KEY_RESULT_ALREADY_FINISHED = "result_already_finished"
    }

    private enum class IncomingInstallPolicy {
        HandleNow,
        Enqueue,
        RejectWhileResultPending
    }

    private val appSettingsRepo by inject<AppSettingsRepository>()
    private val themeStateProvider: ThemeStateProvider by inject()
    private var disableNotificationOnDismiss = false

    private val sessionManager: InstallerSessionManager by inject()
    private var session by mutableStateOf<InstallerSessionRepository?>(null)
    private var job: Job? = null

    private var latestProgress: ProgressEntity = ProgressEntity.Ready
    private var latestInstallResultProgress: ProgressEntity? = null
    private var returnInstallResultRequested = false
    private var resultAlreadyFinished = false

    private val deviceCapabilityProvider: DeviceCapabilityProvider by inject()
    private val permissionChecker: PermissionChecker by inject()
    private val unknownSourcePermissionChecker: UnknownSourcePermissionChecker by inject()
    private val selfUpdateRecoveryManager: SelfUpdateRecoveryManager by inject()
    private lateinit var permissionRequester: PermissionRequester

    // Flag to track if the activity is stopped due to a permission request
    private var isRequestingPermission = false
    private var unknownSourceSettingsLaunchedForFailure = false
    private var pendingUnknownSourcePackageName: String? = null

    private val unknownSourceSettingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val packageName = pendingUnknownSourcePackageName
            pendingUnknownSourcePackageName = null
            isRequestingPermission = false
            unknownSourceSettingsLaunchedForFailure = false

            if (packageName != null && isUnknownSourceAllowed(packageName)) {
                Timber.d("Unknown source permission granted for $packageName. Retrying install.")
                val currentSession = session
                if (currentSession != null &&
                    latestProgress is ProgressEntity.InstallWaitingUnknownSource &&
                    currentSession.multiInstallQueue.isNotEmpty()
                ) {
                    currentSession.installMultiple(currentSession.multiInstallQueue, triggerAuth = false)
                } else {
                    currentSession?.install(false)
                }
            } else {
                Timber.d("Unknown source permission was not granted. Keeping installer in waiting state.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (AppConfig.isDebug && AppConfig.LEVEL == Level.UNSTABLE) logIntentDetails("onCreate", intent)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Timber.d("onCreate. SavedInstanceState is ${if (savedInstanceState == null) "null" else "not null"}")

        lifecycleScope.launch {
            // Re-check after configuration recreation as well: a previous lifecycle coroutine may
            // have been cancelled while waiting for the recovery DataStore transaction.
            if (recoverFromAndroid17SelfUpdate()) return@launch
            initializeInstaller(savedInstanceState)
        }
    }

    private fun initializeInstaller(savedInstanceState: Bundle?) {
        when {
            savedInstanceState != null -> {
                returnInstallResultRequested = savedInstanceState.getBoolean(KEY_RETURN_INSTALL_RESULT_REQUESTED)
                resultAlreadyFinished = savedInstanceState.getBoolean(KEY_RESULT_ALREADY_FINISHED)
            }

            !intent.isSystemConfirmAction() -> {
                updateReturnResultStateFromIntent(intent)
            }
        }

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
            intent?.getStringExtra(KEY_INSTALLER_ID)
        } else {
            savedInstanceState.getString(KEY_INSTALLER_ID)
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

    private suspend fun recoverFromAndroid17SelfUpdate(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN) return false

        // WM Shell rebuilds the task base intent after an Android 17 package update, but it does
        // not preserve the original URI grants or ClipData. Intercept it before creating a session
        // so the incomplete VIEW/SEND intent is treated as completion instead of a new install.
        val platformReferrerPackage = platformLaunchReferrer()
            ?.takeIf { it.scheme == "android-app" }
            ?.host
        val recovered = selfUpdateRecoveryManager.consumeSystemUiRecovery(
            launchedFromUid = launchedFromUidCompat(),
            platformReferrerPackage = platformReferrerPackage,
            intentFlags = intent.flags
        )
        if (!recovered) return false

        Timber.i("Redirecting InstallerActivity restored by SystemUI after self-update.")
        startActivity(SettingsActivity.createSelfUpdateRecoveryIntent(this))
        finishAndRemoveTask()
        return true
    }

    @Suppress("DEPRECATION")
    private fun launchedFromUidCompat(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN) return Process.INVALID_UID
        return launchedFromUid
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
                    PackageInstallerHidden.ACTION_CONFIRM_INSTALL,
                    PackageInstallerHidden.ACTION_CONFIRM_PERMISSIONS,
                    PackageInstallerHidden.ACTION_CONFIRM_PRE_APPROVAL -> resolveConfirm(intent)

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
                session?.close()
                finishWithInstallResultIfRequested()
            }
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val currentId = session?.id
        outState.putString(KEY_INSTALLER_ID, currentId)
        outState.putBoolean(KEY_RETURN_INSTALL_RESULT_REQUESTED, returnInstallResultRequested)
        outState.putBoolean(KEY_RESULT_ALREADY_FINISHED, resultAlreadyFinished)
        Timber.d("onSaveInstanceState: Saving id: $currentId")
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        Timber.d("onNewIntent: Received new intent.")
        if (AppConfig.isDebug && AppConfig.LEVEL == Level.UNSTABLE)
            logIntentDetails("onNewIntent", intent)

        val isSystemConfirmAction = intent.isSystemConfirmAction()

        super.onNewIntent(intent)

        if (isSystemConfirmAction) {
            this.intent = intent
            val sysSessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)

            if (sysSessionId != -1) {
                val requestType = intent.confirmationRequestType()
                // Route the system confirmation request to the currently active session if one exists.
                // This bridges the gap between the suspended commit coroutine and the system UI request.
                val currentSession = this.session
                if (currentSession != null) {
                    Timber.d("onNewIntent: Sending confirm request to ACTIVE session [id=${currentSession.id}]")
                    currentSession.resolveConfirmInstall(this, sysSessionId, requestType)
                } else {
                    // Fallback: Restore or create a new session if this confirmation was triggered
                    // without an active foreground installation process (e.g., silent background trigger).
                    Timber.d("onNewIntent: No active session found. Restoring for system confirm.")
                    restoreInstaller()
                    session?.resolveConfirmInstall(this, sysSessionId, requestType)
                }
            } else {
                Timber.e("onNewIntent: ${intent.action} intent missing EXTRA_SESSION_ID")
                finishWithInstallResultIfRequested()
            }
        } else {
            when (incomingInstallPolicy(intent)) {
                IncomingInstallPolicy.HandleNow -> Unit
                IncomingInstallPolicy.Enqueue -> {
                    sessionManager.enqueueForegroundInstall(Intent(intent).apply { removeExtra(KEY_INSTALLER_ID) })
                    Timber.d("onNewIntent: Deferred foreground install intent.")
                    return
                }

                IncomingInstallPolicy.RejectWhileResultPending -> {
                    Timber.w("onNewIntent: Ignoring foreground install intent while result-bound install is active.")
                    toast(R.string.installer_result_pending)
                    return
                }
            }

            // Proceed with normal intent resolution for standard APK installations.
            this.intent = intent
            updateReturnResultStateFromIntent(intent)
            restoreInstaller()
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
                if (session.closeRequested.value) {
                    Timber.d("onStop: Close already requested. Ignoring background trigger.")
                    return
                }

                // If using session install, we don't hide UI since oems have different package installer impls
                // if (session.config.authorizer == ConfigEntity.Authorizer.None) return

                val currentProgress = latestProgress

                if (currentProgress is ProgressEntity.InstallConfirming) {
                    val details = session.confirmationDetails.value
                    if (details != null) {
                        Timber.d(
                            "onStop: User left install confirmation. Denying system session ${details.sessionId}, " +
                                    "requestType=${details.requestType}, source=${details.sourceAppLabel}"
                        )
                        session.approveConfirmation(details.sessionId, false)
                    } else {
                        Timber.w("onStop: User left install confirmation without details. Closing repository.")
                        session.close()
                    }
                    return
                }

                val isRunning = currentProgress is ProgressEntity.InstallResolving ||
                        currentProgress is ProgressEntity.InstallAnalysing ||
                        currentProgress is ProgressEntity.InstallPreparing ||
                        currentProgress is ProgressEntity.Installing ||
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
            if (savedInstanceState == null) intent?.getStringExtra(KEY_INSTALLER_ID) else savedInstanceState.getString(KEY_INSTALLER_ID)
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
        latestInstallResultProgress = null
        intent?.putExtra(KEY_INSTALLER_ID, session.id)

        Timber.d("restoreInstaller: New installer instance [id=${session.id}] set. Starting collectors.")

        val scope = CoroutineScope(Dispatchers.Main.immediate)
        job = scope.launch {
            launch {
                session.progress.collect { progress ->
                    Timber.d("[id=${session.id}] Activity collected progress: ${progress::class.simpleName}")
                    updateLatestInstallResultProgress(progress)
                    latestProgress = progress
                    handleUnknownSourceInstallPermission(progress)
                    if (shouldReturnInstallResult() && progress.isInstallTerminalResult()) {
                        Timber.d("[id=${session.id}] Result-bound install terminal state detected. Returning result.")
                        sessionManager.clearForegroundInstallQueue()
                        finishWithInstallResultIfRequested(closeSession = true)
                        return@collect
                    }
                    if (progress is ProgressEntity.Finish) {
                        Timber.d("[id=${session.id}] Finish progress detected, finishing activity.")
                        if (shouldReturnInstallResult()) {
                            sessionManager.clearForegroundInstallQueue()
                            finishWithInstallResultIfRequested()
                        } else if (!launchNextPendingInstall() && !this@InstallerActivity.isFinishing) {
                            this@InstallerActivity.finish()
                        }
                    }
                }
            }
            launch {
                session.background.collect { isBackground ->
                    Timber.d("[id=${session.id}] Activity collected background: $isBackground")
                    if (isBackground) {
                        if (launchSelfUnknownSourceSettingsIfNeeded()) {
                            Timber.d("[id=${session.id}] Background request ignored while opening install source settings.")
                            session.background(false)
                            return@collect
                        }
                        if (shouldReturnInstallResult()) {
                            Timber.d("[id=${session.id}] Result-bound install is backgrounded. Waiting for final result.")
                            return@collect
                        }
                        if (launchNextPendingInstall()) {
                            Timber.d("[id=${session.id}] Background mode released foreground slot for deferred install.")
                            return@collect
                        }
                        Timber.d("[id=${session.id}] Background mode detected, finishing activity.")
                        finishWithInstallResultIfRequested()
                    }
                }
            }
        }
    }

    private fun incomingInstallPolicy(newIntent: Intent): IncomingInstallPolicy {
        if (session == null || !latestProgress.isActiveInstallProgress()) {
            return IncomingInstallPolicy.HandleNow
        }

        val currentWantsResult = shouldReturnInstallResult()
        val nextWantsResult = newIntent.getBooleanExtra(Intent.EXTRA_RETURN_RESULT, false)

        return if (currentWantsResult || nextWantsResult) {
            IncomingInstallPolicy.RejectWhileResultPending
        } else {
            IncomingInstallPolicy.Enqueue
        }
    }

    private fun launchNextPendingInstall(): Boolean {
        val nextIntent = sessionManager.takeNextForegroundInstall() ?: return false
        Timber.d("Launching deferred foreground install intent.")
        intent = nextIntent
        updateReturnResultStateFromIntent(nextIntent)
        restoreInstaller()
        checkPermissionsAndStartProcess()
        return true
    }

    private fun ProgressEntity.isActiveInstallProgress(): Boolean =
        this is ProgressEntity.InstallResolving ||
                this is ProgressEntity.InstallResolvedFailed ||
                this is ProgressEntity.InstallResolveSuccess ||
                this is ProgressEntity.InstallPreparing ||
                this is ProgressEntity.InstallAnalysing ||
                this is ProgressEntity.InstallAnalysedFailed ||
                this is ProgressEntity.InstallAnalysedUnsupported ||
                this is ProgressEntity.InstallAnalysedSuccess ||
                this is ProgressEntity.InstallConfirming ||
                this is ProgressEntity.InstallWaitingUnknownSource ||
                this is ProgressEntity.Installing ||
                this is ProgressEntity.InstallCompleted ||
                this is ProgressEntity.InstallFailed ||
                this is ProgressEntity.InstallSuccess ||
                this is ProgressEntity.InstallingModule

    private fun resolveConfirm(intent: Intent) {
        val sessionId = intent.getIntExtra(
            PackageInstaller.EXTRA_SESSION_ID,
            -1
        )

        if (sessionId == -1) {
            Timber.e("${intent.action} intent missing EXTRA_SESSION_ID")
            finishWithInstallResultIfRequested()
            return
        }

        val requestType = intent.confirmationRequestType()
        Timber.d("onCreate: Dispatching resolveConfirmInstall for session $sessionId, type=$requestType")
        session?.resolveConfirmInstall(this, sessionId, requestType)
    }

    private fun handleUnknownSourceInstallPermission(progress: ProgressEntity) {
        if (progress !is ProgressEntity.InstallWaitingUnknownSource) {
            unknownSourceSettingsLaunchedForFailure = false
            return
        }

        if (unknownSourceSettingsLaunchedForFailure) return

        val session = session ?: return

        val packageName = if (session.config.authorizer == Authorizer.None &&
            !deviceCapabilityProvider.isSystemApp &&
            !packageManager.canRequestPackageInstalls()
        ) {
            this.packageName
        } else {
            session.config.initiatorPackageName
        } ?: return

        unknownSourceSettingsLaunchedForFailure = true
        launchUnknownSourceSettings(packageName, session.config)
    }

    fun launchUnknownSourceSettingsForCurrentSession(): Boolean {
        val session = session ?: return false
        val packageName = if (session.config.authorizer == Authorizer.None &&
            !deviceCapabilityProvider.isSystemApp &&
            !packageManager.canRequestPackageInstalls()
        ) {
            this.packageName
        } else {
            session.config.initiatorPackageName
        } ?: return false

        launchUnknownSourceSettings(packageName, session.config)
        return true
    }

    private fun launchSelfUnknownSourceSettingsIfNeeded(): Boolean {
        val session = session ?: return false
        if (session.config.authorizer != Authorizer.None) return false
        if (deviceCapabilityProvider.isSystemApp) return false
        if (packageManager.canRequestPackageInstalls()) return false

        return launchUnknownSourceSettingsForCurrentSession()
    }

    private fun launchUnknownSourceSettings(packageName: String, config: ConfigModel) {
        val pendingPackageName = pendingUnknownSourcePackageName
        if (pendingPackageName != null || isRequestingPermission) {
            Timber.d(
                "Unknown source settings request already active. " +
                        "Ignoring duplicate launch for $packageName, pending=$pendingPackageName"
            )
            return
        }

        lifecycleScope.launch {
            runCatching {
                unknownSourcePermissionChecker.prepareSettingsToggle(packageName, config)
            }.onFailure { error ->
                Timber.w(error, "Failed to prepare unknown source settings for $packageName")
            }

            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                .setData("package:$packageName".toUri())

            pendingUnknownSourcePackageName = packageName
            isRequestingPermission = true
            runCatching {
                unknownSourceSettingsLauncher.launch(intent)
            }.onFailure { error ->
                pendingUnknownSourcePackageName = null
                isRequestingPermission = false
                unknownSourceSettingsLaunchedForFailure = false
                Timber.e(error, "Failed to launch unknown source settings for $packageName")
            }
        }
    }

    private fun isUnknownSourceAllowed(packageName: String) = unknownSourcePermissionChecker.isAllowed(packageName)

    private fun Intent.isSystemConfirmAction(): Boolean =
        action == PackageInstallerHidden.ACTION_CONFIRM_INSTALL ||
                action == PackageInstallerHidden.ACTION_CONFIRM_PERMISSIONS ||
                action == PackageInstallerHidden.ACTION_CONFIRM_PRE_APPROVAL

    private fun Intent.confirmationRequestType(): ConfirmationRequestType =
        when (action) {
            PackageInstallerHidden.ACTION_CONFIRM_PERMISSIONS -> ConfirmationRequestType.PERMISSIONS
            PackageInstallerHidden.ACTION_CONFIRM_PRE_APPROVAL -> ConfirmationRequestType.PRE_APPROVAL
            else -> ConfirmationRequestType.INSTALL
        }

    private fun updateLatestInstallResultProgress(progress: ProgressEntity) {
        if (progress.isInstallTerminalResult()) {
            latestInstallResultProgress = progress
        }
    }

    private fun updateReturnResultStateFromIntent(intent: Intent) {
        returnInstallResultRequested = intent.getBooleanExtra(Intent.EXTRA_RETURN_RESULT, false)
        resultAlreadyFinished = false
    }

    private fun shouldReturnInstallResult(): Boolean =
        returnInstallResultRequested

    private fun finishWithInstallResultIfRequested(closeSession: Boolean = false) {
        if (resultAlreadyFinished) return
        resultAlreadyFinished = true

        if (shouldReturnInstallResult()) {
            val progress = latestInstallResultProgress
            when {
                progress.isInstallSuccess() -> {
                    val result = Intent().putExtra(
                        IntentHidden.EXTRA_INSTALL_RESULT,
                        PackageManagerHidden.INSTALL_SUCCEEDED
                    )
                    setResult(RESULT_OK, result)
                }

                progress.isInstallFailure() -> {
                    val result = Intent().putExtra(
                        IntentHidden.EXTRA_INSTALL_RESULT,
                        installFailureLegacyCode(progress)
                    )
                    setResult(RESULT_FIRST_USER, result)
                }

                else -> setResult(RESULT_CANCELED)
            }
        }

        if (closeSession) {
            session?.close()
        }

        if (!isFinishing) finish()
    }

    private fun ProgressEntity.isInstallTerminalResult(): Boolean =
        this is ProgressEntity.InstallSuccess ||
                this is ProgressEntity.InstallCompleted ||
                this is ProgressEntity.InstallFailed ||
                this is ProgressEntity.InstallAnalysedFailed ||
                this is ProgressEntity.InstallAnalysedUnsupported ||
                this is ProgressEntity.InstallResolvedFailed

    private fun ProgressEntity?.isInstallSuccess(): Boolean =
        this is ProgressEntity.InstallSuccess ||
                (this is ProgressEntity.InstallCompleted && results.isNotEmpty() && results.all { it.success })

    private fun ProgressEntity?.isInstallFailure(): Boolean =
        this is ProgressEntity.InstallFailed ||
                this is ProgressEntity.InstallAnalysedFailed ||
                this is ProgressEntity.InstallAnalysedUnsupported ||
                this is ProgressEntity.InstallResolvedFailed ||
                (this is ProgressEntity.InstallCompleted && !results.all { it.success })

    private fun installFailureLegacyCode(progress: ProgressEntity?): Int =
        (session?.error as? InstallException)?.errorType?.legacyCode
            ?: ((progress as? ProgressEntity.InstallCompleted)
                ?.results
                ?.firstNotNullOfOrNull { (it.error as? InstallException)?.errorType?.legacyCode })
            ?: PackageManagerHidden.INSTALL_FAILED_INTERNAL_ERROR

    private fun showContent() {
        setContent {
            val session = session ?: return@setContent
            val background by session.background.collectAsState(false)
            val progress by session.progress.collectAsState(ProgressEntity.Ready)

            if (background || progress is ProgressEntity.Ready || progress is ProgressEntity.InstallResolving || progress is ProgressEntity.Finish)
                return@setContent

            InstallerActivityContent(session = session, themeStateProvider = themeStateProvider)
        }
    }

    private fun logIntentDetails(tag: String, intent: Intent?) {
        if (intent == null) {
            Timber.d("$tag: Intent is null")
            return
        }
        Timber.d("$tag: Action: ${intent.action}")
        Timber.d("$tag: Data: ${intent.dataString}")
        Timber.d("$tag: Type: ${intent.type}")
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
