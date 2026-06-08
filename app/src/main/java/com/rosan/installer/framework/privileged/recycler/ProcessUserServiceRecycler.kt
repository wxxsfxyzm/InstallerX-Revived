// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.framework.privileged.recycler

import android.content.ComponentName
import android.content.Context
import android.os.Binder
import android.os.IBinder
import android.os.Process
import android.os.RemoteException
import android.system.Os
import androidx.annotation.Keep
import com.rosan.app_process.AppProcess
import com.rosan.installer.IAppProcessService
import com.rosan.installer.IPrivilegedService
import com.rosan.installer.core.env.AppConfig
import com.rosan.installer.di.init.processModules
import com.rosan.installer.framework.privileged.lifecycle.Recyclable
import com.rosan.installer.framework.privileged.lifecycle.Recycler
import com.rosan.installer.framework.privileged.lifecycle.RecyclerManager
import com.rosan.installer.framework.privileged.lifecycle.UserService
import com.rosan.installer.framework.privileged.runtime.DefaultPrivilegedService
import com.rosan.installer.framework.privileged.util.SystemUidEnvironment
import com.rosan.installer.framework.privileged.util.UserServiceUidMode
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import timber.log.Timber
import kotlin.system.exitProcess

class ProcessUserServiceRecycler(
    private val shell: String,
    private val context: Context,
    private val appProcessRecyclerManager: RecyclerManager<String, AppProcessRecycler>,
    private val serviceClass: Class<out IAppProcessService> = AppProcessService::class.java
) : Recycler<ProcessUserServiceRecycler.UserServiceProxy>() {

    class UserServiceProxy(
        val service: IAppProcessService,
        private val appProcessHandle: Recyclable<AppProcess>,
        private val binder: IBinder,
        private val deathRecipient: IBinder.DeathRecipient
    ) : UserService {
        override val privileged: IPrivilegedService = service.privilegedService

        override fun close() {
            // Unlink death recipient to prevent false alarm during normal close
            try {
                binder.unlinkToDeath(deathRecipient, 0)
            } catch (_: Exception) {
                // Ignore if already dead or unlinked
            }

            // Quit the remote service gracefully
            runCatching { service.quit() }
            appProcessHandle.recycle()
        }
    }

    abstract class BaseAppProcessService(
        context: Context,
        uidMode: UserServiceUidMode
    ) : IAppProcessService.Stub() {
        init {
            if (uidMode == UserServiceUidMode.SystemIfRoot) {
                SystemUidEnvironment.switchRootToSystemIfNeeded(TAG)
            }
            if (AppConfig.isDebug && Timber.treeCount == 0) Timber.plant(Timber.DebugTree())
            if (GlobalContext.getOrNull() == null) {
                startKoin {
                    modules(processModules)
                    androidContext(context)
                }
            }
        }

        private val privileged = DefaultPrivilegedService.userService()

        private companion object {
            private const val TAG = "ProcessUserService"
        }

        override fun quit() {
            try {
                // Kill parent shell to ensure clean exit (su/sh process)
                val ppid = Os.getppid()
                Timber.i("Quitting... Killing parent shell (PID: $ppid)")
                Process.killProcess(ppid)
            } catch (e: Exception) {
                Timber.e(e, "Failed to kill parent process")
            } finally {
                exitProcess(0)
            }
        }

        override fun getPrivilegedService(): IPrivilegedService = privileged

        override fun registerDeathToken(token: IBinder?) {
            // This acts as a secondary safety mechanism via Binder
            try {
                token?.linkToDeath({
                    Timber.w("Client died (Binder notification). Quitting...")
                    quit()
                }, 0)
            } catch (e: RemoteException) {
                Timber.e(e, "Client already dead")
                quit()
            }
        }
    }

    class AppProcessService @Keep constructor(context: Context) : BaseAppProcessService(
        context = context,
        uidMode = UserServiceUidMode.Default
    )

    class SystemUidAppProcessService @Keep constructor(context: Context) : BaseAppProcessService(
        context = context,
        uidMode = UserServiceUidMode.SystemIfRoot
    )

    override fun onMake(): UserServiceProxy {
        val appProcessRecycler = appProcessRecyclerManager.get(shell)
        val appProcessHandle = appProcessRecycler.make()

        val maxRetries = 5
        val initialDelay = 100L
        var currentBinder: IBinder? = null

        // Retry logic for obtaining the binder
        var attempt = 0
        while (attempt < maxRetries) {
            try {
                currentBinder = appProcessHandle.entity.isolatedServiceBinder(
                    ComponentName(context, serviceClass)
                )

                if (currentBinder != null) {
                    if (currentBinder.isBinderAlive) {
                        break
                    } else {
                        Timber.w("Attempt ${attempt + 1}: Binder retrieved but dead.")
                    }
                } else {
                    Timber.w("Attempt ${attempt + 1}: isolatedServiceBinder returned null.")
                }
            } catch (e: Exception) {
                Timber.w(e, "Attempt ${attempt + 1}: Exception during bind.")
            }

            attempt++
            if (attempt < maxRetries) {
                Thread.sleep(initialDelay * (1 shl (attempt - 1)))
            }
        }

        val binder = currentBinder

        if (binder == null) {
            appProcessHandle.recycle()
            throw IllegalStateException("Failed to bind AppProcessService after $maxRetries attempts. Child process may have crashed or timed out.")
        }

        val deathRecipient = IBinder.DeathRecipient {
            Timber.w("Remote service died, forcing recycle")
            recycleForcibly()
        }

        try {
            binder.linkToDeath(deathRecipient, 0)
        } catch (e: RemoteException) {
            appProcessHandle.recycle()
            throw e
        }

        val serviceInterface = IAppProcessService.Stub.asInterface(binder)

        // Register a token so the server knows if WE (the client) die
        try {
            serviceInterface.registerDeathToken(Binder())
        } catch (e: RemoteException) {
            appProcessHandle.recycle()
            throw e
        }

        return UserServiceProxy(serviceInterface, appProcessHandle, binder, deathRecipient)
    }
}
