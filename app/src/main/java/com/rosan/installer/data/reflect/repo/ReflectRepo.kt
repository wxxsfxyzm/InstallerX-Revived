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

    fun getField(name: String, clazz: Class<*>): Field?

    fun getDeclaredField(name: String, clazz: Class<*>): Field?

    fun getMethod(name: String, clazz: Class<*>, vararg parameterTypes: Class<*>): Method?

    fun getDeclaredMethod(name: String, clazz: Class<*>, vararg parameterTypes: Class<*>): Method?

    // --- High-level Property Accessors ---

    fun getFieldValue(obj: Any?, name: String, clazz: Class<*>): Any?

    fun setFieldValue(obj: Any?, name: String, clazz: Class<*>, value: Any?)

    fun getStaticFieldValue(name: String, clazz: Class<*>): Any?

    fun setStaticFieldValue(name: String, clazz: Class<*>, value: Any?)

    // --- High-level Method Invocation ---

    fun invokeMethod(
        obj: Any?,
        name: String,
        clazz: Class<*>,
        parameterTypes: Array<Class<*>> = emptyArray(),
        vararg args: Any?
    ): Any?

    fun invokeStaticMethod(
        name: String,
        clazz: Class<*>,
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
