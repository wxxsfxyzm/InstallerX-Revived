package com.rosan.installer.ui.page.main.installer.dialog.inner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.data.app.util.PackageManagerUtil
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParamsType
import com.rosan.installer.ui.page.main.widget.chip.Chip

/**
 * Displays the initial confirmation screen for an uninstall operation.
 * It provides an option to keep app data, shown via an animated Chip.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun uninstallReadyDialog(
    viewModel: InstallerViewModel
): DialogParams {
    // State to control the visibility of the animated chip.
    var showChips by remember { mutableStateOf(false) }

    // Use the shared info dialog and pass the click handler to toggle chip visibility.
    val baseParams = uninstallInfoDialog(
        viewModel = viewModel,
        onTitleExtraClick = { showChips = !showChips }
    )

    // Collect uninstall flags to manage the state of the "keep data" chip.
    val uninstallFlags by viewModel.uninstallFlags.collectAsState()
    val deleteKeepData = (uninstallFlags and PackageManagerUtil.DELETE_KEEP_DATA) != 0
    val deleteAllUsers = (uninstallFlags and PackageManagerUtil.DELETE_ALL_USERS) != 0
    val deleteSystemApp = (uninstallFlags and PackageManagerUtil.DELETE_SYSTEM_APP) != 0

    // Override the 'text' and 'buttons' sections of the base parameters.
    return baseParams.copy(
        text = DialogInnerParams(DialogParamsType.InstallerUninstallReady.id) {
            // Use AnimatedVisibility to show/hide the chip with animation.
            AnimatedVisibility(
                visible = showChips,
                enter = fadeIn() + expandVertically(), // Fade in + expand
                exit = fadeOut() + shrinkVertically()  // Fade out + shrink
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Chip(
                        selected = deleteKeepData,
                        onClick = {
                            // Dispatch the action to toggle the flag in the ViewModel.
                            viewModel.dispatch(
                                InstallerViewAction.ToggleUninstallFlag(
                                    flag = PackageManagerUtil.DELETE_KEEP_DATA,
                                    enable = !deleteKeepData
                                )
                            )
                        },
                        label = stringResource(id = R.string.uninstall_keep_data),
                        icon = AppIcons.Save // Using a "Save" icon for "Keep data"
                    )
                    Chip(
                        selected = deleteAllUsers,
                        onClick = {
                            // Dispatch the action to toggle the flag in the ViewModel.
                            viewModel.dispatch(
                                InstallerViewAction.ToggleUninstallFlag(
                                    flag = PackageManagerUtil.DELETE_ALL_USERS,
                                    enable = !deleteAllUsers
                                )
                            )
                        },
                        label = stringResource(id = R.string.uninstall_all_users),
                        icon = AppIcons.InstallForAllUsers // Using a "Group" icon for "All users"
                    )
                    Chip(
                        selected = deleteSystemApp,
                        onClick = {
                            // Dispatch the action to toggle the flag in the ViewModel.
                            viewModel.dispatch(
                                InstallerViewAction.ToggleUninstallFlag(
                                    flag = PackageManagerUtil.DELETE_SYSTEM_APP,
                                    enable = !deleteSystemApp
                                )
                            )
                        },
                        label = stringResource(id = R.string.uninstall_delete_system_app),
                        icon = AppIcons.BugReport // Using a "Warning" icon for "Delete system app"
                    )
                }
            }
        },
        buttons = dialogButtons(DialogParamsType.InstallerUninstallReady.id) {
            listOf(
                // Uninstall button triggers the uninstall action.
                DialogButton(stringResource(R.string.uninstall)) {
                    viewModel.dispatch(InstallerViewAction.Uninstall)
                },
                // Cancel button closes the dialog.
                DialogButton(stringResource(R.string.cancel)) {
                    viewModel.dispatch(InstallerViewAction.Close)
                }
            )
        }
    )
}