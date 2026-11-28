// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.tappableElementIgnoringVisibility
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog as ComposeDialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.effect.CouiContour
import com.itos.coui.effect.CouiContourStyle
import com.itos.coui.effect.LocalCouiContourGlowEnabled
import com.itos.coui.effect.SpotlightPreset
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.effect.rememberCouiSpotlight
import com.itos.coui.icons.CouiIcons
import com.itos.coui.menu.CouiMenuItem
import com.itos.coui.menu.Menu
import com.itos.coui.menu.ProvideCouiMenuBackdrop
import com.itos.coui.motion.couiPressMotion
import com.itos.coui.motion.couiTouchMotion
import com.itos.coui.motion.rememberCouiPressMotion
import com.itos.coui.motion.rememberCouiTouchMotion
import com.itos.coui.theme.CouiTheme
import com.rosan.installer.R
import com.rosan.installer.domain.engine.exception.ModuleInstallException
import com.rosan.installer.domain.engine.model.install.MmzSelectionMode
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.source.DataType
import com.rosan.installer.domain.session.model.ConfirmationRequestType
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.domain.settings.model.preferences.RootMode
import com.rosan.installer.ui.page.coui.installer.components.rememberCouiAppInfoState
import com.rosan.installer.ui.page.coui.installer.sheetcontent.CouiInstallChoiceContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.CouiInstallCompletedContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.CouiInstallConfirmContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.CouiInstallExtendedMenuContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.CouiInstallFailedContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.CouiInstallModuleContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.CouiInstallPrepareContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.CouiInstallPreparePermissionContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.CouiInstallPreparingContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.CouiInstallSuccessContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.CouiInstallingContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.CouiLoadingContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.CouiNonInstallFailedContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.CouiPrepareSettingsContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.CouiUnarchiveErrorContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.CouiUnarchivePrepareContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.CouiUninstallFailedContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.CouiUninstallPrepareContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.CouiUninstallSuccessContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.CouiUninstallingContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.couiUnarchiveErrorTitle
import com.rosan.installer.ui.page.coui.widgets.LocalCouiGlassSurface
import com.rosan.installer.ui.page.main.installer.InstallerStage
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.rememberUnknownSourcePermissionActionVisible
import com.rosan.installer.ui.page.main.widget.util.InstallerEventCollector
import com.rosan.installer.ui.theme.InstallerCouiTheme
import com.rosan.installer.ui.theme.InstallerTheme
import com.rosan.installer.ui.theme.LocalInstallerColorScheme
import com.rosan.installer.ui.theme.material.dynamicColorScheme
import com.rosan.installer.ui.util.getSupportTitle
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.util.function.Consumer

