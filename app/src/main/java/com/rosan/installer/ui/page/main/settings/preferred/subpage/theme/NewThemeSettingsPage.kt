package com.rosan.installer.ui.page.main.settings.preferred.subpage.theme

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.twotone.Colorize
import androidx.compose.material.icons.twotone.InvertColors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kieronquinn.monetcompat.core.MonetCompat
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewAction
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.page.main.widget.dialog.HideLauncherIconWarningDialog
import com.rosan.installer.ui.page.main.widget.setting.AppBackButton
import com.rosan.installer.ui.page.main.widget.setting.BaseWidget
import com.rosan.installer.ui.page.main.widget.setting.SelectableSettingItem
import com.rosan.installer.ui.page.main.widget.setting.SplicedColumnGroup
import com.rosan.installer.ui.page.main.widget.setting.SwitchWidget
import com.rosan.installer.ui.theme.m3color.PaletteStyle
import com.rosan.installer.ui.theme.m3color.PresetColors
import com.rosan.installer.ui.theme.m3color.RawColor
import com.rosan.installer.ui.theme.m3color.ThemeMode
import com.rosan.installer.ui.theme.m3color.dynamicColorScheme
import com.rosan.installer.ui.theme.none
import com.rosan.installer.ui.util.getDisplayName

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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
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

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        contentWindowInsets = WindowInsets.none,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
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
                            containerColor = MaterialTheme.colorScheme.surfaceBright
                        )
                        Spacer(modifier = Modifier.size(16.dp))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                SplicedColumnGroup(
                    title = stringResource(R.string.theme_settings_ui_style),
                    content = listOf(
                        {
                            // Option 1: Google UI
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
                        },
                        {
                            // Option 2: MIUIX UI
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
                    )
                )
            }

            // --- Google UI Style Options (Legacy vs Expressive) ---
            // Only show this section if Google UI is selected
            if (!state.showMiuixUI) {
                item {
                    SplicedColumnGroup(
                        title = stringResource(R.string.theme_settings_google_ui),
                        content = buildList {
                            add {
                                SwitchWidget(
                                    icon = AppIcons.Theme,
                                    title = stringResource(R.string.theme_settings_use_expressive_ui),
                                    description = stringResource(R.string.theme_settings_use_expressive_ui_desc),
                                    checked = state.showExpressiveUI,
                                    onCheckedChange = {
                                        viewModel.dispatch(
                                            PreferredViewAction.ChangeShowExpressiveUI(it)
                                        )
                                    }
                                )
                            }
                            add {
                                // Theme Mode Selector
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
                            add {
                                // Palette Style Selector
                                BaseWidget(
                                    icon = Icons.Default.Style,
                                    title = stringResource(R.string.theme_settings_palette_style),
                                    description = state.paletteStyle.displayName,
                                    onClick = { showPaletteDialog = true }
                                ) {}
                            }
                            add {
                                // Dynamic Color Switch
                                SwitchWidget(
                                    icon = Icons.TwoTone.InvertColors,
                                    title = stringResource(R.string.theme_settings_dynamic_color),
                                    description = stringResource(R.string.theme_settings_dynamic_color_desc),
                                    checked = state.useDynamicColor,
                                    onCheckedChange = {
                                        viewModel.dispatch(PreferredViewAction.SetUseDynamicColor(it))
                                    }
                                )
                            }
                            add {
                                SwitchWidget(
                                    icon = Icons.TwoTone.Colorize,
                                    title = stringResource(R.string.theme_settings_dynamic_color_follow_icon),
                                    description = stringResource(R.string.theme_settings_dynamic_color_follow_icon_desc),
                                    checked = state.useDynColorFollowPkgIcon,
                                    onCheckedChange = {
                                        viewModel.dispatch(PreferredViewAction.SetDynColorFollowPkgIcon(it))
                                    }
                                )
                            }
                        }
                    )
                }
            }

            if (!state.showMiuixUI) {
                item {
                    AnimatedVisibility(
                        // Define the visibility logic based on Android version and the dynamic color switch state
                        visible = !state.useDynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S,
                        enter = fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)) +
                                expandVertically(animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)),
                        exit = fadeOut(animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)) +
                                shrinkVertically(animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing))
                    ) {
                        SplicedColumnGroup(
                            title = stringResource(R.string.theme_settings_theme_color),
                            content =
                                listOf {
                                    BoxWithConstraints(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 16.dp)
                                    ) {
                                        val itemMinWidth = 88.dp

                                        val columns = (this.maxWidth / itemMinWidth).toInt().coerceAtLeast(1)

                                        val chunkedColors: List<List<Any>> = if (state.useDynamicColor && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                                            var wallpaperColors by remember { mutableStateOf<List<Int>?>(null) }

                                            LaunchedEffect(Unit) {
                                                wallpaperColors = MonetCompat.getInstance().getAvailableWallpaperColors()
                                            }

                                            wallpaperColors?.chunked(columns) ?: PresetColors.chunked(columns)
                                        } else PresetColors.chunked(columns)

                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            chunkedColors.forEach { rowItems ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    rowItems.forEach { namedColor ->
                                                        Box(
                                                            modifier = Modifier.weight(1f),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            if (namedColor is RawColor) {
                                                                ColorSwatchPreview(
                                                                    rawColor = namedColor,
                                                                    currentStyle = state.paletteStyle,
                                                                    isSelected = !state.useDynamicColor && state.seedColor == namedColor.color
                                                                ) {
                                                                    viewModel.dispatch(PreferredViewAction.SetSeedColor(namedColor.color))
                                                                }
                                                            } else if (namedColor is Int) {
                                                                val rawColor = RawColor(namedColor.toHexString(), Color(namedColor))
                                                                ColorSwatchPreview(
                                                                    rawColor,
                                                                    currentStyle = state.paletteStyle,
                                                                    isSelected = state.seedColor == rawColor.color
                                                                ) {
                                                                    viewModel.dispatch(PreferredViewAction.SetSeedColor(rawColor.color))
                                                                }
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
                        )
                    }
                }
            }

            item {
                SplicedColumnGroup(
                    title = stringResource(R.string.theme_settings_package_icons),
                    content = listOf {
                        SwitchWidget(
                            icon = AppIcons.IconPack,
                            title = stringResource(R.string.theme_settings_prefer_system_icon),
                            description = stringResource(R.string.theme_settings_prefer_system_icon_desc),
                            checked = state.preferSystemIcon,
                            onCheckedChange = {
                                viewModel.dispatch(
                                    PreferredViewAction.ChangePreferSystemIcon(it)
                                )
                            }
                        )
                    }

                )
            }
            item {
                SplicedColumnGroup(
                    title = stringResource(R.string.theme_settings_launcher_icons),
                    content = listOf {
                        SwitchWidget(
                            icon = AppIcons.Launcher,
                            title = stringResource(R.string.theme_settings_hide_launcher_icon),
                            description = stringResource(R.string.theme_settings_hide_launcher_icon_desc),
                            checked = !state.showLauncherIcon,
                            onCheckedChange = { newCheckedState ->
                                if (newCheckedState) {
                                    showHideLauncherIconDialog = true
                                } else {
                                    viewModel.dispatch(
                                        PreferredViewAction.ChangeShowLauncherIcon(
                                            true
                                        )
                                    )
                                }
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
internal fun PaletteStyleDialog(
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
internal fun ThemeModeDialog(
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


@Composable
internal fun ColorSwatchPreview(
    rawColor: RawColor,
    currentStyle: PaletteStyle,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDarkForPreview = false
    val scheme = remember(rawColor.color, currentStyle, isDarkForPreview) {
        dynamicColorScheme(
            keyColor = rawColor.color,
            isDark = isDarkForPreview,
            style = currentStyle
        )
    }

    val primaryForSwatch = scheme.primaryContainer.copy(alpha = 0.9f)
    val secondaryForSwatch = scheme.secondaryContainer.copy(alpha = 0.6f)
    val tertiaryForSwatch = scheme.tertiaryContainer.copy(alpha = 0.9f)

    val squircleBackgroundColor = scheme.primary.copy(alpha = 0.3f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(color = squircleBackgroundColor, shape = RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = primaryForSwatch,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true
                    )
                    drawArc(
                        color = tertiaryForSwatch,
                        startAngle = 90f,
                        sweepAngle = 90f,
                        useCenter = true
                    )
                    drawArc(
                        color = secondaryForSwatch,
                        startAngle = 0f,
                        sweepAngle = 90f,
                        useCenter = true
                    )
                }

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(scheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = scheme.inversePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        val displayName = rawColor.getDisplayName(LocalContext.current)
        if (displayName != rawColor.key) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = displayName,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}