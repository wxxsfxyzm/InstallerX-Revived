package com.rosan.installer.data.recycle.model.entity

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.IPackageManager
import android.content.pm.PackageManager
import android.content.pm.ParceledListSlice
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.ServiceManager
import com.rosan.installer.data.recycle.util.InstallIntentFilter
import com.rosan.installer.data.recycle.util.delete
import com.rosan.installer.data.reflect.repo.ReflectRepo
import org.koin.core.component.inject

class DefaultPrivilegedService : BasePrivilegedService() {
    private val reflect by inject<ReflectRepo>()

    override fun delete(paths: Array<out String>) = paths.delete()

    override fun setDefaultInstaller(component: ComponentName, enable: Boolean) {
        val uid = Process.myUid()
        val userId = uid / 100000
        val iPackageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"))

        val intent = Intent(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_DEFAULT)
            .setDataAndType(
                Uri.parse("content://storage/emulated/0/test.apk"),
                "application/vnd.android.package-archive"
            )
        val list = queryIntentActivities(
            iPackageManager,
            intent,
            "application/vnd.android.package-archive",
            PackageManager.MATCH_DEFAULT_ONLY,
            userId
        )
        var bestMatch = 0
        val names = list.map {
            val iPackageName = it.activityInfo.packageName
            val iClassName = it.activityInfo.name

            if (it.match > bestMatch) bestMatch = it.match

            // clear preferred
            iPackageManager.clearPackagePreferredActivities(iPackageName)
            if (uid == 1000) iPackageManager.clearPackagePersistentPreferredActivities(
                iPackageName,
                userId
            )

            ComponentName(iPackageName, iClassName)
        }.toTypedArray()

        if (!enable) return

        iPackageManager.setLastChosenActivity(
            intent,
            intent.type,
            PackageManager.MATCH_DEFAULT_ONLY,
            InstallIntentFilter,
            bestMatch,
            component
        )
        addPreferredActivity(
            iPackageManager,
            InstallIntentFilter,
            bestMatch,
            names,
            component,
            userId,
            true
        )
        if (uid == 1000) addPersistentPreferredActivity(
            iPackageManager,
            InstallIntentFilter,
            component,
            userId
        )
    }

    private fun addPreferredActivity(
        iPackageManager: IPackageManager,
        filter: IntentFilter,
        match: Int,
        names: Array<ComponentName>,
        name: ComponentName,
        userId: Int,
        removeExisting: Boolean
    ) {
        reflect.getDeclaredMethod(
            IPackageManager::class.java,
            "addPreferredActivity",
            IntentFilter::class.java,
            Int::class.java,
            Array<ComponentName>::class.java,
            ComponentName::class.java,
            Int::class.java,
            Boolean::class.java,
        )?.invoke(
            iPackageManager,
            filter,
            match,
            names,
            name,
            userId,
            removeExisting
        )
    }

    private fun addPersistentPreferredActivity(
        iPackageManager: IPackageManager,
        filter: IntentFilter,
        name: ComponentName,
        userId: Int,
    ) {
        reflect.getDeclaredMethod(
            IPackageManager::class.java,
            "addPersistentPreferredActivity",
            IntentFilter::class.java,
            ComponentName::class.java,
            Int::class.java,
        )?.invoke(
            iPackageManager,
            filter,
            name,
            userId,
        )
    }

    private fun queryIntentActivities(
        iPackageManager: IPackageManager,
        intent: Intent,
        resolvedType: String,
        flags: Int,
        userId: Int
    ): List<ResolveInfo> {
        return (
                reflect.getDeclaredMethod(
                    IPackageManager::class.java,
                    "queryIntentActivities",
                    Intent::class.java,
                    String::class.java,
                    Long::class.java,
                    Int::class.java
                )?.invoke(iPackageManager, intent, resolvedType, flags.toLong(), userId)
                        as ParceledListSlice<ResolveInfo>).list
    }
}