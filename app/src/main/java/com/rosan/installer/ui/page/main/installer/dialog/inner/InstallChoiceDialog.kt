// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer.dialog.inner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.data.engine.parser.getDisplayName
import com.rosan.installer.data.engine.parser.getSplitDisplayName
import com.rosan.installer.domain.engine.model.install.MmzSelectionMode
import com.rosan.installer.domain.engine.model.install.SessionMode
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.packageinfo.PackageAnalysisResult
import com.rosan.installer.domain.engine.model.source.DataType
import com.rosan.installer.domain.session.model.SelectInstallEntity
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.dialog.DialogButton
import com.rosan.installer.ui.page.main.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParamsType
import com.rosan.installer.ui.page.main.installer.dialog.dialogButtons
import com.rosan.installer.ui.page.main.widget.card.InfoTipCard
import com.rosan.installer.ui.page.main.widget.setting.NavigationItemWidget
import com.rosan.installer.ui.page.main.widget.setting.SegmentedColumn
import com.rosan.installer.ui.theme.bottomShape
import com.rosan.installer.ui.theme.middleShape
import com.rosan.installer.ui.theme.singleShape
import com.rosan.installer.ui.theme.topShape
import com.rosan.installer.ui.util.getSupportSubtitle
import com.rosan.installer.ui.util.getSupportTitle

@Composable
fun installChoiceDialog(
    viewModel: InstallerViewModel,
): DialogParams {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val config = uiState.config
    val analysisResults = uiState.analysisResults
    val sourceType = analysisResults.firstOrNull()?.appEntities?.firstOrNull()?.app?.sourceType ?: DataType.NONE
    val currentSessionMode = analysisResults.firstOrNull()?.sessionMode ?: SessionMode.Single
    val isMultiApk = currentSessionMode == SessionMode.Batch
    val isModuleApk = sourceType == DataType.MIXED_MODULE_APK
    val isMixedModuleZip = sourceType == DataType.MIXED_MODULE_ZIP
    var selectionMode by remember(sourceType) { mutableStateOf(MmzSelectionMode.INITIAL_CHOICE) }
    val apkChooseAll = config.apkChooseAll

    val allEntities = remember(analysisResults) { analysisResults.flatMap { it.appEntities } }
    val allSelectedEntities = allEntities.filter { it.selected }
    val selectedModuleCount = allSelectedEntities.count { it.app is AppEntity.ModuleEntity }
    val selectedAppCount = allSelectedEntities.count { it.app !is AppEntity.ModuleEntity }
    val totalModuleCount = allEntities.count { it.app is AppEntity.ModuleEntity }

    val isMixedError = selectedModuleCount > 0 && selectedAppCount > 0
    val isMultiModuleError = selectedModuleCount > 1

    val errorMessage = when {
        isMixedError -> stringResource(R.string.installer_error_mixed_selection)
        isMultiModuleError -> stringResource(R.string.installer_error_multiple_modules)
        else -> null
    }

    val isPrimaryActionEnabled = allSelectedEntities.isNotEmpty() && !isMixedError && !isMultiModuleError

    val primaryButtonText = if (isMultiApk) R.string.install else R.string.next
    val primaryButtonAction = if (isMultiApk) {
        { if (isPrimaryActionEnabled) viewModel.dispatch(InstallerViewAction.InstallMultiple) }
    } else {
        { if (isPrimaryActionEnabled) viewModel.dispatch(InstallerViewAction.InstallPrepare) }
    }

    // Determine if we are in the "Back" scenario for Mixed Module Zip
    val isMmzBack = isMixedModuleZip && selectionMode == MmzSelectionMode.APK_CHOICE
    val cancelOrBackText = if (isMmzBack) R.string.back else R.string.cancel

    val cancelOrBackAction: () -> Unit = {
        if (isMmzBack) {
            viewModel.dispatch(InstallerViewAction.SetApkSelection(false))
            selectionMode = MmzSelectionMode.INITIAL_CHOICE
        } else {
            viewModel.dispatch(InstallerViewAction.Close)
        }
    }

    return DialogParams(
        title = DialogInnerParams(DialogParamsType.InstallChoice.id) { Text(sourceType.getSupportTitle()) },
        subtitle = DialogInnerParams(DialogParamsType.InstallChoice.id) {
            sourceType.getSupportSubtitle(selectionMode)?.let { Text(it) }
        },
        content = DialogInnerParams(DialogParamsType.InstallChoice.id) {
            ChoiceContent(
                analysisResults = analysisResults,
                allSelectableEntities = allEntities,
                onSelectMixedModuleType = { installAsModule ->
                    viewModel.dispatch(InstallerViewAction.SelectMixedModuleType(installAsModule))
                },
                onToggleSelection = { packageName, entity, isMultiSelect ->
                    viewModel.dispatch(InstallerViewAction.ToggleSelection(packageName, entity, isMultiSelect))
                },
                onTogglePackageSelection = { packageName, entity ->
                    viewModel.dispatch(InstallerViewAction.TogglePackageSelection(packageName, entity))
                },
                onSelectAllApks = { viewModel.dispatch(InstallerViewAction.SetApkSelection(true)) },
                isModuleApk = isModuleApk,
                isMultiApk = isMultiApk,
                isMixedModuleZip = isMixedModuleZip,
                apkChooseAll = apkChooseAll,
                selectionMode = selectionMode,
                onSetSelectionMode = { selectionMode = it },
                errorMessage = errorMessage,
                totalModuleCount = totalModuleCount,
            )
        },
        buttons = dialogButtons(DialogParamsType.InstallChoice.id) {
            buildList {
                if ((!isModuleApk && !isMixedModuleZip) ||
                    (isMixedModuleZip && selectionMode == MmzSelectionMode.APK_CHOICE)
                ) {
                    add(DialogButton(stringResource(primaryButtonText), enabled = isPrimaryActionEnabled, onClick = primaryButtonAction))
                }
                add(DialogButton(stringResource(cancelOrBackText), onClick = cancelOrBackAction))
            }
        },
    )
}

