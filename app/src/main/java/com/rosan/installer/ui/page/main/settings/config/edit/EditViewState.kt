package com.rosan.installer.ui.page.main.settings.config.edit

import com.rosan.installer.data.settings.model.datastore.entity.NamedPackage
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity

data class EditViewState(
    val data: Data = Data.build(ConfigEntity.default),
    val managedInstallerPackages: List<NamedPackage> = emptyList(),
    val availableUsers: Map<Int, String> = emptyMap()
) {
    data class Data(
        val name: String,
        val description: String,
        val authorizer: ConfigEntity.Authorizer,
        val customizeAuthorizer: String,
        val installMode: ConfigEntity.InstallMode,
        val enableCustomizePackageSource: Boolean,
        val packageSource: ConfigEntity.PackageSource,
        val declareInstaller: Boolean,
        val installer: String,
        val enableCustomizeUser: Boolean,
        val targetUserId: Int,
        val enableManualDexopt: Boolean,
        val forceDexopt: Boolean,
        val dexoptMode: ConfigEntity.DexoptMode,
        val autoDelete: Boolean,
        val displaySdk: Boolean,
        val displaySize: Boolean,
        val forAllUser: Boolean,
        val allowTestOnly: Boolean,
        val allowDowngrade: Boolean,
        val bypassLowTargetSdk: Boolean,
        val allowAllRequestedPermissions: Boolean,
        val splitChooseAll: Boolean
    ) {
        val errorName = name.isEmpty()// || name == "Default"

        val authorizerCustomize = authorizer == ConfigEntity.Authorizer.Customize

        val errorCustomizeAuthorizer = authorizerCustomize && customizeAuthorizer.isEmpty()

        val errorInstaller = declareInstaller && installer.isEmpty()

        fun toConfigEntity(): ConfigEntity = ConfigEntity(
            name = this.name,
            description = this.description,
            authorizer = this.authorizer,
            customizeAuthorizer = if (this.authorizerCustomize) this.customizeAuthorizer else "",
            installMode = this.installMode,
            enableCustomizePackageSource = this.enableCustomizePackageSource,
            packageSource = this.packageSource,
            installer = if (this.declareInstaller) this.installer else null,
            enableCustomizeUser = this.enableCustomizeUser,
            targetUserId = this.targetUserId,
            enableManualDexopt = this.enableManualDexopt,
            dexoptMode = this.dexoptMode,
            forceDexopt = this.forceDexopt,
            autoDelete = this.autoDelete,
            displaySdk = this.displaySdk,
            displaySize = this.displaySize,
            forAllUser = this.forAllUser,
            allowTestOnly = this.allowTestOnly,
            allowDowngrade = this.allowDowngrade,
            bypassLowTargetSdk = this.bypassLowTargetSdk,
            allowAllRequestedPermissions = this.allowAllRequestedPermissions,
            splitChooseAll = this.splitChooseAll
        )

        companion object {
            fun build(config: ConfigEntity): Data = Data(
                name = config.name,
                description = config.description,
                authorizer = config.authorizer,
                customizeAuthorizer = config.customizeAuthorizer,
                installMode = config.installMode,
                enableCustomizePackageSource = config.enableCustomizePackageSource,
                packageSource = config.packageSource,
                declareInstaller = config.installer != null,
                installer = config.installer ?: "",
                enableCustomizeUser = config.enableCustomizeUser,
                targetUserId = config.targetUserId,
                enableManualDexopt = config.enableManualDexopt,
                forceDexopt = config.forceDexopt,
                dexoptMode = config.dexoptMode,
                autoDelete = config.autoDelete,
                displaySdk = config.displaySdk,
                displaySize = config.displaySize,
                forAllUser = config.forAllUser,
                allowTestOnly = config.allowTestOnly,
                allowDowngrade = config.allowDowngrade,
                bypassLowTargetSdk = config.bypassLowTargetSdk,
                allowAllRequestedPermissions = config.allowAllRequestedPermissions,
                splitChooseAll = config.splitChooseAll
            )
        }
    }
}

