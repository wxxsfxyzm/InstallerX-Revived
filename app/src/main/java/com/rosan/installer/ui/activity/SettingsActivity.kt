package com.rosan.installer.ui.activity

import android.content.Context
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import androidx.core.text.HtmlCompat
import com.rosan.installer.R
import com.rosan.installer.ui.page.settings.SettingsPage
import com.rosan.installer.ui.theme.InstallerTheme
import org.koin.core.component.KoinComponent

class SettingsActivity : ComponentActivity(), KoinComponent {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // A surface based on material design theme.
            InstallerTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    /AgreementDialog()
                    SettingsPage()
                }
            }
        }
    }

    @Composable
    private fun AgreementDialog() {
        val preferences = LocalContext.current.getSharedPreferences("app", Context.MODE_PRIVATE)
        var agreed by remember {
            mutableStateOf(preferences.getBoolean("agreement", false))
        }
        preferences.edit {
            putBoolean("agreement", agreed)
            commit()
        }
        if (agreed) return

        AlertDialog(onDismissRequest = { }, title = {
            Text(text = stringResource(id = R.string.agreement_title))
        }, text = {
            val textColor = AlertDialogDefaults.textContentColor.toArgb()
            AndroidView(factory = {
                TextView(it).apply {
                    setTextColor(textColor)
                    movementMethod = LinkMovementMethod.getInstance()
                    text = HtmlCompat.fromHtml(
                        context.getString(R.string.agreement_text),
                        HtmlCompat.FROM_HTML_MODE_COMPACT
                    )
                }
            })
        }, dismissButton = {
            TextButton(onClick = {
                this@SettingsActivity.finish()
            }) {
                Text(text = stringResource(id = R.string.cancel))
            }
        }, confirmButton = {
            TextButton(onClick = {
                agreed = true
            }) {
                Text(text = stringResource(id = R.string.agree))
            }
        })
    }
}