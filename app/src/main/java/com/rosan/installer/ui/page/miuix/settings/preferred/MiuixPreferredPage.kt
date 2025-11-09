package com.rosan.installer.ui.page.miuix.settings.preferred

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.rosan.installer.ui.page.main.settings.SettingsScreen
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewAction
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
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
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun MiuixPreferredPage(
    navController: NavController,
    viewModel: PreferredViewModel = koinViewModel(),
    scrollBehavior: ScrollBehavior,
    paddingValues: PaddingValues
) {
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .scrollEndHaptic()
            .overScrollVertical()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = paddingValues,
        overscrollEffect = null
    ) {
        item { Spacer(modifier = Modifier.size(12.dp)) }
        item { SmallTitle(stringResource(R.string.global)) }
        item {
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp)
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
                    .padding(bottom = 12.dp)
            ) {
                MiuixDisableAdbVerify(
                    checked = !state.adbVerifyEnabled,
                    isError = state.authorizer == ConfigEntity.Authorizer.Dhizuku,
                    enabled = state.authorizer != ConfigEntity.Authorizer.Dhizuku &&
                            state.authorizer != ConfigEntity.Authorizer.None,
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
                MiuixDefaultInstaller(
                    lock = true,
                    enabled = state.authorizer != ConfigEntity.Authorizer.None,
                ) { viewModel.dispatch(PreferredViewAction.SetDefaultInstaller(true)) }
                MiuixDefaultInstaller(
                    lock = false,
                    enabled = state.authorizer != ConfigEntity.Authorizer.None,
                ) { viewModel.dispatch(PreferredViewAction.SetDefaultInstaller(false)) }
                MiuixClearCache()
            }
        }
        item { SmallTitle(stringResource(R.string.other)) }
        item {
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp)
            ) {
                MiuixSettingsAboutItemWidget(
                    headlineContentText = stringResource(R.string.lab),
                    supportingContentText = stringResource(R.string.lab_desc),
                    onClick = { navController.navigate(SettingsScreen.Lab.route) }
                )
                MiuixSettingsAboutItemWidget(
                    headlineContentText = stringResource(R.string.about_detail),
                    supportingContentText = "$revLevel ${RsConfig.VERSION_NAME}",
                    onClick = { navController.navigate(MiuixSettingsScreen.MiuixAbout.route) }
                )
            }
        }
    }
}