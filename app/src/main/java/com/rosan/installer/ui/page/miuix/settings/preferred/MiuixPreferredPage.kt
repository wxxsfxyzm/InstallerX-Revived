package com.rosan.installer.ui.page.miuix.settings.preferred

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import com.rosan.installer.build.Level
import com.rosan.installer.build.RsConfig
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewAction
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewEvent
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewState
import com.rosan.installer.ui.page.main.widget.dialog.ErrorDisplayDialog
import com.rosan.installer.ui.page.miuix.settings.MiuixSettingsScreen
import com.rosan.installer.ui.page.miuix.widgets.MiuixAutoLockInstaller
import com.rosan.installer.ui.page.miuix.widgets.MiuixClearCache
import com.rosan.installer.ui.page.miuix.widgets.MiuixDefaultInstaller
import com.rosan.installer.ui.page.miuix.widgets.MiuixDisableAdbVerify
import com.rosan.installer.ui.page.miuix.widgets.MiuixIgnoreBatteryOptimizationSetting
import com.rosan.installer.ui.page.miuix.widgets.MiuixNavigationItemWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixNoneInstallerTipCard
import com.rosan.installer.ui.page.miuix.widgets.MiuixSettingsAboutItemWidget
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MiuixPreferredPage(
    navController: NavController,
    viewModel: PreferredViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val state = viewModel.state
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

    val scrollBehavior = MiuixScrollBehavior()
    val snackBarHostState = remember { SnackbarHostState() }
    var errorDialogInfo by remember { mutableStateOf<PreferredViewEvent.ShowErrorDialog?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            snackBarHostState.currentSnackbarData?.dismiss()
            when (event) {
                is PreferredViewEvent.ShowSnackbar -> {
                    snackBarHostState.showSnackbar(event.message)
                }

                is PreferredViewEvent.ShowErrorDialog -> {
                    val snackbarResult = snackBarHostState.showSnackbar(
                        message = event.title,
                        actionLabel = context.getString(R.string.details),
                        duration = SnackbarDuration.Short
                    )
                    if (snackbarResult == SnackbarResult.ActionPerformed) {
                        errorDialogInfo = event
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(title = stringResource(id = R.string.preferred), scrollBehavior = scrollBehavior)
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
    ) { innerPadding ->
        when (state.progress) {
            is PreferredViewState.Progress.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding()),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ContainedLoadingIndicator(
                            indicatorColor = MiuixTheme.colorScheme.primary,
                            containerColor = MiuixTheme.colorScheme.surfaceContainer
                        )
                        Text(
                            text = stringResource(id = R.string.loading),
                            style = MiuixTheme.textStyles.main
                        )
                    }
                }
            }

            is PreferredViewState.Progress.Loaded -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .scrollEndHaptic()
                        .overScrollVertical()
                        .padding(top = innerPadding.calculateTopPadding()),
                    overscrollEffect = null
                ) {
                    item { SmallTitle(stringResource(R.string.global)) }
                    item {
                        Card(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 6.dp)
                        ) {
                            MiuixNavigationItemWidget(
                                icon = AppIcons.Theme,
                                title = stringResource(R.string.theme_settings),
                                description = stringResource(R.string.theme_settings_desc),
                                onClick = {
                                    navController.navigate(MiuixSettingsScreen.MiuixTheme.route)
                                }
                            )
                            MiuixNavigationItemWidget(
                                icon = AppIcons.InstallMode,
                                title = stringResource(R.string.installer_settings),
                                description = stringResource(R.string.installer_settings_desc),
                                onClick = {
                                    navController.navigate(MiuixSettingsScreen.MiuixInstallerGlobal.route)
                                }
                            )
                        }
                    }
                    if (viewModel.state.authorizer == ConfigEntity.Authorizer.None) item { MiuixNoneInstallerTipCard() }
                    item { SmallTitle(stringResource(R.string.basic)) }
                    item {
                        Card(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 6.dp)
                        ) {
                            MiuixDisableAdbVerify(
                                checked = !state.adbVerifyEnabled,
                                isError = state.authorizer == ConfigEntity.Authorizer.Dhizuku,
                                enabled = state.authorizer != ConfigEntity.Authorizer.Dhizuku,
                                onCheckedChange = { isDisabled ->
                                    viewModel.dispatch(
                                        PreferredViewAction.SetAdbVerifyEnabledState(!isDisabled)
                                    )
                                }
                            )
                            MiuixIgnoreBatteryOptimizationSetting(
                                checked = state.isIgnoringBatteryOptimizations,
                                enabled = !state.isIgnoringBatteryOptimizations,
                            ) { viewModel.dispatch(PreferredViewAction.RequestIgnoreBatteryOptimization) }
                            MiuixAutoLockInstaller(
                                checked = state.autoLockInstaller,
                                enabled = state.authorizer != ConfigEntity.Authorizer.None,
                            ) { viewModel.dispatch(PreferredViewAction.ChangeAutoLockInstaller(!state.autoLockInstaller)) }
                            MiuixDefaultInstaller(true) { viewModel.dispatch(PreferredViewAction.SetDefaultInstaller(true)) }
                            MiuixDefaultInstaller(false) { viewModel.dispatch(PreferredViewAction.SetDefaultInstaller(false)) }
                            MiuixClearCache()
                        }
                    }
                    item { SmallTitle(stringResource(R.string.other)) }
                    item {
                        Card(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 6.dp)
                        ) {
                            MiuixSettingsAboutItemWidget(
                                imageVector = AppIcons.Info,
                                headlineContentText = stringResource(R.string.about_detail),
                                supportingContentText = "$revLevel ${RsConfig.VERSION_NAME}",
                                onClick = { navController.navigate(MiuixSettingsScreen.MiuixAbout.route) }
                            )
                        }
                    }
                }
            }
        }
    }
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
}