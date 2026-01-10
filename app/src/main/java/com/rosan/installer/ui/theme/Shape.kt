package com.rosan.installer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// Define shapes for the spliced list style.
private val cornerRadius = 16.dp
private val connectionRadius = 5.dp

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