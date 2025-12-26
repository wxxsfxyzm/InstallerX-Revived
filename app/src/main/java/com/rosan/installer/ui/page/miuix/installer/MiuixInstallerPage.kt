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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.data.app.model.entity.DataType
import com.rosan.installer.data.app.model.exception.ModuleInstallCmdInitException
import com.rosan.installer.data.app.model.exception.ModuleInstallException
import com.rosan.installer.data.app.model.exception.ModuleInstallFailedIncompatibleAuthorizerException
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.common.LocalMiPackageInstallerPresent
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.InstallerViewState
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
import com.rosan.installer.ui.theme.m3color.PaletteStyle
import com.rosan.installer.ui.theme.m3color.dynamicColorScheme
import com.rosan.installer.ui.util.getSupportTitle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopup
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.extra.SuperBottomSheet
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.Back
import top.yukonga.miuix.kmp.icon.icons.useful.Cancel
import top.yukonga.miuix.kmp.icon.icons.useful.Info
import top.yukonga.miuix.kmp.icon.icons.useful.Reboot
import top.yukonga.miuix.kmp.icon.icons.useful.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor

private const val SHEET_ANIMATION_DURATION = 200L

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
fun MiuixInstallerPage(
    installer: InstallerRepo,
    activeColorSchemeState: MutableState<ColorScheme>,
    globalColorScheme: ColorScheme,
    isDarkMode: Boolean,
    basePaletteStyle: PaletteStyle
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val showBottomSheet = remember { mutableStateOf(true) }

    val viewModel: InstallerViewModel = koinViewModel { parametersOf(installer) }
    val currentState = viewModel.state
    val settings = viewModel.viewSettings
    val showSettings = viewModel.showMiuixSheetRightActionSettings
    val showPermissions = viewModel.showMiuixPermissionList
    val temporarySeedColor by viewModel.seedColor.collectAsState()
    val colorScheme = activeColorSchemeState.value

    LaunchedEffect(temporarySeedColor, globalColorScheme, isDarkMode, basePaletteStyle) {
        if (temporarySeedColor == null) {
            activeColorSchemeState.value = globalColorScheme
        } else {
            activeColorSchemeState.value = dynamicColorScheme(
                keyColor = temporarySeedColor!!,
                isDark = isDarkMode,
                style = basePaletteStyle
            )
        }
    }

    val sourceType = installer.analysisResults.firstOrNull()?.appEntities?.firstOrNull()?.app?.sourceType ?: DataType.NONE
    val currentPackageName by viewModel.currentPackageName.collectAsState()
    val packageName = currentPackageName ?: installer.analysisResults.firstOrNull()?.packageName ?: ""
    val displayIcons by viewModel.displayIcons.collectAsState()
    val appInfoState = rememberAppInfoState(
        installer = installer,
        currentPackageName = currentPackageName,
        displayIcons = displayIcons
    )

    LaunchedEffect(installer.id) {
        viewModel.dispatch(InstallerViewAction.CollectRepo(installer))
    }

    val sheetTitle = when (currentState) {
        is InstallerViewState.Preparing -> stringResource(R.string.installer_preparing)
        is InstallerViewState.InstallChoice -> stringResource(sourceType.getSupportTitle())
        is InstallerViewState.InstallExtendedMenu -> stringResource(R.string.config_label_install_options)
        is InstallerViewState.InstallPrepare -> when {
            showSettings -> stringResource(R.string.installer_settings)
            showPermissions -> stringResource(R.string.permission_list)
            else -> stringResource(
                if (viewModel.isInstallingModule) R.string.installer_install_module
                else R.string.installer_install_app
            )
        }

        is InstallerViewState.InstallingModule -> if (currentState.isFinished) {
            stringResource(R.string.installer_install_complete)
        } else {
            stringResource(R.string.installer_installing_module)
        }

        is InstallerViewState.InstallConfirm -> stringResource(R.string.installer_install_confirm)
        is InstallerViewState.Installing -> stringResource(R.string.installer_installing)
        is InstallerViewState.InstallCompleted -> stringResource(R.string.installer_install_success)
        is InstallerViewState.InstallSuccess -> stringResource(R.string.installer_install_success)
        is InstallerViewState.InstallFailed -> stringResource(R.string.installer_install_failed)
        is InstallerViewState.UninstallReady -> stringResource(R.string.installer_uninstall_app)
        is InstallerViewState.Uninstalling -> stringResource(R.string.installer_uninstalling)
        is InstallerViewState.UninstallSuccess -> stringResource(R.string.uninstall_success_message)
        is InstallerViewState.UninstallFailed -> stringResource(R.string.uninstall_failed_message)
        is InstallerViewState.AnalyseFailed -> stringResource(R.string.installer_analyse_failed)
        is InstallerViewState.ResolveFailed -> stringResource(R.string.installer_resolve_failed)
        is InstallerViewState.Resolving -> stringResource(R.string.installer_resolving)
        is InstallerViewState.Analysing -> stringResource(R.string.installer_analysing)
        else -> stringResource(R.string.loading)
    }

    val closeSheet: () -> Unit = {
        showBottomSheet.value = false
        scope.launch {
            delay(SHEET_ANIMATION_DURATION)
            viewModel.dispatch(InstallerViewAction.Close)
        }
    }

    val isMiInstallerSupported = LocalMiPackageInstallerPresent.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        content = {}
    )
    SuperBottomSheet(
        show = showBottomSheet, // Always true as long as this page is composed.
        backgroundColor = if (isDynamicColor) colorScheme.surfaceContainerHigh else if (isDarkMode)
            Color(0xFF242424) else Color(0xFFF7F7F7),
        leftAction = {
            when (currentState) {
                is InstallerViewState.InstallChoice -> {
                    // Check the new flag from ViewModel
                    if (viewModel.navigatedFromPrepareToChoice) {
                        // Came from Prepare (re-selecting splits) -> Show Back icon, go back to Prepare
                        MiuixBackButton(
                            icon = MiuixIcons.Useful.Back,
                            onClick = { viewModel.dispatch(InstallerViewAction.InstallPrepare) }
                        )
                    } else {
                        // Initial choice or other origin -> Show Cancel icon, force close
                        MiuixBackButton(
                            icon = MiuixIcons.Useful.Cancel,
                            onClick = closeSheet
                        )
                    }
                }

                is InstallerViewState.InstallConfirm -> {
                    MiuixBackButton(
                        icon = MiuixIcons.Useful.Cancel,
                        onClick = {
                            viewModel.dispatch(InstallerViewAction.ApproveSession(currentState.sessionId, false))
                        }
                    )
                }

                is InstallerViewState.InstallCompleted,
                is InstallerViewState.InstallFailed,
                is InstallerViewState.InstallSuccess,
                is InstallerViewState.UninstallReady,
                is InstallerViewState.UninstallSuccess,
                is InstallerViewState.UninstallFailed,
                is InstallerViewState.AnalyseFailed,
                is InstallerViewState.ResolveFailed -> {
                    MiuixBackButton(
                        icon = MiuixIcons.Useful.Cancel,
                        onClick = {
                            showBottomSheet.value = !showBottomSheet.value
                            scope.launch {
                                delay(SHEET_ANIMATION_DURATION)
                                if (viewModel.isDismissible)
                                    viewModel.dispatch(InstallerViewAction.Close)
                            }
                        }
                    )
                }

                is InstallerViewState.InstallPrepare -> {
                    MiuixBackButton(
                        icon = if (showSettings || showPermissions) MiuixIcons.Useful.Back else MiuixIcons.Useful.Cancel,
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

                is InstallerViewState.InstallExtendedMenu -> {
                    MiuixBackButton(
                        icon = MiuixIcons.Useful.Back,
                        onClick = { viewModel.dispatch(InstallerViewAction.InstallPrepare) }
                    )
                }

                else -> {}
            }
        },
        rightAction = {
            when (currentState) {
                is InstallerViewState.InstallPrepare -> {
                    if (!showSettings && !showPermissions) {
                        IconButton(onClick = { viewModel.dispatch(InstallerViewAction.ShowMiuixSheetRightActionSettings) }) {
                            Icon(
                                imageVector = MiuixIcons.Useful.Settings,
                                contentDescription = stringResource(R.string.installer_settings)
                            )
                        }
                    } else {
                        Spacer(Modifier.size(48.dp))
                    }
                }

                is InstallerViewState.InstallSuccess -> {
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
                            imageVector = MiuixIcons.Useful.Info,
                            contentDescription = stringResource(R.string.installer_settings)
                        )
                    }
                }

                is InstallerViewState.InstallingModule -> {
                    // Only show the reboot menu when the installation is finished
                    if (currentState.isFinished) {
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
        allowDismiss = viewModel.isDismissible,
        onDismissRequest = {
            if (viewModel.isDismissible) {
                // If it is dismissible, then proceed to close the sheet.
                showBottomSheet.value = !showBottomSheet.value
                scope.launch {
                    delay(SHEET_ANIMATION_DURATION) // Wait for sheet animation

                    val state = viewModel.state
                    val disableNotif = settings.disableNotificationOnDismiss
                    val isModule = state is InstallerViewState.InstallingModule

                    // 1. If it's a module install OR the "disable notification" setting is on -> Close
                    // 2. Otherwise (including Preparing, Standard APK install) -> Background
                    val action = if (isModule || disableNotif) {
                        InstallerViewAction.Close
                    } else {
                        InstallerViewAction.Background
                    }

                    viewModel.dispatch(action)
                }
            }
        }
    ) {
        AnimatedContent(
            targetState = viewModel.state::class,
            label = "MiuixSheetContentAnimation",
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 150)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = 150))
            }
        ) { _ ->
            when (viewModel.state) {
                is InstallerViewState.InstallConfirm -> {
                    InstallConfirmContent(
                        colorScheme = colorScheme,
                        isDarkMode = isDarkMode,
                        viewModel = viewModel,
                        onCancel = {
                            showBottomSheet.value = false
                            scope.launch {
                                delay(SHEET_ANIMATION_DURATION)
                                viewModel.dispatch(
                                    InstallerViewAction.ApproveSession(
                                        (viewModel.state as InstallerViewState.InstallConfirm).sessionId,
                                        false
                                    )
                                )
                            }
                        },
                        onConfirm = {
                            showBottomSheet.value = false
                            scope.launch {
                                delay(SHEET_ANIMATION_DURATION)
                                viewModel.dispatch(
                                    InstallerViewAction.ApproveSession(
                                        (viewModel.state as InstallerViewState.InstallConfirm).sessionId,
                                        true
                                    )
                                )
                            }
                        }
                    )
                }

                is InstallerViewState.InstallChoice -> {
                    InstallChoiceContent(
                        colorScheme = colorScheme,
                        isDarkMode = isDarkMode,
                        installer = installer,
                        viewModel = viewModel,
                        onCancel = closeSheet
                    )
                }

                is InstallerViewState.InstallExtendedMenu -> {
                    InstallExtendedMenuContent(
                        colorScheme = colorScheme,
                        isDarkMode = isDarkMode,
                        installer = installer,
                        viewModel = viewModel
                    )
                }

                is InstallerViewState.Preparing -> {
                    InstallPreparingContent(
                        colorScheme,
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

                is InstallerViewState.InstallPrepare -> {
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
                                    colorScheme = colorScheme,
                                    isDarkMode = isDarkMode,
                                    installer = installer,
                                    viewModel = viewModel
                                )
                            }

                            "permissions" -> {
                                InstallPreparePermissionContent(
                                    colorScheme = colorScheme,
                                    isDarkMode = isDarkMode,
                                    installer = installer,
                                    viewModel = viewModel,
                                    onBack = { viewModel.dispatch(InstallerViewAction.HideMiuixPermissionList) }
                                )
                            }

                            else -> { // "prepare"
                                InstallPrepareContent(
                                    colorScheme = colorScheme,
                                    isDarkMode = isDarkMode,
                                    installer = installer,
                                    viewModel = viewModel,
                                    appInfo = appInfoState,
                                    onCancel = closeSheet,
                                    onInstall = {
                                        viewModel.dispatch(InstallerViewAction.Install)
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

                is InstallerViewState.Installing -> {
                    InstallingContent(
                        state = viewModel.state as InstallerViewState.Installing,
                        appInfo = appInfoState,
                        onButtonClick = {
                            scope.launch {
                                delay(SHEET_ANIMATION_DURATION)
                                viewModel.dispatch(InstallerViewAction.Background)
                            }
                        }
                    )
                }

                is InstallerViewState.InstallSuccess -> {
                    InstallSuccessContent(
                        appInfo = appInfoState,
                        installer = installer,
                        dhizukuAutoClose = settings.autoCloseCountDown,
                        onClose = closeSheet
                    )
                }

                is InstallerViewState.InstallCompleted -> {
                    InstallCompletedContent(
                        colorScheme = colorScheme,
                        isDarkMode = isDarkMode,
                        results = (viewModel.state as InstallerViewState.InstallCompleted).results,
                        onClose = closeSheet
                    )
                }

                is InstallerViewState.InstallFailed -> {
                    if (installer.error is ModuleInstallFailedIncompatibleAuthorizerException ||
                        installer.error is ModuleInstallCmdInitException ||
                        installer.error is ModuleInstallException
                    )
                        NonInstallFailedContent(
                            colorScheme = colorScheme,
                            error = installer.error,
                            onClose = closeSheet
                        )
                    else
                        CompositionLocalProvider(
                            LocalMiPackageInstallerPresent provides isMiInstallerSupported
                        ) {
                            InstallFailedContent(
                                colorScheme = colorScheme,
                                isDarkMode = isDarkMode,
                                appInfo = appInfoState,
                                installer = installer,
                                viewModel = viewModel,
                                onClose = closeSheet
                            )
                        }
                }

                is InstallerViewState.InstallingModule -> {
                    val moduleState = viewModel.state as InstallerViewState.InstallingModule
                    InstallModuleContent(
                        colorScheme = colorScheme,
                        isDarkMode = isDarkMode,
                        outputLines = moduleState.output,
                        isFinished = moduleState.isFinished,
                        onClose = closeSheet
                    )
                }

                is InstallerViewState.UninstallReady -> {
                    UninstallPrepareContent(
                        colorScheme = colorScheme,
                        isDarkMode = isDarkMode,
                        viewModel = viewModel,
                        onCancel = closeSheet,
                        onUninstall = { viewModel.dispatch(InstallerViewAction.Uninstall) }
                    )
                }

                is InstallerViewState.Uninstalling -> {
                    UninstallingContent(colorScheme = colorScheme, viewModel = viewModel)
                }

                is InstallerViewState.UninstallSuccess -> {
                    UninstallSuccessContent(colorScheme = colorScheme, viewModel = viewModel, onClose = closeSheet)
                }

                is InstallerViewState.UninstallFailed -> {
                    UninstallFailedContent(
                        installer = installer,
                        viewModel = viewModel,
                        onClose = closeSheet
                    )
                }

                is InstallerViewState.AnalyseFailed, is InstallerViewState.ResolveFailed -> {
                    NonInstallFailedContent(
                        colorScheme = colorScheme,
                        error = installer.error,
                        onClose = closeSheet
                    )
                }

                is InstallerViewState.Resolving, is InstallerViewState.Analysing -> {
                    LoadingContent(
                        statusText = if (viewModel.state is InstallerViewState.Resolving) stringResource(R.string.installer_resolving)
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

@Composable
private fun RebootListPopup(
    modifier: Modifier = Modifier,
    alignment: PopupPositionProvider.Align = PopupPositionProvider.Align.TopRight,
    onReboot: (String) -> Unit
) {
    val showTopPopup = remember { mutableStateOf(false) }

    IconButton(
        modifier = modifier,
        onClick = { showTopPopup.value = true },
        holdDownState = showTopPopup.value
    ) {
        Icon(
            imageVector = MiuixIcons.Useful.Reboot,
            contentDescription = stringResource(id = R.string.reboot),
            tint = MiuixTheme.colorScheme.onBackground
        )
    }

    ListPopup(
        show = showTopPopup,
        popupPositionProvider = ListPopupDefaults.ContextMenuPositionProvider,
        alignment = alignment,
        onDismissRequest = {
            showTopPopup.value = false
        }
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