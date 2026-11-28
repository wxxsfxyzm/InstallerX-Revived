package com.rosan.installer.ui.page.miuix.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppMiuixIcons
import com.rosan.installer.ui.miuix.widgets.MiuixBackButton
import top.yukonga.miuix.kmp.theme.LocalContentColor

@Composable
fun AppMiuixBackButton(
    modifier: Modifier = Modifier,
    icon: ImageVector = AppMiuixIcons.Back,
    iconTint: Color = LocalContentColor.current,
    contentDescription: String = stringResource(id = R.string.back),
    onClick: () -> Unit,
) {
    MiuixBackButton(
        modifier = modifier,
        icon = icon,
        iconTint = iconTint,
        contentDescription = contentDescription,
        onClick = onClick,
    )
}
