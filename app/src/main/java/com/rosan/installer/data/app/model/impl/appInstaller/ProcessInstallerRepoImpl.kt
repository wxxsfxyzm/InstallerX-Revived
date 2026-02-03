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
    ) = runWithProcess(config) {
        super.doInstallWork(config, entities, extra, blacklist, sharedUserIdBlacklist, sharedUserIdExemption)
    }

    override suspend fun doUninstallWork(
        config: ConfigEntity,
        packageName: String,
        extra: InstallExtraInfoEntity
    ) = runWithProcess(config) {
        super.doUninstallWork(config, packageName, extra)
    }

    override suspend fun approveSession(
        config: ConfigEntity,
        sessionId: Int,
        granted: Boolean
    ) = runWithProcess(config) {
        super.approveSession(config, sessionId, granted)
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

    private suspend fun <T> runWithProcess(
        config: ConfigEntity,
        rootShell: String = SHELL_ROOT,
        block: suspend () -> T
    ): T {
        val shell = when (config.authorizer) {
            ConfigEntity.Authorizer.Root -> rootShell
            ConfigEntity.Authorizer.Customize -> config.customizeAuthorizer
            else -> SHELL_SH
        }

        val recycler = ProcessHookRecycler(shell)
        val recyclableHandle = recycler.make()

        return withContext(localService.asContextElement(value = recyclableHandle.entity)) {
            try {
                block()
            } finally {
                recyclableHandle.recycle()
            }
        }
    }
}