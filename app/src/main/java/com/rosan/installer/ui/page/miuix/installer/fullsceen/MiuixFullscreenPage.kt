package com.rosan.installer.ui.page.miuix.installer.fullsceen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.theme.InstallerMiuixTheme
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.basic.ArrowRight
import top.yukonga.miuix.kmp.icon.icons.useful.Back
import top.yukonga.miuix.kmp.icon.icons.useful.Settings

@Composable
fun MiuixFullscreenPage(/*installer: InstallerRepo*/) {
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = "",
                navigationIcon = {
                    MiuixBackButton(
                        modifier = Modifier.padding(start = 16.dp),
                        icon = MiuixIcons.Useful.Back,
                        onClick = { /*navController.navigateUp()*/ }
                    )
                },
                actions = {
                    IconButton(
                        modifier = Modifier.padding(end = 16.dp),
                        onClick = { /*viewModel.dispatch(EditViewAction.SaveData)*/ },
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Useful.Settings,
                            contentDescription = stringResource(R.string.save)
                        )
                    }
                }
            )
        },
        bottomBar = {
            // --- Bottom action buttons ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "Cancel Update" button
                Button(
                    onClick = {},/*onCancelClick*/
                    modifier = Modifier.weight(1f),
                    /*colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )*/
                ) {
                    Text("取消更新")
                }
                // "Continue Update" button
                Button(
                    onClick = {}/*onContinueClick*/,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("继续更新")
                }
            }
        }
    ) { innerPadding ->
        val extraFeaturesList = listOf<@Composable () -> Unit>({ PermissionItem() })
        // --- Main content area ---
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Slot 1: App Info (Icon, Name, Package) ---
            AppInfoSlot()

            Spacer(modifier = Modifier.height(24.dp))

            // --- Slot 2: Update Info Card ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                //shape = MaterialTheme.shapes.large
            ) {
                // The content of the card is provided by the slot
                UpdateInfoCardSlot()
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Slot 3: List of Extra Features ---
            // Using a simple Column as the list is expected to be short.
            // For a long or dynamic list, LazyColumn would be more appropriate.
            Column(modifier = Modifier.fillMaxWidth()) {
                extraFeaturesList.forEach { featureItem ->
                    featureItem()
                }
            }
        }
    }
}

// --- Example Usage and Preview ---

// Example implementation for the app info slot
@Composable
private fun AppInfoSlot() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Android,
            contentDescription = "App Icon",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text("R-安装组件", style = MaterialTheme.typography.titleLarge)
        Text("com.yxer.packageinstalles", style = MaterialTheme.typography.bodyMedium)
    }
}

// Example implementation for the card content slot
@Composable
private fun UpdateInfoCardSlot() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // A helper to create a row for information display
        @Composable
        fun InfoRow(label: String, value: String) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        InfoRow(label = "版本名称:", value = "2.6.10-beta")
        InfoRow(label = "版本代码:", value = "349")
        InfoRow(label = "支持SDK:", value = "28-30")
        InfoRow(label = "应用大小:", value = "2.63MB")
    }
}

// Example implementation for an item in the extra features list
@Composable
private fun PermissionItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "应用权限",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "22项权限 2项隐私相关",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        /*Spacer(modifier = Modifier.width(8.dp))*/
        Icon(
            imageVector = MiuixIcons.Basic.ArrowRight,
            contentDescription = "Navigate to permissions",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Preview(showBackground = true)
@Composable
fun AppUpdateScreenFramePreview() {
    InstallerMiuixTheme {
        MiuixFullscreenPage()
    }
}