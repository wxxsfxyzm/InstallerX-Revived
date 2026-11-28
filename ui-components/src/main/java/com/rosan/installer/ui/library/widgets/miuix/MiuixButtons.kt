package com.rosan.installer.ui.library.widgets.miuix

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.theme.LocalContentColor

/**
 * A standardized back button for navigation, styled for MIUIX.
 *
 * @param onClick The lambda to be executed when the button is clicked.
 * @param modifier The Modifier to be applied to this button.
 * @param icon The vector asset to be displayed inside the button.
 * @param iconTint The tint to be applied to the icon. Defaults to the current content color.
 * @param contentDescription The content description for accessibility.
 */
@Composable
fun MiuixBackButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color = LocalContentColor.current,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
        )
    }
}
