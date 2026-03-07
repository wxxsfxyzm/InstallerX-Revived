// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.settings.mapper

import com.rosan.installer.data.settings.local.room.entity.ConfigEntity
import com.rosan.installer.domain.settings.model.ConfigModel

// Map Room database entity to pure business domain model
fun ConfigEntity.toDomainModel(): ConfigModel {
    val model = ConfigModel(
        id = this.id,
        name = this.name,
        description = this.description,
        authorizer = this.authorizer,
        customizeAuthorizer = this.customizeAuthorizer,
        installMode = this.installMode,
        enableCustomizeInstallReason = this.enableCustomizeInstallReason,
        installReason = this.installReason,
        enableCustomizePackageSource = this.enableCustomizePackageSource,
        packageSource = this.packageSource,
        installRequester = this.installRequester,
        installer = this.installer,
        enableCustomizeUser = this.enableCustomizeUser,
        targetUserId = this.targetUserId,
        enableManualDexopt = this.enableManualDexopt,
        forceDexopt = this.forceDexopt,
        dexoptMode = this.dexoptMode,
        autoDelete = this.autoDelete,
        autoDeleteZip = this.autoDeleteZip,
        displaySize = this.displaySize,
        displaySdk = this.displaySdk,
        forAllUser = this.forAllUser,
        allowTestOnly = this.allowTestOnly,
        allowDowngrade = this.allowDowngrade,
        bypassLowTargetSdk = this.bypassLowTargetSdk,
        allowAllRequestedPermissions = this.allowAllRequestedPermissions,
        splitChooseAll = this.splitChooseAll,
        apkChooseAll = this.apkChooseAll,
        createdAt = this.createdAt,
        modifiedAt = this.modifiedAt
    )

    // Transfer runtime flags
    model.installFlags = this.installFlags
    model.bypassBlacklistInstallSetByUser = this.bypassBlacklistInstallSetByUser
    model.uninstallFlags = this.uninstallFlags
    model.callingFromUid = this.callingFromUid

    return model
}

// Map business domain model back to Room database entity
fun ConfigModel.toEntity(): ConfigEntity {
    val entity = ConfigEntity(
        id = this.id,
        name = this.name,
        description = this.description,
        authorizer = this.authorizer,
        customizeAuthorizer = this.customizeAuthorizer,
        installMode = this.installMode,
        enableCustomizeInstallReason = this.enableCustomizeInstallReason,
        installReason = this.installReason,
        enableCustomizePackageSource = this.enableCustomizePackageSource,
        packageSource = this.packageSource,
        installRequester = this.installRequester,
        installer = this.installer,
        enableCustomizeUser = this.enableCustomizeUser,
        targetUserId = this.targetUserId,
        enableManualDexopt = this.enableManualDexopt,
        forceDexopt = this.forceDexopt,
        dexoptMode = this.dexoptMode,
        autoDelete = this.autoDelete,
        autoDeleteZip = this.autoDeleteZip,
        displaySize = this.displaySize,
        displaySdk = this.displaySdk,
        forAllUser = this.forAllUser,
        allowTestOnly = this.allowTestOnly,
        allowDowngrade = this.allowDowngrade,
        bypassLowTargetSdk = this.bypassLowTargetSdk,
        allowAllRequestedPermissions = this.allowAllRequestedPermissions,
        splitChooseAll = this.splitChooseAll,
        apkChooseAll = this.apkChooseAll,
        createdAt = this.createdAt,
        modifiedAt = this.modifiedAt
    )

    // Transfer runtime flags
    entity.installFlags = this.installFlags
    entity.bypassBlacklistInstallSetByUser = this.bypassBlacklistInstallSetByUser
    entity.uninstallFlags = this.uninstallFlags
    entity.callingFromUid = this.callingFromUid

    return entity
}
