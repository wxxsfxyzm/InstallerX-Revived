// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.settings.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.rosan.installer.BuildConfig
import com.rosan.installer.data.settings.local.datastore.AppDataStore
import com.rosan.installer.data.settings.local.room.INSTALLER_ROOM_SCHEMA_VERSION
import com.rosan.installer.data.settings.local.room.dao.AppDao
import com.rosan.installer.data.settings.local.room.dao.ConfigDao
import com.rosan.installer.data.settings.local.room.dao.OperationHistoryDao
import com.rosan.installer.data.settings.local.room.entity.AppEntity
import com.rosan.installer.data.settings.local.room.entity.ConfigEntity
import com.rosan.installer.data.settings.local.room.entity.OperationHistoryEntity
import com.rosan.installer.domain.settings.model.backup.BackupEnvelope
import com.rosan.installer.domain.settings.model.backup.BackupHistoryEntry
import com.rosan.installer.domain.settings.model.backup.BackupProfile
import com.rosan.installer.domain.settings.model.backup.BackupProfileScope
import com.rosan.installer.domain.settings.model.backup.BackupSettingEntry
import com.rosan.installer.domain.settings.model.backup.BackupSettingType
import com.rosan.installer.domain.settings.model.backup.RestoreResult
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.DexoptMode
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
            formatVersion = CURRENT_FORMAT_VERSION,
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

    override suspend fun restoreBackup(envelope: BackupEnvelope): RestoreResult {
        require(envelope.formatVersion <= CURRENT_FORMAT_VERSION) {
            "Unsupported backup format version: ${envelope.formatVersion}"
        }

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
            initiatorPackageName = initiatorPackageName,
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
            initiatorPackageName = initiatorPackageName,
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

    private companion object {
        /**
         * The current supported backup format version.
         *
         * Increment this value only when introducing breaking changes to the [BackupEnvelope]
         * structure that cannot be handled by the default JSON configuration (e.g., removing
         * non-optional fields or changing field types).
         *
         * Non-breaking additions should keep this version unchanged to maintain
         * cross-version compatibility.
         */
        const val CURRENT_FORMAT_VERSION = 1
    }
}
