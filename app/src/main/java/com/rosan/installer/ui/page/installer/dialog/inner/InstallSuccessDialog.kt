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
import com.rosan.installer.data.common.util.addAll // 假设这个工具函数存在
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.installer.dialog.* // 使用 * 导入

@Composable
fun installSuccessDialog( // 小写开头
    installer: InstallerRepo,
    viewModel: DialogViewModel
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
            viewModel.dispatch(DialogViewAction.Background)
        }
    ).copy(
        buttons = DialogButtons(
            DialogParamsType.InstallerInstallSuccess.id
        ) {
            val list = mutableListOf<DialogButton>()
            val intent = if (packageName.isNotEmpty()) context.packageManager.getLaunchIntentForPackage(packageName) else null
            if (intent != null) {
                list.add(DialogButton(stringResource(R.string.open)) {
                    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    viewModel.dispatch(DialogViewAction.Close)
                })
            }
            list.addAll(
                DialogButton(stringResource(R.string.previous), 2f) {
                    viewModel.dispatch(DialogViewAction.InstallPrepare)
                },
                DialogButton(stringResource(R.string.finish), 1f) {
                    viewModel.dispatch(DialogViewAction.Close)
                }
            )
            return@DialogButtons list
        }
    )
}
