package com.rosan.installer

import android.app.Application
import android.os.Build
import com.kieronquinn.monetcompat.core.MonetCompat
import com.rosan.installer.core.crash.CrashHandler
import com.rosan.installer.core.env.AppConfig
import com.rosan.installer.data.privileged.service.AutoLockService
import com.rosan.installer.di.init.appModules
import com.rosan.installer.domain.engine.model.InstalledAppInfo.Companion.getKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.lsposed.hiddenapibypass.HiddenApiBypass
import timber.log.Timber

class App : Application() {
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

        if (AppConfig.isLogEnabled) Timber.plant(object : Timber.DebugTree() {
            override fun createStackElementTag(element: StackTraceElement): String? {
                return super.createStackElementTag(element)
                    ?.substringBefore('$')
                    ?.take(23)
            }
        })

        startKoin {
            // Koin Android Logger
            androidLogger()
            // Koin Android Context
            androidContext(this@App)
            // use modules
            modules(appModules)
        }

        // Initialize Shizuku module
        val autoLockService: AutoLockService = getKoin().get()
        autoLockService.init()
    }
}