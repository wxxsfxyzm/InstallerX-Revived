// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.core.resParser.parser

import android.content.res.XmlResourceParser

interface AxmlTreeParser {
    companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }

    fun register(path: String, action: XmlResourceParser.() -> Unit): AxmlTreeParser

    fun unregister(path: String): AxmlTreeParser

    fun map(action: XmlResourceParser.(path: String) -> Unit)
}