package com.rosan.installer.data.reflect.repo

/**
 * Extensions for ReflectRepo
 */
inline fun <reified T> ReflectRepo.getStaticValue(clazz: Class<*>, name: String): T? =
    getStaticFieldValue(clazz, name) as? T

inline fun <reified T> ReflectRepo.getValue(obj: Any, name: String): T? =
    getFieldValue(obj, obj.javaClass, name) as? T

inline fun <reified T> ReflectRepo.getValue(obj: Any, clazz: Class<*>, name: String): T? =
    getFieldValue(obj, clazz, name) as? T

inline fun <reified T> ReflectRepo.invoke(
    obj: Any,
    name: String,
    parameterTypes: Array<Class<*>> = emptyArray(),
    vararg args: Any?
): T? = invokeMethod(obj, obj.javaClass, name, parameterTypes, *args) as? T

inline fun <reified T> ReflectRepo.invokeStatic(
    clazz: Class<*>,
    name: String,
    parameterTypes: Array<Class<*>> = emptyArray(),
    vararg args: Any?
): T? = invokeStaticMethod(clazz, name, parameterTypes, *args) as? T