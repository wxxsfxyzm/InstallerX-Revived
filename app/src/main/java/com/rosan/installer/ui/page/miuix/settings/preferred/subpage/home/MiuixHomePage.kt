package com.rosan.installer.ui.page.miuix.settings.preferred.subpage.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.rosan.installer.R
import com.rosan.installer.build.RsConfig
import com.rosan.installer.build.model.entity.Level
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewAction
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewEvent
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.page.miuix.widgets.ErrorDisplaySheet
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.page.miuix.widgets.MiuixNavigationItemWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixUpdateDialog
import com.rosan.installer.util.openUrl
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun MiuixHomePage(
    navController: NavController,
    viewModel: PreferredViewModel
) {
    val context = LocalContext.current
    val state = viewModel.state
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

    val showLoadingDialog = remember { mutableStateOf(false) }
    val showUpdateErrorDialog = remember { mutableStateOf(false) }
    var updateErrorInfo by remember { mutableStateOf<PreferredViewEvent.ShowInAppUpdateErrorDetail?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is PreferredViewEvent.ShowUpdateLoading -> {
                    showLoadingDialog.value = true
                }

                is PreferredViewEvent.HideUpdateLoading -> {
                    showLoadingDialog.value = false
                }

                is PreferredViewEvent.ShowInAppUpdateErrorDetail -> {
                    showLoadingDialog.value = false
                    updateErrorInfo = event
                    showUpdateErrorDialog.value = true
                }

                else -> {}
            }
        }
    }

    Scaffold(
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
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .scrollEndHaptic()
                .overScrollVertical()
                .padding(top = paddingValues.calculateTopPadding()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.size(48.dp)) }

            item {
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
            }
            item {
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MiuixTheme.textStyles.title2,
                )
            }
            item {
                Text(
                    text = "$internetAccessHint$level ${RsConfig.VERSION_NAME} (${RsConfig.VERSION_CODE})",
                    style = MiuixTheme.textStyles.subtitle,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
            if (state.hasUpdate) item {
                Text(
                    text = stringResource(R.string.update_available, state.remoteVersion),
                    style = MiuixTheme.textStyles.subtitle,
                    color = MiuixTheme.colorScheme.primary
                )
            }
            item { Spacer(modifier = Modifier.size(12.dp)) }

            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
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
                    if (state.hasUpdate)
                        MiuixNavigationItemWidget(
                            title = stringResource(R.string.get_update_directly),
                            description = stringResource(R.string.get_update_directly_desc),
                            onClick = { viewModel.dispatch(PreferredViewAction.Update) }
                        )
                }
            }
        }
    }

    SuperDialog(
        show = showLoadingDialog
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfiniteProgressIndicator()
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.updating)
            )
        }
    }

    updateErrorInfo?.let { sheetInfo ->
        ErrorDisplaySheet(
            title = sheetInfo.title,
            showState = showUpdateErrorDialog,
            exception = sheetInfo.exception,
            onDismissRequest = {
                showUpdateErrorDialog.value = false
                updateErrorInfo = null
            }
        )
    }
}