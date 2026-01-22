package com.rosan.installer.data.reflect.repo

/**
 * Extensions for ReflectRepo
 * Pattern: (Target: Any/Class) -> (Name: String) -> (Optional: Class/Types) -> (Args)
 */

inline fun <reified T> ReflectRepo.getStaticValue(name: String, clazz: Class<*>): T? =
    getStaticFieldValue(name, clazz) as? T

inline fun <reified T> ReflectRepo.getValue(obj: Any, name: String, clazz: Class<*>? = null): T? =
    getFieldValue(obj, name, clazz ?: obj.javaClass) as? T

inline fun <reified T> ReflectRepo.invoke(
    obj: Any,
    name: String,
    clazz: Class<*>? = null,
    parameterTypes: Array<Class<*>> = emptyArray(),
    vararg args: Any?
): T? = invokeMethod(obj, name, clazz ?: obj.javaClass, parameterTypes, *args) as? T

inline fun <reified T> ReflectRepo.invokeStatic(
    name: String,
    clazz: Class<*>,
    parameterTypes: Array<Class<*>> = emptyArray(),
    vararg args: Any?
): T? = invokeStaticMethod(name, clazz, parameterTypes, *args) as? T
