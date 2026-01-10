package com.rosan.installer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// Define shapes for the spliced list style.
val CornerRadius = 16.dp
val ConnectionRadius = 5.dp

val topShape = RoundedCornerShape(
    topStart = CornerRadius,
    topEnd = CornerRadius,
    bottomStart = ConnectionRadius,
    bottomEnd = ConnectionRadius
)
val middleShape = RoundedCornerShape(ConnectionRadius)
val bottomShape = RoundedCornerShape(
    topStart = ConnectionRadius,
    topEnd = ConnectionRadius,
    bottomStart = CornerRadius,
    bottomEnd = CornerRadius
)
val singleShape = RoundedCornerShape(CornerRadius)