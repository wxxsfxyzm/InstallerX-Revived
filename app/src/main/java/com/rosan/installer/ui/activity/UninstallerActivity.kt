package com.rosan.installer.ui.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.rosan.installer.R
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.ui.activity.themestate.ThemeUiState
import com.rosan.installer.ui.activity.themestate.createThemeUiStateFlow
import com.rosan.installer.ui.page.main.installer.InstallerPage
import com.rosan.installer.ui.page.miuix.installer.MiuixInstallerPage
import com.rosan.installer.ui.theme.InstallerTheme
import com.rosan.installer.ui.util.PermissionDenialReason
import com.rosan.installer.ui.util.PermissionManager
import com.rosan.installer.util.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class UninstallerActivity : ComponentActivity(), KoinComponent {
    companion object {
        private const val KEY_ID = "uninstaller_id"
        private const val EXTRA_PACKAGE_NAME = "package_name"
    }

    private val appDataStore: AppDataStore by inject()
    private var uiState by mutableStateOf(ThemeUiState())

    private var installer: InstallerRepo? = null
    private var job: Job? = null

    private lateinit var permissionManager: PermissionManager

    // Flag to track if the activity is stopped due to a permission request
    private var isRequestingPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        // Compat Navigation Bar color for Xiaomi Devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            window.isNavigationBarContrastEnforced = false
        super.onCreate(savedInstanceState)
        Timber.d("UninstallerActivity onCreate.")

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

        val installerId = savedInstanceState?.getString(KEY_ID)
        installer = get { parametersOf(installerId) }

        // Start the process only if it's a fresh launch, not a configuration change
        if (savedInstanceState == null) {
            var packageName: String?
            // First, try to get it from our custom extra (for internal calls)
            packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)

            // If not found, try to get it from the intent data (for system calls)
            if (packageName.isNullOrBlank()) {
                val action = intent.action
                if (action == Intent.ACTION_UNINSTALL_PACKAGE || action == Intent.ACTION_DELETE) {
                    intent.data?.schemeSpecificPart?.let {
                        packageName = it
                    }
                }
            }

            if (packageName.isNullOrBlank()) {
                Timber.e("UninstallerActivity started without a package name.")
                installer?.close()
                this.finish()
                return
            }

            Timber.d("Target package to uninstall: $packageName")
            // Trigger the uninstall resolution process
            requestPermissionsAndProceed(packageName)
        }

        startCollectors()
        showContent()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val currentId = installer?.id
        outState.putString(KEY_ID, currentId)
        Timber.d("UninstallerActivity onSaveInstanceState: Saving id: $currentId")
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        super.onStop()
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
            installer?.let { repo ->
                Timber.d("onStop: User left UninstallerActivity. Closing repository.")
                repo.close()
            }
        } else if (isRequestingPermission) {
            Timber.d("onStop: Ignored repo closure due to active permission request.")
            isRequestingPermission = false
        }
    }

    override fun onDestroy() {
        job?.cancel()
        job = null
        // Do not call installer.close() here if you want the process to continue in the background
        Timber.d("UninstallerActivity is being destroyed.")
        super.onDestroy()
    }

    private fun requestPermissionsAndProceed(packageName: String) {
        permissionManager.requestEssentialPermissions(
            onGranted = {
                Timber.d("Permissions granted. Proceeding with uninstall for $packageName")
                installer?.resolveUninstall(this@UninstallerActivity, packageName)
            },
            onDenied = { reason ->
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
                finish()
            }
        )
    }

    private fun startCollectors() {
        job?.cancel()
        val scope = CoroutineScope(Dispatchers.Main.immediate)
        job = scope.launch {
            installer?.progress?.collect { progress ->
                Timber.d("[id=${installer?.id}] Activity collected progress: ${progress::class.simpleName}")
                // Finish the activity on final states
                if (progress is ProgressEntity.Finish) {
                    if (!this@UninstallerActivity.isFinishing) this@UninstallerActivity.finish()
                }
            }
        }
    }

    private fun showContent() {
        setContent {
            if (!uiState.isLoaded) return@setContent

            val currentInstaller = installer
            if (currentInstaller == null) {
                // If repo is null, we can't proceed.
                LaunchedEffect(Unit) {
                    finish()
                }
                return@setContent
            }

            InstallerTheme(
                useMiuix = uiState.useMiuix,
                themeMode = uiState.themeMode,
                paletteStyle = uiState.paletteStyle,
                useDynamicColor = uiState.useDynamicColor,
                useMiuixMonet = uiState.useMiuixMonet,
                seedColor = uiState.seedColor
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (uiState.useMiuix) {
                        MiuixInstallerPage(installer = currentInstaller)
                    } else {
                        InstallerPage(installer = currentInstaller)
                    }
                }
            }
        }
    }
}
