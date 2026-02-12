package com.rosan.installer.ui.page.miuix.installer.sheetcontent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.data.app.util.rememberInstallOptions
import com.rosan.installer.data.installer.model.entity.ExtendedMenuEntity
import com.rosan.installer.data.installer.model.entity.ExtendedMenuItemEntity
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.settings.model.datastore.entity.NamedPackage
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewAction.SetInstaller
import com.rosan.installer.ui.page.main.installer.InstallerViewAction.SetTargetUser
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.dialog.inner.InstallExtendedMenuAction
import com.rosan.installer.ui.page.miuix.widgets.MiuixSwitchWidget
import com.rosan.installer.ui.theme.LocalIsDark
import com.rosan.installer.ui.theme.miuixSheetCardColorDark
import com.rosan.installer.ui.util.isGestureNavigation
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.SpinnerEntry
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.extra.WindowSpinner
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun InstallExtendedMenuContent(
    installer: InstallerRepo,
    viewModel: InstallerViewModel
) {
    val isDarkMode = LocalIsDark.current
    val installOptions = rememberInstallOptions(installer.config.authorizer)
    val installFlags by viewModel.installFlags.collectAsState()
    val managedPackages by viewModel.managedInstallerPackages.collectAsState()
    val selectedInstallerPackageName by viewModel.selectedInstaller.collectAsState()
    val defaultInstallerFromSettings by viewModel.defaultInstallerFromSettings.collectAsState()
    val selectedInstaller = remember(selectedInstallerPackageName, managedPackages) {
        managedPackages.find { it.packageName == selectedInstallerPackageName }
    }
    val availableUsers by viewModel.availableUsers.collectAsState()
    val selectedUserId by viewModel.selectedUserId.collectAsState()
    val customizeUserEnabled = installer.config.enableCustomizeUser
    val menuEntities = remember(installOptions, selectedInstaller, customizeUserEnabled, selectedUserId, availableUsers) {
        buildList {
            // Installer selection
            if (installer.config.authorizer == ConfigEntity.Authorizer.Root ||
                installer.config.authorizer == ConfigEntity.Authorizer.Shizuku
            ) {
                add(
                    ExtendedMenuEntity(
                        action = InstallExtendedMenuAction.CustomizeInstaller,
                        menuItem = ExtendedMenuItemEntity(
                            nameResourceId = R.string.config_installer,
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
                            icon = AppIcons.InstallUser,
                            action = null
                        )
                    )
                )
            }

            // Dynamic install options
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

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.weight(1f, fill = false)) {
            ExtendedMenuLazyList(
                isDarkMode = isDarkMode,
                entities = menuEntities,
                viewModel = viewModel,
                installFlags = installFlags,
                managedPackages = managedPackages,
                selectedInstallerPackageName = selectedInstallerPackageName,
                defaultInstallerFromSettings = defaultInstallerFromSettings,
                availableUsers = availableUsers,
                selectedUserId = selectedUserId
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 24.dp, bottom = if (isGestureNavigation()) 24.dp else 0.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            /*TextButton(
                onClick = { viewModel.dispatch(InstallerViewAction.Close) },
                text = stringResource(R.string.cancel),
                modifier = Modifier.weight(1f),
            )*/
            TextButton(
                onClick = { viewModel.dispatch(InstallerViewAction.InstallPrepare) },
                text = stringResource(R.string.back),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ExtendedMenuLazyList(
    isDarkMode: Boolean,
    entities: SnapshotStateList<ExtendedMenuEntity>,
    viewModel: InstallerViewModel,
    installFlags: Int,
    managedPackages: List<NamedPackage>,
    selectedInstallerPackageName: String?,
    defaultInstallerFromSettings: String?,
    availableUsers: Map<Int, String>,
    selectedUserId: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardColors(
            color = if (isDynamicColor) MiuixTheme.colorScheme.surfaceContainer else
                if (isDarkMode) miuixSheetCardColorDark else Color.White,
            contentColor = MiuixTheme.colorScheme.onSurface
        )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .scrollEndHaptic()
                .overScrollVertical(),
            overscrollEffect = null,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(entities, key = { it.menuItem.nameResourceId }) { item ->
                val option = item.menuItem.action
                val isSelected = option?.let { (installFlags and it.value) != 0 } ?: false

                when (item.action) {
                    is InstallExtendedMenuAction.InstallOption -> {
                        MiuixSwitchWidget(
                            title = stringResource(item.menuItem.nameResourceId),
                            description = item.menuItem.descriptionResourceId?.let { stringResource(it) },
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                option?.let { opt ->
                                    viewModel.toggleInstallFlag(opt.value, checked)
                                }
                            }
                        )
                    }

                    is InstallExtendedMenuAction.CustomizeInstaller -> {
                        val installerFollowSettingsText = stringResource(id = R.string.config_follow_settings)
                        val installerEntries = remember(managedPackages, installerFollowSettingsText) {
                            listOf(SpinnerEntry(title = installerFollowSettingsText)) +
                                    managedPackages.map { SpinnerEntry(title = it.name) }
                        }
                        val selectedInstallerIndex = remember(selectedInstallerPackageName, managedPackages) {
                            if (selectedInstallerPackageName == defaultInstallerFromSettings || selectedInstallerPackageName == null) {
                                0 // "Follow Settings" is at index 0
                            } else {
                                managedPackages.indexOfFirst { it.packageName == selectedInstallerPackageName } + 1 // Offset by 1 for "Follow Settings"
                            }
                        }.coerceAtLeast(0) // Ensure index is not -1

                        WindowSpinner(
                            title = stringResource(R.string.config_installer),
                            items = installerEntries,
                            selectedIndex = selectedInstallerIndex,
                            onSelectedIndexChange = { newIndex ->
                                val selectedPackageName = if (newIndex == 0) {
                                    defaultInstallerFromSettings // Select "Follow Settings" -> use default value
                                } else {
                                    managedPackages.getOrNull(newIndex - 1)?.packageName
                                }
                                viewModel.dispatch(SetInstaller(selectedPackageName))
                            }
                        )
                    }

                    is InstallExtendedMenuAction.CustomizeUser -> {
                        val userEntries = remember(availableUsers) {
                            // Ensure consistent order, e.g., sort by ID
                            availableUsers.entries.sortedBy { it.key }.map { (id, name) ->
                                SpinnerEntry(title = "$name ($id)")
                            }
                        }
                        val userKeysSorted = remember(availableUsers) {
                            availableUsers.keys.sorted()
                        }
                        val selectedUserIndex = remember(selectedUserId, userKeysSorted) {
                            userKeysSorted.indexOf(selectedUserId).coerceAtLeast(0)
                        }

                        WindowSpinner(
                            title = stringResource(R.string.config_target_user),
                            items = userEntries,
                            selectedIndex = selectedUserIndex,
                            onSelectedIndexChange = { newIndex ->
                                userKeysSorted.getOrNull(newIndex)?.let { userId ->
                                    viewModel.dispatch(SetTargetUser(userId))
                                }
                            }
                        )
                    }

                    else -> Unit
                }
            }
        }
    }
}