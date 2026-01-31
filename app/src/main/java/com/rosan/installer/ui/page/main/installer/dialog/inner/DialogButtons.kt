package com.rosan.installer.ui.page.main.installer.dialog.inner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.rosan.installer.ui.page.main.installer.dialog.DialogInnerParams

@Composable
fun dialogButtons(
    id: String,
    content: (@Composable () -> List<DialogButton>)
) = DialogInnerParams(id) {
    val buttons = content.invoke()
    Column(
        modifier = Modifier.clip(RoundedCornerShape(12.dp)),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val single = if (buttons.size > 2) buttons.size % 2 else buttons.size

        // Render single buttons (top section)
        for (i in 0 until single) {
            InnerButton(buttons[i])
        }

        // Render paired buttons (bottom section)
        for (i in single until buttons.size step 2) {
            Box {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    // Key Change 1: Force the Row to be as tall as its tallest child
                    modifier = Modifier.height(IntrinsicSize.Max)
                ) {
                    buttons[i].let {
                        InnerButton(
                            it,
                            Modifier
                                .weight(it.weight)
                                .fillMaxHeight() // Key Change 2: Stretch to fill the parent Row's height
                        )
                    }
                    buttons[i + 1].let {
                        InnerButton(
                            it,
                            Modifier
                                .weight(it.weight)
                                .fillMaxHeight() // Key Change 2: Stretch here too
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InnerButton(
    button: DialogButton, modifier: Modifier = Modifier
) {
    TextButton(
        button.onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        contentPadding = PaddingValues(16.dp)
    ) {
        Text(button.text)
    }
}