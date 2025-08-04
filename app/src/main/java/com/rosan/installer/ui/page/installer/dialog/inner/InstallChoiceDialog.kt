package com.rosan.installer.ui.page.installer.dialog.inner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
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
import com.rosan.installer.data.installer.model.entity.SelectInstallEntity
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.installer.dialog.DialogParams
import com.rosan.installer.ui.page.installer.dialog.DialogParamsType
import com.rosan.installer.ui.page.installer.dialog.DialogViewAction
import com.rosan.installer.ui.page.installer.dialog.DialogViewModel
import com.rosan.installer.util.asUserReadableSplitName
import timber.log.Timber

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun installChoiceDialog(
    installer: InstallerRepo, viewModel: DialogViewModel
): DialogParams {
    // Remember entities as a mutable state list to avoid recomposition
    val entities = remember { installer.entities.toMutableStateList() }
    // obtain containerType from entity
    val containerType = entities.firstOrNull()?.app?.containerType ?: DataType.NONE
    // Set title based on containerType
    val titleRes = if (containerType == DataType.MULTI_APK_ZIP)
        R.string.installer_select_from_zip
    else
        R.string.installer_select_install

    // 根据类型决定按钮文本和行为
    val isMultiApkZip = containerType == DataType.MULTI_APK_ZIP
    val isMultiApk = containerType == DataType.MULTI_APK

    val primaryButtonText = if (isMultiApkZip || isMultiApk) R.string.install else R.string.next
    val primaryButtonAction = if (isMultiApkZip || isMultiApk) {
        {
            installer.entities = entities
            viewModel.dispatch(DialogViewAction.InstallMultiple)
        }
    } else {
        {
            installer.entities = entities
            viewModel.dispatch(DialogViewAction.InstallPrepare)
        }
    }

    return DialogParams(
        icon = DialogInnerParams(
            DialogParamsType.IconWorking.id, workingIcon
        ),
        title = DialogInnerParams(
            DialogParamsType.InstallChoice.id,
        ) {
            Text(stringResource(titleRes))
        },
        subtitle = DialogInnerParams(
            DialogParamsType.InstallChoice.id
        ) {
            when (containerType) {
                DataType.MULTI_APK_ZIP ->
                    Text(stringResource(R.string.installer_multi_apk_zip_description))

                DataType.MULTI_APK ->
                    Text(stringResource(R.string.installer_multi_apk_description))

                else -> null
            }
        },
        content = DialogInnerParams(DialogParamsType.InstallChoice.id) {
            if (containerType == DataType.MULTI_APK_ZIP || containerType == DataType.MULTI_APK) {
                MultiApkMethodChoiceContent(entities = entities, containerType = containerType)
            } else {
                DefaultChoiceContent(entities = entities, containerType = containerType)
            }
        },
        buttons = DialogButtons(
            DialogParamsType.InstallChoice.id
        ) {
            listOf(
                DialogButton(stringResource(primaryButtonText), onClick = primaryButtonAction),
                DialogButton(stringResource(R.string.cancel)) {
                    viewModel.dispatch(DialogViewAction.Close)
                }
            )
        }
    )
}

