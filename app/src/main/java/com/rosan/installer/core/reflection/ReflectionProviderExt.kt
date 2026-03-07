// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.core.reflection

/**
 * Extensions for ReflectionProvider
 * Pattern: (Target: Any/Class) -> (Name: String) -> (Optional: Class/Types) -> (Args)
 */

inline fun <reified T> ReflectionProvider.getStaticValue(name: String, clazz: Class<*>): T? =
    getStaticFieldValue(name, clazz) as? T

inline fun <reified T> ReflectionProvider.getValue(obj: Any, name: String, clazz: Class<*>? = null): T? =
    getFieldValue(obj, name, clazz ?: obj.javaClass) as? T

inline fun <reified T> ReflectionProvider.invoke(
    obj: Any,
    name: String,
    clazz: Class<*>? = null,
    parameterTypes: Array<Class<*>> = emptyArray(),
    vararg args: Any?
): T? = invokeMethod(obj, name, clazz ?: obj.javaClass, parameterTypes, *args) as? T

inline fun <reified T> ReflectionProvider.invokeStatic(
    name: String,
    clazz: Class<*>,
    parameterTypes: Array<Class<*>> = emptyArray(),
    vararg args: Any?
): T? = invokeStaticMethod(name, clazz, parameterTypes, *args) as? T
