package com.rosan.installer.data.reflect.repo

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method

interface ReflectRepo {
    fun getConstructors(clazz: Class<*>): Array<Constructor<*>>

    fun getDeclaredConstructors(clazz: Class<*>): Array<Constructor<*>>

    fun getFields(clazz: Class<*>): Array<Field>

    fun getDeclaredFields(clazz: Class<*>): Array<Field>

    fun getMethods(clazz: Class<*>): Array<Method>

    fun getDeclaredMethods(clazz: Class<*>): Array<Method>

    fun getConstructor(clazz: Class<*>, vararg parameterTypes: Class<*>): Constructor<*>?

    fun getDeclaredConstructor(
        clazz: Class<*>, vararg parameterTypes: Class<*>
    ): Constructor<*>?

    fun getField(clazz: Class<*>, name: String): Field?

    fun getDeclaredField(clazz: Class<*>, name: String): Field?

    fun getMethod(clazz: Class<*>, name: String, vararg parameterTypes: Class<*>): Method?

    fun getDeclaredMethod(clazz: Class<*>, name: String, vararg parameterTypes: Class<*>): Method?

    fun getStaticObjectField(clazz: Class<*>, fieldName: String): Any
}