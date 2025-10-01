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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParamsType
import com.rosan.installer.ui.page.main.installer.dialog.DialogViewModel
import com.rosan.installer.ui.page.main.installer.dialog.DialogViewState

/**
 * Provides a base dialog structure for the uninstall process.
 * It displays the app's icon, label, package name, and version based on UninstallInfo.
 * @param viewModel The ViewModel holding the state and data for the dialog.
 * @return A DialogParams object populated with the app's basic information.
 */
@Composable
fun uninstallInfoDialog(
    viewModel: DialogViewModel,
    onTitleExtraClick: () -> Unit = {}
): DialogParams {
    // Collect the UninstallInfo state from the ViewModel.
    val uninstallInfo by viewModel.uiUninstallInfo.collectAsState()
    val appInfo = uninstallInfo ?: return DialogParams() // Return empty if no info is available.

    // Collect the icon map and get the specific icon for the current package.
    //val iconMap by viewModel.displayIcons.collectAsState()
    //val displayIcon = iconMap[appInfo.packageName]

    // A unique key to ensure AnimatedContent updates correctly when the target app changes.
    //val uniqueContentKey = "${DialogParamsType.UninstallerInfo.id}_${appInfo.packageName}"

    return DialogParams(
        icon = DialogInnerParams(DialogParamsType.InstallerUninstallInfo.id) {
            Image(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp)),
                painter = rememberDrawablePainter(appInfo.appIcon),
                contentDescription = null
            )
        },
        title = DialogInnerParams(DialogParamsType.InstallerUninstallInfo.id) {
            // Use a Row with centered arrangement.
            // This will automatically center its visible children as a group.
            Row(
                modifier = Modifier.animateContentSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = appInfo.appLabel ?: "Unknown Package",
                    modifier = Modifier.basicMarquee()
                )
                // Use AnimatedVisibility to show the button with an animation.
                // When it becomes invisible, it will not take up any space,
                // and the Row will re-center the Text automatically.
                AnimatedVisibility(
                    visible = viewModel.state == DialogViewState.UninstallReady,
                    enter = fadeIn() + slideInHorizontally { it }, // Slide in from the right
                    exit = fadeOut() + slideOutHorizontally { it }  // Slide out to the right
                ) {
                    // This inner Row groups the spacer and button so they animate as one unit.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Add a small spacer between the text and the button.
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Display the package name.
                Text(
                    stringResource(R.string.installer_package_name, appInfo.packageName),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.basicMarquee()
                )

                Spacer(modifier = Modifier.size(8.dp))

                // Display the version information.
                Text(
                    text = stringResource(
                        R.string.installer_version,
                        appInfo.versionName ?: "N/A",
                        appInfo.versionCode ?: "N/A"
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
    )
}