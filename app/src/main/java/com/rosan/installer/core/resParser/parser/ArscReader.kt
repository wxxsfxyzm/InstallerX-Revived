// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.core.resParser.parser

import android.content.res.Configuration
import com.rosan.installer.core.resParser.model.ArscEntry

/**
 * @id = 0xPPTTEEEE
 * @valueId = 0xEEEE
 * @typeId = 0xTT
 * @packageId = 0xPP
 * */
interface ArscReader {
    fun getValue(
        id: Int,
        configuration: Configuration,
        densityDpi: Int? = null
    ): ArscEntry? = getValue(id2PackageId(id), id2TypeId(id), id2ValueId(id), configuration, densityDpi)

    fun getValue(
        packageId: Int,
        typeId: Int,
        valueId: Int,
        configuration: Configuration,
        densityDpi: Int? = null
    ): ArscEntry?

    fun getValueName(
        id: Int,
        configuration: Configuration,
        densityDpi: Int? = null
    ): CharSequence? =
        getValueName(id2PackageId(id), id2TypeId(id), id2ValueId(id), configuration, densityDpi)

    fun getValueName(
        packageId: Int, typeId: Int, valueId: Int,
        configuration: Configuration,
        densityDpi: Int? = null
    ): CharSequence?

    fun getTypeName(packageId: Int, typeId: Int): CharSequence?

    fun getPackageName(packageId: Int): CharSequence?

    fun getString(index: Int): CharSequence?

    companion object {
        fun id2PackageId(id: Int): Int {
            return id shr 24
        }

        fun id2TypeId(id: Int): Int {
            return (id shr 16) % 0x100
        }

        fun id2ValueId(id: Int): Int {
            return id % 0x10000
        }
    }
}