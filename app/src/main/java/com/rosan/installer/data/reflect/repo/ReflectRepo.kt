package com.rosan.installer.data.reflect.repo

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Project-exclusive accessor for system private APIs.
 * Provides caching and automatic accessibility handling.
 */
interface ReflectRepo {
    // --- Cached Reflection Object Accessors ---

    fun getConstructor(clazz: Class<*>, vararg parameterTypes: Class<*>): Constructor<*>?

    fun getDeclaredConstructor(clazz: Class<*>, vararg parameterTypes: Class<*>): Constructor<*>?

    fun getField(clazz: Class<*>, name: String): Field?

    fun getDeclaredField(clazz: Class<*>, name: String): Field?

    fun getMethod(clazz: Class<*>, name: String, vararg parameterTypes: Class<*>): Method?

    fun getDeclaredMethod(clazz: Class<*>, name: String, vararg parameterTypes: Class<*>): Method?

    // --- High-level Property Accessors ---

    fun getFieldValue(obj: Any?, clazz: Class<*>, name: String): Any?

    fun setFieldValue(obj: Any?, clazz: Class<*>, name: String, value: Any?)

    fun getStaticFieldValue(clazz: Class<*>, name: String): Any?

    fun setStaticFieldValue(clazz: Class<*>, name: String, value: Any?)

    // --- High-level Method Invocation ---

    fun invokeMethod(
        obj: Any?,
        clazz: Class<*>,
        name: String,
        parameterTypes: Array<Class<*>> = emptyArray(),
        vararg args: Any?
    ): Any?

    fun invokeStaticMethod(
        clazz: Class<*>,
        name: String,
        parameterTypes: Array<Class<*>> = emptyArray(),
        vararg args: Any?
    ): Any?

    // --- Legacy / Bulk Accessors ---

    fun getConstructors(clazz: Class<*>): Array<Constructor<*>>

    fun getDeclaredConstructors(clazz: Class<*>): Array<Constructor<*>>

    fun getFields(clazz: Class<*>): Array<Field>

    fun getDeclaredFields(clazz: Class<*>): Array<Field>

    fun getMethods(clazz: Class<*>): Array<Method>

    fun getDeclaredMethods(clazz: Class<*>): Array<Method>
}
