package com.rosan.installer.data.app.model.impl.appInstaller

import android.os.IBinder
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.installer.data.app.model.entity.InstallEntity
import com.rosan.installer.data.app.model.entity.InstallExtraInfoEntity
import com.rosan.installer.data.app.util.sourcePath
import com.rosan.installer.data.recycle.util.deletePaths
import com.rosan.installer.data.recycle.util.requireDhizukuPermissionGranted
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import org.koin.core.component.KoinComponent
import timber.log.Timber

object DhizukuInstallerRepoImpl : IBinderInstallerRepoImpl(), KoinComponent {
    override suspend fun iBinderWrapper(iBinder: IBinder): IBinder =
        requireDhizukuPermissionGranted {
            Dhizuku.binderWrapper(iBinder)
        }

    override suspend fun onDeleteWork(
        config: ConfigEntity,
        entities: List<InstallEntity>,
        extra: InstallExtraInfoEntity
    ) {
        // Extract the unique source file paths from the install entities.
        val pathsToDelete = entities.sourcePath()
        if (pathsToDelete.isEmpty()) {
            Timber.tag("onDeleteWork").w("Dhizuku: No source paths found to delete.")
            return
        }

        Timber.tag("onDeleteWork").d("Dhizuku: Attempting to delete paths using standard API: ${pathsToDelete.joinToString()}")

        // Use the deletePaths utility function which handles file deletion using standard APIs,
        // making it suitable for the non-privileged context of app process.
        deletePaths(pathsToDelete)
    }
}