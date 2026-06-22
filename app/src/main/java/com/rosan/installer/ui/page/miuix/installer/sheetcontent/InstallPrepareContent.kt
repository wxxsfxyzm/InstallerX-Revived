// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.installer.sheetcontent

import android.content.ClipData
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.core.env.DeviceConfig
import com.rosan.installer.core.device.model.Manufacturer
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.source.DataType
import com.rosan.installer.domain.engine.model.packageinfo.InstalledAppInfo
import com.rosan.installer.domain.engine.model.packageinfo.sortedBest
import com.rosan.installer.domain.engine.model.install.sourcePath
import com.rosan.installer.domain.engine.usecase.AnalyzeInstallStateUseCase
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.mapper.InstallNoticeResources
import com.rosan.installer.ui.page.main.installer.mapper.InstallStateUiMapper
import com.rosan.installer.ui.page.miuix.installer.components.AdaptiveInfoRow
import com.rosan.installer.ui.page.miuix.installer.components.AppInfoSlot
import com.rosan.installer.ui.page.miuix.installer.components.AppInfoState
import com.rosan.installer.ui.page.miuix.widgets.MiuixInfoChipGroup
import com.rosan.installer.ui.page.miuix.widgets.MiuixInstallerTipCard
import com.rosan.installer.ui.page.miuix.widgets.MiuixNavigationItemWidget
import com.rosan.installer.ui.theme.miuixSheetCardColors
import com.rosan.installer.ui.util.formatSize
import com.rosan.installer.ui.util.isGestureNavigation
import com.rosan.installer.util.toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor

