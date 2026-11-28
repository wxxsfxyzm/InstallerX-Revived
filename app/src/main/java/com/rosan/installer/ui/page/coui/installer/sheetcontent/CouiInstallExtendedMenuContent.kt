// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer.sheetcontent

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itos.coui.layout.Divider
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.InstallerMode
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.coui.widgets.CouiActionButton
import com.rosan.installer.ui.page.coui.widgets.CouiCard
import com.rosan.installer.ui.page.coui.widgets.CouiSpinnerItem
import com.rosan.installer.ui.page.coui.widgets.CouiSwitchItem
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewAction.SetInstaller
import com.rosan.installer.ui.page.main.installer.InstallerViewAction.SetTargetUser
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.components.rememberInstallOptions
import com.rosan.installer.ui.page.main.installer.dialog.ExtendedMenuEntity
import com.rosan.installer.ui.page.main.installer.dialog.ExtendedMenuItemEntity
import com.rosan.installer.ui.page.main.installer.dialog.inner.InstallExtendedMenuAction

@Composable
fun CouiInstallExtendedMenuContent(viewModel: InstallerViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val installFlags = uiState.config.installFlags
    val selectedInstallerPackageName = uiState.config.installer
    val selectedUserId = uiState.config.targetUserId
    val customizeUserEnabled = uiState.config.enableCustomizeUser
    val authorizer = uiState.config.authorizer
    val managedPackages = uiState.managedInstallerPackages
    val defaultInstallerFromSettings = uiState.defaultInstallerFromSettings
    val availableUsers = uiState.availableUsers
    val installOptions = rememberInstallOptions(authorizer)
    val installerMode = uiState.config.installerMode

    val menuEntities = remember(
        installOptions,
        selectedInstallerPackageName,
        installerMode,
        customizeUserEnabled,
        selectedUserId,
        availableUsers,
        authorizer,
    ) {
        buildList {
            if (authorizer == Authorizer.Root || authorizer == Authorizer.Shizuku) {
                add(
                    ExtendedMenuEntity(
                        action = InstallExtendedMenuAction.CustomizeInstallerMode,
                        menuItem = ExtendedMenuItemEntity(
                            nameResourceId = R.string.config_declare_installer,
                            icon = AppIcons.InstallSource,
                            action = null,
                        ),
                    ),
                )
            }
            if ((authorizer == Authorizer.Root || authorizer == Authorizer.Shizuku) && customizeUserEnabled) {
                add(
                    ExtendedMenuEntity(
                        action = InstallExtendedMenuAction.CustomizeUser,
                        menuItem = ExtendedMenuItemEntity(
                            nameResourceId = R.string.config_target_user,
                            icon = AppIcons.InstallUser,
                            action = null,
                        ),
                    ),
                )
            }
            if (authorizer == Authorizer.Root || authorizer == Authorizer.Shizuku) {
                installOptions.forEach { option ->
                    add(
                        ExtendedMenuEntity(
                            action = InstallExtendedMenuAction.InstallOption,
                            menuItem = ExtendedMenuItemEntity(
                                nameResourceId = option.labelResource,
                                descriptionResourceId = option.descResource,
                                icon = null,
                                action = option,
                            ),
                        ),
                    )
                }
            }
        }.toMutableStateList()
    }

    BackHandler {
        viewModel.dispatch(InstallerViewAction.InstallPrepare)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.weight(1f, fill = false)) {
            CouiCard {
                LazyColumn(overscrollEffect = rememberCouiOverscrollEffect()) {
                    itemsIndexed(menuEntities, key = { _, it -> it.menuItem.nameResourceId }) { index, item ->
                        if (index > 0) Divider()
                        val option = item.menuItem.action
                        val isSelected = option?.let { (installFlags and it.value) != 0 } ?: false

                        when (item.action) {
                            is InstallExtendedMenuAction.InstallOption -> {
                                CouiSwitchItem(
                                    title = stringResource(item.menuItem.nameResourceId),
                                    description = item.menuItem.descriptionResourceId?.let { stringResource(it) },
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        option?.let { opt -> viewModel.toggleInstallFlag(opt.value, checked) }
                                    },
                                )
                            }

                            is InstallExtendedMenuAction.CustomizeInstallerMode -> {
                                val modeSelf = stringResource(R.string.config_installer_mode_self)
                                val modeInitiator = stringResource(R.string.config_installer_mode_initiator)
                                val followSettings = stringResource(R.string.config_follow_settings)
                                val unifiedLabels = remember(modeSelf, modeInitiator, followSettings, managedPackages) {
                                    buildList {
                                        add(modeSelf)
                                        add(modeInitiator)
                                        add(followSettings)
                                        managedPackages.forEach { add(it.name) }
                                    }
                                }
                                val selectedIdx = remember(
                                    installerMode,
                                    selectedInstallerPackageName,
                                    defaultInstallerFromSettings,
                                    managedPackages,
                                ) {
                                    when (installerMode) {
                                        InstallerMode.Self -> 0
                                        InstallerMode.Initiator -> 1
                                        InstallerMode.Custom -> {
                                            if (selectedInstallerPackageName == defaultInstallerFromSettings ||
                                                selectedInstallerPackageName == null
                                            ) {
                                                2
                                            } else {
                                                val pkgIndex = managedPackages.indexOfFirst {
                                                    it.packageName == selectedInstallerPackageName
                                                }
                                                if (pkgIndex != -1) 3 + pkgIndex else 2
                                            }
                                        }
                                    }
                                }

                                CouiSpinnerItem(
                                    title = stringResource(R.string.config_declare_installer),
                                    items = unifiedLabels,
                                    selectedIndex = selectedIdx,
                                    onSelectedIndexChange = { newIndex ->
                                        when (newIndex) {
                                            0 -> viewModel.dispatch(InstallerViewAction.SetInstallerMode(InstallerMode.Self))
                                            1 -> viewModel.dispatch(InstallerViewAction.SetInstallerMode(InstallerMode.Initiator))
                                            2 -> {
                                                viewModel.dispatch(InstallerViewAction.SetInstallerMode(InstallerMode.Custom))
                                                viewModel.dispatch(SetInstaller(defaultInstallerFromSettings))
                                            }
                                            else -> {
                                                viewModel.dispatch(InstallerViewAction.SetInstallerMode(InstallerMode.Custom))
                                                managedPackages.getOrNull(newIndex - 3)?.let {
                                                    viewModel.dispatch(SetInstaller(it.packageName))
                                                }
                                            }
                                        }
                                    },
                                )
                            }

                            is InstallExtendedMenuAction.CustomizeUser -> {
                                val userKeysSorted = remember(availableUsers) { availableUsers.keys.sorted() }
                                val userLabels = remember(availableUsers, userKeysSorted) {
                                    userKeysSorted.map { id -> "${availableUsers[id]} ($id)" }
                                }
                                val selectedIdx = remember(selectedUserId, userKeysSorted) {
                                    userKeysSorted.indexOf(selectedUserId).coerceAtLeast(0)
                                }

                                CouiSpinnerItem(
                                    title = stringResource(R.string.config_target_user),
                                    items = userLabels,
                                    selectedIndex = selectedIdx,
                                    onSelectedIndexChange = { newIndex ->
                                        userKeysSorted.getOrNull(newIndex)?.let { userId ->
                                            viewModel.dispatch(SetTargetUser(userId))
                                        }
                                    },
                                )
                            }

                            else -> Unit
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 12.dp),
        ) {
            CouiActionButton(
                text = stringResource(R.string.back),
                onClick = { viewModel.dispatch(InstallerViewAction.InstallPrepare) },
                primary = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
