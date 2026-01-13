package com.rosan.installer.ui.page.main.settings.preferred

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.rosan.installer.R
import com.rosan.installer.build.RsConfig
import com.rosan.installer.build.model.entity.Level
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.SettingsScreen
import com.rosan.installer.ui.page.main.widget.card.InfoTipCard
import com.rosan.installer.ui.page.main.widget.dialog.ErrorDisplayDialog
import com.rosan.installer.ui.page.main.widget.setting.AutoLockInstaller
import com.rosan.installer.ui.page.main.widget.setting.BottomSheetContent
import com.rosan.installer.ui.page.main.widget.setting.ClearCache
import com.rosan.installer.ui.page.main.widget.setting.DefaultInstaller
import com.rosan.installer.ui.page.main.widget.setting.DisableAdbVerify
import com.rosan.installer.ui.page.main.widget.setting.IgnoreBatteryOptimizationSetting
import com.rosan.installer.ui.page.main.widget.setting.SettingsAboutItemWidget
import com.rosan.installer.ui.page.main.widget.setting.SettingsNavigationItemWidget
import com.rosan.installer.ui.page.main.widget.setting.SplicedColumnGroup
import com.rosan.installer.ui.theme.none
import com.rosan.installer.util.OSUtils
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewPreferredPage(
    navController: NavController,
    viewModel: PreferredViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val state = viewModel.state
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.dispatch(PreferredViewAction.RefreshIgnoreBatteryOptimizationStatus)
        }
    }

    val revLevel = when (RsConfig.LEVEL) {
        Level.STABLE -> stringResource(id = R.string.stable)
        Level.PREVIEW -> stringResource(id = R.string.preview)
        Level.UNSTABLE -> stringResource(id = R.string.unstable)
    }

    var updateErrorInfo by remember {
        mutableStateOf<PreferredViewEvent.ShowInAppUpdateErrorDetail?>(
            null
        )
    }
    val snackBarHostState = remember { SnackbarHostState() }
    var errorDialogInfo by remember {
        mutableStateOf<PreferredViewEvent.ShowDefaultInstallerErrorDetail?>(
            null
        )
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            snackBarHostState.currentSnackbarData?.dismiss()
            when (event) {
                is PreferredViewEvent.ShowDefaultInstallerResult -> {
                    snackBarHostState.showSnackbar(event.message)
                }

                is PreferredViewEvent.ShowDefaultInstallerErrorDetail -> {
                    val snackbarResult = snackBarHostState.showSnackbar(
                        message = event.title,
                        actionLabel = context.getString(R.string.details),
                        duration = SnackbarDuration.Short
                    )
                    if (snackbarResult == SnackbarResult.ActionPerformed) {
                        errorDialogInfo = event
                    }
                }

                is PreferredViewEvent.ShowInAppUpdateErrorDetail -> {
                    updateErrorInfo = event
                }

                else -> null
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        contentWindowInsets = WindowInsets.none,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                windowInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = 12.dp)),
                title = { Text(text = stringResource(id = R.string.preferred)) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
    ) { paddingValues ->
        when (state.progress) {
            is PreferredViewState.Progress.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ContainedLoadingIndicator(
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                        Text(
                            text = stringResource(id = R.string.loading),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }

            is PreferredViewState.Progress.Loaded -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // --- Global Settings Group ---
                    item {
                        SplicedColumnGroup(
                            title = stringResource(R.string.personalization)
                        ) {
                            item {
                                SettingsNavigationItemWidget(
                                    icon = AppIcons.Theme,
                                    title = stringResource(R.string.theme_settings),
                                    description = stringResource(R.string.theme_settings_desc),
                                    onClick = { navController.navigate(SettingsScreen.Theme.route) }
                                )
                            }
                            item {
                                SettingsNavigationItemWidget(
                                    icon = AppIcons.InstallMode,
                                    title = stringResource(R.string.installer_settings),
                                    description = stringResource(R.string.installer_settings_desc),
                                    onClick = { navController.navigate(SettingsScreen.InstallerGlobal.route) }
                                )
                            }
                            item {
                                SettingsNavigationItemWidget(
                                    icon = AppIcons.Delete,
                                    title = stringResource(R.string.uninstaller_settings),
                                    description = stringResource(R.string.uninstaller_settings_desc),
                                    onClick = { navController.navigate(SettingsScreen.UninstallerGlobal.route) }
                                )
                            }
                        }
                    }

                    if (viewModel.state.authorizer == ConfigEntity.Authorizer.None)
                        item {
                            val tip = if (OSUtils.isSystemApp) stringResource(R.string.config_authorizer_none_system_app_tips)
                            else stringResource(R.string.config_authorizer_none_tips)
                            InfoTipCard(text = tip)
                        }

                    // --- Basic Settings Group ---
                    item {
                        SplicedColumnGroup(
                            title = stringResource(R.string.basic)
                        ) {
                            item {
                                DisableAdbVerify(
                                    checked = !state.adbVerifyEnabled,
                                    isError = state.authorizer == ConfigEntity.Authorizer.Dhizuku,
                                    enabled = state.authorizer != ConfigEntity.Authorizer.Dhizuku &&
                                            state.authorizer != ConfigEntity.Authorizer.None,
                                    onCheckedChange = { isDisabled ->
                                        viewModel.dispatch(PreferredViewAction.SetAdbVerifyEnabledState(!isDisabled))
                                    }
                                )
                            }
                            item {
                                IgnoreBatteryOptimizationSetting(
                                    checked = state.isIgnoringBatteryOptimizations,
                                    enabled = !state.isIgnoringBatteryOptimizations,
                                ) { viewModel.dispatch(PreferredViewAction.RequestIgnoreBatteryOptimization) }
                            }
                            item {
                                AutoLockInstaller(
                                    checked = state.autoLockInstaller,
                                    enabled = state.authorizer != ConfigEntity.Authorizer.None
                                ) { viewModel.dispatch(PreferredViewAction.ChangeAutoLockInstaller(!state.autoLockInstaller)) }
                            }
                            item {
                                DefaultInstaller(
                                    lock = true,
                                    enabled = state.authorizer != ConfigEntity.Authorizer.None
                                ) { viewModel.dispatch(PreferredViewAction.SetDefaultInstaller(true)) }
                            }
                            item {
                                DefaultInstaller(
                                    lock = false,
                                    enabled = state.authorizer != ConfigEntity.Authorizer.None
                                ) { viewModel.dispatch(PreferredViewAction.SetDefaultInstaller(false)) }
                            }
                            item { ClearCache() }
                        }
                    }
                    // --- Other Settings Group ---
                    item {
                        SplicedColumnGroup(
                            title = stringResource(R.string.other)
                        ) {
                            item {
                                SettingsAboutItemWidget(
                                    imageVector = AppIcons.Lab,
                                    headlineContentText = stringResource(R.string.lab),
                                    supportingContentText = stringResource(R.string.lab_desc),
                                    onClick = { navController.navigate(SettingsScreen.Lab.route) }
                                )
                            }
                            item {
                                SettingsAboutItemWidget(
                                    imageVector = AppIcons.Info,
                                    headlineContentText = stringResource(R.string.about_detail),
                                    supportingContentText = "$revLevel ${RsConfig.VERSION_NAME}",
                                    onClick = { navController.navigate(SettingsScreen.About.route) }
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.size(6.dp)) }
                }
            }
        }
    }

    // Dialogs and bottom sheets stay outside the main Box
    errorDialogInfo?.let { dialogInfo ->
        ErrorDisplayDialog(
            exception = dialogInfo.exception,
            onDismissRequest = { errorDialogInfo = null },
            onRetry = {
                errorDialogInfo = null
                viewModel.dispatch(dialogInfo.retryAction)
            },
            title = dialogInfo.title
        )
    }

    updateErrorInfo?.let { info ->
        ErrorDisplayDialog(
            title = info.title,
            exception = info.exception,
            onDismissRequest = { updateErrorInfo = null }
        )
    }
}