package com.rosan.installer.ui.page.main.settings.preferred.subpage.theme

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.twotone.Colorize
import androidx.compose.material.icons.twotone.InvertColors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.rosan.installer.ui.page.main.widget.dialog.BlurWarningDialog
import com.rosan.installer.ui.page.main.widget.dialog.HideLauncherIconWarningDialog
import com.rosan.installer.ui.page.main.widget.setting.AppBackButton
import com.rosan.installer.ui.page.main.widget.setting.BaseWidget
import com.rosan.installer.ui.page.main.widget.setting.SelectableSettingItem
import com.rosan.installer.ui.page.main.widget.setting.SplicedColumnGroup
import com.rosan.installer.ui.page.main.widget.setting.SwitchWidget
import com.rosan.installer.ui.theme.getM3TopBarColor
import com.rosan.installer.ui.theme.installerHazeEffect
import com.rosan.installer.ui.theme.m3color.PaletteStyle
import com.rosan.installer.ui.theme.m3color.ThemeMode
import com.rosan.installer.ui.theme.none
import com.rosan.installer.ui.theme.rememberMaterial3HazeStyle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

// This is now a top-level composable, likely in its own file.
// It takes NavController instead of an onBack lambda.
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewThemeSettingsPage(
    navController: NavController,
    viewModel: PreferredViewModel,
) {
    val state = viewModel.state
    val topAppBarState = rememberTopAppBarState()
    val hazeState = if (state.useBlur) remember { HazeState() } else null
    val hazeStyle = rememberMaterial3HazeStyle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

    var showHideLauncherIconDialog by remember { mutableStateOf(false) }
    var showPaletteDialog by remember { mutableStateOf(false) }
    var showThemeModeDialog by remember { mutableStateOf(false) }
    var showBlurWarningDialog by remember { mutableStateOf(false) }

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

    LaunchedEffect(Unit) {
        topAppBarState.heightOffset = topAppBarState.heightOffsetLimit
    }

    HideLauncherIconWarningDialog(
        show = showHideLauncherIconDialog,
        onDismiss = { showHideLauncherIconDialog = false },
        onConfirm = {
            showHideLauncherIconDialog = false
            viewModel.dispatch(PreferredViewAction.ChangeShowLauncherIcon(false))
        }
    )

    BlurWarningDialog(
        show = showBlurWarningDialog,
        onDismiss = { showBlurWarningDialog = false },
        onConfirm = {
            showBlurWarningDialog = false
            viewModel.dispatch(PreferredViewAction.SetUseBlur(true))
        }
    )

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        contentWindowInsets = WindowInsets.none,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                modifier = Modifier.installerHazeEffect(hazeState, hazeStyle),
                windowInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = 12.dp)),
                title = {
                    Text(stringResource(R.string.theme_settings))
                },
                navigationIcon = {
                    Row {
                        AppBackButton(
                            onClick = { navController.navigateUp() },
                            icon = Icons.AutoMirrored.TwoTone.ArrowBack,
                            modifier = Modifier.size(36.dp),
                            containerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        )
                        Spacer(modifier = Modifier.size(16.dp))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = hazeState.getM3TopBarColor(),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    scrolledContainerColor = hazeState.getM3TopBarColor()
                )
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(hazeState?.let { Modifier.hazeSource(it) } ?: Modifier),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding()
            )
        ) {
            // --- Group 1: UI Style Selection ---
            item {
                SplicedColumnGroup(
                    title = stringResource(R.string.theme_settings_ui_style)
                ) {
                    // Option 1: Google UI
                    item {
                        SelectableSettingItem(
                            title = stringResource(R.string.theme_settings_google_ui),
                            description = stringResource(R.string.theme_settings_google_ui_desc),
                            selected = !state.showMiuixUI,
                            onClick = {
                                if (state.showMiuixUI) {
                                    viewModel.dispatch(PreferredViewAction.ChangeUseMiuix(false))
                                }
                            }
                        )
                    }
                    // Option 2: MIUIX UI
                    item {
                        SelectableSettingItem(
                            title = stringResource(R.string.theme_settings_miuix_ui),
                            description = stringResource(R.string.theme_settings_miuix_ui_desc),
                            selected = state.showMiuixUI,
                            onClick = {
                                if (!state.showMiuixUI) {
                                    viewModel.dispatch(PreferredViewAction.ChangeUseMiuix(true))
                                }
                            }
                        )
                    }
                }
            }

            // --- Group 2: Google UI Options ---
            item {
                SplicedColumnGroup(
                    title = stringResource(R.string.theme_settings_google_ui)
                ) {
                    item {
                        SwitchWidget(
                            icon = AppIcons.Theme,
                            title = stringResource(R.string.theme_settings_use_expressive_ui),
                            description = stringResource(R.string.theme_settings_use_expressive_ui_desc),
                            checked = state.showExpressiveUI,
                            onCheckedChange = { viewModel.dispatch(PreferredViewAction.ChangeShowExpressiveUI(it)) }
                        )
                    }
                    item {
                        SwitchWidget(
                            icon = AppIcons.Blur,
                            title = stringResource(R.string.theme_settings_use_blur),
                            description = stringResource(R.string.theme_settings_use_blur_desc),
                            checked = state.useBlur,
                            onCheckedChange = { isChecked ->
                                if (isChecked && Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
                                    showBlurWarningDialog = true
                                } else {
                                    viewModel.dispatch(PreferredViewAction.SetUseBlur(isChecked))
                                }
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
                            checked = state.useDynamicColor,
                            onCheckedChange = { viewModel.dispatch(PreferredViewAction.SetUseDynamicColor(it)) }
                        )
                    }
                    item {
                        SwitchWidget(
                            icon = Icons.TwoTone.Colorize,
                            title = stringResource(R.string.theme_settings_dynamic_color_follow_icon),
                            description = stringResource(R.string.theme_settings_dynamic_color_follow_icon_desc),
                            checked = state.useDynColorFollowPkgIcon,
                            onCheckedChange = { viewModel.dispatch(PreferredViewAction.SetDynColorFollowPkgIcon(it)) }
                        )
                    }
                    // Conditional item for Live Activity
                    item(visible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && state.showLiveActivity) {
                        SwitchWidget(
                            icon = Icons.TwoTone.Colorize,
                            title = stringResource(R.string.theme_settings_live_activity_dynamic_color_follow_icon),
                            description = stringResource(R.string.theme_settings_live_activity_dynamic_color_follow_icon_desc),
                            checked = state.useDynColorFollowPkgIconForLiveActivity,
                            onCheckedChange = {
                                viewModel.dispatch(
                                    PreferredViewAction.SetDynColorFollowPkgIconForLiveActivity(
                                        it
                                    )
                                )
                            }
                        )
                    }
                }
            }

            // --- Group 3: Theme Color (Manual Selection) ---
            item {
                AnimatedVisibility(
                    visible = !state.useDynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S,
                    enter = fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)) +
                            expandVertically(animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)) +
                            shrinkVertically(animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing))
                ) {
                    SplicedColumnGroup(
                        title = stringResource(R.string.theme_settings_theme_color)
                    ) {
                        item {
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
                                                        rawColor = rawColor,
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
            }

            // --- Group 4: Package Icons ---
            item {
                SplicedColumnGroup(
                    title = stringResource(R.string.theme_settings_package_icons)
                ) {
                    item {
                        SwitchWidget(
                            icon = AppIcons.IconPack,
                            title = stringResource(R.string.theme_settings_prefer_system_icon),
                            description = stringResource(R.string.theme_settings_prefer_system_icon_desc),
                            checked = state.preferSystemIcon,
                            onCheckedChange = { viewModel.dispatch(PreferredViewAction.ChangePreferSystemIcon(it)) }
                        )
                    }
                }
            }

            // --- Group 5: Launcher Icons ---
            item {
                SplicedColumnGroup(
                    title = stringResource(R.string.theme_settings_launcher_icons)
                ) {
                    item {
                        SwitchWidget(
                            icon = AppIcons.Launcher,
                            title = stringResource(R.string.theme_settings_hide_launcher_icon),
                            description = stringResource(R.string.theme_settings_hide_launcher_icon_desc),
                            checked = !state.showLauncherIcon,
                            onCheckedChange = { newCheckedState ->
                                if (newCheckedState) {
                                    showHideLauncherIconDialog = true
                                } else {
                                    viewModel.dispatch(PreferredViewAction.ChangeShowLauncherIcon(true))
                                }
                            }
                        )
                    }
                }
            }

            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}

@Composable
fun PaletteStyleDialog(
    currentStyle: PaletteStyle,
    onDismiss: () -> Unit,
    onSelect: (PaletteStyle) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_settings_palette_style_desc)) },
        text = {
            Column {
                PaletteStyle.entries.forEach { style ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(style) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (style == currentStyle),
                            onClick = { onSelect(style) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(style.displayName)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
fun ThemeModeDialog(
    currentMode: ThemeMode,
    onDismiss: () -> Unit,
    onSelect: (ThemeMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_settings_theme_mode_desc)) },
        text = {
            Column {
                ThemeMode.entries.forEach { mode ->
                    val modeText = when (mode) {
                        ThemeMode.LIGHT -> stringResource(R.string.theme_settings_theme_mode_light)
                        ThemeMode.DARK -> stringResource(R.string.theme_settings_theme_mode_dark)
                        ThemeMode.SYSTEM -> stringResource(R.string.theme_settings_theme_mode_system)
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(mode) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (mode == currentMode),
                            onClick = { onSelect(mode) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(modeText)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}