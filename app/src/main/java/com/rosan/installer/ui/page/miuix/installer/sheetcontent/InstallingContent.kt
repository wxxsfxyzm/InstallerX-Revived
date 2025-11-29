package com.rosan.installer.ui.page.miuix.installer.sheetcontent

import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.ui.page.main.installer.dialog.inner.UiText
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun InstallingContent(
    colorScheme: ColorScheme,
    baseEntity: AppEntity.BaseEntity?,
    appIcon: Drawable?,
    progressTextRes: UiText?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppInfoSlot(
            icon = appIcon,
            label = baseEntity?.label ?: "Unknown App",
            packageName = baseEntity?.packageName ?: "unknown.package"
        )
        Spacer(modifier = Modifier.height(32.dp))

        progressTextRes?.let { uiText ->
            val formattedText = stringResource(
                id = uiText.id,
                *uiText.formatArgs.toTypedArray()
            )
            Text(
                text = formattedText,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            onClick = {},
            colors = ButtonDefaults.buttonColors(
                color = MiuixTheme.colorScheme.secondaryVariant,
                disabledColor = MiuixTheme.colorScheme.disabledSecondaryVariant
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InfiniteProgressIndicator()
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.installer_installing)
                )
            }
        }
    }
}