@Composable
fun InstallPrepareContent(
    viewModel: InstallerViewModel,
    appInfo: AppInfoState,
    onCancel: () -> Unit,
    onInstall: () -> Unit,
    onLongInstall: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val config = uiState.config
    val currentPackageName = uiState.currentPackageName
    val currentPackage = uiState.analysisResults.find { it.packageName == currentPackageName }
    val settings = uiState.viewSettings

    var isExpanded by remember { mutableStateOf(false) }

    if (currentPackage == null) {
        LoadingContent(statusText = stringResource(id = R.string.loading))
        return
    }

    val allEntities = currentPackage.appEntities
        .filter { it.selected } // Always include selected entities
        .map { it.app }

    val selectedEntities = currentPackage.appEntities
        .filter { it.selected }
        .map { it.app }
    val rawBaseEntity = currentPackage.appEntities
        .map { it.app }
        .filterIsInstance<AppEntity.BaseEntity>()
        .firstOrNull()
    val allAvailableApps = currentPackage.appEntities.map { it.app }
    val primaryEntity = selectedEntities.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
        ?: selectedEntities.filterIsInstance<AppEntity.ModuleEntity>().firstOrNull()
        ?: selectedEntities.filterIsInstance<AppEntity.SplitEntity>().firstOrNull()
        ?: selectedEntities.firstOrNull()
        ?: allAvailableApps.sortedBest().firstOrNull()
    if (primaryEntity == null) {
        LoadingContent(statusText = "No main app entity found")
        return
    }

    val entityToInstall = allEntities.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
    val containerType = primaryEntity.sourceType
    val totalSelectedSize = allEntities.sumOf { it.size }

    val isModuleSelected = selectedEntities.any { it is AppEntity.ModuleEntity }
    val isPureSplit = primaryEntity is AppEntity.SplitEntity
    val isBundleSplitUpdate = primaryEntity is AppEntity.BaseEntity &&
            entityToInstall == null &&
            selectedEntities.isNotEmpty() &&
            !isModuleSelected
    val isSplitUpdateMode = (isBundleSplitUpdate || isPureSplit) && currentPackage.installedAppInfo != null

    val primaryColor = MiuixTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error

    val tagDowngrade = stringResource(R.string.tag_downgrade)
    val downgradeWarning = stringResource(R.string.installer_prepare_type_downgrade)
    val tagSignature = stringResource(R.string.tag_signature)
    val tagSignatureMatch = stringResource(R.string.tag_signature_match)
    val tagSignatureRotation = stringResource(R.string.tag_signature_rotation)
    val tagSignatureRotationUnconfirmed = stringResource(R.string.tag_signature_rotation_unconfirmed)
    val sigNewInstall = stringResource(R.string.installer_prepare_signature_new_install)
    val sigMatch = stringResource(R.string.installer_prepare_signature_match)
    val sigRotationCompatible = stringResource(R.string.installer_prepare_signature_rotation_compatible)
    val sigCandidateRotationUnconfirmed =
        stringResource(R.string.installer_prepare_signature_candidate_rotation_unconfirmed)
    val sigMismatchWarning = stringResource(R.string.installer_prepare_signature_mismatch)
    val sigUnknownWarning = stringResource(R.string.installer_prepare_signature_unknown)
    val sigAnalysisIssue = stringResource(R.string.installer_prepare_signature_analysis_issue)
    val labelPendingSignature = stringResource(R.string.installer_signature_pending_package)
    val labelInstalledSignature = stringResource(R.string.installer_signature_installed_package)
    val labelSignatureAnalysisIssues = stringResource(R.string.installer_signature_analysis_issues)
    val labelSignatureVerificationFailedFiles = stringResource(R.string.installer_signature_verification_failed_files)
    val labelSignatureSplitMismatchFiles = stringResource(R.string.installer_signature_split_mismatch_files)
    val labelSignatureDuplicateSplitNames = stringResource(R.string.installer_signature_duplicate_split_names)
    val labelSignatureSchemes = stringResource(R.string.installer_signature_schemes)
    val labelSignatureCertificate = stringResource(R.string.installer_signature_certificate)
    val labelSignatureCurrentCertificate = stringResource(R.string.installer_signature_current_certificate)
    val labelSignatureCertificateLineage = stringResource(R.string.installer_signature_certificate_lineage)
    val labelSignatureLineageCertificate = stringResource(R.string.installer_signature_lineage_certificate)
    val labelSignatureCurrentMarker = stringResource(R.string.installer_signature_current_marker)
    val labelSignatureSha256 = stringResource(R.string.installer_signature_sha256)
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
    val tagArch32 = stringResource(R.string.tag_arch_32)
    val textArch32 = stringResource(R.string.installer_prepare_arch_32_notice)
    val tagEmulated = stringResource(R.string.tag_arch_emulated)
    val textArchMismatch = stringResource(R.string.installer_prepare_arch_mismatch_notice)
    val tagIdentical = stringResource(R.string.tag_identical)
    val textIdentical = stringResource(R.string.installer_prepare_identical_notice)
    val tagXposed = stringResource(R.string.tag_xposed)
    val labelXposedMinApi = stringResource(R.string.installer_xposed_min_api)
    val labelXposedTargetApi = stringResource(R.string.installer_xposed_target_api)

    val installResources = remember(errorColor, primaryColor) {
        InstallNoticeResources(
            tagDowngrade = tagDowngrade,
            textDowngrade = downgradeWarning,
            tagSignature = tagSignature,
            tagSignatureMatch = tagSignatureMatch,
            tagSignatureRotation = tagSignatureRotation,
            tagSignatureRotationUnconfirmed = tagSignatureRotationUnconfirmed,
            textSigNewInstall = sigNewInstall,
            textSigMatch = sigMatch,
            textSigRotationCompatible = sigRotationCompatible,
            textSigCandidateRotationUnconfirmed = sigCandidateRotationUnconfirmed,
            textSigMismatch = sigMismatchWarning,
            textSigUnknown = sigUnknownWarning,
            textSigAnalysisIssue = sigAnalysisIssue,
            labelPendingSignature = labelPendingSignature,
            labelInstalledSignature = labelInstalledSignature,
            labelSignatureAnalysisIssues = labelSignatureAnalysisIssues,
            labelSignatureVerificationFailedFiles = labelSignatureVerificationFailedFiles,
            labelSignatureSplitMismatchFiles = labelSignatureSplitMismatchFiles,
            labelSignatureDuplicateSplitNames = labelSignatureDuplicateSplitNames,
            labelSignatureSchemes = labelSignatureSchemes,
            labelSignatureCertificate = labelSignatureCertificate,
            labelSignatureCurrentCertificate = labelSignatureCurrentCertificate,
            labelSignatureCertificateLineage = labelSignatureCertificateLineage,
            labelSignatureLineageCertificate = labelSignatureLineageCertificate,
            labelSignatureCurrentMarker = labelSignatureCurrentMarker,
            labelSignatureSha256 = labelSignatureSha256,
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
            primaryColor = primaryColor
        )
    }

    // Inject the pure domain use case
    val analyzeInstallStateUseCase = koinInject<AnalyzeInstallStateUseCase>()

    // Instantiate the UI mapper with the required Compose resources
    val installStateUiMapper = remember(installResources) {
        InstallStateUiMapper(installResources)
    }

    // Execute domain logic and map to UI state within the remember block
    val installStateResult = remember(
        currentPackage,
        entityToInstall,
        isSplitUpdateMode,
        containerType,
        settings.checkAppSignature,
        settings.showSignatureInfoOnMatch,
        settings.showSignatureDetails,
        settings.detectXposedModule,
        installStateUiMapper
    ) {
        // 1. Get pure domain state
        val domainState = analyzeInstallStateUseCase(
            currentPackage = currentPackage,
            entityToInstall = entityToInstall,
            primaryEntity = primaryEntity,
            isSplitUpdateMode = isSplitUpdateMode,
            containerType = containerType,
            systemArch = DeviceConfig.currentArchitecture,
            systemSdkInt = Build.VERSION.SDK_INT,
            checkAppSignature = settings.checkAppSignature,
            showSignatureInfoOnMatch = settings.showSignatureInfoOnMatch,
            showSignatureDetails = settings.showSignatureDetails,
            detectXposedModule = settings.detectXposedModule
        )

        // 2. Map to UI state
        installStateUiMapper.mapToUiState(domainState)
    }
    val notices = installStateResult.notices
    val buttonTextId = installStateResult.buttonTextId

    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            AppInfoSlot(
                appInfo = appInfo,
                onIconClick = {
                    // Trigger the share action using the already resolved primaryEntity
                    if (settings.labTapIconToShare)
                        viewModel.dispatch(InstallerViewAction.ShareApp(primaryEntity))
                }
            )
        }
        item { Spacer(modifier = Modifier.height(4.dp)) }
        item {
            MiuixInfoChipGroup(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                notices = notices
            )
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = miuixSheetCardColors()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (primaryEntity) {
                        is AppEntity.BaseEntity -> {
                            AdaptiveInfoRow(
                                labelResId = R.string.installer_version_name_label,
                                newValue = primaryEntity.versionName,
                                oldValue = currentPackage.installedAppInfo?.versionName,
                                isUninstalled = currentPackage.installedAppInfo?.isUninstalled ?: false,
                                isArchived = currentPackage.installedAppInfo?.isArchived ?: false
                            )
                            AdaptiveInfoRow(
                                labelResId = R.string.installer_version_code_label,
                                newValue = primaryEntity.versionCode.toString(),
                                oldValue = currentPackage.installedAppInfo?.versionCode?.toString(),
                                isUninstalled = currentPackage.installedAppInfo?.isUninstalled ?: false,
                                isArchived = currentPackage.installedAppInfo?.isArchived ?: false
                            )
                            SDKComparison(
                                entityToInstall = primaryEntity,
                                preInstallAppInfo = currentPackage.installedAppInfo,
                                displaySDK = config.displaySdk
                            )

                            AnimatedVisibility(visible = config.displaySize && primaryEntity.size > 0) {
                                val oldSize = currentPackage.installedAppInfo?.packageSize ?: 0L
                                val oldSizeStr = if (oldSize > 0 && !isSplitUpdateMode) oldSize.formatSize() else null
                                val newSizeStr = totalSelectedSize.formatSize()

                                AdaptiveInfoRow(
                                    labelResId = R.string.installer_package_size_label,
                                    newValue = newSizeStr,
                                    oldValue = oldSizeStr
                                )
                            }

                            val isOppoOrOnePlus = DeviceConfig.currentManufacturer == Manufacturer.OPPO ||
                                    DeviceConfig.currentManufacturer == Manufacturer.ONEPLUS
                            val shouldShowOppoRow = isOppoOrOnePlus &&
                                    settings.showOPPOSpecial &&
                                    primaryEntity.sourceType == DataType.APK &&
                                    primaryEntity.minOsdkVersion != null

                            AnimatedVisibility(visible = shouldShowOppoRow) {
                                // At this point, minOsdkVersion is guaranteed to be non-null
                                primaryEntity.minOsdkVersion?.let { version ->
                                    AdaptiveInfoRow(
                                        labelResId = R.string.installer_package_minOsdkVersion_label,
                                        newValue = version,
                                        oldValue = null
                                    )
                                }
                            }
                        }

                        is AppEntity.ModuleEntity -> {
                            val installedModuleInfo = currentPackage.installedModuleInfo
                            AdaptiveInfoRow(
                                labelResId = R.string.installer_version_name_label,
                                newValue = primaryEntity.version,
                                oldValue = installedModuleInfo?.version
                            )
                            AdaptiveInfoRow(
                                labelResId = R.string.installer_version_code_label,
                                newValue = primaryEntity.versionCode.toString(),
                                oldValue = installedModuleInfo?.versionCode?.toString()
                            )
                            AnimatedVisibility(visible = config.displaySdk) {
                                AdaptiveInfoRow(
                                    labelResId = R.string.installer_module_author_label,
                                    newValue = primaryEntity.author,
                                    oldValue = null
                                )
                            }
                            AnimatedVisibility(visible = config.displaySize) {
                                val newSizeStr = totalSelectedSize.formatSize()
                                AdaptiveInfoRow(
                                    labelResId = R.string.installer_package_size_label,
                                    newValue = newSizeStr,
                                    oldValue = null
                                )
                            }
                        }

                        is AppEntity.SplitEntity -> {
                            // Split Name
                            AdaptiveInfoRow(
                                labelResId = R.string.installer_split_name_label,
                                newValue = primaryEntity.splitName,
                                oldValue = null
                            )

                            // SDK Comparison (If splits define min/target SDK)
                            SDKComparison(
                                entityToInstall = primaryEntity,
                                preInstallAppInfo = currentPackage.installedAppInfo,
                                displaySDK = config.displaySdk
                            )

                            // Size
                            AnimatedVisibility(visible = config.displaySize) {
                                val newSizeStr = totalSelectedSize.formatSize()
                                AdaptiveInfoRow(
                                    labelResId = R.string.installer_package_size_label,
                                    newValue = newSizeStr,
                                    oldValue = null // Don't compare with full app size, meaningless
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
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = miuixSheetCardColors()
                ) {
                    // Permissions List
                    if (rawBaseEntity?.permissions?.isNotEmpty() == true)
                        MiuixNavigationItemWidget(
                            title = stringResource(R.string.permission_list),
                            description = stringResource(R.string.permission_list_desc),
                            //insideMargin = PaddingValues(12.dp),
                            onClick = { viewModel.dispatch(InstallerViewAction.ShowMiuixPermissionList) },
                        )

                    // Install Options
                    if (config.authorizer != Authorizer.Dhizuku &&
                        config.authorizer != Authorizer.None
                    )
                        MiuixNavigationItemWidget(
                            title = stringResource(R.string.config_label_install_options),
                            description = stringResource(R.string.config_label_install_options_desc),
                            //insideMargin = PaddingValues(12.dp),
                            onClick = { viewModel.dispatch(InstallerViewAction.InstallExtendedMenu) }
                        )

                    // Select Splits
                    val hasSplits = currentPackage.appEntities.size > 1
                    if (hasSplits) {
                        MiuixNavigationItemWidget(
                            title = stringResource(R.string.installer_select_split),
                            description = stringResource(R.string.installer_select_split_desc),
                            //insideMargin = PaddingValues(12.dp),
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
                exit = fadeOut() + shrinkVertically()
            ) {
                MiuixInstallerTipCard(text = stringResource(R.string.installer_splits_invalid_tip))
            }
        }

        item {
            AnimatedVisibility(
                visible = isSplitUpdateMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                MiuixInstallerTipCard(text = stringResource(R.string.installer_splits_only_tip))
            }
        }

        item {
            AnimatedVisibility(
                visible = (primaryEntity is AppEntity.ModuleEntity) &&
                        primaryEntity.description.isNotBlank() &&
                        config.displaySdk,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                MiuixInstallerTipCard((primaryEntity as AppEntity.ModuleEntity).description)
            }
        }

        // Display lab info card if either setting is enabled
        item {
            AnimatedVisibility(
                visible = !isExpanded && (settings.labShowFilePath || settings.labShowInstallInitiator),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = miuixSheetCardColors()
                ) {
                    if (settings.labShowFilePath) {
                        // Safely extract the source path
                        val path = runCatching {
                            val rawPath = primaryEntity.data.sourcePath()
                            // If multiple files are selected (like Splits), show the parent directory
                            if (selectedEntities.size > 1 && rawPath != null) {
                                rawPath.substringBeforeLast("/") + " (Multi-part)"
                            } else {
                                rawPath
                            }
                        }.getOrNull() ?: stringResource(R.string.installer_label_unknown)

                        BasicComponent(
                            title = stringResource(R.string.lab_show_apk_path_label),
                            summary = path,
                            onClick = {
                                scope.launch {
                                    val clipData = ClipData.newPlainText("APK Path", path)
                                    clipboard.setClipEntry(clipData.toClipEntry())
                                    context.toast(R.string.copied_format, path)
                                }
                            }
                        )
                    }

                    if (settings.labShowInstallInitiator) {
                        // Use the runtime field initiatorPackageName from ConfigModel
                        val initiator = uiState.initiatorAppLabel ?: stringResource(R.string.installer_label_unknown)

                        BasicComponent(
                            title = stringResource(R.string.lab_show_install_initiator_label),
                            summary = initiator,
                            onClick = {
                                scope.launch {
                                    val clipData = ClipData.newPlainText("Install Initiator", initiator)
                                    clipboard.setClipEntry(clipData.toClipEntry())
                                    context.toast(R.string.copied_format, initiator)
                                }
                            }
                        )
                    }
                }
            }
        }

        val canInstallBaseEntity = (primaryEntity as? AppEntity.BaseEntity)?.let { base ->
            if (entityToInstall != null) {
                // Installing Base: Check SDK
                base.minSdk?.toIntOrNull()?.let { sdk -> sdk <= Build.VERSION.SDK_INT } ?: true
            } else {
                // Bundle Split Update: Allowed if installed
                isSplitUpdateMode
            }
        } ?: false

        val canInstallModuleEntity = (primaryEntity as? AppEntity.ModuleEntity)?.let {
            settings.enableModuleInstall
        } ?: false

        val canInstallSplitEntity = (primaryEntity as? AppEntity.SplitEntity)?.let {
            currentPackage.installedAppInfo != null
        } ?: false

        val canInstall = canInstallBaseEntity || canInstallModuleEntity || canInstallSplitEntity

        // Even if we can't install (e.g. because Base is deselected), we might want to expand the menu to fix the selection.
        // We show the button if rawBaseEntity exists (Bundle/APK) and settings allow it.
        val showExpandButton = rawBaseEntity != null && settings.showExtendedMenu

        item {
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            var hasLongPressed by remember { mutableStateOf(false) }
            val viewConfiguration = LocalViewConfiguration.current
            val hapticFeedback = LocalHapticFeedback.current

            // Handle the long press delay logic
            LaunchedEffect(isPressed) {
                if (isPressed) {
                    hasLongPressed = false
                    // Wait for the system's default long press duration
                    delay(viewConfiguration.longPressTimeoutMillis)
                    hasLongPressed = true
                    // Perform haptic feedback immediately before the action
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    // Trigger the long press action
                    onLongInstall()
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(top = 24.dp, bottom = if (isGestureNavigation()) 24.dp else 0.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showExpandButton)
                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        text = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.more),
                        colors = ButtonDefaults.textButtonColors(
                            color = if (isDynamicColor) MiuixTheme.colorScheme.secondaryContainer else MiuixTheme.colorScheme.secondaryVariant,
                            textColor = if (isDynamicColor) MiuixTheme.colorScheme.onSecondaryContainer else MiuixTheme.colorScheme.onSecondaryVariant
                        ),
                        modifier = Modifier.weight(1f),
                    )
                else
                    TextButton(
                        onClick = onCancel,
                        text = stringResource(R.string.cancel),
                        colors = ButtonDefaults.textButtonColors(
                            color = if (isDynamicColor) MiuixTheme.colorScheme.secondaryContainer else MiuixTheme.colorScheme.secondaryVariant,
                            textColor = if (isDynamicColor) MiuixTheme.colorScheme.onSecondaryContainer else MiuixTheme.colorScheme.onSecondaryVariant
                        ),
                        modifier = Modifier.weight(1f),
                    )
                TextButton(
                    onClick = {
                        // Only trigger normal install if long press didn't happen
                        if (!hasLongPressed) onInstall()
                    },
                    enabled = canInstall,
                    text = stringResource(buttonTextId),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    modifier = Modifier.weight(1f),
                    interactionSource = interactionSource
                )
            }
        }
    }
}

@Composable
private fun SDKComparison(
    entityToInstall: AppEntity,
    preInstallAppInfo: InstalledAppInfo?,
    displaySDK: Boolean
) {
    AnimatedVisibility(visible = displaySDK) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Target SDK
            entityToInstall.targetSdk?.let { newTargetSdk ->
                SdkInfoRow(
                    labelResId = R.string.installer_package_target_sdk_label,
                    newSdk = newTargetSdk,
                    oldSdk = preInstallAppInfo?.targetSdk?.toString(),
                    isArchived = preInstallAppInfo?.isArchived ?: false,
                    type = "target"
                )
            }
            // Min SDK
            entityToInstall.minSdk?.let { newMinSdk ->
                SdkInfoRow(
                    labelResId = R.string.installer_package_min_sdk_label,
                    newSdk = newMinSdk,
                    oldSdk = preInstallAppInfo?.minSdk?.toString(),
                    isUninstalled = preInstallAppInfo?.isUninstalled ?: false,
                    isArchived = preInstallAppInfo?.isArchived ?: false,
                    type = "min"
                )
            }
        }
    }
}

