package com.rosan.installer.ui.page.installer.dialog.inner

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowRight
import androidx.compose.material.icons.twotone.ArrowRight
import androidx.compose.material.icons.twotone.AutoFixHigh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun installInfoDialog(
    installer: InstallerRepo, viewModel: DialogViewModel, onTitleExtraClick: () -> Unit = {}
): DialogParams {
    val context: Context = getKoin().get()
    val entities = installer.entities.filter { it.selected }.map { it.app }.sortedBest()
    val entity = entities.first()
    val installed = InstalledAppInfo.buildByPackageName(entity.packageName)
    return DialogParams(icon = DialogInnerParams(
        DialogParamsType.InstallerInfo.id
    ) {
        Image(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp)),
            painter = rememberDrawablePainter(
                (if (entity is AppEntity.BaseEntity) entity.icon
                else installed?.icon) ?: ContextCompat.getDrawable(
                    context,
                    android.R.drawable.sym_def_app_icon
                )
            ), contentDescription = null
        )
    }, title = DialogInnerParams(
        DialogParamsType.InstallerInfo.id
    ) {
        Box {
            Text(
                (if (entity is AppEntity.BaseEntity) entity.label
                else installed?.label) ?: when (entity) {
                    is AppEntity.SplitEntity -> entity.splitName
                    is AppEntity.DexMetadataEntity -> entity.dmName
                    else -> entity.packageName
                },
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
                ), onClick = onTitleExtraClick
            ) {
                Icon(
                    imageVector = Icons.TwoTone.AutoFixHigh, contentDescription = null,
                    modifier = Modifier
                        .padding(4.dp)
                )
            }
        }
    }, subtitle = DialogInnerParams(
        DialogParamsType.InstallerInfo.id
    ) {
        Column {
            if (entity is AppEntity.BaseEntity) if (installed == null) Text(
                stringResource(
                    R.string.installer_version, entity.versionName, entity.versionCode
                ), modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .basicMarquee()
            ) else Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .basicMarquee()
            ) {
                Text(
                    stringResource(
                        R.string.installer_version, installed.versionName, installed.versionCode
                    ),
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                Icon(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.CenterVertically),
                    imageVector = Icons.AutoMirrored.TwoTone.ArrowRight,
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null
                )
                Text(
                    stringResource(
                        R.string.installer_version2, entity.versionName, entity.versionCode
                    ),
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }

            Text(
                stringResource(R.string.installer_package_name, entity.packageName),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .basicMarquee()
            )
        }
    }, buttons = DialogButtons(
        DialogParamsType.ButtonsCancel.id
    ) {
        listOf(DialogButton(stringResource(R.string.cancel)) {
            viewModel.dispatch(DialogViewAction.Close)
        })
    })
}