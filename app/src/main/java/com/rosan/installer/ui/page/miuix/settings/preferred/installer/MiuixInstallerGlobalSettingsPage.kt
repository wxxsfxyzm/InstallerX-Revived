// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.settings.preferred.installer

import android.annotation.SuppressLint
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.core.env.DeviceConfig
import com.rosan.installer.core.device.model.Manufacturer
import com.rosan.installer.domain.settings.model.config.BiometricAuthMode
import com.rosan.installer.domain.settings.model.app.NamedPackage
import com.rosan.installer.domain.settings.model.app.SharedUid
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.navigation.Route
import com.rosan.installer.ui.page.main.settings.preferred.installer.InstallerSettingsAction
import com.rosan.installer.ui.page.main.settings.preferred.installer.InstallerSettingsViewModel
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.page.miuix.widgets.MiuixNavigationItemWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixSwitchWidget
import com.rosan.installer.ui.theme.getMiuixAppBarColor
import com.rosan.installer.ui.theme.installerMiuixBlurEffect
import com.rosan.installer.ui.theme.rememberMiuixBlurBackdrop
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowDialog

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun MiuixInstallerGlobalSettingsPage(
    useBlur: Boolean,
    viewModel: InstallerSettingsViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = MiuixScrollBehavior()

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    val topBarBackdrop = rememberMiuixBlurBackdrop(useBlur)

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerMiuixBlurEffect(topBarBackdrop),
                color = topBarBackdrop.getMiuixAppBarColor(),
                title = stringResource(R.string.installer_settings),
                navigationIcon = {
                    MiuixBackButton(onClick = { navigator.pop() })
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(topBarBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                start = horizontalSafeInsets.calculateStartPadding(layoutDirection),
                top = paddingValues.calculateTopPadding(),
                end = horizontalSafeInsets.calculateEndPadding(layoutDirection)
            ),
            overscrollEffect = null
        ) {
            item { Spacer(modifier = Modifier.size(12.dp)) }
            item { SmallTitle(stringResource(R.string.installer_settings_global_installer)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixNavigationItemWidget(
                        title = stringResource(R.string.dialog_settings),
                        description = stringResource(R.string.dialog_settings_desc),
                        onClick = { navigator.push(Route.DialogSettings) }
                    )
                    MiuixNavigationItemWidget(
                        title = stringResource(R.string.notification_settings),
                        description = stringResource(R.string.notification_settings_desc),
                        onClick = { navigator.push(Route.NotificationSettings) }
                    )
                    MiuixNavigationItemWidget(
                        title = stringResource(R.string.authorizer_customization),
                        description = stringResource(R.string.authorizer_customization_desc),
                        onClick = { navigator.push(Route.AuthorizerCust) }
                    )

                    if (BiometricManager
                            .from(context)
                            .canAuthenticate(BIOMETRIC_WEAK or BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
                    ) {
                        val biometricModes = remember {
                            listOf(
                                BiometricAuthMode.Disable,
                                BiometricAuthMode.Enable,
                                BiometricAuthMode.FollowConfig
                            )
                        }

                        val entries = remember(biometricModes) {
                            biometricModes.map { mode ->
                                val text = when (mode) {
                                    BiometricAuthMode.Disable -> context.getString(R.string.installer_biometric_auth_mode_disable)
                                    BiometricAuthMode.Enable -> context.getString(R.string.installer_biometric_auth_mode_enable)
                                    BiometricAuthMode.FollowConfig -> context.getString(R.string.installer_biometric_auth_mode_follow_config)
                                }
                                DropdownItem(title = text)
                            }
                        }

                        val selectedIndex = remember(uiState.installerRequireBiometricAuth, biometricModes) {
                            biometricModes.indexOf(uiState.installerRequireBiometricAuth).coerceAtLeast(0)
                        }

                        val dynamicSummary = when (biometricModes[selectedIndex]) {
                            BiometricAuthMode.Disable -> stringResource(R.string.installer_biometric_auth_mode_disable_desc)
                            BiometricAuthMode.Enable -> stringResource(R.string.installer_biometric_auth_mode_enable_desc)
                            BiometricAuthMode.FollowConfig -> stringResource(R.string.installer_biometric_auth_mode_follow_config_desc)
                        }

                        WindowSpinnerPreference(
                            title = stringResource(R.string.installer_settings_require_biometric_auth),
                            summary = dynamicSummary,
                            items = entries,
                            selectedIndex = selectedIndex,
                            onSelectedIndexChange = { index ->
                                viewModel.dispatch(
                                    InstallerSettingsAction.ChangeBiometricAuth(biometricModes[index])
                                )
                            }
                        )
                    }
                }
            }

            item { SmallTitle(stringResource(R.string.installer_settings_xposed_detection)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixSwitchWidget(
                        title = stringResource(R.string.config_detect_xposed_module),
                        description = stringResource(R.string.config_detect_xposed_module_desc),
                        checked = uiState.detectXposedModule,
                        onCheckedChange = { viewModel.dispatch(InstallerSettingsAction.ChangeDetectXposedModule(it)) }
                    )

                    AnimatedVisibility(
                        visible = uiState.detectXposedModule,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        MiuixSwitchWidget(
                            title = stringResource(R.string.config_quick_open_lsposed),
                            description = stringResource(R.string.config_quick_open_lsposed_desc),
                            checked = uiState.quickOpenLSPosed,
                            onCheckedChange = { viewModel.dispatch(InstallerSettingsAction.ChangeQuickOpenLSPosed(it)) }
                        )
                    }
                }
            }

            if (DeviceConfig.currentManufacturer == Manufacturer.OPPO || DeviceConfig.currentManufacturer == Manufacturer.ONEPLUS) {
                item { SmallTitle(stringResource(R.string.installer_oppo_related)) }
                item {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        MiuixSwitchWidget(
                            title = stringResource(id = R.string.installer_show_oem_special),
                            description = stringResource(id = R.string.installer_show_oem_special_desc),
                            checked = uiState.showOPPOSpecial,
                            onCheckedChange = { viewModel.dispatch(InstallerSettingsAction.ChangeShowOPPOSpecial(it)) }
                        )
                    }
                }
            }

            item { SmallTitle(stringResource(R.string.config_managed_installer_packages_title)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixManagedPackagesWidget(
                        noContentTitle = stringResource(R.string.config_no_preset_install_sources),
                        packages = uiState.managedInstallerPackages,
                        onAddPackage = { viewModel.dispatch(InstallerSettingsAction.AddManagedInstallerPackage(it)) },
                        onRemovePackage = {
                            viewModel.dispatch(
                                InstallerSettingsAction.RemoveManagedInstallerPackage(it)
                            )
                        }
                    )
                }
            }

            item { SmallTitle(stringResource(R.string.config_managed_blacklist_by_package_name_title)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixManagedPackagesWidget(
                        noContentTitle = stringResource(R.string.config_no_managed_blacklist),
                        packages = uiState.managedBlacklistPackages,
                        onAddPackage = { viewModel.dispatch(InstallerSettingsAction.AddManagedBlacklistPackage(it)) },
                        onRemovePackage = {
                            viewModel.dispatch(
                                InstallerSettingsAction.RemoveManagedBlacklistPackage(it)
                            )
                        }
                    )
                }
            }

            item { SmallTitle(stringResource(R.string.config_managed_blacklist_by_shared_user_id_title)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 16.dp)
                ) {
                    MiuixManagedUidsWidget(
                        noContentTitle = stringResource(R.string.config_no_managed_shared_user_id_blacklist),
                        uids = uiState.managedSharedUserIdBlacklist,
                        onAddUid = {
                            viewModel.dispatch(InstallerSettingsAction.AddManagedSharedUserIdBlacklist(it))
                        },
                        onRemoveUid = {
                            viewModel.dispatch(InstallerSettingsAction.RemoveManagedSharedUserIdBlacklist(it))
                        }
                    )
                    AnimatedVisibility(
                        visible = uiState.managedSharedUserIdBlacklist.isNotEmpty(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline
                        )
                        MiuixManagedPackagesWidget(
                            noContentTitle = stringResource(R.string.config_no_managed_shared_user_id_exempted_packages),
                            noContentDescription = stringResource(R.string.config_shared_uid_prior_to_pkgname_desc),
                            packages = uiState.managedSharedUserIdExemptedPackages,
                            infoText = stringResource(R.string.config_no_managed_shared_user_id_exempted_packages),
                            isInfoVisible = uiState.managedSharedUserIdExemptedPackages.isNotEmpty(),
                            onAddPackage = {
                                viewModel.dispatch(
                                    InstallerSettingsAction.AddManagedSharedUserIdExemptedPackages(it)
                                )
                            },
                            onRemovePackage = {
                                viewModel.dispatch(
                                    InstallerSettingsAction.RemoveManagedSharedUserIdExemptedPackages(it)
                                )
                            }
                        )
                    }
                }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}

@Composable
private fun MiuixManagedPackagesWidget(
    modifier: Modifier = Modifier,
    noContentTitle: String,
    noContentDescription: String = stringResource(R.string.config_add_one_to_get_started),
    packages: List<NamedPackage>,
    infoText: String? = null,
    isInfoVisible: Boolean = false,
    infoColor: Color = MiuixTheme.colorScheme.primary,
    onAddPackage: (NamedPackage) -> Unit,
    onRemovePackage: (NamedPackage) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf<NamedPackage?>(null) }

    Column(modifier = modifier) {
        if (packages.isEmpty()) {
            BasicComponent(
                title = noContentTitle,
                summary = noContentDescription
            )
        } else {
            packages.forEach { item ->
                BasicComponent(
                    title = item.name,
                    summary = item.packageName,
                    endActions = {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    MiuixTheme.colorScheme.primaryContainer.copy(
                                        alpha = 0.2f
                                    )
                                )
                                .clickable { showDeleteConfirmation = item }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.delete),
                                color = MiuixTheme.colorScheme.primary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            AnimatedVisibility(
                visible = isInfoVisible && !infoText.isNullOrBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(infoColor.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = infoText!!,
                        color = infoColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                modifier = Modifier.padding(bottom = 8.dp),
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
            ) {
                Icon(
                    imageVector = AppIcons.Add,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.add),
                    color = MiuixTheme.colorScheme.primary
                )
            }
        }
    }

    if (showAddDialog) {
        MiuixAddPackageDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { newItem ->
                onAddPackage(newItem)
                showAddDialog = false
            }
        )
    }

    showDeleteConfirmation?.let { itemToDelete ->
        MiuixDeleteNamedPackageConfirmationDialog(
            item = itemToDelete,
            onDismiss = { showDeleteConfirmation = null },
            onConfirm = {
                onRemovePackage(itemToDelete)
                showDeleteConfirmation = null
            }
        )
    }
}

