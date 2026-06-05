// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.settings.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.rosan.installer.BuildConfig
import com.rosan.installer.R
import com.rosan.installer.data.settings.local.datastore.AppDataStore
import com.rosan.installer.data.settings.local.room.INSTALLER_ROOM_SCHEMA_VERSION
import com.rosan.installer.data.settings.local.room.dao.AppDao
import com.rosan.installer.data.settings.local.room.dao.ConfigDao
import com.rosan.installer.data.settings.local.room.dao.OperationHistoryDao
import com.rosan.installer.data.settings.local.room.entity.AppEntity
import com.rosan.installer.data.settings.local.room.entity.ConfigEntity
import com.rosan.installer.data.settings.local.room.entity.OperationHistoryEntity
import com.rosan.installer.domain.history.model.InstallMethod
import com.rosan.installer.domain.history.model.OperationStatus
import com.rosan.installer.domain.history.model.OperationType
import com.rosan.installer.domain.history.model.VersionChange
import com.rosan.installer.domain.settings.model.backup.BackupConstants
import com.rosan.installer.domain.settings.model.backup.BackupEnvelope
import com.rosan.installer.domain.settings.model.backup.BackupHistoryEntry
import com.rosan.installer.domain.settings.model.backup.BackupProfile
import com.rosan.installer.domain.settings.model.backup.BackupProfileScope
import com.rosan.installer.domain.settings.model.backup.BackupRestorePreview
import com.rosan.installer.domain.settings.model.backup.BackupSettingEntry
import com.rosan.installer.domain.settings.model.backup.BackupSettingType
import com.rosan.installer.domain.settings.model.backup.BackupValidationException
import com.rosan.installer.domain.settings.model.backup.BackupValidationIssue
import com.rosan.installer.domain.settings.model.backup.BackupValidationSeverity
import com.rosan.installer.domain.settings.model.backup.RestoreResult
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.DexoptMode
import com.rosan.installer.domain.settings.model.config.InstallRequesterMode
import com.rosan.installer.domain.settings.model.config.InstallMode
import com.rosan.installer.domain.settings.model.config.InstallReason
import com.rosan.installer.domain.settings.model.config.InstallerMode
import com.rosan.installer.domain.settings.model.config.PackageSource
import com.rosan.installer.domain.settings.model.config.ToastMode
import com.rosan.installer.domain.settings.repository.BackupRepository
import kotlinx.coroutines.flow.first
import timber.log.Timber

