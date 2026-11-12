// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.usecase

import com.rosan.installer.domain.engine.provider.InstalledAppInfoProvider
import com.rosan.installer.domain.engine.repository.AppInstallerRepository
import com.rosan.installer.domain.history.repository.OperationHistoryRepository
import com.rosan.installer.domain.history.usecase.RecordOperationHistoryUseCase
import com.rosan.installer.domain.settings.model.config.ConfigModel
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class ProcessUninstallUserTest {
    @Test
    fun `uninstall forwards request user independently of install profile target`() = runTest {
        val calls = mutableListOf<List<Any?>>()
        val useCase = ProcessUninstallUseCase(
            appInstaller = stub<AppInstallerRepository> { name, args ->
                check(name == "doUninstallWork")
                calls.add(args.take(3))
                Unit
            },
            installedAppInfoProvider = stub<InstalledAppInfoProvider> { _, _ -> null },
            recordOperationHistory = RecordOperationHistoryUseCase(
                stub<OperationHistoryRepository> { _, _ -> Unit },
            ),
        )
        val config = ConfigModel.default.copy(enableCustomizeUser = true, targetUserId = 0)

        useCase(config, "example.app", userId = 11)

        assertEquals(listOf(listOf<Any?>(config, "example.app", 11)), calls.toList())
    }

    private inline fun <reified T> stub(crossinline handler: (String, Array<out Any?>) -> Any?): T = Proxy.newProxyInstance(
        T::class.java.classLoader,
        arrayOf(T::class.java),
    ) { _, method, args -> handler(method.name, args.orEmpty()) } as T
}
