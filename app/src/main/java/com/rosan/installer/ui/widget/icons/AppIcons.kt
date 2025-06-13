package com.rosan.installer.ui.widget.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.NotInterested
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.WebAsset

/**
 * 将图标统一管理在一个对象中
 */
object AppIcons {
    // 这里使用 Material Icons 作为示例
    // 你需要添加 'androidx.compose.material:material-icons-extended' 依赖来使用所有图标
    // 或者替换成你自己的图标资源
    val Android = Icons.Default.Android

    /**
     * 授权器图标集合
     */
    val None = Icons.Outlined.DoNotDisturbOn
    val Root = Icons.Outlined.Tag

    // TODO collect icons of Shizuku and Dhizuku
    val Shizuku = null // Shizuku 图标的示意
    val Dhizuku = null // Dhizuku 图标的示意
    val Customize = Icons.Outlined.Settings

    /**
     * 安装方式图标集合
     */
    val Dialog = Icons.Outlined.WebAsset
    val AutoDialog = Icons.Outlined.SmartDisplay
    val Notification = Icons.Outlined.Notifications
    val AutoNotification = Icons.Outlined.NotificationsActive
    val Ignore = Icons.Outlined.NotInterested
}