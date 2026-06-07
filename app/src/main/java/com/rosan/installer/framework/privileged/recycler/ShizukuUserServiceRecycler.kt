// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.framework.privileged.recycler

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Process
import androidx.annotation.Keep
import com.rosan.installer.IPrivilegedService
import com.rosan.installer.IShizukuUserService
import com.rosan.installer.core.env.AppConfig
import com.rosan.installer.core.reflection.ReflectionProvider
import com.rosan.installer.core.reflection.ReflectionProviderImpl
import com.rosan.installer.core.reflection.invoke
import com.rosan.installer.core.reflection.invokeStatic
import com.rosan.installer.di.init.processModules
import com.rosan.installer.framework.privileged.lifecycle.Recycler
import com.rosan.installer.framework.privileged.lifecycle.UserService
import com.rosan.installer.framework.privileged.runtime.DefaultPrivilegedService
import com.rosan.installer.framework.privileged.util.SystemUidEnvironment
import com.rosan.installer.framework.privileged.util.UserServiceUidMode
import com.rosan.installer.framework.privileged.util.requireShizukuPermissionGranted
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import rikka.shizuku.Shizuku
import timber.log.Timber
import kotlin.system.exitProcess

class ShizukuUserServiceRecycler(
    private val context: Context,
    private val serviceClass: Class<out IShizukuUserService> = ShizukuUserService::class.java,
    private val processNameSuffix: String = "shizuku_privileged"
) : Recycler<ShizukuUserServiceRecycler.UserServiceProxy>() {

    class UserServiceProxy(val service: IShizukuUserService) : UserService {
        override val privileged: IPrivilegedService = service.privilegedService
        override fun close() = service.destroy()
    }

    abstract class BaseShizukuUserService(
        context: Context,
        private val uidMode: UserServiceUidMode
    ) : IShizukuUserService.Stub() {
        init {
            if (AppConfig.isDebug && Timber.treeCount == 0) Timber.plant(Timber.DebugTree())
            startKoin {
                modules(processModules)
                androidContext(createSystemContext(context))
            }
        }

        private val privileged = DefaultPrivilegedService.userService()

        override fun destroy() {
            exitProcess(0)
        }

        override fun getPrivilegedService(): IPrivilegedService = privileged

        private fun createSystemContext(
            fallbackContext: Context,
            reflection: ReflectionProvider = ReflectionProviderImpl()
        ): Context = try {
            val packageName = SystemUidEnvironment.shizukuPackageNameFor(uidMode, TAG)

            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val userHandleClass = Class.forName("android.os.UserHandle")

            val systemMain = reflection.invokeStatic<Any>("systemMain", activityThreadClass)
                ?: throw IllegalStateException("systemMain returned null")

            val systemContext = reflection.invoke<Context>(systemMain, "getSystemContext", activityThreadClass)
                ?: throw IllegalStateException("systemContext returned null")

            val userId = try {
                reflection.invokeStatic<Int>("myUserId", userHandleClass) ?: (Process.myUid() / 100000)
            } catch (e: Exception) {
                Timber.tag("ShizukuUserService").e(e, "Failed to get userId via reflection")
                Process.myUid() / 100000
            }

            val userHandleConstructor = reflection.getConstructor(userHandleClass, Int::class.javaPrimitiveType ?: Int::class.java)
                ?: throw NoSuchMethodException("UserHandle(int) constructor not found")
            val userHandleInstance = userHandleConstructor.newInstance(userId)

            val contextAsUser = reflection.invoke<Context>(
                obj = systemContext,
                name = "createPackageContextAsUser",
                clazz = systemContext.javaClass,
                parameterTypes = arrayOf(
                    String::class.java,
                    Int::class.javaPrimitiveType ?: Int::class.java,
                    userHandleClass
                ),
                args = arrayOf(
                    packageName,
                    Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY,
                    userHandleInstance
                )
            ) ?: throw IllegalStateException("createPackageContextAsUser returned null")

            val finalContext = contextAsUser.createPackageContext(packageName, 0)

            Timber.tag("ShizukuUserService").d("Created system context: ${finalContext.packageName}, UID: ${Process.myUid()}")
            finalContext

        } catch (e: Exception) {
            Timber.tag("ShizukuUserService").e(e, "Failed to create system context: ${e.message}")
            fallbackContext
        }

        private companion object {
            private const val TAG = "ShizukuUserService"
        }
    }

    class ShizukuUserService @Keep constructor(context: Context) : BaseShizukuUserService(
        context = context,
        uidMode = UserServiceUidMode.Default
    )

    class SystemUidShizukuUserService @Keep constructor(context: Context) : BaseShizukuUserService(
        context = context,
        uidMode = UserServiceUidMode.SystemIfRoot
    )

    override fun onMake(): UserServiceProxy = runBlocking {
        requireShizukuPermissionGranted {
            onInnerMake()
        }
    }

    private suspend fun onInnerMake(): UserServiceProxy = callbackFlow {
        Shizuku.bindUserService(
            Shizuku.UserServiceArgs(
                ComponentName(
                    context, serviceClass
                )
            ).processNameSuffix(processNameSuffix), object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    trySend(UserServiceProxy(IShizukuUserService.Stub.asInterface(service)))
                    service?.linkToDeath({
                        if (entity?.service == service) recycleForcibly()
                    }, 0)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    close()
                }
            })
        awaitClose { }
    }.first()
}
