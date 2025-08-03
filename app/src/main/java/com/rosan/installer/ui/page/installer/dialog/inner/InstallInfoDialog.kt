package com.rosan.installer.ui.page.installer.dialog.inner

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.rosan.installer.R
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.util.InstalledAppInfo
import com.rosan.installer.data.app.util.sortedBest
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.installer.dialog.DialogParams
import com.rosan.installer.ui.page.installer.dialog.DialogParamsType
import com.rosan.installer.ui.page.installer.dialog.DialogViewModel


/**
 * Provides info display: Icon, Title, Subtitle (with version logic).
 * Shows comparison if preInstallAppInfo is provided, otherwise shows only new version.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun installInfoDialog(
    installer: InstallerRepo,
    viewModel: DialogViewModel,
    preInstallAppInfo: InstalledAppInfo?, // Crucial parameter: Info *before* install operation
    onTitleExtraClick: () -> Unit = {}
): DialogParams {
    val iconMap by viewModel.displayIcons.collectAsState()
    val selectedApps = installer.entities.filter { it.selected }.map { it.app }
    // If no apps are selected, return empty DialogParams
    val entityToInstall = selectedApps.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
        ?: selectedApps.sortedBest().firstOrNull()
        ?: return DialogParams()

    // 为当前应用的所有 UI 部件创建一个唯一的 ID
    // 确保 AnimatedContent 能够检测到内容变化
    val uniqueContentKey = "${DialogParamsType.InstallerInfo.id}_${entityToInstall.packageName}"

    val displayLabel: String =
        (if (entityToInstall is AppEntity.BaseEntity) entityToInstall.label else preInstallAppInfo?.label)
            ?: when (entityToInstall) {
                is AppEntity.SplitEntity -> entityToInstall.splitName
                is AppEntity.DexMetadataEntity -> entityToInstall.dmName
                else -> entityToInstall.packageName
            }

    // --- New Icon Loading Logic ---
    // Collect the icon state directly from the ViewModel.
    val displayIcon = iconMap[entityToInstall.packageName]
    // --- New Icon Loading Logic End ---

    return DialogParams(
        icon = DialogInnerParams(uniqueContentKey) {
            Image(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp)),
                painter = rememberDrawablePainter(displayIcon),
                contentDescription = null
            )
        },
        title = DialogInnerParams(uniqueContentKey) {
            Box {
                Text(
                    text = displayLabel,
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
                        imageVector = AppIcons.AutoFixHigh,
                        contentDescription = null,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        },
        // --- 修改：恢复版本显示逻辑，依赖传入的 preInstallAppInfo ---
        subtitle = DialogInnerParams(uniqueContentKey) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                // verticalArrangement = Arrangement.spacedBy(4.dp) // Removed to avoid spacing issues during animation
            ) {
                // --- 显示包名 ---
                Text(
                    stringResource(R.string.installer_package_name, entityToInstall.packageName),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.basicMarquee()
                )

                Spacer(modifier = Modifier.size(8.dp))

                // --- 显示版本信息 ---
                if (entityToInstall is AppEntity.BaseEntity) {
                    if (preInstallAppInfo == null) {
                        // 首次安装或无法获取旧信息: 只显示新版本，不带前缀
                        Text(
                            text = stringResource(
                                R.string.installer_version, // Use base version string
                                entityToInstall.versionName,
                                entityToInstall.versionCode
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.basicMarquee()
                        )
                    } else {
                        // 更新安装: 显示对比，带前缀
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text( // 旧版本带前缀
                                text = stringResource(R.string.old_version_prefix) +
                                        stringResource(
                                            R.string.installer_version_short,
                                            if (preInstallAppInfo.isArchived)
                                                stringResource(R.string.old_version_archived)
                                            else preInstallAppInfo.versionName,
                                            preInstallAppInfo.versionCode
                                        ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.basicMarquee()
                            )
                            Icon(
                                imageVector = AppIcons.ArrowDropDownFilled,
                                contentDescription = "to",
                                tint =
                                    if (preInstallAppInfo.versionCode > entityToInstall.versionCode)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text( // 新版本带前缀
                                text = if (entityToInstall.versionCode >= preInstallAppInfo.versionCode)
                                    stringResource(R.string.upgrade_version_prefix) + stringResource(
                                        R.string.installer_version_short,
                                        entityToInstall.versionName,
                                        entityToInstall.versionCode.toLong()
                                    ) else stringResource(R.string.downgrade_version_prefix) + stringResource(
                                    R.string.installer_version_short,
                                    entityToInstall.versionName,
                                    entityToInstall.versionCode
                                ),
                                color = if (preInstallAppInfo.versionCode > entityToInstall.versionCode)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                    }
                }
                // --- SDK Information Showcase ---
                // Use a single AnimatedVisibility to control the entire SDK block
                AnimatedVisibility(visible = installer.config.displaySdk) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        // Spacing between Min and Target SDK is now handled cleanly here.
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // This Spacer creates a gap between the version info above and this SDK block.
                        // It's animated along with the content, ensuring a smooth transition.
                        Spacer(modifier = Modifier.size(4.dp))
                        // Min SDK
                        entityToInstall.minSdk?.let { newMinSdk ->
                            val oldMinSdk = preInstallAppInfo?.minSdk
                            if (oldMinSdk != null && oldMinSdk.toString() != newMinSdk) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stringResource(
                                            R.string.installer_package_min_sdk,
                                            if (preInstallAppInfo.isArchived)
                                                stringResource(R.string.old_version_archived)
                                            else oldMinSdk.toString()
                                        )
                                    )
                                    Icon(
                                        modifier = Modifier.size(24.dp),
                                        imageVector = AppIcons.ArrowRight,
                                        tint =
                                            if (newMinSdk.toInt() > Build.VERSION.SDK_INT)
                                                MaterialTheme.colorScheme.error
                                            else
                                                MaterialTheme.colorScheme.primary,
                                        contentDescription = null
                                    )
                                    Text(
                                        text = newMinSdk,
                                        color =
                                            if (newMinSdk.toInt() > Build.VERSION.SDK_INT)
                                                MaterialTheme.colorScheme.error
                                            else
                                                MaterialTheme.colorScheme.primary,
                                    )

                                }
                            } else {
                                Text(
                                    text = stringResource(
                                        R.string.installer_package_min_sdk,
                                        newMinSdk
                                    ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.basicMarquee()
                                )
                            }
                        }
                        // Target SDK
                        entityToInstall.targetSdk?.let { newTargetSdk ->
                            val oldTargetSdk = preInstallAppInfo?.targetSdk
                            if (oldTargetSdk != null && oldTargetSdk.toString() != newTargetSdk) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stringResource(
                                            R.string.installer_package_target_sdk,
                                            oldTargetSdk.toString()
                                        )
                                    )
                                    Icon(
                                        modifier = Modifier.size(24.dp),
                                        imageVector = AppIcons.ArrowRight,
                                        tint = MaterialTheme.colorScheme.primary,
                                        contentDescription = null
                                    )
                                    Text(
                                        text = newTargetSdk,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            } else {
                                Text(
                                    text = stringResource(
                                        R.string.installer_package_target_sdk,
                                        newTargetSdk
                                    ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.basicMarquee()
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}
