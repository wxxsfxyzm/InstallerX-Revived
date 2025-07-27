package com.rosan.installer.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material.icons.automirrored.twotone.ArrowRight
import androidx.compose.material.icons.automirrored.twotone.MenuOpen
import androidx.compose.material.icons.automirrored.twotone.Rule
import androidx.compose.material.icons.automirrored.twotone.TrendingDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.NotInterested
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.WebAsset
import androidx.compose.material.icons.twotone.Add
import androidx.compose.material.icons.twotone.AdminPanelSettings
import androidx.compose.material.icons.twotone.Android
import androidx.compose.material.icons.twotone.ArrowBackIosNew
import androidx.compose.material.icons.twotone.ArrowDropDown
import androidx.compose.material.icons.twotone.ArrowUpward
import androidx.compose.material.icons.twotone.AutoFixHigh
import androidx.compose.material.icons.twotone.Badge
import androidx.compose.material.icons.twotone.BugReport
import androidx.compose.material.icons.twotone.ClearAll
import androidx.compose.material.icons.twotone.Close
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.DeveloperMode
import androidx.compose.material.icons.twotone.Downloading
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.material.icons.twotone.Face
import androidx.compose.material.icons.twotone.Favorite
import androidx.compose.material.icons.twotone.FavoriteBorder
import androidx.compose.material.icons.twotone.History
import androidx.compose.material.icons.twotone.HourglassDisabled
import androidx.compose.material.icons.twotone.HourglassEmpty
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material.icons.twotone.KeyboardArrowDown
import androidx.compose.material.icons.twotone.LibraryAddCheck
import androidx.compose.material.icons.twotone.Memory
import androidx.compose.material.icons.twotone.People
import androidx.compose.material.icons.twotone.PermDeviceInformation
import androidx.compose.material.icons.twotone.RoomPreferences
import androidx.compose.material.icons.twotone.Save
import androidx.compose.material.icons.twotone.Search
import androidx.compose.material.icons.twotone.SecurityUpdateWarning
import androidx.compose.material.icons.twotone.SettingsSuggest
import androidx.compose.material.icons.twotone.SystemUpdate
import androidx.compose.material.icons.twotone.Terminal
import androidx.compose.material.icons.twotone.TipsAndUpdates

/**
 * 将图标统一管理在一个对象中
 */
object AppIcons {
    // 这里使用 Material Icons 作为示例
    // 你需要添加 'androidx.compose.material:material-icons-extended' 依赖来使用所有图标
    // 或者替换成你自己的图标资源
    val Android = Icons.TwoTone.Android
    val Info = Icons.TwoTone.Info
    val Add = Icons.TwoTone.Add
    val Edit = Icons.TwoTone.Edit
    val Delete = Icons.TwoTone.Delete
    val Save = Icons.TwoTone.Save
    val BugReport = Icons.TwoTone.BugReport
    val Terminal = Icons.TwoTone.Terminal
    val History = Icons.TwoTone.History
    val Suggestion = Icons.TwoTone.TipsAndUpdates
    val Update = Icons.TwoTone.SystemUpdate
    val Rule = Icons.AutoMirrored.TwoTone.Rule
    val Search = Icons.TwoTone.Search
    val Close = Icons.TwoTone.Close
    val AutoFixHigh = Icons.TwoTone.AutoFixHigh
    val Developer = Icons.TwoTone.DeveloperMode

    // --- 导航图标集合 ---
    val RoomPreferences = Icons.TwoTone.RoomPreferences
    val SettingsSuggest = Icons.TwoTone.SettingsSuggest
    val ArrowBack = Icons.AutoMirrored.TwoTone.ArrowBack
    val ArrowBack1 = Icons.TwoTone.ArrowBackIosNew
    // --- 导航图标结束 ---

    // --- 授权器图标集合 ---
    val None = Icons.Outlined.DoNotDisturbOn
    val Root = Icons.Outlined.Tag
    val Shizuku = null // Shizuku 图标
    val Dhizuku = null // Dhizuku 图标
    val Customize = Icons.Outlined.Settings
    // --- 授权器图标结束 ---

    // --- 安装方式图标集合 ---
    val Dialog = Icons.Outlined.WebAsset
    val AutoDialog = Icons.Outlined.SmartDisplay
    val Notification = Icons.Outlined.Notifications
    val NotificationDisabled = Icons.Outlined.NotificationsOff
    val AutoNotification = Icons.Outlined.NotificationsActive
    val Ignore = Icons.Outlined.NotInterested
    // --- 安装方式图标结束 ---

    // --- 设置图标集合 ---
    val Authorizer = Icons.TwoTone.Memory
    val InstallMode = Icons.TwoTone.Downloading
    val LockDefault = Icons.TwoTone.Favorite
    val UnlockDefault = Icons.TwoTone.FavoriteBorder
    val ClearAll = Icons.TwoTone.ClearAll
    // --- 设置图标结束 ---

    // --- Profile item 图标集合 ---
    val InstallSource = Icons.TwoTone.Face
    val InstallSourceInput = Icons.TwoTone.Badge
    val InstallForAllUsers = Icons.TwoTone.People
    val InstallAllowDowngrade = Icons.AutoMirrored.TwoTone.TrendingDown
    val InstallBypassLowTargetSdk = Icons.TwoTone.SecurityUpdateWarning
    val InstallAllowRestrictedPermissions = Icons.TwoTone.AdminPanelSettings
    val InstallAllowAllRequestedPermissions = Icons.TwoTone.LibraryAddCheck
    // --- 结束 ---

    // --- 箭头图标集合 ---
    // 实心三角小箭头
    val ArrowDropDownFilled = Icons.TwoTone.ArrowDropDown
    val ArrowRight = Icons.AutoMirrored.TwoTone.ArrowRight

    // DropDown用的箭头
    val ArrowDropDown = Icons.TwoTone.KeyboardArrowDown
    // Not needed since rotation animation is handled in the UI
    // val ArrowDropUp = Icons.TwoTone.KeyboardArrowUp

    val ArrowUp = Icons.TwoTone.ArrowUpward
    // --- 箭头结束 ---

    // --- 沙漏图标集合 ---
    val Pausing = Icons.TwoTone.HourglassDisabled
    val Working = Icons.TwoTone.HourglassEmpty
    // --- 沙漏图标结束 ---

    // --- 菜单图标集合 ---
    val Menu = Icons.Filled.Menu
    val MenuOpen = Icons.AutoMirrored.TwoTone.MenuOpen
    // --- 菜单图标结束 ---

    // --- 权限图标集合 ---
    val Permission = Icons.TwoTone.PermDeviceInformation
    // --- 权限图标结束 ---
}