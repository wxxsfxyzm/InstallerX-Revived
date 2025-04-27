package com.rosan.installer.ui.page.installer.dialog.inner

import android.content.Intent
import android.net.Uri
import android.provider.Settings
// 导入需要的库
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
// 可能需要导入 InstalledAppInfo
import com.rosan.installer.data.app.util.InstalledAppInfo
import com.rosan.installer.data.common.util.addAll // 确保导入 addAll
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.installer.dialog.* // 导入 DialogButton, DialogButtons 等

@Composable
fun InstallSuccessDialog(
    installer: InstallerRepo, // 如果所有信息都来自 VM，这个参数可能可以移除
    viewModel: DialogViewModel
): DialogParams {
    val context = LocalContext.current

    // --- 开始: 从 ViewModel 收集状态 ---
    val preInstallAppInfo by viewModel.preInstallAppInfo.collectAsState()
    val currentPackageName by viewModel.currentPackageName.collectAsState()
    // 优先使用 ViewModel 中的包名，如果为空则尝试从 repo 获取 (作为备选)
    val packageName = currentPackageName ?: installer.entities.filter { it.selected }.map { it.app }.firstOrNull()?.packageName ?: ""
    // --- 结束: 从 ViewModel 收集状态 ---

    // --- 调用 InstallInfoDialog 时传入 preInstallAppInfo ---
    return InstallInfoDialog(
        installer = installer, // 如果需要，继续传递 installer
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
            viewModel.dispatch(DialogViewAction.Background) // 或 Close?
        }
    ).copy( // 使用 copy 替换按钮部分
        buttons = DialogButtons(
            DialogParamsType.InstallerInstallSuccess.id // 使用特定 ID
        ) {
            val list = mutableListOf<DialogButton>()
            // 尝试获取启动 Intent
            val intent = if (packageName.isNotEmpty()) context.packageManager.getLaunchIntentForPackage(packageName) else null
            // 如果可以启动，添加 "打开" 按钮
            if (intent != null) {
                list.add(DialogButton(stringResource(R.string.open)) {
                    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    viewModel.dispatch(DialogViewAction.Close) // 打开后通常关闭对话框
                })
            }
            // 添加 "上一步" (返回准备) 和 "完成" (关闭) 按钮
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
