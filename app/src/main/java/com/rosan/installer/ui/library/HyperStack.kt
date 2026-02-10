package app.hypershell.ui.library.navigation

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.hypershell.ui.library.navigation.anim.HyperAnimState
import app.hypershell.ui.library.navigation.anim.HyperStackTransition
import app.hypershell.ui.library.navigation.anim.StandardParallaxTransition
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * A stack-based navigation component that supports overlays with smooth transitions and predictive back gestures.
 *
 * `HyperStack` manages a "base" layer and an "overlay" layer. When a sub-route is active for the given
 * [parentRoute], the corresponding destination from [destinations] is displayed in the overlay layer
 * using the provided [transition].
 *
 * @param parentRoute The route identifier of the parent destination this stack belongs to.
 * @param contentPadding Padding values to be applied to the base content.
 * @param transition The visual transition behavior to use for entering and exiting the overlay.
 * @param scrimColor The color of the dimming scrim applied to the base content when an overlay is visible.
 * @param baseContent The composable content of the base (bottom) layer.
 * @param destinations A map of sub-route keys to their respective composable content. Each destination
 * receives an `onBack` callback to trigger dismissal.
 */
@Composable
fun HyperStack(
    parentRoute: String,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    transition: HyperStackTransition = remember {
        StandardParallaxTransition(overlayShape = RoundedCornerShape(28.dp))
    },
    scrimColor: Color = Color.Black,
    baseContent: @Composable (PaddingValues) -> Unit,
    destinations: Map<String, @Composable (onBack: () -> Unit) -> Unit>
) {
    val controller = LocalHyperNavController.current
    val activeSubRoute = controller.getActiveSubRoute(parentRoute)

    HyperStackEngine(
        visibleState = activeSubRoute,
        transition = transition,
        scrimColor = scrimColor,
        onDismissRequest = { controller.popStack(parentRoute) },
        baseContent = { baseContent(contentPadding) },
        overlayContent = { routeKey ->
            destinations[routeKey]?.invoke { controller.popStack(parentRoute) }
        }
    )
}

/**
 * Internal state machine driving three types of animations for the navigation stack:
 * 1. **Enter**: Triggered when `visibleState` goes from null to non-null.
 * 2. **Exit (Programmatic)**: Triggered when `visibleState` goes from non-null back to null.
 * 3. **Exit (Gesture)**: Driven by the Android Predictive Back gesture (swipe to dismiss).
 *
 * The engine maps all animation progress (entering, dragging, exiting) into a unified [HyperAnimState]
 * which is then used by the [transition] to apply visual transformations.
 *
 * @param visibleState The key of the currently visible overlay, or null if none.
 * @param transition The transition implementation defining the visual logic.
 * @param scrimColor The color of the background scrim.
 * @param onDismissRequest Callback invoked when the overlay should be closed (e.g., via gesture or programmatic back).
 * @param baseContent The content to render in the background.
 * @param overlayContent The content to render in the foreground overlay, given the current state key.
 */