private data class CouiDialogHeaderAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
    val popupContent: (@Composable () -> Unit)? = null,
)

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
fun CouiInstallerPage(
    session: InstallerSessionRepository,
    viewModel: InstallerViewModel = koinViewModel { parametersOf(session) },
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(true) }
    val pendingDismissAction = remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(showDialog) {
        if (!showDialog) {
            delay(180L)
            val action = pendingDismissAction.value
            pendingDismissAction.value = null
            action?.invoke()
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stage = uiState.stage
    val settings = uiState.viewSettings
    val showSettings = uiState.showMiuixSheetRightActionSettings
    val showPermissions = uiState.showMiuixPermissionList
    val temporarySeedColor = uiState.seedColor
    val currentPackageName = uiState.currentPackageName
    val displayIcons = uiState.displayIcons
    val error = uiState.error

    val isDark = InstallerTheme.isDark
    val paletteStyle = InstallerTheme.paletteStyle
    val colorSpec = InstallerTheme.colorSpec
    val useCouiMonet = InstallerTheme.useCouiMonet
    val globalColorScheme = InstallerTheme.colorScheme

    val activeMd3ColorScheme = remember(temporarySeedColor, globalColorScheme, isDark, paletteStyle, colorSpec) {
        temporarySeedColor?.let {
            dynamicColorScheme(keyColor = it, isDark = isDark, style = paletteStyle, colorSpec = colorSpec)
        } ?: globalColorScheme
    }

    val analysisResults = uiState.analysisResults
    val sourceType = analysisResults.firstOrNull()?.appEntities?.firstOrNull()?.app?.sourceType ?: DataType.NONE
    var selectionMode by remember(session.id, sourceType) { mutableStateOf(MmzSelectionMode.INITIAL_CHOICE) }
    var preparedFromTypeChoice by remember(session.id, sourceType) { mutableStateOf(false) }
    var returnedToTypeChoice by remember(session.id, sourceType) { mutableStateOf(false) }
    val isApkChoice = sourceType == DataType.MIXED_MODULE_ZIP && selectionMode == MmzSelectionMode.APK_CHOICE
    val packageName = currentPackageName ?: analysisResults.firstOrNull()?.packageName ?: ""
    val appInfoState = rememberCouiAppInfoState(
        analysisResults = analysisResults,
        currentPackageName = currentPackageName,
        displayIcons = displayIcons,
    )

    LaunchedEffect(session.id) {
        viewModel.dispatch(InstallerViewAction.CollectSession(session))
    }

    InstallerEventCollector(viewModel)

    val dialogTitle = when (stage) {
        is InstallerStage.Preparing -> stringResource(R.string.installer_preparing)
        is InstallerStage.InstallWaitingUnknownSource -> stringResource(R.string.installer_waiting_unknown_source)
        is InstallerStage.InstallChoice -> sourceType.getSupportTitle()
        is InstallerStage.InstallExtendedMenu -> stringResource(R.string.config_label_install_options)
        is InstallerStage.InstallPrepare -> when {
            showSettings -> stringResource(R.string.installer_settings)
            showPermissions -> stringResource(R.string.permission_list)
            else -> stringResource(
                if (viewModel.isInstallingModule) R.string.installer_install_module else R.string.installer_install_app,
            )
        }
        is InstallerStage.InstallingModule -> if (stage.isFinished) {
            stringResource(R.string.installer_install_complete)
        } else {
            stringResource(R.string.installer_installing_module)
        }
        is InstallerStage.InstallConfirm -> stringResource(
            when (stage.requestType) {
                ConfirmationRequestType.PRE_APPROVAL -> R.string.installer_install_pre_approval
                ConfirmationRequestType.PERMISSIONS -> R.string.installer_permissions_confirm
                ConfirmationRequestType.INSTALL -> R.string.installer_install_confirm
            },
        )
        is InstallerStage.Installing -> stringResource(R.string.installer_installing)
        is InstallerStage.InstallCompleted -> stringResource(R.string.installer_install_success)
        is InstallerStage.InstallSuccess -> stringResource(R.string.installer_install_success)
        is InstallerStage.InstallFailed -> stringResource(R.string.installer_install_failed)
        is InstallerStage.UninstallReady -> stringResource(
            if (uiState.uiUninstallInfo?.isArchived == true) R.string.uninstall_archive else R.string.installer_uninstall_app,
        )
        is InstallerStage.Uninstalling -> stringResource(R.string.installer_uninstalling)
        is InstallerStage.UninstallSuccess -> stringResource(R.string.uninstall_success_message)
        is InstallerStage.UninstallFailed -> stringResource(R.string.uninstall_failed_message)
        is InstallerStage.UnarchiveReady -> stringResource(R.string.unarchive_restore)
        is InstallerStage.Unarchiving -> stringResource(R.string.unarchive_restoring)
        is InstallerStage.UnarchiveError -> couiUnarchiveErrorTitle(stage.status, stage.installerLabel)
        is InstallerStage.UnarchiveFailed -> stringResource(R.string.unarchive_failed)
        is InstallerStage.AnalyseFailed -> stringResource(R.string.installer_analyse_failed)
        is InstallerStage.ResolveFailed -> stringResource(R.string.installer_resolve_failed)
        is InstallerStage.Resolving -> stringResource(R.string.installer_resolving)
        is InstallerStage.Analysing -> stringResource(R.string.installer_analysing)
        else -> stringResource(R.string.loading)
    }

    fun dismissDialog(afterDismiss: () -> Unit) {
        if (!showDialog) return
        pendingDismissAction.value = afterDismiss
        showDialog = false
    }

    val closeDialog: () -> Unit = {
        dismissDialog { viewModel.dispatch(InstallerViewAction.Close) }
    }

    val backToTypeChoice: () -> Unit = {
        viewModel.dispatch(InstallerViewAction.SetApkSelection(false))
        analysisResults.flatMap { it.appEntities }
            .filter { it.selected && it.app is AppEntity.ModuleEntity }
            .forEach { entity ->
                viewModel.dispatch(InstallerViewAction.ToggleSelection(entity.app.packageName, entity, true))
            }
        selectionMode = MmzSelectionMode.INITIAL_CHOICE
        preparedFromTypeChoice = false
        returnedToTypeChoice = true
        if (stage !is InstallerStage.InstallChoice) {
            viewModel.dispatch(InstallerViewAction.InstallChoice)
        }
    }

    var showRebootMenu by remember { mutableStateOf(false) }

    // Only show a leading back/cancel icon when navigating back from a sub-page
    // or when the stage lacks a bottom Cancel/Close button (ColorOS native dialog style).
    val leadingAction: CouiDialogHeaderAction? = when (stage) {
        is InstallerStage.Preparing,
        is InstallerStage.InstallWaitingUnknownSource,
        -> CouiDialogHeaderAction(
            icon = CouiIcons.Filled.Close,
            contentDescription = stringResource(R.string.cancel),
            onClick = { dismissDialog { viewModel.dispatch(InstallerViewAction.Cancel) } },
        )

        is InstallerStage.InstallChoice -> when {
            isApkChoice -> CouiDialogHeaderAction(
                icon = CouiIcons.Filled.Back,
                contentDescription = stringResource(R.string.back),
                onClick = backToTypeChoice,
            )
            uiState.navigatedFromPrepareToChoice && !returnedToTypeChoice -> CouiDialogHeaderAction(
                icon = CouiIcons.Filled.Back,
                contentDescription = stringResource(R.string.back),
                onClick = { viewModel.dispatch(InstallerViewAction.InstallPrepare) },
            )
            else -> null
        }

        is InstallerStage.InstallPrepare -> when {
            showSettings -> CouiDialogHeaderAction(
                icon = CouiIcons.Filled.Back,
                contentDescription = stringResource(R.string.back),
                onClick = { viewModel.dispatch(InstallerViewAction.HideMiuixSheetRightActionSettings) },
            )
            showPermissions -> CouiDialogHeaderAction(
                icon = CouiIcons.Filled.Back,
                contentDescription = stringResource(R.string.back),
                onClick = { viewModel.dispatch(InstallerViewAction.HideMiuixPermissionList) },
            )
            preparedFromTypeChoice -> CouiDialogHeaderAction(
                icon = CouiIcons.Filled.Back,
                contentDescription = stringResource(R.string.back),
                onClick = backToTypeChoice,
            )
            else -> null
        }

        is InstallerStage.InstallExtendedMenu -> CouiDialogHeaderAction(
            icon = CouiIcons.Filled.Back,
            contentDescription = stringResource(R.string.back),
            onClick = { viewModel.dispatch(InstallerViewAction.InstallPrepare) },
        )

        else -> null
    }

    val trailingAction: CouiDialogHeaderAction? = when (stage) {
        is InstallerStage.InstallPrepare -> if (!showSettings && !showPermissions) {
            CouiDialogHeaderAction(
                icon = Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.installer_settings),
                onClick = { viewModel.dispatch(InstallerViewAction.ShowMiuixSheetRightActionSettings) },
            )
        } else {
            null
        }

        is InstallerStage.InstallSuccess -> CouiDialogHeaderAction(
            icon = Icons.Outlined.Info,
            contentDescription = stringResource(R.string.installer_settings),
            onClick = {
                if (packageName.isNotEmpty()) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.fromParts("package", packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
                viewModel.dispatch(InstallerViewAction.Close)
            },
        )

        is InstallerStage.InstallingModule -> if (stage.isFinished) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager?
            @Suppress("DEPRECATION")
            val isRebootingUserspaceSupported =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && pm?.isRebootingUserspaceSupported == true
            val rebootLabel = stringResource(R.string.reboot)
            val softRebootLabel = stringResource(R.string.reboot_soft_reboot)
            val userspaceLabel = stringResource(R.string.reboot_userspace)
            val recoveryLabel = stringResource(R.string.reboot_recovery)
            val bootloaderLabel = stringResource(R.string.reboot_bootloader)
            val downloadLabel = stringResource(R.string.reboot_download)
            val edlLabel = stringResource(R.string.reboot_edl)
            val rebootMenuItems = remember(
                uiState.rootMode,
                isRebootingUserspaceSupported,
                rebootLabel,
                softRebootLabel,
                userspaceLabel,
                recoveryLabel,
                bootloaderLabel,
                downloadLabel,
                edlLabel,
            ) {
                buildList {
                    add(CouiMenuItem("", rebootLabel))
                    if (uiState.rootMode == RootMode.KernelSU) {
                        add(CouiMenuItem("ksud_soft_reboot", softRebootLabel))
                    }
                    if (isRebootingUserspaceSupported) {
                        add(CouiMenuItem("userspace", userspaceLabel))
                    }
                    add(CouiMenuItem("recovery", recoveryLabel))
                    add(CouiMenuItem("bootloader", bootloaderLabel))
                    add(CouiMenuItem("download", downloadLabel))
                    add(CouiMenuItem("edl", edlLabel))
                }
            }
            CouiDialogHeaderAction(
                icon = Icons.Outlined.Refresh,
                contentDescription = rebootLabel,
                onClick = { showRebootMenu = true },
                popupContent = {
                    Menu(
                        expanded = showRebootMenu,
                        onDismissRequest = { showRebootMenu = false },
                        items = rebootMenuItems,
                        onItemClick = { item ->
                            showRebootMenu = false
                            viewModel.dispatch(InstallerViewAction.Reboot(item.value))
                        },
                        offset = DpOffset(0.dp, 4.dp),
                    )
                },
            )
        } else {
            null
        }

        else -> null
    }

    val useMonetTheme = useCouiMonet || temporarySeedColor != null

    CompositionLocalProvider(
        LocalInstallerColorScheme provides activeMd3ColorScheme,
    ) {
        InstallerCouiTheme(
            darkTheme = isDark,
            colorScheme = activeMd3ColorScheme,
            useCouiMonet = useMonetTheme,
            compatStatusBarColor = false,
        ) {
            if (showDialog) {
                CouiFloatingInstallerDialog(
                    dark = isDark,
                    useBlur = settings.useBlur,
                    blurRadius = settings.couiBlurRadius,
                    glassOpacity = settings.couiGlassOpacity / 100f,
                    useMonet = useMonetTheme,
                    dismissible = uiState.isDismissible,
                    title = dialogTitle,
                    leadingAction = leadingAction,
                    trailingAction = trailingAction,
                    onDismissRequest = {
                        if (!uiState.isDismissible) return@CouiFloatingInstallerDialog
                        if (stage is InstallerStage.InstallConfirm) {
                            dismissDialog {
                                viewModel.dispatch(InstallerViewAction.ApproveSession(stage.sessionId, false))
                            }
                            return@CouiFloatingInstallerDialog
                        }
                        dismissDialog {
                            val disableNotif = settings.disableNotificationOnDismiss
                            val isModule = stage is InstallerStage.InstallingModule
                            val isUninstall =
                                stage is InstallerStage.UninstallReady ||
                                    stage is InstallerStage.UninstallResolveFailed ||
                                    stage is InstallerStage.UninstallSuccess ||
                                    stage is InstallerStage.UninstallFailed
                            val isUnarchive =
                                stage is InstallerStage.UnarchiveReady ||
                                    stage is InstallerStage.UnarchiveError ||
                                    stage is InstallerStage.UnarchiveFailed
                            val action = if (isModule || disableNotif || isUninstall || isUnarchive) {
                                InstallerViewAction.Close
                            } else {
                                InstallerViewAction.Background
                            }
                            viewModel.dispatch(action)
                        }
                    },
                ) {
                    AnimatedContent(
                        targetState = stage.javaClass,
                        label = "CouiDialogContentAnimation",
                        transitionSpec = {
                            fadeIn(animationSpec = tween(durationMillis = 150)) togetherWith
                                fadeOut(animationSpec = tween(durationMillis = 150))
                        },
                    ) { _ ->
                        when (stage) {
                            is InstallerStage.InstallConfirm -> CouiInstallConfirmContent(
                                viewModel = viewModel,
                                onCancel = {
                                    if (stage.isSelfSession) {
                                        viewModel.dispatch(InstallerViewAction.ApproveSession(stage.sessionId, false))
                                    } else {
                                        dismissDialog {
                                            viewModel.dispatch(InstallerViewAction.ApproveSession(stage.sessionId, false))
                                        }
                                    }
                                },
                                onConfirm = {
                                    if (stage.isSelfSession) {
                                        viewModel.dispatch(InstallerViewAction.ApproveSession(stage.sessionId, true))
                                    } else {
                                        dismissDialog {
                                            viewModel.dispatch(InstallerViewAction.ApproveSession(stage.sessionId, true))
                                        }
                                    }
                                },
                            )

                            is InstallerStage.InstallChoice -> CouiInstallChoiceContent(
                                viewModel = viewModel,
                                selectionMode = selectionMode,
                                onSelectionModeChange = { selectionMode = it },
                                onSelectMixedModuleType = { installAsModule ->
                                    preparedFromTypeChoice = true
                                    returnedToTypeChoice = false
                                    viewModel.dispatch(InstallerViewAction.SelectMixedModuleType(installAsModule))
                                },
                                onBackToTypeChoice = backToTypeChoice,
                                onCancel = closeDialog,
                            )

                            is InstallerStage.InstallExtendedMenu -> CouiInstallExtendedMenuContent(
                                viewModel = viewModel,
                            )

                            is InstallerStage.Preparing -> CouiInstallPreparingContent(
                                viewModel = viewModel,
                                onBackground = {
                                    dismissDialog { viewModel.dispatch(InstallerViewAction.Background) }
                                },
                            )

                            is InstallerStage.InstallWaitingUnknownSource -> {
                                val sourceAppLabel = uiState.unknownSourcePermissionAppLabel
                                    ?: uiState.initiatorAppLabel
                                    ?: stringResource(R.string.installer_label_unknown)
                                val showPermissionAction = rememberUnknownSourcePermissionActionVisible(
                                    isWaitingUnknownSource = true,
                                )
                                CouiInstallPreparingContent(
                                    viewModel = viewModel,
                                    onBackground = {
                                        viewModel.dispatch(InstallerViewAction.RequestUnknownSourcePermission)
                                    },
                                    descriptionText = stringResource(
                                        R.string.installer_waiting_unknown_source_desc,
                                        sourceAppLabel,
                                    ),
                                    buttonTextRes = R.string.suggestion_allow_unknown_source,
                                    showButton = showPermissionAction,
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
                                    label = "CouiPrepareSubState",
                                    transitionSpec = {
                                        fadeIn(animationSpec = tween(durationMillis = 150)) togetherWith
                                            fadeOut(animationSpec = tween(durationMillis = 150))
                                    },
                                ) { subState ->
                                    when (subState) {
                                        "settings" -> CouiPrepareSettingsContent(viewModel = viewModel)

                                        "permissions" -> CouiInstallPreparePermissionContent(
                                            viewModel = viewModel,
                                            onBack = { viewModel.dispatch(InstallerViewAction.HideMiuixPermissionList) },
                                        )

                                        else -> CouiInstallPrepareContent(
                                            viewModel = viewModel,
                                            appInfo = appInfoState,
                                            onCancel = closeDialog,
                                            onInstall = {
                                                viewModel.dispatch(InstallerViewAction.Install(true))
                                                if (settings.autoSilentInstall && !viewModel.isInstallingModule) {
                                                    dismissDialog {
                                                        viewModel.dispatch(InstallerViewAction.Background)
                                                    }
                                                }
                                            },
                                            onLongInstall = if (settings.longClickBackgroundInstall) {
                                                {
                                                    viewModel.dispatch(InstallerViewAction.Install(true))
                                                    if (!viewModel.isInstallingModule) {
                                                        dismissDialog {
                                                            viewModel.dispatch(InstallerViewAction.Background)
                                                        }
                                                    }
                                                }
                                            } else {
                                                null
                                            },
                                        )
                                    }
                                }
                            }

                            is InstallerStage.Installing -> CouiInstallingContent(
                                stage = stage,
                                appInfo = appInfoState,
                                onButtonClick = {
                                    dismissDialog { viewModel.dispatch(InstallerViewAction.Background) }
                                },
                            )

                            is InstallerStage.InstallSuccess -> CouiInstallSuccessContent(
                                appInfo = appInfoState,
                                viewModel = viewModel,
                                closeSessionCountDown = settings.closeSessionCountDown,
                                onClose = closeDialog,
                            )

                            is InstallerStage.InstallCompleted -> CouiInstallCompletedContent(
                                results = stage.results,
                                onClose = closeDialog,
                            )

                            is InstallerStage.InstallFailed -> if (error is ModuleInstallException) {
                                CouiNonInstallFailedContent(error = error, onClose = closeDialog)
                            } else {
                                CouiInstallFailedContent(
                                    appInfo = appInfoState,
                                    viewModel = viewModel,
                                    onClose = closeDialog,
                                )
                            }

                            is InstallerStage.InstallingModule -> CouiInstallModuleContent(
                                outputLines = stage.output,
                                isFinished = stage.isFinished,
                                onClose = closeDialog,
                            )

                            is InstallerStage.UninstallReady -> CouiUninstallPrepareContent(
                                viewModel = viewModel,
                                onCancel = closeDialog,
                                onUninstall = { viewModel.dispatch(InstallerViewAction.Uninstall) },
                            )

                            is InstallerStage.Uninstalling -> CouiUninstallingContent(viewModel = viewModel)

                            is InstallerStage.UninstallSuccess -> CouiUninstallSuccessContent(
                                viewModel = viewModel,
                                onClose = closeDialog,
                            )

                            is InstallerStage.UninstallFailed -> CouiUninstallFailedContent(
                                viewModel = viewModel,
                                onClose = closeDialog,
                            )

                            is InstallerStage.UnarchiveReady -> CouiUnarchivePrepareContent(
                                appLabel = stage.appLabel,
                                installerLabel = stage.installerLabel,
                                onCancel = closeDialog,
                                onRestore = { viewModel.dispatch(InstallerViewAction.StartUnarchive) },
                            )

                            is InstallerStage.Unarchiving -> CouiLoadingContent(
                                statusText = stringResource(R.string.unarchive_restoring),
                            )

                            is InstallerStage.UnarchiveError -> CouiUnarchiveErrorContent(
                                status = stage.status,
                                requiredBytes = stage.requiredBytes,
                                installerLabel = stage.installerLabel,
                                onClose = closeDialog,
                                onPrimaryAction = { viewModel.dispatch(InstallerViewAction.OpenUnarchiveErrorAction) },
                            )

                            is InstallerStage.UnarchiveFailed,
                            is InstallerStage.AnalyseFailed,
                            is InstallerStage.ResolveFailed,
                            -> CouiNonInstallFailedContent(error = error, onClose = closeDialog)

                            is InstallerStage.Resolving,
                            is InstallerStage.Analysing,
                            -> CouiLoadingContent(
                                statusText = if (stage is InstallerStage.Resolving) {
                                    stringResource(R.string.installer_resolving)
                                } else {
                                    stringResource(R.string.installer_analysing)
                                },
                            )

                            else -> CouiLoadingContent(statusText = stringResource(R.string.loading))
                        }
                    }
                }
            }
        }
    }
}

