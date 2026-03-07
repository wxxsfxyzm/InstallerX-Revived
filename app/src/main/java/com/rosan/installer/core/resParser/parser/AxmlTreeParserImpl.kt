// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.core.resParser.parser

import android.content.res.XmlResourceParser
import org.xmlpull.v1.XmlPullParser

class AxmlTreeParserImpl(
    private val xmlPull: XmlResourceParser,
    private val rootPath: String = ""
) : AxmlTreeParser {

    private val names = mutableListOf<String>()

    private val registers = mutableMapOf<String, XmlResourceParser.() -> Unit>()

    override fun register(path: String, action: XmlResourceParser.() -> Unit): AxmlTreeParser {
        registers[path] = action
        return this
    }

    override fun unregister(path: String): AxmlTreeParser {
        registers.remove(path)
        return this
    }

    private fun getCurrentPath(): String {
        return "$rootPath/${names.joinToString("/")}"
    }

    override fun map(action: XmlResourceParser.(path: String) -> Unit) {
        val startDepth = xmlPull.depth
        while (xmlPull.depth >= startDepth) {
            when (xmlPull.next()) {
                XmlPullParser.START_TAG -> {
                    val namespace = xmlPull.namespace
                    val name: String? = xmlPull.name
                    if (namespace.isNullOrEmpty()) names.add("$name")
                    else names.add("$namespace:$name")
                    val path = getCurrentPath()
                    registers.forEach { (regPath, regAction) ->
                        if (regPath == path) {
                            xmlPull.regAction()
                        }
                    }
                    xmlPull.action(path)
                }

                XmlPullParser.END_TAG -> {
                    names.removeLastOrNull()
                }

                XmlPullParser.END_DOCUMENT -> break
            }
        }
    }
}