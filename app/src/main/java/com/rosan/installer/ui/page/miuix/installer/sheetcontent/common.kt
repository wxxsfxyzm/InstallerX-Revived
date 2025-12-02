package com.rosan.installer.ui.page.miuix.installer.sheetcontent

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppInfoSlot(
    icon: Drawable?,
    label: String,
    packageName: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            painter = rememberDrawablePainter(drawable = icon),
            contentDescription = "App Icon",
            modifier = Modifier.size(72.dp)
        )
        Text(label, style = MiuixTheme.textStyles.title2, modifier = Modifier.basicMarquee())
        Text(packageName, style = MiuixTheme.textStyles.subtitle)
    }
}