package top.yukonga.miuix.kmp.extra

// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.G2RoundedCornerShape
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.DialogLayout
import top.yukonga.miuix.kmp.utils.PredictiveBackHandler
import top.yukonga.miuix.kmp.utils.getWindowSize

/**
 * A bottom sheet that slides up from the bottom of the screen.
 * The height adapts to the content size, but will not cover the status bar area.
 *
 * @param show The show state of the [SuperBottomSheet].
 * @param modifier The modifier to be applied to the [SuperBottomSheet].
 * @param title Optional title to display at the top of the [SuperBottomSheet].
 * @param leftAction Optional composable to display on the left side of the title (e.g. a close button).
 * @param rightAction Optional composable to display on the right side of the title (e.g. a submit button).
 * @param backgroundColor The background color of the [SuperBottomSheet].
 * @param enableWindowDim Whether to dim the window behind the [SuperBottomSheet].
 * @param cornerRadius The corner radius of the top corners of the [SuperBottomSheet].
 * @param sheetMaxWidth The maximum width of the [SuperBottomSheet].
 * @param onDismissRequest The callback when the [SuperBottomSheet] is dismissed.
 * @param outsideMargin The margin outside the [SuperBottomSheet].
 * @param insideMargin The margin inside the [SuperBottomSheet].
 * @param defaultWindowInsetsPadding Whether to apply default window insets padding.
 * @param dragHandleColor The color of the drag handle at the top.
 * @param content The [Composable] content of the [SuperBottomSheet].
 */
@Composable
fun SuperBottomSheet(
    show: MutableState<Boolean>,
    modifier: Modifier = Modifier,
    title: String? = null,
    leftAction: @Composable (() -> Unit?)? = null,
    rightAction: @Composable (() -> Unit?)? = null,
    backgroundColor: Color = SuperBottomSheetDefaults.backgroundColor(),
    enableWindowDim: Boolean = true,
    cornerRadius: Dp = SuperBottomSheetDefaults.cornerRadius,
    sheetMaxWidth: Dp = SuperBottomSheetDefaults.maxWidth,
    onDismissRequest: (() -> Unit)? = null,
    outsideMargin: DpSize = SuperBottomSheetDefaults.outsideMargin,
    insideMargin: DpSize = SuperBottomSheetDefaults.insideMargin,
    defaultWindowInsetsPadding: Boolean = true,
    dragHandleColor: Color = SuperBottomSheetDefaults.dragHandleColor(),
    content: @Composable () -> Unit
) {
    if (!show.value) return

    val dimAlpha = remember { mutableFloatStateOf(1f) }
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    val sheetHeightPx = remember { mutableIntStateOf(0) }
    val dragOffsetY = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    DialogLayout(
        visible = show,
        enableWindowDim = enableWindowDim,
        enableAutoLargeScreen = false,
        dimAlpha = dimAlpha
    ) {
        SuperBottomSheetContent(
            modifier = modifier,
            title = title,
            leftAction = leftAction,
            rightAction = rightAction,
            backgroundColor = backgroundColor,
            cornerRadius = cornerRadius,
            sheetMaxWidth = sheetMaxWidth,
            outsideMargin = outsideMargin,
            insideMargin = insideMargin,
            defaultWindowInsetsPadding = defaultWindowInsetsPadding,
            dragHandleColor = dragHandleColor,
            dimAlpha = dimAlpha,
            sheetHeightPx = sheetHeightPx,
            dragOffsetY = dragOffsetY,
            onDismissRequest = currentOnDismissRequest,
            content = content
        )
    }

    PredictiveBackHandler(
        enabled = show.value,
        onBackProgressed = { event ->
            coroutineScope.launch {
                // Calculate offset based on back progress
                val maxOffset = if (sheetHeightPx.value > 0) {
                    sheetHeightPx.value.toFloat()
                } else {
                    500f
                }
                val offset = event.progress * maxOffset
                dragOffsetY.snapTo(offset)

                // Update dim alpha
                dimAlpha.value = 1f - event.progress
            }
        },
        onBackCancelled = {
            coroutineScope.launch {
                // Reset to original position
                dragOffsetY.animateTo(0f, animationSpec = tween(durationMillis = 150))
                dimAlpha.value = 1f
            }
        },
        onBack = {
            currentOnDismissRequest?.invoke()
        }
    )
}

