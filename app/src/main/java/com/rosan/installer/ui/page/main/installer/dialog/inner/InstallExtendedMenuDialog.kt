package com.rosan.installer.ui.page.main.installer.dialog.inner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.ArrowDropDown
import androidx.compose.material.icons.twotone.PermDeviceInformation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.enums.DataType
import com.rosan.installer.data.app.util.rememberInstallOptions
import com.rosan.installer.data.app.util.sortedBest
import com.rosan.installer.data.installer.model.entity.ExtendedMenuEntity
import com.rosan.installer.data.installer.model.entity.ExtendedMenuItemEntity
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.settings.model.datastore.entity.NamedPackage
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParamsType
import com.rosan.installer.util.getBestPermissionLabel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun installExtendedMenuDialog(
    installer: InstallerRepo, viewModel: InstallerViewModel
): DialogParams {
    val currentPackageName by viewModel.currentPackageName.collectAsState()
    val containerType =
        installer.analysisResults.find { it.packageName == currentPackageName }?.appEntities?.first()?.app?.sourceType
    val installOptions = rememberInstallOptions(installer.config.authorizer)
    val installFlags by viewModel.installFlags.collectAsState()
    val managedPackages by viewModel.managedInstallerPackages.collectAsState()
    val selectedInstallerPackageName by viewModel.selectedInstaller.collectAsState()
    val selectedInstaller = remember(selectedInstallerPackageName, managedPackages) {
        managedPackages.find { it.packageName == selectedInstallerPackageName }
    }
    val availableUsers by viewModel.availableUsers.collectAsState()
    val selectedUserId by viewModel.selectedUserId.collectAsState()
    val customizeUserEnabled = installer.config.enableCustomizeUser
    val defaultInstallerHintText = stringResource(id = R.string.config_follow_settings)
    val menuEntities = remember(installOptions, selectedInstaller, customizeUserEnabled, selectedUserId, availableUsers) {
        buildList {
            // Permission List
            if (containerType == DataType.APK)
                add(
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

            // Installer selection
            if (installer.config.authorizer == ConfigEntity.Authorizer.Root ||
                installer.config.authorizer == ConfigEntity.Authorizer.Shizuku
            ) {
                add(
                    ExtendedMenuEntity(
                        action = InstallExtendedMenuAction.CustomizeInstaller,
                        menuItem = ExtendedMenuItemEntity(
                            nameResourceId = R.string.config_installer,
                            description = selectedInstaller?.name ?: defaultInstallerHintText,
                            icon = AppIcons.InstallSource,
                            action = null
                        )
                    )
                )
            }

            // User selection
            if ((installer.config.authorizer == ConfigEntity.Authorizer.Root ||
                        installer.config.authorizer == ConfigEntity.Authorizer.Shizuku
                        ) && customizeUserEnabled
            ) {
                add(
                    ExtendedMenuEntity(
                        action = InstallExtendedMenuAction.CustomizeUser,
                        menuItem = ExtendedMenuItemEntity(
                            nameResourceId = R.string.config_target_user,
                            description = availableUsers[selectedUserId] ?: "Unknown User",
                            icon = AppIcons.InstallUser,
                            action = null
                        )
                    )
                )
            }

            // 动态安装选项
            if (installer.config.authorizer == ConfigEntity.Authorizer.Root ||
                installer.config.authorizer == ConfigEntity.Authorizer.Shizuku
            ) {
                installOptions.forEach { option ->
                    add(
                        ExtendedMenuEntity(
                            action = InstallExtendedMenuAction.InstallOption,
                            menuItem = ExtendedMenuItemEntity(
                                nameResourceId = option.labelResource,
                                descriptionResourceId = option.descResource,
                                icon = null,
                                action = option
                            )
                        )
                    )
                }
            }
        }.toMutableStateList()
    }


    return DialogParams(
        icon = DialogInnerParams(DialogParamsType.IconMenu.id, /*menuIcon*/{}),
        title = DialogInnerParams(
            DialogParamsType.InstallExtendedMenu.id,
        ) {
            Text(
                text = stringResource(R.string.extended_menu),
                style = MaterialTheme.typography.headlineMediumEmphasized
            )
        },
        content = DialogInnerParams(DialogParamsType.InstallExtendedMenu.id) {
            MenuItemWidget(menuEntities, viewModel, installFlags, managedPackages, availableUsers)
        },
        buttons = dialogButtons(
            DialogParamsType.InstallExtendedMenu.id
        ) {
            listOf(DialogButton(stringResource(R.string.next)) {
                viewModel.dispatch(InstallerViewAction.InstallPrepare)
            }, DialogButton(stringResource(R.string.cancel)) {
                viewModel.dispatch(InstallerViewAction.Close)
            })
        })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuItemWidget(
    entities: SnapshotStateList<ExtendedMenuEntity>,
    viewmodel: InstallerViewModel,
    installFlags: Int, // flags from viewmodel
    managedPackages: List<NamedPackage>,
    availableUsers: Map<Int, String>
) {
    val haptic = LocalHapticFeedback.current
    val defaultInstallerFromSettings by viewmodel.defaultInstallerFromSettings.collectAsState()

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

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(4.dp), // 卡片之间的间距
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .heightIn(max = 325.dp)
            .clip(
                // Clip the whole column to ensure content stays within the rounded bounds.
                if (entities.size == 1) singleShape else RoundedCornerShape(cornerRadius)
            ),
    ) {
        itemsIndexed(entities, key = { _, item -> item.menuItem.nameResourceId }) { index, item ->
            // Determine the shape based on the item's position.
            val shape = when {
                entities.size == 1 -> singleShape
                index == 0 -> topShape
                index == entities.size - 1 -> bottomShape
                else -> middleShape
            }

            when (item.action) {
                is InstallExtendedMenuAction.CustomizeInstaller -> {
                    var expanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable), // This is important for the dropdown position
                            onClick = { /* Card itself is not clickable, dropdown handles it */ },
                            shape = shape,
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    modifier = Modifier.size(24.dp),
                                    imageVector = item.menuItem.icon ?: Icons.TwoTone.PermDeviceInformation,
                                    contentDescription = stringResource(item.menuItem.nameResourceId),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(item.menuItem.nameResourceId),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    // Use the dynamic description from the entity
                                    item.menuItem.description?.let { description ->
                                        Text(
                                            text = description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.TwoTone.ArrowDropDown,
                                    contentDescription = "Open menu"
                                )
                            }
                        }

                        // The actual dropdown menu
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            // "System Default" option
                            // Not needed for the moment
                            DropdownMenuItem(
                                text = { Text(text = stringResource(id = R.string.config_follow_settings)) },
                                onClick = {
                                    viewmodel.dispatch(InstallerViewAction.SetInstaller(defaultInstallerFromSettings))
                                    expanded = false
                                }
                            )
                            // Options from managed packages
                            managedPackages.forEach { pkg ->
                                DropdownMenuItem(
                                    text = { Text(text = pkg.name) },
                                    onClick = {
                                        viewmodel.dispatch(InstallerViewAction.SetInstaller(pkg.packageName))
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                is InstallExtendedMenuAction.CustomizeUser -> {
                    var expanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            onClick = { /* No-op */ },
                            shape = shape,
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    modifier = Modifier.size(24.dp),
                                    imageVector = item.menuItem.icon ?: Icons.TwoTone.PermDeviceInformation,
                                    contentDescription = stringResource(item.menuItem.nameResourceId),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(item.menuItem.nameResourceId),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    item.menuItem.description?.let { description ->
                                        Text(
                                            text = description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.TwoTone.ArrowDropDown,
                                    contentDescription = "Open menu"
                                )
                            }
                        }

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            availableUsers.forEach { (userId, userName) ->
                                DropdownMenuItem(
                                    text = { Text("$userName($userId)") },
                                    onClick = {
                                        viewmodel.dispatch(InstallerViewAction.SetTargetUser(userId))
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                else -> { // Logic for other card types (PermissionList, InstallOption)
                    val option = when (item.action) {
                        is InstallExtendedMenuAction.InstallOption -> item.menuItem.action
                        else -> null
                    }

                    // 判断是否选中，仅对安装选项有效
                    val isSelected = option?.let { (installFlags and it.value) != 0 } ?: false

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = shape,
                        onClick = {
                            when (item.action) {
                                is InstallExtendedMenuAction.PermissionList ->
                                    when (item.subMenuId) {
                                        InstallExtendedSubMenuId.PermissionList -> {
                                            viewmodel.dispatch(InstallerViewAction.InstallExtendedSubMenu)
                                        }

                                        else -> {}
                                    }

                                is InstallExtendedMenuAction.InstallOption -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                                    option?.let { opt ->
                                        viewmodel.toggleInstallFlag(opt.value, !isSelected)
                                    }
                                }

                                else -> {}
                            }
                        },
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 0.dp, // if (option != null && isSelected) 1.dp else 2.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (option != null && isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceContainer
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

                                    is InstallExtendedMenuAction.CustomizeInstaller ->
                                        Icon(
                                            modifier = Modifier.size(24.dp),
                                            imageVector = item.menuItem.icon
                                                ?: Icons.TwoTone.PermDeviceInformation,
                                            contentDescription = stringResource(item.menuItem.nameResourceId),
                                        )

                                    is InstallExtendedMenuAction.CustomizeUser ->
                                        Icon(
                                            modifier = Modifier.size(24.dp),
                                            imageVector = item.menuItem.icon
                                                ?: Icons.TwoTone.PermDeviceInformation,
                                            contentDescription = stringResource(item.menuItem.nameResourceId),
                                        )

                                    is InstallExtendedMenuAction.InstallOption ->
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = null, // 交互处理在 Card 的 onClick 中
                                        )

                                    is InstallExtendedMenuAction.TextField -> {}
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(item.menuItem.nameResourceId),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
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
        item { Spacer(modifier = Modifier.size(1.dp)) }
    }
}

@Composable
fun installExtendedMenuSubMenuDialog(
    installer: InstallerRepo, viewModel: InstallerViewModel
): DialogParams {
    val currentPackageName by viewModel.currentPackageName.collectAsState()
    val currentPackage = installer.analysisResults.find { it.packageName == currentPackageName }

    val entity = currentPackage?.appEntities
        ?.filter { it.selected }
        ?.map { it.app }
        ?.sortedBest()
        ?.firstOrNull()
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
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 0.dp)
                    .heightIn(max = 400.dp),
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
        buttons = dialogButtons(
            DialogParamsType.InstallExtendedSubMenu.id
        ) {
            listOf(DialogButton(stringResource(R.string.previous)) {
                viewModel.dispatch(InstallerViewAction.InstallExtendedMenu)
            })
        })
}

@Composable
fun PermissionCard(
    permission: String,
    isHighlight: Boolean,
) {
    val context = LocalContext.current

    val permissionLabel = remember(permission) {
        context.getBestPermissionLabel(permission)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            0.dp, // if (isHighlight) 1.dp else 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlight)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainer
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
                color = MaterialTheme.colorScheme.onSurface,
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