class BackupRepositoryImpl(
    private val configDao: ConfigDao,
    private val appDao: AppDao,
    private val historyDao: OperationHistoryDao,
    private val appDataStore: AppDataStore,
    private val dataStore: DataStore<Preferences>
) : BackupRepository {
    override suspend fun exportBackup(): BackupEnvelope {
        val configs = configDao.all()
        val apps = appDao.allSuspend()
        val history = historyDao.all()
        val preferences = appDataStore.data.first()

        return BackupEnvelope(
            formatVersion = BackupConstants.CURRENT_FORMAT_VERSION,
            appVersionName = BuildConfig.VERSION_NAME,
            appVersionCode = BuildConfig.VERSION_CODE.toLong(),
            roomSchemaVersion = INSTALLER_ROOM_SCHEMA_VERSION,
            createdAt = System.currentTimeMillis(),
            profiles = configs.map { it.toBackupProfile() },
            scopes = apps.map { it.toBackupProfileScope() },
            settings = preferences.toBackupSettings(),
            history = history.map { it.toBackupHistoryEntry() }
        )
    }

    override fun validateBackup(envelope: BackupEnvelope): BackupRestorePreview {
        val issues = mutableListOf<BackupValidationIssue>()

        if (envelope.formatVersion !in 1..BackupConstants.CURRENT_FORMAT_VERSION) {
            issues += errorIssue(
                code = "unsupported_format_version",
                messageResId = R.string.backup_settings_validation_unsupported_format,
                envelope.formatVersion.toString(),
                BackupConstants.CURRENT_FORMAT_VERSION.toString()
            )
        }

        if (envelope.appVersionCode > BuildConfig.VERSION_CODE.toLong()) {
            issues += warningIssue(
                code = "future_app_version",
                messageResId = R.string.backup_settings_validation_future_app_version,
                envelope.appVersionName.ifBlank { envelope.appVersionCode.toString() }
            )
        }

        if (envelope.roomSchemaVersion > INSTALLER_ROOM_SCHEMA_VERSION) {
            issues += warningIssue(
                code = "future_room_schema",
                messageResId = R.string.backup_settings_validation_future_room_schema,
                envelope.roomSchemaVersion.toString(),
                INSTALLER_ROOM_SCHEMA_VERSION.toString()
            )
        }

        if (
            envelope.profiles.isEmpty() &&
            envelope.scopes.isEmpty() &&
            envelope.settings.isEmpty() &&
            envelope.history.isEmpty()
        ) {
            issues += errorIssue(
                code = "empty_backup",
                messageResId = R.string.backup_settings_validation_empty
            )
        }

        val profileIds = linkedSetOf<Long>()
        envelope.profiles.forEach { profile ->
            if (profile.backupId <= 0L) {
                issues += errorIssue(
                    code = "invalid_profile_id",
                    messageResId = R.string.backup_settings_validation_invalid_profile_id,
                    profile.backupId.toString()
                )
            } else if (!profileIds.add(profile.backupId)) {
                issues += errorIssue(
                    code = "duplicate_profile_id",
                    messageResId = R.string.backup_settings_validation_duplicate_profile_id,
                    profile.backupId.toString()
                )
            }

            validateProfile(profile, issues)
        }

        envelope.scopes.mapNotNull { it.packageName?.takeIf { packageName -> packageName.isNotBlank() } }
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .keys
            .forEach { packageName ->
                issues += errorIssue(
                    code = "duplicate_scope_package",
                    messageResId = R.string.backup_settings_validation_duplicate_scope_package,
                    packageName
                )
            }

        envelope.scopes.forEach { scope ->
            if (scope.backupId !in profileIds) {
                issues += warningIssue(
                    code = "missing_scope_profile",
                    messageResId = R.string.backup_settings_validation_missing_scope_profile,
                    scope.packageName.orEmpty().ifBlank { scope.backupId.toString() }
                )
            }
        }

        val validSettings = envelope.settings.count { entry ->
            val supportedKey = AppDataStore.supportedKeys[entry.key]
            val valid = supportedKey != null && entry.matchesSupportedSetting(supportedKey.type)
            if (!valid) {
                issues += warningIssue(
                    code = "ignored_setting",
                    messageResId = R.string.backup_settings_validation_ignored_setting,
                    entry.key
                )
            }
            valid
        }

        envelope.history.forEach { history ->
            validateHistory(history, issues)
        }

        if (issues.any { it.severity == BackupValidationSeverity.ERROR }) {
            throw BackupValidationException(issues)
        }

        return BackupRestorePreview(
            envelope = envelope,
            profileCount = envelope.profiles.size,
            scopeCount = envelope.scopes.count { it.backupId in profileIds },
            settingCount = validSettings,
            historyCount = envelope.history.size,
            ignoredSettingCount = envelope.settings.size - validSettings,
            issues = issues
        )
    }

    override suspend fun restoreBackup(envelope: BackupEnvelope): RestoreResult {
        validateBackup(envelope)

        val importPlan = envelope.toImportPlan()
        val snapshot = createPreRestoreSnapshot()

        return try {
            applyImportPlan(importPlan)
        } catch (exception: Throwable) {
            rollback(snapshot)
            throw exception
        }
    }

    private suspend fun createPreRestoreSnapshot(): PreRestoreSnapshot =
        PreRestoreSnapshot(
            configs = configDao.all(),
            apps = appDao.allSuspend(),
            history = historyDao.all(),
            settings = appDataStore.data.first().toBackupSettings()
        )

    private suspend fun applyImportPlan(importPlan: ImportPlan): RestoreResult {
        appDao.deleteAll()
        configDao.deleteAll()
        historyDao.clear()

        val configIdMap = linkedMapOf<Long, Long>()
        importPlan.profiles.forEach { profile ->
            val newConfigId = configDao.insertAndReturnId(profile.entity)
            configIdMap[profile.backupId] = newConfigId
        }

        val restoredApps = importPlan.scopes.mapNotNull { scope ->
            val configId = configIdMap[scope.backupId] ?: return@mapNotNull null
            scope.entity.copy(configId = configId)
        }
        appDao.insertAll(restoredApps)

        importPlan.history.forEach { entry ->
            historyDao.insert(entry.toEntity())
        }

        val settingsResult = writeSettings(importPlan.settings)

        return RestoreResult(
            restoredProfiles = importPlan.profiles.size,
            restoredScopes = restoredApps.size,
            restoredSettings = settingsResult.restored,
            restoredHistory = importPlan.history.size,
            ignoredSettings = settingsResult.ignored,
            rolledBack = false
        )
    }

    private suspend fun rollback(snapshot: PreRestoreSnapshot) {
        try {
            appDao.deleteAll()
            configDao.deleteAll()
            historyDao.clear()

            val configIdMap = linkedMapOf<Long, Long>()
            snapshot.configs.forEach { config ->
                val oldConfigId = config.id
                val newConfigId = configDao.insertAndReturnId(config)
                configIdMap[oldConfigId] = newConfigId
            }

            val restoredApps = snapshot.apps.mapNotNull { app ->
                val configId = configIdMap[app.configId] ?: return@mapNotNull null
                app.copy(configId = configId)
            }
            appDao.insertAll(restoredApps)

            snapshot.history.forEach { entity ->
                historyDao.insert(entity.copy(id = 0L))
            }

            writeSettings(snapshot.settings)
        } catch (rollbackException: Throwable) {
            Timber.e(rollbackException, "Failed to roll back backup restore state.")
        }
    }

    private suspend fun writeSettings(entries: List<BackupSettingEntry>): SettingsWriteResult {
        var restored = 0
        var ignored = 0

        dataStore.edit { preferences ->
            preferences.clear()
            entries.forEach { entry ->
                if (preferences.writeSupportedSetting(entry)) {
                    restored++
                } else {
                    ignored++
                }
            }
        }

        return SettingsWriteResult(restored = restored, ignored = ignored)
    }

    private fun Preferences.toBackupSettings(): List<BackupSettingEntry> {
        val supportedKeys = AppDataStore.supportedKeys
        return asMap().mapNotNull { (key, value) ->
            val supportedKey = supportedKeys[key.name] ?: return@mapNotNull null
            when (supportedKey.type) {
                AppDataStore.PreferenceValueType.STRING -> (value as? String)?.let {
                    BackupSettingEntry(key.name, BackupSettingType.STRING, it)
                }

                AppDataStore.PreferenceValueType.INT -> (value as? Int)?.let {
                    BackupSettingEntry(key.name, BackupSettingType.INT, it.toString())
                }

                AppDataStore.PreferenceValueType.BOOLEAN -> (value as? Boolean)?.let {
                    BackupSettingEntry(key.name, BackupSettingType.BOOLEAN, it.toString())
                }
            }
        }
    }

    private fun BackupEnvelope.toImportPlan(): ImportPlan {
        val profiles = profiles.map { profile ->
            ImportProfile(
                backupId = profile.backupId,
                entity = profile.toEntity()
            )
        }
        val scopes = scopes.map { scope ->
            ImportScope(
                backupId = scope.backupId,
                entity = scope.toEntity()
            )
        }
        return ImportPlan(
            profiles = profiles,
            scopes = scopes,
            history = history,
            settings = settings
        )
    }

    private fun ConfigEntity.toBackupProfile(): BackupProfile =
        BackupProfile(
            backupId = id,
            name = name,
            description = description,
            authorizer = authorizer.value,
            customizeAuthorizer = customizeAuthorizer,
            installMode = installMode.value,
            toastMode = toastMode.value,
            enableCustomizeInstallReason = enableCustomizeInstallReason,
            installReason = installReason.value,
            enableCustomizePackageSource = enableCustomizePackageSource,
            packageSource = packageSource.value,
            installRequesterMode = installRequesterMode.value,
            installRequester = installRequester,
            installerMode = installerMode.value,
            installer = installer,
            enableCustomizeUser = enableCustomizeUser,
            targetUserId = targetUserId,
            enableManualDexopt = enableManualDexopt,
            forceDexopt = forceDexopt,
            dexoptMode = dexoptMode.value,
            autoDelete = autoDelete,
            autoDeleteZip = autoDeleteZip,
            displaySize = displaySize,
            displaySdk = displaySdk,
            forAllUser = forAllUser,
            allowTestOnly = allowTestOnly,
            allowDowngrade = allowDowngrade,
            bypassLowTargetSdk = bypassLowTargetSdk,
            allowAllRequestedPermissions = allowAllRequestedPermissions,
            allowSigMismatch = allowSigMismatch,
            allowSigUnknown = allowSigUnknown,
            requestUpdateOwnership = requestUpdateOwnership,
            splitChooseAll = splitChooseAll,
            apkChooseAll = apkChooseAll,
            requireBiometricAuth = requireBiometricAuth,
            createdAt = createdAt,
            modifiedAt = modifiedAt
        )

    private fun AppEntity.toBackupProfileScope(): BackupProfileScope =
        BackupProfileScope(
            backupId = configId,
            packageName = packageName,
            createdAt = createdAt,
            modifiedAt = modifiedAt
        )

    private fun OperationHistoryEntity.toBackupHistoryEntry(): BackupHistoryEntry =
        BackupHistoryEntry(
            operationType = operationType,
            status = status,
            packageName = packageName,
            appLabel = appLabel,
            timestamp = timestamp,
            isFreshInstall = isFreshInstall,
            versionChange = versionChange,
            oldVersionName = oldVersionName,
            oldVersionCode = oldVersionCode,
            newVersionName = newVersionName,
            newVersionCode = newVersionCode,
            sourcePaths = sourcePaths,
            initiatorPackageName = initiatorPackageName,
            installerPackageName = installerPackageName,
            installMethod = installMethod,
            authorizer = authorizer,
            installMode = installMode,
            errorSummary = errorSummary,
            errorType = errorType
        )

    private fun BackupProfile.toEntity(): ConfigEntity {
        val now = System.currentTimeMillis()
        return ConfigEntity(
            id = 0L,
            name = name,
            description = description,
            authorizer = Authorizer.fromValueOrDefault(authorizer),
            customizeAuthorizer = customizeAuthorizer,
            installMode = InstallMode.entries.find { it.value == installMode } ?: InstallMode.Dialog,
            toastMode = ToastMode.fromValue(toastMode),
            enableCustomizeInstallReason = enableCustomizeInstallReason,
            installReason = InstallReason.fromInt(installReason),
            enableCustomizePackageSource = enableCustomizePackageSource,
            packageSource = PackageSource.fromInt(packageSource),
            installRequesterMode = InstallRequesterMode.entries.find { it.value == installRequesterMode }
                ?: InstallRequesterMode.Disable,
            installRequester = installRequester,
            installerMode = InstallerMode.entries.find { it.value == installerMode } ?: InstallerMode.Self,
            installer = installer,
            enableCustomizeUser = enableCustomizeUser,
            targetUserId = targetUserId,
            enableManualDexopt = enableManualDexopt,
            forceDexopt = forceDexopt,
            dexoptMode = DexoptMode.entries.find { it.value == dexoptMode } ?: DexoptMode.SpeedProfile,
            autoDelete = autoDelete,
            autoDeleteZip = autoDeleteZip,
            displaySize = displaySize,
            displaySdk = displaySdk,
            forAllUser = forAllUser,
            allowTestOnly = allowTestOnly,
            allowDowngrade = allowDowngrade,
            bypassLowTargetSdk = bypassLowTargetSdk,
            allowAllRequestedPermissions = allowAllRequestedPermissions,
            allowSigMismatch = allowSigMismatch,
            allowSigUnknown = allowSigUnknown,
            requestUpdateOwnership = requestUpdateOwnership,
            splitChooseAll = splitChooseAll,
            apkChooseAll = apkChooseAll,
            requireBiometricAuth = requireBiometricAuth,
            createdAt = createdAt.takeIf { it > 0L } ?: now,
            modifiedAt = modifiedAt.takeIf { it > 0L } ?: now
        )
    }

    private fun validateProfile(
        profile: BackupProfile,
        issues: MutableList<BackupValidationIssue>
    ) {
        if (Authorizer.entries.none { it.value == profile.authorizer }) {
            issues += invalidProfileField("authorizer=${profile.authorizer}")
        }
        if (InstallMode.entries.none { it.value == profile.installMode }) {
            issues += invalidProfileField("installMode=${profile.installMode}")
        }
        if (ToastMode.entries.none { it.value == profile.toastMode }) {
            issues += invalidProfileField("toastMode=${profile.toastMode}")
        }
        if (InstallerMode.entries.none { it.value == profile.installerMode }) {
            issues += invalidProfileField("installerMode=${profile.installerMode}")
        }
        if (InstallRequesterMode.entries.none { it.value == profile.installRequesterMode }) {
            issues += invalidProfileField("installRequesterMode=${profile.installRequesterMode}")
        }
        if (InstallReason.entries.none { it.value == profile.installReason }) {
            issues += invalidProfileField("installReason=${profile.installReason}")
        }
        if (PackageSource.entries.none { it.value == profile.packageSource }) {
            issues += invalidProfileField("packageSource=${profile.packageSource}")
        }
        if (DexoptMode.entries.none { it.value == profile.dexoptMode }) {
            issues += invalidProfileField("dexoptMode=${profile.dexoptMode}")
        }
    }

    private fun validateHistory(
        history: BackupHistoryEntry,
        issues: MutableList<BackupValidationIssue>
    ) {
        if (history.packageName.isBlank()) {
            issues += invalidHistoryField("packageName")
        }
        if (OperationType.entries.none { it.name == history.operationType }) {
            issues += invalidHistoryField("operationType=${history.operationType}")
        }
        if (OperationStatus.entries.none { it.name == history.status }) {
            issues += invalidHistoryField("status=${history.status}")
        }
        if (VersionChange.entries.none { it.name == history.versionChange }) {
            issues += invalidHistoryField("versionChange=${history.versionChange}")
        }
        if (InstallMethod.entries.none { it.name == history.installMethod }) {
            issues += invalidHistoryField("installMethod=${history.installMethod}")
        }
        if (Authorizer.entries.none { it.value == history.authorizer }) {
            issues += invalidHistoryField("authorizer=${history.authorizer}")
        }
        if (InstallMode.entries.none { it.value == history.installMode }) {
            issues += invalidHistoryField("installMode=${history.installMode}")
        }
    }

    private fun BackupSettingEntry.matchesSupportedSetting(type: AppDataStore.PreferenceValueType): Boolean =
        when (type) {
            AppDataStore.PreferenceValueType.STRING -> this.type == BackupSettingType.STRING
            AppDataStore.PreferenceValueType.INT -> this.type == BackupSettingType.INT && value.toIntOrNull() != null
            AppDataStore.PreferenceValueType.BOOLEAN -> this.type == BackupSettingType.BOOLEAN &&
                    value.toBooleanStrictOrNull() != null
        }

    private fun invalidProfileField(field: String): BackupValidationIssue =
        errorIssue(
            code = "invalid_profile_field",
            messageResId = R.string.backup_settings_validation_invalid_profile_field,
            field
        )

    private fun invalidHistoryField(field: String): BackupValidationIssue =
        errorIssue(
            code = "invalid_history_field",
            messageResId = R.string.backup_settings_validation_invalid_history_field,
            field
        )

    private fun errorIssue(
        code: String,
        messageResId: Int,
        vararg args: String
    ): BackupValidationIssue =
        BackupValidationIssue(
            severity = BackupValidationSeverity.ERROR,
            code = code,
            messageResId = messageResId,
            args = args.toList()
        )

    private fun warningIssue(
        code: String,
        messageResId: Int,
        vararg args: String
    ): BackupValidationIssue =
        BackupValidationIssue(
            severity = BackupValidationSeverity.WARNING,
            code = code,
            messageResId = messageResId,
            args = args.toList()
        )

    private fun BackupProfileScope.toEntity(): AppEntity {
        val now = System.currentTimeMillis()
        return AppEntity(
            id = 0L,
            packageName = packageName,
            configId = 0L,
            createdAt = createdAt.takeIf { it > 0L } ?: now,
            modifiedAt = modifiedAt.takeIf { it > 0L } ?: now
        )
    }

    private fun BackupHistoryEntry.toEntity(): OperationHistoryEntity =
        OperationHistoryEntity(
            id = 0L,
            operationType = operationType,
            status = status,
            packageName = packageName,
            appLabel = appLabel,
            timestamp = timestamp,
            isFreshInstall = isFreshInstall,
            versionChange = versionChange,
            oldVersionName = oldVersionName,
            oldVersionCode = oldVersionCode,
            newVersionName = newVersionName,
            newVersionCode = newVersionCode,
            sourcePaths = sourcePaths,
            initiatorPackageName = initiatorPackageName,
            installerPackageName = installerPackageName,
            installMethod = installMethod,
            authorizer = authorizer,
            installMode = installMode,
            errorSummary = errorSummary,
            errorType = errorType
        )

    @Suppress("UNCHECKED_CAST")
    private fun MutablePreferences.writeSupportedSetting(entry: BackupSettingEntry): Boolean {
        val supportedKey = AppDataStore.supportedKeys[entry.key] ?: return false
        return when (supportedKey.type) {
            AppDataStore.PreferenceValueType.STRING -> {
                if (entry.type != BackupSettingType.STRING) return false
                this[supportedKey.key as Preferences.Key<String>] = entry.value
                true
            }

            AppDataStore.PreferenceValueType.INT -> {
                if (entry.type != BackupSettingType.INT) return false
                val value = entry.value.toIntOrNull() ?: return false
                this[supportedKey.key as Preferences.Key<Int>] = value
                true
            }

            AppDataStore.PreferenceValueType.BOOLEAN -> {
                if (entry.type != BackupSettingType.BOOLEAN) return false
                val value = entry.value.toBooleanStrictOrNull() ?: return false
                this[supportedKey.key as Preferences.Key<Boolean>] = value
                true
            }
        }
    }

    private data class PreRestoreSnapshot(
        val configs: List<ConfigEntity>,
        val apps: List<AppEntity>,
        val history: List<OperationHistoryEntity>,
        val settings: List<BackupSettingEntry>
    )

    private data class ImportPlan(
        val profiles: List<ImportProfile>,
        val scopes: List<ImportScope>,
        val history: List<BackupHistoryEntry>,
        val settings: List<BackupSettingEntry>
    )

    private data class ImportProfile(
        val backupId: Long,
        val entity: ConfigEntity
    )

    private data class ImportScope(
        val backupId: Long,
        val entity: AppEntity
    )

    private data class SettingsWriteResult(
        val restored: Int,
        val ignored: Int
    )

}
