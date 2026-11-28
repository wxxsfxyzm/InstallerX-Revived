// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.color

/** Palette styles supported by the dynamic color engine. */
enum class ColorPaletteStyle {
    TonalSpot,
    Neutral,
    Vibrant,
    Expressive,
    Rainbow,
    FruitSalad,
    Monochrome,
    Fidelity,
    Content,
    Cmf,
}

/** Material color specification versions supported by the dynamic color engine. */
enum class ColorSpecVersion {
    Spec2021,
    Spec2025,
    Spec2026,
}

/** How the light or dark appearance is chosen. */
enum class ColorThemeMode {
    System,
    Light,
    Dark,
}
