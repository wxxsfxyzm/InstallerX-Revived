// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.model.backup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BackupEnvelope(
    val formatVersion: Int = 1,
    val appVersionName: String = "",
    val appVersionCode: Long = 0L,
    val roomSchemaVersion: Int = 0,
    val createdAt: Long = 0L,
    val profiles: List<BackupProfile> = emptyList(),
    val scopes: List<BackupProfileScope> = emptyList(),
    val settings: List<BackupSettingEntry> = emptyList()
)

@Serializable
data class BackupProfile(
    val backupId: Long,
    val name: String = "Default",
    val description: String = "",
    val authorizer: String = "global",
    val customizeAuthorizer: String = "",
    val installMode: String = "dialog",
    val toastMode: Int = 0,
    val enableCustomizeInstallReason: Boolean = false,
    val installReason: Int = 0,
    val enableCustomizePackageSource: Boolean = false,
    val packageSource: Int = 1,
    val installRequester: String? = null,
    val installerMode: Int = 0,
    val installer: String? = null,
    val enableCustomizeUser: Boolean = false,
    val targetUserId: Int = 0,
    val enableManualDexopt: Boolean = false,
    val forceDexopt: Boolean = false,
    val dexoptMode: String = "speed-profile",
    val autoDelete: Boolean = false,
    val autoDeleteZip: Boolean = false,
    val displaySize: Boolean = false,
    val displaySdk: Boolean = false,
    val forAllUser: Boolean = false,
    val allowTestOnly: Boolean = false,
    val allowDowngrade: Boolean = false,
    val bypassLowTargetSdk: Boolean = false,
    val allowAllRequestedPermissions: Boolean = false,
    val allowSigMismatch: Boolean = false,
    val allowSigUnknown: Boolean = false,
    val requestUpdateOwnership: Boolean = false,
    val splitChooseAll: Boolean = false,
    val apkChooseAll: Boolean = false,
    val requireBiometricAuth: Boolean = false,
    val createdAt: Long = 0L,
    val modifiedAt: Long = 0L
)

@Serializable
data class BackupProfileScope(
    val backupId: Long,
    val packageName: String? = null,
    val createdAt: Long = 0L,
    val modifiedAt: Long = 0L
)

@Serializable
data class BackupSettingEntry(
    val key: String,
    val type: BackupSettingType,
    val value: String
)

@Serializable
enum class BackupSettingType {
    @SerialName("string")
    STRING,

    @SerialName("int")
    INT,

    @SerialName("boolean")
    BOOLEAN
}

data class RestoreResult(
    val restoredProfiles: Int,
    val restoredScopes: Int,
    val restoredSettings: Int,
    val ignoredSettings: Int,
    val rolledBack: Boolean = false
)
