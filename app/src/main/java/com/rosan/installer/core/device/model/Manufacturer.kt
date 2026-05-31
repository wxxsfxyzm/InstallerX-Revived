// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.core.device.model

enum class Manufacturer(
    val displayName: String
) {
    UNKNOWN("Unknown"),
    GOOGLE("Google"),
    HUAWEI("Huawei"),
    HONOR("Honor"),
    OPPO("OPPO"),
    VIVO("vivo"),
    XIAOMI("Xiaomi"),
    ONEPLUS("OnePlus"),
    REALME("realme"),
    SAMSUNG("Samsung"),
    SONY("Sony"),
    ASUS("ASUS"),
    MOTOROLA("Motorola"),
    NOKIA("Nokia"),
    LG("LG"),
    ZTE("ZTE"),
    LENOVO("Lenovo"),
    MEIZU("Meizu"),
    SMARTISAN("Smartisan"),
    BLACKSHARK("Black Shark");

    companion object {
        /**
         * Finds a Manufacturer enum constant from a string, ignoring case.
         *
         * @param manufacturerString The manufacturer string, typically from Build.MANUFACTURER.
         * @return The matching Manufacturer enum, or UNKNOWN if no match is found.
         */
        fun from(manufacturerString: String): Manufacturer =
            entries.find { it.name == manufacturerString.uppercase() } ?: UNKNOWN
    }
}