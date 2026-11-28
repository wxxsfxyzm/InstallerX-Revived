// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.settings.preferred.lab

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itos.coui.checkbox.Checkbox
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.Dialog
import com.itos.coui.icons.CouiIcons
import com.itos.coui.layout.Divider
import com.itos.coui.layout.Group
import com.itos.coui.layout.Scaffold
import com.itos.coui.menu.rememberCouiWindowBackdrop
import com.itos.coui.sheet.BottomSheet
import com.itos.coui.sheet.CouiSheetButton
import com.itos.coui.text.CouiTextKind
import com.itos.coui.theme.CouiTheme
import com.itos.coui.topappbar.CouiTopAppBarDefaults
import com.itos.coui.topappbar.LargeTopAppBar
import com.rosan.installer.R
import com.rosan.installer.core.env.AppConfig
import com.rosan.installer.domain.device.model.ShizukuMode
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.preferences.RootMode
import com.rosan.installer.domain.settings.model.preferences.SmartAuthorizerCandidate
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.coui.widgets.CouiBackButton
import com.rosan.installer.ui.page.coui.widgets.CouiBasicItem
import com.rosan.installer.ui.page.coui.widgets.CouiDraggableList
import com.rosan.installer.ui.page.coui.widgets.CouiInstallerTipCard
import com.rosan.installer.ui.page.coui.widgets.CouiRootImplementationDialog
import com.rosan.installer.ui.page.coui.widgets.CouiSettingsTipCard
import com.rosan.installer.ui.page.coui.widgets.CouiSpinnerItem
import com.rosan.installer.ui.page.coui.widgets.CouiSwitchItem
import com.rosan.installer.ui.page.coui.widgets.rememberCouiDialogMaterial
import com.rosan.installer.ui.page.main.settings.preferred.lab.LabSettingsAction
import com.rosan.installer.ui.page.main.settings.preferred.lab.LabSettingsViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.itos.coui.text.Text as CouiText

