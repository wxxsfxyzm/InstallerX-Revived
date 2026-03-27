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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.widget.card.InfoTipCard
import com.rosan.installer.ui.page.main.widget.dialog.UnsavedChangesDialog
import com.rosan.installer.ui.page.main.widget.setting.AppBackButton
import com.rosan.installer.ui.page.main.widget.setting.DataAllowAllRequestedPermissionsWidget
import com.rosan.installer.ui.page.main.widget.setting.DataAllowDowngradeWidget
import com.rosan.installer.ui.page.main.widget.setting.DataAllowTestOnlyWidget
import com.rosan.installer.ui.page.main.widget.setting.DataApkChooseAllWidget
import com.rosan.installer.ui.page.main.widget.setting.DataAuthorizerWidget
import com.rosan.installer.ui.page.main.widget.setting.DataAutoDeleteWidget
import com.rosan.installer.ui.page.main.widget.setting.DataBypassLowTargetSdkWidget
import com.rosan.installer.ui.page.main.widget.setting.DataCustomizeAuthorizerWidget
import com.rosan.installer.ui.page.main.widget.setting.DataDeclareInstallerWidget
import com.rosan.installer.ui.page.main.widget.setting.DataDescriptionWidget
import com.rosan.installer.ui.page.main.widget.setting.DataForAllUserWidget
import com.rosan.installer.ui.page.main.widget.setting.DataInstallModeWidget
import com.rosan.installer.ui.page.main.widget.setting.DataInstallReasonWidget
import com.rosan.installer.ui.page.main.widget.setting.DataInstallRequesterWidget
import com.rosan.installer.ui.page.main.widget.setting.DataManualDexoptWidget
import com.rosan.installer.ui.page.main.widget.setting.DataNameWidget
import com.rosan.installer.ui.page.main.widget.setting.DataPackageSourceWidget
import com.rosan.installer.ui.page.main.widget.setting.DataRequestUpdateOwnershipWidget
import com.rosan.installer.ui.page.main.widget.setting.DataShowToastWidget
import com.rosan.installer.ui.page.main.widget.setting.DataSplitChooseAllWidget
import com.rosan.installer.ui.page.main.widget.setting.DataUserWidget
import com.rosan.installer.ui.page.main.widget.setting.DisplaySdkWidget
import com.rosan.installer.ui.page.main.widget.setting.DisplaySizeWidget
import com.rosan.installer.ui.page.main.widget.setting.SplicedColumnGroup
import com.rosan.installer.ui.page.main.widget.util.EditEventCollector
import com.rosan.installer.ui.theme.getM3TopBarColor
import com.rosan.installer.ui.theme.installerHazeEffect
import com.rosan.installer.ui.theme.none
import com.rosan.installer.ui.theme.rememberMaterial3HazeStyle
import com.rosan.installer.ui.util.isNoneActive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewEditPage(
    navController: NavController,
    id: Long? = null,
    viewModel: EditViewModel = koinViewModel { parametersOf(id) },
    useBlur: Boolean
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dispatch = viewModel::dispatch

    val listState = rememberLazyListState()
    val snackBarHostState = remember { SnackbarHostState() }
    val topAppBarState = rememberTopAppBarState()
    val hazeState = if (useBlur) remember { HazeState() } else null
    val hazeStyle = rememberMaterial3HazeStyle()
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
            navController.navigateUp()
        },
        errorMessages = state.activeErrorResIds.map { stringResource(it) }
    )

    val shouldInterceptBackPress = state.hasUnsavedChanges || state.hasErrors

    BackHandler(enabled = shouldInterceptBackPress) {
        showUnsavedDialog = true
    }

    EditEventCollector(viewModel, navController, snackBarHostState)

    val focusManager = LocalFocusManager.current
    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

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
        contentWindowInsets = WindowInsets.none,
        topBar = {
            LargeFlexibleTopAppBar(
                modifier = Modifier.installerHazeEffect(hazeState, hazeStyle),
                windowInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = 12.dp)),
                title = { Text(text = stringResource(id = if (id == null) R.string.add else R.string.update)) },
                navigationIcon = {
                    Row {
                        AppBackButton(
                            onClick = { navController.navigateUp() },
                            icon = Icons.AutoMirrored.TwoTone.ArrowBack,
                            modifier = Modifier.size(36.dp),
                            containerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        )
                        Spacer(modifier = Modifier.size(16.dp))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = hazeState.getM3TopBarColor(),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    scrolledContainerColor = hazeState.getM3TopBarColor()
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
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(hazeState?.let { Modifier.hazeSource(it) } ?: Modifier),
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
                SplicedColumnGroup(
                    title = stringResource(R.string.config_label_main_settings)
                ) {
                    item { DataNameWidget(state, dispatch, { DataDescriptionWidget(state, dispatch) }) }
                    item { DataAuthorizerWidget(state, dispatch) }
                    item(visible = state.data.authorizerCustomize) {
                        DataCustomizeAuthorizerWidget(state, dispatch)
                    }
                    item { DataInstallModeWidget(state, dispatch) }
                    item { DataShowToastWidget(state, dispatch) }
                }
            }

            if (isNoneActive(stateAuthorizer, globalAuthorizer)) {
                item { InfoTipCard(text = stringResource(R.string.config_authorizer_none_tips)) }
            }

            // --- Group 2: Installer Settings ---
            item {
                SplicedColumnGroup(
                    title = stringResource(R.string.config_label_installer_settings)
                ) {
                    item { DataUserWidget(state, dispatch) }
                    item { DataInstallReasonWidget(state, dispatch) }
                    item(visible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        DataPackageSourceWidget(state, dispatch)
                    }
                    item(visible = state.isCustomInstallRequesterEnabled) {
                        DataInstallRequesterWidget(state, dispatch)
                    }
                    item { DataDeclareInstallerWidget(state, dispatch) }
                    item { DataManualDexoptWidget(state, dispatch) }
                    item { DataAutoDeleteWidget(state, dispatch) }
                    item { DisplaySdkWidget(state, dispatch) }
                    item { DisplaySizeWidget(state, dispatch) }
                }
            }

            // --- Group 3: Install Options ---
            item {
                SplicedColumnGroup(
                    title = stringResource(R.string.config_label_install_options)
                ) {
                    item { DataForAllUserWidget(state, dispatch) }
                    item { DataAllowTestOnlyWidget(state, dispatch) }
                    item { DataAllowDowngradeWidget(state, dispatch) }
                    item(visible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        DataBypassLowTargetSdkWidget(state, dispatch)
                    }
                    item { DataAllowAllRequestedPermissionsWidget(state, dispatch) }
                    item(visible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        DataRequestUpdateOwnershipWidget(state, dispatch)
                    }
                }
            }

            // --- Group 4: Preferences Settings ---
            item {
                SplicedColumnGroup(
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
