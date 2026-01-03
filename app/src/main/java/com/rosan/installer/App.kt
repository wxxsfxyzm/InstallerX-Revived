package com.rosan.installer

import android.app.Application
import android.os.Build
import com.kieronquinn.monetcompat.core.MonetCompat
import com.rosan.installer.build.RsConfig
import com.rosan.installer.build.model.entity.Level
import com.rosan.installer.data.recycle.model.impl.AutoLockManager
import com.rosan.installer.di.init.appModules
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

        if (RsConfig.LEVEL == Level.PREVIEW || RsConfig.LEVEL == Level.UNSTABLE || RsConfig.isDebug)
            Timber.plant(Timber.DebugTree())

        startKoin {
            // Koin Android Logger
            androidLogger()
            // Koin Android Context
            androidContext(this@App)
            // use modules
            modules(appModules)
        }

        AutoLockManager.init()
    }
}
