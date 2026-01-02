package com.rosan.installer.data.updater.model.impl

import android.content.Context
import android.os.Process
import com.rosan.installer.data.app.model.entity.InstallEntity
import com.rosan.installer.data.app.model.entity.InstallExtraInfoEntity
import com.rosan.installer.data.app.model.enums.DataType
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.data.updater.repo.AppUpdater
import com.rosan.installer.data.updater.repo.UpdateChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import com.rosan.installer.data.app.model.impl.InstallerRepoImpl as CoreInstaller

class OnlineAppUpdater(
    private val updateChecker: UpdateChecker,
    private val context: Context
) : AppUpdater {
    override suspend fun performInAppUpdate(url: String, config: ConfigEntity) {
        // Get download stream entity (DataEntity)
        // This is the StreamDataEntity obtained from UpdateChecker
        val downloadDataEntity = withContext(Dispatchers.IO) {
            updateChecker.download(url)
        } ?: throw IOException("Failed to open download stream")

        // Construct InstallEntity
        // Wrap the network stream into an entity recognized by InstallerRepo
        val installEntity = InstallEntity(
            name = "base.apk",
            packageName = context.packageName, // Self-update, the package name is itself
            data = downloadDataEntity, // Pass in the network stream
            sourceType = DataType.APK
        )

        // Call the existing InstallerRepo to perform the installation
        // InstallerRepoImpl will automatically dispatch to System/Shizuku/Root implementations
        // IBinderInstallerRepoImpl will automatically call installIt to read the stream and write to Session
        CoreInstaller.doInstallWork(
            config = config, // Pass in config
            entities = listOf(installEntity),
            extra = InstallExtraInfoEntity(userId = Process.myUid() / 100000, ""),
            blacklist = emptyList(),
            sharedUserIdBlacklist = emptyList(),
            sharedUserIdExemption = emptyList()
        )
    }
}