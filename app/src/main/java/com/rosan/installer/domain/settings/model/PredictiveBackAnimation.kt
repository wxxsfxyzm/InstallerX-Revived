// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.model

/**
 * Define Predictive Back Animation types
 */
enum class PredictiveBackAnimation(val value: String) {
    None("none"),
    AOSP("aosp"),
    Scale("scale"),
    KernelSUClassic("ksu_classic"),
    KernelSUOfficial("ksu_official");
}
