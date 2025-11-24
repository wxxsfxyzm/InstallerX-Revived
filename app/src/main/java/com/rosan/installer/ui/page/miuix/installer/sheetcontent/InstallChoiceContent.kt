package com.rosan.installer.ui.page.miuix.installer.sheetcontent

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
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataType
import com.rosan.installer.data.app.model.entity.MmzSelectionMode
import com.rosan.installer.data.app.model.entity.PackageAnalysisResult
import com.rosan.installer.data.app.model.entity.SessionMode
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.main.installer.dialog.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.dialog.InstallerViewModel
import com.rosan.installer.ui.page.miuix.widgets.MiuixCheckboxWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixInstallChoiceTipCard
import com.rosan.installer.ui.page.miuix.widgets.MiuixMultiApkCheckboxWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixNavigationItemWidget
import com.rosan.installer.ui.theme.miuixSheetCardColorDark
import com.rosan.installer.ui.util.getSupportSubtitle
import com.rosan.installer.util.asUserReadableSplitName
import com.rosan.installer.util.getDisplayName
import com.rosan.installer.util.getSplitType
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun InstallChoiceContent(
    colorScheme: ColorScheme,
    isDarkMode: Boolean,
    installer: InstallerRepo,
    viewModel: InstallerViewModel,
    onCancel: () -> Unit
) {
    val analysisResults = installer.analysisResults
    val sourceType = analysisResults.firstOrNull()?.appEntities?.firstOrNull()?.app?.sourceType ?: DataType.NONE
    val currentSessionMode = analysisResults.firstOrNull()?.sessionMode ?: SessionMode.Single
    val isMultiApk = currentSessionMode == SessionMode.Batch
    val isModuleApk = sourceType == DataType.MIXED_MODULE_APK
    val isMixedModuleZip = sourceType == DataType.MIXED_MODULE_ZIP
    var selectionMode by remember(sourceType) { mutableStateOf(MmzSelectionMode.INITIAL_CHOICE) }

    val primaryButtonTextRes = if (isMultiApk) R.string.install else R.string.next
    val primaryButtonAction = if (isMultiApk) {
        { viewModel.dispatch(InstallerViewAction.InstallMultiple) }
    } else {
        { viewModel.dispatch(InstallerViewAction.InstallPrepare) }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val cardText = sourceType.getSupportSubtitle(selectionMode = selectionMode)

        if (cardText != null)
            MiuixInstallChoiceTipCard(cardText)

        if (isMixedModuleZip && selectionMode == MmzSelectionMode.INITIAL_CHOICE) {
            Box(modifier = Modifier.weight(1f, fill = false)) {
                MixedModuleZip_InitialChoice(
                    colorScheme = colorScheme,
                    isDarkMode = isDarkMode,
                    analysisResults = analysisResults,
                    viewModel = viewModel,
                    onSelectModule = { viewModel.dispatch(InstallerViewAction.InstallPrepare) }
                ) { selectionMode = MmzSelectionMode.APK_CHOICE }
            }
        } else {
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

            Box(modifier = Modifier.weight(1f, fill = false)) {
                ChoiceLazyList(
                    colorScheme = colorScheme,
                    isDarkMode = isDarkMode,
                    analysisResults = resultsForList,
                    viewModel = viewModel,
                    isModuleApk = isModuleApk,
                    isMultiApk = isMultiApk || (isMixedModuleZip && selectionMode == MmzSelectionMode.APK_CHOICE)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMultiApk || isModuleApk || isMixedModuleZip) {
                val isBack = isMixedModuleZip && selectionMode == MmzSelectionMode.APK_CHOICE
                TextButton(
                    onClick = {
                        if (isBack) {
                            selectionMode = MmzSelectionMode.INITIAL_CHOICE
                        } else {
                            onCancel()
                        }
                    },
                    text = stringResource(if (isBack) R.string.back else R.string.cancel),
                    modifier = Modifier.weight(1f),
                )
            }

            val showPrimaryButton =
                (!isModuleApk && !isMixedModuleZip) || (isMixedModuleZip && selectionMode == MmzSelectionMode.APK_CHOICE)

            if (showPrimaryButton) {
                val (currentPrimaryTextRes, currentPrimaryAction) =
                    if (isMixedModuleZip && selectionMode == MmzSelectionMode.APK_CHOICE) {
                        R.string.install to { viewModel.dispatch(InstallerViewAction.InstallMultiple) }
                    } else {
                        primaryButtonTextRes to primaryButtonAction
                    }

                TextButton(
                    onClick = currentPrimaryAction,
                    text = stringResource(currentPrimaryTextRes),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    enabled = analysisResults.any { it.appEntities.any { it.selected } },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ChoiceLazyList(
    colorScheme: ColorScheme,
    isDarkMode: Boolean,
    analysisResults: List<PackageAnalysisResult>,
    viewModel: InstallerViewModel,
    isModuleApk: Boolean,
    isMultiApk: Boolean
) {
    val cardColor = if (isDynamicColor) colorScheme.surfaceContainer else
        if (isDarkMode) miuixSheetCardColorDark else Color.White

    if (isModuleApk) {
        val allSelectableEntities = analysisResults.flatMap { it.appEntities }
        val baseSelectableEntity = allSelectableEntities.firstOrNull { it.app is AppEntity.BaseEntity }
        val moduleSelectableEntity = allSelectableEntities.firstOrNull { it.app is AppEntity.ModuleEntity }

        LazyColumn(
            modifier = Modifier
                .wrapContentSize()
                .scrollEndHaptic()
                .overScrollVertical(),
            overscrollEffect = null,
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardColors(
                        color = cardColor,
                        contentColor = MiuixTheme.colorScheme.onSurface
                    )
                ) {
                    if (baseSelectableEntity != null) {
                        val baseEntityInfo = baseSelectableEntity.app as AppEntity.BaseEntity
                        MiuixNavigationItemWidget(
                            title = baseEntityInfo.label ?: "N/A",
                            description = stringResource(R.string.installer_package_name, baseEntityInfo.packageName),
                            onClick = {
                                viewModel.dispatch(
                                    InstallerViewAction.ToggleSelection(
                                        packageName = baseSelectableEntity.app.packageName,
                                        entity = baseSelectableEntity,
                                        isMultiSelect = false
                                    )
                                )
                                viewModel.dispatch(InstallerViewAction.InstallPrepare)
                            }
                        )
                    }

                    if (moduleSelectableEntity != null) {
                        val moduleEntityInfo = moduleSelectableEntity.app as AppEntity.ModuleEntity
                        MiuixNavigationItemWidget(
                            title = moduleEntityInfo.name,
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
            }
        }
    } else if (isMultiApk) {
        LazyColumn(
            modifier = Modifier
                .wrapContentSize()
                .scrollEndHaptic()
                .overScrollVertical(),
            overscrollEffect = null,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            itemsIndexed(analysisResults, key = { _, it -> it.packageName }) { _, packageResult ->
                val itemsInGroup = packageResult.appEntities
                val baseInfo = remember(itemsInGroup) {
                    itemsInGroup.firstNotNullOfOrNull { it.app as? AppEntity.BaseEntity }
                }
                val appLabel = baseInfo?.label ?: packageResult.packageName

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardColors(
                        color = cardColor,
                        contentColor = MiuixTheme.colorScheme.onSurface
                    )
                ) {
                    if (itemsInGroup.size == 1) {
                        // --- Only one option - Use MiuixCheckboxWidget inside Card ---
                        val item = itemsInGroup.first()
                        val app = item.app
                        val versionText = (app as? AppEntity.BaseEntity)?.let {
                            stringResource(R.string.installer_version, it.versionName, it.versionCode)
                        } ?: ""

                        MiuixCheckboxWidget(
                            title = appLabel,
                            description = "${packageResult.packageName}\n$versionText",
                            checked = item.selected,
                            onCheckedChange = {
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
                        // --- Multiple options - Use Column inside Card ---
                        BasicComponent(
                            title = appLabel,
                            summary = packageResult.packageName
                        )
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline
                        )
                        itemsInGroup
                            .sortedByDescending { (it.app as? AppEntity.BaseEntity)?.versionCode ?: 0 }
                            .forEach { item ->
                                val appBaseEntity = item.app as? AppEntity.BaseEntity
                                val titleText = if (appBaseEntity != null) {
                                    stringResource(
                                        R.string.installer_version,
                                        appBaseEntity.versionName,
                                        appBaseEntity.versionCode
                                    )
                                } else {
                                    item.app.name
                                }
                                val summaryText = appBaseEntity?.data?.getSourceTop()?.toString()?.removeSuffix("/")
                                    ?.substringAfterLast('/')

                                MiuixMultiApkCheckboxWidget(
                                    title = titleText,
                                    summary = summaryText,
                                    checked = item.selected,
                                    onCheckedChange = {
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
    } else { // Single-Package Split Mode
        val allEntities = analysisResults.firstOrNull()?.appEntities ?: emptyList()
        val baseEntities =
            allEntities.filter { it.app is AppEntity.BaseEntity || it.app is AppEntity.DexMetadataEntity }
        val splitEntities = allEntities.filter { it.app is AppEntity.SplitEntity }

        // Group splits by type
        val groupedSplits = splitEntities
            .groupBy { (it.app as AppEntity.SplitEntity).splitName.getSplitType() }
            .toSortedMap(compareBy { it.ordinal }) // Sort groups by enum order

        LazyColumn(
            modifier = Modifier
                .wrapContentSize()
                .scrollEndHaptic()
                .overScrollVertical(),
            overscrollEffect = null,
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // --- Base Application ---
            if (baseEntities.isNotEmpty()) {
                item {
                    SmallTitle(
                        stringResource(R.string.split_name_base_group_title),
                        insideMargin = PaddingValues(16.dp, 8.dp)
                    )
                }
                itemsIndexed(baseEntities, key = { _, it -> it.app.name + it.app.packageName }) { _, item ->
                    val (title, description) = when (val app = item.app) {
                        is AppEntity.BaseEntity -> {
                            val desc =
                                "${app.packageName}\n${
                                    stringResource(
                                        R.string.installer_version,
                                        app.versionName,
                                        app.versionCode
                                    )
                                }"
                            (app.label ?: app.packageName) to desc
                        }

                        is AppEntity.DexMetadataEntity -> {
                            app.dmName to app.packageName
                        }

                        else -> item.app.name to item.app.packageName
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardColors(
                            color = cardColor,
                            contentColor = MiuixTheme.colorScheme.onSurface
                        ),
                        pressFeedbackType = PressFeedbackType.Sink
                    ) {
                        MiuixCheckboxWidget(
                            title = title,
                            description = description,
                            checked = item.selected,
                            onCheckedChange = {
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

            // --- Grouped Splits ---
            groupedSplits.forEach { (splitType, entitiesInGroup) ->
                if (entitiesInGroup.isEmpty()) return@forEach

                item {
                    SmallTitle(
                        text = splitType.getDisplayName(),
                        insideMargin = PaddingValues(16.dp, 8.dp)
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardColors(
                            color = cardColor,
                            contentColor = MiuixTheme.colorScheme.onSurface
                        )
                    ) {
                        Column {
                            entitiesInGroup.forEach { item ->
                                val app = item.app as AppEntity.SplitEntity
                                val title = app.splitName.asUserReadableSplitName()
                                val description = stringResource(R.string.installer_file_name, app.name)

                                MiuixCheckboxWidget(
                                    title = title,
                                    description = description,
                                    checked = item.selected,
                                    onCheckedChange = {
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
            }
        }
    }
}

@Composable
private fun MixedModuleZip_InitialChoice(
    colorScheme: ColorScheme,
    isDarkMode: Boolean,
    analysisResults: List<PackageAnalysisResult>,
    viewModel: InstallerViewModel,
    onSelectModule: () -> Unit,
    onSelectApk: () -> Unit
) {
    val cardColor = if (isDynamicColor) colorScheme.surfaceContainer else
        if (isDarkMode) miuixSheetCardColorDark else Color.White

    val allSelectableEntities = analysisResults.flatMap { it.appEntities }
    val moduleSelectableEntity = allSelectableEntities.firstOrNull { it.app is AppEntity.ModuleEntity }
    val baseSelectableEntity = allSelectableEntities.firstOrNull { it.app is AppEntity.BaseEntity }

    LazyColumn(
        modifier = Modifier
            .wrapContentSize()
            .scrollEndHaptic()
            .overScrollVertical(),
        overscrollEffect = null,
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardColors(
                    color = cardColor,
                    contentColor = MiuixTheme.colorScheme.onSurface
                )
            ) {
                if (moduleSelectableEntity != null) {
                    val moduleEntityInfo = moduleSelectableEntity.app as AppEntity.ModuleEntity
                    MiuixNavigationItemWidget(
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
                            onSelectModule()
                        }
                    )
                }

                if (baseSelectableEntity != null) {
                    MiuixNavigationItemWidget(
                        title = stringResource(R.string.installer_choice_install_as_app),
                        description = stringResource(R.string.installer_choice_install_as_app_desc),
                        onClick = {
                            onSelectApk()
                        }
                    )
                }
            }
        }
    }
}