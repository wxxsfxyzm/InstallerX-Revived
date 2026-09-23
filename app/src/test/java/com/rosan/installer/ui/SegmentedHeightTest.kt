package com.rosan.installer.ui

import com.rosan.installer.ui.page.main.widget.setting.segmentedHeightAdvances
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SegmentedHeightTest {
    @Test
    fun longListSharesOneReboundBudget() {
        for (count in listOf(1, 29, 100)) {
            val heights = FloatArray(count) { 80f }
            val expanded = segmentedHeightAdvances(heights, FloatArray(count) { 1.2f }, 24f).sumOf { it.toDouble() }
            val collapsed = segmentedHeightAdvances(heights, FloatArray(count) { -0.2f }, 24f).sumOf { it.toDouble() }
            assertTrue(expanded > heights.sum())
            assertTrue(expanded <= heights.sum() + 24.01f)
            assertTrue(collapsed > 0f && collapsed <= 24.01f)
        }
    }

    @Test
    fun collapseNeverPullsFooterAboveHeaderEvenWithMixedSpringPhases() {
        val advances = segmentedHeightAdvances(
            floatArrayOf(72f, 80f, 80f, 80f, 60f),
            floatArrayOf(1f, -0.15f, 0.2f, 1.1f, 1f),
            24f,
        )
        assertEquals(72f, advances.first(), 0f)
        assertEquals(60f, advances.last(), 0f)
        assertTrue(advances.all { it >= 0f })
    }

    @Test
    fun endpointsAreExactAndJoinWithoutAVelocityDiscontinuity() {
        fun height(progress: Float) = segmentedHeightAdvances(floatArrayOf(2400f), floatArrayOf(progress), 24f)[0]
        assertEquals(0f, height(0f), 0f)
        assertEquals(2400f, height(1f), 0f)
        val epsilon = 0.0001f
        for (endpoint in listOf(0f, 1f)) {
            assertTrue(kotlin.math.abs(height(endpoint - epsilon) - height(endpoint)) < 0.001f)
            assertTrue(kotlin.math.abs(height(endpoint + epsilon) - height(endpoint)) < 0.001f)
        }
    }
}
