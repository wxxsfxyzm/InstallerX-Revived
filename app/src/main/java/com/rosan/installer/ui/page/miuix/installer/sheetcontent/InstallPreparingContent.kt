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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.InstallerViewState
import com.rosan.installer.ui.util.isGestureNavigation
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.patched.ProgressButton
import top.yukonga.miuix.kmp.basic.patched.ProgressButtonDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun InstallPreparingContent(
    viewModel: InstallerViewModel,
    onCancel: () -> Unit
) {
    val currentState = viewModel.state
    val progress = if (currentState is InstallerViewState.Preparing) {
        currentState.progress
    } else {
        0f
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
                text = stringResource(R.string.installer_preparing_desc),
                style = MiuixTheme.textStyles.body1
            )
        }

        val contentColor = if (animatedProgress < 0.45f)
            MiuixTheme.colorScheme.onSecondaryVariant
        else
            MiuixTheme.colorScheme.onPrimary

        ProgressButton(
            progress = animatedProgress,
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = if (isGestureNavigation()) 24.dp else 0.dp),
            colors = ProgressButtonDefaults.progressButtonColors(
                trackColor = MiuixTheme.colorScheme.secondaryVariant,
                progressColor = MiuixTheme.colorScheme.primary,
                contentColor = contentColor
            )
        ) {
            Text(
                color = contentColor,
                text = stringResource(R.string.loading)
            )
        }
    }
}