@Composable
fun CouiLabPage(useBlur: Boolean, viewModel: LabSettingsViewModel = koinViewModel()) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val capabilityProvider = koinInject<DeviceCapabilityProvider>()
    val isSystemApp = capabilityProvider.isSystemApp
    val rootMode by capabilityProvider.rootModeFlow.collectAsStateWithLifecycle()
    val shizukuMode by capabilityProvider.shizukuModeFlow.collectAsStateWithLifecycle()
    val shizukuAuthorized by capabilityProvider.shizukuAuthorizedFlow.collectAsStateWithLifecycle()
    val dhizukuAvailable by capabilityProvider.dhizukuAvailableFlow.collectAsStateWithLifecycle()
    val dhizukuAuthorized by capabilityProvider.dhizukuAuthorizedFlow.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val scrollBehavior = CouiTopAppBarDefaults.exitUntilCollapsedScrollBehavior {
        listState.canScrollForward || listState.canScrollBackward
    }
    val density = LocalDensity.current

    val showRootImplementationDialog = remember { mutableStateOf(false) }
    val showSmartAuthorizerSheet = remember { mutableStateOf(false) }
    var showNonAdbConfirmation by rememberSaveable { mutableStateOf(false) }

    val backdrop = rememberCouiWindowBackdrop()
    val dialogMaterial = rememberCouiDialogMaterial()

    Dialog(
        visible = showNonAdbConfirmation,
        onDismissRequest = { showNonAdbConfirmation = false },
        title = stringResource(R.string.lab_shizuku_non_adb_install),
        message = stringResource(R.string.lab_shizuku_non_adb_install_confirmation, context.packageName),
        confirmAction = CouiDialogAction(
            text = stringResource(R.string.confirm),
            onClick = {
                showNonAdbConfirmation = false
                viewModel.dispatch(LabSettingsAction.LabChangeShizukuNonAdbInstall(true))
            },
            style = CouiDialogActionStyle.Primary,
        ),
        dismissAction = CouiDialogAction(
            text = stringResource(R.string.cancel),
            onClick = { showNonAdbConfirmation = false },
        ),
        material = dialogMaterial,
        backdrop = backdrop,
    )

    CouiRootImplementationDialog(
        showState = showRootImplementationDialog,
        onDismiss = { showRootImplementationDialog.value = false },
        onConfirm = { selectedImplementation ->
            showRootImplementationDialog.value = false
            viewModel.dispatch(LabSettingsAction.LabChangeRootImplementation(selectedImplementation))
            viewModel.dispatch(LabSettingsAction.LabChangeRootModuleFlash(true))
        },
    )

    val smartAuthorizerSummary = uiState.smartAuthorizerCandidates
        .filter { it.enabled }
        .joinToString("->") {
            couiSmartAuthorizerDisplayName(it.authorizer, isSystemApp, context::getString)
        }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CouiTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(R.string.lab)) },
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
            item { CouiSettingsTipCard(stringResource(R.string.lab_tip)) }

            item {
                Group(title = stringResource(R.string.config_authorizer)) {
                    CouiSwitchItem(
                        title = stringResource(R.string.config_try_multiple_authorizers_on_install),
                        description = stringResource(R.string.config_try_multiple_authorizers_on_install_desc),
                        checked = uiState.tryMultipleAuthorizersOnInstall,
                        onCheckedChange = {
                            viewModel.dispatch(LabSettingsAction.LabChangeTryMultipleAuthorizersOnInstall(it))
                        },
                    )
                    AnimatedVisibility(
                        visible = uiState.tryMultipleAuthorizersOnInstall,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        Column {
                            Divider()
                            CouiBasicItem(
                                title = stringResource(R.string.config_smart_authorizer_fallback_list),
                                summary = stringResource(
                                    R.string.config_smart_authorizer_fallback_list_desc,
                                    smartAuthorizerSummary,
                                ),
                                onClick = {
                                    capabilityProvider.refreshPrivilegeStatus()
                                    showSmartAuthorizerSheet.value = true
                                },
                            )
                        }
                    }
                }
            }

            item {
                Group(title = stringResource(R.string.config_authorizer_root)) {
                    CouiSwitchItem(
                        title = stringResource(R.string.lab_module_flashing),
                        description = stringResource(R.string.lab_module_flashing_desc),
                        checked = uiState.labRootEnableModuleFlash,
                        onCheckedChange = { isEnabling ->
                            if (isEnabling) {
                                showRootImplementationDialog.value = true
                            } else {
                                viewModel.dispatch(LabSettingsAction.LabChangeRootModuleFlash(false))
                            }
                        },
                    )
                    AnimatedVisibility(
                        visible = uiState.labRootEnableModuleFlash,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        val data = remember {
                            linkedMapOf(
                                RootMode.Magisk to "Magisk",
                                RootMode.KernelSU to "KernelSU",
                                RootMode.APatch to "APatch",
                            )
                        }
                        val keys = data.keys.toList()
                        val selectedIndex = keys.indexOf(uiState.labRootMode).coerceAtLeast(0)

                        Column {
                            Divider()
                            CouiSpinnerItem(
                                title = stringResource(R.string.lab_module_select_root_impl),
                                items = data.values.toList(),
                                selectedIndex = selectedIndex,
                                onSelectedIndexChange = { newIndex ->
                                    keys.getOrNull(newIndex)?.let { impl ->
                                        viewModel.dispatch(LabSettingsAction.LabChangeRootImplementation(impl))
                                    }
                                },
                            )
                            Divider()
                            CouiSwitchItem(
                                title = stringResource(R.string.lab_module_flashing_show_art),
                                description = stringResource(R.string.lab_module_flashing_show_art_desc),
                                checked = uiState.labRootShowModuleArt,
                                onCheckedChange = {
                                    viewModel.dispatch(LabSettingsAction.LabChangeRootShowModuleArt(it))
                                },
                            )
                        }
                    }
                }
            }

            item {
                Group(title = stringResource(R.string.lab_compatibility)) {
                    CouiSwitchItem(
                        title = stringResource(R.string.lab_shizuku_non_adb_install),
                        description = stringResource(R.string.lab_shizuku_non_adb_install_desc),
                        checked = uiState.labShizukuNonAdbInstall,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                showNonAdbConfirmation = true
                            } else {
                                viewModel.dispatch(LabSettingsAction.LabChangeShizukuNonAdbInstall(false))
                            }
                        },
                    )
                    Divider()
                    CouiSwitchItem(
                        title = stringResource(R.string.lab_enable_abi_override),
                        description = stringResource(R.string.lab_enable_abi_override_desc),
                        checked = uiState.labEnableAbiOverride,
                        onCheckedChange = {
                            viewModel.dispatch(LabSettingsAction.LabChangeEnableAbiOverride(it))
                        },
                    )
                    AnimatedVisibility(
                        visible = uiState.labEnableAbiOverride,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        Column {
                            Divider()
                            CouiSwitchItem(
                                title = stringResource(R.string.lab_try_different_architecture),
                                description = stringResource(R.string.lab_try_different_architecture_desc),
                                checked = uiState.labTryDifferentArchitecture,
                                onCheckedChange = {
                                    viewModel.dispatch(LabSettingsAction.LabChangeTryDifferentArchitecture(it))
                                },
                            )
                        }
                    }
                }
            }

            if (AppConfig.isRespectPlatformInstallPolicyAvailable) {
                item {
                    Group(title = stringResource(R.string.lab_unstable_features)) {
                        CouiSwitchItem(
                            title = stringResource(R.string.lab_respect_platform_install_policy),
                            description = stringResource(R.string.lab_respect_platform_install_policy_desc),
                            checked = uiState.labRespectPlatformInstallPolicy,
                            onCheckedChange = {
                                viewModel.dispatch(LabSettingsAction.LabChangeRespectPlatformInstallPolicy(it))
                            },
                        )
                    }
                }
            }
        }
    }

    BottomSheet(
        visible = showSmartAuthorizerSheet,
        onDismissRequest = { showSmartAuthorizerSheet.value = false },
        leadingButton = CouiSheetButton(
            onClick = { showSmartAuthorizerSheet.value = false },
            icon = {
                Icon(
                    imageVector = CouiIcons.Filled.Close,
                    contentDescription = stringResource(R.string.close),
                )
            },
        ),
        titleContent = {
            CouiText(
                text = stringResource(R.string.config_try_multiple_authorizers_on_install),
                kind = CouiTextKind.SheetTitle,
            )
        },
    ) {
        CouiSmartAuthorizerBottomSheetContent(
            candidates = uiState.smartAuthorizerCandidates,
            rootMode = rootMode,
            shizukuMode = shizukuMode,
            shizukuAuthorized = shizukuAuthorized,
            dhizukuAvailable = dhizukuAvailable,
            dhizukuAuthorized = dhizukuAuthorized,
            isSystemApp = isSystemApp,
            onCandidatesChange = {
                viewModel.dispatch(LabSettingsAction.LabChangeSmartAuthorizerCandidates(it))
            },
        )
    }
}

