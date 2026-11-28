// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer.sheetcontent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.layout.Divider
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.rosan.installer.R
import com.rosan.installer.data.engine.parser.getDisplayName
import com.rosan.installer.data.engine.parser.getSplitDisplayName
import com.rosan.installer.domain.engine.model.install.MmzSelectionMode
import com.rosan.installer.domain.engine.model.install.SessionMode
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.packageinfo.PackageAnalysisResult
import com.rosan.installer.domain.engine.model.source.DataType
import com.rosan.installer.ui.page.coui.widgets.CouiActionButton
import com.rosan.installer.ui.page.coui.widgets.CouiBasicItem
import com.rosan.installer.ui.page.coui.widgets.CouiCard
import com.rosan.installer.ui.page.coui.widgets.CouiCheckboxItem
import com.rosan.installer.ui.page.coui.widgets.CouiInstallerTipCard
import com.rosan.installer.ui.page.coui.widgets.CouiMultiApkCheckboxItem
import com.rosan.installer.ui.page.coui.widgets.CouiNavigationItem
import com.rosan.installer.ui.page.coui.widgets.CouiWarningCard
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.theme.InstallerTheme
import com.rosan.installer.ui.util.getSupportSubtitle

@Composable
fun CouiInstallChoiceContent(
    viewModel: InstallerViewModel,
    selectionMode: MmzSelectionMode,
    onSelectionModeChange: (MmzSelectionMode) -> Unit,
    onSelectMixedModuleType: (Boolean) -> Unit,
    onBackToTypeChoice: () -> Unit,
    onCancel: () -> Unit,
) {
    val isDarkMode = InstallerTheme.isDark
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val analysisResults = uiState.analysisResults
    val sourceType = analysisResults.firstOrNull()?.appEntities?.firstOrNull()?.app?.sourceType ?: DataType.NONE
    val currentSessionMode = analysisResults.firstOrNull()?.sessionMode ?: SessionMode.Single
    val isMultiApk = currentSessionMode == SessionMode.Batch
    val isModuleApk = sourceType == DataType.MIXED_MODULE_APK
    val isMixedModuleZip = sourceType == DataType.MIXED_MODULE_ZIP
    val totalModuleCount = analysisResults.flatMap { it.appEntities }.count { it.app is AppEntity.ModuleEntity }
    val isInstallTypeChoice = isModuleApk ||
        (isMixedModuleZip && selectionMode == MmzSelectionMode.INITIAL_CHOICE && totalModuleCount == 1)

    val primaryButtonTextRes = if (isMultiApk) R.string.install else R.string.next
    val primaryButtonAction = if (isMultiApk) {
        { viewModel.dispatch(InstallerViewAction.InstallMultiple) }
    } else {
        { viewModel.dispatch(InstallerViewAction.InstallPrepare) }
    }

    val allSelectedEntities = analysisResults.flatMap { it.appEntities }.filter { it.selected }
    val selectedModuleCount = allSelectedEntities.count { it.app is AppEntity.ModuleEntity }
    val selectedAppCount = allSelectedEntities.count { it.app !is AppEntity.ModuleEntity }
    val isMixedError = selectedModuleCount > 0 && selectedAppCount > 0
    val isMultiModuleError = selectedModuleCount > 1
    val errorMessage = when {
        isMixedError -> stringResource(R.string.installer_error_mixed_selection)
        isMultiModuleError -> stringResource(R.string.installer_error_multiple_modules)
        else -> null
    }
    val isPrimaryActionEnabled = allSelectedEntities.isNotEmpty() && !isMixedError && !isMultiModuleError

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        sourceType.getSupportSubtitle(selectionMode = selectionMode)?.let { CouiInstallerTipCard(it) }

        AnimatedVisibility(
            visible = errorMessage != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            CouiWarningCard(isDark = isDarkMode, message = errorMessage.orEmpty())
        }

        if (isMixedModuleZip && selectionMode == MmzSelectionMode.INITIAL_CHOICE && totalModuleCount == 1) {
            Box(modifier = Modifier.weight(1f, fill = false)) {
                CouiMixedModuleZipInitialChoice(
                    analysisResults = analysisResults,
                    viewModel = viewModel,
                    apkChooseAll = uiState.config.apkChooseAll,
                    onSelectMixedModuleType = onSelectMixedModuleType,
                    onSelectApk = { onSelectionModeChange(MmzSelectionMode.APK_CHOICE) },
                )
            }
        } else {
            val resultsForList = if (isMixedModuleZip && selectionMode == MmzSelectionMode.APK_CHOICE) {
                analysisResults.mapNotNull { pkgResult ->
                    val apkEntities = pkgResult.appEntities.filter {
                        it.app is AppEntity.BaseEntity || it.app is AppEntity.SplitEntity || it.app is AppEntity.DexMetadataEntity
                    }
                    if (apkEntities.isEmpty()) null else pkgResult.copy(appEntities = apkEntities)
                }
            } else {
                analysisResults
            }

            Box(modifier = Modifier.weight(1f, fill = false)) {
                CouiChoiceLazyList(
                    analysisResults = resultsForList,
                    viewModel = viewModel,
                    isModuleApk = isModuleApk,
                    isMultiApk = isMultiApk || (isMixedModuleZip && selectionMode == MmzSelectionMode.APK_CHOICE),
                    onSelectMixedModuleType = onSelectMixedModuleType,
                )
            }
        }

        if (!isInstallTypeChoice) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isMultiApk || isMixedModuleZip) {
                    val isBack = isMixedModuleZip && selectionMode == MmzSelectionMode.APK_CHOICE
                    CouiActionButton(
                        text = stringResource(if (isBack) R.string.back else R.string.cancel),
                        onClick = { if (isBack) onBackToTypeChoice() else onCancel() },
                        modifier = Modifier.weight(1f),
                    )
                }

                val showPrimaryButton = isMultiApk ||
                    (!isModuleApk && !isMixedModuleZip) ||
                    (isMixedModuleZip && selectionMode == MmzSelectionMode.APK_CHOICE)

                if (showPrimaryButton) {
                    val (currentPrimaryTextRes, currentPrimaryAction) =
                        if (isMixedModuleZip && selectionMode == MmzSelectionMode.APK_CHOICE) {
                            R.string.install to { viewModel.dispatch(InstallerViewAction.InstallMultiple) }
                        } else {
                            primaryButtonTextRes to primaryButtonAction
                        }

                    CouiActionButton(
                        text = stringResource(currentPrimaryTextRes),
                        onClick = currentPrimaryAction,
                        enabled = isPrimaryActionEnabled,
                        primary = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CouiChoiceLazyList(
    analysisResults: List<PackageAnalysisResult>,
    viewModel: InstallerViewModel,
    isModuleApk: Boolean,
    isMultiApk: Boolean,
    onSelectMixedModuleType: (Boolean) -> Unit,
) {
    val pageColors = CouiPageDefaults.themeColors()

    if (isModuleApk) {
        val allSelectableEntities = analysisResults.flatMap { it.appEntities }
        val baseSelectableEntity = allSelectableEntities.firstOrNull { it.app is AppEntity.BaseEntity }
        val moduleSelectableEntity = allSelectableEntities.firstOrNull { it.app is AppEntity.ModuleEntity }

        LazyColumn(
            modifier = Modifier.wrapContentSize(),
            overscrollEffect = rememberCouiOverscrollEffect(),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            item {
                CouiCard {
                    if (baseSelectableEntity != null) {
                        val baseEntityInfo = baseSelectableEntity.app as AppEntity.BaseEntity
                        CouiNavigationItem(
                            title = baseEntityInfo.label ?: "N/A",
                            description = stringResource(R.string.installer_package_name, baseEntityInfo.packageName),
                            onClick = { onSelectMixedModuleType(false) },
                        )
                    }
                    if (baseSelectableEntity != null && moduleSelectableEntity != null) {
                        Divider()
                    }
                    if (moduleSelectableEntity != null) {
                        val moduleEntityInfo = moduleSelectableEntity.app as AppEntity.ModuleEntity
                        CouiNavigationItem(
                            title = moduleEntityInfo.name,
                            description = stringResource(R.string.installer_module_id, moduleEntityInfo.id),
                            onClick = { onSelectMixedModuleType(true) },
                        )
                    }
                }
            }
        }
    } else if (isMultiApk) {
        LazyColumn(
            modifier = Modifier.wrapContentSize(),
            overscrollEffect = rememberCouiOverscrollEffect(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            itemsIndexed(analysisResults, key = { _, it -> it.packageName }) { _, packageResult ->
                val itemsInGroup = packageResult.appEntities
                val baseEntities = remember(itemsInGroup) { itemsInGroup.filter { it.app is AppEntity.BaseEntity } }
                val isTreatAsSingle = baseEntities.size <= 1
                val baseInfo = remember(itemsInGroup) { itemsInGroup.firstNotNullOfOrNull { it.app as? AppEntity.BaseEntity } }
                val moduleInfo = remember(itemsInGroup) { itemsInGroup.firstNotNullOfOrNull { it.app as? AppEntity.ModuleEntity } }
                val appLabel = baseInfo?.label ?: moduleInfo?.name ?: packageResult.packageName

                CouiCard {
                    if (isTreatAsSingle) {
                        val displayItem = baseEntities.firstOrNull() ?: itemsInGroup.first()
                        val app = displayItem.app
                        val versionText = (app as? AppEntity.BaseEntity)?.let {
                            stringResource(R.string.installer_version, it.versionName, it.versionCode)
                        } ?: (app as? AppEntity.ModuleEntity)?.let {
                            stringResource(R.string.installer_version_code_label) + it.versionCode
                        } ?: ""

                        CouiCheckboxItem(
                            title = appLabel,
                            description = "${packageResult.packageName}\n$versionText",
                            checked = displayItem.selected,
                            onCheckedChange = {
                                viewModel.dispatch(
                                    InstallerViewAction.TogglePackageSelection(
                                        packageName = packageResult.packageName,
                                        entity = displayItem,
                                    ),
                                )
                            },
                        )
                    } else {
                        CouiBasicItem(title = appLabel, summary = packageResult.packageName)
                        Divider()
                        itemsInGroup
                            .sortedByDescending { (it.app as? AppEntity.BaseEntity)?.versionCode ?: 0 }
                            .forEachIndexed { idx, item ->
                                if (idx > 0) Divider()
                                val appBaseEntity = item.app as? AppEntity.BaseEntity
                                val titleText = if (appBaseEntity != null) {
                                    stringResource(R.string.installer_version, appBaseEntity.versionName, appBaseEntity.versionCode)
                                } else {
                                    item.app.name
                                }
                                val summaryText = appBaseEntity?.data?.getSourceTop()?.toString()?.removeSuffix("/")
                                    ?.substringAfterLast('/')

                                CouiMultiApkCheckboxItem(
                                    title = titleText,
                                    summary = summaryText,
                                    checked = item.selected,
                                    onCheckedChange = {
                                        viewModel.dispatch(
                                            InstallerViewAction.ToggleSelection(
                                                packageName = packageResult.packageName,
                                                entity = item,
                                                isMultiSelect = false,
                                            ),
                                        )
                                    },
                                )
                            }
                    }
                }
            }
        }
    } else {
        val allEntities = analysisResults.firstOrNull()?.appEntities ?: emptyList()
        val baseEntities = allEntities.filter { it.app is AppEntity.BaseEntity || it.app is AppEntity.DexMetadataEntity }
        val splitEntities = allEntities.filter { it.app is AppEntity.SplitEntity }
        val groupedSplits = splitEntities.groupBy { (it.app as AppEntity.SplitEntity).type }.toSortedMap(compareBy { it.ordinal })

        LazyColumn(
            modifier = Modifier.wrapContentSize(),
            overscrollEffect = rememberCouiOverscrollEffect(),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            if (baseEntities.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.split_name_base_group_title),
                        kind = CouiTextKind.GroupTitle,
                        color = pageColors.summary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                item {
                    CouiCard {
                        baseEntities.forEachIndexed { idx, item ->
                            if (idx > 0) Divider()
                            val (title, description) = when (val app = item.app) {
                                is AppEntity.BaseEntity -> (app.label ?: app.packageName) to
                                    "${app.packageName}\n${stringResource(R.string.installer_version, app.versionName, app.versionCode)}"
                                is AppEntity.DexMetadataEntity -> app.dmName to app.packageName
                                else -> item.app.name to item.app.packageName
                            }
                            CouiCheckboxItem(
                                title = title,
                                description = description,
                                checked = item.selected,
                                onCheckedChange = {
                                    viewModel.dispatch(
                                        InstallerViewAction.ToggleSelection(
                                            packageName = item.app.packageName,
                                            entity = item,
                                            isMultiSelect = true,
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }
            }

            groupedSplits.forEach { (splitType, entitiesInGroup) ->
                if (entitiesInGroup.isEmpty()) return@forEach
                item {
                    Text(
                        text = splitType.getDisplayName(),
                        kind = CouiTextKind.GroupTitle,
                        color = pageColors.summary,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                    )
                }
                item {
                    CouiCard {
                        entitiesInGroup.forEachIndexed { idx, item ->
                            if (idx > 0) Divider()
                            val app = item.app as AppEntity.SplitEntity
                            val title = getSplitDisplayName(
                                type = app.type,
                                configValue = app.configValue,
                                fallbackName = app.splitName,
                            )
                            val description = stringResource(R.string.installer_file_name, app.name)
                            CouiCheckboxItem(
                                title = title,
                                description = description,
                                checked = item.selected,
                                onCheckedChange = {
                                    viewModel.dispatch(
                                        InstallerViewAction.ToggleSelection(
                                            packageName = item.app.packageName,
                                            entity = item,
                                            isMultiSelect = true,
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CouiMixedModuleZipInitialChoice(
    analysisResults: List<PackageAnalysisResult>,
    viewModel: InstallerViewModel,
    apkChooseAll: Boolean,
    onSelectMixedModuleType: (Boolean) -> Unit,
    onSelectApk: () -> Unit,
) {
    val allSelectableEntities = analysisResults.flatMap { it.appEntities }
    val moduleSelectableEntity = allSelectableEntities.firstOrNull { it.app is AppEntity.ModuleEntity }
    val baseSelectableEntity = allSelectableEntities.firstOrNull { it.app is AppEntity.BaseEntity }

    LazyColumn(
        modifier = Modifier.wrapContentSize(),
        overscrollEffect = rememberCouiOverscrollEffect(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        item {
            CouiCard {
                if (moduleSelectableEntity != null) {
                    val moduleEntityInfo = moduleSelectableEntity.app as AppEntity.ModuleEntity
                    CouiNavigationItem(
                        title = stringResource(R.string.installer_choice_install_as_module),
                        description = stringResource(R.string.installer_module_id, moduleEntityInfo.id),
                        onClick = { onSelectMixedModuleType(true) },
                    )
                }
                if (moduleSelectableEntity != null && baseSelectableEntity != null) {
                    Divider()
                }
                if (baseSelectableEntity != null) {
                    CouiNavigationItem(
                        title = stringResource(R.string.installer_choice_install_as_app),
                        description = stringResource(R.string.installer_choice_install_as_app_desc),
                        onClick = {
                            if (apkChooseAll) {
                                viewModel.dispatch(InstallerViewAction.SetApkSelection(true))
                            }
                            onSelectApk()
                        },
                    )
                }
            }
        }
    }
}
