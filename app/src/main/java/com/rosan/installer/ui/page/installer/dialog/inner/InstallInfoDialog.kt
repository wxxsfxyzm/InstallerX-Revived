package com.rosan.installer.ui.page.installer.dialog.inner

import android.content.Context
import android.graphics.drawable.Drawable
// import android.util.Log // 移除 Log 导入
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.ArrowRight
import androidx.compose.material.icons.twotone.AutoFixHigh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
// import androidx.compose.runtime.LaunchedEffect // 移除 LaunchedEffect 导入
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.rosan.installer.R
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.util.InstalledAppInfo
import com.rosan.installer.data.app.util.sortedBest
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.installer.dialog.*
import org.koin.compose.getKoin

import androidx.compose.runtime.LaunchedEffect // <-- 需要导入
import androidx.compose.runtime.getValue       // <-- 需要导入
import androidx.compose.runtime.mutableStateOf  // <-- 需要导入
import androidx.compose.runtime.remember     // <-- 需要导入
import androidx.compose.runtime.setValue
import com.rosan.installer.ui.page.installer.dialog.DialogViewModel // <-- 需要导入
import com.rosan.installer.ui.page.installer.dialog.DialogViewState
// private const val TAG = "InstallInfoDialog" // 移除 TAG

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InstallInfoDialog(
    installer: InstallerRepo,
    viewModel: DialogViewModel,
    preInstallAppInfo: InstalledAppInfo?,
    onTitleExtraClick: () -> Unit = {}
): DialogParams {
    val context: Context = getKoin().get()
    val entities = installer.entities.filter { it.selected }.map { it.app }.sortedBest()
    val entity = entities.firstOrNull() ?: return DialogParams()

    // 移除 LaunchedEffect 日志块
    // LaunchedEffect(preInstallAppInfo) { ... }
    // --- 新增状态管理 ---
    // 记住在安装成功时获取到的旧应用信息
    var rememberedPreInstallInfo by remember { mutableStateOf<InstalledAppInfo?>(null) }
    val currentState = viewModel.state // 获取当前状态

    // 当进入 InstallSuccess 状态且有 preInstallAppInfo 时，记住它
    // 使用 currentState 和 preInstallAppInfo 作为 LaunchedEffect 的 key
    // 这样当它们变化时会重新执行 effect
    LaunchedEffect(currentState, preInstallAppInfo) {
        if (currentState is DialogViewState.InstallSuccess && preInstallAppInfo != null && rememberedPreInstallInfo == null) {
            // 只有在首次进入成功状态且 remembered 为空时才赋值，防止后续 preInstallAppInfo 变 null 时覆盖
            rememberedPreInstallInfo = preInstallAppInfo
        } else if (currentState !is DialogViewState.InstallSuccess) {
            // 如果状态不再是 Success，清除记住的值，以便下次能正确更新
            rememberedPreInstallInfo = null
        }
    }
    // --- 新增状态管理结束 ---


    val iconDrawable: Drawable? = (if (entity is AppEntity.BaseEntity) entity.icon else preInstallAppInfo?.icon)
        ?: ContextCompat.getDrawable(context, android.R.drawable.sym_def_app_icon)

    val appLabel: String = (if (entity is AppEntity.BaseEntity) entity.label else preInstallAppInfo?.label)
        ?: when (entity) {
            is AppEntity.SplitEntity -> entity.splitName
            is AppEntity.DexMetadataEntity -> entity.dmName
            else -> entity.packageName
        }

    return DialogParams(
        icon = DialogInnerParams(DialogParamsType.InstallerInfo.id) {
            Image(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp)),
                painter = rememberDrawablePainter(iconDrawable),
                contentDescription = null
            )
        },
        title = DialogInnerParams(DialogParamsType.InstallerInfo.id) {
            Box {
                Text(
                    text = appLabel,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .absolutePadding(right = 32.dp)
                        .basicMarquee()
                )
                IconButton(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .size(24.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    onClick = onTitleExtraClick
                ) {
                    Icon(
                        imageVector = Icons.TwoTone.AutoFixHigh,
                        contentDescription = null,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        },
        subtitle = DialogInnerParams(DialogParamsType.InstallerInfo.id) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (entity is AppEntity.BaseEntity) {
                    // --- 修改版本显示逻辑 ---
                    // 优先使用记住的成功状态下的旧信息
                    val effectiveOldInfo = if (currentState is DialogViewState.InstallSuccess) {
                        rememberedPreInstallInfo // 在成功状态下，使用记住的值
                    } else {
                        preInstallAppInfo // 其他状态下，使用实时传入的值
                    }

                    if (effectiveOldInfo == null) { // 如果没有旧信息（无论是记住的还是实时的）
                        // 只显示新版本
                        Text(
                            stringResource(
                                R.string.installer_version,
                                entity.versionName,
                                entity.versionCode
                            ),
                            modifier = Modifier.basicMarquee()
                        )
                    } else { // 如果有旧信息
                        // 显示版本对比
                        Row(modifier = Modifier.basicMarquee()) {
                            Text( // 旧版本 (使用 effectiveOldInfo)
                                stringResource(
                                    R.string.installer_version,
                                    effectiveOldInfo.versionName,
                                    effectiveOldInfo.versionCode
                                ),
                                // ...
                            )
                            Icon( // 箭头
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.CenterVertically)
                                    .padding(horizontal = 4.dp),
                                imageVector = Icons.TwoTone.ArrowRight,
                                tint = MaterialTheme.colorScheme.primary,
                                contentDescription = null
                            )
                            Text( // 新版本 (使用 entity)
                                stringResource(
                                    R.string.installer_version2, // 或 R.string.installer_version
                                    entity.versionName,
                                    entity.versionCode
                                ),
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }
                }
                Text( // 包名
                    stringResource(R.string.installer_package_name, entity.packageName),
                    modifier = Modifier.basicMarquee()
                )
            }
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
