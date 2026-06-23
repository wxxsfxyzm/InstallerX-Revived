package com.rosan.installer.ui.page.miuix.installer.sheetcontent

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.ui.page.main.installer.InstallerStage
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.miuix.widgets.ProgressButton
import com.rosan.installer.ui.page.miuix.widgets.ProgressButtonDefaults
import com.rosan.installer.ui.util.isGestureNavigation
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor

@Composable
fun InstallPreparingContent(
    viewModel: InstallerViewModel,
    onBackground: () -> Unit,
    @StringRes descriptionRes: Int = R.string.installer_preparing_desc,
    descriptionText: String? = null,
    @StringRes buttonTextRes: Int = R.string.loading,
    showButton: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stage = uiState.stage
    // Extract progress via Smart Cast from the stage
    val progress = if (stage is InstallerStage.Preparing) {
        stage.progress
    } else {
        -1f // Default to indeterminate if state is wrong
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300),
        label = "ProgressAnimation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.5f)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            InfiniteProgressIndicator()

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = descriptionText ?: stringResource(descriptionRes),
                color = MiuixTheme.colorScheme.onSurface,
                style = MiuixTheme.textStyles.body1
            )
        }

        val contentColor = if (animatedProgress < 0.45f)
            if (isDynamicColor) MiuixTheme.colorScheme.onSecondaryContainer else MiuixTheme.colorScheme.onSecondaryVariant
        else
            MiuixTheme.colorScheme.onPrimary

        if (showButton) {
            ProgressButton(
                progress = animatedProgress,
                onClick = onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = if (isGestureNavigation()) 24.dp else 0.dp),
                colors = ProgressButtonDefaults.progressButtonColors(
                    trackColor = if (isDynamicColor) MiuixTheme.colorScheme.secondaryContainer else MiuixTheme.colorScheme.secondaryVariant,
                    progressColor = MiuixTheme.colorScheme.primary,
                    contentColor = contentColor
                )
            ) {
                Text(
                    color = contentColor,
                    text = stringResource(buttonTextRes)
                )
            }
        }
    }
}
