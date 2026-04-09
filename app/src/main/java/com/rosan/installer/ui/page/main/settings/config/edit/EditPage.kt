// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.config.edit

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.main.widget.card.InfoTipCard
import com.rosan.installer.ui.page.main.widget.dialog.UnsavedChangesDialog
import com.rosan.installer.ui.page.main.widget.setting.AppBackButton
import com.rosan.installer.ui.page.main.widget.setting.LabelWidget
import com.rosan.installer.ui.page.main.widget.util.EditEventCollector
import com.rosan.installer.ui.theme.none
import com.rosan.installer.ui.util.isNoneActive
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EditPage(
    id: Long? = null,
    viewModel: EditViewModel = koinViewModel { parametersOf(id) }
) {
    val navigator = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dispatch = viewModel::dispatch
    val listState = rememberLazyListState()
    val snackBarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showUnsavedDialog by remember { mutableStateOf(false) }

    val stateAuthorizer = state.data.authorizer
    val globalAuthorizer = state.globalAuthorizer

    UnsavedChangesDialog(
        show = showUnsavedDialog,
        onDismiss = {
            showUnsavedDialog = false
        },
        onConfirm = {
            showUnsavedDialog = false
            navigator.pop()
        },
        // Pass the list of active error messages from the ViewModel.
        errorMessages = state.activeErrorResIds.map { stringResource(it) }
    )
    // The condition for interception is now expanded to include errors.
    // If there are unsaved changes OR if there are validation errors, we should intercept.
    val shouldInterceptBackPress = state.hasUnsavedChanges || state.hasErrors

    // Use this new combined condition for the BackHandler.
    BackHandler(enabled = shouldInterceptBackPress) {
        showUnsavedDialog = true
    }

    EditEventCollector(viewModel, snackBarHostState)

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.none,
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(id = if (id == null) R.string.add else R.string.update)) },
                navigationIcon = { AppBackButton(onClick = { navigator.pop() }) },
            )
        },
        floatingActionButton = {
            SmallExtendedFloatingActionButton(
                modifier = Modifier.padding(
                    end = horizontalSafeInsets.calculateEndPadding(layoutDirection),
                    bottom = 16.dp
                ),
                icon = {
                    Icon(
                        imageVector = AppIcons.Save,
                        contentDescription = stringResource(R.string.save)
                    )
                },
                text = { Text(stringResource(R.string.save)) },
                onClick = { viewModel.dispatch(EditViewAction.SaveData) }
            )
        },
        snackbarHost = {
            val state = rememberSwipeToDismissBoxState()
            LaunchedEffect(snackBarHostState.currentSnackbarData) {
                state.snapTo(SwipeToDismissBoxValue.Settled)
            }

            SwipeToDismissBox(
                state = state,
                backgroundContent = {},
                onDismiss = {
                    snackBarHostState.currentSnackbarData?.dismiss()
                }
            ) {
                SnackbarHost(hostState = snackBarHostState)
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(
                start = horizontalSafeInsets.calculateStartPadding(layoutDirection),
                top = paddingValues.calculateTopPadding(),
                end = horizontalSafeInsets.calculateEndPadding(layoutDirection),
                bottom = paddingValues.calculateBottomPadding() + 80.dp
            )
        ) {
            item { DataNameWidget(state = state, dispatch = dispatch) }
            item { DataDescriptionWidget(state = state, dispatch = dispatch) }
            item { DataAuthorizerWidget(state = state, dispatch = dispatch) }
            item { DataCustomizeAuthorizerWidget(state = state, dispatch = dispatch) }
            item { DataInstallModeWidget(state = state, dispatch = dispatch) }
            item { DataShowToastWidget(state = state, dispatch = dispatch, isM3E = false) }

            if (isNoneActive(stateAuthorizer, globalAuthorizer))
                item { InfoTipCard(text = stringResource(R.string.config_authorizer_none_tips)) }

            item { LabelWidget(label = stringResource(R.string.config_label_installer_settings)) }
            item { DataUserWidget(state = state, dispatch = dispatch, isM3E = false) }
            item { DataInstallReasonWidget(state = state, dispatch = dispatch, isM3E = false) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                item { DataPackageSourceWidget(state = state, dispatch = dispatch, isM3E = false) }
            if (state.isCustomInstallRequesterEnabled)
                item { DataInstallRequesterWidget(state = state, dispatch = dispatch, isM3E = false) }
            item { DataDeclareInstallerWidget(state = state, dispatch = dispatch, isM3E = false) }
            item { DataManualDexoptWidget(state = state, dispatch = dispatch, isM3E = false) }
            item { DataAutoDeleteWidget(state = state, dispatch = dispatch, isM3E = false) }
            item { DisplaySdkWidget(state = state, dispatch = dispatch, isM3E = false) }
            item { DisplaySizeWidget(state = state, dispatch = dispatch, isM3E = false) }

            item { LabelWidget(label = stringResource(R.string.config_label_install_options)) }
            item { DataForAllUserWidget(state = state, dispatch = dispatch, isM3E = false) }
            item { DataAllowTestOnlyWidget(state = state, dispatch = dispatch, isM3E = false) }
            item { DataAllowDowngradeWidget(state = state, dispatch = dispatch, isM3E = false) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                item { DataBypassLowTargetSdkWidget(state = state, dispatch = dispatch, isM3E = false) }
            item { DataAllowAllRequestedPermissionsWidget(state = state, dispatch = dispatch, isM3E = false) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                item { DataRequestUpdateOwnershipWidget(state = state, dispatch = dispatch, isM3E = false) }

            item { LabelWidget(label = stringResource(R.string.config_label_preferences)) }
            item { DataSplitChooseAllWidget(state = state, dispatch = dispatch, isM3E = false) }
            item { DataApkChooseAllWidget(state = state, dispatch = dispatch, isM3E = false) }

            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}