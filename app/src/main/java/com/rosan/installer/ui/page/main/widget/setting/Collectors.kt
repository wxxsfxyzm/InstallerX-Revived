package com.rosan.installer.ui.page.main.widget.setting

import android.content.Intent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rosan.installer.R
import com.rosan.installer.ui.page.main.settings.config.all.AllViewAction
import com.rosan.installer.ui.page.main.settings.config.all.AllViewEvent
import com.rosan.installer.ui.page.main.settings.config.all.AllViewModel
import com.rosan.installer.ui.page.main.settings.preferred.subpage.about.AboutEvent
import com.rosan.installer.ui.page.main.settings.preferred.subpage.about.AboutViewModel
import com.rosan.installer.util.toast
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LogEventCollector(viewModel: AboutViewModel) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is AboutEvent.OpenLogShare -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooser = Intent.createChooser(intent, "Share Log")
                    context.startActivity(chooser)
                }

                is AboutEvent.ShareLogFailed -> context.toast(event.error)
                else -> Unit
            }
        }
    }
}

@Composable
fun DeleteEventCollector(viewModel: AllViewModel, snackBarHostState: SnackbarHostState) {
    val snackbarString = stringResource(R.string.delete_success)
    val actionLabel = stringResource(R.string.restore)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AllViewEvent.DeletedConfig -> {
                    val result = snackBarHostState.showSnackbar(
                        message = snackbarString,
                        actionLabel = actionLabel,
                        withDismissAction = true
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.dispatch(
                            AllViewAction.RestoreDataConfig(
                                configModel = event.configModel
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * A generic Composable to listen for specific lifecycle events.
 *
 * @param event The lifecycle event to listen for (e.g., Lifecycle.Event.ON_RESUME).
 * @param lifecycleOwner The lifecycle owner, defaults to the current LocalLifecycleOwner.
 * @param onEvent The action to perform when the event occurs.
 */
@Composable
fun OnLifecycleEvent(
    event: Lifecycle.Event,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onEvent: () -> Unit
) {
    // Use rememberUpdatedState to ensure the latest lambda is captured
    // without restarting the effect if the lambda reference changes.
    val currentOnEvent by rememberUpdatedState(onEvent)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, e ->
            if (e == event) {
                currentOnEvent()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}