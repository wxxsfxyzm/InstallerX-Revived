package com.rosan.installer.util

fun Int.hasFlag(flag: Int) = (this and flag) == flag
fun Int.addFlag(flag: Int) = this or flag
fun Int.removeFlag(flag: Int) = this and flag.inv()
