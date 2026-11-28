// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.settings.config.edit

import android.annotation.SuppressLint
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.Dialog
import com.itos.coui.icons.CouiIcons
import com.itos.coui.layout.Divider
import com.itos.coui.layout.Group
import com.itos.coui.layout.Scaffold
import com.itos.coui.menu.rememberCouiWindowBackdrop
import com.itos.coui.theme.CouiTheme
import com.itos.coui.topappbar.CouiTopAppBarDefaults
import com.itos.coui.topappbar.LargeTopAppBar
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.config.BiometricAuthMode
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.coui.widgets.CouiBackButton
import com.rosan.installer.ui.page.coui.widgets.CouiSettingsTipCard
import com.rosan.installer.ui.page.coui.widgets.CouiTopBarIconButton
import com.rosan.installer.ui.page.coui.widgets.CouiUnsavedChangesDialog
import com.rosan.installer.ui.page.coui.widgets.rememberCouiDialogMaterial
import com.rosan.installer.ui.page.main.settings.config.edit.EditViewAction
import com.rosan.installer.ui.page.main.settings.config.edit.EditViewEvent
import com.rosan.installer.ui.page.main.settings.config.edit.EditViewModel
import com.rosan.installer.ui.util.isNoneActive
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CouiEditPage(id: Long? = null, viewModel: EditViewModel = koinViewModel { parametersOf(id) }, useBlur: Boolean) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dispatch = viewModel::dispatch

    val snackBarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val scrollBehavior = CouiTopAppBarDefaults.exitUntilCollapsedScrollBehavior {
        listState.canScrollForward || listState.canScrollBackward
    }
    val density = LocalDensity.current
    val isWindowFocused = LocalWindowInfo.current.isWindowFocused

    val showUnsavedDialogState = remember { mutableStateOf(false) }
    val showAutoApproveSessionDialogState = remember { mutableStateOf(false) }

    CouiUnsavedChangesDialog(
        showState = showUnsavedDialogState,
        onDismiss = { showUnsavedDialogState.value = false },
        onConfirm = {
            showUnsavedDialogState.value = false
            navigator.pop()
        },
        errorMessages = state.activeErrorResIds.map { stringResource(it) },
    )

    val backdrop = rememberCouiWindowBackdrop()
    val dialogMaterial = rememberCouiDialogMaterial()
    Dialog(
        visible = showAutoApproveSessionDialogState.value,
        onDismissRequest = { showAutoApproveSessionDialogState.value = false },
        title = stringResource(R.string.warning),
        message = stringResource(R.string.config_auto_approve_session_warning),
        confirmAction = CouiDialogAction(
            text = stringResource(R.string.confirm),
            onClick = {
                showAutoApproveSessionDialogState.value = false
                dispatch(EditViewAction.ChangeDataAutoApproveSession(true))
            },
            style = CouiDialogActionStyle.Primary,
        ),
        dismissAction = CouiDialogAction(
            text = stringResource(R.string.cancel),
            onClick = { showAutoApproveSessionDialogState.value = false },
        ),
        material = dialogMaterial,
        backdrop = backdrop,
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
                    val snackBarText =
                        event.messageResId?.let {
                            @SuppressLint("LocalContextGetResourceValueCall")
                            context.getString(it)
                        }
                            ?: event.message
                            ?: unknownErrorString

                    snackBarHostState.showSnackbar(
                        message = snackBarText,
                        withDismissAction = true,
                    )
                }

                is EditViewEvent.Saved -> navigator.pop()
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .then(if (isWindowFocused) Modifier.imePadding() else Modifier)
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CouiTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(id?.let { R.string.update } ?: R.string.add)) },
                navigationIcon = {
                    CouiBackButton(
                        icon = CouiIcons.Filled.Close,
                        onClick = {
                            if (shouldInterceptBackPress) {
                                showUnsavedDialogState.value = true
                            } else {
                                navigator.pop()
                            }
                        },
                    )
                },
                actions = {
                    CouiTopBarIconButton(onClick = { dispatch(EditViewAction.SaveData) }) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = stringResource(R.string.save),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                useCouiBlur = useBlur,
            )
        },
    ) { insets ->
        val contentPadding = remember(scrollBehavior.state, insets, density) {
            scrollBehavior.state.contentPadding(insets, density)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(insets),
                contentPadding = PaddingValues(
                    top = contentPadding.calculateTopPadding() + 8.dp,
                    bottom = contentPadding.calculateBottomPadding() + 20.dp,
                ),
            ) {
                item { CouiDataNameWidget(state = state, dispatch = dispatch) }
                item { CouiDataDescriptionWidget(state = state, dispatch = dispatch) }

                item {
                    Group(title = stringResource(R.string.config)) {
                        CouiDataAuthorizerWidget(state = state, dispatch = dispatch)
                        CouiDataCustomizeAuthorizerWidget(state = state, dispatch = dispatch)
                        Divider()
                        CouiDataInstallModeWidget(state = state, dispatch = dispatch)
                        Divider()
                        CouiDataAutoApproveSessionWidget(
                            state = state,
                            dispatch = dispatch,
                            onEnableRequest = { showAutoApproveSessionDialogState.value = true },
                        )
                        if (state.globalInstallerBiometricAuthMode == BiometricAuthMode.FollowConfig) {
                            Divider()
                            CouiDataRequireBiometricAuthWidget(state = state, dispatch = dispatch)
                        }
                        Divider()
                        CouiToastModeWidget(state = state, dispatch = dispatch)
                    }
                }

                if (isNoneActive(stateAuthorizer, globalAuthorizer)) {
                    item { CouiSettingsTipCard(stringResource(R.string.config_authorizer_none_tips)) }
                }

                item {
                    Group(title = stringResource(R.string.config_label_installer_settings)) {
                        CouiDataUserWidget(state = state, dispatch = dispatch)
                        Divider()
                        CouiInstallReasonWidget(state = state, dispatch = dispatch)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Divider()
                            CouiDataPackageSourceWidget(state = state, dispatch = dispatch)
                        }
                        Divider()
                        CouiDataInstallRequesterWidget(state = state, dispatch = dispatch)
                        Divider()
                        CouiDataDeclareInstallerWidget(state = state, dispatch = dispatch)
                        Divider()
                        CouiDataManualDexoptWidget(state = state, dispatch = dispatch)
                        Divider()
                        CouiDataAutoDeleteWidget(state = state, dispatch = dispatch)
                        Divider()
                        CouiDisplaySdkWidget(state = state, dispatch = dispatch)
                        Divider()
                        CouiDisplaySizeWidget(state = state, dispatch = dispatch)
                    }
                }

                item {
                    Group(title = stringResource(R.string.config_label_install_options)) {
                        CouiDataForAllUserWidget(state = state, dispatch = dispatch)
                        Divider()
                        CouiDataAllowTestOnlyWidget(state = state, dispatch = dispatch)
                        Divider()
                        CouiDataAllowDowngradeWidget(state = state, dispatch = dispatch)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            Divider()
                            CouiDataBypassLowTargetSdkWidget(state = state, dispatch = dispatch)
                        }
                        Divider()
                        CouiDataAllowAllRequestedPermissionsWidget(state = state, dispatch = dispatch)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            Divider()
                            CouiRequestUpdateOwnershipWidget(state = state, dispatch = dispatch)
                        }
                    }
                }

                item {
                    Group(title = stringResource(R.string.config_label_preferences)) {
                        CouiDataSplitChooseAllWidget(state = state, dispatch = dispatch)
                        Divider()
                        CouiDataApkChooseAllWidget(state = state, dispatch = dispatch)
                        if (state.checkAppSignature) {
                            Divider()
                            CouiDataAllowSigMismatchWidget(state = state, dispatch = dispatch)
                            Divider()
                            CouiDataAllowSigUnknownWidget(state = state, dispatch = dispatch)
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = contentPadding.calculateBottomPadding() + 8.dp),
            )
        }
    }
}
