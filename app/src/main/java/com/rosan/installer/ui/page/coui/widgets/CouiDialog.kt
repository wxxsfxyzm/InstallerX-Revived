// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionColors
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.dialog.CouiDialogMaterial
import com.itos.coui.dialog.CouiSingleChoiceItem
import com.itos.coui.dialog.Dialog
import com.itos.coui.dialog.InputDialog
import com.itos.coui.dialog.LoadingDialog
import com.itos.coui.dialog.SingleChoiceDialog
import com.itos.coui.icons.CouiIcons
import com.itos.coui.layout.Divider
import com.itos.coui.layout.Group
import com.itos.coui.menu.rememberCouiWindowBackdrop
import com.itos.coui.sheet.BottomSheet
import com.itos.coui.sheet.CouiSheetButton
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.preferences.GithubUpdateChannel
import com.rosan.installer.domain.settings.model.preferences.RootMode

@Composable
fun rememberCouiDialogMaterial(enableBlur: Boolean = true): CouiDialogMaterial? {
    val backdrop = rememberCouiWindowBackdrop()
    val dark = CouiTheme.isDark
    return if (enableBlur && CouiDialogDefaults.isMaterialSupported() && backdrop != null) {
        CouiDialogDefaults.material(dark)
    } else {
        null
    }
}

@Composable
fun rememberCouiDestructiveActionColors(): CouiDialogActionColors {
    val dark = CouiTheme.isDark
    return remember(dark) {
        CouiDialogDefaults.primaryActionColors(CouiDialogDefaults.colors(dark).destructive)
    }
}

@Composable
fun CouiUnsavedChangesDialog(
    showState: MutableState<Boolean>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    errorMessages: List<String>,
) {
    val hasSpecificErrors = errorMessages.isNotEmpty()
    val dialogTitle = if (hasSpecificErrors) {
        stringResource(R.string.config_dialog_title_invalid)
    } else {
        stringResource(R.string.config_dialog_title_unsaved_changes)
    }
    val message = if (hasSpecificErrors) {
        errorMessages.joinToString("\n")
    } else {
        stringResource(R.string.config_dialog_message_unsaved_changes)
    }
    val backdrop = rememberCouiWindowBackdrop()
    val material = rememberCouiDialogMaterial()

    Dialog(
        visible = showState.value,
        onDismissRequest = onDismiss,
        title = dialogTitle,
        message = message,
        confirmAction = CouiDialogAction(
            text = stringResource(R.string.discard),
            onClick = onConfirm,
            style = CouiDialogActionStyle.Primary,
        ),
        dismissAction = CouiDialogAction(
            text = stringResource(R.string.back),
            onClick = onDismiss,
        ),
        material = material,
        backdrop = backdrop,
    )
}

@Composable
fun CouiUpdateDialog(showState: MutableState<Boolean>, onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val backdrop = rememberCouiWindowBackdrop()
    val material = rememberCouiDialogMaterial()

    Dialog(
        visible = showState.value,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.get_update),
        confirmAction = CouiDialogAction(
            text = stringResource(R.string.cancel),
            onClick = onDismiss,
        ),
        material = material,
        backdrop = backdrop,
    ) {
        Group {
            CouiBasicItem(
                title = "GitHub",
                onClick = {
                    uriHandler.openUri("https://github.com/wxxsfxyzm/InstallerX-Revived/releases")
                    onDismiss()
                },
                trailing = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_link_icon),
                        contentDescription = null,
                    )
                },
            )
            Divider()
            CouiBasicItem(
                title = "Telegram",
                onClick = {
                    uriHandler.openUri("https://t.me/installerx_revived")
                    onDismiss()
                },
                trailing = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_link_icon),
                        contentDescription = null,
                    )
                },
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun CouiUninstallConfirmationDialog(
    showState: MutableState<Boolean>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    keepData: Boolean,
) {
    val message = if (keepData) {
        stringResource(R.string.suggestion_uninstall_alert_dialog_confirm_uninstall_keep_data_message)
    } else {
        stringResource(R.string.suggestion_uninstall_alert_dialog_confirm_uninstall_no_data_message)
    }
    val backdrop = rememberCouiWindowBackdrop()
    val material = rememberCouiDialogMaterial()

    Dialog(
        visible = showState.value,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.suggestion_uninstall_alert_dialog_confirm_action),
        message = message,
        confirmAction = CouiDialogAction(
            text = stringResource(R.string.confirm),
            onClick = onConfirm,
            style = CouiDialogActionStyle.Primary,
            colors = rememberCouiDestructiveActionColors(),
        ),
        dismissAction = CouiDialogAction(
            text = stringResource(R.string.cancel),
            onClick = onDismiss,
        ),
        material = material,
        backdrop = backdrop,
    )
}

