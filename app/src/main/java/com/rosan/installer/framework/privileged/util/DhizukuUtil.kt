// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.framework.privileged.util

import android.content.pm.PackageManager
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener
import com.rosan.installer.domain.privileged.exception.PrivilegedException
import com.rosan.installer.domain.privileged.model.PrivilegedErrorType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

suspend fun <T> requireDhizukuPermissionGranted(action: suspend () -> T): T {
    callbackFlow {
        Dhizuku.init()
        if (Dhizuku.isPermissionGranted()) send(Unit)
        else {
            Dhizuku.requestPermission(object : DhizukuRequestPermissionListener() {
                override fun onRequestPermission(grantResult: Int) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) trySend(Unit)
                    else close(Exception("dhizuku permission denied"))
                }
            })
        }
        awaitClose()
    }.catch {
        throw PrivilegedException(
            errorType = PrivilegedErrorType.DHIZUKU_NOT_WORK,
            message = "Dhizuku not work",
            cause = it
        )
    }.first()
    return action()
}
