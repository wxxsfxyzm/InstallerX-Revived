package com.rosan.installer.ui.widget.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropDownMenuWidget(
    icon: ImageVector? = null,
    title: String,
    description: String? = null,
    enabled: Boolean = true,
    choice: Int,
    data: List<String>,
    onChoiceChange: (Int) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    BaseWidget(
        icon = icon,
        title = title,
        description = description,
        enabled = enabled,
        onClick = {
            expanded = !expanded
        },
        foreContent = {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
            ) {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    data.forEachIndexed { index, item ->
                        val backgroundColor =
                            if (index == choice) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
                        DropdownMenuItem(
                            modifier = Modifier.background(backgroundColor),
                            text = { Text(text = item) },
                            onClick = {
                                onChoiceChange(index)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    ) {
    }
}
