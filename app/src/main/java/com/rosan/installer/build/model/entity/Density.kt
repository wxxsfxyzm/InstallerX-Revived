package com.rosan.installer.build.model.entity

import android.content.res.Resources
import android.util.DisplayMetrics

/**
 * Enum for screen density buckets.
 */
enum class Density(val dpi: Int, val key: String) {
    LDPI(DisplayMetrics.DENSITY_LOW, "ldpi"),
    MDPI(DisplayMetrics.DENSITY_MEDIUM, "mdpi"),
    TVDPI(DisplayMetrics.DENSITY_TV, "tvdpi"),
    HDPI(DisplayMetrics.DENSITY_HIGH, "hdpi"),
    XHDPI(DisplayMetrics.DENSITY_XHIGH, "xhdpi"),
    XXHDPI(DisplayMetrics.DENSITY_XXHIGH, "xxhdpi"),
    XXXHDPI(DisplayMetrics.DENSITY_XXXHIGH, "xxxhdpi"),
    UNKNOWN(0, "unknown"); // Add unknown for safety

    companion object {
        private val map = entries.associateBy(Density::dpi)
        private val keyMap = entries.associateBy { it.key }

        fun from(dpiValue: Int): Density? {
            return map[dpiValue]
        }

        /**
         * Finds a Density enum constant that matches the given density key string.
         * @param keyString The density key (e.g., "xhdpi").
         * @return The corresponding Density enum, or UNKNOWN if no match is found.
         */
        fun fromDensityString(keyString: String?): Density {
            return keyMap[keyString?.lowercase()] ?: UNKNOWN
        }

        /**
         * Generates a prioritized list of densities for the current device.
         * It prefers densities >= the current device's DPI, then falls back to lower densities.
         * This logic replicates the behavior of Android's resource selection.
         */
        fun getPrioritizedList(): List<Density> {
            val allDpis = entries.map { it.dpi }
            val deviceDpi = Resources.getSystem().displayMetrics.densityDpi

            // Partition all known DPIs into two groups:
            // 1. Those that are greater than or equal to the device's DPI.
            // 2. Those that are lower than the device's DPI.
            val (higherOrEqual, lower) = allDpis.partition { it >= deviceDpi }

            // Sort the higher-or-equal group ascending (to find the smallest best match).
            // Sort the lower group descending (to find the largest worst match).
            val sortedDpis = higherOrEqual.sorted() + lower.sortedDescending()

            // Map the sorted integer DPI values back to our Density enum.
            return sortedDpis.mapNotNull { from(it) }
        }
    }
}