/**
 * A Miuix-style dialog for adding a new NamedPackage.
 *
 * @param onDismiss Callback invoked when the dialog is dismissed.
 * @param onConfirm Callback invoked with the new NamedPackage when confirmed.
 */
@Composable
private fun MiuixAddPackageDialog(
    onDismiss: () -> Unit,
    onConfirm: (NamedPackage) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    val isConfirmEnabled = name.isNotBlank() && packageName.isNotBlank()
    val showState = remember { mutableStateOf(true) }

    WindowDialog(
        show = showState.value,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.config_add_new_package),
        content = {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.config_name),
                    useLabelAsPlaceholder = true,
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = stringResource(R.string.config_package_name),
                    useLabelAsPlaceholder = true,
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.cancel),
                        onClick = onDismiss
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.confirm),
                        onClick = { onConfirm(NamedPackage(name, packageName)) },
                        enabled = isConfirmEnabled,
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        }
    )
}

/**
 * A Miuix-style dialog to confirm the deletion of a NamedPackage.
 *
 * @param item The item to be deleted.
 * @param onDismiss Callback invoked when the dialog is dismissed.
 * @param onConfirm Callback invoked when the deletion is confirmed.
 */
@Composable
private fun MiuixDeleteNamedPackageConfirmationDialog(
    item: NamedPackage,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val showState = remember { mutableStateOf(true) }

    WindowDialog(
        show = showState.value,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.config_confirm_deletion),
        content = {
            Column {
                Text(stringResource(R.string.config_confirm_deletion_desc, item.name))
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.cancel),
                        onClick = onDismiss
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.delete),
                        colors = ButtonDefaults.textButtonColors(
                            textColor = MaterialTheme.colorScheme.error
                        ),
                        onClick = onConfirm
                    )
                }
            }
        }
    )
}

