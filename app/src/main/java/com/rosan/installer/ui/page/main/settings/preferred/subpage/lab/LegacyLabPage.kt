package com.rosan.installer.ui.page.main.settings.preferred.subpage.lab

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.rosan.installer.R
import com.rosan.installer.build.RsConfig
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewAction
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.page.main.widget.setting.AppBackButton
import com.rosan.installer.ui.page.main.widget.setting.LabHttpProfileWidget
import com.rosan.installer.ui.page.main.widget.setting.LabelWidget
import com.rosan.installer.ui.page.main.widget.setting.SwitchWidget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegacyLabPage(
    navController: NavHostController,
    viewModel: PreferredViewModel
) {
    val state = viewModel.state
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

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
            item { LabelWidget(stringResource(R.string.config_authorizer_shizuku)) }
            item {
                SwitchWidget(
                    icon = Icons.AutoMirrored.Filled.AltRoute,
                    title = stringResource(R.string.lab_use_hook_mode),
                    description = stringResource(R.string.lab_use_hook_mode_desc),
                    checked = state.labShizukuHookMode,
                    isM3E = false,
                    onCheckedChange = { viewModel.dispatch(PreferredViewAction.LabChangeShizukuHookMode(it)) }
                )
            }
            item { LabelWidget("Unstable features") }
            item {
                SwitchWidget(
                    icon = Icons.AutoMirrored.Filled.AltRoute,
                    title = "Set Install Requester",
                    description = "Set install requester to match system behavior, please note that third-party apps can't always retrieve this info, will default to null in this case",
                    checked = state.labSetInstallRequester,
                    isM3E = false,
                    onCheckedChange = { viewModel.dispatch(PreferredViewAction.LabChangeSetInstallRequester(it)) }
                )
            }
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