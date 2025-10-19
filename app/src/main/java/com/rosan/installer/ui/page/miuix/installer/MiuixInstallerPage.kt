package com.rosan.installer.ui.page.miuix.installer

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
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
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.main.installer.dialog.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.dialog.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.dialog.InstallerViewState
import com.rosan.installer.ui.page.miuix.installer.sheet.MiuixSheetContent
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.extra.patched.SuperBottomSheet
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.Back
import top.yukonga.miuix.kmp.icon.icons.useful.Cancel
import top.yukonga.miuix.kmp.icon.icons.useful.Info
import top.yukonga.miuix.kmp.icon.icons.useful.Settings

private const val SHEET_ANIMATION_DURATION = 100L

@Composable
fun MiuixInstallerPage(
    installer: InstallerRepo,
    onDismiss: () -> Unit = { installer.close() }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val showBottomSheet = remember { mutableStateOf(true) }

    val viewModel: InstallerViewModel = koinViewModel { parametersOf(installer) }
    val currentState = viewModel.state
    val showSettings = viewModel.showMiuixSheetRightActionSettings

    val currentPackageName by viewModel.currentPackageName.collectAsState()
    val packageName = currentPackageName ?: installer.analysisResults.firstOrNull()?.packageName ?: ""

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
                is InstallerViewState.InstallChoice,
                is InstallerViewState.InstallFailed,
                is InstallerViewState.InstallSuccess,
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
                        icon = if (showSettings) MiuixIcons.Useful.Back else MiuixIcons.Useful.Cancel,
                        onClick = {
                            if (showSettings) {
                                viewModel.dispatch(InstallerViewAction.HideMiuixSheetRightActionSettings)
                            } else {
                                showBottomSheet.value = !showBottomSheet.value
                                scope.launch {
                                    delay(SHEET_ANIMATION_DURATION)
                                    if (viewModel.isDismissible)
                                        viewModel.dispatch(InstallerViewAction.Close)
                                }
                            }
                        }
                    )
                }

                else -> {
                    Spacer(Modifier.size(48.dp))
                }
            }
        },
        rightAction = {
            when (currentState) {
                is InstallerViewState.InstallPrepare -> {
                    if (!showSettings) {
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
                            viewModel.dispatch(InstallerViewAction.Background)
                        }) {
                        Icon(
                            imageVector = MiuixIcons.Useful.Info,
                            contentDescription = stringResource(R.string.installer_settings)
                        )
                    }
                }

                else -> {
                    Spacer(Modifier.size(48.dp))
                }
            }
        },
        title = "安装应用",
        insideMargin = DpSize(16.dp, 16.dp),
        onDismissRequest = {
            showBottomSheet.value = !showBottomSheet.value
            scope.launch {
                delay(SHEET_ANIMATION_DURATION) // Wait for sheet animation
                if (viewModel.isDismissible) {
                    if (viewModel.disableNotificationOnDismiss) {
                        viewModel.dispatch(InstallerViewAction.Close)
                    } else {
                        viewModel.dispatch(InstallerViewAction.Background)
                    }
                }
            }
        }
    ) {
        MiuixSheetContent(installer = installer, viewModel = viewModel)
    }
}