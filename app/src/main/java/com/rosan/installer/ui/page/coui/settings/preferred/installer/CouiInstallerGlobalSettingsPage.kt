// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.settings.preferred.installer

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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.dialog.Dialog
import com.itos.coui.icons.CouiIcons
import com.itos.coui.layout.Divider
import com.itos.coui.layout.Group
import com.itos.coui.layout.Scaffold
import com.itos.coui.menu.rememberCouiWindowBackdrop
import com.itos.coui.shape.CouiCapsuleShape
import com.itos.coui.shape.CouiShape
import com.itos.coui.theme.CouiTheme
import com.itos.coui.topappbar.CouiTopAppBarDefaults
import com.itos.coui.topappbar.LargeTopAppBar
import com.rosan.installer.R
import com.rosan.installer.core.device.model.Manufacturer
import com.rosan.installer.core.env.DeviceConfig
import com.rosan.installer.domain.settings.model.app.NamedPackage
import com.rosan.installer.domain.settings.model.app.SharedUid
import com.rosan.installer.domain.settings.model.config.BiometricAuthMode
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.navigation.Route
import com.rosan.installer.ui.page.coui.widgets.CouiBackButton
import com.rosan.installer.ui.page.coui.widgets.CouiBadge
import com.rosan.installer.ui.page.coui.widgets.CouiBasicItem
import com.rosan.installer.ui.page.coui.widgets.CouiNavigationItem
import com.rosan.installer.ui.page.coui.widgets.CouiSpinnerItem
import com.rosan.installer.ui.page.coui.widgets.CouiSwitchItem
import com.rosan.installer.ui.page.coui.widgets.rememberCouiDestructiveActionColors
import com.rosan.installer.ui.page.coui.widgets.rememberCouiDialogMaterial
import com.rosan.installer.ui.page.main.settings.preferred.installer.InstallerSettingsAction
import com.rosan.installer.ui.page.main.settings.preferred.installer.InstallerSettingsViewModel
import org.koin.androidx.compose.koinViewModel

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun CouiInstallerGlobalSettingsPage(useBlur: Boolean, viewModel: InstallerSettingsViewModel = koinViewModel()) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scrollBehavior = CouiTopAppBarDefaults.exitUntilCollapsedScrollBehavior {
        listState.canScrollForward || listState.canScrollBackward
    }
    val density = LocalDensity.current

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CouiTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(R.string.installer_settings)) },
                navigationIcon = {
                    CouiBackButton(onClick = { navigator.pop() })
                },
                scrollBehavior = scrollBehavior,
                useCouiBlur = useBlur,
            )
        },
    ) { insets ->
        val contentPadding = remember(scrollBehavior.state, insets, density) {
            scrollBehavior.state.contentPadding(insets, density)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(insets),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + 4.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
            ),
        ) {
            item {
                Group(title = stringResource(R.string.installer_settings_global_installer)) {
                    CouiNavigationItem(
                        title = stringResource(R.string.dialog_settings),
                        description = stringResource(R.string.dialog_settings_desc),
                        onClick = { navigator.push(Route.DialogSettings) },
                    )
                    Divider()
                    CouiNavigationItem(
                        title = stringResource(R.string.notification_settings),
                        description = stringResource(R.string.notification_settings_desc),
                        onClick = { navigator.push(Route.NotificationSettings) },
                    )
                    Divider()
                    CouiNavigationItem(
                        title = stringResource(R.string.authorizer_customization),
                        description = stringResource(R.string.authorizer_customization_desc),
                        onClick = { navigator.push(Route.AuthorizerCust) },
                    )

                    if (BiometricManager
                            .from(context)
                            .canAuthenticate(BIOMETRIC_WEAK or BIOMETRIC_STRONG or DEVICE_CREDENTIAL) ==
                        BiometricManager.BIOMETRIC_SUCCESS
                    ) {
                        val biometricModes = remember {
                            listOf(
                                BiometricAuthMode.Disable,
                                BiometricAuthMode.Enable,
                                BiometricAuthMode.FollowConfig,
                            )
                        }
                        val labels = remember(biometricModes) {
                            biometricModes.map { mode ->
                                when (mode) {
                                    BiometricAuthMode.Disable -> context.getString(R.string.installer_biometric_auth_mode_disable)
                                    BiometricAuthMode.Enable -> context.getString(R.string.installer_biometric_auth_mode_enable)
                                    BiometricAuthMode.FollowConfig -> context.getString(R.string.installer_biometric_auth_mode_follow_config)
                                }
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

                        Divider()
                        CouiSpinnerItem(
                            title = stringResource(R.string.installer_settings_require_biometric_auth),
                            summary = dynamicSummary,
                            items = labels,
                            selectedIndex = selectedIndex,
                            onSelectedIndexChange = { index ->
                                viewModel.dispatch(InstallerSettingsAction.ChangeBiometricAuth(biometricModes[index]))
                            },
                        )
                    }
                }
            }

            item {
                Group(title = stringResource(R.string.installer_settings_signature_check)) {
                    CouiSwitchItem(
                        title = stringResource(R.string.config_check_app_signature),
                        description = stringResource(R.string.config_check_app_signature_desc),
                        checked = uiState.checkAppSignature,
                        onCheckedChange = { viewModel.dispatch(InstallerSettingsAction.ChangeCheckAppSignature(it)) },
                    )
                    AnimatedVisibility(
                        visible = uiState.checkAppSignature,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Column {
                            Divider()
                            CouiSwitchItem(
                                title = stringResource(R.string.config_check_source_stamp),
                                description = stringResource(R.string.config_check_source_stamp_desc),
                                checked = uiState.checkSourceStamp,
                                onCheckedChange = { viewModel.dispatch(InstallerSettingsAction.ChangeCheckSourceStamp(it)) },
                            )
                            Divider()
                            CouiSwitchItem(
                                title = stringResource(R.string.config_show_signature_info_on_match),
                                description = stringResource(R.string.config_show_signature_info_on_match_desc),
                                checked = uiState.showSignatureInfoOnMatch,
                                onCheckedChange = {
                                    viewModel.dispatch(InstallerSettingsAction.ChangeShowSignatureInfoOnMatch(it))
                                },
                            )
                            Divider()
                            CouiSwitchItem(
                                title = stringResource(R.string.config_show_signature_details),
                                description = stringResource(R.string.config_show_signature_details_desc),
                                checked = uiState.showSignatureDetails,
                                onCheckedChange = {
                                    viewModel.dispatch(InstallerSettingsAction.ChangeShowSignatureDetails(it))
                                },
                            )
                        }
                    }
                }
            }

            item {
                Group(title = stringResource(R.string.installer_settings_xposed_detection)) {
                    CouiSwitchItem(
                        title = stringResource(R.string.config_detect_xposed_module),
                        description = stringResource(R.string.config_detect_xposed_module_desc),
                        checked = uiState.detectXposedModule,
                        onCheckedChange = { viewModel.dispatch(InstallerSettingsAction.ChangeDetectXposedModule(it)) },
                    )
                    AnimatedVisibility(
                        visible = uiState.detectXposedModule,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Column {
                            Divider()
                            CouiSwitchItem(
                                title = stringResource(R.string.config_quick_open_lsposed),
                                description = stringResource(R.string.config_quick_open_lsposed_desc),
                                checked = uiState.quickOpenLSPosed,
                                onCheckedChange = { viewModel.dispatch(InstallerSettingsAction.ChangeQuickOpenLSPosed(it)) },
                            )
                        }
                    }
                }
            }

            if (DeviceConfig.currentManufacturer == Manufacturer.OPPO ||
                DeviceConfig.currentManufacturer == Manufacturer.ONEPLUS
            ) {
                item {
                    Group(title = stringResource(R.string.installer_oppo_related)) {
                        CouiSwitchItem(
                            title = stringResource(id = R.string.installer_show_oem_special),
                            description = stringResource(id = R.string.installer_show_oem_special_desc),
                            checked = uiState.showOPPOSpecial,
                            onCheckedChange = { viewModel.dispatch(InstallerSettingsAction.ChangeShowOPPOSpecial(it)) },
                        )
                    }
                }
            }

            item {
                Group(title = stringResource(R.string.config_managed_installer_packages_title)) {
                    CouiManagedPackagesWidget(
                        noContentTitle = stringResource(R.string.config_no_preset_install_sources),
                        packages = uiState.managedInstallerPackages,
                        onAddPackage = { viewModel.dispatch(InstallerSettingsAction.AddManagedInstallerPackage(it)) },
                        onRemovePackage = { viewModel.dispatch(InstallerSettingsAction.RemoveManagedInstallerPackage(it)) },
                    )
                }
            }

            item {
                Group(title = stringResource(R.string.config_managed_blacklist_by_package_name_title)) {
                    CouiManagedPackagesWidget(
                        noContentTitle = stringResource(R.string.config_no_managed_blacklist),
                        packages = uiState.managedBlacklistPackages,
                        onAddPackage = { viewModel.dispatch(InstallerSettingsAction.AddManagedBlacklistPackage(it)) },
                        onRemovePackage = { viewModel.dispatch(InstallerSettingsAction.RemoveManagedBlacklistPackage(it)) },
                    )
                }
            }

            item {
                Group(title = stringResource(R.string.config_managed_blacklist_by_shared_user_id_title)) {
                    CouiManagedUidsWidget(
                        noContentTitle = stringResource(R.string.config_no_managed_shared_user_id_blacklist),
                        uids = uiState.managedSharedUserIdBlacklist,
                        onAddUid = { viewModel.dispatch(InstallerSettingsAction.AddManagedSharedUserIdBlacklist(it)) },
                        onRemoveUid = { viewModel.dispatch(InstallerSettingsAction.RemoveManagedSharedUserIdBlacklist(it)) },
                    )
                    AnimatedVisibility(
                        visible = uiState.managedSharedUserIdBlacklist.isNotEmpty(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Column {
                            Divider()
                            CouiManagedPackagesWidget(
                                noContentTitle = stringResource(R.string.config_no_managed_shared_user_id_exempted_packages),
                                noContentDescription = stringResource(R.string.config_shared_uid_prior_to_pkgname_desc),
                                packages = uiState.managedSharedUserIdExemptedPackages,
                                infoText = stringResource(R.string.config_no_managed_shared_user_id_exempted_packages),
                                isInfoVisible = uiState.managedSharedUserIdExemptedPackages.isNotEmpty(),
                                onAddPackage = {
                                    viewModel.dispatch(InstallerSettingsAction.AddManagedSharedUserIdExemptedPackages(it))
                                },
                                onRemovePackage = {
                                    viewModel.dispatch(InstallerSettingsAction.RemoveManagedSharedUserIdExemptedPackages(it))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CouiManagedPackagesWidget(
    noContentTitle: String,
    noContentDescription: String = stringResource(R.string.config_add_one_to_get_started),
    packages: List<NamedPackage>,
    infoText: String? = null,
    isInfoVisible: Boolean = false,
    onAddPackage: (NamedPackage) -> Unit,
    onRemovePackage: (NamedPackage) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<NamedPackage?>(null) }
    var lastDeleteTarget by remember { mutableStateOf<NamedPackage?>(null) }
    if (deleteTarget != null) lastDeleteTarget = deleteTarget

    Column(modifier = Modifier.fillMaxWidth()) {
        if (packages.isEmpty()) {
            CouiBasicItem(
                title = noContentTitle,
                summary = noContentDescription,
            )
        } else {
            packages.forEachIndexed { index, item ->
                if (index > 0) Divider()
                CouiBasicItem(
                    title = item.name,
                    summary = item.packageName,
                    trailing = {
                        Box(
                            modifier = Modifier
                                .clip(CouiCapsuleShape)
                                .background(CouiTheme.colorScheme.accent.copy(alpha = 0.14f))
                                .clickable { deleteTarget = item }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.delete),
                                color = CouiTheme.colorScheme.accentOnSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            if (isInfoVisible && !infoText.isNullOrBlank()) {
                CouiBadge(text = infoText)
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(CouiCapsuleShape)
                    .background(CouiTheme.colorScheme.accent.copy(alpha = 0.14f))
                    .clickable { showAddDialog = true }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = CouiIcons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = CouiTheme.colorScheme.accentOnSurface,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.add),
                    color = CouiTheme.colorScheme.accentOnSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }

    CouiAddPackageDialog(
        visible = showAddDialog,
        onDismiss = { showAddDialog = false },
        onConfirm = { newItem ->
            onAddPackage(newItem)
            showAddDialog = false
        },
    )

    val backdrop = rememberCouiWindowBackdrop()
    val material = rememberCouiDialogMaterial()
    Dialog(
        visible = deleteTarget != null,
        onDismissRequest = { deleteTarget = null },
        title = stringResource(R.string.config_confirm_deletion),
        message = lastDeleteTarget?.let { stringResource(R.string.config_confirm_deletion_desc, it.name) },
        confirmAction = CouiDialogAction(
            text = stringResource(R.string.delete),
            onClick = {
                deleteTarget?.let(onRemovePackage)
                deleteTarget = null
            },
            style = CouiDialogActionStyle.Primary,
            colors = rememberCouiDestructiveActionColors(),
        ),
        dismissAction = CouiDialogAction(
            text = stringResource(R.string.cancel),
            onClick = { deleteTarget = null },
        ),
        material = material,
        backdrop = backdrop,
    )
}

@Composable
private fun CouiAddPackageDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (NamedPackage) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    val isConfirmEnabled = name.isNotBlank() && packageName.isNotBlank()
    val backdrop = rememberCouiWindowBackdrop()
    val material = rememberCouiDialogMaterial()

    LaunchedEffect(visible) {
        if (visible) {
            name = ""
            packageName = ""
        }
    }

    Dialog(
        visible = visible,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.config_add_new_package),
        confirmAction = CouiDialogAction(
            text = stringResource(R.string.confirm),
            onClick = { onConfirm(NamedPackage(name.trim(), packageName.trim())) },
            enabled = isConfirmEnabled,
            style = CouiDialogActionStyle.Primary,
        ),
        dismissAction = CouiDialogAction(
            text = stringResource(R.string.cancel),
            onClick = onDismiss,
        ),
        material = material,
        backdrop = backdrop,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CouiDialogDefaults.contentPadding)
                .padding(bottom = CouiDialogDefaults.messageBottom),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CouiDialogTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = stringResource(R.string.config_name),
            )
            CouiDialogTextField(
                value = packageName,
                onValueChange = { packageName = it },
                placeholder = stringResource(R.string.config_package_name),
            )
        }
    }
}

@Composable
private fun CouiManagedUidsWidget(
    noContentTitle: String,
    uids: List<SharedUid>,
    onAddUid: (SharedUid) -> Unit,
    onRemoveUid: (SharedUid) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<SharedUid?>(null) }
    var lastDeleteTarget by remember { mutableStateOf<SharedUid?>(null) }
    if (deleteTarget != null) lastDeleteTarget = deleteTarget

    Column(modifier = Modifier.fillMaxWidth()) {
        if (uids.isEmpty()) {
            CouiBasicItem(
                title = noContentTitle,
                summary = stringResource(R.string.config_add_one_to_get_started),
            )
        } else {
            uids.forEachIndexed { index, item ->
                if (index > 0) Divider()
                CouiBasicItem(
                    title = item.uidName,
                    summary = "UID: ${item.uidValue}",
                    trailing = {
                        Box(
                            modifier = Modifier
                                .clip(CouiCapsuleShape)
                                .background(CouiTheme.colorScheme.accent.copy(alpha = 0.14f))
                                .clickable { deleteTarget = item }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.delete),
                                color = CouiTheme.colorScheme.accentOnSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Row(
                modifier = Modifier
                    .clip(CouiCapsuleShape)
                    .background(CouiTheme.colorScheme.accent.copy(alpha = 0.14f))
                    .clickable { showAddDialog = true }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = CouiIcons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = CouiTheme.colorScheme.accentOnSurface,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.add),
                    color = CouiTheme.colorScheme.accentOnSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }

    CouiAddUidDialog(
        visible = showAddDialog,
        onDismiss = { showAddDialog = false },
        onConfirm = { newUid ->
            onAddUid(newUid)
            showAddDialog = false
        },
    )

    val backdrop = rememberCouiWindowBackdrop()
    val material = rememberCouiDialogMaterial()
    Dialog(
        visible = deleteTarget != null,
        onDismissRequest = { deleteTarget = null },
        title = stringResource(R.string.config_confirm_deletion),
        message = lastDeleteTarget?.let { stringResource(R.string.config_confirm_deletion_desc, it.uidName) },
        confirmAction = CouiDialogAction(
            text = stringResource(R.string.delete),
            onClick = {
                deleteTarget?.let(onRemoveUid)
                deleteTarget = null
            },
            style = CouiDialogActionStyle.Primary,
            colors = rememberCouiDestructiveActionColors(),
        ),
        dismissAction = CouiDialogAction(
            text = stringResource(R.string.cancel),
            onClick = { deleteTarget = null },
        ),
        material = material,
        backdrop = backdrop,
    )
}

@Composable
private fun CouiAddUidDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (SharedUid) -> Unit,
) {
    var uidName by remember { mutableStateOf("") }
    var uidValueString by remember { mutableStateOf("") }
    val isConfirmEnabled = uidName.isNotBlank() && uidValueString.toIntOrNull() != null
    val backdrop = rememberCouiWindowBackdrop()
    val material = rememberCouiDialogMaterial()

    LaunchedEffect(visible) {
        if (visible) {
            uidName = ""
            uidValueString = ""
        }
    }

    Dialog(
        visible = visible,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.config_add_new_shared_uid),
        confirmAction = CouiDialogAction(
            text = stringResource(R.string.confirm),
            onClick = {
                val parsedUid = uidValueString.toIntOrNull() ?: return@CouiDialogAction
                onConfirm(SharedUid(uidName.trim(), parsedUid))
            },
            enabled = isConfirmEnabled,
            style = CouiDialogActionStyle.Primary,
        ),
        dismissAction = CouiDialogAction(
            text = stringResource(R.string.cancel),
            onClick = onDismiss,
        ),
        material = material,
        backdrop = backdrop,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CouiDialogDefaults.contentPadding)
                .padding(bottom = CouiDialogDefaults.messageBottom),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CouiDialogTextField(
                value = uidName,
                onValueChange = { uidName = it },
                placeholder = stringResource(R.string.config_shared_uid_name),
            )
            CouiDialogTextField(
                value = uidValueString,
                onValueChange = { uidValueString = it },
                placeholder = stringResource(R.string.config_shared_uid_value),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
    }
}

@Composable
private fun CouiDialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val scheme = CouiTheme.colorScheme
    val dark = CouiTheme.isDark
    val shape = remember { CouiShape(14.dp, 3f) }
    val bgColor = scheme.onSurface.copy(alpha = if (dark) 0.10f else 0.05f)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        textStyle = TextStyle(color = scheme.onSurface, fontSize = 15.sp),
        cursorBrush = SolidColor(scheme.accent),
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, shape)
            .border(1.dp, scheme.outlineVariant, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = scheme.onSurfaceVariant,
                    fontSize = 15.sp,
                )
            }
            inner()
        },
    )
}
