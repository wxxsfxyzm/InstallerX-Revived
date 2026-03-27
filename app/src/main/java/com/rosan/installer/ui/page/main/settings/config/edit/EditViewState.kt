// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.config.edit

import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.ConfigModel
import com.rosan.installer.domain.settings.model.DexoptMode
import com.rosan.installer.domain.settings.model.InstallMode
import com.rosan.installer.domain.settings.model.InstallReason
import com.rosan.installer.domain.settings.model.NamedPackage
import com.rosan.installer.domain.settings.model.PackageSource

data class EditViewState(
    val data: Data = Data.build(ConfigModel.default),
    val originalData: Data? = null,
    val managedInstallerPackages: List<NamedPackage> = emptyList(),
    val availableUsers: Map<Int, String> = emptyMap(),
    val isCustomInstallRequesterEnabled: Boolean = false,

    // Global states integrated into the view state
    val globalAuthorizer: Authorizer = Authorizer.Global,
    val globalInstallMode: InstallMode = InstallMode.Global
) {
    // Computed property for unsaved changes
    val hasUnsavedChanges: Boolean
        get() = originalData != null && data != originalData

    // Return string resource IDs instead of resolved strings to keep State pure
    val activeErrorResIds: List<Int>
        get() {
            val errors = mutableListOf<Int>()
            with(data) {
                if (errorName) errors.add(R.string.config_error_name)
                if (errorCustomizeAuthorizer) errors.add(R.string.config_error_customize_authorizer)
                if (errorInstaller) errors.add(R.string.config_error_installer)
                if (errorInstallRequester) errors.add(R.string.config_error_package_not_found)
            }
            return errors
        }

    val hasErrors: Boolean
        get() = activeErrorResIds.isNotEmpty()

    data class Data(
        val name: String,
        val description: String,
        val authorizer: Authorizer,
        val customizeAuthorizer: String,
        val installMode: InstallMode,
        val showToast: Boolean,
        val enableCustomizePackageSource: Boolean,
        val packageSource: PackageSource,
        val enableCustomizeInstallReason: Boolean,
        val installReason: InstallReason,
        val enableCustomizeInstallRequester: Boolean,
        val installRequester: String,
        val installRequesterUid: Int? = null,
        val declareInstaller: Boolean,
        val installer: String,
        val enableCustomizeUser: Boolean,
        val targetUserId: Int,
        val enableManualDexopt: Boolean,
        val forceDexopt: Boolean,
        val dexoptMode: DexoptMode,
        val autoDelete: Boolean,
        val autoDeleteZip: Boolean,
        val displaySdk: Boolean,
        val displaySize: Boolean,
        val forAllUser: Boolean,
        val allowTestOnly: Boolean,
        val allowDowngrade: Boolean,
        val bypassLowTargetSdk: Boolean,
        val allowAllRequestedPermissions: Boolean,
        val requestUpdateOwnership: Boolean,
        val splitChooseAll: Boolean,
        val apkChooseAll: Boolean
    ) {
        val errorName = name.isEmpty()// || name == "Default"
        val authorizerCustomize = authorizer == Authorizer.Customize
        val errorCustomizeAuthorizer = authorizerCustomize && customizeAuthorizer.isEmpty()
        val errorInstaller = declareInstaller && installer.isEmpty()
        val errorInstallRequester = enableCustomizeInstallRequester && (installRequester.isEmpty() || installRequesterUid == null)

        fun toConfigModel(): ConfigModel = ConfigModel(
            name = this.name,
            description = this.description,
            authorizer = this.authorizer,
            customizeAuthorizer = if (this.authorizerCustomize) this.customizeAuthorizer else "",
            installMode = this.installMode,
            showToast = this.showToast,
            enableCustomizeInstallReason = this.enableCustomizeInstallReason,
            installReason = this.installReason,
            enableCustomizePackageSource = this.enableCustomizePackageSource,
            packageSource = this.packageSource,
            installRequester = if (this.enableCustomizeInstallRequester) this.installRequester else null,
            installer = if (this.declareInstaller) this.installer else null,
            enableCustomizeUser = this.enableCustomizeUser,
            targetUserId = this.targetUserId,
            enableManualDexopt = this.enableManualDexopt,
            dexoptMode = this.dexoptMode,
            forceDexopt = this.forceDexopt,
            autoDelete = this.autoDelete,
            autoDeleteZip = this.autoDeleteZip,
            displaySdk = this.displaySdk,
            displaySize = this.displaySize,
            forAllUser = this.forAllUser,
            allowTestOnly = this.allowTestOnly,
            allowDowngrade = this.allowDowngrade,
            bypassLowTargetSdk = this.bypassLowTargetSdk,
            allowAllRequestedPermissions = this.allowAllRequestedPermissions,
            requestUpdateOwnership = this.requestUpdateOwnership,
            splitChooseAll = this.splitChooseAll,
            apkChooseAll = this.apkChooseAll
        )

        companion object {
            fun build(config: ConfigModel): Data = Data(
                name = config.name,
                description = config.description,
                authorizer = config.authorizer,
                customizeAuthorizer = config.customizeAuthorizer,
                showToast = config.showToast,
                installMode = config.installMode,
                enableCustomizePackageSource = config.enableCustomizePackageSource,
                enableCustomizeInstallReason = config.enableCustomizeInstallReason,
                installReason = config.installReason,
                packageSource = config.packageSource,
                enableCustomizeInstallRequester = config.installRequester != null,
                installRequester = config.installRequester ?: "",
                installRequesterUid = null,
                declareInstaller = config.installer != null,
                installer = config.installer ?: "",
                enableCustomizeUser = config.enableCustomizeUser,
                targetUserId = config.targetUserId,
                enableManualDexopt = config.enableManualDexopt,
                forceDexopt = config.forceDexopt,
                dexoptMode = config.dexoptMode,
                autoDelete = config.autoDelete,
                autoDeleteZip = config.autoDeleteZip,
                displaySdk = config.displaySdk,
                displaySize = config.displaySize,
                forAllUser = config.forAllUser,
                allowTestOnly = config.allowTestOnly,
                allowDowngrade = config.allowDowngrade,
                bypassLowTargetSdk = config.bypassLowTargetSdk,
                allowAllRequestedPermissions = config.allowAllRequestedPermissions,
                requestUpdateOwnership = config.requestUpdateOwnership,
                splitChooseAll = config.splitChooseAll,
                apkChooseAll = config.apkChooseAll
            )
        }
    }
}
