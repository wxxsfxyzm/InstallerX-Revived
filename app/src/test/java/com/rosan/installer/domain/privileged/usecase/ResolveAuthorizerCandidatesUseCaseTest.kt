// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.privileged.usecase

import com.rosan.installer.domain.device.model.ShizukuMode
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.settings.model.app.NamedPackage
import com.rosan.installer.domain.settings.model.app.SharedUid
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.preferences.AppPreferences
import com.rosan.installer.domain.settings.model.preferences.RootMode
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.domain.settings.repository.IntSetting
import com.rosan.installer.domain.settings.repository.NamedPackageListSetting
import com.rosan.installer.domain.settings.repository.SharedUidListSetting
import com.rosan.installer.domain.settings.repository.StringSetting
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class ResolveAuthorizerCandidatesUseCaseTest {
    @Test
    fun `preferred authorizer leads and is not repeated from the fallback list`() = runTest {
        val useCase = useCase(smartCandidates = "root:1,shizuku:1")

        assertEquals(
            listOf(Authorizer.Shizuku, Authorizer.Root, Authorizer.Dhizuku),
            useCase(Authorizer.Shizuku, customizeAuthorizer = ""),
        )
    }

    @Test
    fun `global preference is dropped so the fallback list stands alone`() = runTest {
        val useCase = useCase(smartCandidates = "shizuku:1")

        assertEquals(
            listOf(Authorizer.Shizuku, Authorizer.Root, Authorizer.Dhizuku),
            useCase(Authorizer.Global, customizeAuthorizer = ""),
        )
    }

    @Test
    fun `customize preference is dropped when the command is blank`() = runTest {
        val useCase = useCase(smartCandidates = "shizuku:0")

        assertEquals(
            listOf(Authorizer.Root, Authorizer.Dhizuku),
            useCase(Authorizer.Customize, customizeAuthorizer = "   "),
        )
    }

    @Test
    fun `customize preference survives when the command is set`() = runTest {
        val useCase = useCase(smartCandidates = "shizuku:0")

        assertEquals(
            listOf(Authorizer.Customize, Authorizer.Root, Authorizer.Dhizuku),
            useCase(Authorizer.Customize, customizeAuthorizer = "su -c"),
        )
    }

    @Test
    fun `every entry stays distinct even when the smart list repeats the preference`() = runTest {
        val useCase = useCase(smartCandidates = "shizuku:1,shizuku:1,root:1")

        val candidates = useCase(Authorizer.Shizuku, customizeAuthorizer = "")

        assertEquals(candidates.distinct(), candidates)
    }

    private fun useCase(
        smartCandidates: String,
        isSessionInstallSupported: Boolean = false,
    ) = ResolveAuthorizerCandidatesUseCase(
        appSettingsRepo = FakeAppSettingsRepository(smartCandidates),
        capabilityProvider = FakeCapabilityProvider(isSessionInstallSupported),
    )
}

private class FakeAppSettingsRepository(private val smartCandidates: String) : AppSettingsRepository {
    override val preferencesFlow: Flow<AppPreferences> = emptyFlow()

    override suspend fun putString(setting: StringSetting, value: String) = Unit

    override fun getString(setting: StringSetting, default: String): Flow<String> = flowOf(if (setting == StringSetting.SmartAuthorizerCandidates) smartCandidates else default)

    override suspend fun putInt(setting: IntSetting, value: Int) = Unit

    override fun getInt(setting: IntSetting, default: Int): Flow<Int> = flowOf(default)

    override suspend fun putBoolean(setting: BooleanSetting, value: Boolean) = Unit

    override fun getBoolean(setting: BooleanSetting, default: Boolean): Flow<Boolean> = flowOf(default)

    override suspend fun putNamedPackageList(
        setting: NamedPackageListSetting,
        packages: List<NamedPackage>,
    ) = Unit

    override fun getNamedPackageList(
        setting: NamedPackageListSetting,
        default: List<NamedPackage>,
    ): Flow<List<NamedPackage>> = flowOf(default)

    override suspend fun putSharedUidList(setting: SharedUidListSetting, uids: List<SharedUid>) = Unit

    override fun getSharedUidList(
        setting: SharedUidListSetting,
        default: List<SharedUid>,
    ): Flow<List<SharedUid>> = flowOf(default)

    override suspend fun updateUninstallFlags(transform: (Int) -> Int) = Unit
}

private class FakeCapabilityProvider(
    override val isSessionInstallSupported: Boolean = false,
) : DeviceCapabilityProvider {
    override val hasMiPackageInstaller = false
    override val isDefaultInstaller = false
    override val isSystemApp = false
    override val isHyperOS = false
    override val isMIUI = false
    override val isSupportMiIsland = false
    override val oplusOSdkVersion: String? = null
    override val deviceName = "test"
    override var isLSPosedActive = false
    override val shizukuModeFlow = MutableStateFlow(ShizukuMode.NONE)
    override val shizukuAuthorizedFlow = MutableStateFlow(false)
    override val dhizukuAvailableFlow = MutableStateFlow(false)
    override val dhizukuAuthorizedFlow = MutableStateFlow(false)
    override val rootModeFlow = MutableStateFlow(RootMode.None)
    override val defaultInstallerFlow = MutableStateFlow("Unknown")

    override fun refreshPrivilegeStatus() = Unit
}
