package com.rosan.installer

import timber.log.Timber
import kotlin.system.exitProcess
import android.os.Process as AndroidProcess

object CrashHandler : Thread.UncaughtExceptionHandler {
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    fun init() {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        // Log the crash to Timber (which pipes it to the file)
        Timber.tag("CRASH").e(e, "Uncaught Exception detected")

        // Give the file logger a moment to drain the buffer to disk
        // This is crucial for async loggers!
        try {
            Thread.sleep(500)
        } catch (_: InterruptedException) {
        }

        // Delegate to default handler or kill process
        defaultHandler?.uncaughtException(t, e) ?: run {
            AndroidProcess.killProcess(AndroidProcess.myPid())
            exitProcess(1)
        }
    }
}