@Composable
private fun SdkInfoRow(
    @StringRes labelResId: Int,
    newSdk: String,
    oldSdk: String?,
    isUninstalled: Boolean = false,
    isArchived: Boolean = false,
    type: String // "min" or "target"
) {
    val newSdkInt = newSdk.toIntOrNull()
    val oldSdkInt = oldSdk?.toIntOrNull()
    val showComparison = oldSdkInt != null && newSdkInt != null && newSdkInt != oldSdkInt

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label to the left.
        Text(
            text = stringResource(labelResId),
            style = MiuixTheme.textStyles.body2,
            fontWeight = FontWeight.SemiBold
        )

        // Label to the right.
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showComparison) {
                // val isDowngrade = newSdkInt < oldSdkInt
                // val isIncompatible = type == "min" && newSdkInt > Build.VERSION.SDK_INT
                // val color = if (isDowngrade || isIncompatible) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

                val oldText = when {
                    isUninstalled -> stringResource(R.string.old_version_uninstalled)
                    isArchived -> stringResource(R.string.old_version_archived)
                    else -> oldSdk
                }

                Text(text = oldText, style = MiuixTheme.textStyles.body2)

                Icon(
                    imageVector = AppIcons.ArrowIndicator,
                    contentDescription = "to",
                    // tint = color,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(16.dp)
                )

                Text(text = newSdk/*, color = color*/, style = MiuixTheme.textStyles.body2)
            } else {
                val isIncompatible = type == "min" && newSdkInt != null && newSdkInt > Build.VERSION.SDK_INT
                val color = if (isIncompatible) MaterialTheme.colorScheme.error else Color.Unspecified

                Text(text = newSdk, color = color, style = MiuixTheme.textStyles.body2)
            }
        }
    }
}
