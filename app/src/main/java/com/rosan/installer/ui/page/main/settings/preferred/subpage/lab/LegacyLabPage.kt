package com.rosan.installer.ui.page.main.settings.preferred.subpage.lab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
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
import com.rosan.installer.ui.page.main.widget.setting.LabelWidget
import com.rosan.installer.ui.page.main.widget.setting.SwitchWidget
import com.rosan.installer.util.OSUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegacyLabPage(
    navController: NavHostController,
    viewModel: PreferredViewModel
) {
    val state = viewModel.state
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
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
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.installer_settings)) },
                navigationIcon = {
                    AppBackButton(onClick = { navController.navigateUp() })
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item { InfoTipCard(text = stringResource(R.string.lab_tip)) }
            // --- Root Section (Module Flashing) ---
            item { LabelWidget(stringResource(R.string.config_authorizer_root)) }
            item {
                SwitchWidget(
                    icon = AppIcons.Root,
                    title = stringResource(R.string.lab_module_flashing),
                    description = stringResource(R.string.lab_module_flashing_desc),
                    checked = state.labRootEnableModuleFlash,
                    isM3E = false,
                    onCheckedChange = { isChecking ->
                        if (isChecking) {
                            // If turning ON, show the dialog first (don't enable yet)
                            showRootImplementationDialog.value = true
                        } else {
                            // If turning OFF, disable immediately
                            viewModel.dispatch(PreferredViewAction.LabChangeRootModuleFlash(false))
                        }
                    }
                )
            }
            // DropDownMenuWidget appears when enabled to allow changing the setting later
            item {
                AnimatedVisibility(
                    visible = state.labRootEnableModuleFlash,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        LabRootImplementationWidget(viewModel)
                        SwitchWidget(
                            icon = AppIcons.Terminal,
                            title = stringResource(R.string.lab_module_flashing_show_art),
                            description = stringResource(R.string.lab_module_flashing_show_art_desc),
                            isM3E = false,
                            checked = state.labRootShowModuleArt,
                            onCheckedChange = { viewModel.dispatch(PreferredViewAction.LabChangeRootShowModuleArt(it)) }
                        )
                        if (OSUtils.isSystemApp)
                            SwitchWidget(
                                icon = AppIcons.FlashPreferRoot,
                                title = stringResource(R.string.lab_module_always_use_root),
                                description = stringResource(R.string.lab_module_always_use_root_desc),
                                isM3E = false,
                                checked = state.labRootModuleAlwaysUseRoot,
                                onCheckedChange = { viewModel.dispatch(PreferredViewAction.LabChangeRootModuleAlwaysUseRoot(it)) }
                            )
                    }
                }
            }
            // --- Unstable Features Section ---
            item { LabelWidget(stringResource(R.string.lab_unstable_features)) }
            item {
                SwitchWidget(
                    icon = AppIcons.InstallRequester,
                    title = stringResource(R.string.lab_set_install_requester),
                    description = stringResource(R.string.lab_set_install_requester_desc),
                    checked = state.labSetInstallRequester,
                    isM3E = false,
                    onCheckedChange = { viewModel.dispatch(PreferredViewAction.LabChangeSetInstallRequester(it)) }
                )
            }
            // --- Internet Access Section ---
            if (RsConfig.isInternetAccessEnabled) {
                item { LabelWidget(stringResource(R.string.internet_access_enabled)) }
                // TODO
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

                // HTTP Profile DropDown
                item { LabHttpProfileWidget(viewModel) }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}