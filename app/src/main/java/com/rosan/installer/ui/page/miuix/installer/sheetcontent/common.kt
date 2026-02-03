package com.rosan.installer.ui.page.miuix.installer.sheetcontent

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.util.sortedBest
import com.rosan.installer.data.installer.repo.InstallerRepo
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Holds the unified display information for the application.
 * This ensures consistency across all installation steps (Prepare, Installing, Success, Failed).
 */
@Immutable
data class AppInfoState(
    val icon: Drawable?,
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
    installer: InstallerRepo,
    currentPackageName: String?,
    displayIcons: Map<String, Drawable?>
): AppInfoState {
    return remember(installer, currentPackageName, displayIcons) {
        val currentPackage = if (currentPackageName != null) {
            installer.analysisResults.find { it.packageName == currentPackageName }
        } else {
            installer.analysisResults.firstOrNull()
        }

        // Default fallback values
        var label = "Unknown App"
        var packageName = "unknown.package"
        var icon: Drawable? = null
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
}

@Composable
fun AppInfoSlot(
    appInfo: AppInfoState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            painter = rememberDrawablePainter(drawable = appInfo.icon),
            contentDescription = "App Icon",
            modifier = Modifier.size(72.dp)
        )
        Text(
            text = appInfo.label,
            style = MiuixTheme.textStyles.title2,
            modifier = Modifier.basicMarquee()
        )
        Text(
            text = appInfo.packageName,
            style = MiuixTheme.textStyles.subtitle,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
    }
}