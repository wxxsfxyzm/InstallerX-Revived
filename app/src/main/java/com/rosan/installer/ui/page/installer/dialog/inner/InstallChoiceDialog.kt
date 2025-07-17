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
import androidx.compose.runtime.LaunchedEffect
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
import com.rosan.installer.data.app.util.DataType
import com.rosan.installer.data.installer.model.entity.SelectInstallEntity
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.installer.dialog.DialogParams
import com.rosan.installer.ui.page.installer.dialog.DialogParamsType
import com.rosan.installer.ui.page.installer.dialog.DialogViewAction
import com.rosan.installer.ui.page.installer.dialog.DialogViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun installChoiceDialog(
    installer: InstallerRepo, viewModel: DialogViewModel
): DialogParams {
    // Remember entities as a mutable state list to avoid recomposition
    val entities = remember { installer.entities.toMutableStateList() }
    // obtain containerType from entity
    val containerType = entities.firstOrNull()?.app?.containerType
    // Set title based on containerType
    val titleRes = if (containerType == DataType.MULTI_APK_ZIP)
        R.string.installer_select_install/*R.string.installer_select_from_zip*/ // TODO 准备一个新文案，例如 "从压缩包中选择"
    else
        R.string.installer_select_install

    return DialogParams(
        icon = DialogInnerParams(
            DialogParamsType.IconWorking.id, workingIcon
        ), title = DialogInnerParams(
            DialogParamsType.InstallChoice.id,
        ) {
            Text(stringResource(titleRes))
        },
        content = DialogInnerParams(DialogParamsType.InstallChoice.id) {
            if (containerType == DataType.MULTI_APK_ZIP) {
                MultiApkZipChoiceContent(entities = entities)
            } else {
                DefaultChoiceContent(entities = entities)
            }
        },
        buttons = DialogButtons(
            DialogParamsType.InstallChoice.id
        ) {
            listOf(DialogButton(stringResource(R.string.next)) {
                installer.entities = entities
                viewModel.dispatch(DialogViewAction.InstallPrepare)
            }, DialogButton(stringResource(R.string.cancel)) {
                viewModel.dispatch(DialogViewAction.Close)
            })
        }
    )
}

@Composable
private fun MultiApkZipChoiceContent(entities: MutableList<SelectInstallEntity>) {
    val groupedEntities = remember(entities.toList()) { entities.groupBy { it.app.packageName } }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(groupedEntities.entries.toList(), key = { it.key }) { (packageName, itemsInGroup) ->
            MultiApkGroupCard(
                packageName = packageName,
                itemsInGroup = itemsInGroup,
                onSelectionChanged = { selectedItem ->
                    val isSingleItemGroup = itemsInGroup.size == 1
                    // 检查被点击的项是否原本就已选中
                    val wasItemAlreadySelected = selectedItem.selected
                    for (i in entities.indices) {
                        val currentItem = entities[i]
                        // 单项安装包组，切换自己的选中状态
                        if (isSingleItemGroup && currentItem.app.name == selectedItem.app.name) {
                            entities[i] = currentItem.copy(selected = !currentItem.selected)
                            break // 找到并处理后即可退出循环
                        }
                        // 同包名多项组，实现单选版本逻辑
                        if (!isSingleItemGroup && currentItem.app.packageName == packageName) {
                            if (wasItemAlreadySelected)
                            // 如果点击的是已选项，则将组内所有项都取消选择
                                entities[i] = currentItem.copy(selected = false)
                            else
                            // 否则，执行标准单选逻辑
                                entities[i] = currentItem.copy(selected = currentItem.app.name == selectedItem.app.name)
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun MultiApkGroupCard(
    packageName: String,
    itemsInGroup: List<SelectInstallEntity>,
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
        val app = item.app as AppEntity.BaseEntity
        // 直接从 item 读取 selected 状态
        val isSelected = item.selected

        SingleItemCard(
            item = item,
            onClick = { onSelectionChanged(item) }
        )
    } else {
        // --- 组内有多个安装包，渲染可展开的选择卡片 ---
        // 默认不展开
        var isExpanded by remember { mutableStateOf(false) }
        val rotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "arrowRotation")

        // 如果组内有选中项，则默认展开
        val hasSelectionInGroup = remember(itemsInGroup) { itemsInGroup.any { it.selected } }
        LaunchedEffect(hasSelectionInGroup) {
            if (hasSelectionInGroup)
                isExpanded = true
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
private fun DefaultChoiceContent(entities: MutableList<SelectInstallEntity>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        itemsIndexed(entities, key = { _, item -> item.app.name + item.app.packageName }) { index, item ->
            SingleItemCard(
                item = item,
                onClick = { entities[index] = item.copy(selected = !item.selected) }
            )
        }
    }
}


/**
 * 可复用的单个安装项卡片
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun SingleItemCard(
    item: SelectInstallEntity,
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
                        Text(
                            text = stringResource(R.string.installer_file_name, app.name),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }

                    is AppEntity.SplitEntity -> {
                        Text(app.splitName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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