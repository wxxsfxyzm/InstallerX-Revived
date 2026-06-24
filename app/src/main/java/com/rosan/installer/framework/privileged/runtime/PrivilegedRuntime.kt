// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.framework.privileged.runtime

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.IActivityManager
import android.content.Context
import android.content.pm.IPackageManager
import android.net.IConnectivityManager
import android.os.IBinder
import android.os.IUserManager
import android.os.ServiceManager
import com.rosan.installer.core.reflection.ReflectionProvider
import com.rosan.installer.framework.privileged.util.ShizukuContext
import com.rosan.installer.framework.privileged.util.ShizukuHook
import com.rosan.installer.framework.privileged.util.SystemContext
import com.rosan.installer.framework.privileged.util.resolveSettingsBinder
import timber.log.Timber

private const val TAG = "PrivilegedRuntime"

@SuppressLint("PrivateApi")
internal sealed interface PrivilegedRuntime {
    val name: String
    val canCallSystemRestrictedPreferredApis: Boolean

    fun settingsResolverContext(context: Context): Context

    fun activityCallerPackage(context: Context): String

    fun packageManager(): IPackageManager

    fun activityManager(): IActivityManager

    fun userManager(): IUserManager

    fun settingsBinder(reflect: ReflectionProvider): IBinder?

    fun connectivityManager(): IConnectivityManager

    fun appOpsBinder(): IBinder?

    fun appOpsManager(context: Context, reflect: ReflectionProvider): AppOpsManager {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val binder = appOpsBinder() ?: return appOps
        val service = Class.forName("com.android.internal.app.IAppOpsService\$Stub")
            .getMethod("asInterface", IBinder::class.java)
            .invoke(null, binder)

        reflect.setFieldValue(appOps, "mService", appOps.javaClass, service)
        return appOps
    }

    data object SystemApp : Direct("SystemApp", canCallSystemRestrictedPreferredApis = false)

    data object UserService : Direct("UserService", canCallSystemRestrictedPreferredApis = false)

    data object ShizukuHooked : PrivilegedRuntime {
        override val name = "ShizukuHook"
        override val canCallSystemRestrictedPreferredApis = false

        override fun settingsResolverContext(context: Context): Context {
            Timber.tag(TAG).d("Using ShellContextResolver for $name.")
            return ShizukuContext(context)
        }

        override fun activityCallerPackage(context: Context) = "com.android.shell"

        override fun packageManager(): IPackageManager {
            Timber.tag(TAG).d("Getting IPackageManager in $name mode.")
            return ShizukuHook.hookedPackageManager
        }

        override fun activityManager(): IActivityManager {
            Timber.tag(TAG).d("Getting IActivityManager in $name mode.")
            return ShizukuHook.hookedActivityManager
        }

        override fun userManager(): IUserManager {
            Timber.tag(TAG).d("Getting IUserManager in $name mode.")
            return ShizukuHook.hookedUserManager
        }

        override fun settingsBinder(reflect: ReflectionProvider): IBinder? {
            Timber.tag(TAG).d("Getting Settings Binder in $name mode.")
            return ShizukuHook.hookedSettingsBinder
        }

        override fun connectivityManager(): IConnectivityManager {
            Timber.tag(TAG).d("Getting IConnectivityManager in $name mode.")
            return ShizukuHook.hookedConnectivityManager
        }

        override fun appOpsBinder(): IBinder? {
            Timber.tag(TAG).d("Getting AppOps Binder in $name mode.")
            return ShizukuHook.hookedAppOpsBinder
        }
    }

    class BinderWrapped(
        override val name: String,
        private val useAppCallerPackage: Boolean,
        private val binderWrapper: (IBinder) -> IBinder
    ) : PrivilegedRuntime {
        override val canCallSystemRestrictedPreferredApis = true

        override fun settingsResolverContext(context: Context): Context {
            Timber.tag(TAG).d("Using SystemContextResolver for $name.")
            return SystemContext(context)
        }

        override fun activityCallerPackage(context: Context): String {
            return if (useAppCallerPackage) context.packageName else "com.android.shell"
        }

        override fun packageManager(): IPackageManager {
            Timber.tag(TAG).d("Getting IPackageManager in $name mode.")
            val original = ServiceManager.getService("package")
            return IPackageManager.Stub.asInterface(binderWrapper(original))
        }

        override fun activityManager(): IActivityManager {
            Timber.tag(TAG).d("Getting IActivityManager in $name mode.")
            val original = ServiceManager.getService(Context.ACTIVITY_SERVICE)
            return IActivityManager.Stub.asInterface(binderWrapper(original))
        }

        override fun userManager(): IUserManager {
            Timber.tag(TAG).d("Getting IUserManager in $name mode.")
            val original = ServiceManager.getService(Context.USER_SERVICE)
            return IUserManager.Stub.asInterface(binderWrapper(original))
        }

        override fun settingsBinder(reflect: ReflectionProvider): IBinder? {
            Timber.tag(TAG).d("Getting Settings Binder in $name mode.")
            val original = reflect.resolveSettingsBinder()?.originalBinder
            return original?.let(binderWrapper)
        }

        override fun connectivityManager(): IConnectivityManager {
            Timber.tag(TAG).d("Getting IConnectivityManager in $name mode.")
            val original = ServiceManager.getService(Context.CONNECTIVITY_SERVICE)
            return IConnectivityManager.Stub.asInterface(binderWrapper(original))
        }

        override fun appOpsBinder(): IBinder {
            Timber.tag(TAG).d("Getting AppOps Binder in $name mode.")
            val original = ServiceManager.getService(Context.APP_OPS_SERVICE)
            return binderWrapper(original)
        }
    }

    sealed class Direct(
        override val name: String,
        override val canCallSystemRestrictedPreferredApis: Boolean
    ) : PrivilegedRuntime {
        override fun settingsResolverContext(context: Context): Context {
            Timber.tag(TAG).d("Using ShellContextResolver for $name.")
            return ShizukuContext(context)
        }

        override fun activityCallerPackage(context: Context): String {
            return when (this) {
                SystemApp -> context.packageName
                UserService -> "com.android.shell"
            }
        }

        override fun packageManager(): IPackageManager {
            Timber.tag(TAG).d("Getting IPackageManager in $name mode.")
            return IPackageManager.Stub.asInterface(ServiceManager.getService("package"))
        }

        override fun activityManager(): IActivityManager {
            Timber.tag(TAG).d("Getting IActivityManager in $name mode.")
            return IActivityManager.Stub.asInterface(ServiceManager.getService(Context.ACTIVITY_SERVICE))
        }

        override fun userManager(): IUserManager {
            Timber.tag(TAG).d("Getting IUserManager in $name mode.")
            return IUserManager.Stub.asInterface(ServiceManager.getService(Context.USER_SERVICE))
        }

        override fun settingsBinder(reflect: ReflectionProvider): IBinder? {
            Timber.tag(TAG).d("Getting Settings Binder in $name mode.")
            return reflect.resolveSettingsBinder()?.originalBinder
        }

        override fun connectivityManager(): IConnectivityManager {
            Timber.tag(TAG).d("Getting IConnectivityManager in $name mode.")
            return IConnectivityManager.Stub.asInterface(ServiceManager.getService(Context.CONNECTIVITY_SERVICE))
        }

        override fun appOpsBinder(): IBinder? {
            Timber.tag(TAG).d("Getting AppOps Binder in $name mode.")
            return ServiceManager.getService(Context.APP_OPS_SERVICE)
        }
    }
}
