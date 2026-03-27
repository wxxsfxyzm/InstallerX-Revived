// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.settings.config.edit

import android.annotation.SuppressLint
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rosan.installer.R
import com.rosan.installer.ui.page.main.settings.config.edit.EditViewAction
import com.rosan.installer.ui.page.main.settings.config.edit.EditViewEvent
import com.rosan.installer.ui.page.main.settings.config.edit.EditViewModel
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataAllowAllRequestedPermissionsWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataAllowDowngradeWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataAllowTestOnlyWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataApkChooseAllWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataAuthorizerWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataAutoDeleteWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataBypassLowTargetSdkWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataCustomizeAuthorizerWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataDeclareInstallerWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataDescriptionWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataForAllUserWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataInstallModeWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataInstallRequesterWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataManualDexoptWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataNameWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataPackageSourceWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataSplitChooseAllWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataUserWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDisplaySdkWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDisplaySizeWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixInstallReasonWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixRequestUpdateOwnershipWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixSettingsTipCard
import com.rosan.installer.ui.page.miuix.widgets.MiuixShowToastWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixUnsavedChangesDialog
import com.rosan.installer.ui.theme.getMiuixAppBarColor
import com.rosan.installer.ui.theme.installerHazeEffect
import com.rosan.installer.ui.theme.rememberMiuixHazeStyle
import com.rosan.installer.ui.util.isNoneActive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun MiuixEditPage(
    navController: NavController,
    id: Long? = null,
    viewModel: EditViewModel = koinViewModel { parametersOf(id) },
    useBlur: Boolean
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dispatch = viewModel::dispatch

    val snackBarHostState = remember { SnackbarHostState() }
    val scrollBehavior = MiuixScrollBehavior()
    val hazeState = if (useBlur) remember { HazeState() } else null
    val hazeStyle = rememberMiuixHazeStyle()
    val showUnsavedDialogState = remember { mutableStateOf(false) }

    MiuixUnsavedChangesDialog(
        showState = showUnsavedDialogState,
        onDismiss = {
            showUnsavedDialogState.value = false
        },
        onConfirm = {
            showUnsavedDialogState.value = false
            navController.navigateUp()
        },
        errorMessages = state.activeErrorResIds.map { stringResource(it) }
    )

    val shouldInterceptBackPress = state.hasUnsavedChanges || state.hasErrors

    BackHandler(enabled = shouldInterceptBackPress) {
        showUnsavedDialogState.value = true
    }

    val stateAuthorizer = state.data.authorizer
    val globalAuthorizer = state.globalAuthorizer

    val unknownErrorString = stringResource(R.string.installer_unknown_error)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is EditViewEvent.SnackBar -> {
                    // Resolve priority:
                    // 1. Specific Resource ID
                    // 2. Explicit String message
                    // 3. Localized generic fallback
                    val snackBarText = event.messageResId?.let { @SuppressLint("LocalContextGetResourceValueCall") context.getString(it) }
                        ?: event.message
                        ?: unknownErrorString

                    snackBarHostState.showSnackbar(
                        message = snackBarText,
                        withDismissAction = true,
                    )
                }

                is EditViewEvent.Saved -> {
                    navController.navigateUp()
                }
            }
        }
    }

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                modifier = Modifier.installerHazeEffect(hazeState, hazeStyle),
                color = hazeState.getMiuixAppBarColor(),
                scrollBehavior = scrollBehavior,
                title = stringResource(id = if (id == null) R.string.add else R.string.update),
                navigationIcon = {
                    MiuixBackButton(
                        modifier = Modifier.padding(start = 16.dp),
                        icon = MiuixIcons.Regular.Close,
                        onClick = { navController.navigateUp() }
                    )
                },
                actions = {
                    IconButton(
                        modifier = Modifier.padding(end = 16.dp),
                        onClick = { dispatch(EditViewAction.SaveData) },
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Regular.Ok,
                            contentDescription = stringResource(R.string.save)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(state = snackBarHostState) },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(hazeState?.let { Modifier.hazeSource(it) } ?: Modifier)
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                start = horizontalSafeInsets.calculateStartPadding(layoutDirection),
                top = paddingValues.calculateTopPadding(),
                end = horizontalSafeInsets.calculateEndPadding(layoutDirection)
            ),
            overscrollEffect = null,
        ) {
            item { Spacer(modifier = Modifier.size(12.dp)) }
            item { MiuixDataNameWidget(state = state, dispatch = dispatch) }
            item { MiuixDataDescriptionWidget(state = state, dispatch = dispatch) }
            item { SmallTitle(stringResource(R.string.config)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixDataAuthorizerWidget(state = state, dispatch = dispatch)
                    MiuixDataCustomizeAuthorizerWidget(state = state, dispatch = dispatch)
                    MiuixDataInstallModeWidget(state = state, dispatch = dispatch)
                    MiuixShowToastWidget(state = state, dispatch = dispatch)
                }
            }
            if (isNoneActive(stateAuthorizer, globalAuthorizer))
                item { MiuixSettingsTipCard(stringResource(R.string.config_authorizer_none_tips)) }
            item { SmallTitle(stringResource(R.string.config_label_installer_settings)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixDataUserWidget(state = state, dispatch = dispatch)
                    MiuixInstallReasonWidget(state = state, dispatch = dispatch)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        MiuixDataPackageSourceWidget(state = state, dispatch = dispatch)
                    if (state.isCustomInstallRequesterEnabled)
                        MiuixDataInstallRequesterWidget(state = state, dispatch = dispatch)
                    MiuixDataDeclareInstallerWidget(state = state, dispatch = dispatch)
                    MiuixDataManualDexoptWidget(state = state, dispatch = dispatch)
                    MiuixDataAutoDeleteWidget(state = state, dispatch = dispatch)
                    MiuixDisplaySdkWidget(state = state, dispatch = dispatch)
                    MiuixDisplaySizeWidget(state = state, dispatch = dispatch)
                }
            }
            item { SmallTitle(stringResource(R.string.config_label_install_options)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 16.dp)
                ) {
                    MiuixDataForAllUserWidget(state = state, dispatch = dispatch)
                    MiuixDataAllowTestOnlyWidget(state = state, dispatch = dispatch)
                    MiuixDataAllowDowngradeWidget(state = state, dispatch = dispatch)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                        MiuixDataBypassLowTargetSdkWidget(state = state, dispatch = dispatch)
                    MiuixDataAllowAllRequestedPermissionsWidget(state = state, dispatch = dispatch)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                        MiuixRequestUpdateOwnershipWidget(state = state, dispatch = dispatch)
                }
            }
            item { SmallTitle(stringResource(R.string.config_label_preferences)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 16.dp)
                ) {
                    MiuixDataSplitChooseAllWidget(state = state, dispatch = dispatch)
                    MiuixDataApkChooseAllWidget(state = state, dispatch = dispatch)
                }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}
