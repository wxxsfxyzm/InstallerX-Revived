// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.widget.util

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rosan.installer.R
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.navigation.Navigator
import com.rosan.installer.ui.navigation.Route
import com.rosan.installer.ui.page.main.installer.InstallerViewEvent
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.settings.config.all.AllViewAction
import com.rosan.installer.ui.page.main.settings.config.all.AllViewEvent
import com.rosan.installer.ui.page.main.settings.config.all.AllViewModel
import com.rosan.installer.ui.page.main.settings.config.edit.EditViewEvent
import com.rosan.installer.ui.page.main.settings.config.edit.EditViewModel
import com.rosan.installer.ui.page.main.settings.preferred.about.AboutEvent
import com.rosan.installer.ui.page.main.settings.preferred.about.AboutViewModel
import com.rosan.installer.util.getErrorMessage
import com.rosan.installer.util.toast
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

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
fun AllViewEventCollector(
    viewModel: AllViewModel,
    navigator: Navigator,
    onShowSnackbar: suspend (message: String, actionLabel: String) -> Boolean
) {
    val deleteSuccessString = stringResource(id = R.string.delete_success)
    val restoreString = stringResource(id = R.string.restore)

    LaunchedEffect(viewModel, navigator) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is AllViewEvent.DeletedConfig -> {
                    // Trigger the UI-specific snackbar and check if action was clicked
                    val actionPerformed = onShowSnackbar(deleteSuccessString, restoreString)
                    if (actionPerformed) {
                        viewModel.dispatch(
                            AllViewAction.RestoreDataConfig(configModel = event.configModel)
                        )
                    }
                }

                is AllViewEvent.NavigateToEditConfig -> {
                    navigator.push(Route.EditConfig(event.id))
                }

                is AllViewEvent.NavigateToApplyConfig -> {
                    navigator.push(Route.ApplyConfig(event.id))
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

@Composable
fun InstallerEventCollector(viewModel: InstallerViewModel) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is InstallerViewEvent.ShowToast -> context.toast(event.message)

                is InstallerViewEvent.ShowToastRes -> context.toast(event.messageResId)

                is InstallerViewEvent.ShowErrorToast -> context.toast(event.error.getErrorMessage(context))

                is InstallerViewEvent.ShareFile -> {
                    try {
                        // Parse the original URI string back into an android.net.Uri
                        val uri = event.uriString.toUri()

                        // Directly use the URI in the intent
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = event.mimeType
                            putExtra(Intent.EXTRA_STREAM, uri)
                            // Grant read permission so the receiving app can access the URI
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }

                        val chooser = Intent.createChooser(shareIntent, null)
                        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                        context.startActivity(chooser)

                    } catch (e: Exception) {
                        Timber.e(e, "Failed to share file via URI")
                        context.toast("Failed to share file")
                    }
                }
            }
        }
    }
}

@Composable
fun EditEventCollector(
    viewModel: EditViewModel,
    snackBarHostState: SnackbarHostState
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val unknownErrorString = stringResource(R.string.installer_unknown_error)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is EditViewEvent.SnackBar -> {
                    // Resolve priority:
                    // 1. Specific Resource ID
                    // 2. Explicit String message
                    // 3. Localized generic fallback
                    val snackBarText = event.messageResId?.let { @SuppressLint("LocalContextGetResourceValueCall") context.getString(it) }
                        ?: event.message
                        ?: unknownErrorString

                    snackBarHostState.showSnackbar(
                        message = snackBarText,
                        withDismissAction = true,
                    )
                }

                is EditViewEvent.Saved -> {
                    navigator.pop()
                }
            }
        }
    }
}
