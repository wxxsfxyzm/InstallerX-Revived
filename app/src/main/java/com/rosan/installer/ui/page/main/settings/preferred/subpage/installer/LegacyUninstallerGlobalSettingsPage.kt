package com.rosan.installer.ui.page.main.settings.preferred.subpage.installer

import android.content.Intent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.rosan.installer.R
import com.rosan.installer.ui.activity.UninstallerActivity
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewEvent
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.page.main.widget.card.InfoTipCard
import com.rosan.installer.ui.page.main.widget.dialog.UninstallPackageDialog
import com.rosan.installer.ui.page.main.widget.setting.AppBackButton
import com.rosan.installer.ui.page.main.widget.setting.LabelWidget
import com.rosan.installer.ui.page.main.widget.setting.SettingsNavigationItemWidget
import com.rosan.installer.ui.page.main.widget.setting.UninstallForAllUsersWidget
import com.rosan.installer.ui.page.main.widget.setting.UninstallKeepDataWidget
import com.rosan.installer.ui.page.main.widget.setting.UninstallRequireBiometricAuthWidget
import com.rosan.installer.ui.page.main.widget.setting.UninstallSystemAppWidget
import com.rosan.installer.ui.theme.none
import com.rosan.installer.util.toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegacyUninstallerGlobalSettingsPage(
    navController: NavController,
    viewModel: PreferredViewModel,
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showUninstallInputDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is PreferredViewEvent.ShowMessage -> context.toast(event.resId)
                else -> Unit
            }
        }
    }

    if (showUninstallInputDialog)
        UninstallPackageDialog(
            onDismiss = { showUninstallInputDialog = false },
            onConfirm = { packageName ->
                showUninstallInputDialog = false
                val intent = Intent(context, UninstallerActivity::class.java).apply {
                    putExtra("package_name", packageName)
                }
                context.startActivity(intent)
            }
        )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.none,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.uninstaller_settings)) },
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
            item { InfoTipCard(text = stringResource(R.string.uninstall_authorizer_tip)) }
            item { LabelWidget(label = stringResource(R.string.global)) }
            item { UninstallKeepDataWidget(viewModel, isM3E = false) }
            item { UninstallForAllUsersWidget(viewModel, isM3E = false) }
            item { UninstallSystemAppWidget(viewModel, isM3E = false) }
            item { UninstallRequireBiometricAuthWidget(viewModel, false) }
            item { LabelWidget(label = stringResource(R.string.uninstall_call_uninstaller)) }
            item {
                SettingsNavigationItemWidget(
                    icon = AppIcons.Delete,
                    title = stringResource(R.string.uninstall_call_uninstaller_manually),
                    description = stringResource(R.string.uninstall_call_uninstaller_manually_desc)
                ) {
                    showUninstallInputDialog = true
                }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}