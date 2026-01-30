package com.rosan.installer.ui.page.main.settings.preferred.subpage.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rosan.installer.BuildConfig
import com.rosan.installer.R
import com.rosan.installer.build.RsConfig
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.SettingsScreen
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewAction
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.page.main.widget.card.StatusWidget
import com.rosan.installer.ui.page.main.widget.setting.AppBackButton
import com.rosan.installer.ui.page.main.widget.setting.BottomSheetContent
import com.rosan.installer.ui.page.main.widget.setting.ExportLogsWidget
import com.rosan.installer.ui.page.main.widget.setting.LabelWidget
import com.rosan.installer.ui.page.main.widget.setting.LogEventCollector
import com.rosan.installer.ui.page.main.widget.setting.SettingsAboutItemWidget
import com.rosan.installer.ui.page.main.widget.setting.SwitchWidget
import com.rosan.installer.ui.page.main.widget.setting.UpdateLoadingIndicator
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomePage(
    navController: NavController,
    viewModel: PreferredViewModel
) {
    val context = LocalContext.current
    val hazeState = remember { HazeState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val uriHandler = LocalUriHandler.current
    var showBottomSheet by remember { mutableStateOf(false) }

    LogEventCollector(viewModel)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .hazeSource(state = hazeState),
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = stringResource(id = R.string.about))
                    },
                    scrollBehavior = scrollBehavior,
                    navigationIcon = { AppBackButton(onClick = { navController.navigateUp() }) }
                )
            },
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = 8.dp, bottom = 12.dp)
                    ) {
                        StatusWidget(viewModel)
                    }
                }
                item { LabelWidget(stringResource(R.string.about)) }
                item {
                    SettingsAboutItemWidget(
                        imageVector = AppIcons.ViewSourceCode,
                        headlineContentText = stringResource(R.string.get_source_code),
                        supportingContentText = stringResource(R.string.get_source_code_detail),
                        onClick = { uriHandler.openUri("https://github.com/wxxsfxyzm/InstallerX-Revived") }
                    )
                }
                item {
                    SettingsAboutItemWidget(
                        imageVector = AppIcons.OpenSourceLicense,
                        headlineContentText = stringResource(R.string.open_source_license),
                        supportingContentText = stringResource(R.string.open_source_license_settings_description),
                        onClick = { navController.navigate(SettingsScreen.OpenSourceLicense.route) }
                    )
                }
                item {
                    SettingsAboutItemWidget(
                        imageVector = AppIcons.Update,
                        headlineContentText = stringResource(R.string.get_update),
                        supportingContentText = stringResource(R.string.get_update_detail),
                        onClick = { showBottomSheet = true }
                    )
                }
                if (viewModel.state.hasUpdate)
                    item {
                        SettingsAboutItemWidget(
                            imageVector = AppIcons.Download,
                            headlineContentText = stringResource(R.string.get_update_directly),
                            supportingContentText = stringResource(R.string.get_update_directly_desc),
                            onClick = { viewModel.dispatch(PreferredViewAction.Update) }
                        )
                    }
                if (RsConfig.isLogEnabled && context.packageName == BuildConfig.APPLICATION_ID) {
                    item { LabelWidget(stringResource(R.string.debug)) }
                    item {
                        SwitchWidget(
                            icon = AppIcons.BugReport,
                            title = stringResource(R.string.save_logs),
                            description = stringResource(R.string.save_logs_desc),
                            checked = viewModel.state.enableFileLogging,
                            onCheckedChange = { viewModel.dispatch(PreferredViewAction.SetEnableFileLogging(it)) }
                        )
                    }
                    item {
                        AnimatedVisibility(
                            visible = viewModel.state.enableFileLogging,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) { ExportLogsWidget(viewModel) }
                    }
                }
                item { Spacer(Modifier.navigationBarsPadding()) }
            }

        }

        if (showBottomSheet) {
            ModalBottomSheet(onDismissRequest = { showBottomSheet = false }) {
                BottomSheetContent(
                    title = stringResource(R.string.get_update),
                    hasUpdate = viewModel.state.hasUpdate,
                    onDirectUpdateClick = {
                        showBottomSheet = false
                        viewModel.dispatch(PreferredViewAction.Update)
                    }
                )
            }
        }
        UpdateLoadingIndicator(hazeState = hazeState, viewModel = viewModel)
    }
}