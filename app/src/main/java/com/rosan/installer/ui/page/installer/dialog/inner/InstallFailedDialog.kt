package com.rosan.installer.ui.page.installer.dialog.inner

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.installer.dialog.* // 使用 * 导入

// 假设 errorText 在 dialog 包下可访问
// import com.rosan.installer.ui.page.installer.dialog.errorText

@Composable
fun installFailedDialog( // 小写开头
    installer: InstallerRepo, viewModel: DialogViewModel
): DialogParams {
    val context = LocalContext.current

    val preInstallAppInfo by viewModel.preInstallAppInfo.collectAsState()
    val currentPackageName by viewModel.currentPackageName.collectAsState()
    val packageName = currentPackageName ?: installer.entities.filter { it.selected }.map { it.app }
        .firstOrNull()?.packageName ?: ""

    return InstallInfoDialog( // 调用大写开头的 InstallInfoDialog
        installer = installer,
        viewModel = viewModel,
        preInstallAppInfo = preInstallAppInfo,
        onTitleExtraClick = {
            if (packageName.isNotEmpty()) {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.fromParts("package", packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    ).copy(
        text = DialogInnerParams(
            DialogParamsType.InstallerInstallFailed.id,
            errorText(installer, viewModel) // 假设 errorText 可访问
        ),
        buttons = DialogButtons(
            DialogParamsType.InstallerInstallFailed.id
        ) {
            listOf(
                DialogButton(stringResource(R.string.previous)) {
                    viewModel.dispatch(DialogViewAction.InstallPrepare)
                },
                DialogButton(stringResource(R.string.cancel)) {
                    viewModel.dispatch(DialogViewAction.Close)
                }
            )
        }
    )
}