/**
 * A reusable Miuix-style widget to display and manage a list of SharedUid items.
 * It is stateless and relies on callbacks to handle data modifications.
 *
 * @param modifier The modifier to be applied to the widget's container.
 * @param noContentTitle The title to display if no uids are available.
 * @param uids The list of SharedUid items to display.
 * @param onAddUid A callback invoked when a new uid should be added.
 * @param onRemoveUid A callback invoked when an existing uid should be removed.
 */
@Composable
private fun MiuixManagedUidsWidget(
    modifier: Modifier = Modifier,
    noContentTitle: String,
    uids: List<SharedUid>,
    onAddUid: (SharedUid) -> Unit,
    onRemoveUid: (SharedUid) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf<SharedUid?>(null) }

    // Main container for the widget
    Column(modifier = modifier) {
        // Display each UID in the list or a placeholder message
        if (uids.isEmpty()) {
            BasicComponent(
                title = noContentTitle,
                summary = stringResource(R.string.config_add_one_to_get_started)
            )
        } else {
            uids.forEach { item ->
                BasicComponent(
                    title = item.uidName,
                    summary = "UID: ${item.uidValue}",
                    endActions = {
                        // Custom delete button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50)) // Pill shape
                                .background(
                                    MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                )
                                .clickable { showDeleteConfirmation = item }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.delete),
                                color = MiuixTheme.colorScheme.primary,
                                style = MiuixTheme.textStyles.button
                            )
                        }
                    }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                modifier = Modifier.padding(bottom = 8.dp),
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
            ) {
                Icon(
                    imageVector = AppIcons.Add,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.add),
                    color = MiuixTheme.colorScheme.primary
                )
            }
        }
    }

    // --- Dialogs ---

    // Dialog for adding a new UID
    if (showAddDialog) {
        MiuixAddUidDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { newUID ->
                onAddUid(newUID)
                showAddDialog = false
            }
        )
    }

    // Dialog for confirming deletion
    showDeleteConfirmation?.let { uidToDelete ->
        MiuixDeleteSharedUidConfirmationDialog(
            item = uidToDelete,
            onDismiss = { showDeleteConfirmation = null },
            onConfirm = {
                onRemoveUid(uidToDelete)
                showDeleteConfirmation = null
            }
        )
    }
}

