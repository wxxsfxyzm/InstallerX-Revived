package com.rosan.installer.ui.page.installer.dialog.inner

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.installer.dialog.DialogParams
import com.rosan.installer.ui.page.installer.dialog.DialogParamsType
import com.rosan.installer.ui.page.installer.dialog.DialogViewAction
import com.rosan.installer.ui.page.installer.dialog.DialogViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun installingDialog( // 小写开头
    installer: InstallerRepo, viewModel: DialogViewModel
): DialogParams {
    val preInstallAppInfo by viewModel.preInstallAppInfo.collectAsState()

    // Call InstallInfoDialog for base structure (icon, title, subtitle with new version)
    val baseParams = InstallInfoDialog(
        installer = installer,
        viewModel = viewModel,
        preInstallAppInfo = preInstallAppInfo,
        onTitleExtraClick = {}
    )

    // Override text and buttons
    return baseParams.copy(
        text = DialogInnerParams(
            DialogParamsType.InstallerInstalling.id
        ) {
            // --- M3E ---
            // LinearProgressIndicator(Modifier.fillMaxWidth())
            LinearWavyProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                amplitude = 0.5f
            )
        },
        buttons = DialogButtons(DialogParamsType.ButtonsCancel.id) {
            listOf(
                DialogButton(stringResource(R.string.cancel)) {
                    viewModel.dispatch(DialogViewAction.Close)
                }
            )
        }
    )
}
