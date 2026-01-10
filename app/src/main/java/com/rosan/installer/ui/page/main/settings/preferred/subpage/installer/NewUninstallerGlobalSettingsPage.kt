package com.rosan.installer.ui.page.main.settings.preferred.subpage.installer

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rosan.installer.R
import com.rosan.installer.ui.activity.UninstallerActivity
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewEvent
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.page.main.widget.card.InfoTipCard
import com.rosan.installer.ui.page.main.widget.dialog.UninstallPackageDialog
import com.rosan.installer.ui.page.main.widget.setting.AppBackButton
import com.rosan.installer.ui.page.main.widget.setting.SettingsNavigationItemWidget
import com.rosan.installer.ui.page.main.widget.setting.SplicedColumnGroup
import com.rosan.installer.ui.page.main.widget.setting.UninstallForAllUsersWidget
import com.rosan.installer.ui.page.main.widget.setting.UninstallKeepDataWidget
import com.rosan.installer.ui.page.main.widget.setting.UninstallRequireBiometricAuthWidget
import com.rosan.installer.ui.page.main.widget.setting.UninstallSystemAppWidget
import com.rosan.installer.ui.theme.none
import com.rosan.installer.util.toast

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewUninstallerGlobalSettingsPage(
    navController: NavController,
    viewModel: PreferredViewModel
) {
    val context = LocalContext.current
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

    LaunchedEffect(Unit) {
        topAppBarState.heightOffset = topAppBarState.heightOffsetLimit
    }

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
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        contentWindowInsets = WindowInsets.none,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                windowInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = 12.dp)),
                title = {
                    Text(stringResource(R.string.uninstaller_settings))
                },
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
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item { InfoTipCard(text = stringResource(R.string.uninstall_authorizer_tip)) }
            // --- Group 1: Global Settings ---
            item {
                SplicedColumnGroup(
                    title = stringResource(R.string.global)
                ) {
                    item { UninstallKeepDataWidget(viewModel) }
                    item { UninstallForAllUsersWidget(viewModel) }
                    item { UninstallSystemAppWidget(viewModel) }
                    item { UninstallRequireBiometricAuthWidget(viewModel) }
                }
            }

            // --- Group 2: Manual Uninstaller Call ---
            item {
                SplicedColumnGroup(
                    title = stringResource(R.string.uninstall_call_uninstaller)
                ) {
                    item {
                        SettingsNavigationItemWidget(
                            icon = AppIcons.Delete,
                            title = stringResource(R.string.uninstall_call_uninstaller_manually),
                            description = stringResource(R.string.uninstall_call_uninstaller_manually_desc)
                        ) {
                            showUninstallInputDialog = true
                        }
                    }
                }
            }
            
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}