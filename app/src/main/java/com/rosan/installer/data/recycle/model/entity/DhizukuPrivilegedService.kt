package com.rosan.installer.data.recycle.model.entity

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.installer.ICommandOutputListener
import com.rosan.installer.data.recycle.util.InstallIntentFilter
import com.rosan.installer.data.recycle.util.deletePaths
import timber.log.Timber

class DhizukuPrivilegedService : BasePrivilegedService() {
    private val devicePolicyManager: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    override fun delete(paths: Array<out String>) = deletePaths(paths)

    override fun performDexOpt(
        packageName: String,
        compilerFilter: String,
        force: Boolean
    ): Boolean {
        throw UnsupportedOperationException("Not supported in DhizukuPrivilegedService")
    }

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

    override fun execArr(command: Array<String>): String {
        // Device Owner Privileged Service does not support shell access
        throw UnsupportedOperationException("Not supported in DhizukuPrivilegedService")
    }

    override fun execArrWithCallback(command: Array<String>, listener: ICommandOutputListener?) {
        // Device Owner Privileged Service does not support shell access
        throw UnsupportedOperationException("Not supported in DhizukuPrivilegedService")
    }

    // Dhizuku does not support privileged activity
    override fun startActivityPrivileged(intent: Intent): Boolean {
        return false
    }

    override fun setAdbVerify(enabled: Boolean) {
        throw UnsupportedOperationException("Not supported in DhizukuPrivilegedService")
    }

    /**
     * Grants a runtime permission to a specific package.
     * This requires Device Owner or Profile Owner privileges.
     *
     * @param packageName The package to grant the permission to.
     * @param permission The name of the permission to grant (e.g., android.Manifest.permission.CAMERA).
     */
    override fun grantRuntimePermission(packageName: String, permission: String) {
        // Check for null or empty arguments to avoid errors
        if (packageName.isEmpty() || permission.isEmpty()) {
            Timber.tag("DhizukuPrivilegedService")
                .w("grantRuntimePermission called with invalid arguments: packageName=$packageName, permission=$permission")
            throw IllegalArgumentException("packageName and permission must not be empty.")
        }

        try {
            val ownerComponent = Dhizuku.getOwnerComponent()
            Timber.tag("DhizukuPrivilegedService")
                .d("Attempting to grant permission '$permission' to package '$packageName' using owner '$ownerComponent'")

            // Use setPermissionGrantState to grant the permission
            // The user will not be able to revoke this permission from the settings UI
            devicePolicyManager.setPermissionGrantState(
                ownerComponent,
                packageName,
                permission,
                DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
            )
            Timber.tag("DhizukuPrivilegedService")
                .i("Successfully granted permission '$permission' to package '$packageName'")
        } catch (t: Throwable) {
            Timber.tag("DhizukuPrivilegedService")
                .e(t, "Failed to grant permission '$permission' to package '$packageName'")
            throw t
        }
    }

    /**
     * Checks if a runtime permission has been granted by the Device/Profile Owner's policy.
     *
     * @param packageName The package to check.
     * @param permission The name of the permission to check.
     * @return true if the permission is in the GRANTED state according to the policy, false otherwise.
     */
    override fun isPermissionGranted(packageName: String?, permission: String?): Boolean {
        // Check for null or empty arguments
        if (packageName.isNullOrEmpty() || permission.isNullOrEmpty()) {
            Timber.tag("DhizukuPrivilegedService")
                .w("isPermissionGranted called with invalid arguments: packageName=$packageName, permission=$permission")
            return false
        }

        try {
            val ownerComponent = Dhizuku.getOwnerComponent()
            // Get the current grant state set by the policy
            val grantState = devicePolicyManager.getPermissionGrantState(
                ownerComponent,
                packageName,
                permission
            )

            // Return true only if the policy has explicitly granted the permission
            return grantState == DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
        } catch (t: Throwable) {
            Timber.tag("DhizukuPrivilegedService")
                .e(t, "Failed to check permission grant state for package '$packageName'")
            // In case of an error, it's safer to assume the permission is not granted
            return false
        }
    }

    override fun getUsers(): Map<Int, String> {
        throw UnsupportedOperationException("Not supported in DhizukuPrivilegedService")
    }

    override fun getSessionDetails(sessionId: Int): Bundle {
        throw UnsupportedOperationException("Not supported in DhizukuPrivilegedService")
    }
}