@Composable
private fun <T> HyperStackEngine(
    visibleState: T?,
    transition: HyperStackTransition,
    scrimColor: Color,
    onDismissRequest: () -> Unit,
    baseContent: @Composable () -> Unit,
    overlayContent: @Composable (T) -> Unit
) {
    val scope = rememberCoroutineScope()

    // Independent animatables for different phases
    val gestureProgress = remember { Animatable(0f) }
    val exitProgress = remember { Animatable(0f) }
    val enterProgress = remember { Animatable(0f) }

    var interruptOffset by remember { mutableFloatStateOf(0f) }
    var isEntering by remember { mutableStateOf(false) }
    var currentOverlayData by remember { mutableStateOf<T?>(null) }
    var lastGestureSide by remember { mutableFloatStateOf(1f) }

    // ========================================================
    // 1. State Transition Logic
    // ========================================================
    LaunchedEffect(visibleState) {
        if (visibleState != null) {
            // >>> Enter Phase <<<
            interruptOffset = 0f

            // Handle interrupt: if exiting, reverse to enter
            val currentExit = exitProgress.value
            val startEnterValue = if (currentExit > 0f) 1f - currentExit else 0f

            // Reset other states
            exitProgress.snapTo(0f)
            gestureProgress.snapTo(0f)
            currentOverlayData = visibleState
            isEntering = true
            enterProgress.snapTo(startEnterValue)

            try {
                enterProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = transition.enterAnimationSpec
                )
            } catch (e: CancellationException) {
                // Expected when interrupted by PredictiveBack
            } finally {
                // Animation finished or interrupted
            }
        } else {
            // >>> Exit Phase (Programmatic) <<<

            // Handle interrupt: if entering, snap to equivalent exit position
            val currentEnter = enterProgress.value
            if (currentEnter > 0f && currentEnter < 1f) {
                val mappedStartExit = 1f - currentEnter
                exitProgress.snapTo(mappedStartExit)
                enterProgress.snapTo(1f)
            }

            if (currentOverlayData != null) {
                try {
                    isEntering = false
                    exitProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = transition.exitAnimationSpec
                    )
                } finally {
                    // Cleanup after exit completes
                    currentOverlayData = null
                    exitProgress.snapTo(0f)
                    gestureProgress.snapTo(0f)
                    interruptOffset = 0f
                }
            }
        }
    }

    // ========================================================
    // 2. Predictive Back Gesture
    // ========================================================

    // Constants for gesture rewind (simulating MIUI behavior)
    val totalEnterDuration = 400
    val minRewindDuration = 100

    PredictiveBackHandler(enabled = currentOverlayData != null && visibleState != null) { progressFlow ->
        // 1. Interrupt Logic: Capture current state on gesture start
        if (isEntering) {
            val currentEnter = enterProgress.value
            enterProgress.stop()
            interruptOffset = 1f - currentEnter
            gestureProgress.snapTo(interruptOffset)
            enterProgress.snapTo(1f)
            isEntering = false
        } else {
            interruptOffset = 0f
        }

        try {
            progressFlow.collect { backEvent ->
                val side = if (backEvent.swipeEdge == 0) 1f else -1f
                if (lastGestureSide != side) {
                    lastGestureSide = side
                }

                // 2. Linear Tracking
                // Map gesture progress directly to visual progress
                val targetProgress = interruptOffset + (backEvent.progress * (1f - interruptOffset))
                gestureProgress.snapTo(targetProgress)
            }
            // Gesture confirmed -> Trigger dismiss
            onDismissRequest()
        } catch (e: CancellationException) {
            // 3. Gesture Cancelled (Rewind)
            // Logic: Scale duration based on remaining distance to maintain velocity feel

            val currentGestureVal = gestureProgress.value

            val animDuration = (currentGestureVal * totalEnterDuration).toInt()
                .coerceAtLeast(minRewindDuration)

            scope.launch {
                // Handoff back to Enter logic if needed, or just rewind gesture
                // Here we treat rewind as "resuming enter" visually

                val resumeEnterVal = 1f - currentGestureVal
                enterProgress.snapTo(resumeEnterVal)
                gestureProgress.snapTo(0f)
                interruptOffset = 0f
                isEntering = true

                enterProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = animDuration,
                        easing = LinearEasing // Linear maintains the gesture velocity
                    )
                )
                isEntering = false
            }
        }
    }

    // ========================================================
    // 3. Rendering
    // ========================================================
    val animState = HyperAnimState(
        gestureProgress = gestureProgress.value,
        exitProgress = exitProgress.value,
        enterProgress = enterProgress.value,
        isEntering = isEntering,
        lastGestureSide = lastGestureSide,
        isOverlayVisible = currentOverlayData != null
    )

    val shouldRenderOverlay = currentOverlayData != null || abs(exitProgress.value) < 1f || isEntering

    with(transition) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val containerSize = IntSize(constraints.maxWidth, constraints.maxHeight)

            // Base Layer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0f)
                    .transformBase(animState, containerSize)
            ) {
                baseContent()
            }

            // Scrim Layer
            if (shouldRenderOverlay && (currentOverlayData != null || animState.isPostCommit || gestureProgress.value > 0f || isEntering)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(scrimColor.copy(alpha = scrimAlpha(animState)))
                        .zIndex(1f)
                )
            }

            // Overlay Layer
            if (shouldRenderOverlay) {
                currentOverlayData?.let { data ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(2f)
                            .transformOverlay(animState, containerSize)
                    ) {
                        overlayContent(data)
                    }
                }
            }
        }
    }
}
