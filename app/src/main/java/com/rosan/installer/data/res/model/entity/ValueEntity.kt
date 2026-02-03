package com.rosan.installer.data.res.model.entity

data class ValueEntity(
    val type: Type,
    val data: Int
) : Entry {
    enum class Type(val value: UByte) {
        NULL(0x00u),
        REFERENCE(0x01u),
        ATTRIBUTE(0x02u),
        STRING(0x03u),
        FLOAT(0x04u),
        DIMENSION(0x05u),
        FRACTION(0x06u),
        INT_DEC(0x10u),
        INT_HEX(0x11u),
        INT_BOOLEAN(0x12u),
        INT_COLOR_ARGB8(0x1cu),
        INT_COLOR_RGB8(0x1du),
        INT_COLOR_ARGB4(0x1eu),
        INT_COLOR_RGB4(0x1fu);

        companion object {
            const val FIRST_INT: UByte = 0x10u

            const val FIRST_COLOR_INT: UByte = 0x1cu

            const val LAST_COLOR_INT: UByte = 0x1fu

            const val LAST_INT: UByte = 0x1fu

            fun build(value: UByte): Type {
                return entries.find { it.value == value } ?: NULL
            }
        }
    }
}
