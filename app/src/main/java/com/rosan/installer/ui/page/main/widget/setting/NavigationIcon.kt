package com.rosan.installer.ui.page.main.widget.setting

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppIcons

/**
 * A standardized back button for the application.
 *
 * This composable encapsulates the specific styling for the navigation icon,
 * making it reusable across different screens.
 *
 * @param onClick The lambda to be executed when the button is clicked.
 * @param modifier The Modifier to be applied to this button.
 * @param icon The vector asset to be displayed inside the button. Defaults to a back arrow.
 * @param contentDescription The content description for accessibility.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = AppIcons.ArrowBack, // Default icon is ArrowBack
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.1f),
    contentDescription: String = stringResource(id = R.string.back)
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        // Consistent shapes for the button.
        shapes = IconButtonDefaults.shapes(
            // shape = CircleShape
        ),
        // Consistent colors for the button.
        colors = IconButtonDefaults.iconButtonColors(
            // The color of the icon inside the button.
            contentColor = MaterialTheme.colorScheme.onSurface,
            // The background color of the button.
            // Using a more standard color for a filled icon button variant.
            containerColor = containerColor,
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription
        )
    }
}