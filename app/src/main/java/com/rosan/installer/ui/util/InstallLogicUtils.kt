// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors

package com.rosan.installer.ui.util

import com.rosan.installer.R
import com.rosan.installer.build.RsConfig
import com.rosan.installer.build.model.entity.Architecture
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.PackageAnalysisResult
import com.rosan.installer.data.app.model.enums.DataType
import com.rosan.installer.data.app.model.enums.SignatureMatchStatus
import com.rosan.installer.ui.page.main.installer.dialog.inner.InstallStateResult
import com.rosan.installer.ui.page.main.installer.dialog.inner.InstallWarningResources
import com.rosan.installer.ui.page.main.widget.chip.WarningModel

object InstallLogicUtils {

    /**
     * Analyzes the installation state to generate warnings and determine the button text.
     *
     * @param currentPackage The currently installed package information.
     * @param entityToInstall The specific entity (APK/Split) being considered for install.
     * @param primaryEntity The main entity used for architecture checks (sometimes different from entityToInstall).
     * @param isSplitUpdateMode Whether this is a split update.
     * @param containerType The type of container (APK, APKS, etc.).
     * @param systemArch The current system architecture (e.g., from RsConfig).
     * @param systemSdkInt The current system SDK version (Build.VERSION.SDK_INT).
     * @param resources The UI strings and colors required for models.
     */
    fun analyzeInstallState(
        currentPackage: PackageAnalysisResult,
        entityToInstall: AppEntity.BaseEntity?,
        primaryEntity: AppEntity?,
        isSplitUpdateMode: Boolean,
        containerType: DataType?,
        systemArch: Architecture,
        systemSdkInt: Int,
        resources: InstallWarningResources
    ): InstallStateResult {
        val oldInfo = currentPackage.installedAppInfo
        val signatureStatus = currentPackage.signatureMatchStatus

        val warnings = mutableListOf<WarningModel>()
        var finalButtonTextId = R.string.install

        // 1. Determine Button Text
        if (entityToInstall != null) {
            if (oldInfo == null) {
                finalButtonTextId = R.string.install
            } else {
                when {
                    entityToInstall.versionCode > oldInfo.versionCode -> {
                        finalButtonTextId = R.string.upgrade
                    }

                    entityToInstall.versionCode < oldInfo.versionCode -> {
                        // Add Downgrade Warning
                        warnings.add(
                            WarningModel(
                                resources.tagDowngrade,
                                resources.textDowngrade,
                                resources.errorColor
                            )
                        )
                        finalButtonTextId = R.string.install_anyway
                    }

                    oldInfo.isArchived -> {
                        finalButtonTextId = R.string.unarchive
                    }

                    entityToInstall.versionName == oldInfo.versionName -> {
                        finalButtonTextId = R.string.reinstall
                    }

                    else -> {
                        finalButtonTextId = R.string.install
                    }
                }
            }
        }

        // 2. Check Signature Status
        if (!isSplitUpdateMode && (containerType == DataType.APK || containerType == DataType.APKS)) {
            when (signatureStatus) {
                SignatureMatchStatus.MISMATCH -> {
                    // Priority High: Add to index 0
                    warnings.add(
                        0,
                        WarningModel(
                            resources.tagSignature,
                            resources.textSigMismatch,
                            resources.errorColor
                        )
                    )
                    finalButtonTextId = R.string.install_anyway
                }

                SignatureMatchStatus.UNKNOWN_ERROR -> {
                    warnings.add(
                        0,
                        WarningModel(
                            resources.tagSignature,
                            resources.textSigUnknown,
                            resources.tertiaryColor
                        )
                    )
                }

                else -> {}
            }
        }

        // 3. Check Min SDK
        val newMinSdk = entityToInstall?.minSdk?.toIntOrNull()
        if (newMinSdk != null && newMinSdk > systemSdkInt) {
            // Priority High
            warnings.add(
                0,
                WarningModel(resources.tagSdk, resources.textSdkIncompatible, resources.errorColor)
            )
        }

        // 4. Check Architecture Compatibility
        val appArch = (primaryEntity as? AppEntity.BaseEntity)?.arch
        if (appArch != null && appArch != Architecture.NONE && appArch != Architecture.UNKNOWN) {
            val isSys64 = systemArch == Architecture.ARM64 || systemArch == Architecture.X86_64
            val isApp32 = appArch == Architecture.ARM || appArch == Architecture.ARMEABI || appArch == Architecture.X86

            // Warning: Running 32-bit app on 64-bit system (if considered a warning in your context)
            if (isSys64 && isApp32) {
                warnings.add(
                    WarningModel(
                        shortLabel = resources.tagArch32,
                        fullDescription = resources.textArch32,
                        color = resources.tertiaryColor
                    )
                )
            }

            // Warning: Emulation mismatch (ARM on x86 or vice versa)
            val sysIsArm = RsConfig.isArm
            val appIsX86 = appArch == Architecture.X86 || appArch == Architecture.X86_64
            val sysIsX86 = RsConfig.isX86
            val appIsArm = appArch == Architecture.ARM || appArch == Architecture.ARM64 || appArch == Architecture.ARMEABI

            if ((sysIsArm && appIsX86) || (sysIsX86 && appIsArm)) {
                warnings.add(
                    0,
                    WarningModel(
                        shortLabel = resources.tagEmulated,
                        fullDescription = resources.textArchMismatchFormat.format(appArch.name, systemArch.name),
                        color = resources.tertiaryColor
                    )
                )
            }
        }

        return InstallStateResult(warnings, finalButtonTextId)
    }
}