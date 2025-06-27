package com.rosan.installer.ui.icons

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rosan.installer.ui.theme.InstallerTheme

@Composable
fun IconPreview(icon: ImageVector, name: String) {
    Column {
        Icon(
            imageVector = icon,
            contentDescription = name,
            modifier = Modifier.size(48.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAllIcons() {
    InstallerTheme {
        Column {
            IconPreview(AppIcons.Android, "Android")
            IconPreview(AppIcons.None, "None")
            IconPreview(AppIcons.Root, "Root")
            IconPreview(AppIcons.Customize, "Customize")
            IconPreview(AppIcons.Dialog, "Dialog")
            IconPreview(AppIcons.AutoDialog, "AutoDialog")
            IconPreview(AppIcons.Notification, "Notification")
            IconPreview(AppIcons.AutoNotification, "AutoNotification")
            IconPreview(AppIcons.Ignore, "Ignore")
            IconPreview(AppIcons.ArrowDropDownFilled, "ArrowDropDownFilled")
            IconPreview(AppIcons.ArrowRight, "ArrowRight")
            IconPreview(AppIcons.ArrowDropDown, "ArrowDropDown")
            IconPreview(AppIcons.Pausing, "Pause")
            IconPreview(AppIcons.Working, "Working")
            IconPreview(AppIcons.MenuOpen, "MenuOpen")
            // 添加所有其他图标的预览
        }
    }
}