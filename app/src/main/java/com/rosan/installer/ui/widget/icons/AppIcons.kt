package com.rosan.installer.ui.widget.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tag

/**
 * 将图标统一管理在一个对象中
 */
object AppIcons {
    // 这里使用 Material Icons 作为示例
    // 你需要添加 'androidx.compose.material:material-icons-extended' 依赖来使用所有图标
    // 或者替换成你自己的图标资源
    val Android = Icons.Default.Android
    val None = Icons.Outlined.DoNotDisturbOn
    val Root = Icons.Outlined.Tag

    // TODO collect icons of Shizuku and Dhizuku
    val Shizuku = null // Shizuku 图标的示意
    val Dhizuku = null // Dhizuku 图标的示意
    val Customize = Icons.Outlined.Settings
}