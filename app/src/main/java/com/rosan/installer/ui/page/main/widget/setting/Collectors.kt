package com.rosan.installer.ui.page.main.widget.setting

import android.content.Intent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.rosan.installer.R
import com.rosan.installer.ui.page.main.settings.config.all.AllViewAction
import com.rosan.installer.ui.page.main.settings.config.all.AllViewEvent
import com.rosan.installer.ui.page.main.settings.config.all.AllViewModel
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewEvent
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.util.toast
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LogEventCollector(viewModel: PreferredViewModel) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is PreferredViewEvent.OpenLogShare -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooser = Intent.createChooser(intent, "Share Log")
                    context.startActivity(chooser)
                }

                is PreferredViewEvent.ShareLogFailed -> context.toast(event.error)
                else -> null
            }
        }
    }
}

@Composable
fun DeleteEventCollector(viewModel: AllViewModel, snackBarHostState: SnackbarHostState) {
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AllViewEvent.DeletedConfig -> {
                    val result = snackBarHostState.showSnackbar(
                        message = viewModel.context.getString(R.string.delete_success),
                        actionLabel = viewModel.context.getString(R.string.restore),
                        withDismissAction = true
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.dispatch(
                            AllViewAction.RestoreDataConfig(
                                configEntity = event.configEntity
                            )
                        )
                    }
                }
            }
        }
    }
}