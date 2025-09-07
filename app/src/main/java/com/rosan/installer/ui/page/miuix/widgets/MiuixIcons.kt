package com.rosan.installer.ui.page.miuix.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.Back

/**
 * A standardized back button for the application, styled for MIUIX.
 *
 * This composable uses the Miuix IconButton for a consistent look and feel
 * with the miuix-kmp library, typically used in the 'navigationIcon' slot of a TopAppBar.
 *
 * @param onClick The lambda to be executed when the button is clicked.
 * @param modifier The Modifier to be applied to this button.
 * @param icon The vector asset to be displayed inside the button. Defaults to a back arrow.
 * @param contentDescription The content description for accessibility.
 */
@Composable
fun MiuixBackButton(
    modifier: Modifier = Modifier,
    icon: ImageVector = MiuixIcons.Useful.Back,
    contentDescription: String = stringResource(id = R.string.back),
    onClick: () -> Unit,
) {
    // Use the IconButton from the miuix-kmp library.
    // It is frameless by default, which is the standard style for TopAppBar actions in MIUIX.
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription
        )
    }
}