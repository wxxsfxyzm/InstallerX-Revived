package com.rosan.installer.data.app.model.impl.appInstaller

import android.os.IBinder
import com.rosan.app_process.AppProcess
import com.rosan.installer.data.app.model.entity.InstallEntity
import com.rosan.installer.data.app.model.entity.InstallExtraInfoEntity
import com.rosan.installer.data.recycle.model.impl.recycler.AppProcessRecyclers
import com.rosan.installer.data.recycle.repo.Recyclable
import com.rosan.installer.data.recycle.util.SHELL_ROOT
import com.rosan.installer.data.recycle.util.SHELL_SH
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext

object ProcessInstallerRepoImpl : IBinderInstallerRepoImpl() {
    private val localRecycler = ThreadLocal<Recyclable<AppProcess>>()

    private fun createRecycler(config: ConfigEntity): Recyclable<AppProcess> =
        AppProcessRecyclers.get(
            when (config.authorizer) {
                ConfigEntity.Authorizer.Root -> SHELL_ROOT
                ConfigEntity.Authorizer.Customize -> config.customizeAuthorizer
                else -> SHELL_SH
            }
        ).make()

    override suspend fun doInstallWork(
        config: ConfigEntity,
        entities: List<InstallEntity>,
        extra: InstallExtraInfoEntity,
        blacklist: List<String>,
        sharedUserIdBlacklist: List<String>,
        sharedUserIdExemption: List<String>
    ) {
        val recycler = createRecycler(config)

        withContext(localRecycler.asContextElement(value = recycler)) {
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
                recycler.recycle()
            }
        }
    }

    override suspend fun doUninstallWork(
        config: ConfigEntity,
        packageName: String,
        extra: InstallExtraInfoEntity
    ) {
        val recycler = createRecycler(config)

        withContext(localRecycler.asContextElement(value = recycler)) {
            try {
                super.doUninstallWork(config, packageName, extra)
            } finally {
                recycler.recycle()
            }
        }
    }

    override suspend fun iBinderWrapper(iBinder: IBinder): IBinder {
        val recycler = localRecycler.get()
            ?: throw IllegalStateException(
                "Recycler is null in iBinderWrapper. " +
                        "This indicates doInstallWork/doUninstallWork is not properly scoping the ThreadLocal. " +
                        "Make sure you are calling this within the managed context."
            )

        return recycler.entity.binderWrapper(iBinder)
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