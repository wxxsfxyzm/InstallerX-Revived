package com.rosan.installer.ui.page.main.settings.config.edit

import com.rosan.installer.data.settings.model.datastore.entity.NamedPackage
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity

data class EditViewState(
    val data: Data = Data.build(ConfigEntity.default),
    val managedInstallerPackages: List<NamedPackage> = emptyList(),
    val availableUsers: Map<Int, String> = emptyMap(),
    val isCustomInstallRequesterEnabled: Boolean = false
) {
    data class Data(
        val name: String,
        val description: String,
        val authorizer: ConfigEntity.Authorizer,
        val customizeAuthorizer: String,
        val installMode: ConfigEntity.InstallMode,
        val enableCustomizePackageSource: Boolean,
        val packageSource: ConfigEntity.PackageSource,
        val enableCustomizeInstallReason: Boolean,
        val installReason: ConfigEntity.InstallReason,
        val enableCustomizeInstallRequester: Boolean,
        val installRequester: String,
        val installRequesterUid: Int? = null,
        val declareInstaller: Boolean,
        val installer: String,
        val enableCustomizeUser: Boolean,
        val targetUserId: Int,
        val enableManualDexopt: Boolean,
        val forceDexopt: Boolean,
        val dexoptMode: ConfigEntity.DexoptMode,
        val autoDelete: Boolean,
        val autoDeleteZip: Boolean,
        val displaySdk: Boolean,
        val displaySize: Boolean,
        val forAllUser: Boolean,
        val allowTestOnly: Boolean,
        val allowDowngrade: Boolean,
        val bypassLowTargetSdk: Boolean,
        val allowAllRequestedPermissions: Boolean,
        val splitChooseAll: Boolean,
        val apkChooseAll: Boolean
    ) {
        val errorName = name.isEmpty()// || name == "Default"

        val authorizerCustomize = authorizer == ConfigEntity.Authorizer.Customize

        val errorCustomizeAuthorizer = authorizerCustomize && customizeAuthorizer.isEmpty()

        val errorInstaller = declareInstaller && installer.isEmpty()

        // Validation: Error if enabled but package name is empty OR (package name is not empty but UID not found)
        val errorInstallRequester = enableCustomizeInstallRequester && (installRequester.isEmpty() || installRequesterUid == null)

        fun toConfigEntity(): ConfigEntity = ConfigEntity(
            name = this.name,
            description = this.description,
            authorizer = this.authorizer,
            customizeAuthorizer = if (this.authorizerCustomize) this.customizeAuthorizer else "",
            installMode = this.installMode,
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
            splitChooseAll = this.splitChooseAll,
            apkChooseAll = this.apkChooseAll
        )

        companion object {
            fun build(config: ConfigEntity): Data = Data(
                name = config.name,
                description = config.description,
                authorizer = config.authorizer,
                customizeAuthorizer = config.customizeAuthorizer,
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
                splitChooseAll = config.splitChooseAll,
                apkChooseAll = config.apkChooseAll
            )
        }
    }
}

