// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.installer.sheetcontent

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.core.env.DeviceConfig
import com.rosan.installer.domain.device.model.Manufacturer
import com.rosan.installer.domain.engine.model.AppEntity
import com.rosan.installer.domain.engine.model.DataType
import com.rosan.installer.domain.engine.model.InstalledAppInfo
import com.rosan.installer.domain.engine.model.sortedBest
import com.rosan.installer.domain.engine.usecase.AnalyzeInstallStateUseCase
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.dialog.inner.InstallNoticeResources
import com.rosan.installer.ui.page.main.installer.mapper.InstallStateUiMapper
import com.rosan.installer.ui.page.miuix.widgets.MiuixInfoChipGroup
import com.rosan.installer.ui.page.miuix.widgets.MiuixInstallerTipCard
import com.rosan.installer.ui.page.miuix.widgets.MiuixNavigationItemWidget
import com.rosan.installer.ui.theme.InstallerTheme
import com.rosan.installer.ui.theme.miuixSheetCardColorDark
import com.rosan.installer.ui.util.formatSize
import com.rosan.installer.ui.util.isGestureNavigation
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InstallPrepareContent(
    session: InstallerSessionRepository,
    viewModel: InstallerViewModel,
    appInfo: AppInfoState,
    onCancel: () -> Unit,
    onInstall: () -> Unit
) {
    val isDarkMode = InstallerTheme.isDark
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentPackageName = uiState.currentPackageName
    val currentPackage = session.analysisResults.find { it.packageName == currentPackageName }
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
    val sigMismatchWarning = stringResource(R.string.installer_prepare_signature_mismatch)
    val sigUnknownWarning = stringResource(R.string.installer_prepare_signature_unknown)
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
            textSigMismatch = sigMismatchWarning,
            textSigUnknown = sigUnknownWarning,
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
    val (notices, buttonTextId) = remember(
        currentPackage,
        entityToInstall,
        isSplitUpdateMode,
        containerType,
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
            systemSdkInt = Build.VERSION.SDK_INT
        )

        // 2. Map to UI state
        val uiState = installStateUiMapper.mapToUiState(domainState)

        // 3. Destructure the data class components
        Pair(uiState.notices, uiState.buttonTextId)
    }

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
                colors = CardColors(
                    color = if (isDynamicColor) MiuixTheme.colorScheme.surfaceContainer else
                        if (isDarkMode) miuixSheetCardColorDark else Color.White,
                    contentColor = MiuixTheme.colorScheme.onSurface
                )
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
                                installer = session
                            )

                            AnimatedVisibility(visible = session.config.displaySize && primaryEntity.size > 0) {
                                val oldSize = currentPackage.installedAppInfo?.packageSize ?: 0L
                                val oldSizeStr = if (oldSize > 0 && !isSplitUpdateMode) oldSize.formatSize() else null
                                val newSizeStr = totalSelectedSize.formatSize()

                                AdaptiveInfoRow(
                                    labelResId = R.string.installer_package_size_label,
                                    newValue = newSizeStr,
                                    oldValue = oldSizeStr
                                )
                            }

                            if (DeviceConfig.currentManufacturer == Manufacturer.OPPO || DeviceConfig.currentManufacturer == Manufacturer.ONEPLUS) {
                                AnimatedVisibility(visible = settings.showOPPOSpecial && primaryEntity.sourceType == DataType.APK) {
                                    primaryEntity.minOsdkVersion?.let {
                                        AdaptiveInfoRow(
                                            labelResId = R.string.installer_package_minOsdkVersion_label,
                                            newValue = it,
                                            oldValue = null
                                        )
                                    }
                                }
                            }
                        }

                        is AppEntity.ModuleEntity -> {
                            AdaptiveInfoRow(
                                labelResId = R.string.installer_version_name_label,
                                newValue = primaryEntity.version,
                                oldValue = null
                            )
                            AdaptiveInfoRow(
                                labelResId = R.string.installer_version_code_label,
                                newValue = primaryEntity.versionCode.toString(),
                                oldValue = null
                            )
                            AnimatedVisibility(visible = session.config.displaySdk) {
                                AdaptiveInfoRow(
                                    labelResId = R.string.installer_module_author_label,
                                    newValue = primaryEntity.author,
                                    oldValue = null
                                )
                            }
                            AnimatedVisibility(visible = session.config.displaySize) {
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
                                installer = session
                            )

                            // Size
                            AnimatedVisibility(visible = session.config.displaySize) {
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
                    colors = CardColors(
                        color = if (isDynamicColor) MiuixTheme.colorScheme.surfaceContainer else
                            if (isDarkMode) miuixSheetCardColorDark else Color.White,
                        contentColor = MiuixTheme.colorScheme.onSurface
                    )
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
                    if (session.config.authorizer != Authorizer.Dhizuku &&
                        session.config.authorizer != Authorizer.None
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
                        session.config.displaySdk,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                MiuixInstallerTipCard((primaryEntity as AppEntity.ModuleEntity).description)
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
                        text = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
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
                    onClick = onInstall,
                    enabled = canInstall,
                    text = stringResource(buttonTextId),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AdaptiveInfoRow(
    @StringRes labelResId: Int,
    newValue: String,
    oldValue: String?,
    isUninstalled: Boolean = false,
    isArchived: Boolean = false
) {
    val showComparison = oldValue != null && newValue != oldValue
    val oldTextContent = when {
        isArchived -> stringResource(R.string.old_version_archived)
        isUninstalled -> if (oldValue.isNullOrEmpty()) stringResource(R.string.old_version_uninstalled) else oldValue
        else -> oldValue.orEmpty()
    }

    Layout(
        content = {
            // Index 0: Label
            Text(
                text = stringResource(labelResId),
                style = MiuixTheme.textStyles.body2,
                fontWeight = FontWeight.SemiBold
            )

            if (showComparison) {
                // Index 1: Old text
                Text(
                    text = oldTextContent,
                    style = MiuixTheme.textStyles.body2,
                    textAlign = TextAlign.End
                )
                // Index 2: Arrow
                Icon(
                    imageVector = AppIcons.ArrowIndicator,
                    contentDescription = "to",
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(16.dp)
                )
                // Index 3: New text
                Text(
                    text = newValue,
                    style = MiuixTheme.textStyles.body2,
                    textAlign = TextAlign.End
                )
            } else {
                // Index 1: Single text when no comparison
                Text(
                    text = newValue,
                    style = MiuixTheme.textStyles.body2,
                    textAlign = TextAlign.End
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) { measurables, constraints ->
        val spacing = 16.dp.roundToPx()

        // Measure label exactly once
        val labelPlaceable = measurables[0].measure(Constraints(minWidth = 0, maxWidth = constraints.maxWidth))

        if (showComparison) {
            val oldMeasurable = measurables[1]
            val arrowMeasurable = measurables[2]
            val newMeasurable = measurables[3]

            // Use Intrinsic measurements to check required width WITHOUT calling measure() multiple times
            val oldMaxWidthReq = oldMeasurable.maxIntrinsicWidth(constraints.maxHeight)
            val arrowMaxWidthReq = arrowMeasurable.maxIntrinsicWidth(constraints.maxHeight)
            val newMaxWidthReq = newMeasurable.maxIntrinsicWidth(constraints.maxHeight)

            val totalSingleLineWidth = labelPlaceable.width + spacing + oldMaxWidthReq + arrowMaxWidthReq + newMaxWidthReq

            if (totalSingleLineWidth <= constraints.maxWidth) {
                // Single line mode: Space is sufficient. We can safely measure them now.
                val oldPlaceable = oldMeasurable.measure(Constraints(minWidth = 0, maxWidth = oldMaxWidthReq))
                val arrowPlaceable = arrowMeasurable.measure(Constraints(minWidth = 0, maxWidth = arrowMaxWidthReq))
                val newPlaceable = newMeasurable.measure(Constraints(minWidth = 0, maxWidth = newMaxWidthReq))

                val height = maxOf(labelPlaceable.height, oldPlaceable.height, arrowPlaceable.height, newPlaceable.height)

                layout(constraints.maxWidth, height) {
                    labelPlaceable.placeRelative(
                        x = 0,
                        y = Alignment.CenterVertically.align(labelPlaceable.height, height)
                    )

                    var currentX = constraints.maxWidth

                    currentX -= newPlaceable.width
                    newPlaceable.placeRelative(
                        x = currentX,
                        y = Alignment.CenterVertically.align(newPlaceable.height, height)
                    )

                    currentX -= arrowPlaceable.width
                    arrowPlaceable.placeRelative(
                        x = currentX,
                        y = Alignment.CenterVertically.align(arrowPlaceable.height, height)
                    )

                    currentX -= oldPlaceable.width
                    oldPlaceable.placeRelative(
                        x = currentX,
                        y = Alignment.CenterVertically.align(oldPlaceable.height, height)
                    )
                }
            } else {
                // Stacked mode: Space is insufficient. Two rows.

                // Line 1: Old text shares the row with the Label
                val oldMaxWidth = maxOf(0, constraints.maxWidth - labelPlaceable.width - spacing)
                val oldPlaceable = oldMeasurable.measure(Constraints(minWidth = 0, maxWidth = oldMaxWidth))

                // Line 2: New text takes full width minus the arrow
                val arrowPlaceable = arrowMeasurable.measure(Constraints())
                val newMaxWidth = maxOf(0, constraints.maxWidth - arrowPlaceable.width)
                val newPlaceable = newMeasurable.measure(Constraints(minWidth = 0, maxWidth = newMaxWidth))

                val verticalSpacing = 4.dp.roundToPx()
                val line1Height = maxOf(labelPlaceable.height, oldPlaceable.height)
                val line2Height = maxOf(arrowPlaceable.height, newPlaceable.height)
                val totalHeight = line1Height + verticalSpacing + line2Height

                layout(constraints.maxWidth, totalHeight) {
                    // Place Line 1
                    labelPlaceable.placeRelative(
                        x = 0,
                        y = Alignment.CenterVertically.align(labelPlaceable.height, line1Height)
                    )
                    oldPlaceable.placeRelative(
                        x = constraints.maxWidth - oldPlaceable.width,
                        y = Alignment.CenterVertically.align(oldPlaceable.height, line1Height)
                    )

                    // Place Line 2
                    val line2Y = line1Height + verticalSpacing
                    newPlaceable.placeRelative(
                        x = constraints.maxWidth - newPlaceable.width,
                        y = line2Y + Alignment.CenterVertically.align(newPlaceable.height, line2Height)
                    )
                    // Anchor arrow directly to the left of the new text
                    arrowPlaceable.placeRelative(
                        x = constraints.maxWidth - newPlaceable.width - arrowPlaceable.width,
                        y = line2Y + Alignment.CenterVertically.align(arrowPlaceable.height, line2Height)
                    )
                }
            }
        } else {
            // Single value mode
            val valueMaxWidth = maxOf(0, constraints.maxWidth - labelPlaceable.width - spacing)
            val valuePlaceable = measurables[1].measure(Constraints(minWidth = 0, maxWidth = valueMaxWidth))

            val height = maxOf(labelPlaceable.height, valuePlaceable.height)
            layout(constraints.maxWidth, height) {
                labelPlaceable.placeRelative(
                    x = 0,
                    y = Alignment.CenterVertically.align(labelPlaceable.height, height)
                )
                valuePlaceable.placeRelative(
                    x = constraints.maxWidth - valuePlaceable.width,
                    y = Alignment.CenterVertically.align(valuePlaceable.height, height)
                )
            }
        }
    }
}

@Composable
private fun SDKComparison(
    entityToInstall: AppEntity,
    preInstallAppInfo: InstalledAppInfo?,
    installer: InstallerSessionRepository
) {
    AnimatedVisibility(visible = installer.config.displaySdk) {
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