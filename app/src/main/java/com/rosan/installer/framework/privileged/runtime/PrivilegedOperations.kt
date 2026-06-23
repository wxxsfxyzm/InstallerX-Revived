// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.framework.privileged.runtime

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import com.rosan.installer.ICommandOutputListener

/**
 * In-process privileged operation surface.
 *
 * Binder hook backends use this interface directly. Only operations that truly need a
 * remote process are also exposed through IPrivilegedService.
 */
interface PrivilegedOperations {
    fun delete(paths: Array<out String>)
    fun performDexOpt(packageName: String, compilerFilter: String, force: Boolean): Boolean
    fun setDefaultInstaller(component: ComponentName, enable: Boolean)
    fun startActivityPrivileged(intent: Intent): Boolean
    fun sendBroadcastPrivileged(intent: Intent): Boolean
    fun execArr(command: Array<String>): String
    fun execArrWithCallback(command: Array<String>, listener: ICommandOutputListener?)
    fun setAdbVerify(enabled: Boolean)
    fun grantRuntimePermission(packageName: String, permission: String)
    fun isPermissionGranted(packageName: String, permission: String): Boolean
    fun getUsers(): Map<Int, String>
    fun getSessionDetails(sessionId: Int): Bundle?
    fun setPackageNetworkingEnabled(uid: Int, enabled: Boolean)
    fun prepareUnknownSourceAppOp(uid: Int, packageName: String): Int
}