/**
 * A Miuix-style dialog for adding a new SharedUid.
 *
 * @param onDismiss Callback invoked when the dialog is dismissed.
 * @param onConfirm Callback invoked with the new SharedUid when confirmed.
 */
@Composable
private fun MiuixAddUidDialog(
    onDismiss: () -> Unit,
    onConfirm: (SharedUid) -> Unit
) {
    var uidName by remember { mutableStateOf("") }
    var uidValueString by remember { mutableStateOf("") }
    val showState = remember { mutableStateOf(true) }

    // Confirm button is enabled if both name and value are not blank and value is a valid integer.
    val isConfirmEnabled = uidName.isNotBlank() && uidValueString.toIntOrNull() != null

    WindowDialog(
        show = showState.value,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.config_add_new_shared_uid),
        content = {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = uidName,
                    onValueChange = { uidName = it },
                    label = stringResource(R.string.config_shared_uid_name),
                    useLabelAsPlaceholder = true,
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = uidValueString,
                    onValueChange = { uidValueString = it },
                    label = stringResource(R.string.config_shared_uid_value),
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.cancel),
                        onClick = onDismiss
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.confirm),
                        onClick = {
                            val uidValue = uidValueString.toInt()
                            onConfirm(SharedUid(uidName, uidValue))
                        },
                        enabled = isConfirmEnabled,
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        }
    )
}

/**
 * A Miuix-style dialog to confirm the deletion of a SharedUid.
 *
 * @param item The item to be deleted.
 * @param onDismiss Callback invoked when the dialog is dismissed.
 * @param onConfirm Callback invoked when the deletion is confirmed.
 */
@Composable
private fun MiuixDeleteSharedUidConfirmationDialog(
    item: SharedUid,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val showState = remember { mutableStateOf(true) }

    WindowDialog(
        show = showState.value,
        title = stringResource(R.string.config_confirm_deletion),
    ) {
        Column {
            Text(stringResource(R.string.config_confirm_deletion_desc, item.uidName))
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.cancel),
                    onClick = onDismiss
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.delete),
                    colors = ButtonDefaults.textButtonColors(
                        textColor = MaterialTheme.colorScheme.error
                    ),
                    onClick = onConfirm
                )
            }
        }
    }
}