@Composable
private fun MultiApkMethodChoiceContent(
    entities: MutableList<SelectInstallEntity>,
    containerType: DataType
) {
    val groupedEntities = remember(entities.toList()) { entities.groupBy { it.app.packageName } }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        item { Spacer(modifier = Modifier.size(1.dp)) }
        items(groupedEntities.entries.toList(), key = { it.key }) { (packageName, itemsInGroup) ->
            var isExpanded by remember { mutableStateOf(itemsInGroup.any { it.selected }) }
            val onExpandToggle = remember(itemsInGroup, entities) {
                {
                    val isExpanding = !isExpanded
                    Timber.d("onExpandToggle: Group '$packageName', isExpanding=$isExpanding, hasSelection=${itemsInGroup.any { it.selected }}")
                    if (isExpanding && itemsInGroup.none { it.selected }) {
                        // Atomically update the master list: remove the old group, add the new one.
                        entities.replaceAll { currentItem ->
                            if (currentItem.app.packageName != packageName) {
                                Timber.d("----> Setting ${currentItem.app.name} to selected=true")
                                return@replaceAll currentItem
                            }
                            // Compare with the found item using direct object reference.
                            currentItem.copy(selected = (currentItem == itemsInGroup))
                        }
                    }
                    isExpanded = !isExpanded
                }
            }
            MultiApkGroupCard(
                packageName = packageName,
                itemsInGroup = itemsInGroup,
                containerType = containerType,
                isExpanded = isExpanded,
                onExpandToggle = onExpandToggle,
                onSelectionChanged = { selectedItem ->
                    Timber.d("===== onSelectionChanged: START for group '$packageName' =====")

                    val isSingleItemGroup = itemsInGroup.size == 1
                    val wasItemAlreadySelected = selectedItem.selected

                    Timber.d("--> Clicked Item: ${selectedItem.app.name}, Current Selected Status = $wasItemAlreadySelected")

                    if (!isSingleItemGroup && wasItemAlreadySelected) {
                        Timber.d("--> Logic branch: Collapse card because a selected item was clicked again.")
                        isExpanded = false
                    }

                    entities.replaceAll { currentItem ->
                        // Ignore items not in the current package group.
                        if (currentItem.app.packageName != packageName) {
                            return@replaceAll currentItem
                        }

                        if (isSingleItemGroup) {
                            // In single item group, just toggle the clicked one
                            if (currentItem === selectedItem) { // Use reference equality for absolute certainty
                                currentItem.copy(selected = !currentItem.selected)
                            } else {
                                currentItem
                            }
                        } else {
                            // Multi-item group (radio button behavior)
                            if (wasItemAlreadySelected) {
                                // If the clicked item was already selected, deselect everything in the group.
                                currentItem.copy(selected = false)
                            } else {
                                // Otherwise, select ONLY the one that is the exact same object as the clicked one.
                                val shouldBeSelected = (currentItem === selectedItem)
                                currentItem.copy(selected = shouldBeSelected)
                            }
                        }
                    }
                    val finalGroupState = entities
                        .filter { it.app.packageName == packageName }
                        .joinToString { "${it.app.name}:${it.selected}" }
                    Timber.d("===== onSelectionChanged: END. Final state for group '$packageName': [$finalGroupState] =====")
                }
            )
        }
        item { Spacer(modifier = Modifier.size(1.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun MultiApkGroupCard(
    packageName: String,
    itemsInGroup: List<SelectInstallEntity>,
    containerType: DataType,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onSelectionChanged: (SelectInstallEntity) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isSingleItemInGroup = itemsInGroup.size == 1

    val baseInfo = remember(itemsInGroup) {
        itemsInGroup.firstNotNullOfOrNull { it.app as? AppEntity.BaseEntity }
    }
    val appLabel = baseInfo?.label ?: packageName

    // ======= 条件渲染分支 =======
    if (isSingleItemInGroup) {
        // --- 组内只有一个安装包，渲染可点击切换的卡片 ---
        val item = itemsInGroup.first()

        SingleItemCard(
            item = item,
            containerType = containerType,
            onClick = { onSelectionChanged(item) }
        )
    } else {
        // --- 组内有多个安装包，渲染可展开的选择卡片 ---
        // The LaunchedEffect that was here is no longer needed, as state is managed by the parent.
        val rotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "arrowRotation")

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandToggle() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        appLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        packageName,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .alpha(0.7f)
                            .basicMarquee()
                    )
                }
                Icon(
                    imageVector = AppIcons.ArrowDropDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
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
                            // 直接从 item 读取 selected 状态
                            val isSelected = item.selected
                            val app = item.app as AppEntity.BaseEntity
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                                    onSelectionChanged(item)
                                },
                                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 1.dp else 2.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                                            onSelectionChanged(item)
                                        }
                                    )
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 12.dp)
                                    ) {
                                        Text(
                                            text = stringResource(
                                                R.string.installer_version,
                                                app.versionName,
                                                app.versionCode
                                            ),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (containerType == DataType.MULTI_APK_ZIP)
                                            Text(
                                                text = stringResource(R.string.installer_file_name, app.name),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                    }
                                }
                            }
                        }
                }
            }
        }
    }
}

/**
 * 默认的内容区域，用于非 MULTI_APK_ZIP 场景
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DefaultChoiceContent(
    entities: MutableList<SelectInstallEntity>,
    containerType: DataType
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        item { Spacer(modifier = Modifier.size(1.dp)) }
        itemsIndexed(entities, key = { _, item -> item.app.name + item.app.packageName }) { index, item ->
            SingleItemCard(
                item = item,
                containerType = containerType,
                onClick = { entities[index] = item.copy(selected = !item.selected) }
            )
        }
        item { Spacer(modifier = Modifier.size(1.dp)) }
    }
}


/**
 * 可复用的单个安装项卡片
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun SingleItemCard(
    item: SelectInstallEntity,
    containerType: DataType,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isSelected = item.selected
    val app = item.app

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
            onClick()
        },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = null, // 点击由父级 Card 处理
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                // 这个 when 语句现在可以处理所有类型的 AppEntity
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
                        if (containerType != DataType.MULTI_APK)
                            Text(
                                text = stringResource(R.string.installer_file_name, app.name),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
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
                        Text(
                            text = stringResource(R.string.installer_file_name, app.name),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    is AppEntity.CollectionEntity -> {
                        // code should not reach here, as this is not a valid entity for installation
                        throw IllegalStateException(
                            "CollectionEntity should not be present in DefaultChoiceContent"
                        )
                    }
                }
            }
        }
    }
}