package com.rosan.installer.ui.page.installer.dialog.inner

// 导入需要的库
// 可能需要导入 InstalledAppInfo
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
import com.rosan.installer.ui.page.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.installer.dialog.DialogParams
import com.rosan.installer.ui.page.installer.dialog.DialogParamsType
import com.rosan.installer.ui.page.installer.dialog.DialogViewAction
import com.rosan.installer.ui.page.installer.dialog.DialogViewModel

@Composable
fun installFailedDialog(
    installer: InstallerRepo, viewModel: DialogViewModel
): DialogParams {
    val context = LocalContext.current

    // --- 开始: 从 ViewModel 收集状态 ---
    val preInstallAppInfo by viewModel.preInstallAppInfo.collectAsState()
    val currentPackageName by viewModel.currentPackageName.collectAsState()
    val packageName = currentPackageName ?: installer.entities.filter { it.selected }.map { it.app }
        .firstOrNull()?.packageName ?: ""
    // --- 结束: 从 ViewModel 收集状态 ---

    // --- 调用 InstallInfoDialog 时传入 preInstallAppInfo ---
    return installInfoDialog(
        installer = installer,
        viewModel = viewModel,
        preInstallAppInfo = preInstallAppInfo, // <-- 传递从 ViewModel 获取的值
        onTitleExtraClick = { // 保持标题点击逻辑
            if (packageName.isNotEmpty()) { // 安全检查
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.fromParts("package", packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            // 失败时点击标题附加按钮后是否需要特殊操作？
            // viewModel.dispatch(DialogViewAction.Background)
        }
    ).copy( // 使用 copy 添加错误文本并替换按钮
        text = DialogInnerParams( // 添加错误信息显示
            DialogParamsType.InstallerInstallFailed.id, // 使用特定 ID
            errorText(installer, viewModel) // 调用公共的错误显示 Composable
        ),
        buttons = DialogButtons( // 替换按钮
            DialogParamsType.InstallerInstallFailed.id // 使用特定 ID
        ) {
            // 失败时通常提供 "重试/上一步" 和 "取消"
            listOf(
                DialogButton(stringResource(R.string.previous)) { // "上一步" 通常返回准备阶段
                    viewModel.dispatch(DialogViewAction.InstallPrepare)
                },
                DialogButton(stringResource(R.string.cancel)) { // "取消" 关闭对话框
                    viewModel.dispatch(DialogViewAction.Close)
                }
            )
        }
    )
}
