package com.rosan.installer.ui.page.installer.dialog.inner

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import timber.log.Timber

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
                action = InstallExtendedMenuAction.SubMenu,
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

@Composable
fun MenuItemWidget(
    entities: SnapshotStateList<ExtendedMenuEntity>,
    installFlags: Int, // 接收从 ViewModel 观察到的 flags
    viewmodel: DialogViewModel
) {
    LazyColumn {
        itemsIndexed(entities) { index, item ->
            // 为了在 Checkbox 分支中复用，我们先在这里获取 option 对象
            val option = when (item.action) {
                is InstallExtendedMenuAction.InstallOption -> item.menuItem.action
                else -> null
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .clickable {
                        when (item.action) {
                            is InstallExtendedMenuAction.SubMenu ->
                                when (item.subMenuId) {
                                    InstallExtendedSubMenuId.PermissionList -> {
                                        // 打开权限列表子菜单
                                        viewmodel.dispatch(DialogViewAction.InstallExtendedSubMenu)
                                    }

                                    else -> null // 其他子菜单可以在这里处理
                                }

                            is InstallExtendedMenuAction.InstallOption ->

                                // 点击事件的逻辑
                                // 不再修改本地的 item.selected
                                // 而是调用 ViewModel 的方法来更新全局的 installFlags
                                option?.let { opt ->
                                    val isSelected = (installFlags and opt.value) != 0
                                    viewmodel.toggleInstallFlag(opt.value, !isSelected)
                                }

                            is InstallExtendedMenuAction.TextField -> null
                        }
                    },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp), // 标准的可点击区域大小，与 Checkbox 保持一致
                    contentAlignment = Alignment.Center // 让内部的 Checkbox 或 Icon 居中
                ) {
                    when (item.action) {
                        is InstallExtendedMenuAction.SubMenu ->
                            Icon(
                                modifier = Modifier.size(24.dp),
                                imageVector = item.menuItem.icon
                                    ?: Icons.TwoTone.PermDeviceInformation,
                                contentDescription = stringResource(item.menuItem.nameResourceId),
                            )

                        is InstallExtendedMenuAction.InstallOption ->
                            Checkbox(
                                checked = option?.let { (installFlags and it.value) != 0 } ?: false,
                                onCheckedChange = { isChecked ->
                                    // onCheckedChange 的回调同样调用 ViewModel 的方法
                                    option?.let { opt ->
                                        viewmodel.toggleInstallFlag(opt.value, isChecked)
                                    }
                                }
                            )

                        is InstallExtendedMenuAction.TextField ->
                            null
                    }
                }
                Column(
                    modifier = Modifier
                        .padding(start = 16.dp) // 与前面的图标/复选框保持固定间距
                        .weight(1f) // (可选，但推荐) 让文本列占据剩余空间，防止长文本溢出
                ) {
                    Text(
                        text = stringResource(item.menuItem.nameResourceId),
                        style = MaterialTheme.typography.titleMedium
                    )
                    item.menuItem.descriptionResourceId?.let { descriptionId ->
                        Text(
                            text = stringResource(descriptionId),
                            style = MaterialTheme.typography.bodyMedium
                        )
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
    // 从 ViewModel 中收集状态。当 ViewModel 中的值变化时，这里会自动更新。
    val permissionToGrant by viewModel.permissionsToGrant.collectAsState()
    // 在 entity 变化时，用 ViewModel 的方法初始化一次状态
    LaunchedEffect(Unit) {
        viewModel.initializePermissionsIfNeeded(entity)
    }
    Timber.tag("permissionList").d("Permissions: $permissionToGrant")
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
                        isSelected = permissionToGrant.contains(permission),
                        onPermissionClick = {
                            // 将点击事件通知给 ViewModel 去处理
                            viewModel.togglePermissionGrant(permission)

                        }
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
    isSelected: Boolean,        // 是否被选中
    onPermissionClick: () -> Unit // 点击事件回调
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
            defaultElevation = if (isSelected) 1.dp else 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        onClick = onPermissionClick // 使用传入的回调
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