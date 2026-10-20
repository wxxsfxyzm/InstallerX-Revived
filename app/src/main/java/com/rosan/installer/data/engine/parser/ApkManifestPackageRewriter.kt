// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Rewrites manifest identity strings without changing component class names or resource IDs. */
internal object ApkManifestPackageRewriter {
    fun rewrite(manifest: ByteArray, oldPackage: String, newPackage: String): ByteArray {
        val xml = ByteBuffer.wrap(manifest.copyOf()).order(ByteOrder.LITTLE_ENDIAN)
        require(xml.shortAt(0) == 3 && xml.getInt(4) == manifest.size) { "Invalid binary Android manifest" }
        val poolOffset = xml.shortAt(2)
        require(xml.shortAt(poolOffset) == 1) { "Missing manifest string pool" }
        val poolSize = xml.getInt(poolOffset + 4)
        val strings = readStrings(xml, poolOffset)
        val indices = strings.withIndex().associate { it.value to it.index }.toMutableMap()
        fun index(value: String): Int = indices.getOrPut(value) {
            strings.add(value)
            strings.lastIndex
        }

        var packageChanged = false
        var offset = poolOffset + poolSize
        while (offset < manifest.size) {
            val size = xml.getInt(offset + 4)
            require(size >= 8 && size <= manifest.size - offset) { "Invalid manifest chunk" }
            if (xml.shortAt(offset) == 0x0102) {
                val extension = offset + xml.shortAt(offset + 2)
                val tag = strings[xml.getInt(extension + 4)]
                val attributeStart = extension + xml.shortAt(extension + 8)
                val attributeSize = xml.shortAt(extension + 10)
                val count = xml.shortAt(extension + 12)
                require(attributeSize >= 20 && attributeStart + count * attributeSize <= offset + size)
                repeat(count) { attributeIndex ->
                    val attribute = attributeStart + attributeIndex * attributeSize
                    val name = strings[xml.getInt(attribute + 4)]
                    if (xml.get(attribute + 15).toInt() == 3) {
                        val value = strings[xml.getInt(attribute + 16)]
                        val namespace = xml.getInt(attribute)
                        val android = namespace >= 0 && strings[namespace] == ANDROID_NAMESPACE
                        val replacement = when {
                            tag == "manifest" && namespace == -1 && name == "package" -> {
                                require(value == oldPackage) { "Unexpected APK package: $value" }
                                packageChanged = true
                                newPackage
                            }

                            android && name == "authorities" -> value.split(';').joinToString(";") {
                                replaceIdentity(it, oldPackage, newPackage)
                            }

                            android && (name in IDENTITY_ATTRIBUTES || (name == "name" && tag in PERMISSION_TAGS)) ->
                                replaceIdentity(value, oldPackage, newPackage)

                            android && (name in CLASS_ATTRIBUTES || (name == "name" && tag in COMPONENT_TAGS)) -> when {
                                value.startsWith('.') -> oldPackage + value
                                '.' !in value -> "$oldPackage.$value"
                                else -> value
                            }

                            else -> value
                        }
                        if (replacement != value) {
                            val replacementIndex = index(replacement)
                            if (xml.getInt(attribute + 8) != -1) xml.putInt(attribute + 8, replacementIndex)
                            xml.putInt(attribute + 16, replacementIndex)
                        }
                    }
                }
            }
            offset += size
        }
        require(packageChanged) { "Manifest has no package attribute" }
        val pool = writeStrings(xml, poolOffset, strings)
        val result = manifest.copyOfRange(0, poolOffset) + pool + xml.array().copyOfRange(poolOffset + poolSize, manifest.size)
        ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN).putInt(4, result.size)
        return result
    }

    private fun replaceIdentity(value: String, oldPackage: String, newPackage: String): String = when {
        value == oldPackage -> newPackage
        value.startsWith("$oldPackage.") || value.startsWith("$oldPackage:") -> newPackage + value.removePrefix(oldPackage)
        else -> value
    }

    private fun readStrings(xml: ByteBuffer, offset: Int): MutableList<String> {
        val headerSize = xml.shortAt(offset + 2)
        val count = xml.getInt(offset + 8)
        val start = offset + xml.getInt(offset + 20)
        val utf8 = xml.getInt(offset + 16) and 0x100 != 0
        return MutableList(count) { index ->
            val input = xml.duplicate().order(ByteOrder.LITTLE_ENDIAN)
            input.position(start + xml.getInt(offset + headerSize + index * 4))
            val bytes = if (utf8) {
                input.length8() // UTF-16 character count precedes the UTF-8 byte count.
                ByteArray(input.length8())
            } else {
                ByteArray(input.length16() * 2)
            }
            input.get(bytes)
            bytes.toString(if (utf8) Charsets.UTF_8 else Charsets.UTF_16LE)
        }
    }

    private fun writeStrings(xml: ByteBuffer, offset: Int, strings: List<String>): ByteArray {
        val headerSize = xml.shortAt(offset + 2)
        val styleCount = xml.getInt(offset + 12)
        val oldCount = xml.getInt(offset + 8)
        val oldStylesStart = xml.getInt(offset + 24)
        val oldSize = xml.getInt(offset + 4)
        val utf8 = xml.getInt(offset + 16) and 0x100 != 0
        val stringData = ByteArrayOutputStream()
        val offsets = strings.map { value ->
            val position = stringData.size()
            val bytes = value.toByteArray(if (utf8) Charsets.UTF_8 else Charsets.UTF_16LE)
            if (utf8) {
                stringData.length8(value.length)
                stringData.length8(bytes.size)
                stringData.write(bytes)
                stringData.write(0)
            } else {
                stringData.length16(value.length)
                stringData.write(bytes)
                stringData.write(byteArrayOf(0, 0))
            }
            position
        }
        while (stringData.size() % 4 != 0) stringData.write(0)
        val start = headerSize + (strings.size + styleCount) * 4
        val styles = if (oldStylesStart == 0) byteArrayOf() else xml.array().copyOfRange(offset + oldStylesStart, offset + oldSize)
        val result = ByteBuffer.allocate(start + stringData.size() + styles.size).order(ByteOrder.LITTLE_ENDIAN)
        result.put(xml.array(), offset, headerSize)
        result.putInt(4, result.capacity())
        result.putInt(8, strings.size)
        result.putInt(16, xml.getInt(offset + 16) and 1.inv()) // Appended strings are not sorted.
        result.putInt(20, start)
        result.putInt(24, if (styles.isEmpty()) 0 else start + stringData.size())
        offsets.forEach(result::putInt)
        repeat(styleCount) { result.putInt(xml.getInt(offset + headerSize + oldCount * 4 + it * 4)) }
        result.put(stringData.toByteArray()).put(styles)
        return result.array()
    }

    private fun ByteBuffer.shortAt(offset: Int): Int = getShort(offset).toInt() and 0xffff

    private fun ByteBuffer.length8(): Int {
        val first = get().toInt() and 0xff
        return if (first and 0x80 == 0) first else ((first and 0x7f) shl 8) or (get().toInt() and 0xff)
    }

    private fun ByteBuffer.length16(): Int {
        val first = short.toInt() and 0xffff
        return if (first and 0x8000 == 0) first else ((first and 0x7fff) shl 16) or (short.toInt() and 0xffff)
    }

    private fun ByteArrayOutputStream.length8(length: Int) {
        require(length <= 0x7fff)
        if (length > 0x7f) write((length shr 8) or 0x80)
        write(length and 0xff)
    }

    private fun ByteArrayOutputStream.length16(length: Int) {
        if (length > 0x7fff) {
            write((length shr 16) and 0xff)
            write((length shr 24) or 0x80)
        }
        write(length and 0xff)
        write((length shr 8) and 0xff)
    }

    private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    private val IDENTITY_ATTRIBUTES = setOf("permission", "readPermission", "writePermission", "taskAffinity", "process")
    private val CLASS_ATTRIBUTES = setOf("targetActivity", "parentActivityName", "backupAgent", "appComponentFactory", "manageSpaceActivity", "zygotePreloadName")
    private val COMPONENT_TAGS = setOf("application", "activity", "activity-alias", "service", "receiver", "provider", "instrumentation")
    private val PERMISSION_TAGS = setOf("permission", "permission-group", "permission-tree", "uses-permission", "uses-permission-sdk-23", "uses-permission-sdk-m")
}
