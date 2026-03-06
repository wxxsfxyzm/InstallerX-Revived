// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.core.resParser.parser

import android.content.res.XmlResourceParser
import android.util.TypedValue

interface AxmlPullParser : XmlResourceParser {
    fun getAttributeTypedValue(index: Int): TypedValue?
}