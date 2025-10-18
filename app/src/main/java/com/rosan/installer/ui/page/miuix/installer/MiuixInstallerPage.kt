package com.rosan.installer.ui.page.miuix.installer

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.main.installer.dialog.DialogViewAction
import com.rosan.installer.ui.page.main.installer.dialog.DialogViewModel
import com.rosan.installer.ui.page.miuix.installer.sheet.MiuixSheetContent
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.extra.patched.SuperBottomSheet
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.Cancel
import top.yukonga.miuix.kmp.icon.icons.useful.Settings

@Composable
fun MiuixInstallerPage(
    installer: InstallerRepo,
    onDismiss: () -> Unit = { installer.close() }
) {
    val scope = rememberCoroutineScope()
    val showBottomSheet = remember { mutableStateOf(true) }

    val viewModel: DialogViewModel = koinViewModel { parametersOf(installer) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        content = {}
    )
    SuperBottomSheet(
        show = showBottomSheet, // Always true as long as this page is composed.
        backgroundColor = if (isSystemInDarkTheme()) Color(0xFF242424) else Color(0xFFF7F7F7),
        leftAction = {
            MiuixBackButton(
                // modifier = Modifier.padding(start = 16.dp),
                icon = MiuixIcons.Useful.Cancel,
                onClick = { }
            )
        },
        rightAction = {
            IconButton(
                onClick = {},
                modifier = Modifier
            ) {
                Icon(
                    imageVector = MiuixIcons.Useful.Settings,
                    contentDescription = ""
                )
            }
        },
        title = "安装应用",
        insideMargin = DpSize(16.dp, 16.dp),
        onDismissRequest = {
            showBottomSheet.value = !showBottomSheet.value
            scope.launch {
                delay(100L) // Wait for sheet animation
                if (viewModel.isDismissible) {
                    if (viewModel.disableNotificationOnDismiss) {
                        viewModel.dispatch(DialogViewAction.Close)
                    } else {
                        viewModel.dispatch(DialogViewAction.Background)
                    }
                }
            }
        }
    ) {
        MiuixSheetContent(installer = installer, viewModel = viewModel)
    }
}