@Composable
private fun SuperBottomSheetContent(
    modifier: Modifier,
    title: String?,
    leftAction: @Composable (() -> Unit?)? = null,
    rightAction: @Composable (() -> Unit?)? = null,
    backgroundColor: Color,
    cornerRadius: Dp,
    sheetMaxWidth: Dp,
    outsideMargin: DpSize,
    insideMargin: DpSize,
    defaultWindowInsetsPadding: Boolean,
    dragHandleColor: Color,
    dimAlpha: MutableState<Float>,
    sheetHeightPx: MutableState<Int>,
    dragOffsetY: Animatable<Float, *>,
    onDismissRequest: (() -> Unit)?,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val windowSize by rememberUpdatedState(getWindowSize())
    val windowHeight by remember(windowSize, density) {
        derivedStateOf { windowSize.height.dp / density.density }
    }

    val statusBars = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val captionBar = WindowInsets.captionBar.asPaddingValues().calculateTopPadding()
    val displayCutout = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val statusBarHeight = remember { with(density) { maxOf(statusBars, captionBar, displayCutout) } }

    val rootBoxModifier = Modifier
        .pointerInput(onDismissRequest) {
            detectTapGestures(
                onTap = {
                    onDismissRequest?.invoke()
                }
            )
        }
        .fillMaxSize()

    Box(modifier = rootBoxModifier) {
        SuperBottomSheetColumn(
            modifier = modifier,
            title = title,
            leftAction = leftAction,
            rightAction = rightAction,
            backgroundColor = backgroundColor,
            cornerRadius = cornerRadius,
            sheetMaxWidth = sheetMaxWidth,
            outsideMargin = outsideMargin,
            insideMargin = insideMargin,
            defaultWindowInsetsPadding = defaultWindowInsetsPadding,
            dragHandleColor = dragHandleColor,
            windowHeight = windowHeight,
            statusBarHeight = statusBarHeight,
            sheetHeightPx = sheetHeightPx,
            dragOffsetY = dragOffsetY,
            dimAlpha = dimAlpha,
            density = density,
            onDismissRequest = onDismissRequest,
            content = content
        )
    }
}

@Composable
private fun SuperBottomSheetColumn(
    modifier: Modifier,
    title: String?,
    leftAction: @Composable (() -> Unit?)?,
    rightAction: @Composable (() -> Unit?)?,
    backgroundColor: Color,
    cornerRadius: Dp,
    sheetMaxWidth: Dp,
    outsideMargin: DpSize,
    insideMargin: DpSize,
    defaultWindowInsetsPadding: Boolean,
    dragHandleColor: Color,
    windowHeight: Dp,
    statusBarHeight: Dp,
    sheetHeightPx: MutableState<Int>,
    dragOffsetY: Animatable<Float, *>,
    dimAlpha: MutableState<Float>,
    density: Density,
    onDismissRequest: (() -> Unit)?,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Calculate the overscroll offset for background fill
    val dragOffsetYValue by remember { derivedStateOf { dragOffsetY.value } }
    val overscrollOffsetPx by remember {
        derivedStateOf {
            (-dragOffsetYValue).coerceAtLeast(0f)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Background fill for the area revealed when dragging up (overscroll effect)
        if (overscrollOffsetPx > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .widthIn(max = sheetMaxWidth)
                    .fillMaxWidth()
                    .height(with(density) { overscrollOffsetPx.toDp() })
                    .padding(horizontal = outsideMargin.width)
                    .background(backgroundColor)
            )
        }

        Column(
            modifier = modifier
                .pointerInput(Unit) {
                    detectTapGestures { /* Consume click to prevent dismissal */ }
                }
                .widthIn(max = sheetMaxWidth)
                .fillMaxWidth()
                .wrapContentHeight()
                .heightIn(max = windowHeight - statusBarHeight)
                .onGloballyPositioned { coordinates ->
                    sheetHeightPx.value = coordinates.size.height
                }
                .graphicsLayer {
                    translationY = dragOffsetY.value
                }
                .then(
                    if (defaultWindowInsetsPadding)
                        Modifier.imePadding()
                    else
                        Modifier
                )
                .padding(horizontal = outsideMargin.width)
                .clip(G2RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius))
                .background(backgroundColor)
                .padding(horizontal = insideMargin.width)
                .padding(bottom = insideMargin.height)
        ) {
            // Drag handle area
            DragHandleArea(
                dragHandleColor = dragHandleColor,
                windowHeight = windowHeight,
                sheetHeightPx = sheetHeightPx,
                dragOffsetY = dragOffsetY,
                dimAlpha = dimAlpha,
                density = density,
                coroutineScope = coroutineScope,
                onDismissRequest = onDismissRequest
            )

            // Title and actions
            TitleAndActionsRow(
                title = title,
                leftAction = leftAction,
                rightAction = rightAction
            )

            // Content
            content()
        }
    }
}

