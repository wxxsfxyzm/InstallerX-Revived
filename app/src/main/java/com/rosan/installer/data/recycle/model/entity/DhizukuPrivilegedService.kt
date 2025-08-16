package com.rosan.installer.data.recycle.model.entity

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.installer.data.recycle.util.InstallIntentFilter
import com.rosan.installer.data.recycle.util.deletePaths
import timber.log.Timber

class DhizukuPrivilegedService : BasePrivilegedService() {
    private val devicePolicyManager: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    override fun delete(paths: Array<out String>) = deletePaths(paths)

    override fun setDefaultInstaller(component: ComponentName, enable: Boolean) {
        try {
            val ownerComponent = Dhizuku.getOwnerComponent()

            // 可以在这里添加日志，检查 ownerComponent 是否为 null
            Timber.tag("DhizukuPrivilegedService").d("Owner component: $ownerComponent")

            devicePolicyManager.clearPackagePersistentPreferredActivities(
                ownerComponent,
                component.packageName
            )
            if (!enable) return
            devicePolicyManager.addPersistentPreferredActivity(
                ownerComponent,
                InstallIntentFilter, component
            )
        } catch (t: Throwable) { // <--- 使用 Throwable 来捕获包括 Error 在内的所有问题
            // 捕获所有异常，防止进程崩溃
            Timber.tag("DhizukuPrivilegedService")
                .e(t, "Failed to set default installer due to a throwable")
            // 重新抛出，让客户端知道操作失败了
            throw t
        }
    }

    override fun execLine(command: String): String {
        // Device Owner Privileged Service does not support shell access
        throw UnsupportedOperationException("Not supported in DhizukuPrivilegedService")
    }


    override fun execArr(command: Array<String>): String {
        // Device Owner Privileged Service does not support shell access
        throw UnsupportedOperationException("Not supported in DhizukuPrivilegedService")
    }

    // Dhizuku does not support privileged activity
    override fun startActivityPrivileged(intent: Intent): Boolean {
        return false
    }

    override fun grantRuntimePermission(packageName: String?, permission: String?) {
        TODO("Not yet implemented")
    }

    override fun isPermissionGranted(packageName: String?, permission: String?): Boolean {
        TODO("Not yet implemented")
    }
}