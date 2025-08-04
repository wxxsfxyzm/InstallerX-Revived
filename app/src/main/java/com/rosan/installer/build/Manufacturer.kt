package com.rosan.installer.build

enum class Manufacturer {
    UNKNOWN,
    GOOGLE,
    HUAWEI,
    HONOR,
    OPPO,
    VIVO,
    XIAOMI,
    ONEPLUS,
    REALME,
    SAMSUNG,
    SONY,
    ASUS,
    MOTOROLA,
    NOKIA,
    LG,
    ZTE,
    LENOVO,
    MEIZU,
    SMARTISAN,
    BLACKSHARK;

    companion object {
        /**
         * Finds a Manufacturer enum constant from a string, ignoring case.
         * This centralizes the lookup logic within the enum itself.
         * @param manufacturerString The manufacturer string, typically from Build.MANUFACTURER.
         * @return The matching Manufacturer enum, or UNKNOWN if no match is found.
         */
        fun from(manufacturerString: String): Manufacturer {
            // Use values().find for a clean, scalable, and case-insensitive search.
            return entries.find { it.name == manufacturerString.uppercase() } ?: UNKNOWN
        }
    }
}