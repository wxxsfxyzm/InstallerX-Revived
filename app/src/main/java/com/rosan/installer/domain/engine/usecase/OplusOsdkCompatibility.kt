// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.usecase

internal fun isOplusOsdkIncompatible(required: String?, device: String?): Boolean {
    fun String.parseVersion(): List<Long>? = split('.').map { part ->
        part.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }?.toLongOrNull() ?: return null
    }

    val requiredParts = required?.parseVersion() ?: return false
    val deviceParts = device?.parseVersion() ?: return false
    for (index in 0 until maxOf(requiredParts.size, deviceParts.size)) {
        val requiredPart = requiredParts.getOrElse(index) { 0L }
        val devicePart = deviceParts.getOrElse(index) { 0L }
        if (devicePart != requiredPart) return devicePart < requiredPart
    }
    return false
}
