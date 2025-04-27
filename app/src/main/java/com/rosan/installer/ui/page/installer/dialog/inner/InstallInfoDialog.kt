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
                    if (preInstallAppInfo == null) {
                        // Log.d(TAG, "Displaying only new version because preInstallAppInfo is null.") // 移除日志
                        Text(
                            stringResource(
                                R.string.installer_version,
                                entity.versionName,
                                entity.versionCode
                            ),
                            modifier = Modifier.basicMarquee()
                        )
                    } else {
                        // Log.d(TAG, "Displaying version comparison: Old=${preInstallAppInfo?.versionName} -> New=${entity.versionName}") // 移除日志
                        Row(modifier = Modifier.basicMarquee()) {
                            Text( // 旧版本
                                stringResource(
                                    R.string.installer_version,
                                    preInstallAppInfo.versionName,
                                    preInstallAppInfo.versionCode
                                ),
                                modifier = Modifier.align(Alignment.CenterVertically)
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
                            Text( // 新版本
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
