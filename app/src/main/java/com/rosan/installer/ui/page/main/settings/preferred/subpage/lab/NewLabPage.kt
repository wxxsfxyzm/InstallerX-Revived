package com.rosan.installer.ui.page.main.settings.preferred.subpage.lab

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.rosan.installer.R
import com.rosan.installer.core.env.AppConfig
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.widget.card.InfoTipCard
import com.rosan.installer.ui.page.main.widget.dialog.RootImplementationSelectionDialog
import com.rosan.installer.ui.page.main.widget.setting.AppBackButton
import com.rosan.installer.ui.page.main.widget.setting.LabHttpProfileWidget
import com.rosan.installer.ui.page.main.widget.setting.LabRootImplementationWidget
import com.rosan.installer.ui.page.main.widget.setting.SplicedColumnGroup
import com.rosan.installer.ui.page.main.widget.setting.SwitchWidget
import com.rosan.installer.ui.theme.getM3TopBarColor
import com.rosan.installer.ui.theme.installerHazeEffect
import com.rosan.installer.ui.theme.none
import com.rosan.installer.ui.theme.rememberMaterial3HazeStyle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewLabPage(
    navController: NavHostController,
    viewModel: LabSettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val capabilityProvider = koinInject<DeviceCapabilityProvider>()
    val topAppBarState = rememberTopAppBarState()
    val hazeState = if (uiState.useBlur) remember { HazeState() } else null
    val hazeStyle = rememberMaterial3HazeStyle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val showRootImplementationDialog = remember { mutableStateOf(false) }
    val isMiIslandSupported = remember { capabilityProvider.isSupportMiIsland }

    if (showRootImplementationDialog.value) {
        RootImplementationSelectionDialog(
            currentSelection = uiState.labRootImplementation,
            onDismiss = { showRootImplementationDialog.value = false },
            onConfirm = { selectedImplementation ->
                showRootImplementationDialog.value = false
                // 1. Save the selected implementation
                viewModel.dispatch(LabSettingsAction.LabChangeRootImplementation(selectedImplementation))
                // 2. Enable the flash module feature
                viewModel.dispatch(LabSettingsAction.LabChangeRootModuleFlash(true))
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        contentWindowInsets = WindowInsets.none,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                modifier = Modifier.installerHazeEffect(hazeState, hazeStyle),
                windowInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = 12.dp)),
                title = {
                    Text(stringResource(R.string.lab))
                },
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
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(hazeState?.let { Modifier.hazeSource(it) } ?: Modifier),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + 12.dp
            )
        ) {
            item { InfoTipCard(text = stringResource(R.string.lab_tip)) }
            item {
                SplicedColumnGroup(
                    title = stringResource(R.string.config_authorizer_root)
                ) {
                    item {
                        SwitchWidget(
                            icon = AppIcons.Root,
                            title = stringResource(R.string.lab_module_flashing),
                            description = stringResource(R.string.lab_module_flashing_desc),
                            checked = uiState.labRootEnableModuleFlash,
                            onCheckedChange = { isChecking ->
                                if (isChecking) {
                                    showRootImplementationDialog.value = true
                                } else {
                                    viewModel.dispatch(LabSettingsAction.LabChangeRootModuleFlash(false))
                                }
                            }
                        )
                    }
                    item(visible = uiState.labRootEnableModuleFlash) {
                        LabRootImplementationWidget(viewModel)
                    }
                    item(visible = uiState.labRootEnableModuleFlash) {
                        SwitchWidget(
                            icon = AppIcons.Terminal,
                            title = stringResource(R.string.lab_module_flashing_show_art),
                            description = stringResource(R.string.lab_module_flashing_show_art_desc),
                            checked = uiState.labRootShowModuleArt,
                            onCheckedChange = {
                                viewModel.dispatch(LabSettingsAction.LabChangeRootShowModuleArt(it))
                            }
                        )
                    }
                    item(visible = uiState.labRootEnableModuleFlash && capabilityProvider.isSystemApp) {
                        SwitchWidget(
                            icon = AppIcons.FlashPreferRoot,
                            title = stringResource(R.string.lab_module_always_use_root),
                            description = stringResource(R.string.lab_module_always_use_root_desc),
                            checked = uiState.labRootModuleAlwaysUseRoot,
                            onCheckedChange = {
                                viewModel.dispatch(LabSettingsAction.LabChangeRootModuleAlwaysUseRoot(it))
                            }
                        )
                    }
                }
            }
            item {
                SplicedColumnGroup(
                    title = stringResource(R.string.lab_unstable_features)
                ) {
                    if (isMiIslandSupported) item {
                        SwitchWidget(
                            title = stringResource(R.string.lab_mi_island),
                            description = stringResource(R.string.lab_mi_island_desc),
                            checked = uiState.labUseMiIsland,
                            onCheckedChange = { viewModel.dispatch(LabSettingsAction.LabChangeUseMiIsland(it)) }
                        )
                    }
                    item {
                        SwitchWidget(
                            icon = AppIcons.InstallRequester,
                            title = stringResource(R.string.lab_set_install_requester),
                            description = stringResource(R.string.lab_set_install_requester_desc),
                            checked = uiState.labSetInstallRequester,
                            onCheckedChange = { viewModel.dispatch(LabSettingsAction.LabChangeSetInstallRequester(it)) }
                        )
                    }
                }
            }

            if (AppConfig.isInternetAccessEnabled)
                item {
                    SplicedColumnGroup(
                        title = stringResource(R.string.internet_access_enabled)
                    ) {
                        /*item {
                            SwitchWidget(
                                icon = Icons.Default.Download,
                                title = stringResource(R.string.lab_http_save_file),
                                description = stringResource(R.string.lab_http_save_file_desc),
                                checked = uiState.labHttpSaveFile,
                                isM3E = false,
                                onCheckedChange = { viewModel.dispatch(LabSettingsAction.LabChangeHttpSaveFile(it)) }
                            )
                        }*/
                        item { LabHttpProfileWidget(viewModel) }
                    }
                }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}