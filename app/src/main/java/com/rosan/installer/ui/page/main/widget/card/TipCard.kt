package com.rosan.installer.ui.page.main.widget.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.config.all.AllViewAction
import com.rosan.installer.ui.page.main.settings.config.all.AllViewModel

/**
 * A general-purpose card for displaying tips or important information.
 *
 * @param modifier The modifier to be applied to the card.
 * @param tipContent The composable slot for the main tip message.
 * This is typically a Row with an Icon and Text.
 * @param actionContent The composable slot for action buttons, typically aligned to the end.
 * It's within a ColumnScope, so modifiers like `align` are allowed.
 * @param noPadding Don't add more padding in internal
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TipCard(
    modifier: Modifier = Modifier,
    cardShape: RoundedCornerShape = RoundedCornerShape(16.dp),
    noPadding: Boolean = false,
    tipContent: @Composable () -> Unit,
    actionContent: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = if (noPadding) {
            modifier
                .fillMaxWidth()
        } else {
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)
        ),
        shape = cardShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp)
        ) {
            tipContent()
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = actionContent
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScopeTipCard(
    viewModel: AllViewModel,
    cardShape: RoundedCornerShape = RoundedCornerShape(12.dp),
) {
    TipCard(
        cardShape = cardShape,
        tipContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = AppIcons.Tip,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiary
                )
                Text(
                    text = stringResource(R.string.scope_tips),
                    style = MaterialTheme.typography.bodyMediumEmphasized,
                    color = MaterialTheme.colorScheme.onTertiary
                )
            }
        }
    ) {
        TextButton(
            onClick = { viewModel.dispatch(AllViewAction.UserReadScopeTips) },
            modifier = Modifier
                .align(Alignment.End)
                .padding(bottom = 2.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
        ) {
            Text(stringResource(R.string.got_it))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InfoTipCard(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = AppIcons.Tip,
    noPadding: Boolean = false
) {
    TipCard(
        modifier = modifier,
        noPadding = noPadding,
        tipContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiary
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMediumEmphasized,
                    color = MaterialTheme.colorScheme.onTertiary
                )
            }
        }
    ) {
        Spacer(modifier = Modifier.size(16.dp))
    }
}