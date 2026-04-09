package com.rosan.installer.ui.page.main.widget.setting

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.ui.page.main.settings.preferred.about.AboutEvent
import com.rosan.installer.ui.page.main.settings.preferred.about.AboutViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdateLoadingIndicator(
    hazeState: HazeState?,
    viewModel: AboutViewModel
) {
    var showUpdateLoading by remember { mutableStateOf(false) }

    BackHandler(enabled = showUpdateLoading) {
        // 这里留空，代表拦截返回事件但不执行任何操作
        // 如果想给用户提示，可以在这里 emit 一个 Toast 事件
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is AboutEvent.ShowUpdateLoading -> showUpdateLoading = true
                is AboutEvent.HideUpdateLoading -> showUpdateLoading = false
                is AboutEvent.ShowInAppUpdateErrorDetail -> showUpdateLoading = false
                else -> null
            }
        }
    }

    AnimatedVisibility(
        visible = showUpdateLoading,
        enter = fadeIn(animationSpec = tween(durationMillis = 300)),
        exit = fadeOut(animationSpec = tween(durationMillis = 300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(hazeState?.let {
                    Modifier.hazeEffect(
                        state = hazeState,
                        style = HazeStyle(
                            backgroundColor = MaterialTheme.colorScheme.surface,
                            tint = HazeTint(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                            blurRadius = 25.dp,
                            noiseFactor = 0f
                        )
                    ) {
                        blurEnabled = true
                    }
                } ?: Modifier)
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* Intercept clicks */ },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ContainedLoadingIndicator(
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    Text(
                        text = stringResource(R.string.updating),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}