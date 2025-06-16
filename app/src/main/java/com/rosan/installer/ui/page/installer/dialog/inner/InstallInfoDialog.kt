package com.rosan.installer.ui.page.installer.dialog.inner

// import androidx.compose.runtime.LaunchedEffect // No longer needed here
// import androidx.compose.runtime.getValue // No longer needed here
// import androidx.compose.runtime.mutableStateOf // No longer needed here
// import androidx.compose.runtime.remember // No longer needed here
// import androidx.compose.runtime.setValue // No longer needed here
// import com.rosan.installer.ui.page.installer.dialog.DialogViewState // No longer needed here
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.twotone.AutoFixHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.rosan.installer.R
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.util.AppIconCache
import com.rosan.installer.data.app.util.InstalledAppInfo
import com.rosan.installer.data.app.util.sortedBest
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.installer.dialog.DialogParams
import com.rosan.installer.ui.page.installer.dialog.DialogParamsType
import com.rosan.installer.ui.page.installer.dialog.DialogViewModel
import org.koin.compose.getKoin

/**
 * Provides info display: Icon, Title, Subtitle (with version logic).
 * Shows comparison if preInstallAppInfo is provided, otherwise shows only new version.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InstallInfoDialog( // 大写开头
    installer: InstallerRepo,
    viewModel: DialogViewModel, // Keep for consistency or future use
    preInstallAppInfo: InstalledAppInfo?, // Crucial parameter: Info *before* install operation
    onTitleExtraClick: () -> Unit = {}
): DialogParams {
    val context: Context = getKoin().get()
    val density = LocalDensity.current

    val entityToInstall =
        installer.entities.filter { it.selected }.map { it.app }.sortedBest().firstOrNull()
            ?: return DialogParams()

    // No need for remembered state or LaunchedEffect here, logic depends directly on passed preInstallAppInfo

    /*    val displayIcon: Drawable? =
            (if (entityToInstall is AppEntity.BaseEntity) entityToInstall.icon else preInstallAppInfo?.icon)
                ?: ContextCompat.getDrawable(context, android.R.drawable.sym_def_app_icon)*/

    // --- ICON INTEGRATION LOGIC ---

    // 1. Set an initial icon to prevent flickering. For upgrades, use the already-loaded
    //    low-res icon. For new installs, use the icon from the APK.
    val initialIcon = if (preInstallAppInfo != null) {
        preInstallAppInfo.icon
    } else {
        (entityToInstall as? AppEntity.BaseEntity)?.icon
    }

    // 2. Create a state for the icon, initialized with our best first guess.
    var displayIcon by remember { mutableStateOf(initialIcon) }

    // 3. Use LaunchedEffect to load the high-quality icon asynchronously.
    LaunchedEffect(preInstallAppInfo) {
        val iconSizePx = with(density) { 64.dp.toPx().toInt() }

        val finalIcon =
            if (preInstallAppInfo != null && preInstallAppInfo.applicationInfo != null) {
                // UPGRADE: Use the new AppIconLoaderUtil for the installed app's icon.
                AppIconCache.loadIconDrawable(
                    context,
                    preInstallAppInfo.applicationInfo,
                    iconSizePx
                )
            } else {
                // NEW INSTALL: Use the icon from the new package entity.
                (entityToInstall as? AppEntity.BaseEntity)?.icon
            }

        // Update state if the loaded icon is not null.
        if (finalIcon != null) {
            displayIcon = finalIcon
        }
    }

    val displayLabel: String =
        (if (entityToInstall is AppEntity.BaseEntity) entityToInstall.label else preInstallAppInfo?.label)
            ?: when (entityToInstall) {
                is AppEntity.SplitEntity -> entityToInstall.splitName
                is AppEntity.DexMetadataEntity -> entityToInstall.dmName
                else -> entityToInstall.packageName
            }

    // Fallback to a default system icon if displayIcon is ever null.
    val painterIcon =
        displayIcon ?: ContextCompat.getDrawable(context, android.R.drawable.sym_def_app_icon)


    return DialogParams(
        icon = DialogInnerParams(DialogParamsType.InstallerInfo.id) {
            Image(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp)),
                painter = rememberDrawablePainter(painterIcon),
                contentDescription = null
            )
        },
        title = DialogInnerParams(DialogParamsType.InstallerInfo.id) {
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
                        imageVector = Icons.TwoTone.AutoFixHigh,
                        contentDescription = null,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        },
        // --- 修改：恢复版本显示逻辑，依赖传入的 preInstallAppInfo ---
        subtitle = DialogInnerParams(DialogParamsType.InstallerInfo.id) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (entityToInstall is AppEntity.BaseEntity) {
                    // 直接使用传入的 preInstallAppInfo 作为旧版本信息
                    val oldInfo = preInstallAppInfo

                    if (oldInfo == null) {
                        // 首次安装或无法获取旧信息: 只显示新版本，不带前缀
                        Text(
                            text = stringResource(
                                R.string.installer_version, // Use base version string
                                entityToInstall.versionName,
                                entityToInstall.versionCode.toLong()
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.basicMarquee()
                        )
                    } else {
                        // 更新安装: 显示对比，带前缀
                        Text( // 旧版本带前缀
                            text = stringResource(R.string.old_version_prefix) + stringResource(
                                R.string.installer_version,
                                oldInfo.versionName,
                                oldInfo.versionCode.toLong()
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.basicMarquee()
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowDownward,
                            contentDescription = "to",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text( // 新版本带前缀
                            text = stringResource(R.string.new_version_prefix) + stringResource(
                                R.string.installer_version,
                                entityToInstall.versionName,
                                entityToInstall.versionCode.toLong()
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                }
                // 如果有SDK信息则显示SDK
                if (installer.config.displaySdk) {
                    entityToInstall.minSdk?.let {
                        Text(
                            stringResource(R.string.installer_package_min_sdk, it),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                    entityToInstall.targetSdk?.let {
                        Text(
                            stringResource(R.string.installer_package_target_sdk, it),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                }
                // 总是显示包名
                Text(
                    stringResource(R.string.installer_package_name, entityToInstall.packageName),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
        // --- 修改结束 ---
        // buttons parameter removed, to be set by caller via .copy()
    )
}
