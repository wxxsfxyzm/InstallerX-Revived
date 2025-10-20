package com.rosan.installer

import android.app.Application
import com.rosan.installer.build.Level
import com.rosan.installer.build.RsConfig
import com.rosan.installer.di.init.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber

class App : Application() {
    override fun onCreate() {
        CrashHandler.init()
        super.onCreate()
        if (RsConfig.LEVEL == Level.PREVIEW || RsConfig.LEVEL == Level.UNSTABLE || RsConfig.isDebug) {
            Timber.plant(Timber.DebugTree())
        }
        startKoin {
            // Koin Android Logger
            androidLogger()
            // Koin Android Context
            androidContext(this@App)
            // use modules
            modules(appModules)
        }
    }
}
