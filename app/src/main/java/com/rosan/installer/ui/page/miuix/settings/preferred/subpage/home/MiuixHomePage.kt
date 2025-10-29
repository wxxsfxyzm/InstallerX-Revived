package com.rosan.installer.ui.page.miuix.settings.preferred.subpage.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.rosan.installer.R
import com.rosan.installer.build.Level
import com.rosan.installer.build.RsConfig
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.page.miuix.widgets.MiuixNavigationItemWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixUpdateDialog
import com.rosan.installer.util.openUrl
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun MiuixHomePage(
    navController: NavController
) {
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior()
    val showUpdateDialog = remember { mutableStateOf(false) }

    val internetAccessHint = if (RsConfig.isInternetAccessEnabled) stringResource(R.string.internet_access_enabled)
    else stringResource(R.string.internet_access_disabled)
    val level = when (RsConfig.LEVEL) {
        Level.STABLE -> stringResource(id = R.string.stable)
        Level.PREVIEW -> stringResource(id = R.string.preview)
        Level.UNSTABLE -> stringResource(id = R.string.unstable)
    }

    MiuixUpdateDialog(
        showState = showUpdateDialog,
        onDismiss = { showUpdateDialog.value = false }
    )

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        topBar = {
            TopAppBar(
                title = stringResource(id = R.string.about),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    MiuixBackButton(
                        modifier = Modifier.padding(start = 16.dp),
                        onClick = { navController.navigateUp() })
                }
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .scrollEndHaptic()
                .overScrollVertical()
                .padding(top = innerPadding.calculateTopPadding()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.size(48.dp))

            Image(
                modifier = Modifier.size(80.dp),
                painter = rememberDrawablePainter(
                    drawable = ContextCompat.getDrawable(
                        LocalContext.current,
                        R.mipmap.ic_launcher
                    )
                ),
                contentDescription = stringResource(id = R.string.app_name)
            )
            Text(
                text = stringResource(id = R.string.app_name),
                style = MiuixTheme.textStyles.title2,
            )
            Text(
                text = "$internetAccessHint$level ${RsConfig.VERSION_NAME} (${RsConfig.VERSION_CODE})",
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )

            Spacer(modifier = Modifier.size(12.dp))

            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 6.dp)
            ) {
                MiuixNavigationItemWidget(
                    title = stringResource(R.string.get_source_code),
                    description = stringResource(R.string.get_source_code_detail),
                    onClick = { context.openUrl("https://github.com/wxxsfxyzm/InstallerX-Revived") }
                )
                MiuixNavigationItemWidget(
                    title = stringResource(R.string.get_update),
                    description = stringResource(R.string.get_update_detail),
                    onClick = { showUpdateDialog.value = true }
                )
            }
        }

    }
}