package com.rosan.installer.ui.page.installer.dialog.inner

import android.annotation.SuppressLint
// 导入需要的 Compose 相关库
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.BugReport
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.People
import androidx.compose.material.icons.twotone.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.* // 导入 collectAsState, getValue 等
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.data.app.util.sortedBest
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.installer.dialog.*

// InstallPrepareEmptyDialog 和 InstallPrepareTooManyDialog 保持不变

@Composable
private fun InstallPrepareEmptyDialog(
    installer: InstallerRepo, viewModel: DialogViewModel
): DialogParams {
    // ... (代码不变) ...
    return DialogParams(icon = DialogInnerParams(
        DialogParamsType.IconPausing.id, pausingIcon
    ), title = DialogInnerParams(
        DialogParamsType.InstallerPrepare.id,
    ) {
        Text(stringResource(R.string.installer_prepare_install))
    }, text = DialogInnerParams(
        DialogParamsType.InstallerPrepareEmpty.id
    ) {
        Text(stringResource(R.string.installer_prepare_install_empty))
    }, buttons = DialogButtons(
        DialogParamsType.ButtonsCancel.id
    ) {
        listOf(DialogButton(stringResource(R.string.previous)) {
            viewModel.dispatch(DialogViewAction.InstallChoice)
        }, DialogButton(stringResource(R.string.cancel)) {
            viewModel.dispatch(DialogViewAction.Close)
        })
    })
}

@Composable
private fun InstallPrepareTooManyDialog(
    installer: InstallerRepo, viewModel: DialogViewModel
): DialogParams {
    // ... (代码不变) ...
    return DialogParams(icon = DialogInnerParams(
        DialogParamsType.IconPausing.id, pausingIcon
    ), title = DialogInnerParams(
        DialogParamsType.InstallerPrepare.id,
    ) {
        Text(stringResource(R.string.installer_prepare_install))
    }, text = DialogInnerParams(
        DialogParamsType.InstallerPrepareTooMany.id
    ) {
        Text(stringResource(R.string.installer_prepare_install_too_many))
    }, buttons = DialogButtons(
        DialogParamsType.ButtonsCancel.id
    ) {
        listOf(DialogButton(stringResource(R.string.previous)) {
            viewModel.dispatch(DialogViewAction.InstallChoice)
        }, DialogButton(stringResource(R.string.cancel)) {
            viewModel.dispatch(DialogViewAction.Close)
        })
    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Chip(
    selected: Boolean, onClick: () -> Unit, label: String, icon: ImageVector
) {
    // ... (代码不变) ...
    FilterChip(selected = selected, onClick = onClick, leadingIcon = {
        Icon(
            modifier = Modifier.size(FilterChipDefaults.IconSize),
            imageVector = icon,
            contentDescription = null
        )
    }, label = {
        Text(label)
    })
}

@SuppressLint("UnrememberedMutableState") // 这个注解可能需要保留，取决于你的具体逻辑
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InstallPrepareDialog(
    installer: InstallerRepo, viewModel: DialogViewModel
): DialogParams {
    val entities = installer.entities.filter { it.selected }.map { it.app }.sortedBest()
    if (entities.isEmpty()) return InstallPrepareEmptyDialog(installer, viewModel)
    if (entities.groupBy { it.packageName }.size > 1) return InstallPrepareTooManyDialog(
        installer, viewModel
    )

    // --- 开始: 从 ViewModel 收集 preInstallAppInfo 状态 ---
    val preInstallAppInfo by viewModel.preInstallAppInfo.collectAsState()
    // --- 结束: 从 ViewModel 收集状态 ---

    var showChips by remember { mutableStateOf(false) }

    // --- 调用 InstallInfoDialog 时传入 preInstallAppInfo ---
    return InstallInfoDialog(
        installer = installer,
        viewModel = viewModel,
        preInstallAppInfo = preInstallAppInfo, // <-- 传递从 ViewModel 获取的值
        onTitleExtraClick = { showChips = !showChips } // 保持原有的标题点击逻辑
    ).copy( // 使用 copy 修改 InstallInfoDialog 返回的基础 DialogParams
        text = DialogInnerParams( // 添加或替换 text 部分
            DialogParamsType.InstallerPrepareInstall.id // 使用正确的 ID
        ) {
            LazyColumn(horizontalAlignment = Alignment.CenterHorizontally) {
                item {
                    Text(stringResource(R.string.installer_prepare_install_dsp))
                }
                if (showChips) item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Chip 相关的状态和逻辑保持不变
                        var forAllUser by remember { mutableStateOf(installer.config.forAllUser) }
                        var allowTestOnly by remember { mutableStateOf(installer.config.allowTestOnly) }
                        var allowDowngrade by remember { mutableStateOf(installer.config.allowDowngrade) }
                        var autoDelete by remember { mutableStateOf(installer.config.autoDelete) }

                        // 更新 installer.config 的逻辑可能移到 ViewModel 更佳，但暂时保留
                        LaunchedEffect(forAllUser, allowTestOnly, allowDowngrade, autoDelete) {
                            installer.config = installer.config.copy(
                                forAllUser = forAllUser,
                                allowTestOnly = allowTestOnly,
                                allowDowngrade = allowDowngrade,
                                autoDelete = autoDelete
                            )
                        }

                        Chip(selected = forAllUser, onClick = { forAllUser = !forAllUser }, label = stringResource(id = R.string.config_for_all_user), icon = Icons.TwoTone.People)
                        Chip(selected = allowTestOnly, onClick = { allowTestOnly = !allowTestOnly }, label = stringResource(id = R.string.config_allow_test_only), icon = Icons.TwoTone.BugReport)
                        Chip(selected = allowDowngrade, onClick = { allowDowngrade = !allowDowngrade }, label = stringResource(id = R.string.config_allow_downgrade), icon = Icons.TwoTone.TrendingDown)
                        Chip(selected = autoDelete, onClick = { autoDelete = !autoDelete }, label = stringResource(id = R.string.config_auto_delete), icon = Icons.TwoTone.Delete)
                    }
                }
            }
        },
        buttons = DialogButtons( // 替换按钮部分
            DialogParamsType.InstallerPrepareInstall.id // 使用正确的 ID
        ) {
            // 按钮逻辑保持不变
            listOf(
                DialogButton(stringResource(R.string.install)) { viewModel.dispatch(DialogViewAction.Install) },
                DialogButton(stringResource(R.string.previous), 2f) { viewModel.dispatch(DialogViewAction.InstallChoice) },
                DialogButton(stringResource(R.string.cancel), 1f) { viewModel.dispatch(DialogViewAction.Close) }
            )
        }
    )
}
