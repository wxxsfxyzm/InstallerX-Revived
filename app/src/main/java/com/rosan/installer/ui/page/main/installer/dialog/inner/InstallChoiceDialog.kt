package com.rosan.installer.ui.page.main.installer.dialog.inner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataType
import com.rosan.installer.data.app.model.entity.MmzSelectionMode
import com.rosan.installer.data.app.model.entity.PackageAnalysisResult
import com.rosan.installer.data.app.model.entity.SessionMode
import com.rosan.installer.data.app.util.getSplitDisplayName
import com.rosan.installer.data.installer.model.entity.SelectInstallEntity
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParamsType
import com.rosan.installer.ui.page.main.widget.setting.SettingsNavigationItemWidget
import com.rosan.installer.ui.page.main.widget.setting.SplicedColumnGroup
import com.rosan.installer.ui.util.getSupportSubtitle
import com.rosan.installer.ui.util.getSupportTitle
import timber.log.Timber

@Composable
fun installChoiceDialog(
    installer: InstallerRepo, viewModel: InstallerViewModel
): DialogParams {
    val analysisResults = installer.analysisResults
    val sourceType = analysisResults.firstOrNull()?.appEntities?.firstOrNull()?.app?.sourceType ?: DataType.NONE
    val currentSessionMode = analysisResults.firstOrNull()?.sessionMode ?: SessionMode.Single
    val isMultiApk = currentSessionMode == SessionMode.Batch
    val isModuleApk = sourceType == DataType.MIXED_MODULE_APK
    val isMixedModuleZip = sourceType == DataType.MIXED_MODULE_ZIP
    var selectionMode by remember(sourceType) { mutableStateOf(MmzSelectionMode.INITIAL_CHOICE) }

    val titleRes = sourceType.getSupportTitle()
    val primaryButtonText = if (isMultiApk) R.string.install else R.string.next
    val primaryButtonAction = if (isMultiApk) {
        { viewModel.dispatch(InstallerViewAction.InstallMultiple) }
    } else {
        { viewModel.dispatch(InstallerViewAction.InstallPrepare) }
    }

    return DialogParams(
        icon = DialogInnerParams(DialogParamsType.IconWorking.id, workingIcon),
        title = DialogInnerParams(DialogParamsType.InstallChoice.id) { Text(stringResource(titleRes)) },
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
                selectionMode = selectionMode,
                onSetSelectionMode = { selectionMode = it }
            )
        },
        buttons = DialogButtons(DialogParamsType.InstallChoice.id) {
            buildList {
                if ((!isModuleApk && !isMixedModuleZip) ||
                    (isMixedModuleZip && selectionMode == MmzSelectionMode.APK_CHOICE)
                )
                    add(DialogButton(stringResource(primaryButtonText), onClick = primaryButtonAction))
                add(DialogButton(stringResource(R.string.cancel)) { viewModel.dispatch(InstallerViewAction.Close) })
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
    selectionMode: MmzSelectionMode,
    onSetSelectionMode: (MmzSelectionMode) -> Unit
) {
    // Define shapes for different positions
    val cornerRadius = 16.dp
    val connectionRadius = 5.dp
    val topShape = RoundedCornerShape(
        topStart = cornerRadius,
        topEnd = cornerRadius,
        bottomStart = connectionRadius,
        bottomEnd = connectionRadius
    )
    val middleShape = RoundedCornerShape(connectionRadius)
    val bottomShape = RoundedCornerShape(
        topStart = connectionRadius,
        topEnd = connectionRadius,
        bottomStart = cornerRadius,
        bottomEnd = cornerRadius
    )
    val singleShape = RoundedCornerShape(cornerRadius)

    val allAppEntities = analysisResults.flatMap { it.appEntities }.map { it.app }
    val baseEntityInfo = allAppEntities.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
    val moduleEntityInfo = allAppEntities.filterIsInstance<AppEntity.ModuleEntity>().firstOrNull()

    Timber.tag("InstallChoice").d("baseEntityInfo: $baseEntityInfo")
    Timber.tag("InstallChoice").d("moduleEntityInfo: $moduleEntityInfo")

    if (isModuleApk) {
        val allSelectableEntities = analysisResults.flatMap { it.appEntities }
        val baseSelectableEntity = allSelectableEntities.firstOrNull { it.app is AppEntity.BaseEntity }
        val moduleSelectableEntity = allSelectableEntities.firstOrNull { it.app is AppEntity.ModuleEntity }

        SplicedColumnGroup(
            content = buildList {
                if (baseSelectableEntity != null) {
                    val baseEntityInfo = baseSelectableEntity.app as AppEntity.BaseEntity
                    add {
                        SettingsNavigationItemWidget(
                            iconPlaceholder = false,
                            title = baseEntityInfo.label ?: "N/A",
                            description = stringResource(R.string.installer_package_name, baseEntityInfo.packageName),
                            onClick = {
                                // The initial state of baseSelectableEntity.selected is false.
                                // Toggling it will set its 'selected' state to true.
                                // The isMultiSelect=false flag ensures the other entity remains false.
                                viewModel.dispatch(
                                    InstallerViewAction.ToggleSelection(
                                        packageName = baseSelectableEntity.app.packageName,
                                        entity = baseSelectableEntity,
                                        isMultiSelect = false
                                    )
                                )
                                // After updating the selection state, immediately proceed to the prepare screen.
                                viewModel.dispatch(InstallerViewAction.InstallPrepare)
                            }
                        )
                    }
                }
                if (moduleSelectableEntity != null) {
                    val moduleEntityInfo = moduleSelectableEntity.app as AppEntity.ModuleEntity
                    add {
                        SettingsNavigationItemWidget(
                            iconPlaceholder = false,
                            title = moduleEntityInfo.name,
                            description = stringResource(R.string.installer_module_id, moduleEntityInfo.id),
                            onClick = {
                                // The initial state of moduleSelectableEntity.selected is false.
                                // Toggling it will set its 'selected' state to true.
                                viewModel.dispatch(
                                    InstallerViewAction.ToggleSelection(
                                        packageName = moduleSelectableEntity.app.packageName,
                                        entity = moduleSelectableEntity,
                                        isMultiSelect = false
                                    )
                                )
                                // After updating the selection state, immediately proceed to the prepare screen.
                                viewModel.dispatch(InstallerViewAction.InstallPrepare)
                            }
                        )
                    }
                }
            }
        )
    } else if (isMixedModuleZip && selectionMode == MmzSelectionMode.INITIAL_CHOICE) {
        val allSelectableEntities = analysisResults.flatMap { it.appEntities }
        val moduleSelectableEntity = allSelectableEntities.firstOrNull { it.app is AppEntity.ModuleEntity }
        val baseSelectableEntity = allSelectableEntities.firstOrNull { it.app is AppEntity.BaseEntity }

        SplicedColumnGroup(
            content = buildList {
                if (moduleSelectableEntity != null) {
                    val moduleEntityInfo = moduleSelectableEntity.app as AppEntity.ModuleEntity
                    add {
                        SettingsNavigationItemWidget(
                            iconPlaceholder = false,
                            title = stringResource(R.string.installer_choice_install_as_module),
                            description = stringResource(R.string.installer_module_id, moduleEntityInfo.id),
                            onClick = {
                                viewModel.dispatch(
                                    InstallerViewAction.ToggleSelection(
                                        packageName = moduleSelectableEntity.app.packageName,
                                        entity = moduleSelectableEntity,
                                        isMultiSelect = false
                                    )
                                )
                                viewModel.dispatch(InstallerViewAction.InstallPrepare)
                            }
                        )
                    }
                }
                if (baseSelectableEntity != null) {
                    add {
                        SettingsNavigationItemWidget(
                            iconPlaceholder = false,
                            title = stringResource(R.string.installer_choice_install_as_app),
                            description = stringResource(R.string.installer_choice_install_as_app_desc),
                            onClick = {
                                onSetSelectionMode(MmzSelectionMode.APK_CHOICE)
                            }
                        )
                    }
                }
            }
        )
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
        // --- Single-Package Split Mode ---
        val entities = analysisResults.firstOrNull()?.appEntities ?: emptyList()
        val listSize = entities.size
        if (listSize == 0) return

        LazyColumn(
            modifier = Modifier.heightIn(max = 325.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            itemsIndexed(entities, key = { _, it -> it.app.name + it.app.packageName }) { index, item ->
                val shape = when {
                    listSize == 1 -> singleShape
                    index == 0 -> topShape
                    index == listSize - 1 -> bottomShape
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun MultiApkGroupCard(
    packageResult: PackageAnalysisResult,
    viewModel: InstallerViewModel,
    shape: Shape
) {
    val itemsInGroup = packageResult.appEntities
    val isSingleItemInGroup = itemsInGroup.size == 1

    var isExpanded by remember { mutableStateOf(itemsInGroup.any { it.selected }) }

    val baseInfo = remember(itemsInGroup) { itemsInGroup.firstNotNullOfOrNull { it.app as? AppEntity.BaseEntity } }
    val appLabel = baseInfo?.label ?: packageResult.packageName

    if (isSingleItemInGroup) {
        val item = itemsInGroup.first()
        SingleItemCard(
            item = item,
            shape = shape,
            onClick = {
                viewModel.dispatch(
                    InstallerViewAction.ToggleSelection(
                        packageName = packageResult.packageName,
                        entity = item,
                        isMultiSelect = true
                    )
                )
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
                            .alpha(0.7f)
                            .basicMarquee()
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
                                isRadio = true,
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun SingleItemCard(
    item: SelectInstallEntity,
    shape: Shape,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Card(
        // Padding is now handled by the parent LazyColumn's contentPadding.
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
            onClick()
        },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = if (item.selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp/*defaultElevation = if (pkg.selected) 2.dp else 1.dp*/)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectableSubCard(item: SelectInstallEntity, isRadio: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
            onClick()
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp/*defaultElevation = if (pkg.selected) 1.dp else 2.dp*/),
        colors = CardDefaults.cardColors(containerColor = if (item.selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemContent(app: AppEntity) {
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
                    fontWeight = FontWeight.Bold
                )
                Text(
                    app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .alpha(0.7f)
                        .basicMarquee()
                )
                Text(
                    text = stringResource(R.string.installer_version, app.versionName, app.versionCode),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.installer_file_name, app.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            is AppEntity.DexMetadataEntity -> {
                Text(app.dmName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .alpha(0.7f)
                        .basicMarquee()
                )
            }
            // Should never happen!
            else -> null
        }
    }
}

/**
 * A composable for displaying an item in a multi-APK selection list.
 * Shows version information and the source file name.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MultiApkItemContent(app: AppEntity.BaseEntity) {
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
            fontWeight = FontWeight.Bold
        )
        // Filename (styled as a smaller body text with marquee)
        Text(
            text = app.data.getSourceTop().toString().removeSuffix("/").substringAfterLast('/'), // The original filename
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .alpha(0.7f)
                .basicMarquee()
        )
    }
}