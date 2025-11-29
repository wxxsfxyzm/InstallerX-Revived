package com.rosan.installer.ui.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.lifecycle.lifecycleScope
import com.rosan.installer.R
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.ui.activity.themestate.ThemeUiState
import com.rosan.installer.ui.activity.themestate.createThemeUiStateFlow
import com.rosan.installer.ui.page.main.installer.InstallerPage
import com.rosan.installer.ui.page.miuix.installer.MiuixInstallerPage
import com.rosan.installer.ui.theme.InstallerMaterialExpressiveTheme
import com.rosan.installer.ui.theme.InstallerMiuixTheme
import com.rosan.installer.ui.theme.m3color.ThemeMode
import com.rosan.installer.ui.theme.m3color.dynamicColorScheme
import com.rosan.installer.ui.theme.primaryLight
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

    private var installer: InstallerRepo? = null
    private var job: Job? = null

    private lateinit var permissionManager: PermissionManager

    private var uiState by mutableStateOf(ThemeUiState())

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
                finish()
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

            val useDarkTheme = when (uiState.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            val colorRes =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) colorResource(id = android.R.color.system_accent1_500) else primaryLight
            val globalColorScheme = remember(uiState, useDarkTheme) {
                // 1. If A12+ and Dynamic -> Use System Resource
                // 2. Otherwise -> Use uiState.seedColor (which is now either Manual Color OR Wallpaper Color for A11)
                val keyColor =
                    if (uiState.useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) colorRes else uiState.seedColor

                dynamicColorScheme(
                    keyColor = keyColor,
                    isDark = useDarkTheme,
                    style = uiState.paletteStyle
                )
            }

            val activeColorSchemeState = remember { mutableStateOf(globalColorScheme) }

            LaunchedEffect(globalColorScheme) {
                activeColorSchemeState.value = globalColorScheme
            }

            if (uiState.useMiuix) {
                InstallerMiuixTheme(
                    darkTheme = useDarkTheme,
                    themeMode = uiState.themeMode,
                    useMiuixMonet = uiState.useMiuixMonet,
                    seedColor = activeColorSchemeState.value.primary
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        MiuixInstallerPage(
                            installer = currentInstaller,
                            activeColorSchemeState = activeColorSchemeState,
                            globalColorScheme = globalColorScheme,
                            isDarkMode = useDarkTheme,
                            basePaletteStyle = uiState.paletteStyle
                        )
                    }
                }
            } else {
                InstallerMaterialExpressiveTheme(
                    darkTheme = useDarkTheme,
                    colorScheme = activeColorSchemeState.value,
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        InstallerPage(
                            installer = currentInstaller,
                            activeColorSchemeState = activeColorSchemeState,
                            globalColorScheme = globalColorScheme,
                            isDarkMode = useDarkTheme,
                            basePaletteStyle = uiState.paletteStyle
                        )
                    }
                }
            }
        }
    }
}