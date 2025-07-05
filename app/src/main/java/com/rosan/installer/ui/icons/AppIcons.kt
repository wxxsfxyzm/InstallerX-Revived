package com.rosan.installer.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowRight
import androidx.compose.material.icons.automirrored.twotone.MenuOpen
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.NotInterested
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.WebAsset
import androidx.compose.material.icons.twotone.ArrowDropDown
import androidx.compose.material.icons.twotone.ArrowUpward
import androidx.compose.material.icons.twotone.HourglassDisabled
import androidx.compose.material.icons.twotone.HourglassEmpty
import androidx.compose.material.icons.twotone.KeyboardArrowDown
import androidx.compose.material.icons.twotone.PermDeviceInformation

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
    val NotificationDisabled = Icons.Outlined.NotificationsOff
    val AutoNotification = Icons.Outlined.NotificationsActive
    val Ignore = Icons.Outlined.NotInterested

    /**
     * 箭头图标集合
     */
    // 实心三角小箭头
    val ArrowDropDownFilled = Icons.TwoTone.ArrowDropDown
    val ArrowRight = Icons.AutoMirrored.TwoTone.ArrowRight

    // DropDown用的箭头
    val ArrowDropDown = Icons.TwoTone.KeyboardArrowDown
    // Not needed since rotation animation is handled in the UI
    // val ArrowDropUp = Icons.TwoTone.KeyboardArrowUp

    val ArrowUp = Icons.TwoTone.ArrowUpward

    /**
     * 沙漏图标集合
     */
    val Pausing = Icons.TwoTone.HourglassDisabled
    val Working = Icons.TwoTone.HourglassEmpty

    /**
     * 菜单图标
     */
    val MenuOpen = Icons.AutoMirrored.TwoTone.MenuOpen

    /**
     * 权限管理
     */
    val Permission = Icons.TwoTone.PermDeviceInformation
}