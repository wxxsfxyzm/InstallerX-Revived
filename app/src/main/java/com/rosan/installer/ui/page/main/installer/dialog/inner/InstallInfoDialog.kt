package com.rosan.installer.ui.page.main.installer.dialog.inner

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.rosan.installer.R
import com.rosan.installer.build.RsConfig
import com.rosan.installer.build.model.entity.Manufacturer
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.InstalledAppInfo
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


/**
 * Provides info display: Icon, Title, Subtitle (with version logic).
 * Shows comparison if preInstallAppInfo is provided, otherwise shows only new version.
 */
@Composable
fun installInfoDialog(
    installer: InstallerRepo,
    viewModel: InstallerViewModel,
    onTitleExtraClick: () -> Unit = {}
): DialogParams {
    val settings = viewModel.viewSettings
    val iconMap by viewModel.displayIcons.collectAsState()
    val currentPackageName by viewModel.currentPackageName.collectAsState()
    val currentPackage = installer.analysisResults.find { it.packageName == currentPackageName }
    // If there's no current package to display, return empty params.
    if (currentPackage == null) return DialogParams()
    // The pre-install info is now directly available within main data model.
    val preInstallAppInfo = currentPackage.installedAppInfo
    val selectableEntities = currentPackage.appEntities

    val selectedApps = selectableEntities.filter { it.selected }.map { it.app }
    val totalSize = selectedApps.sumOf { it.size }
    // If no apps are selected, return empty DialogParams
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

    // Collect the icon state directly from the ViewModel.
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
            // Use a Row with centered arrangement.
            // This will automatically center its visible children as a group.
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.animateContentSize()
            ) {
                Text(
                    text = displayLabel,
                    modifier = Modifier
                        .basicMarquee()
                )
                // Use AnimatedVisibility to show the button with an animation.
                // When it becomes invisible, it will not take up any space,
                // and the Row will re-center the Text automatically.
                AnimatedVisibility(
                    visible = viewModel.state == InstallerViewState.InstallPrepare || viewModel.state == InstallerViewState.InstallSuccess,
                    enter = fadeIn() + slideInHorizontally { it }, // Slide in from the right
                    exit = fadeOut() + slideOutHorizontally { it }  // Slide out to the right
                ) {
                    // This inner Row groups the spacer and button so they animate as one unit.
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
        },
        subtitle = DialogInnerParams(uniqueContentKey) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                // verticalArrangement = Arrangement.spacedBy(4.dp) // Removed to avoid spacing issues during animation
            ) {
                // --- PackageName Display  ---
                Text(
                    stringResource(R.string.installer_package_name, entityToInstall.packageName),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.basicMarquee()
                )

                Spacer(modifier = Modifier.size(8.dp))
                // --- Version Info Display ---
                when (entityToInstall) {
                    is AppEntity.BaseEntity -> {
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
                            //
                            // true = singleLine, false = multiLine
                            val defaultIsSingleLine = settings.versionCompareInSingleLine

                            // Pair(first = isSingleLine, second = shouldAnimate)
                            var contentState by remember {
                                mutableStateOf(Pair(defaultIsSingleLine, false)) // Initial state, don't animate
                            }

                            // Sync with ViewModel default value without animation
                            LaunchedEffect(defaultIsSingleLine) {
                                if (contentState.first != defaultIsSingleLine) {
                                    contentState =
                                        Pair(defaultIsSingleLine, false) // Sync state, but flag for no animation
                                }
                            }

                            Box(
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    // On every click, toggle the line mode and ALWAYS enable animation
                                    contentState = Pair(!contentState.first, true)
                                }
                            ) {
                                AnimatedContent(
                                    targetState = contentState,
                                    transitionSpec = {
                                        // Check the 'shouldAnimate' flag from our state Pair
                                        if (targetState.second) {
                                            // Animate: This is for user clicks
                                            fadeIn(tween(200)) togetherWith fadeOut(tween(200)) using
                                                    SizeTransform { _, _ -> tween(250) }
                                        } else {
                                            // No animation: This is for initial composition or programmatic changes
                                            fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                                        }
                                    },
                                    label = "VersionViewAnimation"
                                ) { state ->
                                    // state is the Pair(isSingleLine, shouldAnimate)
                                    if (state.first) {
                                        VersionCompareSingleLine(preInstallAppInfo, entityToInstall)
                                    } else {
                                        VersionCompareMultiLine(preInstallAppInfo, entityToInstall)
                                    }
                                }
                            }
                        }
                    }

                    is AppEntity.ModuleEntity -> {
                        // For modules, just show the current info without comparison.
                        Text(
                            text = stringResource(
                                R.string.installer_version,
                                entityToInstall.version,
                                entityToInstall.versionCode
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.basicMarquee()
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = stringResource(
                                R.string.installer_author,
                                entityToInstall.author
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.basicMarquee(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    else -> {}
                }
                // --- SDK Information Showcase ---
                val defaultSdkSingleLine = !settings.sdkCompareInMultiLine
                var sdkContentState by remember { mutableStateOf(Pair(defaultSdkSingleLine, false)) }
                when (entityToInstall) {
                    is AppEntity.ModuleEntity -> {}

                    else -> {
                        AnimatedVisibility(
                            visible = installer.config.displaySdk
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        sdkContentState = Pair(!sdkContentState.first, true)
                                    }
                            ) {
                                AnimatedContent(
                                    targetState = sdkContentState,
                                    transitionSpec = {
                                        if (targetState.second) {
                                            fadeIn(tween(200)) togetherWith fadeOut(tween(200)) using
                                                    SizeTransform { _, _ -> tween(250) }
                                        } else {
                                            fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                                        }
                                    },
                                    label = "SdkViewAnimation"
                                ) { state ->
                                    if (state.first) {
                                        // compact single-line: 使用已有的短标签资源
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            entityToInstall.minSdk?.let { newMinSdk ->
                                                SdkInfoCompact(
                                                    shortLabelResId = R.string.installer_package_min_sdk_label_short,
                                                    newSdk = newMinSdk,
                                                    oldSdk = preInstallAppInfo?.minSdk?.toString(),
                                                    isUninstalled = preInstallAppInfo?.isUninstalled ?: false,
                                                    isArchived = preInstallAppInfo?.isArchived ?: false,
                                                    type = "min"
                                                )
                                            }
                                            Spacer(modifier = Modifier.size(16.dp))
                                            entityToInstall.targetSdk?.let { newTargetSdk ->
                                                SdkInfoCompact(
                                                    shortLabelResId = R.string.installer_package_target_sdk_label_short,
                                                    newSdk = newTargetSdk,
                                                    oldSdk = preInstallAppInfo?.targetSdk?.toString(),
                                                    isUninstalled = preInstallAppInfo?.isUninstalled ?: false,
                                                    isArchived = preInstallAppInfo?.isArchived ?: false,
                                                    type = "target"
                                                )
                                            }
                                        }
                                    } else {
                                        // expanded multi-line: 每个 SDK 独占一行，label 只出现一次（左侧），值使用 value-format 显示带 "(Android N)"
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            entityToInstall.minSdk?.let { newMinSdk ->
                                                SdkInfoExpanded(
                                                    labelPrefixResId = R.string.installer_package_min_label,
                                                    newSdk = newMinSdk,
                                                    oldSdk = preInstallAppInfo?.minSdk?.toString(),
                                                    isUninstalled = preInstallAppInfo?.isUninstalled ?: false,
                                                    isArchived = preInstallAppInfo?.isArchived ?: false,
                                                    type = "min"
                                                )
                                            }
                                            entityToInstall.targetSdk?.let { newTargetSdk ->
                                                SdkInfoExpanded(
                                                    labelPrefixResId = R.string.installer_package_target_label,
                                                    newSdk = newTargetSdk,
                                                    oldSdk = preInstallAppInfo?.targetSdk?.toString(),
                                                    isUninstalled = preInstallAppInfo?.isUninstalled ?: false,
                                                    isArchived = preInstallAppInfo?.isArchived ?: false,
                                                    type = "target"
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // --- Size Display ---
                AnimatedVisibility(visible = installer.config.displaySize && totalSize > 0L) {
                    Column {
                        Spacer(modifier = Modifier.size(8.dp))
                        SizeInfoDisplay(
                            oldSize = preInstallAppInfo?.packageSize ?: 0L,
                            newSize = totalSize
                        )
                    }
                }
                // --- OPPO Info Display ---
                if (RsConfig.currentManufacturer == Manufacturer.OPPO || RsConfig.currentManufacturer == Manufacturer.ONEPLUS)
                    AnimatedVisibility(settings.showOPPOSpecial && entityToInstall.sourceType == DataType.APK) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            (entityToInstall as AppEntity.BaseEntity).minOsdkVersion?.let {
                                Text(
                                    text = stringResource(R.string.installer_package_minOsdkVersion, it),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

            }
        }
    )
}


/**
 * Composable for displaying version comparison in multiple lines (the original style).
 */
@Composable
private fun VersionCompareMultiLine(
    preInstallAppInfo: InstalledAppInfo,
    entityToInstall: AppEntity.BaseEntity
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val oldVersionText = when {
            preInstallAppInfo.isArchived -> stringResource(R.string.old_version_archived)
            preInstallAppInfo.isUninstalled -> stringResource(R.string.old_version_uninstalled)
            else -> preInstallAppInfo.versionName
        }

        Text( // Old version with prefix
            text = stringResource(R.string.old_version_prefix) +
                    stringResource(
                        R.string.installer_version_short,
                        oldVersionText,
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
        Text( // New version with prefix
            text = if (entityToInstall.versionCode >= preInstallAppInfo.versionCode)
                stringResource(R.string.upgrade_version_prefix) + stringResource(
                    R.string.installer_version_short,
                    entityToInstall.versionName,
                    entityToInstall.versionCode
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

/**
 * Composable for displaying version comparison in a single line (e.g., "1.0 (1) -> 2.0 (2)").
 */
@Composable
private fun VersionCompareSingleLine(
    preInstallAppInfo: InstalledAppInfo,
    entityToInstall: AppEntity.BaseEntity
) {
    val isDowngrade = preInstallAppInfo.versionCode > entityToInstall.versionCode
    val color = if (isDowngrade) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val oldVersionNameContent = when {
        preInstallAppInfo.isArchived -> stringResource(R.string.old_version_archived)
        preInstallAppInfo.isUninstalled -> stringResource(R.string.old_version_uninstalled)
        else -> preInstallAppInfo.versionName
    }
    val oldVersionText = stringResource(
        R.string.installer_version_short,
        oldVersionNameContent,
        preInstallAppInfo.versionCode
    )
    val newVersionText = stringResource(
        R.string.installer_version2,
        entityToInstall.versionName,
        entityToInstall.versionCode
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
    ) {
        Text(text = oldVersionText, textAlign = TextAlign.Center)
        Icon(
            imageVector = AppIcons.ArrowRight,
            contentDescription = "to",
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(text = newVersionText, color = color, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SdkInfoCompact(
    @StringRes shortLabelResId: Int,
    newSdk: String,
    oldSdk: String?,
    isUninstalled: Boolean = false,
    isArchived: Boolean = false,
    type: String
) {
    val newSdkInt = newSdk.toIntOrNull()
    val oldSdkInt = oldSdk?.toIntOrNull()

    val showComparison = oldSdkInt != null && newSdkInt != null && newSdkInt != oldSdkInt

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
    ) {
        if (showComparison) {
            val isDowngrade = newSdkInt < oldSdkInt
            val isIncompatible = type == "min" && newSdkInt > Build.VERSION.SDK_INT
            val color =
                if (isDowngrade || isIncompatible) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

            val oldValueContent = when {
                isArchived -> stringResource(R.string.old_version_archived)
                isUninstalled -> stringResource(R.string.old_version_uninstalled)
                else -> oldSdk.orEmpty()
            }
            val oldText = stringResource(shortLabelResId, oldValueContent)
            Text(text = oldText, style = MaterialTheme.typography.bodyMedium)

            Icon(
                imageVector = AppIcons.ArrowRight,
                contentDescription = "to",
                tint = color,
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = newSdk,
                color = color,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            val textColor = if (type == "min" && newSdkInt != null && newSdkInt > Build.VERSION.SDK_INT) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            val newSdkText = stringResource(shortLabelResId, newSdk)
            Text(
                text = newSdkText,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Expanded (multi-line) SDK display.
 * - labelPrefixResId: label only, e.g. "minSDK:" (no value placeholders)
 * - valueFormatResId: value format, e.g. "%1$s (Android %2$s)"
 */
@Composable
private fun SdkInfoExpanded(
    @StringRes labelPrefixResId: Int,
    newSdk: String,
    oldSdk: String?,
    isUninstalled: Boolean = false,
    isArchived: Boolean = false,
    type: String
) {
    val newSdkInt = newSdk.toIntOrNull()
    val oldSdkInt = oldSdk?.toIntOrNull()
    val showComparison = oldSdkInt != null && newSdkInt != null && newSdkInt != oldSdkInt

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
    ) {
        val labelPrefix = stringResource(labelPrefixResId)

        if (showComparison) {
            val isDowngrade = newSdkInt < oldSdkInt
            val isIncompatible = type == "min" && newSdkInt > Build.VERSION.SDK_INT
            val color =
                if (isDowngrade || isIncompatible) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

            // --- Label ---
            Text(text = labelPrefix, style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.size(4.dp))

            // --- Old value ---
            when {
                isArchived -> Text(
                    text = stringResource(R.string.old_version_archived),
                    style = MaterialTheme.typography.bodyMedium
                )

                isUninstalled -> Text(
                    text = stringResource(R.string.old_version_uninstalled),
                    style = MaterialTheme.typography.bodyMedium
                )

                else -> SdkValueWithIcon(sdk = oldSdk, color = MaterialTheme.colorScheme.onSurface)
            }

            Icon(
                imageVector = AppIcons.ArrowRight,
                contentDescription = "to",
                tint = color,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(20.dp)
            )

            // --- New value ---
            SdkValueWithIcon(sdk = newSdk, color = color)

        } else {
            val textColor = if (type == "min" && newSdkInt != null && newSdkInt > Build.VERSION.SDK_INT) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            }

            Text(text = labelPrefix, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.size(4.dp))
            SdkValueWithIcon(sdk = newSdk, color = textColor)
        }
    }
}

@Composable
private fun SdkValueWithIcon(
    sdk: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = sdk,
            color = color,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.size(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(color.copy(alpha = 0.1f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = AppIcons.Android,
                contentDescription = "Android",
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.size(2.dp))
            Text(
                text = sdk.toAndroidVersionName(),
                color = color,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SizeInfoDisplay(
    oldSize: Long,
    newSize: Long
) {
    val showComparison = oldSize > 0L && oldSize != newSize
    var showDiffOnly by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .clickable(
                enabled = showComparison,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showDiffOnly = !showDiffOnly
            }
    ) {
        AnimatedContent(
            targetState = showComparison && showDiffOnly,
            transitionSpec = {
                fadeIn(tween(200)) togetherWith fadeOut(tween(200)) using
                        SizeTransform { _, _ -> tween(250) }
            },
            label = "SizeDisplayAnimation"
        ) { isDiffMode ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
            ) {
                // Label: "Size:"
                Text(
                    text = stringResource(R.string.installer_package_size_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (isDiffMode) {
                    // --- Diff Mode ---
                    val diff = newSize - oldSize

                    // 1. Get absolute value for formatting (ensure formatSize receives a positive number)
                    val absDiff = abs(diff)
                    val diffString = absDiff.formatSize()

                    // 2. Manually append the sign based on whether diff is positive or negative
                    val finalString = when {
                        diff > 0 -> "+$diffString" // Increased
                        diff < 0 -> "-$diffString" // Decreased
                        else -> diffString         // Unchanged (0 B)
                    }

                    // Color logic: Use Primary color for both increase and decrease
                    val color = MaterialTheme.colorScheme.primary

                    Text(
                        text = finalString,
                        style = MaterialTheme.typography.bodyMedium,
                        color = color,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                } else {
                    // --- Full Mode: "35.2 MB -> 42.0 MB" ---
                    if (showComparison) {
                        // Old size
                        Text(
                            text = oldSize.formatSize(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        Icon(
                            imageVector = AppIcons.ArrowRight,
                            contentDescription = "to",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(24.dp)
                        )
                    } else {
                        // Add a spacer to separate the new size from the label when there is no old version
                        Spacer(modifier = Modifier.size(4.dp))
                    }

                    // New size
                    Text(
                        text = newSize.formatSize(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}