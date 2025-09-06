package com.rosan.installer.ui.page.main.settings.config.edit

import com.rosan.installer.data.settings.model.datastore.entity.NamedPackage
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity

data class EditViewState(
    val data: Data = Data.build(ConfigEntity.default),
    val managedInstallerPackages: List<NamedPackage> = emptyList()
) {
    data class Data(
        val name: String,
        val description: String,
        val authorizer: ConfigEntity.Authorizer,
        val customizeAuthorizer: String,
        val installMode: ConfigEntity.InstallMode,
        val declareInstaller: Boolean,
        val installer: String,
        val enableManualDexopt: Boolean,
        val forceDexopt: Boolean,
        val dexoptMode: ConfigEntity.DexoptMode,
        val autoDelete: Boolean,
        val displaySdk: Boolean,
        val forAllUser: Boolean,
        val allowTestOnly: Boolean,
        val allowDowngrade: Boolean,
        val allowRestrictedPermissions: Boolean,
        val bypassLowTargetSdk: Boolean,
        val allowAllRequestedPermissions: Boolean
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
            installer = if (this.declareInstaller) this.installer else null,
            enableManualDexopt = this.enableManualDexopt,
            dexoptMode = this.dexoptMode,
            forceDexopt = this.forceDexopt,
            autoDelete = this.autoDelete,
            displaySdk = this.displaySdk,
            forAllUser = this.forAllUser,
            allowTestOnly = this.allowTestOnly,
            allowDowngrade = this.allowDowngrade,
            allowRestrictedPermissions = this.allowRestrictedPermissions,
            bypassLowTargetSdk = this.bypassLowTargetSdk,
            allowAllRequestedPermissions = this.allowAllRequestedPermissions,
        )

        companion object {
            fun build(config: ConfigEntity): Data = Data(
                name = config.name,
                description = config.description,
                authorizer = config.authorizer,
                customizeAuthorizer = config.customizeAuthorizer,
                installMode = config.installMode,
                declareInstaller = config.installer != null,
                installer = config.installer ?: "",
                enableManualDexopt = config.enableManualDexopt,
                forceDexopt = config.forceDexopt,
                dexoptMode = config.dexoptMode,
                autoDelete = config.autoDelete,
                displaySdk = config.displaySdk,
                forAllUser = config.forAllUser,
                allowTestOnly = config.allowTestOnly,
                allowDowngrade = config.allowDowngrade,
                allowRestrictedPermissions = config.allowRestrictedPermissions,
                bypassLowTargetSdk = config.bypassLowTargetSdk,
                allowAllRequestedPermissions = config.allowAllRequestedPermissions
            )
        }
    }
}

