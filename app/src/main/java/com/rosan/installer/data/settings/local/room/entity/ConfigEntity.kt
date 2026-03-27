// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.data.settings.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.DexoptMode
import com.rosan.installer.domain.settings.model.InstallMode
import com.rosan.installer.domain.settings.model.InstallReason
import com.rosan.installer.domain.settings.model.PackageSource

@Entity(
    tableName = "config",
    indices = []
)
data class ConfigEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") var id: Long = 0L,
    @ColumnInfo(name = "name", defaultValue = "'Default'") var name: String = "Default",
    @ColumnInfo(name = "description") var description: String,
    @ColumnInfo(name = "authorizer") var authorizer: Authorizer,
    @ColumnInfo(name = "customize_authorizer") var customizeAuthorizer: String,
    @ColumnInfo(name = "install_mode") var installMode: InstallMode,
    @ColumnInfo(name = "show_toast", defaultValue = "0") var showToast: Boolean = true,
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
    @ColumnInfo(name = "auto_delete", defaultValue = "0") var autoDelete: Boolean,
    @ColumnInfo(name = "auto_delete_zip", defaultValue = "0") var autoDeleteZip: Boolean = false,
    @ColumnInfo(name = "display_size", defaultValue = "0") var displaySize: Boolean = false,
    @ColumnInfo(name = "display_sdk", defaultValue = "0") var displaySdk: Boolean = false,
    @ColumnInfo(name = "for_all_user", defaultValue = "0") var forAllUser: Boolean = false,
    @ColumnInfo(name = "allow_test_only", defaultValue = "0") var allowTestOnly: Boolean = false,
    @ColumnInfo(name = "allow_downgrade", defaultValue = "0") var allowDowngrade: Boolean = false,
    @ColumnInfo(name = "bypass_low_target_sdk", defaultValue = "0") var bypassLowTargetSdk: Boolean = false,
    @ColumnInfo(name = "allow_all_requested_permissions", defaultValue = "0") var allowAllRequestedPermissions: Boolean = false,
    @ColumnInfo(name = "request_update_ownership", defaultValue = "0") var requestUpdateOwnership: Boolean = false,
    @ColumnInfo(name = "split_choose_all", defaultValue = "0") var splitChooseAll: Boolean = false,
    @ColumnInfo(name = "apk_choose_all", defaultValue = "0") var apkChooseAll: Boolean = false,
    @ColumnInfo(name = "created_at") var createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "modified_at") var modifiedAt: Long = System.currentTimeMillis(),
) {
    // Variable to store the installation flags
    @Ignore
    var installFlags: Int = 0

    @Ignore
    var bypassBlacklistInstallSetByUser: Boolean = false

    // Variable to store the uninstallation flags
    @Ignore
    var uninstallFlags: Int = 0

    // Variable to store the calling UID
    @Ignore
    var callingFromUid: Int? = null
}
