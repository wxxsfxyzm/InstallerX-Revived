package com.rosan.installer.ui.page.miuix.installer.sheet

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.rosan.installer.R
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.recycle.util.openAppPrivileged
import com.rosan.installer.ui.page.main.installer.dialog.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.dialog.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.dialog.InstallerViewState
import com.rosan.installer.ui.page.miuix.widgets.MiuixErrorTextBlock
import com.rosan.installer.ui.page.miuix.widgets.MiuixSwitchWidget
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.BackHandler

@Composable
fun MiuixSheetContent(
    installer: InstallerRepo,
    viewModel: InstallerViewModel = koinViewModel {
        parametersOf(installer)
    }
) {
    LaunchedEffect(installer.id) {
        viewModel.dispatch(InstallerViewAction.CollectRepo(installer))
    }

    val showSettings = viewModel.showMiuixSheetRightActionSettings
    val currentPackageName by viewModel.currentPackageName.collectAsState()
    val packageName = currentPackageName ?: installer.analysisResults.firstOrNull()?.packageName ?: ""
    val displayIcons by viewModel.displayIcons.collectAsState()
    val installProgress by viewModel.installProgress.collectAsState()
    val installProgressText by viewModel.installProgressText.collectAsState()

    val analysisResult =
        if (currentPackageName != null) installer.analysisResults.find { it.packageName == currentPackageName } else null
    val baseEntity = analysisResult?.appEntities?.map { it.app }?.filterIsInstance<AppEntity.BaseEntity>()?.firstOrNull()
    val appIcon = if (currentPackageName != null) displayIcons[currentPackageName] else null

    BackHandler(
        enabled = showSettings,
        onBack = {
            viewModel.dispatch(InstallerViewAction.HideMiuixSheetRightActionSettings)
        }
    )

    AnimatedContent(
        targetState = viewModel.state,
        label = "MiuixSheetContentAnimation",
        transitionSpec = {
            // Define the animation.
            // fadeIn/fadeOut controls how the content itself appears/disappears.
            // SizeTransform handles the container size animation.
            fadeIn() togetherWith fadeOut() using SizeTransform(
                // Set clip to false to prevent content from being cut off during size change.
                clip = false
            )
        }
    ) { targetState ->
        when (targetState) {
            is InstallerViewState.InstallPrepare -> {
                AnimatedContent(
                    targetState = showSettings,
                    label = "PrepareContentVsSettings"
                ) { isShowingSettings ->
                    if (isShowingSettings) {
                        // 显示设置内容
                        PrepareSettingsContent(installer, viewModel)
                    } else {
                        // 显示原来的准备内容
                        InstallPrepareContent(
                            baseEntity = baseEntity,
                            appIcon = appIcon,
                            installer = installer,
                            onCancel = { viewModel.dispatch(InstallerViewAction.Close) },
                            onInstall = { viewModel.dispatch(InstallerViewAction.Install) }
                        )
                    }
                }
            }

            is InstallerViewState.Installing -> {
                // Show a progress indicator during installation.
                InstallingContent(
                    baseEntity = baseEntity,
                    appIcon = appIcon,
                    //progress = installProgress,
                    progressText = installProgressText?.toString()
                )
            }

            is InstallerViewState.InstallSuccess -> {
                InstallSuccessContent(
                    baseEntity = baseEntity,
                    installer = installer,
                    packageName = packageName,
                    appIcon = appIcon,
                    dhizukuAutoClose = viewModel.autoCloseCountDown,
                    onClose = { viewModel.dispatch(InstallerViewAction.Close) }
                )
            }

            is InstallerViewState.InstallFailed -> {
                InstallFailureContent(
                    baseEntity = baseEntity,
                    appIcon = appIcon,
                    error = installer.error,
                    onClose = { viewModel.dispatch(InstallerViewAction.Close) }
                )
            }

            is InstallerViewState.AnalyseFailed, is InstallerViewState.ResolveFailed -> {
                NonInstallFailureContent(
                    error = installer.error,
                    onClose = { viewModel.dispatch(InstallerViewAction.Close) }
                )
            }

            is InstallerViewState.Resolving, is InstallerViewState.Analysing -> {
                LoadingContent(
                    statusText = if (targetState is InstallerViewState.Resolving) stringResource(R.string.installer_resolving)
                    else stringResource(R.string.installer_analysing)
                )
            }

            else -> {
                LoadingContent(statusText = stringResource(R.string.loading))
            }
        }
    }
}

@Composable
private fun InstallPrepareContent(
    baseEntity: AppEntity.BaseEntity?,
    appIcon: Drawable?,
    installer: InstallerRepo,
    onCancel: () -> Unit,
    onInstall: () -> Unit
) {
    val cardColor = if (isSystemInDarkTheme()) Color(0xFF434343) else Color(0xFFFFFFFF)

    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                AppInfoSlot(
                    icon = appIcon,
                    label = baseEntity?.label ?: "Unknown App",
                    packageName = baseEntity?.packageName ?: "unknown.package"
                )
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardColors(
                    color = cardColor,
                    contentColor = MiuixTheme.colorScheme.onSurface
                )
            ) {
                UpdateInfoCardSlot(
                    versionName = baseEntity?.versionName ?: "N/A",
                    versionCode = baseEntity?.versionCode?.toString() ?: "N/A",
                    displaySdk = installer.config.displaySdk,
                    sdkVersion = "${baseEntity?.minSdk ?: "N/A"} - ${baseEntity?.targetSdk ?: "N/A"}"
                )
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
        item {
            ActionButtons(
                cancelText = stringResource(R.string.cancel),
                confirmText = stringResource(R.string.install),
                onCancel = onCancel,
                onConfirm = onInstall
            )
        }
    }
}

