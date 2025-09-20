package com.rosan.installer.ui.page.miuix.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

@Composable
fun MiuixIntNumberPickerWidget(
    title: String,
    description: String? = null,
    enabled: Boolean = true,
    value: Int,
    startInt: Int,
    endInt: Int,
    onValueChange: (Int) -> Unit
) {
    // The main Column now fills width instead of the whole screen, which is more typical for a settings item.
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // This Box contains the title and description text.
            Box(modifier = Modifier.weight(1f)) {
                Column {
                    Text(
                        text = title,
                        color = MiuixTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium
                    )
                    description?.let {
                        Text(
                            text = it,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary, // Use a slightly different color for description
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            // Adjusted padding to align with standard list item specs.
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // The custom Miuix Slider component replaces the Material Slider.
            Slider(
                progress = value.toFloat(),
                onProgressChange = {
                    // The slider's callback provides a Float, which we round to the nearest Int.
                    onValueChange(it.roundToInt())
                },
                modifier = Modifier.weight(1f),
                enabled = enabled,
                minValue = startInt.toFloat(),
                maxValue = endInt.toFloat(),
                // Setting decimalPlaces to 0 makes the slider behave like an integer selector.
                decimalPlaces = 0,
                // Use the slider's built-in haptic feedback for step changes.
                hapticEffect = SliderDefaults.SliderHapticEffect.Step
            )
            Spacer(modifier = Modifier.width(16.dp))
            // The current integer value is displayed next to the slider.
            Text(
                text = value.toString(),
                style = MiuixTheme.textStyles.button,
                color = MiuixTheme.colorScheme.primary
            )
        }
    }
}