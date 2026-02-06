package com.rosan.installer

import android.app.Application
import android.os.Build
import com.kieronquinn.monetcompat.core.MonetCompat
import com.rosan.installer.build.RsConfig
import com.rosan.installer.data.recycle.model.impl.AutoLockManager
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.di.init.appModules
import com.rosan.installer.util.timber.FileLoggingTree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.lsposed.hiddenapibypass.HiddenApiBypass
import timber.log.Timber

class App : Application() {
    // Lazy inject AppDataStore
    private val appDataStore: AppDataStore by inject()

    // Keep a reference to the current file logging tree to uproot/release it later
    private var fileLoggingTree: FileLoggingTree? = null

    // Application scope for collecting DataStore flows
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        CrashHandler.init()
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            HiddenApiBypass.addHiddenApiExemptions("")

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            MonetCompat.setup(this)
            MonetCompat.enablePaletteCompat()
            MonetCompat.getInstance().updateMonetColors()
        }

        initTimber()

        startKoin {
            // Koin Android Logger
            androidLogger()
            // Koin Android Context
            androidContext(this@App)
            // use modules
            modules(appModules)
        }

        // Initialize Shizuku module
        AutoLockManager.init()
    }

    private fun initTimber() {
        // 1. Console Logging (Logcat)
        // Controlled by build config (e.g., only visible in Debug builds)
        if (RsConfig.isLogEnabled) {
            Timber.plant(Timber.DebugTree())
        }

        // 2. File Logging
        // Controlled dynamically by User Preference (DataStore)
        appScope.launch {
            appDataStore.getBoolean(AppDataStore.ENABLE_FILE_LOGGING, true).collectLatest { enabled ->
                updateFileLoggingState(enabled)
            }
        }
    }

    /**
     * Dynamically adds or removes the FileLoggingTree based on the setting.
     */
    private fun updateFileLoggingState(enabled: Boolean) {
        if (enabled && this.packageName == BuildConfig.APPLICATION_ID) {
            // Enable: Plant if not already planted
            if (fileLoggingTree == null) {
                val tree = FileLoggingTree(this)
                Timber.plant(tree)
                fileLoggingTree = tree
            }
        } else {
            // Disable: Uproot and Release resources if it exists
            fileLoggingTree?.let { tree ->
                Timber.uproot(tree)
                tree.release() // Stop the background thread!
                fileLoggingTree = null
            }
        }
    }
}
