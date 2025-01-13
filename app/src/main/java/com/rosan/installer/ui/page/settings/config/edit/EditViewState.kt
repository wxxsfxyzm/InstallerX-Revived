package com.rosan.installer.ui.page.settings.config.edit

import com.rosan.installer.data.settings.model.room.entity.ConfigEntity

data class EditViewState(
    val data: Data = Data.build(ConfigEntity.default)
) {
    data class Data(
        val name: String,
        val description: String,
        val authorizer: ConfigEntity.Authorizer,
        val customizeAuthorizer: String,
        val installMode: ConfigEntity.InstallMode,
        val declareInstaller: Boolean,
        val installer: String,
        val forAllUser: Boolean,
        val allowTestOnly: Boolean,
        val allowDowngrade: Boolean,
        val autoDelete: Boolean,
    ) {
        val errorName = name.isEmpty()// || name == "Default"

        val authorizerCustomize = authorizer == ConfigEntity.Authorizer.Customize

        val errorCustomizeAuthorizer = authorizerCustomize && customizeAuthorizer.isEmpty()

        val errorInstaller = declareInstaller && installer.isEmpty()

        fun toConfigEntity(): ConfigEntity = ConfigEntity(
            name = this.name,
            description = this.description,
            authorizer = this.authorizer,
            customizeAuthorizer = if (this.authorizerCustomize)
                this.customizeAuthorizer
            else
                "",
            installMode = this.installMode,
            installer = if (this.declareInstaller)
                this.installer
            else
                null,
            forAllUser = this.forAllUser,
            allowTestOnly = this.allowTestOnly,
            allowDowngrade = this.allowDowngrade,
            autoDelete = this.autoDelete
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
                forAllUser = config.forAllUser,
                allowTestOnly = config.allowTestOnly,
                allowDowngrade = config.allowDowngrade,
                autoDelete = config.autoDelete
            )
        }
    }
}

