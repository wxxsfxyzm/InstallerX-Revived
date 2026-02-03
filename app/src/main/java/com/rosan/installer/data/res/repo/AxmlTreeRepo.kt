package com.rosan.installer.data.res.repo

import android.content.res.XmlResourceParser

interface AxmlTreeRepo {
    companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }

    fun register(path: String, action: XmlResourceParser.() -> Unit): AxmlTreeRepo

    fun unregister(path: String): AxmlTreeRepo

    fun map(action: XmlResourceParser.(path: String) -> Unit)
}