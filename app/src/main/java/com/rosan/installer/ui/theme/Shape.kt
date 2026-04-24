// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// Define shapes for the segmented list style.
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
