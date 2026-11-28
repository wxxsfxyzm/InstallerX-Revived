// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.slider.Slider
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import kotlin.math.roundToInt

@Composable
fun CouiIntNumberPickerWidget(
    title: String,
    description: String? = null,
    enabled: Boolean = true,
    value: Int,
    startInt: Int,
    endInt: Int,
    valueSuffix: String = "",
    subduedValue: Boolean = false,
    onValueClick: (() -> Unit)? = null,
    onValueChange: (Int) -> Unit,
) {
    val pageColors = CouiPageDefaults.themeColors()
    val steps = (endInt - startInt - 1).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                kind = CouiTextKind.ItemTitle,
                color = if (enabled) pageColors.title else pageColors.disabled,
            )
            description?.let {
                Text(
                    text = it,
                    kind = CouiTextKind.ItemSummary,
                    color = if (enabled) pageColors.summary else pageColors.disabled,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Slider(
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = title },
                value = value.coerceIn(startInt, endInt).toFloat(),
                onValueChange = { onValueChange(it.roundToInt()) },
                valueRange = startInt.toFloat()..endInt.toFloat(),
                steps = steps,
                showTickMarks = false,
                enabled = enabled,
            )

            Spacer(modifier = Modifier.width(14.dp))

            val valueModifier = Modifier
                .defaultMinSize(minWidth = 36.dp)
                .then(if (onValueClick != null) Modifier.clickable(onClick = onValueClick) else Modifier)
                .padding(horizontal = 4.dp, vertical = 6.dp)

            Text(
                text = "$value$valueSuffix",
                kind = CouiTextKind.ItemValue,
                color = when {
                    !enabled -> pageColors.disabled
                    subduedValue -> pageColors.summary
                    else -> pageColors.accent
                },
                modifier = valueModifier,
            )
        }
    }
}
