// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.installer.sheetcontent

import android.content.ClipData
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.domain.engine.model.AppEntity
import com.rosan.installer.domain.engine.model.PackageAnalysisResult
import com.rosan.installer.domain.engine.model.sortedBest
import com.rosan.installer.util.toast
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Holds the unified display information for the application.
 * This ensures consistency across all installation steps (Prepare, Installing, Success, Failed).
 */
@Immutable
data class AppInfoState(
    val icon: ImageBitmap?,
    val label: String,
    val packageName: String,
    // Helper to access the underlying entity if specific logic needs it (e.g. version comparison)
    val primaryEntity: AppEntity? = null
)

/**
 * Calculates the best AppInfoState based on the installer repository and view model data.
 * Consolidates the logic for determining which entity (Base, Module, or Split) represents the app.
 */
@Composable
fun rememberAppInfoState(
    analysisResults: List<PackageAnalysisResult>,
    currentPackageName: String?,
    displayIcons: Map<String, ImageBitmap?>
): AppInfoState = remember(analysisResults, currentPackageName, displayIcons) {
    val currentPackage = if (currentPackageName != null) {
        analysisResults.find { it.packageName == currentPackageName }
    } else {
        analysisResults.firstOrNull()
    }

    // Default fallback values
    var label = "Unknown App"
    var packageName = "unknown.package"
    var icon: ImageBitmap? = null
    var primaryEntity: AppEntity? = null

    if (currentPackage != null) {
        val allApps = currentPackage.appEntities.map { it.app }

        // Logic extracted from InstallPrepareContent:
        // Prioritize BaseEntity -> ModuleEntity -> SplitEntity -> Best Guess
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

        // Resolve icon
        if (currentPackageName != null) {
            icon = displayIcons[currentPackageName]
        }
    }

    AppInfoState(
        icon = icon,
        label = label,
        packageName = packageName,
        primaryEntity = primaryEntity
    )
}

@Composable
fun AppInfoSlot(
    modifier: Modifier = Modifier,
    appInfo: AppInfoState,
    // Callback for icon click events. Null means not clickable.
    onIconClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(16.dp))
                .then(
                    if (onIconClick != null) Modifier.clickable { onIconClick() } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (appInfo.icon != null) {
                Image(
                    bitmap = appInfo.icon,
                    contentDescription = "App Icon",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Text(
            modifier = Modifier
                .basicMarquee()
                .pointerInput(appInfo.label) {
                    detectTapGestures(
                        onLongPress = {
                            // Copy the app name to clipboard on long press
                            scope.launch {
                                val clipData = ClipData.newPlainText("App Name", appInfo.label)
                                clipboard.setClipEntry(clipData.toClipEntry())
                                context.toast(R.string.copied_format, appInfo.label)
                            }
                        }
                    )
                },
            text = appInfo.label,
            style = MiuixTheme.textStyles.title2,
            color = MiuixTheme.colorScheme.onSurface
        )
        Text(
            modifier = Modifier
                .basicMarquee()
                .pointerInput(appInfo.packageName) {
                    detectTapGestures(
                        onLongPress = {
                            // Copy the package name to clipboard on long press
                            scope.launch {
                                val clipData = ClipData.newPlainText("Package Name", appInfo.packageName)
                                clipboard.setClipEntry(clipData.toClipEntry())
                                context.toast(R.string.copied_format, appInfo.packageName)
                            }
                        }
                    )
                },
            text = appInfo.packageName,
            style = MiuixTheme.textStyles.subtitle,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
    }
}
