package com.rosan.installer.ui.page.main.settings.preferred.subpage.theme

import android.os.Build
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewAction
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.page.main.widget.dialog.HideLauncherIconWarningDialog
import com.rosan.installer.ui.page.main.widget.setting.AppBackButton
import com.rosan.installer.ui.page.main.widget.setting.SelectableSettingItem
import com.rosan.installer.ui.page.main.widget.setting.SplicedColumnGroup
import com.rosan.installer.ui.page.main.widget.setting.SwitchWidget

// This is now a top-level composable, likely in its own file.
// It takes NavController instead of an onBack lambda.
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewThemeSettingsPage(
    navController: NavController,
    viewModel: PreferredViewModel,
) {
    val state = viewModel.state
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    var showHideLauncherIconDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        topAppBarState.heightOffset = topAppBarState.heightOffsetLimit
    }

    HideLauncherIconWarningDialog(
        show = showHideLauncherIconDialog,
        onDismiss = { showHideLauncherIconDialog = false },
        onConfirm = {
            showHideLauncherIconDialog = false
            viewModel.dispatch(PreferredViewAction.ChangeShowLauncherIcon(false))
        }
    )

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                windowInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = 12.dp)),
                title = {
                    Text(stringResource(R.string.theme_settings))
                },
                navigationIcon = {
                    Row {
                        AppBackButton(
                            onClick = { navController.navigateUp() },
                            icon = Icons.AutoMirrored.TwoTone.ArrowBack,
                            modifier = Modifier.size(36.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceBright
                        )
                        Spacer(modifier = Modifier.size(16.dp))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                SplicedColumnGroup(
                    title = stringResource(R.string.theme_settings_ui_style),
                    content = listOf(
                        {
                            // Option 1: Google UI
                            SelectableSettingItem(
                                title = stringResource(R.string.theme_settings_google_ui),
                                description = stringResource(R.string.theme_settings_google_ui_desc),
                                selected = !state.showMiuixUI,
                                onClick = {
                                    if (state.showMiuixUI) { // Only dispatch if changing state
                                        viewModel.dispatch(PreferredViewAction.ChangeUseMiuix(false))
                                    }
                                }
                            )
                        },
                        {
                            // Option 2: MIUIX UI
                            SelectableSettingItem(
                                title = stringResource(R.string.theme_settings_miuix_ui),
                                description = stringResource(R.string.theme_settings_miuix_ui_desc),
                                selected = state.showMiuixUI,
                                onClick = {
                                    if (!state.showMiuixUI) { // Only dispatch if changing state
                                        viewModel.dispatch(PreferredViewAction.ChangeUseMiuix(true))
                                    }
                                }
                            )
                        }
                    )
                )
            }

            // --- Group 2: Google UI Style Options (Legacy vs Expressive) ---
            // Only show this section if Google UI is selected
            if (!state.showMiuixUI) {
                item {
                    SplicedColumnGroup(
                        title = stringResource(R.string.theme_settings_google_ui),
                        content = buildList {
                            add {
                                SwitchWidget(
                                    icon = AppIcons.Theme,
                                    title = stringResource(R.string.theme_settings_use_expressive_ui),
                                    description = stringResource(R.string.theme_settings_use_expressive_ui_desc),
                                    checked = state.showExpressiveUI,
                                    onCheckedChange = {
                                        viewModel.dispatch(
                                            PreferredViewAction.ChangeShowExpressiveUI(it)
                                        )
                                    }
                                )
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA)
                                add {
                                    SwitchWidget(
                                        icon = AppIcons.LiveActivity,
                                        title = stringResource(R.string.theme_settings_use_live_activity),
                                        description = stringResource(R.string.theme_settings_use_live_activity_desc),
                                        checked = state.showLiveActivity,
                                        onCheckedChange = {
                                            viewModel.dispatch(
                                                PreferredViewAction.ChangeShowLiveActivity(it)
                                            )
                                        }
                                    )
                                }
                        }
                    )
                }
            }
            item {
                SplicedColumnGroup(
                    title = stringResource(R.string.theme_settings_package_icons),
                    content = listOf {
                        SwitchWidget(
                            icon = AppIcons.IconPack,
                            title = stringResource(R.string.theme_settings_prefer_system_icon),
                            description = stringResource(R.string.theme_settings_prefer_system_icon_desc),
                            checked = state.preferSystemIcon,
                            onCheckedChange = {
                                viewModel.dispatch(
                                    PreferredViewAction.ChangePreferSystemIcon(it)
                                )
                            }
                        )
                    }

                )
            }
            item {
                SplicedColumnGroup(
                    title = stringResource(R.string.theme_settings_launcher_icons),
                    content = listOf {
                        SwitchWidget(
                            icon = AppIcons.Launcher,
                            title = stringResource(R.string.theme_settings_hide_launcher_icon),
                            description = stringResource(R.string.theme_settings_hide_launcher_icon_desc),
                            checked = !state.showLauncherIcon,
                            onCheckedChange = { newCheckedState ->
                                if (newCheckedState) {
                                    showHideLauncherIconDialog = true
                                } else {
                                    viewModel.dispatch(
                                        PreferredViewAction.ChangeShowLauncherIcon(
                                            true
                                        )
                                    )
                                }
                            }
                        )
                    }
                )
            }
        }
    }
}