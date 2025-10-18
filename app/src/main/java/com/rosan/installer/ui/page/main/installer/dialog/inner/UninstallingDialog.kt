package com.rosan.installer.ui.page.main.installer.dialog.inner

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.main.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParamsType
import com.rosan.installer.ui.page.main.installer.dialog.InstallerViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun uninstallingDialog(
    installer: InstallerRepo, viewModel: InstallerViewModel
): DialogParams {
    // val progressTextResource by viewModel.installProgressText.collectAsState()
    val progress by viewModel.installProgress.collectAsState()

    // Call InstallInfoDialog for base structure (icon, title, subtitle with new version)
    val baseParams = uninstallInfoDialog(
        viewModel = viewModel,
        onTitleExtraClick = {}
    )

    // Override text and buttons
    return baseParams.copy(
        text = DialogInnerParams(
            DialogParamsType.InstallerInstalling.id
        ) {
            Column {
                /*// Show the progress text if available
                progressTextResource?.let { uiText ->
                    // use stringResource to format the text
                    val formattedText = stringResource(
                        id = uiText.id,
                        *uiText.formatArgs.toTypedArray() // spread operator (*)
                    )
                    Text(
                        text = formattedText,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }*/
                Text(
                    text = "Uninstalling...", //stringResource(R.string.install_progress_text_uninstalling),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                // --- M3E ---
                val currentProgress = progress
                if (currentProgress != null) {
                    // Multi-APK ZIP has specified progress
                    val animatedProgress by animateFloatAsState(
                        targetValue = currentProgress,
                        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                        label = "ProgressBarAnimation"
                    )

                    LinearWavyProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxWidth(),
                        amplitude = { 0f }
                    )
                } else {
                    // other method have unspecified progress
                    LinearWavyProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        amplitude = 0f // not wavy
                    )
                }
            }
        },
        buttons = DialogButtons(DialogParamsType.ButtonsCancel.id) {
            emptyList()
        }
    )
}
