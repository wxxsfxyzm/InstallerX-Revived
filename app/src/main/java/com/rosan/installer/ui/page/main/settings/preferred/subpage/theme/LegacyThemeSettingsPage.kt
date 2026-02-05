package com.rosan.installer.ui.page.main.settings.preferred.subpage.theme

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.twotone.Colorize
import androidx.compose.material.icons.twotone.InvertColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewAction
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.page.main.widget.card.ColorSwatchPreview
import com.rosan.installer.ui.page.main.widget.dialog.HideLauncherIconWarningDialog
import com.rosan.installer.ui.page.main.widget.setting.AppBackButton
import com.rosan.installer.ui.page.main.widget.setting.BaseWidget
import com.rosan.installer.ui.page.main.widget.setting.LabelWidget
import com.rosan.installer.ui.page.main.widget.setting.SelectableSettingItem
import com.rosan.installer.ui.page.main.widget.setting.SwitchWidget
import com.rosan.installer.ui.theme.m3color.ThemeMode
import com.rosan.installer.ui.theme.none

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegacyThemeSettingsPage(
    navController: NavController,
    viewModel: PreferredViewModel,
) {
    val state = viewModel.state
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showHideLauncherIconDialog by remember { mutableStateOf(false) }

    var showPaletteDialog by remember { mutableStateOf(false) }
    var showThemeModeDialog by remember { mutableStateOf(false) }

    if (showPaletteDialog) {
        PaletteStyleDialog(
            currentStyle = state.paletteStyle,
            onDismiss = { showPaletteDialog = false },
            onSelect = { style ->
                viewModel.dispatch(PreferredViewAction.SetPaletteStyle(style))
                showPaletteDialog = false
            }
        )
    }

    if (showThemeModeDialog) {
        ThemeModeDialog(
            currentMode = state.themeMode,
            onDismiss = { showThemeModeDialog = false },
            onSelect = { mode ->
                viewModel.dispatch(PreferredViewAction.SetThemeMode(mode))
                showThemeModeDialog = false
            }
        )
    }

    HideLauncherIconWarningDialog(
        show = showHideLauncherIconDialog,
        onDismiss = { showHideLauncherIconDialog = false },
        onConfirm = {
            showHideLauncherIconDialog = false
            viewModel.dispatch(PreferredViewAction.ChangeShowLauncherIcon(false))
        }
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.none,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.theme_settings)) },
                navigationIcon = { AppBackButton(onClick = { navController.navigateUp() }) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                LabelWidget(label = stringResource(R.string.theme_settings_ui_style))
            }
            item {
                Column(modifier = Modifier.padding(start = 36.dp, end = 12.dp)) {
                    SelectableSettingItem(
                        title = stringResource(R.string.theme_settings_google_ui),
                        description = stringResource(R.string.theme_settings_google_ui_desc),
                        selected = !state.showMiuixUI,
                        onClick = {
                            if (state.showMiuixUI) { // Only dispatch if changing state
                                viewModel.dispatch(PreferredViewAction.ChangeUseMiuix(false))
                            }
                        }
                    )

                    SelectableSettingItem(
                        title = stringResource(R.string.theme_settings_miuix_ui),
                        description = stringResource(R.string.theme_settings_miuix_ui_desc),
                        selected = state.showMiuixUI,
                        onClick = {
                            if (!state.showMiuixUI) { // Only dispatch if changing state
                                viewModel.dispatch(PreferredViewAction.ChangeUseMiuix(true))
                            }
                        }
                    )
                }
            }
            item { LabelWidget(stringResource(R.string.theme_settings_google_ui)) }
            item {
                SwitchWidget(
                    icon = AppIcons.Theme,
                    title = stringResource(R.string.theme_settings_use_expressive_ui),
                    description = stringResource(R.string.theme_settings_use_expressive_ui_desc),
                    checked = state.showExpressiveUI,
                    isM3E = false,
                    onCheckedChange = {
                        viewModel.dispatch(PreferredViewAction.ChangeShowExpressiveUI(it))
                    }
                )
            }
            item {
                BaseWidget(
                    icon = Icons.Default.DarkMode,
                    title = stringResource(R.string.theme_settings_theme_mode),
                    description = when (state.themeMode) {
                        ThemeMode.LIGHT -> stringResource(R.string.theme_settings_theme_mode_light)
                        ThemeMode.DARK -> stringResource(R.string.theme_settings_theme_mode_dark)
                        ThemeMode.SYSTEM -> stringResource(R.string.theme_settings_theme_mode_system)
                    },
                    onClick = { showThemeModeDialog = true }
                ) {}
            }
            item {
                BaseWidget(
                    icon = Icons.Default.Style,
                    title = stringResource(R.string.theme_settings_palette_style),
                    description = state.paletteStyle.displayName,
                    onClick = { showPaletteDialog = true }
                ) {}
            }
            item {
                SwitchWidget(
                    icon = Icons.TwoTone.InvertColors,
                    title = stringResource(R.string.theme_settings_dynamic_color),
                    description = stringResource(R.string.theme_settings_dynamic_color_desc),
                    isM3E = false,
                    checked = state.useDynamicColor,
                    onCheckedChange = {
                        viewModel.dispatch(PreferredViewAction.SetUseDynamicColor(it))
                    }
                )
            }
            item {
                SwitchWidget(
                    icon = Icons.TwoTone.Colorize,
                    title = stringResource(R.string.theme_settings_dynamic_color_follow_icon),
                    description = stringResource(R.string.theme_settings_dynamic_color_follow_icon_desc),
                    isM3E = false,
                    checked = state.useDynColorFollowPkgIcon,
                    onCheckedChange = {
                        viewModel.dispatch(PreferredViewAction.SetDynColorFollowPkgIcon(it))
                    }
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && state.showLiveActivity)
                item {
                    SwitchWidget(
                        icon = Icons.TwoTone.Colorize,
                        title = stringResource(R.string.theme_settings_live_activity_dynamic_color_follow_icon),
                        description = stringResource(R.string.theme_settings_live_activity_dynamic_color_follow_icon_desc),
                        isM3E = false,
                        checked = state.useDynColorFollowPkgIconForLiveActivity,
                        onCheckedChange = {
                            viewModel.dispatch(PreferredViewAction.SetDynColorFollowPkgIconForLiveActivity(it))
                        }
                    )
                }

            item {
                AnimatedVisibility(
                    visible = !state.useDynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S,
                    enter = fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)) +
                            expandVertically(animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)) +
                            shrinkVertically(animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing))
                ) {
                    Column {
                        LabelWidget(stringResource(R.string.theme_settings_theme_color))
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 16.dp)
                        ) {
                            val itemMinWidth = 88.dp
                            val columns = (this.maxWidth / itemMinWidth).toInt().coerceAtLeast(1)
                            val chunkedColors = state.availableColors.chunked(columns)

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                chunkedColors.forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        rowItems.forEach { rawColor ->
                                            Box(
                                                modifier = Modifier.weight(1f),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                ColorSwatchPreview(
                                                    rawColor,
                                                    currentStyle = state.paletteStyle,
                                                    textStyle = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                                                    textColor = MaterialTheme.colorScheme.onSurface,
                                                    isSelected = state.seedColor == rawColor.color &&
                                                            !(state.useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S),
                                                ) {
                                                    viewModel.dispatch(PreferredViewAction.SetSeedColor(rawColor.color))
                                                }
                                            }
                                        }

                                        val remaining = columns - rowItems.size
                                        if (remaining > 0) {
                                            repeat(remaining) {
                                                Spacer(Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { LabelWidget(stringResource(R.string.theme_settings_package_icons)) }
            item {
                SwitchWidget(
                    icon = AppIcons.IconPack,
                    title = stringResource(R.string.theme_settings_prefer_system_icon),
                    description = stringResource(R.string.theme_settings_prefer_system_icon_desc),
                    checked = state.preferSystemIcon,
                    isM3E = false,
                    onCheckedChange = {
                        viewModel.dispatch(
                            PreferredViewAction.ChangePreferSystemIcon(it)
                        )
                    }
                )
            }
            item { LabelWidget(stringResource(R.string.theme_settings_launcher_icons)) }
            item {
                SwitchWidget(
                    icon = AppIcons.BugReport,
                    title = stringResource(R.string.theme_settings_hide_launcher_icon),
                    description = stringResource(R.string.theme_settings_hide_launcher_icon_desc),
                    checked = !state.showLauncherIcon,
                    isM3E = false,
                    onCheckedChange = { newCheckedState ->
                        if (newCheckedState) {
                            showHideLauncherIconDialog = true
                        } else {
                            viewModel.dispatch(PreferredViewAction.ChangeShowLauncherIcon(true))
                        }
                    }
                )
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}