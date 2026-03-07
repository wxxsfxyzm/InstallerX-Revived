// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.subpage.installer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.domain.settings.provider.SystemEnvProvider
import com.rosan.installer.domain.settings.repository.AppSettingsRepo
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
    appSettingsRepo: AppSettingsRepo,
    private val systemEnvProvider: SystemEnvProvider,
    private val updateSetting: UpdateSettingUseCase,
    private val managePackageListUseCase: ManagePackageListUseCase,
    private val manageSharedUidListUseCase: ManageSharedUidListUseCase
) : ViewModel() {

    val state: StateFlow<InstallerSettingsState> = appSettingsRepo.preferencesFlow.map { prefs ->
        InstallerSettingsState(
            isLoading = false,
            useBlur = prefs.useBlur,
            authorizer = prefs.authorizer,
            dhizukuAutoCloseCountDown = prefs.dhizukuAutoCloseCountDown,
            installMode = prefs.installMode,
            showLiveActivity = prefs.showLiveActivity,
            installerRequireBiometricAuth = prefs.installerRequireBiometricAuth,
            notificationSuccessAutoClearSeconds = prefs.notificationSuccessAutoClearSeconds,
            versionCompareInSingleLine = prefs.versionCompareInSingleLine,
            sdkCompareInMultiLine = prefs.sdkCompareInMultiLine,
            showDialogInstallExtendedMenu = prefs.showDialogInstallExtendedMenu,
            showSmartSuggestion = prefs.showSmartSuggestion,
            showDialogWhenPressingNotification = prefs.showDialogWhenPressingNotification,
            autoSilentInstall = prefs.autoSilentInstall,
            disableNotificationForDialogInstall = prefs.disableNotificationForDialogInstall,
            showOPPOSpecial = prefs.showOPPOSpecial,
            managedInstallerPackages = prefs.managedInstallerPackages,
            managedBlacklistPackages = prefs.managedBlacklistPackages,
            managedSharedUserIdBlacklist = prefs.managedSharedUserIdBlacklist,
            managedSharedUserIdExemptedPackages = prefs.managedSharedUserIdExemptedPackages
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = InstallerSettingsState(isLoading = true)
    )

    fun dispatch(action: InstallerSettingsAction) {
        when (action) {
            is InstallerSettingsAction.ChangeGlobalAuthorizer -> viewModelScope.launch {
                updateSetting(
                    StringSetting.Authorizer,
                    action.authorizer.value
                )
            }

            is InstallerSettingsAction.ChangeDhizukuAutoCloseCountDown -> {
                if (action.countDown in 1..10) viewModelScope.launch { updateSetting(IntSetting.DialogAutoCloseCountdown, action.countDown) }
            }

            is InstallerSettingsAction.ChangeGlobalInstallMode -> viewModelScope.launch {
                updateSetting(
                    StringSetting.InstallMode,
                    action.installMode.value
                )
            }

            is InstallerSettingsAction.ChangeShowLiveActivity -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.ShowLiveActivity,
                    action.showLiveActivity
                )
            }

            is InstallerSettingsAction.ChangeBiometricAuth -> changeBiometricAuth(action.require)
            is InstallerSettingsAction.ChangeNotificationSuccessAutoClearSeconds -> viewModelScope.launch {
                updateSetting(
                    IntSetting.NotificationSuccessAutoClearSeconds,
                    action.seconds
                )
            }

            is InstallerSettingsAction.ChangeVersionCompareInSingleLine -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.DialogVersionCompareSingleLine,
                    action.compareInSingleLine
                )
            }

            is InstallerSettingsAction.ChangeSdkCompareInMultiLine -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.DialogSdkCompareMultiLine,
                    action.compareInMultiLine
                )
            }

            is InstallerSettingsAction.ChangeShowDialogInstallExtendedMenu -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.DialogShowExtendedMenu,
                    action.showMenu
                )
            }

            is InstallerSettingsAction.ChangeShowSuggestion -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.DialogShowIntelligentSuggestion,
                    action.showSuggestion
                )
            }

            is InstallerSettingsAction.ChangeShowDialogWhenPressingNotification -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.ShowDialogWhenPressingNotification,
                    action.showDialog
                )
            }

            is InstallerSettingsAction.ChangeAutoSilentInstall -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.DialogAutoSilentInstall,
                    action.autoSilentInstall
                )
            }

            is InstallerSettingsAction.ChangeShowDisableNotification -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.DialogDisableNotificationOnDismiss,
                    action.disable
                )
            }

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
        }
    }

    private fun changeBiometricAuth(biometricAuth: Boolean) = viewModelScope.launch {
        if (systemEnvProvider.authenticateBiometric(isInstaller = true)) {
            updateSetting(BooleanSetting.InstallerRequireBiometricAuth, biometricAuth)
        }
    }
}
