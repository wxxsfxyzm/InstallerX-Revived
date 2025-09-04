package com.rosan.installer.data.app.model.impl.installer

import android.os.IBinder
import com.rosan.app_process.AppProcess
import com.rosan.installer.data.app.model.entity.InstallEntity
import com.rosan.installer.data.app.model.entity.InstallExtraInfoEntity
import com.rosan.installer.data.recycle.model.impl.AppProcessRecyclers
import com.rosan.installer.data.recycle.repo.Recyclable
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity

object ProcessInstallerRepoImpl : IBinderInstallerRepoImpl() {
    private lateinit var recycler: Recyclable<AppProcess>

    override suspend fun doInstallWork(
        config: ConfigEntity,
        entities: List<InstallEntity>,
        extra: InstallExtraInfoEntity,
        blacklist: List<String>,
        sharedUserIdBlacklist: List<String>,
        sharedUserIdExemption: List<String>
    ) {
        recycler = AppProcessRecyclers.get(
            when (config.authorizer) {
                ConfigEntity.Authorizer.Root -> "su"
                ConfigEntity.Authorizer.Customize -> config.customizeAuthorizer
                else -> "sh"
            }
        ).make()
        super.doInstallWork(config, entities, extra, blacklist, sharedUserIdBlacklist, sharedUserIdExemption)
    }

    override suspend fun doUninstallWork(config: ConfigEntity, packageName: String, extra: InstallExtraInfoEntity) {
        recycler = AppProcessRecyclers.get(
            when (config.authorizer) {
                ConfigEntity.Authorizer.Root -> "su"
                ConfigEntity.Authorizer.Customize -> config.customizeAuthorizer
                else -> "sh"
            }
        ).make()
        super.doUninstallWork(config, packageName, extra)
    }

    override suspend fun iBinderWrapper(iBinder: IBinder): IBinder =
        recycler.entity.binderWrapper(iBinder)

    override suspend fun doFinishWork(
        config: ConfigEntity,
        entities: List<InstallEntity>,
        extraInfo: InstallExtraInfoEntity,
        result: Result<Unit>
    ) {
        super.doFinishWork(config, entities, extraInfo, result)
        recycler.recycle()
    }
}