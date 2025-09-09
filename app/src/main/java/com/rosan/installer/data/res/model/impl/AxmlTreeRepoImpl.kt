package com.rosan.installer.data.res.model.impl

import android.content.res.XmlResourceParser
import com.rosan.installer.data.res.repo.AxmlTreeRepo
import org.koin.core.component.KoinComponent
import org.xmlpull.v1.XmlPullParser

class AxmlTreeRepoImpl(private val xmlPull: XmlResourceParser, private val rootPath: String = "") :
    AxmlTreeRepo, KoinComponent {

    private val names = mutableListOf<String>()

    private val registers = mutableMapOf<String, XmlResourceParser.() -> Unit>()

    override fun register(path: String, action: XmlResourceParser.() -> Unit): AxmlTreeRepo {
        registers[path] = action
        return this
    }

    override fun unregister(path: String): AxmlTreeRepo {
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
                    registers.map {
                        if (it.key == path) {
                            it.value.let { xmlPull.it() }
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