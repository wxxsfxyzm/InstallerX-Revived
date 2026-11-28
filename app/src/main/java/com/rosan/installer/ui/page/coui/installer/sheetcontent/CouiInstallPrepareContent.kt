// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer.sheetcontent

import android.content.ClipData
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itos.coui.layout.Divider
import com.itos.coui.scroll.couiVerticalScroll
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.theme.CouiTheme
import com.rosan.installer.R
import com.rosan.installer.core.device.model.Manufacturer
import com.rosan.installer.core.env.DeviceConfig
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.model.install.sourcePath
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.packageinfo.InstalledAppInfo
import com.rosan.installer.domain.engine.model.packageinfo.sortedBest
import com.rosan.installer.domain.engine.model.source.DataType
import com.rosan.installer.domain.engine.usecase.AnalyzeInstallStateUseCase
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.coui.installer.components.CouiAdaptiveInfoRow
import com.rosan.installer.ui.page.coui.installer.components.CouiAppInfoSlot
import com.rosan.installer.ui.page.coui.installer.components.CouiAppInfoState
import com.rosan.installer.ui.page.coui.widgets.CouiActionButton
import com.rosan.installer.ui.page.coui.widgets.CouiBasicItem
import com.rosan.installer.ui.page.coui.widgets.CouiCard
import com.rosan.installer.ui.page.coui.widgets.CouiInfoChipGroup
import com.rosan.installer.ui.page.coui.widgets.CouiInstallerTipCard
import com.rosan.installer.ui.page.coui.widgets.CouiNavigationItem
import com.rosan.installer.ui.page.coui.widgets.CouiSwitchItem
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.mapper.InstallNoticeResources
import com.rosan.installer.ui.page.main.installer.mapper.InstallStateUiMapper
import com.rosan.installer.ui.util.formatSize
import com.rosan.installer.util.pm.getBestPermissionLabel
import com.rosan.installer.util.toast
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun CouiInstallPrepareContent(
    viewModel: InstallerViewModel,
    appInfo: CouiAppInfoState,
    onCancel: () -> Unit,
    onInstall: () -> Unit,
    onLongInstall: (() -> Unit)?,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val config = uiState.config
    val currentPackageName = uiState.currentPackageName
    val currentPackage = uiState.analysisResults.find { it.packageName == currentPackageName }
    val settings = uiState.viewSettings

    var isExpanded by remember { mutableStateOf(false) }

    if (currentPackage == null) {
        CouiLoadingContent(statusText = stringResource(id = R.string.loading))
        return
    }

    val selectedEntities = currentPackage.appEntities.filter { it.selected }.map { it.app }
    val rawBaseEntity = currentPackage.appEntities.map { it.app }.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
    val allAvailableApps = currentPackage.appEntities.map { it.app }
    val primaryEntity = selectedEntities.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
        ?: selectedEntities.filterIsInstance<AppEntity.ModuleEntity>().firstOrNull()
        ?: selectedEntities.filterIsInstance<AppEntity.SplitEntity>().firstOrNull()
        ?: selectedEntities.firstOrNull()
        ?: allAvailableApps.sortedBest().firstOrNull()
    if (primaryEntity == null) {
        CouiLoadingContent(statusText = "No main app entity found")
        return
    }

    val entityToInstall = selectedEntities.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
    val containerType = primaryEntity.sourceType
    val totalSelectedSize = selectedEntities.sumOf { it.size }
    val isModuleSelected = selectedEntities.any { it is AppEntity.ModuleEntity }
    val isPureSplit = primaryEntity is AppEntity.SplitEntity
    val isBundleSplitUpdate = primaryEntity is AppEntity.BaseEntity &&
        entityToInstall == null &&
        selectedEntities.isNotEmpty() &&
        !isModuleSelected
    val isSplitUpdateMode = (isBundleSplitUpdate || isPureSplit) && currentPackage.installedAppInfo != null

    val primaryColor = CouiTheme.colorScheme.accentOnSurface
    val errorColor = MaterialTheme.colorScheme.error

    val tagDowngrade = stringResource(R.string.tag_downgrade)
    val downgradeWarning = stringResource(R.string.installer_prepare_type_downgrade)
    val tagSignature = stringResource(R.string.tag_signature)
    val tagSignatureMatch = stringResource(R.string.tag_signature_match)
    val tagSignatureRotation = stringResource(R.string.tag_signature_rotation)
    val tagSignatureRotationUnconfirmed = stringResource(R.string.tag_signature_rotation_unconfirmed)
    val tagAospSignature = stringResource(R.string.tag_aosp_signature)
    val textAospSignature = stringResource(R.string.installer_prepare_aosp_signature)
    val sigNewInstall = stringResource(R.string.installer_prepare_signature_new_install)
    val sigMatch = stringResource(R.string.installer_prepare_signature_match)
    val sigRotationCompatible = stringResource(R.string.installer_prepare_signature_rotation_compatible)
    val sigCandidateRotationUnconfirmed =
        stringResource(R.string.installer_prepare_signature_candidate_rotation_unconfirmed)
    val sigMismatchWarning = stringResource(R.string.installer_prepare_signature_mismatch)
    val sigUnknownWarning = stringResource(R.string.installer_prepare_signature_unknown)
    val sigAnalysisIssue = stringResource(R.string.installer_prepare_signature_analysis_issue)
    val sigSigningBlockNotInstalled =
        stringResource(R.string.installer_prepare_signature_signing_block_not_installed)
    val sigSigningBlockMatch = stringResource(R.string.installer_prepare_signature_signing_block_match)
    val sigSigningBlockUnknown = stringResource(R.string.installer_prepare_signature_signing_block_unknown)
    val labelPendingSignature = stringResource(R.string.installer_signature_pending_package)
    val labelInstalledSignature = stringResource(R.string.installer_signature_installed_package)
    val labelSignatureAnalysisIssues = stringResource(R.string.installer_signature_analysis_issues)
    val labelSignatureVerificationFailedFiles = stringResource(R.string.installer_signature_verification_failed_files)
    val labelSignatureSplitMismatchFiles = stringResource(R.string.installer_signature_split_mismatch_files)
    val labelSignatureDuplicateSplitNames = stringResource(R.string.installer_signature_duplicate_split_names)
    val labelSignatureSchemes = stringResource(R.string.installer_signature_schemes)
    val labelSignatureDeclaredSchemes = stringResource(R.string.installer_signature_declared_schemes)
    val labelSignatureCertificate = stringResource(R.string.installer_signature_certificate)
    val labelSignatureCurrentCertificate = stringResource(R.string.installer_signature_current_certificate)
    val labelSignatureCertificateLineage = stringResource(R.string.installer_signature_certificate_lineage)
    val labelSignatureLineageCertificate = stringResource(R.string.installer_signature_lineage_certificate)
    val labelSignatureCurrentMarker = stringResource(R.string.installer_signature_current_marker)
    val labelSignatureSha256 = stringResource(R.string.installer_signature_sha256)
    val labelSignatureSourceStampSha256 = stringResource(R.string.installer_signature_source_stamp_sha256)
    val labelSignatureSourceStamp = stringResource(R.string.installer_signature_source_stamp)
    val labelSignatureSourceStampStatus = stringResource(R.string.installer_signature_source_stamp_status)
    val textSourceStampUnverified = stringResource(R.string.installer_signature_source_stamp_unverified)
    val textSourceStampVerified = stringResource(R.string.installer_signature_source_stamp_verified)
    val textSourceStampFailed = stringResource(R.string.installer_signature_source_stamp_failed)
    val textSourceStampCertificateMismatch = stringResource(R.string.installer_signature_source_stamp_certificate_mismatch)
    val textSourceStampVerificationError = stringResource(R.string.installer_signature_source_stamp_verification_error)
    val labelSignatureSourceStampTimestamp = stringResource(R.string.installer_signature_source_stamp_timestamp)
    val labelSignatureSourceStampCertificate = stringResource(R.string.installer_signature_source_stamp_certificate)
    val labelSignatureSourceStampLineage = stringResource(R.string.installer_signature_source_stamp_lineage)
    val labelSignatureInformation = stringResource(R.string.installer_signature_information)
    val labelSignatureSha1 = stringResource(R.string.installer_signature_sha1)
    val labelSignatureSubject = stringResource(R.string.installer_signature_subject)
    val labelSignatureIssuer = stringResource(R.string.installer_signature_issuer)
    val labelSignatureValidFrom = stringResource(R.string.installer_signature_valid_from)
    val labelSignatureValidUntil = stringResource(R.string.installer_signature_valid_until)
    val labelSignaturePublicKeyAlgorithm = stringResource(R.string.installer_signature_public_key_algorithm)
    val labelSignatureAlgorithm = stringResource(R.string.installer_signature_algorithm)
    val labelSignatureWarnings = stringResource(R.string.installer_signature_warnings)
    val labelSignatureErrors = stringResource(R.string.installer_signature_errors)
    val labelSignatureNoCertificates = stringResource(R.string.installer_signature_no_certificates)
    val tagSdk = stringResource(R.string.tag_sdk)
    val sdkIncompatibleWarning = stringResource(R.string.installer_prepare_sdk_incompatible)
    val tagOplusOsdk = stringResource(R.string.tag_oplus_osdk)
    val oplusOsdkIncompatibleWarning = stringResource(R.string.installer_prepare_oplus_osdk_incompatible)
    val tagArch32 = stringResource(R.string.tag_arch_32)
    val textArch32 = stringResource(R.string.installer_prepare_arch_32_notice)
    val tagEmulated = stringResource(R.string.tag_arch_emulated)
    val textArchMismatch = stringResource(R.string.installer_prepare_arch_mismatch_notice)
    val tagIdentical = stringResource(R.string.tag_identical)
    val textIdentical = stringResource(R.string.installer_prepare_identical_notice)
    val tagXposed = stringResource(R.string.tag_xposed)
    val labelXposedMinApi = stringResource(R.string.installer_xposed_min_api)
    val labelXposedTargetApi = stringResource(R.string.installer_xposed_target_api)
    val labelSignatureHybridClassical = stringResource(R.string.installer_signature_hybrid_classical)
    val labelSignatureHybridPqc = stringResource(R.string.installer_signature_hybrid_pqc)

    val installResources = remember(errorColor, primaryColor, tagAospSignature, textAospSignature) {
        InstallNoticeResources(
            tagDowngrade = tagDowngrade,
            textDowngrade = downgradeWarning,
            tagSignature = tagSignature,
            tagSignatureMatch = tagSignatureMatch,
            tagSignatureRotation = tagSignatureRotation,
            tagSignatureRotationUnconfirmed = tagSignatureRotationUnconfirmed,
            tagAospSignature = tagAospSignature,
            textAospSignature = textAospSignature,
            textSigNewInstall = sigNewInstall,
            textSigMatch = sigMatch,
            textSigRotationCompatible = sigRotationCompatible,
            textSigCandidateRotationUnconfirmed = sigCandidateRotationUnconfirmed,
            textSigMismatch = sigMismatchWarning,
            textSigUnknown = sigUnknownWarning,
            textSigAnalysisIssue = sigAnalysisIssue,
            textSigSigningBlockNotInstalled = sigSigningBlockNotInstalled,
            textSigSigningBlockMatch = sigSigningBlockMatch,
            textSigSigningBlockUnknown = sigSigningBlockUnknown,
            labelPendingSignature = labelPendingSignature,
            labelInstalledSignature = labelInstalledSignature,
            labelSignatureAnalysisIssues = labelSignatureAnalysisIssues,
            labelSignatureVerificationFailedFiles = labelSignatureVerificationFailedFiles,
            labelSignatureSplitMismatchFiles = labelSignatureSplitMismatchFiles,
            labelSignatureDuplicateSplitNames = labelSignatureDuplicateSplitNames,
            labelSignatureSchemes = labelSignatureSchemes,
            labelSignatureDeclaredSchemes = labelSignatureDeclaredSchemes,
            labelSignatureCertificate = labelSignatureCertificate,
            labelSignatureCurrentCertificate = labelSignatureCurrentCertificate,
            labelSignatureCertificateLineage = labelSignatureCertificateLineage,
            labelSignatureLineageCertificate = labelSignatureLineageCertificate,
            labelSignatureCurrentMarker = labelSignatureCurrentMarker,
            labelSignatureHybridClassical = labelSignatureHybridClassical,
            labelSignatureHybridPqc = labelSignatureHybridPqc,
            labelSignatureSha256 = labelSignatureSha256,
            labelSignatureSourceStampSha256 = labelSignatureSourceStampSha256,
            labelSignatureSourceStamp = labelSignatureSourceStamp,
            labelSignatureSourceStampStatus = labelSignatureSourceStampStatus,
            textSourceStampUnverified = textSourceStampUnverified,
            textSourceStampVerified = textSourceStampVerified,
            textSourceStampFailed = textSourceStampFailed,
            textSourceStampCertificateMismatch = textSourceStampCertificateMismatch,
            textSourceStampVerificationError = textSourceStampVerificationError,
            labelSignatureSourceStampTimestamp = labelSignatureSourceStampTimestamp,
            labelSignatureSourceStampCertificate = labelSignatureSourceStampCertificate,
            labelSignatureSourceStampLineage = labelSignatureSourceStampLineage,
            labelSignatureInformation = labelSignatureInformation,
            labelSignatureSha1 = labelSignatureSha1,
            labelSignatureSubject = labelSignatureSubject,
            labelSignatureIssuer = labelSignatureIssuer,
            labelSignatureValidFrom = labelSignatureValidFrom,
            labelSignatureValidUntil = labelSignatureValidUntil,
            labelSignaturePublicKeyAlgorithm = labelSignaturePublicKeyAlgorithm,
            labelSignatureAlgorithm = labelSignatureAlgorithm,
            labelSignatureWarnings = labelSignatureWarnings,
            labelSignatureErrors = labelSignatureErrors,
            labelSignatureNoCertificates = labelSignatureNoCertificates,
            tagSdk = tagSdk,
            textSdkIncompatible = sdkIncompatibleWarning,
            tagOplusOsdk = tagOplusOsdk,
            textOplusOsdkIncompatible = oplusOsdkIncompatibleWarning,
            tagArch32 = tagArch32,
            textArch32 = textArch32,
            tagEmulated = tagEmulated,
            textArchMismatchFormat = textArchMismatch,
            tagIdentical = tagIdentical,
            textIdentical = textIdentical,
            tagXposed = tagXposed,
            labelXposedMinApi = labelXposedMinApi,
            labelXposedTargetApi = labelXposedTargetApi,
            errorColor = errorColor,
            tertiaryColor = primaryColor,
            primaryColor = primaryColor,
        )
    }

    val analyzeInstallStateUseCase = koinInject<AnalyzeInstallStateUseCase>()
    val deviceCapabilityProvider = koinInject<DeviceCapabilityProvider>()
    val checkOplusOsdk = settings.showOPPOSpecial &&
        (
            DeviceConfig.currentManufacturer == Manufacturer.OPPO ||
                DeviceConfig.currentManufacturer == Manufacturer.ONEPLUS
            )
    val deviceOplusOsdkVersion = if (checkOplusOsdk) deviceCapabilityProvider.oplusOSdkVersion else null
    val installStateUiMapper = remember(installResources) { InstallStateUiMapper(installResources) }

    val installStateResult = remember(
        currentPackage,
        entityToInstall,
        isSplitUpdateMode,
        containerType,
        settings.checkAppSignature,
        settings.showSignatureInfoOnMatch,
        settings.showSignatureDetails,
        settings.detectXposedModule,
        checkOplusOsdk,
        deviceOplusOsdkVersion,
        installStateUiMapper,
    ) {
        val domainState = analyzeInstallStateUseCase(
            currentPackage = currentPackage,
            entityToInstall = entityToInstall,
            primaryEntity = primaryEntity,
            isSplitUpdateMode = isSplitUpdateMode,
            containerType = containerType,
            systemArch = DeviceConfig.currentArchitecture,
            systemSdkInt = Build.VERSION.SDK_INT,
            checkOplusOsdk = checkOplusOsdk,
            deviceOplusOsdkVersion = deviceOplusOsdkVersion,
            checkAppSignature = settings.checkAppSignature,
            showSignatureInfoOnMatch = settings.showSignatureInfoOnMatch,
            showSignatureDetails = settings.showSignatureDetails,
            detectXposedModule = settings.detectXposedModule,
        )
        installStateUiMapper.mapToUiState(domainState)
    }

    val notices = installStateResult.notices
    val buttonTextId = installStateResult.buttonTextId

    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        overscrollEffect = rememberCouiOverscrollEffect(),
    ) {
        item {
            CouiAppInfoSlot(
                appInfo = appInfo,
                onIconClick = if (settings.labTapIconToShare) {
                    { viewModel.dispatch(InstallerViewAction.ShareApp(primaryEntity)) }
                } else {
                    null
                },
            )
        }
        item { Spacer(modifier = Modifier.height(6.dp)) }
        item {
            CouiInfoChipGroup(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                notices = notices,
            )
        }
        item {
            CouiCard(modifier = Modifier.padding(vertical = 6.dp)) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    when (primaryEntity) {
                        is AppEntity.BaseEntity -> {
                            CouiAdaptiveInfoRow(
                                labelResId = R.string.installer_version_name_label,
                                newValue = primaryEntity.versionName,
                                oldValue = currentPackage.installedAppInfo?.versionName,
                                isUninstalled = currentPackage.installedAppInfo?.isUninstalled ?: false,
                                isArchived = currentPackage.installedAppInfo?.isArchived ?: false,
                                hideIdenticalComparison = settings.hideIdenticalComparisons,
                            )
                            CouiAdaptiveInfoRow(
                                labelResId = R.string.installer_version_code_label,
                                newValue = primaryEntity.versionCode.toString(),
                                oldValue = currentPackage.installedAppInfo?.versionCode?.toString(),
                                isUninstalled = currentPackage.installedAppInfo?.isUninstalled ?: false,
                                isArchived = currentPackage.installedAppInfo?.isArchived ?: false,
                                hideIdenticalComparison = settings.hideIdenticalComparisons,
                            )
                            CouiSdkComparison(
                                entityToInstall = primaryEntity,
                                preInstallAppInfo = currentPackage.installedAppInfo,
                                displaySDK = config.displaySdk,
                                hideIdenticalComparison = settings.hideIdenticalComparisons,
                            )
                            AnimatedVisibility(visible = config.displaySize && primaryEntity.size > 0) {
                                val oldSize = currentPackage.installedAppInfo?.packageSize ?: 0L
                                val oldSizeStr = if (oldSize > 0 && !isSplitUpdateMode) oldSize.formatSize() else null
                                CouiAdaptiveInfoRow(
                                    labelResId = R.string.installer_package_size_label,
                                    newValue = totalSelectedSize.formatSize(),
                                    oldValue = oldSizeStr,
                                    hideIdenticalComparison = settings.hideIdenticalComparisons,
                                )
                            }
                            val isOppoOrOnePlus = DeviceConfig.currentManufacturer == Manufacturer.OPPO ||
                                DeviceConfig.currentManufacturer == Manufacturer.ONEPLUS
                            val shouldShowOppoRow = isOppoOrOnePlus &&
                                settings.showOPPOSpecial &&
                                primaryEntity.sourceType == DataType.APK &&
                                primaryEntity.minOsdkVersion != null
                            AnimatedVisibility(visible = shouldShowOppoRow) {
                                primaryEntity.minOsdkVersion?.let { version ->
                                    CouiAdaptiveInfoRow(
                                        labelResId = R.string.installer_package_minOsdkVersion_label,
                                        newValue = version,
                                        oldValue = null,
                                    )
                                }
                            }
                        }

                        is AppEntity.ModuleEntity -> {
                            val installedModuleInfo = currentPackage.installedModuleInfo
                            CouiAdaptiveInfoRow(
                                labelResId = R.string.installer_version_name_label,
                                newValue = primaryEntity.version,
                                oldValue = installedModuleInfo?.version,
                                hideIdenticalComparison = settings.hideIdenticalComparisons,
                            )
                            CouiAdaptiveInfoRow(
                                labelResId = R.string.installer_version_code_label,
                                newValue = primaryEntity.versionCode.toString(),
                                oldValue = installedModuleInfo?.versionCode?.toString(),
                                hideIdenticalComparison = settings.hideIdenticalComparisons,
                            )
                            AnimatedVisibility(visible = config.displaySdk) {
                                CouiAdaptiveInfoRow(
                                    labelResId = R.string.installer_module_author_label,
                                    newValue = primaryEntity.author,
                                    oldValue = null,
                                )
                            }
                            AnimatedVisibility(visible = config.displaySize) {
                                CouiAdaptiveInfoRow(
                                    labelResId = R.string.installer_package_size_label,
                                    newValue = totalSelectedSize.formatSize(),
                                    oldValue = null,
                                )
                            }
                        }

                        is AppEntity.SplitEntity -> {
                            CouiAdaptiveInfoRow(
                                labelResId = R.string.installer_split_name_label,
                                newValue = primaryEntity.splitName,
                                oldValue = null,
                            )
                            CouiSdkComparison(
                                entityToInstall = primaryEntity,
                                preInstallAppInfo = currentPackage.installedAppInfo,
                                displaySDK = config.displaySdk,
                                hideIdenticalComparison = settings.hideIdenticalComparisons,
                            )
                            AnimatedVisibility(visible = config.displaySize) {
                                CouiAdaptiveInfoRow(
                                    labelResId = R.string.installer_package_size_label,
                                    newValue = totalSelectedSize.formatSize(),
                                    oldValue = null,
                                )
                            }
                        }

                        else -> {}
                    }
                }
            }
        }

        item {
            AnimatedVisibility(
                visible = (rawBaseEntity != null) && isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                CouiCard(modifier = Modifier.padding(vertical = 6.dp)) {
                    var shown = false
                    if (rawBaseEntity?.permissions?.isNotEmpty() == true) {
                        CouiNavigationItem(
                            title = stringResource(R.string.permission_list),
                            description = stringResource(R.string.permission_list_desc),
                            onClick = { viewModel.dispatch(InstallerViewAction.ShowMiuixPermissionList) },
                        )
                        shown = true
                    }
                    if (config.authorizer != Authorizer.Dhizuku && config.authorizer != Authorizer.None) {
                        if (shown) Divider()
                        CouiNavigationItem(
                            title = stringResource(R.string.config_label_install_options),
                            description = stringResource(R.string.config_label_install_options_desc),
                            onClick = { viewModel.dispatch(InstallerViewAction.InstallExtendedMenu) },
                        )
                        shown = true
                    }
                    if (currentPackage.appEntities.size > 1) {
                        if (shown) Divider()
                        CouiNavigationItem(
                            title = stringResource(R.string.installer_select_split),
                            description = stringResource(R.string.installer_select_split_desc),
                            onClick = { viewModel.dispatch(InstallerViewAction.InstallChoice) },
                        )
                    }
                }
            }
        }

        val isInvalidSplitInstall = currentPackage.installedAppInfo == null &&
            entityToInstall == null &&
            selectedEntities.any { it is AppEntity.SplitEntity }

        item {
            AnimatedVisibility(
                visible = isInvalidSplitInstall,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                CouiInstallerTipCard(text = stringResource(R.string.installer_splits_invalid_tip))
            }
        }

        item {
            AnimatedVisibility(
                visible = isSplitUpdateMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                CouiInstallerTipCard(text = stringResource(R.string.installer_splits_only_tip))
            }
        }

        item {
            AnimatedVisibility(
                visible = (primaryEntity is AppEntity.ModuleEntity) &&
                    primaryEntity.description.isNotBlank() &&
                    config.displaySdk,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                CouiInstallerTipCard((primaryEntity as AppEntity.ModuleEntity).description)
            }
        }

        item {
            AnimatedVisibility(
                visible = !isExpanded && (settings.labShowFilePath || settings.labShowInstallInitiator),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                CouiCard(modifier = Modifier.padding(vertical = 6.dp)) {
                    if (settings.labShowFilePath) {
                        val path = runCatching {
                            val rawPath = primaryEntity.data.sourcePath()
                            if (selectedEntities.size > 1 && rawPath != null) {
                                rawPath.substringBeforeLast("/") + " (Multi-part)"
                            } else {
                                rawPath
                            }
                        }.getOrNull() ?: stringResource(R.string.installer_label_unknown)

                        CouiBasicItem(
                            title = stringResource(R.string.lab_show_apk_path_label),
                            summary = path,
                            onClick = {
                                scope.launch {
                                    clipboard.setClipEntry(ClipData.newPlainText("APK Path", path).toClipEntry())
                                    context.toast(R.string.copied_format, path)
                                }
                            },
                        )
                    }
                    if (settings.labShowFilePath && settings.labShowInstallInitiator) {
                        Divider()
                    }
                    if (settings.labShowInstallInitiator) {
                        val initiator = uiState.initiatorAppLabel ?: stringResource(R.string.installer_label_unknown)
                        CouiBasicItem(
                            title = stringResource(R.string.lab_show_install_initiator_label),
                            summary = initiator,
                            onClick = {
                                scope.launch {
                                    clipboard.setClipEntry(ClipData.newPlainText("Install Initiator", initiator).toClipEntry())
                                    context.toast(R.string.copied_format, initiator)
                                }
                            },
                        )
                    }
                }
            }
        }

        val canInstallBaseEntity = (primaryEntity as? AppEntity.BaseEntity)?.let { base ->
            if (entityToInstall != null) {
                base.minSdk?.toIntOrNull()?.let { sdk -> sdk <= Build.VERSION.SDK_INT } ?: true
            } else {
                isSplitUpdateMode
            }
        } ?: false
        val canInstallModuleEntity = (primaryEntity as? AppEntity.ModuleEntity)?.let {
            settings.enableModuleInstall
        } ?: false
        val canInstallSplitEntity = (primaryEntity as? AppEntity.SplitEntity)?.let {
            currentPackage.installedAppInfo != null
        } ?: false
        val canInstall = !installStateResult.isOplusOsdkIncompatible &&
            (canInstallBaseEntity || canInstallModuleEntity || canInstallSplitEntity)
        val showExpandButton = rawBaseEntity != null && settings.showExtendedMenu

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showExpandButton) {
                    CouiActionButton(
                        text = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.more),
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    CouiActionButton(
                        text = stringResource(R.string.cancel),
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                    )
                }
                CouiActionButton(
                    text = stringResource(buttonTextId),
                    onClick = onInstall,
                    onLongClick = onLongInstall?.let { longAction ->
                        {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            longAction()
                        }
                    },
                    enabled = canInstall,
                    primary = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CouiSdkComparison(
    entityToInstall: AppEntity,
    preInstallAppInfo: InstalledAppInfo?,
    displaySDK: Boolean,
    hideIdenticalComparison: Boolean,
) {
    AnimatedVisibility(visible = displaySDK) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            entityToInstall.targetSdk?.let { newTargetSdk ->
                CouiSdkInfoRow(
                    labelResId = R.string.installer_package_target_sdk_label,
                    newSdk = newTargetSdk,
                    oldSdk = preInstallAppInfo?.targetSdk?.toString(),
                    isArchived = preInstallAppInfo?.isArchived ?: false,
                    hideIdenticalComparison = hideIdenticalComparison,
                    type = "target",
                )
            }
            entityToInstall.minSdk?.let { newMinSdk ->
                CouiSdkInfoRow(
                    labelResId = R.string.installer_package_min_sdk_label,
                    newSdk = newMinSdk,
                    oldSdk = preInstallAppInfo?.minSdk?.toString(),
                    isUninstalled = preInstallAppInfo?.isUninstalled ?: false,
                    isArchived = preInstallAppInfo?.isArchived ?: false,
                    hideIdenticalComparison = hideIdenticalComparison,
                    type = "min",
                )
            }
        }
    }
}

