// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer.mapper

import com.rosan.installer.R
import com.rosan.installer.domain.engine.model.packageinfo.AppSignatureInfo
import com.rosan.installer.domain.engine.model.packageinfo.PackageSignatureAnalysis
import com.rosan.installer.domain.engine.model.packageinfo.SignatureCertificateInfo
import com.rosan.installer.domain.engine.model.packageinfo.SignatureMatchStatus
import com.rosan.installer.domain.engine.model.state.DomainInstallState
import com.rosan.installer.domain.engine.model.state.InstallActionType
import com.rosan.installer.domain.engine.model.state.InstallNotice
import com.rosan.installer.domain.engine.model.state.SignatureNoticeDetails
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

        return InstallStateResult(
            notices = noticesUiModels.toMutableList(),
            buttonTextId = buttonTextId
        )
    }

    private fun mapNotice(notice: InstallNotice) =
        when (notice) {
            is InstallNotice.Downgrade -> NoticeModel(
                shortLabel = resources.tagDowngrade,
                fullDescription = resources.textDowngrade,
                color = resources.errorColor
            )

            is InstallNotice.SignatureSummary -> NoticeModel(
                shortLabel = if (notice.hasPackageSignatureIssues) {
                    resources.tagSignature
                } else {
                    notice.status.toSignatureChipLabel()
                },
                fullDescription = buildSignatureDescription(
                    summary = if (notice.hasPackageSignatureIssues) {
                        resources.textSigAnalysisIssue
                    } else {
                        notice.status.toSignatureSummaryText()
                    },
                    details = notice.details
                ),
                color = if (notice.hasPackageSignatureIssues) {
                    resources.errorColor
                } else {
                    notice.status.toSignatureColor()
                }
            )

            is InstallNotice.SignatureMismatch -> NoticeModel(
                shortLabel = resources.tagSignature,
                fullDescription = buildSignatureDescription(
                    summary = resources.textSigMismatch,
                    details = notice.details
                ),
                color = resources.errorColor
            )

            is InstallNotice.SignatureUnknown -> NoticeModel(
                shortLabel = resources.tagSignature,
                fullDescription = buildSignatureDescription(
                    summary = resources.textSigUnknown,
                    details = notice.details
                ),
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

    private fun SignatureMatchStatus.toSignatureSummaryText() = when (this) {
        SignatureMatchStatus.NOT_INSTALLED -> resources.textSigNewInstall
        SignatureMatchStatus.MATCH -> resources.textSigMatch
        SignatureMatchStatus.ROTATION_COMPATIBLE -> resources.textSigRotationCompatible
        SignatureMatchStatus.CANDIDATE_ROTATION_UNCONFIRMED -> resources.textSigCandidateRotationUnconfirmed
        SignatureMatchStatus.MISMATCH -> resources.textSigMismatch
        SignatureMatchStatus.UNKNOWN_ERROR -> resources.textSigUnknown
    }

    private fun SignatureMatchStatus.toSignatureChipLabel() = when (this) {
        SignatureMatchStatus.MATCH -> resources.tagSignatureMatch
        SignatureMatchStatus.ROTATION_COMPATIBLE -> resources.tagSignatureRotation
        SignatureMatchStatus.CANDIDATE_ROTATION_UNCONFIRMED -> resources.tagSignatureRotationUnconfirmed
        else -> resources.tagSignature
    }

    private fun SignatureMatchStatus.toSignatureColor() = when (this) {
        SignatureMatchStatus.MISMATCH -> resources.errorColor
        SignatureMatchStatus.ROTATION_COMPATIBLE,
        SignatureMatchStatus.CANDIDATE_ROTATION_UNCONFIRMED,
        SignatureMatchStatus.UNKNOWN_ERROR -> resources.tertiaryColor

        SignatureMatchStatus.NOT_INSTALLED,
        SignatureMatchStatus.MATCH -> resources.primaryColor
    }

    private fun buildSignatureDescription(
        summary: String,
        details: SignatureNoticeDetails?
    ): String {
        val detailText = details?.toDisplayText().orEmpty()
        return if (detailText.isBlank()) summary else "$summary\n\n$detailText"
    }

    private fun SignatureNoticeDetails.toDisplayText(): String {
        return buildString {
            pendingSignatureInfo?.let { info ->
                appendSignatureInfo(resources.labelPendingSignature, info)
            }
            installedSignatureInfo?.let { info ->
                if (isNotEmpty()) append("\n\n")
                appendSignatureInfo(resources.labelInstalledSignature, info)
            }
            if (packageSignatureAnalysis.hasIssues) {
                if (isNotEmpty()) append("\n\n")
                appendPackageSignatureAnalysis(packageSignatureAnalysis)
            }
        }
    }

    private fun StringBuilder.appendPackageSignatureAnalysis(analysis: PackageSignatureAnalysis) {
        appendLine(resources.labelSignatureAnalysisIssues)
        appendIssueList(resources.labelSignatureVerificationFailedFiles, analysis.verificationFailedFiles)
        appendIssueList(resources.labelSignatureSplitMismatchFiles, analysis.splitSignatureMismatchFiles)
        appendIssueList(resources.labelSignatureDuplicateSplitNames, analysis.duplicateSplitNames)
        trimTrailingWhitespace()
    }

    private fun StringBuilder.appendIssueList(label: String, values: List<String>) {
        if (values.isEmpty()) return
        appendLine("- $label")
        values.forEach { appendLine("  - $it") }
    }

    private fun StringBuilder.appendSignatureInfo(
        title: String,
        info: AppSignatureInfo
    ) {
        appendLine(title)
        if (info.verifiedSchemes.isNotEmpty()) {
            appendLine("${resources.labelSignatureSchemes}: ${info.verifiedSchemes.joinToString(" + ")}")
        }
        val showSigningCertificateHistory = info.shouldShowSigningCertificateHistory()
        val historyContainsCurrentSigner = info.signerSha256Set.all { sha256 ->
            sha256 in info.signingCertificateHistorySha256Set
        }
        when {
            showSigningCertificateHistory && historyContainsCurrentSigner -> {
                appendSigningCertificateHistory(info)
            }

            info.certificates.isEmpty() -> {
                appendLine(resources.labelSignatureNoCertificates)
                if (showSigningCertificateHistory) {
                    appendLine()
                    appendSigningCertificateHistory(info)
                }
            }

            else -> {
                appendCurrentCertificates(info)
                if (showSigningCertificateHistory) {
                    appendLine()
                    appendSigningCertificateHistory(info)
                }
            }
        }

        val visibleWarnings = info.userVisibleWarnings()
        if (visibleWarnings.isNotEmpty()) {
            appendLine()
            appendLine(resources.labelSignatureWarnings)
            visibleWarnings.forEach { appendLine("- $it") }
        }

        if (info.errors.isNotEmpty()) {
            appendLine()
            appendLine(resources.labelSignatureErrors)
            info.errors.forEach { appendLine("- $it") }
        }

        trimTrailingWhitespace()
    }

    private fun StringBuilder.appendCurrentCertificates(info: AppSignatureInfo) {
        info.certificates.forEachIndexed { index, certificate ->
            if (index > 0) appendLine()
            val label = if (info.certificates.size == 1) {
                resources.labelSignatureCurrentCertificate
            } else {
                "${resources.labelSignatureCurrentCertificate} ${index + 1}"
            }
            appendCertificateDetails(label, certificate)
        }
    }

    private fun StringBuilder.appendSigningCertificateHistory(info: AppSignatureInfo) {
        appendLine(resources.labelSignatureCertificateLineage)
        info.signingCertificateHistory.forEachIndexed { index, certificate ->
            if (index > 0) appendLine()
            val currentMarker = if (certificate.sha256 in info.signerSha256Set) {
                " (${resources.labelSignatureCurrentMarker})"
            } else {
                ""
            }
            appendCertificateDetails(
                "${resources.labelSignatureLineageCertificate} ${index + 1}$currentMarker",
                certificate
            )
        }
    }

    private fun StringBuilder.appendCertificateDetails(
        title: String,
        certificate: SignatureCertificateInfo
    ) {
        appendLine(title)
        appendLine("${resources.labelSignatureSha256}: ${certificate.sha256.formatDigestForDisplay()}")
        appendLine("${resources.labelSignatureSha1}: ${certificate.sha1.formatDigestForDisplay()}")
        appendLine("${resources.labelSignatureSubject}: ${certificate.subject}")
        appendLine("${resources.labelSignatureIssuer}: ${certificate.issuer}")
        appendLine("${resources.labelSignatureValidFrom}: ${certificate.validFrom.orEmpty()}")
        appendLine("${resources.labelSignatureValidUntil}: ${certificate.validUntil.orEmpty()}")
        certificate.publicKeyAlgorithm?.let {
            appendLine("${resources.labelSignaturePublicKeyAlgorithm}: $it")
        }
        certificate.signatureAlgorithm?.let {
            appendLine("${resources.labelSignatureAlgorithm}: $it")
        }
    }

    private fun AppSignatureInfo.shouldShowSigningCertificateHistory(): Boolean {
        if (signingCertificateHistory.isEmpty()) return false
        return signingCertificateHistorySha256Set != signerSha256Set ||
                signingCertificateHistory.size > certificates.size
    }

    private fun AppSignatureInfo.userVisibleWarnings(): List<String> {
        return warnings.filterNot { warning ->
            warning.contains("not protected by signature", ignoreCase = true) &&
                    warning.contains("META-INF/", ignoreCase = true)
        }
    }

    private fun StringBuilder.trimTrailingWhitespace() {
        while (isNotEmpty() && this[length - 1].isWhitespace()) {
            deleteCharAt(length - 1)
        }
    }

    private fun String.formatDigestForDisplay(): String {
        return if (length % 2 == 0 && all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
            chunked(2).joinToString(":") { it.uppercase() }
        } else {
            this
        }
    }
}
