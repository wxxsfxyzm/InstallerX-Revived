package com.rosan.installer.ui.page.main.settings.preferred.subpage.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
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
import com.rosan.installer.ui.page.main.widget.setting.LogEventCollector
import com.rosan.installer.ui.page.main.widget.setting.SettingsNavigationItemWidget
import com.rosan.installer.ui.page.main.widget.setting.SplicedColumnGroup
import com.rosan.installer.ui.page.main.widget.setting.SwitchWidget
import com.rosan.installer.ui.page.main.widget.setting.UpdateLoadingIndicator
import com.rosan.installer.ui.theme.getM3TopBarColor
import com.rosan.installer.ui.theme.installerHazeEffect
import com.rosan.installer.ui.theme.rememberMaterial3HazeStyle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewHomePage(
    navController: NavController,
    viewModel: PreferredViewModel
) {
    val state = viewModel.state
    val context = LocalContext.current
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val topBarHazeState = if (state.useBlur) remember { HazeState() } else null
    val indicatorHazeState = if (state.useBlur) remember { HazeState() } else null
    val hazeStyle = rememberMaterial3HazeStyle()
    val uriHandler = LocalUriHandler.current
    var showBottomSheet by remember { mutableStateOf(false) }

    LogEventCollector(viewModel)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .then(indicatorHazeState?.let { Modifier.hazeSource(it) } ?: Modifier),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            topBar = {
                LargeFlexibleTopAppBar(
                    modifier = Modifier.installerHazeEffect(topBarHazeState, hazeStyle),
                    windowInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = 12.dp)),
                    title = { Text(text = stringResource(id = R.string.about)) },
                    scrollBehavior = scrollBehavior,
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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = topBarHazeState.getM3TopBarColor(),
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        scrolledContainerColor = topBarHazeState.getM3TopBarColor()
                    )
                )
            },
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .then(topBarHazeState?.let { Modifier.hazeSource(it) } ?: Modifier),
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding()
                ),
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
                item {
                    SplicedColumnGroup(
                        title = stringResource(R.string.about)
                    ) {
                        item {
                            SettingsNavigationItemWidget(
                                icon = AppIcons.ViewSourceCode,
                                title = stringResource(R.string.get_source_code),
                                description = stringResource(R.string.get_source_code_detail),
                                onClick = { uriHandler.openUri("https://github.com/wxxsfxyzm/InstallerX-Revived") }
                            )
                        }
                        item {
                            SettingsNavigationItemWidget(
                                icon = AppIcons.OpenSourceLicense,
                                title = stringResource(R.string.open_source_license),
                                description = stringResource(R.string.open_source_license_settings_description),
                                onClick = { navController.navigate(SettingsScreen.OpenSourceLicense.route) }
                            )
                        }
                        item {
                            SettingsNavigationItemWidget(
                                icon = AppIcons.Update,
                                title = stringResource(R.string.get_update),
                                description = stringResource(R.string.get_update_detail),
                                onClick = { showBottomSheet = true }
                            )
                        }
                        if (viewModel.state.hasUpdate)
                            item {
                                SettingsNavigationItemWidget(
                                    icon = AppIcons.Download,
                                    title = stringResource(R.string.get_update_directly),
                                    description = stringResource(R.string.get_update_directly_desc),
                                    onClick = { viewModel.dispatch(PreferredViewAction.Update) }
                                )
                            }
                    }
                }
                if (RsConfig.isLogEnabled && context.packageName == BuildConfig.APPLICATION_ID)
                    item {
                        SplicedColumnGroup(
                            title = stringResource(R.string.debug)
                        ) {
                            item {
                                SwitchWidget(
                                    icon = AppIcons.BugReport,
                                    title = stringResource(R.string.save_logs),
                                    description = stringResource(R.string.save_logs_desc),
                                    checked = viewModel.state.enableFileLogging,
                                    onCheckedChange = { viewModel.dispatch(PreferredViewAction.SetEnableFileLogging(it)) }
                                )
                            }
                            item(visible = viewModel.state.enableFileLogging) {
                                ExportLogsWidget(viewModel)
                            }
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
        UpdateLoadingIndicator(hazeState = indicatorHazeState, viewModel = viewModel)
    }
}