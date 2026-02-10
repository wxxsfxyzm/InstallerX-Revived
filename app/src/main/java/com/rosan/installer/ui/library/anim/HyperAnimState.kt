package app.hypershell.ui.library.navigation.anim

import androidx.compose.runtime.Immutable
import kotlin.math.abs

/**
 * Represents the state of a navigation animation in HyperShell.
 *
 * @property gestureProgress The progress of a physical gesture, ranging from 0f to 1f.
 * @property exitProgress The progress of a programmatic exit transition, ranging from 0f to 1f.
 * @property enterProgress The progress of an entering transition, ranging from 0f to 1f.
 * @property isEntering Whether the transition is currently in the entering phase.
 * @property lastGestureSide Indicates which side the last gesture originated from: 1f for left edge, -1f for right edge.
 * @property isOverlayVisible Whether the navigation overlay (e.g., a scrim or background dim) should be visible.
 */
@Immutable
data class HyperAnimState(
    val gestureProgress: Float,
    val exitProgress: Float,
    val enterProgress: Float,
    val isEntering: Boolean,
    val lastGestureSide: Float,
    val isOverlayVisible: Boolean
) {
    /**
     * Determines if the transition is in the post-commit (exiting) phase.
     * Uses a small threshold to avoid floating point errors.
     */
    val isPostCommit: Boolean
        get() = abs(exitProgress) > 0.001f
}
