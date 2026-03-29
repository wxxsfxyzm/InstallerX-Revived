// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
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
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.engine.model.AppEntity
import com.rosan.installer.domain.engine.model.DataType
import com.rosan.installer.domain.engine.model.sortedBest
import com.rosan.installer.domain.session.model.ExtendedMenuEntity
import com.rosan.installer.domain.session.model.ExtendedMenuItemEntity
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.NamedPackage
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.components.rememberInstallOptions
import com.rosan.installer.ui.page.main.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParamsType
import com.rosan.installer.util.pm.getBestPermissionLabel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun installExtendedMenuDialog(
    viewModel: InstallerViewModel
): DialogParams {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentPackageName = uiState.currentPackageName
    val managedPackages = uiState.managedInstallerPackages

    val installFlags = uiState.config.installFlags
    val selectedInstallerPackageName = uiState.config.installer
    val selectedUserId = uiState.config.targetUserId
    val customizeUserEnabled = uiState.config.enableCustomizeUser
    val authorizer = uiState.config.authorizer

    val containerType =
        uiState.analysisResults.find { it.packageName == currentPackageName }?.appEntities?.first()?.app?.sourceType
    val installOptions = rememberInstallOptions(uiState.config.authorizer)

    val selectedInstaller = remember(selectedInstallerPackageName, managedPackages) {
        managedPackages.find { it.packageName == selectedInstallerPackageName }
    }
    val defaultInstallerHintText = stringResource(id = R.string.config_follow_settings)

    val menuEntities = remember(installOptions, selectedInstaller, customizeUserEnabled, selectedUserId, uiState.availableUsers, authorizer) {
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
            if (authorizer == Authorizer.Root || authorizer == Authorizer.Shizuku) {
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
            if ((authorizer == Authorizer.Root || authorizer == Authorizer.Shizuku) && customizeUserEnabled) {
                add(
                    ExtendedMenuEntity(
                        action = InstallExtendedMenuAction.CustomizeUser,
                        menuItem = ExtendedMenuItemEntity(
                            nameResourceId = R.string.config_target_user,
                            description = uiState.availableUsers[selectedUserId] ?: "Unknown User",
                            icon = AppIcons.InstallUser,
                            action = null
                        )
                    )
                )
            }

            // Dynamic installation options
            if (authorizer == Authorizer.Root || authorizer == Authorizer.Shizuku) {
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
            MenuItemWidget(
                menuEntities,
                viewModel,
                installFlags,
                managedPackages,
                uiState.availableUsers,
                uiState.defaultInstallerFromSettings
            )
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
    installFlags: Int,
    managedPackages: List<NamedPackage>,
    availableUsers: Map<Int, String>,
    defaultInstallerFromSettings: String? // Added parameter to receive value
) {
    val haptic = LocalHapticFeedback.current

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
        // Spacing between cards
        verticalArrangement = Arrangement.spacedBy(4.dp),
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

                    // Check if selected, valid only for install options
                    val isSelected = option?.let { (installFlags and it.value) != 0 } ?: false

                    // Determine background container color
                    val containerColor = if (option != null && isSelected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceContainer

                    // Automatically derive optimal content color based on container color
                    val contentColor = MaterialTheme.colorScheme.contentColorFor(containerColor)

                    // Derive a variant color for secondary text with alpha modification
                    val variantContentColor = contentColor.copy(alpha = 0.7f)

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
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = containerColor,
                            contentColor = contentColor
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
                                    is InstallExtendedMenuAction.PermissionList,
                                    is InstallExtendedMenuAction.CustomizeInstaller,
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
                                            onCheckedChange = null, // Interaction is handled in the Card's onClick
                                        )

                                    is InstallExtendedMenuAction.TextField -> {}
                                    else -> {}
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(item.menuItem.nameResourceId),
                                    style = MaterialTheme.typography.titleMedium,
                                    // Title inherits the default contentColor perfectly
                                    color = contentColor
                                )
                                item.menuItem.descriptionResourceId?.let { descriptionId ->
                                    Text(
                                        text = stringResource(descriptionId),
                                        style = MaterialTheme.typography.bodyMedium,
                                        // Apply the derived variant color for the description
                                        color = variantContentColor
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
    viewModel: InstallerViewModel
): DialogParams {
    // Observe the single source of truth
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentPackageName = uiState.currentPackageName

    val currentPackage = uiState.analysisResults.find { it.packageName == currentPackageName }

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
                itemsIndexed(permissionList) { _, permission ->
                    PermissionCard(
                        permission = permission,
                        // Note: If you need to read selection state from viewmodel later, use uiState here
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

    // Determine the background color
    val containerColor = if (isHighlight)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceContainer

    // Automatically get the matching content color
    val contentColor = MaterialTheme.colorScheme.contentColorFor(containerColor)

    // Create a variant color based on the content color for secondary text
    val variantContentColor = contentColor.copy(alpha = 0.7f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                // Use the calculated label
                text = permissionLabel,
                style = MaterialTheme.typography.bodyLarge,
                // Inherits contentColor from Card by default, but you can explicitly set it
                color = contentColor,
            )
            Text(
                // Subtitle shows the original permission string
                text = permission,
                style = MaterialTheme.typography.bodySmall,
                // Use the variant color
                color = variantContentColor,
            )
        }
    }
}