@Composable
fun CouiErrorDisplaySheet(
    showState: MutableState<Boolean>,
    exception: Throwable,
    onDismissRequest: () -> Unit,
    onRetry: (() -> Unit)? = null,
    title: String,
) {
    BottomSheet(
        visible = showState,
        onDismissRequest = onDismissRequest,
        leadingButton = CouiSheetButton(
            onClick = onDismissRequest,
            icon = {
                Icon(
                    imageVector = CouiIcons.Filled.Close,
                    contentDescription = stringResource(R.string.close),
                )
            },
        ),
        titleContent = {
            Text(text = title, kind = CouiTextKind.SheetTitle)
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CouiErrorTextBlock(
                error = exception,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (onRetry != null) {
                    CouiActionButton(
                        text = stringResource(R.string.cancel),
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f),
                    )
                    CouiActionButton(
                        text = stringResource(R.string.retry),
                        onClick = onRetry,
                        primary = true,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    CouiActionButton(
                        text = stringResource(R.string.close),
                        onClick = onDismissRequest,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
fun CouiRootImplementationDialog(
    showState: MutableState<Boolean>,
    onDismiss: () -> Unit,
    onConfirm: (RootMode) -> Unit,
) {
    val rootModes = remember {
        listOf(
            CouiSingleChoiceItem(RootMode.Magisk, "Magisk"),
            CouiSingleChoiceItem(RootMode.KernelSU, "KernelSU"),
            CouiSingleChoiceItem(RootMode.APatch, "APatch"),
        )
    }
    var selectedImpl by remember { mutableStateOf(RootMode.Magisk) }

    SingleChoiceDialog(
        visible = showState.value,
        title = stringResource(R.string.lab_module_select_root_impl),
        items = rootModes,
        selectedValue = selectedImpl,
        onItemSelected = { selectedImpl = it },
        onDismissRequest = onDismiss,
        actionRows = listOf(
            listOf(
                CouiDialogAction(text = stringResource(R.string.cancel), onClick = onDismiss),
                CouiDialogAction(
                    text = stringResource(R.string.confirm),
                    onClick = {
                        onConfirm(selectedImpl)
                        onDismiss()
                    },
                    style = CouiDialogActionStyle.Primary,
                ),
            ),
        ),
    )
}

@Composable
fun CouiUninstallPackageDialog(
    showState: MutableState<Boolean>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var packageName by remember { mutableStateOf(TextFieldValue("")) }
    val backdrop = rememberCouiWindowBackdrop()
    val material = rememberCouiDialogMaterial()

    LaunchedEffect(showState.value) {
        if (showState.value) {
            packageName = TextFieldValue("")
        }
    }

    InputDialog(
        visible = showState.value,
        value = packageName,
        onValueChange = { packageName = it },
        onDismissRequest = onDismiss,
        title = stringResource(R.string.uninstall_enter_package_name),
        placeholder = stringResource(R.string.config_package_name),
        clearContentDescription = stringResource(R.string.clear),
        confirmAction = CouiDialogAction(
            text = stringResource(R.string.confirm),
            onClick = {
                onConfirm(packageName.text.trim())
                packageName = TextFieldValue("")
            },
            enabled = packageName.text.isNotBlank(),
            style = CouiDialogActionStyle.Primary,
        ),
        dismissAction = CouiDialogAction(
            text = stringResource(R.string.cancel),
            onClick = onDismiss,
        ),
        material = material,
        backdrop = backdrop,
    )
}

@Composable
fun CouiGithubUpdateChannelSelectionDialog(
    showState: MutableState<Boolean>,
    currentSelection: GithubUpdateChannel,
    onDismiss: () -> Unit,
    onConfirm: (GithubUpdateChannel) -> Unit,
) {
    val official = stringResource(R.string.lab_update_github_proxy_official)
    val proxy7ed = stringResource(R.string.lab_update_github_proxy_7ed)
    val custom = stringResource(R.string.lab_update_github_proxy_custom)
    val items = remember(official, proxy7ed, custom) {
        listOf(
            CouiSingleChoiceItem(GithubUpdateChannel.OFFICIAL, official),
            CouiSingleChoiceItem(GithubUpdateChannel.PROXY_7ED, proxy7ed),
            CouiSingleChoiceItem(GithubUpdateChannel.CUSTOM, custom),
        )
    }
    var selectedChannel by remember(currentSelection, showState.value) { mutableStateOf(currentSelection) }

    SingleChoiceDialog(
        visible = showState.value,
        title = stringResource(R.string.lab_update_github_proxy),
        items = items,
        selectedValue = selectedChannel,
        onItemSelected = { selectedChannel = it },
        onDismissRequest = onDismiss,
        actionRows = listOf(
            listOf(
                CouiDialogAction(text = stringResource(R.string.cancel), onClick = onDismiss),
                CouiDialogAction(
                    text = stringResource(R.string.confirm),
                    onClick = {
                        onConfirm(selectedChannel)
                        onDismiss()
                    },
                    style = CouiDialogActionStyle.Primary,
                ),
            ),
        ),
    )
}

@Composable
fun CouiCustomGithubProxyUrlDialog(
    showState: MutableState<Boolean>,
    initialUrl: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var url by remember { mutableStateOf(TextFieldValue(initialUrl, TextRange(initialUrl.length))) }
    val backdrop = rememberCouiWindowBackdrop()
    val material = rememberCouiDialogMaterial()

    LaunchedEffect(showState.value, initialUrl) {
        if (showState.value) {
            url = TextFieldValue(initialUrl, TextRange(initialUrl.length))
        }
    }

    InputDialog(
        visible = showState.value,
        value = url,
        onValueChange = { url = it },
        onDismissRequest = onDismiss,
        title = stringResource(R.string.lab_update_github_proxy_custom),
        placeholder = "GHProxy URL",
        clearContentDescription = stringResource(R.string.clear),
        confirmAction = CouiDialogAction(
            text = stringResource(R.string.confirm),
            onClick = {
                onConfirm(url.text)
                onDismiss()
            },
            enabled = url.text.isNotBlank(),
            style = CouiDialogActionStyle.Primary,
        ),
        dismissAction = CouiDialogAction(
            text = stringResource(R.string.cancel),
            onClick = onDismiss,
        ),
        material = material,
        backdrop = backdrop,
    )
}

@Composable
fun CouiCustomAuthorizerDialog(
    showState: MutableState<Boolean>,
    initialAuthorizer: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var authorizer by remember { mutableStateOf(TextFieldValue(initialAuthorizer, TextRange(initialAuthorizer.length))) }
    val backdrop = rememberCouiWindowBackdrop()
    val material = rememberCouiDialogMaterial()

    LaunchedEffect(showState.value, initialAuthorizer) {
        if (showState.value) {
            authorizer = TextFieldValue(initialAuthorizer, TextRange(initialAuthorizer.length))
        }
    }

    InputDialog(
        visible = showState.value,
        value = authorizer,
        onValueChange = { authorizer = it },
        onDismissRequest = onDismiss,
        title = stringResource(R.string.config_authorizer_customize),
        placeholder = stringResource(R.string.config_authorizer_customize),
        clearContentDescription = stringResource(R.string.clear),
        confirmAction = CouiDialogAction(
            text = stringResource(R.string.confirm),
            onClick = {
                onConfirm(authorizer.text)
                onDismiss()
            },
            enabled = authorizer.text.isNotBlank(),
            style = CouiDialogActionStyle.Primary,
        ),
        dismissAction = CouiDialogAction(
            text = stringResource(R.string.cancel),
            onClick = onDismiss,
        ),
        material = material,
        backdrop = backdrop,
    )
}

@Composable
fun CouiBlockingLoadingDialog(
    visible: Boolean,
    message: String = stringResource(R.string.loading),
) {
    val backdrop = rememberCouiWindowBackdrop()
    val material = rememberCouiDialogMaterial()
    LoadingDialog(
        visible = visible,
        message = message.ifBlank { "..." },
        material = material,
        backdrop = backdrop,
    )
}
