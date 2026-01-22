package com.rosan.installer.data.reflect.model.impl

import com.rosan.installer.data.reflect.repo.ReflectRepo
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

class ReflectRepoImpl : ReflectRepo {
    private val fieldCache = ConcurrentHashMap<String, Field>()
    private val methodCache = ConcurrentHashMap<String, Method>()
    private val constructorCache = ConcurrentHashMap<String, Constructor<*>>()

    @Suppress("UNCHECKED_CAST")
    override fun getConstructors(clazz: Class<*>): Array<Constructor<*>> =
        clazz.constructors.onEach { it.isAccessible = true } as Array<Constructor<*>>

    @Suppress("UNCHECKED_CAST")
    override fun getDeclaredConstructors(clazz: Class<*>): Array<Constructor<*>> =
        clazz.declaredConstructors.onEach { it.isAccessible = true } as Array<Constructor<*>>

    @Suppress("UNCHECKED_CAST")
    override fun getFields(clazz: Class<*>): Array<Field> =
        clazz.fields.onEach { it.isAccessible = true } as Array<Field>

    @Suppress("UNCHECKED_CAST")
    override fun getDeclaredFields(clazz: Class<*>): Array<Field> =
        clazz.declaredFields.onEach { it.isAccessible = true } as Array<Field>

    @Suppress("UNCHECKED_CAST")
    override fun getMethods(clazz: Class<*>): Array<Method> =
        clazz.methods.onEach { it.isAccessible = true } as Array<Method>

    @Suppress("UNCHECKED_CAST")
    override fun getDeclaredMethods(clazz: Class<*>): Array<Method> =
        clazz.declaredMethods.onEach { it.isAccessible = true } as Array<Method>

    override fun getConstructor(clazz: Class<*>, vararg parameterTypes: Class<*>): Constructor<*>? {
        val key = clazz.name + parameterTypes.joinToString(prefix = "(", postfix = ")") { it.name }
        return constructorCache.getOrPut(key) {
            try {
                clazz.getConstructor(*parameterTypes).apply { isAccessible = true }
            } catch (_: NoSuchMethodException) {
                null
            }
        }
    }

    override fun getDeclaredConstructor(
        clazz: Class<*>,
        vararg parameterTypes: Class<*>
    ): Constructor<*>? {
        val key = "decl:" + clazz.name + parameterTypes.joinToString(prefix = "(", postfix = ")") { it.name }
        return constructorCache.getOrPut(key) {
            try {
                clazz.getDeclaredConstructor(*parameterTypes).apply { isAccessible = true }
            } catch (_: NoSuchMethodException) {
                null
            }
        }
    }

    override fun getField(clazz: Class<*>, name: String): Field? {
        val key = clazz.name + "#" + name
        return fieldCache.getOrPut(key) {
            try {
                clazz.getField(name).apply { isAccessible = true }
            } catch (_: NoSuchFieldException) {
                null
            }
        }
    }

    override fun getDeclaredField(clazz: Class<*>, name: String): Field? {
        val key = "decl:" + clazz.name + "#" + name
        return fieldCache.getOrPut(key) {
            try {
                clazz.getDeclaredField(name).apply { isAccessible = true }
            } catch (_: NoSuchFieldException) {
                null
            }
        }
    }

    override fun getMethod(
        clazz: Class<*>,
        name: String,
        vararg parameterTypes: Class<*>
    ): Method? {
        val key = clazz.name + "#" + name + parameterTypes.joinToString(prefix = "(", postfix = ")") { it.name }
        return methodCache.getOrPut(key) {
            try {
                clazz.getMethod(name, *parameterTypes).apply { isAccessible = true }
            } catch (_: NoSuchMethodException) {
                null
            }
        }
    }

    override fun getDeclaredMethod(
        clazz: Class<*>,
        name: String,
        vararg parameterTypes: Class<*>
    ): Method? {
        val key = "decl:" + clazz.name + "#" + name + parameterTypes.joinToString(prefix = "(", postfix = ")") { it.name }
        return methodCache.getOrPut(key) {
            try {
                clazz.getDeclaredMethod(name, *parameterTypes).apply { isAccessible = true }
            } catch (_: NoSuchMethodException) {
                null
            }
        }
    }

    override fun getFieldValue(obj: Any?, clazz: Class<*>, name: String): Any? {
        return (getDeclaredField(clazz, name) ?: getField(clazz, name))?.get(obj)
    }

    override fun setFieldValue(obj: Any?, clazz: Class<*>, name: String, value: Any?) {
        (getDeclaredField(clazz, name) ?: getField(clazz, name))?.set(obj, value)
    }

    override fun getStaticFieldValue(clazz: Class<*>, name: String): Any? {
        return getFieldValue(null, clazz, name)
    }

    override fun setStaticFieldValue(clazz: Class<*>, name: String, value: Any?) {
        setFieldValue(null, clazz, name, value)
    }

    override fun invokeMethod(
        obj: Any?,
        clazz: Class<*>,
        name: String,
        parameterTypes: Array<Class<*>>,
        vararg args: Any?
    ): Any? {
        val method = getDeclaredMethod(clazz, name, *parameterTypes)
            ?: getMethod(clazz, name, *parameterTypes)
        return method?.invoke(obj, *args)
    }

    override fun invokeStaticMethod(
        clazz: Class<*>,
        name: String,
        parameterTypes: Array<Class<*>>,
        vararg args: Any?
    ): Any? {
        return invokeMethod(null, clazz, name, parameterTypes, *args)
    }

    private inline fun <K : Any, V : Any> ConcurrentHashMap<K, V>.getOrPut(key: K, defaultValue: () -> V?): V? {
        val existing = get(key)
        if (existing != null) return existing
        val newValue = defaultValue() ?: return null
        return putIfAbsent(key, newValue) ?: newValue
    }
}
