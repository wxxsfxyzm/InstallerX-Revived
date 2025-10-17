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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.main.installer.dialog.DialogViewAction
import com.rosan.installer.ui.page.main.installer.dialog.DialogViewModel
import com.rosan.installer.ui.page.main.installer.dialog.DialogViewState
import com.rosan.installer.ui.page.miuix.widgets.MiuixErrorTextBlock
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MiuixSheetContent(
    installer: InstallerRepo,
    viewModel: DialogViewModel = koinViewModel {
        parametersOf(installer)
    } // ViewModel is now a parameter
) {
    LaunchedEffect(installer.id) {
        viewModel.dispatch(DialogViewAction.CollectRepo(installer))
    }

    // Collect state from the ViewModel. The UI will automatically recompose when the state changes.
    val currentPackageName by viewModel.currentPackageName.collectAsState()
    val displayIcons by viewModel.displayIcons.collectAsState()
    val installProgress by viewModel.installProgress.collectAsState()
    val installProgressText by viewModel.installProgressText.collectAsState()

    // Find the relevant analysis result for the current package
    val analysisResult =
        if (currentPackageName != null) installer.analysisResults.find { it.packageName == currentPackageName } else null
    val baseEntity = analysisResult?.appEntities?.map { it.app }?.filterIsInstance<AppEntity.BaseEntity>()?.firstOrNull()
    val appIcon = if (currentPackageName != null) displayIcons[currentPackageName] else null

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
            //is DialogViewState.
            is DialogViewState.InstallPrepare -> {
                // This is the main UI for showing app details before installation.
                InstallPrepareContent(
                    baseEntity = baseEntity,
                    appIcon = appIcon,
                    onCancel = { viewModel.dispatch(DialogViewAction.Close) },
                    onInstall = { viewModel.dispatch(DialogViewAction.Install) }
                )
            }

            is DialogViewState.Installing -> {
                // Show a progress indicator during installation.
                InstallingContent(
                    baseEntity = baseEntity,
                    appIcon = appIcon,
                    //progress = installProgress,
                    progressText = installProgressText?.toString() // Assuming you have a way to resolve UiText
                )
            }

            is DialogViewState.InstallSuccess -> {
                // Show a success message.
                InstallResultContent(
                    baseEntity = baseEntity,
                    appIcon = appIcon,
                    error = installer.error,
                    onClose = { viewModel.dispatch(DialogViewAction.Close) }
                )
            }

            is DialogViewState.InstallFailed -> {
                // Show a failure message.
                InstallResultContent(
                    baseEntity = baseEntity,
                    appIcon = appIcon,
                    error = installer.error,
                    onClose = { viewModel.dispatch(DialogViewAction.Close) }
                )
            }

            is DialogViewState.AnalyseFailed, DialogViewState.ResolveFailed -> {

            }
            // You can add more states here as needed (Resolving, Analysing, etc.)
            is DialogViewState.Resolving, is DialogViewState.Analysing -> {
                LoadingContent(statusText = if (targetState is DialogViewState.Resolving) "解析中..." else "分析中...")
            }

            else -> {
                // A default state, perhaps a loading indicator or an empty view.
                LoadingContent(statusText = "请稍候...")
            }
        }
    }
}

@Composable
private fun InstallPrepareContent(
    baseEntity: AppEntity.BaseEntity?,
    appIcon: Drawable?,
    onCancel: () -> Unit,
    onInstall: () -> Unit
) {
    val cardColor = if (isSystemInDarkTheme()) Color(0xFF434343) else Color(0xFFFFFFFF)

    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            /*Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                colors = CardColors(
                    color = cardColor,
                    contentColor = MiuixTheme.colorScheme.onSurface
                )
            ) {*/
            // Use a Box to add internal padding and center the content.
            Box(
                modifier = Modifier
                    .fillMaxWidth() // Ensure the Box fills the Card's width
                    .padding(vertical = 16.dp), // Add vertical padding as requested
                contentAlignment = Alignment.Center // Center the AppInfoSlot
            ) {
                AppInfoSlot(
                    icon = appIcon,
                    label = baseEntity?.label ?: "Unknown App",
                    packageName = baseEntity?.packageName ?: "unknown.package"
                )
            }
            //}
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp)
                    .padding(bottom = 6.dp),
                colors = CardColors(
                    color = cardColor,
                    contentColor = MiuixTheme.colorScheme.onSurface
                )
            ) {
                UpdateInfoCardSlot(
                    versionName = baseEntity?.versionName ?: "N/A",
                    versionCode = baseEntity?.versionCode?.toString() ?: "N/A",
                    sdkVersion = "${baseEntity?.minSdk ?: "N/A"}-${baseEntity?.targetSdk ?: "N/A"}",
                    size = ""
                )
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
        item {
            ActionButtons(
                cancelText = "取消更新",
                confirmText = "继续更新",
                onCancel = onCancel,
                onConfirm = onInstall
            )
        }
    }

}

@Composable
private fun InstallingContent(
    baseEntity: AppEntity.BaseEntity?,
    appIcon: Drawable?,
    progressText: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppInfoSlot(
            icon = appIcon,
            label = baseEntity?.label ?: "Unknown App",
            packageName = baseEntity?.packageName ?: "unknown.package"
        )
        Spacer(modifier = Modifier.height(32.dp))
        InfiniteProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = progressText ?: "正在安装...",
            style = MaterialTheme.typography.bodyLarge
        )
        // You can also use the `progress` float to drive a determinate progress bar
    }
}

@Composable
private fun NonInstallResultContent(
    message: String,
) {

}

@Composable
private fun InstallResultContent(
    baseEntity: AppEntity.BaseEntity?,
    appIcon: Drawable?,
    error: Throwable,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
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
            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("关闭")
            }
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
            Text(statusText, style = MaterialTheme.typography.bodyLarge)
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
            .padding(horizontal = 0.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onCancel,
            modifier = Modifier.weight(1f),
        ) {
            Text(cancelText)
        }
        Button(
            onClick = onConfirm,
            modifier = Modifier.weight(1f)
        ) {
            Text(confirmText)
        }
    }
}

@Composable
private fun AppInfoSlot(icon: Drawable?, label: String, packageName: String) {
    // Wrap in a Column to arrange the items vertically and center them horizontally.
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp) // Adds space between items
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
private fun UpdateInfoCardSlot(versionName: String, versionCode: String, sdkVersion: String, size: String) {
    @Composable
    fun InfoRow(label: String, value: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MiuixTheme.textStyles.body2)
            Text(value, style = MiuixTheme.textStyles.body2, fontWeight = FontWeight.SemiBold)
        }
    }
    InfoRow(label = "版本名称:", value = versionName)
    InfoRow(label = "版本代码:", value = versionCode)
    InfoRow(label = "支持SDK:", value = sdkVersion)
}