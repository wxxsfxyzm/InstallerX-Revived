// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.framework.privileged.runtime

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.IBinder
import android.os.ServiceManager
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.installer.ICommandOutputListener
import com.rosan.installer.core.reflection.ReflectionProvider
import com.rosan.installer.framework.privileged.util.createDhizukuOwnerContext
import com.rosan.installer.framework.privileged.util.deletePaths
import org.koin.core.component.inject
import timber.log.Timber

private const val DHIZUKU_TAG = "DhizukuPrivilegedService"

private val DhizukuInstallIntentFilter = IntentFilter().apply {
    addAction(Intent.ACTION_MAIN)
    addAction(Intent.ACTION_VIEW)
    @Suppress("Deprecation") addAction(Intent.ACTION_INSTALL_PACKAGE)
    addCategory(Intent.CATEGORY_DEFAULT)
    addDataScheme(ContentResolver.SCHEME_CONTENT)
    addDataScheme(ContentResolver.SCHEME_FILE)
    addDataType("application/vnd.android.package-archive")
}

@SuppressLint("PrivateApi")
class DhizukuPrivilegedService(
    private val binderWrapper: (IBinder) -> IBinder
) : BasePrivilegedService(), PrivilegedOperations {
    private val reflect by inject<ReflectionProvider>()

    private val devicePolicyManager: DevicePolicyManager by lazy {
        val ownerContext = context.createDhizukuOwnerContext()

        Timber.tag(DHIZUKU_TAG).d("Using owner package context: ${ownerContext.packageName}")
        (ownerContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager).apply {
            val original = ServiceManager.getService(Context.DEVICE_POLICY_SERVICE)
                ?: throw IllegalStateException("device_policy service is not available")
            val wrapped = binderWrapper(original)
            val stubClass = Class.forName("android.app.admin.IDevicePolicyManager\$Stub")
            val service = reflect.invokeStaticMethod(
                name = "asInterface",
                clazz = stubClass,
                parameterTypes = arrayOf(IBinder::class.java),
                wrapped
            )
            reflect.setFieldValue(this, "mService", DevicePolicyManager::class.java, service)
            reflect.setFieldValue(this, "mContext", DevicePolicyManager::class.java, ownerContext)
        }
    }

    override fun delete(paths: Array<out String>) = deletePaths(paths.toList())

    override fun setDefaultInstaller(component: ComponentName, enable: Boolean) {
        try {
            val ownerComponent = Dhizuku.getOwnerComponent()
            Timber.tag(DHIZUKU_TAG).d("Owner component: $ownerComponent")

            devicePolicyManager.clearPackagePersistentPreferredActivities(
                ownerComponent,
                component.packageName
            )
            if (!enable) return
            devicePolicyManager.addPersistentPreferredActivity(
                ownerComponent,
                DhizukuInstallIntentFilter,
                component
            )
        } catch (t: Throwable) {
            Timber.tag(DHIZUKU_TAG).e(t, "Failed to set default installer due to a throwable")
            throw t
        }
    }

    override fun performDexOpt(packageName: String, compilerFilter: String, force: Boolean): Boolean =
        unsupported()

    override fun startActivityPrivileged(intent: Intent): Boolean = false

    override fun sendBroadcastPrivileged(intent: Intent): Boolean = false

    override fun execArr(command: Array<String>): String = unsupported()

    override fun execArrWithCallback(command: Array<String>, listener: ICommandOutputListener?) {
        unsupported<Unit>()
    }

    override fun setAdbVerify(enabled: Boolean) {
        unsupported<Unit>()
    }

    override fun grantRuntimePermission(packageName: String, permission: String) {
        unsupported<Unit>()
    }

    override fun isPermissionGranted(packageName: String, permission: String): Boolean =
        unsupported()

    override fun getUsers(): Map<Int, String> = unsupported()

    override fun getSessionDetails(sessionId: Int): Bundle? = unsupported()

    override fun setPackageNetworkingEnabled(uid: Int, enabled: Boolean) {
        unsupported<Unit>()
    }

    override fun prepareUnknownSourceAppOp(uid: Int, packageName: String): Int = unsupported()

    private fun <T> unsupported(): T =
        throw UnsupportedOperationException("Not supported in DhizukuPrivilegedService")
}
