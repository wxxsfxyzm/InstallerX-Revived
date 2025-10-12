package com.rosan.installer.data.reflect.model.impl

import com.rosan.installer.data.reflect.repo.ReflectRepo
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method

class ReflectRepoImpl : ReflectRepo {
    // These methods are already direct passthroughs and are fine.
    override fun getConstructors(clazz: Class<*>): Array<Constructor<*>> = clazz.constructors
    override fun getDeclaredConstructors(clazz: Class<*>): Array<Constructor<*>> = clazz.declaredConstructors
    override fun getFields(clazz: Class<*>): Array<Field> = clazz.fields
    override fun getDeclaredFields(clazz: Class<*>): Array<Field> = clazz.declaredFields
    override fun getMethods(clazz: Class<*>): Array<Method> = clazz.methods
    override fun getDeclaredMethods(clazz: Class<*>): Array<Method> = clazz.declaredMethods

    /**
     * Optimized: Directly calls clazz.getConstructor instead of looping.
     */
    override fun getConstructor(clazz: Class<*>, vararg parameterTypes: Class<*>): Constructor<*>? {
        return try {
            clazz.getConstructor(*parameterTypes)
        } catch (e: NoSuchMethodException) {
            null
        }
    }

    /**
     * Optimized: Directly calls clazz.getDeclaredConstructor instead of looping.
     */
    override fun getDeclaredConstructor(
        clazz: Class<*>,
        vararg parameterTypes: Class<*>
    ): Constructor<*>? {
        return try {
            clazz.getDeclaredConstructor(*parameterTypes)
        } catch (e: NoSuchMethodException) {
            null
        }
    }

    /**
     * Optimized: Directly calls clazz.getField instead of looping.
     */
    override fun getField(clazz: Class<*>, name: String): Field? {
        return try {
            clazz.getField(name)
        } catch (e: NoSuchFieldException) {
            null
        }
    }

    /**
     * Optimized: Directly calls clazz.getDeclaredField instead of looping.
     */
    override fun getDeclaredField(clazz: Class<*>, name: String): Field? {
        return try {
            clazz.getDeclaredField(name)
        } catch (e: NoSuchFieldException) {
            null
        }
    }

    /**
     * Optimized: Directly calls clazz.getMethod instead of looping.
     */
    override fun getMethod(
        clazz: Class<*>,
        name: String,
        vararg parameterTypes: Class<*>
    ): Method? {
        return try {
            clazz.getMethod(name, *parameterTypes)
        } catch (e: NoSuchMethodException) {
            null
        }
    }

    /**
     * Optimized: Directly calls clazz.getDeclaredMethod instead of looping.
     */
    override fun getDeclaredMethod(
        clazz: Class<*>,
        name: String,
        vararg parameterTypes: Class<*>
    ): Method? {
        return try {
            clazz.getDeclaredMethod(name, *parameterTypes)
        } catch (e: NoSuchMethodException) {
            null
        }
    }
}