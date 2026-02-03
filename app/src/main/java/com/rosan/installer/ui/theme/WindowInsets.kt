package com.rosan.installer.ui.theme

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.only

val WindowInsets.Companion.none: WindowInsets
    get() = WindowInsets(0)

fun WindowInsets.exclude(sides: WindowInsetsSides): WindowInsets =
    this.exclude(this.only(sides))