@Composable
private fun ChoiceContent(
    analysisResults: List<PackageAnalysisResult>,
    allSelectableEntities: List<SelectInstallEntity>,
    onSelectMixedModuleType: (Boolean) -> Unit,
    onSelectAllApks: () -> Unit,
    onToggleSelection: (String, SelectInstallEntity, Boolean) -> Unit,
    onTogglePackageSelection: (String, SelectInstallEntity) -> Unit,
    isModuleApk: Boolean,
    isMultiApk: Boolean,
    isMixedModuleZip: Boolean,
    apkChooseAll: Boolean,
    selectionMode: MmzSelectionMode,
    onSetSelectionMode: (MmzSelectionMode) -> Unit,
    errorMessage: String?,
    totalModuleCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        AnimatedVisibility(visible = errorMessage != null) {
            InfoTipCard(
                text = errorMessage ?: "",
                icon = null,
                noPadding = false,
            )
        }
        if (isModuleApk) {
            val baseSelectableEntity = allSelectableEntities.firstOrNull { it.app is AppEntity.BaseEntity }
            val moduleSelectableEntity = allSelectableEntities.firstOrNull { it.app is AppEntity.ModuleEntity }

            SegmentedColumn {
                baseSelectableEntity?.let { entity ->
                    item {
                        val baseEntityInfo = entity.app as AppEntity.BaseEntity
                        NavigationItemWidget(
                            iconPlaceholder = false,
                            title = baseEntityInfo.label ?: "N/A",
                            description = stringResource(R.string.installer_package_name, baseEntityInfo.packageName),
                            onClick = {
                                onSelectMixedModuleType(false)
                            },
                        )
                    }
                }
                moduleSelectableEntity?.let { entity ->
                    item {
                        val moduleEntityInfo = entity.app as AppEntity.ModuleEntity
                        NavigationItemWidget(
                            iconPlaceholder = false,
                            title = moduleEntityInfo.name,
                            description = stringResource(R.string.installer_module_id, moduleEntityInfo.id),
                            onClick = {
                                onSelectMixedModuleType(true)
                            },
                        )
                    }
                }
            }
        } else if (isMixedModuleZip && selectionMode == MmzSelectionMode.INITIAL_CHOICE && totalModuleCount == 1) {
            val moduleSelectableEntity = allSelectableEntities.firstOrNull { it.app is AppEntity.ModuleEntity }
            val baseSelectableEntity = allSelectableEntities.firstOrNull { it.app is AppEntity.BaseEntity }

            SegmentedColumn {
                moduleSelectableEntity?.let { entity ->
                    item {
                        val moduleEntityInfo = entity.app as AppEntity.ModuleEntity
                        NavigationItemWidget(
                            iconPlaceholder = false,
                            title = stringResource(R.string.installer_choice_install_as_module),
                            description = stringResource(R.string.installer_module_id, moduleEntityInfo.id),
                            onClick = {
                                onSelectMixedModuleType(true)
                            },
                        )
                    }
                }
                if (baseSelectableEntity != null) {
                    item {
                        NavigationItemWidget(
                            iconPlaceholder = false,
                            title = stringResource(R.string.installer_choice_install_as_app),
                            description = stringResource(R.string.installer_choice_install_as_app_desc),
                            onClick = {
                                if (apkChooseAll) {
                                    onSelectAllApks()
                                }
                                onSetSelectionMode(MmzSelectionMode.APK_CHOICE)
                            },
                        )
                    }
                }
            }
        } else if (isMultiApk || (isMixedModuleZip && selectionMode == MmzSelectionMode.APK_CHOICE)) {
            // --- Multi-APK Mode ---
            val resultsForList = if (isMixedModuleZip && selectionMode == MmzSelectionMode.APK_CHOICE) {
                analysisResults.mapNotNull { pkgResult ->
                    val apkEntities = pkgResult.appEntities.filter {
                        it.app is AppEntity.BaseEntity || it.app is AppEntity.SplitEntity || it.app is AppEntity.DexMetadataEntity
                    }
                    if (apkEntities.isEmpty()) {
                        null
                    } else {
                        pkgResult.copy(appEntities = apkEntities)
                    }
                }
            } else {
                analysisResults
            }

            val listSize = resultsForList.size
            if (listSize == 0) return

            LazyColumn(
                modifier = Modifier.heightIn(max = 325.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                itemsIndexed(resultsForList, key = { _, it -> it.packageName }) { index, packageResult ->
                    val shape = when {
                        listSize == 1 -> singleShape
                        index == 0 -> topShape
                        index == listSize - 1 -> bottomShape
                        else -> middleShape
                    }
                    MultiApkGroupCard(
                        packageResult = packageResult,
                        onToggleSelection = onToggleSelection,
                        onTogglePackageSelection = onTogglePackageSelection,
                        modifier = Modifier.fillMaxWidth(),
                        shape = shape,
                    )
                }
            }
        } else {
            // --- Single-Package Split Mode (Enhanced with Grouping) ---
            val allEntities = analysisResults.firstOrNull()?.appEntities ?: emptyList()
            if (allEntities.isEmpty()) return

            val baseEntities =
                allEntities.filter { it.app is AppEntity.BaseEntity || it.app is AppEntity.DexMetadataEntity }
            val splitEntities = allEntities.filter { it.app is AppEntity.SplitEntity }

            val groupedSplits = splitEntities
                .groupBy { (it.app as AppEntity.SplitEntity).type }
                .toSortedMap(compareBy { it.ordinal })

            val displayGroups = buildList {
                if (baseEntities.isNotEmpty()) {
                    add(stringResource(R.string.split_name_base_group_title) to baseEntities)
                }
                groupedSplits.forEach { (type, entities) ->
                    add(type.getDisplayName() to entities)
                }
            }

            LazyColumn(
                modifier = Modifier.heightIn(max = 325.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                displayGroups.forEach { (groupTitle, itemsInGroup) ->
                    item {
                        Text(
                            text = groupTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }

                    itemsIndexed(itemsInGroup) { index, item ->
                        val shape = when {
                            itemsInGroup.size == 1 -> singleShape
                            index == 0 -> topShape
                            index == itemsInGroup.size - 1 -> bottomShape
                            else -> middleShape
                        }

                        SingleItemCard(
                            item = item,
                            shape = shape,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                onToggleSelection(item.app.packageName, item, true)
                            },
                        )
                    }
                    item { Spacer(modifier = Modifier.height(4.dp)) }
                }
            }
        }
    }
}

@Composable
private fun MultiApkGroupCard(
    packageResult: PackageAnalysisResult,
    onToggleSelection: (String, SelectInstallEntity, Boolean) -> Unit,
    onTogglePackageSelection: (String, SelectInstallEntity) -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    val itemsInGroup = packageResult.appEntities

    // A base APK and its splits are selected together; multiple base APKs are version alternatives.
    val baseEntities = remember(itemsInGroup) {
        itemsInGroup.filter { it.app is AppEntity.BaseEntity }
    }
    val isSingleItemInGroup = baseEntities.size <= 1

    var isExpanded by remember { mutableStateOf(itemsInGroup.any { it.selected }) }

    val baseInfo = remember(itemsInGroup) { itemsInGroup.firstNotNullOfOrNull { it.app as? AppEntity.BaseEntity } }
    val moduleInfo = remember(itemsInGroup) { itemsInGroup.firstNotNullOfOrNull { it.app as? AppEntity.ModuleEntity } }
    val appLabel = baseInfo?.label ?: moduleInfo?.name ?: packageResult.packageName

    if (isSingleItemInGroup) {
        // Fallback to the first item if no BaseEntity is found
        val item = baseEntities.firstOrNull() ?: itemsInGroup.first()
        SingleItemCard(
            item = item,
            shape = shape,
            modifier = modifier,
            onClick = {
                onTogglePackageSelection(packageResult.packageName, item)
            },
        )
    } else {
        val sortedItems = remember(itemsInGroup) {
            itemsInGroup.sortedByDescending { (it.app as? AppEntity.BaseEntity)?.versionCode ?: 0 }
        }
        val rotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "arrowRotation")
        Card(
            modifier = modifier,
            shape = shape,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(appLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        packageResult.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .basicMarquee(),
                        // Apply dynamic transparent color directly instead of alpha modifier
                        color = LocalContentColor.current.copy(alpha = 0.7f),
                    )
                }
                Icon(
                    imageVector = AppIcons.ArrowDropDown,
                    contentDescription = "Expand",
                    modifier = Modifier.rotate(rotation),
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    sortedItems.forEach { item ->
                        SelectableSubCard(
                            item = item,
                            onClick = {
                                onToggleSelection(packageResult.packageName, item, false)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SingleItemCard(
    item: SelectInstallEntity,
    shape: Shape,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    // Resolve dynamic container color and content color
    val containerColor = if (item.selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
    val contentColor = MaterialTheme.colorScheme.contentColorFor(containerColor)

    Card(
        modifier = modifier,
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
            onClick()
        },
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = item.selected,
                onCheckedChange = null,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
            Spacer(modifier = Modifier.width(16.dp))
            ItemContent(app = item.app)
        }
    }
}

@Composable
private fun SelectableSubCard(
    item: SelectInstallEntity,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    // Resolve dynamic container color and content color
    val containerColor = if (item.selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
    val contentColor = MaterialTheme.colorScheme.contentColorFor(containerColor)

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
            onClick()
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = item.selected, onClick = onClick)
            (item.app as? AppEntity.BaseEntity)?.let { baseEntity ->
                MultiApkItemContent(app = baseEntity)
            }
        }
    }
}

@Composable
private fun ItemContent(app: AppEntity) {
    // Utilize the contextual content color generated by the parent Card
    val contentColor = LocalContentColor.current
    val variantContentColor = contentColor.copy(alpha = 0.7f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 12.dp),
    ) {
        when (app) {
            is AppEntity.BaseEntity -> {
                Text(
                    app.label ?: app.packageName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                )
                Text(
                    app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.basicMarquee(),
                    color = variantContentColor,
                )
                Text(
                    text = stringResource(R.string.installer_version, app.versionName, app.versionCode),
                    style = MaterialTheme.typography.bodyMedium,
                    color = variantContentColor,
                )
            }

            is AppEntity.SplitEntity -> {
                Text(
                    text = getSplitDisplayName(
                        type = app.type,
                        configValue = app.configValue,
                        fallbackName = app.splitName,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                )
                Text(
                    text = stringResource(R.string.installer_file_name, app.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = variantContentColor,
                )
            }

            is AppEntity.DexMetadataEntity -> {
                Text(
                    app.dmName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                )
                Text(
                    app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.basicMarquee(),
                    color = variantContentColor,
                )
            }

            is AppEntity.ModuleEntity -> {
                Text(
                    app.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                )
                Text(
                    stringResource(R.string.installer_module_id, app.id),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.basicMarquee(),
                    color = variantContentColor,
                )
                Text(
                    text = stringResource(R.string.installer_version_code_label) + app.versionCode,
                    style = MaterialTheme.typography.bodyMedium,
                    color = variantContentColor,
                )
            }
        }
    }
}

/**
 * A composable for displaying an item in a multi-APK selection list.
 * Shows version information and the source file name.
 */
@Composable
private fun MultiApkItemContent(app: AppEntity.BaseEntity) {
    // Utilize the contextual content color generated by the parent Card
    val contentColor = LocalContentColor.current
    val variantContentColor = contentColor.copy(alpha = 0.7f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // Version information (styled as a title)
        Text(
            text = stringResource(R.string.installer_version, app.versionName, app.versionCode),
            style = MaterialTheme.typography.titleSmallEmphasized,
            fontWeight = FontWeight.Bold,
            color = contentColor,
        )
        // Filename (styled as a smaller body text with marquee)
        Text(
            text = app.data.getSourceTop().toString().removeSuffix("/").substringAfterLast('/'),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.basicMarquee(),
            color = variantContentColor,
        )
    }
}
