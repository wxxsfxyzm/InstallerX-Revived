package com.rosan.installer.ui.page.installer.dialog.inner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.installer.dialog.DialogViewModel
import com.rosan.installer.util.help

val pausingIcon: @Composable () -> Unit = {
    Icon(
        imageVector = AppIcons.Pausing, contentDescription = null
    )
}

val workingIcon: @Composable () -> Unit = {
    Icon(
        imageVector = AppIcons.Working, contentDescription = null
    )
}

val menuIcon: @Composable () -> Unit = {
    Icon(
        imageVector = AppIcons.MenuOpen, contentDescription = null
    )
}

val errorTextBlock: ((installer: InstallerRepo, viewModel: DialogViewModel) -> (@Composable () -> Unit)) =
    { installer, viewModel ->
        {
            var expanded by remember { mutableStateOf(false) }
            val rotation by animateFloatAsState(targetValue = if (expanded) -180f else 0f)

            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onErrorContainer) {
                LazyColumn(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    expanded = !expanded
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = AppIcons.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .rotate(rotation)
                            )
                            Text(
                                text = installer.error.help(),
                                fontWeight = FontWeight.Bold,
                                maxLines = Int.MAX_VALUE
                            )
                        }

                    }
                    item {
                        AnimatedVisibility(
                            visible = expanded,
                            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top), // 进入动画：淡入 + 垂直展开
                            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)  // 退出动画：淡出 + 垂直收起
                        ) {
                            BasicTextField(
                                value = installer.error.stackTraceToString().trim(),
                                onValueChange = {},
                                readOnly = true
                            )
                        }
                    }
                }
            }
        }
    }