@Composable
private fun DragHandleArea(
    dragHandleColor: Color,
    windowHeight: Dp,
    sheetHeightPx: MutableState<Int>,
    dragOffsetY: Animatable<Float, *>,
    dimAlpha: MutableState<Float>,
    density: Density,
    coroutineScope: CoroutineScope,
    onDismissRequest: (() -> Unit)?
) {
    val dragStartOffset = remember { mutableFloatStateOf(0f) }
    val isPressing = remember { mutableFloatStateOf(0f) }
    val pressScale = remember { Animatable(1f) }
    val pressWidth = remember { Animatable(45f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {
                        coroutineScope.launch {
                            dragStartOffset.floatValue = dragOffsetY.value
                            dragOffsetY.snapTo(dragOffsetY.value)
                            // Animate press effect
                            isPressing.floatValue = 1f
                            launch {
                                pressScale.animateTo(
                                    targetValue = 1.15f,
                                    animationSpec = tween(durationMillis = 100)
                                )
                            }
                            launch {
                                pressWidth.animateTo(
                                    targetValue = 55f,
                                    animationSpec = tween(durationMillis = 100)
                                )
                            }
                        }
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            // Reset press effect
                            isPressing.floatValue = 0f
                            launch {
                                pressScale.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(durationMillis = 150)
                                )
                            }
                            launch {
                                pressWidth.animateTo(
                                    targetValue = 45f,
                                    animationSpec = tween(durationMillis = 150)
                                )
                            }

                            val currentOffset = dragOffsetY.value
                            val dragDelta = currentOffset - dragStartOffset.floatValue

                            when {
                                // Dragged down significantly -> dismiss
                                dragDelta > 150f -> {
                                    onDismissRequest?.invoke()
                                    val windowHeightPx = windowHeight.value * density.density
                                    dragOffsetY.animateTo(
                                        targetValue = windowHeightPx,
                                        animationSpec = tween(durationMillis = 250)
                                    )
                                }
                                // Reset to original position (including overscroll bounce back)
                                else -> {
                                    dragOffsetY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = tween(durationMillis = 250)
                                    )
                                    dimAlpha.value = 1f
                                }
                            }
                        }
                    },
                    onVerticalDrag = { _, dragAmount ->
                        coroutineScope.launch {
                            val newOffset = dragOffsetY.value + dragAmount

                            // Apply damping effect when dragging upward (negative offset)
                            val finalOffset = if (newOffset < 0) {
                                // Overscroll effect: reduce drag amount with damping
                                val dampingFactor = 0.1f // Adjust this value for more/less resistance
                                val dampedAmount = dragAmount * dampingFactor
                                (dragOffsetY.value + dampedAmount).coerceAtMost(0f)
                            } else {
                                // Normal drag downward
                                newOffset
                            }

                            dragOffsetY.snapTo(finalOffset)

                            // Update dim alpha based on downward drag only
                            val thresholdPx = if (sheetHeightPx.value > 0) sheetHeightPx.value.toFloat() else 500f
                            val alpha = if (finalOffset >= 0) {
                                // Dragging down - reduce alpha
                                1f - (finalOffset / thresholdPx).coerceIn(0f, 1f)
                            } else {
                                // Dragging up or at base position - keep alpha at 1
                                1f
                            }
                            dimAlpha.value = alpha
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Drag handle indicator
        val handleAlpha = lerp(0.2f, 0.35f, isPressing.floatValue)

        Box(
            modifier = Modifier
                .width(pressWidth.value.dp)
                .height(4.dp)
                .graphicsLayer {
                    scaleY = pressScale.value
                }
                .clip(G2RoundedCornerShape(2.dp))
                .background(dragHandleColor.copy(alpha = handleAlpha))
        )
    }
}

@Composable
private fun TitleAndActionsRow(
    title: String?,
    leftAction: @Composable (() -> Unit?)?,
    rightAction: @Composable (() -> Unit?)?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 12.dp)
    ) {
        // left action (e.g. close button)
        Box(modifier = Modifier.align(Alignment.CenterStart)) {
            leftAction?.invoke()
        }

        // title text
        title?.let {
            Text(
                text = it,
                modifier = Modifier.align(Alignment.Center),
                fontSize = MiuixTheme.textStyles.title4.fontSize,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = MiuixTheme.colorScheme.onSurface
            )
        }

        // right action (e.g. submit button)
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            rightAction?.invoke()
        }
    }
}

object SuperBottomSheetDefaults {

    /**
     * The default background color of the [SuperBottomSheet].
     */
    @Composable
    fun backgroundColor() = MiuixTheme.colorScheme.surface

    /**
     * The default color of the drag handle.
     */
    @Composable
    fun dragHandleColor() = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.2f)

    /**
     * The default corner radius of the [SuperBottomSheet].
     */
    val cornerRadius = 28.dp

    /**
     * The default maximum width of the [SuperBottomSheet].
     */
    val maxWidth = 640.dp

    /**
     * The default margin outside the [SuperBottomSheet].
     */
    val outsideMargin = DpSize(0.dp, 0.dp)

    /**
     * The default margin inside the [SuperBottomSheet].
     */
    val insideMargin = DpSize(24.dp, 0.dp)
}