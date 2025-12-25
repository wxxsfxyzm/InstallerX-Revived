package com.rosan.installer.data.app.model.impl.appInstaller

import android.os.IBinder
import com.rosan.installer.data.app.model.entity.InstallEntity
import com.rosan.installer.data.app.model.entity.InstallExtraInfoEntity
import com.rosan.installer.data.recycle.model.impl.recycler.ProcessHookRecycler
import com.rosan.installer.data.recycle.util.SHELL_ROOT
import com.rosan.installer.data.recycle.util.SHELL_SH
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext

object ProcessInstallerRepoImpl : IBinderInstallerRepoImpl() {
    private val localService = ThreadLocal<ProcessHookRecycler.HookedUserService>()

    override suspend fun doInstallWork(
        config: ConfigEntity,
        entities: List<InstallEntity>,
        extra: InstallExtraInfoEntity,
        blacklist: List<String>,
        sharedUserIdBlacklist: List<String>,
        sharedUserIdExemption: List<String>
    ) {
        val shell = when (config.authorizer) {
            ConfigEntity.Authorizer.Root -> SHELL_ROOT
            ConfigEntity.Authorizer.Customize -> config.customizeAuthorizer
            else -> SHELL_SH
        }

        val recycler = ProcessHookRecycler(shell)
        val recyclableHandle = recycler.make()

        withContext(localService.asContextElement(value = recyclableHandle.entity)) {
            try {
                super.doInstallWork(
                    config,
                    entities,
                    extra,
                    blacklist,
                    sharedUserIdBlacklist,
                    sharedUserIdExemption
                )
            } finally {
                recyclableHandle.recycle()
            }
        }
    }

    override suspend fun doUninstallWork(
        config: ConfigEntity,
        packageName: String,
        extra: InstallExtraInfoEntity
    ) {
        val shell = when (config.authorizer) {
            ConfigEntity.Authorizer.Root -> SHELL_ROOT
            ConfigEntity.Authorizer.Customize -> config.customizeAuthorizer
            else -> SHELL_SH
        }

        val recycler = ProcessHookRecycler(shell)
        val recyclableHandle = recycler.make()

        withContext(localService.asContextElement(value = recyclableHandle.entity)) {
            try {
                super.doUninstallWork(config, packageName, extra)
            } finally {
                recyclableHandle.recycle()
            }
        }
    }

    override suspend fun iBinderWrapper(iBinder: IBinder): IBinder {
        val service = localService.get()
            ?: throw IllegalStateException(
                "Service is null in iBinderWrapper. " +
                        "Make sure doInstallWork/doUninstallWork calls are properly scoped."
            )

        return service.binderWrapper(iBinder)
    }

    override suspend fun doFinishWork(
        config: ConfigEntity,
        entities: List<InstallEntity>,
        extraInfo: InstallExtraInfoEntity,
        result: Result<Unit>
    ) {
        super.doFinishWork(config, entities, extraInfo, result)
    }
}