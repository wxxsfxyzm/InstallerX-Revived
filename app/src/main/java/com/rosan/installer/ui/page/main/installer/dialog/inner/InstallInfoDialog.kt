package com.rosan.installer.ui.page.main.installer.dialog.inner

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Archive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.rosan.installer.R
import com.rosan.installer.build.RsConfig
import com.rosan.installer.build.model.entity.Manufacturer
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.enums.DataType
import com.rosan.installer.data.app.util.sortedBest
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.InstallerViewState
import com.rosan.installer.ui.page.main.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParamsType
import com.rosan.installer.ui.util.formatSize
import com.rosan.installer.ui.util.toAndroidVersionName
import kotlin.math.abs

@Composable
fun installInfoDialog(
    installer: InstallerRepo,
    viewModel: InstallerViewModel,
    warningContent: @Composable () -> Unit = {}, // 新增：用于接收警告筹码的插槽
    onTitleExtraClick: () -> Unit = {}
): DialogParams {
    val settings = viewModel.viewSettings
    val iconMap by viewModel.displayIcons.collectAsState()
    val currentPackageName by viewModel.currentPackageName.collectAsState()
    val currentPackage = installer.analysisResults.find { it.packageName == currentPackageName }

    if (currentPackage == null) return DialogParams()
    val preInstallAppInfo = currentPackage.installedAppInfo
    val selectableEntities = currentPackage.appEntities

    val selectedApps = selectableEntities.filter { it.selected }.map { it.app }
    val totalSize = selectedApps.sumOf { it.size }

    val entityToInstall = selectedApps.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
        ?: selectedApps.filterIsInstance<AppEntity.ModuleEntity>().firstOrNull()
        ?: selectedApps.sortedBest().firstOrNull()
        ?: return DialogParams()
    val isModule = entityToInstall is AppEntity.ModuleEntity

    val uniqueContentKey = "${DialogParamsType.InstallerInfo.id}_${entityToInstall.packageName}"

    val displayLabel: String =
        (if (entityToInstall is AppEntity.BaseEntity) entityToInstall.label else preInstallAppInfo?.label)
            ?: when (entityToInstall) {
                is AppEntity.ModuleEntity -> entityToInstall.name
                is AppEntity.SplitEntity -> entityToInstall.splitName
                is AppEntity.DexMetadataEntity -> entityToInstall.dmName
                else -> entityToInstall.packageName
            }

    val displayIcon = iconMap[entityToInstall.packageName]

    return DialogParams(
        icon = DialogInnerParams(uniqueContentKey) {
            AnimatedContent(
                targetState = displayIcon,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(150))
                },
                label = "IconLoadAnimation"
            ) { icon ->
                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        painter = rememberDrawablePainter(icon),
                        contentDescription = null
                    )
                }
            }
        },
        title = DialogInnerParams(uniqueContentKey) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.animateContentSize()
                ) {
                    Text(
                        text = displayLabel,
                        modifier = Modifier.basicMarquee()
                    )
                    AnimatedVisibility(
                        visible = viewModel.state == InstallerViewState.InstallPrepare || viewModel.state == InstallerViewState.InstallSuccess,
                        enter = fadeIn() + slideInHorizontally { it },
                        exit = fadeOut() + slideOutHorizontally { it }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .size(24.dp),
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                onClick = onTitleExtraClick
                            ) {
                                if (isModule)
                                    Icon(
                                        imageVector = Icons.TwoTone.Archive,
                                        contentDescription = null,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                else
                                    Icon(
                                        imageVector = AppIcons.Android,
                                        contentDescription = null,
                                        modifier = Modifier.padding(4.dp)
                                    )
                            }
                        }
                    }
                }
                // --- PackageName (Keep centered outside the card) ---
                Text(
                    text = entityToInstall.packageName,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.basicMarquee()
                )
            }
        },
        subtitle = DialogInnerParams(uniqueContentKey) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // 将 WarningChipGroup 安插在这里
                warningContent()

                // --- MD3 Info Card ---
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Remove bottom padding to align perfectly with right buttons
                        .padding(top = 4.dp)
                        .graphicsLayer(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        when (entityToInstall) {
                            is AppEntity.BaseEntity -> {
                                val isDowngrade = preInstallAppInfo != null && preInstallAppInfo.versionCode > entityToInstall.versionCode
                                val statusColor = if (isDowngrade) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

                                CompactMd3AdaptiveInfoRow(
                                    labelResId = R.string.installer_version_name_label,
                                    newValue = entityToInstall.versionName,
                                    oldValue = preInstallAppInfo?.versionName,
                                    isUninstalled = preInstallAppInfo?.isUninstalled ?: false,
                                    isArchived = preInstallAppInfo?.isArchived ?: false,
                                    defaultSingleLine = settings.versionCompareInSingleLine,
                                    statusColor = statusColor
                                )
                                CompactMd3AdaptiveInfoRow(
                                    labelResId = R.string.installer_version_code_label,
                                    newValue = entityToInstall.versionCode.toString(),
                                    oldValue = preInstallAppInfo?.versionCode?.toString(),
                                    isUninstalled = preInstallAppInfo?.isUninstalled ?: false,
                                    isArchived = preInstallAppInfo?.isArchived ?: false,
                                    defaultSingleLine = settings.versionCompareInSingleLine,
                                    statusColor = statusColor
                                )
                            }

                            is AppEntity.ModuleEntity -> {
                                CompactMd3AdaptiveInfoRow(
                                    labelResId = R.string.installer_version_name_label,
                                    newValue = entityToInstall.version,
                                    oldValue = null,
                                    defaultSingleLine = true
                                )
                                CompactMd3AdaptiveInfoRow(
                                    labelResId = R.string.installer_version_code_label,
                                    newValue = entityToInstall.versionCode.toString(),
                                    oldValue = null,
                                    defaultSingleLine = true
                                )
                                CompactMd3AdaptiveInfoRow(
                                    labelResId = R.string.installer_module_author_label,
                                    newValue = entityToInstall.author,
                                    oldValue = null,
                                    defaultSingleLine = true
                                )
                            }

                            is AppEntity.SplitEntity -> {
                                CompactMd3AdaptiveInfoRow(
                                    labelResId = R.string.installer_split_name_label,
                                    newValue = entityToInstall.splitName,
                                    oldValue = null,
                                    defaultSingleLine = true
                                )
                            }

                            else -> {}
                        }

                        // --- SDK Information ---
                        if (installer.config.displaySdk && entityToInstall !is AppEntity.ModuleEntity) {
                            entityToInstall.targetSdk?.let { newTargetSdk ->
                                CompactMd3SdkInfoRow(
                                    labelResId = R.string.installer_package_target_sdk_label,
                                    newSdk = newTargetSdk,
                                    oldSdk = preInstallAppInfo?.targetSdk?.toString(),
                                    isUninstalled = preInstallAppInfo?.isUninstalled ?: false,
                                    isArchived = preInstallAppInfo?.isArchived ?: false,
                                    type = "target",
                                    defaultSingleLine = !settings.sdkCompareInMultiLine
                                )
                            }
                            entityToInstall.minSdk?.let { newMinSdk ->
                                CompactMd3SdkInfoRow(
                                    labelResId = R.string.installer_package_min_sdk_label,
                                    newSdk = newMinSdk,
                                    oldSdk = preInstallAppInfo?.minSdk?.toString(),
                                    isUninstalled = preInstallAppInfo?.isUninstalled ?: false,
                                    isArchived = preInstallAppInfo?.isArchived ?: false,
                                    type = "min",
                                    defaultSingleLine = !settings.sdkCompareInMultiLine
                                )
                            }
                        }

                        // --- Size Display ---
                        if (installer.config.displaySize && totalSize > 0L) {
                            CompactMd3SizeInfoRow(
                                oldSize = preInstallAppInfo?.packageSize ?: 0L,
                                newSize = totalSize
                            )
                        }

                        // --- OPPO Info Display ---
                        if ((RsConfig.currentManufacturer == Manufacturer.OPPO || RsConfig.currentManufacturer == Manufacturer.ONEPLUS)
                            && settings.showOPPOSpecial && entityToInstall.sourceType == DataType.APK
                        ) {
                            (entityToInstall as AppEntity.BaseEntity).minOsdkVersion?.let {
                                CompactMd3AdaptiveInfoRow(
                                    labelResId = R.string.installer_package_minOsdkVersion_label,
                                    newValue = it,
                                    oldValue = null,
                                    defaultSingleLine = true
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun CompactMd3AdaptiveInfoRow(
    @StringRes labelResId: Int,
    newValue: String,
    oldValue: String?,
    isUninstalled: Boolean = false,
    isArchived: Boolean = false,
    defaultSingleLine: Boolean = true,
    statusColor: Color? = null
) {
    var isSingleLine by remember { mutableStateOf(defaultSingleLine) }

    LaunchedEffect(defaultSingleLine) {
        isSingleLine = defaultSingleLine
    }

    val showComparison = oldValue != null && newValue != oldValue
    val oldTextContent = when {
        isArchived -> stringResource(R.string.old_version_archived)
        isUninstalled -> if (oldValue.isNullOrEmpty()) stringResource(R.string.old_version_uninstalled) else oldValue
        else -> oldValue.orEmpty()
    }
    val defaultColor = MaterialTheme.colorScheme.onSurface
    val finalValueColor = if (showComparison && statusColor != null) statusColor else defaultColor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isSingleLine = !isSingleLine }
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .animateContentSize()
    ) {
        if (isSingleLine) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(labelResId),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(end = 12.dp)
                )

                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .basicMarquee(iterations = Int.MAX_VALUE, velocity = 30.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showComparison) {
                        Text(
                            text = oldTextContent,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1
                        )
                        Icon(
                            imageVector = AppIcons.ArrowRight,
                            contentDescription = "to",
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(14.dp),
                            tint = statusColor ?: MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = newValue,
                        color = finalValueColor,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                }
            }
        } else {
            if (showComparison) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(labelResId),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = oldTextContent,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.End,
                        modifier = Modifier.basicMarquee()
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = AppIcons.ArrowRight,
                        contentDescription = "to",
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(14.dp),
                        tint = statusColor ?: MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = newValue,
                        color = finalValueColor,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.End,
                        modifier = Modifier.basicMarquee()
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(labelResId),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = newValue,
                        color = finalValueColor,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.End,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactMd3SdkInfoRow(
    @StringRes labelResId: Int,
    newSdk: String,
    oldSdk: String?,
    isUninstalled: Boolean = false,
    isArchived: Boolean = false,
    type: String, // "min" or "target"
    defaultSingleLine: Boolean = true
) {
    var isSingleLine by remember { mutableStateOf(defaultSingleLine) }

    LaunchedEffect(defaultSingleLine) {
        isSingleLine = defaultSingleLine
    }

    val newSdkInt = newSdk.toIntOrNull()
    val oldSdkInt = oldSdk?.toIntOrNull()
    val showComparison = oldSdkInt != null && newSdkInt != null && newSdkInt != oldSdkInt

    val isIncompatible = type == "min" && newSdkInt != null && newSdkInt > Build.VERSION.SDK_INT
    val sdkColor = if (isIncompatible) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    val oldTextContent = when {
        isUninstalled -> stringResource(R.string.old_version_uninstalled)
        isArchived -> stringResource(R.string.old_version_archived)
        else -> oldSdk.orEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isSingleLine = !isSingleLine }
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .animateContentSize()
    ) {
        if (isSingleLine) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(labelResId),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(end = 12.dp)
                )

                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .basicMarquee(iterations = Int.MAX_VALUE, velocity = 30.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showComparison) {
                        Text(
                            text = oldTextContent,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1
                        )
                        Icon(
                            imageVector = AppIcons.ArrowRight,
                            contentDescription = "to",
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(14.dp),
                            tint = sdkColor
                        )
                    }
                    Text(
                        text = newSdk,
                        color = if (showComparison || isIncompatible) sdkColor else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                }
            }
        } else {
            if (showComparison) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(labelResId),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = oldTextContent,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.End
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = AppIcons.ArrowRight,
                        contentDescription = "to",
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(14.dp),
                        tint = sdkColor
                    )
                    Text(
                        text = newSdk,
                        color = sdkColor,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.End
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(${newSdk.toAndroidVersionName()})",
                        color = sdkColor.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.End
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(labelResId),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = newSdk,
                            color = if (isIncompatible) sdkColor else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.End
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(${newSdk.toAndroidVersionName()})",
                            color = (if (isIncompatible) sdkColor else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactMd3SizeInfoRow(
    oldSize: Long,
    newSize: Long
) {
    val showComparison = oldSize > 0L && oldSize != newSize
    var showDiffOnly by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = showComparison) { showDiffOnly = !showDiffOnly }
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.installer_package_size_label),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(end = 12.dp)
            )

            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .basicMarquee(iterations = Int.MAX_VALUE, velocity = 30.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showComparison && showDiffOnly) {
                    val diff = newSize - oldSize
                    val diffString = abs(diff).formatSize()
                    val finalString = when {
                        diff > 0 -> "+$diffString"
                        diff < 0 -> "-$diffString"
                        else -> diffString
                    }
                    Text(
                        text = finalString,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                } else {
                    if (showComparison) {
                        Text(
                            text = oldSize.formatSize(),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1
                        )
                        Icon(
                            imageVector = AppIcons.ArrowRight,
                            contentDescription = "to",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(14.dp)
                        )
                    }
                    Text(
                        text = newSize.formatSize(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (showComparison) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }
    }
}