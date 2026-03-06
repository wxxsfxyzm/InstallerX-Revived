// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.data.engine.executor.appInstaller

import android.content.Context
import android.os.IBinder
import com.rosan.installer.core.reflection.ReflectionProvider
import com.rosan.installer.data.privileged.repository.recycler.ProcessHookRecycler
import com.rosan.installer.data.privileged.util.SHELL_ROOT
import com.rosan.installer.data.privileged.util.SHELL_SH

import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.model.InstallEntity
import com.rosan.installer.domain.engine.model.InstallExtraInfoEntity
import com.rosan.installer.domain.privileged.provider.PostInstallTaskProvider
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.ConfigModel

class ProcessInstallerRepoImpl(
    context: Context,
    reflect: ReflectionProvider,
    capabilityProvider: DeviceCapabilityProvider,
    postInstallTaskProvider: PostInstallTaskProvider
) : IBinderInstallerRepoImpl(context, reflect, capabilityProvider, postInstallTaskProvider) {
    private var localService: ProcessHookRecycler.HookedUserService? = null

    override suspend fun doInstallWork(
        config: ConfigModel,
        entities: List<InstallEntity>,
        extra: InstallExtraInfoEntity,
        blacklist: List<String>,
        sharedUserIdBlacklist: List<String>,
        sharedUserIdExemption: List<String>
    ) = runWithProcess(config) {
        super.doInstallWork(config, entities, extra, blacklist, sharedUserIdBlacklist, sharedUserIdExemption)
    }

    override suspend fun doUninstallWork(
        config: ConfigModel,
        packageName: String,
        extra: InstallExtraInfoEntity
    ) = runWithProcess(config) {
        super.doUninstallWork(config, packageName, extra)
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
        extraInfo: InstallExtraInfoEntity,
        result: Result<Unit>
    ) {
        super.doFinishWork(config, entities, extraInfo, result)
    }

    private suspend fun <T> runWithProcess(
        config: ConfigModel,
        rootShell: String = SHELL_ROOT,
        block: suspend () -> T
    ): T {
        val shell = when (config.authorizer) {
            Authorizer.Root -> rootShell
            Authorizer.Customize -> config.customizeAuthorizer
            else -> SHELL_SH
        }

        val recycler = ProcessHookRecycler(shell)
        val recyclableHandle = recycler.make()

        localService = recyclableHandle.entity
        try {
            return block()
        } finally {
            localService = null
            recyclableHandle.recycle()
        }
    }
}