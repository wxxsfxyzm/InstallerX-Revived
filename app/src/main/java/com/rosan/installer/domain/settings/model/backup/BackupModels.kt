// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.model.backup

import androidx.annotation.StringRes
import com.rosan.installer.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object BackupConstants {
    /**
     * The current supported backup format version.
     *
     * Increment this value only when introducing breaking changes to the [BackupEnvelope]
     * structure that cannot be handled by the default JSON configuration.
     */
    const val CURRENT_FORMAT_VERSION = 1
}

@Serializable
data class BackupEnvelope(
    /**
     * The schema version of the backup format.
     * Used to ensure forward compatibility and handle breaking changes in the backup structure.
     */
    val formatVersion: Int = 1,
    val appVersionName: String = "",
    val appVersionCode: Long = 0L,
    val roomSchemaVersion: Int = 0,
    val createdAt: Long = 0L,
    val profiles: List<BackupProfile> = emptyList(),
    val scopes: List<BackupProfileScope> = emptyList(),
    val settings: List<BackupSettingEntry> = emptyList(),
    val history: List<BackupHistoryEntry> = emptyList()
)

@Serializable
data class BackupHistoryEntry(
    val operationType: String,
    val status: String,
    val packageName: String,
    val appLabel: String? = null,
    val timestamp: Long,
    val isFreshInstall: Boolean? = null,
    val versionChange: String,
    val oldVersionName: String? = null,
    val oldVersionCode: Long? = null,
    val newVersionName: String? = null,
    val newVersionCode: Long? = null,
    val sourcePaths: List<String> = emptyList(),
    val initiatorPackageName: String? = null,
    val installerPackageName: String? = null,
    val installMethod: String,
    val authorizer: String,
    val installMode: String,
    val errorSummary: String? = null,
    val errorType: String? = null
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
    val installRequesterMode: Int = 0,
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
    val restoredHistory: Int = 0,
    val ignoredSettings: Int,
    val rolledBack: Boolean = false
)

data class BackupRestorePreview(
    val envelope: BackupEnvelope,
    val profileCount: Int,
    val scopeCount: Int,
    val settingCount: Int,
    val historyCount: Int,
    val ignoredSettingCount: Int,
    val issues: List<BackupValidationIssue>
) {
    val warnings: List<BackupValidationIssue>
        get() = issues.filter { it.severity == BackupValidationSeverity.WARNING }
}

data class BackupValidationIssue(
    val severity: BackupValidationSeverity,
    val code: String,
    @param:StringRes val messageResId: Int,
    val args: List<String> = emptyList()
)

enum class BackupValidationSeverity {
    ERROR,
    WARNING
}

class BackupValidationException(
    val issues: List<BackupValidationIssue>
) : IllegalArgumentException(
    issues.firstOrNull()?.code ?: "backup_validation_failed"
) {
    constructor(@StringRes messageResId: Int, code: String = "backup_validation_failed") : this(
        listOf(
            BackupValidationIssue(
                severity = BackupValidationSeverity.ERROR,
                code = code,
                messageResId = messageResId
            )
        )
    )
}
