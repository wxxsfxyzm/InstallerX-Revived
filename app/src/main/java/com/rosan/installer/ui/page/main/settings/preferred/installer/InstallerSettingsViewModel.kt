// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.installer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.domain.settings.model.BiometricAuthMode
import com.rosan.installer.domain.settings.provider.SystemEnvProvider
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.domain.settings.repository.IntSetting
import com.rosan.installer.domain.settings.repository.NamedPackageListSetting
import com.rosan.installer.domain.settings.repository.SharedUidListSetting
import com.rosan.installer.domain.settings.repository.StringSetting
import com.rosan.installer.domain.settings.usecase.settings.ManagePackageListUseCase
import com.rosan.installer.domain.settings.usecase.settings.ManageSharedUidListUseCase
import com.rosan.installer.domain.settings.usecase.settings.UpdateSettingUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InstallerSettingsViewModel(
    appSettingsRepo: AppSettingsRepository,
    private val systemEnvProvider: SystemEnvProvider,
    private val updateSetting: UpdateSettingUseCase,
    private val managePackageListUseCase: ManagePackageListUseCase,
    private val manageSharedUidListUseCase: ManageSharedUidListUseCase
) : ViewModel() {

    val state: StateFlow<InstallerSettingsState> = appSettingsRepo.preferencesFlow.map { prefs ->
        InstallerSettingsState(
            authorizer = prefs.authorizer,
            alwaysUseRootInSystem = prefs.alwaysUseRootInSystem,
            dhizukuAutoCloseCountDown = prefs.dhizukuAutoCloseCountDown,
            installerRequireBiometricAuth = prefs.installerRequireBiometricAuth,
            showOPPOSpecial = prefs.showOPPOSpecial,
            managedInstallerPackages = prefs.managedInstallerPackages,
            managedBlacklistPackages = prefs.managedBlacklistPackages,
            managedSharedUserIdBlacklist = prefs.managedSharedUserIdBlacklist,
            managedSharedUserIdExemptedPackages = prefs.managedSharedUserIdExemptedPackages
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = InstallerSettingsState()
    )

    fun dispatch(action: InstallerSettingsAction) {
        when (action) {
            is InstallerSettingsAction.ChangeGlobalAuthorizer -> viewModelScope.launch {
                updateSetting(
                    StringSetting.Authorizer,
                    action.authorizer.value
                )
            }

            is InstallerSettingsAction.ChangeAlwaysUseRootInSystem -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.AlwaysUseRootInSystem,
                    action.alwaysUseRootInSystem
                )
            }

            is InstallerSettingsAction.ChangeDhizukuAutoCloseCountDown -> {
                if (action.countDown in 1..10) viewModelScope.launch { updateSetting(IntSetting.DialogAutoCloseCountdown, action.countDown) }
            }

            is InstallerSettingsAction.ChangeBiometricAuth -> changeBiometricAuth(action.mode)

            is InstallerSettingsAction.ChangeShowOPPOSpecial -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.DialogShowOppoSpecial,
                    action.show
                )
            }

            is InstallerSettingsAction.AddManagedInstallerPackage -> viewModelScope.launch {
                managePackageListUseCase.addPackage(NamedPackageListSetting.ManagedInstallerPackages, action.pkg)
            }

            is InstallerSettingsAction.RemoveManagedInstallerPackage -> viewModelScope.launch {
                managePackageListUseCase.removePackage(NamedPackageListSetting.ManagedInstallerPackages, action.pkg)
            }

            is InstallerSettingsAction.AddManagedBlacklistPackage -> viewModelScope.launch {
                managePackageListUseCase.addPackage(NamedPackageListSetting.ManagedBlacklistPackages, action.pkg)
            }

            is InstallerSettingsAction.RemoveManagedBlacklistPackage -> viewModelScope.launch {
                managePackageListUseCase.removePackage(NamedPackageListSetting.ManagedBlacklistPackages, action.pkg)
            }

            is InstallerSettingsAction.AddManagedSharedUserIdExemptedPackages -> viewModelScope.launch {
                managePackageListUseCase.addPackage(NamedPackageListSetting.ManagedSharedUserIdExemptedPackages, action.pkg)
            }

            is InstallerSettingsAction.RemoveManagedSharedUserIdExemptedPackages -> viewModelScope.launch {
                managePackageListUseCase.removePackage(NamedPackageListSetting.ManagedSharedUserIdExemptedPackages, action.pkg)
            }

            is InstallerSettingsAction.AddManagedSharedUserIdBlacklist -> viewModelScope.launch {
                manageSharedUidListUseCase.addUid(SharedUidListSetting.ManagedSharedUserIdBlacklist, action.uid)
            }

            is InstallerSettingsAction.RemoveManagedSharedUserIdBlacklist -> viewModelScope.launch {
                manageSharedUidListUseCase.removeUid(SharedUidListSetting.ManagedSharedUserIdBlacklist, action.uid)
            }

            is InstallerSettingsAction.MoveManagedInstallerPackage -> viewModelScope.launch {
                managePackageListUseCase.movePackage(NamedPackageListSetting.ManagedInstallerPackages, action.fromIndex, action.toIndex)
            }

            is InstallerSettingsAction.MoveManagedBlacklistPackage -> viewModelScope.launch {
                managePackageListUseCase.movePackage(NamedPackageListSetting.ManagedBlacklistPackages, action.fromIndex, action.toIndex)
            }

            is InstallerSettingsAction.MoveManagedSharedUserIdBlacklist -> viewModelScope.launch {
                manageSharedUidListUseCase.moveUid(
                    SharedUidListSetting.ManagedSharedUserIdBlacklist,
                    action.fromIndex,
                    action.toIndex
                )
            }

            is InstallerSettingsAction.MoveManagedSharedUserIdExemptedPackages -> viewModelScope.launch {
                managePackageListUseCase.movePackage(
                    NamedPackageListSetting.ManagedSharedUserIdExemptedPackages,
                    action.fromIndex,
                    action.toIndex
                )
            }
        }
    }

    private fun changeBiometricAuth(mode: BiometricAuthMode) = viewModelScope.launch {
        if (systemEnvProvider.authenticateBiometric(isInstaller = true)) {
            updateSetting(StringSetting.InstallerBiometricAuthMode, mode.value)
        }
    }
}
