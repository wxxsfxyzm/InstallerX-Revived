package com.rosan.installer.ui.page.settings.preferred.subpage.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.settings.preferred.PreferredViewAction
import com.rosan.installer.ui.page.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.widget.setting.AppBackButton
import com.rosan.installer.ui.widget.setting.LabelWidget
import com.rosan.installer.ui.widget.setting.SwitchWidget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegacyThemeSettingsPage(
    navController: NavController,
    viewModel: PreferredViewModel,
) {
    val state = viewModel.state
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

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
                SwitchWidget(
                    icon = AppIcons.Theme,
                    title = stringResource(R.string.theme_settings_use_refreshed_ui),
                    description = stringResource(R.string.theme_settings_use_refreshed_ui_desc),
                    checked = state.showRefreshedUI,
                    onCheckedChange = {
                        viewModel.dispatch(PreferredViewAction.ChangeShowRefreshedUI(it))
                    }
                )
            }
        }
    }
}