package com.rosan.installer.ui.page.main.widget.setting

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewEvent
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.util.toast

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