@Composable
private fun PrepareSettingsContent(installer: InstallerRepo, viewModel: InstallerViewModel) {
    var autoDelete by remember { mutableStateOf(installer.config.autoDelete) }
    var displaySdk by remember { mutableStateOf(installer.config.displaySdk) }

    LaunchedEffect(autoDelete, displaySdk) {
        val currentConfig = installer.config
        if (currentConfig.autoDelete != autoDelete) installer.config.autoDelete = autoDelete
        if (currentConfig.displaySdk != displaySdk) installer.config.displaySdk = displaySdk
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            MiuixSwitchWidget(
                title = stringResource(R.string.config_display_sdk_version),
                description = stringResource(R.string.config_display_sdk_version_desc),
                checked = displaySdk,
                onCheckedChange = {
                    val newValue = !displaySdk
                    displaySdk = newValue
                    installer.config.displaySdk = newValue
                }
            )
            MiuixSwitchWidget(
                title = stringResource(R.string.config_auto_delete),
                description = stringResource(R.string.config_auto_delete_desc),
                checked = installer.config.autoDelete,
                onCheckedChange = {
                    val newValue = !autoDelete
                    autoDelete = newValue
                    installer.config.autoDelete = newValue
                }
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun InstallingContent(
    baseEntity: AppEntity.BaseEntity?,
    appIcon: Drawable?,
    progressText: String?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppInfoSlot(
            icon = appIcon,
            label = baseEntity?.label ?: "Unknown App",
            packageName = baseEntity?.packageName ?: "unknown.package"
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            enabled = false,
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
        ) {
            Row {
                InfiniteProgressIndicator()
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = progressText ?: stringResource(R.string.installer_installing)
                )
            }
        }
    }
}

@Composable
private fun NonInstallFailureContent(
    error: Throwable,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MiuixErrorTextBlock(
            error = error,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
        ) {
            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.close))
            }
        }
    }
}

@Composable
private fun InstallSuccessContent(
    baseEntity: AppEntity.BaseEntity?,
    installer: InstallerRepo,
    appIcon: Drawable?,
    packageName: String,
    dhizukuAutoClose: Int,
    onClose: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppInfoSlot(
            icon = appIcon,
            label = baseEntity?.label ?: "Unknown App",
            packageName = baseEntity?.packageName ?: "unknown.package"
        )
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.installer_install_success),
            style = MiuixTheme.textStyles.headline2,
            fontWeight = FontWeight.Bold,
            color = MiuixTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val context = LocalContext.current
            val intent =
                if (packageName.isNotEmpty()) context.packageManager.getLaunchIntentForPackage(
                    packageName
                ) else null
            TextButton(
                text = stringResource(R.string.finish),
                modifier = Modifier.weight(1f),
                onClick = onClose,
            )
            if (intent != null)
                TextButton(
                    text = stringResource(R.string.open),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = {
                        coroutineScope.launch {
                            openAppPrivileged(
                                context = context,
                                config = installer.config,
                                packageName = packageName,
                                dhizukuAutoCloseSeconds = dhizukuAutoClose,
                                onSuccess = onClose
                            )
                        }
                    }
                )
        }
    }
}

@Composable
private fun InstallFailureContent(
    baseEntity: AppEntity.BaseEntity?,
    appIcon: Drawable?,
    error: Throwable,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppInfoSlot(
            icon = appIcon,
            label = baseEntity?.label ?: "Unknown App",
            packageName = baseEntity?.packageName ?: "unknown.package"
        )
        Spacer(modifier = Modifier.height(32.dp))
        MiuixErrorTextBlock(
            error = error,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(/*horizontal = 16.dp, */vertical = 24.dp),
        ) {
            TextButton(
                onClick = onClose,
                text = stringResource(R.string.close),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LoadingContent(statusText: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.5f),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            InfiniteProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(statusText, style = MiuixTheme.textStyles.body1)
        }
    }
}


@Composable
private fun ActionButtons(
    cancelText: String,
    confirmText: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onCancel,
            text = cancelText,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = onConfirm,
            text = confirmText,
            colors = ButtonDefaults.textButtonColorsPrimary(),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AppInfoSlot(
    icon: Drawable?,
    label: String,
    packageName: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            painter = rememberDrawablePainter(drawable = icon),
            contentDescription = "App Icon",
            modifier = Modifier.size(72.dp)
        )
        Text(label, style = MiuixTheme.textStyles.title2)
        Text(packageName, style = MiuixTheme.textStyles.subtitle)
    }
}

@Composable
private fun UpdateInfoCardSlot(
    versionName: String,
    versionCode: String,
    displaySdk: Boolean,
    sdkVersion: String
) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {

        @Composable
        fun InfoRow(label: String, value: String) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(label, style = MiuixTheme.textStyles.body2, fontWeight = FontWeight.SemiBold)
                Text(value, style = MiuixTheme.textStyles.body2)
            }
        }

        InfoRow(label = "版本名称:", value = versionName)
        InfoRow(label = "版本代码:", value = versionCode)
        if (displaySdk)
            InfoRow(label = "支持SDK:", value = sdkVersion)
    }
}