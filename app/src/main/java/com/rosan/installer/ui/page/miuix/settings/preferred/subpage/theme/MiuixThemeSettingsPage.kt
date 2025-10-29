package com.rosan.installer.ui.page.miuix.settings.preferred.subpage.theme

import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rosan.installer.R
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewAction
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.page.miuix.widgets.MiuixHideLauncherIconWarningDialog
import com.rosan.installer.ui.page.miuix.widgets.MiuixSwitchWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixThemeEngineWidget
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun MiuixThemeSettingsPage(
    navController: NavController,
    viewModel: PreferredViewModel,
) {
    val state = viewModel.state
    val scrollBehavior = MiuixScrollBehavior()
    val showHideLauncherIconDialog = remember { mutableStateOf(false) }

    MiuixHideLauncherIconWarningDialog(
        showState = showHideLauncherIconDialog,
        onDismiss = { showHideLauncherIconDialog.value = false },
        onConfirm = {
            showHideLauncherIconDialog.value = false
            viewModel.dispatch(PreferredViewAction.ChangeShowLauncherIcon(false))
        }
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = stringResource(R.string.theme_settings),
                navigationIcon = {
                    MiuixBackButton(modifier = Modifier.padding(start = 16.dp), onClick = { navController.navigateUp() })
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .scrollEndHaptic()
                .overScrollVertical()
                .padding(top = paddingValues.calculateTopPadding()),
            overscrollEffect = null
        ) {
            item { SmallTitle(stringResource(R.string.theme_settings_ui_style)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 6.dp)
                ) {
                    MiuixThemeEngineWidget(
                        currentThemeIsMiuix = state.showMiuixUI,
                        onThemeChange = { useMiuix ->
                            viewModel.dispatch(PreferredViewAction.ChangeUseMiuix(useMiuix))
                        }
                    )
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA)
                item { SmallTitle(stringResource(R.string.theme_settings_google_ui)) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA)
                item {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 6.dp)
                    ) {
                        MiuixSwitchWidget(
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
            item { SmallTitle(stringResource(R.string.theme_settings_package_icons)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 6.dp)
                ) {
                    MiuixSwitchWidget(
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
            }
            item { SmallTitle(stringResource(R.string.theme_settings_launcher_icons)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 6.dp)
                ) {
                    MiuixSwitchWidget(
                        title = stringResource(R.string.theme_settings_hide_launcher_icon),
                        description = stringResource(R.string.theme_settings_hide_launcher_icon_desc),
                        checked = !state.showLauncherIcon,
                        onCheckedChange = { newCheckedState ->
                            if (newCheckedState) {
                                showHideLauncherIconDialog.value = true
                            } else {
                                viewModel.dispatch(PreferredViewAction.ChangeShowLauncherIcon(true))
                            }
                        }
                    )
                }
            }
        }
    }
}