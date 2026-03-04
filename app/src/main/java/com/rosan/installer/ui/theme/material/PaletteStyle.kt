// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.theme.material

enum class PaletteStyle(
    val displayName: String,
    val desc: String = ""
) {
    TonalSpot("Tonal Spot"),
    Neutral("Neutral"),
    Vibrant("Vibrant"),
    Expressive("Expressive"),
    Rainbow("Rainbow"),
    FruitSalad("FruitSalad"),
    Monochrome("Monochrome"),
    Fidelity("Fidelity"),
    Content("Content");

    val supportsSpec2025: Boolean
        get() = this == TonalSpot ||
                this == Neutral ||
                this == Vibrant ||
                this == Expressive
}
