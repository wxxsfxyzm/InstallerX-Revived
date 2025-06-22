package com.rosan.installer.data.recycle.model.entity

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.shared.DhizukuVariables
import com.rosan.installer.data.recycle.util.InstallIntentFilter
import com.rosan.installer.data.recycle.util.delete

class DhizukuPrivilegedService : BasePrivilegedService() {
    private val devicePolicyManager: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    override fun delete(paths: Array<out String>) = paths.delete()

    override fun setDefaultInstaller(component: ComponentName, enable: Boolean) {
        devicePolicyManager.clearPackagePersistentPreferredActivities(
            // TODO
            // DhizukuVariables.PARAM_COMPONENT
            Dhizuku.getOwnerComponent(),
            component.packageName
        )
        if (!enable) return
        devicePolicyManager.addPersistentPreferredActivity(
            // TODO
            // DhizukuVariables.PARAM_COMPONENT,
            Dhizuku.getOwnerComponent(),
            InstallIntentFilter, component
        )
    }
}