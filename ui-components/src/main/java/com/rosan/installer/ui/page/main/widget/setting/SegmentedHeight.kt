package com.rosan.installer.ui.page.main.widget.setting

import kotlin.math.expm1

/**
 * Nonnegative layout advances with a single, soft rebound budget for the whole group.
 * Smoothstep makes velocity zero at the endpoints. Beyond either endpoint the
 * quadratic displacement joins with zero slope, preserving a small spring rebound
 * without a hard clamp or negative heights pulling subsequent content backwards.
 */
internal fun segmentedHeightAdvances(
    heights: FloatArray,
    progresses: FloatArray,
    reboundLimit: Float,
): FloatArray {
    require(heights.size == progresses.size)
    require(reboundLimit > 0f)
    val overflow = FloatArray(heights.size)
    val advances = FloatArray(heights.size)
    var totalOverflow = 0.0
    heights.indices.forEach { index ->
        val height = heights[index]
        val progress = progresses[index]
        val within = progress.coerceIn(0f, 1f)
        advances[index] = height * within * within * (3f - 2f * within)
        val beyond = progress - within
        overflow[index] = height * 3f * beyond * beyond
        totalOverflow += overflow[index]
    }
    if (totalOverflow > 0.0) {
        // expm1 retains precision for tiny displacements near the endpoint.
        val rebound = -reboundLimit * expm1(-totalOverflow / reboundLimit)
        val scale = (rebound / totalOverflow).toFloat()
        advances.indices.forEach { advances[it] += overflow[it] * scale }
    }
    return advances
}
