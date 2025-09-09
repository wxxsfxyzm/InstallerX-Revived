package com.rosan.installer.ui.page.main.widget.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * 一个设置分组容器，它将多个设置项在视觉上“拼接”在一起。
 * 能够根据子项的位置（顶部、中间、底部）自动应用不同的圆角形状和分割线。
 *
 * @param title 分组的标题，显示在内容上方。
 * @param content 一个 Composable 函数的列表，列表中的每一项都是一个设置项。
 * @param modifier Modifier 会被应用到整个分组。
 */
@Composable
fun SplicedColumnGroup(
    modifier: Modifier = Modifier,
    title: String = "",
    content: List<@Composable () -> Unit>,
) {
    if (content.isEmpty()) return

    val cornerRadius = 16.dp
    val connectionRadius = 5.dp
    // Define shapes for different positions.
    val topShape = RoundedCornerShape(
        topStart = cornerRadius,
        topEnd = cornerRadius,
        bottomStart = connectionRadius,
        bottomEnd = connectionRadius
    )
    val middleShape = RoundedCornerShape(connectionRadius)
    val bottomShape = RoundedCornerShape(
        topStart = connectionRadius,
        topEnd = connectionRadius,
        bottomStart = cornerRadius,
        bottomEnd = cornerRadius
    )
    val singleShape = RoundedCornerShape(cornerRadius)

    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        // Group title
        if (title != "")
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )

        // The container for setting items.
        Column(
            modifier = Modifier.clip(
                // Clip the whole column to ensure content stays within the rounded bounds.
                if (content.size == 1) singleShape else RoundedCornerShape(cornerRadius)
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            content.forEachIndexed { index, itemContent ->
                // Determine the shape based on the pkg's position.
                val shape = when {
                    content.size == 1 -> singleShape
                    index == 0 -> topShape
                    index == content.size - 1 -> bottomShape
                    else -> middleShape
                }

                // Apply background with the correct shape to the pkg.
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceBright, shape)
                ) {
                    itemContent()
                }
            }
        }
    }
}