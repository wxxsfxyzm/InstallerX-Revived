// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.data.engine.executor.appinstaller

import android.content.Context
import android.os.IBinder
import com.rosan.installer.core.reflection.ReflectionProvider
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.model.install.InstallEntity
import com.rosan.installer.domain.engine.model.install.InstallMetadata
import com.rosan.installer.domain.privileged.provider.PostInstallTaskProvider
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.ConfigModel
import com.rosan.installer.framework.privileged.recycler.ProcessHookRecycler
import com.rosan.installer.framework.privileged.util.AppProcessTerminal
import com.rosan.installer.framework.privileged.util.SHELL_SH
import com.rosan.installer.framework.privileged.util.ShellCommand
import com.rosan.installer.framework.privileged.util.requireCustomizeAuthorizer
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf

class ProcessAppInstallerRepoImpl(
    context: Context,
    reflect: ReflectionProvider,
    capabilityProvider: DeviceCapabilityProvider,
    postInstallTaskProvider: PostInstallTaskProvider,
) : IBinderAppInstallerRepoImpl(context, reflect, capabilityProvider, postInstallTaskProvider) {
    private var localService: ProcessHookRecycler.HookedUserService? = null

    override suspend fun doInstallWork(
        config: ConfigModel,
        entities: List<InstallEntity>,
        metadata: InstallMetadata,
        respectPlatformInstallPolicy: Boolean,
        blacklist: List<String>,
        sharedUserIdBlacklist: List<String>,
        sharedUserIdExemption: List<String>
    ) = runWithProcess(config) {
        super.doInstallWork(
            config,
            entities,
            metadata,
            respectPlatformInstallPolicy,
            blacklist,
            sharedUserIdBlacklist,
            sharedUserIdExemption
        )
    }

    override suspend fun doUninstallWork(
        config: ConfigModel,
        packageName: String
    ) = runWithProcess(config) {
        super.doUninstallWork(config, packageName)
    }

    override suspend fun approveSession(
        config: ConfigModel,
        sessionId: Int,
        granted: Boolean
    ) = runWithProcess(config) {
        super.approveSession(config, sessionId, granted)
    }

    override suspend fun iBinderWrapper(iBinder: IBinder): IBinder {
        val service = localService
            ?: throw IllegalStateException(
                "Service is null in iBinderWrapper. " +
                        "Make sure doInstallWork/doUninstallWork calls are properly scoped."
            )

        return service.binderWrapper(iBinder)
    }

    override suspend fun doFinishWork(
        config: ConfigModel,
        entities: List<InstallEntity>,
        result: Result<Unit>
    ) {
        super.doFinishWork(config, entities, result)
    }

    private suspend fun <T> runWithProcess(
        config: ConfigModel,
        rootTerminal: AppProcessTerminal = AppProcessTerminal.Root,
        block: suspend () -> T
    ): T {
        val terminal = when (config.authorizer) {
            Authorizer.Root -> rootTerminal
            Authorizer.Customize -> AppProcessTerminal.Customize(
                ShellCommand.parse(requireCustomizeAuthorizer(config.customizeAuthorizer))
            )

            else -> AppProcessTerminal.Customize(ShellCommand.of(SHELL_SH))
        }

        val handle = GlobalContext.get().get<ProcessHookRecycler> { parametersOf(terminal) }.make()

        return try {
            localService = handle.entity
            block()
        } finally {
            localService = null
            handle.recycle()
        }
    }
}
