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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataType
import com.rosan.installer.data.app.model.entity.PackageAnalysisResult
import com.rosan.installer.data.installer.model.entity.SelectInstallEntity
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParamsType
import com.rosan.installer.ui.page.main.installer.dialog.DialogViewAction
import com.rosan.installer.ui.page.main.installer.dialog.DialogViewModel
import com.rosan.installer.util.asUserReadableSplitName

@Composable
fun installChoiceDialog(
    installer: InstallerRepo, viewModel: DialogViewModel
): DialogParams {
    val analysisResults = installer.analysisResults
    val containerType = analysisResults.firstOrNull()?.appEntities?.firstOrNull()?.app?.containerType ?: DataType.NONE

    val isMultiApk = containerType == DataType.MULTI_APK || containerType == DataType.MULTI_APK_ZIP

    val titleRes = if (isMultiApk) R.string.installer_select_from_zip else R.string.installer_select_install
    val primaryButtonText = if (isMultiApk) R.string.install else R.string.next
    val primaryButtonAction = if (isMultiApk) {
        { viewModel.dispatch(DialogViewAction.InstallMultiple) }
    } else {
        { viewModel.dispatch(DialogViewAction.InstallPrepare) }
    }

    return DialogParams(
        icon = DialogInnerParams(DialogParamsType.IconWorking.id, workingIcon),
        title = DialogInnerParams(DialogParamsType.InstallChoice.id) { Text(stringResource(titleRes)) },
        subtitle = DialogInnerParams(DialogParamsType.InstallChoice.id) {
            when (containerType) {
                DataType.MULTI_APK_ZIP -> Text(stringResource(R.string.installer_multi_apk_zip_description))
                DataType.MULTI_APK -> Text(stringResource(R.string.installer_multi_apk_description))
                else -> null
            }
        },
        content = DialogInnerParams(DialogParamsType.InstallChoice.id) {
            ChoiceContent(
                analysisResults = analysisResults,
                viewModel = viewModel,
                isMultiApk = isMultiApk
            )
        },
        buttons = DialogButtons(DialogParamsType.InstallChoice.id) {
            listOf(
                DialogButton(stringResource(primaryButtonText), onClick = primaryButtonAction),
                DialogButton(stringResource(R.string.cancel)) { viewModel.dispatch(DialogViewAction.Close) }
            )
        }
    )
}

@Composable
private fun ChoiceContent(
    analysisResults: List<PackageAnalysisResult>,
    viewModel: DialogViewModel,
    isMultiApk: Boolean
) {
    // A much cleaner implementation using contentPadding for consistent spacing.
    if (isMultiApk) {
        // --- Multi-APK Mode ---
        LazyColumn(
            modifier = Modifier.heightIn(max = 325.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(analysisResults, key = { it.packageName }) { packageResult ->
                MultiApkGroupCard(
                    packageResult = packageResult,
                    viewModel = viewModel
                )
            }
        }
    } else {
        // --- Single-Package Split Mode ---
        val entities = analysisResults.firstOrNull()?.appEntities ?: emptyList()
        LazyColumn(
            modifier = Modifier.heightIn(max = 325.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(entities, key = { it.app.name + it.app.packageName }) { item ->
                SingleItemCard(
                    item = item,
                    onClick = {
                        viewModel.dispatch(
                            DialogViewAction.ToggleSelection(
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
    viewModel: DialogViewModel
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
            onClick = {
                viewModel.dispatch(
                    DialogViewAction.ToggleSelection(
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
                                        DialogViewAction.ToggleSelection(
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
private fun SingleItemCard(item: SelectInstallEntity, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Card(
        // Padding is now handled by the parent LazyColumn's contentPadding.
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
            onClick()
        },
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
                    app.splitName.asUserReadableSplitName(),
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