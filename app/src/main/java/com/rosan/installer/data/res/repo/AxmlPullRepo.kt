package com.rosan.installer.data.res.repo

import android.content.res.XmlResourceParser
import android.util.TypedValue

interface AxmlPullRepo : XmlResourceParser {
    fun getAttributeTypedValue(index: Int): TypedValue?
}