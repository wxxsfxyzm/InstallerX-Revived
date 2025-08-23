package com.rosan.installer.data.settings.model.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import org.koin.core.component.KoinComponent

@Entity(
    tableName = "config",
    indices = [
    ]
)
data class ConfigEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0L,
    @ColumnInfo(name = "name", defaultValue = "'Default'") var name: String = "Default",
    @ColumnInfo(name = "description") var description: String,
    @ColumnInfo(name = "authorizer") var authorizer: Authorizer,
    @ColumnInfo(name = "customize_authorizer") var customizeAuthorizer: String,
    @ColumnInfo(name = "install_mode") var installMode: InstallMode,
    @ColumnInfo(name = "installer") var installer: String?,
    @ColumnInfo(name = "enable_manual_dexopt", defaultValue = "0") var enableManualDexopt: Boolean = false,
    @ColumnInfo(
        name = "dexopt_mode",
        defaultValue = "'speed-profile'"
    ) var dexoptMode: DexoptMode = DexoptMode.SpeedProfile,
    @ColumnInfo(name = "auto_delete") var autoDelete: Boolean,
    @ColumnInfo(name = "display_sdk", defaultValue = "0") var displaySdk: Boolean = false,
    @ColumnInfo(name = "for_all_user") var forAllUser: Boolean,
    @ColumnInfo(name = "allow_test_only") var allowTestOnly: Boolean,
    @ColumnInfo(name = "allow_downgrade") var allowDowngrade: Boolean,
    @ColumnInfo(name = "allow_restricted_permissions", defaultValue = "0") var allowRestrictedPermissions: Boolean,
    @ColumnInfo(name = "bypass_low_target_sdk", defaultValue = "0") var bypassLowTargetSdk: Boolean,
    @ColumnInfo(name = "allow_all_requested_permissions", defaultValue = "0") var allowAllRequestedPermissions: Boolean,
    @ColumnInfo(name = "created_at") var createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "modified_at") var modifiedAt: Long = System.currentTimeMillis(),
) {
    companion object : KoinComponent {
        var default = ConfigEntity(
            description = "",
            authorizer = Authorizer.Global,
            customizeAuthorizer = "",
            installMode = InstallMode.Global,
            installer = null,
            enableManualDexopt = false,
            dexoptMode = DexoptMode.SpeedProfile,
            autoDelete = false,
            forAllUser = false,
            allowTestOnly = false,
            allowDowngrade = false,
            allowRestrictedPermissions = false,
            bypassLowTargetSdk = false,
            allowAllRequestedPermissions = false,
        )

        val XiaomiDefault = ConfigEntity(
            description = "",
            authorizer = Authorizer.Global,
            customizeAuthorizer = "",
            installMode = InstallMode.Dialog,
            installer = "com.miui.packageinstaller",
            enableManualDexopt = false,
            dexoptMode = DexoptMode.SpeedProfile,
            autoDelete = false,
            forAllUser = false,
            allowTestOnly = false,
            allowDowngrade = false,
            allowRestrictedPermissions = false,
            bypassLowTargetSdk = false,
            allowAllRequestedPermissions = false,
        )
    }

    @Ignore
    var installFlags: Int = 0

    @Ignore
    var uninstallFlags: Int = 0

    val isCustomizeAuthorizer: Boolean
        get() = authorizer == Authorizer.Customize

    enum class Authorizer(val value: String) {
        Global("global"),
        None("none"),
        Root("root"),
        Shizuku("shizuku"),
        Dhizuku("dhizuku"),
        Customize("customize");
    }

    enum class InstallMode(val value: String) {
        Global("global"),
        Dialog("dialog"),
        AutoDialog("auto_dialog"),
        Notification("notification"),
        AutoNotification("auto_notification"),
        Ignore("ignore");
    }

    enum class DexoptMode(val value: String) {
        Verify("verify"),
        SpeedProfile("speed-profile"),
        Speed("speed"),
        Everything("everything");
    }

    enum class Analyser(val value: String) {
        R0s("r0s"),
        System("system");
    }
}
