package com.rosan.installer.ui.page.installer.dialog.inner

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.rosan.installer.R
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.util.InstalledAppInfo
import com.rosan.installer.data.app.util.sortedBest
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.installer.dialog.DialogParams
import com.rosan.installer.ui.page.installer.dialog.DialogParamsType
import com.rosan.installer.ui.page.installer.dialog.DialogViewModel
import com.rosan.installer.ui.page.installer.dialog.DialogViewState
import org.koin.compose.getKoin

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InstallInfoDialog( // 大写开头
    installer: InstallerRepo,
    viewModel: DialogViewModel,
    preInstallAppInfo: InstalledAppInfo?,
    onTitleExtraClick: () -> Unit = {}
): DialogParams {
    val context: Context = getKoin().get()
    val entityToInstall = installer.entities.filter { it.selected }.map { it.app }.sortedBest().firstOrNull()
        ?: return DialogParams()

    val currentState = viewModel.state
    var rememberedOldVersionInfo by remember { mutableStateOf<InstalledAppInfo?>(null) }

    LaunchedEffect(currentState, preInstallAppInfo) {
        if (currentState is DialogViewState.InstallSuccess && preInstallAppInfo != null) {
            if (rememberedOldVersionInfo?.packageName != preInstallAppInfo.packageName || rememberedOldVersionInfo?.versionCode != preInstallAppInfo.versionCode) {
                rememberedOldVersionInfo = preInstallAppInfo
            }
        } else if (currentState !is DialogViewState.InstallSuccess) {
            if (rememberedOldVersionInfo != null) {
                rememberedOldVersionInfo = null
            }
        }
    }

    val displayIcon: Drawable? =
        (if (entityToInstall is AppEntity.BaseEntity) entityToInstall.icon else preInstallAppInfo?.icon)
            ?: ContextCompat.getDrawable(context, android.R.drawable.sym_def_app_icon)

    val displayLabel: String =
        (if (entityToInstall is AppEntity.BaseEntity) entityToInstall.label else preInstallAppInfo?.label)
            ?: when (entityToInstall) {
                is AppEntity.SplitEntity -> entityToInstall.splitName
                is AppEntity.DexMetadataEntity -> entityToInstall.dmName
                else -> entityToInstall.packageName
            }

    return DialogParams(
        icon = DialogInnerParams(DialogParamsType.InstallerInfo.id) {
            Image(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp)),
                painter = rememberDrawablePainter(displayIcon),
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
        subtitle = DialogInnerParams(DialogParamsType.InstallerInfo.id) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (entityToInstall is AppEntity.BaseEntity) {
                    val oldInfoForDisplay = if (currentState is DialogViewState.InstallSuccess) {
                        rememberedOldVersionInfo
                    } else {
                        preInstallAppInfo
                    }

                    if (oldInfoForDisplay == null) {
                        Text(
                            text = stringResource(R.string.new_version_prefix) + stringResource(
                                R.string.installer_version,
                                entityToInstall.versionName,
                                entityToInstall.versionCode.toLong()
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.basicMarquee()
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.old_version_prefix) + stringResource(
                                R.string.installer_version,
                                oldInfoForDisplay.versionName,
                                oldInfoForDisplay.versionCode.toLong()
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
                        Text(
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

                Text(
                    stringResource(R.string.installer_package_name, entityToInstall.packageName),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
    )
}
