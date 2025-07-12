package com.rosan.installer.ui.page.installer.dialog.inner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.PermDeviceInformation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.util.rememberInstallOptions
import com.rosan.installer.data.app.util.sortedBest
import com.rosan.installer.data.installer.model.entity.ExtendedMenuEntity
import com.rosan.installer.data.installer.model.entity.ExtendedMenuItemEntity
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.installer.dialog.DialogParams
import com.rosan.installer.ui.page.installer.dialog.DialogParamsType
import com.rosan.installer.ui.page.installer.dialog.DialogViewAction
import com.rosan.installer.ui.page.installer.dialog.DialogViewModel
import com.rosan.installer.util.getBestPermissionLabel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun installExtendedMenuDialog(
    installer: InstallerRepo, viewModel: DialogViewModel
): DialogParams {
    val installOptions = rememberInstallOptions()
    val installFlags by viewModel.installFlags.collectAsState()
    val menuEntities = remember(installOptions) {
        // 保留原来的静态“权限列表”子菜单
        val staticMenus = listOf(
            ExtendedMenuEntity(
                action = InstallExtendedMenuAction.PermissionList,
                subMenuId = InstallExtendedSubMenuId.PermissionList,
                menuItem = ExtendedMenuItemEntity(
                    nameResourceId = R.string.permission_list,
                    descriptionResourceId = R.string.permission_list_desc,
                    icon = AppIcons.Permission,
                    action = null
                )
            )
        )
        // 动态地将每个 InstallOption 转换为一个 ExtendedMenuEntity
        val dynamicOptions =
            if (installer.config.authorizer == ConfigEntity.Authorizer.Root ||
                installer.config.authorizer == ConfigEntity.Authorizer.Shizuku
            )
                installOptions.map { option ->
                    ExtendedMenuEntity(
                        action = InstallExtendedMenuAction.InstallOption,
                        menuItem = ExtendedMenuItemEntity(
                            // 使用 stringResource 从资源ID解析文本
                            nameResourceId = option.labelResource,
                            descriptionResourceId = option.descResource,
                            icon = null, // 这里没有图标
                            // 将原始的 option 对象存起来，以便后续使用
                            action = option
                        )
                    )
                }
            else emptyList()
        // 合并列表并创建可观察的状态列表
        (staticMenus + dynamicOptions).toMutableStateList()
    }

    return DialogParams(
        icon = DialogInnerParams(DialogParamsType.IconMenu.id, menuIcon),
        title = DialogInnerParams(
            DialogParamsType.InstallExtendedMenu.id,
        ) {
            Text(
                text = stringResource(R.string.extended_menu),
                style = MaterialTheme.typography.headlineMediumEmphasized
            )
        },
        content = DialogInnerParams(DialogParamsType.InstallExtendedMenu.id) {
            MenuItemWidget(menuEntities, installFlags, viewModel)
        },
        buttons = DialogButtons(
            DialogParamsType.InstallExtendedMenu.id
        ) {
            listOf(DialogButton(stringResource(R.string.next)) {
                viewModel.dispatch(DialogViewAction.InstallPrepare)
            }, DialogButton(stringResource(R.string.cancel)) {
                viewModel.dispatch(DialogViewAction.Close)
            })
        })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuItemWidget(
    entities: SnapshotStateList<ExtendedMenuEntity>,
    installFlags: Int, // 接收从 ViewModel 观察到的 flags
    viewmodel: DialogViewModel
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(4.dp), // 卡片之间的间距
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        itemsIndexed(entities) { _, item ->
            val option = when (item.action) {
                is InstallExtendedMenuAction.InstallOption -> item.menuItem.action
                else -> null
            }

            // 判断是否选中，仅对安装选项有效
            val isSelected = option?.let { (installFlags and it.value) != 0 } ?: false

            // 使用 Card 作为可点击区域和背景
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    when (item.action) {
                        is InstallExtendedMenuAction.PermissionList ->
                            when (item.subMenuId) {
                                InstallExtendedSubMenuId.PermissionList -> {
                                    viewmodel.dispatch(DialogViewAction.InstallExtendedSubMenu)
                                }

                                else -> {}
                            }

                        is InstallExtendedMenuAction.InstallOption ->
                            option?.let { opt ->
                                viewmodel.toggleInstallFlag(opt.value, !isSelected)
                            }

                        is InstallExtendedMenuAction.TextField -> {}
                    }
                },
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (option != null && isSelected) 1.dp else 2.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (option != null && isSelected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (item.action) {
                            is InstallExtendedMenuAction.PermissionList ->
                                Icon(
                                    modifier = Modifier.size(24.dp),
                                    imageVector = item.menuItem.icon
                                        ?: Icons.TwoTone.PermDeviceInformation,
                                    contentDescription = stringResource(item.menuItem.nameResourceId),
                                )

                            is InstallExtendedMenuAction.InstallOption ->
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { isChecked ->
                                        option?.let { opt ->
                                            viewmodel.toggleInstallFlag(opt.value, isChecked)
                                        }
                                    }
                                )

                            is InstallExtendedMenuAction.TextField -> {}
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(item.menuItem.nameResourceId),
                            style = MaterialTheme.typography.titleMedium
                        )
                        item.menuItem.descriptionResourceId?.let { descriptionId ->
                            Text(
                                text = stringResource(descriptionId),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun installExtendedMenuSubMenuDialog(
    installer: InstallerRepo, viewModel: DialogViewModel
): DialogParams {
    val entity =
        installer.entities.filter { it.selected }.map { it.app }.sortedBest().firstOrNull()
    val permissionList = remember(entity) {
        (entity as? AppEntity.BaseEntity)?.permissions?.sorted()?.toMutableStateList()
            ?: mutableStateListOf()
    }
    return DialogParams(
        icon = DialogInnerParams(DialogParamsType.IconMenu.id, permissionIcon),
        title = DialogInnerParams(
            DialogParamsType.InstallExtendedSubMenu.id,
        ) {
            Text(stringResource(R.string.permission_list))
        },
        content = DialogInnerParams(
            DialogParamsType.InstallExtendedSubMenu.id
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp)
            ) {
                itemsIndexed(permissionList) { index, permission ->
                    PermissionCard(
                        permission = permission,
                        // 从 ViewModel 的 state 中读取是否选中
                        isHighlight = false
                    )
                }
                item { Spacer(modifier = Modifier.size(1.dp)) }
            }
        },
        buttons = DialogButtons(
            DialogParamsType.InstallExtendedSubMenu.id
        ) {
            listOf(DialogButton(stringResource(R.string.previous)) {
                viewModel.dispatch(DialogViewAction.InstallExtendedMenu)
            })
        })
}

@Composable
fun PermissionCard(
    permission: String,         // 当前权限字符串
    isHighlight: Boolean,        // 是否被选中
) {
    val context = LocalContext.current
    // 使用 remember(key) 记住计算结果。
    // 只有当 permission 这个 key 变化时，才会重新计算标签。
    val permissionLabel = remember(permission) {
        getBestPermissionLabel(context, permission)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isHighlight) 1.dp else 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlight)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                // 直接使用我们计算好的标签
                text = permissionLabel,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                // 副标题仍然显示原始权限字符串
                text = permission,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}