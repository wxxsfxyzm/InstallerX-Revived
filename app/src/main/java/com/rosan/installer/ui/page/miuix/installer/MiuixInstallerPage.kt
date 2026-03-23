// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.installer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.exception.ModuleInstallCmdInitException
import com.rosan.installer.domain.engine.exception.ModuleInstallException
import com.rosan.installer.domain.engine.exception.ModuleInstallFailedIncompatibleAuthorizerException
import com.rosan.installer.domain.engine.model.DataType
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.ui.icons.AppMiuixIcons
import com.rosan.installer.ui.page.main.installer.InstallerStage
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.widget.util.ToastEventCollector
import com.rosan.installer.ui.page.miuix.installer.sheetcontent.InstallChoiceContent
import com.rosan.installer.ui.page.miuix.installer.sheetcontent.InstallCompletedContent
import com.rosan.installer.ui.page.miuix.installer.sheetcontent.InstallConfirmContent
import com.rosan.installer.ui.page.miuix.installer.sheetcontent.InstallExtendedMenuContent
import com.rosan.installer.ui.page.miuix.installer.sheetcontent.InstallFailedContent
import com.rosan.installer.ui.page.miuix.installer.sheetcontent.InstallModuleContent
import com.rosan.installer.ui.page.miuix.installer.sheetcontent.InstallPrepareContent
import com.rosan.installer.ui.page.miuix.installer.sheetcontent.InstallPreparePermissionContent
import com.rosan.installer.ui.page.miuix.installer.sheetcontent.InstallPreparingContent
import com.rosan.installer.ui.page.miuix.installer.sheetcontent.InstallSuccessContent
import com.rosan.installer.ui.page.miuix.installer.sheetcontent.InstallingContent
import com.rosan.installer.ui.page.miuix.installer.sheetcontent.LoadingContent
import com.rosan.installer.ui.page.miuix.installer.sheetcontent.NonInstallFailedContent
import com.rosan.installer.ui.page.miuix.installer.sheetcontent.PrepareSettingsContent
import com.rosan.installer.ui.page.miuix.installer.sheetcontent.UninstallFailedContent
import com.rosan.installer.ui.page.miuix.installer.sheetcontent.UninstallPrepareContent
import com.rosan.installer.ui.page.miuix.installer.sheetcontent.UninstallSuccessContent
import com.rosan.installer.ui.page.miuix.installer.sheetcontent.UninstallingContent
import com.rosan.installer.ui.page.miuix.installer.sheetcontent.rememberAppInfoState
import com.rosan.installer.ui.page.miuix.widgets.DropdownItem
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.theme.InstallerMiuixTheme
import com.rosan.installer.ui.theme.InstallerTheme
import com.rosan.installer.ui.theme.LocalInstallerColorScheme
import com.rosan.installer.ui.theme.material.dynamicColorScheme
import com.rosan.installer.ui.theme.miuixSheetColorDark
import com.rosan.installer.ui.theme.miuixSheetColorLight
import com.rosan.installer.ui.util.WindowBlurEffect
import com.rosan.installer.ui.util.getSupportTitle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.extra.WindowBottomSheet
import top.yukonga.miuix.kmp.extra.WindowListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor

