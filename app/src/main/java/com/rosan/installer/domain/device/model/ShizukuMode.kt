package com.rosan.installer.domain.device.model

enum class ShizukuMode(val desc: String) {
    ROOT("Root"),
    SHELL("Shell"),
    NONE("");

    companion object {
        fun fromUid(uid: Int): ShizukuMode = when (uid) {
            0 -> ROOT
            2000 -> SHELL
            else -> NONE
        }
    }
}