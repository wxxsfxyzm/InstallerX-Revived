package com.rosan.installer.ui.page.miuix.installer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.miuix.installer.fullsceen.MiuixFullscreenPage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.extra.SuperBottomSheet

@Composable
fun MiuixInstallerPage(
    installer: InstallerRepo,
    onDismiss: () -> Unit = { installer.close() } // Add a dismiss callback parameter.
) {
    val scope = rememberCoroutineScope()
    val showBottomSheet = remember { mutableStateOf(true) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        content = {}
    )
    SuperBottomSheet(
        show = showBottomSheet, // Always true as long as this page is composed.
        onDismissRequest = {
            showBottomSheet.value = !showBottomSheet.value
            scope.launch {
                delay(100L) // Wait for 100 milliseconds (the 'L' is for Long)
                installer.close() // Execute this after the delay
            }
        } // When user taps outside or presses back, call the lambda.
    ) {
        MiuixFullscreenPage(installer = installer)
    }

    //
}