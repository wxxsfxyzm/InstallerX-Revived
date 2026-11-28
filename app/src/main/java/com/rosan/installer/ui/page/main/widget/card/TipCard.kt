package com.rosan.installer.ui.page.main.widget.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
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
import com.rosan.installer.ui.feature.settings.config.all.AllViewAction
import com.rosan.installer.ui.feature.settings.config.all.AllViewModel
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.md3.widgets.card.InfoTipCard
import com.rosan.installer.ui.md3.widgets.card.TipCard
import com.rosan.installer.ui.md3.widgets.card.TitleTipCard

@Composable
fun ScopeTipCard(
    viewModel: AllViewModel,
    modifier: Modifier = Modifier,
    cardShape: RoundedCornerShape = RoundedCornerShape(12.dp),
) {
    TipCard(
        modifier = modifier.fillMaxWidth(),
        cardShape = cardShape,
        noPadding = true,
        tipContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = AppIcons.Tip,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiary,
                )
                Text(
                    text = stringResource(R.string.scope_tips),
                    style = MaterialTheme.typography.bodyMediumEmphasized,
                    color = MaterialTheme.colorScheme.onTertiary,
                )
            }
        },
    ) {
        TextButton(
            onClick = { viewModel.dispatch(AllViewAction.UserReadScopeTips) },
            modifier = Modifier
                .align(Alignment.End)
                .padding(bottom = 2.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ),
        ) {
            Text(stringResource(R.string.got_it))
        }
    }
}

@Composable
fun AppInfoTipCard(text: String, modifier: Modifier = Modifier, icon: ImageVector? = AppIcons.Tip, noPadding: Boolean = false) {
    InfoTipCard(text = text, modifier = modifier, icon = icon, noPadding = noPadding)
}

@Composable
fun AppTitleTipCard(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = AppIcons.Tip,
    noPadding: Boolean = false,
) {
    TitleTipCard(title = title, text = text, modifier = modifier, icon = icon, noPadding = noPadding)
}