/**
 * ColorOS 17 native bottom-floating frosted-glass dialog container.
 * Uses Android 12+ cross-window background blur clipped tightly to the floating 45dp squircle card,
 * leaving the underlying app sharp and softly dimmed while illuminating the card rim with CouiContour.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CouiFloatingInstallerDialog(
    dark: Boolean,
    useBlur: Boolean,
    blurRadius: Int,
    glassOpacity: Float,
    useMonet: Boolean,
    dismissible: Boolean,
    title: String,
    leadingAction: CouiDialogHeaderAction?,
    trailingAction: CouiDialogHeaderAction?,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val dialogColors = CouiDialogDefaults.themeColors(dark)
    val couiSurface = CouiTheme.colorScheme.surface
    val contourEnabled = LocalCouiContourGlowEnabled.current

    // Capture navigation bar height from the host Activity before entering the Dialog window.
    val navBarBottomPx = WindowInsets.navigationBars.getBottom(density)
    val gestureNav = WindowInsets.tappableElementIgnoringVisibility.getBottom(density) == 0
    val bottomMarginPx = with(density) {
        val base = CouiDialogDefaults.bottomMargin.roundToPx()
        if (gestureNav) {
            maxOf(base, navBarBottomPx + 8.dp.roundToPx())
        } else {
            maxOf(16.dp.roundToPx(), navBarBottomPx + 12.dp.roundToPx())
        }
    }
    val horizontalMarginPx = with(density) { CouiDialogDefaults.horizontalMargin.roundToPx() }
    val maxWidthPx = with(density) { CouiDialogDefaults.maxWidth.roundToPx() }
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.roundToPx() }
    val cardWidthPx = minOf((screenWidthPx - horizontalMarginPx * 2).coerceAtLeast(1), maxWidthPx)
    val maxCardHeight = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        minOf(CouiDialogDefaults.landscapeMaxHeight, (configuration.screenHeightDp * 0.85f).dp)
    } else {
        minOf(CouiDialogDefaults.maxHeight, (configuration.screenHeightDp * 0.82f).dp)
    }

    val crossWindowBlurEnabled = rememberCrossWindowBlurEnabled()
    val effectiveBlurRadius = if (useBlur && crossWindowBlurEnabled) blurRadius.coerceIn(0, 200) else 0
    val resolvedGlassAlpha = glassOpacity.coerceIn(0.40f, 1f)
    val useGlassSurface = useBlur && (effectiveBlurRadius > 0 || resolvedGlassAlpha < 0.995f)

    val glassTintColor = remember(dark, useMonet, couiSurface, resolvedGlassAlpha) {
        val base = when {
            useMonet -> couiSurface
            dark -> Color(0xFF222226)
            else -> Color(0xFFF2F3F6)
        }
        base.copy(alpha = resolvedGlassAlpha)
    }
    val solidSurfaceColor = dialogColors.surface
    // Exact 45dp circular radius is strictly tangent to and inside CouiShape(45.dp, 3f) at all angles.
    val windowBlurCornerRadiusPx = with(density) { CouiDialogDefaults.cornerRadius.toPx() }
    val contourRadiusPx = windowBlurCornerRadiusPx

    ComposeDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = dismissible,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        DisposableEffect(
            dialogWindow,
            dark,
            effectiveBlurRadius,
            cardWidthPx,
            bottomMarginPx,
            windowBlurCornerRadiusPx,
        ) {
            var layoutListener: View.OnLayoutChangeListener? = null
            dialogWindow?.let { window ->
                window.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
                window.setWindowAnimations(R.style.CouiInstallerDialogAnimation)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                }
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                window.setDimAmount(if (dark) 0.48f else 0.22f)
                window.attributes = window.attributes.apply {
                    width = cardWidthPx
                    height = WindowManager.LayoutParams.WRAP_CONTENT
                    y = bottomMarginPx
                }
                val decor = window.decorView
                decor.setPadding(0, 0, 0, 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && effectiveBlurRadius > 0) {
                    val fallbackHeight = (windowBlurCornerRadiusPx * 2f).toInt().coerceAtLeast(1)
                    fun buildBlurDrawable(w: Int, h: Int): RoundedBlurWindowDrawable {
                        val safeW = w.coerceAtLeast(1)
                        val safeH = h.coerceAtLeast(1)
                        val r = minOf(windowBlurCornerRadiusPx, safeW / 2f, safeH / 2f)
                        return RoundedBlurWindowDrawable(r, safeW, safeH).apply {
                            setBounds(0, 0, safeW, safeH)
                        }
                    }

                    decor.outlineProvider = object : ViewOutlineProvider() {
                        override fun getOutline(view: View, outline: Outline) {
                            if (view.width > 0 && view.height > 0) {
                                val r = minOf(windowBlurCornerRadiusPx, view.width / 2f, view.height / 2f)
                                outline.setRoundRect(0, 0, view.width, view.height, r)
                            }
                        }
                    }
                    decor.clipToOutline = true

                    val initW = decor.width.takeIf { it > 0 } ?: cardWidthPx.coerceAtLeast(1)
                    val initH = decor.height.takeIf { it > 0 } ?: fallbackHeight
                    window.setBackgroundDrawable(buildBlurDrawable(initW, initH))
                    window.setBackgroundBlurRadius(effectiveBlurRadius)

                    var appliedW = decor.width
                    var appliedH = decor.height
                    val listener = View.OnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                        if (v.width > 0 && v.height > 0 && (v.width != appliedW || v.height != appliedH)) {
                            appliedW = v.width
                            appliedH = v.height
                            // Pass a fresh drawable with real layout bounds so DecorView.updateBackgroundBlurCorners()
                            // reads MODE_ROUND_RECT with the exact 45dp radius instead of an empty initial outline.
                            window.setBackgroundDrawable(buildBlurDrawable(v.width, v.height))
                            v.invalidateOutline()
                        }
                    }
                    decor.addOnLayoutChangeListener(listener)
                    layoutListener = listener
                } else {
                    decor.clipToOutline = false
                    decor.outlineProvider = ViewOutlineProvider.BACKGROUND
                    window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        window.setBackgroundBlurRadius(0)
                    }
                }
            }
            onDispose {
                val listener = layoutListener
                if (listener != null) {
                    dialogWindow?.decorView?.removeOnLayoutChangeListener(listener)
                }
            }
        }

        val contour = remember(contourEnabled, useGlassSurface) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && contourEnabled) {
                CouiContour(style = CouiContourStyle.Dialog)
            } else {
                null
            }
        }
        val contourPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }

        ProvideCouiMenuBackdrop(isolateContent = false) { couiWindowSource ->
            CompositionLocalProvider(LocalCouiGlassSurface provides useGlassSurface) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxCardHeight)
                        .then(couiWindowSource)
                        .clip(CouiDialogDefaults.shape)
                        .drawBehind {
                            drawRect(if (useGlassSurface) glassTintColor else solidSurfaceColor)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && contour != null) {
                                drawIntoCanvas { canvas ->
                                    canvas.nativeCanvas.drawRect(
                                        0f,
                                        0f,
                                        size.width,
                                        size.height,
                                        contourPaint.apply {
                                            shader = contour.shader(
                                                size = size,
                                                density = this@drawBehind.density,
                                                radiusPx = contourRadiusPx,
                                                cornerWeight = 3f,
                                                surfaceColor = Color.Transparent,
                                                innerGlowAlpha = if (dark) 0.04f else 0.42f,
                                                lineAlpha = if (dark) 0.30f else 0.65f,
                                                effectAlpha = 1f,
                                                lineWidthPx = (if (dark) 2f else 4f) * this@drawBehind.density,
                                            )
                                        },
                                    )
                                }
                            }
                        },
                ) {
                    CouiFloatingDialogHeader(
                        title = title,
                        titleColor = dialogColors.title,
                        dark = dark,
                        useGlass = useGlassSurface,
                        leadingAction = leadingAction,
                        trailingAction = trailingAction,
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
private fun CouiFloatingDialogHeader(
    title: String,
    titleColor: Color,
    dark: Boolean,
    useGlass: Boolean,
    leadingAction: CouiDialogHeaderAction?,
    trailingAction: CouiDialogHeaderAction?,
) {
    val hasCornerAction = leadingAction != null || trailingAction != null
    val titleHorizontalPadding = if (hasCornerAction) 52.dp else 24.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 8.dp),
    ) {
        if (leadingAction != null) {
            Box(modifier = Modifier.align(Alignment.CenterStart)) {
                CouiDialogCornerButton(
                    icon = leadingAction.icon,
                    contentDescription = leadingAction.contentDescription,
                    dark = dark,
                    useGlass = useGlass,
                    onClick = leadingAction.onClick,
                )
                leadingAction.popupContent?.invoke()
            }
        }

        Text(
            text = title,
            color = titleColor,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = titleHorizontalPadding),
        )

        if (trailingAction != null) {
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                CouiDialogCornerButton(
                    icon = trailingAction.icon,
                    contentDescription = trailingAction.contentDescription,
                    dark = dark,
                    useGlass = useGlass,
                    onClick = trailingAction.onClick,
                )
                trailingAction.popupContent?.invoke()
            }
        }
    }
}

@Composable
private fun CouiDialogCornerButton(
    icon: ImageVector,
    contentDescription: String,
    dark: Boolean,
    useGlass: Boolean,
    onClick: () -> Unit,
) {
    val glow = LocalCouiContourGlowEnabled.current
    val source = remember { MutableInteractionSource() }
    val motion = rememberCouiTouchMotion(true)
    val press = rememberCouiPressMotion()
    val (lightInput, drawLight) = rememberCouiSpotlight(
        interactive = true,
        dark = dark,
        preset = SpotlightPreset.OpaqueSmall2,
        cancelOnConsumedMove = true,
    )
    val contour = remember(glow) {
        if (glow && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            CouiContour(style = CouiContourStyle.CircleButton)
        } else {
            null
        }
    }
    val contourPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }
    val bgColor = when {
        useGlass && dark -> Color(0x2BFFFFFF)
        useGlass -> Color(0xEBFFFFFF)
        dark -> Color(0x1CFFFFFF)
        else -> Color(0x0D000000)
    }
    Box(
        modifier = Modifier
            .size(36.dp)
            .graphicsLayer {
                scaleX = motion.scaleX.value + press.scale.value - 1f
                scaleY = motion.scaleY.value + press.scale.value - 1f
                translationX = motion.translationX.value
                translationY = motion.translationY.value
            }
            .couiTouchMotion(motion, true)
            .couiPressMotion(press, true)
            .then(lightInput)
            .clip(CircleShape)
            .drawWithContent {
                drawRect(bgColor)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && contour != null) {
                    contourPaint.shader = contour.shader(
                        size = size,
                        density = density,
                        radiusPx = size.minDimension / 2f,
                        cornerWeight = 2f,
                        surfaceColor = Color.Transparent,
                        innerGlowAlpha = if (dark) 0.03f else 0.08f,
                        lineAlpha = if (dark) 0.16f else 0.78f,
                        effectAlpha = 1f,
                        lineWidthPx = 2.5f * density,
                    )
                    drawIntoCanvas { it.nativeCanvas.drawRect(0f, 0f, size.width, size.height, contourPaint) }
                }
                drawContent()
                drawLight?.invoke(this)
            }
            .couiButtonFeedback(dark = dark, interactionSource = source, pressFeedback = !useGlass)
            .clickable(
                interactionSource = source,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(19.dp),
            tint = CouiTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Window background drawable whose [getOutline] always supplies a valid [Outline.setRoundRect]
 * even before the first layout pass, preventing [android.view.Window.setBackgroundBlurRadius]
 * from falling back to a 0-radius rectangular blur behind rounded dialogs.
 */
