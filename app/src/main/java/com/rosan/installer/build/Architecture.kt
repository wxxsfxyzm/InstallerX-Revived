package com.rosan.installer.build

// Enum for representing CPU architectures with their ABI string and a display name.
enum class Architecture(val arch: String, val displayName: String) {
    // Each constant now holds its technical ABI name and a user-friendly display name.
    ARMEABI("armeabi", "ARMEABI (32-bit)"),
    ARM("armeabi-v7a", "ARMv7a (32-bit)"),
    ARM64("arm64-v8a", "ARMv8a (64-bit)"),
    X86("x86", "x86 (32-bit)"),
    X86_64("x86_64", "x86 (64-bit)"),
    MIPS("mips", "MIPS (32-bit)"),
    MIPS64("mips64", "MIPS (64-bit)"),
    UNKNOWN("unknown", "Unknown");

    companion object {
        /**
         * Finds an Architecture enum constant that matches the given ABI string.
         * This function handles common aliases (e.g., with underscores).
         * @param archString The ABI string (e.g., "arm64-v8a" or "arm64_v8a").
         * @return The corresponding Architecture enum, or UNKNOWN if no match is found.
         */
        fun fromArchString(archString: String?): Architecture {
            // Normalize the input string for robust matching.
            return when (archString?.lowercase()) {
                "armeabi" -> ARMEABI
                "armeabi-v7a", "armeabi_v7a" -> ARM
                "arm64-v8a", "arm64_v8a" -> ARM64
                "x86" -> X86
                "x86_64" -> X86_64
                "mips" -> MIPS
                "mips64" -> MIPS64
                else -> UNKNOWN
            }
        }
    }
}