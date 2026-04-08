// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.settings.preferred.about

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.shapes.RoundedRectangle
import com.rosan.installer.BuildConfig
import com.rosan.installer.R
import com.rosan.installer.core.env.AppConfig
import com.rosan.installer.domain.device.model.Level
import com.rosan.installer.ui.library.blend.ColorBlendToken
import com.rosan.installer.ui.library.effect.BgEffectBackground
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.navigation.Route
import com.rosan.installer.ui.page.main.settings.preferred.about.AboutAction
import com.rosan.installer.ui.page.main.settings.preferred.about.AboutEvent
import com.rosan.installer.ui.page.main.settings.preferred.about.AboutState
import com.rosan.installer.ui.page.main.settings.preferred.about.AboutViewModel
import com.rosan.installer.ui.page.main.widget.util.LogEventCollector
import com.rosan.installer.ui.page.miuix.settings.preferred.MiuixNavigationItemWidget
import com.rosan.installer.ui.page.miuix.widgets.ErrorDisplaySheet
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.page.miuix.widgets.MiuixSwitchWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixUpdateDialog
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.isRenderEffectSupported
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun MiuixBlendAboutPage(
    viewModel: AboutViewModel = koinViewModel()
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val showUpdateDialog = remember { mutableStateOf(false) }

    // Format app version string
    val internetAccessHint = if (AppConfig.isInternetAccessEnabled) stringResource(R.string.internet_access_enabled)
    else stringResource(R.string.internet_access_disabled)

    val level = when (AppConfig.LEVEL) {
        Level.STABLE -> stringResource(id = R.string.stable)
        Level.PREVIEW -> stringResource(id = R.string.preview)
        Level.UNSTABLE -> stringResource(id = R.string.unstable)
    }

    val versionInfoText = stringResource(
        id = R.string.app_version_info_format,
        internetAccessHint,
        level,
        AppConfig.VERSION_NAME,
        AppConfig.VERSION_CODE
    )

    MiuixUpdateDialog(
        showState = showUpdateDialog,
        onDismiss = { showUpdateDialog.value = false }
    )

    val showLoadingDialog = remember { mutableStateOf(false) }
    val showUpdateErrorDialog = remember { mutableStateOf(false) }
    var updateErrorInfo by remember { mutableStateOf<AboutEvent.ShowInAppUpdateErrorDetail?>(null) }

    LogEventCollector(viewModel)

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is AboutEvent.ShowUpdateLoading -> showLoadingDialog.value = true
                is AboutEvent.HideUpdateLoading -> showLoadingDialog.value = false
                is AboutEvent.ShowInAppUpdateErrorDetail -> {
                    showLoadingDialog.value = false
                    updateErrorInfo = event
                    showUpdateErrorDialog.value = true
                }

                else -> {}
            }
        }
    }

    // Scroll state and progress calculations for animations
    val lazyListState = rememberLazyListState()
    val topAppBarScrollBehavior = MiuixScrollBehavior()
    var logoHeightPx by remember { mutableIntStateOf(0) }

    val scrollProgress by remember {
        derivedStateOf {
            if (logoHeightPx <= 0) {
                0f
            } else {
                val index = lazyListState.firstVisibleItemIndex
                val offset = lazyListState.firstVisibleItemScrollOffset
                if (index > 0) 1f else (offset.toFloat() / logoHeightPx).coerceIn(0f, 1f)
            }
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = stringResource(id = R.string.about),
                scrollBehavior = topAppBarScrollBehavior,
                color = MiuixTheme.colorScheme.surface.copy(alpha = if (scrollProgress == 1f) 1f else 0f),
                titleColor = MiuixTheme.colorScheme.onSurface.copy(alpha = scrollProgress),
                navigationIcon = { MiuixBackButton(onClick = { navigator.pop() }) }
            )
        },
    ) { innerPadding ->
        AboutContentBody(
            padding = innerPadding,
            uiState = uiState,
            versionInfoText = versionInfoText,
            lazyListState = lazyListState,
            scrollProgress = scrollProgress,
            topAppBarScrollBehavior = topAppBarScrollBehavior,
            onLogoHeightChanged = { logoHeightPx = it },
            onGetUpdateClicked = { showUpdateDialog.value = true },
            onDirectUpdateClicked = { viewModel.dispatch(AboutAction.PerformUpdate) },
            onLicenseClicked = { navigator.push(Route.OpenSourceLicense) },
            onLogToggle = { viewModel.dispatch(AboutAction.SetEnableFileLogging(it)) },
            onLogExport = { viewModel.dispatch(AboutAction.ShareLog) },
            uriHandler = uriHandler,
            context = context
        )
    }

    // Fullscreen dialogs
    WindowDialog(show = showLoadingDialog.value) {
        BackHandler { }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfiniteProgressIndicator()
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = stringResource(R.string.updating))
        }
    }

    updateErrorInfo?.let { sheetInfo ->
        ErrorDisplaySheet(
            title = sheetInfo.title,
            showState = showUpdateErrorDialog,
            exception = sheetInfo.exception,
            onDismissRequest = {
                showUpdateErrorDialog.value = false
                updateErrorInfo = null
            }
        )
    }
}

