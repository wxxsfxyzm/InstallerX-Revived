package com.rosan.installer.ui.page.installer.dialog.inner

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.installer.dialog.* // 使用 * 导入

@Composable
fun installingDialog( // 小写开头
    installer: InstallerRepo, viewModel: DialogViewModel
): DialogParams {
    val preInstallAppInfo by viewModel.preInstallAppInfo.collectAsState()

    return InstallInfoDialog( // 调用大写开头的 InstallInfoDialog
        installer = installer,
        viewModel = viewModel,
        preInstallAppInfo = preInstallAppInfo,
        onTitleExtraClick = {} // 安装中无额外点击操作
    ).copy(
        text = DialogInnerParams(
            DialogParamsType.InstallerInstalling.id
        ) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
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
