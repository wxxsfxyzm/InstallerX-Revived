// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.usecase

import com.rosan.installer.core.env.DeviceConfig
import com.rosan.installer.domain.device.model.Architecture
import com.rosan.installer.domain.engine.model.AppEntity
import com.rosan.installer.domain.engine.model.DataType
import com.rosan.installer.domain.engine.model.PackageAnalysisResult
import com.rosan.installer.domain.engine.model.PackageIdentityStatus
import com.rosan.installer.domain.engine.model.SignatureMatchStatus
import com.rosan.installer.domain.engine.model.state.DomainInstallState
import com.rosan.installer.domain.engine.model.state.InstallActionType
import com.rosan.installer.domain.engine.model.state.InstallNotice

class AnalyzeInstallStateUseCase {

    /**
     * Analyzes the installation state purely based on business rules.
     * Using the 'invoke' operator allows calling this use case as a function.
     */
    operator fun invoke(
        currentPackage: PackageAnalysisResult,
        entityToInstall: AppEntity.BaseEntity?,
        primaryEntity: AppEntity?,
        isSplitUpdateMode: Boolean,
        containerType: DataType?,
        systemArch: Architecture,
        systemSdkInt: Int
    ): DomainInstallState {
        val oldInfo = currentPackage.installedAppInfo
        val notices = mutableListOf<InstallNotice>()
        var actionType = InstallActionType.INSTALL

        // 1. Determine Action Type and Downgrade Warning
        if (entityToInstall != null && oldInfo != null) {
            when {
                entityToInstall.versionCode > oldInfo.versionCode -> {
                    actionType = InstallActionType.UPGRADE
                }

                entityToInstall.versionCode < oldInfo.versionCode -> {
                    notices.add(InstallNotice.Downgrade)
                    actionType = InstallActionType.DOWNGRADE_INSTALL_ANYWAY
                }

                oldInfo.isArchived -> {
                    actionType = InstallActionType.UNARCHIVE
                }

                entityToInstall.versionName == oldInfo.versionName -> {
                    actionType = InstallActionType.REINSTALL
                }
            }
        }

        // 2. Check Signature Status
        if (!isSplitUpdateMode && (containerType == DataType.APK || containerType == DataType.APKS)) {
            when (currentPackage.signatureMatchStatus) {
                SignatureMatchStatus.MISMATCH -> {
                    notices.add(0, InstallNotice.SignatureMismatch)
                    actionType = InstallActionType.SIGNATURE_MISMATCH_INSTALL_ANYWAY
                }

                SignatureMatchStatus.UNKNOWN_ERROR -> {
                    notices.add(0, InstallNotice.SignatureUnknown)
                }

                else -> {}
            }
        }

        // 3. Check Min SDK
        val newMinSdk = entityToInstall?.minSdk?.toIntOrNull()
        if (newMinSdk != null && newMinSdk > systemSdkInt) {
            notices.add(0, InstallNotice.SdkIncompatible)
        }

        // 4. Check Architecture Compatibility
        val appArch = (primaryEntity as? AppEntity.BaseEntity)?.arch
        if (appArch != null && appArch != Architecture.NONE && appArch != Architecture.UNKNOWN) {
            val isSys64 = systemArch == Architecture.ARM64 || systemArch == Architecture.X86_64
            val isApp32 = appArch == Architecture.ARM || appArch == Architecture.ARMEABI || appArch == Architecture.X86

            if (isSys64 && isApp32) {
                notices.add(InstallNotice.Arch32On64)
            }

            val sysIsArm = DeviceConfig.isArm
            val appIsX86 = appArch == Architecture.X86 || appArch == Architecture.X86_64
            val sysIsX86 = DeviceConfig.isX86
            val appIsArm = appArch == Architecture.ARM || appArch == Architecture.ARM64 || appArch == Architecture.ARMEABI

            if ((sysIsArm && appIsX86) || (sysIsX86 && appIsArm)) {
                notices.add(0, InstallNotice.Emulated(appArch, systemArch))
            }
        }

        // 5. Check Package Identity
        if (currentPackage.identityStatus == PackageIdentityStatus.IDENTICAL) {
            notices.add(InstallNotice.Identical)
        }

        // 6. Check Xposed Module Info
        val xposedInfo = (primaryEntity as? AppEntity.BaseEntity)?.xposedInfo
        if (xposedInfo != null) {
            notices.add(
                InstallNotice.Xposed(
                    minApi = xposedInfo.minApi,
                    targetApi = xposedInfo.targetApi,
                    description = xposedInfo.description
                )
            )
        }

        return DomainInstallState(actionType, notices)
    }
}
