// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.widget.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun BaseItemContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // Read the dynamic shape from the SegmentedColumn environment
    val baseShape = LocalSegmentedItemShape.current
    val backgroundColor = MaterialTheme.colorScheme.surfaceBright

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = backgroundColor,
        shape = baseShape
    ) {
        // Container to keep children aligned and respect the shape
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}