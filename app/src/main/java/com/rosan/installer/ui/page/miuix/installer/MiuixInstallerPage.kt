package com.rosan.installer.ui.page.miuix.installer

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataType
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.main.installer.dialog.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.dialog.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.dialog.InstallerViewState
import com.rosan.installer.ui.page.miuix.installer.sheetcontent.InstallChoiceContent
import com.rosan.installer.ui.page.miuix.installer.sheetcontent.InstallCompletedContent
import com.rosan.installer.ui.page.miuix.installer.sheetcontent.InstallExtendedMenuContent
import com.rosan.installer.ui.page.miuix.installer.sheetcontent.InstallFailedContent
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
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.util.getSupportTitle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.extra.SuperBottomSheet
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.Back
import top.yukonga.miuix.kmp.icon.icons.useful.Cancel
import top.yukonga.miuix.kmp.icon.icons.useful.Info
import top.yukonga.miuix.kmp.icon.icons.useful.Settings
import top.yukonga.miuix.kmp.utils.BackHandler

private const val SHEET_ANIMATION_DURATION = 250L

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
fun MiuixInstallerPage(
    installer: InstallerRepo,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val showBottomSheet = remember { mutableStateOf(true) }

    val viewModel: InstallerViewModel = koinViewModel { parametersOf(installer) }
    val currentState = viewModel.state
    val showSettings = viewModel.showMiuixSheetRightActionSettings
    val showPermissions = viewModel.showMiuixPermissionList

    val containerType = installer.analysisResults.firstOrNull()?.appEntities?.firstOrNull()?.app?.containerType ?: DataType.NONE
    val currentPackageName by viewModel.currentPackageName.collectAsState()
    val packageName = currentPackageName ?: installer.analysisResults.firstOrNull()?.packageName ?: ""
    val displayIcons by viewModel.displayIcons.collectAsState()
    val installProgressTextRes by viewModel.installProgressText.collectAsState()

    val analysisResult =
        if (currentPackageName != null) installer.analysisResults.find { it.packageName == currentPackageName } else null
    val baseEntity = analysisResult?.appEntities?.map { it.app }?.filterIsInstance<AppEntity.BaseEntity>()?.firstOrNull()
    val appIcon = if (currentPackageName != null) displayIcons[currentPackageName] else null

    LaunchedEffect(installer.id) {
        viewModel.dispatch(InstallerViewAction.CollectRepo(installer))
    }

    BackHandler(
        enabled = showSettings || showPermissions,
        onBack = {
            if (showSettings) {
                viewModel.dispatch(InstallerViewAction.HideMiuixSheetRightActionSettings)
            } else if (showPermissions) {
                viewModel.dispatch(InstallerViewAction.HideMiuixPermissionList)
            }
        }
    )

    val sheetTitle = when (currentState) {
        is InstallerViewState.Preparing -> stringResource(R.string.installer_preparing)
        is InstallerViewState.InstallChoice -> stringResource(containerType.getSupportTitle())
        is InstallerViewState.InstallExtendedMenu -> stringResource(R.string.config_label_install_options)
        is InstallerViewState.InstallPrepare -> when {
            showSettings -> stringResource(R.string.installer_settings)
            showPermissions -> stringResource(R.string.permission_list)
            else -> stringResource(R.string.installer_install_app)
        }

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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        content = {}
    )
    SuperBottomSheet(
        show = showBottomSheet, // Always true as long as this page is composed.
        backgroundColor = if (isSystemInDarkTheme()) Color(0xFF242424) else Color(0xFFF7F7F7),
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
                                if (viewModel.isDismissible) {
                                    showBottomSheet.value = !showBottomSheet.value
                                    scope.launch {
                                        delay(SHEET_ANIMATION_DURATION)
                                        viewModel.dispatch(InstallerViewAction.Close)
                                    }
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

                else -> null
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

                else -> null
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
                    if (viewModel.disableNotificationOnDismiss) {
                        viewModel.dispatch(InstallerViewAction.Close)
                    } else {
                        viewModel.dispatch(InstallerViewAction.Background)
                    }
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
                is InstallerViewState.InstallChoice -> {
                    InstallChoiceContent(installer, viewModel, closeSheet)
                }

                is InstallerViewState.InstallExtendedMenu -> {
                    InstallExtendedMenuContent(installer, viewModel)
                }

                is InstallerViewState.Preparing -> {
                    InstallPreparingContent(viewModel = viewModel)
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
                                PrepareSettingsContent(installer, viewModel)
                            }

                            "permissions" -> {
                                InstallPreparePermissionContent(
                                    installer = installer,
                                    viewModel = viewModel,
                                    onBack = { viewModel.dispatch(InstallerViewAction.HideMiuixPermissionList) }
                                )
                            }

                            else -> { // "prepare"
                                InstallPrepareContent(
                                    installer = installer,
                                    viewModel = viewModel,
                                    onCancel = closeSheet,
                                    onInstall = { viewModel.dispatch(InstallerViewAction.Install) }
                                )
                            }
                        }
                    }
                }

                is InstallerViewState.Installing -> {
                    // TODO Show a progress indicator during installation.
                    InstallingContent(
                        baseEntity = baseEntity,
                        appIcon = appIcon,
                        // progress = installProgress,
                        progressTextRes = installProgressTextRes
                    )
                }

                is InstallerViewState.InstallSuccess -> {
                    InstallSuccessContent(
                        baseEntity = baseEntity,
                        installer = installer,
                        packageName = packageName,
                        appIcon = appIcon,
                        dhizukuAutoClose = viewModel.autoCloseCountDown,
                        onClose = closeSheet
                    )
                }

                is InstallerViewState.InstallCompleted -> {
                    InstallCompletedContent(
                        results = (viewModel.state as InstallerViewState.InstallCompleted).results,
                        onClose = closeSheet
                    )
                }

                is InstallerViewState.InstallFailed -> {
                    InstallFailedContent(
                        baseEntity = baseEntity,
                        appIcon = appIcon,
                        installer = installer,
                        viewModel = viewModel,
                        onClose = closeSheet
                    )
                }

                is InstallerViewState.UninstallReady -> {
                    UninstallPrepareContent(
                        viewModel = viewModel,
                        onCancel = closeSheet,
                        onUninstall = { viewModel.dispatch(InstallerViewAction.Uninstall) }
                    )
                }

                is InstallerViewState.Uninstalling -> {
                    UninstallingContent(viewModel = viewModel)
                }

                is InstallerViewState.UninstallSuccess -> {
                    UninstallSuccessContent(viewModel = viewModel, onClose = closeSheet)
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