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
import com.rosan.installer.domain.engine.model.AppEntity
import com.rosan.installer.domain.engine.model.DataType
import com.rosan.installer.domain.engine.model.MmzSelectionMode
import com.rosan.installer.domain.engine.model.PackageAnalysisResult
import com.rosan.installer.domain.engine.model.SessionMode
import com.rosan.installer.domain.session.model.SelectInstallEntity
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.dialog.DialogButton
import com.rosan.installer.ui.page.main.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParamsType
import com.rosan.installer.ui.page.main.installer.dialog.dialogButtons
import com.rosan.installer.ui.page.main.settings.preferred.SettingsNavigationItemWidget
import com.rosan.installer.ui.page.main.widget.card.InfoTipCard
import com.rosan.installer.ui.page.main.widget.setting.SegmentedColumn
import com.rosan.installer.ui.theme.bottomShape
import com.rosan.installer.ui.theme.middleShape
import com.rosan.installer.ui.theme.singleShape
import com.rosan.installer.ui.theme.topShape
import com.rosan.installer.ui.util.getSupportSubtitle
import com.rosan.installer.ui.util.getSupportTitle

@Composable
fun installChoiceDialog(
    viewModel: InstallerViewModel
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

    val allSelectedEntities = analysisResults.flatMap { it.appEntities }.filter { it.selected }
    val selectedModuleCount = allSelectedEntities.count { it.app is AppEntity.ModuleEntity }
    val selectedAppCount = allSelectedEntities.count { it.app !is AppEntity.ModuleEntity }
    val totalModuleCount = analysisResults.flatMap { it.appEntities }.count { it.app is AppEntity.ModuleEntity }

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
            analysisResults.flatMap { it.appEntities }
                .filter { it.selected && it.app !is AppEntity.ModuleEntity }
                .forEach { entity ->
                    viewModel.dispatch(
                        InstallerViewAction.ToggleSelection(
                            packageName = entity.app.packageName,
                            entity = entity,
                            isMultiSelect = true
                        )
                    )
                }
            selectionMode = MmzSelectionMode.INITIAL_CHOICE
        } else {
            viewModel.dispatch(InstallerViewAction.Close)
        }
    }

    return DialogParams(
        icon = DialogInnerParams(DialogParamsType.IconWorking.id, {}),
        title = DialogInnerParams(DialogParamsType.InstallChoice.id) { Text(sourceType.getSupportTitle()) },
        subtitle = DialogInnerParams(DialogParamsType.InstallChoice.id) {
            sourceType.getSupportSubtitle(selectionMode)?.let { Text(it) }
        },
        content = DialogInnerParams(DialogParamsType.InstallChoice.id) {
            ChoiceContent(
                analysisResults = analysisResults,
                viewModel = viewModel,
                isModuleApk = isModuleApk,
                isMultiApk = isMultiApk,
                isMixedModuleZip = isMixedModuleZip,
                apkChooseAll = apkChooseAll,
                selectionMode = selectionMode,
                onSetSelectionMode = { selectionMode = it },
                errorMessage = errorMessage,
                totalModuleCount = totalModuleCount
            )
        },
        buttons = dialogButtons(DialogParamsType.InstallChoice.id) {
            buildList {
                if ((!isModuleApk && !isMixedModuleZip) ||
                    (isMixedModuleZip && selectionMode == MmzSelectionMode.APK_CHOICE)
                )
                    add(DialogButton(stringResource(primaryButtonText), onClick = primaryButtonAction))
                add(DialogButton(stringResource(cancelOrBackText), onClick = cancelOrBackAction))
            }
        }
    )
}

