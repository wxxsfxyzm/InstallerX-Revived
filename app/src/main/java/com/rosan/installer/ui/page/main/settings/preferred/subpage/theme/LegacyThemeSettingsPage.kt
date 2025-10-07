package com.rosan.installer.ui.page.main.settings.preferred.subpage.theme

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.rosan.installer.ui.page.main.widget.setting.LabelWidget
import com.rosan.installer.ui.page.main.widget.setting.SelectableSettingItem
import com.rosan.installer.ui.page.main.widget.setting.SwitchWidget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegacyThemeSettingsPage(
    navController: NavController,
    viewModel: PreferredViewModel,
) {
    val state = viewModel.state
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showHideLauncherIconDialog by remember { mutableStateOf(false) }

    HideLauncherIconWarningDialog(
        show = showHideLauncherIconDialog,
        onDismiss = { showHideLauncherIconDialog = false },
        onConfirm = {
            showHideLauncherIconDialog = false
            viewModel.dispatch(PreferredViewAction.ChangeShowLauncherIcon(false))
        }
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.theme_settings)) },
                navigationIcon = { AppBackButton(onClick = { navController.navigateUp() }) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                LabelWidget(label = stringResource(R.string.theme_settings_ui_style))
            }
            item {
                Column(modifier = Modifier.padding(start = 36.dp, end = 12.dp)) {
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
            }
            item { LabelWidget(stringResource(R.string.theme_settings_google_ui)) }
            item {
                SwitchWidget(
                    icon = AppIcons.Theme,
                    title = stringResource(R.string.theme_settings_use_expressive_ui),
                    description = stringResource(R.string.theme_settings_use_expressive_ui_desc),
                    checked = state.showExpressiveUI,
                    isM3E = false,
                    onCheckedChange = {
                        viewModel.dispatch(PreferredViewAction.ChangeShowExpressiveUI(it))
                    }
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA)
                item {
                    SwitchWidget(
                        icon = AppIcons.LiveActivity,
                        title = stringResource(R.string.theme_settings_use_live_activity),
                        description = stringResource(R.string.theme_settings_use_live_activity_desc),
                        checked = state.showLiveActivity,
                        isM3E = false,
                        onCheckedChange = {
                            viewModel.dispatch(
                                PreferredViewAction.ChangeShowLiveActivity(it)
                            )
                        }
                    )
                }
            item { LabelWidget(stringResource(R.string.theme_settings_package_icons)) }
            item {
                SwitchWidget(
                    icon = AppIcons.IconPack,
                    title = stringResource(R.string.theme_settings_prefer_system_icon),
                    description = stringResource(R.string.theme_settings_prefer_system_icon_desc),
                    checked = state.preferSystemIcon,
                    isM3E = false,
                    onCheckedChange = {
                        viewModel.dispatch(
                            PreferredViewAction.ChangePreferSystemIcon(it)
                        )
                    }
                )
            }
            item { LabelWidget(stringResource(R.string.theme_settings_launcher_icons)) }
            item {
                SwitchWidget(
                    icon = AppIcons.BugReport,
                    title = stringResource(R.string.theme_settings_hide_launcher_icon),
                    description = stringResource(R.string.theme_settings_hide_launcher_icon_desc),
                    checked = !state.showLauncherIcon,
                    isM3E = false,
                    onCheckedChange = { newCheckedState ->
                        if (newCheckedState) {
                            showHideLauncherIconDialog = true
                        } else {
                            viewModel.dispatch(PreferredViewAction.ChangeShowLauncherIcon(true))
                        }
                    }
                )
            }
        }
    }
}