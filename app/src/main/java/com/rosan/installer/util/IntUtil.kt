package com.rosan.installer.util

fun Int.hasFlag(flag: Int) = flag and this == flag
fun Int.addFlag(flag: Int) = this or flag
fun Int.removeFlag(flag: Int) = this and flag.inv()
