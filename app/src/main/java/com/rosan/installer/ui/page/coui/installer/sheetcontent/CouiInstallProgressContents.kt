// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer.sheetcontent

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.shape.CouiShape
import com.itos.coui.theme.CouiTheme
import com.rosan.installer.R
import com.rosan.installer.domain.engine.model.install.InstallPhase
import com.rosan.installer.domain.session.model.ConfirmationRequestType
import com.rosan.installer.ui.page.coui.installer.components.CouiAppInfoSlot
import com.rosan.installer.ui.page.coui.installer.components.CouiAppInfoState
import com.rosan.installer.ui.page.coui.widgets.CouiActionButton
import com.rosan.installer.ui.page.coui.widgets.CouiCard
import com.rosan.installer.ui.page.coui.widgets.CouiInstallerTipCard
import com.rosan.installer.ui.page.coui.widgets.CouiProgressButton
import com.rosan.installer.ui.page.main.installer.InstallerStage
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.util.KeyEventBlocker

@Composable
fun CouiLoadingContent(statusText: String) {
    val scheme = CouiTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(34.dp),
                strokeWidth = 3.dp,
                color = scheme.controlAccent,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = statusText,
                color = scheme.onSurface,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
fun CouiInstallPreparingContent(
    viewModel: InstallerViewModel,
    onBackground: () -> Unit,
    @StringRes descriptionRes: Int = R.string.installer_preparing_desc,
    descriptionText: String? = null,
    @StringRes buttonTextRes: Int = R.string.loading,
    showButton: Boolean = true,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stage = uiState.stage
    val progress = if (stage is InstallerStage.Preparing) stage.progress else -1f
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceAtLeast(0f),
        animationSpec = tween(durationMillis = 300),
        label = "CouiPreparingProgress",
    )
    val scheme = CouiTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(36.dp))
        CircularProgressIndicator(
            modifier = Modifier.size(36.dp),
            strokeWidth = 3.dp,
            color = scheme.controlAccent,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = descriptionText ?: stringResource(descriptionRes),
            color = scheme.onSurface,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(48.dp))
        if (showButton) {
            CouiProgressButton(
                progress = animatedProgress,
                onClick = onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            ) {
                Text(
                    text = stringResource(buttonTextRes),
                    color = LocalContentColor.current,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
fun CouiInstallingContent(
    stage: InstallerStage.Installing,
    appInfo: CouiAppInfoState,
    onButtonClick: () -> Unit,
) {
    val scheme = CouiTheme.colorScheme
    val displayLabel = stage.appLabel ?: appInfo.label
    val progressText = if (stage.total > 1) {
        stringResource(R.string.installing_progress_text, displayLabel, stage.current, stage.total)
    } else {
        null
    }
    val animatedProgress by animateFloatAsState(
        targetValue = stage.progress,
        animationSpec = tween(durationMillis = 300),
        label = "CouiInstallingProgress",
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CouiAppInfoSlot(appInfo = appInfo)
        Spacer(modifier = Modifier.height(28.dp))

        progressText?.let {
            Text(
                text = it,
                fontSize = 13.sp,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
            )
        }

        CouiProgressButton(
            progress = animatedProgress,
            onClick = onButtonClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 12.dp),
        ) {
            val contentColor = LocalContentColor.current
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = contentColor,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                color = contentColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                text = stringResource(
                    if (stage.total == 1 && stage.phase == InstallPhase.WRITING) {
                        R.string.installer_writing
                    } else {
                        R.string.installer_installing
                    },
                ),
            )
        }
    }
}

@Composable
fun CouiInstallModuleContent(
    outputLines: List<String>,
    isFinished: Boolean,
    onClose: () -> Unit,
) {
    KeyEventBlocker { it.key == Key.VolumeDown || it.key == Key.VolumeUp }
    val lazyListState = rememberLazyListState()
    val scheme = CouiTheme.colorScheme

    LaunchedEffect(outputLines.size, isFinished) {
        if (outputLines.isNotEmpty()) {
            if (!isFinished) {
                lazyListState.animateScrollToItem(index = outputLines.size - 1)
            } else {
                lazyListState.scrollToItem(index = outputLines.size - 1)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CouiCard(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 280.dp)
                .weight(1f, fill = false),
        ) {
            LazyColumn(
                state = lazyListState,
                overscrollEffect = rememberCouiOverscrollEffect(),
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
            ) {
                items(outputLines) { line ->
                    Text(
                        text = line,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = if (line.startsWith("ERROR:")) Color(0xFFDB382C) else scheme.onSurface,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (isFinished) {
            CouiActionButton(
                text = stringResource(R.string.close),
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            )
        } else {
            CouiProgressButton(
                progress = 0f,
                onClick = {},
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            ) {
                val contentColor = LocalContentColor.current
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = contentColor,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.installer_installing),
                    color = contentColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
fun CouiInstallConfirmContent(
    viewModel: InstallerViewModel,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sessionInfo = uiState.stage as? InstallerStage.InstallConfirm ?: return
    val scheme = CouiTheme.colorScheme

    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        overscrollEffect = rememberCouiOverscrollEffect(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 12.dp, bottom = 20.dp),
            ) {
                if (sessionInfo.appIcon != null) {
                    Image(
                        bitmap = sessionInfo.appIcon.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CouiShape(20.dp, 3f)),
                    )
                } else {
                    Spacer(modifier = Modifier.size(84.dp))
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = sessionInfo.appLabel.toString(),
                    fontSize = 18.sp,
                    color = scheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        item {
            val tipMessage = when {
                sessionInfo.requestType == ConfirmationRequestType.PRE_APPROVAL -> {
                    val initiator = sessionInfo.sourceAppLabel ?: stringResource(R.string.installer_label_unknown)
                    stringResource(R.string.install_confirm_pre_approval_tip, initiator)
                }
                sessionInfo.requestType == ConfirmationRequestType.PERMISSIONS -> {
                    val initiator = sessionInfo.sourceAppLabel ?: stringResource(R.string.installer_label_unknown)
                    stringResource(R.string.install_confirm_permissions_tip, initiator)
                }
                sessionInfo.isOwnershipConflict -> {
                    val owner = sessionInfo.sourceAppLabel ?: stringResource(R.string.installer_label_unknown)
                    stringResource(R.string.install_confirm_question_update_owner_reminder, owner)
                }
                !sessionInfo.isSelfSession -> {
                    val initiator = sessionInfo.sourceAppLabel ?: stringResource(R.string.installer_label_unknown)
                    stringResource(R.string.install_confirm_external_request_tip, initiator)
                }
                else -> stringResource(R.string.installer_prepare_type_unknown_confirm)
            }
            CouiInstallerTipCard(tipMessage)
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CouiActionButton(
                    text = stringResource(R.string.cancel),
                    onClick = onCancel,
                    enabled = !uiState.isConfirmationSubmitting,
                    modifier = Modifier.weight(1f),
                )
                val confirmText = when {
                    sessionInfo.isOwnershipConflict -> stringResource(R.string.install_anyway)
                    sessionInfo.requestType == ConfirmationRequestType.PRE_APPROVAL -> stringResource(R.string.pre_approve)
                    else -> stringResource(R.string.confirm)
                }
                CouiActionButton(
                    text = confirmText,
                    onClick = onConfirm,
                    enabled = !uiState.isConfirmationSubmitting,
                    primary = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
