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
    @ColumnInfo(name = "id") var id: Long = 0L,
    @ColumnInfo(name = "name", defaultValue = "'Default'") var name: String = "Default",
    @ColumnInfo(name = "description") var description: String,
    @ColumnInfo(name = "authorizer") var authorizer: Authorizer,
    @ColumnInfo(name = "customize_authorizer") var customizeAuthorizer: String,
    @ColumnInfo(name = "install_mode") var installMode: InstallMode,
    @ColumnInfo(name = "enable_customize_install_reason", defaultValue = "0")
    var enableCustomizeInstallReason: Boolean = false,
    @ColumnInfo(
        name = "install_reason",
        defaultValue = "0" // Corresponds to INSTALL_REASON_UNKNOWN
    ) var installReason: InstallReason = InstallReason.UNKNOWN,
    @ColumnInfo(name = "enable_customize_package_source", defaultValue = "0")
    var enableCustomizePackageSource: Boolean = false,
    @ColumnInfo(
        name = "package_source",
        defaultValue = "1" // Corresponds to PACKAGE_SOURCE_OTHER
    ) var packageSource: PackageSource = PackageSource.OTHER,
    @ColumnInfo(name = "install_requester") var installRequester: String? = null,
    @ColumnInfo(name = "installer") var installer: String?,
    @ColumnInfo(name = "enable_customize_user", defaultValue = "0") var enableCustomizeUser: Boolean = false,
    @ColumnInfo(name = "target_user_id", defaultValue = "0") var targetUserId: Int = 0,
    @ColumnInfo(name = "enable_manual_dexopt", defaultValue = "0") var enableManualDexopt: Boolean = false,
    @ColumnInfo(name = "force_dexopt", defaultValue = "0") var forceDexopt: Boolean = false,
    @ColumnInfo(
        name = "dexopt_mode",
        defaultValue = "'speed-profile'"
    ) var dexoptMode: DexoptMode = DexoptMode.SpeedProfile,
    @ColumnInfo(name = "auto_delete") var autoDelete: Boolean,
    @ColumnInfo(name = "auto_delete_zip", defaultValue = "0") var autoDeleteZip: Boolean = false,
    @ColumnInfo(name = "display_size", defaultValue = "0") var displaySize: Boolean = false,
    @ColumnInfo(name = "display_sdk", defaultValue = "0") var displaySdk: Boolean = false,
    @ColumnInfo(name = "for_all_user") var forAllUser: Boolean,
    @ColumnInfo(name = "allow_test_only") var allowTestOnly: Boolean,
    @ColumnInfo(name = "allow_downgrade") var allowDowngrade: Boolean,
    @ColumnInfo(name = "bypass_low_target_sdk", defaultValue = "0") var bypassLowTargetSdk: Boolean,
    @ColumnInfo(name = "allow_all_requested_permissions", defaultValue = "0") var allowAllRequestedPermissions: Boolean,
    @ColumnInfo(name = "split_choose_all", defaultValue = "0") var splitChooseAll: Boolean,
    @ColumnInfo(name = "apk_choose_all", defaultValue = "0") var apkChooseAll: Boolean,
    @ColumnInfo(name = "created_at") var createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "modified_at") var modifiedAt: Long = System.currentTimeMillis(),
) {
    companion object : KoinComponent {
        var default = ConfigEntity(
            description = "",
            authorizer = Authorizer.Global,
            customizeAuthorizer = "",
            installMode = InstallMode.Global,
            enableCustomizeInstallReason = false,
            installReason = InstallReason.UNKNOWN,
            enableCustomizePackageSource = false,
            packageSource = PackageSource.OTHER,
            installer = null,
            enableCustomizeUser = false,
            targetUserId = 0,
            enableManualDexopt = false,
            forceDexopt = false,
            dexoptMode = DexoptMode.SpeedProfile,
            autoDelete = false,
            autoDeleteZip = false,
            displaySize = false,
            displaySdk = false,
            forAllUser = false,
            allowTestOnly = false,
            allowDowngrade = false,
            bypassLowTargetSdk = false,
            allowAllRequestedPermissions = false,
            splitChooseAll = false,
            apkChooseAll = false
        )

        val XiaomiDefault = ConfigEntity(
            description = "",
            authorizer = Authorizer.Global,
            customizeAuthorizer = "",
            installMode = InstallMode.Global,
            enableCustomizeInstallReason = false,
            installReason = InstallReason.UNKNOWN,
            enableCustomizePackageSource = false,
            packageSource = PackageSource.OTHER,
            installer = "com.miui.packageinstaller",
            enableCustomizeUser = false,
            targetUserId = 0,
            enableManualDexopt = false,
            forceDexopt = false,
            dexoptMode = DexoptMode.SpeedProfile,
            autoDelete = false,
            autoDeleteZip = false,
            displaySize = false,
            displaySdk = false,
            forAllUser = false,
            allowTestOnly = false,
            allowDowngrade = false,
            bypassLowTargetSdk = false,
            allowAllRequestedPermissions = false,
            splitChooseAll = false,
            apkChooseAll = false
        )
    }

    @Ignore
    var installFlags: Int = 0

    @Ignore
    var bypassBlacklistInstallSetByUser: Boolean = false

    @Ignore
    var uninstallFlags: Int = 0

    /**
     * 安装/卸载调用的来源方
     */
    @Ignore
    var callingFromUid: Int? = null

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

    /**
     * Define Install Reasons,
     * Sync with Android's Install Reason
     *
     * @see android.content.pm.PackageInstaller.SessionParams.setInstallReason
     */
    enum class InstallReason(val value: Int) {
        /**
         * Code indicating that the reason for installing this package is unknown.
         * @see android.content.pm.PackageManager.INSTALL_REASON_UNKNOWN
         */
        UNKNOWN(0),

        /**
         * Code indicating that this package was installed due to enterprise policy.
         * @see android.content.pm.PackageManager.INSTALL_REASON_POLICY
         */
        POLICY(1),

        /**
         * Code indicating that this package was installed as part of restoring from another device.
         * @see android.content.pm.PackageManager.INSTALL_REASON_DEVICE_RESTORE
         */
        DEVICE_RESTORE(2),

        /**
         * Code indicating that this package was installed as part of device setup.
         * @see android.content.pm.PackageManager.INSTALL_REASON_DEVICE_SETUP
         */
        DEVICE_SETUP(3),

        /**
         * Code indicating that the package installation was initiated by the user.
         * NOTE: this will cause launcher release desktop icon
         * @see android.content.pm.PackageManager.INSTALL_REASON_USER
         */
        USER(4);

        companion object {
            fun fromInt(value: Int) = entries.find { it.value == value } ?: UNKNOWN
        }
    }

    enum class PackageSource(val value: Int) {
        UNSPECIFIED(0),     // Corresponds to PACKAGE_SOURCE_UNSPECIFIED
        OTHER(1),           // Corresponds to PACKAGE_SOURCE_OTHER
        STORE(2),           // Corresponds to PACKAGE_SOURCE_STORE
        LOCAL_FILE(3),      // Corresponds to PACKAGE_SOURCE_LOCAL_FILE
        DOWNLOADED_FILE(4); // Corresponds to PACKAGE_SOURCE_DOWNLOADED_FILE

        companion object {
            fun fromInt(value: Int) = entries.find { it.value == value } ?: OTHER
        }
    }
}