private const val SHEET_ANIMATION_DURATION = 450L

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
fun MiuixInstallerPage(
    session: InstallerSessionRepository,
    viewModel: InstallerViewModel = koinViewModel { parametersOf(session) }
) {
    val context = LocalContext.current
    val capabilityProvider = koinInject<DeviceCapabilityProvider>()
    val scope = rememberCoroutineScope()
    val showBottomSheet = remember { mutableStateOf(true) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stage = uiState.stage
    val settings = uiState.viewSettings
    val showSettings = uiState.showMiuixSheetRightActionSettings
    val showPermissions = uiState.showMiuixPermissionList
    val temporarySeedColor = uiState.seedColor
    val currentPackageName = uiState.currentPackageName
    val displayIcons = uiState.displayIcons

    val globalSeedColor = InstallerTheme.seedColor
    val themeMode = InstallerTheme.themeMode
    val useMiuixMonet = InstallerTheme.useMiuixMonet
    val useDynamicColor = InstallerTheme.useDynamicColor
    val isDark = InstallerTheme.isDark
    val paletteStyle = InstallerTheme.paletteStyle
    val colorSpec = InstallerTheme.colorSpec
    val globalColorScheme = InstallerTheme.colorScheme

    val activeSeedColor = temporarySeedColor ?: globalSeedColor
    val activeMd3ColorScheme = remember(activeSeedColor, globalColorScheme, isDark, paletteStyle) {
        temporarySeedColor?.let {
            dynamicColorScheme(keyColor = it, isDark = isDark, style = paletteStyle, colorSpec = colorSpec)
        } ?: globalColorScheme
    }

    val sourceType = session.analysisResults.firstOrNull()?.appEntities?.firstOrNull()?.app?.sourceType ?: DataType.NONE
    val packageName = currentPackageName ?: session.analysisResults.firstOrNull()?.packageName ?: ""
    val appInfoState = rememberAppInfoState(
        session = session,
        currentPackageName = currentPackageName,
        displayIcons = displayIcons
    )

    LaunchedEffect(session.id) {
        viewModel.dispatch(InstallerViewAction.CollectSession(session))
    }

    ToastEventCollector(viewModel)

    val sheetTitle = when (stage) {
        is InstallerStage.Preparing -> stringResource(R.string.installer_preparing)
        is InstallerStage.InstallChoice -> sourceType.getSupportTitle()
        is InstallerStage.InstallExtendedMenu -> stringResource(R.string.config_label_install_options)
        is InstallerStage.InstallPrepare -> when {
            showSettings -> stringResource(R.string.installer_settings)
            showPermissions -> stringResource(R.string.permission_list)
            else -> stringResource(
                if (viewModel.isInstallingModule) R.string.installer_install_module
                else R.string.installer_install_app
            )
        }

        is InstallerStage.InstallingModule -> if (stage.isFinished) {
            stringResource(R.string.installer_install_complete)
        } else {
            stringResource(R.string.installer_installing_module)
        }

        is InstallerStage.InstallConfirm -> stringResource(R.string.installer_install_confirm)
        is InstallerStage.Installing -> stringResource(R.string.installer_installing)
        is InstallerStage.InstallCompleted -> stringResource(R.string.installer_install_success)
        is InstallerStage.InstallSuccess -> stringResource(R.string.installer_install_success)
        is InstallerStage.InstallFailed -> stringResource(R.string.installer_install_failed)
        is InstallerStage.UninstallReady -> stringResource(R.string.installer_uninstall_app)
        is InstallerStage.Uninstalling -> stringResource(R.string.installer_uninstalling)
        is InstallerStage.UninstallSuccess -> stringResource(R.string.uninstall_success_message)
        is InstallerStage.UninstallFailed -> stringResource(R.string.uninstall_failed_message)
        is InstallerStage.AnalyseFailed -> stringResource(R.string.installer_analyse_failed)
        is InstallerStage.ResolveFailed -> stringResource(R.string.installer_resolve_failed)
        is InstallerStage.Resolving -> stringResource(R.string.installer_resolving)
        is InstallerStage.Analysing -> stringResource(R.string.installer_analysing)
        else -> stringResource(R.string.loading)
    }

    val closeSheet: () -> Unit = {
        showBottomSheet.value = false
        scope.launch {
            delay(SHEET_ANIMATION_DURATION)
            viewModel.dispatch(InstallerViewAction.Close)
        }
    }

    val isMiInstallerSupported = capabilityProvider.hasMiPackageInstaller

    CompositionLocalProvider(
        LocalInstallerColorScheme provides activeMd3ColorScheme
    ) {
        InstallerMiuixTheme(
            seedColor = activeSeedColor,
            paletteStyle = paletteStyle,
            colorSpec = colorSpec,
            darkTheme = isDark,
            themeMode = themeMode,
            useMiuixMonet = useMiuixMonet,
            useDynamicColor = useDynamicColor && temporarySeedColor == null,
            compatStatusBarColor = false
        ) {
            WindowBottomSheet(
                show = showBottomSheet.value, // Always true as long as this page is composed.
                backgroundColor = if (isDynamicColor) MiuixTheme.colorScheme.surfaceContainerHigh else if (isDark)
                    miuixSheetColorDark else miuixSheetColorLight,
                startAction = {
                    when (stage) {
                        is InstallerStage.InstallChoice -> {
                            // Check the new flag from uiState
                            if (uiState.navigatedFromPrepareToChoice) {
                                // Came from Prepare (re-selecting splits) -> Show Back icon, go back to Prepare
                                MiuixBackButton(
                                    icon = AppMiuixIcons.Back,
                                    iconTint = MiuixTheme.colorScheme.onSurface,
                                    onClick = { viewModel.dispatch(InstallerViewAction.InstallPrepare) }
                                )
                            } else {
                                // Initial choice or other origin -> Show Cancel icon, force close
                                MiuixBackButton(
                                    icon = AppMiuixIcons.Close,
                                    iconTint = MiuixTheme.colorScheme.onSurface,
                                    onClick = closeSheet
                                )
                            }
                        }

                        is InstallerStage.InstallConfirm -> {
                            MiuixBackButton(
                                icon = AppMiuixIcons.Close,
                                iconTint = MiuixTheme.colorScheme.onSurface,
                                onClick = {
                                    viewModel.dispatch(InstallerViewAction.ApproveSession(stage.sessionId, false))
                                }
                            )
                        }

                        is InstallerStage.InstallCompleted,
                        is InstallerStage.InstallFailed,
                        is InstallerStage.InstallSuccess,
                        is InstallerStage.UninstallReady,
                        is InstallerStage.UninstallSuccess,
                        is InstallerStage.UninstallFailed,
                        is InstallerStage.AnalyseFailed,
                        is InstallerStage.ResolveFailed -> {
                            MiuixBackButton(
                                icon = AppMiuixIcons.Close,
                                iconTint = MiuixTheme.colorScheme.onSurface,
                                onClick = {
                                    showBottomSheet.value = !showBottomSheet.value
                                    scope.launch {
                                        delay(SHEET_ANIMATION_DURATION)
                                        if (uiState.isDismissible)
                                            viewModel.dispatch(InstallerViewAction.Close)
                                    }
                                }
                            )
                        }

                        is InstallerStage.InstallPrepare -> {
                            MiuixBackButton(
                                icon = if (showSettings || showPermissions) AppMiuixIcons.Back else AppMiuixIcons.Close,
                                iconTint = MiuixTheme.colorScheme.onSurface,
                                onClick = {
                                    if (showSettings) {
                                        viewModel.dispatch(InstallerViewAction.HideMiuixSheetRightActionSettings)
                                    } else if (showPermissions) {
                                        viewModel.dispatch(InstallerViewAction.HideMiuixPermissionList)
                                    } else {
                                        showBottomSheet.value = !showBottomSheet.value
                                        scope.launch {
                                            delay(SHEET_ANIMATION_DURATION)
                                            viewModel.dispatch(InstallerViewAction.Close)
                                        }
                                    }
                                }
                            )
                        }

                        is InstallerStage.InstallExtendedMenu -> {
                            MiuixBackButton(
                                icon = AppMiuixIcons.Back,
                                iconTint = MiuixTheme.colorScheme.onSurface,
                                onClick = { viewModel.dispatch(InstallerViewAction.InstallPrepare) }
                            )
                        }

                        else -> {}
                    }
                },
                endAction = {
                    when (stage) {
                        is InstallerStage.InstallPrepare -> {
                            if (!showSettings && !showPermissions) {
                                IconButton(onClick = { viewModel.dispatch(InstallerViewAction.ShowMiuixSheetRightActionSettings) }) {
                                    Icon(
                                        imageVector = AppMiuixIcons.Settings,
                                        tint = MiuixTheme.colorScheme.onSurface,
                                        contentDescription = stringResource(R.string.installer_settings)
                                    )
                                }
                            } else {
                                Spacer(Modifier.size(48.dp))
                            }
                        }

                        is InstallerStage.InstallSuccess -> {
                            IconButton(
                                onClick = {
                                    if (packageName.isNotEmpty()) {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                            .setData(Uri.fromParts("package", packageName, null))
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    }
                                    viewModel.dispatch(InstallerViewAction.Close)
                                }) {
                                Icon(
                                    imageVector = AppMiuixIcons.Info,
                                    tint = MiuixTheme.colorScheme.onSurface,
                                    contentDescription = stringResource(R.string.installer_settings)
                                )
                            }
                        }

                        is InstallerStage.InstallingModule -> {
                            // Only show the reboot menu when the installation is finished
                            if (stage.isFinished) {
                                RebootListPopup(
                                    onReboot = { reason ->
                                        viewModel.dispatch(InstallerViewAction.Reboot(reason))
                                    }
                                )
                            }
                        }

                        else -> {}
                    }
                },
                title = sheetTitle, // DYNAMIC TITLE
                insideMargin = DpSize(16.dp, 16.dp),
                allowDismiss = uiState.isDismissible,
                onDismissRequest = {
                    if (uiState.isDismissible) {
                        // If it is dismissible, then proceed to close the sheet.
                        showBottomSheet.value = !showBottomSheet.value
                        scope.launch {
                            delay(SHEET_ANIMATION_DURATION) // Wait for sheet animation

                            val disableNotif = settings.disableNotificationOnDismiss
                            val isModule = stage is InstallerStage.InstallingModule
                            val isUninstall =
                                stage is InstallerStage.UninstallReady || stage is InstallerStage.UninstallResolveFailed ||
                                        stage is InstallerStage.UninstallSuccess || stage is InstallerStage.UninstallFailed

                            // 1. If it's a module install, OR uninstall OR the "disable notification" setting is on -> Close
                            // 2. Otherwise (including Preparing, Standard APK install) -> Background
                            val action = if (isModule || disableNotif || isUninstall) {
                                InstallerViewAction.Close
                            } else {
                                InstallerViewAction.Background
                            }

                            viewModel.dispatch(action)
                        }
                    }
                }

            ) {
                val radius = if (showBottomSheet.value) 30 else 0
                AnimatedContent(
                    targetState = radius,
                    label = "BlurTransition",
                    transitionSpec = {
                        fadeIn(animationSpec = tween(durationMillis = 350)) togetherWith
                                fadeOut(animationSpec = tween(durationMillis = 350))
                    }
                ) { targetState ->
                    WindowBlurEffect(useBlur = settings.useBlur, blurRadius = targetState)
                }

                // Using stage.javaClass instead of viewModel.state::class for stability
                AnimatedContent(
                    targetState = stage.javaClass,
                    label = "MiuixSheetContentAnimation",
                    transitionSpec = {
                        fadeIn(animationSpec = tween(durationMillis = 150)) togetherWith
                                fadeOut(animationSpec = tween(durationMillis = 150))
                    }
                ) { _ ->
                    when (stage) {
                        is InstallerStage.InstallConfirm -> {
                            InstallConfirmContent(
                                viewModel = viewModel,
                                onCancel = {
                                    showBottomSheet.value = false
                                    scope.launch {
                                        delay(SHEET_ANIMATION_DURATION)
                                        viewModel.dispatch(
                                            InstallerViewAction.ApproveSession(stage.sessionId, false)
                                        )
                                    }
                                },
                                onConfirm = {
                                    showBottomSheet.value = false
                                    scope.launch {
                                        delay(SHEET_ANIMATION_DURATION)
                                        viewModel.dispatch(
                                            InstallerViewAction.ApproveSession(stage.sessionId, true)
                                        )
                                    }
                                }
                            )
                        }

                        is InstallerStage.InstallChoice -> {
                            InstallChoiceContent(
                                session = session,
                                viewModel = viewModel,
                                onCancel = closeSheet
                            )
                        }

                        is InstallerStage.InstallExtendedMenu -> {
                            InstallExtendedMenuContent(
                                session = session,
                                viewModel = viewModel
                            )
                        }

                        is InstallerStage.Preparing -> {
                            InstallPreparingContent(
                                viewModel = viewModel,
                                onCancel = {
                                    showBottomSheet.value = false
                                    scope.launch {
                                        delay(SHEET_ANIMATION_DURATION)
                                        viewModel.dispatch(InstallerViewAction.Cancel)
                                    }
                                }
                            )
                        }

                        is InstallerStage.InstallPrepare -> {
                            val prepareSubState = when {
                                showSettings -> "settings"
                                showPermissions -> "permissions"
                                else -> "prepare"
                            }

                            AnimatedContent(
                                targetState = prepareSubState,
                                label = "PrepareContentVsSettingsVsPermissions",
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(durationMillis = 150)) togetherWith
                                            fadeOut(animationSpec = tween(durationMillis = 150))
                                }
                            ) { subState ->
                                when (subState) {
                                    "settings" -> {
                                        PrepareSettingsContent(
                                            session = session,
                                            viewModel = viewModel
                                        )
                                    }

                                    "permissions" -> {
                                        InstallPreparePermissionContent(
                                            session = session,
                                            viewModel = viewModel,
                                            onBack = { viewModel.dispatch(InstallerViewAction.HideMiuixPermissionList) }
                                        )
                                    }

                                    else -> { // "prepare"
                                        InstallPrepareContent(
                                            session = session,
                                            viewModel = viewModel,
                                            appInfo = appInfoState,
                                            onCancel = closeSheet,
                                            onInstall = {
                                                viewModel.dispatch(InstallerViewAction.Install(true))
                                                if (settings.autoSilentInstall && !viewModel.isInstallingModule) {
                                                    showBottomSheet.value = false
                                                    scope.launch {
                                                        delay(SHEET_ANIMATION_DURATION)
                                                        viewModel.dispatch(InstallerViewAction.Background)
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        is InstallerStage.Installing -> {
                            InstallingContent(
                                state = stage,
                                appInfo = appInfoState,
                                onButtonClick = {
                                    scope.launch {
                                        delay(SHEET_ANIMATION_DURATION)
                                        viewModel.dispatch(InstallerViewAction.Background)
                                    }
                                }
                            )
                        }

                        is InstallerStage.InstallSuccess -> {
                            InstallSuccessContent(
                                appInfo = appInfoState,
                                session = session,
                                dhizukuAutoClose = settings.autoCloseCountDown,
                                onClose = closeSheet
                            )
                        }

                        is InstallerStage.InstallCompleted -> {
                            InstallCompletedContent(
                                results = stage.results,
                                onClose = closeSheet
                            )
                        }

                        is InstallerStage.InstallFailed -> {
                            if (session.error is ModuleInstallFailedIncompatibleAuthorizerException ||
                                session.error is ModuleInstallCmdInitException ||
                                session.error is ModuleInstallException
                            )
                                NonInstallFailedContent(
                                    error = session.error,
                                    onClose = closeSheet
                                )
                            else
                                InstallFailedContent(
                                    appInfo = appInfoState,
                                    session = session,
                                    viewModel = viewModel,
                                    onClose = closeSheet
                                )
                        }

                        is InstallerStage.InstallingModule -> {
                            InstallModuleContent(
                                outputLines = stage.output,
                                isFinished = stage.isFinished,
                                onClose = closeSheet
                            )
                        }

                        is InstallerStage.UninstallReady -> {
                            UninstallPrepareContent(
                                viewModel = viewModel,
                                onCancel = closeSheet,
                                onUninstall = { viewModel.dispatch(InstallerViewAction.Uninstall) }
                            )
                        }

                        is InstallerStage.Uninstalling -> {
                            UninstallingContent(viewModel = viewModel)
                        }

                        is InstallerStage.UninstallSuccess -> {
                            UninstallSuccessContent(viewModel = viewModel, onClose = closeSheet)
                        }

                        is InstallerStage.UninstallFailed -> {
                            UninstallFailedContent(
                                session = session,
                                viewModel = viewModel,
                                onClose = closeSheet
                            )
                        }

                        is InstallerStage.AnalyseFailed, is InstallerStage.ResolveFailed -> {
                            NonInstallFailedContent(
                                error = session.error,
                                onClose = closeSheet
                            )
                        }

                        is InstallerStage.Resolving, is InstallerStage.Analysing -> {
                            LoadingContent(
                                statusText = if (stage is InstallerStage.Resolving) stringResource(R.string.installer_resolving)
                                else stringResource(R.string.installer_analysing)
                            )
                        }

                        else -> {
                            LoadingContent(statusText = stringResource(R.string.loading))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RebootListPopup(
    modifier: Modifier = Modifier,
    alignment: PopupPositionProvider.Align = PopupPositionProvider.Align.TopEnd,
    onReboot: (String) -> Unit
) {
    val showTopPopup = remember { mutableStateOf(false) }

    IconButton(
        modifier = modifier,
        onClick = { showTopPopup.value = true },
        holdDownState = showTopPopup.value
    ) {
        Icon(
            imageVector = AppMiuixIcons.Refresh,
            contentDescription = stringResource(id = R.string.reboot),
            tint = MiuixTheme.colorScheme.onBackground
        )
    }

    WindowListPopup(
        show = showTopPopup.value,
        popupPositionProvider = ListPopupDefaults.ContextMenuPositionProvider,
        alignment = alignment,
        onDismissRequest = { showTopPopup.value = false }
    ) {
        val context = LocalContext.current
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager?

        @Suppress("DEPRECATION")
        val isRebootingUserspaceSupported =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && pm?.isRebootingUserspaceSupported == true

        ListPopupColumn {
            val rebootOptions = remember {
                val options = mutableListOf(
                    Pair(R.string.reboot, ""),
                    Pair(R.string.reboot_recovery, "recovery"),
                    Pair(R.string.reboot_bootloader, "bootloader"),
                    Pair(R.string.reboot_download, "download"),
                    Pair(R.string.reboot_edl, "edl")
                )
                if (isRebootingUserspaceSupported) {
                    options.add(1, Pair(R.string.reboot_userspace, "userspace"))
                }
                options
            }

            rebootOptions.forEachIndexed { idx, (id, reason) ->
                RebootDropdownItem(
                    id = id,
                    reason = reason,
                    showTopPopup = showTopPopup,
                    optionSize = rebootOptions.size,
                    index = idx,
                    onReboot = onReboot
                )
            }
        }
    }
}

@Composable
private fun RebootDropdownItem(
    @StringRes id: Int,
    reason: String = "",
    showTopPopup: MutableState<Boolean>,
    optionSize: Int,
    index: Int,
    onReboot: (String) -> Unit
) {
    DropdownItem(
        text = stringResource(id),
        optionSize = optionSize,
        index = index,
        onSelectedIndexChange = {
            onReboot(reason)
            showTopPopup.value = false
        }
    )
}
