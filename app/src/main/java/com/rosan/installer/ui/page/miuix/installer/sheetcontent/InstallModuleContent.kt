package com.rosan.installer.ui.page.miuix.installer.sheetcontent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rosan.installer.R
import com.rosan.installer.ui.theme.LocalIsDark
import com.rosan.installer.ui.util.KeyEventBlocker
import com.rosan.installer.ui.util.isGestureNavigation
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor

/**
 * A Composable that displays the real-time output of a module installation process.
 * It features a scrollable log area that automatically follows new output,
 * and a close button.
 *
 * @param colorScheme The color scheme for the UI.
 * @param isDarkMode Whether the UI is in dark mode.
 * @param outputLines The list of log lines to display.
 * @param isFinished Whether the installation process has completed.
 * @param onClose Lambda to be invoked when the close button is clicked.
 */
@Composable
fun InstallModuleContent(
    outputLines: List<String>,
    isFinished: Boolean,
    onClose: () -> Unit
) {
    KeyEventBlocker {
        it.key == Key.VolumeDown || it.key == Key.VolumeUp
    }
    val lazyListState = rememberLazyListState()

    // Coroutine to auto-scroll to the bottom as new lines are added,
    // but ONLY if the installation is not yet finished.
    LaunchedEffect(outputLines.size) {
        if (!isFinished && outputLines.isNotEmpty()) {
            lazyListState.animateScrollToItem(index = outputLines.size - 1)
        }
    }

    val isDarkMode = LocalIsDark.current
    val cardColor = if (isDynamicColor) MiuixTheme.colorScheme.surfaceContainer else
        if (isDarkMode) Color.Black else Color.White

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // The main content area for log output. It will grow with content.
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 300.dp)
                .weight(1f, fill = false),
            colors = CardColors(
                color = cardColor,
                contentColor = MiuixTheme.colorScheme.onSurface
            )
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
            ) {
                items(outputLines) { line ->
                    Text(
                        text = line,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        // Color-code the output: lines starting with "ERROR:" will be red.
                        color = if (line.startsWith("ERROR:")) Color.Red else MiuixTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // The button at the bottom changes based on the finished state.
        if (isFinished) {
            Button(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(top = 24.dp, bottom = if (isGestureNavigation()) 24.dp else 0.dp),
            ) {
                Text(stringResource(R.string.close))
            }
        } else {
            Button(
                enabled = false,
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(top = 24.dp, bottom = if (isGestureNavigation()) 24.dp else 0.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InfiniteProgressIndicator()
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = stringResource(R.string.installer_installing))
                }
            }
        }
    }
}