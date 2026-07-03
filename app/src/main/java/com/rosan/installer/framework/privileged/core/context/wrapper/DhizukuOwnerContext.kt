// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.framework.privileged.core.context.wrapper

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import com.rosan.dhizuku.api.Dhizuku

class DhizukuOwnerContext private constructor(
    base: Context,
    private val ownerPackageName: String
) : ContextWrapper(base) {
    override fun getPackageName() = ownerPackageName

    override fun getOpPackageName() = ownerPackageName

    companion object {
        fun create(base: Context): DhizukuOwnerContext {
            val ownerPackageName = Dhizuku.getOwnerPackageName()
            val ownerContext = try {
                base.createPackageContext(ownerPackageName, CONTEXT_IGNORE_SECURITY)
            } catch (e: PackageManager.NameNotFoundException) {
                throw IllegalStateException("Dhizuku owner package is not installed: $ownerPackageName", e)
            }
            return DhizukuOwnerContext(ownerContext, ownerPackageName)
        }
    }
}

fun Context.createDhizukuOwnerContext(): DhizukuOwnerContext =
    DhizukuOwnerContext.create(this)
