// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer.components

import android.content.ClipData
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itos.coui.shape.CouiShape
import com.itos.coui.theme.CouiTheme
import com.rosan.installer.R
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.packageinfo.PackageAnalysisResult
import com.rosan.installer.domain.engine.model.packageinfo.sortedBest
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.util.toast
import kotlinx.coroutines.launch

@Immutable
data class CouiAppInfoState(
    val icon: ImageBitmap?,
    val label: String,
    val packageName: String,
    val primaryEntity: AppEntity? = null,
)

@Composable
fun rememberCouiAppInfoState(
    analysisResults: List<PackageAnalysisResult>,
    currentPackageName: String?,
    displayIcons: Map<String, ImageBitmap?>,
): CouiAppInfoState = remember(analysisResults, currentPackageName, displayIcons) {
    val currentPackage = if (currentPackageName != null) {
        analysisResults.find { it.packageName == currentPackageName }
    } else {
        analysisResults.firstOrNull()
    }

    var label = "Unknown App"
    var packageName = "unknown.package"
    var icon: ImageBitmap? = null
    var primaryEntity: AppEntity? = null

    if (currentPackage != null) {
        val allApps = currentPackage.appEntities.map { it.app }
        primaryEntity = allApps.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
            ?: allApps.filterIsInstance<AppEntity.ModuleEntity>().firstOrNull()
            ?: allApps.filterIsInstance<AppEntity.SplitEntity>().firstOrNull()
            ?: allApps.sortedBest().firstOrNull()

        primaryEntity?.let { entity ->
            packageName = entity.packageName
            label = when (entity) {
                is AppEntity.BaseEntity -> entity.label ?: entity.packageName
                is AppEntity.ModuleEntity -> entity.name
                else -> entity.packageName
            }
        }

        if (currentPackageName != null) {
            icon = displayIcons[currentPackageName]
        }
    }

    CouiAppInfoState(
        icon = icon,
        label = label,
        packageName = packageName,
        primaryEntity = primaryEntity,
    )
}