@Composable
private fun ChoiceContent(
    analysisResults: List<PackageAnalysisResult>,
    viewModel: InstallerViewModel,
    isModuleApk: Boolean = false,
    isMultiApk: Boolean,
    isMixedModuleZip: Boolean,
    apkChooseAll: Boolean,
    selectionMode: MmzSelectionMode,
    onSetSelectionMode: (MmzSelectionMode) -> Unit,
    errorMessage: String?,
    totalModuleCount: Int
) {
    Column {
        AnimatedVisibility(visible = errorMessage != null) {
            InfoTipCard(
                text = errorMessage ?: "",
                icon = null,
                noPadding = false
            )
        }
        if (isModuleApk) {
            val allSelectableEntities = analysisResults.flatMap { it.appEntities }
            val baseSelectableEntity = allSelectableEntities.firstOrNull { it.app is AppEntity.BaseEntity }
            val moduleSelectableEntity = allSelectableEntities.firstOrNull { it.app is AppEntity.ModuleEntity }

            SegmentedColumn {
                baseSelectableEntity?.let { entity ->
                    item {
                        val baseEntityInfo = entity.app as AppEntity.BaseEntity
                        SettingsNavigationItemWidget(
                            iconPlaceholder = false,
                            title = baseEntityInfo.label ?: "N/A",
                            description = stringResource(R.string.installer_package_name, baseEntityInfo.packageName),
                            onClick = {
                                if (moduleSelectableEntity?.selected == true) {
                                    viewModel.dispatch(
                                        InstallerViewAction.ToggleSelection(
                                            packageName = moduleSelectableEntity.app.packageName,
                                            entity = moduleSelectableEntity,
                                            isMultiSelect = true
                                        )
                                    )
                                }

                                viewModel.dispatch(
                                    InstallerViewAction.ToggleSelection(
                                        packageName = entity.app.packageName,
                                        entity = entity,
                                        isMultiSelect = false
                                    )
                                )
                                viewModel.dispatch(InstallerViewAction.InstallPrepare)
                            }
                        )
                    }
                }
                moduleSelectableEntity?.let { entity ->
                    item {
                        val moduleEntityInfo = entity.app as AppEntity.ModuleEntity
                        SettingsNavigationItemWidget(
                            iconPlaceholder = false,
                            title = moduleEntityInfo.name,
                            description = stringResource(R.string.installer_module_id, moduleEntityInfo.id),
                            onClick = {
                                if (baseSelectableEntity?.selected == true) {
                                    viewModel.dispatch(
                                        InstallerViewAction.ToggleSelection(
                                            packageName = baseSelectableEntity.app.packageName,
                                            entity = baseSelectableEntity,
                                            isMultiSelect = true
                                        )
                                    )
                                }

                                viewModel.dispatch(
                                    InstallerViewAction.ToggleSelection(
                                        packageName = entity.app.packageName,
                                        entity = entity,
                                        isMultiSelect = false
                                    )
                                )
                                viewModel.dispatch(InstallerViewAction.InstallPrepare)
                            }
                        )
                    }
                }
            }
        } else if (isMixedModuleZip && selectionMode == MmzSelectionMode.INITIAL_CHOICE && totalModuleCount == 1) {
            val allSelectableEntities = analysisResults.flatMap { it.appEntities }
            val moduleSelectableEntity = allSelectableEntities.firstOrNull { it.app is AppEntity.ModuleEntity }
            val baseSelectableEntity = allSelectableEntities.firstOrNull { it.app is AppEntity.BaseEntity }

            SegmentedColumn {
                moduleSelectableEntity?.let { entity ->
                    item {
                        val moduleEntityInfo = entity.app as AppEntity.ModuleEntity
                        SettingsNavigationItemWidget(
                            iconPlaceholder = false,
                            title = stringResource(R.string.installer_choice_install_as_module),
                            description = stringResource(R.string.installer_module_id, moduleEntityInfo.id),
                            onClick = {
                                analysisResults.flatMap { it.appEntities }
                                    .filter { it.app !is AppEntity.ModuleEntity && it.selected }
                                    .forEach { apkEntity ->
                                        viewModel.dispatch(
                                            InstallerViewAction.ToggleSelection(
                                                packageName = apkEntity.app.packageName,
                                                entity = apkEntity,
                                                isMultiSelect = true
                                            )
                                        )
                                    }
                                viewModel.dispatch(
                                    InstallerViewAction.ToggleSelection(
                                        packageName = entity.app.packageName,
                                        entity = entity,
                                        isMultiSelect = false
                                    )
                                )
                                viewModel.dispatch(InstallerViewAction.InstallPrepare)
                            }
                        )
                    }
                }
                if (baseSelectableEntity != null) {
                    item {
                        SettingsNavigationItemWidget(
                            iconPlaceholder = false,
                            title = stringResource(R.string.installer_choice_install_as_app),
                            description = stringResource(R.string.installer_choice_install_as_app_desc),
                            onClick = {
                                if (apkChooseAll) {
                                    analysisResults.flatMap { it.appEntities }
                                        .filter { it.app !is AppEntity.ModuleEntity && !it.selected }
                                        .forEach { entity ->
                                            viewModel.dispatch(
                                                InstallerViewAction.ToggleSelection(
                                                    packageName = entity.app.packageName,
                                                    entity = entity,
                                                    isMultiSelect = true
                                                )
                                            )
                                        }
                                }
                                onSetSelectionMode(MmzSelectionMode.APK_CHOICE)
                            }
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
                    if (apkEntities.isEmpty()) null
                    else pkgResult.copy(appEntities = apkEntities)
                }
            } else {
                analysisResults
            }

            val listSize = resultsForList.size
            if (listSize == 0) return

            LazyColumn(
                modifier = Modifier.heightIn(max = 325.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
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
                        viewModel = viewModel,
                        shape = shape
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
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                displayGroups.forEach { (groupTitle, itemsInGroup) ->
                    item {
                        Text(
                            text = groupTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 0.dp, bottom = 0.dp, start = 16.dp)
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
                            onClick = {
                                viewModel.dispatch(
                                    InstallerViewAction.ToggleSelection(
                                        packageName = item.app.packageName,
                                        entity = item,
                                        isMultiSelect = true
                                    )
                                )
                            }
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
    viewModel: InstallerViewModel,
    shape: Shape
) {
    val itemsInGroup = packageResult.appEntities

    // Filter out the base entities to determine if it should be displayed as a single app item
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
            onClick = {
                // Sync the selection state for all items (Base and Splits) in the group
                val targetState = !item.selected
                itemsInGroup.forEach { entity ->
                    if (entity.selected != targetState) {
                        viewModel.dispatch(
                            InstallerViewAction.ToggleSelection(
                                packageName = packageResult.packageName,
                                entity = entity,
                                isMultiSelect = true
                            )
                        )
                    }
                }
            }
        )
    } else {
        val rotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "arrowRotation")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
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
                        color = LocalContentColor.current.copy(alpha = 0.7f)
                    )
                }
                Icon(
                    imageVector = AppIcons.ArrowDropDown,
                    contentDescription = "Expand",
                    modifier = Modifier.rotate(rotation)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsInGroup
                        .sortedByDescending { (it.app as? AppEntity.BaseEntity)?.versionCode ?: 0 }
                        .forEach { item ->
                            SelectableSubCard(
                                item = item,
                                onClick = {
                                    viewModel.dispatch(
                                        InstallerViewAction.ToggleSelection(
                                            packageName = packageResult.packageName,
                                            entity = item,
                                            isMultiSelect = false
                                        )
                                    )
                                }
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
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    // Resolve dynamic container color and content color
    val containerColor = if (item.selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
    val contentColor = MaterialTheme.colorScheme.contentColorFor(containerColor)

    Card(
        // Padding is now handled by the parent LazyColumn's contentPadding.
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
            onClick()
        },
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.selected,
                onCheckedChange = null,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            Spacer(modifier = Modifier.width(16.dp))
            ItemContent(app = item.app)
        }
    }
}

@Composable
private fun SelectableSubCard(
    item: SelectInstallEntity,
    isRadio: Boolean = true,
    onClick: () -> Unit
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
            contentColor = contentColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isRadio) {
                RadioButton(selected = item.selected, onClick = onClick)
            } else {
                Checkbox(checked = item.selected, onCheckedChange = { onClick() })
            }
            if (isRadio)
                (item.app as? AppEntity.BaseEntity)?.let { baseEntity ->
                    MultiApkItemContent(app = baseEntity)
                }
            else
                ItemContent(app = item.app)
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
            .padding(end = 12.dp)
    ) {
        when (app) {
            is AppEntity.BaseEntity -> {
                Text(
                    app.label ?: app.packageName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.basicMarquee(),
                    color = variantContentColor
                )
                Text(
                    text = stringResource(R.string.installer_version, app.versionName, app.versionCode),
                    style = MaterialTheme.typography.bodyMedium,
                    color = variantContentColor
                )
            }

            is AppEntity.SplitEntity -> {
                Text(
                    text = getSplitDisplayName(
                        type = app.type,
                        configValue = app.configValue,
                        fallbackName = app.splitName
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = stringResource(R.string.installer_file_name, app.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = variantContentColor
                )
            }

            is AppEntity.DexMetadataEntity -> {
                Text(
                    app.dmName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.basicMarquee(),
                    color = variantContentColor
                )
            }

            is AppEntity.ModuleEntity -> {
                Text(
                    app.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    stringResource(R.string.installer_module_id, app.id),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.basicMarquee(),
                    color = variantContentColor
                )
                Text(
                    text = stringResource(R.string.installer_version_code_label) + app.versionCode,
                    style = MaterialTheme.typography.bodyMedium,
                    color = variantContentColor
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
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Version information (styled as a title)
        Text(
            text = stringResource(R.string.installer_version, app.versionName, app.versionCode),
            style = MaterialTheme.typography.titleSmallEmphasized,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
        // Filename (styled as a smaller body text with marquee)
        Text(
            text = app.data.getSourceTop().toString().removeSuffix("/").substringAfterLast('/'),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.basicMarquee(),
            color = variantContentColor
        )
    }
}