@Composable
private fun CouiSmartAuthorizerBottomSheetContent(
    candidates: List<SmartAuthorizerCandidate>,
    rootMode: RootMode,
    shizukuMode: ShizukuMode,
    shizukuAuthorized: Boolean,
    dhizukuAvailable: Boolean,
    dhizukuAuthorized: Boolean,
    isSystemApp: Boolean,
    onCandidatesChange: (List<SmartAuthorizerCandidate>) -> Unit,
) {
    val context = LocalContext.current
    var sheetCandidates by remember { mutableStateOf(candidates) }

    LaunchedEffect(candidates) {
        if (sheetCandidates != candidates) {
            sheetCandidates = candidates
        }
    }

    fun updateCandidates(next: List<SmartAuthorizerCandidate>) {
        sheetCandidates = next
        onCandidatesChange(next)
    }

    fun toggleCandidate(candidate: SmartAuthorizerCandidate, enabled: Boolean) {
        if (!enabled && candidate.enabled && sheetCandidates.count { it.enabled } <= 1) {
            Toast.makeText(
                context,
                context.getString(R.string.config_smart_authorizer_must_choose_one),
                Toast.LENGTH_SHORT,
            ).show()
            return
        }
        updateCandidates(sheetCandidates.couiToggle(candidate.authorizer, enabled))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CouiInstallerTipCard(stringResource(R.string.config_smart_authorizer_fallback_list_tip))
        CouiDraggableList(
            items = sheetCandidates,
            itemKey = { it.authorizer.value },
            itemName = { couiSmartAuthorizerDisplayName(it.authorizer, isSystemApp, context::getString) },
            itemDescription = {
                couiSmartAuthorizerAvailabilityDescription(
                    authorizer = it.authorizer,
                    rootMode = rootMode,
                    shizukuMode = shizukuMode,
                    shizukuAuthorized = shizukuAuthorized,
                    dhizukuAvailable = dhizukuAvailable,
                    dhizukuAuthorized = dhizukuAuthorized,
                    isSystemApp = isSystemApp,
                    getString = context::getString,
                )
            },
            onMove = { from, to ->
                updateCandidates(sheetCandidates.couiMove(from, to))
            },
            onItemClick = { candidate ->
                toggleCandidate(candidate, !candidate.enabled)
            },
            trailingContent = { candidate ->
                Checkbox(checked = candidate.enabled)
            },
        )
    }
}

