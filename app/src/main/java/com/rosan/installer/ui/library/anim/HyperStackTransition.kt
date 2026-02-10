package app.hypershell.ui.library.navigation.anim

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize

/**
 * Defines the visual behavior and animation specifications for stack-based navigation transitions.
 *
 * Implementations of this interface control how the incoming and outgoing screens are transformed,
 * as well as the behavior of any associated overlays or scrims.
 */
@Stable
interface HyperStackTransition {

    /**
     * The animation specification used for the entering phase of the transition.
     */
    val enterAnimationSpec: FiniteAnimationSpec<Float>

    /**
     * The animation specification used for the exiting (post-commit) phase of the transition.
     */
    val exitAnimationSpec: FiniteAnimationSpec<Float>

    /**
     * Applies visual transformations to the base content (the screen itself) based on the current [state].
     *
     * @param state The current animation and gesture state.
     * @param size The size of the layout being transformed.
     * @return A [Modifier] with the applied transformations.
     */
    fun Modifier.transformBase(state: HyperAnimState, size: IntSize): Modifier

    /**
     * Applies visual transformations to the overlay content (e.g., a shared element or top layer) based on the [state].
     *
     * @param state The current animation and gesture state.
     * @param size The size of the layout being transformed.
     * @return A [Modifier] with the applied transformations.
     */
    fun Modifier.transformOverlay(state: HyperAnimState, size: IntSize): Modifier

    /**
     * Calculates the alpha transparency for the navigation scrim based on the current [state].
     *
     * @param state The current animation and gesture state.
     * @return The alpha value, typically between 0f (transparent) and 1f (opaque).
     */
    fun scrimAlpha(state: HyperAnimState): Float
}
