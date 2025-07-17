package com.rosan.installer.build

enum class Architecture(val arch: String) {
    ARM("armeabi-v7a"),
    ARM64("arm64-v8a"),
    X86("x86"),
    X86_64("x86_64"),
    MIPS("mips"),
    MIPS64("mips64"),
    UNKNOWN("unknown");
}