@Composable
fun CouiAppInfoSlot(
    modifier: Modifier = Modifier,
    appInfo: CouiAppInfoState,
    onIconClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val scheme = CouiTheme.colorScheme

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CouiShape(16.dp, 3f))
                .then(if (onIconClick != null) Modifier.clickable { onIconClick() } else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (appInfo.icon != null) {
                Image(
                    bitmap = appInfo.icon,
                    contentDescription = "App Icon",
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Text(
            modifier = Modifier
                .basicMarquee()
                .pointerInput(appInfo.label) {
                    detectTapGestures(
                        onLongPress = {
                            scope.launch {
                                val clipData = ClipData.newPlainText("App Name", appInfo.label)
                                clipboard.setClipEntry(clipData.toClipEntry())
                                context.toast(R.string.copied_format, appInfo.label)
                            }
                        },
                    )
                },
            text = appInfo.label,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = scheme.onSurface,
        )
        Text(
            modifier = Modifier
                .basicMarquee()
                .pointerInput(appInfo.packageName) {
                    detectTapGestures(
                        onLongPress = {
                            scope.launch {
                                val clipData = ClipData.newPlainText("Package Name", appInfo.packageName)
                                clipboard.setClipEntry(clipData.toClipEntry())
                                context.toast(R.string.copied_format, appInfo.packageName)
                            }
                        },
                    )
                },
            text = appInfo.packageName,
            fontSize = 13.sp,
            color = scheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun CouiAdaptiveInfoRow(
    @StringRes labelResId: Int,
    newValue: String,
    oldValue: String?,
    isUninstalled: Boolean = false,
    isArchived: Boolean = false,
    hideIdenticalComparison: Boolean = false,
) {
    val scheme = CouiTheme.colorScheme
    val showComparison = oldValue != null && (!hideIdenticalComparison || newValue != oldValue)
    val oldTextContent = when {
        isArchived -> stringResource(R.string.old_version_archived)
        isUninstalled -> if (oldValue.isNullOrEmpty()) stringResource(R.string.old_version_uninstalled) else oldValue
        else -> oldValue.orEmpty()
    }

    Layout(
        content = {
            Text(
                text = stringResource(labelResId),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = scheme.onSurface,
            )

            if (showComparison) {
                Text(
                    text = oldTextContent,
                    fontSize = 14.sp,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                )
                Icon(
                    imageVector = AppIcons.ArrowIndicator,
                    contentDescription = "to",
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(16.dp),
                )
                Text(
                    text = newValue,
                    fontSize = 14.sp,
                    color = scheme.onSurface,
                    textAlign = TextAlign.End,
                )
            } else {
                Text(
                    text = newValue,
                    fontSize = 14.sp,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        val spacing = 16.dp.roundToPx()
        val labelPlaceable = measurables[0].measure(Constraints(minWidth = 0, maxWidth = constraints.maxWidth))

        if (showComparison) {
            val oldMeasurable = measurables[1]
            val arrowMeasurable = measurables[2]
            val newMeasurable = measurables[3]

            val oldMaxWidthReq = oldMeasurable.maxIntrinsicWidth(constraints.maxHeight)
            val arrowMaxWidthReq = arrowMeasurable.maxIntrinsicWidth(constraints.maxHeight)
            val newMaxWidthReq = newMeasurable.maxIntrinsicWidth(constraints.maxHeight)
            val totalSingleLineWidth = labelPlaceable.width + spacing + oldMaxWidthReq + arrowMaxWidthReq + newMaxWidthReq

            if (totalSingleLineWidth <= constraints.maxWidth) {
                val oldPlaceable = oldMeasurable.measure(Constraints(minWidth = 0, maxWidth = oldMaxWidthReq))
                val arrowPlaceable = arrowMeasurable.measure(Constraints(minWidth = 0, maxWidth = arrowMaxWidthReq))
                val newPlaceable = newMeasurable.measure(Constraints(minWidth = 0, maxWidth = newMaxWidthReq))
                val height = maxOf(labelPlaceable.height, oldPlaceable.height, arrowPlaceable.height, newPlaceable.height)

                layout(constraints.maxWidth, height) {
                    labelPlaceable.placeRelative(0, Alignment.CenterVertically.align(labelPlaceable.height, height))
                    var currentX = constraints.maxWidth
                    currentX -= newPlaceable.width
                    newPlaceable.placeRelative(currentX, Alignment.CenterVertically.align(newPlaceable.height, height))
                    currentX -= arrowPlaceable.width
                    arrowPlaceable.placeRelative(currentX, Alignment.CenterVertically.align(arrowPlaceable.height, height))
                    currentX -= oldPlaceable.width
                    oldPlaceable.placeRelative(currentX, Alignment.CenterVertically.align(oldPlaceable.height, height))
                }
            } else {
                val oldMaxWidth = maxOf(0, constraints.maxWidth - labelPlaceable.width - spacing)
                val oldPlaceable = oldMeasurable.measure(Constraints(minWidth = 0, maxWidth = oldMaxWidth))
                val arrowPlaceable = arrowMeasurable.measure(Constraints())
                val newMaxWidth = maxOf(0, constraints.maxWidth - arrowPlaceable.width)
                val newPlaceable = newMeasurable.measure(Constraints(minWidth = 0, maxWidth = newMaxWidth))
                val verticalSpacing = 4.dp.roundToPx()
                val line1Height = maxOf(labelPlaceable.height, oldPlaceable.height)
                val line2Height = maxOf(arrowPlaceable.height, newPlaceable.height)
                val totalHeight = line1Height + verticalSpacing + line2Height

                layout(constraints.maxWidth, totalHeight) {
                    labelPlaceable.placeRelative(0, Alignment.CenterVertically.align(labelPlaceable.height, line1Height))
                    oldPlaceable.placeRelative(
                        constraints.maxWidth - oldPlaceable.width,
                        Alignment.CenterVertically.align(oldPlaceable.height, line1Height),
                    )
                    val line2Y = line1Height + verticalSpacing
                    newPlaceable.placeRelative(
                        constraints.maxWidth - newPlaceable.width,
                        line2Y + Alignment.CenterVertically.align(newPlaceable.height, line2Height),
                    )
                    arrowPlaceable.placeRelative(
                        constraints.maxWidth - newPlaceable.width - arrowPlaceable.width,
                        line2Y + Alignment.CenterVertically.align(arrowPlaceable.height, line2Height),
                    )
                }
            }
        } else {
            val valueMaxWidth = maxOf(0, constraints.maxWidth - labelPlaceable.width - spacing)
            val valuePlaceable = measurables[1].measure(Constraints(minWidth = 0, maxWidth = valueMaxWidth))
            val height = maxOf(labelPlaceable.height, valuePlaceable.height)
            layout(constraints.maxWidth, height) {
                labelPlaceable.placeRelative(0, Alignment.CenterVertically.align(labelPlaceable.height, height))
                valuePlaceable.placeRelative(
                    constraints.maxWidth - valuePlaceable.width,
                    Alignment.CenterVertically.align(valuePlaceable.height, height),
                )
            }
        }
    }
}
