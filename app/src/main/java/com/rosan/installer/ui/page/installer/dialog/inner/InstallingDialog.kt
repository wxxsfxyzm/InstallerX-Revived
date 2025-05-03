package com.rosan.installer.ui.page.installer.dialog.inner

// 导入需要的库
// --- ***** 确认必要的导入 ***** ---
// --- ***** 结束确认导入 ***** ---
// 可能需要导入 InstalledAppInfo
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
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

@Composable
fun installingDialog(
    installer: InstallerRepo, viewModel: DialogViewModel
): DialogParams {
    // --- 开始: 从 ViewModel 收集 preInstallAppInfo 状态 ---
    val preInstallAppInfo by viewModel.preInstallAppInfo.collectAsState()
    // --- 结束: 从 ViewModel 收集状态 ---

    // --- 调用 InstallInfoDialog 时传入 preInstallAppInfo ---
    return installInfoDialog(
        installer = installer,
        viewModel = viewModel,
        preInstallAppInfo = preInstallAppInfo, // <-- 传递从 ViewModel 获取的值
        onTitleExtraClick = {} // 安装中，标题附加按钮通常无操作
    ).copy( // 使用 copy 修改基础信息
        text = DialogInnerParams( // 添加进度条
            DialogParamsType.InstallerInstalling.id
        ) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        },
        // 安装中通常只有一个取消按钮
        buttons = DialogButtons(DialogParamsType.ButtonsCancel.id) { // 例如，只保留取消
            listOf(
                DialogButton(stringResource(R.string.cancel)) {
                    // 现在 DialogViewAction.Close 应该能正确解析了
                    viewModel.dispatch(DialogViewAction.Close) // 或者 repo.cancelInstall() 等
                }
            )
        }
    )
}