@Composable
private fun CouiSdkInfoRow(
    @StringRes labelResId: Int,
    newSdk: String,
    oldSdk: String?,
    isUninstalled: Boolean = false,
    isArchived: Boolean = false,
    hideIdenticalComparison: Boolean,
    type: String,
) {
    val scheme = CouiTheme.colorScheme
    val newSdkInt = newSdk.toIntOrNull()
    val oldSdkInt = oldSdk?.toIntOrNull()
    val showComparison = oldSdkInt != null && newSdkInt != null &&
        (!hideIdenticalComparison || newSdkInt != oldSdkInt)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(labelResId),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = scheme.onSurface,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showComparison) {
                val oldText = when {
                    isUninstalled -> stringResource(R.string.old_version_uninstalled)
                    isArchived -> stringResource(R.string.old_version_archived)
                    else -> oldSdk.orEmpty()
                }
                Text(text = oldText, fontSize = 14.sp, color = scheme.onSurfaceVariant)
                Icon(
                    imageVector = AppIcons.ArrowIndicator,
                    contentDescription = "to",
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(16.dp),
                )
                Text(text = newSdk, fontSize = 14.sp, color = scheme.onSurface)
            } else {
                val isIncompatible = type == "min" && newSdkInt != null && newSdkInt > Build.VERSION.SDK_INT
                val color = if (isIncompatible) MaterialTheme.colorScheme.error else scheme.onSurfaceVariant
                Text(text = newSdk, color = color, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun CouiPrepareSettingsContent(viewModel: InstallerViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.viewSettings
    val config = uiState.config

    BackHandler {
        viewModel.dispatch(InstallerViewAction.HideMiuixSheetRightActionSettings)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .couiVerticalScroll(rememberScrollState())
            .padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CouiInstallerTipCard(stringResource(R.string.installer_temp_settings_tip))
        CouiCard(modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)) {
            CouiSwitchItem(
                title = stringResource(R.string.config_display_sdk_version),
                description = stringResource(
                    id = R.string.combined_description_format,
                    stringResource(id = R.string.config_display_sdk_version_desc),
                    stringResource(id = R.string.config_display_module_extra_info_desc),
                ),
                checked = config.displaySdk,
                onCheckedChange = { newValue -> viewModel.updateConfig { it.copy(displaySdk = newValue) } },
            )
            Divider()
            CouiSwitchItem(
                title = stringResource(R.string.config_display_size),
                description = stringResource(R.string.config_display_size_desc),
                checked = config.displaySize,
                onCheckedChange = { newValue -> viewModel.updateConfig { it.copy(displaySize = newValue) } },
            )
            Divider()
            CouiSwitchItem(
                title = stringResource(R.string.config_auto_delete),
                description = stringResource(R.string.config_auto_delete_desc),
                checked = config.autoDelete,
                onCheckedChange = { newValue -> viewModel.updateConfig { it.copy(autoDelete = newValue) } },
            )
            if (DeviceConfig.currentManufacturer == Manufacturer.OPPO ||
                DeviceConfig.currentManufacturer == Manufacturer.ONEPLUS
            ) {
                Divider()
                CouiSwitchItem(
                    title = stringResource(R.string.installer_show_oem_special),
                    description = stringResource(R.string.installer_show_oem_special_desc),
                    checked = settings.showOPPOSpecial,
                    onCheckedChange = { newValue ->
                        viewModel.dispatch(InstallerViewAction.SetTempShowOPPOSpecial(newValue))
                    },
                )
            }
        }
        CouiCard(modifier = Modifier.padding(bottom = 8.dp)) {
            CouiSwitchItem(
                title = stringResource(R.string.lab_show_apk_path),
                description = stringResource(R.string.lab_show_apk_path_desc),
                checked = settings.labShowFilePath,
                onCheckedChange = { newValue ->
                    viewModel.dispatch(InstallerViewAction.SetTempLabShowFilePath(newValue))
                },
            )
            Divider()
            CouiSwitchItem(
                title = stringResource(R.string.lab_show_install_initiator),
                description = stringResource(R.string.lab_show_install_initiator_desc),
                checked = settings.labShowInstallInitiator,
                onCheckedChange = { newValue ->
                    viewModel.dispatch(InstallerViewAction.SetTempLabShowInstallInitiator(newValue))
                },
            )
        }
    }
}

@Composable
fun CouiInstallPreparePermissionContent(viewModel: InstallerViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentPackage = uiState.analysisResults.find { it.packageName == uiState.currentPackageName }
    val entity = currentPackage?.appEntities
        ?.filter { it.selected }
        ?.map { it.app }
        ?.sortedBest()
        ?.firstOrNull()
    val permissionList = remember(entity) {
        (entity as? AppEntity.BaseEntity)?.permissions?.sorted()?.toMutableStateList() ?: mutableStateListOf()
    }

    BackHandler { onBack() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CouiCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .padding(vertical = 6.dp),
        ) {
            LazyColumn(overscrollEffect = rememberCouiOverscrollEffect()) {
                items(permissionList) { permission ->
                    val context = LocalContext.current
                    val permissionLabel = remember(permission) { context.getBestPermissionLabel(permission) }
                    CouiBasicItem(
                        title = permissionLabel,
                        summary = permission,
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 12.dp),
        ) {
            CouiActionButton(
                text = stringResource(R.string.back),
                onClick = onBack,
                primary = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