private fun couiSmartAuthorizerDisplayName(
    authorizer: Authorizer,
    isSystemApp: Boolean,
    getString: (Int) -> String,
): String = if (authorizer == Authorizer.None) {
    getString(
        if (isSystemApp) R.string.working_status_system_installer else R.string.config_authorizer_none,
    )
} else {
    getString(authorizer.displayNameRes)
}

private fun couiSmartAuthorizerAvailabilityDescription(
    authorizer: Authorizer,
    rootMode: RootMode,
    shizukuMode: ShizukuMode,
    shizukuAuthorized: Boolean,
    dhizukuAvailable: Boolean,
    dhizukuAuthorized: Boolean,
    isSystemApp: Boolean,
    getString: (Int) -> String,
): String = when (authorizer) {
    Authorizer.Root -> if (rootMode != RootMode.None) {
        "${getString(R.string.available)} (${rootMode.name})"
    } else {
        getString(R.string.unavailable)
    }

    Authorizer.Shizuku -> when {
        shizukuAuthorized -> "${getString(R.string.activate)} (${shizukuMode.desc})"
        shizukuMode != ShizukuMode.NONE -> getString(R.string.shizuku_not_authorized)
        else -> getString(R.string.shizuku_not_available)
    }

    Authorizer.Dhizuku -> when {
        dhizukuAuthorized -> getString(R.string.activate)
        dhizukuAvailable -> getString(R.string.dhizuku_not_authorized)
        else -> getString(R.string.dhizuku_not_available)
    }

    Authorizer.None -> getString(
        if (isSystemApp) {
            R.string.working_status_system_installer_desc
        } else {
            R.string.working_status_none_authorizer_desc
        },
    )

    else -> authorizer.value
}

private fun List<SmartAuthorizerCandidate>.couiMove(from: Int, to: Int): List<SmartAuthorizerCandidate> {
    if (from !in indices || to !in indices || from == to) return this
    return toMutableList().apply {
        val item = removeAt(from)
        add(to, item)
    }
}

private fun List<SmartAuthorizerCandidate>.couiToggle(
    authorizer: Authorizer,
    enabled: Boolean,
): List<SmartAuthorizerCandidate> = map { candidate ->
    if (candidate.authorizer == authorizer) {
        candidate.copy(enabled = enabled)
    } else {
        candidate
    }
}
