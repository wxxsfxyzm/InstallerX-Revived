// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer.mapper

import com.rosan.installer.R
import com.rosan.installer.domain.engine.model.state.DomainInstallState
import com.rosan.installer.domain.engine.model.state.InstallActionType
import com.rosan.installer.domain.engine.model.state.InstallNotice
import com.rosan.installer.ui.page.main.installer.dialog.inner.InstallNoticeResources
import com.rosan.installer.ui.page.main.installer.dialog.inner.InstallStateResult
import com.rosan.installer.ui.page.main.widget.chip.NoticeModel

class InstallStateUiMapper(
    private val resources: InstallNoticeResources
) {
    fun mapToUiState(domainState: DomainInstallState): InstallStateResult {
        val noticesUiModels = domainState.notices.map { notice ->
            mapNotice(notice)
        }

        val buttonTextId = when (domainState.actionType) {
            InstallActionType.UPGRADE -> R.string.upgrade
            InstallActionType.DOWNGRADE_INSTALL_ANYWAY,
            InstallActionType.SIGNATURE_MISMATCH_INSTALL_ANYWAY -> R.string.install_anyway

            InstallActionType.UNARCHIVE -> R.string.unarchive
            InstallActionType.REINSTALL -> R.string.reinstall
            InstallActionType.INSTALL -> R.string.install
        }

        return InstallStateResult(noticesUiModels.toMutableList(), buttonTextId)
    }

    // 💡 方法名改为 mapNotice
    private fun mapNotice(notice: InstallNotice): NoticeModel {
        return when (notice) {
            is InstallNotice.Downgrade -> NoticeModel(
                shortLabel = resources.tagDowngrade,
                fullDescription = resources.textDowngrade,
                color = resources.errorColor
            )

            is InstallNotice.SignatureMismatch -> NoticeModel(
                shortLabel = resources.tagSignature,
                fullDescription = resources.textSigMismatch,
                color = resources.errorColor
            )

            is InstallNotice.SignatureUnknown -> NoticeModel(
                shortLabel = resources.tagSignature,
                fullDescription = resources.textSigUnknown,
                color = resources.tertiaryColor
            )

            is InstallNotice.SdkIncompatible -> NoticeModel(
                shortLabel = resources.tagSdk,
                fullDescription = resources.textSdkIncompatible,
                color = resources.errorColor
            )

            is InstallNotice.Arch32On64 -> NoticeModel(
                shortLabel = resources.tagArch32,
                fullDescription = resources.textArch32,
                color = resources.tertiaryColor
            )

            is InstallNotice.Emulated -> NoticeModel(
                shortLabel = resources.tagEmulated,
                fullDescription = resources.textArchMismatchFormat.format(notice.appArch.name, notice.sysArch.name),
                color = resources.tertiaryColor
            )

            is InstallNotice.Identical -> NoticeModel(
                shortLabel = resources.tagIdentical,
                fullDescription = resources.textIdentical,
                color = resources.primaryColor
            )

            is InstallNotice.Xposed -> {
                val details = buildString {
                    notice.minApi?.let { append("${resources.labelXposedMinApi}$it\n") }
                    notice.targetApi?.let { append("${resources.labelXposedTargetApi}$it\n") }
                    if (!notice.description.isNullOrBlank()) {
                        if (isNotEmpty()) append("\n")
                        append(notice.description)
                    }
                }.trim()

                NoticeModel(
                    shortLabel = resources.tagXposed,
                    fullDescription = details.ifEmpty { resources.tagXposed },
                    color = resources.primaryColor
                )
            }
        }
    }
}
