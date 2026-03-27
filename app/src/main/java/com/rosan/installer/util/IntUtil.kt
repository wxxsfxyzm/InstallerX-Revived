// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.util

fun Int.hasFlag(flag: Int) = (this and flag) == flag
fun Int.addFlag(flag: Int) = this or flag
fun Int.removeFlag(flag: Int) = this and flag.inv()
