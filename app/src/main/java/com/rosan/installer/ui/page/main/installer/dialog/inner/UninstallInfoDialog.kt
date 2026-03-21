// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer.dialog.inner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.installer.InstallerStage
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParamsType

/**
 * Provides a base dialog structure for the uninstall process.
 * It displays the app's icon, label, package name, and version based on UninstallInfo.
 * @param viewModel The ViewModel holding the state and data for the dialog.
 * @param onTitleExtraClick Callback to be invoked when the extra button in the title is clicked.
 * @return A DialogParams object populated with the app's basic information.
 */
@Composable
fun uninstallInfoDialog(
    viewModel: InstallerViewModel,
    onTitleExtraClick: () -> Unit = {}
): DialogParams {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appInfo = uiState.uiUninstallInfo ?: return DialogParams()
    val stage = uiState.stage

    // Get the managed ImageBitmap from the centralized map
    val displayIcon = uiState.displayIcons[appInfo.packageName]

    return DialogParams(
        icon = DialogInnerParams(DialogParamsType.InstallerUninstallInfo.id) {
            // Using AnimatedContent to handle transition from loading (null) to loaded
            androidx.compose.animation.AnimatedContent(targetState = displayIcon) { iconBitmap ->
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (iconBitmap != null) {
                        Image(
                            bitmap = iconBitmap,
                            modifier = Modifier.fillMaxSize(),
                            contentDescription = null
                        )
                    } else {
                        // Fixed size placeholder while loading
                        Spacer(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        },
        title = DialogInnerParams(DialogParamsType.InstallerUninstallInfo.id) {
            Row(
                modifier = Modifier.animateContentSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = appInfo.appLabel ?: "Unknown Package",
                    modifier = Modifier.basicMarquee()
                )
                AnimatedVisibility(
                    visible = stage == InstallerStage.UninstallReady,
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
                            Icon(
                                imageVector = AppIcons.AutoFixHigh,
                                contentDescription = null,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
            }
        },
        subtitle = DialogInnerParams(DialogParamsType.InstallerUninstallInfo.id) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.installer_package_name, appInfo.packageName),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.basicMarquee()
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(
                        R.string.installer_version,
                        appInfo.versionName ?: "N/A",
                        appInfo.versionCode?.toString() ?: "N/A"
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
    )
}
