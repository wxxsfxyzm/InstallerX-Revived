package com.rosan.installer.ui.page.main.settings.preferred.subpage.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewAction
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.widget.setting.AppBackButton
import com.rosan.installer.ui.widget.setting.SelectableSettingItem
import com.rosan.installer.ui.widget.setting.SplicedColumnGroup
import com.rosan.installer.ui.widget.setting.SwitchWidget

// This is now a top-level composable, likely in its own file.
// It takes NavController instead of an onBack lambda.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewThemeSettingsPage(
    navController: NavController,
    viewModel: PreferredViewModel,
) {
    val state = viewModel.state
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            TopAppBar(
                windowInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = 12.dp)),
                title = { Text(stringResource(R.string.theme_settings)) },
                navigationIcon = {
                    AppBackButton(
                        onClick = { navController.navigateUp() },
                        icon = Icons.AutoMirrored.TwoTone.ArrowBack,
                        modifier = Modifier.size(36.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceBright
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                                title = "Google UI", // TODO: Replace with stringResource
                                description = "标准 Material Design 界面", // TODO: Replace with stringResource
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
                                title = "MIUIX UI", // TODO: Replace with stringResource
                                description = "类 HyperOS 风格界面", // TODO: Replace with stringResource
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
            item {
                // Only show this section if Google UI is selected
                AnimatedVisibility(visible = !state.showMiuixUI) {
                    SplicedColumnGroup(
                        title = "Google UI 样式", // TODO: Replace with stringResource
                        content = listOf {
                            SwitchWidget(
                                icon = AppIcons.Theme,
                                title = stringResource(R.string.theme_settings_use_refreshed_ui), // "Use Refreshed UI"
                                description = stringResource(R.string.theme_settings_use_refreshed_ui_desc), // "Enable M3 expressive style..."
                                checked = state.showExpressiveUI,
                                onCheckedChange = {
                                    viewModel.dispatch(PreferredViewAction.ChangeShowExpressiveUI(it))
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}