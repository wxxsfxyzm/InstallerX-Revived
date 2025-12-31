package com.rosan.installer.ui.page.miuix.settings.preferred.subpage.installer

import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rosan.installer.R
import com.rosan.installer.ui.activity.UninstallerActivity
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewEvent
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.page.miuix.widgets.MiuixNavigationItemWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixSettingsTipCard
import com.rosan.installer.ui.page.miuix.widgets.MiuixUninstallForAllUsersWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixUninstallKeepDataWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixUninstallPackageDialog
import com.rosan.installer.ui.page.miuix.widgets.MiuixUninstallRequireBiometricAuthWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixUninstallSystemAppWidget
import com.rosan.installer.util.toast
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun MiuixUninstallerGlobalSettingsPage(
    navController: NavController,
    viewModel: PreferredViewModel
) {
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior()
    var showUninstallInputDialog = remember { mutableStateOf(false) }
    val hazeState = remember { HazeState() }
    val hazeStyle = HazeStyle(
        backgroundColor = MiuixTheme.colorScheme.surface,
        tint = HazeTint(MiuixTheme.colorScheme.surface.copy(0.8f))
    )

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is PreferredViewEvent.ShowMessage -> context.toast(event.resId)
                else -> Unit
            }
        }
    }

    MiuixUninstallPackageDialog(
        showState = showUninstallInputDialog,
        onDismiss = { showUninstallInputDialog.value = false },
        onConfirm = { packageName ->
            showUninstallInputDialog.value = false
            val intent = Intent(context, UninstallerActivity::class.java).apply {
                putExtra("package_name", packageName)
            }
            context.startActivity(intent)
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.hazeEffect(hazeState) {
                    style = hazeStyle
                    blurRadius = 30.dp
                    noiseFactor = 0f
                },
                title = stringResource(R.string.uninstaller_settings),
                navigationIcon = {
                    MiuixBackButton(
                        modifier = Modifier.padding(start = 16.dp),
                        onClick = { navController.navigateUp() })
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(top = paddingValues.calculateTopPadding()),
            overscrollEffect = null
        ) {
            item { Spacer(modifier = Modifier.size(12.dp)) }
            item { MiuixSettingsTipCard(stringResource(R.string.uninstall_authorizer_tip)) }
            item { SmallTitle(stringResource(R.string.global)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 16.dp)
                ) {
                    MiuixUninstallKeepDataWidget(viewModel)
                    MiuixUninstallForAllUsersWidget(viewModel)
                    MiuixUninstallSystemAppWidget(viewModel)
                    MiuixUninstallRequireBiometricAuthWidget(viewModel)
                }
            }
            item { SmallTitle(stringResource(R.string.uninstall_call_uninstaller)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 16.dp)
                ) {
                    MiuixNavigationItemWidget(
                        title = stringResource(R.string.uninstall_call_uninstaller_manually),
                        description = stringResource(R.string.uninstall_call_uninstaller_manually_desc)
                    ) {
                        showUninstallInputDialog.value = true
                    }
                }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}