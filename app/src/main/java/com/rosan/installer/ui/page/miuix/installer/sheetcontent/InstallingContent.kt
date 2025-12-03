package com.rosan.installer.ui.page.miuix.installer.sheetcontent

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.ui.page.main.installer.InstallerViewState
import com.rosan.installer.ui.util.isGestureNavigation
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.patched.ProgressButton
import top.yukonga.miuix.kmp.basic.patched.ProgressButtonDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun InstallingContent(
    state: InstallerViewState.Installing,
    baseEntity: AppEntity.BaseEntity?,
    appIcon: Drawable?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val displayLabel = state.appLabel ?: baseEntity?.label ?: "Unknown App"

        AppInfoSlot(
            icon = appIcon,
            label = state.appLabel ?: baseEntity?.label ?: "Unknown App",
            packageName = baseEntity?.packageName ?: "unknown.package"
        )
        Spacer(modifier = Modifier.height(32.dp))

        val progressText = if (state.total > 1) {
            // Batch install: "Installing AppName (1/5)"
            stringResource(
                R.string.installing_progress_text,
                displayLabel,
                state.current,
                state.total
            )
        } else null

        progressText?.let {
            Text(
                text = it,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }

        // Animate the progress value just like in InstallPreparingContent
        val animatedProgress by animateFloatAsState(
            targetValue = state.progress,
            animationSpec = tween(durationMillis = 300),
            label = "ProgressAnimation"
        )

        // Dynamic content color based on progress fill
        val contentColor = if (animatedProgress < 0.45f)
            MiuixTheme.colorScheme.onSecondaryVariant
        else
            MiuixTheme.colorScheme.onPrimary

        ProgressButton(
            progress = animatedProgress,
            onClick = {}, // Action is disabled during installation
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = if (isGestureNavigation()) 24.dp else 0.dp),
            colors = ProgressButtonDefaults.progressButtonColors(
                trackColor = MiuixTheme.colorScheme.secondaryVariant,
                progressColor = MiuixTheme.colorScheme.primary,
                contentColor = contentColor
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InfiniteProgressIndicator(
                    color = contentColor
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    color = contentColor,
                    text = stringResource(R.string.installer_installing)
                )
            }
        }
    }
}