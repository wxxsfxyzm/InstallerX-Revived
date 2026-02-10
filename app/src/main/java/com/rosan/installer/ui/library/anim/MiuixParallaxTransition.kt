package app.hypershell.ui.library.navigation.anim

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A MIUI-style parallax navigation transition.
 *
 * This transition applies a parallax offset to the background screen and slides the foreground screen
 * on top of it. It also applies a corner radius to the foreground screen during the animation
 * and manages a dimming scrim on the background.
 *
 * @property cornerRadius The corner radius applied to the foreground screen during animation.
 */
class MiuixParallaxTransition(
    private val cornerRadius: Dp = 20.dp,
) : HyperStackTransition {

    companion object {
        /**
         * The factor by which the background screen is offset relative to the foreground screen's movement.
         */
        private const val PARALLAX_FACTOR = 0.25f

        /**
         * The maximum alpha value of the background scrim.
         */
        private const val MAX_SCRIM_ALPHA = 0.5f
    }

    /**
     * Custom spring easing for the harmonic oscillator effect, providing a natural "snap" feel.
     */
    private val springEasing = object : Easing {
        override fun transform(fraction: Float): Float {
            if (fraction <= 0f) return 0f
            if (fraction >= 1f) return 1f

            val response = 0.8f
            val damping = 0.95f
            val omega = 2.0 * PI / response
            val k = omega * omega
            val c = damping * 4.0 * PI / response

            val w = (sqrt(4.0 * k - c * c) / 2.0).toFloat()
            val r = (-c / 2.0).toFloat()
            val c2 = r / w

            val t = fraction.toDouble()
            val decay = exp(r * t)
            return (decay * (-cos(w * t) + c2 * sin(w * t)) + 1.0).toFloat()
        }
    }

    override val enterAnimationSpec: FiniteAnimationSpec<Float> = tween(
        durationMillis = 500,
        easing = springEasing
    )

    override val exitAnimationSpec: FiniteAnimationSpec<Float> = tween(
        durationMillis = 500,
        easing = springEasing
    )

    override fun Modifier.transformBase(state: HyperAnimState, size: IntSize): Modifier = this.graphicsLayer {
        if (!state.isOverlayVisible) {
            translationX = 0f
            return@graphicsLayer
        }

        val widthPx = size.width.toFloat()
        val maxParallaxOffset = -widthPx * PARALLAX_FACTOR

        translationX = when {
            // Case A: Gesture or Exit (Linear mix)
            state.gestureProgress > 0f || state.isPostCommit -> {
                val effectiveProgress = if (state.isPostCommit) {
                    state.gestureProgress + (1f - state.gestureProgress) * state.exitProgress
                } else {
                    state.gestureProgress
                }
                maxParallaxOffset * (1f - effectiveProgress)
            }
            // Case B: Entering (Driven by spring spec)
            state.isEntering -> maxParallaxOffset * state.enterProgress
            // Case C: Stable
            else -> maxParallaxOffset
        }
        transformOrigin = TransformOrigin.Center
    }

    override fun Modifier.transformOverlay(state: HyperAnimState, size: IntSize): Modifier {
        val isAnimating = state.enterProgress > 0f || state.gestureProgress > 0f || abs(state.exitProgress) > 0f
        val currentShape = if (isAnimating || state.isEntering) RoundedCornerShape(cornerRadius) else RectangleShape

        return this
            .clip(currentShape)
            .graphicsLayer {
                val widthPx = size.width.toFloat()

                translationX = when {
                    state.isEntering -> widthPx * (1f - state.enterProgress)
                    state.gestureProgress > 0f || state.isPostCommit -> {
                        val effectiveProgress = if (state.isPostCommit) {
                            state.gestureProgress + (1f - state.gestureProgress) * state.exitProgress
                        } else {
                            state.gestureProgress
                        }
                        widthPx * effectiveProgress
                    }

                    else -> 0f
                }

                shadowElevation = if (translationX > 0f && translationX < widthPx) 6.dp.toPx() else 0f
            }
    }

    override fun scrimAlpha(state: HyperAnimState): Float {
        val progress = when {
            state.isEntering -> state.enterProgress
            state.isPostCommit -> {
                val combined = state.gestureProgress + (1f - state.gestureProgress) * state.exitProgress
                1f - combined
            }

            else -> 1f - state.gestureProgress
        }
        return MAX_SCRIM_ALPHA * progress.coerceIn(0f, 1f)
    }
}
