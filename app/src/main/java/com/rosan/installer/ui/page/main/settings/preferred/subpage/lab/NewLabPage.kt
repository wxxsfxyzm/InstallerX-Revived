package com.rosan.installer.ui.page.main.settings.preferred.subpage.lab

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.rosan.installer.R
import com.rosan.installer.build.RsConfig
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewAction
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.page.main.widget.card.InfoTipCard
import com.rosan.installer.ui.page.main.widget.dialog.RootImplementationSelectionDialog
import com.rosan.installer.ui.page.main.widget.setting.AppBackButton
import com.rosan.installer.ui.page.main.widget.setting.LabHttpProfileWidget
import com.rosan.installer.ui.page.main.widget.setting.LabRootImplementationWidget
import com.rosan.installer.ui.page.main.widget.setting.SplicedColumnGroup
import com.rosan.installer.ui.page.main.widget.setting.SwitchWidget
import com.rosan.installer.ui.theme.none
import com.rosan.installer.util.OSUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewLabPage(
    navController: NavHostController,
    viewModel: PreferredViewModel
) {
    val state = viewModel.state
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val showRootImplementationDialog = remember { mutableStateOf(false) }

    if (showRootImplementationDialog.value) {
        RootImplementationSelectionDialog(
            currentSelection = state.labRootImplementation,
            onDismiss = { showRootImplementationDialog.value = false },
            onConfirm = { selectedImplementation ->
                showRootImplementationDialog.value = false
                // 1. Save the selected implementation
                viewModel.dispatch(PreferredViewAction.LabChangeRootImplementation(selectedImplementation))
                // 2. Enable the flash module feature
                viewModel.dispatch(PreferredViewAction.LabChangeRootModuleFlash(true))
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
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                            checked = state.labRootEnableModuleFlash,
                            onCheckedChange = { isChecking ->
                                if (isChecking) {
                                    showRootImplementationDialog.value = true
                                } else {
                                    viewModel.dispatch(PreferredViewAction.LabChangeRootModuleFlash(false))
                                }
                            }
                        )
                    }
                    item(visible = state.labRootEnableModuleFlash) {
                        LabRootImplementationWidget(viewModel)
                    }
                    item(visible = state.labRootEnableModuleFlash) {
                        SwitchWidget(
                            icon = AppIcons.Terminal,
                            title = stringResource(R.string.lab_module_flashing_show_art),
                            description = stringResource(R.string.lab_module_flashing_show_art_desc),
                            checked = state.labRootShowModuleArt,
                            onCheckedChange = {
                                viewModel.dispatch(PreferredViewAction.LabChangeRootShowModuleArt(it))
                            }
                        )
                    }
                    item(visible = state.labRootEnableModuleFlash && OSUtils.isSystemApp) {
                        SwitchWidget(
                            icon = AppIcons.FlashPreferRoot,
                            title = stringResource(R.string.lab_module_always_use_root),
                            description = stringResource(R.string.lab_module_always_use_root_desc),
                            checked = state.labRootModuleAlwaysUseRoot,
                            onCheckedChange = {
                                viewModel.dispatch(PreferredViewAction.LabChangeRootModuleAlwaysUseRoot(it))
                            }
                        )
                    }
                }
            }
            item {
                SplicedColumnGroup(
                    title = stringResource(R.string.lab_unstable_features)
                ) {
                    item {
                        SwitchWidget(
                            icon = AppIcons.InstallRequester,
                            title = stringResource(R.string.lab_set_install_requester),
                            description = stringResource(R.string.lab_set_install_requester_desc),
                            checked = state.labSetInstallRequester,
                            onCheckedChange = { viewModel.dispatch(PreferredViewAction.LabChangeSetInstallRequester(it)) }
                        )
                    }
                }
            }

            if (RsConfig.isInternetAccessEnabled)
                item {
                    SplicedColumnGroup(
                        title = stringResource(R.string.internet_access_enabled)
                    ) {
                        /*item {
                            SwitchWidget(
                                icon = Icons.Default.Download,
                                title = stringResource(R.string.lab_http_save_file),
                                description = stringResource(R.string.lab_http_save_file_desc),
                                checked = state.labHttpSaveFile,
                                isM3E = false,
                                onCheckedChange = { viewModel.dispatch(PreferredViewAction.LabChangeHttpSaveFile(it)) }
                            )
                        }*/
                        item { LabHttpProfileWidget(viewModel) }
                    }
                }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}