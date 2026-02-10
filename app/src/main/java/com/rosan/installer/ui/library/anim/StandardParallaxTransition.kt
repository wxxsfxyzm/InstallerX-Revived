package app.hypershell.ui.library.navigation.anim

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max

/**
 * A standard parallax navigation transition.
 *
 * This transition scales down the background screen slightly and offsets it to create a parallax effect.
 * The foreground screen slides in over the background, optionally with a custom [overlayShape]
 * and shadow.
 *
 * @property overlayShape The shape of the foreground screen (overlay). Defaults to [RectangleShape].
 * @property edgeMargin The margin from the edge of the screen during gesture interactions.
 * @property shadowElevation The maximum elevation of the shadow cast by the foreground screen.
 */
class StandardParallaxTransition(
    private val overlayShape: Shape? = null,
    private val edgeMargin: Dp = 16.dp,
    private val shadowElevation: Dp = 6.dp
) : HyperStackTransition {

    companion object {
        private const val MAX_SCALE = 0.9f
        private const val PARALLAX_FACTOR = 0.25f
        private const val MAX_SCRIM_ALPHA = 0.4f
        private const val GESTURE_MOVEMENT_FACTOR = 0.3f // Base layer moves less during gesture
    }

    private val standardEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

    override val enterAnimationSpec: FiniteAnimationSpec<Float> = tween(
        durationMillis = 450,
        easing = standardEasing
    )

    override val exitAnimationSpec: FiniteAnimationSpec<Float> = tween(
        durationMillis = 450,
        easing = standardEasing
    )

    override fun Modifier.transformBase(state: HyperAnimState, size: IntSize): Modifier = this.graphicsLayer {
        val widthPx = size.width.toFloat()
        val maxParallaxOffset = -widthPx * PARALLAX_FACTOR

        val baseTranslationX = when {
            // Case 1: Entering
            state.isEntering -> maxParallaxOffset * state.enterProgress

            // Case 2: Post-Commit (Exit)
            state.isPostCommit -> {
                val lastGesturePos = maxParallaxOffset * (1f - GESTURE_MOVEMENT_FACTOR)
                lastGesturePos * (1f - abs(state.exitProgress))
            }

            // Case 3: Initial / Hidden
            !state.isOverlayVisible -> 0f

            // Case 4: Gesture Interaction
            else -> {
                val currentMovement = maxParallaxOffset * (state.gestureProgress * GESTURE_MOVEMENT_FACTOR)
                maxParallaxOffset - currentMovement
            }
        }

        translationX = baseTranslationX
        transformOrigin = TransformOrigin.Center
    }

    override fun Modifier.transformOverlay(state: HyperAnimState, size: IntSize): Modifier = this.graphicsLayer {
        val widthPx = size.width.toFloat()
        val marginPx = edgeMargin.toPx()
        val overlayScale = 1f - (1f - MAX_SCALE) * state.gestureProgress

        val naturalGap = widthPx * (1f - overlayScale) / 2f
        val marginShift = max(0f, naturalGap - marginPx) * state.lastGestureSide
        val enterTranslationX = widthPx * (1f - state.enterProgress)

        val overlayTranslationX = when {
            state.isPostCommit -> (widthPx + marginPx) * state.exitProgress
            state.isEntering -> enterTranslationX
            else -> marginShift
        }

        translationX = overlayTranslationX
        scaleX = overlayScale
        scaleY = overlayScale
        transformOrigin = TransformOrigin.Center

        // Shadow elevation logic
        val targetElevation = this@StandardParallaxTransition.shadowElevation.toPx()
        shadowElevation = when {
            state.isEntering -> targetElevation * state.enterProgress
            state.isPostCommit -> targetElevation * (1f - state.exitProgress)
            else -> targetElevation * (1f - state.gestureProgress) // Fade shadow during gesture
        }

        if (overlayShape != null) {
            this.clip = true
            this.shape = overlayShape
        } else {
            this.clip = false
            this.shape = RectangleShape
        }
    }

    override fun scrimAlpha(state: HyperAnimState): Float = when {
        state.isEntering -> MAX_SCRIM_ALPHA * state.enterProgress
        state.isPostCommit -> MAX_SCRIM_ALPHA * (1f - abs(state.exitProgress))
        else -> MAX_SCRIM_ALPHA * state.gestureProgress
    }
}