private class RoundedBlurWindowDrawable(
    private val radiusPx: Float,
    private val fallbackWidthPx: Int,
    private val fallbackHeightPx: Int,
) : GradientDrawable() {
    init {
        shape = RECTANGLE
        cornerRadius = radiusPx
        setColor(android.graphics.Color.argb(1, 255, 255, 255))
    }

    override fun getOutline(outline: Outline) {
        val w = bounds.width().takeIf { it > 0 } ?: fallbackWidthPx.coerceAtLeast(1)
        val h = bounds.height().takeIf { it > 0 } ?: fallbackHeightPx.coerceAtLeast(1)
        val r = minOf(radiusPx, w / 2f, h / 2f)
        outline.setRoundRect(0, 0, w, h, r)
        outline.alpha = 1f
    }
}

@Composable
private fun rememberCrossWindowBlurEnabled(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
    val context = LocalContext.current
    val wm = remember(context) { context.getSystemService(WindowManager::class.java) }
    var isEnabled by remember(wm) { mutableStateOf(wm?.isCrossWindowBlurEnabled == true) }

    DisposableEffect(wm) {
        val manager = wm ?: return@DisposableEffect onDispose {}
        val listener = Consumer<Boolean> { enabled -> isEnabled = enabled }
        manager.addCrossWindowBlurEnabledListener(listener)
        onDispose { manager.removeCrossWindowBlurEnabledListener(listener) }
    }
    return isEnabled
}
