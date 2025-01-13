package com.rosan.installer

import java.lang.Thread.UncaughtExceptionHandler

class CrashHandler : UncaughtExceptionHandler {
    companion object {
        private var isInit = false

        private var defaultHandler: UncaughtExceptionHandler? = null

        fun init() {
            synchronized(this) {
                if (isInit) return
                isInit = true
                defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
                Thread.setDefaultUncaughtExceptionHandler(CrashHandler())
            }
        }
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        defaultHandler?.uncaughtException(t, e)
    }
}