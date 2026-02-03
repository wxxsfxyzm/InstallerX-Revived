package com.rosan.installer.data.res.repo

import android.content.res.Configuration
import com.rosan.installer.data.res.model.entity.Entry

/**
 * @id = 0xPPTTEEEE
 * @valueId = 0xEEEE
 * @typeId = 0xTT
 * @packageId = 0xPP
 * */
interface ArscRepo {
    fun getValue(
        id: Int,
        configuration: Configuration,
        densityDpi: Int? = null
    ): Entry? = getValue(id2PackageId(id), id2TypeId(id), id2ValueId(id), configuration, densityDpi)

    fun getValue(
        packageId: Int,
        typeId: Int,
        valueId: Int,
        configuration: Configuration,
        densityDpi: Int? = null
    ): Entry?

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