@Composable
private fun AboutContentBody(
    padding: PaddingValues,
    uiState: AboutState,
    versionInfoText: String,
    lazyListState: LazyListState,
    scrollProgress: Float,
    topAppBarScrollBehavior: ScrollBehavior,
    onLogoHeightChanged: (Int) -> Unit,
    onGetUpdateClicked: () -> Unit,
    onDirectUpdateClicked: () -> Unit,
    onLicenseClicked: () -> Unit,
    onLogToggle: (Boolean) -> Unit,
    onLogExport: () -> Unit,
    uriHandler: UriHandler,
    context: Context
) {
    val layoutDirection = LocalLayoutDirection.current
    val isDark = isSystemInDarkTheme()

    // Texture set states
    var showTextureSet by remember { mutableStateOf(false) }
    var blurEnable by remember { mutableStateOf(isRenderEffectSupported()) }
    val dynamicBackground = remember { mutableStateOf(isRuntimeShaderSupported()) }
    val effectBackground = remember { mutableStateOf(isRuntimeShaderSupported()) }

    var blurRadius by remember { mutableFloatStateOf(60f) }
    var noiseCoefficient by remember { mutableFloatStateOf(BlurDefaults.NoiseCoefficient) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }

    val backdrop = rememberLayerBackdrop()
    val surface = MiuixTheme.colorScheme.surface.copy(alpha = 0.6f)

    // Blend configurations map
    val blendConfigs = remember(isDark) {
        mapOf(
            "Default" to if (isDark) ColorBlendToken.Overlay_Extra_Thin_Dark else ColorBlendToken.Pured_Regular_Light,
            "None" to emptyList(),
            "SrcOver" to listOf(BlendColorEntry(surface, BlurBlendMode.SrcOver)),
            "Screen" to listOf(BlendColorEntry(surface, BlurBlendMode.Screen)),
            "Multiply" to listOf(BlendColorEntry(surface, BlurBlendMode.Multiply)),
            "Overlay" to listOf(BlendColorEntry(surface, BlurBlendMode.Overlay)),
            "Soft Light" to listOf(BlendColorEntry(surface, BlurBlendMode.SoftLight)),
            "Linear Light" to listOf(BlendColorEntry(surface, BlurBlendMode.LinearLight)),
            "Linear Light Grey" to listOf(BlendColorEntry(surface, BlurBlendMode.LinearLightWithGreyscale)),
            "Linear Light Lab" to listOf(BlendColorEntry(surface, BlurBlendMode.LinearLightLab)),
            "Lab Lighten" to listOf(BlendColorEntry(surface, BlurBlendMode.LabLightenWithGreyscale)),
            "Lab Darken" to listOf(BlendColorEntry(surface, BlurBlendMode.LabDarkenWithGreyscale)),
            "MI Difference" to listOf(BlendColorEntry(surface, BlurBlendMode.MiDifference)),
            "MI Color Dodge" to listOf(BlendColorEntry(surface, BlurBlendMode.MiColorDodge)),
            "MI Color Burn" to listOf(BlendColorEntry(surface, BlurBlendMode.MiColorBurn)),
            "Plus Lighter" to listOf(BlendColorEntry(surface, BlurBlendMode.PlusLighter)),
            "Plus Darker" to listOf(BlendColorEntry(surface, BlurBlendMode.PlusDarker)),
        )
    }

    val configEntries = blendConfigs.entries.toList()
    var blendModeIndex by remember { mutableIntStateOf(0) }
    val currentConfigValue = configEntries.getOrNull(blendModeIndex)?.value ?: emptyList()

    val logoBlend = remember(isDark) {
        if (isDark) {
            listOf(
                BlendColorEntry(Color(0xe6a1a1a1), BlurBlendMode.ColorDodge),
                BlendColorEntry(Color(0x4de6e6e6), BlurBlendMode.LinearLight),
                BlendColorEntry(Color(0xff1af500), BlurBlendMode.Lab),
            )
        } else {
            listOf(
                BlendColorEntry(Color(0xcc4a4a4a), BlurBlendMode.ColorBurn),
                BlendColorEntry(Color(0xff4f4f4f), BlurBlendMode.LinearLight),
                BlendColorEntry(Color(0xff1af200), BlurBlendMode.Lab),
            )
        }
    }

    // Animation states
    val density = LocalDensity.current
    var logoHeightDp by remember { mutableStateOf(300.dp) }
    var logoAreaY by remember { mutableFloatStateOf(0f) }
    var iconY by remember { mutableFloatStateOf(0f) }
    var projectNameY by remember { mutableFloatStateOf(0f) }
    var versionCodeY by remember { mutableFloatStateOf(0f) }

    var iconProgress by remember { mutableFloatStateOf(0f) }
    var projectNameProgress by remember { mutableFloatStateOf(0f) }
    var versionCodeProgress by remember { mutableFloatStateOf(0f) }
    var initialLogoAreaY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemScrollOffset }
            .onEach { offset ->
                if (lazyListState.firstVisibleItemIndex > 0) {
                    if (iconProgress != 1f) iconProgress = 1f
                    if (projectNameProgress != 1f) projectNameProgress = 1f
                    if (versionCodeProgress != 1f) versionCodeProgress = 1f
                    return@onEach
                }

                if (initialLogoAreaY == 0f && logoAreaY > 0f) {
                    initialLogoAreaY = logoAreaY
                }
                val refLogoAreaY = if (initialLogoAreaY > 0f) initialLogoAreaY else logoAreaY

                val stage1TotalLength = refLogoAreaY - versionCodeY
                val stage2TotalLength = versionCodeY - projectNameY
                val stage3TotalLength = projectNameY - iconY

                val versionCodeDelay = stage1TotalLength * 0.5f
                versionCodeProgress = ((offset.toFloat() - versionCodeDelay) / (stage1TotalLength - versionCodeDelay).coerceAtLeast(1f))
                    .coerceIn(0f, 1f)
                projectNameProgress = ((offset.toFloat() - stage1TotalLength) / stage2TotalLength.coerceAtLeast(1f))
                    .coerceIn(0f, 1f)
                iconProgress = ((offset.toFloat() - stage1TotalLength - stage2TotalLength) / stage3TotalLength.coerceAtLeast(1f))
                    .coerceIn(0f, 1f)
            }
            .collect { }
    }

    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    val listContentPadding = PaddingValues(
        start = horizontalSafeInsets.calculateStartPadding(layoutDirection),
        top = padding.calculateTopPadding(),
        end = horizontalSafeInsets.calculateEndPadding(layoutDirection)
    )

    val logoPadding = PaddingValues(
        top = padding.calculateTopPadding() + 40.dp,
        start = horizontalSafeInsets.calculateStartPadding(layoutDirection),
        end = horizontalSafeInsets.calculateEndPadding(layoutDirection)
    )

    BgEffectBackground(
        dynamicBackground = dynamicBackground.value,
        modifier = Modifier.fillMaxSize(),
        bgModifier = Modifier.layerBackdrop(backdrop),
        effectBackground = effectBackground.value,
        alpha = 1f - scrollProgress,
    ) {
        // Sticky animated header section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = logoPadding.calculateTopPadding() + 52.dp,
                    start = logoPadding.calculateStartPadding(layoutDirection),
                    end = logoPadding.calculateEndPadding(layoutDirection)
                )
                .onSizeChanged { size ->
                    with(density) { logoHeightDp = size.height.toDp() }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer {
                        alpha = 1 - iconProgress
                        scaleX = 1 - (iconProgress * 0.05f)
                        scaleY = 1 - (iconProgress * 0.05f)
                    }
                    .onGloballyPositioned { coordinates ->
                        if (iconY != 0f) return@onGloballyPositioned
                        val y = coordinates.positionInWindow().y
                        val size = coordinates.size
                        iconY = y + size.height
                    },
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_monochrome),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 2.0f
                            scaleY = 2.0f
                        }
                        .textureBlur(
                            backdrop = backdrop,
                            shape = RoundedRectangle(16.dp),
                            blurRadius = 200f,
                            noiseCoefficient = BlurDefaults.NoiseCoefficient,
                            colors = BlurColors(blendColors = logoBlend),
                            contentBlendMode = BlendMode.DstIn,
                            enabled = blurEnable,
                        ),
                    contentDescription = null
                )
            }

            Text(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 5.dp)
                    .onGloballyPositioned { coordinates ->
                        if (projectNameY != 0f) return@onGloballyPositioned
                        val y = coordinates.positionInWindow().y
                        val size = coordinates.size
                        projectNameY = y + size.height
                    }
                    .graphicsLayer {
                        alpha = 1 - projectNameProgress
                        scaleX = 1 - (projectNameProgress * 0.05f)
                        scaleY = 1 - (projectNameProgress * 0.05f)
                    }
                    .textureBlur(
                        backdrop = backdrop,
                        shape = RoundedRectangle(16.dp),
                        blurRadius = 200f,
                        noiseCoefficient = BlurDefaults.NoiseCoefficient,
                        colors = BlurColors(blendColors = logoBlend),
                        contentBlendMode = BlendMode.DstIn,
                        enabled = blurEnable,
                    ),
                text = stringResource(id = R.string.app_name),
                fontWeight = FontWeight.Bold,
                fontSize = 35.sp,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = 1 - versionCodeProgress
                        scaleX = 1 - (versionCodeProgress * 0.05f)
                        scaleY = 1 - (versionCodeProgress * 0.05f)
                    }
                    .onGloballyPositioned { coordinates ->
                        if (versionCodeY != 0f) return@onGloballyPositioned
                        val y = coordinates.positionInWindow().y
                        val size = coordinates.size
                        versionCodeY = y + size.height
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    text = versionInfoText,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
                if (uiState.hasUpdate) {
                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = stringResource(R.string.update_available, uiState.remoteVersion),
                        fontSize = 14.sp,
                        color = MiuixTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Scrollable content area
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
            contentPadding = listContentPadding,
        ) {
            // Transparent spacer matching logo height
            item(key = "logoSpacer") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(
                            logoHeightDp + 52.dp + logoPadding.calculateTopPadding() - listContentPadding.calculateTopPadding() + 126.dp,
                        )
                        .onSizeChanged { size ->
                            onLogoHeightChanged(size.height)
                        }
                        .pointerInput(Unit) {
                            detectTapGestures {
                                showTextureSet = true
                            }
                        }
                        .onGloballyPositioned { coordinates ->
                            val y = coordinates.positionInWindow().y
                            val size = coordinates.size
                            logoAreaY = y + size.height
                        },
                    contentAlignment = Alignment.TopCenter,
                    content = { }
                )
            }

            item(key = "about_content") {
                Column(
                    modifier = Modifier
                        .fillParentMaxHeight()
                        .padding(bottom = listContentPadding.calculateBottomPadding()),
                ) {
                    SmallTitle(stringResource(R.string.about))

                    Card(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                            .textureBlur(
                                backdrop = backdrop,
                                shape = RoundedRectangle(16.dp),
                                blurRadius = blurRadius,
                                noiseCoefficient = noiseCoefficient,
                                colors = BlurColors(
                                    blendColors = currentConfigValue,
                                    brightness = brightness,
                                    contrast = contrast,
                                    saturation = saturation,
                                ),
                                enabled = blurEnable,
                            ),
                        colors = CardDefaults.defaultColors(
                            if (blurEnable) Color.Transparent else MiuixTheme.colorScheme.surfaceContainer,
                            Color.Transparent,
                        ),
                    ) {
                        MiuixNavigationItemWidget(
                            title = stringResource(R.string.get_source_code),
                            description = stringResource(R.string.get_source_code_detail),
                            onClick = { uriHandler.openUri("https://github.com/wxxsfxyzm/InstallerX-Revived") }
                        )
                        MiuixNavigationItemWidget(
                            title = stringResource(R.string.open_source_license),
                            description = stringResource(R.string.open_source_license_settings_description),
                            onClick = onLicenseClicked
                        )
                        MiuixNavigationItemWidget(
                            title = stringResource(R.string.get_update),
                            description = stringResource(R.string.get_update_detail),
                            onClick = onGetUpdateClicked
                        )
                        if (uiState.hasUpdate) {
                            MiuixNavigationItemWidget(
                                title = stringResource(R.string.get_update_directly),
                                description = stringResource(R.string.get_update_directly_desc),
                                onClick = onDirectUpdateClicked
                            )
                        }
                    }

                    if (AppConfig.isLogEnabled && context.packageName == BuildConfig.APPLICATION_ID) {
                        SmallTitle(stringResource(R.string.debug))
                        Card(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp)
                                .textureBlur(
                                    backdrop = backdrop,
                                    shape = RoundedRectangle(16.dp),
                                    blurRadius = blurRadius,
                                    noiseCoefficient = noiseCoefficient,
                                    colors = BlurColors(
                                        blendColors = currentConfigValue,
                                        brightness = brightness,
                                        contrast = contrast,
                                        saturation = saturation,
                                    ),
                                    enabled = blurEnable,
                                ),
                            colors = CardDefaults.defaultColors(
                                if (blurEnable) Color.Transparent else MiuixTheme.colorScheme.surfaceContainer,
                                Color.Transparent,
                            ),
                        ) {
                            MiuixSwitchWidget(
                                title = stringResource(R.string.save_logs),
                                description = stringResource(R.string.save_logs_desc),
                                checked = uiState.enableFileLogging,
                                onCheckedChange = onLogToggle
                            )
                            AnimatedVisibility(
                                visible = uiState.enableFileLogging,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                BasicComponent(
                                    title = stringResource(R.string.export_logs),
                                    summary = stringResource(R.string.export_logs_desc),
                                    onClick = onLogExport
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    // Dev mode texture settings bottom sheet
    OverlayBottomSheet(
        show = showTextureSet,
        title = "Texture Set",
        onDismissRequest = {
            showTextureSet = false
        },
        insideMargin = DpSize(0.dp, 0.dp),
    ) {
        LazyColumn {
            item {
                Card {
                    BasicComponent(
                        title = "Effect Background Enabled",
                        endActions = {
                            Switch(
                                effectBackground.value,
                                {
                                    effectBackground.value = it
                                },
                            )
                        },
                        insideMargin = PaddingValues(16.dp, 16.dp, 16.dp, 0.dp),
                    )
                    BasicComponent(
                        title = "Dynamic Background Enabled",
                        endActions = {
                            Switch(
                                dynamicBackground.value,
                                {
                                    dynamicBackground.value = it
                                },
                            )
                        },
                        insideMargin = PaddingValues(16.dp, 16.dp, 16.dp, 0.dp),
                    )
                    BasicComponent(
                        title = "Blur Enable",
                        endActions = {
                            Switch(
                                blurEnable,
                                {
                                    blurEnable = it
                                },
                            )
                        },
                        insideMargin = PaddingValues(16.dp, 16.dp, 16.dp, 0.dp),
                    )

                    // Blur radius setting
                    BasicComponent(
                        title = "Blur Radius",
                        endActions = { ValueText("${blurRadius.toInt()}") },
                        bottomAction = {
                            Slider(
                                value = blurRadius / 200f,
                                onValueChange = { blurRadius = it * 200f },
                            )
                        },
                        insideMargin = PaddingValues(16.dp, 16.dp, 16.dp, 0.dp),
                    )

                    // Noise setting
                    BasicComponent(
                        title = "Noise",
                        endActions = { ValueText("${(noiseCoefficient * 10000).toInt() / 10000f}") },
                        bottomAction = {
                            Slider(
                                value = noiseCoefficient / 0.1f,
                                onValueChange = { noiseCoefficient = it * 0.1f },
                            )
                        },
                        insideMargin = PaddingValues(16.dp, 16.dp, 16.dp, 0.dp),
                    )

                    // Brightness setting
                    BasicComponent(
                        title = "Brightness",
                        endActions = { ValueText("${(brightness * 100).toInt() / 100f}") },
                        bottomAction = {
                            Slider(
                                value = (brightness + 1f) / 2f,
                                onValueChange = { brightness = it * 2f - 1f },
                            )
                        },
                        insideMargin = PaddingValues(16.dp, 16.dp, 16.dp, 0.dp),
                    )

                    // Contrast setting
                    BasicComponent(
                        title = "Contrast",
                        endActions = { ValueText("${(contrast * 100).toInt() / 100f}") },
                        bottomAction = {
                            Slider(
                                value = contrast / 3f,
                                onValueChange = { contrast = it * 3f },
                            )
                        },
                        insideMargin = PaddingValues(16.dp, 16.dp, 16.dp, 0.dp),
                    )

                    // Saturation setting
                    BasicComponent(
                        title = "Saturation",
                        endActions = { ValueText("${(saturation * 100).toInt() / 100f}") },
                        bottomAction = {
                            Slider(
                                value = saturation / 3f,
                                onValueChange = { saturation = it * 3f },
                            )
                        },
                        insideMargin = PaddingValues(16.dp, 16.dp, 16.dp, 0.dp),
                    )

                    // Blend mode setting
                    val currentConfigName = configEntries.getOrNull(blendModeIndex)?.key ?: "Default"
                    val modeId = blendConfigs[currentConfigName]
                    BasicComponent(
                        title = "Blend Mode",
                        endActions = {
                            ValueText(currentConfigName + if (modeId != null) " ($modeId)" else "")
                        },
                        bottomAction = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                TextButton(
                                    text = "Prev",
                                    onClick = {
                                        blendModeIndex = (blendModeIndex - 1 + blendConfigs.size) % blendConfigs.size
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(
                                    text = "Next",
                                    onClick = {
                                        blendModeIndex = (blendModeIndex + 1) % blendConfigs.size
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        },
                    )
                    Spacer(modifier = Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()))
                }
            }
        }
    }
}

@Suppress("FunctionName")
@Composable
private fun ValueText(text: String) {
    Text(
        text = text,
        fontSize = MiuixTheme.textStyles.body2.fontSize,
        color = MiuixTheme.colorScheme.onSurfaceVariantActions,
    )
}