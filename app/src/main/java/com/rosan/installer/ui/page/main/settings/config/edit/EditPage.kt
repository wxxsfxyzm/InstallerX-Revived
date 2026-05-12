// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.config.edit

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.BiometricAuthMode
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.main.widget.card.InfoTipCard
import com.rosan.installer.ui.page.main.widget.dialog.UnsavedChangesDialog
import com.rosan.installer.ui.page.main.widget.setting.AppBackButton
import com.rosan.installer.ui.page.main.widget.setting.SegmentedColumn
import com.rosan.installer.ui.page.main.widget.snackbar.SwipeableSnackbarHost
import com.rosan.installer.ui.page.main.widget.util.EditEventCollector
import com.rosan.installer.ui.theme.getMaterial3AppBarColor
import com.rosan.installer.ui.theme.installerMaterial3BlurEffect
import com.rosan.installer.ui.theme.rememberMaterial3BlurBackdrop
import com.rosan.installer.ui.util.isNoneActive
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import top.yukonga.miuix.kmp.blur.layerBackdrop

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EditPage(
    id: Long? = null,
    viewModel: EditViewModel = koinViewModel { parametersOf(id) },
    useBlur: Boolean
) {
    val navigator = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dispatch = viewModel::dispatch

    val listState = rememberLazyListState()
    val snackBarHostState = remember { SnackbarHostState() }
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    var showUnsavedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        topAppBarState.heightOffset = topAppBarState.heightOffsetLimit
    }

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
        errorMessages = state.activeErrorResIds.map { stringResource(it) }
    )

    val shouldInterceptBackPress = state.hasUnsavedChanges || state.hasErrors

    BackHandler(enabled = shouldInterceptBackPress) {
        showUnsavedDialog = true
    }

    EditEventCollector(viewModel, snackBarHostState)

    val focusManager = LocalFocusManager.current
    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    val backdrop = rememberMaterial3BlurBackdrop(useBlur)

    val isHigherThanT = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val isHigherThanU = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                modifier = Modifier.installerMaterial3BlurEffect(backdrop),
                windowInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = 12.dp)),
                title = { Text(text = stringResource(id = if (id == null) R.string.add else R.string.update)) },
                navigationIcon = {
                    Row {
                        AppBackButton(
                            onClick = { navigator.pop() },
                            icon = Icons.AutoMirrored.TwoTone.ArrowBack,
                            modifier = Modifier.size(36.dp),
                            containerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        )
                        Spacer(modifier = Modifier.size(16.dp))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backdrop.getMaterial3AppBarColor(),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    scrolledContainerColor = backdrop.getMaterial3AppBarColor()
                )
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
                onClick = { dispatch(EditViewAction.SaveData) }
            )
        },
        snackbarHost = {
            SwipeableSnackbarHost(
                hostState = snackBarHostState,
                snackbar = { SnackbarHost(hostState = snackBarHostState) }
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
            contentPadding = PaddingValues(
                start = horizontalSafeInsets.calculateStartPadding(layoutDirection),
                top = paddingValues.calculateTopPadding(),
                end = horizontalSafeInsets.calculateEndPadding(layoutDirection),
                bottom = paddingValues.calculateBottomPadding() + 80.dp
            ),
            state = listState,
        ) {
            // --- Group 1: Main Settings ---
            item {
                SegmentedColumn(
                    title = stringResource(R.string.config_label_main_settings)
                ) {
                    item { DataNameWidget(state, dispatch, { DataDescriptionWidget(state, dispatch) }) }
                    dataAuthorizerWidget(state, dispatch)
                    item { DataInstallModeWidget(state, dispatch) }
                    if (state.globalInstallerBiometricAuthMode == BiometricAuthMode.FollowConfig)
                        item { DataRequireBiometricAuthWidget(state, dispatch) }
                    item { DataShowToastWidget(state, dispatch) }
                }
            }

            if (isNoneActive(stateAuthorizer, globalAuthorizer)) {
                item { InfoTipCard(text = stringResource(R.string.config_authorizer_none_tips)) }
            }

            // --- Group 2: Installer Settings ---
            item {
                SegmentedColumn(
                    title = stringResource(R.string.config_label_installer_settings)
                ) {
                    dataUserWidget(state, dispatch)
                    dataInstallReasonWidget(state, dispatch)
                    if (isHigherThanT) dataPackageSourceWidget(state, dispatch)
                    if (state.isCustomInstallRequesterEnabled) dataInstallRequesterWidget(state, dispatch)
                    dataDeclareInstallerWidget(state, dispatch)
                    dataManualDexoptWidget(state, dispatch)
                    dataAutoDeleteWidget(state, dispatch)
                    item { DisplaySdkWidget(state, dispatch) }
                    item { DisplaySizeWidget(state, dispatch) }
                }
            }
            // --- Group 3: Install Options ---
            item {
                SegmentedColumn(
                    title = stringResource(R.string.config_label_install_options)
                ) {
                    item { DataForAllUserWidget(state, dispatch) }
                    item { DataAllowTestOnlyWidget(state, dispatch) }
                    item { DataAllowDowngradeWidget(state, dispatch) }
                    item { DataAllowSigMismatchWidget(state, dispatch) }
                    item { DataAllowSigUnknownWidget(state, dispatch) }
                    if (isHigherThanU) item { DataBypassLowTargetSdkWidget(state, dispatch) }
                    item { DataAllowAllRequestedPermissionsWidget(state, dispatch) }
                    if (isHigherThanU) item { DataRequestUpdateOwnershipWidget(state, dispatch) }
                }
            }

            // --- Group 4: Preferences Settings ---
            item {
                SegmentedColumn(
                    title = stringResource(R.string.config_label_preferences)
                ) {
                    item { DataSplitChooseAllWidget(state, dispatch) }
                    item { DataApkChooseAllWidget(state, dispatch) }
                }
            }

            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}
