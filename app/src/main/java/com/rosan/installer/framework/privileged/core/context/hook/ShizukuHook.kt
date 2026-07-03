// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.framework.privileged.core.context.hook

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.IActivityManager
import android.content.Context
import android.content.pm.IPackageManager
import android.net.IConnectivityManager
import android.os.IBinder
import android.os.IUserManager
import com.rosan.installer.core.reflection.ReflectionProvider
import com.rosan.installer.core.reflection.getStaticValue
import com.rosan.installer.core.reflection.getValue
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import timber.log.Timber

@SuppressLint("PrivateApi", "DiscouragedPrivateApi")
object ShizukuHook : KoinComponent {
    private val reflect by inject<ReflectionProvider>()

    val hookedPackageManager: IPackageManager by lazy {
        Timber.tag("ShizukuHook").d("Creating on-demand hooked IPackageManager...")
        val originalBinder = SystemServiceHelper.getSystemService("package")
        val originalPM = IPackageManager.Stub.asInterface(originalBinder)

        val wrapper = ShizukuBinderWrapper(originalPM.asBinder())
        IPackageManager.Stub.asInterface(wrapper).also {
            Timber.tag("ShizukuHook").i("On-demand hooked IPackageManager created.")
        }
    }

    val hookedActivityManager: IActivityManager by lazy {
        Timber.tag("ShizukuHook").d("Creating on-demand hooked IActivityManager...")
        val amSingleton = reflect.getStaticValue<Any>("IActivityManagerSingleton", ActivityManager::class.java)
            ?: throw NullPointerException("Failed to retrieve IActivityManagerSingleton")
        val singletonClass = Class.forName("android.util.Singleton")

        // Explicitly passing singletonClass is safer because mInstance is private in the base class
        val originalAM = reflect.getValue<IActivityManager>(amSingleton, "mInstance", singletonClass)
            ?: throw NullPointerException("Failed to retrieve mInstance from Singleton")

        val wrapper = ShizukuBinderWrapper(originalAM.asBinder())
        IActivityManager.Stub.asInterface(wrapper).also {
            Timber.tag("ShizukuHook").i("On-demand hooked IActivityManager created.")
        }
    }

    val hookedUserManager: IUserManager by lazy {
        Timber.tag("ShizukuHook").d("Creating on-demand hooked IUserManager...")
        val originalBinder = SystemServiceHelper.getSystemService(Context.USER_SERVICE)
        val originalUM = IUserManager.Stub.asInterface(originalBinder)
        val wrapper = ShizukuBinderWrapper(originalUM.asBinder())
        IUserManager.Stub.asInterface(wrapper).also {
            Timber.tag("ShizukuHook").i("On-demand hooked IUserManager created.")
        }
    }

    val hookedSettingsBinder: IBinder? by lazy {
        Timber.tag("ShizukuHook").d("Creating on-demand hooked Settings Binder...")
        try {
            val info = reflect.resolveSettingsBinder() ?: return@lazy null

            ShizukuBinderWrapper(info.originalBinder).also {
                Timber.tag("ShizukuHook").i("On-demand hooked Settings Binder created.")
            }
        } catch (e: Exception) {
            Timber.tag("ShizukuHook").e(e, "Failed to create hooked Settings Binder")
            null
        }
    }

    val hookedConnectivityManager: IConnectivityManager by lazy {
        Timber.tag("ShizukuHook").d("Creating on-demand hooked IConnectivityManager...")
        try {
            val originalBinder = SystemServiceHelper.getSystemService(Context.CONNECTIVITY_SERVICE)
            val originalCM = IConnectivityManager.Stub.asInterface(originalBinder)
            val wrapper = ShizukuBinderWrapper(originalCM.asBinder())
            IConnectivityManager.Stub.asInterface(wrapper).also {
                Timber.tag("ShizukuHook").i("On-demand hooked IConnectivityManager created.")
            }
        } catch (e: Exception) {
            Timber.tag("ShizukuHook").e(e, "Failed to create hooked IConnectivityManager")
            throw e
        }
    }

    val hookedAppOpsBinder: IBinder? by lazy {
        Timber.tag("ShizukuHook").d("Creating on-demand hooked AppOps Binder...")
        runCatching {
            ShizukuBinderWrapper(SystemServiceHelper.getSystemService(Context.APP_OPS_SERVICE))
        }.onFailure { error ->
            Timber.tag("ShizukuHook").e(error, "Failed to create hooked AppOps Binder")
        }.getOrNull()
    }
}
